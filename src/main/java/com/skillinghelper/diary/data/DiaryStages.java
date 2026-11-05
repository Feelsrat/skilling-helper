package com.skillinghelper.diary.data;

import com.skillinghelper.data.Location;
import java.util.List;
import net.runelite.api.gameval.ItemID;

/**
 * Task parts that come up in several diaries, written once.
 */
final class DiaryStages
{
	static final int[] ESSENCE = {ItemID.BLANKRUNE_DAEYALT, ItemID.BLANKRUNE_HIGH, ItemID.BLANKRUNE};
	static final int[] PURE_ESSENCE = {ItemID.BLANKRUNE_DAEYALT, ItemID.BLANKRUNE_HIGH};
	static final int[] LIGHT = {ItemID.BULLSEYE_LANTERN_LIT, ItemID.OIL_LANTERN_LIT, ItemID.OIL_LAMP_LIT,
		ItemID.CANDLE_LANTERN_LIT, ItemID.CAVE_GOBLIN_MINING_HELMET_LIT, ItemID.TORCH_LIT, ItemID.LIT_CANDLE,
		ItemID.UNLIT_CANDLE};
	static final int[] CROSSBOW = {ItemID.ACB, ItemID.XBOWS_CROSSBOW_DRAGON, ItemID.XBOWS_CROSSBOW_RUNITE,
		ItemID.XBOWS_CROSSBOW_ADAMANTITE, ItemID.XBOWS_CROSSBOW_MITHRIL, ItemID.XBOWS_CROSSBOW_STEEL,
		ItemID.XBOWS_CROSSBOW_IRON, ItemID.XBOWS_CROSSBOW_BRONZE, ItemID.CROSSBOW};
	/**
	 * Shields that stop dragonfire, best first; the anti-dragon shield is free from Duke Horacio in Lumbridge.
	 */
	static final int[] DRAGONFIRE_SHIELDS = {ItemID.DRAGONFIRE_SHIELD, ItemID.DRAGONFIRE_WARD,
		ItemID.DRAGONFIRE_SHIELD_UNCHARGED, ItemID.DRAGONFIRE_WARD_UNCHARGED, ItemID.ANTIDRAGONBREATHSHIELD};
	static final int[] SKILLCAPES = {ItemID.SKILLCAPE_QP_TRIMMED, ItemID.SKILLCAPE_QP,
		ItemID.SKILLCAPE_ATTACK_TRIMMED, ItemID.SKILLCAPE_ATTACK, ItemID.SKILLCAPE_STRENGTH_TRIMMED,
		ItemID.SKILLCAPE_STRENGTH, ItemID.SKILLCAPE_DEFENCE_TRIMMED, ItemID.SKILLCAPE_DEFENCE,
		ItemID.SKILLCAPE_RANGING_TRIMMED, ItemID.SKILLCAPE_RANGING, ItemID.SKILLCAPE_PRAYER_TRIMMED,
		ItemID.SKILLCAPE_PRAYER, ItemID.SKILLCAPE_MAGIC_TRIMMED, ItemID.SKILLCAPE_MAGIC,
		ItemID.SKILLCAPE_RUNECRAFTING_TRIMMED, ItemID.SKILLCAPE_RUNECRAFTING, ItemID.SKILLCAPE_HITPOINTS_TRIMMED,
		ItemID.SKILLCAPE_HITPOINTS, ItemID.SKILLCAPE_AGILITY_TRIMMED, ItemID.SKILLCAPE_AGILITY,
		ItemID.SKILLCAPE_HERBLORE_TRIMMED, ItemID.SKILLCAPE_HERBLORE, ItemID.SKILLCAPE_THIEVING_TRIMMED,
		ItemID.SKILLCAPE_THIEVING, ItemID.SKILLCAPE_CRAFTING_TRIMMED, ItemID.SKILLCAPE_CRAFTING,
		ItemID.SKILLCAPE_FLETCHING_TRIMMED, ItemID.SKILLCAPE_FLETCHING, ItemID.SKILLCAPE_SLAYER_TRIMMED,
		ItemID.SKILLCAPE_SLAYER, ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED, ItemID.SKILLCAPE_CONSTRUCTION,
		ItemID.SKILLCAPE_MINING_TRIMMED, ItemID.SKILLCAPE_MINING, ItemID.SKILLCAPE_SMITHING_TRIMMED,
		ItemID.SKILLCAPE_SMITHING, ItemID.SKILLCAPE_FISHING_TRIMMED, ItemID.SKILLCAPE_FISHING,
		ItemID.SKILLCAPE_COOKING_TRIMMED, ItemID.SKILLCAPE_COOKING, ItemID.SKILLCAPE_FIREMAKING_TRIMMED,
		ItemID.SKILLCAPE_FIREMAKING, ItemID.SKILLCAPE_WOODCUTTING_TRIMMED, ItemID.SKILLCAPE_WOODCUTTING,
		ItemID.SKILLCAPE_FARMING_TRIMMED, ItemID.SKILLCAPE_FARMING, ItemID.SKILLCAPE_HUNTING_TRIMMED,
		ItemID.SKILLCAPE_HUNTING, ItemID.SKILLCAPE_SAILING_TRIMMED, ItemID.SKILLCAPE_SAILING,
		ItemID.SKILLCAPE_AD_TRIMMED, ItemID.SKILLCAPE_AD};

