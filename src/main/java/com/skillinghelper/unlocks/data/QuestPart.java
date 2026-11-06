package com.skillinghelper.unlocks.data;

import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * A quest to do on the way to an unlock. The plugin doesn't guide quests itself: it sends the player to the Quest
 * Helper plugin, which shows every step.
 */
@Value
@Builder
public class QuestPart
{
	Quest quest;
	/**
	 * Skill levels the quest needs (the main ones, so the list can say what's missing). Quest Helper shows the rest.
	 */
	@Singular
	Map<Skill, Integer> skills;
	/**
	 * Counts as far enough once this varbit is at least {@link #varbitMin}, before the quest is finished (e.g. fairy
	 * rings open part-way through Fairytale II). 0 when the quest has to be finished.
	 */
	int varbit;
	int varbitMin;
	/**
	 * Extra advice shown on the step, or null.
	 */
	String note;
}
