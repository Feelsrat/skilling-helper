package com.skillinghelper;

import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar, such as the Collection Log Guide, to
 * open the Clue Scroll Guide panel on one clue tier.
 */
@Value
public class OpenClueGuideRequest
{
	/**
	 * The tier: "beginner", "easy", "medium", "hard", "elite" or "master".
	 */
	String tier;
}
