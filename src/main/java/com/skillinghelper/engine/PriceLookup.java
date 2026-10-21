package com.skillinghelper.engine;

/**
 * Grand Exchange prices. Backed by RuneLite's ItemManager (and optionally live OSRS Wiki prices) in game, and by
 * fixed values in tests.
 */
public interface PriceLookup
{
	/**
	 * Price to buy one item.
	 */
	int price(int itemId);

	/**
	 * Price one item sells for, before tax.
	 */
	default int sellPrice(int itemId)
	{
		return price(itemId);
	}

	/**
	 * Gold actually received for selling one item on the GE, after the 2% tax.
	 */
	default int sellPriceAfterTax(int itemId)
	{
		int p = sellPrice(itemId);
		return p - geTax(p);
	}

	/**
	 * Grand Exchange tax on one item: 2%, rounded down, nothing below 50 gp, capped at 5M.
	 */
	static int geTax(int price)
	{
		if (price < 50)
		{
			return 0;
		}
		return (int) Math.min(5_000_000L, (long) price * 2 / 100);
	}
}
