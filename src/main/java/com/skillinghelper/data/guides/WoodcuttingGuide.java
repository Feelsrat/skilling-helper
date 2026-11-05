package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
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
 * Based on the OSRS wiki "Free-to-play Woodcutting training" and "Pay-to-play Woodcutting training" guides.
 */
final class WoodcuttingGuide
{
	private static final String F2P_WIKI = "Free-to-play_Woodcutting_training";
	private static final String P2P_WIKI = "Pay-to-play_Woodcutting_training";
	private static final Location HARDWOOD_GROVE = Location.of("Hardwood Grove (Tai Bwo Wannai)", 2823, 3083, 7);
	private static final Location WC_GUILD_BANK = Location.of("Woodcutting Guild bank chest", 1591, 3476, 5);

	static List<Method> methods()
	{
		return List.of(
			tree("wc_trees_varrock", "Trees (Varrock West)", 1, 25, 4_000, "Tree",
				Location.of("Trees near Varrock West bank", 3165, 3415, 10), Banks.VARROCK_WEST, ItemID.LOGS)
				.tag(MethodTag.POWER).tag(MethodTag.CHEAP).dropByDefault(true)
				.wikiPage(F2P_WIKI)
				.rateAt(15, 8_000)
				.howTo("The normal trees (just called \"Tree\") are on the grass right outside Varrock West bank: "
					+ "follow the arrow.")
				.howTo(chop("tree"))
				.build(),
			tree("wc_oaks_varrock", "Oak trees (Varrock West)", 15, 37.5, 12_000, "Oak tree",
				Location.of("Oaks near Varrock West bank", 3165, 3415, 10), Banks.VARROCK_WEST, ItemID.OAK_LOGS)
				.tag(MethodTag.POWER).tag(MethodTag.CHEAP).dropByDefault(true)
				.wikiPage(F2P_WIKI)
				.rateAt(30, 22_000).rateAt(45, 28_000)
				.howTo("The oak trees (big, round, light-green trees) are on the grass right outside Varrock West "
					+ "bank: follow the arrow.")
				.howTo(chop("oak tree"))
				.build(),
			tree("wc_willows_draynor", "Willow trees (Draynor)", 30, 67.5, 25_000, "Willow tree",
				Location.of("Draynor willows", 3087, 3235, 7), Banks.DRAYNOR, ItemID.WILLOW_LOGS)
				.tag(MethodTag.POWER).tag(MethodTag.FAST).tag(MethodTag.CHEAP).dropByDefault(true)
				.notes("The bank is only a few steps north, so banking willows for a little profit costs very little xp.")
				.wikiPage(F2P_WIKI)
				.rateAt(45, 38_000).rateAt(60, 48_000).rateAt(75, 55_000)
				.howTo("The willow trees (drooping branches) grow along the river just south of Draynor Village "
					+ "bank: follow the arrow.")
				.howTo(chop("willow tree"))
				.build(),
			tree("wc_teak_grove", "Teak trees (Hardwood Grove)", 35, 85, 30_000, "Teak tree",
				HARDWOOD_GROVE, null, ItemID.TEAK_LOGS)
				.members(true).tag(MethodTag.POWER).tag(MethodTag.FAST).dropByDefault(true)
				.requirementText("100 trading sticks to enter the Hardwood Grove (Tai Bwo Wannai)")
				.notes("The fastest Woodcutting xp from 35. Drop the logs as you go. "
					+ "Trading sticks come from helping clean up the Tai Bwo Wannai village.")
				.wikiPage(P2P_WIKI)
				.rateAt(50, 45_000).rateAt(65, 55_000).rateAt(80, 62_000).rateAt(99, 70_000)
				.getThere("First time only: you need 100 trading sticks. Get them in Tai Bwo Wannai village "
					+ "(south Karamja) by helping with the village clean-up: cut the jungle around the village "
					+ "fence with a machete or axe, then talk to Gabooty for sticks.")
				.getThere("Walk to the Hardwood Grove fence on the east side of Tai Bwo Wannai and click the "
					+ "gate: the guard takes the 100 trading sticks once, then you can come and go.")
				.howTo(chop("teak tree"))
				.build(),
			tree("wc_maples_seers", "Maple trees (Seers' Village)", 45, 100, 28_000, "Maple tree",
				Location.of("Seers' Village maples", 2727, 3499, 6), Banks.SEERS, ItemID.MAPLE_LOGS)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.CHEAP)
				.notes("Maples sit right next to Seers' bank. Bank the logs for Fletching or Firemaking later.")
				.wikiPage(P2P_WIKI)
				.rateAt(60, 38_000).rateAt(75, 45_000).rateAt(90, 50_000)
				.howTo("The maple trees (orange leaves) stand just north of Seers' Village bank: follow the "
					+ "arrow.")
				.howTo(chop("maple tree"))
				.build(),
			tree("wc_yews_edgeville", "Yew trees (Edgeville)", 60, 175, 20_000, "Yew tree",
				Location.of("Edgeville yews", 3087, 3475, 7), Banks.EDGEVILLE, ItemID.YEW_LOGS)
				.tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.notes("Three yews south of Edgeville bank. Very AFK and the logs sell well.")
				.wikiPage(F2P_WIKI)
				.rateAt(75, 27_000).rateAt(90, 34_000).rateAt(99, 37_000)
				.howTo("Walk south from Edgeville bank past the houses: the yew trees (big dark-green trees) are "
					+ "in a row along the south side of the village.")
				.howTo(chop("yew tree"))
				.build(),
			tree("wc_yews_guild", "Yew trees (Woodcutting Guild)", 60, 175, 24_000, "Yew tree",
				Location.of("Woodcutting Guild yews", 1590, 3494, 8), WC_GUILD_BANK, ItemID.YEW_LOGS)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.requirementText("60 Woodcutting to enter the guild (gives an invisible +7 boost)")
				.wikiPage(P2P_WIKI)
				.rateAt(75, 31_000).rateAt(90, 38_000).rateAt(99, 41_000)
				.getThere("The Woodcutting Guild is on the west side of Hosidius in Great Kourend. Click the "
					+ "guild gate to go in: it only opens at 60 Woodcutting.")
				.howTo("The yew trees are in the middle of the guild, just north of the bank chest.")
				.howTo(chop("yew tree"))
				.build(),
			tree("wc_mahogany_grove", "Mahogany trees (Hardwood Grove)", 50, 125, 22_000, "Mahogany tree",
				HARDWOOD_GROVE, Banks.SHILO, ItemID.MAHOGANY_LOGS)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.requirementText("100 trading sticks to enter the Hardwood Grove (Tai Bwo Wannai)")
				.notes("Mahogany logs sell well for Construction planks. Shilo Village bank is a short walk south-east.")
				.wikiPage(P2P_WIKI)
				.rateAt(70, 35_000).rateAt(90, 45_000)
				.getThere("First time only: you need 100 trading sticks. Get them in Tai Bwo Wannai village "
					+ "(south Karamja) by helping with the village clean-up: cut the jungle around the village "
					+ "fence with a machete or axe, then talk to Gabooty for sticks.")
				.getThere("Walk to the Hardwood Grove fence on the east side of Tai Bwo Wannai and click the "
					+ "gate: the guard takes the 100 trading sticks once, then you can come and go.")
				.howTo(chop("mahogany tree"))
				.build(),
			tree("wc_magic_guild", "Magic trees (Woodcutting Guild)", 75, 250, 20_000, "Magic tree",
				Location.of("Woodcutting Guild magic trees", 1578, 3488, 6), WC_GUILD_BANK, ItemID.MAGIC_LOGS)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.requirementText("60 Woodcutting to enter the guild")
				.wikiPage(P2P_WIKI)
				.rateAt(90, 28_000).rateAt(99, 32_000)
				.getThere("The Woodcutting Guild is on the west side of Hosidius in Great Kourend. Click the "
					+ "guild gate to go in: it only opens at 60 Woodcutting.")
				.howTo("The magic trees (pale blue, sparkly trees) are in the west part of the guild.")
				.howTo(chop("magic tree"))
				.build(),
			tree("wc_redwood_guild", "Redwood trees (Woodcutting Guild)", 90, 380, 55_000, "Redwood tree",
				Location.area("Woodcutting Guild redwoods", 1571, 3488, 8), WC_GUILD_BANK, ItemID.REDWOOD_LOGS)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.FAST)
				.target(Target.object("Climb", "Rope ladder"))
				.requirementText("60 Woodcutting to enter the guild")
				.notes("Climb the rope ladders up the redwood platforms. Very AFK; dropping the logs is fastest, "
					+ "or bank them at the guild bank chest.")
				.wikiPage(P2P_WIKI)
				.rateAt(99, 65_000)
				.getThere("The Woodcutting Guild is on the west side of Hosidius in Great Kourend. Click the "
					+ "guild gate to go in: it only opens at 60 Woodcutting.")
				.howTo("The redwoods are the huge trees in the south-west of the guild. Click the highlighted "
					+ "rope ladder to climb onto the platform that runs around them, then chop from up there.")
				.howTo(chop("redwood tree"))
				.build(),
			tree("wc_camphor_conch", "Camphor trees (The Great Conch)", 66, 143, 50_000, "Camphor tree",
				Location.of("Sacred Grove camphor trees (The Great Conch)", 3196, 2481, 12), null,
				ItemID.CAMPHOR_LOGS)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.FAST).dropByDefault(true)
				.questStarted(Quest.TROUBLED_TORTUGANS)
				.notes("From the wiki's AFK guide: a tree lasts about 2 minutes of chopping. Camphor logs make the "
					+ "planks for Sailing boat parts, so they're worth keeping if you sail.")
				.wikiPage("Camphor_tree")
				.rateAt(80, 63_000).rateAt(99, 70_000)
				.getThere("The Great Conch is south of Menaphos; you need Troubled Tortugans started (give the "
					+ "bandage to Floopa). Fairy ring C-J-Q or a charter ship gets you there; the camphor trees are "
					+ "in the Sacred Grove: follow the arrow.")
				.howTo(chop("camphor tree"))
				.build(),
			tree("wc_ironwood_sunbleak", "Ironwood trees (Sunbleak Island)", 80, 175, 60_000, "Ironwood tree",
				Location.of("Sunbleak Island ironwood trees", 2216, 2330, 18), null, ItemID.IRONWOOD_LOGS)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.FAST).dropByDefault(true)
				.skillLevel(Skill.SAILING, 72)
				.notes("From the wiki's AFK guide: a tree lasts about 4 minutes of chopping, and it's the best "
					+ "Woodcutting xp in the game with forester's rations. Sunbleak has a bank chest you can build.")
				.wikiPage("Ironwood_tree")
				.rateAt(90, 68_000).rateAt(99, 75_000)
				.getThere("Sunbleak Island is only reachable by boat (72 Sailing and an adamant helm for the kelp). "
					+ "Teleport to Wyrmscraig with a necklace of passage, call your boat and sail north-west.")
				.howTo(chop("ironwood tree"))
				.build(),
			tree("wc_rosewood_drumstick", "Rosewood trees (Drumstick Isle)", 92, 212.5, 70_000, "Rosewood tree",
				Location.of("Drumstick Isle rosewood trees", 2147, 3548, 12), null, ItemID.ROSEWOOD_LOGS)
				.members(true).tag(MethodTag.AFK).dropByDefault(true)
				.skillLevel(Skill.SAILING, 79)
				.notes("From the wiki's AFK guide: the longest a tree lasts (about 4.5 minutes) and the logs are the "
					+ "most valuable. Keep them in your boat's cargo hold rather than dropping them.")
				.wikiPage("Rosewood_tree")
				.rateAt(99, 85_000)
				.getThere("Drumstick Isle is west of Piscatoris and needs 79 Sailing and an adamant keel to moor. "
					+ "Take a charter ship to Prifddinas, sail to the cyan Portal of Meilyr north-west of it, and it "
					+ "drops you just north of the island.")
				.howTo(chop("rosewood tree"))
				.build()
		);
	}

	/**
	 * The beginner steps every tree shares, after the "where are the trees" step.
	 */
	private static List<String> chop(String tree)
	{
		return List.of(
			"Have your axe in your inventory, or wield it to free a slot (the guide picks your best one).",
			"Left-click a highlighted " + tree + " once (\"Chop down\"). Your character keeps chopping on their own "
				+ "until the tree falls, so there's no need to keep clicking.",
			"Logs land in your inventory one at a time. When the tree turns into a stump, click the next highlighted "
				+ tree + ".",
			"When your inventory is full, the guide moves on by itself: it tells you to drop the logs or takes you to "
				+ "the bank.");
	}

	private static Method.MethodBuilder tree(String id, String name, int level, double xp, int xpHr, String treeName,
		Location location, Location bank, int logId)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.WOODCUTTING)
			.type(MethodType.GATHER)
			.name(name)
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour(xpHr)
			.location(location)
			.bank(bank)
			.toolType(ToolType.AXE)
			.target(Target.object("Chop down", treeName))
			.product(logId)
			.instruction("Chop down the " + treeName.toLowerCase() + "s");
	}

	private WoodcuttingGuide()
	{
	}
}
