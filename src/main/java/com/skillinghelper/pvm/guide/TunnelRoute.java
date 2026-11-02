package com.skillinghelper.pvm.guide;

import java.util.List;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * The way through the Barrows tunnels to the chest, worked out from the doors that can be opened right now.
 */
@Value
public class TunnelRoute
{
	/**
	 * Tiles to walk, from the player towards the chest (up to and just past the next door).
	 */
	List<WorldPoint> path;
	/**
	 * The next door to click, or null when the chest can be reached without opening one.
	 */
	WorldPoint door;
	/**
	 * Where the path ends: the far side of the next door, or a tile next to the chest.
	 */
	WorldPoint end;

	public boolean atChest()
	{
		return door == null;
	}
}
