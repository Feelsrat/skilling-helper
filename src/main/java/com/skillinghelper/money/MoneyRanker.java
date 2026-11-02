package com.skillinghelper.money;

import com.skillinghelper.engine.Format;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.money.data.MoneyItem;
import com.skillinghelper.money.data.MoneyMethod;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Checks every wiki money making method against the player and sorts them by profit at today's prices: inputs at
 * what they cost to buy, products at what they sell for after the 2% Grand Exchange tax.
 */
public final class MoneyRanker
{
	private MoneyRanker()
	{
	}

	/**
	 * Every method: the ones the player can do and that make money first, then by profit per hour.
	 */
	public static List<MoneyOption> rank(Collection<MoneyMethod> methods, MoneyPlayer player, PriceLookup prices)
	{
		return rank(methods, player, prices, Collections.emptyMap());
	}

	/**
	 * @param volumes items traded on the GE in the last day, by id; empty when unknown
	 */
	public static List<MoneyOption> rank(Collection<MoneyMethod> methods, MoneyPlayer player, PriceLookup prices,
		Map<Integer, Long> volumes)
	{
		List<MoneyOption> out = new ArrayList<>();
		for (MoneyMethod m : methods)
		{
			out.add(evaluate(m, player, prices, volumes));
		}
		out.sort(Comparator.comparing(MoneyOption::isGood).reversed()
			.thenComparing(Comparator.comparing(MoneyOption::isAvailable).reversed())
			.thenComparing(Comparator.comparingLong(MoneyOption::getProfitPerHour).reversed()));
		return out;
	}

	public static MoneyOption evaluate(MoneyMethod m, MoneyPlayer player, PriceLookup prices)
	{
		return evaluate(m, player, prices, Collections.emptyMap());
	}

	public static MoneyOption evaluate(MoneyMethod m, MoneyPlayer player, PriceLookup prices,
		Map<Integer, Long> volumes)
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

		double inputs = 0;
		for (MoneyItem in : m.getInputs())
		{
			inputs += in.getQuantity() * buyPrice(in, prices);
		}
		double outputs = 0;
		double tax = 0;
		for (MoneyItem out : m.getOutputs())
		{
			int sell = sellPrice(out, prices);
			if (sell > 0)
			{
				outputs += out.getQuantity() * sell;
				tax += out.getQuantity() * PriceLookup.geTax(sell);
			}
			else
			{
				// Coins, untradeables and drop-table rolls: the wiki's own value, no tax
				outputs += out.getQuantity() * out.getWikiValue();
			}
		}
		long profit = Math.round(outputs - tax - inputs);
		long perHour = m.isRecurring() ? Math.round(profit / Math.max(0.05, m.getRunHours())) : profit;

