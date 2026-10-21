package com.skillinghelper.data;

import lombok.Value;

/**
 * One place to visit on a run, e.g. a bird house spot on Fossil Island or a herb patch. The guide leads the player
 * from one stop that needs a visit to the next instead of to the general area.
 */
@Value
public class Stop
{
	/**
	 * Key the plugin uses to report the stop's state (see {@code PlayerState#getStopWaits}).
	 */
	String id;
	Location location;
	/**
	 * For farming patches: the patch type and the varbit holding its state (only valid near the patch). Null otherwise.
	 */
	PatchType patch;
	int varbit;
	/**
	 * Fastest way there, e.g. a teleport. May be null.
	 */
	String hint;

	public static Stop of(String id, String name, int x, int y, String hint)
	{
		return new Stop(id, Location.of(name, x, y, 3), null, -1, hint);
	}

	/**
	 * A farming patch. The point should be the farming area itself (RuneLite's world map icon), so the patch and its
	 * neighbours fall inside the stop.
	 */
	public static Stop patch(String id, String name, int x, int y, PatchType type, int varbit, String hint)
	{
		return new Stop(id, Location.of(name, x, y, 8), type, varbit, hint);
	}
}
