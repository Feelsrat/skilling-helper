package com.skillinghelper.banked;

import com.skillinghelper.banked.data.BankedActivities;
import com.skillinghelper.banked.data.BankedActivity;
import com.skillinghelper.data.Banks;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.StepResolver;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Works out what the items in the bank are worth in xp, which activity uses up each one, and the order to do them
 * in. Pure logic over a {@link PlayerState}, so it's unit tested without the client.
 */
public final class BankedPlanner
{
	/**
	 * Saved choice for a group the player doesn't want to use up.
	 */
	public static final String SKIP = "skip";

	private final StepResolver resolver;

	public BankedPlanner(PriceLookup prices)
	{
		this.resolver = new StepResolver(prices);
	}

	/**
	 * One banked item and the ways to use it.
	 */
	@Value
	public static class Group
	{
		String key;
		String itemName;
		int iconId;
		int count;
		List<Option> options;
		/**
		 * The option that will be used, or null when the player skips this item or can't do any option yet.
		 */
		Option chosen;
		boolean skipped;
	}

	/**
	 * One way to use a banked item.
	 */
	@Value
	public static class Option
	{
		BankedActivity activity;
		int actions;
		long xp;
		/**
		 * Why the player can't do it yet, or null.
		 */
		String locked;
	}

	/**
	 * The total for one skill if every chosen activity is done.
	 */
	@Value
	public static class SkillTotal
	{
		Skill skill;
		long xp;
		int levelNow;
		int levelAfter;
	}

	public List<Group> groups(PlayerState s, Map<String, String> choices)
	{
		return groups(s, choices, null);
	}

	/**
	 * Every group with at least one item in the bank or inventory, biggest xp first.
	 *
	 * @param choices saved activity id (or {@link #SKIP}) per group key
	 * @param focus   only use items for this skill, so items another skill needs aren't used up (logs kept for
	 *                Construction instead of burnt); null for every skill
	 */
	public List<Group> groups(PlayerState s, Map<String, String> choices, Skill focus)
	{
		Map<String, List<BankedActivity>> byGroup = new LinkedHashMap<>();
		for (BankedActivity a : BankedActivities.all())
		{
			byGroup.computeIfAbsent(a.getGroup(), k -> new ArrayList<>()).add(a);
		}
		List<Group> groups = new ArrayList<>();
		for (Map.Entry<String, List<BankedActivity>> e : byGroup.entrySet())
		{
			BankedActivity first = e.getValue().get(0);
			int count = s.owned(allIds(e.getValue()));
			if (count <= 0)
			{
				continue;
			}
			List<Option> options = new ArrayList<>();
			for (BankedActivity a : e.getValue())
			{
				if (focus == null || a.getSkill() == focus)
				{
					options.add(new Option(a, a.actions(s), a.xp(s), lockReason(a, s)));
				}
			}
			if (options.isEmpty())
			{
				continue;
			}
			String saved = choices.get(e.getKey());
			boolean skipped = SKIP.equals(saved);
			Option chosen = skipped ? null : choose(options, saved);
			groups.add(new Group(e.getKey(), first.getItemName(), first.getIconId(), count, options, chosen,
				skipped));
		}
		groups.sort(Comparator.comparingLong((Group g) -> g.getChosen() == null ? -1 : g.getChosen().getXp())
			.reversed());
		return groups;
	}

	/**
	 * Skills that have a use for something in the bank, for the skill picker.
	 */
	public List<Skill> skillsWithUses(PlayerState s)
	{
		Set<Skill> skills = EnumSet.noneOf(Skill.class);
		for (BankedActivity a : BankedActivities.all())
		{
			if (!skills.contains(a.getSkill()) && a.itemCount(s) > 0)
			{
				skills.add(a.getSkill());
			}
		}
		return new ArrayList<>(skills);
	}

