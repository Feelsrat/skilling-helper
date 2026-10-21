package com.skillinghelper.data;

import lombok.Value;

/**
 * One place to visit on a run, e.g. a bird house spot on Fossil Island. The guide leads the player from one stop
 * that needs a visit to the next instead of to the general area.
 */
@Value
public class Stop
{
	/**
	 * Key the plugin uses to report the stop's state (see {@code PlayerState#getStopWaits}).
	 */
	String id;
	Location location;

	public static Stop of(String id, String name, int x, int y)
	{
		return new Stop(id, Location.of(name, x, y, 3));
	}
}
