package com.skillinghelper.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Weapon and armour tiers per equipment slot, best first. Used to suggest the best gear the player owns (to wear) or
 * could buy on the GE. Only gear without quest requirements is listed, so every suggestion can actually be worn.
 */
@Getter
public enum CombatGear
{
	MELEE_WEAPON(CombatStyle.MELEE, "weapon",
		tier("Abyssal whip", Skill.ATTACK, 70, 0, true, ItemID.ABYSSAL_WHIP),
		tier("Rune scimitar", Skill.ATTACK, 40, 0, false, ItemID.RUNE_SCIMITAR),
		tier("Adamant scimitar", Skill.ATTACK, 30, 0, false, ItemID.ADAMANT_SCIMITAR),
		tier("Mithril scimitar", Skill.ATTACK, 20, 0, false, ItemID.MITHRIL_SCIMITAR),
		tier("Black scimitar", Skill.ATTACK, 10, 0, false, ItemID.BLACK_SCIMITAR),
		tier("Steel scimitar", Skill.ATTACK, 5, 0, false, ItemID.STEEL_SCIMITAR),
		tier("Iron scimitar", Skill.ATTACK, 1, 0, false, ItemID.IRON_SCIMITAR)),
	MELEE_HELM(CombatStyle.MELEE, "helmet",
		tier("Rune full helm", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_FULL_HELM),
		tier("Adamant full helm", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_FULL_HELM),
		tier("Mithril full helm", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_FULL_HELM),
		tier("Black full helm", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_FULL_HELM),
		tier("Steel full helm", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_FULL_HELM),
		tier("Iron full helm", Skill.DEFENCE, 1, 0, false, ItemID.IRON_FULL_HELM)),
	MELEE_BODY(CombatStyle.MELEE, "body",
		tier("Rune chainbody", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_CHAINBODY),
		tier("Adamant platebody", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_PLATEBODY),
		tier("Mithril platebody", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_PLATEBODY),
		tier("Black platebody", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_PLATEBODY),
		tier("Steel platebody", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_PLATEBODY),
		tier("Iron platebody", Skill.DEFENCE, 1, 0, false, ItemID.IRON_PLATEBODY)),
	MELEE_LEGS(CombatStyle.MELEE, "legs",
		tier("Rune platelegs", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_PLATELEGS),
		tier("Adamant platelegs", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_PLATELEGS),
		tier("Mithril platelegs", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_PLATELEGS),
		tier("Black platelegs", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_PLATELEGS),
		tier("Steel platelegs", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_PLATELEGS),
		tier("Iron platelegs", Skill.DEFENCE, 1, 0, false, ItemID.IRON_PLATELEGS)),
	MELEE_SHIELD(CombatStyle.MELEE, "shield",
		tier("Rune kiteshield", Skill.DEFENCE, 40, 0, false, ItemID.RUNE_KITESHIELD),
		tier("Adamant kiteshield", Skill.DEFENCE, 30, 0, false, ItemID.ADAMANT_KITESHIELD),
		tier("Mithril kiteshield", Skill.DEFENCE, 20, 0, false, ItemID.MITHRIL_KITESHIELD),
		tier("Black kiteshield", Skill.DEFENCE, 10, 0, false, ItemID.BLACK_KITESHIELD),
		tier("Steel kiteshield", Skill.DEFENCE, 5, 0, false, ItemID.STEEL_KITESHIELD),
		tier("Iron kiteshield", Skill.DEFENCE, 1, 0, false, ItemID.IRON_KITESHIELD)),
	RANGED_WEAPON(CombatStyle.RANGED, "bow",
		tier("Magic shortbow", Skill.RANGED, 50, 0, true, ItemID.MAGIC_SHORTBOW),
		tier("Yew shortbow", Skill.RANGED, 40, 0, false, ItemID.YEW_SHORTBOW),
		tier("Maple shortbow", Skill.RANGED, 30, 0, false, ItemID.MAPLE_SHORTBOW),
		tier("Willow shortbow", Skill.RANGED, 20, 0, false, ItemID.WILLOW_SHORTBOW),
		tier("Oak shortbow", Skill.RANGED, 5, 0, false, ItemID.OAK_SHORTBOW),
		tier("Shortbow", Skill.RANGED, 1, 0, false, ItemID.SHORTBOW)),
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
		tier("Leather vambraces", Skill.RANGED, 1, 0, false, ItemID.LEATHER_VAMBRACES));

	/**
	 * One tier of gear. {@code defenceLevel} is an extra Defence requirement (0 if none).
	 */
	@Value
	public static class Tier
	{
		String name;
		Skill skill;
		int level;
		int defenceLevel;
		boolean members;
		int[] ids;
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
		return new Tier(name, skill, level, defenceLevel, members, ids);
	}
}
