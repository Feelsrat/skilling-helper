package com.skillinghelper.pvm.data;

import java.util.Set;
import lombok.Value;

/**
 * Something to bring. Any one of {@link #ids} counts as owning it; an empty set means it can't be checked
 * (e.g. "food") and is only shown as a reminder.
 */
@Value
public class ItemNeed
{
	String name;
	Set<Integer> ids;
	/**
	 * True when the content can't be done without it, false when it just helps.
	 */
	boolean required;

	public static ItemNeed need(String name, Integer... ids)
	{
		return new ItemNeed(name, Set.of(ids), true);
	}

	public static ItemNeed bring(String name, Integer... ids)
	{
		return new ItemNeed(name, Set.of(ids), false);
	}

	public boolean isCheckable()
	{
		return !ids.isEmpty();
	}
}
