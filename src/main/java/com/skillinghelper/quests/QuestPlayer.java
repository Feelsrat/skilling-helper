package com.skillinghelper.quests;

import com.skillinghelper.travel.TravelPick;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * What the quest order guide knows about the player, read on the client thread.
 */
@Value
@Builder(toBuilder = true)
public class QuestPlayer
{
	/**
	 * Quest state by wiki quest name. Missing means not started.
	 */
	Map<String, QuestState> quests;
	Map<Skill, Integer> levels;
	Map<Skill, Integer> boosted;
	int combatLevel;
	int questPoints;
	boolean members;
	/**
	 * Achievement Diary rewards claimed, as "REGION.TIER".
	 */
	Set<String> claimedDiaries;
	/**
	 * Route activities the player ticked off (the game doesn't track them as quests), by ref.
	 */
	Set<String> ticked;
	boolean allowWilderness;
	WorldPoint location;
	/**
	 * Whether the Quest Helper plugin is installed and on.
	 */
	boolean questHelper;
	/**
	 * Quest Helper's own "Auto start helper" setting: it starts guiding a quest the moment the game says it started.
	 */
	boolean questHelperAutoStart;
	/**
	 * The player's ignore list ({@link QuestIgnoreList}).
	 */
	Set<String> ignored;
	@Builder.Default
	List<TravelPick> travel = Collections.emptyList();

	public QuestState state(String quest)
	{
		QuestState s = quests == null ? null : quests.get(quest);
		return s == null ? QuestState.NOT_STARTED : s;
	}

	public int level(Skill s)
	{
		return levels == null ? 1 : levels.getOrDefault(s, 1);
	}

	public int boostedLevel(Skill s)
	{
		return boosted == null ? level(s) : boosted.getOrDefault(s, level(s));
	}
}
