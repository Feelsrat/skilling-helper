package com.skillinghelper.banked.data;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import com.skillinghelper.data.guides.GuideRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Every banked item the guide knows how to turn into xp, and the ways to do it. Xp per item and levels are the
 * OSRS wiki's; actions per hour are rough rates including banking, only used for the time estimate.
 */
public final class BankedActivities
{
	public static final Location CATHERBY_RANGE = Location.of("Catherby range", 2817, 3443, 3);
	public static final Location AL_KHARID_RANGE = Location.of("Al Kharid range", 3273, 3180, 3);
	private static final Location EDGEVILLE_FURNACE = Location.of("Edgeville furnace", 3109, 3499, 4);
	private static final Location VARROCK_ANVIL = Location.of("Varrock West anvils", 3188, 3425, 4);
	private static final Location SAWMILL = Location.of("Varrock sawmill", 3302, 3491, 5);
	private static final Location HOUSE_PORTAL = Location.of("Rimmington house portal", 2954, 3224, 8);

	private static final ItemReq KNIFE = ItemReq.tool("Knife", ItemID.KNIFE);
	private static final ItemReq CHISEL = ItemReq.tool("Chisel", ItemID.CHISEL);
	private static final ItemReq NEEDLE = ItemReq.tool("Needle", ItemID.NEEDLE);
	private static final ItemReq THREAD = ItemReq.tool("Thread", ItemID.THREAD);
	private static final ItemReq PIPE = ItemReq.tool("Glassblowing pipe", ItemID.GLASSBLOWINGPIPE);
	private static final ItemReq HAMMER = ItemReq.tool("Hammer", ItemID.HAMMER);
	private static final ItemReq SAW = ItemReq.tool("Saw", ItemID.POH_SAW);
	private static final ItemReq TINDERBOX = ItemReq.tool("Tinderbox", ItemID.TINDERBOX);

	private static final List<BankedActivity> ALL = build();
	private static final Map<String, BankedActivity> BY_ID = index(ALL);

	private BankedActivities()
	{
	}

	public static List<BankedActivity> all()
	{
		return ALL;
	}

	public static BankedActivity byId(String id)
	{
		return BY_ID.get(id);
	}

	private static Map<String, BankedActivity> index(List<BankedActivity> all)
	{
		Map<String, BankedActivity> byId = new LinkedHashMap<>();
		for (BankedActivity a : all)
		{
			if (byId.put(a.getId(), a) != null)
			{
				throw new IllegalStateException("Duplicate banked activity " + a.getId());
			}
		}
		return Collections.unmodifiableMap(byId);
	}

	private static List<BankedActivity> build()
	{
		List<BankedActivity> a = new ArrayList<>();
		prayer(a);
		herblore(a);
		cooking(a);
		firemakingAndFletching(a);
		crafting(a);
		smithing(a);
		construction(a);
		fromSkillGuides(a);
		return Collections.unmodifiableList(a);
	}

	// ---- Prayer ----

	private static void prayer(List<BankedActivity> a)
	{
		bury(a, "Bones", ItemID.BONES, ItemID.Cert.BONES, 4.5, false);
		bury(a, "Wolf bones", ItemID.WOLF_BONES, ItemID.Cert.WOLF_BONES, 4.5, true);
		bury(a, "Bat bones", ItemID.BAT_BONES, ItemID.Cert.BAT_BONES, 5.3, true);
		bury(a, "Big bones", ItemID.BIG_BONES, ItemID.Cert.BIG_BONES, 15, false);
		bury(a, "Jogre bones", ItemID.TBWT_JOGRE_BONES, ItemID.Cert.TBWT_JOGRE_BONES, 15, true);
		bury(a, "Zogre bones", ItemID.ZOGRE_BONES, ItemID.Cert.ZOGRE_BONES, 22.5, true);
		bury(a, "Babydragon bones", ItemID.BABYDRAGON_BONES, ItemID.Cert.BABYDRAGON_BONES, 30, true);
		bury(a, "Wyrmling bones", ItemID.BABYWYRM_BONES, ItemID.Cert.BABYWYRM_BONES, 21, true);
		bury(a, "Wyrm bones", ItemID.WYRM_BONES, ItemID.Cert.WYRM_BONES, 50, true);
		bury(a, "Dragon bones", ItemID.DRAGON_BONES, ItemID.Cert.DRAGON_BONES, 72, false);
		bury(a, "Wyvern bones", ItemID.WYVERN_BONES, ItemID.Cert.WYVERN_BONES, 72, true);
		bury(a, "Drake bones", ItemID.DRAKE_BONES, ItemID.Cert.DRAKE_BONES, 80, true);
		bury(a, "Fayrg bones", ItemID.ZOGRE_ANCESTRAL_BONES_FAYG, ItemID.Cert.ZOGRE_ANCESTRAL_BONES_FAYG, 84, true);
		bury(a, "Lava dragon bones", ItemID.LAVA_DRAGON_BONES, ItemID.Cert.LAVA_DRAGON_BONES, 85, true);
		bury(a, "Raurg bones", ItemID.ZOGRE_ANCESTRAL_BONES_RAURG, ItemID.Cert.ZOGRE_ANCESTRAL_BONES_RAURG, 96, true);
		bury(a, "Hydra bones", ItemID.HYDRA_BONES, ItemID.Cert.HYDRA_BONES, 110, true);
		bury(a, "Dagannoth bones", ItemID.DAGANNOTH_KING_BONES, ItemID.Cert.DAGANNOTH_KING_BONES, 125, true);
		bury(a, "Ourg bones", ItemID.ZOGRE_ANCESTRAL_BONES_OURG, ItemID.Cert.ZOGRE_ANCESTRAL_BONES_OURG, 140, true);
		bury(a, "Superior dragon bones", ItemID.DRAGON_BONES_SUPERIOR, ItemID.Cert.DRAGON_BONES_SUPERIOR, 150, true);
		scatter(a, "Fiendish ashes", ItemID.FIENDISH_ASHES, 10);
		scatter(a, "Vile ashes", ItemID.VILE_ASHES, 25);
		scatter(a, "Malicious ashes", ItemID.MALICIOUS_ASHES, 65);
		scatter(a, "Abyssal ashes", ItemID.ABYSSAL_ASHES, 85);
		scatter(a, "Infernal ashes", ItemID.INFERNAL_ASHES, 110);
	}

