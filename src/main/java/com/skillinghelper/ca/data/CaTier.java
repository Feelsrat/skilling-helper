package com.skillinghelper.ca.data;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * The six Combat Achievement tiers. Points from tasks of any tier count towards every tier's reward; each tier's
 * reward unlocks at a points total (read live from the game, with the wiki's numbers as a fallback).
 */
@Getter
public enum CaTier
{
	EASY("Easy", 1, VarbitID.CA_THRESHOLD_EASY, 41, ItemID.CA_OFFHAND_EASY, ItemID.CA_TELEPORT_ANIM_OFFHAND_EASY),
	MEDIUM("Medium", 2, VarbitID.CA_THRESHOLD_MEDIUM, 169, ItemID.CA_OFFHAND_MEDIUM,
		ItemID.CA_TELEPORT_ANIM_OFFHAND_MEDIUM),
	HARD("Hard", 3, VarbitID.CA_THRESHOLD_HARD, 436, ItemID.CA_OFFHAND_HARD, ItemID.CA_TELEPORT_ANIM_OFFHAND_HARD),
	ELITE("Elite", 4, VarbitID.CA_THRESHOLD_ELITE, 1100, ItemID.CA_OFFHAND_ELITE,
		ItemID.CA_TELEPORT_ANIM_OFFHAND_ELITE),
	MASTER("Master", 5, VarbitID.CA_THRESHOLD_MASTER, 1965, ItemID.CA_OFFHAND_MASTER,
		ItemID.CA_TELEPORT_ANIM_OFFHAND_MASTER, ItemID.CA_TELEPORT_ANIM_OFFHAND_MASTER_AVERNIC),
	GRANDMASTER("Grandmaster", 6, VarbitID.CA_THRESHOLD_GRANDMASTER, 2697, ItemID.CA_OFFHAND_GRANDMASTER,
		ItemID.CA_TELEPORT_ANIM_OFFHAND_GRANDMASTER, ItemID.CA_TELEPORT_ANIM_OFFHAND_GRANDMASTER_AVERNIC);

	private final String label;
	/**
	 * Points each task of this tier is worth.
	 */
	private final int points;
	/**
	 * Varbit holding the points needed for this tier's reward.
	 */
	private final int thresholdVarbit;
	/**
	 * Points needed per the wiki when the data was built, used until the game's own number is read.
	 */
	private final int fallbackThreshold;
	/**
	 * Ghommal's hilt for this tier, every version: owning one means the reward was claimed.
	 */
	private final int[] hiltIds;

	CaTier(String label, int points, int thresholdVarbit, int fallbackThreshold, int... hiltIds)
	{
		this.label = label;
		this.points = points;
		this.thresholdVarbit = thresholdVarbit;
		this.fallbackThreshold = fallbackThreshold;
		this.hiltIds = hiltIds;
	}

	public static CaTier fromLabel(String s)
	{
		for (CaTier t : values())
		{
			if (t.label.equalsIgnoreCase(s.trim()))
			{
				return t;
			}
		}
		throw new IllegalArgumentException("Unknown Combat Achievement tier: " + s);
	}
}
