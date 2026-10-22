package com.skillinghelper.engine;

/**
 * Short, readable numbers for the panel and step text.
 */
public final class Format
{
	private Format()
	{
	}

	/**
	 * Gold as "1,234 gp", "250K gp" or "12M gp".
	 */
	public static String gp(long amount)
	{
		if (amount >= 10_000_000)
		{
			return (amount / 1_000_000) + "M gp";
		}
		if (amount >= 100_000)
		{
			return (amount / 1_000) + "K gp";
		}
		return String.format("%,d gp", amount);
	}

	/**
	 * A duration as "45m" or "3h 05m".
	 */
	public static String hours(double hours)
	{
		long minutes = Math.round(hours * 60);
		if (minutes < 60)
		{
			return Math.max(1, minutes) + "m";
		}
		return String.format("%dh %02dm", minutes / 60, minutes % 60);
	}
}
