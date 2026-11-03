package com.skillinghelper.plunder;

import com.skillinghelper.engine.GuideStep;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/**
 * Follows a Pyramid Plunder round: keeps the room's urns, chest, sarcophagus, trap and doors (from spawn events), reads
 * their states each tick and picks the one object to click next. Outside it remembers which pyramid door led to the
 * Guardian mummy and which led to an empty room. Everything runs on the client thread.
 */
@Singleton
public class PlunderTracker
{
	private enum Kind
	{
		URN,
		CHEST,
		SARCOPHAGUS,
		TRAP,
		DOOR,
		EXIT,
		ENTRANCE,
		EMPTY_ROOM_EXIT,
	}

	private static final Map<Integer, Kind> KINDS = new HashMap<>();

	static
	{
		for (int id : new int[]{
			ObjectID.NTK_URN_TYPE1_MULTI_1, ObjectID.NTK_URN_TYPE1_MULTI_2, ObjectID.NTK_URN_TYPE1_MULTI_3,
			ObjectID.NTK_URN_TYPE1_MULTI_4, ObjectID.NTK_URN_TYPE1_MULTI_5, ObjectID.NTK_URN_TYPE2_MULTI_6,
			ObjectID.NTK_URN_TYPE2_MULTI_7, ObjectID.NTK_URN_TYPE2_MULTI_8, ObjectID.NTK_URN_TYPE2_MULTI_9,
			ObjectID.NTK_URN_TYPE2_MULTI_10, ObjectID.NTK_URN_TYPE3_MULTI_11, ObjectID.NTK_URN_TYPE3_MULTI_12,
			ObjectID.NTK_URN_TYPE3_MULTI_13, ObjectID.NTK_URN_TYPE3_MULTI_14, ObjectID.NTK_URN_TYPE3_MULTI_15})
		{
			KINDS.put(id, Kind.URN);
		}
		KINDS.put(ObjectID.NTK_GOLDEN_CHEST_MULTI, Kind.CHEST);
		KINDS.put(ObjectID.NTK_SARCOPHAGUS_MULTI, Kind.SARCOPHAGUS);
		KINDS.put(ObjectID.NTK_SPEARTRAP_INMOTION, Kind.TRAP);
		for (int id : new int[]{ObjectID.NTK_TOMB_DOOR1, ObjectID.NTK_TOMB_DOOR2, ObjectID.NTK_TOMB_DOOR3,
			ObjectID.NTK_TOMB_DOOR4})
		{
			KINDS.put(id, Kind.DOOR);
		}
		KINDS.put(ObjectID.NTK_TOMB_DOOR_EXIT, Kind.EXIT);
		for (int id : new int[]{ObjectID.NTK_PYRAMID_DOOR_NORTH_MULTI, ObjectID.NTK_PYRAMID_DOOR_EAST_MULTI,
			ObjectID.NTK_PYRAMID_DOOR_SOUTH_MULTI, ObjectID.NTK_PYRAMID_DOOR_WEST_MULTI})
		{
			KINDS.put(id, Kind.ENTRANCE);
		}
		KINDS.put(ObjectID.NTK_ANTECHAMBER_EXIT, Kind.EMPTY_ROOM_EXIT);
	}

	private static final Set<Integer> URN_SNAKE = Set.of(ObjectID.NTK_URN1_SNAKE, ObjectID.NTK_URN2_SNAKE,
		ObjectID.NTK_URN3_SNAKE);
	private static final Set<Integer> URN_CHARMED = Set.of(ObjectID.NTK_URN1_SNAKE_CHARMED,
		ObjectID.NTK_URN2_SNAKE_CHARMED, ObjectID.NTK_URN3_SNAKE_CHARMED);
	private static final Set<Integer> URN_OPEN = Set.of(ObjectID.NTK_URN1_OPEN, ObjectID.NTK_URN2_OPEN,
		ObjectID.NTK_URN3_OPEN);

	/**
	 * The middle of the four pyramid doors in Sophanem.
	 */
	private static final WorldPoint PYRAMID = new WorldPoint(3288, 2794, 0);
	private static final int NEAR_PYRAMID = 20;
	/**
	 * How long after leaving a pyramid door an arrival in a pyramid room still counts as coming through it.
	 */
	private static final int ENTRY_TICKS = 10;
	/**
	 * Ticks to wait for the Guardian mummy to appear before calling the room empty.
	 */
	private static final int MUMMY_WAIT_TICKS = 3;

