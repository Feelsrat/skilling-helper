package com.skillinghelper.engine;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.RatePace;
import com.skillinghelper.data.TrainingPreference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.ToIntFunction;
import lombok.Value;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Ranks training methods for the player's state and preference, and splits a goal into a level-by-level plan.
 */
public final class MethodPlanner
{
	private final PriceLookup prices;
	private RatePace pace = RatePace.EFFICIENT;
	private boolean allowWilderness;

	public MethodPlanner(PriceLookup prices)
	{
		this.prices = prices;
	}

	/**
	 * How fast the player plays, which scales every guide rate.
	 */
	public void setPace(RatePace pace)
	{
		this.pace = pace == null ? RatePace.EFFICIENT : pace;
	}

	/**
	 * Whether the player opted in to Wilderness methods. When not, they're ranked after every other usable method
	 * and never put in a plan unless the player picks one.
	 */
	public void setAllowWilderness(boolean allowWilderness)
	{
		this.allowWilderness = allowWilderness;
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
	 * The method that earns the most gold per hour of everything the player can do right now: unlocked, affordable
	 * and making a profit. Each method is costed over the climb to its skill's goal. Null when nothing makes money.
	 */
	public MethodOption mostProfitable(Collection<Method> methods, PlayerState state, ToIntFunction<Skill> goal)
	{
		MethodOption best = null;
		for (Method m : methods)
		{
			Skill skill = m.getSkill();
			int level = state.level(skill);
			long xpNeeded = xpBetween(state.xp(skill), Math.max(level + 1, goal.applyAsInt(skill)));
			MethodOption option = evaluate(m, state, TrainingPreference.PROFIT, level, xpNeeded);
			if (!option.isPickable() || !option.isAffordable() || option.gpPerHour() <= 0)
			{
				continue;
			}
			if (best == null || option.gpPerHour() > best.gpPerHour())
			{
				best = option;
			}
		}
		return best;
	}

	/**
	 * How many levels the "Fastest leveling" pick races to.
	 */
	public static final int FASTEST_LEVELS = 10;

	/**
	 * The quickest way to gain {@code levels} levels in one skill, over every skill that has methods: for each skill,
	 * the climb is split at every level a method unlocks or tops out (its level range), and each leg uses the fastest
	 * method the player can do and afford there, timed at the calibrated pace. Skills too close to 99 for the full
	 * gain are only compared per level gained. Null when no skill has a usable climb.
	 */
	public FastestClimb fastestClimb(Collection<Method> methods, PlayerState state, int levels)
	{
		Map<Skill, List<Method>> bySkill = new EnumMap<>(Skill.class);
		for (Method m : methods)
		{
			bySkill.computeIfAbsent(m.getSkill(), k -> new ArrayList<>()).add(m);
		}
		FastestClimb best = null;
		for (Map.Entry<Skill, List<Method>> e : bySkill.entrySet())
		{
			FastestClimb climb = climb(e.getKey(), e.getValue(), state, levels);
			if (climb != null && (best == null || climb.hoursFor(levels) < best.hoursFor(levels)))
			{
				best = climb;
			}
		}
		return best;
	}

	/**
	 * One skill's fastest climb of {@code levels} levels (capped at 99), or null when some leg has no usable method.
	 */
	FastestClimb climb(Skill skill, List<Method> methods, PlayerState state, int levels)
	{
		int current = state.level(skill);
		int target = Math.min(Experience.MAX_REAL_LEVEL, current + levels);
		if (current >= target)
		{
			return null;
		}
		TreeSet<Integer> breakpoints = breakpoints(methods, current, target);
		for (Method m : methods)
		{
			// A method stops being the right one at the top of its range, so a leg never runs past it
			if (m.topLevel() > current && m.topLevel() < target)
			{
				breakpoints.add(m.topLevel());
			}
		}
		int currentXp = state.xp(skill);
		List<PlanSegment> legs = new ArrayList<>();
		double hours = 0;
		Integer from = breakpoints.first();
		for (Integer to : breakpoints.tailSet(from, false))
		{
			long xp = xpBetween(Math.max(currentXp, Experience.getXpForLevel(from)), to);
			MethodOption fastest = fastestAt(methods, state, from, xp, false);
			if (fastest == null)
			{
				// Past the top of every range: still better to name something than give up on the skill
				fastest = fastestAt(methods, state, from, xp, true);
			}
			if (fastest == null)
			{
				return null;
			}
			hours += fastest.hours(xp);
			PlanSegment last = legs.isEmpty() ? null : legs.get(legs.size() - 1);
			if (last != null && last.getOption().getMethod() == fastest.getMethod())
			{
				legs.set(legs.size() - 1, new PlanSegment(last.getFromLevel(), to, last.getXp() + xp,
					last.getOption()));
			}
			else
			{
				legs.add(new PlanSegment(from, to, xp, fastest));
			}
			from = to;
		}
		return new FastestClimb(skill, current, target, legs, hours);
	}

	/**
	 * The method with the most xp/hr the player can pick, afford and isn't past the top of (unless
	 * {@code outleveledToo}) at {@code level}. Null when there's none.
	 */
	private MethodOption fastestAt(List<Method> methods, PlayerState state, int level, long xp, boolean outleveledToo)
	{
		MethodOption best = null;
		for (Method m : methods)
		{
			if (!outleveledToo && m.isOutleveled(level, state.combatLevel()))
			{
				continue;
			}
			MethodOption option = evaluate(m, state, TrainingPreference.FAST, level, xp);
			if (!option.isPickable() || !option.isAffordable() || option.getXpPerHour() <= 0)
			{
				continue;
			}
			if (best == null || option.getXpPerHour() > best.getXpPerHour())
			{
				best = option;
			}
		}
		return best;
	}

	/**
	 * The fastest way found to gain levels in one skill.
	 */
	@Value
	public static class FastestClimb
	{
		Skill skill;
		int fromLevel;
		int toLevel;
		/**
		 * The legs in order, one per method used.
		 */
		List<PlanSegment> legs;
		double hours;

		/**
		 * The time scaled to {@code levels} levels, so a skill near 99 that can't gain them all compares fairly.
		 */
		double hoursFor(int levels)
		{
			return hours * levels / Math.max(1, toLevel - fromLevel);
		}
	}

	/**
	 * The player's lowest-level skill among {@code skills}, breaking ties by the least xp. Null if none are given.
	 */
	public static Skill lowestSkill(Collection<Skill> skills, PlayerState state)
	{
		Skill lowest = null;
		for (Skill s : skills)
		{
			if (lowest == null || state.level(s) < state.level(lowest)
				|| (state.level(s) == state.level(lowest) && state.xp(s) < state.xp(lowest)))
			{
				lowest = s;
			}
		}
		return lowest;
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
			if (!ranked.isEmpty() && ranked.get(0).isPickable())
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

		double xpHr = averageXpPerHour(m, level, xpNeeded) * pace.factor(m);
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
		boolean wildernessSkipped = !allowWilderness && m.isWilderness();
		if (wildernessSkipped)
		{
			// And Wilderness ones after everything else, unless the player opted in
			score -= 2e9;
		}

		return new MethodOption(m, blockers(m, state, level), gpPerAction, actions, inputs.buyCost, affordable,
			inputs.ownedFraction, inputs.unownedCostPerAction, xpHr, xpPerAction, score, wildernessSkipped);
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
		for (Map.Entry<Skill, Integer> e : m.getSkillLevels().entrySet())
		{
			if (state.level(e.getKey()) < e.getValue())
			{
				blockers.add("Needs " + e.getValue() + " " + e.getKey().getName());
			}
		}
		for (Quest q : m.getQuestsStarted())
		{
			if (!state.getStartedQuests().contains(q) && !state.getCompletedQuests().contains(q))
			{
				blockers.add("Needs " + q.getName() + " started");
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
		for (ItemReq listed : m.getInputs())
		{
			ItemReq input = CombatGearAdvisor.resolve(listed, state);
			if (input == null)
			{
				continue;
			}
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
