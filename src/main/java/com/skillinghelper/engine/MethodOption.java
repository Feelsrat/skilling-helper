package com.skillinghelper.engine;

import com.skillinghelper.data.Method;
import java.util.List;
import lombok.Value;

/**
 * A method evaluated for the player's current state: whether it's available, what it costs and how long it takes.
 */
@Value
public class MethodOption
{
	Method method;
	/**
	 * Reasons the method can't be used yet. Empty when available.
	 */
	List<String> blockers;
	/**
	 * Gold gained (positive) or spent (negative) per action, at current GE prices.
	 */
	double gpPerAction;
	/**
	 * Actions needed to reach the goal with this method.
	 */
	int actions;
	/**
	 * Gold the player still has to spend on inputs they don't own.
	 */
	long buyCost;
	boolean affordable;
	/**
	 * Fraction (0-1) of the needed inputs the player already owns.
	 */
	double ownedInputFraction;
	/**
	 * GP per action spent on inputs the player still has to buy.
	 */
	double buyCostPerAction;
	/**
	 * Xp per hour of play, from the guide's estimate.
	 */
	double xpPerHour;
	/**
	 * Xp per action at the player's level.
	 */
	double xpPerAction;
	double score;

	public boolean isAvailable()
	{
		return blockers.isEmpty();
	}

	public double hours(long xpNeeded)
	{
		return xpPerHour <= 0 ? 0 : (double) xpNeeded / xpPerHour;
	}

	/**
	 * For runs: how many runs the xp takes (one action per stop).
	 */
	public int runs(long xpNeeded)
	{
		int stops = Math.max(1, method.getStops().size());
		return xpPerAction <= 0 ? 0 : (int) Math.ceil(xpNeeded / xpPerAction / stops);
	}

	public boolean isRun()
	{
		return method.getCycleMinutes() > 0;
	}

	/**
	 * GP gained (positive) or spent (negative) per xp.
	 */
	public double gpPerXp()
	{
		return xpPerAction <= 0 ? 0 : gpPerAction / xpPerAction;
	}

	public long gpPerHour()
	{
		if (xpPerAction <= 0)
		{
			return 0;
		}
		return Math.round(gpPerAction * xpPerHour / xpPerAction);
	}
}
