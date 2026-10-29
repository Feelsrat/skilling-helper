package com.skillinghelper.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Weapon and armour tiers per equipment slot, best first. Used to suggest the best gear the player owns (to wear) or
 * could buy on the GE. Only gear without quest requirements is listed, so every suggestion can actually be worn. Pieces
 * that cost more than a few million (Ghrazi rapier, Armadyl crossbow, Bandos...) are left to the PvM guides.
 */
@Getter
public enum CombatGear
{
	MELEE_WEAPON(CombatStyle.MELEE, "weapon",
		tier("Abyssal tentacle", Skill.ATTACK, 75, 0, true, ItemID.ABYSSAL_TENTACLE),
		tier("Abyssal whip", Skill.ATTACK, 70, 0, true, ItemID.ABYSSAL_WHIP),
		// The dragon scimitar and longsword need quests; the dragon sword doesn't and hits about as hard
		tier("Dragon sword", Skill.ATTACK, 60, 0, true, ItemID.DRAGON_SHORTSWORD),
		tier("Rune scimitar", Skill.ATTACK, 40, 0, false, ItemID.RUNE_SCIMITAR),
		tier("Adamant scimitar", Skill.ATTACK, 30, 0, false, ItemID.ADAMANT_SCIMITAR),
		tier("Mithril scimitar", Skill.ATTACK, 20, 0, false, ItemID.MITHRIL_SCIMITAR),
		tier("Black scimitar", Skill.ATTACK, 10, 0, false, ItemID.BLACK_SCIMITAR),
		tier("Steel scimitar", Skill.ATTACK, 5, 0, false, ItemID.STEEL_SCIMITAR),
		tier("Iron scimitar", Skill.ATTACK, 1, 0, false, ItemID.IRON_SCIMITAR)),
	MELEE_HELM(CombatStyle.MELEE, "helmet",
		tier("Dragon med helm", Skill.DEFENCE, 60, 0, true, ItemID.DRAGON_MED_HELM),
		tier("Rune full helm", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_FULL_HELM),
		tier("Adamant full helm", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_FULL_HELM),
		tier("Mithril full helm", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_FULL_HELM),
		tier("Black full helm", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_FULL_HELM),
		tier("Steel full helm", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_FULL_HELM),
		tier("Iron full helm", Skill.DEFENCE, 1, 0, false, ItemID.IRON_FULL_HELM)),
	MELEE_BODY(CombatStyle.MELEE, "body",
		tier("Dragon chainbody", Skill.DEFENCE, 60, 0, true, ItemID.DRAGON_CHAINBODY),
		tier("Rune chainbody", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_CHAINBODY),
		tier("Adamant platebody", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_PLATEBODY),
		tier("Mithril platebody", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_PLATEBODY),
		tier("Black platebody", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_PLATEBODY),
		tier("Steel platebody", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_PLATEBODY),
		tier("Iron platebody", Skill.DEFENCE, 1, 0, false, ItemID.IRON_PLATEBODY)),
	MELEE_LEGS(CombatStyle.MELEE, "legs",
		tier("Dragon platelegs", Skill.DEFENCE, 60, 0, true, ItemID.DRAGON_PLATELEGS),
		tier("Rune platelegs", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_PLATELEGS),
		tier("Adamant platelegs", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_PLATELEGS),
		tier("Mithril platelegs", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_PLATELEGS),
		tier("Black platelegs", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_PLATELEGS),
		tier("Steel platelegs", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_PLATELEGS),
		tier("Iron platelegs", Skill.DEFENCE, 1, 0, false, ItemID.IRON_PLATELEGS)),
	MELEE_SHIELD(CombatStyle.MELEE, "shield",
		tier("Dragon sq shield", Skill.DEFENCE, 60, 0, true, ItemID.DRAGON_SQ_SHIELD),
		tier("Rune kiteshield", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_KITESHIELD),
		tier("Adamant kiteshield", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_KITESHIELD),
		tier("Mithril kiteshield", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_KITESHIELD),
		tier("Black kiteshield", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_KITESHIELD),
		tier("Steel kiteshield", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_KITESHIELD),
		tier("Iron kiteshield", Skill.DEFENCE, 1, 0, false, ItemID.IRON_KITESHIELD)),
	MELEE_AMULET(CombatStyle.MELEE, "amulet",
		tier("Amulet of fury", null, 0, 0, true, ItemID.ENCHANTED_ONYX_AMULET),
		tier("Amulet of glory", null, 0, 0, true, Ids.GLORY),
		tier("Amulet of strength", null, 0, 0, false, ItemID.AMULET_OF_STRENGTH),
		tier("Amulet of power", null, 0, 0, false, ItemID.AMULET_OF_POWER)),
	MELEE_GLOVES(CombatStyle.MELEE, "gloves",
		tier("Combat bracelet", null, 0, 0, true, Ids.COMBAT_BRACELET),
		tier("Leather gloves", null, 0, 0, false, ItemID.LEATHER_GLOVES)),
	MELEE_BOOTS(CombatStyle.MELEE, "boots",
		tier("Dragon boots", Skill.DEFENCE, 60, 0, true, ItemID.DRAGON_BOOTS),
		tier("Rune boots", Skill.DEFENCE, 40, 0, true, ItemID.RUNE_ARMOURED_BOOTS),
		tier("Leather boots", null, 0, 0, false, ItemID.LEATHER_BOOTS)),
	MELEE_RING(CombatStyle.MELEE, "ring",
		tier("Berserker ring", null, 0, 0, true, ItemID.BERZERKER_RING),
		tier("Warrior ring", null, 0, 0, true, ItemID.WARRIOR_RING)),
	RANGED_WEAPON(CombatStyle.RANGED, "weapon",
		weapon("Toxic blowpipe", 75, true, Ammo.DARTS, ItemID.TOXIC_BLOWPIPE_LOADED, ItemID.TOXIC_BLOWPIPE),
		weapon("Rune crossbow", 61, true, Ammo.BOLTS, ItemID.XBOWS_CROSSBOW_RUNITE),
		weapon("Magic shortbow", 50, true, Ammo.ARROWS, ItemID.MAGIC_SHORTBOW),
		weapon("Yew shortbow", 40, false, Ammo.ARROWS, ItemID.YEW_SHORTBOW),
		weapon("Maple shortbow", 30, false, Ammo.ARROWS, ItemID.MAPLE_SHORTBOW),
		weapon("Willow shortbow", 20, false, Ammo.ARROWS, ItemID.WILLOW_SHORTBOW),
		weapon("Oak shortbow", 5, false, Ammo.ARROWS, ItemID.OAK_SHORTBOW),
		weapon("Shortbow", 1, false, Ammo.ARROWS, ItemID.SHORTBOW)),
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
		ammo("Runite bolts", 61, true, Ammo.BOLTS, ItemID.XBOWS_CROSSBOW_BOLTS_RUNITE),
		ammo("Adamant bolts", 46, true, Ammo.BOLTS, ItemID.XBOWS_CROSSBOW_BOLTS_ADAMANTITE),
		ammo("Amethyst arrows", 50, true, Ammo.ARROWS, ItemID.AMETHYST_ARROW),
		ammo("Rune arrows", 40, false, Ammo.ARROWS, ItemID.RUNE_ARROW),
		ammo("Adamant arrows", 30, false, Ammo.ARROWS, ItemID.ADAMANT_ARROW),
		ammo("Mithril arrows", 20, false, Ammo.ARROWS, ItemID.MITHRIL_ARROW),
		ammo("Steel arrows", 5, false, Ammo.ARROWS, ItemID.STEEL_ARROW),
		ammo("Iron arrows", 1, false, Ammo.ARROWS, ItemID.IRON_ARROW)),
	RANGED_HELM(CombatStyle.RANGED, "helmet",
		tier("Coif", Skill.RANGED, 20, 0, false, ItemID.COIF),
		tier("Leather cowl", null, 0, 0, false, ItemID.LEATHER_COWL)),
	RANGED_BODY(CombatStyle.RANGED, "body",
		tier("Black d'hide body", Skill.RANGED, 70, 40, true, ItemID.BLACK_DRAGONHIDE_BODY),
		tier("Red d'hide body", Skill.RANGED, 60, 40, true, ItemID.RED_DRAGONHIDE_BODY),
		tier("Blue d'hide body", Skill.RANGED, 50, 40, true, ItemID.BLUE_DRAGONHIDE_BODY),
		tier("Green d'hide body", Skill.RANGED, 40, 40, false, ItemID.DRAGONHIDE_BODY),
		tier("Studded body", Skill.RANGED, 20, 20, false, ItemID.STUDDED_BODY),
		tier("Hardleather body", Skill.DEFENCE, 10, 0, false, ItemID.HARDLEATHER_BODY),
		tier("Leather body", Skill.DEFENCE, 1, 0, false, ItemID.LEATHER_ARMOUR)),
	RANGED_LEGS(CombatStyle.RANGED, "legs",
		tier("Black d'hide chaps", Skill.RANGED, 70, 0, true, ItemID.BLACK_DRAGONHIDE_CHAPS),
		tier("Red d'hide chaps", Skill.RANGED, 60, 0, true, ItemID.RED_DRAGONHIDE_CHAPS),
		tier("Blue d'hide chaps", Skill.RANGED, 50, 0, true, ItemID.BLUE_DRAGONHIDE_CHAPS),
		tier("Green d'hide chaps", Skill.RANGED, 40, 0, false, ItemID.DRAGONHIDE_CHAPS),
		tier("Studded chaps", Skill.RANGED, 20, 0, false, ItemID.STUDDED_CHAPS),
		tier("Leather chaps", Skill.RANGED, 1, 0, false, ItemID.LEATHER_CHAPS)),
	RANGED_HANDS(CombatStyle.RANGED, "vambraces",
		tier("Black d'hide vambraces", Skill.RANGED, 70, 0, true, ItemID.BLACK_DRAGON_VAMBRACES),
		tier("Red d'hide vambraces", Skill.RANGED, 60, 0, true, ItemID.RED_DRAGON_VAMBRACES),
		tier("Blue d'hide vambraces", Skill.RANGED, 50, 0, true, ItemID.BLUE_DRAGON_VAMBRACES),
		tier("Green d'hide vambraces", Skill.RANGED, 40, 0, false, ItemID.DRAGON_VAMBRACES),
		tier("Leather vambraces", Skill.RANGED, 1, 0, false, ItemID.LEATHER_VAMBRACES)),
	RANGED_BOOTS(CombatStyle.RANGED, "boots",
		tier("Snakeskin boots", Skill.RANGED, 30, 30, true, ItemID.SNAKESKIN_BOOTS),
		tier("Leather boots", null, 0, 0, false, ItemID.LEATHER_BOOTS)),
	RANGED_AMULET(CombatStyle.RANGED, "amulet",
		tier("Amulet of fury", null, 0, 0, true, ItemID.ENCHANTED_ONYX_AMULET),
		tier("Amulet of glory", null, 0, 0, true, Ids.GLORY),
		tier("Amulet of power", null, 0, 0, false, ItemID.AMULET_OF_POWER));

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
	 * One tier of gear. {@code skill} is null when there's no level requirement; {@code defenceLevel} is an extra
	 * Defence requirement (0 if none). {@code ammo} is what a weapon fires, or what kind of ammo this is.
	 */
	@Value
	public static class Tier
	{
		String name;
		Skill skill;
		int level;
		int defenceLevel;
		boolean members;
		Ammo ammo;
		int[] ids;

		/**
		 * Whether this ammo can be fired from {@code weapon}.
		 */
		public boolean firedFrom(Tier weapon)
		{
			return weapon.ammo != Ammo.NONE && ammo == weapon.ammo && level <= weapon.level;
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
		return new Tier(name, skill, level, defenceLevel, members, Ammo.NONE, ids);
	}

	private static Tier weapon(String name, int level, boolean members, Ammo fires, int... ids)
	{
		return new Tier(name, Skill.RANGED, level, 0, members, fires, ids);
	}

	private static Tier ammo(String name, int level, boolean members, Ammo kind, int... ids)
	{
		return new Tier(name, Skill.RANGED, level, 0, members, kind, ids);
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
