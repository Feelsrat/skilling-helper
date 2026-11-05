package com.skillinghelper.minigame.data;

import lombok.Value;

/**
 * Where the game keeps a minigame's reward currency: a varp, a varbit, or an item the player holds (e.g. golden
 * nuggets). Several varps add up (Mastering Mixology's three resins are shown separately, see {@link #label}).
 */
@Value
public class Points
{
	String label;
	int varp;
	int varbit;
	/**
	 * The currency is an item, counted in the bank and inventory, or 0.
	 */
	int itemId;

	public static Points varp(String label, int varp)
	{
		return new Points(label, varp, 0, 0);
	}

	public static Points varbit(String label, int varbit)
	{
		return new Points(label, 0, varbit, 0);
	}

	public static Points item(String label, int itemId)
	{
		return new Points(label, 0, 0, itemId);
	}
}
