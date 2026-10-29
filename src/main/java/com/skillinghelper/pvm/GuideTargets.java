package com.skillinghelper.pvm;

import com.skillinghelper.TargetTracker;
import com.skillinghelper.data.Target;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
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
 * Keeps the NPCs and objects the running guide might highlight, from spawn events rather than scanning the scene.
 * Only the guide's names (and banks) are tracked.
 */
@Singleton
class GuideTargets
{
	@Value
	static class Tracked<T>
	{
		T entity;
		String name;
		String[] actions;
	}

	private final Client client;
	private final Set<String> npcNames = new HashSet<>();
	private final Set<String> objectNames = new HashSet<>();
	private final Map<NPC, Tracked<NPC>> npcs = new HashMap<>();
	private final Map<TileObject, Tracked<TileObject>> objects = new HashMap<>();

	@Inject
	GuideTargets(Client client)
	{
		this.client = client;
	}

	/**
	 * Tracks these names from now on, rescanning what's already loaded. Empty sets stop tracking.
	 */
	void track(Set<String> npcs, Set<String> objects)
	{
		npcNames.clear();
		objectNames.clear();
		npcNames.addAll(npcs);
		objectNames.addAll(objects);
		if (!npcs.isEmpty() || !objects.isEmpty())
		{
			for (Target t : TargetTracker.BANK_TARGETS)
			{
				(t.getKind() == Target.Kind.NPC ? npcNames : objectNames).addAll(t.getNames());
			}
		}
		rescan();
	}

	Collection<Tracked<NPC>> getNpcs()
	{
		return Collections.unmodifiableCollection(npcs.values());
	}

	Collection<Tracked<TileObject>> getObjects()
	{
		return Collections.unmodifiableCollection(objects.values());
	}

	void clear()
	{
		npcs.clear();
		objects.clear();
	}

	private void rescan()
	{
		clear();
		if (client.getGameState() != GameState.LOGGED_IN || (npcNames.isEmpty() && objectNames.isEmpty()))
		{
			return;
		}
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return;
		}
		for (NPC npc : wv.npcs())
		{
			addNpc(npc);
		}
		for (Tile[] column : wv.getScene().getTiles()[wv.getPlane()])
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

	private void addNpc(NPC npc)
	{
		if (npc == null || npcNames.isEmpty())
		{
			return;
		}
		NPCComposition def = npc.getTransformedComposition();
		if (def == null)
		{
			def = npc.getComposition();
		}
		if (def != null && def.getName() != null && matches(npcNames, def.getName()))
		{
			npcs.put(npc, new Tracked<>(npc, def.getName(), def.getActions()));
		}
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
		if (def.getImpostorIds() != null && def.getImpostor() != null)
		{
			def = def.getImpostor();
		}
		if (def.getName() != null && matches(objectNames, def.getName()))
		{
			objects.put(obj, new Tracked<>(obj, def.getName(), def.getActions()));
		}
	}

	/**
	 * Exact names, or names containing a tracked word (for "moss giant" matching "Moss giant" variants).
	 */
	private static boolean matches(Set<String> names, String name)
	{
		String lower = name.replace(' ', ' ').toLowerCase(Locale.ROOT);
		if (names.contains(lower))
		{
			return true;
		}
		for (String n : names)
		{
			if (lower.contains(n))
			{
				return true;
			}
		}
		return false;
	}
}
