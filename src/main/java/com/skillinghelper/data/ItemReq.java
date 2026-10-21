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
	 * How many to carry in the inventory for one trip. 0 means the whole stack (runes, arrows, coins...).
	 */
	int quantity;
	/**
	 * How many are used up per action. 0 for tools that are never consumed.
	 */
	int perAction;
	/**
	 * Something that replaces this item entirely when carried or worn, e.g. a staff of air instead of air runes.
	 * Null when there's no substitute.
	 */
	String substituteName;
	int[] substituteIds;

	public static ItemReq tool(String name, int... ids)
	{
		return new ItemReq(name, ids, 1, 0, null, null);
	}

	/**
	 * Several copies of a tool that isn't used up, e.g. bird snares or box traps.
	 */
	public static ItemReq tools(String name, int count, int... ids)
	{
		return new ItemReq(name, ids, count, 0, null, null);
	}

	/**
	 * An optional supply to bring, e.g. 10 food. {@code carry} 0 means the whole stack.
	 */
	public static ItemReq supply(String name, int carry, int... ids)
	{
		return new ItemReq(name, ids, carry, 0, null, null);
	}

	public static ItemReq consumed(String name, int carry, int perAction, int... ids)
	{
		return new ItemReq(name, ids, carry, perAction, null, null);
	}

	/**
	 * A stackable item (runes, arrows) used {@code perAction} at a time. The whole stack is taken each trip.
	 */
	public static ItemReq stack(String name, int perAction, int... ids)
	{
		return new ItemReq(name, ids, 0, perAction, null, null);
	}

	/**
	 * Copy of this requirement that is also satisfied by carrying or wearing one of {@code ids}.
	 */
	public ItemReq orUse(String name, int... ids)
	{
		return new ItemReq(this.name, this.ids, quantity, perAction, name, ids);
	}

	public boolean hasSubstitute()
	{
		return substituteIds != null && substituteIds.length > 0;
	}

	public boolean isWholeStack()
	{
		return quantity <= 0;
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