	private static void bury(List<BankedActivity> a, String name, int id, int noted, double xp, boolean members)
	{
		gildedAltar(a, name, id, noted, xp);
		Method m = bankMethod("banked_bury_" + key(name), Skill.PRAYER, "Bury " + lower(name), 1, xp, 1_800)
			.members(members)
			.input(ItemReq.consumed(name, 28, 1, id))
			.useItem(id)
			.instruction("Click each of the " + lower(name) + " to bury them, then bank for more")
			.notes("Offering bones at a gilded altar or the Wilderness Chaos altar gives 3.5x the xp; "
				+ "Skilling Helper's Prayer guide covers those.")
			.wikiPage("Prayer_training")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, name, id, 1, 0, id, noted).toBuilder()
			.method(m.toBuilder().clearInputs().input(ItemReq.consumed(name, 28, 1, id, noted)).build()).build());
	}

	/**
	 * Bones offered at a gilded altar with both burners lit: 3.5x the xp. In the player's own house, or a house party
	 * through the Rimmington portal; Phials next to the portal unnotes more bones.
	 */
	private static void gildedAltar(List<BankedActivity> a, String name, int id, int noted, double buryXp)
	{
		Method m = Method.builder()
			.id("banked_altar_" + key(name))
			.skill(Skill.PRAYER)
			.type(MethodType.PROCESS)
			.name("Offer " + lower(name) + " at a gilded altar")
			.minLevel(1)
			.xpPerAction(buryXp * 3.5)
			.xpPerHour((int) (buryXp * 3.5 * 1_600))
			.members(true)
			.location(HOUSE_PORTAL)
			.instanced(true)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.stack(name + " (withdraw as notes)", 1, id, noted))
			.supply(ItemReq.supply("Coins for Phials (5 gp a bone)", 0, ItemID.COINS))
			.useItem(id)
			.target(Target.object(null, "Altar"))
			.target(Target.object(null, "Portal"))
			.target(Target.npc("Exchange", "Phials"))
			.instruction("Use the " + lower(name) + " on the gilded altar with both incense burners lit; when you run "
				+ "out, leave the house and give the noted bones to Phials to unnote them")
			.notes("3.5x the xp of burying. Withdraw the bones as notes (the Note button in the bank).")
			.requirementText("A gilded altar with lit burners: your own house (75 Construction) or a house party "
				+ "(world 330 is the usual one)")
			.wikiPage("Gilded_altar")
			.build();
		a.add(activity(m, BankedActivity.Place.FIXED, name, id, 1, 0, id, noted));
	}

	private static void scatter(List<BankedActivity> a, String name, int id, double xp)
	{
		Method m = bankMethod("banked_scatter_" + key(name), Skill.PRAYER, "Scatter " + lower(name), 1, xp, 1_800)
			.members(true)
			.input(ItemReq.consumed(name, 28, 1, id))
			.useItem(id)
			.instruction("Click each of the " + lower(name) + " to scatter them, then bank for more")
			.wikiPage("Demonic_ashes")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, name, id, 1, 0, id));
	}

	// ---- Herblore ----

	private static void herblore(List<BankedActivity> a)
	{
		clean(a, "Guam leaf", 3, 2.5, ItemID.UNIDENTIFIED_GUAM);
		clean(a, "Marrentill", 5, 3.8, ItemID.UNIDENTIFIED_MARENTILL);
		clean(a, "Tarromin", 11, 5, ItemID.UNIDENTIFIED_TARROMIN);
		clean(a, "Harralander", 20, 6.3, ItemID.UNIDENTIFIED_HARRALANDER);
		clean(a, "Ranarr weed", 25, 7.5, ItemID.UNIDENTIFIED_RANARR);
		clean(a, "Toadflax", 30, 8, ItemID.UNIDENTIFIED_TOADFLAX);
		clean(a, "Irit leaf", 40, 8.8, ItemID.UNIDENTIFIED_IRIT);
		clean(a, "Avantoe", 48, 10, ItemID.UNIDENTIFIED_AVANTOE);
		clean(a, "Kwuarm", 54, 11.3, ItemID.UNIDENTIFIED_KWUARM);
		clean(a, "Snapdragon", 59, 11.8, ItemID.UNIDENTIFIED_SNAPDRAGON);
		clean(a, "Cadantine", 65, 12.5, ItemID.UNIDENTIFIED_CADANTINE);
		clean(a, "Lantadyme", 67, 13.1, ItemID.UNIDENTIFIED_LANTADYME);
		clean(a, "Dwarf weed", 70, 13.8, ItemID.UNIDENTIFIED_DWARF_WEED);
		clean(a, "Torstol", 75, 15, ItemID.UNIDENTIFIED_TORSTOL);

		potion(a, "Attack potions", 3, 25, "Guam", ItemID.GUAM_LEAF, ItemID.GUAMVIAL, "Eyes of newt",
			ItemID.EYE_OF_NEWT);
		potion(a, "Antipoisons", 5, 37.5, "Marrentill", ItemID.MARENTILL, ItemID.MARRENTILLVIAL,
			"Unicorn horn dust", ItemID.UNICORN_HORN_DUST);
		potion(a, "Strength potions", 12, 50, "Tarromin", ItemID.TARROMIN, ItemID.TARROMINVIAL, "Limpwurt roots",
			ItemID.LIMPWURT_ROOT);
		potion(a, "Restore potions", 22, 62.5, "Harralander", ItemID.HARRALANDER, ItemID.HARRALANDERVIAL,
			"Red spiders' eggs", ItemID.RED_SPIDERS_EGGS);
		potion(a, "Energy potions", 26, 67.5, "Harralander", ItemID.HARRALANDER, ItemID.HARRALANDERVIAL,
			"Chocolate dust", ItemID.CHOCOLATE_DUST);
		potion(a, "Agility potions", 34, 80, "Toadflax", ItemID.TOADFLAX, ItemID.TOADFLAXVIAL, "Toad's legs",
			ItemID.TOADS_LEGS);
		potion(a, "Prayer potions", 38, 87.5, "Ranarr", ItemID.RANARR_WEED, ItemID.RANARRVIAL, "Snape grass",
			ItemID.SNAPE_GRASS);
		potion(a, "Super attack potions", 45, 100, "Irit", ItemID.IRIT_LEAF, ItemID.IRITVIAL, "Eyes of newt",
			ItemID.EYE_OF_NEWT);
		potion(a, "Superantipoisons", 48, 112.5, "Irit", ItemID.IRIT_LEAF, ItemID.IRITVIAL, "Unicorn horn dust",
			ItemID.UNICORN_HORN_DUST);
		potion(a, "Fishing potions", 50, 112.5, "Avantoe", ItemID.AVANTOE, ItemID.AVANTOEVIAL, "Snape grass",
			ItemID.SNAPE_GRASS);
		potion(a, "Super energy potions", 52, 117.5, "Avantoe", ItemID.AVANTOE, ItemID.AVANTOEVIAL,
			"Mort myre fungi", ItemID.MORTMYREMUSHROOM);
		potion(a, "Super strength potions", 55, 125, "Kwuarm", ItemID.KWUARM, ItemID.KWUARMVIAL, "Limpwurt roots",
			ItemID.LIMPWURT_ROOT);
		potion(a, "Super restores", 63, 142.5, "Snapdragon", ItemID.SNAPDRAGON, ItemID.SNAPDRAGONVIAL,
			"Red spiders' eggs", ItemID.RED_SPIDERS_EGGS);
		potion(a, "Super defence potions", 66, 150, "Cadantine", ItemID.CADANTINE, ItemID.CADANTINEVIAL,
			"White berries", ItemID.WHITE_BERRIES);
		potion(a, "Antifire potions", 69, 157.5, "Lantadyme", ItemID.LANTADYME, ItemID.LANTADYMEVIAL,
			"Dragon scale dust", ItemID.DRAGON_SCALE_DUST);
		potion(a, "Ranging potions", 72, 162.5, "Dwarf weed", ItemID.DWARF_WEED, ItemID.DWARFWEEDVIAL,
			"Wines of Zamorak", ItemID.WINE_OF_ZAMORAK);
		potion(a, "Magic potions", 76, 172.5, "Lantadyme", ItemID.LANTADYME, ItemID.LANTADYMEVIAL, "Potato cacti",
			ItemID.CACTUS_POTATO);
		potion(a, "Zamorak brews", 78, 175, "Torstol", ItemID.TORSTOL, ItemID.TORSTOLVIAL, "Jangerberries",
			ItemID.JANGERBERRIES);
		potion(a, "Saradomin brews", 81, 180, "Toadflax", ItemID.TOADFLAX, ItemID.TOADFLAXVIAL, "Crushed nests",
			ItemID.CRUSHED_BIRD_NEST);
	}

	private static void clean(List<BankedActivity> a, String herb, int level, double xp, int grimy)
	{
		String name = "Grimy " + lower(herb);
		Method m = bankMethod("banked_clean_" + key(herb), Skill.HERBLORE, "Clean " + name, level, xp, 4_500)
			.members(true)
			.quest(Quest.DRUIDIC_RITUAL)
			.input(ItemReq.consumed(name, 28, 1, grimy))
			.useItem(grimy)
			.instruction("Click each " + name + " to clean it, then bank for more")
			.wikiPage("Herblore_training")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, name, grimy, 1, 0, grimy));
	}

	/**
	 * A potion made from the unfinished potion, or from the clean herb and a vial of water. Both count as banked;
	 * the secondary is bought if the bank is short of it.
	 */
	private static void potion(List<BankedActivity> a, String potions, int level, double xp, String herb,
		int cleanHerb, int unf, String secondaryName, int secondary)
	{
		String unfName = herb + " potions (unf)";
		ItemReq unfinished = ItemReq.consumed(unfName, 14, 1, unf)
			.madeFrom("Use the clean " + lower(herb) + " on vials of water", true,
				ItemReq.part("Clean " + lower(herb), 1, cleanHerb),
				ItemReq.part("Vials of water", 1, ItemID.VIAL_WATER));
		Method m = bankMethod("banked_potion_" + key(potions), Skill.HERBLORE, "Make " + lower(potions), level, xp,
			2_500)
			.members(true)
			.quest(Quest.DRUIDIC_RITUAL)
			.input(unfinished)
			.input(ItemReq.consumed(secondaryName, 14, 1, secondary))
			.useItem(secondary).useItem(unf)
			.instruction("Use the " + lower(secondaryName) + " on the " + lower(unfName) + " and press Space to "
				+ "make them all")
			.notes("Clean herbs are made into unfinished potions first, and any missing " + lower(secondaryName)
				+ " are bought at the Grand Exchange.")
			.wikiPage("Herblore_training")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, "Clean " + lower(herb) + " / unf potions", cleanHerb, 1, 1,
			unf, cleanHerb));
	}

	// ---- Cooking ----

	private static void cooking(List<BankedActivity> a)
	{
		cook(a, "Raw shrimps", ItemID.RAW_SHRIMP, 1, 30, false);
		cook(a, "Raw anchovies", ItemID.RAW_ANCHOVIES, 1, 30, false);
		cook(a, "Raw sardine", ItemID.RAW_SARDINE, 1, 40, false);
		cook(a, "Raw beef", ItemID.RAW_BEEF, 1, 30, false);
		cook(a, "Raw chicken", ItemID.RAW_CHICKEN, 1, 30, false);
		cook(a, "Raw herring", ItemID.RAW_HERRING, 5, 50, false);
		cook(a, "Raw trout", ItemID.RAW_TROUT, 15, 70, false);
		cook(a, "Raw cod", ItemID.RAW_COD, 18, 75, true);
		cook(a, "Raw pike", ItemID.RAW_PIKE, 20, 80, false);
		cook(a, "Raw salmon", ItemID.RAW_SALMON, 25, 90, false);
		cook(a, "Raw tuna", ItemID.RAW_TUNA, 30, 100, false);
		cook(a, "Raw cave eel", ItemID.RAW_CAVE_EEL, 38, 115, true);
		cook(a, "Raw lobster", ItemID.RAW_LOBSTER, 40, 120, false);
		cook(a, "Raw bass", ItemID.RAW_BASS, 43, 130, true);
		cook(a, "Raw swordfish", ItemID.RAW_SWORDFISH, 45, 140, false);
		cook(a, "Raw monkfish", ItemID.RAW_MONKFISH, 62, 150, true);
		cook(a, "Raw shark", ItemID.RAW_SHARK, 80, 210, true);
		cook(a, "Raw sea turtle", ItemID.RAW_SEATURTLE, 82, 211.3, true);
		cook(a, "Raw anglerfish", ItemID.RAW_ANGLERFISH, 84, 230, true);
		cook(a, "Raw dark crab", ItemID.RAW_DARK_CRAB, 90, 215, true);
		cook(a, "Raw manta ray", ItemID.RAW_MANTARAY, 91, 216.3, true);

		Method karambwan = Method.builder()
			.id("banked_cook_karambwan")
			.skill(Skill.COOKING)
			.type(MethodType.PROCESS)
			.name("Cook raw karambwan")
			.minLevel(30)
			.xpPerAction(190)
			.xpPerHour(190 * 1_300)
			.members(true)
			.quest(Quest.TAI_BWO_WANNAI_TRIO)
			.location(CATHERBY_RANGE)
			.bank(Banks.CATHERBY)
			.input(ItemReq.consumed("Raw karambwan", 28, 1, ItemID.TBWT_RAW_KARAMBWAN))
			.useItem(ItemID.TBWT_RAW_KARAMBWAN)
			.target(Target.object("Cook", "Range"))
			.instruction("Use the raw karambwan on the range and press Space to cook them all")
			.wikiPage("Raw_karambwan")
			.build();
		a.add(activity(karambwan, BankedActivity.Place.RANGE, "Raw karambwan", ItemID.TBWT_RAW_KARAMBWAN, 1, 0,
			ItemID.TBWT_RAW_KARAMBWAN));

		Method wine = bankMethod("banked_cook_wine", Skill.COOKING, "Make jugs of wine", 35, 200, 2_500)
			.members(true)
			.input(ItemReq.consumed("Grapes", 14, 1, ItemID.GRAPES))
			.input(ItemReq.consumed("Jugs of water", 14, 1, ItemID.JUG_WATER))
			.useItem(ItemID.GRAPES).useItem(ItemID.JUG_WATER)
			.instruction("Use the grapes on the jugs of water and press Space to make them all")
			.notes("The xp comes a few seconds later, when the wine ferments. Missing jugs of water are bought.")
			.wikiPage("Jug_of_wine")
			.build();
		a.add(activity(wine, BankedActivity.Place.ANY_BANK, "Grapes", ItemID.GRAPES, 1, 0, ItemID.GRAPES));
	}

	private static void cook(List<BankedActivity> a, String rawName, int raw, int level, double xp, boolean members)
	{
		String food = lower(rawName.substring(4));
		Method m = Method.builder()
			.id("banked_cook_" + key(rawName))
			.skill(Skill.COOKING)
			.type(MethodType.PROCESS)
			.name("Cook " + food)
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * 1_300))
			.members(members)
			.location(CATHERBY_RANGE)
			.bank(Banks.CATHERBY)
			.input(ItemReq.consumed(rawName, 28, 1, raw))
			.useItem(raw)
			.target(Target.object("Cook", "Range"))
			.instruction("Use the " + lower(rawName) + " on the range and press Space to cook them all")
			.notes("A range burns less than a fire. Cooking gauntlets help with lobsters, swordfish, monkfish, sharks "
				+ "and anglerfish.")
			.wikiPage(members ? "Pay-to-play_Cooking_training" : "Free-to-play_Cooking_training")
			.build();
		a.add(activity(m, BankedActivity.Place.RANGE, rawName, raw, 1, 0, raw));
	}

	// ---- Logs: Firemaking and Fletching ----

	private static void firemakingAndFletching(List<BankedActivity> a)
	{
		logs(a, "Logs", ItemID.LOGS, 1, 40, false, 1, 5, 5, 10, 10, ItemID.UNSTRUNG_SHORTBOW,
			ItemID.UNSTRUNG_LONGBOW, 15);
		logs(a, "Oak logs", ItemID.OAK_LOGS, 15, 60, false, 20, 16.5, 25, 25, 15, ItemID.UNSTRUNG_OAK_SHORTBOW,
			ItemID.UNSTRUNG_OAK_LONGBOW, 30);
		logs(a, "Willow logs", ItemID.WILLOW_LOGS, 30, 90, false, 35, 33.3, 40, 41.5, 30,
			ItemID.UNSTRUNG_WILLOW_SHORTBOW, ItemID.UNSTRUNG_WILLOW_LONGBOW, 45);
		logs(a, "Maple logs", ItemID.MAPLE_LOGS, 45, 135, false, 50, 50, 55, 58.3, 45,
			ItemID.UNSTRUNG_MAPLE_SHORTBOW, ItemID.UNSTRUNG_MAPLE_LONGBOW, 60);
		logs(a, "Yew logs", ItemID.YEW_LOGS, 60, 202.5, false, 65, 67.5, 70, 75, 60, ItemID.UNSTRUNG_YEW_SHORTBOW,
			ItemID.UNSTRUNG_YEW_LONGBOW, 75);
		logs(a, "Magic logs", ItemID.MAGIC_LOGS, 75, 303.8, false, 80, 83.3, 85, 91.5, 75,
			ItemID.UNSTRUNG_MAGIC_SHORTBOW, ItemID.UNSTRUNG_MAGIC_LONGBOW, 90);
		burn(a, "Teak logs", ItemID.TEAK_LOGS, 35, 105, true);
		burn(a, "Mahogany logs", ItemID.MAHOGANY_LOGS, 50, 157.5, true);
		burn(a, "Redwood logs", ItemID.REDWOOD_LOGS, 90, 350, true);

		string(a, "Shortbow (u)", ItemID.UNSTRUNG_SHORTBOW, 5, 5);
		string(a, "Longbow (u)", ItemID.UNSTRUNG_LONGBOW, 10, 10);
		string(a, "Oak shortbow (u)", ItemID.UNSTRUNG_OAK_SHORTBOW, 20, 16.5);
		string(a, "Oak longbow (u)", ItemID.UNSTRUNG_OAK_LONGBOW, 25, 25);
		string(a, "Willow shortbow (u)", ItemID.UNSTRUNG_WILLOW_SHORTBOW, 35, 33.3);
		string(a, "Willow longbow (u)", ItemID.UNSTRUNG_WILLOW_LONGBOW, 40, 41.5);
		string(a, "Maple shortbow (u)", ItemID.UNSTRUNG_MAPLE_SHORTBOW, 50, 50);
		string(a, "Maple longbow (u)", ItemID.UNSTRUNG_MAPLE_LONGBOW, 55, 58.3);
		string(a, "Yew shortbow (u)", ItemID.UNSTRUNG_YEW_SHORTBOW, 65, 67.5);
		string(a, "Yew longbow (u)", ItemID.UNSTRUNG_YEW_LONGBOW, 70, 75);
		string(a, "Magic shortbow (u)", ItemID.UNSTRUNG_MAGIC_SHORTBOW, 80, 83.3);
		string(a, "Magic longbow (u)", ItemID.UNSTRUNG_MAGIC_LONGBOW, 85, 91.5);

		Method headless = bankMethod("banked_fletch_headless", Skill.FLETCHING, "Make headless arrows", 1, 15, 700)
			.members(true)
			.input(ItemReq.stack("Arrow shafts", 15, ItemID.ARROW_SHAFT))
			.input(ItemReq.stack("Feathers", 15, ItemID.FEATHER))
			.useItem(ItemID.FEATHER).useItem(ItemID.ARROW_SHAFT)
			.instruction("Use the feathers on the arrow shafts and make sets of 15 until they run out")
			.notes("Missing feathers are bought at the Grand Exchange.")
			.wikiPage("Headless_arrow")
			.build();
		a.add(activity(headless, BankedActivity.Place.ANY_BANK, "Arrow shafts", ItemID.ARROW_SHAFT, 15, 1,
			ItemID.ARROW_SHAFT));

		arrows(a, "Bronze arrowtips", ItemID.BRONZE_ARROWHEADS, 1, 1.3);
		arrows(a, "Iron arrowtips", ItemID.IRON_ARROWHEADS, 15, 2.5);
		arrows(a, "Steel arrowtips", ItemID.STEEL_ARROWHEADS, 30, 5);
		arrows(a, "Mithril arrowtips", ItemID.MITHRIL_ARROWHEADS, 45, 7.5);
		arrows(a, "Adamant arrowtips", ItemID.ADAMANT_ARROWHEADS, 60, 10);
		arrows(a, "Rune arrowtips", ItemID.RUNE_ARROWHEADS, 75, 12.5);
		arrows(a, "Amethyst arrowtips", ItemID.AMETHYST_ARROWHEADS, 82, 13.5);
		arrows(a, "Dragon arrowtips", ItemID.DRAGON_ARROWHEADS, 90, 15);

		darts(a, "Bronze dart tip", ItemID.BRONZE_DART_TIP, 10, 1.8);
		darts(a, "Iron dart tip", ItemID.IRON_DART_TIP, 22, 3.8);
		darts(a, "Steel dart tip", ItemID.STEEL_DART_TIP, 37, 7.5);
		darts(a, "Mithril dart tip", ItemID.MITHRIL_DART_TIP, 52, 11.2);
		darts(a, "Adamant dart tip", ItemID.ADAMANT_DART_TIP, 67, 15);
		darts(a, "Rune dart tip", ItemID.RUNE_DART_TIP, 81, 18.8);
		darts(a, "Amethyst dart tip", ItemID.AMETHYST_DART_TIP, 90, 21);
		darts(a, "Dragon dart tip", ItemID.DRAGON_DART_TIP, 95, 25);
	}

	/**
	 * One kind of log: burn it, cut it into a shortbow or longbow (u), or into arrow shafts.
	 */
	private static void logs(List<BankedActivity> a, String name, int id, int fmLevel, double fmXp,
		boolean fmMembers, int shortLevel, double shortXp, int longLevel, double longXp, int shaftLevel,
		int shortbow, int longbow, int shafts)
	{
		burn(a, name, id, fmLevel, fmXp, fmMembers);
		String wood = name.equals("Logs") ? "" : name.substring(0, name.indexOf(' ')).toLowerCase() + " ";
		fletch(a, name, id, "banked_fletch_" + key(wood + "shortbow"), "Fletch " + wood + "shortbows (u)",
			shortLevel, shortXp, shortbow, wood + "shortbow (u)");
		fletch(a, name, id, "banked_fletch_" + key(wood + "longbow"), "Fletch " + wood + "longbows (u)", longLevel,
			longXp, longbow, wood + "longbow (u)");
		// Arrow shafts: 15 per log, plus 15 more for each wood tier above normal logs
		fletch(a, name, id, "banked_fletch_" + key(wood + "shafts"), "Cut " + lower(name) + " into arrow shafts",
			shaftLevel, shafts / 3.0, ItemID.ARROW_SHAFT, "arrow shafts (" + shafts + " per log)");
	}

	private static void burn(List<BankedActivity> a, String name, int id, int level, double xp, boolean members)
	{
		Method m = Method.builder()
			.id("banked_burn_" + key(name))
			.skill(Skill.FIREMAKING)
			.type(MethodType.PROCESS)
			.name("Burn " + lower(name))
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * 1_100))
			.members(members)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.tool(TINDERBOX)
			.input(ItemReq.consumed(name, 27, 1, id))
			.useItem(ItemID.TINDERBOX).useItem(id)
			.instruction("Stand on a free east-facing lane and use the tinderbox on the " + lower(name)
				+ ". You step west after each fire, so keep lighting until the inventory is empty")
			.notes("The Grand Exchange has long clear lanes and a bank in the middle.")
			.wikiPage("Pay-to-play_Firemaking_training")
			.build();
		a.add(activity(m, BankedActivity.Place.FIXED, name, id, 1, 0, id));
	}

	private static void fletch(List<BankedActivity> a, String logName, int logId, String id, String title, int level,
		double xp, int product, String productName)
	{
		Method m = bankMethod(id, Skill.FLETCHING, title, level, xp, 1_700)
			.members(true)
			.tool(KNIFE)
			.input(ItemReq.consumed(logName, 27, 1, logId))
			.product(product)
			.useItem(ItemID.KNIFE).useItem(logId)
			.instruction("Use the knife on the " + lower(logName) + ", pick " + productName
				+ " and press Space to cut them all")
			.wikiPage("Fletching_training")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, logName, logId, 1, 0, logId));
	}

	private static void string(List<BankedActivity> a, String name, int id, int level, double xp)
	{
		Method m = bankMethod("banked_string_" + key(name), Skill.FLETCHING, "String " + lower(name).replace(" (u)",
				"s"), level, xp, 2_400)
			.members(true)
			.input(ItemReq.consumed(name, 14, 1, id))
			.input(ItemReq.consumed("Bow strings", 14, 1, ItemID.BOW_STRING))
			.useItem(ItemID.BOW_STRING).useItem(id)
			.instruction("Use the bow strings on the " + lower(name) + " and press Space to string them all")
			.notes("Missing bow strings are bought at the Grand Exchange.")
			.wikiPage("Fletching_training")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, name, id, 1, 1, id));
	}

	private static void arrows(List<BankedActivity> a, String name, int id, int level, double xpEach)
	{
		String arrow = name.replace(" arrowtips", " arrows");
		Method m = bankMethod("banked_arrows_" + key(name), Skill.FLETCHING, "Make " + lower(arrow), level,
			xpEach * 15, 700)
			.members(true)
			.input(ItemReq.stack(name, 15, id))
			.input(ItemReq.stack("Headless arrows", 15, ItemID.HEADLESS_ARROW))
			.useItem(id).useItem(ItemID.HEADLESS_ARROW)
			.instruction("Use the " + lower(name) + " on the headless arrows and make sets of 15 until they run out")
			.notes("Make headless arrows first if you have arrow shafts. Missing headless arrows are bought.")
			.wikiPage("Fletching_training")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, name, id, 15, 2, id));
	}

	private static void darts(List<BankedActivity> a, String name, int id, int level, double xpEach)
	{
		String dart = name.replace(" dart tip", " darts");
		Method m = bankMethod("banked_darts_" + key(name), Skill.FLETCHING, "Make " + lower(dart), level, xpEach * 10,
			1_500)
			.members(true)
			.quest(Quest.THE_TOURIST_TRAP)
			.input(ItemReq.stack(name + "s", 10, id))
			.input(ItemReq.stack("Feathers", 10, ItemID.FEATHER))
			.useItem(id).useItem(ItemID.FEATHER)
			.instruction("Use the feathers on the " + lower(name) + "s, ten at a time, until they run out")
			.notes("Missing feathers are bought at the Grand Exchange.")
			.wikiPage("Dart")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, name + "s", id, 10, 0, id));
	}

	// ---- Crafting ----

	private static void crafting(List<BankedActivity> a)
	{
		gem(a, "Uncut opal", ItemID.UNCUT_OPAL, 1, 15, true);
		gem(a, "Uncut jade", ItemID.UNCUT_JADE, 13, 20, true);
		gem(a, "Uncut red topaz", ItemID.UNCUT_RED_TOPAZ, 16, 25, true);
		gem(a, "Uncut sapphire", ItemID.UNCUT_SAPPHIRE, 20, 50, false);
		gem(a, "Uncut emerald", ItemID.UNCUT_EMERALD, 27, 67.5, false);
		gem(a, "Uncut ruby", ItemID.UNCUT_RUBY, 34, 85, false);
		gem(a, "Uncut diamond", ItemID.UNCUT_DIAMOND, 43, 107.5, false);
		gem(a, "Uncut dragonstone", ItemID.UNCUT_DRAGONSTONE, 55, 137.5, true);
		gem(a, "Uncut onyx", ItemID.UNCUT_ONYX, 67, 167.5, true);
		gem(a, "Uncut zenyte", ItemID.UNCUT_ZENYTE, 89, 200, true);

		leather(a, "Leather", ItemID.LEATHER, "leather gloves", 1, 13.8, 1, false);
		leather(a, "Leather", ItemID.LEATHER, "leather bodies", 14, 25, 1, false);
		leather(a, "Leather", ItemID.LEATHER, "leather chaps", 18, 27, 1, false);
		leather(a, "Leather", ItemID.LEATHER, "coifs", 38, 37, 1, false);
		leather(a, "Hard leather", ItemID.HARD_LEATHER, "hardleather bodies", 28, 35, 1, false);
		hide(a, "Green", ItemID.DRAGON_LEATHER, 57, 62);
		hide(a, "Blue", ItemID.DRAGON_LEATHER_BLUE, 66, 70);
		hide(a, "Red", ItemID.DRAGON_LEATHER_RED, 73, 78);
		hide(a, "Black", ItemID.DRAGON_LEATHER_BLACK, 79, 86);

		glass(a, "beer glasses", 1, 17.5);
		glass(a, "vials", 33, 35);
		glass(a, "unpowered orbs", 46, 52.5);
		glass(a, "lantern lenses", 49, 55);
		glass(a, "empty light orbs", 87, 70);

		battlestaff(a, "Water orb", ItemID.WATER_ORB, "water", 54, 100);
		battlestaff(a, "Earth orb", ItemID.EARTH_ORB, "earth", 58, 112.5);
		battlestaff(a, "Fire orb", ItemID.FIRE_ORB, "fire", 62, 125);
		battlestaff(a, "Air orb", ItemID.AIR_ORB, "air", 66, 137.5);
	}

	private static void gem(List<BankedActivity> a, String name, int id, int level, double xp, boolean members)
	{
		Method m = bankMethod("banked_cut_" + key(name), Skill.CRAFTING, "Cut " + lower(name.substring(6)) + "s",
			level, xp, 2_700)
			.members(members)
			.tool(CHISEL)
			.input(ItemReq.consumed(name, 27, 1, id))
			.useItem(ItemID.CHISEL).useItem(id)
			.instruction("Use the chisel on the " + lower(name) + "s and press Space to cut them all")
			.wikiPage(members ? "Pay-to-play_Crafting_training" : "Free-to-play_Crafting_training")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, name, id, 1, 0, id));
	}

	private static void leather(List<BankedActivity> a, String name, int id, String product, int level, double xp,
		int perItem, boolean members)
	{
		Method m = bankMethod("banked_craft_" + key(product), Skill.CRAFTING, "Craft " + product, level, xp, 1_600)
			.members(members)
			.tool(NEEDLE).tool(THREAD)
			.input(ItemReq.consumed(name, 26, perItem, id))
			.useItem(ItemID.NEEDLE).useItem(id)
			.instruction("Use the needle on the " + lower(name) + ", pick " + product + " and press Space to make "
				+ "them all")
			.notes("Thread runs out every 5 items, so keep a stack in the inventory.")
			.wikiPage("Free-to-play_Crafting_training")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, name, id, perItem, 0, id));
	}

	/**
	 * Green, blue, red or black dragon leather: vambraces (1 leather), chaps (2) or bodies (3), all the same xp per
	 * leather; bodies are the fewest clicks.
	 */
	private static void hide(List<BankedActivity> a, String colour, int leather, int level, double xpPerLeather)
	{
		String name = colour + " dragon leather";
		String c = colour.toLowerCase();
		dhide(a, name, leather, c + " d'hide vambraces", level, xpPerLeather, 1);
		dhide(a, name, leather, c + " d'hide chaps", level + 3, xpPerLeather * 2, 2);
		dhide(a, name, leather, c + " d'hide bodies", level + 6 - (colour.equals("Black") ? 1 : 0),
			xpPerLeather * 3, 3);
	}

	private static void dhide(List<BankedActivity> a, String name, int leather, String product, int level, double xp,
		int perItem)
	{
		Method m = bankMethod("banked_craft_" + key(product), Skill.CRAFTING, "Craft " + product, level, xp,
			perItem == 1 ? 1_800 : 1_200)
			.members(true)
			.tool(NEEDLE).tool(THREAD)
			.input(ItemReq.consumed(name, perItem == 3 ? 24 : 26, perItem, leather))
			.useItem(ItemID.NEEDLE).useItem(leather)
			.instruction("Use the needle on the " + lower(name) + ", pick " + product + " and press Space to make "
				+ "them all")
			.notes("Thread runs out every 5 items, so keep a stack in the inventory.")
			.wikiPage("Pay-to-play_Crafting_training")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, name, leather, perItem, 0, leather));
	}

	private static void glass(List<BankedActivity> a, String product, int level, double xp)
	{
		Method m = bankMethod("banked_glass_" + key(product), Skill.CRAFTING, "Blow " + product, level, xp, 1_600)
			.members(true)
			.tool(PIPE)
			.input(ItemReq.consumed("Molten glass", 27, 1, ItemID.MOLTEN_GLASS))
			.useItem(ItemID.GLASSBLOWINGPIPE).useItem(ItemID.MOLTEN_GLASS)
			.instruction("Use the glassblowing pipe on the molten glass, pick " + product + " and press Space to "
				+ "make them all")
			.wikiPage("Pay-to-play_Crafting_training")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, "Molten glass", ItemID.MOLTEN_GLASS, 1, 0,
			ItemID.MOLTEN_GLASS));
	}

	private static void battlestaff(List<BankedActivity> a, String orbName, int orb, String element, int level,
		double xp)
	{
		Method m = bankMethod("banked_staff_" + element, Skill.CRAFTING, "Make " + element + " battlestaves", level,
			xp, 2_500)
			.members(true)
			.input(ItemReq.consumed(orbName + "s", 14, 1, orb))
			.input(ItemReq.consumed("Battlestaves", 14, 1, ItemID.BATTLESTAFF))
			.useItem(orb).useItem(ItemID.BATTLESTAFF)
			.instruction("Use the " + lower(orbName) + "s on the battlestaves and press Space to make them all")
			.notes("Missing battlestaves are bought at the Grand Exchange (or from Zaff in Varrock).")
			.wikiPage("Battlestaff")
			.build();
		a.add(activity(m, BankedActivity.Place.ANY_BANK, orbName + "s", orb, 1, 0, orb));
	}

	// ---- Smithing ----

	private static void smithing(List<BankedActivity> a)
	{
		smelt(a, "bronze", "Copper ore", ItemID.COPPER_ORE, 1, 6.2, ItemReq.consumed("Tin ore", 14, 1,
			ItemID.TIN_ORE), 14);
		smelt(a, "iron", "Iron ore", ItemID.IRON_ORE, 15, 12.5, null, 28);
		smelt(a, "silver", "Silver ore", ItemID.SILVER_ORE, 20, 13.7, null, 28);
		smelt(a, "steel", "Iron ore", ItemID.IRON_ORE, 30, 17.5, coal(2, 9), 9);
		smelt(a, "gold", "Gold ore", ItemID.GOLD_ORE, 40, 22.5, null, 28);
		smelt(a, "mithril", "Mithril ore", ItemID.MITHRIL_ORE, 50, 30, coal(4, 5), 5);
		smelt(a, "adamantite", "Adamantite ore", ItemID.ADAMANTITE_ORE, 70, 37.5, coal(6, 4), 4);
		smelt(a, "runite", "Runite ore", ItemID.RUNITE_ORE, 85, 50, coal(8, 3), 3);

		anvil(a, "Bronze bar", ItemID.BRONZE_BAR, "bronze", 1, 18, 12.5);
		anvil(a, "Iron bar", ItemID.IRON_BAR, "iron", 15, 33, 25);
		anvil(a, "Steel bar", ItemID.STEEL_BAR, "steel", 30, 48, 37.5);
		anvil(a, "Mithril bar", ItemID.MITHRIL_BAR, "mithril", 50, 68, 50);
		anvil(a, "Adamantite bar", ItemID.ADAMANTITE_BAR, "adamant", 70, 88, 62.5);
		anvil(a, "Runite bar", ItemID.RUNITE_BAR, "rune", 85, 99, 75);
	}

	private static ItemReq coal(int perBar, int ores)
	{
		return ItemReq.consumed("Coal", perBar * ores, perBar, ItemID.COAL);
	}

	private static void smelt(List<BankedActivity> a, String bar, String oreName, int ore, int level, double xp,
		ItemReq second, int perTrip)
	{
		Method.MethodBuilder b = Method.builder()
			.id("banked_smelt_" + bar)
			.skill(Skill.SMITHING)
			.type(MethodType.PROCESS)
			.name("Smelt " + bar + " bars")
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * 1_300))
			.location(EDGEVILLE_FURNACE)
			.bank(Banks.EDGEVILLE)
			.input(ItemReq.consumed(oreName, perTrip, 1, ore))
			.target(Target.object("Smelt", "Furnace"))
			.instruction("Click the furnace, choose " + bar + " and press Space to smelt them all")
			.wikiPage("Smithing_training");
		if (second != null)
		{
			b.input(second).notes("Missing " + second.getName().toLowerCase() + " is bought at the Grand Exchange.");
		}
		if (bar.equals("iron"))
		{
			b.notes("Iron only smelts half the time without a ring of forging.");
		}
		if (bar.equals("gold"))
		{
			b.notes("Goldsmith gauntlets (Family Crest) more than double the xp.");
		}
		a.add(activity(b.build(), BankedActivity.Place.FIXED, oreName, ore, 1, 0, ore));
	}

	/**
	 * Smithing bars at an anvil: the xp per bar is the same for every item, so it's daggers until platebodies (five
	 * bars a click) unlock.
	 */
	private static void anvil(List<BankedActivity> a, String barName, int bar, String metal, int daggerLevel,
		int bodyLevel, double xpPerBar)
	{
		a.add(activity(anvilMethod("banked_smith_" + metal + "_daggers", metal + " daggers", daggerLevel, xpPerBar, 1,
			barName, bar), BankedActivity.Place.FIXED, barName + "s", bar, 1, 1, bar));
		a.add(activity(anvilMethod("banked_smith_" + metal + "_platebodies", metal + " platebodies", bodyLevel,
			xpPerBar * 5, 5, barName, bar), BankedActivity.Place.FIXED, barName + "s", bar, 5, 1, bar));
	}

	private static Method anvilMethod(String id, String product, int level, double xp, int barsEach, String barName,
		int bar)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.SMITHING)
			.type(MethodType.PROCESS)
			.name("Smith " + product)
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * 1_800 / barsEach))
			.location(VARROCK_ANVIL)
			.bank(Banks.VARROCK_WEST)
			.tool(HAMMER)
			.input(ItemReq.consumed(barName + "s", barsEach == 1 ? 27 : 25, barsEach, bar))
			.useItem(bar)
			.target(Target.object("Smith", "Anvil"))
			.instruction("Use the bars on the anvil, pick " + product + " and let it make them all")
			.wikiPage("Smithing_training")
			.build();
	}

	// ---- Construction ----

	/**
	 * House builds. They join the log groups, so oak, teak and mahogany logs can go to Construction instead of being
	 * burnt or fletched: the guide has the logs made into planks at the sawmill. Planks already in the bank count too.
	 */
	private static void construction(List<BankedActivity> a)
	{
		build(a, "banked_con_oak_chairs", "Oak chairs", 19, 120, 2, "Oak planks", ItemID.PLANK_OAK, "Oak logs",
			ItemID.OAK_LOGS, 250, "Chair space");
		build(a, "banked_con_oak_larders", "Oak larders", 33, 480, 8, "Oak planks", ItemID.PLANK_OAK, "Oak logs",
			ItemID.OAK_LOGS, 250, "Larder space");
		build(a, "banked_con_teak_tables", "Teak tables", 38, 360, 4, "Teak planks", ItemID.PLANK_TEAK, "Teak logs",
			ItemID.TEAK_LOGS, 500, "Table space");
		build(a, "banked_con_mahogany_tables", "Mahogany tables", 52, 840, 6, "Mahogany planks",
			ItemID.PLANK_MAHOGANY, "Mahogany logs", ItemID.MAHOGANY_LOGS, 1_500, "Table space");
	}

	private static void build(List<BankedActivity> a, String id, String name, int level, double xp, int planks,
		String plankName, int plank, String logName, int logs, int fee, String space)
	{
		ItemReq sawn = ItemReq.consumed(plankName, 24, planks, plank)
			.madeFrom("Buy-plank from the sawmill operator: " + logName.toLowerCase() + " and " + fee + " gp per plank",
				true, ItemReq.part(logName, 1, logs), ItemReq.stack("Coins", fee, ItemID.COINS))
			.madeAt(SAWMILL, Target.npc("Buy-plank", "Sawmill operator"));
		Method m = Method.builder()
			.id(id)
			.skill(Skill.CONSTRUCTION)
			.type(MethodType.PROCESS)
			.name("Build " + name.toLowerCase())
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * 400))
			.members(true)
			.location(HOUSE_PORTAL)
			.instanced(true)
			.bank(Banks.GRAND_EXCHANGE)
			.tool(HAMMER).tool(SAW)
			.input(sawn)
			.target(Target.object("Build", space))
			.target(Target.object("Remove", name.substring(0, name.length() - 1)))
			.instruction("Enter your house in build mode, build " + name.toLowerCase() + " in the "
				+ space.toLowerCase() + ", remove them and build again")
			.notes("Your " + logName.toLowerCase() + " are made into planks at the sawmill first (" + fee
				+ " gp each). A servant (or Phials in Rimmington for noted planks) saves most of the walking.")
			.wikiPage("Construction_training")
			.build();
		a.add(BankedActivity.builder()
			.method(m)
			.place(BankedActivity.Place.FIXED)
			.group(groupKey(logs))
			.itemName(logName)
			.iconId(logs)
			.countIds(new int[]{plank, logs})
			.perAction(planks)
			.stage(0)
			.build());
	}

	// ---- Runecraft and Magic, from Skilling Helper's own guides ----

	private static void fromSkillGuides(List<BankedActivity> a)
	{
		GuideRepository guides = new GuideRepository();
		int[] essence = {ItemID.BLANKRUNE, ItemID.BLANKRUNE_HIGH};
		for (String id : new String[]{"rc_air", "rc_earth", "rc_fire"})
		{
			guide(a, guides, id, BankedActivity.Place.FIXED, "Rune or pure essence", ItemID.BLANKRUNE_HIGH, 0,
				essence);
		}
		guide(a, guides, "magic_superheat_iron", BankedActivity.Place.ANY_BANK, "Iron ore", ItemID.IRON_ORE, 0,
			ItemID.IRON_ORE);
		guide(a, guides, "magic_plank_make_oak", BankedActivity.Place.ANY_BANK, "Oak logs", ItemID.OAK_LOGS, 0,
			ItemID.OAK_LOGS);
		guide(a, guides, "magic_plank_make_teak", BankedActivity.Place.ANY_BANK, "Teak logs", ItemID.TEAK_LOGS, 0,
			ItemID.TEAK_LOGS);
		guide(a, guides, "magic_plank_make_mahogany", BankedActivity.Place.ANY_BANK, "Mahogany logs",
			ItemID.MAHOGANY_LOGS, 0, ItemID.MAHOGANY_LOGS);
		guide(a, guides, "magic_enchant_recoil", BankedActivity.Place.ANY_BANK, "Sapphire rings",
			ItemID.SAPPHIRE_RING, 0, ItemID.SAPPHIRE_RING);
		guide(a, guides, "magic_enchant_power", BankedActivity.Place.ANY_BANK, "Diamond amulets",
			ItemID.STRUNG_DIAMOND_AMULET, 0, ItemID.STRUNG_DIAMOND_AMULET);
		guide(a, guides, "magic_enchant_glory", BankedActivity.Place.ANY_BANK, "Dragonstone amulets",
			ItemID.STRUNG_DRAGONSTONE_AMULET, 0, ItemID.STRUNG_DRAGONSTONE_AMULET);
		// Strung bows come from stringing, so these go after it
		guide(a, guides, "magic_high_alch_yew", BankedActivity.Place.ANY_BANK, "Yew longbows", ItemID.YEW_LONGBOW, 2,
			ItemID.YEW_LONGBOW, ItemID.Cert.YEW_LONGBOW);
		guide(a, guides, "magic_high_alch_magic", BankedActivity.Place.ANY_BANK, "Magic longbows",
			ItemID.MAGIC_LONGBOW, 2, ItemID.MAGIC_LONGBOW, ItemID.Cert.MAGIC_LONGBOW);
	}

	/**
	 * One of Skilling Helper's methods as a banked activity, with the banked item moved to the first input.
	 */
	private static void guide(List<BankedActivity> a, GuideRepository guides, String methodId,
		BankedActivity.Place place, String itemName, int iconId, int stage, int... countIds)
	{
		Method m = guides.byId(methodId);
		if (m == null)
		{
			throw new IllegalStateException("No Skilling Helper method " + methodId);
		}
		ItemReq key = null;
		List<ItemReq> rest = new ArrayList<>();
		for (ItemReq input : m.getInputs())
		{
			if (key == null && input.matches(countIds[0]))
			{
				key = input;
			}
			else
			{
				rest.add(input);
			}
		}
		if (key == null)
		{
			throw new IllegalStateException(methodId + " doesn't use item " + countIds[0]);
		}
		Method banked = m.toBuilder()
			.id("banked_" + methodId)
			.clearInputs().input(key).inputs(rest)
			.build();
		a.add(activity(banked, place, itemName, iconId, Math.max(1, key.getPerAction()), stage, countIds));
	}

	// ---- Helpers ----

	/**
	 * A method done at a bank. The bank is a placeholder: the guide uses whichever bank is closest.
	 */
	private static Method.MethodBuilder bankMethod(String id, Skill skill, String name, int level, double xp,
		int actionsPerHour)
	{
		return Method.builder()
			.id(id)
			.skill(skill)
			.type(MethodType.PROCESS)
			.name(name)
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * actionsPerHour))
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE);
	}

	private static BankedActivity activity(Method m, BankedActivity.Place place, String itemName, int iconId,
		int perAction, int stage, int... countIds)
	{
		return BankedActivity.builder()
			.method(m)
			.place(place)
			.group(groupKey(countIds))
			.itemName(itemName)
			.iconId(iconId)
			.countIds(countIds)
			.perAction(perAction)
			.stage(stage)
			.build();
	}

	/**
	 * Activities that count the same items share a group.
	 */
	private static String groupKey(int... ids)
	{
		StringBuilder sb = new StringBuilder();
		for (int id : ids)
		{
			sb.append(sb.length() == 0 ? "" : "+").append(id);
		}
		return sb.toString();
	}

	private static String key(String name)
	{
		return name.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
	}

	private static String lower(String s)
	{
		return s.toLowerCase();
	}
}
