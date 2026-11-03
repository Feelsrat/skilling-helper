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
				.build(),
			rock("mine_iron_varrock", "Iron rocks (Varrock south-east)", 15, 35, 15_000, VARROCK_SE,
				Banks.VARROCK_EAST, new String[]{"Iron rocks"}, ItemID.IRON_ORE)
				.tag(MethodTag.POWER).tag(MethodTag.FAST).tag(MethodTag.CHEAP).dropByDefault(true)
				.notes("Stand where you can reach several iron rocks without moving and drop ore as you go.")
				.wikiPage(F2P_WIKI)
				.rateAt(30, 28_000).rateAt(45, 38_000).rateAt(60, 45_000).rateAt(75, 50_000)
				.build(),
			rock("mine_iron_al_kharid", "Iron rocks (Al Kharid)", 15, 35, 18_000,
				Location.of("Al Kharid mine (north iron)", 3298, 3310, 8), Banks.AL_KHARID,
				new String[]{"Iron rocks"}, ItemID.IRON_ORE)
				.tag(MethodTag.POWER).tag(MethodTag.FAST).dropByDefault(true)
				.wikiPage(F2P_WIKI)
				.rateAt(30, 32_000).rateAt(45, 42_000).rateAt(60, 50_000).rateAt(75, 55_000)
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
				.build(),
			rock("mine_granite_quarry", "Granite (Desert Quarry)", 45, 60, 50_000,
				Location.of("Desert Quarry", 3172, 2912, 10), null, new String[]{"Granite rocks"},
				ItemID.ENAKH_GRANITE_TINY, ItemID.ENAKH_GRANITE_SMALL, ItemID.ENAKH_GRANITE_MEDIUM)
				.members(true).tag(MethodTag.POWER).tag(MethodTag.FAST).dropByDefault(true)
				.supply(ItemReq.supply("Waterskins (desert heat)", 4, ItemID.WATER_SKIN4, ItemID.WATER_SKIN3,
					ItemID.WATER_SKIN2, ItemID.WATER_SKIN1))
				.notes("The fastest Mining xp from 45, but only with 3-tick mining (swamp tar on a clean herb, then "
					+ "mine). Without it the rates are much lower. Drop the granite as you go.")
				.wikiPage(P2P_WIKI)
				.rateAt(60, 65_000).rateAt(75, 85_000).rateAt(99, 110_000)
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
				.build(),
			rock("mine_amethyst_guild", "Amethyst (Mining Guild)", 92, 240, 20_000,
				Location.of("Mining Guild amethyst crystals", 3022, 9705, 8),
				Location.of("Mining Guild bank", 3013, 9718, 5), new String[]{"Amethyst crystals"}, ItemID.AMETHYST)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.requirementText("60 Mining to enter the Mining Guild")
				.notes("Very low effort and the amethyst sells well (or chisel it into darts and arrowtips).")
				.wikiPage(P2P_WIKI)
				.rateAt(99, 25_000)
				.build()
		);
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
