package com.skillinghelper.minigame.data;

import lombok.Value;

/**
 * Something a minigame gives: a reward item (any of {@link #ids} counts as owned), an unlock read from a varbit, or
 * something that can't be owned (xp, supplies), and why it's worth it.
 */
@Value
public class Reward
{
	String name;
	/**
	 * Why you'd want it, short.
	 */
	String why;
	/**
	 * Worth working for (shown first, with an owned check); otherwise listed under "Also".
	 */
	boolean worth;
	/**
	 * What it costs, e.g. "750 points", or null.
	 */
	String cost;
	/**
	 * The cost in the minigame's {@link Points}, so the panel can say when it's affordable, or 0.
	 */
	int price;
	/**
	 * Item ids that count as having it (outfit pieces list each piece as its own reward), or empty.
	 */
	int[] ids;
	/**
	 * An unlock: owned once this varbit is above 0, or 0.
	 */
	int unlockVarbit;

	public static Reward item(String name, String why, String cost, int price, int... ids)
	{
		return new Reward(name, why, true, cost, price, ids, 0);
	}

	public static Reward unlock(String name, String why, String cost, int price, int varbit)
	{
		return new Reward(name, why, true, cost, price, new int[0], varbit);
	}

	/**
	 * Worth having but can't be owned or checked (xp, supplies, a drop table).
	 */
	public static Reward other(String name, String why)
	{
		return new Reward(name, why, true, null, 0, new int[0], 0);
	}

	/**
	 * Nice to know about, not worth chasing.
	 */
	public static Reward also(String name, String why, int... ids)
	{
		return new Reward(name, why, false, null, 0, ids, 0);
	}

	public boolean checkable()
	{
		return ids.length > 0 || unlockVarbit > 0;
	}
}
