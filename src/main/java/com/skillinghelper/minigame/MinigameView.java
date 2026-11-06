package com.skillinghelper.minigame;

import com.skillinghelper.diary.DiaryStep;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Everything the side panel shows, built on the client thread. Compared by value so the panel only redraws when
 * something it shows changed.
 */
@Value
@Builder
public class MinigameView
{
	/**
	 * One minigame in the picker and the "still worth doing" list.
	 */
	@Value
	public static class Row
	{
		String id;
		String name;
		String category;
		/**
		 * Requirements met.
		 */
		boolean ready;
		/**
		 * Rewards worth chasing owned / that can be checked.
		 */
		int owned;
		int checkable;
	}

	@Value
	public static class RewardRow
	{
		String name;
		String why;
		String cost;
		boolean worth;
		/**
		 * Null when it can't be checked; else whether the player has it.
		 */
		Boolean owned;
		boolean affordable;
	}

	/**
	 * Where the full guide lives when it isn't here.
	 */
	public enum Handoff
	{
		NONE,
		SKILLING_HELPER,
		PVM_PROGRESSION
	}

	boolean loggedIn;
	/**
	 * Whether the bank has been seen (otherwise owned checks only count what's carried).
	 */
	boolean bankKnown;
	List<Row> rows;

	String id;
	String name;
	String summary;
	String levelRange;
	String team;
	boolean dangerous;
	boolean wilderness;
	String getThere;
	List<String> missing;
	List<String> met;
	List<String> notes;
	List<String> bring;
	List<String> balances;
	List<RewardRow> rewards;

	Handoff handoff;
	/**
	 * The plugin with the full guide is turned on.
	 */
	boolean handoffActive;
	boolean guidedHere;
	boolean running;
	DiaryStep step;
	boolean wildernessAllowed;
}
