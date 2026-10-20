package com.skillinghelper.data;

import lombok.Value;

/**
 * An item the method needs. Any of {@link #ids} satisfies it (e.g. noted variants or upgraded versions).
 */
@Value
public class ItemReq
{
	String name;
	int[] ids;
	/**
	 * How many to carry in the inventory for one trip.
	 */
	int quantity;
	/**
	 * How many are used up per action. 0 for tools that are never consumed.
	 */
	int perAction;

	public static ItemReq tool(String name, int... ids)
	{
		return new ItemReq(name, ids, 1, 0);
	}

	public static ItemReq consumed(String name, int carry, int perAction, int... ids)
	{
		return new ItemReq(name, ids, carry, perAction);
	}

	public int primaryId()
	{
		return ids[0];
	}

	public boolean matches(int itemId)
	{
		for (int id : ids)
		{
			if (id == itemId)
			{
				return true;
			}
		}
		return false;
	}
}
