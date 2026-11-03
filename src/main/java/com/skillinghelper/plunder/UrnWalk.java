package com.skillinghelper.plunder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The shortest walk from the player through {@code count} of a room's urns. A room has at most 13, so it's found
 * exactly over subsets (Held-Karp) rather than guessed. Idea from Nubles' Codex Plunder Pilot (BSD-2-Clause).
 */
public final class UrnWalk
{
	/**
	 * Above this many urns fall back to nearest-first, to keep the subset table small.
	 */
	static final int EXACT = 15;

	private UrnWalk()
	{
	}

	/**
	 * @return indexes into the urns, in walking order
	 */
	public static List<Integer> order(int fromX, int fromY, int[] xs, int[] ys, int count)
	{
		int n = Math.min(xs.length, ys.length);
		count = Math.max(0, Math.min(count, n));
		if (count == 0)
		{
			return Collections.emptyList();
		}
		return n <= EXACT ? exact(fromX, fromY, xs, ys, n, count) : nearestFirst(fromX, fromY, xs, ys, n, count);
	}

	private static List<Integer> exact(int fromX, int fromY, int[] xs, int[] ys, int n, int count)
	{
		int full = 1 << n;
		int[] cost = new int[full * n];
		int[] prev = new int[full * n];
		Arrays.fill(cost, Integer.MAX_VALUE);
		for (int i = 0; i < n; i++)
		{
			cost[(1 << i) * n + i] = steps(fromX, fromY, xs[i], ys[i]);
			prev[(1 << i) * n + i] = -1;
		}
		int bestCost = Integer.MAX_VALUE;
		int bestMask = 0;
		int bestEnd = 0;
		for (int mask = 1; mask < full; mask++)
		{
			int size = Integer.bitCount(mask);
			if (size > count)
			{
				continue;
			}
			for (int end = 0; end < n; end++)
			{
				int c = cost[mask * n + end];
				if (c == Integer.MAX_VALUE)
				{
					continue;
				}
				if (size == count)
				{
					if (c < bestCost)
					{
						bestCost = c;
						bestMask = mask;
						bestEnd = end;
					}
					continue;
				}
				for (int next = 0; next < n; next++)
				{
					if ((mask & (1 << next)) != 0)
					{
						continue;
					}
					int to = (mask | (1 << next)) * n + next;
					int len = c + steps(xs[end], ys[end], xs[next], ys[next]);
					if (len < cost[to])
					{
						cost[to] = len;
						prev[to] = end;
					}
				}
			}
		}
		List<Integer> path = new ArrayList<>();
		int mask = bestMask;
		int end = bestEnd;
		while (end >= 0)
		{
			path.add(end);
			int p = prev[mask * n + end];
			mask &= ~(1 << end);
			end = p;
		}
		Collections.reverse(path);
		return path;
	}

	private static List<Integer> nearestFirst(int fromX, int fromY, int[] xs, int[] ys, int n, int count)
	{
		List<Integer> path = new ArrayList<>();
		boolean[] used = new boolean[n];
		int cx = fromX;
		int cy = fromY;
		for (int k = 0; k < count; k++)
		{
			int best = -1;
			int bestDist = Integer.MAX_VALUE;
			for (int i = 0; i < n; i++)
			{
				int d = steps(cx, cy, xs[i], ys[i]);
				if (!used[i] && d < bestDist)
				{
					best = i;
					bestDist = d;
				}
			}
			used[best] = true;
			path.add(best);
			cx = xs[best];
			cy = ys[best];
		}
		return path;
	}

	/**
	 * Tiles walked between two tiles: the game walks diagonally as cheaply as straight.
	 */
	static int steps(int x1, int y1, int x2, int y2)
	{
		return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
	}
}
