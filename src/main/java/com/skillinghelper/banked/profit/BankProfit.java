package com.skillinghelper.banked.profit;

import com.skillinghelper.banked.BankedPlanner;
import com.skillinghelper.banked.data.BankedActivity;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.engine.Format;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.PriceLookup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;

/**
 * Looks through the bank for items the player can process into something worth more on the GE, and how much more.
 * Banked items are valued at what they'd sell for as they are, so the profit is what processing adds. Pure logic over
 * a {@link PlayerState}, so it's unit tested without the client.
 */
public final class BankProfit
{
	/**
	 * Actions per hour for activities with no xp to work it out from (unfinished potions).
	 */
	private static final int NO_XP_ACTIONS_PER_HOUR = 2_500;

	private BankProfit()
	{
	}

	/**
	 * What {@link #find} found: picks the player can do now, and ones a few levels or a quest away.
	 */
	@Value
	public static class Result
	{
		public static final Result NONE = new Result(Collections.emptyList(), Collections.emptyList());

		List<ProfitPick> now;
		List<ProfitPick> soon;

		public long totalNow()
		{
			return now.stream().mapToLong(ProfitPick::getProfit).sum();
		}
	}

	/**
	 * Every profitable way to process what's in the bank, the best one per banked item, most profit first.
	 *
	 * @param soonLevels how many levels away a recipe can be to be listed as "soon"
	 * @param volumes    items traded on the GE in the last day, by id; empty when unknown
	 */
	public static Result find(PlayerState s, PriceLookup prices, int soonLevels, Map<Integer, Long> volumes)
	{
		if (!s.bankKnown())
		{
			return Result.NONE;
		}
		Map<String, ProfitPick> bestNow = new HashMap<>();
		List<ProfitPick> soon = new ArrayList<>();
		for (ProfitRecipe r : ProfitRecipes.all())
		{
			ProfitPick p = evaluate(r, s, prices, soonLevels, volumes);
			if (p == null || p.getProfit() <= 0)
			{
				continue;
			}
			if (p.isDoable())
			{
				bestNow.merge(r.getActivity().getGroup(), p, (a, b) -> a.getProfit() >= b.getProfit() ? a : b);
			}
			else
			{
				soon.add(p);
			}
		}
		// A recipe that's a few levels away is only worth mentioning when it beats what the item can do today
		Map<String, ProfitPick> bestSoon = new HashMap<>();
		for (ProfitPick p : soon)
		{
			String group = p.getRecipe().getActivity().getGroup();
			ProfitPick now = bestNow.get(group);
			if (now == null || p.getProfit() > now.getProfit())
			{
				bestSoon.merge(group, p, (a, b) -> a.getLevelsAway() < b.getLevelsAway()
					|| (a.getLevelsAway() == b.getLevelsAway() && a.getProfit() >= b.getProfit()) ? a : b);
			}
		}
		Comparator<ProfitPick> byProfit = Comparator.comparingLong(ProfitPick::getProfit).reversed();
		List<ProfitPick> nowList = new ArrayList<>(bestNow.values());
		nowList.sort(byProfit);
		List<ProfitPick> soonList = new ArrayList<>(bestSoon.values());
		soonList.sort(byProfit);
		return new Result(Collections.unmodifiableList(nowList), Collections.unmodifiableList(soonList));
	}

