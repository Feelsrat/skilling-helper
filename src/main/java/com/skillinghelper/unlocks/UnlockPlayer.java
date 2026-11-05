package com.skillinghelper.unlocks;

import com.skillinghelper.diary.DiaryPlayer;
import java.util.Collections;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * What the unlocks guide reads: the player (same as the diary guide reads) plus diary progress and which of the
 * plugins it hands work to are on.
 */
@Value
@Builder(toBuilder = true)
public class UnlockPlayer
{
	DiaryPlayer player;
	/**
	 * "REGION.TIER" to {tasks done, tasks in the tier}.
	 */
	@Builder.Default
	Map<String, int[]> diaryProgress = Collections.emptyMap();
	boolean questHelper;
	boolean diaryGuides;
	boolean skillingHelper;
}
