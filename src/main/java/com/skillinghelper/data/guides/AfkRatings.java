package com.skillinghelper.data.guides;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import java.util.HashMap;
import java.util.Map;

/**
 * Roughly how many clicks (inputs) an hour each method takes at an efficient pace, so the guides can show how AFK a
 * method is and the "Most AFK" pick can rank them: a redwood tree needs a click a minute, cleaning herbs one a second.
 * Estimates count the clicks of the loop itself, banking and walking included (e.g. a range-cooking inventory is about
 * 8 clicks for 28 fish). Methods without an entry get a rough guess from their type and tags, so new guides always
 * have one; add an entry here to replace the guess.
 */
final class AfkRatings
{
	private static final Map<String, Integer> CLICKS = new HashMap<>();
	/**
	 * Melee/Ranged/Magic/Hitpoints guides share their monsters, so they're rated by the part after the skill name.
	 */
	private static final Map<String, Integer> COMBAT = new HashMap<>();

	static
	{
		// Combat: one click per kill, or a reset every ~10 minutes for crabs
		COMBAT.put("chickens", 400);
		COMBAT.put("cows", 250);
		COMBAT.put("combat_spells_cows", 250);
		COMBAT.put("al_kharid_warriors", 200);
		COMBAT.put("hill_giants", 150);
		COMBAT.put("gemstone_crab", 20); // follow it through the tunnel when it burrows, every ~10 minutes
		COMBAT.put("sand_crabs", 60);
		COMBAT.put("rock_crabs", 60);
		COMBAT.put("ammonite_crabs", 60);
		COMBAT.put("nmz", 60); // absorption sips and a rock cake every few minutes (wiki: 21 clicks per 20 minutes)
		COMBAT.put("sulphur_naguas", 120); // aggressive, so mostly picking up drops and stepping back
		COMBAT.put("maniacal_monkeys", 60); // wiki AFK guide: a reset every 10 minutes
		COMBAT.put("skeletal_monkeys", 200);
		COMBAT.put("gargoyles", 250);
		COMBAT.put("ogres_cannon", 150);
		COMBAT.put("vyrewatch_sentinels", 20); // wiki AFK guide: up to 17 minutes between clicks
		COMBAT.put("desert_bandits", 20);

		// Agility: one click per obstacle, plus marks of grace
		for (String id : new String[]{"gnome", "draynor", "al_kharid", "varrock", "barbarian", "canifis",
			"shayzien_advanced", "ape_atoll", "falador", "seers", "werewolf", "pollnivneach", "rellekka", "ardougne",
			"wilderness"})
		{
			clicks("agility_" + id, 700);
		}
		clicks("agility_brimhaven", 800);
		clicks("agility_pyramid", 900);
		clicks("agility_wyrm_basic", 360); // wiki AFK guide: 6 clicks per 60-second lap
		clicks("agility_wyrm_advanced", 360);
		clicks("agility_prifddinas", 550);
		clicks("agility_sepulchre", 1200);

		// Construction: build, remove, butler
		clicks("con_crude_chairs", 1300);
		clicks("con_bookcases", 1300);
		clicks("con_oak_chairs", 1500);
		clicks("con_oak_larders", 1800);
		clicks("con_mythical_capes", 1800);
		clicks("con_mahogany_tables", 1500);
		clicks("con_teak_benches", 1500);
		clicks("con_oak_doors", 1500);
		clicks("con_mahogany_homes", 600);

		// Cooking: ~5 clicks per inventory at a range (wiki AFK guide: 1:07 per inventory), ~6 per bank-standing 14
		for (String id : new String[]{"shrimp", "trout", "salmon", "karambwan", "tuna", "lobster", "swordfish",
			"monkfish", "shark", "anglerfish", "dark_crab"})
		{
			clicks("cook_" + id, 300);
		}
		clicks("cook_wine", 1000);
		clicks("cook_pineapple_pizza", 1000);

		// Crafting
		for (String id : new String[]{"glass_beer", "glass_lantern", "glass_oil_lamp", "glass_vial", "glass_orb",
			"glass_lens", "glass_light_orb", "leather_gloves", "leather_body", "hard_leather_body", "cut_sapphire",
			"cut_emerald", "cut_ruby", "cut_diamond", "cut_dragonstone", "amethyst_arrowtips"})
		{
			clicks("craft_" + id, 400);
		}
		for (String id : new String[]{"gold_rings", "gold_amulets", "sapphire_rings", "emerald_rings", "ruby_rings",
			"diamond_rings", "dragonstone_rings", "diamond_amulets", "dragonstone_amulets"})
		{
			clicks("craft_" + id, 500);
		}
		clicks("craft_spin_flax", 350);
		for (String id : new String[]{"water_battlestaff", "earth_battlestaff", "fire_battlestaff", "air_battlestaff"})
		{
			clicks("craft_" + id, 1100);
		}
		for (String id : new String[]{"green_dhide_body", "blue_dhide_body", "red_dhide_body", "black_dhide_body"})
		{
			clicks("craft_" + id, 900);
		}

		// Farming
		clicks("farm_allotments", 600);
		for (String id : new String[]{"oak", "willow", "maple", "yew", "magic"})
		{
			clicks("farm_tree_" + id, 500);
		}
		clicks("farm_herb_runs", 600);
		clicks("farm_tithe", 2500);

		// Firemaking: tinderbox on every log
		for (String id : new String[]{"logs", "oak", "willow", "maple", "yew", "magic"})
		{
			clicks("fm_" + id, 1200);
		}
		clicks("fm_wintertodt", 400);

		// Fishing
		clicks("fish_shrimp_draynor", 300);
		clicks("fish_trawler", 200);
		clicks("fish_fly_barbarian", 400);
		clicks("fish_tempoross", 800);
		clicks("fish_lobster_catherby", 100);
		clicks("fish_aerial_molch", 1500);
		clicks("fish_barbarian_otto", 500);
		clicks("fish_monkfish_piscatoris", 150);
		clicks("fish_sharks_catherby", 80);
		clicks("fish_anglerfish_piscarilius", 80);

		// Fletching: ~5 clicks per inventory when cutting or stringing; darts and bolts need a click per 10
		for (String id : new String[]{"arrow_shafts", "shortbow", "longbow", "oak_short", "oak_long", "willow_short",
			"willow_long", "maple_short", "maple_long", "yew_short", "yew_long", "magic_short", "magic_long"})
		{
			clicks("fletch_" + id, 350);
		}
		for (String id : new String[]{"string_willow_long", "string_maple_long", "string_yew_long",
			"string_magic_long"})
		{
			clicks("fletch_" + id, 900);
		}
		clicks("fletch_headless_arrows", 800);
		clicks("fletch_adamant_arrows", 800);
		clicks("fletch_rune_arrows", 800);
		for (String id : new String[]{"steel_darts", "mithril_darts", "adamant_darts", "rune_darts", "adamant_bolts",
			"runite_bolts"})
		{
			clicks("fletch_" + id, 3000);
		}

		// Herblore: potions ~5 clicks per 14, cleaning ~5 per inventory
		for (String id : new String[]{"attack", "antipoison", "strength", "energy", "prayer", "super_attack",
			"super_antipoison", "super_energy", "super_strength", "super_restore", "super_defence", "antifire",
			"ranging", "magic", "saradomin_brew", "super_combat"})
		{
			clicks("herb_" + id, 900);
		}
		for (String id : new String[]{"guam", "marrentill", "tarromin", "harralander", "ranarr", "toadflax", "irit",
			"avantoe", "kwuarm", "snapdragon", "cadantine", "lantadyme", "dwarf_weed", "torstol"})
		{
			clicks("herb_clean_" + id, 550); // one click cleans the whole inventory (wiki: 34s, 5 clicks)
		}
		clicks("herb_druidic_ritual", 300);

		// Hunter
		clicks("hunter_birdhouses", 400);
		clicks("hunter_crimson_swift", 1200);
		clicks("hunter_tropical_wagtail", 1200);
		clicks("hunter_copper_longtail", 1000);
		clicks("hunter_ruby_harvest", 1000);
		clicks("hunter_feldip_weasels", 1000);
		clicks("hunter_swamp_lizards", 1200);
		clicks("hunter_orange_salamanders", 1200);
		clicks("hunter_red_salamanders", 1200);
		clicks("hunter_black_salamanders", 1200);
		clicks("hunter_jerboas", 1000);
		clicks("hunter_falconry", 1500);
		clicks("hunter_razor_kebbits", 600);
		clicks("hunter_red_chinchompas", 1200);
		clicks("hunter_black_chinchompas", 1200);
		clicks("hunter_herbiboar", 600);

		// Magic: two clicks per cast on an item, one per teleport
		clicks("magic_enchant_opal_bolts", 600);
		// Enchanting jewellery and Plank Make repeat on the whole inventory by themselves (wiki AFK guide)
		clicks("magic_enchant_recoil", 200);
		clicks("magic_enchant_power", 200);
		clicks("magic_enchant_glory", 200);
		clicks("magic_low_alch", 2400);
		clicks("magic_high_alch_magic", 2400);
		clicks("magic_high_alch_yew", 2400);
		clicks("magic_varrock_tele", 1500);
		clicks("magic_falador_tele", 1500);
		clicks("magic_camelot_tele", 1500);
		clicks("magic_superheat_iron", 2200);
		clicks("magic_string_jewellery", 250);
		clicks("magic_plank_make_oak", 240);
		clicks("magic_plank_make_teak", 240);
		clicks("magic_plank_make_mahogany", 240);

		// Mining
		clicks("mine_copper_tin_varrock", 1500);
		clicks("mine_iron_al_kharid", 1800);
		clicks("mine_iron_varrock", 1800);
		clicks("mine_motherlode", 200);
		clicks("mine_gem_shilo", 600);
		clicks("mine_granite_quarry", 1500);
		clicks("mine_amethyst_guild", 60);

		// Prayer
		clicks("prayer_bury_bones", 2000);
		clicks("prayer_bury_big_bones", 2000);
		clicks("prayer_bury_dragon_bones", 2000);
		clicks("prayer_gilded_altar", 300);
		clicks("prayer_chaos_altar", 400);
		clicks("prayer_ectofuntus", 800);

		// Runecraft
		for (String id : new String[]{"air", "mind", "water", "earth", "fire", "body", "cosmic", "astral", "nature"})
		{
			clicks("rc_" + id, 400);
		}
		clicks("rc_ourania", 800); // wiki AFK guide: 25s runs between bursts of ~15 clicks
		clicks("rc_lava", 700);
		clicks("rc_gotr", 1000);
		clicks("rc_blood_arceuus", 1000); // wiki AFK guide: long loop, ~56 clicks between 55s waits
		clicks("rc_soul_arceuus", 1000);

		// Sailing
		clicks("sail_pandemonium", 500);
		// Salvaging: wiki AFK guide, about 30 clicks (sort, drop, re-hook) every time the wreck fills the inventory
		clicks("sail_salvage_small", 180);
		clicks("sail_salvage_fishy", 800);
		clicks("sail_salvage_barracuda", 750);
		clicks("sail_salvage_large", 700);
		clicks("sail_salvage_pirate", 680);
		clicks("sail_salvage_mercenary", 550);
		clicks("sail_salvage_fremennik", 500);
		clicks("sail_salvage_merchant", 450);

		// Slayer
		clicks("slayer_turael", 400);
		clicks("slayer_mazchna", 350);
		clicks("slayer_vannaka", 300);
		clicks("slayer_chaeldar", 300);
		clicks("slayer_konar", 300);
		clicks("slayer_nieve", 300);
		clicks("slayer_duradel", 300);

		// Smithing: furnace ~6 clicks per inventory, anvil ~5 per 5 platebodies
		for (String id : new String[]{"bronze_bars", "iron_bars", "steel_bars", "gold_bars"})
		{
			clicks("smith_" + id, 500);
		}
		for (String id : new String[]{"bronze_daggers", "bronze_platebodies", "iron_platebodies",
			"steel_platebodies", "mithril_platebodies", "adamant_platebodies", "rune_battleaxes"})
		{
			clicks("smith_" + id, 750);
		}
		for (String id : new String[]{"mithril_dart_tips", "adamant_dart_tips", "rune_dart_tips"})
		{
			clicks("smith_" + id, 350);
		}
		clicks("smith_cannonballs", 120); // wiki AFK guide: ~1 minute per inventory with the double mould
		clicks("smith_giants_foundry", 800);
		clicks("smith_blast_furnace_gold", 2000);

		// Thieving
		clicks("thieve_men", 1500);
		clicks("thieve_cake_stall", 1000);
		clicks("thieve_tea_stall", 1000);
		clicks("thieve_fruit_stall", 2000);
		clicks("thieve_pyramid_plunder", 1500);
		clicks("thieve_master_farmer", 1500);
		clicks("thieve_blackjacking", 3000);
		clicks("thieve_artefacts", 200);
		clicks("thieve_rogues_den", 1500);
		clicks("thieve_varlamore_valuables", 300);
		clicks("thieve_ardougne_knights", 2500);
		clicks("thieve_vyres", 2000);
		clicks("thieve_elves", 2000);

		// Woodcutting: fewer clicks the slower the tree
		clicks("wc_trees_varrock", 1500);
		clicks("wc_oaks_varrock", 600);
		clicks("wc_willows_draynor", 600);
		clicks("wc_teak_grove", 600);
		clicks("wc_maples_seers", 200); // wiki AFK guide: a click a minute plus banking
		clicks("wc_mahogany_grove", 200);
		clicks("wc_yews_edgeville", 150);
		clicks("wc_yews_guild", 150);
		clicks("wc_magic_guild", 80);
		clicks("wc_redwood_guild", 40);

		// Methods from the wiki's "Guide:AFK Skilling Methods": 3600 / AFK time x clicks per AFK, from its table
		clicks("wc_infected_roots", 120);
		clicks("wc_engorged_bloodwood", 100);
		clicks("wc_camphor_conch", 150);
		clicks("wc_ironwood_sunbleak", 100);
		clicks("wc_rosewood_drumstick", 80);
		clicks("mine_barronite", 200);
		clicks("mine_daeyalt", 80);
		clicks("mine_infernal_shale", 450);
		clicks("mine_salax_salt", 15);
		clicks("mine_sunstone_monolith", 120);
		clicks("mine_ascension_rocks", 40);
		clicks("fish_karambwan", 120);
		clicks("fish_karambwanji", 5);
		clicks("fish_dark_crabs", 70);
		clicks("fish_sacred_eels", 90);
		clicks("fish_infernal_eels", 90);
		clicks("fish_civitas_keys", 230);
		clicks("agility_bf_pedals", 45);
		clicks("agility_poh_traps", 15);
		clicks("agility_vampyrium_rockslide", 16);
		clicks("strength_bf_pump", 5);
		clicks("thieve_summer_garden", 200);
		clicks("hunter_maniacal_monkeys", 150);
		clicks("hunter_stymphikes", 240);
		clicks("magic_splashing", 5);
		clicks("magic_barrage_monkeys", 40);
		clicks("herb_guam_tar", 460);
		clicks("herb_harralander_tar", 460);
		clicks("herb_irit_tar", 460);
		clicks("herb_stamina", 700);
		clicks("herb_anti_venom", 700);
		clicks("fletch_battlestaves", 370);
		clicks("fletch_redwood_shields", 380);
		clicks("fletch_diamond_bolt_tips", 270);
		for (String log : new String[]{"maple", "yew", "magic", "redwood"})
		{
			clicks("fm_campfire_" + log, 100);
		}
		clicks("craft_fire_pot_lids", 150);
	}

