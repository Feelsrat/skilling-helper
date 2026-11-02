package com.skillinghelper.money;

import com.skillinghelper.money.data.MoneyMethod;
import java.util.List;
import lombok.Value;

/**
 * A money making method checked against the player: what's missing, and what it makes at today's prices.
 */
@Value
public class MoneyOption
{
	MoneyMethod method;
	/**
	 * Requirements the player doesn't meet, including not having the gold for a trip's supplies. Empty when they can
	 * do it now.
	 */
	List<String> blockers;
	/**
	 * Requirements the plugin can't check (diaries, unlocks, quests it doesn't know).
	 */
	List<String> unchecked;
	/**
	 * What the inputs cost to buy, per hour (per run for recurring methods), at today's buy prices.
	 */
	long inputCost;
	/**
	 * What the products sell for, per hour or run, at today's sell prices before tax.
	 */
	long outputValue;
	/**
	 * Grand Exchange tax on selling the products.
	 */
	long geTax;
	/**
	 * Profit per hour, or per run for recurring methods: products after tax minus inputs.
	 */
	long profit;
	/**
	 * Profit per hour of play, which for recurring methods is the run's profit over the time it takes. Used to sort.
	 */
	long profitPerHour;
	/**
	 * Gold still needed to buy one trip's supplies, after what the player already owns. -1 when the bank hasn't been
	 * seen.
	 */
	long tripCost;
	/**
	 * Warnings about buying or selling on the Grand Exchange: products that barely trade, inputs capped by the buy
	 * limit.
	 */
	List<String> marketWarnings;
	/**
	 * A main product hardly trades on the Grand Exchange, so it may not sell at all.
	 */
	boolean hardToSell;
	/**
	 * Whether trade volumes were known (live wiki prices on), so {@link #hardToSell} means anything.
	 */
	boolean volumesKnown;

	public boolean isAvailable()
	{
		return blockers.isEmpty();
	}

	public boolean isProfitable()
	{
		return profit > 0;
	}

	/**
	 * Available, making money and selling: what the panel lists by default.
	 */
	public boolean isGood()
	{
		return isAvailable() && isProfitable() && !hardToSell;
	}
}
