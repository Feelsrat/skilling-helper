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
 * Based on the OSRS wiki "Free-to-play Woodcutting training" and "Pay-to-play Woodcutting training" guides.
 */
final class WoodcuttingGuide
{
	private static final String F2P_WIKI = "Free-to-play_Woodcutting_training";
	private static final String P2P_WIKI = "Pay-to-play_Woodcutting_training";
	private static final Location WC_GUILD_BANK = Location.of("Woodcutting Guild bank chest", 1591, 3476, 5);

	static List<Method> methods()
	{
		return List.of(
			tree("wc_trees_varrock", "Trees (Varrock West)", 1, 25, 8_000, "Tree",
				Location.of("Trees near Varrock West bank", 3165, 3415, 10), Banks.VARROCK_WEST, ItemID.LOGS)
				.tag(MethodTag.POWER).tag(MethodTag.CHEAP).dropByDefault(true)
				.wikiPage(F2P_WIKI)
				.build(),
			tree("wc_oaks_varrock", "Oak trees (Varrock West)", 15, 37.5, 18_000, "Oak tree",
				Location.of("Oaks near Varrock West bank", 3165, 3415, 10), Banks.VARROCK_WEST, ItemID.OAK_LOGS)
				.tag(MethodTag.POWER).tag(MethodTag.CHEAP).dropByDefault(true)
				.wikiPage(F2P_WIKI)
				.build(),
			tree("wc_willows_draynor", "Willow trees (Draynor)", 30, 67.5, 35_000, "Willow tree",
				Location.of("Draynor willows", 3087, 3235, 7), Banks.DRAYNOR, ItemID.WILLOW_LOGS)
				.tag(MethodTag.POWER).tag(MethodTag.FAST).tag(MethodTag.CHEAP).dropByDefault(true)
				.notes("The bank is only a few steps north, so banking willows for a little profit costs very little xp.")
				.wikiPage(F2P_WIKI)
				.build(),
			tree("wc_maples_seers", "Maple trees (Seers' Village)", 45, 100, 45_000, "Maple tree",
				Location.of("Seers' Village maples", 2727, 3499, 6), Banks.SEERS, ItemID.MAPLE_LOGS)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.CHEAP)
				.notes("Maples sit right next to Seers' bank. Bank the logs for Fletching or Firemaking later.")
				.wikiPage(P2P_WIKI)
				.build(),
			tree("wc_yews_edgeville", "Yew trees (Edgeville)", 60, 175, 35_000, "Yew tree",
				Location.of("Edgeville yews", 3087, 3475, 7), Banks.EDGEVILLE, ItemID.YEW_LOGS)
				.tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.notes("Three yews south of Edgeville bank. Very AFK and the logs sell well.")
				.wikiPage(F2P_WIKI)
				.build(),
			tree("wc_yews_guild", "Yew trees (Woodcutting Guild)", 60, 175, 45_000, "Yew tree",
				Location.of("Woodcutting Guild yews", 1590, 3494, 8), WC_GUILD_BANK, ItemID.YEW_LOGS)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.requirementText("60 Woodcutting to enter the guild (gives an invisible +7 boost)")
				.wikiPage(P2P_WIKI)
				.build(),
			tree("wc_magic_guild", "Magic trees (Woodcutting Guild)", 75, 250, 40_000, "Magic tree",
				Location.of("Woodcutting Guild magic trees", 1578, 3488, 6), WC_GUILD_BANK, ItemID.MAGIC_LOGS)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.requirementText("60 Woodcutting to enter the guild")
				.wikiPage(P2P_WIKI)
				.build()
		);
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
