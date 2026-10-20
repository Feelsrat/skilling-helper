package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import com.skillinghelper.data.ToolType;
import java.util.List;
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
			rock("mine_copper_tin_varrock", "Copper & tin (Varrock south-east)", 1, 17.5, 12_000, VARROCK_SE,
				Banks.VARROCK_EAST, new String[]{"Copper rocks", "Tin rocks"}, ItemID.COPPER_ORE, ItemID.TIN_ORE)
				.tag(MethodTag.POWER).tag(MethodTag.CHEAP).dropByDefault(true)
				.notes("Bank the ore instead of dropping it if you want to smelt bronze bars for Smithing.")
				.wikiPage(F2P_WIKI)
				.build(),
			rock("mine_iron_varrock", "Iron rocks (Varrock south-east)", 15, 35, 30_000, VARROCK_SE,
				Banks.VARROCK_EAST, new String[]{"Iron rocks"}, ItemID.IRON_ORE)
				.tag(MethodTag.POWER).tag(MethodTag.FAST).tag(MethodTag.CHEAP).dropByDefault(true)
				.notes("Stand where you can reach several iron rocks without moving and drop ore as you go.")
				.wikiPage(F2P_WIKI)
				.build(),
			rock("mine_iron_al_kharid", "Iron rocks (Al Kharid)", 15, 35, 35_000,
				Location.of("Al Kharid mine (north iron)", 3298, 3310, 8), Banks.AL_KHARID,
				new String[]{"Iron rocks"}, ItemID.IRON_ORE)
				.tag(MethodTag.POWER).tag(MethodTag.FAST).dropByDefault(true)
				.wikiPage(F2P_WIKI)
				.build(),
			Method.builder()
				.id("mine_motherlode")
				.skill(Skill.MINING)
				.type(MethodType.ACTIVITY)
				.name("Motherlode Mine")
				.minLevel(30)
				.xpPerAction(60)
				.xpPerHour(30_000)
				.members(true)
				.tag(MethodTag.AFK).tag(MethodTag.PROFIT).tag(MethodTag.MINIGAME)
				.location(Location.of("Motherlode Mine", 3757, 5666, 12))
				.toolType(ToolType.PICKAXE)
				.target(Target.object("Mine", "Ore vein"))
				.target(Target.object("Deposit", "Hopper"))
				.target(Target.object("Search", "Sack"))
				.target(Target.object("Deposit", "Bank deposit box"))
				.instruction("Mine ore veins until your inventory is full of pay-dirt, deposit it in the hopper, "
					+ "and search the sack when it fills up. Fix broken struts with a hammer for bonus ore.")
				.notes("Very AFK, earns golden nuggets for the Prospector kit (+2.5% Mining xp).")
				.wikiPage("Motherlode_Mine")
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
