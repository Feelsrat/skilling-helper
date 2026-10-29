package com.skillinghelper.pvm;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * What the progression check needs to know about the player, read from the client on the client thread.
 */
@Value
@Builder
public class PlayerView
{
	@Singular
	Map<Skill, Integer> levels;
	@Singular
	Map<Quest, QuestState> quests;
	/**
	 * Canonical item ids carried, worn or last seen in the bank.
	 */
	@Singular("owned")
	Set<Integer> owned;
	/**
	 * Kill counts by content id.
	 */
	@Singular("killCount")
	Map<String, Integer> killCounts;
	/**
	 * Content ids the player marked as done themselves.
	 */
	@Singular("markedDone")
	Set<String> markedDone;

	public int level(Skill skill)
	{
		return levels.getOrDefault(skill, 1);
	}

	public QuestState quest(Quest quest)
	{
		return quests.getOrDefault(quest, QuestState.NOT_STARTED);
	}

	public int killCount(String contentId)
	{
		return killCounts.getOrDefault(contentId, 0);
	}
}