	private final Client client;
	private final Map<TileObject, Kind> objects = new HashMap<>();
	/**
	 * This tick's gold chests, one per room.
	 */
	private final List<TileObject> chests = new ArrayList<>();
	private NPC mummy;

	// The way in
	private int correctDoor = -1;
	private long correctAt;
	private final Map<Integer, Long> wrongDoors = new HashMap<>();
	private int lastNearDoor = -1;
	private int lastNearTick;
	private int pendingDoor = -1;
	private int pendingTicks;
	private boolean wasInsidePyramid;

	// The round
	private int room;
	private TileObject urnTarget;
	/**
	 * The planned urns in walking order, the next one first.
	 */
	private List<TileObject> urnRoute = new ArrayList<>();
	private int routeCount = -1;
	private final Set<NPC> chasers = new HashSet<>();
	private TileObject doorTarget;
	private boolean sarcophagusSearched;

	/**
	 * The object to click next, or null.
	 */
	@Getter
	private TileObject highlight;
	/**
	 * The menu option to click on {@link #highlight}.
	 */
	@Getter
	private String highlightOption;
	/**
	 * The Pyramid Plunder step while the player is at or in the pyramid, or null elsewhere.
	 */
	@Getter
	private GuideStep step;
	@Getter
	private boolean inRound;
	/**
	 * Whether to number {@link #getUrnRoute()} in the room: only while urns are the next thing to do.
	 */
	@Getter
	private boolean urnsNext;
	/**
	 * Put "Check for Snakes" and "Charm Snake" on left-click on urns.
	 */
	private boolean swapUrns;
	private boolean swapEnabled = true;
	private boolean charmOn;

	@Inject
	public PlunderTracker(Client client)
	{
		this.client = client;
	}

	public void clear()
	{
		objects.clear();
		chests.clear();
		chasers.clear();
		urnRoute = new ArrayList<>();
		routeCount = -1;
		mummy = null;
		highlight = null;
		step = null;
		inRound = false;
		urnTarget = null;
		doorTarget = null;
	}

