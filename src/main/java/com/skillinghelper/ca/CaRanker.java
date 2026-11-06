package com.skillinghelper.ca;

import com.skillinghelper.ca.data.CaMonster;
import com.skillinghelper.ca.data.CaMonsters;
import com.skillinghelper.ca.data.CaTask;
import com.skillinghelper.ca.data.CaType;
import com.skillinghelper.data.CombatGear;
import com.skillinghelper.data.CombatStyle;
import com.skillinghelper.diary.DiaryGuide;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.diary.data.DiaryTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * Sorts tasks from easiest to hardest for this player: their combat stats and the best gear they own against what
 * each boss asks for, the task's tier and kind, how many kills it takes, and whether it needs a team. Tasks for the
 * same boss that are about as easy are kept together, so one trip does several.
 */
public final class CaRanker
{
	/**
	 * Further below the boss than this and the task counts as too hard for now.
	 */
	static final int STRETCH = 12;
	/**
	 * Tasks for a boss scoring within this of its easiest task are done on the same trip, right after it.
	 */
	static final int SAME_TRIP = 6;

	public enum Fit
	{
		DONE,
		/**
		 * Strong enough for it.
		 */
		READY,
		/**
		 * A little under what's suggested: doable with care.
		 */
		STRETCH,
		/**
		 * Well under what's suggested.
		 */
		TOO_HARD,
		/**
		 * A quest, level, item or setting is missing.
		 */
		LOCKED
	}

	@Value
	public static class Ranked
	{
		CaTask task;
		CaMonster monster;
		int score;
		Fit fit;
		/**
		 * Why it's ranked where it is, e.g. "Suggested strength 70, you're 64".
		 */
		String reason;
		List<String> missing;
	}

	/**
	 * How strong the player fights in one style: the stat that matters most, Defence and Hitpoints, and the best
	 * gear they own (or carry) that they can wear.
	 */
	@Value
	public static class Power
	{
		int value;
		CombatStyle style;
		/**
		 * The weapon behind {@link #value}, or null when none is owned.
		 */
		String weapon;
	}

	private CaRanker()
	{
	}

	public static Power power(DiaryPlayer p)
	{
		Power best = null;
		for (CombatStyle style : CombatStyle.values())
		{
			int stat;
			switch (style)
			{
				case MELEE:
					stat = (p.level(Skill.ATTACK) + p.level(Skill.STRENGTH)) / 2;
					break;
				case RANGED:
					stat = p.level(Skill.RANGED);
					break;
				default:
					stat = p.level(Skill.MAGIC);
			}
			CombatGear.Tier weapon = bestOwned(style, "weapon", p);
			CombatGear.Tier body = bestOwned(style, "body", p);
			int gear = ((weapon == null ? 1 : weapon.getLevel()) + (body == null ? 1 : body.getLevel())) / 2;
			// Gear tops out around level 80, so it's scaled up to sit on the same 1-99 scale as stats
			int gearScaled = Math.min(99, gear * 6 / 5);
			int value = (3 * stat + p.level(Skill.DEFENCE) + p.level(Skill.HITPOINTS) + 2 * gearScaled) / 7;
			if (best == null || value > best.getValue())
			{
				best = new Power(value, style, weapon == null ? null : weapon.getName());
			}
		}
		return best;
	}

