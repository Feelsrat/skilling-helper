package com.skillinghelper.engine;

import com.skillinghelper.data.ItemReq;
import java.util.Set;

/**
 * Small helpers for working with sets and arrays of item ids.
 */
final class ItemIds
{
	private ItemIds()
	{
	}

	static void addAll(Set<Integer> ids, int[] add)
	{
		for (int id : add)
		{
			ids.add(id);
		}
	}

	/**
	 * Adds an item requirement's ids, and its substitute's.
	 */
	static void addReq(Set<Integer> ids, ItemReq req)
	{
		addAll(ids, req.getIds());
		if (req.hasSubstitute())
		{
			addAll(ids, req.getSubstituteIds());
		}
	}

	static void removeAll(Set<Integer> ids, int[] remove)
	{
		for (int id : remove)
		{
			ids.remove(id);
		}
	}

	static boolean containsAny(Set<Integer> ids, int[] any)
	{
		for (int id : any)
		{
			if (ids.contains(id))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * The first of these items the player has, preferring one in the inventory over one in the bank. -1 if none.
	 */
	static int firstOwned(int[] ids, PlayerState s)
	{
		for (int id : ids)
		{
			if (s.getInventory().has(id))
			{
				return id;
			}
		}
		for (int id : ids)
		{
			if (s.bankCount(id) > 0)
			{
				return id;
			}
		}
		return -1;
	}
}
