package com.skillinghelper.plunder;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemHighlight;
import com.skillinghelper.engine.StepType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.gameval.ItemID;

/**
 * The Pyramid Plunder round plan and the one thing to click next. Kept free of client references so it can be unit
 * tested. Strategy from the OSRS wiki "Pyramid Plunder" page (chests from room 4, urns in the deepest rooms, chests
 * last before leaving a room, a snake charm below 51 Thieving, a lockpick for the doors). The clock budgeting idea,
 * replanning every tick and giving the time left to whatever pays the most xp per tick, comes from Nubles' Codex
 * Plunder Pilot (BSD-2-Clause).
 */
public final class PyramidPlunder
{
	public static final String METHOD_ID = "thieve_pyramid_plunder";
	/**
	 * The eight plunder rooms.
	 */
	public static final int REGION = 7749;
	public static final int ROOMS = 8;
	/**
	 * A round lasts five minutes.
	 */
	public static final int ROUND_TICKS = 500;
	/**
	 * Typical time to get through a room without looting it (trap, walk, finding the door), and to pass just the
	 * last room's trap.
	 */
	static final int PASS_TICKS = 25;
	static final int TRAP_TICKS = 6;
	/**
	 * Typical time per search, failed tries and the walk between included.
	 */
	static final int URN_TICKS = 10;
	/**
	 * Checking, charming and then searching takes three clicks instead of one.
	 */
	static final int CHARMED_URN_TICKS = 14;
	static final int CHEST_TICKS = 6;
	static final int SARCOPHAGUS_TICKS = 12;
	/**
	 * Held back so the last search isn't cut off by the clock.
	 */
	static final int MARGIN_TICKS = 10;
	/**
	 * Hit points below which a snake bite and its poison start to hurt.
	 */
	static final int LOW_HP = 12;
	/**
	 * Below this the snake charm makes urns faster; above it checking for snakes only slows you down.
	 */
	public static final int SNAKE_CHARM_BELOW = 51;
	/**
	 * Below this a lockpick is clearly worth it: doors open far more often, for half the door xp.
	 */
	public static final int LOCKPICK_BELOW = 71;
	/**
	 * The correct way into the pyramid changes every 25 minutes.
	 */
	public static final long ENTRANCE_SECONDS = 25 * 60;

	static final int[] URN_XP = {60, 90, 150, 215, 300, 450, 675, 825};
	static final int[] CHEST_XP = {40, 60, 100, 140, 200, 300, 450, 550};
	static final int[] DOOR_XP = {40, 60, 100, 140, 200, 300, 450, 0};
	static final int[] URNS = {13, 13, 12, 13, 13, 13, 13, 13};
	static final int[] SCEPTRE_ONE_IN = {4200, 2800, 1600, 950, 800, 750, 650, 650};

	public static final Location ALI = Location.of("Ali the Snake Charmer (Pollnivneach)", 3355, 2953, 4);
	public static final Target MONEY_POT = Target.object(null, "Money pot");
	public static final Target MUMMY = Target.npc("Start-minigame", "Guardian mummy");

	public static final int[] SCEPTRES = {
		ItemID.NTK_JEWELLED_SCEPTRE_1, ItemID.NTK_JEWELLED_SCEPTRE_2, ItemID.NTK_JEWELLED_SCEPTRE_3,
		ItemID.NTK_JEWELLED_SCEPTRE_4, ItemID.NTK_JEWELLED_SCEPTRE_5, ItemID.NTK_JEWELLED_SCEPTRE_6,
		ItemID.NTK_JEWELLED_SCEPTRE_7, ItemID.NTK_JEWELLED_SCEPTRE_8,
	};
	public static final int UNCHARGED_SCEPTRE = ItemID.NTK_JEWELLED_SCEPTRE_0;
	public static final int[] GOLD_ARTEFACTS = {ItemID.NTK_SEAL_GOLD, ItemID.NTK_SCARAB_GOLD,
		ItemID.NTK_STATUETTE_GOLD};
	public static final int[] STONE_ARTEFACTS = {ItemID.NTK_SEAL_STONE, ItemID.NTK_SCARAB_STONE,
		ItemID.NTK_STATUETTE_STONE};
	public static final int[] POTTERY_ARTEFACTS = {ItemID.NTK_IVORY_COMB, ItemID.NTK_SCARAB_POTTERY,
		ItemID.NTK_STATUETTE_POTTERY};
	public static final int SNAKE_CHARM = ItemID.SNAKE_FLUTE;
	public static final int LOCKPICK = ItemID.LOCKPICK;

