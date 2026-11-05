package com.skillinghelper.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import net.runelite.api.Quest;

/**
 * "Any {@code needed} of these quests", e.g. Nightmare Zone opens after 5 of its quests. Each quest can carry a
 * label, such as the boss it unlocks, which is shown instead of the quest name where it helps.
 */
@Value
public class QuestGroup
{
	/**
	 * What the quests unlock, e.g. "Nightmare Zone".
	 */
	String name;
	int needed;
	/**
	 * Quests in order of preference, each with a label (or null).
	 */
	Map<Quest, String> quests;

	public static QuestGroup of(String name, int needed, Quest... quests)
	{
		Map<Quest, String> m = new LinkedHashMap<>();
		for (Quest q : quests)
		{
			m.put(q, null);
		}
		return new QuestGroup(name, needed, m);
	}

	/**
	 * Quests that each unlock a labelled thing, e.g. a Nightmare Zone boss, as quest, label pairs.
	 */
	public static QuestGroup labelled(String name, int needed, Object... questLabelPairs)
	{
		Map<Quest, String> m = new LinkedHashMap<>();
		for (int i = 0; i + 1 < questLabelPairs.length; i += 2)
		{
			m.put((Quest) questLabelPairs[i], (String) questLabelPairs[i + 1]);
		}
		return new QuestGroup(name, needed, m);
	}

	/**
	 * Quests of the group the player has done, in preference order.
	 */
	public List<Quest> done(Collection<Quest> completed)
	{
		List<Quest> done = new ArrayList<>();
		for (Quest q : quests.keySet())
		{
			if (completed.contains(q))
			{
				done.add(q);
			}
		}
		return done;
	}

	public boolean isMet(Collection<Quest> completed)
	{
		return done(completed).size() >= needed;
	}

	/**
	 * The labels of the first {@code needed} quests the player has done, e.g. "Count Draynor, Sand Snake, King
	 * Roald", or all of them when fewer are done.
	 */
	public String picks(Collection<Quest> completed)
	{
		List<String> use = new ArrayList<>();
		for (Quest q : done(completed))
		{
			if (use.size() >= needed)
			{
				break;
			}
			String label = quests.get(q);
			use.add(label == null ? q.getName() : label);
		}
		return use.isEmpty() ? "none yet" : String.join(", ", use);
	}

	/**
	 * The label of a quest, e.g. "Count Draynor (Vampyre Slayer)", or just the quest name.
	 */
	public String describe(Quest q)
	{
		String label = quests.get(q);
		return label == null ? q.getName() : label + " (" + q.getName() + ")";
	}

	/**
	 * What the player has and what is missing, e.g. "Nightmare Zone: 3 of 5 quests done. Do 2 more of: Lost City,
	 * Fight Arena...". When met, lists the first {@code needed} it will use.
	 */
	public String summary(Collection<Quest> completed)
	{
		List<Quest> done = done(completed);
		if (done.size() >= needed)
		{
			return name + ": " + picks(completed);
		}
		List<String> missing = new ArrayList<>();
		for (Quest q : quests.keySet())
		{
			if (!completed.contains(q) && missing.size() < 6)
			{
				missing.add(describe(q));
			}
		}
		return name + ": " + done.size() + " of " + needed + " unlocked. Finish " + (needed - done.size())
			+ " more of: " + String.join(", ", missing) + (missing.size() < quests.size() - done.size() ? "..." : "");
	}
}
