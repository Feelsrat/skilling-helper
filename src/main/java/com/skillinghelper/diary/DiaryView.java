package com.skillinghelper.diary;

import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryTier;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Everything the side panel shows, built on the client thread. Compared by value so the panel only redraws when
 * something it shows changed.
 */
@Value
@Builder
public class DiaryView
{
	public enum TaskState
	{
		DONE,
		/**
		 * The guide is on this task right now.
		 */
		CURRENT,
		/**
		 * The player has what it takes.
		 */
		READY,
		/**
		 * Requirements missing (listed on the row).
		 */
		LOCKED,
		/**
		 * Skipped by the player this session.
		 */
		SKIPPED
	}

	@Value
	public static class TierRow
	{
		DiaryTier tier;
		int done;
		int total;
		boolean claimed;
	}

	@Value
	public static class TaskRow
	{
		String key;
		int number;
		String name;
		String text;
		TaskState state;
		List<String> missing;
		List<String> notes;
		List<String> items;
		boolean wilderness;
	}

	DiaryRegion region;
	DiaryTier tier;
	boolean loggedIn;
	/**
	 * Whether the region has step-by-step guides yet.
	 */
	boolean guided;
	/**
	 * Rewards claimed per region, for the region picker.
	 */
	Map<DiaryRegion, Integer> claimedTiers;
	List<TierRow> tiers;
	List<TaskRow> tasks;
	/**
	 * Everything still missing across the tier's unfinished tasks, without repeats.
	 */
	List<String> stillNeeded;
	boolean running;
	DiaryStep step;
	boolean wildernessAllowed;
}
