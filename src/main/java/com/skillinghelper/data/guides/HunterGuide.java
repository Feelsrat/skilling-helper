package com.skillinghelper.data.guides;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Based on the OSRS wiki "Hunter training" guide. Hunting areas match RuneLite's world map.
 */
final class HunterGuide
{
	private static final String WIKI = "Hunter_training";
	// Enough for the most traps you can lay below level 60; extras cost almost nothing
	private static final ItemReq SNARES = ItemReq.tools("Bird snares", 3, ItemID.HUNTING_OJIBWAY_BIRD_SNARE);
	private static final String TRAPS = "You can set one more trap every 20 levels (two at 20, three at 40...).";

	static List<Method> methods()
	{
		return List.of(
			birds("hunter_crimson_swift", "Crimson swifts (Feldip Hills)", 1, 34, 7_000,
				Location.of("Feldip hunter area", 2557, 2912, 25), "Crimson swift", ItemID.HUNTING_JUNGLE_FEATHER)
				.build(),
			birds("hunter_copper_longtail", "Copper longtails (Piscatoris)", 9, 61, 10_000,
				Location.of("Piscatoris hunter area", 2335, 3584, 25), "Copper longtail",
				ItemID.HUNTING_WOODLAND_FEATHER)
				.build(),
			birds("hunter_tropical_wagtail", "Tropical wagtails (Feldip Hills)", 19, 95, 18_000,
				Location.of("Feldip hunter area", 2557, 2912, 25), "Tropical wagtail", ItemID.HUNTING_STRIPY_BIRD_FEATHER)
				.build(),
			Method.builder()
				.id("hunter_swamp_lizards")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Swamp lizards (Canifis)")
				.minLevel(29)
				.xpPerAction(152)
				.xpPerHour(30_000)
				.members(true)
				.quest(Quest.PRIEST_IN_PERIL)
				.location(Location.of("Canifis hunter area", 3553, 3438, 20))
				.tool(ItemReq.tools("Ropes", 3, ItemID.ROPE))
				.tool(ItemReq.tools("Small fishing nets", 3, ItemID.NET))
				.target(Target.object(null, "Young tree", "Net trap"))
				.target(Target.npc(null, "Swamp lizard"))
				.instruction("Set net traps on the young trees with a rope and small net, then check them when a "
					+ "lizard is caught")
				.notes("One rope and net per trap; a spare of each covers traps that collapse. " + TRAPS)
				.wikiPage(WIKI)
				.build(),
			Method.builder()
				.id("hunter_birdhouses")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Birdhouse runs (Fossil Island)")
				.minLevel(5)
				.xpPerAction(280)
				.xpPerHour(60_000)
				.members(true)
				.tag(MethodTag.AFK).tag(MethodTag.CHEAP)
				.quest(Quest.BONE_VOYAGE)
				.location(Location.of("Fossil Island (House on the Hill)", 3763, 3869, 60))
				.tool(ItemReq.tool("Hammer", ItemID.HAMMER))
				.tool(ItemReq.tool("Chisel", ItemID.CHISEL))
				.input(ItemReq.consumed("Logs for your level (oak 14+, willow 24+, teak 34+, maple 44+, mahogany 49+, "
					+ "yew 59+, magic 74+)", 4, 1, ItemID.OAK_LOGS, ItemID.LOGS, ItemID.WILLOW_LOGS, ItemID.TEAK_LOGS,
					ItemID.MAPLE_LOGS, ItemID.MAHOGANY_LOGS, ItemID.YEW_LOGS, ItemID.MAGIC_LOGS, ItemID.REDWOOD_LOGS))
				.input(ItemReq.consumed("Clockworks", 4, 1, ItemID.POH_CLOCKWORK_MECHANISM))
				.input(ItemReq.consumed("Hop or grain seeds (10 per house)", 40, 10, ItemID.BARLEY_SEED,
					ItemID.HAMMERSTONE_HOP_SEED, ItemID.ASGARNIAN_HOP_SEED, ItemID.JUTE_SEED, ItemID.YANILLIAN_HOP_SEED,
					ItemID.KRANDORIAN_HOP_SEED, ItemID.WILDBLOOD_HOP_SEED))
				.target(Target.object(null, "Space", "Bird house", "Oak bird house", "Willow bird house",
					"Teak bird house", "Maple bird house", "Mahogany bird house", "Yew bird house", "Magic bird house",
					"Redwood bird house"))
				.instruction("Build bird houses on the four Fossil Island spots, fill them with seeds and come back "
					+ "after 50 minutes to collect")
				.notes("Only a few minutes of work per hour, so do it between other skills. A digsite pendant "
					+ "teleports you straight there. Clockworks are made at a POH clockmaker's bench or bought. "
					+ "Gives bird nests too.")
				.wikiPage("Birdhouse_run")
				.build(),
			Method.builder()
				.id("hunter_red_chinchompas")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Red chinchompas (Feldip Hills)")
				.minLevel(63)
				.xpPerAction(265)
				.xpPerHour(100_000)
				.members(true)
				.tag(MethodTag.PROFIT).tag(MethodTag.FAST)
				.location(Location.of("Feldip hunter area", 2557, 2912, 25))
				.tool(ItemReq.tools("Box traps", 5, ItemID.HUNTING_BOX_TRAP))
				.supply(Supplies.food(5))
				.target(Target.object(null, "Box trap", "Shaking box"))
				.target(Target.npc(null, "Carnivorous chinchompa"))
				.instruction("Lay box traps in a square around a chinchompa spot and reset them as they catch")
				.notes("Chinchompas sell well on the GE. " + TRAPS)
				.wikiPage(WIKI)
				.build()
		);
	}

	private static Method.MethodBuilder birds(String id, String name, int level, double xp, int xpHr,
		Location where, String bird, int feather)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.HUNTER)
			.type(MethodType.ACTIVITY)
			.name(name)
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour(xpHr)
			.members(true)
			.tag(MethodTag.CHEAP).tag(MethodTag.POWER)
			.location(where)
			.tool(SNARES)
			.dropByDefault(true)
			.product(ItemID.BONES).product(ItemID.SPIT_RAW_BIRD_MEAT).product(feather)
			.target(Target.object(null, "Bird snare"))
			.target(Target.npc(null, bird))
			.instruction("Lay bird snares near the " + bird.toLowerCase() + "s and check them when a bird is caught")
			.notes("Drop the bones and meat; keep the feathers to sell. " + TRAPS)
			.wikiPage(WIKI);
	}

	private HunterGuide()
	{
	}
}
