package com.skillinghelper.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.With;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Weapon and armour tiers per equipment slot, best first. Used to suggest the best gear the player owns (to wear) or
 * could buy on the GE. Gear that needs a quest or minigame is marked {@link Tier#isBuyable() earned}: it is used when
 * the player owns it but never suggested as a buy. Pieces that cost more than a few million are marked
 * {@link Tier#isPricey() pricey} and only suggested when the player has the coins.
 */
@Getter
public enum CombatGear
{
	MELEE_WEAPON(CombatStyle.MELEE, "weapon",
		tier("Osmumten's fang", Skill.ATTACK, 82, 0, true, ItemID.OSMUMTENS_FANG).pricey(),
		tier("Ghrazi rapier", Skill.ATTACK, 80, 0, true, ItemID.GHRAZI_RAPIER).pricey(),
		tier("Abyssal tentacle", Skill.ATTACK, 75, 0, true, ItemID.ABYSSAL_TENTACLE),
		tier("Abyssal whip", Skill.ATTACK, 70, 0, true, ItemID.ABYSSAL_WHIP),
		// Monkey Madness I; only used when owned
		tier("Dragon scimitar", Skill.ATTACK, 60, 0, true, ItemID.DRAGON_SCIMITAR).earned(),
		// The dragon scimitar and longsword need quests; the dragon sword doesn't and hits about as hard
		tier("Dragon sword", Skill.ATTACK, 60, 0, true, ItemID.DRAGON_SHORTSWORD),
		tier("Rune scimitar", Skill.ATTACK, 40, 0, false, ItemID.RUNE_SCIMITAR),
		tier("Adamant scimitar", Skill.ATTACK, 30, 0, false, ItemID.ADAMANT_SCIMITAR),
		tier("Mithril scimitar", Skill.ATTACK, 20, 0, false, ItemID.MITHRIL_SCIMITAR),
		tier("Black scimitar", Skill.ATTACK, 10, 0, false, ItemID.BLACK_SCIMITAR),
		tier("Steel scimitar", Skill.ATTACK, 5, 0, false, ItemID.STEEL_SCIMITAR),
		tier("Iron scimitar", Skill.ATTACK, 1, 0, false, ItemID.IRON_SCIMITAR)),
	MELEE_HELM(CombatStyle.MELEE, "helmet",
		tier("Torva full helm", Skill.DEFENCE, 80, 0, true, ItemID.TORVA_HELM).pricey(),
		tier("Neitiznot faceguard", Skill.DEFENCE, 70, 0, true, ItemID.NEITIZNOT_FACEGUARD).earned(),
		tier("Helm of neitiznot", Skill.DEFENCE, 55, 0, true, ItemID.FRIS_KINGLY_HELM).earned(),
		tier("Dragon med helm", Skill.DEFENCE, 60, 0, true, ItemID.DRAGON_MED_HELM),
		tier("Rune full helm", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_FULL_HELM),
		tier("Adamant full helm", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_FULL_HELM),
		tier("Mithril full helm", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_FULL_HELM),
		tier("Black full helm", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_FULL_HELM),
		tier("Steel full helm", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_FULL_HELM),
		tier("Iron full helm", Skill.DEFENCE, 1, 0, false, ItemID.IRON_FULL_HELM)),
	MELEE_BODY(CombatStyle.MELEE, "body",
		tier("Torva platebody", Skill.DEFENCE, 80, 0, true, ItemID.TORVA_CHEST).pricey(),
		tier("Bandos chestplate", Skill.DEFENCE, 65, 0, true, ItemID.BANDOS_CHESTPLATE).pricey(),
		tier("Fighter torso", Skill.DEFENCE, 40, 0, true, ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO).earned(),
		tier("Dragon chainbody", Skill.DEFENCE, 60, 0, true, ItemID.DRAGON_CHAINBODY),
		tier("Rune chainbody", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_CHAINBODY),
		tier("Adamant platebody", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_PLATEBODY),
		tier("Mithril platebody", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_PLATEBODY),
		tier("Black platebody", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_PLATEBODY),
		tier("Steel platebody", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_PLATEBODY),
		tier("Iron platebody", Skill.DEFENCE, 1, 0, false, ItemID.IRON_PLATEBODY)),
	MELEE_LEGS(CombatStyle.MELEE, "legs",
		tier("Torva platelegs", Skill.DEFENCE, 80, 0, true, ItemID.TORVA_LEGS).pricey(),
		tier("Bandos tassets", Skill.DEFENCE, 65, 0, true, ItemID.BANDOS_SKIRT).pricey(),
		tier("Dragon platelegs", Skill.DEFENCE, 60, 0, true, ItemID.DRAGON_PLATELEGS),
		tier("Rune platelegs", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_PLATELEGS),
		tier("Adamant platelegs", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_PLATELEGS),
		tier("Mithril platelegs", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_PLATELEGS),
		tier("Black platelegs", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_PLATELEGS),
		tier("Steel platelegs", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_PLATELEGS),
		tier("Iron platelegs", Skill.DEFENCE, 1, 0, false, ItemID.IRON_PLATELEGS)),
	MELEE_SHIELD(CombatStyle.MELEE, "shield",
		tier("Avernic defender", Skill.DEFENCE, 70, 0, true, ItemID.INFERNAL_DEFENDER).earned(),
		tier("Dragon defender", Skill.DEFENCE, 60, 0, true, ItemID.DRAGON_PARRYINGDAGGER).earned(),
		tier("Rune defender", Skill.DEFENCE, 40, 0, true, ItemID.RUNE_PARRYINGDAGGER).earned(),
		tier("Dragon sq shield", Skill.DEFENCE, 60, 0, true, ItemID.DRAGON_SQ_SHIELD),
		tier("Rune kiteshield", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_KITESHIELD),
		tier("Adamant kiteshield", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_KITESHIELD),
		tier("Mithril kiteshield", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_KITESHIELD),
		tier("Black kiteshield", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_KITESHIELD),
		tier("Steel kiteshield", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_KITESHIELD),
		tier("Iron kiteshield", Skill.DEFENCE, 1, 0, false, ItemID.IRON_KITESHIELD)),
	MELEE_AMULET(CombatStyle.MELEE, "amulet",
		tier("Amulet of torture", null, 0, 0, true, ItemID.ZENYTE_AMULET_ENCHANTED).pricey(),
		tier("Amulet of fury", null, 0, 0, true, ItemID.ENCHANTED_ONYX_AMULET),
		tier("Amulet of glory", null, 0, 0, true, Ids.GLORY),
		tier("Amulet of strength", null, 0, 0, false, ItemID.AMULET_OF_STRENGTH),
		tier("Amulet of power", null, 0, 0, false, ItemID.AMULET_OF_POWER)),
	MELEE_CAPE(CombatStyle.MELEE, "cape",
		tier("Infernal cape", null, 0, 0, true, ItemID.INFERNAL_CAPE).earned(),
		tier("Fire cape", null, 0, 0, true, ItemID.TZHAAR_CAPE_FIRE).earned(),
		tier("Obsidian cape", null, 0, 0, true, ItemID.TZHAAR_CAPE_OBSIDIAN)),
	MELEE_GLOVES(CombatStyle.MELEE, "gloves",
		tier("Ferocious gloves", Skill.ATTACK, 80, 80, true, ItemID.FEROCIOUS_GLOVES).earned(),
		tier("Barrows gloves", null, 0, 0, true, ItemID.HUNDRED_GAUNTLETS_LEVEL_10).earned(),
		tier("Dragon gloves", null, 0, 0, true, ItemID.HUNDRED_GAUNTLETS_LEVEL_9).earned(),
		tier("Combat bracelet", null, 0, 0, true, Ids.COMBAT_BRACELET),
		tier("Leather gloves", null, 0, 0, false, ItemID.LEATHER_GLOVES)),
	MELEE_BOOTS(CombatStyle.MELEE, "boots",
		tier("Primordial boots", Skill.DEFENCE, 75, 0, true, ItemID.PRIMORDIAL_BOOTS).pricey(),
		tier("Dragon boots", Skill.DEFENCE, 60, 0, true, ItemID.DRAGON_BOOTS),
		tier("Rune boots", Skill.DEFENCE, 40, 0, true, ItemID.RUNE_ARMOURED_BOOTS),
		tier("Leather boots", null, 0, 0, false, ItemID.LEATHER_BOOTS)),
	MELEE_RING(CombatStyle.MELEE, "ring",
		tier("Ultor ring", null, 0, 0, true, ItemID.ULTOR_RING).pricey(),
		tier("Berserker ring (i)", null, 0, 0, true, ItemID.NZONE_BERZERKER_RING).earned(),
		tier("Berserker ring", null, 0, 0, true, ItemID.BERZERKER_RING),
		tier("Warrior ring", null, 0, 0, true, ItemID.WARRIOR_RING)),
	/**
	 * Bows and the blowpipe take both hands; crossbows leave the shield slot free for a ranged offhand. A weapon
	 * that fires nothing ({@link Ammo#NONE}) needs no ammo.
	 */
	RANGED_WEAPON(CombatStyle.RANGED, "weapon",
		weapon("Twisted bow", 75, Ammo.ARROWS, ItemID.TWISTED_BOW).twoHanded().pricey(),
		weapon("Bow of faerdhinen", 80, Ammo.NONE, ItemID.BOW_OF_FAERDHINEN, ItemID.BOW_OF_FAERDHINEN_INFINITE,
			ItemID.BOW_OF_FAERDHINEN_INFINITE_ITHELL, ItemID.BOW_OF_FAERDHINEN_INFINITE_IORWERTH,
			ItemID.BOW_OF_FAERDHINEN_INFINITE_TRAHAEARN, ItemID.BOW_OF_FAERDHINEN_INFINITE_CADARN,
			ItemID.BOW_OF_FAERDHINEN_INFINITE_CRWYS, ItemID.BOW_OF_FAERDHINEN_INFINITE_MEILYR,
			ItemID.BOW_OF_FAERDHINEN_INFINITE_AMLODD).twoHanded().earned(),
		weapon("Zaryte crossbow", 80, Ammo.BOLTS, ItemID.ZARYTE_XBOW).pricey(),
		weapon("Toxic blowpipe", 75, Ammo.DARTS, ItemID.TOXIC_BLOWPIPE_LOADED, ItemID.TOXIC_BLOWPIPE).twoHanded(),
		weapon("Armadyl crossbow", 70, Ammo.BOLTS, ItemID.ACB).pricey(),
		weapon("Dragon hunter crossbow", 70, Ammo.BOLTS, ItemID.DRAGONHUNTER_XBOW, ItemID.DRAGONHUNTER_XBOW_VORKATH,
			ItemID.DRAGONHUNTER_XBOW_KBD).pricey(),
		weapon("Dragon crossbow", 64, Ammo.BOLTS, ItemID.XBOWS_CROSSBOW_DRAGON),
		weapon("Rune crossbow", 61, Ammo.BOLTS, ItemID.XBOWS_CROSSBOW_RUNITE),
		weapon("Magic shortbow", 50, Ammo.ARROWS, ItemID.MAGIC_SHORTBOW).twoHanded(),
		weapon("Yew shortbow", 40, Ammo.ARROWS, ItemID.YEW_SHORTBOW).f2p().twoHanded(),
		weapon("Maple shortbow", 30, Ammo.ARROWS, ItemID.MAPLE_SHORTBOW).f2p().twoHanded(),
		weapon("Willow shortbow", 20, Ammo.ARROWS, ItemID.WILLOW_SHORTBOW).f2p().twoHanded(),
		weapon("Oak shortbow", 5, Ammo.ARROWS, ItemID.OAK_SHORTBOW).f2p().twoHanded(),
		weapon("Shortbow", 1, Ammo.ARROWS, ItemID.SHORTBOW).f2p().twoHanded()),
	/**
	 * Only the ammo the weapon in use fires is suggested. A bow fires arrows up to its own level (a willow shortbow
	 * fires up to mithril, a magic shortbow up to amethyst), and the same cap works for crossbows and darts.
	 */
	RANGED_AMMO(CombatStyle.RANGED, "ammo",
		ammo("Dragon darts", 60, true, Ammo.DARTS, ItemID.DRAGON_DART),
		ammo("Amethyst darts", 50, true, Ammo.DARTS, ItemID.AMETHYST_DART),
		ammo("Rune darts", 40, true, Ammo.DARTS, ItemID.RUNE_DART),
		ammo("Adamant darts", 30, true, Ammo.DARTS, ItemID.ADAMANT_DART),
		ammo("Mithril darts", 20, true, Ammo.DARTS, ItemID.MITHRIL_DART),
		// Only dragon-tier crossbows (level 64+) fire dragon bolts
		ammo("Dragon bolts", 64, true, Ammo.BOLTS, ItemID.DRAGON_BOLTS),
		ammo("Runite bolts", 61, true, Ammo.BOLTS, ItemID.XBOWS_CROSSBOW_BOLTS_RUNITE),
		ammo("Adamant bolts", 46, true, Ammo.BOLTS, ItemID.XBOWS_CROSSBOW_BOLTS_ADAMANTITE),
		ammo("Dragon arrows", 60, true, Ammo.ARROWS, ItemID.DRAGON_ARROW),
		ammo("Amethyst arrows", 50, true, Ammo.ARROWS, ItemID.AMETHYST_ARROW),
		ammo("Rune arrows", 40, false, Ammo.ARROWS, ItemID.RUNE_ARROW),
		ammo("Adamant arrows", 30, false, Ammo.ARROWS, ItemID.ADAMANT_ARROW),
		ammo("Mithril arrows", 20, false, Ammo.ARROWS, ItemID.MITHRIL_ARROW),
		ammo("Steel arrows", 5, false, Ammo.ARROWS, ItemID.STEEL_ARROW),
		ammo("Iron arrows", 1, false, Ammo.ARROWS, ItemID.IRON_ARROW)),
	/**
	 * The offhand for a one-handed crossbow; never suggested with a bow or the blowpipe.
	 */
	RANGED_SHIELD(CombatStyle.RANGED, "shield",
		tier("Twisted buckler", Skill.RANGED, 75, 75, true, ItemID.TWISTED_BUCKLER).pricey(),
		// Needs Dragon Slayer I started, so it's only used when owned
		tier("Dragonfire ward", Skill.RANGED, 70, 75, true, ItemID.DRAGONFIRE_WARD, ItemID.DRAGONFIRE_WARD_UNCHARGED)
			.earned(),
		tier("Odium ward", Skill.DEFENCE, 60, 0, true, ItemID.ODIUM_WARD),
		tier("Antler guard", Skill.PRAYER, 50, 0, true, ItemID.CUSTODIAN_ANTLER_GUARD),
		tier("Black d'hide shield", Skill.RANGED, 70, 40, true, ItemID.BLACK_DHIDE_SHIELD),
		tier("Red d'hide shield", Skill.RANGED, 60, 40, true, ItemID.RED_DHIDE_SHIELD),
		tier("Blue d'hide shield", Skill.RANGED, 50, 40, true, ItemID.BLUE_DHIDE_SHIELD),
		tier("Green d'hide shield", Skill.RANGED, 40, 40, true, ItemID.GREEN_DHIDE_SHIELD)),
	RANGED_HELM(CombatStyle.RANGED, "helmet",
		tier("Masori mask (f)", Skill.RANGED, 80, 80, true, ItemID.MASORI_MASK_FORTIFIED).pricey(),
		tier("Masori mask", Skill.RANGED, 80, 30, true, ItemID.MASORI_MASK).pricey(),
		tier("Armadyl helmet", Skill.RANGED, 70, 70, true, ItemID.ARMADYL_HELMET, ItemID.ARMADYL_HELMET_ORNAMENT)
			.pricey(),
		tier("Archer helm", Skill.DEFENCE, 45, 0, true, ItemID.VIKING_HELMET_RANGE).earned(),
		tier("Coif", Skill.RANGED, 20, 0, false, ItemID.COIF),
		tier("Leather cowl", null, 0, 0, false, ItemID.LEATHER_COWL)),
	RANGED_BODY(CombatStyle.RANGED, "body",
		tier("Masori body (f)", Skill.RANGED, 80, 80, true, ItemID.MASORI_BODY_FORTIFIED).pricey(),
		tier("Masori body", Skill.RANGED, 80, 30, true, ItemID.MASORI_BODY).pricey(),
		tier("Armadyl chestplate", Skill.RANGED, 70, 70, true, ItemID.ARMADYL_CHESTPLATE,
			ItemID.ARMADYL_CHESTPLATE_ORNAMENT).pricey(),
		tier("Black d'hide body", Skill.RANGED, 70, 40, true, ItemID.BLACK_DRAGONHIDE_BODY),
		tier("Red d'hide body", Skill.RANGED, 60, 40, true, ItemID.RED_DRAGONHIDE_BODY),
		tier("Blue d'hide body", Skill.RANGED, 50, 40, true, ItemID.BLUE_DRAGONHIDE_BODY),
		tier("Green d'hide body", Skill.RANGED, 40, 40, false, ItemID.DRAGONHIDE_BODY),
		tier("Studded body", Skill.RANGED, 20, 20, false, ItemID.STUDDED_BODY),
		tier("Hardleather body", Skill.DEFENCE, 10, 0, false, ItemID.HARDLEATHER_BODY),
		tier("Leather body", Skill.DEFENCE, 1, 0, false, ItemID.LEATHER_ARMOUR)),
	RANGED_LEGS(CombatStyle.RANGED, "legs",
		tier("Masori chaps (f)", Skill.RANGED, 80, 80, true, ItemID.MASORI_CHAPS_FORTIFIED).pricey(),
		tier("Masori chaps", Skill.RANGED, 80, 30, true, ItemID.MASORI_CHAPS).pricey(),
		tier("Armadyl chainskirt", Skill.RANGED, 70, 70, true, ItemID.ARMADYL_SKIRT, ItemID.ARMADYL_SKIRT_ORNAMENT)
			.pricey(),
		tier("Black d'hide chaps", Skill.RANGED, 70, 0, true, ItemID.BLACK_DRAGONHIDE_CHAPS),
		tier("Red d'hide chaps", Skill.RANGED, 60, 0, true, ItemID.RED_DRAGONHIDE_CHAPS),
		tier("Blue d'hide chaps", Skill.RANGED, 50, 0, true, ItemID.BLUE_DRAGONHIDE_CHAPS),
		tier("Green d'hide chaps", Skill.RANGED, 40, 0, false, ItemID.DRAGONHIDE_CHAPS),
		tier("Studded chaps", Skill.RANGED, 20, 0, false, ItemID.STUDDED_CHAPS),
		tier("Leather chaps", Skill.RANGED, 1, 0, false, ItemID.LEATHER_CHAPS)),
	RANGED_HANDS(CombatStyle.RANGED, "vambraces",
		tier("Zaryte vambraces", Skill.RANGED, 80, 45, true, ItemID.ZARYTE_VAMBRACES).pricey(),
		tier("Barrows gloves", null, 0, 0, true, ItemID.HUNDRED_GAUNTLETS_LEVEL_10).earned(),
		tier("Black d'hide vambraces", Skill.RANGED, 70, 0, true, ItemID.BLACK_DRAGON_VAMBRACES),
		tier("Red d'hide vambraces", Skill.RANGED, 60, 0, true, ItemID.RED_DRAGON_VAMBRACES),
		tier("Blue d'hide vambraces", Skill.RANGED, 50, 0, true, ItemID.BLUE_DRAGON_VAMBRACES),
		tier("Green d'hide vambraces", Skill.RANGED, 40, 0, false, ItemID.DRAGON_VAMBRACES),
		tier("Leather vambraces", Skill.RANGED, 1, 0, false, ItemID.LEATHER_VAMBRACES)),
	RANGED_BOOTS(CombatStyle.RANGED, "boots",
		tier("Pegasian boots", Skill.RANGED, 75, 75, true, ItemID.PEGASIAN_BOOTS).pricey(),
		tier("Snakeskin boots", Skill.RANGED, 30, 30, true, ItemID.SNAKESKIN_BOOTS),
		tier("Leather boots", null, 0, 0, false, ItemID.LEATHER_BOOTS)),
	RANGED_AMULET(CombatStyle.RANGED, "amulet",
		tier("Necklace of anguish", null, 0, 0, true, ItemID.ZENYTE_NECKLACE_ENCHANTED,
			ItemID.ZENYTE_NECKLACE_ORNAMENT).pricey(),
		tier("Amulet of fury", null, 0, 0, true, ItemID.ENCHANTED_ONYX_AMULET),
		tier("Amulet of glory", null, 0, 0, true, Ids.GLORY),
		tier("Amulet of power", null, 0, 0, false, ItemID.AMULET_OF_POWER)),
	RANGED_CAPE(CombatStyle.RANGED, "cape",
		tier("Ava's assembler", Skill.RANGED, 70, 0, true, ItemID.AVAS_ASSEMBLER).earned(),
		tier("Ava's accumulator", Skill.RANGED, 50, 0, true, ItemID.ANMA_50_REWARD).earned(),
		tier("Ava's attractor", Skill.RANGED, 30, 0, true, ItemID.ANMA_30_REWARD).earned()),
	RANGED_RING(CombatStyle.RANGED, "ring",
		tier("Venator ring", null, 0, 0, true, ItemID.VENATOR_RING).earned(),
		tier("Archers ring (i)", null, 0, 0, true, ItemID.NZONE_RANGER_RING).earned(),
		tier("Archers ring", null, 0, 0, true, ItemID.RANGER_RING).pricey()),
	/**
	 * Air staves: every combat spell uses air runes, and wind spells need nothing else.
	 */
	MAGIC_WEAPON(CombatStyle.MAGIC, "weapon",
		staff("Mystic air staff", 40, true, ItemID.MYSTIC_AIR_STAFF),
		staff("Air battlestaff", 30, true, ItemID.AIR_BATTLESTAFF),
		tier("Staff of air", null, 0, 0, false, ItemID.STAFF_OF_AIR)),
	MAGIC_HELM(CombatStyle.MAGIC, "helmet",
		tier("Ancestral hat", Skill.MAGIC, 75, 65, true, ItemID.ANCESTRAL_HAT).pricey(),
		tier("Mystic hat", Skill.MAGIC, 40, 20, true, ItemID.MYSTIC_HAT, ItemID.MYSTIC_HAT_DARK, ItemID.MYSTIC_HAT_LIGHT,
			ItemID.MYSTIC_HAT_DUSK),
		tier("Wizard hat", null, 0, 0, false, ItemID.BLUEWIZHAT)),
	MAGIC_BODY(CombatStyle.MAGIC, "body",
		tier("Ancestral robe top", Skill.MAGIC, 75, 65, true, ItemID.ANCESTRAL_ROBE_TOP).pricey(),
		tier("Mystic robe top", Skill.MAGIC, 40, 20, true, ItemID.MYSTIC_ROBE_TOP, ItemID.MYSTIC_ROBE_TOP_DARK,
			ItemID.MYSTIC_ROBE_TOP_LIGHT, ItemID.MYSTIC_ROBE_TOP_DUSK),
		tier("Wizard robe", null, 0, 0, false, ItemID.WIZARDS_ROBE)),
	MAGIC_LEGS(CombatStyle.MAGIC, "legs",
		tier("Ancestral robe bottom", Skill.MAGIC, 75, 65, true, ItemID.ANCESTRAL_ROBE_BOTTOM).pricey(),
		tier("Mystic robe bottom", Skill.MAGIC, 40, 20, true, ItemID.MYSTIC_ROBE_BOTTOM,
			ItemID.MYSTIC_ROBE_BOTTOM_DARK, ItemID.MYSTIC_ROBE_BOTTOM_LIGHT, ItemID.MYSTIC_ROBE_BOTTOM_DUSK),
		tier("Blue skirt", null, 0, 0, false, ItemID.BLUE_SKIRT)),
	MAGIC_GLOVES(CombatStyle.MAGIC, "gloves",
		tier("Tormented bracelet", null, 0, 0, true, ItemID.ZENYTE_BRACELET_ENCHANTED,
			ItemID.ZENYTE_BRACELET_ENCHANTED_ORNAMENT).pricey(),
		tier("Barrows gloves", null, 0, 0, true, ItemID.HUNDRED_GAUNTLETS_LEVEL_10).earned(),
		tier("Mystic gloves", Skill.MAGIC, 40, 20, true, ItemID.MYSTIC_GLOVES, ItemID.MYSTIC_GLOVES_DARK,
			ItemID.MYSTIC_GLOVES_LIGHT, ItemID.MYSTIC_GLOVES_DUSK)),
	MAGIC_BOOTS(CombatStyle.MAGIC, "boots",
		tier("Eternal boots", Skill.MAGIC, 75, 75, true, ItemID.ETERNAL_BOOTS).pricey(),
		tier("Mystic boots", Skill.MAGIC, 40, 20, true, ItemID.MYSTIC_BOOTS, ItemID.MYSTIC_BOOTS_DARK,
			ItemID.MYSTIC_BOOTS_LIGHT, ItemID.MYSTIC_BOOTS_DUSK),
		tier("Wizard boots", Skill.MAGIC, 20, 0, true, ItemID.BOOTS_WIZARD)),
	MAGIC_AMULET(CombatStyle.MAGIC, "amulet",
		tier("Occult necklace", Skill.MAGIC, 70, 0, true, ItemID.OCCULT_NECKLACE),
		tier("Amulet of glory", null, 0, 0, true, Ids.GLORY),
		tier("Amulet of magic", null, 0, 0, false, ItemID.AMULET_OF_MAGIC)),
	MAGIC_CAPE(CombatStyle.MAGIC, "cape",
		tier("Imbued god cape", Skill.MAGIC, 75, 0, true, ItemID.MA2_SARADOMIN_CAPE, ItemID.MA2_GUTHIX_CAPE,
			ItemID.MA2_ZAMORAK_CAPE, ItemID.MA2_ZAROS_CAPE).earned(),
		tier("God cape", Skill.MAGIC, 60, 0, true, ItemID.SARADOMIN_CAPE, ItemID.GUTHIX_CAPE, ItemID.ZAMORAK_CAPE)
			.earned());

	/**
	 * What a ranged weapon fires, or what a piece of ammo is. {@link #NONE} for everything else.
	 */
	public enum Ammo
	{
		NONE,
		ARROWS,
		BOLTS,
		DARTS
	}

	/**
	 * The ammo input of a ranged training method. Any arrows, bolts or darts count as carrying it; the guide picks
	 * which ones to buy from the player's weapon and level. A loaded blowpipe carries its own darts.
	 */
	public static final ItemReq AMMO_INPUT = ItemReq.stack("Ammo", 1, allIds(Ammo.ARROWS, Ammo.BOLTS, Ammo.DARTS))
		.orUse("Loaded toxic blowpipe", ItemID.TOXIC_BLOWPIPE_LOADED);

	/**
	 * Chinchompas thrown at groups of monsters, best first, with the Ranged level each needs. The chinchompa is the
	 * weapon, so chinning methods suggest no bow, ammo or offhand.
	 */
	public static final String[] CHINCHOMPA_NAMES = {"Black chinchompas", "Red chinchompas", "Chinchompas"};
	public static final int[] CHINCHOMPA_IDS = {ItemID.CHINCHOMPA_BLACK, ItemID.CHINCHOMPA_BIG_CAPTURED,
		ItemID.CHINCHOMPA_CAPTURED};
	public static final int[] CHINCHOMPA_LEVELS = {65, 55, 45};
	/**
	 * The ammo input of a chinning method. The guide names the best kind the player owns and can throw, or else the
	 * best they can buy.
	 */
	public static final ItemReq CHINCHOMPA_INPUT = ItemReq.stack("Chinchompas", 1, ItemID.CHINCHOMPA_CAPTURED,
		ItemID.CHINCHOMPA_BIG_CAPTURED, ItemID.CHINCHOMPA_BLACK);

	/**
	 * One tier of gear. {@code skill} is null when there's no level requirement; {@code defenceLevel} and
	 * {@code attackLevel} are extra Defence and Attack requirements (0 if none). {@code ammo} is what a weapon fires,
	 * or what kind of ammo this is.
	 */
	@Value
	@With(AccessLevel.PRIVATE)
	public static class Tier
	{
		String name;
		Skill skill;
		int level;
		int defenceLevel;
		int attackLevel;
		boolean members;
		Ammo ammo;
		int[] ids;
		/**
		 * Can be bought on the GE without a quest or minigame. Gear that has to be earned (defenders, fire cape,
		 * Barrows gloves...) is only suggested when the player already owns it.
		 */
		boolean buyable;
		/**
		 * Costs more than a few million, so it is only suggested as a buy when the player has the coins for it.
		 */
		boolean pricey;
		/**
		 * A two-handed weapon, which leaves no room for a shield or offhand.
		 */
		boolean twoHanded;

		/**
		 * Whether this ammo can be fired from {@code weapon}.
		 */
		public boolean firedFrom(Tier weapon)
		{
			return weapon.ammo != Ammo.NONE && ammo == weapon.ammo && level <= weapon.level;
		}

		Tier earned()
		{
			return withBuyable(false);
		}

		Tier pricey()
		{
			return withPricey(true);
		}

		Tier twoHanded()
		{
			return withTwoHanded(true);
		}

		Tier f2p()
		{
			return withMembers(false);
		}
	}

	/**
	 * Every charge of an item, in a holder class because enum constants can't refer to the enum's own static fields.
	 */
	private static final class Ids
	{
		static final int[] GLORY = {ItemID.AMULET_OF_GLORY_6, ItemID.AMULET_OF_GLORY_5, ItemID.AMULET_OF_GLORY_4,
			ItemID.AMULET_OF_GLORY_3, ItemID.AMULET_OF_GLORY_2, ItemID.AMULET_OF_GLORY_1, ItemID.AMULET_OF_GLORY};
		static final int[] COMBAT_BRACELET = {ItemID.JEWL_BRACELET_OF_COMBAT_4, ItemID.JEWL_BRACELET_OF_COMBAT_6,
			ItemID.JEWL_BRACELET_OF_COMBAT_5, ItemID.JEWL_BRACELET_OF_COMBAT_3, ItemID.JEWL_BRACELET_OF_COMBAT_2,
			ItemID.JEWL_BRACELET_OF_COMBAT_1, ItemID.JEWL_BRACELET_OF_COMBAT};
	}

	private final CombatStyle style;
	private final String slotName;
	private final List<Tier> tiers;

	CombatGear(CombatStyle style, String slotName, Tier... tiers)
	{
		this.style = style;
		this.slotName = slotName;
		this.tiers = Collections.unmodifiableList(Arrays.asList(tiers));
	}

	private static Tier tier(String name, Skill skill, int level, int defenceLevel, boolean members, int... ids)
	{
		return new Tier(name, skill, level, defenceLevel, 0, members, Ammo.NONE, ids, true, false, false);
	}

	/**
	 * A battlestaff or mystic staff, which needs the same Attack and Magic level.
	 */
	private static Tier staff(String name, int level, boolean members, int... ids)
	{
		return new Tier(name, Skill.MAGIC, level, 0, level, members, Ammo.NONE, ids, true, false, false);
	}

	/**
	 * A members ranged weapon; {@code fires} caps its ammo at the weapon's own level.
	 */
	private static Tier weapon(String name, int level, Ammo fires, int... ids)
	{
		return new Tier(name, Skill.RANGED, level, 0, 0, true, fires, ids, true, false, false);
	}

	private static Tier ammo(String name, int level, boolean members, Ammo kind, int... ids)
	{
		return new Tier(name, Skill.RANGED, level, 0, 0, members, kind, ids, true, false, false);
	}

	/**
	 * Every ammo id of these kinds, arrows first, best first within each kind.
	 */
	private static int[] allIds(Ammo... kinds)
	{
		List<Integer> ids = new ArrayList<>();
		for (Ammo kind : kinds)
		{
			for (Tier t : RANGED_AMMO.tiers)
			{
				if (t.ammo == kind)
				{
					for (int id : t.ids)
					{
						ids.add(id);
					}
				}
			}
		}
		return ids.stream().mapToInt(Integer::intValue).toArray();
	}
}
