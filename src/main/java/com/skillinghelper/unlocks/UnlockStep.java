package com.skillinghelper.unlocks;

import com.skillinghelper.diary.DiaryStep;
import lombok.Value;

/**
 * The step to show for an unlock, plus what the panel's button hands the player to (another plugin).
 */
@Value
public class UnlockStep
{
	public enum Action
	{
		NONE,
		/**
		 * Do a quest with the Quest Helper plugin (no button: Quest Helper can't be opened from here).
		 */
		QUEST,
		/**
		 * Open the Achievement Diary guide for the unlock's diary tier.
		 */
		DIARY,
		/**
		 * Open Skilling Helper on the skill (or method) that earns the points.
		 */
		TRAIN
	}

	String unlockId;
	DiaryStep step;
	Action action;
}
