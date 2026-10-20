package com.skillinghelper.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * How the player wants to train. Drives which method the planner recommends.
 */
@Getter
@RequiredArgsConstructor
public enum TrainingPreference
{
	RECOMMENDED("Recommended", "A balance of xp/hr, effort and cost"),
	FAST("Fastest", "Highest xp/hr, ignores effort"),
	AFK("AFK", "Fewest clicks per hour"),
	CHEAP("Cheapest", "Lowest GP cost, prefers items you already own"),
	PROFIT("Profit", "Makes money while training");

	private final String label;
	private final String description;

	@Override
	public String toString()
	{
		return label;
	}
}