	private PyramidPlunder()
	{
	}

	public enum UrnState
	{
		CLOSED,
		SNAKE,
		CHARMED,
		OPEN,
	}

	/**
	 * What to click next. Each comes with the object the overlay highlights.
	 */
	public enum Action
	{
		/**
		 * Outside: search the pyramid door that leads to the Guardian mummy.
		 */
		ENTER,
		/**
		 * In one of the three empty rooms behind a wrong door: leave through its door.
		 */
		LEAVE_EMPTY_ROOM,
		/**
		 * In the lobby: start a round with the Guardian mummy.
		 */
		START,
		PASS_TRAP,
		CHECK_URN,
		CHARM_URN,
		SEARCH_URN,
		OPEN_CHEST,
		OPEN_SARCOPHAGUS,
		SEARCH_SARCOPHAGUS,
		PICK_DOOR,
		LEAVE,
	}

	/**
	 * Something chasing the player.
	 */
	public enum Threat
	{
		NONE,
		SWARM,
		MUMMY,
	}

	/**
	 * What to loot in one room.
	 */
	@Value
	public static class RoomPlan
	{
		int room;
		/**
		 * Urns worth searching with the time left.
		 */
		int urns;
		/**
		 * Urns still closed in the room.
		 */
		int urnsInRoom;
		boolean chest;
		boolean sarcophagus;
		/**
		 * The last room you can enter: leave once it's done.
		 */
		boolean last;
	}

	/**
	 * Everything the next action depends on during a round.
	 */
	@Value
	@Builder(toBuilder = true)
	public static class Situation
	{
		int level;
		int room;
		int ticksLeft;
		boolean trapActive;
		/**
		 * Urns in this room not searched yet.
		 */
		int urnsLeft;
		/**
		 * The urn the overlay picked to do next, or null when there's none in view.
		 */
		UrnState nextUrn;
		boolean chestClosed;
		boolean sarcophagusClosed;
		/**
		 * Opened but not searched yet.
		 */
		boolean sarcophagusOpen;
		int doorsClosed;
		boolean carriesCharm;
		boolean carriesLockpick;
		boolean exitInView;
		boolean inventoryFull;
		boolean sceptreHunt;
		int prayerLevel;
		@Builder.Default
		Threat threat = Threat.NONE;
	}

	/**
	 * The deepest room this Thieving level opens: room 1 at 21, one more every 10 levels, room 8 at 91.
	 */
	public static int maxRoom(int level)
	{
		if (level < 21)
		{
			return 0;
		}
		return Math.min(ROOMS, 1 + (level - 21) / 10);
	}

	public static int roomLevel(int room)
	{
		return 21 + (room - 1) * 10;
	}

	public static boolean useCharm(int level)
	{
		return level < SNAKE_CHARM_BELOW;
	}

	public static boolean useLockpick(int level)
	{
		return level < LOCKPICK_BELOW;
	}

	private static final class Option
	{
		final int room;
		final int kind;
		final int tier;
		final int ticks;
		final double density;

		Option(int room, int kind, int tier, int ticks, double value)
		{
			this.room = room;
			this.kind = kind;
			this.tier = tier;
			this.ticks = ticks;
			this.density = value / ticks;
		}
	}

	private static final int URN = 0;
	private static final int CHEST = 1;
	private static final int SARCOPHAGUS = 2;

