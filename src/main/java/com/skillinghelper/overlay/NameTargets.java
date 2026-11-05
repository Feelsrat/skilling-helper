package com.skillinghelper.overlay;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
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
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.eventbus.Subscribe;

/**
 * Keeps the NPCs and objects a running guide might outline, by name, from spawn events rather than scanning the
 * scene every frame. Only the names the guide asks for are tracked. Each guide makes its own.
 */
public class NameTargets
{
	private final Client client;
	private final Set<String> npcNames = new HashSet<>();
	private final Set<String> objectNames = new HashSet<>();
	private final Map<NPC, String> npcs = new HashMap<>();
	private final Map<TileObject, String> objects = new HashMap<>();

	public NameTargets(Client client)
	{
		this.client = client;
	}

	/**
	 * Tracks these names from now on, picking up what's already loaded. Empty sets stop tracking. Client thread.
	 */
	public void track(Set<String> npcs, Set<String> objects)
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

	public Collection<Map.Entry<NPC, String>> getNpcs()
	{
		return npcs.entrySet();
	}

	public Collection<Map.Entry<TileObject, String>> getObjects()
	{
		return objects.entrySet();
	}

	public void clear()
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
		// Every floor: rooftop courses and upstairs objects are on other planes than the player
		for (Tile[][] plane : wv.getScene().getTiles())
		{
			for (Tile[] row : plane)
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
					addObject(tile.getWallObject());
					addObject(tile.getDecorativeObject());
					addObject(tile.getGroundObject());
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

	private void addObject(TileObject o)
	{
		if (o == null || objectNames.isEmpty())
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
}
