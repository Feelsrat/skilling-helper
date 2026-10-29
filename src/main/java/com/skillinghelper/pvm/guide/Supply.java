package com.skillinghelper.pvm.guide;

import java.util.List;
import lombok.Value;
import lombok.With;

/**
 * Something carried in the inventory, with exactly how many to bring.
 */
@Value
@With
public class Supply
{
	/**
	 * Fill whatever inventory space the rest of the loadout leaves.
	 */
	public static final int FILL = -1;

	/**
	 * One kind of item that counts for this supply, e.g. sharks for food. Several ids for potion doses or charges.
	 */
	@Value
	public static class Option
	{
		String name;
		int[] ids;

		public static Option of(String name, int... ids)
		{
			return new Option(name, ids);
		}
	}

	String name;
	/**
	 * What counts, best first. The first option is the one to buy when short.
	 */
	List<Option> options;
	/**
	 * How many to leave the bank with, or {@link #FILL}.
	 */
	int amount;
	/**
	 * Go back to the bank once fewer than this are carried.
	 */
	int restockBelow;
	/**
	 * Stacks in one inventory slot (runes, ammo, teleport tablets).
	 */
	boolean stacks;
	/**
	 * Can't do the content without it.
	 */
	boolean required;
	/**
	 * Can be bought on the Grand Exchange.
	 */
	boolean buyable;
	/**
	 * Used up on the trip, so it counts towards what a trip costs. False for tools like a spade.
	 */
	boolean consumed;
	/**
	 * Why it's brought, shown next to it.
	 */
	String note;

	/**
	 * A consumable such as food or potions, restocked once fewer than a third are left.
	 */
	public static Supply of(String name, int amount, Option... options)
	{
		return new Supply(name, List.of(options), amount, Math.max(1, amount / 3), false, false, true, true, null);
	}

	/**
	 * A stackable consumable such as runes, ammo or teleports.
	 */
	public static Supply stack(String name, int amount, Option... options)
	{
		return new Supply(name, List.of(options), amount, 1, true, false, true, true, null);
	}

	/**
	 * A tool that isn't used up, such as a spade. Required.
	 */
	public static Supply tool(String name, Option... options)
	{
		return new Supply(name, List.of(options), 1, 1, false, true, true, false, null);
	}

	public Supply note(String note)
	{
		return withNote(note);
	}

	public Supply required()
	{
		return withRequired(true);
	}

	public Supply amount(int amount)
	{
		return withAmount(amount).withRestockBelow(amount == FILL ? restockBelow : Math.max(1, Math.min(restockBelow,
			amount)));
	}

	public int slots(int count)
	{
		if (count <= 0)
		{
			return 0;
		}
		return stacks ? 1 : count;
	}

	public int[] allIds()
	{
		return options.stream().flatMapToInt(o -> java.util.Arrays.stream(o.getIds())).toArray();
	}
}