	/**
	 * The rest of the round, from the current room to the deepest one. Getting through each room costs a fixed time;
	 * the time left goes to the searches that pay the most per tick, so the deepest rooms' urns win and shallow rooms
	 * are walked through. Called every tick, so a slow room shortens what follows.
	 *
	 * @param room the room the player is in, or 0 before the round starts
	 */
	public static List<RoomPlan> plan(int level, int room, boolean trapDone, int urnsLeft, boolean chestDone,
		boolean sarcophagusDone, int ticksLeft, boolean sceptreHunt)
	{
		return plan(level, room, trapDone, urnsLeft, chestDone, sarcophagusDone, ticksLeft, sceptreHunt, false);
	}

	public static List<RoomPlan> plan(int level, int room, boolean trapDone, int urnsLeft, boolean chestDone,
		boolean sarcophagusDone, int ticksLeft, boolean sceptreHunt, boolean charming)
	{
		int urnTicks = charming && useCharm(level) ? CHARMED_URN_TICKS : URN_TICKS;
		int top = Math.max(1, maxRoom(level));
		int start = room < 1 ? 1 : Math.min(room, top);
		boolean fresh = room < 1;
		int fixed = 0;
		List<Option> options = new ArrayList<>();
		for (int r = start; r <= top; r++)
		{
			boolean here = r == start && !fresh;
			boolean trapPassed = here && trapDone;
			if (r == top)
			{
				fixed += trapPassed ? 0 : TRAP_TICKS;
			}
			else
			{
				fixed += trapPassed ? PASS_TICKS - TRAP_TICKS : PASS_TICKS;
			}
			// The wiki keeps urns to the last two rooms: earlier ones pay too little for the walk
			int urns = r < top - 1 ? 0 : here ? urnsLeft : URNS[r - 1];
			for (int u = 0; u < urns; u++)
			{
				options.add(new Option(r, URN, 1, urnTicks, URN_XP[r - 1]));
			}
			if (!(here && chestDone))
			{
				options.add(sceptreHunt
					? new Option(r, CHEST, 0, CHEST_TICKS, 1.0 / SCEPTRE_ONE_IN[r - 1])
					: new Option(r, CHEST, r >= 4 ? 0 : 1, CHEST_TICKS, CHEST_XP[r - 1]));
			}
			if (sceptreHunt && r >= 4 && !(here && sarcophagusDone))
			{
				options.add(new Option(r, SARCOPHAGUS, 0, SARCOPHAGUS_TICKS, 1.0 / SCEPTRE_ONE_IN[r - 1]));
			}
		}
		options.sort(Comparator.<Option>comparingInt(o -> o.tier).thenComparing(o -> -o.density));
		int budget = ticksLeft - fixed - MARGIN_TICKS;
		int[] urns = new int[ROOMS + 1];
		boolean[] chest = new boolean[ROOMS + 1];
		boolean[] sarcophagus = new boolean[ROOMS + 1];
		for (Option o : options)
		{
			if (o.ticks > budget)
			{
				continue;
			}
			budget -= o.ticks;
			if (o.kind == URN)
			{
				urns[o.room]++;
			}
			else if (o.kind == CHEST)
			{
				chest[o.room] = true;
			}
			else
			{
				sarcophagus[o.room] = true;
			}
		}
		List<RoomPlan> plans = new ArrayList<>();
		for (int r = start; r <= top; r++)
		{
			boolean here = r == start && !fresh;
			plans.add(new RoomPlan(r, urns[r], here ? urnsLeft : URNS[r - 1], chest[r], sarcophagus[r], r == top));
		}
		return plans;
	}

	/**
	 * The plan for a whole fresh round.
	 */
	public static List<RoomPlan> freshPlan(int level, boolean sceptreHunt)
	{
		return plan(level, 0, false, 0, false, false, ROUND_TICKS, sceptreHunt);
	}

