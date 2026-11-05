package com.skillinghelper.collection.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The five tabs of the in-game collection log, in the game's order.
 */
@Getter
@RequiredArgsConstructor
public enum ClogTab
{
	BOSSES("Bosses"),
	RAIDS("Raids"),
	CLUES("Clues"),
	MINIGAMES("Minigames"),
	OTHER("Other");

	private final String label;

	static ClogTab of(String label)
	{
		for (ClogTab t : values())
		{
			if (t.label.equalsIgnoreCase(label))
			{
				return t;
			}
		}
		throw new IllegalArgumentException("Unknown collection log tab " + label);
	}

	@Override
	public String toString()
	{
		return label;
	}
}
