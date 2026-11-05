package com.skillinghelper.ca.data;

import lombok.Getter;

/**
 * The kinds of Combat Achievement task, roughly from least to most demanding.
 */
@Getter
public enum CaType
{
	KILL_COUNT("Kill Count", 0),
	MECHANICAL("Mechanical", 2),
	RESTRICTION("Restriction", 2),
	PERFECTION("Perfection", 4),
	STAMINA("Stamina", 5),
	SPEED("Speed", 5);

	private final String label;
	/**
	 * How much harder this kind is than a plain kill, used to rank tasks.
	 */
	private final int effort;

	CaType(String label, int effort)
	{
		this.label = label;
		this.effort = effort;
	}

	public static CaType fromLabel(String s)
	{
		for (CaType t : values())
		{
			if (t.label.equalsIgnoreCase(s.trim()))
			{
				return t;
			}
		}
		throw new IllegalArgumentException("Unknown Combat Achievement type: " + s);
	}
}
