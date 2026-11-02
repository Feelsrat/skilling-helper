package com.skillinghelper.money;

import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.money.data.MoneyItem;
import com.skillinghelper.money.data.MoneyMethod;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Checks every wiki money making method against the player and sorts them by profit at today's prices.
 */
public final class MoneyRanker
{
	private MoneyRanker()
	{
	}

	/**
	 * Every method, the ones the player can do first, then by profit per hour.
	 */
	public static List<MoneyOption> rank(Collection<MoneyMethod> methods, MoneyPlayer player, PriceLookup prices)
	{
		List<MoneyOption> out = new ArrayList<>();
		for (MoneyMethod m : methods)
		{
			out.add(evaluate(m, player, prices));
		}
		out.sort(Comparator.comparing(MoneyOption::isAvailable).reversed()
			.thenComparing(Comparator.comparingLong(MoneyOption::getProfitPerHour).reversed()));
		return out;
	}

	public static MoneyOption evaluate(MoneyMethod m, MoneyPlayer player, PriceLookup prices)
	{
		List<String> blockers = new ArrayList<>();
		List<String> unchecked = new ArrayList<>();
		if (m.isMembers() && !player.isMembers())
		{
			blockers.add("Members only");
		}
		for (Map.Entry<Skill, Integer> e : m.getSkills().entrySet())
		{
			if (player.level(e.getKey()) < e.getValue())
			{
				blockers.add("Needs " + e.getValue() + " " + e.getKey().getName() + " (you have "
					+ player.level(e.getKey()) + ")");
			}
		}
		for (String quest : m.getQuests())
		{
			String key = quest.toLowerCase(Locale.ROOT);
			if (!player.getKnownQuests().contains(key))
			{
				unchecked.add(quest);
			}
			else if (!player.getQuestsDone().contains(key))
			{
				blockers.add("Needs " + quest);
			}
		}
		if (m.getOther() != null)
		{
			unchecked.add(m.getOther());
		}
		long profit = profit(m, prices);
		long perHour = m.isRecurring() ? Math.round(profit / Math.max(0.05, m.getRunHours())) : profit;
		return new MoneyOption(m, blockers, unchecked, profit, perHour, startCost(m, prices));
	}

	/**
	 * The wiki's profit, moved by how much each item's price has changed since. Items without a GE price keep the
	 * wiki's value.
	 */
	static long profit(MoneyMethod m, PriceLookup prices)
	{
		double profit = m.getWikiProfit();
		for (MoneyItem out : m.getOutputs())
		{
			profit += out.getQuantity() * priceChange(out, prices);
		}
		for (MoneyItem in : m.getInputs())
		{
			profit -= in.getQuantity() * priceChange(in, prices);
		}
		return Math.round(profit);
	}

	private static double priceChange(MoneyItem item, PriceLookup prices)
	{
		int now = price(item, prices);
		return now <= 0 ? 0 : now - item.getWikiValue();
	}

	private static int price(MoneyItem item, PriceLookup prices)
	{
		return prices == null || item.getId() <= 0 ? 0 : prices.price(item.getId());
	}

	private static long startCost(MoneyMethod m, PriceLookup prices)
	{
		double cost = 0;
		for (MoneyItem in : m.getInputs())
		{
			int now = price(in, prices);
			cost += in.getQuantity() * (now > 0 ? now : in.getWikiValue());
		}
		return Math.round(cost);
	}
}
