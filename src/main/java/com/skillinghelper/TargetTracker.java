package com.skillinghelper;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.Target;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.eventbus.Subscribe;

/**
 * Keeps track of objects and NPCs any guide might want to highlight, using spawn events instead of scanning the
 * scene every frame. Only names used by guides (plus banks) are tracked, so the collections stay small.
 */
@Singleton
public class TargetTracker
{
	public static final List<Target> BANK_TARGETS = List.of(
		Target.object("Bank", "Bank booth"),
		Target.object(null, "Bank chest"),
		Target.object(null, "Bank deposit box"),
		Target.npc("Bank", "Banker", "Gnome banker", "Ghost banker", "Emerald Benedict")
	);

	@Value
	public static class Tracked<T>
	{
		T entity;
		String name;
		String[] actions;
	}

	private final Client client;
	private final Set<String> objectNames = new HashSet<>();
	private final Set<String> npcNames = new HashSet<>();
	private final Map<TileObject, Tracked<TileObject>> objects = new HashMap<>();
	private final Map<NPC, Tracked<NPC>> npcs = new HashMap<>();
	/**
	 * Which guide name each object with several states (by base object id) answers to, if any.
	 */
	private final Map<Integer, Match> impostorMatches = new HashMap<>();

	@AllArgsConstructor
	private static final class Match
	{
		final String name;
		final String[] actions;
	}

	private static final Match NO_MATCH = new Match(null, null);

	@Inject
	TargetTracker(Client client)
	{
		this.client = client;
	}

	/**
	 * Sets which names are worth tracking. Call before the scene loads (at plugin start).
	 */
	public void setInterestingTargets(Collection<Method> methods)
	{
		objectNames.clear();
		npcNames.clear();
		impostorMatches.clear();
		for (Method m : methods)
		{
			addNames(m.getTargets());
			// Where to make or fetch items: furnaces, the tanner, Otto's bed...
			for (ItemReq req : m.getTools())
			{
				addItemNames(req);
			}
			for (ItemReq req : m.getInputs())
			{
				addItemNames(req);
			}
		}
		addNames(BANK_TARGETS);
	}

	private void addItemNames(ItemReq req)
	{
		for (Target t : new Target[]{req.getSourceTarget(), req.getMakeTarget()})
		{
			if (t != null)
			{
				addNames(List.of(t));
			}
		}
	}

	private void addNames(List<Target> targets)
	{
		for (Target t : targets)
		{
			(t.getKind() == Target.Kind.OBJECT ? objectNames : npcNames).addAll(t.getNames());
		}
	}

	public Collection<Tracked<TileObject>> getObjects()
	{
		return Collections.unmodifiableCollection(objects.values());
	}

	public Collection<Tracked<NPC>> getNpcs()
	{
		return Collections.unmodifiableCollection(npcs.values());
	}

	public void clear()
	{
		objects.clear();
		npcs.clear();
	}

	/**
	 * Reads the whole scene once. Only used when the plugin starts while already logged in, since spawn events
	 * for the current scene have already fired.
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
		Tile[][] tiles = wv.getScene().getTiles()[wv.getPlane()];
		for (Tile[] column : tiles)
		{
			for (Tile tile : column)
			{
				if (tile == null)
				{
					continue;
				}
				for (GameObject go : tile.getGameObjects())
				{
					addObject(go);
				}
				addObject(tile.getWallObject());
				addObject(tile.getDecorativeObject());
				addObject(tile.getGroundObject());
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOADING || e.getGameState() == GameState.LOGIN_SCREEN
			|| e.getGameState() == GameState.HOPPING)
		{
			clear();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		addObject(e.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		objects.remove(e.getGameObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned e)
	{
		addObject(e.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned e)
	{
		objects.remove(e.getWallObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned e)
	{
		addObject(e.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned e)
	{
		objects.remove(e.getDecorativeObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned e)
	{
		addObject(e.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned e)
	{
		objects.remove(e.getGroundObject());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		addNpc(e.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged e)
	{
		npcs.remove(e.getNpc());
		addNpc(e.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		npcs.remove(e.getNpc());
	}

	private void addObject(TileObject obj)
	{
		if (obj == null || objectNames.isEmpty())
		{
			return;
		}
		ObjectComposition def = client.getObjectDefinition(obj.getId());
		if (def == null)
		{
			return;
		}
		if (def.getImpostorIds() != null)
		{
			// Farming patches and bird house spaces change name as they grow or get built ("Herb patch" becomes
			// "Herbs"), so match on any of the object's states and keep it tracked whatever state it's in
			Match match = impostorMatches.computeIfAbsent(obj.getId(), id -> impostorMatch(id));
			if (match != NO_MATCH)
			{
				objects.put(obj, new Tracked<>(obj, match.name, match.actions));
			}
			return;
		}
		String name = def.getName();
		if (name != null && objectNames.contains(name.toLowerCase(Locale.ROOT)))
		{
			objects.put(obj, new Tracked<>(obj, name, def.getActions()));
		}
	}

	private Match impostorMatch(int objectId)
	{
		ObjectComposition def = client.getObjectDefinition(objectId);
		ObjectComposition current = def.getImpostor();
		if (current != null && current.getName() != null
			&& objectNames.contains(current.getName().toLowerCase(Locale.ROOT)))
		{
			return new Match(current.getName(), current.getActions());
		}
		if (def.getName() != null && objectNames.contains(def.getName().toLowerCase(Locale.ROOT)))
		{
			return new Match(def.getName(), def.getActions());
		}
		for (int id : def.getImpostorIds())
		{
			if (id == -1)
			{
				continue;
			}
			ObjectComposition state = client.getObjectDefinition(id);
			if (state != null && state.getName() != null
				&& objectNames.contains(state.getName().toLowerCase(Locale.ROOT)))
			{
				return new Match(state.getName(), state.getActions());
			}
		}
		return NO_MATCH;
	}

	private void addNpc(NPC npc)
	{
		if (npc == null)
		{
			return;
		}
		NPCComposition def = npc.getTransformedComposition();
		if (def == null)
		{
			def = npc.getComposition();
		}
		if (def == null || def.getName() == null)
		{
			return;
		}
		if (npcNames.contains(def.getName().toLowerCase(Locale.ROOT)))
		{
			npcs.put(npc, new Tracked<>(npc, def.getName(), def.getActions()));
		}
	}
}