	/**
	 * Reads the scene once, for a plugin started while already logged in.
	 */
	public void rescan()
	{
		clear();
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return;
		}
		for (NPC npc : wv.npcs())
		{
			addNpc(npc);
		}
		for (Tile[][] plane : wv.getScene().getTiles())
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null)
					{
						continue;
					}
					for (GameObject go : tile.getGameObjects())
					{
						add(go);
					}
					add(tile.getWallObject());
				}
			}
		}
	}

	/**
	 * Call every game tick.
	 *
	 * @param active      whether the Pyramid Plunder guide is open
	 * @param sceptreHunt also open every chest and the sarcophagi from room 4
	 */
	public void tick(boolean active, boolean sceptreHunt)
	{
		highlight = null;
		highlightOption = null;
		step = null;
		urnsNext = false;
		swapUrns = false;
		Player player = client.getLocalPlayer();
		WorldPoint me = player == null ? null : player.getWorldLocation();
		if (me == null)
		{
			inRound = false;
			return;
		}
		boolean inRegion = me.getRegionID() == PyramidPlunder.REGION;
		int timer = client.getVarbitValue(VarbitID.NTK_PLAYER_TIMER_COUNT);
		inRound = inRegion && me.getPlane() == 0 && timer > 0;
		// The Guardian mummy's room and the three empty ones are upstairs in the same region
		boolean insideRoom = inRegion && me.getPlane() >= 2;
		boolean lobby = insideRoom && mummyNear(me);
		trackEntrance(me, insideRoom, lobby);
		if (!inRound)
		{
			room = 0;
			urnRoute = new ArrayList<>();
			routeCount = -1;
			urnTarget = null;
			doorTarget = null;
			sarcophagusSearched = false;
		}
		if (!active)
		{
			return;
		}

		int level = client.getRealSkillLevel(Skill.THIEVING);
		boolean charm = carries(PyramidPlunder.SNAKE_CHARM);
		boolean lockpick = carries(PyramidPlunder.LOCKPICK);
		swapUrns = inRound && swapEnabled && charm && PyramidPlunder.useCharm(level, charmOn);
		if (inRound)
		{
			roundTick(me, level, charm, lockpick, timer, sceptreHunt);
		}
		else if (lobby)
		{
			ItemContainer inv = client.getItemContainer(InventoryID.INV);
			step = PyramidPlunder.startStep(level, charmOn, charm, lockpick, sceptreHunt,
				PyramidPlunder.canCharge(carries(PyramidPlunder.UNCHARGED_SCEPTRE), count(PyramidPlunder.GOLD_ARTEFACTS),
					count(PyramidPlunder.STONE_ARTEFACTS), count(PyramidPlunder.POTTERY_ARTEFACTS)),
				inv == null ? 28 : 28 - inv.count(), client.getBoostedSkillLevel(Skill.HITPOINTS),
				client.getRealSkillLevel(Skill.PRAYER));
		}
		else if (insideRoom)
		{
			step = PyramidPlunder.emptyRoomStep();
			pick(nearest(me, Kind.EMPTY_ROOM_EXIT, null), "Leave");
		}
		else if (me.getPlane() == 0 && me.distanceTo2D(PYRAMID) <= NEAR_PYRAMID)
		{
			int sceptre = sceptre();
			TileObject door = sceptre > 0 ? null : entrance(me);
			pick(door, "Search");
			step = PyramidPlunder.enterStep(level, charmOn, charm, lockpick, sceptre, door != null && knownCorrect(door));
		}
	}

	private void roundTick(WorldPoint me, int level, boolean charm, boolean lockpick, int timer, boolean sceptreHunt)
	{
		chests.clear();
		for (Map.Entry<TileObject, Kind> e : objects.entrySet())
		{
			if (e.getValue() == Kind.CHEST)
			{
				chests.add(e.getKey());
			}
		}
		int r = client.getVarbitValue(VarbitID.NTK_ROOM_NUMBER);
		if (r != room)
		{
			room = r;
			urnTarget = null;
			urnRoute = new ArrayList<>();
			routeCount = -1;
			doorTarget = null;
			sarcophagusSearched = false;
		}
		TileObject chest = nearest(me, Kind.CHEST, null);
		List<TileObject> urns = new ArrayList<>();
		TileObject sarcophagus = null;
		int doorsClosed = 0;
		for (Map.Entry<TileObject, Kind> e : objects.entrySet())
		{
			TileObject obj = e.getKey();
			if (!inRoom(obj, chest))
			{
				continue;
			}
			switch (e.getValue())
			{
				case URN:
					if (urnState(obj) != PyramidPlunder.UrnState.OPEN)
					{
						urns.add(obj);
					}
					break;
				case SARCOPHAGUS:
					sarcophagus = obj;
					break;
				case DOOR:
					if (state(obj) == ObjectID.NTK_TOMB_DOOR_NOANIM)
					{
						doorsClosed++;
					}
					break;
				default:
					break;
			}
		}

		if (doorTarget == null || !inRoom(doorTarget, chest) || state(doorTarget) != ObjectID.NTK_TOMB_DOOR_NOANIM)
		{
			doorTarget = nearest(me, Kind.DOOR, chest, ObjectID.NTK_TOMB_DOOR_NOANIM);
		}
		int sarcState = sarcophagus == null ? -1 : state(sarcophagus);
		TileObject exit = nearest(me, Kind.EXIT, chest);

		PyramidPlunder.Situation sit0 = PyramidPlunder.Situation.builder()
			.level(level)
			.room(r)
			.ticksLeft(PyramidPlunder.ROUND_TICKS - timer)
			.trapActive(client.getVarbitValue(VarbitID.NTK_TRAP_ACTIVE) == 1)
			.urnsLeft(urns.size())
			.chestClosed(chest != null && state(chest) == ObjectID.NTK_GOLDEN_CHEST_CLOSED)
			.sarcophagusClosed(sarcState == ObjectID.NTK_SARCOPHAGUS)
			.sarcophagusOpen(sarcState == ObjectID.NTK_SARCOPHAGUS_OPEN && !sarcophagusSearched)
			.doorsClosed(doorsClosed)
			.carriesCharm(charm)
			.charmOn(charmOn)
			.carriesLockpick(lockpick)
			.exitInView(exit != null)
			.inventoryFull(inventoryFull())
			.sceptreHunt(sceptreHunt)
			.prayerLevel(client.getRealSkillLevel(Skill.PRAYER))
			.threat(threat(player()))
			.build();
		updateRoute(me, urns, PyramidPlunder.urnsToSearch(sit0));
		urnTarget = urnRoute.isEmpty() ? null : urnRoute.get(0);
		PyramidPlunder.Situation sit = sit0.toBuilder()
			.nextUrn(urnTarget == null ? null : urnState(urnTarget))
			.build();
		PyramidPlunder.Action action = PyramidPlunder.decide(sit);
		urnsNext = action == PyramidPlunder.Action.CHECK_URN || action == PyramidPlunder.Action.CHARM_URN
			|| action == PyramidPlunder.Action.SEARCH_URN;
		switch (action)
		{
			case PASS_TRAP:
				pick(nearest(me, Kind.TRAP, chest), "Pass");
				break;
			case CHECK_URN:
				pick(urnTarget, "Check for Snakes");
				break;
			case CHARM_URN:
				pick(urnTarget, "Charm Snake");
				break;
			case SEARCH_URN:
				pick(urnTarget, "Search");
				break;
			case OPEN_CHEST:
				pick(chest, "Search");
				break;
			case OPEN_SARCOPHAGUS:
				pick(sarcophagus, "Open");
				break;
			case SEARCH_SARCOPHAGUS:
				pick(sarcophagus, "Search");
				break;
			case PICK_DOOR:
				pick(doorTarget, "Pick-lock");
				break;
			case LEAVE:
			default:
				pick(exit, "Quick-leave");
				break;
		}
		step = PyramidPlunder.step(sit, action);
	}

	/**
	 * Replans the walk through the urns when one gets searched or the plan wants a different number of them. The
	 * urn being worked on stays first while it's still closed, so the outline doesn't jump between two close urns.
	 */
	private void updateRoute(WorldPoint me, List<TileObject> closed, int count)
	{
		count = Math.min(count, closed.size());
		if (count == routeCount && closed.containsAll(urnRoute) && urnRoute.size() == count)
		{
			return;
		}
		routeCount = count;
		TileObject keep = !urnRoute.isEmpty() && closed.contains(urnRoute.get(0)) && count > 0 ? urnRoute.get(0) : null;
		List<TileObject> rest = new ArrayList<>(closed);
		rest.remove(keep);
		WorldPoint from = keep != null ? keep.getWorldLocation() : me;
		int left = keep != null ? count - 1 : count;
		int[] xs = new int[rest.size()];
		int[] ys = new int[rest.size()];
		for (int i = 0; i < rest.size(); i++)
		{
			xs[i] = rest.get(i).getWorldLocation().getX();
			ys[i] = rest.get(i).getWorldLocation().getY();
		}
		List<TileObject> route = new ArrayList<>();
		if (keep != null)
		{
			route.add(keep);
		}
		for (int i : UrnWalk.order(from.getX(), from.getY(), xs, ys, left))
		{
			route.add(rest.get(i));
		}
		urnRoute = route;
	}

	/**
	 * The planned urns in walking order, the next one first.
	 */
	public List<TileObject> getUrnRoute()
	{
		return Collections.unmodifiableList(urnRoute);
	}

	private Player player()
	{
		return client.getLocalPlayer();
	}

	private PyramidPlunder.Threat threat(Player me)
	{
		for (NPC npc : chasers)
		{
			if (me != null && npc.getInteracting() == me)
			{
				return npc.getId() == NpcID.NTK_SCARAB_SWARM ? PyramidPlunder.Threat.SWARM
					: PyramidPlunder.Threat.MUMMY;
			}
		}
		return PyramidPlunder.Threat.NONE;
	}

	private void pick(TileObject obj, String option)
	{
		highlight = obj;
		highlightOption = obj == null ? null : option;
	}

	// ---- The way in ----

	/**
	 * Remembers the pyramid door the player last stood at, and once they arrive inside, whether it led to the mummy.
	 */
	private void trackEntrance(WorldPoint me, boolean insideRoom, boolean lobby)
	{
		int tick = client.getTickCount();
		if (!insideRoom && me.getPlane() == 0 && me.distanceTo2D(PYRAMID) <= NEAR_PYRAMID)
		{
			TileObject door = nearest(me, Kind.ENTRANCE, null);
			if (door != null && door.getWorldLocation().distanceTo2D(me) <= 2)
			{
				lastNearDoor = door.getId();
				lastNearTick = tick;
			}
		}
		if (insideRoom && !wasInsidePyramid && lastNearDoor != -1 && tick - lastNearTick <= ENTRY_TICKS)
		{
			pendingDoor = lastNearDoor;
			pendingTicks = MUMMY_WAIT_TICKS;
		}
		wasInsidePyramid = insideRoom;
		if (pendingDoor == -1)
		{
			return;
		}
		long now = Instant.now().getEpochSecond();
		if (lobby)
		{
			correctDoor = pendingDoor;
			correctAt = now;
			wrongDoors.remove(pendingDoor);
			pendingDoor = -1;
		}
		else if (!insideRoom || --pendingTicks <= 0)
		{
			wrongDoors.put(pendingDoor, now);
			if (correctDoor == pendingDoor)
			{
				correctDoor = -1;
			}
			pendingDoor = -1;
		}
	}

	private boolean knownCorrect(TileObject door)
	{
		return door.getId() == correctDoor
			&& Instant.now().getEpochSecond() - correctAt < PyramidPlunder.ENTRANCE_SECONDS;
	}

	/**
	 * The door that led to the mummy within the last 25 minutes, else the nearest one not seen leading to an empty
	 * room in that time.
	 */
	private TileObject entrance(WorldPoint me)
	{
		long now = Instant.now().getEpochSecond();
		TileObject best = null;
		TileObject any = null;
		int bestDist = Integer.MAX_VALUE;
		int anyDist = Integer.MAX_VALUE;
		for (Map.Entry<TileObject, Kind> e : objects.entrySet())
		{
			if (e.getValue() != Kind.ENTRANCE)
			{
				continue;
			}
			TileObject door = e.getKey();
			if (knownCorrect(door))
			{
				return door;
			}
			int d = door.getWorldLocation().distanceTo2D(me);
			if (d < anyDist)
			{
				any = door;
				anyDist = d;
			}
			Long wrongAt = wrongDoors.get(door.getId());
			if ((wrongAt == null || now - wrongAt >= PyramidPlunder.ENTRANCE_SECONDS) && d < bestDist)
			{
				best = door;
				bestDist = d;
			}
		}
		return best != null ? best : any;
	}

	private boolean mummyNear(WorldPoint me)
	{
		WorldPoint at = mummy == null ? null : mummy.getWorldLocation();
		return at != null && at.getPlane() == me.getPlane() && at.distanceTo2D(me) <= 15;
	}

	// ---- Objects ----

	/**
	 * Each room has one gold chest, so an object belongs to the room whose chest is closest to it.
	 */
	private boolean inRoom(TileObject obj, TileObject roomChest)
	{
		if (roomChest == null)
		{
			return true;
		}
		WorldPoint at = obj.getWorldLocation();
		long mine = dist2(at, roomChest.getWorldLocation());
		for (TileObject other : chests)
		{
			if (other != roomChest && other.getPlane() == obj.getPlane()
				&& dist2(at, other.getWorldLocation()) < mine)
			{
				return false;
			}
		}
		return true;
	}

	private TileObject nearest(WorldPoint me, Kind kind, TileObject roomChest)
	{
		return nearest(me, kind, roomChest, -1);
	}

	/**
	 * The closest object of this kind on the player's plane, in the chest's room, in the given state (-1 for any).
	 */
	private TileObject nearest(WorldPoint me, Kind kind, TileObject roomChest, int wantState)
	{
		List<TileObject> found = new ArrayList<>();
		for (Map.Entry<TileObject, Kind> e : objects.entrySet())
		{
			TileObject obj = e.getKey();
			if (e.getValue() == kind && obj.getPlane() == me.getPlane()
				&& (roomChest == null || inRoom(obj, roomChest)) && (wantState == -1 || state(obj) == wantState))
			{
				found.add(obj);
			}
		}
		return closest(me, found);
	}

	private static TileObject closest(WorldPoint me, List<TileObject> objs)
	{
		TileObject best = null;
		long bestDist = Long.MAX_VALUE;
		for (TileObject obj : objs)
		{
			long d = dist2(obj.getWorldLocation(), me);
			if (d < bestDist)
			{
				best = obj;
				bestDist = d;
			}
		}
		return best;
	}

	private static long dist2(WorldPoint a, WorldPoint b)
	{
		long dx = a.getX() - b.getX();
		long dy = a.getY() - b.getY();
		return dx * dx + dy * dy;
	}

	/**
	 * The id of the state an object with several states is in now, e.g. a closed or opened chest.
	 */
	private int state(TileObject obj)
	{
		ObjectComposition def = client.getObjectDefinition(obj.getId());
		if (def != null && def.getImpostorIds() != null)
		{
			ObjectComposition now = def.getImpostor();
			return now == null ? -1 : now.getId();
		}
		return obj.getId();
	}

	private PyramidPlunder.UrnState urnState(TileObject urn)
	{
		int id = state(urn);
		if (URN_OPEN.contains(id))
		{
			return PyramidPlunder.UrnState.OPEN;
		}
		if (URN_CHARMED.contains(id))
		{
			return PyramidPlunder.UrnState.CHARMED;
		}
		if (URN_SNAKE.contains(id))
		{
			return PyramidPlunder.UrnState.SNAKE;
		}
		return PyramidPlunder.UrnState.CLOSED;
	}

	private boolean carries(int id)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		return inv != null && inv.contains(id);
	}

	/**
	 * How many of these the inventory holds, notes included.
	 */
	private int count(int[] ids)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv == null)
		{
			return 0;
		}
		int n = 0;
		for (Item item : inv.getItems())
		{
			for (int id : ids)
			{
				if (item.getId() == id)
				{
					n += item.getQuantity();
				}
			}
		}
		return n;
	}

	private boolean inventoryFull()
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		return inv != null && inv.count() >= 28;
	}

	/**
	 * A charged Pharaoh's sceptre carried or worn, or 0.
	 */
	private int sceptre()
	{
		for (int container : new int[]{InventoryID.INV, InventoryID.WORN})
		{
			ItemContainer c = client.getItemContainer(container);
			if (c == null)
			{
				continue;
			}
			for (int id : PyramidPlunder.SCEPTRES)
			{
				if (c.contains(id))
				{
					return id;
				}
			}
		}
		return 0;
	}

	private void add(TileObject obj)
	{
		if (obj == null)
		{
			return;
		}
		Kind kind = KINDS.get(obj.getId());
		if (kind != null)
		{
			objects.put(obj, kind);
		}
	}

	private static final Set<Integer> CHASER_IDS = Set.of(NpcID.NTK_SCARAB_SWARM, NpcID.NTK_MUMMY_1,
		NpcID.NTK_MUMMY_2, NpcID.NTK_MUMMY_3, NpcID.NTK_MUMMY_4, NpcID.NTK_MUMMY_5);

	private void addNpc(NPC npc)
	{
		if (npc == null)
		{
			return;
		}
		if (npc.getId() == NpcID.NTK_MUMMY_GUARDIAN)
		{
			mummy = npc;
		}
		else if (CHASER_IDS.contains(npc.getId()))
		{
			chasers.add(npc);
		}
	}

	// ---- Events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOADING || e.getGameState() == GameState.LOGIN_SCREEN
			|| e.getGameState() == GameState.HOPPING)
		{
			objects.clear();
			chasers.clear();
			mummy = null;
			urnTarget = null;
			urnRoute = new ArrayList<>();
			routeCount = -1;
			doorTarget = null;
			highlight = null;
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		add(e.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		objects.remove(e.getGameObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned e)
	{
		add(e.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned e)
	{
		objects.remove(e.getWallObject());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		addNpc(e.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		if (e.getNpc() == mummy)
		{
			mummy = null;
		}
		chasers.remove(e.getNpc());
	}

	public void setSwapEnabled(boolean on)
	{
		swapEnabled = on;
	}

	public void setCharmOn(boolean on)
	{
		charmOn = on;
	}

	/**
	 * While charming snakes, an urn's left-click is "Charm Snake" when it has a snake and "Check for Snakes" while
	 * it's closed, so the whole urn is left-clicks. Only reorders the urn's own options, like Menu Entry Swapper.
	 */
	@Subscribe(priority = -1)
	public void onPostMenuSort(PostMenuSort e)
	{
		if (!swapUrns || client.isMenuOpen() || client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}
		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		for (String option : URN_SWAPS)
		{
			for (int i = entries.length - 1; i >= 0; i--)
			{
				MenuEntry entry = entries[i];
				if (KINDS.get(entry.getIdentifier()) != Kind.URN
					|| !option.equalsIgnoreCase(Text.removeTags(entry.getOption())))
				{
					continue;
				}
				if (i != entries.length - 1)
				{
					entries[i] = entries[entries.length - 1];
					entries[entries.length - 1] = entry;
					menu.setMenuEntries(entries);
				}
				return;
			}
		}
	}

	private static final String[] URN_SWAPS = {"Charm Snake", "Check for Snakes"};

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		// The opened sarcophagus looks the same before and after its search
		if (inRound && e.getMenuAction() != MenuAction.WALK && e.getId() == ObjectID.NTK_SARCOPHAGUS_MULTI
			&& "Search".equalsIgnoreCase(e.getMenuOption()))
		{
			sarcophagusSearched = true;
		}
	}
}
