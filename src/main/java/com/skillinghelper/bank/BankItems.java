package com.skillinghelper.bank;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;

/**
 * Reads what is really in the bank. A bank placeholder (left behind when you withdraw all of an item) shows the
 * item greyed out with amount 0, and canonicalizing its id turns it back into the real item, so it has to be
 * skipped before counting or every guide thinks you still own one.
 */
public final class BankItems
{
	private BankItems()
	{
	}

	/**
	 * Owned amount per canonical item id, leaving out empty slots and placeholders.
	 */
	public static Map<Integer, Integer> count(ItemContainer container, ItemManager itemManager)
	{
		if (container == null)
		{
			return new HashMap<>();
		}
		return count(container.getItems(),
			id -> itemManager.getItemComposition(id).getPlaceholderTemplateId() != -1,
			itemManager::canonicalize);
	}

	static Map<Integer, Integer> count(Item[] items, IntPredicate isPlaceholder, IntUnaryOperator canonicalize)
	{
		Map<Integer, Integer> counts = new HashMap<>();
		for (Item item : items)
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0 || isPlaceholder.test(item.getId()))
			{
				continue;
			}
			counts.merge(canonicalize.applyAsInt(item.getId()), item.getQuantity(), Integer::sum);
		}
		return counts;
	}
}
