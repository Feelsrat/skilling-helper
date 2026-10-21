package com.skillinghelper.data.guides;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.PatchType;
import com.skillinghelper.data.Stop;
import com.skillinghelper.data.Target;
import java.util.List;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Based on the OSRS wiki "Farming training" guide. Patch locations and varbits match RuneLite's world map and Time
 * Tracking plugin. Runs lead the player from patch to patch, skipping patches that are still growing.
 */
final class FarmingGuide
{
	private static final String WIKI = "Farming_training";
	private static final ItemReq SPADE = ItemReq.tool("Spade", ItemID.SPADE);
	private static final ItemReq RAKE = ItemReq.tool("Rake", ItemID.RAKE);
	private static final ItemReq DIBBER = ItemReq.tool("Seed dibber", ItemID.DIBBER);
	private static final ItemReq COMPOST = ItemReq.supply("Compost (ultracompost is best)", 5,
		ItemID.BUCKET_ULTRACOMPOST, ItemID.BUCKET_SUPERCOMPOST, ItemID.BUCKET_COMPOST);

	// Each farming area keeps its herb patch in the same varbit; the varbit only means this patch when nearby
	private static final List<Stop> HERB_PATCHES = List.of(
		Stop.patch("herb_falador", "Falador herb patch", 3052, 3309, PatchType.HERB, VarbitID.FARMING_TRANSMIT_D,
			"Fastest: explorer's ring 2+ teleport, or Falador Teleport and walk south"),
		Stop.patch("herb_port_phasmatys", "Port Phasmatys herb patch", 3598, 3524, PatchType.HERB,
			VarbitID.FARMING_TRANSMIT_D, "Fastest: ectophial, then walk west"),
		Stop.patch("herb_catherby", "Catherby herb patch", 2810, 3462, PatchType.HERB, VarbitID.FARMING_TRANSMIT_D,
			"Fastest: Camelot Teleport, then walk east to Catherby"),
		Stop.patch("herb_ardougne", "Ardougne herb patch", 2663, 3375, PatchType.HERB, VarbitID.FARMING_TRANSMIT_D,
			"Fastest: Ardougne cloak farm teleport, or Ardougne Teleport and walk north"),
		Stop.patch("herb_hosidius", "Hosidius herb patch", 1729, 3558, PatchType.HERB, VarbitID.FARMING_TRANSMIT_D,
			"Fastest: Xeric's talisman (Xeric's Glade) or a Hosidius house teleport"));
	private static final List<Stop> TREE_PATCHES = List.of(
		Stop.patch("tree_lumbridge", "Lumbridge tree patch", 3189, 3233, PatchType.TREE, VarbitID.FARMING_TRANSMIT_A,
			"Fastest: Lumbridge Teleport or home teleport"),
		Stop.patch("tree_varrock", "Varrock tree patch", 3226, 3457, PatchType.TREE, VarbitID.FARMING_TRANSMIT_A,
			"Fastest: Varrock Teleport, then walk north-east to the castle"),
		Stop.patch("tree_falador", "Falador tree patch", 3005, 3375, PatchType.TREE, VarbitID.FARMING_TRANSMIT_A,
			"Fastest: Falador Teleport, then walk to Falador Park"),
		Stop.patch("tree_taverley", "Taverley tree patch", 2933, 3436, PatchType.TREE, VarbitID.FARMING_TRANSMIT_A,
			"Fastest: Falador Teleport and walk west, or a Taverley house teleport"),
		Stop.patch("tree_gnome_stronghold", "Gnome Stronghold tree patch", 2434, 3418, PatchType.TREE,
			VarbitID.FARMING_TRANSMIT_A, "Fastest: spirit tree or a necklace of passage to the Gnome Stronghold"));

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
				.stop(Stop.patch("allotment_falador_nw", "Falador allotment (north-west)", 3052, 3309,
					PatchType.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, "Fastest: Falador Teleport, then walk south"))
				.stop(Stop.patch("allotment_falador_se", "Falador allotment (south-east)", 3052, 3309,
					PatchType.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, null))
				.tool(SPADE).tool(RAKE).tool(DIBBER)
				.input(ItemReq.stack("Allotment seeds (potatoes to start; 3 per patch)", 3, ItemID.POTATO_SEED,
					ItemID.ONION_SEED, ItemID.CABBAGE_SEED, ItemID.TOMATO_SEED, ItemID.SWEETCORN_SEED, ItemID.STRAWBERRY_SEED,
					ItemID.WATERMELON_SEED, ItemID.SNAPE_GRASS_SEED))
				.supply(COMPOST)
				.target(Target.object(null, "Allotment"))
				.target(Target.npc(null, "Elstan"))
				.instruction("Harvest the crop (or rake the weeds), then plant 3 seeds")
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
			treeRun("farm_tree_oak", "Oak tree runs", 15, 481, "Oak saplings", ItemID.PLANTPOT_OAK_SAPLING, 20_000,
				basket("Baskets of tomatoes (gardener payment)", ItemID.BASKET_TOMATO_5, "tomatoes", ItemID.TOMATO)),
			treeRun("farm_tree_willow", "Willow tree runs", 30, 1_481, "Willow saplings", ItemID.PLANTPOT_WILLOW_SAPLING,
				50_000, basket("Baskets of apples (gardener payment)", ItemID.BASKET_APPLE_5, "cooking apples",
					ItemID.COOKING_APPLE)),
			treeRun("farm_tree_maple", "Maple tree runs", 45, 3_448, "Maple saplings", ItemID.PLANTPOT_MAPLE_SAPLING,
				100_000, basket("Baskets of oranges (gardener payment)", ItemID.BASKET_ORANGE_5, "oranges", ItemID.ORANGE)),
			treeRun("farm_tree_yew", "Yew tree runs", 60, 7_150, "Yew saplings", ItemID.PLANTPOT_YEW_SAPLING, 150_000,
				ItemReq.consumed("Cactus spines (10 per tree, gardener payment)", 50, 10, ItemID.CACTUS_SPINE)),
			treeRun("farm_tree_magic", "Magic tree runs", 75, 13_913, "Magic saplings", ItemID.PLANTPOT_MAGIC_TREE_SAPLING,
				200_000, ItemReq.consumed("Coconuts (25 per tree, gardener payment)", 125, 25, ItemID.COCONUT)),
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
				.stops(HERB_PATCHES)
				.tool(SPADE).tool(RAKE).tool(DIBBER)
				.input(ItemReq.stack("Herb seeds (ranarr is the usual pick; any herb you can plant works)", 1,
					ItemID.RANARR_SEED, ItemID.SNAPDRAGON_SEED, ItemID.TORSTOL_SEED, ItemID.TOADFLAX_SEED, ItemID.AVANTOE_SEED,
					ItemID.KWUARM_SEED, ItemID.CADANTINE_SEED, ItemID.LANTADYME_SEED, ItemID.DWARF_WEED_SEED, ItemID.IRIT_SEED,
					ItemID.HARRALANDER_SEED, ItemID.TARROMIN_SEED, ItemID.MARRENTILL_SEED, ItemID.GUAM_SEED,
					ItemID.HUASCA_SEED))
				.supply(COMPOST)
				.target(Target.object(null, "Herb patch"))
				.instruction("Harvest the herbs (or rake the weeds), add compost and plant a herb seed")
				.notes("A run takes a few minutes every 80 minutes and is one of the best money makers. Use ultracompost "
					+ "and magic secateurs if you have them.")
				.wikiPage("Herb_run")
				.build()
		);
	}

	/**
	 * A full basket of fruit for a gardener. These are rarely on the GE, so the guide has the player fill empty
	 * baskets (GE, or Sarah's farming shop south of Falador) with 5 fruit each.
	 */
	private static ItemReq basket(String name, int full, String fruitName, int fruit)
	{
		return ItemReq.consumed(name, 5, 1, full).madeFrom("Use " + fruitName + " on empty baskets (5 per basket). "
				+ "Empty baskets are on the GE or at Sarah's farming shop south of Falador", true,
			ItemReq.part("Empty baskets", 1, ItemID.BASKET_EMPTY),
			ItemReq.part(Character.toUpperCase(fruitName.charAt(0)) + fruitName.substring(1), 5, fruit));
	}

	private static Method treeRun(String id, String name, int level, double xp, String saplingName, int sapling,
		int xpHr, ItemReq payment)
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
			.stops(TREE_PATCHES)
			.tool(SPADE)
			.input(ItemReq.consumed(saplingName, 5, 1, sapling))
			.input(payment)
			.supply(ItemReq.supply("Coins (200 gp to have each grown tree removed)", 0, ItemID.COINS))
			.target(Target.object(null, "Tree patch"))
			.target(Target.npc("Pay", "Fayeth", "Treznor", "Heskel", "Alain", "Prissy Scilla"))
			.instruction("Check the grown tree's health and pay the gardener 200 gp to remove it, then plant a "
				+ "sapling and pay the gardener to protect it")
			.notes("Trees take a few hours to grow; come back, check health and pay to have the tree removed. Pay "
				+ "the gardener with the payment items to protect each tree. "
				+ "The most xp per minute in the game, but saplings cost money.")
			.wikiPage("Tree_run")
			.build();
	}

	private FarmingGuide()
	{
	}
}