	static List<RoomPlan> plan(Situation s)
	{
		return plan(s.level, Math.max(1, s.room), !s.trapActive, s.urnsLeft, !s.chestClosed,
			!s.sarcophagusClosed && !s.sarcophagusOpen, s.ticksLeft, s.sceptreHunt, s.carriesCharm);
	}

	/**
	 * The next thing to click inside a round.
	 */
	public static Action decide(Situation s)
	{
		return decide(s, plan(s).get(0));
	}

	private static Action decide(Situation s, RoomPlan here)
	{
		if (s.trapActive)
		{
			return Action.PASS_TRAP;
		}
		if (here.urns > 0 && s.nextUrn != null && s.nextUrn != UrnState.OPEN)
		{
			if (useCharm(s.level) && s.carriesCharm)
			{
				switch (s.nextUrn)
				{
					case CLOSED:
						return Action.CHECK_URN;
					case SNAKE:
						return Action.CHARM_URN;
					default:
						break;
				}
			}
			return Action.SEARCH_URN;
		}
		// Chests and sarcophagi go last: a scarab swarm or mummy is easier to leave behind on the way out
		if (here.chest && s.chestClosed)
		{
			return Action.OPEN_CHEST;
		}
		if (here.sarcophagus && s.sarcophagusOpen)
		{
			return Action.SEARCH_SARCOPHAGUS;
		}
		if (here.sarcophagus && s.sarcophagusClosed)
		{
			return Action.OPEN_SARCOPHAGUS;
		}
		return here.last ? Action.LEAVE : Action.PICK_DOOR;
	}

	/**
	 * How many urns the plan wants searched in the player's room now, for numbering the walk between them.
	 */
	public static int urnsToSearch(Situation s)
	{
		return s.trapActive && s.urnsLeft == 0 ? 0 : plan(s).get(0).urns;
	}

