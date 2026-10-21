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
			fish("fish_shrimp_draynor", "Shrimp & anchovies (Draynor)", 1, 15, 6_000,
				Location.of("Draynor fishing spots", 3086, 3228, 6), Banks.DRAYNOR, "Net", "Fishing spot")
				.tool(ItemReq.tool("Small fishing net", ItemID.NET))
				.product(ItemID.RAW_SHRIMP).product(ItemID.RAW_ANCHOVIES)
				.tag(MethodTag.CHEAP)
				.notes("Bank the fish and cook them for Cooking xp.")
				.wikiPage(F2P_WIKI)
				.build(),
			fish("fish_fly_barbarian", "Fly fishing (Barbarian Village)", 20, 55, 25_000,
				Location.of("Barbarian Village river", 3105, 3430, 7), Banks.EDGEVILLE, "Lure", "Rod Fishing spot")
				.tool(ItemReq.tool("Fly fishing rod", ItemID.FLY_FISHING_ROD, ItemID.FISHINGROD_PEARL_FLY))
				.input(FEATHERS)
				.product(ItemID.RAW_TROUT).product(ItemID.RAW_SALMON)
				.tag(MethodTag.POWER).tag(MethodTag.FAST).tag(MethodTag.CHEAP).dropByDefault(true)
				.notes("Trout from 20, salmon from 30. There's a permanent fire next to the spots if you want to cook.")
				.wikiPage(F2P_WIKI)
				.build(),
			fish("fish_lobster_catherby", "Lobsters (Catherby)", 40, 90, 18_000,
				Location.of("Catherby beach", 2840, 3431, 8), Banks.CATHERBY, "Cage", "Fishing spot")
				.tool(ItemReq.tool("Lobster pot", ItemID.LOBSTER_POT))
				.product(ItemID.RAW_LOBSTER)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.wikiPage(P2P_WIKI)
				.build(),
			fish("fish_barbarian_otto", "Barbarian fishing (Otto's Grotto)", 48, 60, 45_000,
				Location.of("Otto's Grotto", 2500, 3510, 8), null, "Use-rod", "Fishing spot")
				.tool(ItemReq.tool("Barbarian rod", ItemID.BRUT_FISHING_ROD, ItemID.FISHINGROD_PEARL_BRUT))
				.input(FEATHERS)
				.product(ItemID.BRUT_SPAWNING_TROUT).product(ItemID.BRUT_SPAWNING_SALMON).product(ItemID.BRUT_STURGEON)
				.members(true).tag(MethodTag.POWER).tag(MethodTag.FAST).dropByDefault(true)
				.requirementText("Barbarian Training (talk to Otto), 15 Agility and 15 Strength")
				.notes("Also gives Agility and Strength xp. Drop fish as you go.")
				.wikiPage("Barbarian_Fishing")
				.build(),
			fish("fish_sharks_catherby", "Sharks (Catherby)", 76, 110, 22_000,
				Location.of("Catherby beach", 2840, 3431, 8), Banks.CATHERBY, "Harpoon", "Fishing spot")
				.toolType(ToolType.HARPOON)
				.product(ItemID.RAW_SHARK)
				.members(true).tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.notes("Use the spots that offer Net and Harpoon.")
				.wikiPage(P2P_WIKI)
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
				.build()
		);
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
