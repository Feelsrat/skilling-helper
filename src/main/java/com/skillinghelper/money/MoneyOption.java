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
	 * Requirements the player doesn't meet. Empty when they can do it now.
	 */
	List<String> blockers;
	/**
	 * Requirements the plugin can't check (diaries, unlocks, quests it doesn't know).
	 */
	List<String> unchecked;
	/**
	 * Profit per hour, or per run for recurring methods, with today's prices where known.
	 */
	long profit;
	/**
	 * Profit per hour of play, which for recurring methods is the run's profit over the time it takes. Used to sort.
	 */
	long profitPerHour;
	/**
	 * Gold the method needs up front for one hour (or one run) of inputs, at today's prices.
	 */
	long startCost;

	public boolean isAvailable()
	{
		return blockers.isEmpty();
	}
}