		long tripCost = tripCost(m, player, prices);
		if (tripCost >= 0 && player.getCoins() >= 0 && tripCost > player.getCoins())
		{
			blockers.add("Needs about " + Format.gp(tripCost) + " for one trip's supplies (you have "
				+ Format.gp(player.getCoins()) + ")");
		}
		List<String> market = new ArrayList<>();
		boolean hardToSell = sellCheck(m, prices, volumes, outputs, market);
		buyLimitCheck(m, market);
		return new MoneyOption(m, blockers, unchecked, Math.round(inputs), Math.round(outputs), Math.round(tax),
			profit, perHour, tripCost, market, hardToSell, !volumes.isEmpty());
	}

	/**
	 * Compares how many of each main product an hour (or run) makes with how many traded on the GE in the last
	 * day. True when one barely trades at all, so the method's profit is only on paper.
	 */
	static boolean sellCheck(MoneyMethod m, PriceLookup prices, Map<Integer, Long> volumes, double outputValue,
		List<String> warnings)
	{
		if (volumes.isEmpty())
		{
			return false;
		}
		String per = m.isRecurring() ? " a run" : " an hour";
		boolean hard = false;
		for (Map.Entry<MoneyItem, Double> e : byItem(m.getOutputs()).entrySet())
		{
			MoneyItem item = e.getKey();
			double q = e.getValue();
			double value = q * Math.max(1, sellPrice(item, prices));
			// Only the products the money comes from: rare drops and by-products don't decide it
			if (item.getId() <= 0 || q < 1 || value < outputValue * 0.1)
			{
				continue;
			}
			long daily = volumes.getOrDefault(item.getId(), 0L);
			if (daily == 0)
			{
				hard = true;
				warnings.add("Nobody traded " + item.getName() + " on the GE in the last day: it may not sell.");
			}
			else if (q > daily)
			{
				hard = true;
				warnings.add("Hard to sell: you'd make " + count(q) + " " + item.getName() + per + ", but only "
					+ count(daily) + " traded on the GE in the last day.");
			}
			else if (q > daily / 10.0)
			{
				warnings.add("Sells slowly: " + count(q) + " " + item.getName() + per + " is "
					+ Math.round(100 * q / daily) + "% of a day's GE trading (" + count(daily)
					+ "). Expect to wait, or sell a bit lower.");
			}
		}
		return hard;
	}

	/**
	 * Inputs the method uses faster than the GE's 4-hour buy limit allows.
	 */
	static void buyLimitCheck(MoneyMethod m, List<String> warnings)
	{
		for (Map.Entry<MoneyItem, Double> e : byItem(m.getInputs()).entrySet())
		{
			MoneyItem item = e.getKey();
			double q = e.getValue();
			int limit = item.getBuyLimit();
			if (limit <= 0 || item.isCoins())
			{
				continue;
			}
			// A recurring run uses its inputs once; otherwise four hours of play use four hours' worth
			double per4h = m.isRecurring() ? q : q * 4;
			if (per4h > limit)
			{
				warnings.add("GE buy limit: you can buy " + count(limit) + " " + item.getName()
					+ " every 4 hours, but " + (m.isRecurring() ? "a run uses " : "4 hours use ") + count(per4h)
					+ ". Stock up over a few days, or expect to do it in shorter sessions.");
			}
		}
	}

	/**
	 * The same item listed more than once (e.g. runes from two parts of a trip) counted together.
	 */
	private static Map<MoneyItem, Double> byItem(List<MoneyItem> items)
	{
		Map<Integer, MoneyItem> first = new LinkedHashMap<>();
		Map<MoneyItem, Double> out = new LinkedHashMap<>();
		for (MoneyItem item : items)
		{
			MoneyItem key = item.getId() > 0 ? first.computeIfAbsent(item.getId(), id -> item) : item;
			out.merge(key, item.getQuantity(), Double::sum);
		}
		return out;
	}

	private static String count(double n)
	{
		return String.format("%,d", Math.round(n));
	}

	/**
	 * Gold to buy one trip's supplies that the player doesn't already own, or -1 if their bank hasn't been seen.
	 */
	static long tripCost(MoneyMethod m, MoneyPlayer player, PriceLookup prices)
	{
		if (player.getOwned() == null)
		{
			return -1;
		}
		long cost = 0;
		for (Map.Entry<MoneyItem, Integer> e : MoneyGuide.tripAmounts(m, player.getStackable()).entrySet())
		{
			MoneyItem item = e.getKey();
			if (item.isCoins())
			{
				cost += e.getValue();
				continue;
			}
			int owned = player.getOwned().getOrDefault(MoneyGuide.itemId(item), 0);
			cost += (long) Math.max(0, e.getValue() - owned) * buyPrice(item, prices);
		}
		return cost;
	}

	/**
	 * Today's buy price, or the wiki's when the item has no GE price.
	 */
	static double buyPrice(MoneyItem item, PriceLookup prices)
	{
		int now = prices == null || item.getId() <= 0 ? 0 : prices.price(item.getId());
		return now > 0 ? now : item.getWikiValue();
	}

	private static int sellPrice(MoneyItem item, PriceLookup prices)
	{
		if (item.getId() <= 0)
		{
			return 0;
		}
		int now = prices == null ? 0 : prices.sellPrice(item.getId());
		return now > 0 ? now : (int) Math.round(item.getWikiValue());
	}
}
