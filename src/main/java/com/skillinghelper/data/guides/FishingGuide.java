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
 * Based on the OSRS wiki "Free-to-play Fishing training" and "Pay-to-play Fishing training" guides.
 */
final class FishingGuide
{
	private static final String F2P_WIKI = "Free-to-play_Fishing_training";
	private static final String P2P_WIKI = "Pay-to-play_Fishing_training";
	private static final ItemReq FEATHERS = ItemReq.consumed("Feathers", 1000, 1, ItemID.FEATHER);

	static List<Method> methods()
	{
		return List.of(
			fish("fish_shrimp_draynor", "Shrimp & anchovies (Draynor)", 1, 15, 5_000,
				Location.of("Draynor fishing spots", 3086, 3228, 6), Banks.DRAYNOR, "Net", "Fishing spot")
				.tool(ItemReq.tool("Small fishing net", ItemID.NET))
				.product(ItemID.RAW_SHRIMP).product(ItemID.RAW_ANCHOVIES)
				.tag(MethodTag.CHEAP)
				.notes("Bank the fish and cook them for Cooking xp.")
				.wikiPage(F2P_WIKI)
				.rateAt(15, 10_000)
				.howTo("The fishing spots are on the shore just south of Draynor Village bank, past the willow "
					+ "trees: follow the arrow.")
				.howTo(catching("Net"))
				.build(),
			fish("fish_fly_barbarian", "Fly fishing (Barbarian Village)", 20, 55, 15_000,
				Location.of("Barbarian Village river", 3105, 3430, 7), Banks.EDGEVILLE, "Lure", "Rod Fishing spot")
				.tool(ItemReq.tool("Fly fishing rod", ItemID.FLY_FISHING_ROD, ItemID.FISHINGROD_PEARL_FLY))
				.input(FEATHERS)
				.product(ItemID.RAW_TROUT).product(ItemID.RAW_SALMON)
				.tag(MethodTag.POWER).tag(MethodTag.FAST).tag(MethodTag.CHEAP).dropByDefault(true)
				.notes("Trout from 20, salmon from 30. There's a permanent fire next to the spots if you want to cook.")
				.wikiPage(F2P_WIKI)
				.rateAt(30, 25_000).rateAt(40, 33_000).rateAt(50, 40_000).rateAt(70, 50_000)
				.howTo("The fishing spots are in the river on the east side of Barbarian Village, next to a fire "
					+ "that never goes out: follow the arrow.")
				.howTo(catching("Lure"))
				.howTo("Feathers are used up one per fish. Buy a few hundred at the GE or Gerrant's fishing shop "
					+ "in Port Sarim.")
				.build(),
			fish("fish_lobster_catherby", "Lobsters (Catherby)", 40, 90, 14_000,
				Location.of("Catherby beach", 2840, 3431, 8), Banks.CATHERBY, "Cage", "Fishing spot")
				.tool(ItemReq.tool("Lobster pot", ItemID.LOBSTER_POT))
				.product(ItemID.RAW_LOBSTER)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.wikiPage(P2P_WIKI)
				.rateAt(60, 20_000).rateAt(76, 25_000).rateAt(90, 28_000)
				.howTo("The fishing spots are in the sea along Catherby beach, just east of the bank: follow the "
					+ "arrow. Pick a spot that has the \"Cage\" option.")
				.howTo(catching("Cage"))
				.build(),
			fish("fish_barbarian_otto", "Barbarian fishing (Otto's Grotto)", 48, 60, 30_000,
				Location.of("Otto's Grotto", 2500, 3510, 8), null, "Use-rod", "Fishing spot")
				.tool(ItemReq.tool("Barbarian rod", ItemID.BRUT_FISHING_ROD, ItemID.FISHINGROD_PEARL_BRUT)
					.from("Search the bed in Otto's hut at the grotto to get one (talk to Otto first to start the "
						+ "training)", null, Target.object("Search", "Bed")))
				.input(FEATHERS)
				.product(ItemID.BRUT_SPAWNING_TROUT).product(ItemID.BRUT_SPAWNING_SALMON).product(ItemID.BRUT_STURGEON)
				.members(true).tag(MethodTag.POWER).tag(MethodTag.FAST).dropByDefault(true)
				.requirementText("Barbarian Training (talk to Otto), 15 Agility and 15 Strength")
				.notes("Also gives Agility and Strength xp. Drop fish as you go.")
				.wikiPage("Barbarian_Fishing")
				.rateAt(58, 42_000).rateAt(70, 55_000).rateAt(80, 62_000).rateAt(90, 70_000)
				.getThere("Otto's Grotto is just south of the Barbarian Outpost, by Baxtorian Falls. Talk to Otto "
					+ "Godblessed in the hut and ask about barbarian training, then pick fishing.")
				.howTo("Search the bed in Otto's hut to get a barbarian rod (it's free).")
				.howTo("Bring feathers or fishing bait: each catch uses one.")
				.howTo(catching("Use-rod"))
				.howTo("Drop the fish as you go (hold Shift and click). This method never banks.")
				.build(),
			fish("fish_monkfish_piscatoris", "Monkfish (Piscatoris)", 62, 120, 30_000,
				Location.of("Piscatoris fishing spots", 2337, 3702, 8), Banks.PISCATORIS, "Net", "Fishing spot")
				.tool(ItemReq.tool("Small fishing net", ItemID.NET))
				.product(ItemID.RAW_MONKFISH)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.quest(Quest.SWAN_SONG)
				.notes("The bank is right next to the spots, so banking costs little. Monkfish are good food too.")
				.wikiPage(P2P_WIKI)
				.rateAt(75, 35_000).rateAt(90, 40_000)
				.getThere("The Piscatoris Fishing Colony is in the far north-west of Kandarin; you need Swan Song "
					+ "done. Fairy ring A-K-Q lands nearby, then walk west.")
				.howTo("The fishing spots are in the water right next to the colony's bank: follow the arrow.")
				.howTo(catching("Net"))
				.build(),
			fish("fish_sharks_catherby", "Sharks (Catherby)", 76, 110, 20_000,
				Location.of("Catherby beach", 2840, 3431, 8), Banks.CATHERBY, "Harpoon", "Fishing spot")
				.toolType(ToolType.HARPOON)
				.product(ItemID.RAW_SHARK)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.notes("Use the spots that offer Net and Harpoon.")
				.wikiPage(P2P_WIKI)
				.rateAt(90, 28_000).rateAt(99, 30_000)
				.howTo("The fishing spots are in the sea along Catherby beach, just east of the bank: follow the "
					+ "arrow. Pick a spot that has the \"Harpoon\" option next to \"Net\".")
				.howTo(catching("Harpoon"))
				.build(),
			Method.builder()
				.id("fish_tempoross")
				.skill(Skill.FISHING)
				.type(MethodType.ACTIVITY)
				.name("Tempoross")
				.minLevel(35)
				.xpPerAction(1_000)
				.xpPerHour(30_000)
				.rateAt(50, 45_000).rateAt(70, 60_000).rateAt(90, 70_000)
				.members(true)
				.tag(MethodTag.MINIGAME).tag(MethodTag.FAST)
				.location(Location.of("Tempoross Cove", 3035, 2850, 30))
				.toolType(ToolType.HARPOON)
				.target(Target.npc("Harpoon", "Fishing spot"))
				.target(Target.object(null, "Ammunition crate", "Shrine", "Totem pole", "Mast", "Water pump"))
				.supply(ItemReq.supply("Bucket of water (for fires)", 2, ItemID.BUCKET_WATER))
				.instruction("Board the boat at Tempoross Cove. Harpoon fish, cook them at the shrine and load the "
					+ "ammunition crate. Tether to a totem pole or mast when the storm wave comes, then attack "
					+ "Tempoross when it's down")
				.notes("Each round gives reward permits for the Reward pool (fish, Angler outfit pieces, tome of water, "
					+ "pet). Douse fires with buckets of water; you can fill buckets at the water pump.")
				.wikiPage("Tempoross")
				.getThere("Get to the Ruins of Unkah: talk to Ferryman Sathwood on the dock just south of Al "
					+ "Kharid bank and take his ferry (or use the Minigame Teleport to Tempoross).")
				.getThere("Bring a harpoon, a bucket, a rope and a hammer (spares are in the crates on the boat "
					+ "if you forget). Leave everything else in the bank: you need lots of free space for fish.")
				.howTo("Climb the rope ladder onto the boat at the end of the dock and wait. The game starts by "
					+ "itself after 30 seconds; pick \"Solo-start\" on the ladder to play alone.")
				.howTo("Fill your inventory at a highlighted fishing spot (\"Harpoon\"). Spots with fish jumping "
					+ "out of the water are faster.")
				.howTo("Optional: cook the fish at the highlighted shrine on the island for more damage.")
				.howTo("Run onto the boat and click the highlighted ammunition crate next to a cannon to load all "
					+ "your fish. Repeat until Tempoross's energy bar (top of the screen) is empty.")
				.howTo("When the chat warns that a colossal wave is coming, click the nearest highlighted mast or "
					+ "totem pole straight away to tie yourself on with your rope; otherwise the wave knocks your "
					+ "fish and tools out of your hands.")
				.howTo("If lightning sets the boat on fire, fill your bucket at the water pump and click the fire "
					+ "to put it out. If a mast or totem breaks, click it with your hammer to fix it.")
				.howTo("When Tempoross goes under the water, click the spirit pool at the end of the dock "
					+ "(\"Harpoon\") and keep fishing it until the game ends.")
				.howTo("Back at the Ruins of Unkah, take a net from the spirit angler and click the reward pool "
					+ "once per reward permit you earned.")
				.build(),
			Method.builder()
				.id("fish_aerial_molch")
				.skill(Skill.FISHING)
				.type(MethodType.ACTIVITY)
				.name("Aerial fishing (Lake Molch)")
				.minLevel(43)
				.xpPerAction(35)
				.xpPerHour(25_000)
				.rateAt(60, 32_000).rateAt(80, 40_000).rateAt(99, 45_000)
				.members(true)
				.tag(MethodTag.CHEAP)
				.location(Location.of("Lake Molch", 1370, 3633, 15))
				.target(Target.npc("Catch", "Fishing spot"))
				.target(Target.npc(null, "Alry the Angler"))
				.target(Target.object(null, "Boaty"))
				.supply(ItemReq.supply("King worms or fish chunks (bait)", 500, ItemID.KING_WORM, ItemID.FISH_CHUNKS))
				.requirementText("35 Hunter")
				.instruction("Talk to Alry the Angler for the cormorant's glove, take the boat to the island and send "
					+ "your cormorant at the fishing spots")
				.notes("Also gives Hunter xp, and Cooking xp if you cut the fish into chunks with a knife to re-use as "
					+ "bait. Earns molch pearls for the Angler outfit and pearl rods.")
				.wikiPage("Aerial_fishing")
				.getThere("Lake Molch is in the middle of Great Kourend, south of Shayzien. Follow the arrow to "
					+ "the lake.")
				.howTo("Talk to Alry the Angler on the shore to get a cormorant's glove and a cormorant for free. "
					+ "Wear the glove; your weapon and shield slots must be empty.")
				.howTo("Click the highlighted boat (Boaty) to row out to the island in the lake.")
				.howTo("Pick up king worms around the island to use as bait, or bring fish offcuts. Keep a knife "
					+ "in your inventory.")
				.howTo("Click a highlighted fishing spot to send your cormorant. It always comes back with a "
					+ "fish.")
				.howTo("Use the knife on the fish to cut them into offcuts (Cooking xp, and the offcuts are "
					+ "bait). Repeat.")
				.howTo("Trade molch pearls you find to Alry for the Angler outfit and better rods.")
				.build(),
			Method.builder()
				.id("fish_trawler")
				.skill(Skill.FISHING)
				.type(MethodType.ACTIVITY)
				.name("Fishing Trawler (Angler outfit)")
				.minLevel(15)
				.xpPerAction(300)
				.xpPerHour(15_000)
				.members(true)
				.tag(MethodTag.MINIGAME).tag(MethodTag.AFK)
				.location(Location.of("Fishing Trawler (Port Khazard)", 2667, 3163, 12))
				.target(Target.object(null, "Gangplank", "Leak", "Net", "Trawler net"))
				.target(Target.npc(null, "Murphy"))
				.instruction("Board the trawler with Murphy, patch leaks and bail water until the net comes up, then "
					+ "collect your fish and reward pieces")
				.notes("Slow xp, but every game is a chance at an Angler piece. The full outfit gives +2.5% Fishing xp "
					+ "for the rest of the grind.")
				.wikiPage("Fishing_Trawler")
				.getThere("The Fishing Trawler is at Port Khazard, south of Ardougne (follow the arrow). You need "
					+ "15 Fishing.")
				.getThere("Bring a hammer, a few ropes, lots of swamp paste and a bailing bucket. The Khazard "
					+ "General Store next to the dock sells all of them; boxes on the boat also have buckets, "
					+ "hammers and axes.")
				.howTo("Talk to Murphy on the dock, then cross the gangplank to board the trawler. It sails when "
					+ "the timer runs out.")
				.howTo("On the boat: click a leak in the side of the hull with swamp paste in your inventory to "
					+ "patch it. If water gets in, use the bailing bucket to bail it and empty it over the side.")
				.howTo("If the net at the back tears, click it with a rope to fix it. The more you help, the more "
					+ "points you get (50 points is enough for a reward).")
				.howTo("When you're back at the dock, click the highlighted trawler net to collect your fish and "
					+ "any Angler outfit pieces.")
				.build(),
			fish("fish_anglerfish_piscarilius", "Anglerfish (Port Piscarilius)", 82, 120, 15_000,
				Location.of("Port Piscarilius anglerfish spots", 1830, 3772, 8), Banks.PISCARILIUS, "Bait",
				"Rod Fishing spot")
				.tool(ItemReq.tool("Fishing rod", ItemID.FISHING_ROD, ItemID.FISHINGROD_PEARL))
				.input(ItemReq.consumed("Sandworms", 500, 1, ItemID.PISCARILIUS_SANDWORMS))
				.product(ItemID.RAW_ANGLERFISH)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.notes("Slow xp but very profitable and AFK. Sandworms are sold at the tackle shop in Port "
					+ "Piscarilius or made from Fishing Bait at the Hosidius worm farm.")
				.wikiPage(P2P_WIKI)
				.rateAt(90, 18_000).rateAt(99, 20_000)
				.howTo("The anglerfish spots are on the coast of Port Piscarilius (north-east Great Kourend): "
					+ "follow the arrow.")
				.howTo("Buy sandworms (the bait) at the fishing shop in Port Piscarilius.")
				.howTo(catching("Bait"))
				.build()
		);
	}

	/**
	 * The beginner steps every fishing spot shares, after the "where are the spots" step.
	 */
	private static List<String> catching(String action)
	{
		return List.of(
			"Keep your fishing gear (and bait or feathers, if this method uses them) in your inventory. The guide's "
				+ "bank step takes them out for you.",
			"Fishing spots are the moving ripples in the water. Left-click a highlighted spot with \"" + action
				+ "\". If the left-click says something else, right-click the spot and pick \"" + action + "\".",
			"Your character keeps fishing on their own. Spots move every few minutes: when your character stops, "
				+ "click the nearest highlighted spot again.",
			"When your inventory is full, the guide moves on by itself: it tells you to drop the fish or takes you to "
				+ "the bank.");
	}

	private static Method.MethodBuilder fish(String id, String name, int level, double xp, int xpHr, Location location,
		Location bank, String action, String spotName)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.FISHING)
			.type(MethodType.GATHER)
			.name(name)
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour(xpHr)
			.location(location)
			.bank(bank)
			.target(Target.npc(action, spotName))
			.instruction("Click the fishing spot (" + action + ")");
	}

	private FishingGuide()
	{
	}
}
