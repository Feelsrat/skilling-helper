package com.skillinghelper.collection.data;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Which collection log pages (and pets) already have a step-by-step guide elsewhere in this plugin pack. The guide
 * there does the walking, the bank trip and the clicking; this plugin only says which slots are worth going for.
 * ClogDataTest checks every target still exists.
 */
public final class ClogLinks
{
	private static final Map<String, GuideLink> ENTRIES = new HashMap<>();
	/**
	 * Pets whose page has no guide of its own (or isn't a page at all), by pet name as on the "All Pets" page.
	 */
	private static final Map<String, GuideLink> PETS = new HashMap<>();

	static
	{
		// PvM Progression ladder
		pvm("Obor", "obor");
		pvm("Bryophyta", "bryophyta");
		pvm("Scurrius", "scurrius");
		pvm("Pest Control", "pest_control");
		pvm("Giant Mole", "giant_mole");
		pvm("Barrows Chests", "barrows");
		pvm("Barbarian Assault", "barbarian_assault");
		pvm("Sarachnis", "sarachnis");
		pvm("King Black Dragon", "king_black_dragon");
		pvm("Moons of Peril", "moons_of_peril");
		pvm("Kalphite Queen", "kalphite_queen");
		pvm("Zulrah", "zulrah");
		pvm("The Fight Caves", "fight_caves");
		pvm("Vorkath", "vorkath");
		pvm("General Graardor", "god_wars");
		pvm("Commander Zilyana", "god_wars");
		pvm("Kree'arra", "god_wars");
		pvm("K'ril Tsutsaroth", "god_wars");
		pvm("The Gauntlet", "gauntlet");
		pvm("Tombs of Amascut", "tombs_of_amascut");
		pvm("Chambers of Xeric", "chambers_of_xeric");
		pvm("Theatre of Blood", "theatre_of_blood");
		pvm("Fortis Colosseum", "fortis_colosseum");
		pvm("The Inferno", "inferno");
		// The defenders come from the cyclopes in the Warriors' Guild
		pvm("Cyclopes", "warriors_guild");

		// Skilling Helper methods
		method("Tempoross", "fish_tempoross");
		method("Wintertodt", "fm_wintertodt");
		method("Giants' Foundry", "smith_giants_foundry");
		method("Motherlode Mine", "mine_motherlode");
		method("Camdozaal", "mine_barronite");
		method("Fishing Trawler", "fish_trawler");
		method("Aerial Fishing", "fish_aerial_molch");
		method("Brimhaven Agility Arena", "agility_brimhaven");
		method("Hallowed Sepulchre", "agility_sepulchre");
		method("Rogues' Den", "thieve_rogues_den");
		method("Tithe Farm", "farm_tithe");
		method("Guardians of the Rift", "rc_gotr");
		method("Mahogany Homes", "con_mahogany_homes");
		method("Monkey Backpacks", "agility_ape_atoll");
		method("Colossal Wyrm Agility", "agility_wyrm_basic");

		// Whole-skill guides
		ENTRIES.put("Rooftop Agility", GuideLink.skill(Skill.AGILITY));
		ENTRIES.put("Forestry", GuideLink.skill(Skill.WOODCUTTING));
		ENTRIES.put("Hunter Guild", GuideLink.skill(Skill.HUNTER));
		ENTRIES.put("Slayer", GuideLink.skill(Skill.SLAYER));

		// Skilling pets: a chance on every action of the skill, higher the higher your level
		PETS.put("Heron", GuideLink.skill(Skill.FISHING));
		PETS.put("Rock golem", GuideLink.skill(Skill.MINING));
		PETS.put("Beaver", GuideLink.skill(Skill.WOODCUTTING));
		PETS.put("Baby chinchompa", GuideLink.skill(Skill.HUNTER));
		PETS.put("Giant Squirrel", GuideLink.skill(Skill.AGILITY));
		PETS.put("Tangleroot", GuideLink.skill(Skill.FARMING));
		PETS.put("Rocky", GuideLink.skill(Skill.THIEVING));
		PETS.put("Rift guardian", GuideLink.skill(Skill.RUNECRAFT));
		PETS.put("Soup", GuideLink.skill(Skill.SAILING));
		PETS.put("Herbi", GuideLink.method("hunter_herbiboar"));
	}

	private ClogLinks()
	{
	}

	private static void pvm(String entry, String id)
	{
		ENTRIES.put(entry, GuideLink.pvm(id));
	}

	private static void method(String entry, String id)
	{
		ENTRIES.put(entry, GuideLink.method(id));
	}

	/**
	 * The guide for a page, or null.
	 */
	public static GuideLink of(ClogEntry e)
	{
		return ENTRIES.get(e.getName());
	}

	/**
	 * The guide for a pet that has one of its own (skilling pets, Herbi), or null.
	 */
	public static GuideLink petLink(String petName)
	{
		return PETS.get(petName);
	}

	/**
	 * The guide for one slot: a skilling pet's skill, else the guide of the first page it's on that has one.
	 */
	public static GuideLink of(ClogItem item, ClogData data)
	{
		GuideLink pet = PETS.get(item.getName());
		if (pet != null)
		{
			return pet;
		}
		for (ClogEntry e : data.entriesOf(item.getId()))
		{
			GuideLink l = ENTRIES.get(e.getName());
			if (l != null)
			{
				return l;
			}
		}
		return null;
	}

	static Map<String, GuideLink> entries()
	{
		return ENTRIES;
	}

	static Map<String, GuideLink> pets()
	{
		return PETS;
	}
}
