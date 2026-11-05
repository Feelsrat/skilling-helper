package com.skillinghelper.planner.api;

import java.util.Collections;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Everything a {@link PlanStepProvider} may read about the account, gathered on the client thread. The var and
 * config readers are only valid during the {@link PlanStepProvider#steps} call that received them.
 */
@Value
@Builder(toBuilder = true)
public class AccountState
{
	@Builder.Default
	Map<Skill, Integer> levels = Collections.emptyMap();
	int combatLevel;
	@Builder.Default
	Map<Quest, QuestState> quests = Collections.emptyMap();
	@Builder.Default
	IntUnaryOperator varbit = id -> 0;
	@Builder.Default
	IntUnaryOperator varp = id -> 0;
	/**
	 * Reads this account's RuneLite profile config: "group.key" to its value (or null). Lets a provider read what
	 * its own plugin saved, such as PvM Progression's kill counts.
	 */
	@Builder.Default
	UnaryOperator<String> profileConfig = key -> null;
	/**
	 * Item id to count across the inventory, worn equipment and the last-seen bank.
	 */
	@Builder.Default
	Map<Integer, Integer> owned = Collections.emptyMap();
	/**
	 * Whether {@link #owned} includes the bank (it has been seen on this account).
	 */
	boolean bankKnown;
	/**
	 * Grand Exchange price of an item id.
	 */
	@Builder.Default
	IntUnaryOperator price = id -> 0;
	boolean members;
	boolean allowWilderness;
	/**
	 * The shared "hide outleveled guides" setting.
	 */
	boolean hideOutleveled;

	public int level(Skill s)
	{
		return levels.getOrDefault(s, s == Skill.HITPOINTS ? 10 : 1);
	}

	public QuestState quest(Quest q)
	{
		return quests.getOrDefault(q, QuestState.NOT_STARTED);
	}

	public int owned(int itemId)
	{
		return owned.getOrDefault(itemId, 0);
	}

	public long coins()
	{
		return owned(ItemID.COINS);
	}
}
