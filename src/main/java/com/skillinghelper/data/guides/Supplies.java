package com.skillinghelper.data.guides;

import com.skillinghelper.data.ItemReq;
import net.runelite.api.gameval.ItemID;

/**
 * Optional supplies shared by several guides.
 */
final class Supplies
{
	static ItemReq food(int count)
	{
		return ItemReq.supply("Food (lobsters, swordfish, monkfish...)", count, ItemID.LOBSTER, ItemID.SWORDFISH,
			ItemID.MONKFISH, ItemID.SHARK, ItemID.SALMON, ItemID.TROUT, ItemID.CAKE);
	}

	static final ItemReq STAMINA = ItemReq.supply("Stamina potions", 2, ItemID._4DOSESTAMINA, ItemID._3DOSESTAMINA,
		ItemID._2DOSESTAMINA, ItemID._1DOSESTAMINA);

	static final ItemReq COINS = ItemReq.supply("Coins", 0, ItemID.COINS);

	private Supplies()
	{
	}
}
