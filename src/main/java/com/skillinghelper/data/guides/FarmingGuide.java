package com.skillinghelper.data.guides;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import java.util.List;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Based on the OSRS wiki "Farming training" guide. Patch locations match RuneLite's world map. Runs visit several
 * patches; the guide points at the first one and lists the rest.
 */
final class FarmingGuide
{
	private static final String WIKI = "Farming_training";
	private static final ItemReq SPADE = ItemReq.tool("Spade", ItemID.SPADE);
	private static final ItemReq RAKE = ItemReq.tool("Rake", ItemID.RAKE);
	private static final ItemReq DIBBER = ItemReq.tool("Seed dibber", ItemID.DIBBER);

	static List<Method> methods()
	{
		return List.of(
			Method.builder()
				.id("farm_allotments")
				.skill(Skill.FARMING)
				.type(MethodType.ACTIVITY)
				.name("Potatoes in allotments (Falador)")
				.minLevel(1)
				.xpPerAction(9.5)
				.xpPerHour(3_000)
				.members(true)
				.tag(MethodTag.CHEAP)
				.location(Location.of("Falador farming patches", 3052, 3309, 8))
				.tool(SPADE).tool(RAKE).tool(DIBBER)
				.input(ItemReq.stack("Potato seeds", 3, ItemID.POTATO_SEED))
				.target(Target.object(null, "Allotment"))
				.target(Target.npc(null, "Elstan"))
				.instruction("Rake the allotments, plant 3 potato seeds in each and come back to harvest")
				.notes("Pay the gardener Elstan to protect the crops (a sack of potatoes later). Move on to tree runs at 15.")
				.wikiPage(WIKI)
				.build(),
			Method.builder()
				.id("farm_tithe")
				.skill(Skill.FARMING)
				.type(MethodType.ACTIVITY)
				.name("Tithe Farm")
				.minLevel(34)
				.xpPerAction(60)
				.xpPerHour(45_000)
				.members(true)
				.tag(MethodTag.MINIGAME).tag(MethodTag.CHEAP)
				.location(Location.of("Tithe Farm", 1796, 3501, 25))
				.tool(SPADE).tool(DIBBER)
				.tool(ItemReq.tool("Watering cans", ItemID.WATERING_CAN_8, ItemID.WATERING_CAN_7, ItemID.WATERING_CAN_6,
					ItemID.WATERING_CAN_5, ItemID.WATERING_CAN_4, ItemID.WATERING_CAN_3, ItemID.WATERING_CAN_2,
					ItemID.WATERING_CAN_1))
				.target(Target.object(null, "Seed table", "Tithe patch", "Sack", "Water barrel"))
				.instruction("Take seeds from the table, plant and water a full row of patches, water them twice more "
					+ "as they grow, then harvest and deposit the fruit in the sack")
				.notes("Costs nothing but watering cans and earns the Farmer's outfit (+2.5% Farming xp) and a seed box.")
				.wikiPage("Tithe_Farm")
				.build(),
			treeRun("farm_tree_oak", "Oak tree runs", 15, 481, "Oak saplings", ItemID.PLANTPOT_OAK_SAPLING, 20_000),
			treeRun("farm_tree_willow", "Willow tree runs", 30, 1_481, "Willow saplings", ItemID.PLANTPOT_WILLOW_SAPLING,
				50_000),
			treeRun("farm_tree_maple", "Maple tree runs", 45, 3_448, "Maple saplings", ItemID.PLANTPOT_MAPLE_SAPLING,
				100_000),
			treeRun("farm_tree_yew", "Yew tree runs", 60, 7_150, "Yew saplings", ItemID.PLANTPOT_YEW_SAPLING, 150_000),
			treeRun("farm_tree_magic", "Magic tree runs", 75, 13_913, "Magic saplings", ItemID.PLANTPOT_MAGIC_TREE_SAPLING,
				200_000),
			Method.builder()
				.id("farm_herb_runs")
				.skill(Skill.FARMING)
				.type(MethodType.ACTIVITY)
				.name("Herb runs (ranarr)")
				.minLevel(32)
				.xpPerAction(240)
				.xpPerHour(20_000)
				.members(true)
				.tag(MethodTag.PROFIT).tag(MethodTag.AFK)
				.location(Location.of("Falador herb patch", 3058, 3311, 6))
				.tool(SPADE).tool(RAKE).tool(DIBBER)
				.input(ItemReq.stack("Ranarr seeds", 1, ItemID.RANARR_SEED))
				.target(Target.object(null, "Herb patch"))
				.instruction("Harvest, rake and replant each herb patch: Falador, Catherby, Ardougne, Port Phasmatys "
					+ "and Hosidius")
				.notes("A run takes a few minutes every 80 minutes and is one of the best money makers. Use ultracompost "
					+ "and magic secateurs if you have them.")
				.wikiPage("Herb_run")
				.build()
		);
	}

	private static Method treeRun(String id, String name, int level, double xp, String saplingName, int sapling,
		int xpHr)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.FARMING)
			.type(MethodType.ACTIVITY)
			.name(name)
			.minLevel(level)
			// Planting plus checking health, per tree
			.xpPerAction(xp)
			// Tree runs are a few minutes of work, so this is xp per hour of actual playtime
			.xpPerHour(xpHr)
			.members(true)
			.tag(MethodTag.FAST).tag(MethodTag.AFK)
			.location(Location.of("Lumbridge tree patch", 3189, 3233, 6))
			.tool(SPADE)
			.input(ItemReq.consumed(saplingName, 5, 1, sapling))
			.target(Target.object(null, "Tree patch"))
			.target(Target.npc("Pay", "Fayeth", "Treznor", "Heskel", "Alain", "Prissy Scilla"))
			.instruction("Plant a sapling in each tree patch and pay the gardener to protect it: Lumbridge, Varrock, "
				+ "Falador, Taverley and the Gnome Stronghold")
			.notes("Trees take a few hours to grow; come back, check health and pay to have the tree removed. "
				+ "The most xp per minute in the game, but saplings cost money.")
			.wikiPage("Tree_run")
			.build();
	}

	private FarmingGuide()
	{
	}
}
