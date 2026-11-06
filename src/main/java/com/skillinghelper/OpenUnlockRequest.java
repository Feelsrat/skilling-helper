package com.skillinghelper;

import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar, such as the Account Planner, to open
 * the Account Unlocks panel on one unlock and start its guide.
 */
@Value
public class OpenUnlockRequest
{
	/**
	 * The unlock's id, e.g. "graceful".
	 */
	String unlockId;
}
