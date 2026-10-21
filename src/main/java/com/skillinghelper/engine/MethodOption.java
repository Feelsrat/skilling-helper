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
	double score;

	public boolean isAvailable()
	{
		return blockers.isEmpty();
	}

	public double hours(long xpNeeded)
	{
		return method.getXpPerHour() <= 0 ? 0 : (double) xpNeeded / method.getXpPerHour();
	}

	/**
	 * GP gained (positive) or spent (negative) per xp.
	 */
	public double gpPerXp()
	{
		return method.getXpPerAction() <= 0 ? 0 : gpPerAction / method.getXpPerAction();
	}

	public long gpPerHour()
	{
		if (method.getXpPerAction() <= 0)
		{
			return 0;
		}
		return Math.round(gpPerAction * method.getXpPerHour() / method.getXpPerAction());
	}
}
