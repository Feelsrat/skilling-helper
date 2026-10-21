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
	/**
	 * Count any floor as being there, e.g. for rooftop agility courses where the player climbs between planes.
	 */
	boolean anyPlane;

	public static Location of(String name, int x, int y, int plane, int radius)
	{
		return new Location(name, new WorldPoint(x, y, plane), radius, false);
	}

	public static Location of(String name, int x, int y, int radius)
	{
		return of(name, x, y, 0, radius);
	}

	/**
	 * A spot that spans several floors (rooftops, multi-level buildings).
	 */
	public static Location area(String name, int x, int y, int radius)
	{
		return new Location(name, new WorldPoint(x, y, 0), radius, true);
	}

	public boolean contains(WorldPoint wp)
	{
		return wp != null
			&& (anyPlane || wp.getPlane() == point.getPlane())
			&& Math.max(Math.abs(wp.getX() - point.getX()), Math.abs(wp.getY() - point.getY())) <= radius;
	}
}
