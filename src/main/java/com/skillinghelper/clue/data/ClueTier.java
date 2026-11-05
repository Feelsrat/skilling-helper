package com.skillinghelper.clue.data;

import java.util.List;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * The six clue scroll tiers, with what the wiki says about each: how long a trail is, what it can ask of you, its
 * STASH units and its rewards.
 */
@Getter
public enum ClueTier
{
	BEGINNER("Beginner", "1 to 3 steps", false, false, ItemID.TRAIL_REWARD_CASKET_BEGINNER,
		12, "Plank", ItemID.WOODPLANK, 0, null,
		"Explore emote after 600 beginner clues",
		List.of("Anagrams, ciphers, maps, hot-and-cold and emote steps, all in free-to-play Misthalin and Asgarnia.",
			"No combat and no Wilderness."),
		List.of("Silly cosmetics: mole and frog slippers, bear and demon feet, Jester cape, Shoulder parrot.",
			"Monk's robes (t), Amulet of defence (t), the Sandwich lady outfit and Rune scimitar ornament kits.",
			"Black armour and weapons, and a rare Black pickaxe.",
			"Common: runes, arrows, food and low-level gear.",
			"Accounts under 10 hours old get no uniques.")),
	EASY("Easy", "2 to 4 steps", true, false, ItemID.TRAIL_REWARD_CASKET_EASY,
		27, "Plank", ItemID.WOODPLANK, 0, "1/50",
		"Large spade after 500 easy clues",
		List.of("Cryptic, emote, map and music steps. No coordinates, no combat and no Wilderness."),
		List.of("Trimmed (t) and gold-trimmed (g) bronze, iron, steel and black armour, heraldic black armour.",
			"Studded, leather and wizard robes (t)/(g), berets, highwayman mask, Bob shirts, team capes, god book pages.",
			"Willow comp bow, Amulet of magic (t), golden chef's hat and apron.",
			"Shared rewards (all tiers): teleport scrolls, firelighters, god blessings, purple sweets, Master scroll book.",
			"Common: runes, food and low-level gear.")),
	MEDIUM("Medium", "3 to 5 steps", true, false, ItemID.TRAIL_REWARD_CASKET_MEDIUM,
		42, "Oak plank", ItemID.PLANK_OAK, 0, "1/30",
		"Clueless scroll after 400 medium clues",
		List.of("Adds coordinate steps (dig where the Clue Scroll plugin points), puzzle boxes and light boxes.",
			"Some steps need you to kill a monster first (from a chicken up to a Market Guard). No Wilderness."),
		List.of("Ranger boots, wizard boots, holy sandals, climbing boots (g) and spiked manacles.",
			"Trimmed and gold-trimmed mithril and adamant, green d'hide (t)/(g), god mitres, cloaks, stoles and croziers.",
			"Boaters, headbands, elegant clothes, Yew comp bow and Strength amulet (t).",
			"Shared rewards (all tiers) and common runes, food and mid-level gear.")),
	HARD("Hard", "4 to 6 steps", true, true, ItemID.TRAIL_REWARD_CASKET_HARD,
		55, "Teak plank", ItemID.PLANK_TEAK, 0, "1/15",
		"Uri transform emote after 300 hard clues",
		List.of("Harder puzzles and emote steps, and some steps are in the Wilderness.",
			"Some steps make you kill a Zamorak or Saradomin wizard (or the Double agent) first: bring weapon, armour and food."),
		List.of("Rune (t)/(g) and heraldic rune armour, god platebody sets (Zamorak, Saradomin, Guthix, Ancient, Armadyl, Bandos).",
			"God d'hide sets, red and blue d'hide (t)/(g), Robin Hood hat, enchanted robes, Magic comp bow, Amulet of glory (t4).",
			"Dragon boots ornament kit and pirate hats.",
			"Mega-rare: 3rd age pieces, gilded armour.",
			"Shared rewards (all tiers) and common runes, food and rune gear.")),
	ELITE("Elite", "5 to 7 steps", true, true, ItemID.TRAIL_REWARD_CASKET_ELITE,
		77, "Mahogany plank", ItemID.PLANK_MAHOGANY, 0, "1/5",
		"Heavy casket after 200 elite clues",
		List.of("Long trails all over the world, some in the Wilderness.",
			"Some steps make you kill an Armadylean or Bandosian guard first: bring weapon, armour and food.",
			"A finished elite casket can turn into the Mimic (1/35) once you've switched that on at Watson's house."),
		List.of("Dragon and Fury ornament kits, infinity colour kits, royal outfit, musketeer outfit and dragon masks.",
			"Rangers' tunic and tights, Ranger gloves, Holy wraps, black d'hide (t)/(g), Katana, Dragon cane, Kourend scarves.",
			"Fun items like the Ring of nature, Giant boot, Afro and Top hat.",
			"Mega-rare: 3rd age pieces and gilded armour.",
			"Shared rewards (all tiers) and common runes, food, potions and resources.")),
	MASTER("Master", "6 to 8 steps", true, true, ItemID.TRAIL_REWARD_CASKET_MASTER,
		88, "Mahogany plank", ItemID.PLANK_MAHOGANY, 1, null,
		"Scroll sack after 100 master clues",
		List.of("The longest trails, with Wilderness steps and fights against Brassican Mages, the Ancient trio or the Double agent.",
			"A finished master casket can turn into the Mimic (1/15) once you've switched that on at Watson's house."),
		List.of("Occult, Torture, Anguish, Tormented, godsword and dragon ornament kits.",
			"Robes of darkness, samurai and Ankou outfits, mummy outfit, demon masks and Kourend hoods.",
			"Mega-rare: 3rd age weapons and armour, gilded items, Bucket helm (g), Ring of coins.",
			"1/1,000: the Bloodhound pet.",
			"Shared rewards (all tiers) and common runes, food and resources."));

	private final String label;
	private final String steps;
	private final boolean members;
	/**
	 * Some steps of this tier can be in the Wilderness.
	 */
	private final boolean wildernessSteps;
	private final int rewardCasket;
	private final int stashLevel;
	private final String plankName;
	private final int plankId;
	/**
	 * Gold leaf per STASH unit (master only).
	 */
	private final int goldLeaf;
	/**
	 * Chance a casket of this tier has a master clue in it, or null.
	 */
	private final String masterChance;
	private final String milestone;
	private final List<String> whatToExpect;
	private final List<String> rewardKinds;

	ClueTier(String label, String steps, boolean members, boolean wildernessSteps, int rewardCasket, int stashLevel,
		String plankName, int plankId, int goldLeaf, String masterChance, String milestone, List<String> whatToExpect,
		List<String> rewardKinds)
	{
		this.label = label;
		this.steps = steps;
		this.members = members;
		this.wildernessSteps = wildernessSteps;
		this.rewardCasket = rewardCasket;
		this.stashLevel = stashLevel;
		this.plankName = plankName;
		this.plankId = plankId;
		this.goldLeaf = goldLeaf;
		this.masterChance = masterChance;
		this.milestone = milestone;
		this.whatToExpect = whatToExpect;
		this.rewardKinds = rewardKinds;
	}

	/**
	 * The word the game puts in item names, e.g. "easy" in "Clue scroll (easy)".
	 */
	public String key()
	{
		return name().toLowerCase();
	}

	public String getWikiPage()
	{
		return "Clue_scroll_(" + key() + ")";
	}

	public static ClueTier fromKey(String key)
	{
		for (ClueTier t : values())
		{
			if (t.key().equals(key))
			{
				return t;
			}
		}
		return null;
	}
}