	static final DiaryItem MITH_GRAPPLE = DiaryItem.of("Mith grapple", ItemID.XBOWS_GRAPPLE_TIP_BOLT_MITHRIL_ROPE);
	static final DiaryItem ANY_CROSSBOW = DiaryItem.anyOf("Any crossbow", 1, ItemID.XBOWS_CROSSBOW_BRONZE, CROSSBOW);
	static final DiaryItem LIGHT_SOURCE = DiaryItem.anyOf("Light source (a candle is fine)", 1, ItemID.UNLIT_CANDLE,
		LIGHT);
	static final DiaryItem DRAGONFIRE_SHIELD = DiaryItem.owned("Anti-dragon shield",
		"Ask Duke Horacio on the first floor of Lumbridge Castle for one (free once you've started Dragon Slayer I).",
		DRAGONFIRE_SHIELDS);
	static final DiaryItem SKILLCAPE = DiaryItem.owned("A skillcape or quest cape",
		"Buy a skillcape from its master once you have 99 in that skill, or the Quest point cape from the Wise "
			+ "Old Man in Draynor once every quest is done.", SKILLCAPES);

	private DiaryStages()
	{
	}

	/**
	 * Enter a rune altar through its ruins, then craft.
	 *
	 * @param where    how to find the ruins, e.g. "north-east of Varrock"
	 * @param ruins    the ruins outside
	 * @param altar    the altar room
	 * @param talisman the talisman to use on the ruins
	 */
	static List<DiaryStage> runeAltar(String where, Location ruins, Zone altar, String title, int talisman, String extra)
	{
		DiaryStage.DiaryStageBuilder inside = DiaryStage.builder()
			.title(title)
			.inside(altar)
			.object("Altar")
			.line("1. Click Craft-rune on the Altar (outlined) with the essence in your inventory.");
		if (extra != null)
		{
			inside.line(extra);
		}
		inside.line("The portal behind you takes you back out.");
		return List.of(
			DiaryStage.builder()
				.title("Enter the altar")
				.at(ruins)
				.object("Mysterious ruins")
				.use(talisman).use(ItemID.ELEMENTAL_TALISMAN)
				.line("1. The ruins are " + where + ".")
				.line("2. Wear the tiara and click Enter on the Mysterious ruins (outlined),")
				.line("3. or use the talisman (outlined) on the ruins.")
				.build(),
			inside.build());
	}

	/**
	 * Climb down the Edgeville trapdoor into the Edgeville (Varrock) dungeon.
	 */
	static DiaryStage edgevilleTrapdoor()
	{
		return DiaryStage.builder()
			.title("Climb down into Edgeville Dungeon")
			.at(Location.of("Trapdoor in Edgeville", 3097, 3468, 0, 2))
			.object("Trapdoor")
			.line("1. The trapdoor is in the ruined building north of Edgeville bank, west of the church.")
			.line("2. Click Open on the Trapdoor (outlined), then Climb-down.")
			.line("Warning: there are aggressive monsters down there at low levels; bring food.")
			.build();
	}

	static final Zone EDGEVILLE_DUNGEON = Zone.of(3067, 9821, 3288, 10000, 0);

	/**
	 * Stronghold of Security: the floors, top to bottom.
	 */
	static final Zone STRONGHOLD_1 = Zone.of(1854, 5183, 1918, 5248, 0);
	static final Zone STRONGHOLD_2 = Zone.of(1983, 5183, 2049, 5248, 0);
	static final Zone STRONGHOLD_3 = Zone.of(2113, 5246, 2177, 5313, 0);
	static final Zone STRONGHOLD_4 = Zone.of(2302, 5183, 2369, 5250, 0);

	static DiaryStage strongholdEntrance()
	{
		return DiaryStage.builder()
			.title("Climb into the Stronghold of Security")
			.at(Location.of("Stronghold of Security, Barbarian Village", 3081, 3420, 0, 2))
			.object("Entrance")
			.line("1. The entrance is a hole in the middle of Barbarian Village, west of Varrock.")
			.line("2. Click Climb-down on the Entrance (outlined).")
			.line("Inside, every door asks a security question. The safe answer is always the one that keeps your "
				+ "password and bank PIN secret and doesn't trust anyone asking for them.")
			.build();
	}

	static final DiaryItem FAIRY_STAFF = DiaryItem.owned("Dramen or Lunar staff",
		"Make a Dramen staff during Lost City: cut a branch from the Dramen tree on Entrana and use a knife on it.",
		ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF, ItemID.DRAMEN_STAFF);
	static final Zone ZANARIS = Zone.of(2369, 4352, 2497, 4481, 0);

	/**
	 * Travel by fairy ring: in through the Lumbridge Swamp shed to Zanaris, then dial the code. Any other fairy ring
	 * works the same way.
	 */
	static List<DiaryStage> fairyRing(String code, String arrival)
	{
		String dials = String.join(", ", code.split(""));
		return List.of(
			DiaryStage.builder()
				.title("Go into Zanaris through the swamp shed")
				.at(Location.of("Shed in Lumbridge Swamp", 3202, 3169, 0, 2))
				.object("Door")
				.use(ItemID.DRAMEN_STAFF).use(ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF)
				.line("1. Wield your Dramen or Lunar staff (outlined in your inventory).")
				.line("2. Walk to the small shed in Lumbridge Swamp, south of the castle, open the Door (outlined) and walk in.")
				.line("Already next to another fairy ring? Use that one instead with code " + code + ".")
				.build(),
			DiaryStage.builder()
				.title("Take the fairy ring to code " + code)
				.inside(ZANARIS)
				.at(Location.of("Fairy ring in Zanaris", 2412, 4434, 0, 3))
				.object("Fairy ring")
				.line("1. Wield your Dramen or Lunar staff.")
				.line("2. Click Configure on the Fairy ring (outlined).")
				.line("3. Turn the dials to " + dials + " and click Teleport. " + arrival)
				.build());
	}
}
