package com.skillinghelper;

import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by the Money Making plugin to have the Banked XP Guide walk the
 * player through processing one banked item for profit and then selling what it made.
 */
@Value
public class StartBankProfitRequest
{
	/**
	 * Id of the profit recipe (a banked activity id).
	 */
	String recipeId;
}