	private static CombatGear.Tier bestOwned(CombatStyle style, String slot, DiaryPlayer p)
	{
		for (CombatGear g : CombatGear.values())
		{
			if (g.getStyle() != style || !g.getSlotName().equals(slot))
			{
				continue;
			}
			for (CombatGear.Tier t : g.getTiers())
			{
				if (t.getSkill() != null && p.level(t.getSkill()) < t.getLevel())
				{
					continue;
				}
				if (t.getDefenceLevel() > 0 && p.level(Skill.DEFENCE) < t.getDefenceLevel())
				{
					continue;
				}
				for (int id : t.getIds())
				{
					if (p.owned(id) > 0)
					{
						return t;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Every task in {@code tasks}, easiest first, done ones last.
	 *
	 * @param kc kill counts by lower-case name, from the game's chat messages
	 */
	public static List<Ranked> rank(Collection<CaTask> tasks, DiaryPlayer p, Power power, Map<String, Integer> kc,
		Set<String> skipped)
	{
		Map<String, Integer> doneByMonster = new HashMap<>();
		for (CaTask t : tasks)
		{
			if (p.getDone().contains(t.key()))
			{
				doneByMonster.merge(t.getMonster(), 1, Integer::sum);
			}
		}
		List<Ranked> out = new ArrayList<>();
		for (CaTask t : tasks)
		{
			out.add(rankOne(t, p, power, kc, doneByMonster.getOrDefault(t.getMonster(), 0) > 0));
		}
		// Keep a boss's tasks of about the same ease together, so one trip does them all
		Map<String, Integer> bestByMonster = new HashMap<>();
		for (Ranked r : out)
		{
			if (r.getFit() == Fit.READY || r.getFit() == Fit.STRETCH)
			{
				bestByMonster.merge(r.getTask().getMonster(), r.getScore(), Math::min);
			}
		}
		out.sort(Comparator
			.comparingInt((Ranked r) -> order(r.getFit()))
			.thenComparingInt(r -> sortKey(r, bestByMonster))
			.thenComparing(r -> r.getTask().getMonster())
			.thenComparingInt(Ranked::getScore)
			.thenComparingInt(r -> skipped.contains(r.getTask().key()) ? 1 : 0)
			.thenComparingInt(r -> r.getTask().getId()));
		return out;
	}

	private static int sortKey(Ranked r, Map<String, Integer> bestByMonster)
	{
		Integer best = bestByMonster.get(r.getTask().getMonster());
		return best != null && r.getScore() <= best + SAME_TRIP ? best : r.getScore();
	}

	private static int order(Fit fit)
	{
		switch (fit)
		{
			case READY:
			case STRETCH:
				return 0;
			case TOO_HARD:
				return 1;
			case LOCKED:
				return 2;
			default:
				return 3;
		}
	}

	static Ranked rankOne(CaTask t, DiaryPlayer p, Power power, Map<String, Integer> kc, boolean bossDoneBefore)
	{
		CaMonster m = CaMonsters.get(t.getMonster());
		if (p.getDone().contains(t.key()))
		{
			return new Ranked(t, m, Integer.MAX_VALUE, Fit.DONE, "Done", List.of());
		}
		DiaryTask dt = CaGuide.diaryTask(t);
		List<String> missing = new ArrayList<>(DiaryGuide.missing(dt, p));
		if (missing.isEmpty() && !DiaryGuide.obtainable(dt, p))
		{
			for (DiaryItem item : dt.getItems())
			{
				if (item.getBuyId() <= 0 && item.getTool() == null && owned(item, p) < item.getAmount())
				{
					missing.add(item.getName());
				}
			}
		}
		int level = m == null ? 70 : m.getLevel();
		int gap = level - power.getValue();
		int score = t.getTier().getPoints() * 3 + t.getType().getEffort() * 2 + Math.min(20, (t.count() - 1) / 3);
		if (gap > 0)
		{
			score += gap * (t.getType() == CaType.SPEED ? 4 : 2);
		}
		else
		{
			score += Math.max(-6, gap / 4);
		}
		if (m != null && m.getTeam() > 1)
		{
			score += 12;
		}
		if (bossDoneBefore || kcFor(m, t, kc) > 0)
		{
			score -= 2;
		}
		Fit fit;
		String reason;
		if (!missing.isEmpty())
		{
			fit = Fit.LOCKED;
			reason = "Needs " + String.join(", ", missing);
		}
		else if (gap > STRETCH)
		{
			fit = Fit.TOO_HARD;
			reason = "Suggested strength " + level + ", you're about " + power.getValue();
		}
		else if (gap > 0)
		{
			fit = Fit.STRETCH;
			reason = "A bit above you (suggested " + level + ", you're about " + power.getValue() + ")";
		}
		else
		{
			fit = Fit.READY;
			reason = m != null && m.getTeam() > 1 ? "Fits your stats; usually done with a team" : "Fits your stats";
		}
		return new Ranked(t, m, score, fit, reason, missing);
	}

	private static int owned(DiaryItem item, DiaryPlayer p)
	{
		int n = 0;
		for (int id : item.getIds())
		{
			n += p.owned(id);
		}
		return n;
	}

	/**
	 * The player's kill count for the task's boss from chat messages, 0 when not seen.
	 */
	static int kcFor(CaMonster m, CaTask t, Map<String, Integer> kc)
	{
		if (m == null)
		{
			return kc.getOrDefault(t.getMonster().toLowerCase(), 0);
		}
		int best = 0;
		for (String name : m.getKcNames())
		{
			best = Math.max(best, kc.getOrDefault(name, 0));
		}
		return best;
	}
}
