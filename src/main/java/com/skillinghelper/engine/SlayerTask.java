package com.skillinghelper.engine;

import lombok.Builder;
import lombok.Value;

/**
 * The player's current Slayer assignment, read from the game's Slayer varps.
 */
@Value
@Builder(toBuilder = true)
public class SlayerTask
{
	/**
	 * The master id (SLAYER_MASTER varbit) for Krystilia, whose tasks only count in the Wilderness.
	 */
	public static final int KRYSTILIA = 7;

	/**
	 * The game's task id (SLAYER_TARGET varp).
	 */
	int taskId;
	/**
	 * The task's name as the game shows it, e.g. "Aberrant spectres".
	 */
	String name;
	/**
	 * Kills left.
	 */
	int remaining;
	/**
	 * Konar's area for the task (SLAYER_AREA varp), or 0 when the task can be done anywhere.
	 */
	int areaId;
	/**
	 * The name of Konar's area, or null.
	 */
	String areaName;
	/**
	 * Who gave the task (SLAYER_MASTER varbit).
	 */
	int masterId;
	int points;
	/**
	 * Some of the task's monsters are close by, so the player has found them even if the guide's spots don't cover
	 * where they're standing.
	 */
	boolean monstersNearby;

	public boolean isWilderness()
	{
		return masterId == KRYSTILIA;
	}
}
