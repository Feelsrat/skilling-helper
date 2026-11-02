package com.skillinghelper.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * Fire spells to autocast for combat Magic training, best first. A magic training method lists the
 * {@link #CATALYST_INPUT}, {@link #AIR_INPUT} and {@link #FIRE_INPUT} placeholders, and the guide turns them into the
 * runes of the best spell the player can cast.
 */
@Getter
@AllArgsConstructor
public enum CombatSpell
{
	FIRE_SURGE("Fire Surge", 95, 50.5, 24, true, "Wrath runes", ItemID.WRATHRUNE, 7, 10),
	FIRE_WAVE("Fire Wave", 75, 42.5, 20, true, "Blood runes", ItemID.BLOODRUNE, 5, 7),
	FIRE_BLAST("Fire Blast", 59, 34.5, 16, false, "Death runes", ItemID.DEATHRUNE, 4, 5),
	FIRE_BOLT("Fire Bolt", 35, 22.5, 12, false, "Chaos runes", ItemID.CHAOSRUNE, 3, 4),
	FIRE_STRIKE("Fire Strike", 13, 11.5, 8, false, "Mind runes", ItemID.MINDRUNE, 2, 3);

	public static final int[] AIR_STAVES = {ItemID.STAFF_OF_AIR, ItemID.AIR_BATTLESTAFF, ItemID.MYSTIC_AIR_STAFF,
		ItemID.SMOKE_BATTLESTAFF, ItemID.MIST_BATTLESTAFF, ItemID.DUST_BATTLESTAFF};
	public static final int[] FIRE_STAVES = {ItemID.STAFF_OF_FIRE, ItemID.FIRE_BATTLESTAFF, ItemID.MYSTIC_FIRE_STAFF,
		ItemID.LAVA_BATTLESTAFF, ItemID.SMOKE_BATTLESTAFF, ItemID.STEAM_BATTLESTAFF, ItemID.TOME_OF_FIRE};

	/**
	 * The spell's catalyst rune (mind, chaos, death, blood or wrath), lowest spell first; the guide narrows it to the
	 * one rune the player's spell uses.
	 */
	public static final ItemReq CATALYST_INPUT = ItemReq.stack("Spell runes", 1, ItemID.MINDRUNE, ItemID.CHAOSRUNE,
		ItemID.DEATHRUNE, ItemID.BLOODRUNE, ItemID.WRATHRUNE);
	public static final ItemReq AIR_INPUT = ItemReq.stack("Air runes", 1, ItemID.AIRRUNE)
		.orUse("Staff of air", AIR_STAVES);
	public static final ItemReq FIRE_INPUT = ItemReq.stack("Fire runes", 1, ItemID.FIRERUNE)
		.orUse("Staff of fire", FIRE_STAVES);

	private final String spellName;
	private final int level;
	private final double baseXp;
	private final int maxHit;
	private final boolean members;
	private final String catalystName;
	private final int catalyst;
	private final int air;
	private final int fire;

	/**
	 * The best spell at this Magic level, or null below Fire Strike.
	 */
	public static CombatSpell best(int magicLevel, boolean members)
	{
		for (CombatSpell spell : values())
		{
			if (magicLevel >= spell.level && (members || !spell.members))
			{
				return spell;
			}
		}
		return null;
	}
}
