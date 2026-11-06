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

		// Minigame Guides (with the Skilling Helper method that has the same requirements)
		minigame("Tempoross", "tempoross", "fish_tempoross");
		minigame("Wintertodt", "wintertodt", "fm_wintertodt");
		minigame("Giants' Foundry", "giants_foundry", "smith_giants_foundry");
		minigame("Motherlode Mine", "motherlode", "mine_motherlode");
		minigame("Fishing Trawler", "fishing_trawler", "fish_trawler");
		minigame("Brimhaven Agility Arena", "brimhaven_arena", "agility_brimhaven");
		minigame("Hallowed Sepulchre", "hallowed_sepulchre", "agility_sepulchre");
		minigame("Rogues' Den", "rogues_den", "thieve_rogues_den");
		minigame("Tithe Farm", "tithe_farm", "farm_tithe");
		minigame("Guardians of the Rift", "gotr", "rc_gotr");
		minigame("Mahogany Homes", "mahogany_homes", "con_mahogany_homes");
		minigame("Castle Wars", "castle_wars", null);
		minigame("Soul Wars", "soul_wars", null);
		minigame("Magic Training Arena", "mage_training_arena", null);
		minigame("Volcanic Mine", "volcanic_mine", null);
		minigame("Mastering Mixology", "mastering_mixology", null);
		minigame("Vale Totems", "vale_totems", null);

		// Skilling Helper methods
		method("Camdozaal", "mine_barronite");
		method("Aerial Fishing", "fish_aerial_molch");
		method("Monkey Backpacks", "agility_ape_atoll");
		method("Colossal Wyrm Agility", "agility_wyrm_basic");

		// Clue Scroll Guide: each tier's view covers its uniques, rare and shared rewards and scroll case milestones
		for (String tier : new String[]{"Beginner", "Easy", "Medium", "Hard", "Elite", "Master"})
		{
			ENTRIES.put(tier + " Treasure Trails", GuideLink.clue(tier.toLowerCase()));
		}
		ENTRIES.put("Hard Treasure Trail Rewards (Rare)", GuideLink.clue("hard"));
		ENTRIES.put("Elite Treasure Trail Rewards (Rare)", GuideLink.clue("elite"));
		ENTRIES.put("Master Treasure Trail Rewards (Rare)", GuideLink.clue("master"));
		ENTRIES.put("Shared Treasure Trail Rewards", GuideLink.clue("master"));
		ENTRIES.put("Scroll Cases", GuideLink.clue("master"));

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

	private static void minigame(String entry, String id, String requirementsOf)
	{
		ENTRIES.put(entry, GuideLink.minigame(id, requirementsOf));
	}

	/**
	 * Clog pages whose boss the Combat Achievements list names differently.
	 */
	private static final Map<String, String> CA_BOSS = Map.ofEntries(
		Map.entry("Barrows Chests", "Barrows"),
		Map.entry("Callisto and Artio", "Callisto"),
		Map.entry("Venenatis and Spindel", "Venenatis"),
		Map.entry("Vet'ion and Calvar'ion", "Vet'ion"),
		Map.entry("Dagannoth Kings", "Dagannoth Rex"),
		Map.entry("The Fight Caves", "TzTok-Jad"),
		Map.entry("The Inferno", "TzKal-Zuk"),
		Map.entry("The Gauntlet", "Crystalline Hunllef"),
		Map.entry("The Leviathan", "Leviathan"),
		Map.entry("The Mad Angel", "Mad Angel"),
		Map.entry("The Whisperer", "Whisperer"),
		Map.entry("Tormented Demons", "Tormented Demon"),
		Map.entry("Glough's Experiments", "Demonic Gorilla"));

	/**
	 * The monster name the Combat Achievements list uses for this page's boss, or null when it has no combat
	 * tasks. {@code caMonsters} is every monster name in that list, lower case.
	 */
	public static String caBoss(ClogEntry e, java.util.Set<String> caMonsters)
	{
		String name = CA_BOSS.getOrDefault(e.getName(), e.getName());
		return caMonsters.contains(name.toLowerCase()) ? name : null;
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
