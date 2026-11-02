package com.skillinghelper.pvm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;
import net.runelite.api.CollisionDataFlag;

/**
 * Shortest walk over a scene's collision map, where walls can only be crossed at doors that open. Kept free of the
 * client so it can be tested.
 */
final class TunnelPath
{
	/**
	 * Wall edges, as in {@code WallObject.getOrientationA()}.
	 */
	static final int WEST = 1;
	static final int NORTH = 2;
	static final int EAST = 4;
	static final int SOUTH = 8;

	/**
	 * Crossing a door costs this many tiles of walking: each door can let a monster out, so fewer is better.
	 */
	private static final int DOOR_COST = 12;

	private static final int[][] STEPS = {
		// dx, dy, wall on this tile, wall on the next tile, door edge here, door edge there
		{0, 1, CollisionDataFlag.BLOCK_MOVEMENT_NORTH, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH, NORTH, SOUTH},
		{1, 0, CollisionDataFlag.BLOCK_MOVEMENT_EAST, CollisionDataFlag.BLOCK_MOVEMENT_WEST, EAST, WEST},
		{0, -1, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH, CollisionDataFlag.BLOCK_MOVEMENT_NORTH, SOUTH, NORTH},
		{-1, 0, CollisionDataFlag.BLOCK_MOVEMENT_WEST, CollisionDataFlag.BLOCK_MOVEMENT_EAST, WEST, EAST},
	};

	@Value
	static class Result
	{
		/**
		 * Scene tiles from the start to the goal, as {x, y}.
		 */
		List<int[]> path;
		/**
		 * Index in {@link #path} of the tile before the first door crossed, or -1 when no door is crossed.
		 */
		int doorStep;
		/**
		 * The tile holding that door, as {x, y}, or null.
		 */
		int[] door;
	}

	static int key(int x, int y)
	{
		return (x << 16) | y;
	}

	/**
	 * @param flags     collision flags for the plane, indexed [x][y]
	 * @param openDoors wall edges (bitmask of {@link #WEST} etc.) that can be passed, by {@link #key}
	 * @param goals     tiles to reach, by {@link #key}
	 * @return the cheapest route, or null when no goal can be reached
	 */
	static Result find(int[][] flags, Map<Integer, Integer> openDoors, int startX, int startY, Set<Integer> goals)
	{
		int w = flags.length;
		int h = w == 0 ? 0 : flags[0].length;
		if (startX < 0 || startY < 0 || startX >= w || startY >= h || goals.isEmpty())
		{
			return null;
		}
		int[] cost = new int[w * h];
		int[] parent = new int[w * h];
		boolean[] viaDoor = new boolean[w * h];
		java.util.Arrays.fill(cost, Integer.MAX_VALUE);
		int start = startX * h + startY;
		cost[start] = 0;
		parent[start] = -1;
		// Dial's algorithm: one bucket per cost, costs are small whole numbers
		List<Deque<Integer>> buckets = new ArrayList<>();
		addTo(buckets, 0, start);
		int found = -1;
		for (int c = 0; c < buckets.size() && found < 0; c++)
		{
			Deque<Integer> bucket = buckets.get(c);
			while (!bucket.isEmpty())
			{
				int cell = bucket.poll();
				if (cost[cell] != c)
				{
					continue;
				}
				int x = cell / h;
				int y = cell % h;
				if (goals.contains(key(x, y)))
				{
					found = cell;
					break;
				}
				for (int[] step : STEPS)
				{
					int nx = x + step[0];
					int ny = y + step[1];
					if (nx < 0 || ny < 0 || nx >= w || ny >= h
						|| (flags[nx][ny] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0)
					{
						continue;
					}
					boolean wall = (flags[x][y] & step[2]) != 0 || (flags[nx][ny] & step[3]) != 0;
					boolean door = false;
					if (wall)
					{
						door = (openDoors.getOrDefault(key(x, y), 0) & step[4]) != 0
							|| (openDoors.getOrDefault(key(nx, ny), 0) & step[5]) != 0;
						if (!door)
						{
							continue;
						}
					}
					int next = nx * h + ny;
					int nc = c + (door ? DOOR_COST : 1);
					if (nc < cost[next])
					{
						cost[next] = nc;
						parent[next] = cell;
						viaDoor[next] = door;
						addTo(buckets, nc, next);
					}
				}
			}
		}
		if (found < 0)
		{
			return null;
		}
		List<int[]> path = new ArrayList<>();
		List<Boolean> doors = new ArrayList<>();
		for (int cell = found; cell >= 0; cell = parent[cell])
		{
			path.add(new int[]{cell / h, cell % h});
			doors.add(viaDoor[cell]);
		}
		Collections.reverse(path);
		Collections.reverse(doors);
		for (int i = 1; i < path.size(); i++)
		{
			if (doors.get(i))
			{
				int[] from = path.get(i - 1);
				int[] to = path.get(i);
				int edge = to[0] > from[0] ? EAST : to[0] < from[0] ? WEST : to[1] > from[1] ? NORTH : SOUTH;
				boolean here = (openDoors.getOrDefault(key(from[0], from[1]), 0) & edge) != 0;
				return new Result(path, i - 1, here ? from : to);
			}
		}
		return new Result(path, -1, null);
	}

	private static void addTo(List<Deque<Integer>> buckets, int c, int cell)
	{
		while (buckets.size() <= c)
		{
			buckets.add(new ArrayDeque<>());
		}
		buckets.get(c).add(cell);
	}

	private TunnelPath()
	{
	}
}