	/**
	 * One recipe against the bank, or null when the bank has none of its items or it's too far off.
	 */
	public static ProfitPick evaluate(ProfitRecipe r, PlayerState s, PriceLookup prices, int soonLevels,
		Map<Integer, Long> volumes)
	{
		BankedActivity a = r.getActivity();
		Method m = a.getMethod();
		int actions = a.actions(s);
		if (actions <= 0)
		{
			return null;
		}
		int levelsAway = Math.max(0, m.getMinLevel() - s.level(m.getSkill()));
		String locked = BankedPlanner.lockReason(a, s);
		if (locked != null)
		{
			if (levelsAway > soonLevels)
			{
				return null;
			}
			locked = lockText(a, s, levelsAway);
		}

		List<String> lines = new ArrayList<>();
		long inputValue = 0;
		long buyCost = 0;
		ItemReq key = a.keyInput();
		long keyLeft = (long) actions * a.getPerAction();
		for (int id : a.getCountIds())
		{
			long use = Math.min(s.owned(id), keyLeft);
			if (use <= 0)
			{
				continue;
			}
			keyLeft -= use;
			long each = sellValue(id, prices);
			if (!key.matches(id) && key.hasRecipe())
			{
				// A part of the key item (clean herbs for unfinished potions): the other parts are bought
				for (ItemReq part : key.getRecipe())
				{
					if (!part.matches(id) && part.getPerAction() > 0)
					{
						long partCost = (long) part.getPerAction() * buyValue(part.primaryId(), prices);
						each += partCost;
						buyCost += use * partCost;
					}
				}
			}
			inputValue += use * each;
		}
		lines.add(String.format("%,d %s (yours)", (long) actions * a.getPerAction(), a.getItemName().toLowerCase()));

		List<ItemReq> inputs = m.getInputs();
		for (int i = 1; i < inputs.size(); i++)
		{
			ItemReq in = inputs.get(i);
			if (in.getPerAction() <= 0 || in.getIds().length == 0 || s.ownsSubstitute(in))
			{
				continue;
			}
			long need = (long) actions * in.getPerAction();
			long have = Math.min(need, s.owned(in.getIds()));
			long buy = need - have;
			int id = in.primaryId();
			inputValue += have * sellValue(id, prices);
			long cost = buy * buyValue(id, prices);
			inputValue += cost;
			buyCost += cost;
			String name = in.getName().toLowerCase();
			if (id == ItemID.COINS)
			{
				lines.add(Format.gp(need) + " in fees");
			}
			else if (buy <= 0)
			{
				lines.add(String.format("%,d %s (yours)", need, name));
			}
			else if (have > 0)
			{
				lines.add(String.format("%,d %s: %,d yours, buy %,d (~%s)", need, name, have, buy, Format.gp(cost)));
			}
			else
			{
				lines.add(String.format("Buy %,d %s (~%s)", buy, name, Format.gp(cost)));
			}
		}

		long products = (long) Math.floor(actions * r.getPerAction());
		int productEach = Math.max(0, prices.sellPriceAfterTax(r.getProductId()));
		long productValue = products * productEach;
		long profit = productValue - inputValue;

		List<String> warnings = new ArrayList<>();
		if (buyCost > s.gp())
		{
			warnings.add("Buying the rest costs " + Format.gp(buyCost) + " and you have " + Format.gp(s.gp())
				+ ", so make some gold first or do part of it.");
		}
		if (!volumes.isEmpty())
		{
			long daily = volumes.getOrDefault(r.getProductId(), 0L);
			if (daily <= 0)
			{
				warnings.add(r.getProductName() + " hardly trades on the GE, so it may not sell.");
			}
			else if (products > daily)
			{
				warnings.add(String.format("Only about %,d %s trade a day, so selling %,d may take a while.", daily,
					r.getProductName().toLowerCase(), products));
			}
		}
		if (r.getNote() != null)
		{
			warnings.add(r.getNote());
		}
		double hours = actions / actionsPerHour(m);
		return new ProfitPick(r, actions, products, productEach, productValue, inputValue, buyCost, profit, hours,
			Collections.unmodifiableList(lines), locked, levelsAway, Collections.unmodifiableList(warnings));
	}

	private static String lockText(BankedActivity a, PlayerState s, int levelsAway)
	{
		Method m = a.getMethod();
		List<String> parts = new ArrayList<>();
		if (levelsAway > 0)
		{
			parts.add(String.format("%s %d (you're %d)", m.getSkill().getName(), m.getMinLevel(),
				s.level(m.getSkill())));
		}
		for (Quest q : m.getQuests())
		{
			if (!s.getCompletedQuests().contains(q))
			{
				parts.add(q.getName());
			}
		}
		if (m.isMembers() && !s.isMembersWorld())
		{
			parts.add("a members' world");
		}
		return "Needs " + String.join(", ", parts);
	}

	public static double actionsPerHour(Method m)
	{
		return m.getXpPerAction() > 0 && m.getXpPerHour() > 0 ? m.getXpPerHour() / m.getXpPerAction()
			: NO_XP_ACTIONS_PER_HOUR;
	}

	private static long sellValue(int id, PriceLookup prices)
	{
		return id == ItemID.COINS ? 1 : Math.max(0, prices.sellPriceAfterTax(id));
	}

	private static long buyValue(int id, PriceLookup prices)
	{
		return id == ItemID.COINS ? 1 : Math.max(0, prices.price(id));
	}
}
