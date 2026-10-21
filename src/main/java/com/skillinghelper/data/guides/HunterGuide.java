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
	private static final ItemReq SNARE = ItemReq.tool("Bird snares", ItemID.HUNTING_OJIBWAY_BIRD_SNARE);
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
				.tool(ItemReq.tool("Ropes", ItemID.ROPE))
				.tool(ItemReq.tool("Small fishing nets", ItemID.NET))
				.target(Target.object(null, "Young tree", "Net trap"))
				.target(Target.npc(null, "Swamp lizard"))
				.instruction("Set net traps on the young trees with a rope and small net, then check them when a "
					+ "lizard is caught")
				.notes("Bring one rope and net per trap. " + TRAPS)
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
				.target(Target.object(null, "Space", "Bird house", "Oak bird house", "Willow bird house",
					"Teak bird house", "Maple bird house", "Mahogany bird house", "Yew bird house", "Magic bird house",
					"Redwood bird house"))
				.instruction("Build bird houses on the four Fossil Island spots, fill them with seeds and come back "
					+ "after 50 minutes to collect")
				.notes("Only a few minutes of work per hour, so do it between other skills. Bring a hammer, chisel, "
					+ "logs, clockworks and 10 hop or other seeds per house. Gives bird nests too.")
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
				.tool(ItemReq.tool("Box traps", ItemID.HUNTING_BOX_TRAP))
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
			.tool(SNARE)
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
