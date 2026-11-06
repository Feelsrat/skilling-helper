package com.skillinghelper.diary;

import com.skillinghelper.travel.TravelPick;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * Everything the diary guide reads about the player, gathered on the client thread each tick.
 */
@Value
@Builder(toBuilder = true)
public class DiaryPlayer
{
	WorldPoint location;
	@Builder.Default
	Map<Skill, Integer> levels = Collections.emptyMap();
	/**
	 * Boosted levels (potions, stews); count for skills that allow boosts.
	 */
	@Builder.Default
	Map<Skill, Integer> boosted = Collections.emptyMap();
	int combatLevel;
	@Builder.Default
	Map<Quest, QuestState> quests = Collections.emptyMap();
	/**
	 * Keys of tasks the game says are done.
	 */
	@Builder.Default
	Set<String> done = Collections.emptySet();
	/**
	 * "REGION.TIER" of rewards already claimed.
	 */
	@Builder.Default
	Set<String> claimed = Collections.emptySet();
	@Builder.Default
	IntUnaryOperator varbit = id -> 0;
	@Builder.Default
	IntUnaryOperator varp = id -> 0;
	/**
	 * Item id to count across the inventory and worn equipment.
	 */
	@Builder.Default
	Map<Integer, Integer> carried = Collections.emptyMap();
	/**
	 * Item id to count in the bank (placeholders left out), or null before the bank was opened this session.
	 */
	Map<Integer, Integer> bank;
	boolean bankOpen;
	/**
	 * Key of the task the bank was last opened for, so a skipped travel item doesn't send the player back again.
	 */
	String bankedFor;
	/**
	 * Empty inventory slots.
	 */
	@Builder.Default
	int freeSlots = 28;
	/**
	 * Whether an item stacks in one inventory slot.
	 */
	@Builder.Default
	IntPredicate stackable = id -> false;
	boolean members;
	boolean allowWilderness;
	boolean preferShops;
	@Builder.Default
	IntUnaryOperator buyPrice = id -> 0;
	/**
	 * Travel item and/or coins to bring for the step's destination.
	 */
	@Builder.Default
	List<TravelPick> travel = Collections.emptyList();

	public int level(Skill s)
	{
		return levels.getOrDefault(s, 1);
	}

	public int boostedLevel(Skill s)
	{
		return Math.max(level(s), boosted.getOrDefault(s, 0));
	}

	public QuestState quest(Quest q)
	{
		return quests.getOrDefault(q, QuestState.NOT_STARTED);
	}

	public int owned(int id)
	{
		return carried.getOrDefault(id, 0) + (bank == null ? 0 : bank.getOrDefault(id, 0));
	}
}
