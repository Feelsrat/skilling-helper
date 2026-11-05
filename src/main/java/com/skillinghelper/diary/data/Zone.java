package com.skillinghelper.diary.data;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * A rectangle of tiles, e.g. a cave or a building, used to tell which part of a task the player is at.
 */
@Value
public class Zone
{
	int minX;
	int minY;
	int maxX;
	int maxY;
	/**
	 * The floor, or -1 for any floor.
	 */
	int plane;

	public static Zone of(int x1, int y1, int x2, int y2, int plane)
	{
		return new Zone(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), plane);
	}

	public static Zone anyFloor(int x1, int y1, int x2, int y2)
	{
		return of(x1, y1, x2, y2, -1);
	}

	public boolean contains(WorldPoint wp)
	{
		return wp != null && (plane < 0 || wp.getPlane() == plane)
			&& wp.getX() >= minX && wp.getX() <= maxX && wp.getY() >= minY && wp.getY() <= maxY;
	}
}
