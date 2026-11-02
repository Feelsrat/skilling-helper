package com.skillinghelper.pvm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.CollisionDataFlag;
import org.junit.Test;

public class TunnelPathTest
{
	/**
	 * Two rooms side by side, x 0-4 and x 5-9, with a wall between x=4 and x=5 and doors in it at y=2 and y=7.
	 */
	private static int[][] twoRooms()
	{
		int[][] flags = new int[10][10];
		for (int y = 0; y < 10; y++)
		{
			flags[4][y] |= CollisionDataFlag.BLOCK_MOVEMENT_EAST;
			flags[5][y] |= CollisionDataFlag.BLOCK_MOVEMENT_WEST;
		}
		return flags;
	}

	@Test
	public void goesThroughTheDoorThatOpens()
	{
		Map<Integer, Integer> open = new HashMap<>();
		// Only the far door (y=7) opens; it sits on the east edge of tile (4,7)
		open.put(TunnelPath.key(4, 7), TunnelPath.EAST);
		TunnelPath.Result r = TunnelPath.find(twoRooms(), open, 1, 2, Set.of(TunnelPath.key(8, 2)));
		assertNotNull(r);
		assertArrayEquals(new int[]{4, 7}, r.getDoor());
		assertArrayEquals(new int[]{4, 7}, r.getPath().get(r.getDoorStep()));
		assertArrayEquals(new int[]{5, 7}, r.getPath().get(r.getDoorStep() + 1));
		assertArrayEquals(new int[]{8, 2}, r.getPath().get(r.getPath().size() - 1));
	}

	@Test
	public void doorOnTheOtherSideCountsToo()
	{
		Map<Integer, Integer> open = new HashMap<>();
		// The same door, described from the room beyond: west edge of (5,2)
		open.put(TunnelPath.key(5, 2), TunnelPath.WEST);
		TunnelPath.Result r = TunnelPath.find(twoRooms(), open, 1, 2, Set.of(TunnelPath.key(8, 2)));
		assertNotNull(r);
		assertArrayEquals(new int[]{5, 2}, r.getDoor());
	}

	@Test
	public void lockedDoorsBlock()
	{
		assertNull(TunnelPath.find(twoRooms(), new HashMap<>(), 1, 2, Set.of(TunnelPath.key(8, 2))));
	}

	@Test
	public void noDoorNeededInTheSameRoom()
	{
		TunnelPath.Result r = TunnelPath.find(twoRooms(), new HashMap<>(), 1, 2, Set.of(TunnelPath.key(3, 5)));
		assertNotNull(r);
		assertNull(r.getDoor());
		assertEquals(-1, r.getDoorStep());
		assertEquals(6, r.getPath().size());
	}

	@Test
	public void aDoorOnTheWrongEdgeDoesNotOpenTheWall()
	{
		Map<Integer, Integer> open = new HashMap<>();
		// A door on the north edge of (4,2) mustn't let the player walk east through the wall
		open.put(TunnelPath.key(4, 2), TunnelPath.NORTH);
		assertNull(TunnelPath.find(twoRooms(), open, 1, 2, Set.of(TunnelPath.key(8, 2))));
	}
}
