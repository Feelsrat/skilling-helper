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
		// Best (most healing) first, so only the best food the player has enough of is suggested
		return ItemReq.supply("Food (sharks, monkfish, lobsters...)", count, ItemID.SHARK, ItemID.MONKFISH,
			ItemID.SWORDFISH, ItemID.LOBSTER, ItemID.SALMON, ItemID.TROUT, ItemID.CAKE)
			.bestFirst("Sharks", "Monkfish", "Swordfish", "Lobsters", "Salmon", "Trout", "Cakes");
	}

	static final ItemReq STAMINA = ItemReq.supply("Stamina potions", 2, ItemID._4DOSESTAMINA, ItemID._3DOSESTAMINA,
		ItemID._2DOSESTAMINA, ItemID._1DOSESTAMINA).bestFirst("Stamina potion (4)", "Stamina potion (3)",
		"Stamina potion (2)", "Stamina potion (1)");

	static final String[] WATERSKIN_NAMES = {"Waterskin (4)", "Waterskin (3)", "Waterskin (2)", "Waterskin (1)"};

	/**
	 * Any pickaxe, to mine the Gemstone Crab's shell for gems after it burrows. The price shown is a rune pickaxe's.
	 */
	static final ItemReq PICKAXE = ItemReq.supply("Pickaxe (to mine the crab's shell for gems)", 1,
		IntStream.concat(IntStream.of(ItemID.RUNE_PICKAXE),
			Arrays.stream(ToolType.PICKAXE.allIds()).filter(id -> id != ItemID.RUNE_PICKAXE)).toArray());

	static final ItemReq COINS = ItemReq.supply("Coins", 0, ItemID.COINS);

	private Supplies()
	{
	}
}
