package com.skillinghelper.pvm.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Rough stage of a player's PvM journey, used to group the ladder in the panel.
 */
@Getter
@RequiredArgsConstructor
public enum Tier
{
	FIRST_BOSSES("Getting started"),
	EARLY("Early game"),
	MID("Mid game"),
	LATE("Late game"),
	RAIDS("Raids"),
	ENDGAME("Endgame challenges");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
