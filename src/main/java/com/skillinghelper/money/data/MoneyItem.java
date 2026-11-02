package com.skillinghelper.money.data;

import lombok.Value;

/**
 * One input or output of a money making method, as the wiki's profit calculation counts it.
 */
@Value
public class MoneyItem
{
	/**
	 * Item id, or -1 when the wiki name isn't a Grand Exchange item (coins, untradeables, drop table rolls).
	 */
	int id;
	/**
	 * How many per hour, or per run for recurring methods.
	 */
	double quantity;
	/**
	 * The price the wiki used, so profit can be updated with today's prices.
	 */
	double wikiValue;
	/**
	 * How many the Grand Exchange lets one player buy every 4 hours, or 0 when unknown.
	 */
	int buyLimit;
	String name;

	public boolean isCoins()
	{
		return "Coins".equalsIgnoreCase(name);
	}
}
