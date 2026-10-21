package com.skillinghelper.engine;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.TrainingPreference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Ranks training methods for the player's state and preference, and splits a goal into a level-by-level plan.
 */
public final class MethodPlanner
{
	private final PriceLookup prices;

	public MethodPlanner(PriceLookup prices)
	{
		this.prices = prices;
	}

	/**
	 * Evaluates every method for a skill at the player's current level, best first. Unavailable methods sort last.
	 */
	public List<MethodOption> rank(List<Method> methods, PlayerState state, TrainingPreference pref, int targetLevel)
	{
		Skill skill = methods.isEmpty() ? null : methods.get(0).getSkill();
		int level = skill == null ? 1 : state.level(skill);
		long xpNeeded = skill == null ? 0 : xpBetween(state.xp(skill), targetLevel);
		return rankAt(methods, state, pref, level, xpNeeded);
	}

	/**
	 * Splits the climb from the current level to {@code targetLevel} into segments, switching method whenever a
	 * better one unlocks. If {@code forced} is set and usable, it is used for the whole goal.
	 */
	public List<PlanSegment> plan(List<Method> methods, PlayerState state, TrainingPreference pref, int targetLevel,
		Method forced)
	{
		List<PlanSegment> plan = new ArrayList<>();
		if (methods.isEmpty())
		{
			return plan;
		}
		Skill skill = methods.get(0).getSkill();
		int current = state.level(skill);
		int currentXp = state.xp(skill);
		if (current >= targetLevel)
		{
			return plan;
		}

		if (forced != null)
		{
			MethodOption option = evaluate(forced, state, pref, current, xpBetween(currentXp, targetLevel));
			if (option.isAvailable())
			{
				plan.add(new PlanSegment(current, targetLevel, xpBetween(currentXp, targetLevel), option));
				return plan;
			}
		}

		TreeSet<Integer> breakpoints = new TreeSet<>();
		breakpoints.add(current);
		for (Method m : methods)
		{
			if (m.getMinLevel() > current && m.getMinLevel() < targetLevel)
			{
				breakpoints.add(m.getMinLevel());
			}
		}
		breakpoints.add(targetLevel);

		Integer from = breakpoints.first();
		for (Integer to : breakpoints.tailSet(from, false))
		{
			int startXp = Math.max(currentXp, Experience.getXpForLevel(from));
			long xp = xpBetween(startXp, to);
			List<MethodOption> ranked = rankAt(methods, state, pref, from, xp);
			MethodOption best = ranked.isEmpty() || !ranked.get(0).isAvailable() ? null : ranked.get(0);
			if (best != null)
			{
				PlanSegment last = plan.isEmpty() ? null : plan.get(plan.size() - 1);
				if (last != null && last.getOption().getMethod() == best.getMethod())
				{
					MethodOption merged = evaluate(best.getMethod(), state, pref, last.getFromLevel(), last.getXp() + xp);
					plan.set(plan.size() - 1, new PlanSegment(last.getFromLevel(), to, last.getXp() + xp, merged));
				}
				else
				{
					plan.add(new PlanSegment(from, to, xp, best));
				}
			}
			from = to;
		}
		return plan;
	}

	private List<MethodOption> rankAt(List<Method> methods, PlayerState state, TrainingPreference pref, int level,
		long xpNeeded)
	{
		List<MethodOption> options = new ArrayList<>();
		for (Method m : methods)
		{
			options.add(evaluate(m, state, pref, level, xpNeeded));
		}
		options.sort(Comparator.comparing(MethodOption::isAvailable).reversed()
			.thenComparing(Comparator.comparingDouble(MethodOption::getScore).reversed()));
		return options;
	}

