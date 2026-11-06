package com.skillinghelper;

import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar, such as the Account Planner, to open
 * the Quest Order Guide on one quest and start its guide (which hands the quest to Quest Helper).
 */
@Value
public class OpenQuestRequest
{
	/**
	 * The quest's wiki name, e.g. "Recipe for Disaster/Freeing Evil Dave".
	 */
	String quest;
}
