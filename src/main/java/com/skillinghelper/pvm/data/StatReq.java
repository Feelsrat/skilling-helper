package com.skillinghelper.pvm.data;

import lombok.Value;
import net.runelite.api.Skill;

/**
 * A level for a piece of content: either needed to get in at all, or the level most players are comfortable at.
 */
@Value
public class StatReq
{
	Skill skill;
	int level;
	/**
	 * True when the content can't be done without it (e.g. a Slayer level), false when it's a recommendation.
	 */
	boolean required;

	public static StatReq rec(Skill skill, int level)
	{
		return new StatReq(skill, level, false);
	}

	public static StatReq need(Skill skill, int level)
	{
		return new StatReq(skill, level, true);
	}
}
