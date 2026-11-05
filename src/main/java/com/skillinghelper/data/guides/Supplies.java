package com.skillinghelper.data.guides;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.ToolType;
import java.util.Arrays;
import java.util.stream.IntStream;
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

	/**
	 * Any pickaxe, to mine the Gemstone Crab's shell for gems after it burrows. The price shown is a rune pickaxe's.
	 */
	static final ItemReq PICKAXE = ItemReq.supply("Pickaxe (any, to mine the crab's shell for gems)", 1,
		IntStream.concat(IntStream.of(ItemID.RUNE_PICKAXE),
			Arrays.stream(ToolType.PICKAXE.allIds()).filter(id -> id != ItemID.RUNE_PICKAXE)).toArray());

	static final ItemReq COINS = ItemReq.supply("Coins", 0, ItemID.COINS);

	private Supplies()
	{
	}
}
