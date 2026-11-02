package com.skillinghelper.data;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * Standard spellbook combat spells to autocast for Magic training, best first. Each element beats the one before it
 * at the same tier, and the next tier's wind spell beats the last tier's fire spell (Wind Wave hits harder than Fire
 * Blast). A magic training method lists the {@link #CATALYST_INPUT}, {@link #AIR_INPUT} and {@link #ELEMENT_INPUT}
 * placeholders, and the guide turns them into the runes of the best spell the player can cast.
 */
@Getter
@AllArgsConstructor
public enum CombatSpell
{
	FIRE_SURGE("Fire Surge", 95, 50.5, 24, true, Catalyst.WRATH, 7, Element.FIRE, 10),
	EARTH_SURGE("Earth Surge", 90, 48.5, 23, true, Catalyst.WRATH, 7, Element.EARTH, 10),
	WATER_SURGE("Water Surge", 85, 46.5, 22, true, Catalyst.WRATH, 7, Element.WATER, 10),
	WIND_SURGE("Wind Surge", 81, 44.5, 21, true, Catalyst.WRATH, 7, null, 0),
	FIRE_WAVE("Fire Wave", 75, 42.5, 20, true, Catalyst.BLOOD, 5, Element.FIRE, 7),
	EARTH_WAVE("Earth Wave", 70, 40, 19, true, Catalyst.BLOOD, 5, Element.EARTH, 7),
	WATER_WAVE("Water Wave", 65, 37.5, 18, true, Catalyst.BLOOD, 5, Element.WATER, 7),
	WIND_WAVE("Wind Wave", 62, 36, 17, true, Catalyst.BLOOD, 5, null, 0),
	FIRE_BLAST("Fire Blast", 59, 34.5, 16, false, Catalyst.DEATH, 4, Element.FIRE, 5),
	EARTH_BLAST("Earth Blast", 53, 31.5, 15, false, Catalyst.DEATH, 3, Element.EARTH, 4),
	WATER_BLAST("Water Blast", 47, 28.5, 14, false, Catalyst.DEATH, 3, Element.WATER, 3),
	WIND_BLAST("Wind Blast", 41, 25.5, 13, false, Catalyst.DEATH, 3, null, 0),
	FIRE_BOLT("Fire Bolt", 35, 22.5, 12, false, Catalyst.CHAOS, 3, Element.FIRE, 4),
	EARTH_BOLT("Earth Bolt", 29, 19.5, 11, false, Catalyst.CHAOS, 2, Element.EARTH, 3),
	WATER_BOLT("Water Bolt", 23, 16.5, 10, false, Catalyst.CHAOS, 2, Element.WATER, 2),
	WIND_BOLT("Wind Bolt", 17, 13.5, 9, false, Catalyst.CHAOS, 2, null, 0),
	FIRE_STRIKE("Fire Strike", 13, 11.5, 8, false, Catalyst.MIND, 2, Element.FIRE, 3),
	EARTH_STRIKE("Earth Strike", 9, 9.5, 6, false, Catalyst.MIND, 1, Element.EARTH, 2),
	WATER_STRIKE("Water Strike", 5, 7.5, 4, false, Catalyst.MIND, 1, Element.WATER, 1),
	WIND_STRIKE("Wind Strike", 1, 5.5, 2, false, Catalyst.MIND, 1, null, 0);

	public static final int[] AIR_STAVES = Staves.AIR;
	public static final int[] FIRE_STAVES = Staves.FIRE;
	public static final int[] WATER_STAVES = Staves.WATER;
	public static final int[] EARTH_STAVES = Staves.EARTH;

	/**
	 * In a holder class so {@link Element}'s constants can use them while this enum is still initialising.
	 */
	private static final class Staves
	{
		static final int[] AIR = {ItemID.STAFF_OF_AIR, ItemID.AIR_BATTLESTAFF, ItemID.MYSTIC_AIR_STAFF,
			ItemID.SMOKE_BATTLESTAFF, ItemID.MIST_BATTLESTAFF, ItemID.DUST_BATTLESTAFF};
		static final int[] FIRE = {ItemID.STAFF_OF_FIRE, ItemID.FIRE_BATTLESTAFF, ItemID.MYSTIC_FIRE_STAFF,
			ItemID.LAVA_BATTLESTAFF, ItemID.SMOKE_BATTLESTAFF, ItemID.STEAM_BATTLESTAFF, ItemID.TOME_OF_FIRE};
		static final int[] WATER = {ItemID.STAFF_OF_WATER, ItemID.WATER_BATTLESTAFF, ItemID.MYSTIC_WATER_STAFF,
			ItemID.MUD_BATTLESTAFF, ItemID.STEAM_BATTLESTAFF, ItemID.MIST_BATTLESTAFF};
		static final int[] EARTH = {ItemID.STAFF_OF_EARTH, ItemID.EARTH_BATTLESTAFF, ItemID.MYSTIC_EARTH_STAFF,
			ItemID.LAVA_BATTLESTAFF, ItemID.MYSTIC_LAVA_STAFF, ItemID.MUD_BATTLESTAFF, ItemID.MYSTIC_MUD_STAFF,
			ItemID.DUST_BATTLESTAFF, ItemID.MYSTIC_DUST_BATTLESTAFF};
	}

	@Getter
	@AllArgsConstructor
	public enum Catalyst
	{
		MIND("Mind runes", ItemID.MINDRUNE),
		CHAOS("Chaos runes", ItemID.CHAOSRUNE),
		DEATH("Death runes", ItemID.DEATHRUNE),
		BLOOD("Blood runes", ItemID.BLOODRUNE),
		WRATH("Wrath runes", ItemID.WRATHRUNE);

		private final String runeName;
		private final int rune;
	}

	/**
	 * The second elemental rune of water, earth and fire spells (wind spells only use air runes).
	 */
	@Getter
	@AllArgsConstructor
	public enum Element
	{
		WATER("Water runes", ItemID.WATERRUNE, "Staff of water", Staves.WATER),
		EARTH("Earth runes", ItemID.EARTHRUNE, "Staff of earth", Staves.EARTH),
		FIRE("Fire runes", ItemID.FIRERUNE, "Staff of fire", Staves.FIRE);

		private final String runeName;
		private final int rune;
		private final String staffName;
		private final int[] staves;
	}

	/**
	 * The spell's catalyst rune, lowest spell first; the guide narrows it to the one rune the player's spell uses.
	 */
	public static final ItemReq CATALYST_INPUT = ItemReq.stack("Spell runes", 1, ItemID.MINDRUNE, ItemID.CHAOSRUNE,
		ItemID.DEATHRUNE, ItemID.BLOODRUNE, ItemID.WRATHRUNE);
	public static final ItemReq AIR_INPUT = ItemReq.stack("Air runes", 1, ItemID.AIRRUNE)
		.orUse("Staff of air", AIR_STAVES);
	/**
	 * The spell's water, earth or fire runes. Dropped for wind spells.
	 */
	public static final ItemReq ELEMENT_INPUT = ItemReq.stack("Elemental runes", 1, ItemID.WATERRUNE,
		ItemID.EARTHRUNE, ItemID.FIRERUNE).orUse("Elemental staff", allStaves());

	private final String spellName;
	private final int level;
	private final double baseXp;
	private final int maxHit;
	private final boolean members;
	private final Catalyst catalyst;
	private final int air;
	private final Element element;
	private final int elementRunes;

	/**
	 * The best spell at this Magic level.
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
		return WIND_STRIKE;
	}

	private static int[] allStaves()
	{
		return Stream.of(Staves.WATER, Staves.EARTH, Staves.FIRE).flatMapToInt(IntStream::of).distinct().toArray();
	}
}
