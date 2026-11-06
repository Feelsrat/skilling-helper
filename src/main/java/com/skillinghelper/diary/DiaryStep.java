package com.skillinghelper.diary;

import com.skillinghelper.data.Location;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * What to do right now for the running diary guide. Rebuilt every tick and read by the overlays and the panel.
 */
@Value
@Builder(toBuilder = true)
public class DiaryStep
{
	public enum Kind
	{
		/**
		 * Nothing left the player can do in this tier yet.
		 */
		LOCKED,
		/**
		 * An item can't be bought and the player doesn't own one.
		 */
		GET_ITEM,
		/**
		 * Buy what's missing (Grand Exchange or a shop).
		 */
		BUY,
		/**
		 * Walk to a bank and open it.
		 */
		OPEN_BANK,
		/**
		 * Bank is open: take out what the task needs.
		 */
		WITHDRAW,
		/**
		 * Walk to where the task (or its next part) happens.
		 */
		TRAVEL,
		/**
		 * At the spot: do it.
		 */
		DO,
		/**
		 * Every task is done: claim the reward.
		 */
		CLAIM,
		/**
		 * Reward claimed.
		 */
		DONE
	}

	Kind kind;
	/**
	 * The task this step is for, or null for claim and done steps.
	 */
	String taskKey;
	String title;
	@Singular
	List<String> lines;
	/**
	 * Where to walk to, or null when the step doesn't move the player.
	 */
	Location destination;
	/**
	 * Where the task itself happens, even while this step sends the player to a bank first. Travel items are
	 * planned for this spot.
	 */
	Location goal;
	@Builder.Default
	Set<String> npcs = Collections.emptySet();
	@Builder.Default
	Set<String> objects = Collections.emptySet();
	/**
	 * Items to outline in the bank, with how many to take out.
	 */
	@Builder.Default
	Map<Integer, Integer> withdraw = Collections.emptyMap();
	/**
	 * Items to outline in the inventory.
	 */
	@Builder.Default
	Set<Integer> use = Collections.emptySet();
	/**
	 * Travel item to take out of the bank, outlined in its own colour.
	 */
	@Builder.Default
	Set<Integer> travel = Collections.emptySet();

	/**
	 * Everything this step wants from the bank, for the shared bank filter button: what to take out, what gets
	 * used, and the travel item.
	 */
	public Set<Integer> bankItems()
	{
		Set<Integer> items = new HashSet<>(withdraw.keySet());
		items.addAll(use);
		items.addAll(travel);
		return items;
	}
}
