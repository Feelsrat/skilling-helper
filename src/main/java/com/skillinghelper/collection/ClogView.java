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
	Mode mode;
	ClogTab tab;
	List<SlotRow> slots;
	List<EntryRow> entries;
	EntryDetail detail;
	boolean wildernessAllowed;
	boolean showLocked;

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
	}

	@Value
	public static class EntryDetail
	{
		String name;
		ClogTab tab;
		String intro;
		int have;
		int total;
		List<SlotRow> slots;
		List<String> steps;
		List<String> missing;
		List<String> train;
		boolean wilderness;
		String guide;
		boolean hasPlace;
		boolean showingWay;
		/**
		 * The boss as Combat Achievement Guides names it, when it has combat tasks; else null.
		 */
		String combatAchievements;
	}
}
