package com.skillinghelper;

import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar, such as the Account Planner, to open
 * the Money Making panel so the player can pick a money maker.
 */
@Value
public class OpenMoneyMakingRequest
{
	/**
	 * Gold the player is short of, shown in the panel's chat note; 0 when not known.
	 */
	long goal;
}
