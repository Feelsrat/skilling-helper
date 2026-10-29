package com.skillinghelper.pvm.guide;

import static com.skillinghelper.pvm.guide.Gear.buy;
import static com.skillinghelper.pvm.guide.Gear.earn;
import com.skillinghelper.pvm.guide.Gear.Ammo;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Melee and ranged gear per slot, best first, from end-game down to bronze. Level requirements are the commonly known
 * ones (from memory; the wiki can't be reached from where this was written). Each guide caps what it suggests buying
 * with its budget, so a first boss doesn't suggest a 100m weapon.
 */
public final class GearSets
{
	private static final Skill ATT = Skill.ATTACK;
	private static final Skill DEF = Skill.DEFENCE;
	private static final Skill RNG = Skill.RANGED;

	public static final GearSlot MELEE_WEAPON = GearSlot.of("weapon",
		buy("Osmumten's fang", ATT, 82, 0, true, ItemID.OSMUMTENS_FANG),
		buy("Ghrazi rapier", ATT, 80, 0, true, ItemID.GHRAZI_RAPIER),
		buy("Abyssal tentacle", ATT, 75, 0, true, ItemID.ABYSSAL_TENTACLE),
		buy("Abyssal whip", ATT, 70, 0, true, ItemID.ABYSSAL_WHIP),
		earn("Dragon scimitar", ATT, 60, 0, true, ItemID.DRAGON_SCIMITAR),
		earn("Dragon longsword", ATT, 60, 0, true, ItemID.DRAGON_LONGSWORD),
		buy("Rune scimitar", ATT, 40, 0, false, ItemID.RUNE_SCIMITAR),
		buy("Adamant scimitar", ATT, 30, 0, false, ItemID.ADAMANT_SCIMITAR),
		buy("Mithril scimitar", ATT, 20, 0, false, ItemID.MITHRIL_SCIMITAR),
		buy("Black scimitar", ATT, 10, 0, false, ItemID.BLACK_SCIMITAR),
		buy("Steel scimitar", ATT, 5, 0, false, ItemID.STEEL_SCIMITAR),
		buy("Iron scimitar", ATT, 1, 0, false, ItemID.IRON_SCIMITAR));

	public static final GearSlot MELEE_HELM = GearSlot.of("helmet",
		buy("Torva full helm", DEF, 80, 0, true, ItemID.TORVA_HELM),
		earn("Neitiznot faceguard", DEF, 70, 0, true, ItemID.NEITIZNOT_FACEGUARD),
		earn("Helm of neitiznot", DEF, 55, 0, true, ItemID.FRIS_KINGLY_HELM),
		buy("Obsidian helmet", DEF, 60, 0, true, ItemID.OBSIDIAN_HELMET),
		earn("Berserker helm", DEF, 45, 0, true, ItemID.VIKING_HELMET_CRUSH),
		buy("Rune full helm", DEF, 40, 0, false, ItemID.RUNE_FULL_HELM),
		buy("Adamant full helm", DEF, 30, 0, false, ItemID.ADAMANT_FULL_HELM),
		buy("Mithril full helm", DEF, 20, 0, false, ItemID.MITHRIL_FULL_HELM),
		buy("Steel full helm", DEF, 5, 0, false, ItemID.STEEL_FULL_HELM),
		buy("Iron full helm", DEF, 1, 0, false, ItemID.IRON_FULL_HELM));

	public static final GearSlot MELEE_BODY = GearSlot.of("body",
		buy("Torva platebody", DEF, 80, 0, true, ItemID.TORVA_CHEST),
		buy("Bandos chestplate", DEF, 65, 0, true, ItemID.BANDOS_CHESTPLATE),
		earn("Fighter torso", DEF, 40, 0, true, ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO),
		buy("Obsidian platebody", DEF, 60, 0, true, ItemID.OBSIDIAN_PLATEBODY),
		earn("Rune platebody", DEF, 40, 0, false, ItemID.RUNE_PLATEBODY),
		buy("Rune chainbody", DEF, 40, 0, false, ItemID.RUNE_CHAINBODY),
		buy("Adamant platebody", DEF, 30, 0, false, ItemID.ADAMANT_PLATEBODY),
		buy("Mithril platebody", DEF, 20, 0, false, ItemID.MITHRIL_PLATEBODY),
		buy("Steel platebody", DEF, 5, 0, false, ItemID.STEEL_PLATEBODY),
		buy("Iron platebody", DEF, 1, 0, false, ItemID.IRON_PLATEBODY));

	public static final GearSlot MELEE_LEGS = GearSlot.of("legs",
		buy("Torva platelegs", DEF, 80, 0, true, ItemID.TORVA_LEGS),
		buy("Bandos tassets", DEF, 65, 0, true, ItemID.BANDOS_SKIRT),
		buy("Obsidian platelegs", DEF, 60, 0, true, ItemID.OBSIDIAN_PLATELEGS),
		buy("Dragon platelegs", DEF, 60, 0, true, ItemID.DRAGON_PLATELEGS),
		buy("Rune platelegs", DEF, 40, 0, false, ItemID.RUNE_PLATELEGS),
		buy("Adamant platelegs", DEF, 30, 0, false, ItemID.ADAMANT_PLATELEGS),
		buy("Mithril platelegs", DEF, 20, 0, false, ItemID.MITHRIL_PLATELEGS),
		buy("Steel platelegs", DEF, 5, 0, false, ItemID.STEEL_PLATELEGS),
		buy("Iron platelegs", DEF, 1, 0, false, ItemID.IRON_PLATELEGS));

	public static final GearSlot MELEE_SHIELD = GearSlot.of("shield",
		earn("Avernic defender", DEF, 70, 0, true, ItemID.INFERNAL_DEFENDER),
		earn("Dragon defender", DEF, 60, 0, true, ItemID.DRAGON_PARRYINGDAGGER),
		earn("Rune defender", DEF, 40, 0, true, ItemID.RUNE_PARRYINGDAGGER),
		buy("Rune kiteshield", DEF, 40, 0, false, ItemID.RUNE_KITESHIELD),
		buy("Adamant kiteshield", DEF, 30, 0, false, ItemID.ADAMANT_KITESHIELD),
		buy("Mithril kiteshield", DEF, 20, 0, false, ItemID.MITHRIL_KITESHIELD),
		buy("Steel kiteshield", DEF, 5, 0, false, ItemID.STEEL_KITESHIELD),
		buy("Iron kiteshield", DEF, 1, 0, false, ItemID.IRON_KITESHIELD));

	public static final GearSlot MELEE_AMULET = GearSlot.of("amulet",
		buy("Amulet of torture", null, 0, 0, true, ItemID.ZENYTE_AMULET_ENCHANTED),
		buy("Amulet of fury", null, 0, 0, true, ItemID.ENCHANTED_ONYX_AMULET),
		buy("Amulet of glory", null, 0, 0, true, ItemID.AMULET_OF_GLORY_6, ItemID.AMULET_OF_GLORY_5,
			ItemID.AMULET_OF_GLORY_4, ItemID.AMULET_OF_GLORY_3, ItemID.AMULET_OF_GLORY_2, ItemID.AMULET_OF_GLORY_1,
			ItemID.AMULET_OF_GLORY),
		buy("Amulet of power", null, 0, 0, false, ItemID.AMULET_OF_POWER),
		buy("Amulet of strength", null, 0, 0, false, ItemID.AMULET_OF_STRENGTH));

	public static final GearSlot MELEE_CAPE = GearSlot.of("cape",
		earn("Infernal cape", null, 0, 0, true, ItemID.INFERNAL_CAPE),
		earn("Fire cape", null, 0, 0, true, ItemID.TZHAAR_CAPE_FIRE),
		buy("Obsidian cape", null, 0, 0, true, ItemID.TZHAAR_CAPE_OBSIDIAN));

	public static final GearSlot MELEE_GLOVES = GearSlot.of("gloves",
		earn("Ferocious gloves", ATT, 80, 80, true, ItemID.FEROCIOUS_GLOVES),
		earn("Barrows gloves", null, 0, 0, true, ItemID.HUNDRED_GAUNTLETS_LEVEL_10),
		earn("Dragon gloves", null, 0, 0, true, ItemID.HUNDRED_GAUNTLETS_LEVEL_9),
		earn("Rune gloves", null, 0, 0, true, ItemID.HUNDRED_GAUNTLETS_LEVEL_8),
		buy("Combat bracelet", null, 0, 0, true, ItemID.JEWL_BRACELET_OF_COMBAT_4, ItemID.JEWL_BRACELET_OF_COMBAT_6,
			ItemID.JEWL_BRACELET_OF_COMBAT_5, ItemID.JEWL_BRACELET_OF_COMBAT_3, ItemID.JEWL_BRACELET_OF_COMBAT_2,
			ItemID.JEWL_BRACELET_OF_COMBAT_1, ItemID.JEWL_BRACELET_OF_COMBAT),
		buy("Leather gloves", null, 0, 0, false, ItemID.LEATHER_GLOVES));

	public static final GearSlot MELEE_BOOTS = GearSlot.of("boots",
		buy("Primordial boots", DEF, 75, 0, true, ItemID.PRIMORDIAL_BOOTS),
		buy("Dragon boots", DEF, 60, 0, true, ItemID.DRAGON_BOOTS),
		buy("Rune boots", DEF, 40, 0, true, ItemID.RUNE_ARMOURED_BOOTS),
		buy("Climbing boots", null, 0, 0, true, ItemID.DEATH_CLIMBINGBOOTS),
		buy("Leather boots", null, 0, 0, false, ItemID.LEATHER_BOOTS));

	public static final GearSlot MELEE_RING = GearSlot.of("ring",
		buy("Ultor ring", null, 0, 0, true, ItemID.ULTOR_RING),
		earn("Berserker ring (i)", null, 0, 0, true, ItemID.NZONE_BERZERKER_RING),
		buy("Berserker ring", null, 0, 0, true, ItemID.BERZERKER_RING),
		buy("Warrior ring", null, 0, 0, true, ItemID.WARRIOR_RING));

	public static final GearSlot RANGED_WEAPON = GearSlot.of("weapon",
		buy("Twisted bow", RNG, 75, 0, true, ItemID.TWISTED_BOW).firing(Ammo.ARROWS),
		earn("Bow of faerdhinen", RNG, 80, 0, true, ItemID.BOW_OF_FAERDHINEN),
		buy("Toxic blowpipe", RNG, 75, 0, true, ItemID.TOXIC_BLOWPIPE_LOADED, ItemID.TOXIC_BLOWPIPE)
			.firing(Ammo.DARTS),
		buy("Armadyl crossbow", RNG, 70, 0, true, ItemID.ACB).firing(Ammo.BOLTS),
		buy("Rune crossbow", RNG, 61, 0, true, ItemID.XBOWS_CROSSBOW_RUNITE).firing(Ammo.BOLTS),
		buy("Magic shortbow", RNG, 50, 0, true, ItemID.MAGIC_SHORTBOW).firing(Ammo.ARROWS),
		buy("Yew shortbow", RNG, 40, 0, false, ItemID.YEW_SHORTBOW).firing(Ammo.ARROWS),
		buy("Maple shortbow", RNG, 30, 0, false, ItemID.MAPLE_SHORTBOW).firing(Ammo.ARROWS),
		buy("Willow shortbow", RNG, 20, 0, false, ItemID.WILLOW_SHORTBOW).firing(Ammo.ARROWS),
		buy("Oak shortbow", RNG, 5, 0, false, ItemID.OAK_SHORTBOW).firing(Ammo.ARROWS));

	public static final GearSlot RANGED_HELM = GearSlot.of("helmet",
		buy("Masori mask (f)", RNG, 80, 30, true, ItemID.MASORI_MASK_FORTIFIED),
		buy("Armadyl helmet", RNG, 70, 70, true, ItemID.ARMADYL_HELMET),
		earn("Archer helm", DEF, 45, 0, true, ItemID.VIKING_HELMET_RANGE),
		buy("Coif", RNG, 20, 0, false, ItemID.COIF),
		buy("Leather cowl", null, 0, 0, false, ItemID.LEATHER_COWL));

	public static final GearSlot RANGED_BODY = GearSlot.of("body",
		buy("Masori body (f)", RNG, 80, 30, true, ItemID.MASORI_BODY_FORTIFIED),
		buy("Armadyl chestplate", RNG, 70, 70, true, ItemID.ARMADYL_CHESTPLATE),
		buy("Black d'hide body", RNG, 70, 40, true, ItemID.BLACK_DRAGONHIDE_BODY),
		buy("Red d'hide body", RNG, 60, 40, true, ItemID.RED_DRAGONHIDE_BODY),
		buy("Blue d'hide body", RNG, 50, 40, true, ItemID.BLUE_DRAGONHIDE_BODY),
		buy("Green d'hide body", RNG, 40, 40, false, ItemID.DRAGONHIDE_BODY),
		buy("Studded body", RNG, 20, 20, false, ItemID.STUDDED_BODY));

	public static final GearSlot RANGED_LEGS = GearSlot.of("legs",
		buy("Masori chaps (f)", RNG, 80, 30, true, ItemID.MASORI_CHAPS_FORTIFIED),
		buy("Armadyl chainskirt", RNG, 70, 70, true, ItemID.ARMADYL_SKIRT),
		buy("Black d'hide chaps", RNG, 70, 0, true, ItemID.BLACK_DRAGONHIDE_CHAPS),
		buy("Red d'hide chaps", RNG, 60, 0, true, ItemID.RED_DRAGONHIDE_CHAPS),
		buy("Blue d'hide chaps", RNG, 50, 0, true, ItemID.BLUE_DRAGONHIDE_CHAPS),
		buy("Green d'hide chaps", RNG, 40, 0, false, ItemID.DRAGONHIDE_CHAPS),
		buy("Studded chaps", RNG, 20, 0, false, ItemID.STUDDED_CHAPS));

	public static final GearSlot RANGED_GLOVES = GearSlot.of("gloves",
		buy("Zaryte vambraces", RNG, 80, 45, true, ItemID.ZARYTE_VAMBRACES),
		earn("Barrows gloves", null, 0, 0, true, ItemID.HUNDRED_GAUNTLETS_LEVEL_10),
		buy("Black d'hide vambraces", RNG, 70, 0, true, ItemID.BLACK_DRAGON_VAMBRACES),
		buy("Red d'hide vambraces", RNG, 60, 0, true, ItemID.RED_DRAGON_VAMBRACES),
		buy("Blue d'hide vambraces", RNG, 50, 0, true, ItemID.BLUE_DRAGON_VAMBRACES),
		buy("Green d'hide vambraces", RNG, 40, 0, false, ItemID.DRAGON_VAMBRACES),
		buy("Leather vambraces", null, 0, 0, false, ItemID.LEATHER_VAMBRACES));

	public static final GearSlot RANGED_BOOTS = GearSlot.of("boots",
		buy("Pegasian boots", RNG, 75, 75, true, ItemID.PEGASIAN_BOOTS),
		buy("Snakeskin boots", RNG, 30, 30, true, ItemID.SNAKESKIN_BOOTS),
		buy("Leather boots", null, 0, 0, false, ItemID.LEATHER_BOOTS));

	public static final GearSlot RANGED_AMULET = GearSlot.of("amulet",
		buy("Necklace of anguish", null, 0, 0, true, ItemID.ZENYTE_NECKLACE_ENCHANTED),
		MELEE_AMULET.getOptions().get(1),
		MELEE_AMULET.getOptions().get(2),
		MELEE_AMULET.getOptions().get(3));

	public static final GearSlot RANGED_CAPE = GearSlot.of("cape",
		earn("Ava's assembler", RNG, 70, 0, true, ItemID.AVAS_ASSEMBLER),
		earn("Ava's accumulator", RNG, 50, 0, true, ItemID.ANMA_50_REWARD),
		earn("Ava's attractor", RNG, 30, 0, true, ItemID.ANMA_30_REWARD));

	public static final GearSlot RANGED_RING = GearSlot.of("ring",
		buy("Venator ring", null, 0, 0, true, ItemID.VENATOR_RING),
		earn("Archers ring (i)", null, 0, 0, true, ItemID.NZONE_RANGER_RING));

	/**
	 * Melee gear for every slot.
	 */
	public static List<GearSlot> melee()
	{
		return new ArrayList<>(List.of(MELEE_WEAPON, MELEE_HELM, MELEE_BODY, MELEE_LEGS, MELEE_SHIELD, MELEE_AMULET,
			MELEE_CAPE, MELEE_GLOVES, MELEE_BOOTS, MELEE_RING));
	}

	/**
	 * Ranged gear for every slot. Ammo is added to the supplies from the weapon picked.
	 */
	public static List<GearSlot> ranged()
	{
		return new ArrayList<>(List.of(RANGED_WEAPON, RANGED_HELM, RANGED_BODY, RANGED_LEGS, RANGED_AMULET,
			RANGED_CAPE, RANGED_GLOVES, RANGED_BOOTS, RANGED_RING));
	}

	/**
	 * The same slots with one replaced (matched by name), e.g. an anti-dragon shield instead of the usual shield.
	 */
	public static List<GearSlot> with(List<GearSlot> slots, GearSlot replacement)
	{
		List<GearSlot> out = new ArrayList<>();
		boolean replaced = false;
		for (GearSlot s : slots)
		{
			if (s.getName().equals(replacement.getName()))
			{
				out.add(replacement);
				replaced = true;
			}
			else
			{
				out.add(s);
			}
		}
		if (!replaced)
		{
			out.add(replacement);
		}
		return out;
	}

	private GearSets()
	{
	}
}
