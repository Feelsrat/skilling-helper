package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Based on the OSRS wiki "Herblore training" guide. Every potion is made from a bought unfinished potion and a
 * secondary, so costs come straight from GE prices.
 */
final class HerbloreGuide
{
	private static final String WIKI = "Herblore_training";
	// 14 potions per trip, about 2,500 an hour
	private static final int POTIONS_PER_HOUR = 2_500;
	// One click cleans the whole inventory now; about 3,000 an hour with banking (wiki AFK guide: 45k xp/hr torstol)
	private static final int HERBS_CLEANED_PER_HOUR = 3_000;

	static List<Method> methods()
	{
		List<Method> m = new ArrayList<>();
		m.add(Method.builder()
			.id("herb_druidic_ritual")
			.skill(Skill.HERBLORE)
			.type(MethodType.ACTIVITY)
			.name("Druidic Ritual (quest)")
			.minLevel(1)
			.xpPerAction(250)
			.xpPerHour(1_000)
			.members(true)
			.location(Location.of("Taverley stone circle", 2925, 3486, 10))
			.target(Target.npc("Talk-to", "Kaqemeex", "Sanfew"))
			.instruction("Talk to Kaqemeex at the Taverley stone circle to start Druidic Ritual")
			.notes("The quest unlocks Herblore and takes you straight to level 3. Use Quest Helper for the steps.")
			.wikiPage("Druidic_Ritual")
			.howTo("Talk to the highlighted Kaqemeex at the stone circle north of Taverley to start the quest. He "
				+ "sends you to Sanfew.")
			.howTo("Go to the herb shop house in the middle of Taverley, climb the stairs and talk to Sanfew. He "
				+ "asks for four kinds of enchanted meat.")
			.howTo("Get raw beef, raw rat meat, raw bear meat and raw chicken (kill a cow, rat, bear and chicken, "
				+ "or buy them on the GE).")
			.howTo("Go into Taverley Dungeon (ladder south of the stone circle), past the gate with the suits of "
				+ "armour, and use each raw meat on the Cauldron of Thunder.")
			.howTo("Bring the four enchanted meats to Sanfew, then talk to Kaqemeex again to finish. Herblore is "
				+ "now unlocked at level 3.")
			.build());
		m.add(potion("herb_attack", "Attack potions", 3, 25, ItemID.GUAMVIAL, "Guam potions (unf)", ItemID.GUAM_LEAF,
			ItemID.EYE_OF_NEWT, "Eyes of newt", ItemID._3DOSE1ATTACK).tag(MethodTag.CHEAP).build());
		m.add(potion("herb_antipoison", "Antipoisons", 5, 37.5, ItemID.MARRENTILLVIAL, "Marrentill potions (unf)",
			ItemID.MARENTILL, ItemID.UNICORN_HORN_DUST, "Unicorn horn dust", ItemID._3DOSEANTIPOISON).build());
		m.add(potion("herb_strength", "Strength potions", 12, 50, ItemID.TARROMINVIAL, "Tarromin potions (unf)",
			ItemID.TARROMIN, ItemID.LIMPWURT_ROOT, "Limpwurt roots", ItemID._3DOSE1STRENGTH).build());
		m.add(potion("herb_energy", "Energy potions", 26, 67.5, ItemID.HARRALANDERVIAL, "Harralander potions (unf)",
			ItemID.HARRALANDER, ItemID.CHOCOLATE_DUST, "Chocolate dust", ItemID._3DOSE1ENERGY).build());
		m.add(potion("herb_prayer", "Prayer potions", 38, 87.5, ItemID.RANARRVIAL, "Ranarr potions (unf)",
			ItemID.RANARR_WEED, ItemID.SNAPE_GRASS, "Snape grass", ItemID._3DOSEPRAYERRESTORE)
			.tag(MethodTag.PROFIT).build());
		m.add(potion("herb_super_attack", "Super attack potions", 45, 100, ItemID.IRITVIAL, "Irit potions (unf)",
			ItemID.IRIT_LEAF, ItemID.EYE_OF_NEWT, "Eyes of newt", ItemID._3DOSE2ATTACK).build());
		m.add(potion("herb_super_strength", "Super strength potions", 55, 125, ItemID.KWUARMVIAL,
			"Kwuarm potions (unf)", ItemID.KWUARM, ItemID.LIMPWURT_ROOT, "Limpwurt roots", ItemID._3DOSE2STRENGTH)
			.build());
		m.add(potion("herb_super_restore", "Super restores", 63, 142.5, ItemID.SNAPDRAGONVIAL,
			"Snapdragon potions (unf)", ItemID.SNAPDRAGON, ItemID.RED_SPIDERS_EGGS, "Red spiders' eggs",
			ItemID._3DOSE2RESTORE).build());
		m.add(potion("herb_super_defence", "Super defence potions", 66, 150, ItemID.CADANTINEVIAL,
			"Cadantine potions (unf)", ItemID.CADANTINE, ItemID.WHITE_BERRIES, "White berries", ItemID._3DOSE2DEFENSE)
			.build());
		m.add(potion("herb_super_antipoison", "Super antipoisons", 48, 106.3, ItemID.IRITVIAL, "Irit potions (unf)",
			ItemID.IRIT_LEAF, ItemID.UNICORN_HORN_DUST, "Unicorn horn dust", ItemID._3DOSE2ANTIPOISON).build());
		m.add(potion("herb_super_energy", "Super energy potions", 52, 117.5, ItemID.AVANTOEVIAL,
			"Avantoe potions (unf)", ItemID.AVANTOE, ItemID.MORTMYREMUSHROOM, "Mort myre fungi",
			ItemID._3DOSE2ENERGY).build());
		m.add(potion("herb_antifire", "Antifire potions", 69, 157.5, ItemID.LANTADYMEVIAL, "Lantadyme potions (unf)",
			ItemID.LANTADYME, ItemID.DRAGON_SCALE_DUST, "Dragon scale dust", ItemID._3DOSE1ANTIDRAGON).build());
		m.add(potion("herb_ranging", "Ranging potions", 72, 162.5, ItemID.DWARFWEEDVIAL, "Dwarf weed potions (unf)",
			ItemID.DWARF_WEED, ItemID.WINE_OF_ZAMORAK, "Wines of Zamorak", ItemID._3DOSERANGERSPOTION).build());
		m.add(potion("herb_magic", "Magic potions", 76, 172.5, ItemID.LANTADYMEVIAL, "Lantadyme potions (unf)",
			ItemID.LANTADYME, ItemID.CACTUS_POTATO, "Potato cacti", ItemID._3DOSE1MAGIC).build());
		m.add(potion("herb_saradomin_brew", "Saradomin brews", 81, 180, ItemID.TOADFLAXVIAL, "Toadflax potions (unf)",
			ItemID.TOADFLAX, ItemID.CRUSHED_BIRD_NEST, "Crushed nests", ItemID._3DOSEPOTIONOFSARADOMIN).build());
		m.add(Method.builder()
			.id("herb_super_combat")
			.skill(Skill.HERBLORE)
			.type(MethodType.PROCESS)
			.name("Make super combat potions")
			.minLevel(90)
			.xpPerAction(150)
			.xpPerHour(150 * 2_000)
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.PROFIT)
			.quest(Quest.DRUIDIC_RITUAL)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.consumed("Torstol potions (unf) or clean torstol", 7, 1, ItemID.TORSTOLVIAL, ItemID.TORSTOL))
			.input(ItemReq.consumed("Super attack (4)", 7, 1, ItemID._4DOSE2ATTACK))
			.input(ItemReq.consumed("Super strength (4)", 7, 1, ItemID._4DOSE2STRENGTH))
			.input(ItemReq.consumed("Super defence (4)", 7, 1, ItemID._4DOSE2DEFENSE))
			.product(ItemID._4DOSECOMBAT)
			.useItem(ItemID.TORSTOL).useItem(ItemID.TORSTOLVIAL).useItem(ItemID._4DOSE2ATTACK)
			.instruction("Use the torstol on a super attack potion and press Space to make them all")
			.notes("Each potion takes a torstol and one 4-dose super attack, strength and defence.")
			.wikiPage("Money_making_guide/Making_super_combat_potions")
			.howTo("Stand next to any banker or bank booth (the Grand Exchange has lots in the middle).")
			.howTo("Take out 7 torstol, 7 super attack (4), 7 super strength (4) and 7 super defence (4) potions.")
			.howTo("Click a torstol, then a super attack potion. Press Space to make them all.")
			.howTo("Bank the super combats, take out more and repeat.")
			.build());
		// From the wiki's AFK guide: tar and stackable secondaries make a whole inventory in one click
		m.add(tar("herb_guam_tar", "Guam tar", 19, 30, ItemID.GUAM_LEAF, "Clean guam", ItemID.SALAMANDER_TAR_GREEN)
			.build());
		m.add(tar("herb_harralander_tar", "Harralander tar", 44, 72.5, ItemID.HARRALANDER, "Clean harralander",
			ItemID.SALAMANDER_TAR_BLACK).build());
		m.add(tar("herb_irit_tar", "Irit tar", 55, 85, ItemID.IRIT_LEAF, "Clean irit", ItemID.SALAMANDER_TAR_MOUNTAIN)
			.build());
		m.add(Method.builder()
			.id("herb_stamina")
			.skill(Skill.HERBLORE)
			.type(MethodType.PROCESS)
			.name("Make stamina potions")
			.minLevel(77)
			.xpPerAction(102)
			.xpPerHour(102 * 2_700)
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.AFK)
			.quest(Quest.DRUIDIC_RITUAL)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.consumed("Super energy (4)", 27, 1, ItemID._4DOSE2ENERGY))
			.input(ItemReq.stack("Amylase crystals", 4, ItemID.AMYLASE))
			.product(ItemID._4DOSESTAMINA)
			.useItem(ItemID.AMYLASE).useItem(ItemID._4DOSE2ENERGY)
			.instruction("Use the amylase crystals on a super energy (4) and press Space to make them all")
			.notes("Amylase stacks, so one click makes 27 potions. Amylase comes from Mark of grace trades or the "
				+ "GE.")
			.wikiPage(WIKI)
			.howTo(HowTo.bankStanding("27 super energy (4) and a stack of amylase crystals (4 per potion)",
				"the amylase crystals, then a super energy", "stamina potions"))
			.build());
		m.add(Method.builder()
			.id("herb_anti_venom")
			.skill(Skill.HERBLORE)
			.type(MethodType.PROCESS)
			.name("Make anti-venoms")
			.minLevel(87)
			.xpPerAction(120)
			.xpPerHour(120 * 2_700)
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.AFK)
			.quest(Quest.DRUIDIC_RITUAL)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.consumed("Antidote++ (4)", 27, 1, ItemID.ANTIDOTE__4))
			.input(ItemReq.stack("Zulrah's scales", 20, ItemID.SNAKEBOSS_SCALE))
			.product(ItemID.ANTIVENOM4)
			.useItem(ItemID.SNAKEBOSS_SCALE).useItem(ItemID.ANTIDOTE__4)
			.instruction("Use the Zulrah's scales on an antidote++ (4) and press Space to make them all")
			.notes("The scales stack, so one click makes 27 potions. The wiki's AFK guide uses this for its rates.")
			.wikiPage(WIKI)
			.howTo(HowTo.bankStanding("27 antidote++ (4) and a stack of Zulrah's scales (20 per potion)",
				"the Zulrah's scales, then an antidote++", "anti-venoms"))
			.build());
		m.add(clean("herb_clean_guam", "guam leaves", 3, 2.5, ItemID.UNIDENTIFIED_GUAM, ItemID.GUAM_LEAF).build());
		m.add(clean("herb_clean_marrentill", "marrentill", 5, 3.8, ItemID.UNIDENTIFIED_MARENTILL, ItemID.MARENTILL)
			.build());
		m.add(clean("herb_clean_tarromin", "tarromin", 11, 5, ItemID.UNIDENTIFIED_TARROMIN, ItemID.TARROMIN).build());
		m.add(clean("herb_clean_harralander", "harralander", 20, 6.3, ItemID.UNIDENTIFIED_HARRALANDER,
			ItemID.HARRALANDER).build());
		m.add(clean("herb_clean_ranarr", "ranarr weed", 25, 7.5, ItemID.UNIDENTIFIED_RANARR, ItemID.RANARR_WEED)
			.build());
		m.add(clean("herb_clean_toadflax", "toadflax", 30, 8, ItemID.UNIDENTIFIED_TOADFLAX, ItemID.TOADFLAX).build());
		m.add(clean("herb_clean_irit", "irit leaves", 40, 8.8, ItemID.UNIDENTIFIED_IRIT, ItemID.IRIT_LEAF).build());
		m.add(clean("herb_clean_avantoe", "avantoe", 48, 10, ItemID.UNIDENTIFIED_AVANTOE, ItemID.AVANTOE).build());
		m.add(clean("herb_clean_kwuarm", "kwuarm", 54, 11.3, ItemID.UNIDENTIFIED_KWUARM, ItemID.KWUARM).build());
		m.add(clean("herb_clean_snapdragon", "snapdragon", 59, 11.8, ItemID.UNIDENTIFIED_SNAPDRAGON,
			ItemID.SNAPDRAGON).build());
		m.add(clean("herb_clean_cadantine", "cadantine", 65, 12.5, ItemID.UNIDENTIFIED_CADANTINE, ItemID.CADANTINE)
			.build());
		m.add(clean("herb_clean_lantadyme", "lantadyme", 67, 13.1, ItemID.UNIDENTIFIED_LANTADYME, ItemID.LANTADYME)
			.build());
		m.add(clean("herb_clean_dwarf_weed", "dwarf weed", 70, 13.8, ItemID.UNIDENTIFIED_DWARF_WEED,
			ItemID.DWARF_WEED).build());
		m.add(clean("herb_clean_torstol", "torstol", 75, 15, ItemID.UNIDENTIFIED_TORSTOL, ItemID.TORSTOL).build());
		return m;
	}

	/**
	 * Secondaries the player can grind themselves from a cheaper item, e.g. unicorn horns into dust.
	 */
	private static ItemReq ground(String name, int secondary)
	{
		ItemReq req = ItemReq.consumed(name, 14, 1, secondary);
		switch (secondary)
		{
			case ItemID.UNICORN_HORN_DUST:
				return grind(req, "Unicorn horns", ItemID.UNICORN_HORN);
			case ItemID.CHOCOLATE_DUST:
				return grind(req, "Chocolate bars", ItemID.CHOCOLATE_BAR);
			case ItemID.CRUSHED_BIRD_NEST:
				return grind(req, "Empty bird nests", ItemID.BIRD_NEST_EMPTY);
			default:
				return req;
		}
	}

	private static ItemReq grind(ItemReq req, String fromName, int from)
	{
		return req.madeFrom("Use the pestle and mortar on the " + fromName.toLowerCase(), false,
			ItemReq.part(fromName, 1, from), ItemReq.tool("Pestle and mortar", ItemID.PESTLE_AND_MORTAR));
	}

	/**
	 * Cleaning grimy herbs at a bank: low xp, but a steady wiki money maker that needs nothing but the herbs.
	 */
	private static Method.MethodBuilder clean(String id, String herbName, int level, double xp, int grimy, int clean)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.HERBLORE)
			.type(MethodType.PROCESS)
			.name("Clean grimy " + herbName)
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * HERBS_CLEANED_PER_HOUR))
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.PROFIT)
			.quest(Quest.DRUIDIC_RITUAL)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.consumed("Grimy " + herbName, 28, 1, grimy))
			.product(clean)
			.useItem(grimy)
			.instruction("Click a grimy " + herbName + " once to clean the whole inventory, then bank and repeat")
			.howTo("Stand next to any banker or bank booth (the Grand Exchange has lots in the middle).")
			.howTo("Open the bank and take out 28 grimy " + herbName + " (the guide highlights them).")
			.howTo("Close the bank and click one grimy herb (\"Clean\"). Your character cleans the rest of the "
				+ "inventory by themselves.")
			.howTo("When they're all clean, deposit them, take out 28 more and repeat.")
			.notes("Slow xp but it pays: clean herbs sell for more than grimy ones. Profit depends on today's prices.")
			.wikiPage("Money_making_guide/Cleaning_grimy_" + herbName.replace(' ', '_'));
	}

	/**
	 * Grinding a herb into 15 tar with swamp tar: the swamp tar and the tar stack, so a whole inventory of herbs goes in
	 * one click.
	 */
	private static Method.MethodBuilder tar(String id, String name, int level, double xp, int herb, String herbName,
		int product)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.HERBLORE)
			.type(MethodType.PROCESS)
			.name("Make " + name.toLowerCase())
			.minLevel(level)
			.xpPerAction(xp)
			// One herb per 2 ticks with Make-All, about 2,000 an hour with banking (wiki AFK guide: 170k xp/hr irit)
			.xpPerHour((int) (xp * 2_000))
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.AFK)
			.quest(Quest.DRUIDIC_RITUAL)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.tool(ItemReq.tool("Pestle and mortar", ItemID.PESTLE_AND_MORTAR))
			.input(ItemReq.consumed(herbName, 26, 1, herb))
			.input(ItemReq.stack("Swamp tar", 15, ItemID.SWAMP_TAR))
			.product(product)
			.useItem(herb).useItem(ItemID.SWAMP_TAR)
			.instruction("Use the " + herbName.toLowerCase() + " on the swamp tar and press Space to make it all")
			.notes("Each herb and 15 swamp tar make 15 " + name.toLowerCase() + " for Hunter salamanders. Swamp tar "
				+ "is cheap on the GE or picked in Lumbridge Swamp.")
			.wikiPage("Herblore_training#Making_tar")
			.howTo(HowTo.bankStanding("your pestle and mortar, the swamp tar (15 per herb) and 26 "
					+ herbName.toLowerCase(), "a herb, then the swamp tar", name.toLowerCase()));
	}

	private static Method.MethodBuilder potion(String id, String name, int level, double xp, int unf, String unfName,
		int herb, int secondary, String secondaryName, int product)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.HERBLORE)
			.type(MethodType.PROCESS)
			.name("Make " + name.toLowerCase())
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * POTIONS_PER_HOUR))
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.FAST)
			.quest(Quest.DRUIDIC_RITUAL)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.consumed(unfName, 14, 1, unf).madeFrom("Use the clean herbs on vials of water (clean any "
					+ "grimy herbs first)", false,
				ItemReq.part("Clean " + unfName.substring(0, unfName.indexOf(' ')).toLowerCase(), 1, herb),
				ItemReq.part("Vials of water", 1, ItemID.VIAL_WATER)))
			.input(ground(secondaryName, secondary))
			.product(product)
			.useItem(secondary).useItem(unf)
			.instruction("Use the " + secondaryName.toLowerCase() + " on the " + unfName.toLowerCase()
				+ " and press Space to make all")
			.howTo(HowTo.bankStanding("14 " + unfName.toLowerCase() + " and 14 " + secondaryName.toLowerCase(),
				"the " + secondaryName.toLowerCase() + ", then an unfinished potion",
				name.toLowerCase()))
			.notes("If you have the herbs and vials, or they're cheaper than unfinished potions, the guide has you make "
				+ "the unfinished potions first. An amulet of chemistry gives a chance of 4-dose potions.")
			.wikiPage(WIKI);
	}

	private HerbloreGuide()
	{
	}
}