	MethodOption evaluate(Method m, PlayerState state, TrainingPreference pref, int level, long xpNeeded)
	{
		List<String> blockers = new ArrayList<>();
		if (m.getMinLevel() > level)
		{
			blockers.add("Needs level " + m.getMinLevel());
		}
		if (m.getCombatLevel() > 0 && state.combatLevel() < m.getCombatLevel())
		{
			blockers.add("Needs combat level " + m.getCombatLevel());
		}
		if (m.isMembers() && !state.isMembersWorld())
		{
			blockers.add("Members only");
		}
		for (Quest q : m.getQuests())
		{
			if (!state.getCompletedQuests().contains(q))
			{
				blockers.add("Needs " + q.getName());
			}
		}

		int actions = m.getXpPerAction() <= 0 ? 0 : (int) Math.ceil(xpNeeded / m.getXpPerAction());

		double inputCost = 0;
		double unownedCostPerAction = 0;
		long buyCost = 0;
		long tripCost = 0;
		long neededTotal = 0;
		long ownedTotal = 0;
		for (ItemReq input : m.getInputs())
		{
			long needed = (long) actions * input.getPerAction();
			neededTotal += needed;
			if (input.hasSubstitute() && state.owned(input.getSubstituteIds()) > 0)
			{
				// e.g. a staff of air instead of air runes: nothing to buy
				ownedTotal += needed;
				continue;
			}
			int price = prices.price(input.primaryId());
			inputCost += (double) price * input.getPerAction();
			long owned = Math.min(needed, state.owned(input.getIds()));
			ownedTotal += owned;
			buyCost += (needed - owned) * price;
			if (owned < needed)
			{
				unownedCostPerAction += (double) price * input.getPerAction();
				long trip = input.isWholeStack() ? Math.min(needed - owned, 1_000) : input.getQuantity();
				tripCost += Math.min(needed - owned, trip) * price;
			}
		}
		double productValue = 0;
		// Power-training drops the produce, so it earns nothing
		if (!m.getProducts().isEmpty() && !m.isDropByDefault())
		{
			for (int product : m.getProducts())
			{
				productValue += prices.sellPriceAfterTax(product);
			}
			productValue /= m.getProducts().size();
		}
		productValue += m.getCoinsPerAction();
		double gpPerAction = productValue - inputCost;
		double ownedFraction = neededTotal == 0 ? 1 : (double) ownedTotal / neededTotal;
		// A method that makes money pays for itself after the first batch, so it only needs one trip's worth of gold
		boolean affordable = buyCost <= state.gp() || (gpPerAction >= 0 && tripCost <= state.gp());

		double xpHr = m.getXpPerHour();
		double actionsPerHour = m.getXpPerAction() <= 0 ? 0 : xpHr / m.getXpPerAction();
		double gpHr = gpPerAction * actionsPerHour;
		// Items already in the bank don't need buying, so they only count partially against the method
		double cashGpHr = (productValue - inputCost * (1 - ownedFraction)) * actionsPerHour;
		double score;
		switch (pref)
		{
			case FAST:
				score = xpHr;
				break;
			case AFK:
				score = xpHr * (m.hasTag(MethodTag.AFK) ? 3 : 1) * (m.getType() == MethodType.ACTIVITY ? 0.8 : 1);
				break;
			case CHEAP:
				score = xpHr + 10 * Math.min(cashGpHr, 0) + 30_000 * ownedFraction * (m.getInputs().isEmpty() ? 0 : 1);
				break;
			case PROFIT:
				score = 3 * gpHr + 0.2 * xpHr;
				break;
			case RECOMMENDED:
			default:
				score = xpHr + 2 * Math.min(cashGpHr, 0) + (m.hasTag(MethodTag.AFK) ? 0.15 * xpHr : 0)
					+ 20_000 * ownedFraction * (m.getInputs().isEmpty() ? 0 : 1);
				break;
		}
		if (!affordable)
		{
			// Rank methods the player can't pay for after every affordable one
			score -= 1e9;
		}

		return new MethodOption(m, blockers, gpPerAction, actions, buyCost, affordable, ownedFraction, unownedCostPerAction,
			score);
	}

	public static long xpBetween(int currentXp, int targetLevel)
	{
		int clamped = Math.max(1, Math.min(Experience.MAX_REAL_LEVEL, targetLevel));
		return Math.max(0, Experience.getXpForLevel(clamped) - currentXp);
	}
}
