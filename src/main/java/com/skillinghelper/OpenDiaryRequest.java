package com.skillinghelper;

import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar, such as the Account Planner, to open
 * the Achievement Diary Guides panel on one diary tier and start its guide.
 */
@Value
public class OpenDiaryRequest
{
	/**
	 * {@code DiaryRegion} name, e.g. "VARROCK".
	 */
	String region;
	/**
	 * {@code DiaryTier} name, e.g. "EASY".
	 */
	String tier;
}
