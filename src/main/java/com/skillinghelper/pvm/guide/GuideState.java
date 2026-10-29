package com.skillinghelper.pvm.guide;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * What a guide needs to know about the player and their surroundings right now. Built on the client thread each tick.
 */
@Value
@Builder
public class GuideState
{
	@Singular
	Map<Skill, Integer> levels;
	/**
	 * Inventory item counts by id.
	 */
	@Singular("inv")
	Map<Integer, Integer> inventory;
	/**
	 * Worn item counts by id (ammo stacks).
	 */
	@Singular("wearing")
	Map<Integer, Integer> worn;
	/**
	 * Bank counts by canonical id, or null when the bank hasn't been opened yet.
	 */
	Map<Integer, Integer> bank;
	boolean bankOpen;
	/**
	 * Inventory slots in use.
	 */
	int usedSlots;
	/**
	 * Where the player is. Inside an instance this is the matching spot on the normal map.
	 */
	WorldPoint location;
	boolean inInstance;
	/**
	 * How many of each NPC (lowercase name) are close by.
	 */
	@Singular("npc")
	Map<String, Integer> npcs;
	@Singular
	Map<Integer, Integer> varbits;
	boolean members;
	int hitpoints;
	int maxHitpoints;
	int prayer;
	/**
	 * A kill or chest of this content was counted in the last few ticks.
	 */
	boolean justKilled;
	/**
	 * Guide-specific things the plugin remembers, e.g. which Barrows brother has the tunnel.
	 */
	@Singular
	Set<String> flags;

	public int level(Skill skill)
	{
		return levels.getOrDefault(skill, 1);
	}

	public int inventory(int... ids)
	{
		return count(inventory, ids);
	}

	public int worn(int... ids)
	{
		return count(worn, ids);
	}

	public int carried(int... ids)
	{
		return inventory(ids) + worn(ids);
	}

	public boolean bankKnown()
	{
		return bank != null;
	}

	public int banked(int... ids)
	{
		return bank == null ? 0 : count(bank, ids);
	}

	public boolean owns(int... ids)
	{
		return carried(ids) > 0 || banked(ids) > 0;
	}

	public int npc(String name)
	{
		return npcs.getOrDefault(name.toLowerCase(Locale.ROOT), 0);
	}

	public int varbit(int id)
	{
		return varbits.getOrDefault(id, 0);
	}

	public boolean flag(String flag)
	{
		return flags.contains(flag);
	}

	public int freeSlots()
	{
		return Math.max(0, 28 - usedSlots);
	}

	public boolean in(com.skillinghelper.data.Location place)
	{
		return place != null && place.contains(location);
	}

	private static int count(Map<Integer, Integer> items, int... ids)
	{
		int n = 0;
		for (int id : ids)
		{
			n += items.getOrDefault(id, 0);
		}
		return n;
	}
}
