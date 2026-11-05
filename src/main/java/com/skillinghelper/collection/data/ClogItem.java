package com.skillinghelper.collection.data;

import lombok.Value;

/**
 * One slot of the collection log.
 */
@Value
public class ClogItem
{
	int id;
	String name;
	/**
	 * Share of the players the wiki tracks who have this slot, 0-100, or -1 when the wiki has no figure. Used as
	 * "how quick is this slot": the slots most players have are the ones that come quickest.
	 */
	double percent;

	/**
	 * {@link #percent} for ranking, with unknown slots treated as rare.
	 */
	public double rankPercent()
	{
		return percent < 0 ? 0.5 : percent;
	}
}
