package com.skillinghelper.data;

import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Wearable items that boost a skill. If the player owns any piece, the guide tells them to wear it.
 * Each inner array is one slot; any id in it counts for that slot.
 */
@Getter
public enum SkillOutfit
{
	LUMBERJACK(Skill.WOODCUTTING, "Lumberjack outfit", "+2.5% Woodcutting xp with the full set",
		new int[]{ItemID.RAMBLE_LUMBERJACK_HAT, ItemID.FORESTRY_LUMBERJACK_HAT},
		new int[]{ItemID.RAMBLE_LUMBERJACK_TOP, ItemID.FORESTRY_LUMBERJACK_TOP},
		new int[]{ItemID.RAMBLE_LUMBERJACK_LEGS, ItemID.FORESTRY_LUMBERJACK_LEGS},
		new int[]{ItemID.RAMBLE_LUMBERJACK_BOOTS, ItemID.FORESTRY_LUMBERJACK_BOOTS}),
	PROSPECTOR(Skill.MINING, "Prospector kit", "+2.5% Mining xp with the full set",
		new int[]{ItemID.MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_HAT_GOLD},
		new int[]{ItemID.MOTHERLODE_REWARD_TOP, ItemID.MOTHERLODE_REWARD_TOP_GOLD},
		new int[]{ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_LEGS_GOLD},
		new int[]{ItemID.MOTHERLODE_REWARD_BOOTS, ItemID.MOTHERLODE_REWARD_BOOTS_GOLD}),
	ANGLER(Skill.FISHING, "Angler outfit", "+2.5% Fishing xp with the full set",
		new int[]{ItemID.TRAWLER_REWARD_HAT, ItemID.SPIRIT_ANGLER_HAT},
		new int[]{ItemID.TRAWLER_REWARD_TOP, ItemID.SPIRIT_ANGLER_TOP},
		new int[]{ItemID.TRAWLER_REWARD_LEGS, ItemID.SPIRIT_ANGLER_LEGS},
		new int[]{ItemID.TRAWLER_REWARD_BOOTS, ItemID.SPIRIT_ANGLER_BOOTS}),
	PYROMANCER(Skill.FIREMAKING, "Pyromancer outfit", "+2.5% Firemaking xp with the full set",
		new int[]{ItemID.PYROMANCER_HOOD},
		new int[]{ItemID.PYROMANCER_TOP},
		new int[]{ItemID.PYROMANCER_BOTTOM},
		new int[]{ItemID.PYROMANCER_BOOTS},
		new int[]{ItemID.PYROMANCER_GLOVES}),
	COOKING_GAUNTLETS(Skill.COOKING, "Cooking gauntlets", "Burn fewer lobsters, swordfish, monkfish and sharks",
		new int[]{ItemID.GAUNTLETS_OF_COOKING});

	private final Skill skill;
	private final String name;
	private final String bonus;
	private final int[][] slots;

	SkillOutfit(Skill skill, String name, String bonus, int[]... slots)
	{
		this.skill = skill;
		this.name = name;
		this.bonus = bonus;
		this.slots = slots;
	}
}
