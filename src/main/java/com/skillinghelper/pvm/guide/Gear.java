package com.skillinghelper.pvm.guide;

import lombok.Value;
import net.runelite.api.Skill;

/**
 * One piece of gear a loadout slot can use. Pieces that need a quest or a minigame to wear or get are never
 * suggested as a buy, only used when the player already owns them.
 */
@Value
public class Gear
{
	/**
	 * What a ranged weapon fires, so the loadout can add the right ammo.
	 */
	public enum Ammo
	{
		NONE,
		ARROWS,
		BOLTS,
		DARTS
	}

	String name;
	Skill skill;
	int level;
	/**
	 * Defence level needed on top of {@link #level}, or 0.
	 */
	int defence;
	boolean members;
	boolean buyable;
	Ammo ammo;
	/**
	 * Any of these counts as owning it, e.g. every charge of an amulet of glory. The first is the one to buy.
	 */
	int[] ids;

	public static Gear buy(String name, Skill skill, int level, int defence, boolean members, int... ids)
	{
		return new Gear(name, skill, level, defence, members, true, Ammo.NONE, ids);
	}

	/**
	 * Gear you have to earn (quest, minigame, boss drop that can't be bought for a sensible price).
	 */
	public static Gear earn(String name, Skill skill, int level, int defence, boolean members, int... ids)
	{
		return new Gear(name, skill, level, defence, members, false, Ammo.NONE, ids);
	}

	public Gear firing(Ammo ammo)
	{
		return new Gear(name, skill, level, defence, members, buyable, ammo, ids);
	}

	public int getBuyId()
	{
		return ids[0];
	}
}
