package com.skillinghelper.pvm;

import com.skillinghelper.pvm.guide.TunnelRoute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

/**
 * Works out the way through the Barrows tunnels to the chest from the doors that open right now. Which doors open
 * changes every run (and after a wrong puzzle answer), so it's read from the doors themselves each time: a door that
 * opens has an "Open" option, a locked one has none. Runs on the client thread.
 */
@Singleton
class BarrowsTunnel
{
	private static final Set<Integer> DOORS = Set.of(
		ObjectID.BARROWS_DOOR_A_R, ObjectID.BARROWS_DOOR_B_R, ObjectID.BARROWS_DOOR_C_R, ObjectID.BARROWS_DOOR_D_R,
		ObjectID.BARROWS_DOOR_E_R, ObjectID.BARROWS_DOOR_F_R, ObjectID.BARROWS_DOOR_G_R, ObjectID.BARROWS_DOOR_H_R,
		ObjectID.BARROWS_DOOR_I_R, ObjectID.BARROWS_DOOR_J_R, ObjectID.BARROWS_DOOR_K_R, ObjectID.BARROWS_DOOR_L_R,
		ObjectID.BARROWS_DOOR_M_R, ObjectID.BARROWS_DOOR_N_R, ObjectID.BARROWS_DOOR_O_R, ObjectID.BARROWS_DOOR_P_R,
		ObjectID.BARROWS_DOOR_A_L, ObjectID.BARROWS_DOOR_B_L, ObjectID.BARROWS_DOOR_C_L, ObjectID.BARROWS_DOOR_D_L,
		ObjectID.BARROWS_DOOR_E_L, ObjectID.BARROWS_DOOR_F_L, ObjectID.BARROWS_DOOR_G_L, ObjectID.BARROWS_DOOR_H_L,
		ObjectID.BARROWS_DOOR_I_L, ObjectID.BARROWS_DOOR_J_L, ObjectID.BARROWS_DOOR_K_L, ObjectID.BARROWS_DOOR_L_L,
		ObjectID.BARROWS_DOOR_M_L, ObjectID.BARROWS_DOOR_N_L, ObjectID.BARROWS_DOOR_O_L, ObjectID.BARROWS_DOOR_P_L,
		ObjectID.BARROWS_DOOR_UNLOCKED_R, ObjectID.BARROWS_DOOR_UNLOCKED_L);

	private final Client client;
	private final GuideTargets targets;

	@Inject
	BarrowsTunnel(Client client, GuideTargets targets)
	{
		this.client = client;
		this.targets = targets;
	}

	/**
	 * The way from the player to the chest, or null when it can't be worked out (chest not loaded, no way through).
	 */
	TunnelRoute route()
	{
		Player me = client.getLocalPlayer();
		WorldView wv = client.getTopLevelWorldView();
		if (me == null || wv == null || wv.getCollisionMaps() == null)
		{
			return null;
		}
		int plane = wv.getPlane();
		CollisionData map = wv.getCollisionMaps()[plane];
		LocalPoint here = me.getLocalLocation();
		if (map == null || here == null)
		{
			return null;
		}
		Map<Integer, Integer> open = new HashMap<>();
		Set<Integer> goals = new HashSet<>();
		int[][] flags = map.getFlags();
		for (GuideTargets.Tracked<TileObject> t : targets.getObjects())
		{
			TileObject obj = t.getEntity();
			if (obj.getPlane() != plane)
			{
				continue;
			}
			if (obj instanceof WallObject && DOORS.contains(obj.getId()) && opens(obj.getId()))
			{
				LocalPoint lp = obj.getLocalLocation();
				open.merge(TunnelPath.key(lp.getSceneX(), lp.getSceneY()), ((WallObject) obj).getOrientationA(),
					(a, b) -> a | b);
			}
			else if (obj instanceof GameObject && "chest".equalsIgnoreCase(t.getName()))
			{
				addAround((GameObject) obj, flags, goals);
			}
		}
		TunnelPath.Result r = TunnelPath.find(flags, open, here.getSceneX(), here.getSceneY(), goals);
		if (r == null)
		{
			return null;
		}
		// Up to and just past the next door; all the way when the chest is in reach
		int last = r.getDoorStep() < 0 ? r.getPath().size() - 1 : r.getDoorStep() + 1;
		List<WorldPoint> path = new ArrayList<>();
		for (int i = 0; i <= last; i++)
		{
			int[] tile = r.getPath().get(i);
			path.add(WorldPoint.fromScene(wv, tile[0], tile[1], plane));
		}
		WorldPoint door = r.getDoor() == null ? null : WorldPoint.fromScene(wv, r.getDoor()[0], r.getDoor()[1], plane);
		return new TunnelRoute(path, door, path.get(path.size() - 1));
	}

	/**
	 * A door whose current form has an "Open" option; locked ones have no options at all.
	 */
	private boolean opens(int id)
	{
		ObjectComposition def = client.getObjectDefinition(id);
		if (def != null && def.getImpostorIds() != null)
		{
			def = def.getImpostor();
		}
		if (def == null || def.getActions() == null)
		{
			return false;
		}
		for (String action : def.getActions())
		{
			if ("Open".equalsIgnoreCase(action))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * The free tiles touching the chest.
	 */
	private static void addAround(GameObject chest, int[][] flags, Set<Integer> goals)
	{
		Point min = chest.getSceneMinLocation();
		Point max = chest.getSceneMaxLocation();
		if (min == null || max == null)
		{
			return;
		}
		for (int x = min.getX() - 1; x <= max.getX() + 1; x++)
		{
			for (int y = min.getY() - 1; y <= max.getY() + 1; y++)
			{
				boolean inside = x >= min.getX() && x <= max.getX() && y >= min.getY() && y <= max.getY();
				if (!inside && x >= 0 && y >= 0 && x < flags.length && y < flags[x].length)
				{
					goals.add(TunnelPath.key(x, y));
				}
			}
		}
	}
}
