package com.skillinghelper.data.guides;

import com.skillinghelper.data.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Where each method stops being worth it: the level at which a better method of the same kind has opened up, so the
 * panel can show "Lvl 30 → 60" and hide methods the player has outgrown. 99 means it's worth doing all the way.
 * Every method must have an entry (GuideCoverageTest checks), so new methods get a range too.
 */
final class LevelRanges
{
	private static final Map<String, int[]> RANGES = new HashMap<>();

	static
	{
		// Agility
		range("agility_gnome", 10); // 1+ Gnome Stronghold Agility Course
		range("agility_draynor", 20); // 10+ Draynor Village Rooftop
		range("agility_al_kharid", 30); // 20+ Al Kharid Rooftop
		range("agility_brimhaven", 50); // 20+ Brimhaven Agility Arena
		range("agility_pyramid", 99); // 30+ Agility Pyramid
		range("agility_varrock", 40); // 30+ Varrock Rooftop
		range("agility_barbarian", 48); // 35+ Barbarian Outpost Agility Course
		range("agility_canifis", 60); // 40+ Canifis Rooftop
		range("agility_shayzien_advanced", 60); // 45+ Shayzien Advanced Agility Course
		range("agility_ape_atoll", 75); // 48+ Ape Atoll Agility Course
		range("agility_falador", 60); // 50+ Falador Rooftop
		range("agility_wyrm_basic", 62); // 50+ Colossal Wyrm Agility Course (basic)
		range("agility_sepulchre", 99); // 52+ Hallowed Sepulchre
		range("agility_wilderness", 75); // 52+ Wilderness Agility Course
		range("agility_seers", 80); // 60+ Seers' Village Rooftop
		range("agility_werewolf", 80); // 60+ Werewolf Agility Course
		range("agility_wyrm_advanced", 99); // 62+ Colossal Wyrm Agility Course (advanced)
		range("agility_pollnivneach", 80); // 70+ Pollnivneach Rooftop
		range("agility_prifddinas", 99); // 75+ Prifddinas Agility Course
		range("agility_rellekka", 90); // 80+ Rellekka Rooftop
		range("agility_ardougne", 99); // 90+ Ardougne Rooftop
		// Attack
		range("attack_chickens", 10); // 1+ Chickens (Lumbridge)
		range("attack_gemstone_crab", 99); // 1+ Gemstone Crab (Varlamore)
		range("attack_cows", 20); // 10+ Cows (Lumbridge)
		range("attack_al_kharid_warriors", 40); // 20+ Al Kharid warriors
		range("attack_sand_crabs", 70); // 20+ Sand crabs (Hosidius)
		range("attack_hill_giants", 60); // 30+ Hill giants (Edgeville Dungeon)
		range("attack_nmz", 99); // 60+ Nightmare Zone
		// Construction
		range("con_crude_chairs", 4); // 1+ Build crude wooden chairs
		range("con_mahogany_homes", 99); // 1+ Mahogany Homes contracts
		range("con_bookcases", 19); // 4+ Build wooden bookcases
		range("con_oak_chairs", 33); // 19+ Build oak chairs
		range("con_oak_larders", 52); // 33+ Build oak larders
		range("con_mythical_capes", 99); // 50+ Build mounted mythical capes
		range("con_mahogany_tables", 99); // 52+ Build mahogany tables
		range("con_teak_benches", 99); // 66+ Build teak garden benches
		range("con_oak_doors", 99); // 74+ Build oak doors
		// Cooking
		range("cook_shrimp", 15); // 1+ Shrimp (Al Kharid range)
		range("cook_trout", 30); // 15+ Trout (Al Kharid range)
		range("cook_salmon", 40); // 25+ Salmon (Al Kharid range)
		range("cook_karambwan", 99); // 30+ Karambwans (Catherby)
		range("cook_tuna", 40); // 30+ Tuna (Al Kharid range)
		range("cook_wine", 99); // 35+ Jugs of wine (bank-standing)
		range("cook_lobster", 62); // 40+ Lobsters (Al Kharid range)
		range("cook_swordfish", 62); // 45+ Swordfish (Al Kharid range)
		range("cook_monkfish", 80); // 62+ Monkfish (Catherby range)
		range("cook_pineapple_pizza", 99); // 65+ Pineapple pizzas (bank-standing)
		range("cook_shark", 99); // 80+ Sharks (Catherby range)
		range("cook_anglerfish", 99); // 84+ Anglerfish (Catherby range)
		range("cook_dark_crab", 99); // 90+ Dark crabs (Catherby range)
		// Crafting
		range("craft_glass_beer", 4); // 1+ Blow beer glasses
		range("craft_leather_gloves", 14); // 1+ Craft leather gloves
		range("craft_glass_lantern", 12); // 4+ Blow candle lanterns
		range("craft_gold_rings", 20); // 5+ Gold rings (Edgeville furnace)
		range("craft_gold_amulets", 20); // 8+ Gold amulets (u) (Edgeville furnace)
		range("craft_spin_flax", 99); // 10+ Spin flax (Seers' Village)
		range("craft_glass_oil_lamp", 33); // 12+ Blow oil lamps
		range("craft_leather_body", 28); // 14+ Craft leather bodies
		range("craft_cut_sapphire", 27); // 20+ Cut sapphires
		range("craft_sapphire_rings", 27); // 20+ Sapphire rings (Edgeville furnace)
		range("craft_cut_emerald", 34); // 27+ Cut emeralds
		range("craft_emerald_rings", 34); // 27+ Emerald rings (Edgeville furnace)
		range("craft_hard_leather_body", 63); // 28+ Craft hard leather bodies
		range("craft_glass_vial", 46); // 33+ Blow vials
		range("craft_cut_ruby", 43); // 34+ Cut rubies
		range("craft_ruby_rings", 43); // 34+ Ruby rings (Edgeville furnace)
		range("craft_cut_diamond", 55); // 43+ Cut diamonds
		range("craft_diamond_rings", 55); // 43+ Diamond rings (Edgeville furnace)
		range("craft_glass_orb", 87); // 46+ Blow unpowered orbs
		range("craft_glass_lens", 87); // 49+ Blow lantern lenses
		range("craft_water_battlestaff", 58); // 54+ Make water battlestaves
		range("craft_cut_dragonstone", 99); // 55+ Cut dragonstones
		range("craft_dragonstone_rings", 99); // 55+ Dragonstone rings (Edgeville furnace)
		range("craft_earth_battlestaff", 62); // 58+ Make earth battlestaves
		range("craft_fire_battlestaff", 66); // 62+ Make fire battlestaves
		range("craft_green_dhide_body", 71); // 63+ Craft green d'hide bodies
		range("craft_air_battlestaff", 99); // 66+ Make air battlestaves
		range("craft_diamond_amulets", 80); // 70+ Diamond amulets (u) (Edgeville furnace)
		range("craft_blue_dhide_body", 77); // 71+ Craft blue d'hide bodies
		range("craft_red_dhide_body", 84); // 77+ Craft red d'hide bodies
		range("craft_dragonstone_amulets", 99); // 80+ Dragonstone amulets (u) (Edgeville furnace)
		range("craft_black_dhide_body", 99); // 84+ Craft black d'hide bodies
		range("craft_amethyst_arrowtips", 99); // 85+ Cut amethyst into arrowtips
		range("craft_glass_light_orb", 99); // 87+ Blow empty light orbs
		// Defence
		range("defence_chickens", 10); // 1+ Chickens (Lumbridge)
		range("defence_gemstone_crab", 99); // 1+ Gemstone Crab (Varlamore)
		range("defence_cows", 20); // 10+ Cows (Lumbridge)
		range("defence_al_kharid_warriors", 40); // 20+ Al Kharid warriors
		range("defence_sand_crabs", 70); // 20+ Sand crabs (Hosidius)
		range("defence_hill_giants", 60); // 30+ Hill giants (Edgeville Dungeon)
		range("defence_nmz", 99); // 60+ Nightmare Zone
		// Farming
		range("farm_allotments", 32); // 1+ Potatoes in allotments (Falador)
		range("farm_tree_oak", 30); // 15+ Oak tree runs
		range("farm_tree_willow", 45); // 30+ Willow tree runs
		range("farm_herb_runs", 99); // 32+ Herb runs (ranarr)
		range("farm_tithe", 99); // 34+ Tithe Farm
		range("farm_tree_maple", 60); // 45+ Maple tree runs
		range("farm_tree_yew", 75); // 60+ Yew tree runs
		range("farm_tree_magic", 99); // 75+ Magic tree runs
		// Firemaking
		range("fm_logs", 15); // 1+ Logs (Grand Exchange)
		range("fm_oak", 30); // 15+ Oak logs (Grand Exchange)
		range("fm_willow", 45); // 30+ Willow logs (Grand Exchange)
		range("fm_maple", 60); // 45+ Maple logs (Grand Exchange)
		range("fm_wintertodt", 99); // 50+ Wintertodt
		range("fm_yew", 75); // 60+ Yew logs (Grand Exchange)
		range("fm_magic", 99); // 75+ Magic logs (Grand Exchange)
		// Fishing
		range("fish_shrimp_draynor", 20); // 1+ Shrimp & anchovies (Draynor)
		range("fish_trawler", 99); // 15+ Fishing Trawler (Angler outfit)
		range("fish_fly_barbarian", 48); // 20+ Fly fishing (Barbarian Village)
		range("fish_tempoross", 99); // 35+ Tempoross
		range("fish_lobster_catherby", 76); // 40+ Lobsters (Catherby)
		range("fish_aerial_molch", 99); // 43+ Aerial fishing (Lake Molch)
		range("fish_barbarian_otto", 99); // 48+ Barbarian fishing (Otto's Grotto)
		range("fish_monkfish_piscatoris", 82); // 62+ Monkfish (Piscatoris)
		range("fish_sharks_catherby", 99); // 76+ Sharks (Catherby)
		range("fish_anglerfish_piscarilius", 99); // 82+ Anglerfish (Port Piscarilius)
		// Fletching
		range("fletch_arrow_shafts", 5); // 1+ Cut arrow shafts
		range("fletch_headless_arrows", 52); // 1+ Headless arrows
		range("fletch_shortbow", 10); // 5+ Cut shortbows (u)
		range("fletch_longbow", 20); // 10+ Cut longbows (u)
		range("fletch_oak_short", 25); // 20+ Cut oak shortbows (u)
		range("fletch_oak_long", 35); // 25+ Cut oak longbows (u)
		range("fletch_willow_short", 40); // 35+ Cut willow shortbows (u)
		range("fletch_steel_darts", 52); // 37+ Steel darts
		range("fletch_string_willow_long", 55); // 40+ String willow longbows
		range("fletch_willow_long", 50); // 40+ Cut willow longbows (u)
		range("fletch_maple_short", 55); // 50+ Cut maple shortbows (u)
		range("fletch_mithril_darts", 67); // 52+ Mithril darts
		range("fletch_maple_long", 65); // 55+ Cut maple longbows (u)
		range("fletch_string_maple_long", 70); // 55+ String maple longbows
		range("fletch_adamant_arrows", 75); // 60+ Adamant arrows
		range("fletch_adamant_bolts", 69); // 61+ Adamant bolts
		range("fletch_yew_short", 70); // 65+ Cut yew shortbows (u)
		range("fletch_adamant_darts", 81); // 67+ Adamant darts
		range("fletch_runite_bolts", 99); // 69+ Runite bolts
		range("fletch_string_yew_long", 85); // 70+ String yew longbows
		range("fletch_yew_long", 80); // 70+ Cut yew longbows (u)
		range("fletch_rune_arrows", 99); // 75+ Rune arrows
		range("fletch_magic_short", 85); // 80+ Cut magic shortbows (u)
		range("fletch_rune_darts", 99); // 81+ Rune darts
		range("fletch_magic_long", 99); // 85+ Cut magic longbows (u)
		range("fletch_string_magic_long", 99); // 85+ String magic longbows
		// Herblore
		range("herb_druidic_ritual", 3); // 1+ Druidic Ritual (quest)
		range("herb_attack", 12); // 3+ Make attack potions
		range("herb_clean_guam", 20); // 3+ Clean grimy guam leaves
		range("herb_antipoison", 12); // 5+ Make antipoisons
		range("herb_clean_marrentill", 20); // 5+ Clean grimy marrentill
		range("herb_clean_tarromin", 30); // 11+ Clean grimy tarromin
		range("herb_strength", 26); // 12+ Make strength potions
		range("herb_clean_harralander", 40); // 20+ Clean grimy harralander
		range("herb_clean_ranarr", 99); // 25+ Clean grimy ranarr weed
		range("herb_energy", 38); // 26+ Make energy potions
		range("herb_clean_toadflax", 54); // 30+ Clean grimy toadflax
		range("herb_prayer", 99); // 38+ Make prayer potions
		range("herb_clean_irit", 65); // 40+ Clean grimy irit leaves
		range("herb_super_attack", 55); // 45+ Make super attack potions
		range("herb_clean_avantoe", 70); // 48+ Clean grimy avantoe
		range("herb_super_antipoison", 55); // 48+ Make super antipoisons
		range("herb_super_energy", 63); // 52+ Make super energy potions
		range("herb_clean_kwuarm", 75); // 54+ Clean grimy kwuarm
		range("herb_super_strength", 63); // 55+ Make super strength potions
		range("herb_clean_snapdragon", 99); // 59+ Clean grimy snapdragon
		range("herb_super_restore", 99); // 63+ Make super restores
		range("herb_clean_cadantine", 99); // 65+ Clean grimy cadantine
		range("herb_super_defence", 72); // 66+ Make super defence potions
		range("herb_clean_lantadyme", 99); // 67+ Clean grimy lantadyme
		range("herb_antifire", 72); // 69+ Make antifire potions
		range("herb_clean_dwarf_weed", 99); // 70+ Clean grimy dwarf weed
		range("herb_ranging", 81); // 72+ Make ranging potions
		range("herb_clean_torstol", 99); // 75+ Clean grimy torstol
		range("herb_magic", 81); // 76+ Make magic potions
		range("herb_saradomin_brew", 99); // 81+ Make saradomin brews
		range("herb_super_combat", 99); // 90+ Make super combat potions
		// Hitpoints
		range("hitpoints_chickens", 10); // 1+ Chickens (Lumbridge)
		range("hitpoints_gemstone_crab", 99); // 1+ Gemstone Crab (Varlamore)
		range("hitpoints_cows", 20); // 10+ Cows (Lumbridge)
		range("hitpoints_al_kharid_warriors", 40); // 20+ Al Kharid warriors
		range("hitpoints_sand_crabs", 70); // 20+ Sand crabs (Hosidius)
		range("hitpoints_hill_giants", 60); // 30+ Hill giants (Edgeville Dungeon)
		range("hitpoints_nmz", 99); // 60+ Nightmare Zone
		// Hunter
		range("hunter_crimson_swift", 9); // 1+ Crimson swifts (Feldip Hills)
		range("hunter_birdhouses", 99); // 5+ Birdhouse runs (Fossil Island)
		range("hunter_feldip_weasels", 19); // 7+ Feldip weasels (Feldip Hills)
		range("hunter_copper_longtail", 19); // 9+ Copper longtails (Piscatoris)
		range("hunter_ruby_harvest", 29); // 15+ Ruby harvests and copper longtails (Piscatoris)
		range("hunter_tropical_wagtail", 29); // 19+ Tropical wagtails (Feldip Hills)
		range("hunter_swamp_lizards", 47); // 29+ Swamp lizards (Canifis)
		range("hunter_jerboas", 63); // 39+ Embertailed jerboas (Locus Oasis)
		range("hunter_falconry", 63); // 43+ Falconry (Piscatoris)
		range("hunter_orange_salamanders", 59); // 47+ Orange salamanders (Uzer)
		range("hunter_razor_kebbits", 63); // 49+ Razor-backed kebbits (Piscatoris)
		range("hunter_red_salamanders", 67); // 59+ Red salamanders (Ourania)
		range("hunter_red_chinchompas", 99); // 63+ Red chinchompas (Feldip Hills)
		range("hunter_black_salamanders", 99); // 67+ Black salamanders (Wilderness)
		range("hunter_black_chinchompas", 99); // 73+ Black chinchompas (Wilderness)
		range("hunter_herbiboar", 99); // 80+ Herbiboar (Fossil Island)
		// Magic
		range("magic_combat_spells_cows", 55); // 1+ Cows (autocast your best spell)
		range("magic_gemstone_crab", 99); // 1+ Gemstone Crab (autocast your best spell)
		range("magic_enchant_opal_bolts", 25); // 4+ Enchant opal bolts
		range("magic_enchant_recoil", 55); // 7+ Enchant sapphire rings (rings of recoil)
		range("magic_low_alch", 55); // 21+ Low Level Alchemy (yew longbows)
		range("magic_varrock_tele", 37); // 25+ Varrock Teleport (repeat casting)
		range("magic_falador_tele", 45); // 37+ Falador Teleport (repeat casting)
		range("magic_superheat_iron", 75); // 43+ Superheat Item on iron ore
		range("magic_camelot_tele", 99); // 45+ Camelot Teleport (repeat casting)
		range("magic_high_alch_magic", 99); // 55+ High Alchemy (magic longbows)
		range("magic_high_alch_yew", 99); // 55+ High Alchemy (yew longbows)
		range("magic_enchant_power", 68); // 57+ Lvl-4 Enchant (diamond amulets)
		range("magic_enchant_glory", 99); // 68+ Lvl-5 Enchant (dragonstone amulets)
		range("magic_string_jewellery", 99); // 80+ String Jewellery (gold amulets)
		range("magic_plank_make_mahogany", 99); // 86+ Plank Make (mahogany logs)
		range("magic_plank_make_oak", 99); // 86+ Plank Make (oak logs)
		range("magic_plank_make_teak", 99); // 86+ Plank Make (teak logs)
		// Mining
		range("mine_copper_tin_varrock", 15); // 1+ Copper & tin (Varrock south-east)
		range("mine_iron_al_kharid", 75); // 15+ Iron rocks (Al Kharid)
		range("mine_iron_varrock", 75); // 15+ Iron rocks (Varrock south-east)
		range("mine_motherlode", 99); // 30+ Motherlode Mine
		range("mine_gem_shilo", 99); // 40+ Gem rocks (Shilo Village mine)
		range("mine_granite_quarry", 99); // 45+ Granite (Desert Quarry)
		range("mine_amethyst_guild", 99); // 92+ Amethyst (Mining Guild)
		// Prayer
		range("prayer_bury_big_bones", 43); // 1+ Bury big bones
		range("prayer_bury_bones", 15); // 1+ Bury bones
		range("prayer_bury_dragon_bones", 99); // 1+ Bury dragon bones
		range("prayer_chaos_altar", 99); // 1+ Chaos Temple (Wilderness) with dragon bones
		range("prayer_ectofuntus", 99); // 1+ Ectofuntus with dragon bones
		range("prayer_gilded_altar", 99); // 1+ Gilded altar with dragon bones
		// Ranged
		range("ranged_chickens", 10); // 1+ Chickens (Lumbridge)
		range("ranged_gemstone_crab", 99); // 1+ Gemstone Crab (Varlamore)
		range("ranged_cows", 20); // 10+ Cows (Lumbridge)
		range("ranged_al_kharid_warriors", 40); // 20+ Al Kharid warriors
		range("ranged_sand_crabs", 70); // 20+ Sand crabs (Hosidius)
		range("ranged_hill_giants", 60); // 30+ Hill giants (Edgeville Dungeon)
		// Runecraft
		range("rc_air", 14); // 1+ Air runes (Falador)
		range("rc_ourania", 99); // 1+ Ourania Altar (ZMI)
		range("rc_mind", 14); // 2+ Mind runes (Falador)
		range("rc_water", 14); // 5+ Water runes (Lumbridge Swamp)
		range("rc_earth", 14); // 9+ Earth runes (Varrock)
		range("rc_fire", 23); // 14+ Fire runes (Al Kharid)
		range("rc_body", 27); // 20+ Body runes (Edgeville)
		range("rc_lava", 99); // 23+ Lava runes (Fire altar)
		range("rc_cosmic", 44); // 27+ Cosmic runes (Zanaris)
		range("rc_gotr", 99); // 27+ Guardians of the Rift
		range("rc_astral", 99); // 40+ Astral runes (Lunar Isle)
		range("rc_nature", 99); // 44+ Nature runes (Karamja)
		range("rc_blood_arceuus", 99); // 77+ Blood runes (Arceuus, AFK)
		range("rc_soul_arceuus", 99); // 90+ Soul runes (Arceuus, AFK)
		// Sailing
		range("sail_pandemonium", 15); // 1+ Pandemonium (quest) and port tasks
		range("sail_salvage_small", 26); // 15+ Salvage small shipwrecks
		range("sail_salvage_fishy", 35); // 26+ Salvage fishy shipwrecks
		range("sail_salvage_barracuda", 53); // 35+ Salvage barracuda shipwrecks
		range("sail_salvage_large", 99); // 53+ Salvage large shipwrecks
		// Slayer
		combat("slayer_chaeldar", 99, 85); // Tasks from Chaeldar
		range("slayer_konar", 99); // 1+ Tasks from Konar quo Maten
		combat("slayer_mazchna", 99, 40); // Tasks from Mazchna
		range("slayer_nieve", 99); // 1+ Tasks from Nieve
		combat("slayer_turael", 99, 20); // Tasks from Turael
		combat("slayer_vannaka", 99, 70); // Tasks from Vannaka
		range("slayer_duradel", 99); // 50+ Tasks from Duradel
		// Smithing
		range("smith_bronze_bars", 15); // 1+ Smelt bronze bars (Al Kharid furnace)
		range("smith_bronze_daggers", 18); // 1+ Smith bronze daggers (Varrock)
		range("smith_giants_foundry", 99); // 15+ Giants' Foundry
		range("smith_iron_bars", 30); // 15+ Smelt iron bars (Edgeville furnace)
		range("smith_bronze_platebodies", 33); // 18+ Smith bronze platebodies (Varrock)
		range("smith_steel_bars", 40); // 30+ Smelt steel bars (Edgeville furnace)
		range("smith_iron_platebodies", 48); // 33+ Smith iron platebodies (Varrock)
		range("smith_cannonballs", 99); // 35+ Steel cannonballs (Edgeville)
		range("smith_blast_furnace_gold", 99); // 40+ Blast Furnace gold bars
		range("smith_gold_bars", 99); // 40+ Smelt gold bars (Edgeville furnace)
		range("smith_steel_platebodies", 68); // 48+ Smith steel platebodies (Varrock)
		range("smith_mithril_dart_tips", 74); // 54+ Smith mithril dart tips (Varrock)
		range("smith_mithril_platebodies", 88); // 68+ Smith mithril platebodies (Varrock)
		range("smith_adamant_dart_tips", 89); // 74+ Smith adamant dart tips (Varrock)
		range("smith_adamant_platebodies", 99); // 88+ Smith adamant platebodies (Varrock)
		range("smith_rune_dart_tips", 99); // 89+ Smith rune dart tips (Varrock)
		range("smith_rune_battleaxes", 99); // 95+ Smith rune battleaxes (Varrock)
		// Strength
		range("strength_chickens", 10); // 1+ Chickens (Lumbridge)
		range("strength_gemstone_crab", 99); // 1+ Gemstone Crab (Varlamore)
		range("strength_cows", 20); // 10+ Cows (Lumbridge)
		range("strength_al_kharid_warriors", 40); // 20+ Al Kharid warriors
		range("strength_sand_crabs", 70); // 20+ Sand crabs (Hosidius)
		range("strength_hill_giants", 60); // 30+ Hill giants (Edgeville Dungeon)
		range("strength_nmz", 99); // 60+ Nightmare Zone
		// Thieving
		range("thieve_men", 5); // 1+ Pickpocket men (Lumbridge)
		range("thieve_cake_stall", 25); // 5+ Baker's stall (Ardougne)
		range("thieve_tea_stall", 25); // 5+ Tea stall (Varrock)
		range("thieve_pyramid_plunder", 99); // 21+ Pyramid Plunder
		range("thieve_fruit_stall", 55); // 25+ Fruit stalls (Hosidius)
		range("thieve_master_farmer", 99); // 38+ Pickpocket master farmers (Draynor)
		range("thieve_blackjacking", 99); // 45+ Blackjacking (Pollnivneach)
		range("thieve_artefacts", 99); // 49+ Stealing artefacts (Port Piscarilius)
		range("thieve_rogues_den", 99); // 50+ Rogues' Den (Rogue outfit)
		range("thieve_varlamore_valuables", 99); // 50+ Stealing valuables (Civitas illa Fortis)
		range("thieve_ardougne_knights", 99); // 55+ Pickpocket knights of Ardougne
		range("thieve_vyres", 99); // 82+ Pickpocket vyres (Darkmeyer)
		range("thieve_elves", 99); // 85+ Pickpocket elves (Lletya)
		// Woodcutting
		range("wc_trees_varrock", 15); // 1+ Trees (Varrock West)
		range("wc_oaks_varrock", 30); // 15+ Oak trees (Varrock West)
		range("wc_willows_draynor", 60); // 30+ Willow trees (Draynor)
		range("wc_teak_grove", 99); // 35+ Teak trees (Hardwood Grove)
		range("wc_maples_seers", 75); // 45+ Maple trees (Seers' Village)
		range("wc_mahogany_grove", 99); // 50+ Mahogany trees (Hardwood Grove)
		range("wc_yews_edgeville", 99); // 60+ Yew trees (Edgeville)
		range("wc_yews_guild", 99); // 60+ Yew trees (Woodcutting Guild)
		range("wc_magic_guild", 99); // 75+ Magic trees (Woodcutting Guild)
		range("wc_redwood_guild", 99); // 90+ Redwood trees (Woodcutting Guild)
	}

	private LevelRanges()
	{
	}

	private static void range(String id, int maxLevel)
	{
		RANGES.put(id, new int[]{maxLevel, 0});
	}

	/**
	 * For Slayer masters, whose tasks are outgrown by combat level once a better master gives tasks.
	 */
	private static void combat(String id, int maxLevel, int maxCombatLevel)
	{
		RANGES.put(id, new int[]{maxLevel, maxCombatLevel});
	}

	static boolean has(String id)
	{
		return RANGES.containsKey(id);
	}

	/**
	 * The method with its range filled in, or unchanged (worth it to 99) when it has no entry.
	 */
	static Method apply(Method m)
	{
		int[] r = RANGES.get(m.getId());
		return r == null ? m : m.toBuilder().maxLevel(r[0]).maxCombatLevel(r[1]).build();
	}
}
