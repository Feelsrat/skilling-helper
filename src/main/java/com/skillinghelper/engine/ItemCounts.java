package com.skillinghelper.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Item id to quantity for one container (inventory, equipment or bank).
 */
public final class ItemCounts
{
	public static final ItemCounts EMPTY = new ItemCounts(Collections.emptyMap());

	private final Map<Integer, Integer> counts;

	public ItemCounts(Map<Integer, Integer> counts)
	{
		this.counts = counts;
	}

	public static ItemCounts of(int... idQtyPairs)
	{
		Map<Integer, Integer> m = new HashMap<>();
		for (int i = 0; i + 1 < idQtyPairs.length; i += 2)
		{
			m.merge(idQtyPairs[i], idQtyPairs[i + 1], Integer::sum);
		}
		return new ItemCounts(m);
	}

	public int count(int... ids)
	{
		int total = 0;
		for (int id : ids)
		{
			total += counts.getOrDefault(id, 0);
		}
		return total;
	}

	public boolean has(int... ids)
	{
		return count(ids) > 0;
	}

	public Map<Integer, Integer> asMap()
	{
		return Collections.unmodifiableMap(counts);
	}
}
