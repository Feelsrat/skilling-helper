package com.skillinghelper.bank;

import java.util.Map;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class BankItemsTest
{
	// Made-up placeholder id standing in for "Coal (placeholder)"
	private static final int COAL_PLACEHOLDER = 900_000;

	@Test
	public void placeholdersAndEmptySlotsAreNotOwned()
	{
		Item[] bank = {
			new Item(ItemID.COAL, 50),
			new Item(COAL_PLACEHOLDER, 0),
			new Item(ItemID.LOBSTER, 0),
			new Item(-1, 0),
			new Item(ItemID.LOBSTER, 3),
		};
		Map<Integer, Integer> counts = BankItems.count(bank,
			id -> id == COAL_PLACEHOLDER,
			id -> id == COAL_PLACEHOLDER ? ItemID.COAL : id);
		assertEquals(Map.of(ItemID.COAL, 50, ItemID.LOBSTER, 3), counts);
	}

	@Test
	public void withdrawingEverythingLeavesNothingOwned()
	{
		// Even if the client ever reported a placeholder with an amount, it is still not an item
		Item[] bank = {new Item(COAL_PLACEHOLDER, 1)};
		Map<Integer, Integer> counts = BankItems.count(bank,
			id -> id == COAL_PLACEHOLDER,
			id -> ItemID.COAL);
		assertFalse(counts.containsKey(ItemID.COAL));
	}
}
