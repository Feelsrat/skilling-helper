package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
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
 * Based on the OSRS wiki "Construction training" guide. Building happens in the player's instanced house, so any
 * instance counts as being there; outside, the guide points at the Rimmington house portal.
 */
final class ConstructionGuide
{
	private static final Location SAWMILL = Location.of("Varrock sawmill", 3302, 3491, 5);
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
			build("con_mythical_capes", "Mounted mythical capes", 50, 370, 3, "Teak planks", ItemID.PLANK_TEAK,
				"Guild trophy space")
				.clearTargets()
				.target(Target.object("Build", "Guild trophy space"))
				.target(Target.object("Remove", "Mythical cape"))
				.quest(Quest.DRAGON_SLAYER_II)
				.tool(ItemReq.tool("Mythical cape", ItemID.MYTHICAL_CAPE))
				.notes("More xp per teak plank than other teak furniture; the cape comes back when you remove it. "
					+ "Built in the Quest hall.")
				.build(),
			build("con_teak_benches", "Teak garden benches", 66, 540, 6, "Teak planks", ItemID.PLANK_TEAK,
				"Seating space")
				.clearTargets()
				.target(Target.object("Build", "Seating space"))
				.target(Target.object("Remove", "Teak garden bench"))
				.tag(MethodTag.FAST)
				.notes("Needs a Superior garden. The fastest use of teak planks, but needs precise clicks.")
				.build(),
			build("con_oak_doors", "Oak doors", 74, 600, 10, "Oak planks", ItemID.PLANK_OAK, "Door space")
				.clearTargets()
				.target(Target.object("Build", "Door space"))
				.target(Target.object("Remove", "Oak door"))
				.tag(MethodTag.FAST).tag(MethodTag.CHEAP)
				.notes("Built in a dungeon room. Cheaper and less click-intensive than mahogany tables.")
				.build(),
			Method.builder()
				.id("con_mahogany_homes")
				.skill(Skill.CONSTRUCTION)
				.type(MethodType.ACTIVITY)
				.name("Mahogany Homes contracts")
				.minLevel(1)
				.xpPerAction(300)
				.xpPerHour(25_000)
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
				.rateAt(20, 40_000).rateAt(50, 60_000).rateAt(70, 85_000)
				.howTo("Talk to the highlighted Amy in the contracts office in Falador (follow the arrow) and ask "
					+ "for a contract. Pick the tier that matches your planks: Beginner (planks), Novice (oak), "
					+ "Adept (teak) or Expert (mahogany).")
				.howTo("She tells you the client and the town (Varrock, Falador, East Ardougne or Hosidius). Take "
					+ "a hammer, a saw, about 26 planks and a few steel bars, and go there. The house is marked "
					+ "on your world map.")
				.howTo("Inside the house, click each broken or missing piece of furniture and pick \"Repair\" or "
					+ "\"Build\". Check every room, upstairs too.")
				.howTo("When everything is done, talk to the client, then go back to Amy for your reward and the "
					+ "next contract.")
				.howTo("Spend the carpenter points Amy gives you on the Carpenter's outfit (more Construction "
					+ "xp).")
				.build()
		);
	}

	/**
	 * Planks, which the sawmill operator north-east of Varrock makes from logs for a fee.
	 */
	private static ItemReq sawn(String plankName, int perBuild, int plank)
	{
		int logs;
		String logName;
		int fee;
		switch (plank)
		{
			case ItemID.PLANK_OAK:
				logs = ItemID.OAK_LOGS;
				logName = "Oak logs";
				fee = 250;
				break;
			case ItemID.PLANK_TEAK:
				logs = ItemID.TEAK_LOGS;
				logName = "Teak logs";
				fee = 500;
				break;
			case ItemID.PLANK_MAHOGANY:
				logs = ItemID.MAHOGANY_LOGS;
				logName = "Mahogany logs";
				fee = 1_500;
				break;
			default:
				logs = ItemID.LOGS;
				logName = "Logs";
				fee = 100;
				break;
		}
		return ItemReq.consumed(plankName, 24, perBuild, plank)
			.madeFrom("Buy-plank from the sawmill operator: " + logName.toLowerCase() + " and " + fee
				+ " gp per plank", false, ItemReq.part(logName, 1, logs), ItemReq.stack("Coins", fee, ItemID.COINS))
			.madeAt(SAWMILL, Target.npc("Buy-plank", "Sawmill operator"));
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
			.input(sawn(plankName, planks, plank))
			.target(Target.object("Build", space))
			.target(Target.object("Remove", name.substring(0, name.length() - 1)))
			.instruction("In build mode, build " + name.toLowerCase() + " in the " + space.toLowerCase()
				+ ", then remove them and build again")
			.getThere("No house yet? Buy one from any estate agent (Varrock, Falador, Ardougne...) for 1,000 coins. "
				+ "Its portal is in Rimmington.")
			.howTo("Take your hammer, saw and the " + plankName.toLowerCase() + " out of the bank (the guide does "
				+ "this with you).")
			.howTo("Click the house portal in Rimmington and choose \"Build mode\".")
			.howTo("Go to your " + room(space) + ". If you don't have one yet, click a door to an empty spot in "
				+ "build mode, pick \"Build\" and choose the " + room(space) + ".")
			.howTo("Right-click the highlighted " + space.toLowerCase() + ", pick \"Build\", then click "
				+ name.toLowerCase().replaceAll("s$", "") + " in the menu.")
			.howTo("Right-click the new " + name.toLowerCase().replaceAll("s$", "") + ", pick \"Remove\" and "
				+ "confirm with \"Yes\". Build it again, and repeat until your planks run out.")
			.howTo("For more planks, leave by the portal and use noted planks on Phials next to it (\"Exchange "
				+ "All\"), or have a demon butler fetch them from your bank.")
			.wikiPage(WIKI);
	}

	/**
	 * The house room that holds this furniture space.
	 */
	private static String room(String space)
	{
		switch (space)
		{
			case "Larder space":
				return "kitchen";
			case "Table space":
				return "dining room";
			case "Guild trophy space":
				return "quest hall";
			case "Seating space":
				return "superior garden";
			case "Door space":
				return "dungeon room";
			default:
				return "parlour";
		}
	}

	private ConstructionGuide()
	{
	}
}
