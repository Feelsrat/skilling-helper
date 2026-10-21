package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
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
 * Based on the OSRS wiki "Construction training" guide. Building happens in the player's instanced house, so any
 * instance counts as being there; outside, the guide points at the Rimmington house portal.
 */
final class ConstructionGuide
{
	private static final String WIKI = "Construction_training";
	private static final Location HOUSE_PORTAL = Location.of("Rimmington house portal", 2954, 3224, 8);
	private static final ItemReq HAMMER = ItemReq.tool("Hammer", ItemID.HAMMER);
	private static final ItemReq SAW = ItemReq.tool("Saw", ItemID.POH_SAW);
	// Build-and-remove cycles per hour with a servant fetching planks
	private static final int BUILDS_PER_HOUR = 500;

	static List<Method> methods()
	{
		return List.of(
			build("con_crude_chairs", "Crude wooden chairs", 1, 58, 2, "Planks", ItemID.WOODPLANK, "Chair space")
				.input(ItemReq.stack("Nails", 2, ItemID.NAILS, ItemID.NAILS_IRON, ItemID.NAILS_MITHRIL,
					ItemID.NAILS_ADAMANT, ItemID.NAILS_RUNE))
				.tag(MethodTag.CHEAP)
				.build(),
			build("con_bookcases", "Wooden bookcases", 4, 115, 4, "Planks", ItemID.WOODPLANK, "Bookcase space")
				.input(ItemReq.stack("Nails", 4, ItemID.NAILS, ItemID.NAILS_IRON, ItemID.NAILS_MITHRIL,
					ItemID.NAILS_ADAMANT, ItemID.NAILS_RUNE))
				.build(),
			build("con_oak_chairs", "Oak chairs", 19, 120, 2, "Oak planks", ItemID.PLANK_OAK, "Chair space").build(),
			build("con_oak_larders", "Oak larders", 33, 480, 8, "Oak planks", ItemID.PLANK_OAK, "Larder space")
				.tag(MethodTag.FAST)
				.notes("The standard way to train: build a kitchen and use a demon butler (or Phials) to fetch planks.")
				.build(),
			build("con_mahogany_tables", "Mahogany tables", 52, 840, 6, "Mahogany planks", ItemID.PLANK_MAHOGANY,
				"Table space")
				.tag(MethodTag.FAST)
				.build(),
			Method.builder()
				.id("con_mahogany_homes")
				.skill(Skill.CONSTRUCTION)
				.type(MethodType.ACTIVITY)
				.name("Mahogany Homes contracts")
				.minLevel(1)
				.xpPerAction(300)
				.xpPerHour(40_000)
				.members(true)
				.tag(MethodTag.MINIGAME).tag(MethodTag.CHEAP)
				.location(Location.of("Falador contracts office", 2989, 3363, 8))
				.tool(HAMMER).tool(SAW)
				.target(Target.npc(null, "Amy"))
				.supply(ItemReq.supply("Planks for your tier (plank, oak, teak or mahogany)", 26, ItemID.PLANK_OAK,
					ItemID.WOODPLANK, ItemID.PLANK_TEAK, ItemID.PLANK_MAHOGANY))
				.supply(ItemReq.supply("Steel bars", 4, ItemID.STEEL_BAR))
				.instruction("Get a contract from Amy, go to the client's house and repair or build the furniture "
					+ "marked on the map")
				.notes("Uses fewer planks per xp than building in your house and earns the Carpenter's outfit "
					+ "(+2.5% Construction xp).")
				.wikiPage("Mahogany_Homes")
				.build()
		);
	}

	private static Method.MethodBuilder build(String id, String name, int level, double xp, int planks,
		String plankName, int plank, String space)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.CONSTRUCTION)
			.type(MethodType.PROCESS)
			.name("Build " + name.toLowerCase())
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * BUILDS_PER_HOUR))
			.members(true)
			.location(HOUSE_PORTAL)
			.instanced(true)
			.bank(Banks.GRAND_EXCHANGE)
			.tool(HAMMER).tool(SAW)
			.input(ItemReq.consumed(plankName, 24, planks, plank))
			.target(Target.object("Build", space))
			.target(Target.object("Remove", name.substring(0, name.length() - 1)))
			.instruction("In build mode, build " + name.toLowerCase() + " in the " + space.toLowerCase()
				+ ", then remove them and build again")
			.wikiPage(WIKI);
	}

	private ConstructionGuide()
	{
	}
}