	/**
	 * The panel step for a round: what to click, why, and the plan for the whole round.
	 */
	public static GuideStep step(Situation s, Action a)
	{
		int maxRoom = Math.max(1, maxRoom(s.level));
		int r = Math.max(1, Math.min(ROOMS, s.room));
		List<RoomPlan> plans = plan(s);
		RoomPlan here = plans.get(0);
		GuideStep.GuideStepBuilder b = GuideStep.builder().type(StepType.TRAIN);
		String time = "Room " + r + " of " + maxRoom + ", " + timeLeft(s.ticksLeft) + " left.";
		threatLine(b, s);
		switch (a)
		{
			case PASS_TRAP:
				b.title("Pass the speartrap");
				b.line("Click the outlined speartrap: \"Pass\" it before walking on, or it hits you.");
				break;
			case CHECK_URN:
				b.title("Check the urn for snakes");
				b.line("Click \"Check for Snakes\" on the outlined urn. With \"Left-click snake options\" on (the "
					+ "default), that and \"Charm Snake\" are the urn's left-click.");
				b.line("Next: \"Charm Snake\", then \"Search\". If the check fails, just check again.");
				b.line("Note: you never click the snake charm itself. Carrying it gives the urn its \"Charm Snake\" "
					+ "option.");
				b.line("Why: below " + SNAKE_CHARM_BELOW + " Thieving the charm saves failed searches and poison, "
					+ "for the same xp.");
				break;
			case CHARM_URN:
				b.title("Charm the snake");
				b.line("Click \"Charm Snake\" on the outlined urn (not the charm in your inventory), then "
					+ "\"Search\" it.");
				break;
			case SEARCH_URN:
				b.title("Search the urn");
				b.line("Click \"Search\" on the outlined urn (" + URN_XP[r - 1] + " xp). " + here.urns
					+ (here.urns == 1 ? " urn" : " urns") + " to search here; the numbers show the shortest walk.");
				if (s.nextUrn == UrnState.CLOSED && useCharm(s.level) && !s.carriesCharm)
				{
					b.line("Tip: a snake charm from Ali in Pollnivneach makes urns safer below "
						+ SNAKE_CHARM_BELOW + " Thieving.");
				}
				else if (s.nextUrn == UrnState.CLOSED && !useCharm(s.level))
				{
					b.line("Just search: at your level checking for snakes is slower than the odd bite.");
				}
				break;
			case OPEN_CHEST:
				b.title("Open the gold chest");
				b.line("Click \"Search\" on the outlined Grand Gold Chest (" + CHEST_XP[r - 1] + " xp).");
				b.line(s.prayerLevel >= 43
					? "Tip: turn on Protect from Melee first in case scarabs come out."
					: "If scarabs come out, move on: they only follow you in this room.");
				break;
			case OPEN_SARCOPHAGUS:
				b.title("Open the sarcophagus");
				b.line("Click \"Open\" on the outlined sarcophagus: Strength xp and a sceptre chance.");
				b.line("A mummy can rise out of it. Pick a door straight after.");
				break;
			case SEARCH_SARCOPHAGUS:
				b.title("Search the sarcophagus");
				b.line("Click \"Search\" on the outlined sarcophagus for its artefact and sceptre roll.");
				break;
			case PICK_DOOR:
				b.title("Pick the tomb door");
				b.line("Click \"Pick-lock\" on the outlined Tomb Door (" + (s.carriesLockpick
					? DOOR_XP[r - 1] / 2 + " xp with your lockpick" : DOOR_XP[r - 1] + " xp") + ").");
				b.line("One door goes on; if it's a dead end the next door gets outlined. After three wrong doors the "
					+ "fourth always works.");
				if (s.carriesLockpick)
				{
					b.line("Note: your lockpick works by itself when you pick the lock; you never click it.");
				}
				if (!s.carriesLockpick && useLockpick(s.level))
				{
					b.line("Tip: bring a lockpick next round: doors open much more often at your level.");
				}
				if (here.urnsInRoom > 0 && here.urns == 0 && !s.trapActive && plans.size() > 1)
				{
					b.line("Leave these urns: the time left pays more in the deeper rooms.");
				}
				break;
			case LEAVE:
			default:
				b.title("Leave and start a new round");
				b.line(s.exitInView ? "Right-click the outlined exit Tomb Door and \"Quick-leave\"."
					: "Leave through any exit Tomb Door (\"Quick-leave\"), or teleport out.");
				b.line("A fresh round restarts the 5-minute timer, which beats waiting this one out.");
				break;
		}
		if (s.inventoryFull)
		{
			b.line("Warning: your inventory is full. Drop a pottery or stone artefact (or eat) to keep looting; "
				+ "keep gold ones and ivory combs, they're worth the most.");
		}
		b.line(time);
		b.line(goalLine(s.sceptreHunt));
		for (String line : roundPlan(plans, r))
		{
			b.line(line);
		}
		return b.build();
	}

	static String goalLine(boolean sceptreHunt)
	{
		return sceptreHunt
			? "Goal: the Pharaoh's sceptre (every chest, sarcophagi from room 4). Turn off \"Hunt the sceptre\" in "
				+ "the settings for the most xp."
			: "Goal: the most Thieving xp. Turn on \"Hunt the sceptre\" in the settings to go for the sceptre.";
	}

	private static void threatLine(GuideStep.GuideStepBuilder b, Situation s)
	{
		if (s.threat == Threat.NONE)
		{
			return;
		}
		String what = s.threat == Threat.SWARM ? "A scarab swarm" : "A mummy";
		b.line("Warning: " + what + " is attacking you. " + (s.prayerLevel >= 43
			? "Turn on Protect from Melee, then carry on." : "Eat if you need to; it stays behind when you go "
			+ "through the next door."));
	}

