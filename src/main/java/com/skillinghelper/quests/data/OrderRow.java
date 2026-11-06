package com.skillinghelper.quests.data;

import java.util.List;
import lombok.Value;

/**
 * One row of a wiki optimal quest route: a quest, the first part of one, an Achievement Diary tier, or another
 * unlock (balloon route, museum quiz...).
 */
@Value
public class OrderRow
{
	public enum Kind
	{
		QUEST,
		/**
		 * Start a quest and do its first part only; the full quest comes later on the route.
		 */
		PARTIAL,
		/**
		 * An Achievement Diary tier: ref is "REGION.TIER", or "ALL.TIER" for every region's tier.
		 */
		DIARY,
		/**
		 * Something the game doesn't track as a quest; the player ticks it off.
		 */
		ACTIVITY
	}

	Route route;
	/**
	 * Position on the route, from 0.
	 */
	int index;
	Kind kind;
	/**
	 * Quest name (QUEST, PARTIAL), diary "REGION.TIER", or the activity's wiki page.
	 */
	String ref;
	/**
	 * What the wiki's route calls it.
	 */
	String label;
	/**
	 * The route's own tips for this row, one per line (sub-points start with two spaces).
	 */
	List<String> notes;

	/**
	 * Unique within a route.
	 */
	public String key()
	{
		return kind + ":" + ref;
	}
}
