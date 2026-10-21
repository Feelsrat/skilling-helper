package com.skillinghelper.data;

import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Wearable items that boost a skill and can't be bought on the GE: they're earned from an activity. If the player owns
 * any piece, the guide tells them to wear it; if not, it points at the method that earns it.
 * Each inner array is one slot; any id in it counts for that slot.
 */
@Getter
public enum SkillOutfit
{
	LUMBERJACK(Skill.WOODCUTTING, "Lumberjack outfit", "+2.5% Woodcutting xp with the full set", 0.025,
		"Forestry events while chopping (anima-infused bark)", null,
		new int[]{ItemID.RAMBLE_LUMBERJACK_HAT, ItemID.FORESTRY_LUMBERJACK_HAT},
		new int[]{ItemID.RAMBLE_LUMBERJACK_TOP, ItemID.FORESTRY_LUMBERJACK_TOP},
		new int[]{ItemID.RAMBLE_LUMBERJACK_LEGS, ItemID.FORESTRY_LUMBERJACK_LEGS},
		new int[]{ItemID.RAMBLE_LUMBERJACK_BOOTS, ItemID.FORESTRY_LUMBERJACK_BOOTS}),
	PROSPECTOR(Skill.MINING, "Prospector kit", "+2.5% Mining xp with the full set", 0.025,
		"Golden nuggets from the Motherlode Mine", "mine_motherlode",
		new int[]{ItemID.MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_HAT_GOLD},
		new int[]{ItemID.MOTHERLODE_REWARD_TOP, ItemID.MOTHERLODE_REWARD_TOP_GOLD},
		new int[]{ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_LEGS_GOLD},
		new int[]{ItemID.MOTHERLODE_REWARD_BOOTS, ItemID.MOTHERLODE_REWARD_BOOTS_GOLD}),
	ANGLER(Skill.FISHING, "Angler outfit", "+2.5% Fishing xp with the full set", 0.025,
		"Fishing Trawler minigame", "fish_trawler",
		new int[]{ItemID.TRAWLER_REWARD_HAT, ItemID.SPIRIT_ANGLER_HAT},
		new int[]{ItemID.TRAWLER_REWARD_TOP, ItemID.SPIRIT_ANGLER_TOP},
		new int[]{ItemID.TRAWLER_REWARD_LEGS, ItemID.SPIRIT_ANGLER_LEGS},
		new int[]{ItemID.TRAWLER_REWARD_BOOTS, ItemID.SPIRIT_ANGLER_BOOTS}),
	PYROMANCER(Skill.FIREMAKING, "Pyromancer outfit", "+2.5% Firemaking xp with the full set", 0.025,
		"Wintertodt supply crates", "fm_wintertodt",
		new int[]{ItemID.PYROMANCER_HOOD},
		new int[]{ItemID.PYROMANCER_TOP},
		new int[]{ItemID.PYROMANCER_BOTTOM},
		new int[]{ItemID.PYROMANCER_BOOTS},
		new int[]{ItemID.PYROMANCER_GLOVES}),
	COOKING_GAUNTLETS(Skill.COOKING, "Cooking gauntlets", "Burn fewer lobsters, swordfish, monkfish and sharks", 0,
		"Family Crest quest reward", null,
		new int[]{ItemID.GAUNTLETS_OF_COOKING}),
	FARMER(Skill.FARMING, "Farmer's outfit", "+2.5% Farming xp with the full set", 0.025,
		"Tithe Farm points", "farm_tithe",
		new int[]{ItemID.TITHE_REWARD_HAT_MALE, ItemID.TITHE_REWARD_HAT_FEMALE},
		new int[]{ItemID.TITHE_REWARD_TORSO_MALE, ItemID.TITHE_REWARD_TORSO_FEMALE},
		new int[]{ItemID.TITHE_REWARD_LEGS_MALE, ItemID.TITHE_REWARD_LEGS_FEMALE},
		new int[]{ItemID.TITHE_REWARD_FEET_MALE, ItemID.TITHE_REWARD_FEET_FEMALE}),
	CARPENTER(Skill.CONSTRUCTION, "Carpenter's outfit", "+2.5% Construction xp with the full set", 0.025,
		"Mahogany Homes points", "con_mahogany_homes",
		new int[]{ItemID.CONSTRUCTION_HAT},
		new int[]{ItemID.CONSTRUCTION_SHIRT},
		new int[]{ItemID.CONSTRUCTION_TROUSERS},
		new int[]{ItemID.CONSTRUCTION_BOOTS}),
	RAIMENTS_OF_THE_EYE(Skill.RUNECRAFT, "Raiments of the Eye", "Up to +60% runes crafted with the full set", 0,
		"Guardians of the Rift rewards", "rc_gotr",
		new int[]{ItemID.HAT_OF_THE_EYE, ItemID.HAT_OF_THE_EYE_RED},
		new int[]{ItemID.ROBE_TOP_OF_THE_EYE},
		new int[]{ItemID.ROBE_BOTTOM_OF_THE_EYE},
		new int[]{ItemID.BOOTS_OF_THE_EYE}),
	SMITHS_UNIFORM(Skill.SMITHING, "Smiths' uniform", "Smith faster at anvils (up to 20% of actions are instant)", 0.15,
		"Giants' Foundry reputation", "smith_giants_foundry",
		new int[]{ItemID.SMITHING_UNIFORM_TORSO},
		new int[]{ItemID.SMITHING_UNIFORM_LEGS},
		new int[]{ItemID.SMITHING_UNIFORM_BOOTS},
		new int[]{ItemID.SMITHING_UNIFORM_GLOVES, ItemID.SMITHING_UNIFORM_GLOVES_ICE}),
	ROGUE(Skill.THIEVING, "Rogue outfit", "Double loot from pickpocketing with the full set", 0,
		"Rogues' Den maze", "thieve_rogues_den",
		new int[]{ItemID.ROGUESDEN_HELM},
		new int[]{ItemID.ROGUESDEN_BODY},
		new int[]{ItemID.ROGUESDEN_LEGS},
		new int[]{ItemID.ROGUESDEN_GLOVES},
		new int[]{ItemID.ROGUESDEN_BOOTS});

	private final Skill skill;
	private final String name;
	private final String bonus;
	/**
	 * Extra xp (or speed) with the full set, e.g. 0.025 for +2.5%. 0 when the bonus isn't xp.
	 */
	private final double xpBonus;
	/**
	 * Where the outfit comes from.
	 */
	private final String source;
	/**
	 * Id of the guide method that earns it, or null if there's no matching method.
	 */
	private final String sourceMethodId;
	private final int[][] slots;

	SkillOutfit(Skill skill, String name, String bonus, double xpBonus, String source, String sourceMethodId,
		int[]... slots)
	{
		this.skill = skill;
		this.name = name;
		this.bonus = bonus;
		this.xpBonus = xpBonus;
		this.source = source;
		this.sourceMethodId = sourceMethodId;
		this.slots = slots;
	}
}
