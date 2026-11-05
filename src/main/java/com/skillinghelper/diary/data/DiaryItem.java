package com.skillinghelper.diary.data;

import com.skillinghelper.data.ToolType;
import lombok.Value;

/**
 * Something to bring for a task. Any of {@link #ids} will do, best first: the guide picks the best one the player
 * already owns, and only buys {@link #buyId} when they own none.
 */
@Value
public class DiaryItem
{
	String name;
	int amount;
	int[] ids;
	/**
	 * What to buy when none is owned, or 0 when it can't be bought (see {@link #howToGet}).
	 */
	int buyId;
	/**
	 * How to get it when it can't be bought, or null.
	 */
	String howToGet;
	/**
	 * A tool family (axe, pickaxe): any tier the player has the level for counts, best owned first.
	 */
	ToolType tool;
	/**
	 * Not needed any more once this varbit is above 0 (e.g. a rope already tied to a cave entrance), or 0.
	 */
	int skipVarbit;

	public static DiaryItem of(String name, int amount, int id)
	{
		return new DiaryItem(name, amount, new int[]{id}, id, null, null, 0);
	}

	public static DiaryItem of(String name, int id)
	{
		return of(name, 1, id);
	}

	/**
	 * Any of these will do, best first; buys {@code buyId} if none is owned.
	 */
	public static DiaryItem anyOf(String name, int amount, int buyId, int... ids)
	{
		return new DiaryItem(name, amount, ids, buyId, null, null, 0);
	}

	/**
	 * Can't be bought: the player has to own one already, and {@code howToGet} says how.
	 */
	public static DiaryItem owned(String name, String howToGet, int... ids)
	{
		return new DiaryItem(name, 1, ids, 0, howToGet, null, 0);
	}

	public static DiaryItem tool(ToolType tool)
	{
		return new DiaryItem("Any " + tool.getNoun(), 1, tool.allIds(), 0, null, tool, 0);
	}

	public DiaryItem skipWhenVarbit(int varbit)
	{
		return new DiaryItem(name, amount, ids, buyId, howToGet, tool, varbit);
	}
}
