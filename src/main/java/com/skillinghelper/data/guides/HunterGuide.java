package com.skillinghelper.data.guides;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Stop;
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
	private static final int[] BIRD_HOUSES = {ItemID.BIRDHOUSE_NORMAL, ItemID.BIRDHOUSE_OAK, ItemID.BIRDHOUSE_WILLOW,
		ItemID.BIRDHOUSE_TEAK, ItemID.BIRDHOUSE_MAPLE, ItemID.BIRDHOUSE_MAHOGANY, ItemID.BIRDHOUSE_YEW,
		ItemID.BIRDHOUSE_MAGIC, ItemID.BIRDHOUSE_REDWOOD};
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
				// Hunter xp per bird house by log type; xp per hour is per hour of play (about 4 minutes a run)
				.xpPerAction(280)
				.xpAt(new int[]{14, 420}).xpAt(new int[]{24, 560}).xpAt(new int[]{34, 700}).xpAt(new int[]{44, 820})
				.xpAt(new int[]{49, 960}).xpAt(new int[]{59, 1_020}).xpAt(new int[]{74, 1_140})
				.xpAt(new int[]{89, 1_200})
				.xpPerHour(17_000)
				.cycleMinutes(50)
				.members(true)
				.tag(MethodTag.AFK).tag(MethodTag.CHEAP)
				.quest(Quest.BONE_VOYAGE)
				.location(Location.of("Fossil Island bird house spots", 3722, 3818, 70))
				// Run order: the Verdant Valley pair by the Digsite pendant, then the Mushroom Meadow pair by mushtree
				.stop(Stop.of("birdhouse_valley_ne", "Verdant Valley bird house (northeast)", 3768, 3761,
					"Fastest: digsite pendant to Fossil Island, then walk east"))
				.stop(Stop.of("birdhouse_valley_sw", "Verdant Valley bird house (southwest)", 3763, 3755, null))
				.stop(Stop.of("birdhouse_meadow_s", "Mushroom Meadow bird house (south)", 3679, 3815,
					"Fastest: the magic mushtree next to the Verdant Valley spots, to Mushroom Meadow"))
				.stop(Stop.of("birdhouse_meadow_n", "Mushroom Meadow bird house (north)", 3677, 3882, null))
				.tool(ItemReq.tool("Hammer", ItemID.HAMMER))
				.tool(ItemReq.tool("Chisel", ItemID.CHISEL))
				// Bird houses crafted ahead of the run replace the logs and clockworks they were made from
				.input(ItemReq.consumed("Logs for your level (oak 14+, willow 24+, teak 34+, maple 44+, mahogany 49+, "
					+ "yew 59+, magic 74+)", 4, 1, ItemID.OAK_LOGS, ItemID.LOGS, ItemID.WILLOW_LOGS, ItemID.TEAK_LOGS,
					ItemID.MAPLE_LOGS, ItemID.MAHOGANY_LOGS, ItemID.YEW_LOGS, ItemID.MAGIC_LOGS, ItemID.REDWOOD_LOGS)
					.orUse("Bird houses", BIRD_HOUSES))
				.input(ItemReq.consumed("Clockworks", 4, 1, ItemID.POH_CLOCKWORK_MECHANISM).orUse("Bird houses",
					BIRD_HOUSES))
				.input(ItemReq.consumed("Hop or grain seeds (10 per house)", 40, 10, ItemID.BARLEY_SEED,
					ItemID.HAMMERSTONE_HOP_SEED, ItemID.ASGARNIAN_HOP_SEED, ItemID.JUTE_SEED, ItemID.YANILLIAN_HOP_SEED,
					ItemID.KRANDORIAN_HOP_SEED, ItemID.WILDBLOOD_HOP_SEED))
				.target(Target.object(null, "Space", "Bird house", "Oak bird house", "Willow bird house",
					"Teak bird house", "Maple bird house", "Mahogany bird house", "Yew bird house", "Magic bird house",
					"Redwood bird house"))
				.instruction("Empty the bird house if there is one, build a new one in the space and add 10 seeds")
				.notes("Craft the bird houses before you go (use a clockwork on your logs with a hammer and chisel). "
					+ "A digsite pendant teleports you next to the Verdant Valley spots; the mushtree there takes you "
					+ "to Mushroom Meadow. They fill up in 50 minutes, so do other skills in between. "
					+ "Clockworks are made at a POH clockmaker's bench or bought. Gives bird nests too.")
				.wikiPage("Birdhouse_run")
				.build(),
			Method.builder()
				.id("hunter_feldip_weasels")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Feldip weasels (Feldip Hills)")
				.minLevel(7)
				.xpPerAction(48)
				.xpPerHour(13_000)
				.members(true)
				.tag(MethodTag.CHEAP)
				.location(Location.of("Feldip hunter area", 2557, 2912, 25))
				.tool(ItemReq.tool("Noose wand", ItemID.NOOSE_WAND))
				.supply(ItemReq.supply("Rings of pursuit (show the whole track)", 3, ItemID.RING_OF_PURSUIT))
				.target(Target.object("Inspect", "Burrow"))
				.target(Target.object("Attack", "Bush"))
				.instruction("Inspect a burrow, follow the tracks to the end and attack the bush to catch the weasel")
				.notes("A Feldip Hills teleport scroll lands close by. A ring of pursuit reveals the whole trail at once.")
				.wikiPage(WIKI)
				.build(),
			Method.builder()
				.id("hunter_ruby_harvest")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Ruby harvests and copper longtails (Piscatoris)")
				.minLevel(15)
				.xpPerAction(24)
				.xpPerHour(20_000)
				.members(true)
				.tag(MethodTag.CHEAP)
				.location(Location.of("Piscatoris hunter area", 2335, 3584, 25))
				.tool(ItemReq.tool("Butterfly net", ItemID.HUNTING_BUTTERFLY_NET))
				.tool(ItemReq.tools("Butterfly jars", 10, ItemID.BUTTERFLY_JAR))
				.tool(SNARES)
				.target(Target.npc("Catch", "Ruby harvest"))
				.target(Target.object(null, "Bird snare"))
				.instruction("Keep bird snares set for copper longtails and net ruby harvests in between")
				.notes("Release the butterflies to re-use the jars. " + TRAPS)
				.wikiPage(WIKI)
				.build(),
			Method.builder()
				.id("hunter_jerboas")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Embertailed jerboas (Locus Oasis)")
				.minLevel(39)
				.xpPerAction(137)
				.xpPerHour(40_000)
				.members(true)
				.tag(MethodTag.FAST)
				.location(Location.of("Locus Oasis hunter area", 1671, 3001, 20))
				.quest(Quest.EAGLES_PEAK)
				.tool(ItemReq.tools("Box traps", 3, ItemID.HUNTING_BOX_TRAP))
				.target(Target.object(null, "Box trap", "Shaking box"))
				.target(Target.npc(null, "Embertailed jerboa"))
				.instruction("Lay box traps near the jerboas and reset them when they catch")
				.notes("Only part of Eagles' Peak is needed for box traps. Fairy ring A-J-P is right next to the area. "
					+ TRAPS)
				.wikiPage(WIKI)
				.rateAt(43, 50_000)
				.build(),
			Method.builder()
				.id("hunter_falconry")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Falconry (Piscatoris)")
				.minLevel(43)
				.xpPerAction(104)
				.xpPerHour(45_000)
				.members(true)
				.tag(MethodTag.FAST)
				.location(Location.of("Piscatoris falconry area", 2379, 3599, 15))
				.xpAt(new int[]{57, 132})
				.supply(ItemReq.supply("Coins (500 to rent the falcon)", 500, ItemID.COINS))
				.supply(Supplies.STAMINA)
				.target(Target.npc(null, "Matthias", "Spotted kebbit", "Dark kebbit"))
				.instruction("Rent a gyr falcon from Matthias, click a kebbit to send the falcon, then retrieve it "
					+ "and drop the loot")
				.notes("Your weapon, shield and gloves slots must be empty. Dark kebbits from 57. Fairy ring A-K-Q.")
				.wikiPage("Falconry")
				.rateAt(57, 65_000)
				.build(),
			Method.builder()
				.id("hunter_orange_salamanders")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Orange salamanders (Uzer)")
				.minLevel(47)
				.xpPerAction(224)
				.xpPerHour(40_000)
				.members(true)
				.tag(MethodTag.AFK)
				.location(Location.of("Uzer hunter area", 3401, 3104, 20))
				.tool(ItemReq.tools("Ropes", 3, ItemID.ROPE))
				.tool(ItemReq.tools("Small fishing nets", 3, ItemID.NET))
				.supply(ItemReq.supply("Waterskins (desert heat)", 10, ItemID.WATER_SKIN4, ItemID.WATER_SKIN3,
					ItemID.WATER_SKIN2, ItemID.WATER_SKIN1))
				.target(Target.object(null, "Young tree", "Net trap"))
				.target(Target.npc(null, "Orange salamander"))
				.instruction("Set net traps on the young trees and drop the salamanders as you catch them")
				.notes("Less clicking than falconry. Desert clothing makes waterskins last longer. " + TRAPS)
				.wikiPage(WIKI)
				.build(),
			Method.builder()
				.id("hunter_razor_kebbits")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Razor-backed kebbits (Piscatoris)")
				.minLevel(49)
				.xpPerAction(348.5)
				.xpPerHour(90_000)
				.members(true)
				.tag(MethodTag.FAST)
				.location(Location.of("Piscatoris hunter area", 2335, 3584, 25))
				.tool(ItemReq.tool("Noose wand", ItemID.NOOSE_WAND))
				.supply(ItemReq.supply("Rings of pursuit", 5, ItemID.RING_OF_PURSUIT))
				.supply(Supplies.STAMINA)
				.target(Target.object("Inspect", "Burrow"))
				.target(Target.object("Attack", "Bush"))
				.instruction("Search a burrow, follow the tracks and attack the bush they end at")
				.notes("The fastest Hunter xp from 49 to 73. Wear a ring of pursuit and weight-reducing clothes; "
					+ "fairy ring A-K-Q then run east.")
				.wikiPage(WIKI)
				.rateAt(60, 110_000).rateAt(73, 130_000)
				.build(),
			Method.builder()
				.id("hunter_red_salamanders")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Red salamanders (Ourania)")
				.minLevel(59)
				.xpPerAction(272)
				.xpPerHour(80_000)
				.members(true)
				.tag(MethodTag.FAST)
				.location(Location.of("Ourania hunter area", 2475, 3240, 15))
				.tool(ItemReq.tools("Ropes", 5, ItemID.ROPE))
				.tool(ItemReq.tools("Small fishing nets", 5, ItemID.NET))
				.supply(Supplies.STAMINA)
				.target(Target.object(null, "Young tree", "Net trap"))
				.target(Target.npc(null, "Red salamander"))
				.instruction("Set net traps on the young trees and drop the salamanders as you catch them")
				.notes("South of the Ourania Cave. Five ropes and nets let you reset before picking up collapsed traps. "
					+ TRAPS)
				.wikiPage(WIKI)
				.rateAt(67, 100_000)
				.build(),
			Method.builder()
				.id("hunter_red_chinchompas")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Red chinchompas (Feldip Hills)")
				.minLevel(63)
				.xpPerAction(265)
				.xpPerHour(75_000)
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
				.rateAt(80, 105_000).rateAt(90, 130_000)
				.build(),
			Method.builder()
				.id("hunter_black_salamanders")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Black salamanders (Wilderness)")
				.minLevel(67)
				.xpPerAction(319.2)
				.xpPerHour(90_000)
				.members(true)
				.tag(MethodTag.FAST)
				.location(Location.of("Bone Yard hunter area (Wilderness)", 3294, 3673, 15))
				.tool(ItemReq.tools("Ropes", 6, ItemID.ROPE))
				.tool(ItemReq.tools("Small fishing nets", 6, ItemID.NET))
				.target(Target.object(null, "Young tree", "Net trap"))
				.target(Target.npc(null, "Black salamander"))
				.instruction("Set net traps on the young trees and drop the salamanders as you catch them")
				.notes("Level 20 Wilderness: bring only what you can afford to lose. You get one extra trap in the "
					+ "Wilderness. Burning amulet to the Chaos Temple, then run north-east.")
				.wikiPage(WIKI)
				.rateAt(73, 110_000)
				.build(),
			Method.builder()
				.id("hunter_black_chinchompas")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Black chinchompas (Wilderness)")
				.minLevel(73)
				.xpPerAction(315)
				.xpPerHour(120_000)
				.members(true)
				.tag(MethodTag.FAST).tag(MethodTag.PROFIT)
				.location(Location.of("Wilderness hunter area", 3142, 3771, 15))
				.quest(Quest.EAGLES_PEAK)
				.tool(ItemReq.tools("Box traps", 5, ItemID.HUNTING_BOX_TRAP))
				.target(Target.object(null, "Box trap", "Shaking box"))
				.target(Target.npc(null, "Black chinchompa"))
				.instruction("Lay box traps around the chinchompas and reset them as they catch")
				.notes("Level 32 Wilderness and popular with player killers: bring only what you can afford to lose. "
					+ "Black chinchompas sell for a lot. You get one extra trap in the Wilderness.")
				.wikiPage(WIKI)
				.rateAt(85, 150_000).rateAt(99, 180_000)
				.build(),
			Method.builder()
				.id("hunter_herbiboar")
				.skill(Skill.HUNTER)
				.type(MethodType.ACTIVITY)
				.name("Herbiboar (Fossil Island)")
				.minLevel(80)
				.xpPerAction(2100)
				.xpPerHour(60_000)
				.members(true)
				.tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.location(Location.of("Mushroom Forest (Fossil Island)", 3701, 3849, 35))
				.quest(Quest.BONE_VOYAGE)
				.requirementText("31 Herblore to harvest the herbs")
				.supply(Supplies.STAMINA)
				.target(Target.object(null, "Start", "Tunnel", "Hidden object"))
				.target(Target.npc("Harvest", "Herbiboar"))
				.instruction("Inspect a starting point, follow the footsteps to the tunnel and attack it, then harvest "
					+ "the herbiboar")
				.notes("Low effort and the herbs make decent money. An open herb sack saves space.")
				.wikiPage("Herbiboar")
				.rateAt(90, 70_000).rateAt(99, 80_000)
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
