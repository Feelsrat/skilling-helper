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
			long xp = xpBetween(currentXp, targetLevel);
			MethodOption option = evaluate(forced, state, pref, current, xp);
			if (option.isAvailable())
			{
				plan.add(new PlanSegment(current, targetLevel, xp, option));
				return plan;
			}
		}

		TreeSet<Integer> breakpoints = breakpoints(methods, current, targetLevel);
		Integer from = breakpoints.first();
		for (Integer to : breakpoints.tailSet(from, false))
		{
			int startXp = Math.max(currentXp, Experience.getXpForLevel(from));
			long xp = xpBetween(startXp, to);
			List<MethodOption> ranked = rankAt(methods, state, pref, from, xp);
			if (!ranked.isEmpty() && ranked.get(0).isAvailable())
			{
				addSegment(plan, state, pref, new PlanSegment(from, to, xp, ranked.get(0)));
			}
			from = to;
		}
		return plan;
	}

	/**
	 * The xp per hour over a whole climb, for methods that speed up as the player levels: the xp divided by the time
	 * it takes level by level, so a long climb isn't timed at its slow starting rate.
	 */
	static double averageXpPerHour(Method m, int level, long xpNeeded)
	{
		double startRate = m.xpPerHour(level);
		if (xpNeeded <= 0 || startRate <= 0)
		{
			return startRate;
		}
		double hours = 0;
		long xpLeft = xpNeeded;
		for (int l = level; xpLeft > 0; l++)
		{
			long xpThisLevel = l >= Experience.MAX_VIRT_LEVEL ? xpLeft
				: Math.min(xpLeft, Experience.getXpForLevel(l + 1) - Experience.getXpForLevel(l));
			hours += xpThisLevel / m.xpPerHour(l);
			xpLeft -= xpThisLevel;
		}
		return xpNeeded / hours;
	}

	/**
	 * The levels where the plan may switch method: now, each level a new method unlocks, and the goal.
	 */
	private static TreeSet<Integer> breakpoints(List<Method> methods, int current, int targetLevel)
	{
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
		return breakpoints;
	}

	/**
	 * Adds a segment, or extends the last one when it uses the same method.
	 */
	private void addSegment(List<PlanSegment> plan, PlayerState state, TrainingPreference pref, PlanSegment next)
	{
		PlanSegment last = plan.isEmpty() ? null : plan.get(plan.size() - 1);
		Method method = next.getOption().getMethod();
		if (last == null || last.getOption().getMethod() != method)
		{
			plan.add(next);
			return;
		}
		long xp = last.getXp() + next.getXp();
		MethodOption merged = evaluate(method, state, pref, last.getFromLevel(), xp);
		plan.set(plan.size() - 1, new PlanSegment(last.getFromLevel(), next.getToLevel(), xp, merged));
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
		double xpPerAction = m.xpPerAction(level);
		int actions = xpPerAction <= 0 ? 0 : (int) Math.ceil(xpNeeded / xpPerAction);
		InputCosts inputs = inputCosts(m, state, actions);
		double productValue = productValue(m);
		double gpPerAction = productValue - inputs.costPerAction;
		// A method that makes money pays for itself after the first batch, so it only needs one trip's worth of gold
		boolean affordable = inputs.buyCost <= state.gp() || (gpPerAction >= 0 && inputs.tripCost <= state.gp());

		Integer measuredXpHr = state.getMeasuredXpPerHour().get(m.getId());
		double xpHr = measuredXpHr != null ? measuredXpHr : averageXpPerHour(m, level, xpNeeded);
		double actionsPerHour = xpPerAction <= 0 ? 0 : xpHr / xpPerAction;
		double gpHr = gpPerAction * actionsPerHour;
		// Items already in the bank don't need buying, so they only count partially against the method
		double cashGpHr = (productValue - inputs.costPerAction * (1 - inputs.ownedFraction)) * actionsPerHour;
		double score = score(m, pref, xpHr, gpHr, cashGpHr, inputs.ownedFraction);
		if (!affordable)
		{
			// Rank methods the player can't pay for after every affordable one
			score -= 1e9;
		}

		return new MethodOption(m, blockers(m, state, level), gpPerAction, actions, inputs.buyCost, affordable,
			inputs.ownedFraction, inputs.unownedCostPerAction, xpHr, measuredXpHr != null, xpPerAction, score);
	}

	/**
	 * Why the player can't use a method yet. Empty when they can.
	 */
	private static List<String> blockers(Method m, PlayerState state, int level)
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
		return blockers;
	}

	/**
	 * What a method's inputs cost for the goal, given what the player already owns.
	 */
	private static final class InputCosts
	{
		/**
		 * GP per action spent on inputs, owned or not.
		 */
		double costPerAction;
		/**
		 * GP per action spent on inputs the player still has to buy.
		 */
		double unownedCostPerAction;
		/**
		 * GP to buy everything the goal needs that the player doesn't own.
		 */
		long buyCost;
		/**
		 * GP to buy one trip's worth of what the player doesn't own.
		 */
		long tripCost;
		/**
		 * Fraction (0-1) of the needed inputs the player already owns.
		 */
		double ownedFraction;
	}

	private InputCosts inputCosts(Method m, PlayerState state, int actions)
	{
		InputCosts costs = new InputCosts();
		long neededTotal = 0;
		long ownedTotal = 0;
		for (ItemReq input : m.getInputs())
		{
			long needed = (long) actions * input.getPerAction();
			neededTotal += needed;
			if (state.ownsSubstitute(input))
			{
				// e.g. a staff of air instead of air runes: nothing to buy
				ownedTotal += needed;
				continue;
			}
			int price = prices.price(input.primaryId());
			costs.costPerAction += (double) price * input.getPerAction();
			long owned = Math.min(needed, state.owned(input.getIds()));
			ownedTotal += owned;
			costs.buyCost += (needed - owned) * price;
			if (owned < needed)
			{
				costs.unownedCostPerAction += (double) price * input.getPerAction();
				long trip = input.isWholeStack() ? Math.min(needed - owned, 1_000) : input.getQuantity();
				costs.tripCost += Math.min(needed - owned, trip) * price;
			}
		}
		costs.ownedFraction = neededTotal == 0 ? 1 : (double) ownedTotal / neededTotal;
		return costs;
	}

	/**
	 * GP earned per action: the average product's price after GE tax, plus any coins. Power-training drops the
	 * produce, so that earns nothing.
	 */
	private double productValue(Method m)
	{
		double productValue = 0;
		if (!m.getProducts().isEmpty() && !m.isDropByDefault())
		{
			for (int product : m.getProducts())
			{
				productValue += prices.sellPriceAfterTax(product);
			}
			productValue /= m.getProducts().size();
		}
		return productValue + m.getCoinsPerAction();
	}

	/**
	 * How well a method fits the training preference; higher is better.
	 */
	private static double score(Method m, TrainingPreference pref, double xpHr, double gpHr, double cashGpHr,
		double ownedFraction)
	{
		// Owning the inputs already only matters for methods that use any
		double ownsInputs = ownedFraction * (m.getInputs().isEmpty() ? 0 : 1);
		switch (pref)
		{
			case FAST:
				return xpHr;
			case AFK:
				return xpHr * (m.hasTag(MethodTag.AFK) ? 3 : 1) * (m.getType() == MethodType.ACTIVITY ? 0.8 : 1);
			case CHEAP:
				return xpHr + 10 * Math.min(cashGpHr, 0) + 30_000 * ownsInputs;
			case PROFIT:
				return 3 * gpHr + 0.2 * xpHr;
			case RECOMMENDED:
			default:
				return xpHr + 2 * Math.min(cashGpHr, 0) + (m.hasTag(MethodTag.AFK) ? 0.15 * xpHr : 0)
					+ 20_000 * ownsInputs;
		}
	}

	public static long xpBetween(int currentXp, int targetLevel)
	{
		int clamped = Math.max(1, Math.min(Experience.MAX_REAL_LEVEL, targetLevel));
		return Math.max(0, Experience.getXpForLevel(clamped) - currentXp);
	}
}
