package com.skillinghelper.money;

import java.util.Map;

/**
 * Which wiki money making guides the other plugins in this jar have a click-by-click guide for.
 */
public final class FullGuides
{
	/**
	 * Money making guide pages that PvM Progression has a full guide for, by page name prefix.
	 */
	static final Map<String, String> PVM_GUIDES = Map.ofEntries(
		Map.entry("Money making guide/Barrows", "barrows"),
		Map.entry("Money making guide/Killing Bryophyta", "bryophyta"),
		Map.entry("Money making guide/Killing Scurrius", "scurrius"),
		Map.entry("Money making guide/Killing the Giant Mole", "giant_mole"),
		Map.entry("Money making guide/Killing Sarachnis", "sarachnis"),
		Map.entry("Money making guide/Killing the King Black Dragon", "king_black_dragon"),
		Map.entry("Money making guide/Moons of Peril", "moons_of_peril"),
		Map.entry("Money making guide/Killing the Kalphite Queen", "kalphite_queen"),
		Map.entry("Money making guide/Killing Zulrah", "zulrah"),
		Map.entry("Money making guide/Killing Vorkath", "vorkath"),
		Map.entry("Money making guide/Killing General Graardor", "god_wars"),
		Map.entry("Money making guide/Killing Commander Zilyana", "god_wars"),
		Map.entry("Money making guide/Killing Kree'arra", "god_wars"),
		Map.entry("Money making guide/Completing The Gauntlet", "gauntlet"),
		Map.entry("Money making guide/Completing The Corrupted Gauntlet", "gauntlet"),
		Map.entry("Money making guide/Tombs of Amascut", "tombs_of_amascut"),
		Map.entry("Money making guide/Chambers of Xeric", "chambers_of_xeric"),
		Map.entry("Money making guide/Theatre of Blood", "theatre_of_blood"),
		Map.entry("Money making guide/Completing the Fortis Colosseum", "fortis_colosseum"),
		Map.entry("Money making guide/Barbarian assault", "barbarian_assault"));

	private FullGuides()
	{
	}

	/**
	 * The PvM Progression ladder entry with a full guide for this money making page, or null.
	 */
	public static String pvmContent(String page)
	{
		for (Map.Entry<String, String> e : PVM_GUIDES.entrySet())
		{
			if (page.startsWith(e.getKey()))
			{
				return e.getValue();
			}
		}
		return null;
	}
}
