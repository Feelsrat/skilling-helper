package com.skillinghelper.quests.data;

import java.util.List;
import lombok.Value;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * One quest or miniquest as the wiki describes it: where it starts, what it needs and what it gives.
 */
@Value
public class QuestInfo
{
	/**
	 * A skill (or combat level, when {@code skill} is null) the quest needs.
	 */
	@Value
	public static class SkillReq
	{
		Skill skill;
		int level;
		boolean boostable;

		public String label()
		{
			return level + " " + (skill == null ? "Combat" : skill.getName());
		}
	}

	/**
	 * The wiki page name, e.g. "Recipe for Disaster/Freeing Evil Dave".
	 */
	String name;
	boolean members;
	String difficulty;
	String length;
	/**
	 * Where and how it starts, e.g. "Talk to the Cook in the kitchen of Lumbridge Castle."
	 */
	String start;
	/**
	 * The start spot, or null when the wiki has none.
	 */
	WorldPoint startPoint;
	/**
	 * The first name in the start text, usually the NPC to talk to. Empty when there's none.
	 */
	String startNpc;
	List<SkillReq> skills;
	/**
	 * Quests that must be finished first (the wiki lists the whole chain).
	 */
	List<String> quests;
	/**
	 * Quests that only need to be started first.
	 */
	List<String> questsStarted;
	int questPoints;
	/**
	 * Recommended combat level, 0 if none.
	 */
	int combat;
	List<String> items;
	List<String> kills;
	List<String> rewards;

	/**
	 * The name the in-game Quest list shows: Recipe for Disaster's parts all sit under one line.
	 */
	public String listName()
	{
		return name.startsWith(QuestData.RFD + "/") ? QuestData.RFD : name;
	}

	/**
	 * A short name for the panel: "RFD: Freeing Evil Dave".
	 */
	public String shortName()
	{
		return name.startsWith(QuestData.RFD + "/") ? "RFD: " + name.substring(QuestData.RFD.length() + 1) : name;
	}
}
