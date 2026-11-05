package com.skillinghelper;

import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar to open the Combat Achievement Guides
 * panel on one boss's tasks ({@link #boss}) or on one tier ({@link #tier}, which also starts its guide).
 */
@Value
public class OpenCombatAchievementsRequest
{
	/**
	 * The boss as the OSRS Wiki's Combat Achievements list names it, e.g. "Giant Mole", "Kree'arra",
	 * "Theatre of Blood: Hard Mode". Case doesn't matter. Null when opening a tier.
	 */
	String monster;
	/**
	 * A tier name ("EASY" ... "GRANDMASTER"), or null when opening a boss.
	 */
	String tier;

	public static OpenCombatAchievementsRequest boss(String monster)
	{
		return new OpenCombatAchievementsRequest(monster, null);
	}

	public static OpenCombatAchievementsRequest tier(String tier)
	{
		return new OpenCombatAchievementsRequest(null, tier);
	}
}
