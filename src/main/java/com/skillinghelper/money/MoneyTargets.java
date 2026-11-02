package com.skillinghelper.money;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;

/**
 * Keeps the NPCs and objects the running money guide might outline, from spawn events rather than scanning the
 * scene. Only names the guide can use are tracked.
 */
@Singleton
class MoneyTargets
{
	private final Client client;
	private final Set<String> npcNames = new HashSet<>();
	private final Set<String> objectNames = new HashSet<>();
	private final Map<NPC, String> npcs = new HashMap<>();
	private final Map<GameObject, String> objects = new HashMap<>();

	@Inject
	MoneyTargets(Client client)
	{
		this.client = client;
	}

	/**
	 * Tracks these names from now on, picking up what's already loaded. Empty sets stop tracking. Client thread.
	 */
	void track(Set<String> npcs, Set<String> objects)
	{
		if (npcNames.equals(npcs) && objectNames.equals(objects))
		{
			return;
		}
		npcNames.clear();
		npcNames.addAll(npcs);
		objectNames.clear();
		objectNames.addAll(objects);
		rescan();
	}

	Collection<Map.Entry<NPC, String>> getNpcs()
	{
		return npcs.entrySet();
	}

	Collection<Map.Entry<GameObject, String>> getObjects()
	{
		return objects.entrySet();
	}

	void clear()
	{
		npcNames.clear();
		objectNames.clear();
		npcs.clear();
		objects.clear();
	}

	private void rescan()
	{
		npcs.clear();
		objects.clear();
		if (client.getGameState() != GameState.LOGGED_IN || (npcNames.isEmpty() && objectNames.isEmpty()))
		{
			return;
		}
		WorldView wv = client.getTopLevelWorldView();
		for (NPC npc : wv.npcs())
		{
			addNpc(npc);
		}
		if (objectNames.isEmpty())
		{
			return;
		}
		Tile[][] tiles = wv.getScene().getTiles()[wv.getPlane()];
		for (Tile[] row : tiles)
		{
			for (Tile tile : row)
			{
				if (tile == null)
				{
					continue;
				}
				for (GameObject o : tile.getGameObjects())
				{
					if (o != null)
					{
						addObject(o);
					}
				}
			}
		}
	}

	private void addNpc(NPC npc)
	{
		String name = npc.getName();
		if (name != null && npcNames.contains(name))
		{
			npcs.put(npc, name);
		}
	}

	private void addObject(GameObject o)
	{
		if (objectNames.isEmpty())
		{
			return;
		}
		ObjectComposition c = client.getObjectDefinition(o.getId());
		if (c != null && c.getImpostorIds() != null && c.getImpostor() != null)
		{
			c = c.getImpostor();
		}
		if (c != null && objectNames.contains(c.getName()))
		{
			objects.put(o, c.getName());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOADING)
		{
			// Objects spawn again after the new area loads; NPCs that stay in view don't
			objects.clear();
		}
		else if (e.getGameState() == GameState.LOGIN_SCREEN || e.getGameState() == GameState.HOPPING)
		{
			npcs.clear();
			objects.clear();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		addNpc(e.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		npcs.remove(e.getNpc());
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
}
