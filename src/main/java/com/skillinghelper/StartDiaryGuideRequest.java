package com.skillinghelper;

import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryTier;
import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar, such as Account Unlocks, to open the
 * Achievement Diary guide for one diary tier and start it.
 */
@Value
public class StartDiaryGuideRequest
{
	DiaryRegion region;
	DiaryTier tier;
}
