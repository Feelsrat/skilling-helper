package com.skillinghelper.collection;

import com.skillinghelper.collection.data.ClogEntry;
import com.skillinghelper.collection.data.ClogLinks;
import com.skillinghelper.collection.data.GuideLink;
import com.skillinghelper.data.Method;
import com.skillinghelper.pvm.PlayerView;
import com.skillinghelper.pvm.Readiness;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.QuestReq;
import com.skillinghelper.pvm.data.StatReq;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * What keeps the player from a collection log page, as far as this plugin pack knows: the linked PvM guide's
 * quests and levels, the linked Skilling Helper method's levels and quests, and a monster's Slayer level. Pages
 * without a link are only checked for Slayer level (the wiki page has the rest).
 */
public final class ClogRequirements
{
	@Value
	public static class Check
	{
		static final Check NONE = new Check(List.of(), List.of());

		/**
		 * Hard requirements not met: quests and required levels. The page can't be done yet.
		 */
		List<String> missing;
		/**
		 * Recommended levels not reached yet (doable, but harder).
		 */
		List<String> train;

		public boolean isLocked()
		{
			return !missing.isEmpty();
		}
	}

	private final Map<String, PvmContent> pvm = new HashMap<>();
	private final Map<String, Method> methods = new HashMap<>();

	public ClogRequirements(Collection<PvmContent> ladder, Collection<Method> allMethods)
	{
		for (PvmContent c : ladder)
		{
			pvm.put(c.getId(), c);
		}
		for (Method m : allMethods)
		{
			methods.put(m.getId(), m);
		}
	}

	public PvmContent pvm(String id)
	{
		return pvm.get(id);
	}

	public Method method(String id)
	{
		return methods.get(id);
	}

	/**
	 * Every quest a check can look at, so the plugin knows which to read.
	 */
	public Set<Quest> quests()
	{
		Set<Quest> out = EnumSet.noneOf(Quest.class);
		for (PvmContent c : pvm.values())
		{
			for (QuestReq q : c.getQuests())
			{
				out.add(q.getQuest());
			}
		}
		for (Method m : methods.values())
		{
			out.addAll(m.getQuests());
			out.addAll(m.getQuestsStarted());
		}
		return out;
	}

	public Check check(ClogEntry e, Map<Skill, Integer> levels, Map<Quest, QuestState> quests)
	{
		return check(e, ClogLinks.of(e), levels, quests);
	}

	public Check check(ClogEntry e, GuideLink link, Map<Skill, Integer> levels, Map<Quest, QuestState> quests)
	{
		List<String> missing = new ArrayList<>();
		List<String> train = new ArrayList<>();
		if (e != null && e.getSlayerLevel() > level(levels, Skill.SLAYER))
		{
			missing.add("Level " + e.getSlayerLevel() + " Slayer");
		}
		if (link != null && link.getKind() == GuideLink.Kind.PVM && pvm.containsKey(link.getTarget()))
		{
			PlayerView.PlayerViewBuilder view = PlayerView.builder().levels(levels).quests(quests);
			Readiness r = Readiness.of(pvm.get(link.getTarget()), view.build());
			for (QuestReq q : r.getMissingQuests())
			{
				missing.add((q.isStartedIsEnough() ? "Start " : "") + q.getQuest().getName());
			}
			for (StatReq s : r.getMissingLevels())
			{
				missing.add("Level " + s.getLevel() + " " + s.getSkill().getName());
			}
			for (StatReq s : r.getLevelGaps())
			{
				train.add(s.getSkill().getName() + " " + s.getLevel() + " recommended");
			}
		}
		else if (link != null && link.getRequirementsOf() != null && methods.containsKey(link.getRequirementsOf()))
		{
			Method m = methods.get(link.getRequirementsOf());
			if (m.getMinLevel() > level(levels, m.getSkill()))
			{
				missing.add("Level " + m.getMinLevel() + " " + m.getSkill().getName());
			}
			for (Map.Entry<Skill, Integer> s : m.getSkillLevels().entrySet())
			{
				if (s.getValue() > level(levels, s.getKey()))
				{
					missing.add("Level " + s.getValue() + " " + s.getKey().getName());
				}
			}
			for (Quest q : m.getQuests())
			{
				if (quests.getOrDefault(q, QuestState.NOT_STARTED) != QuestState.FINISHED)
				{
					missing.add(q.getName());
				}
			}
			for (Quest q : m.getQuestsStarted())
			{
				if (quests.getOrDefault(q, QuestState.NOT_STARTED) == QuestState.NOT_STARTED)
				{
					missing.add("Start " + q.getName());
				}
			}
		}
		return missing.isEmpty() && train.isEmpty() ? Check.NONE : new Check(missing, train);
	}

	private static int level(Map<Skill, Integer> levels, Skill s)
	{
		return levels.getOrDefault(s, 1);
	}
}
