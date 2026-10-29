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

	public static GearSlot of(String name, Gear... options)
	{
		return new GearSlot(name, List.of(options));
	}
}
