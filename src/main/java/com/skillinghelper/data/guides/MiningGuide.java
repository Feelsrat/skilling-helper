package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import com.skillinghelper.data.ToolType;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Based on the OSRS wiki "Free-to-play Mining training" and "Pay-to-play Mining training" guides.
 */
final class MiningGuide
{
	private static final String F2P_WIKI = "Free-to-play_Mining_training";
	private static final String P2P_WIKI = "Pay-to-play_Mining_training";
	private static final Location VARROCK_SE = Location.of("Varrock south-east mine", 3286, 3365, 8);

	static List<Method> methods()
	{
		return List.of(
			rock("mine_copper_tin_varrock", "Copper & tin (Varrock south-east)", 1, 17.5, 5_000, VARROCK_SE,
				Banks.VARROCK_EAST, new String[]{"Copper rocks", "Tin rocks"}, ItemID.COPPER_ORE, ItemID.TIN_ORE)
				.tag(MethodTag.POWER).tag(MethodTag.CHEAP).dropByDefault(true)
				.notes("Bank the ore instead of dropping it if you want to smelt bronze bars for Smithing.")
				.wikiPage(F2P_WIKI)
				.rateAt(10, 9_000).rateAt(15, 12_000)
				.howTo("The mine is just south-east of Varrock, outside the east gate: follow the arrow. Copper "
					+ "rocks have orange spots, tin rocks have grey ones.")
				.howTo("Mine both: you need one copper and one tin for each bronze bar if you want to smelt "
					+ "later.")
				.howTo(mine("copper or tin rock"))
				.build(),
			rock("mine_iron_varrock", "Iron rocks (Varrock south-east)", 15, 35, 15_000, VARROCK_SE,
				Banks.VARROCK_EAST, new String[]{"Iron rocks"}, ItemID.IRON_ORE)
				.tag(MethodTag.POWER).tag(MethodTag.FAST).tag(MethodTag.CHEAP).dropByDefault(true)
				.notes("Stand where you can reach several iron rocks without moving and drop ore as you go.")
				.wikiPage(F2P_WIKI)
				.rateAt(30, 28_000).rateAt(45, 38_000).rateAt(60, 45_000).rateAt(75, 50_000)
				.howTo("The mine is just south-east of Varrock, outside the east gate: follow the arrow. Iron "
					+ "rocks have dark red-brown spots.")
				.howTo("Stand on a tile that touches two or three iron rocks, so you can switch between them "
					+ "without walking.")
				.howTo(mine("iron rock"))
				.build(),
			rock("mine_iron_al_kharid", "Iron rocks (Al Kharid)", 15, 35, 18_000,
				Location.of("Al Kharid mine (north iron)", 3298, 3310, 8), Banks.AL_KHARID,
				new String[]{"Iron rocks"}, ItemID.IRON_ORE)
				.tag(MethodTag.POWER).tag(MethodTag.FAST).dropByDefault(true)
				.wikiPage(F2P_WIKI)
				.rateAt(30, 32_000).rateAt(45, 42_000).rateAt(60, 50_000).rateAt(75, 55_000)
				.howTo("The Al Kharid mine is the long rocky valley north of Al Kharid: follow the arrow to the "
					+ "iron rocks (dark red-brown spots) in the north part.")
				.howTo("Stand on a tile that touches two or three iron rocks, so you can switch between them "
					+ "without walking.")
				.howTo(mine("iron rock"))
				.build(),
			rock("mine_gem_shilo", "Gem rocks (Shilo Village mine)", 40, 65, 40_000,
				Location.of("Shilo Village underground mine", 2842, 9388, 12), Banks.SHILO,
				new String[]{"Gem rocks"}, ItemID.UNCUT_OPAL, ItemID.UNCUT_JADE, ItemID.UNCUT_RED_TOPAZ,
				ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND)
				.members(true).tag(MethodTag.PROFIT)
				.quest(Quest.SHILO_VILLAGE)
				.requirementText("Medium Karamja Diary for the underground part of the mine")
				.notes("Wear a charged amulet of glory to find gem rocks faster. A gem bag saves trips; there's a "
					+ "deposit box in the mine.")
				.wikiPage(P2P_WIKI)
				.rateAt(50, 46_000).rateAt(60, 52_000).rateAt(70, 58_000).rateAt(80, 66_000).rateAt(90, 72_000)
				.getThere("Shilo Village is in the south of Karamja; the gem mine is in the north-east of the "
					+ "village. The rocks under the ground (down the stairs) need the medium Karamja Diary.")
				.howTo("Gem rocks are the rocks with colourful sparkles. Each one gives a random uncut gem (opal "
					+ "up to diamond).")
				.howTo(mine("gem rock"))
				.howTo("Bank at the deposit box inside the mine, or the Shilo Village bank.")
				.build(),
			rock("mine_granite_quarry", "Granite (Desert Quarry)", 45, 60, 50_000,
				Location.of("Desert Quarry", 3172, 2912, 10), null, new String[]{"Granite rocks"},
				ItemID.ENAKH_GRANITE_TINY, ItemID.ENAKH_GRANITE_SMALL, ItemID.ENAKH_GRANITE_MEDIUM)
				.members(true).tag(MethodTag.POWER).tag(MethodTag.FAST).dropByDefault(true)
				.supply(ItemReq.supply("Waterskins (desert heat)", 4, ItemID.WATER_SKIN4, ItemID.WATER_SKIN3,
					ItemID.WATER_SKIN2, ItemID.WATER_SKIN1).bestFirst(Supplies.WATERSKIN_NAMES))
				.notes("The fastest Mining xp from 45, but only with 3-tick mining (swamp tar on a clean herb, then "
					+ "mine). Without it the rates are much lower. Drop the granite as you go.")
				.wikiPage(P2P_WIKI)
				.rateAt(60, 65_000).rateAt(75, 85_000).rateAt(99, 110_000)
				.getThere("The Desert Quarry is in the Kharidian Desert south of the Shantay Pass: follow the "
					+ "arrow. Bring waterskins: the desert heat hurts you every minute without water.")
				.howTo("Granite rocks are the big grey rocks in the quarry.")
				.howTo(mine("granite rock"))
				.howTo("Drop the granite as you go (shift-click). Granite is heavy and sells for little.")
				.build(),
			Method.builder()
				.id("mine_motherlode")
				.skill(Skill.MINING)
				.type(MethodType.ACTIVITY)
				.name("Motherlode Mine")
				.minLevel(30)
				.xpPerAction(60)
				.xpPerHour(20_000)
				.members(true)
				.tag(MethodTag.AFK).tag(MethodTag.PROFIT).tag(MethodTag.MINIGAME)
				.location(Location.of("Motherlode Mine", 3757, 5666, 12))
				.toolType(ToolType.PICKAXE)
				.supply(ItemReq.supply("Hammer (to fix broken struts)", 1, ItemID.HAMMER))
				.target(Target.object("Mine", "Ore vein"))
				.target(Target.object("Deposit", "Hopper"))
				.target(Target.object("Search", "Sack"))
				.target(Target.object("Deposit", "Bank deposit box"))
				.instruction("Mine ore veins until your inventory is full of pay-dirt, deposit it in the hopper, "
					+ "and search the sack when it fills up. Fix broken struts with a hammer for bonus ore.")
				.notes("Very AFK, earns golden nuggets for the Prospector kit (+2.5% Mining xp).")
				.wikiPage("Motherlode_Mine")
				.rateAt(45, 28_000).rateAt(60, 35_000).rateAt(72, 45_000).rateAt(85, 55_000)
				.getThere("Take the stairs in the house in north-east Falador (east of the Party Room): they lead "
					+ "down into the Dwarven Mine right next to the Motherlode cave. A skills necklace (Mining "
					+ "Guild) lands close by too.")
				.howTo("Bring a pickaxe and a hammer. Leave everything else in the bank: you want an empty "
					+ "inventory.")
				.howTo("Click a highlighted ore vein (the glowing veins in the walls) and keep mining it until "
					+ "your inventory is full of pay-dirt.")
				.howTo("Carry the pay-dirt to the highlighted hopper in the middle of the mine and click it to "
					+ "deposit everything.")
				.howTo("If the water wheels next to the hopper have stopped, click the broken strut with your "
					+ "hammer to fix it, or the pay-dirt doesn't move along.")
				.howTo("Repeat about four times: the counter on screen shows how much pay-dirt is in the sack. "
					+ "When the chat says the sack is getting full, click the highlighted sack to collect the "
					+ "ore, then put it in the bank deposit box next to it.")
				.howTo("Trade the golden nuggets you find to Prospector Percy for the Prospector kit (more Mining "
					+ "xp).")
				.build(),
			rock("mine_amethyst_guild", "Amethyst (Mining Guild)", 92, 240, 20_000,
				Location.of("Mining Guild amethyst crystals", 3022, 9705, 8),
				Location.of("Mining Guild bank", 3013, 9718, 5), new String[]{"Amethyst crystals"}, ItemID.AMETHYST)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.requirementText("60 Mining to enter the Mining Guild")
				.notes("Very low effort and the amethyst sells well (or chisel it into darts and arrowtips).")
				.wikiPage(P2P_WIKI)
				.rateAt(99, 25_000)
				.getThere("The Mining Guild entrance is the building just south of Falador east bank: climb down "
					+ "the ladder inside. The door only opens at 60 Mining.")
				.howTo("The amethyst crystals are the purple crystal walls in the south-east part of the guild "
					+ "(the patch west of the bank chest needs the elite Falador Diary).")
				.howTo(mine("amethyst crystal"))
				.build()
		);
	}

