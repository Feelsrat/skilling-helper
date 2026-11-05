package com.skillinghelper.planner;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.RatePace;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Rough hours to train a skill between two levels, from the Skilling Helper guides' xp rates: at each level the
 * best method the player could use there (outside the Wilderness; quests it needs are assumed done), scaled by the "Your pace"
 * setting. Only meant for ordering the plan and a ballpark "time to max".
 */
public final class SkillHours
{
	/**
	 * Hitpoints has no guides of its own: it comes with melee/ranged/magic training at about a third of the xp.
	 */
	private static final double HITPOINTS_SHARE = 1.0 / 3;
	/**
	 * Used for a level no method covers, so a gap in the data doesn't read as free.
	 */
	private static final double FALLBACK_XP_HR = 20_000;

	/**
	 * Cumulative hours from level 1 to each level, per skill.
	 */
	private final Map<Skill, double[]> cumulative = new EnumMap<>(Skill.class);

	public SkillHours(Function<Skill, List<Method>> methods, RatePace pace)
	{
		for (Skill skill : Skill.values())
		{
			if (skill == Skill.HITPOINTS)
			{
				continue;
			}
			cumulative.put(skill, table(methods.apply(skill), pace, 1));
		}
		double[] melee = table(methods.apply(Skill.ATTACK), pace, HITPOINTS_SHARE);
		double[] hp = new double[Experience.MAX_REAL_LEVEL + 1];
		// Hitpoints starts at 10
		for (int lvl = 11; lvl <= Experience.MAX_REAL_LEVEL; lvl++)
		{
			hp[lvl] = melee[lvl] - melee[10];
		}
		cumulative.put(Skill.HITPOINTS, hp);
	}

	private static double[] table(Collection<Method> methods, RatePace pace, double share)
	{
		double[] t = new double[Experience.MAX_REAL_LEVEL + 1];
		for (int lvl = 2; lvl <= Experience.MAX_REAL_LEVEL; lvl++)
		{
			int from = lvl - 1;
			double rate = bestRate(methods, from, pace) * share;
			double xp = Experience.getXpForLevel(lvl) - Experience.getXpForLevel(from);
			t[lvl] = t[from] + xp / (rate > 0 ? rate : FALLBACK_XP_HR * share);
		}
		return t;
	}

	static double bestRate(Collection<Method> methods, int level, RatePace pace)
	{
		double best = 0;
		for (Method m : methods)
		{
			if (m.getMinLevel() > level || m.isWilderness())
			{
				continue;
			}
			best = Math.max(best, m.xpPerHour(level) * pace.factor(m));
		}
		return best;
	}

	/**
	 * Hours to go from level {@code from} to {@code to} (0 when already there).
	 */
	public double hours(Skill skill, int from, int to)
	{
		double[] t = cumulative.get(skill);
		if (t == null || to <= from)
		{
			return 0;
		}
		int a = clamp(from);
		int b = clamp(to);
		return Math.max(0, t[b] - t[a]);
	}

	private static int clamp(int level)
	{
		return Math.max(1, Math.min(Experience.MAX_REAL_LEVEL, level));
	}
}
