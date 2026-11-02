package com.skillinghelper;

import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar, such as Money Making, to open the
 * Skilling Helper guide with one method chosen, e.g. a money maker that has a full click-by-click guide there.
 */
@Value
public class OpenMethodRequest
{
	String methodId;
}