	/**
	 * One line per group of rooms with the same plan, marking the room the player is in.
	 *
	 * @param currentRoom 0 before the round starts
	 */
	public static List<String> roundPlan(List<RoomPlan> plans, int currentRoom)
	{
		List<String> out = new ArrayList<>();
		if (!plans.isEmpty() && plans.get(0).room > 1)
		{
			int done = plans.get(0).room - 1;
			out.add("✔ " + (done == 1 ? "Room 1" : "Rooms 1-" + done) + ": done.");
		}
		int i = 0;
		while (i < plans.size())
		{
			RoomPlan first = plans.get(i);
			int j = i;
			while (j + 1 < plans.size() && first.urns == 0 && !first.last && first.room != currentRoom
				&& same(first, plans.get(j + 1)) && plans.get(j + 1).room != currentRoom)
			{
				j++;
			}
			int end = plans.get(j).room;
			String marker = first.room == currentRoom ? "➜ " : "• ";
			String rooms = first.room == end ? "Room " + first.room : "Rooms " + first.room + "-" + end;
			out.add(marker + rooms + ": " + describe(first));
			i = j + 1;
		}
		return out;
	}

	private static boolean same(RoomPlan a, RoomPlan b)
	{
		return a.urns == b.urns && a.chest == b.chest && a.sarcophagus == b.sarcophagus && a.last == b.last;
	}

	static String describe(RoomPlan p)
	{
		List<String> parts = new ArrayList<>();
		parts.add("trap");
		if (p.urns > 0)
		{
			parts.add(p.urns >= p.urnsInRoom ? "every urn" : p.urns + (p.urns == 1 ? " urn" : " urns"));
		}
		if (p.chest)
		{
			parts.add("chest");
		}
		if (p.sarcophagus)
		{
			parts.add("sarcophagus");
		}
		parts.add(p.last ? "leave" : "door");
		return String.join(", then ", parts) + ".";
	}

	/**
	 * "4:05" style time left.
	 */
	public static String timeLeft(int ticks)
	{
		int seconds = Math.max(0, ticks) * 6 / 10;
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}

	// ---- Before a round ----

