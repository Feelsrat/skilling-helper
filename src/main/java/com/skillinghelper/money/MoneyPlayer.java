package com.skillinghelper.money;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * What the method list needs to know about the account: levels, quests and world type.
 */
@Value
@Builder(toBuilder = true)
public class MoneyPlayer
{
	public static final MoneyPlayer NONE = MoneyPlayer.builder().members(true).build();

	@Singular
	Map<Skill, Integer> levels;
	/**
	 * Lower-case names of finished quests.
	 */
	@Builder.Default
	Set<String> questsDone = Collections.emptySet();
	/**
	 * Lower-case names of every quest the plugin can read, so other names are reported as unchecked.
	 */
	@Builder.Default
	Set<String> knownQuests = Collections.emptySet();
	boolean members;
	/**
	 * Coins carried plus coins in the bank, or -1 when the bank hasn't been seen.
	 */
	@Builder.Default
	long coins = -1;
	/**
	 * Item id to count carried plus banked, or null when the bank hasn't been seen.
	 */
	Map<Integer, Integer> owned;
	/**
	 * Item ids that stack in one inventory slot.
	 */
	@Builder.Default
	Set<Integer> stackable = Collections.emptySet();

	public int level(Skill skill)
	{
		return levels.getOrDefault(skill, 1);
	}
}