	/**
	 * The beginner steps every rock shares, after the "where are the rocks" step.
	 */
	private static List<String> mine(String rock)
	{
		return List.of(
			"Have your pickaxe in your inventory, or wield it to free a slot (the guide picks your best one).",
			"Left-click a highlighted " + rock + " once (\"Mine\"). Your character keeps swinging on their own until "
				+ "the ore comes out.",
			"The rock goes grey (empty) after it gives ore. Click another highlighted " + rock + " straight away; the "
				+ "empty one fills back up after a while.",
			"When your inventory is full, the guide moves on by itself: it tells you to drop the ore or takes you to "
				+ "the bank.");
	}

	private static Method.MethodBuilder rock(String id, String name, int level, double xp, int xpHr, Location location,
		Location bank, String[] rocks, int... ores)
	{
		Method.MethodBuilder b = Method.builder()
			.id(id)
			.skill(Skill.MINING)
			.type(MethodType.GATHER)
			.name(name)
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour(xpHr)
			.location(location)
			.bank(bank)
			.toolType(ToolType.PICKAXE)
			.target(Target.object("Mine", rocks))
			.instruction("Mine the " + String.join(" and ", rocks).toLowerCase());
		for (int ore : ores)
		{
			b.product(ore);
		}
		return b;
	}

	private MiningGuide()
	{
	}
}