	/**
	 * The player is in the lobby: start a round.
	 */
	public static GuideStep startStep(int level, boolean carriesCharm, boolean carriesLockpick, boolean sceptreHunt,
		boolean canCharge, int freeSlots, int hitpoints, int prayerLevel)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.type(StepType.TRAIN)
			.title("Start a round")
			.highlight(MUMMY)
			.line("Right-click the Guardian mummy and \"Start-minigame\".");
		if (canCharge)
		{
			b.line("First: \"Talk-to\" the mummy to charge your Pharaoh's sceptre with artefacts. Then it teleports "
				+ "you straight back here each round.");
			b.itemHighlight(UNCHARGED_SCEPTRE, ItemHighlight.USE);
		}
		prepLines(b, level, carriesCharm, carriesLockpick);
		List<RoomPlan> fresh = freshPlan(level, sceptreHunt);
		int searches = 0;
		for (RoomPlan p : fresh)
		{
			searches += p.urns + (p.chest ? 1 : 0) + (p.sarcophagus ? 1 : 0);
		}
		if (freeSlots < searches)
		{
			b.line("Warning: only " + freeSlots + " free slots. A round like this finds about " + searches
				+ " artefacts, and a full inventory stops the loot.");
		}
		if (hitpoints < LOW_HP)
		{
			b.line("Warning: low hitpoints. Urn snakes bite for up to 4 and poison, so eat first.");
		}
		if (carriesLockpick && !useLockpick(level))
		{
			b.line("Tip: at your level the lockpick halves door xp for few saved tries; you can leave it in the bank.");
		}
		if (carriesCharm && !useCharm(level))
		{
			b.line("Tip: from " + SNAKE_CHARM_BELOW + " Thieving the snake charm only slows you down; leave it.");
		}
		if (prayerLevel >= 43)
		{
			b.line("Tip: Protect from Melee stops all damage from scarab swarms and mummies.");
		}
		b.line(goalLine(sceptreHunt));
		for (String line : roundPlan(fresh, 0))
		{
			b.line(line);
		}
		return b.build();
	}

	/**
	 * Outside the pyramid in Sophanem: teleport in with a sceptre, or search the door that leads to the mummy.
	 *
	 * @param knownDoor whether the right door is known (seen within the last 25 minutes)
	 */
	public static GuideStep enterStep(int level, boolean carriesCharm, boolean carriesLockpick, int sceptreId,
		boolean knownDoor)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder().type(StepType.TRAIN);
		if (sceptreId > 0)
		{
			b.title("Teleport in with the sceptre")
				.line("Rub your Pharaoh's sceptre and pick Jalsavrah: it puts you next to the Guardian mummy.")
				.itemHighlight(sceptreId, ItemHighlight.USE);
		}
		else if (knownDoor)
		{
			b.title("Enter the pyramid")
				.line("\"Search\" the outlined door: it led to the Guardian mummy last time.");
		}
		else
		{
			b.title("Find the way in")
				.line("\"Search\" the outlined pyramid door. Only one of the four leads to the Guardian mummy.")
				.line("If you land in an empty room, leave and the next door gets outlined. The right door stays the "
					+ "same for about 25 minutes.");
		}
		prepLines(b, level, carriesCharm, carriesLockpick);
		return b.build();
	}

	/**
	 * Behind a wrong door: nothing here, so leave and try the next door.
	 */
	public static GuideStep emptyRoomStep()
	{
		return GuideStep.builder()
			.type(StepType.TRAIN)
			.title("Wrong door: leave this room")
			.line("This room is empty. Click the outlined door to leave, then search the next outlined pyramid door.")
			.line("Leaving always puts you outside the north door.")
			.build();
	}

	private static void prepLines(GuideStep.GuideStepBuilder b, int level, boolean carriesCharm,
		boolean carriesLockpick)
	{
		if (useCharm(level) && !carriesCharm)
		{
			b.line("Bring: a snake charm. Below " + SNAKE_CHARM_BELOW + " Thieving it makes urns faster and safer. "
				+ "Just carry it; the guide tells you when to charm.");
		}
		if (useLockpick(level) && !carriesLockpick)
		{
			b.line("Bring: a lockpick. Doors open far more often at your level, for half the door xp. Just carry "
				+ "it; picking a lock uses it by itself.");
		}
	}

	/**
	 * An uncharged sceptre can be charged by the Guardian mummy for 6 gold, 12 stone or 24 pottery/ivory artefacts.
	 */
	public static boolean canCharge(boolean hasUnchargedSceptre, int gold, int stone, int pottery)
	{
		return hasUnchargedSceptre && (gold >= 6 || stone >= 12 || pottery >= 24);
	}

	/**
	 * What to do with the artefacts, added when banking them.
	 */
	public static List<String> artefactLines()
	{
		return List.of(
			"Money: sell the artefacts on the Grand Exchange. It pays about the same as Simon Templeton and is "
				+ "quicker; ivory combs are worth much more there.",
			"Tip: with a Pharaoh's sceptre, keep some to charge it: 6 gold, 12 stone or 24 pottery/ivory "
				+ "artefacts per charge, from the Guardian mummy.");
	}

	/**
	 * Below 51 Thieving without a snake charm anywhere: Ali in Pollnivneach gives one for a coin, on the way.
	 */
	public static boolean needsCharm(int level, boolean bankKnown, int owned)
	{
		return useCharm(level) && bankKnown && owned == 0;
	}

	public static GuideStep charmSourceStep(boolean atAli)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.type(atAli ? StepType.GET_ITEMS : StepType.WALK)
			.title(atAli ? "Get a snake charm" : "Get a snake charm from Ali")
			.highlight(MONEY_POT)
			.line("Use 1 coin on the Money pot next to Ali the Snake Charmer, south of the bar in Pollnivneach. He "
				+ "gives you a snake charm and a snake basket.")
			.line("Below " + SNAKE_CHARM_BELOW + " Thieving the charm stops urn snakes biting, which saves time and "
				+ "food. Pollnivneach is on the carpet route to Sophanem.");
		return b.build();
	}
}
