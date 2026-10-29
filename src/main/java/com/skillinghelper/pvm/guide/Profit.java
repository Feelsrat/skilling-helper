package com.skillinghelper.pvm.guide;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Roughly how much it makes. The numbers are ballpark figures from the OSRS Wiki money making guides (from memory,
 * since the wiki can't be reached from where this was written) at the recommended stats and gear.
 */
@Value
public class Profit
{
	@Getter
	@RequiredArgsConstructor
	public enum Verdict
	{
		GOOD("Good money"),
		SOME("Some money"),
		LOW("Not much money"),
		NONE("Not for money: you do it for the reward");

		private final String label;
	}

	/**
	 * Average gp per hour once you're used to it, before supply costs; 0 when it isn't done for money.
	 */
	int gpPerHour;
	/**
	 * What the money (or the point) comes from.
	 */
	String note;

	public static Profit gp(int gpPerHour, String note)
	{
		return new Profit(gpPerHour, note);
	}

	public static Profit reward(String note)
	{
		return new Profit(0, note);
	}

	public Verdict verdict()
	{
		if (gpPerHour <= 0)
		{
			return Verdict.NONE;
		}
		if (gpPerHour >= 1_000_000)
		{
			return Verdict.GOOD;
		}
		return gpPerHour >= 300_000 ? Verdict.SOME : Verdict.LOW;
	}
}