	private static int[] allIds(List<BankedActivity> activities)
	{
		Set<Integer> ids = new LinkedHashSet<>();
		for (BankedActivity a : activities)
		{
			for (int id : a.getCountIds())
			{
				ids.add(id);
			}
		}
		return ids.stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * The saved option if it's still possible, otherwise the one with the most xp per item (fewest clicks on a tie).
	 */
	private static Option choose(List<Option> options, String saved)
	{
		Option best = null;
		for (Option o : options)
		{
			if (o.getLocked() != null || o.getActions() <= 0)
			{
				continue;
			}
			if (o.getActivity().getId().equals(saved))
			{
				return o;
			}
			if (best == null || better(o, best))
			{
				best = o;
			}
		}
		return best;
	}

	private static boolean better(Option o, Option than)
	{
		double xpEach = xpPerItem(o.getActivity());
		double otherEach = xpPerItem(than.getActivity());
		if (Math.abs(xpEach - otherEach) > 0.01)
		{
			return xpEach > otherEach;
		}
		return o.getActivity().getPerAction() > than.getActivity().getPerAction();
	}

	private static double xpPerItem(BankedActivity a)
	{
		return a.getMethod().getXpPerAction() / Math.max(1, a.getPerAction());
	}

	/**
	 * Why the player can't do an activity now: a level, quest or members' world they don't have. Null when they can.
	 */
	static String lockReason(BankedActivity a, PlayerState s)
	{
		Method m = a.getMethod();
		if (s.level(m.getSkill()) < m.getMinLevel())
		{
			return "Needs " + m.getSkill().getName() + " " + m.getMinLevel();
		}
		if (m.isMembers() && !s.isMembersWorld())
		{
			return "Members' world only";
		}
		for (Quest q : m.getQuests())
		{
			if (!s.getCompletedQuests().contains(q))
			{
				return "Needs " + q.getName();
			}
		}
		return null;
	}

	/**
	 * The chosen activities that can be done now, in the order to do them: activities that make another's items
	 * first, then by skill, then the most xp.
	 */
	public static List<Option> queue(List<Group> groups)
	{
		List<Option> queue = new ArrayList<>();
		for (Group g : groups)
		{
			if (g.getChosen() != null && g.getChosen().getActions() > 0)
			{
				queue.add(g.getChosen());
			}
		}
		queue.sort(Comparator.comparingInt((Option o) -> o.getActivity().getStage())
			.thenComparing(o -> o.getActivity().getSkill().ordinal())
			.thenComparing(Comparator.comparingLong(Option::getXp).reversed()));
		return queue;
	}

	/**
	 * Xp and levels per skill for doing everything in the queue.
	 */
	public static List<SkillTotal> totals(List<Option> queue, PlayerState s)
	{
		Map<Skill, Long> xp = new EnumMap<>(Skill.class);
		for (Option o : queue)
		{
			xp.merge(o.getActivity().getSkill(), o.getXp(), Long::sum);
		}
		List<SkillTotal> totals = new ArrayList<>();
		for (Map.Entry<Skill, Long> e : xp.entrySet())
		{
			int now = s.xp(e.getKey());
			long after = Math.min(Experience.MAX_SKILL_XP, now + e.getValue());
			totals.add(new SkillTotal(e.getKey(), e.getValue(), s.level(e.getKey()),
				Experience.getLevelForXp((int) after)));
		}
		totals.sort(Comparator.comparingLong(SkillTotal::getXp).reversed());
		return totals;
	}

	/**
	 * The activity's method placed where the player will do it: the closest bank, or the closest range.
	 */
	public static Method placed(BankedActivity a, PlayerState s)
	{
		Method m = a.getMethod();
		switch (a.getPlace())
		{
			case ANY_BANK:
			{
				Location bank = s.getLocation() == null || s.isInInstance() ? null
					: Banks.nearest(s.getLocation(), s.isMembersWorld());
				if (bank == null)
				{
					bank = Banks.GRAND_EXCHANGE;
				}
				return m.toBuilder().location(bank).bank(bank).build();
			}
			case RANGE:
				if (!s.isMembersWorld())
				{
					return m.toBuilder().location(BankedActivities.AL_KHARID_RANGE).bank(Banks.AL_KHARID).build();
				}
				return m;
			case FIXED:
			default:
				return m;
		}
	}

	/**
	 * The guide for one activity: what's needed and the next step to use up the rest of the banked items.
	 */
	public Plan plan(BankedActivity a, PlayerState s)
	{
		Method m = placed(a, s);
		int actions = a.actions(s);
		List<Requirement> reqs = resolver.requirements(m, s, actions);
		GuideStep step = resolver.resolveActions(m, s, actions, false, reqs);
		return new Plan(m, actions, reqs, step);
	}

	/**
	 * What {@link #plan} decided.
	 */
	@Value
	public static class Plan
	{
		Method method;
		int actions;
		List<Requirement> requirements;
		GuideStep step;
	}
}
