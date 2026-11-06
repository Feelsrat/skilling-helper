package com.skillinghelper.ca;

import com.skillinghelper.ca.data.CaTier;
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
public class CaView
{
	@Value
	public static class TierRow
	{
		CaTier tier;
		/**
		 * Points this tier's reward needs.
		 */
		int threshold;
		int done;
		int total;
		boolean reached;
		/**
		 * True when the hilt is owned, false when it isn't, null before the bank was seen.
		 */
		Boolean claimed;
	}

	@Value
	public static class TaskRow
	{
		String key;
		String name;
		CaTier tier;
		String monster;
		String type;
		String text;
		CaRanker.Fit fit;
		String reason;
		List<String> missing;
		boolean current;
		boolean skipped;
		boolean wilderness;
	}

	boolean loggedIn;
	/**
	 * The tier picked in the panel, or null for every tier.
	 */
	CaTier scope;
	/**
	 * Only this boss's tasks are listed, or null for all.
	 */
	String focus;
	int points;
	List<TierRow> tiers;
	int power;
	String powerStyle;
	String weapon;
	/**
	 * The easiest tasks not done yet, at most {@link #limit}.
	 */
	List<TaskRow> tasks;
	int limit;
	/**
	 * Unfinished tasks in the picked scope, shown or not.
	 */
	int left;
	int doneInScope;
	int totalInScope;
	boolean running;
	DiaryStep step;
	/**
	 * Boss guide id in the PvM Progression plugin for the current task, or null.
	 */
	String pvmId;
	/**
	 * Wiki page of the current task, or null.
	 */
	String wikiPage;
	boolean wildernessAllowed;
}
