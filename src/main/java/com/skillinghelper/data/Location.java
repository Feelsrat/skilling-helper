package com.skillinghelper.data;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * A named spot in the world. The player is considered "there" when within {@link #radius} tiles.
 */
@Value
public class Location
{
	String name;
	WorldPoint point;
	int radius;

	public static Location of(String name, int x, int y, int plane, int radius)
	{
		return new Location(name, new WorldPoint(x, y, plane), radius);
	}

	public static Location of(String name, int x, int y, int radius)
	{
		return of(name, x, y, 0, radius);
	}

	public boolean contains(WorldPoint wp)
	{
		return wp != null
			&& wp.getPlane() == point.getPlane()
			&& wp.distanceTo2D(point) <= radius;
	}
}
