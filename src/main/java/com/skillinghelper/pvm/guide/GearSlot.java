package com.skillinghelper.pvm.guide;

import java.util.List;
import lombok.Value;

/**
 * An equipment slot and the gear for it, best first.
 */
@Value
public class GearSlot
{
	String name;
	List<Gear> options;
	/**
	 * A weapon to swap to during the trip, carried in the inventory alongside the main one (e.g. a staff for the
	 * Barrows brothers weak to Magic).
	 */
	boolean swap;

	public static GearSlot of(String name, Gear... options)
	{
		return new GearSlot(name, List.of(options), false);
	}

	/**
	 * A weapon carried to swap to; the main weapon and this one can each be worn or in the inventory.
	 */
	public static GearSlot swap(String name, Gear... options)
	{
		return new GearSlot(name, List.of(options), true);
	}
}