	private AfkRatings()
	{
	}

	private static void clicks(String id, int perHour)
	{
		CLICKS.put(id, perHour);
	}

	static boolean has(String id)
	{
		return CLICKS.containsKey(id) || COMBAT.containsKey(combatPart(id));
	}

	/**
	 * The method with its clicks per hour filled in, from the table or a rough guess.
	 */
	static Method apply(Method m)
	{
		return m.getClicksPerHour() > 0 ? m : m.toBuilder().clicksPerHour(estimate(m)).build();
	}

	static int estimate(Method m)
	{
		Integer listed = CLICKS.get(m.getId());
		if (listed == null)
		{
			listed = COMBAT.get(combatPart(m.getId()));
		}
		if (listed != null)
		{
			return listed;
		}
		// Guesses for methods without an entry
		if (m.hasTag(MethodTag.AFK))
		{
			return 150;
		}
		if (m.getType() == MethodType.PROCESS)
		{
			return m.hasTag(MethodTag.BANKSTANDING) ? 900 : 600;
		}
		return m.getType() == MethodType.ACTIVITY ? 700 : 800;
	}

	/**
	 * "attack_sand_crabs" → "sand_crabs".
	 */
	private static String combatPart(String id)
	{
		for (String prefix : new String[]{"attack_", "strength_", "defence_", "hitpoints_", "ranged_", "magic_"})
		{
			if (id.startsWith(prefix))
			{
				return id.substring(prefix.length());
			}
		}
		return id;
	}
}
