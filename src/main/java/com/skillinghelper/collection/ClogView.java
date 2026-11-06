package com.skillinghelper.collection;

import com.skillinghelper.collection.data.ClogTab;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Everything the side panel shows. Compared by value so the panel only redraws when something changed.
 */
@Value
@Builder
public class ClogView
{
	public enum Mode
	{
		QUICKEST("Quickest new slots"),
		CLOSEST("Closest to complete"),
		PETS("Pets"),
		BROWSE("Browse the log");

		private final String label;

		Mode(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	boolean loggedIn;
	/**
	 * Slots filled (seen in the log, announced in chat, or owned).
	 */
	int have;
	int total;
	/**
	 * Slots whose state the plugin has seen on a log page (filled or empty).
	 */
	int seen;
	/**
	 * The game's own count of filled slots and of all slots, 0 when the game hasn't sent it yet.
	 */
	int gameCount;
	int gameTotal;
	/**
	 * When the log was last read in full (the log's Search button), epoch millis; 0 = never.
	 */
	long syncedAt;
	/**
	 * Slots the game counts as filled beyond what the plugin knows (new since the last sync, while the new-slot chat
	 * message is off), 0 when in step.
	 */
	int unsynced;
	Mode mode;
	ClogTab tab;
	List<SlotRow> slots;
	List<EntryRow> entries;
	boolean wildernessAllowed;
	boolean showLocked;
	/**
	 * The running guide: its step and what it's going for; null when no guide runs.
	 */
	Guide guide;

	@Value
	public static class Guide
	{
		String entry;
		String slot;
		com.skillinghelper.diary.DiaryStep step;
		/**
		 * Plugin the page's full guide is in, or null.
		 */
		String handOff;
		/**
		 * Combat Achievement Guides' name for the boss, or null.
		 */
		String combatAchievements;
		/**
		 * Every slot of the page.
		 */
		List<SlotRow> slots;
		int have;
		int total;
	}

	@Value
	public static class SlotRow
	{
		int id;
		String name;
		/**
		 * The page to go to for it.
		 */
		String entry;
		double percent;
		ClogProgress.State state;
		List<String> missing;
		boolean wilderness;
		/**
		 * Plugin with a full guide for it, or null.
		 */
		String guide;
		/**
		 * The plugin can take the player there (a guide, boss data or a place on the map).
		 */
		boolean guided;
	}

	@Value
	public static class EntryRow
	{
		String name;
		ClogTab tab;
		int have;
		int total;
		/**
		 * The quickest slots still to get, e.g. "Mole claw (61%)".
		 */
		List<String> next;
		List<String> missing;
		boolean wilderness;
		String guide;
		boolean guided;
	}
}
