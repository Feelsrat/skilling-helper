package com.skillinghelper.clue;

import com.skillinghelper.clue.data.ClueTier;
import com.skillinghelper.diary.DiaryStep;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Everything the clue panel shows, built on the client thread. Compared by value so the panel only redraws when
 * something it shows changed.
 */
@Value
@Builder
public class ClueView
{
	@Value
	public static class WayRow
	{
		String key;
		String name;
		/**
		 * What the player still needs before they can do it; empty when ready.
		 */
		List<String> missing;
		/**
		 * Gold to spend on it at today's prices (impling jars), or 0.
		 */
		long cost;
		boolean current;
		boolean wilderness;
	}

	@Value
	public static class StashRow
	{
		String key;
		String name;
		String where;
		List<String> items;
		/**
		 * True when every item for it is in the bank or carried; null when that can't be checked (bank not seen, or
		 * some items come in many versions).
		 */
		Boolean ownAll;
		boolean wilderness;
		boolean current;
	}

	@Value
	public static class KitRow
	{
		String label;
		boolean ok;
	}

	boolean loggedIn;
	boolean members;
	ClueTier tier;
	/**
	 * Clues of this tier the account finished (from the chat), 0 when not seen yet.
	 */
	int completed;
	@Builder.Default
	ClueHoldings holdings = ClueHoldings.of(Collections.emptyMap(), null, id -> null);
	boolean running;
	DiaryStep step;
	boolean cluePluginOn;
	boolean wildernessAllowed;
	@Builder.Default
	List<WayRow> ways = Collections.emptyList();
	@Builder.Default
	List<KitRow> kit = Collections.emptyList();
	int constructionLevel;
	@Builder.Default
	List<StashRow> stashes = Collections.emptyList();
	long averageCasket;
	@Builder.Default
	List<ClueRewards.Pick> topUniques = Collections.emptyList();
}
