package com.skillinghelper;

import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar, such as Money Making, to start the
 * PvM Progression guide for one boss or raid.
 */
@Value
public class StartPvmGuideRequest
{
	/**
	 * The PvM ladder entry's id, e.g. "barrows".
	 */
	String contentId;
}
