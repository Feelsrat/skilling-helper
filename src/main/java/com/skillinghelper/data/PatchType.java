package com.skillinghelper.data;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * Farming patch types that guides run through, and how to read a patch's varbit. Growing value ranges come from
 * RuneLite's Time Tracking plugin (PatchImplementation); every other value (weeds, empty, ready, diseased, dead,
 * waiting for a health check) means the patch needs a visit.
 */
@Getter
public enum PatchType
{
	HERB("Herbs", ItemID.RANARR_SEED, 20, new int[][]{
		{4, 7}, {11, 14}, {18, 21}, {25, 28}, {32, 35}, {39, 42}, {46, 49}, {53, 56}, {60, 63}, {68, 71}, {75, 78},
		{82, 85}, {89, 92}, {96, 99}, {103, 106}, {192, 195}}),
	ALLOTMENT("Allotments", ItemID.POTATO_SEED, 10, new int[][]{
		{6, 9}, {13, 16}, {20, 23}, {27, 30}, {34, 39}, {43, 48}, {52, 59}, {63, 69},
		// Watered crops
		{70, 73}, {77, 80}, {84, 87}, {91, 94}, {98, 103}, {107, 112}, {116, 123}, {128, 134}}),
	TREE("Trees", ItemID.PLANTPOT_OAK_SAPLING, 40, new int[][]{
		// The value after each range is "check health"
		{8, 11}, {15, 20}, {24, 31}, {35, 44}, {48, 59}});

	/**
	 * Name for timers and notifications, e.g. "Herbs".
	 */
	private final String label;
	private final int icon;
	/**
	 * Minutes per growth stage.
	 */
	private final int stageMinutes;
	private final int[][] growing;

	PatchType(String label, int icon, int stageMinutes, int[][] growing)
	{
		this.label = label;
		this.icon = icon;
		this.stageMinutes = stageMinutes;
		this.growing = growing;
	}

	/**
	 * Roughly how long until a patch with this varbit value needs a visit, or 0 if it needs one now.
	 */
	public int minutesLeft(int value)
	{
		for (int[] range : growing)
		{
			if (value >= range[0] && value <= range[1])
			{
				return (range[1] - value + 1) * stageMinutes;
			}
		}
		return 0;
	}
}
