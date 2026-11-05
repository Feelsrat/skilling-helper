package com.skillinghelper.diary.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.ToolType;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Karamja diary, every tier. Easy to hard keep each task in its own varbit; elite uses bits of one varp. Task wording
 * and requirements from the OSRS Wiki, listed in walking order.
 */
final class KaramjaDiary
{
	private static final Location VOLCANO = Location.of("Karamja Volcano entrance", 2857, 3169, 0, 2);
	private static final Zone VOLCANO_CAVE = Zone.of(2821, 9545, 2879, 9663, 0);
	private static final Zone TZHAAR = Zone.of(2360, 5056, 2560, 5185, 0);
	private static final Location BRIMHAVEN_DUNGEON = Location.of("Brimhaven Dungeon entrance", 2745, 3155, 0, 2);
	private static final Zone BRIMHAVEN_DUNGEON_ZONE = Zone.anyFloor(2560, 9411, 2752, 9599);
	private static final Location NATURE_RUINS = Location.of("Nature altar ruins, north of Shilo Village", 2869, 3019, 0, 3);
	private static final Zone NATURE_ALTAR = Zone.of(2374, 4809, 2421, 4859, 0);
	private static final DiaryItem NATURE_ACCESS = DiaryItem.anyOf("Nature tiara or talisman", 1, ItemID.NATURE_TALISMAN,
		ItemID.TIARA_NATURE, ItemID.NATURE_TALISMAN);
	private static final DiaryItem DUNGEON_FEE = DiaryItem.of("Coins (dungeon fee)", 875, ItemID.COINS);

	private KaramjaDiary()
	{
	}

	static List<DiaryTask> tasks()
	{
		List<DiaryTask> out = new ArrayList<>();
		easy(out);
		medium(out);
		hard(out);
		elite(out);
		return out;
	}

	private static DiaryStage volcanoDown()
	{
		return DiaryStage.builder()
			.title("Climb into Karamja Volcano")
			.at(VOLCANO)
			.object("Rocks")
			.line("1. The volcano is in the middle of north Karamja, east of Brimhaven.")
			.line("2. Click Climb-down on the Rocks (outlined) at the top.")
			.build();
	}

	private static DiaryStage intoTzhaar()
	{
		return DiaryStage.builder()
			.title("Go into the TzHaar city")
			.inside(VOLCANO_CAVE)
			.at(Location.of("TzHaar cave entrance, Karamja Volcano", 2864, 9572, 0, 2))
			.object("Cave entrance")
			.line("1. Walk south-east to the Cave entrance (outlined) and Enter it.")
			.build();
	}

	private static DiaryStage brimhavenEntrance()
	{
		return DiaryStage.builder()
			.title("Enter Brimhaven Dungeon")
			.at(BRIMHAVEN_DUNGEON)
			.npc("Saniboch")
			.object("Dungeon entrance")
			.line("1. The dungeon is south-west of Brimhaven. Pay Saniboch (outlined) 875 coins.")
			.line("2. Click Enter on the Dungeon entrance (outlined).")
			.build();
	}

	private static DiaryStage brimhavenStage(String title, Location at, String object, String... lines)
	{
		DiaryStage.DiaryStageBuilder b = DiaryStage.builder()
			.title(title)
			.inside(BRIMHAVEN_DUNGEON_ZONE)
			.at(at)
			.object(object);
		for (String line : lines)
		{
			b.line(line);
		}
		return b.build();
	}

	private static void easy(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.EASY;
		out.add(task(t, 4, VarbitID.ATJUN_EASY_BOAT_SARIM, "Boat to Port Sarim", "Travel to Port Sarim via the dock, east of Musa Point.")
			.item(DiaryItem.of("Coins", 30, ItemID.COINS))
			.stage(DiaryStage.builder()
				.title("Sail from Musa Point to Port Sarim")
				.at(Location.of("Customs officer, Musa Point dock", 2954, 3147, 0, 3))
				.npc("Customs officer")
				.line("1. If you're not on Karamja yet, take the boat from Port Sarim to Karamja first (30 coins).")
				.line("2. Talk to the Customs officer (outlined) on the Musa Point dock and pay 30 coins to sail.")
				.build())
			.build());
		out.add(task(t, 8, VarbitID.ATJUN_EASY_SEAWEED, "5 seaweed", "Collect 5 seaweed from anywhere on Karamja.")
			.varbitDone(5)
			.stage(DiaryStage.builder()
				.title("Pick up seaweed 5 times")
				.at(Location.of("Beach by Jiminua's Jungle Store", 2924, 3150, 0, 6))
				.line("1. Seaweed sometimes washes up on the beach near Jiminua's Jungle Store.")
				.line("2. Pick one up, drop it, and pick it up again until you've done it 5 times.")
				.build())
			.build());
		out.add(task(t, 1, VarbitID.ATJUN_EASY_BANANA, "5 bananas", "Pick 5 bananas from the plantation located east of the volcano.")
			.varbitDone(5)
			.stage(DiaryStage.builder()
				.title("Pick 5 bananas")
				.at(Location.of("Banana plantation, Musa Point", 2916, 3162, 0, 5))
				.object("Banana Tree").object("Banana tree")
				.line("1. Click Pick on the Banana trees (outlined) until you've picked 5 bananas.")
				.build())
			.build());
		out.add(task(t, 7, VarbitID.ATJUN_EASY_FISHING, "Musa Point fishing", "Use the Fishing spots north of the banana plantation.")
			.item(DiaryItem.anyOf("Small fishing net, lobster pot or harpoon", 1, ItemID.NET, ItemID.NET,
				ItemID.LOBSTER_POT, ItemID.HARPOON))
			.stage(DiaryStage.builder()
				.title("Fish at Musa Point")
				.at(Location.of("Fishing spots north of the banana plantation", 2924, 3178, 0, 4))
				.npc("Fishing spot")
				.line("1. Click a Fishing spot (outlined) on the coast; Net with a small net works at any level.")
				.line("2. Catching anything finishes the task.")
				.build())
			.build());
		out.add(task(t, 9, VarbitID.ATJUN_EASY_TZHAAR, "Enter the Fight Cave", "Attempt the TzHaar Fight Pits or Fight Cave.")
			.stage(volcanoDown())
			.stage(intoTzhaar())
			.stage(DiaryStage.builder()
				.title("Enter the Fight Cave, then leave")
				.inside(TZHAAR)
				.at(Location.of("Fight Cave entrance, TzHaar city", 2438, 5167, 0, 2))
				.object("Cave entrance")
				.line("1. Walk west through the city to the Fight Cave entrance (outlined) and Enter it.")
				.line("2. Once the first wave starts, the task is done: you can leave at once through the exit.")
				.line("Don't take valuables you'd mind losing if you stay; dying in the Fight Cave keeps your items though.")
				.build())
			.build());
		out.add(task(t, 10, VarbitID.ATJUN_EASY_JOGRE, "Kill a jogre", "Kill a jogre in the Pothole dungeon.")
			.note("A weapon and some food (jogres are level 53)")
			.stage(DiaryStage.builder()
				.title("Jump into the Pothole")
				.at(Location.of("Pothole, south of the volcano", 2825, 3119, 0, 2))
				.object("Pothole")
				.line("1. The pothole is south-west of the volcano. Click Enter on it (outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Kill a jogre")
				.inside(Zone.of(2824, 9462, 2883, 9533, 0))
				.npc("Jogre")
				.line("1. Kill a Jogre (outlined).")
				.build())
			.build());
		out.add(task(t, 3, VarbitID.ATJUN_EASY_GOLD, "Gold on Karamja", "Mine some gold from the rocks on the north-west peninsula of Karamja.")
			.skill(Skill.MINING, 40)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(DiaryStage.builder()
				.title("Mine gold north of Brimhaven")
				.at(Location.of("Gold rocks, north-west Karamja", 2732, 3223, 0, 4))
				.object("Gold rocks")
				.line("1. The gold rocks are on the peninsula north of Brimhaven.")
				.line("2. Mine the Gold rocks (outlined) until you get one ore.")
				.build())
			.build());
		out.add(task(t, 2, VarbitID.ATJUN_EASY_SWING, "Rope swing", "Use the rope swing to travel to the Moss Giant Island north-west of Karamja.")
			.skill(Skill.AGILITY, 10)
			.stage(DiaryStage.builder()
				.title("Swing over to Moss Giant Island")
				.at(Location.of("Rope swing, north-west Karamja", 2708, 3209, 0, 2))
				.object("Ropeswing")
				.line("1. The rope swing is on the north-west tip of Karamja, past the gold rocks.")
				.line("2. Click Swing-on on the Ropeswing (outlined).")
				.build())
			.build());
		out.add(task(t, 5, VarbitID.ATJUN_EASY_BOAT_ARDY, "Boat to Ardougne", "Travel to Ardougne via the port near Brimhaven.")
			.item(DiaryItem.of("Coins", 30, ItemID.COINS))
			.stage(DiaryStage.builder()
				.title("Sail from Brimhaven to Ardougne")
				.at(Location.of("Brimhaven dock", 2772, 3228, 0, 3))
				.npc("Captain Barnaby")
				.line("1. Talk to Captain Barnaby (outlined) on the Brimhaven dock and pay 30 coins.")
				.build())
			.build());
		out.add(task(t, 6, VarbitID.ATJUN_EASY_CAIRN, "Cairn Island", "Explore Cairn Island to the west of Karamja.")
			.skill(Skill.AGILITY, 15)
			.stage(DiaryStage.builder()
				.title("Climb onto Cairn Island")
				.at(Location.of("Rocks to Cairn Island, west Karamja", 2770, 2979, 0, 4))
				.object("Rocks")
				.line("1. Cairn Island is off Karamja's west coast, south of Brimhaven.")
				.line("2. Climb the Rocks (outlined) by the shore and walk onto the island.")
				.build())
			.build());
	}

	private static void medium(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.MEDIUM;
		out.add(task(t, 12, VarbitID.ATJUN_MED_GLIDER, "Glider to Karamja", "Use the gnome glider to travel to Karamja.")
			.quest(Quest.THE_GRAND_TREE)
			.stage(DiaryStage.builder()
				.title("Fly to Karamja by gnome glider")
				.line("1. Go to any gnome glider (Tree Gnome Stronghold, Al Kharid, White Wolf Mountain...).")
				.line("2. Talk to the pilot and pick Gandius (Karamja).")
				.build())
			.build());
		out.add(task(t, 6, VarbitID.ATJUN_MED_SPIDER, "Spider on a stick", "Cook a spider on a stick.")
			.skill(Skill.COOKING, 16)
			.item(DiaryItem.of("Spider carcass", ItemID.VILLAGE_SPIDER_CARCASS))
			.item(DiaryItem.anyOf("Skewer stick or arrow shaft", 1, ItemID.ARROW_SHAFT, ItemID.TBW_SKEWER_STICK,
				ItemID.ARROW_SHAFT))
			.stage(DiaryStage.builder()
				.title("Skewer and cook the spider")
				.use(ItemID.VILLAGE_SPIDER_CARCASS).use(ItemID.ARROW_SHAFT).use(ItemID.TBW_SKEWER_STICK)
				.line("1. Use the Spider carcass on the skewer stick or arrow shaft (outlined).")
				.line("2. Use it on any fire or range to cook it. This works anywhere.")
				.build())
			.build());
		out.add(task(t, 1, VarbitID.ATJUN_MED_AGILITY, "Agility Arena ticket", "Claim a ticket from the Agility Arena in Brimhaven.")
			.note("40 Agility recommended")
			.item(DiaryItem.of("Coins", 200, ItemID.COINS))
			.stage(DiaryStage.builder()
				.title("Go down into the Agility Arena")
				.at(Location.of("Brimhaven Agility Arena", 2809, 3194, 0, 3))
				.npc("Cap'n Izzy No-Beard")
				.object("Ladder")
				.line("1. Pay Cap'n Izzy No-Beard (outlined) 200 coins, then Climb-down the Ladder.")
				.build())
			.stage(DiaryStage.builder()
				.title("Tag the flashing pillar")
				.inside(Zone.anyFloor(2747, 9531, 2813, 9601))
				.line("1. Look for the pillar with a flashing arrow over it.")
				.line("2. Cross the obstacles to it and click Tag. The ticket is the task.")
				.build())
			.build());
		out.add(task(t, 2, VarbitID.ATJUN_MED_VOLCANO, "Volcano hidden wall", "Discover hidden wall in the dungeon below the volcano.")
			.questStarted(Quest.DRAGON_SLAYER_I)
			.stage(volcanoDown())
			.stage(DiaryStage.builder()
				.title("Find the hidden wall")
				.inside(VOLCANO_CAVE)
				.at(Location.of("Hidden wall, Karamja Volcano", 2836, 9600, 0, 2))
				.object("Wall")
				.line("1. Walk north-west to the wall (outlined) and click Open on it.")
				.build())
			.build());
		out.add(task(t, 3, VarbitID.ATJUN_MED_CRANDOR, "Visit Crandor", "Visit the Isle of Crandor via the dungeon below the volcano.")
			.questStarted(Quest.DRAGON_SLAYER_I)
			.note("Lesser demons on the way; bring food")
			.stage(volcanoDown())
			.stage(DiaryStage.builder()
				.title("Climb the rope up to Crandor")
				.inside(VOLCANO_CAVE)
				.at(Location.of("Climbing rope to Crandor", 2833, 9657, 0, 2))
				.object("Wall").object("Climbing rope")
				.line("1. Go through the hidden wall north-west and past the lesser demons.")
				.line("2. Click Climb on the Climbing rope (outlined) at the north end.")
				.build())
			.build());
		out.add(task(t, 4, VarbitID.ATJUN_MED_CART, "Shilo cart", "Use Vigroy and Hajedy's cart service.")
			.quest(Quest.SHILO_VILLAGE)
			.item(DiaryItem.of("Coins", 200, ItemID.COINS))
			.stage(DiaryStage.builder()
				.title("Ride the cart")
				.at(Location.of("Hajedy's cart, Brimhaven", 2778, 3212, 0, 4))
				.npc("Hajedy")
				.line("1. Talk to Hajedy (outlined) east of Brimhaven and pay 200 coins to ride to Shilo Village.")
				.build())
			.build());
		out.add(task(t, 13, VarbitID.ATJUN_MED_FARMING, "Brimhaven fruit tree", "Grow a healthy fruit tree in the patch near Brimhaven.")
			.skill(Skill.FARMING, 27)
			.item(DiaryItem.anyOf("Fruit tree sapling (apple is fine)", 1, ItemID.PLANTPOT_APPLE_SAPLING,
				ItemID.PLANTPOT_PALM_SAPLING, ItemID.PLANTPOT_APPLE_SAPLING))
			.item(DiaryItem.of("Spade", ItemID.SPADE))
			.item(DiaryItem.of("Rake", ItemID.RAKE))
			.stage(DiaryStage.builder()
				.title("Grow a fruit tree in the Brimhaven patch")
				.at(Location.of("Fruit tree patch, Brimhaven", 2765, 3213, 0, 3))
				.object("Fruit Tree Patch").object("Fruit tree patch")
				.line("1. Rake the Fruit tree patch (outlined), then plant the sapling.")
				.line("2. Come back when it's fully grown (about 16 hours for apple) and click Check-health on it.")
				.build())
			.build());
		out.add(task(t, 15, VarbitID.ATJUN_MED_SHILO_VINES, "Brimhaven vines", "Chop the vines to gain deeper access to Brimhaven Dungeon.")
			.skill(Skill.WOODCUTTING, 10)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DUNGEON_FEE)
			.stage(brimhavenEntrance())
			.stage(brimhavenStage("Chop the vines", Location.of("Vines, Brimhaven Dungeon", 2690, 9564, 0, 2), "Vines",
				"1. Click Chop-down on the Vines (outlined) just inside the dungeon."))
			.build());
		out.add(task(t, 16, VarbitID.ATJUN_MED_SHILO_LAVA, "Brimhaven stepping stones", "Cross the lava using the stepping stones within Brimhaven Dungeon.")
			.skill(Skill.WOODCUTTING, 10).skill(Skill.AGILITY, 12)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DUNGEON_FEE)
			.stage(brimhavenEntrance())
			.stage(brimhavenStage("Cross the stepping stones", Location.of("Stepping stones, Brimhaven Dungeon", 2649, 9561, 0, 2),
				"Stepping stones", "1. Chop through the vines, then walk west to the Stepping stones (outlined) and Jump-across."))
			.build());
		out.add(task(t, 17, VarbitID.ATJUN_MED_SHILO_STAIRS, "Brimhaven stairs", "Climb the stairs within Brimhaven Dungeon.")
			.skill(Skill.WOODCUTTING, 10)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DUNGEON_FEE)
			.stage(brimhavenEntrance())
			.stage(brimhavenStage("Climb the stairs by the moss giants", Location.of("Stairs, Brimhaven Dungeon", 2637, 9515, 0, 2),
				"Stairs", "1. Past the stepping stones, walk south to the Stairs (outlined) by the moss giants and climb them."))
			.build());
		out.add(task(t, 5, VarbitID.ATJUN_MED_CLEANUP, "100% Tai Bwo Wannai favour", "Earn 100% favour in the village of Tai Bwo Wannai.")
			.quest(Quest.JUNGLE_POTION)
			.skill(Skill.WOODCUTTING, 10)
			.item(DiaryItem.anyOf("Machete", 1, ItemID.MACHETTE, ItemID.MACHETTE))
			.stage(DiaryStage.builder()
				.title("Clean up Tai Bwo Wannai")
				.at(Location.of("Murcaily, Tai Bwo Wannai", 2815, 3083, 0, 5))
				.npc("Murcaily")
				.object("Jungle").object("Light jungle").object("Medium jungle").object("Dense jungle")
				.object("Village fence").object("Broken fence")
				.line("1. Talk to Murcaily (outlined) in the east of the village to start the cleanup.")
				.line("2. Hack the jungle (outlined) inside the fence with your machete and repair broken fences.")
				.line("3. Keep going until your favour reaches 100%. Keep the trading sticks for the machete task.")
				.build())
			.build());
		out.add(task(t, 11, VarbitID.ATJUN_MED_MACHETTE, "Gem machete", "Exchange gems for a machete.")
			.quest(Quest.JUNGLE_POTION)
			.note("300 trading sticks (from the village cleanup)")
			.item(DiaryItem.of("Gout tuber", ItemID.VILLAGE_RARE_TUBER))
			.item(DiaryItem.anyOf("Opal (cut or uncut)", 3, ItemID.UNCUT_OPAL, ItemID.OPAL, ItemID.UNCUT_OPAL))
			.stage(DiaryStage.builder()
				.title("Trade gems for a machete")
				.at(Location.of("Safta Doc, Tai Bwo Wannai", 2790, 3100, 0, 3))
				.npc("Safta Doc")
				.line("1. First talk to Sharimika in the village about machetes, if you haven't.")
				.line("2. Talk to Safta Doc (outlined) with 3 opals, the Gout tuber and 300 trading sticks.")
				.build())
			.build());
		out.add(task(t, 8, VarbitID.ATJUN_MED_TEAK, "Teak log", "Cut a log from a teak tree.")
			.quest(Quest.JUNGLE_POTION)
			.skill(Skill.WOODCUTTING, 35)
			.note("Hardwood Grove: 100 trading sticks to enter")
			.item(DiaryItem.tool(ToolType.AXE))
			.stage(DiaryStage.builder()
				.title("Chop a teak in the Hardwood Grove")
				.at(Location.of("Hardwood Grove, Tai Bwo Wannai", 2822, 3078, 0, 4))
				.object("Hardwood grove doors").object("Teak").object("Teak tree")
				.line("1. Go through the Hardwood grove doors in the south of Tai Bwo Wannai (100 trading sticks).")
				.line("2. Chop a Teak (outlined) until you get a log.")
				.build())
			.build());
		out.add(task(t, 9, VarbitID.ATJUN_MED_MAHOGANY, "Mahogany log", "Cut a log from a mahogany tree.")
			.quest(Quest.JUNGLE_POTION)
			.skill(Skill.WOODCUTTING, 50)
			.note("Hardwood Grove: 100 trading sticks to enter")
			.item(DiaryItem.tool(ToolType.AXE))
			.stage(DiaryStage.builder()
				.title("Chop a mahogany in the Hardwood Grove")
				.at(Location.of("Hardwood Grove, Tai Bwo Wannai", 2820, 3080, 0, 4))
				.object("Hardwood grove doors").object("Mahogany").object("Mahogany tree")
				.line("1. Go through the Hardwood grove doors (100 trading sticks).")
				.line("2. Chop a Mahogany (outlined) until you get a log.")
				.build())
			.build());
		out.add(task(t, 19, VarbitID.ATJUN_MED_TOPAZ, "Red topaz", "Mine a red topaz from a gem rock.")
			.quest(Quest.JUNGLE_POTION).quest(Quest.SHILO_VILLAGE)
			.skill(Skill.MINING, 40)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(DiaryStage.builder()
				.title("Mine gem rocks in Shilo Village")
				.at(Location.of("Gem mine, Shilo Village", 2823, 2999, 0, 4))
				.object("Gem rocks")
				.line("1. Mine the Gem rocks (outlined) in the north-east of Shilo Village.")
				.line("2. Keep mining (bank in the village) until a Red topaz comes out. It can take a while.")
				.build())
			.build());
		out.add(task(t, 14, VarbitID.ATJUN_MED_GRAAHK, "Horned graahk", "Trap a horned graahk.")
			.skill(Skill.HUNTER, 41)
			.item(DiaryItem.of("Teasing stick", ItemID.HUNTING_TEASING_STICK))
			.item(DiaryItem.of("Knife", ItemID.KNIFE))
			.item(DiaryItem.of("Logs", ItemID.LOGS))
			.stage(DiaryStage.builder()
				.title("Catch a horned graahk in a pit")
				.at(Location.of("Horned graahk area, Karamja cliffs", 2770, 3003, 0, 6))
				.npc("Horned graahk").object("Pit")
				.line("1. The graahks are on the cliffs north-west of Shilo Village (reached by the cart or Cairn Isle).")
				.line("2. Click Trap on a Pit (outlined) with the logs and knife to cover it.")
				.line("3. Tease a Horned graahk with the stick, then jump over the pit so it follows you in.")
				.build())
			.build());
		out.add(task(t, 7, VarbitID.ATJUN_MED_KHAZARD, "Lady of the Waves", "Charter the Lady of the Waves from Cairn Isle to Port Khazard.")
			.quest(Quest.SHILO_VILLAGE)
			.item(DiaryItem.anyOf("Ship ticket (or 50 coins)", 1, ItemID.SHILOSHIPTICKET, ItemID.SHILOSHIPTICKET))
			.stage(DiaryStage.builder()
				.title("Climb aboard the Lady of the Waves")
				.at(Location.of("Lady of the Waves, south of Cairn Isle", 2763, 2952, 0, 3))
				.object("Ladder")
				.npc("Captain Shanks")
				.line("1. The ship is moored west of Shilo Village. Climb the Ladder (outlined) onto the deck.")
				.line("2. Talk to Captain Shanks (outlined) and sail to Port Khazard.")
				.build())
			.build());
		out.add(task(t, 18, VarbitID.ATJUN_MED_CHARTER, "Karamja shipyard charter", "Charter a ship from the shipyard in the far east of Karamja.")
			.questStarted(Quest.THE_GRAND_TREE)
			.item(DiaryItem.of("Coins (fare)", 1_000, ItemID.COINS))
			.stage(DiaryStage.builder()
				.title("Charter a ship at the shipyard")
				.at(Location.of("Karamja shipyard", 3001, 3032, 0, 5))
				.npc("Trader Crewmember").npc("Trader crewmember")
				.line("1. The shipyard is on Karamja's far south-east coast. Show your pass at the gate if asked.")
				.line("2. Talk to a Trader Crewmember (outlined) and charter a ship anywhere.")
				.build())
			.build());
		out.add(task(t, 10, VarbitID.ATJUN_MED_KARAMBWAN, "Catch a karambwan", "Catch a karambwan.")
			.questStarted(Quest.TAI_BWO_WANNAI_TRIO)
			.skill(Skill.FISHING, 65)
			.item(DiaryItem.of("Karambwan vessel", ItemID.TBWT_KARAMBWAN_VESSEL))
			.item(DiaryItem.of("Small fishing net", ItemID.NET))
			.stage(DiaryStage.builder()
				.title("Net some karambwanji")
				.at(Location.of("Karambwanji spot, south of Tai Bwo Wannai", 2791, 3019, 0, 4))
				.npc("Fishing spot")
				.line("1. Net a few Karambwanji at the fishing spot (outlined) by the lake south of Tai Bwo Wannai.")
				.build())
			.stage(DiaryStage.builder()
				.title("Catch a karambwan")
				.carrying(ItemID.TBWT_RAW_KARAMBWANJI)
				.at(Location.of("Karambwan spot, Musa Point", 2899, 3119, 0, 4))
				.npc("Fishing spot")
				.use(ItemID.TBWT_RAW_KARAMBWANJI).use(ItemID.TBWT_KARAMBWAN_VESSEL)
				.line("1. Use the karambwanji on the vessel to bait it.")
				.line("2. Fish at the karambwan spot (outlined) on the coast east of the volcano.")
				.build())
			.build());
	}

	private static void hard(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.HARD;
		out.add(task(t, 3, VarbitID.ATJUN_HARD_OOMLIE, "Oomlie wrap", "Eat an oomlie wrap.")
			.item(DiaryItem.owned("Cooked oomlie wrap", "Wrap a raw oomlie in a palm leaf (50 Cooking) and cook it, or "
				+ "buy one on the Grand Exchange.", ItemID.COOKED_OOMLIE))
			.stage(DiaryStage.builder()
				.title("Eat the oomlie wrap")
				.use(ItemID.COOKED_OOMLIE)
				.line("1. Click Eat on the Cooked oomlie wrap (outlined).")
				.build())
			.build());
		out.add(task(t, 5, VarbitID.ATJUN_HARD_KARAMBWAN, "Cook a karambwan", "Cook a karambwan thoroughly.")
			.quest(Quest.TAI_BWO_WANNAI_TRIO)
			.skill(Skill.COOKING, 30)
			.note("Tinsay taught you to cook them after the quest")
			.item(DiaryItem.of("Raw karambwan", 3, ItemID.TBWT_RAW_KARAMBWAN))
			.stage(DiaryStage.builder()
				.title("Cook a karambwan thoroughly")
				.use(ItemID.TBWT_RAW_KARAMBWAN)
				.line("1. Use a Raw karambwan on any range or fire and choose Cook (not Partially cook).")
				.line("2. Repeat with another if it burns.")
				.build())
			.build());
		out.add(task(t, 4, VarbitID.ATJUN_HARD_NATURE, "Craft nature runes", "Craft some nature runes from Essence.")
			.skill(Skill.RUNECRAFT, 44)
			.item(DiaryItem.anyOf("Pure essence", 1, ItemID.BLANKRUNE_HIGH, DiaryStages.PURE_ESSENCE))
			.item(NATURE_ACCESS)
			.stages(DiaryStages.runeAltar("north of Shilo Village, in the south Karamja jungle", NATURE_RUINS,
				NATURE_ALTAR, "Craft some nature runes", ItemID.NATURE_TALISMAN, null))
			.build());
		out.add(task(t, 7, VarbitID.ATJUN_HARD_XBOW, "Volcano crossbow shortcut", "Use the crossbow shortcut south of the volcano.")
			.skill(Skill.AGILITY, 53).skill(Skill.RANGED, 42).skill(Skill.STRENGTH, 21)
			.item(DiaryStages.MITH_GRAPPLE)
			.item(DiaryStages.ANY_CROSSBOW)
			.stage(DiaryStage.builder()
				.title("Grapple across the shortcut")
				.at(Location.of("Grapple shortcut south of the volcano", 2874, 3135, 0, 3))
				.object("Strong Tree").object("Strong tree")
				.line("1. Wield the crossbow and the Mith grapple.")
				.line("2. Click Swing on the Strong tree (outlined) south of the volcano.")
				.build())
			.build());
		out.add(task(t, 9, VarbitID.ATJUN_HARD_DURADEL, "Task from Duradel", "Be assigned a Slayer task by the Slayer Master in Shilo Village.")
			.quest(Quest.SHILO_VILLAGE)
			.combat(100)
			.skill(Skill.SLAYER, 50)
			.stage(DiaryStage.builder()
				.title("Go up to Duradel")
				.at(Location.of("Duradel's hut, Shilo Village", 2871, 2971, 0, 2))
				.object("Ladder")
				.line("1. Climb-up the Ladder (outlined) in the hut in north-east Shilo Village.")
				.build())
			.stage(DiaryStage.builder()
				.title("Get a task from Duradel")
				.inside(Zone.of(2863, 2964, 2876, 2986, 1))
				.npc("Duradel")
				.line("1. Talk to Duradel (outlined) and ask for an assignment. Finish your current task first.")
				.build())
			.build());
		out.add(task(t, 10, VarbitID.ATJUN_HARD_DRAGON, "Metal dragon in Brimhaven", "Kill a metal dragon in Brimhaven Dungeon.")
			.skill(Skill.WOODCUTTING, 10)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DUNGEON_FEE)
			.item(DiaryStages.DRAGONFIRE_SHIELD)
			.note("Gear and food to kill a bronze dragon (level 131)")
			.stage(brimhavenEntrance())
			.stage(DiaryStage.builder()
				.title("Kill a bronze dragon")
				.inside(BRIMHAVEN_DUNGEON_ZONE)
				.at(Location.of("Bronze dragons, Brimhaven Dungeon", 2735, 9488, 0, 6))
				.npc("Bronze dragon").npc("Iron dragon").npc("Steel dragon")
				.line("1. Wear your anti-dragon shield. Chop through the vines and head south to the metal dragons.")
				.line("2. Kill a Bronze dragon (outlined); iron or steel ones count too.")
				.build())
			.build());
		out.add(task(t, 8, VarbitID.ATJUN_HARD_PALM, "5 palm leaves", "Collect 5 palm leaves.")
			.varbitDone(5)
			.questStarted(Quest.LEGENDS_QUEST)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DiaryItem.anyOf("Machete", 1, ItemID.MACHETTE, ItemID.MACHETTE))
			.stage(DiaryStage.builder()
				.title("Get palm leaves in the Kharazi Jungle")
				.at(Location.of("Leafy palm trees, Kharazi Jungle", 2845, 2915, 0, 6))
				.object("Leafy palm tree")
				.line("1. Cut through the jungle south of Shilo Village with your machete.")
				.line("2. Chop a Leafy palm tree (outlined), then pick the leaf up, drop it and pick it up until you've done it 5 times.")
				.build())
			.build());
		out.add(task(t, 6, VarbitID.ATJUN_HARD_DEATHWING, "Kill a death wing", "Kill a deathwing in the dungeon under the Kharazi Jungle.")
			.questStarted(Quest.LEGENDS_QUEST)
			.skill(Skill.AGILITY, 50).skill(Skill.THIEVING, 50).skill(Skill.MINING, 52).skill(Skill.STRENGTH, 50)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.item(DiaryItem.anyOf("Machete", 1, ItemID.MACHETTE, ItemID.MACHETTE))
			.item(DiaryItem.of("Lockpick", ItemID.LOCKPICK))
			.stage(DiaryStage.builder()
				.title("Enter the cave under the Kharazi Jungle")
				.at(Location.of("Cave entrance, Kharazi Jungle", 2782, 2937, 0, 3))
				.object("Rocks").object("Cave entrance")
				.line("1. Cut south through the jungle to the cave entrance by the rocks (outlined).")
				.line("2. Search the rocks and enter.")
				.build())
			.stage(DiaryStage.builder()
				.title("Get past the bookcase and gate")
				.inside(Zone.of(2764, 9314, 2815, 9345, 0))
				.object("Bookcase").object("Ancient Gate")
				.line("1. Search the Bookcase (outlined) and slide past it.")
				.line("2. Smash through the Ancient gate (outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Kill a death wing")
				.inside(Zone.of(2748, 9272, 2818, 9313, 0))
				.npc("Deathwing").npc("Death wing")
				.line("1. Kill a Death wing (outlined) in the cavern.")
				.build())
			.build());
		out.add(task(t, 2, VarbitID.ATJUN_HARD_FIGHTCAVE, "Kill a Ket-Zek", "Kill a Ket-Zek in the Fight Caves.")
			.note("Gear to reach wave 31 of the Fight Cave")
			.stage(volcanoDown())
			.stage(intoTzhaar())
			.stage(DiaryStage.builder()
				.title("Reach wave 31 and kill a Ket-Zek")
				.inside(TZHAAR)
				.at(Location.of("Fight Cave entrance, TzHaar city", 2438, 5167, 0, 2))
				.object("Cave entrance")
				.npc("Ket-Zek")
				.line("1. Enter the Fight Cave (outlined). Ket-Zeks first show up on wave 31.")
				.line("2. Use Protect from Magic against Ket-Zek and kill it, then you can leave.")
				.build())
			.build());
		out.add(task(t, 1, VarbitID.ATJUN_HARD_FIGHTPITS, "Fight Pits champion", "Become the Champion of the Fight Pits.")
			.note("At least one other player in the pits")
			.stage(volcanoDown())
			.stage(intoTzhaar())
			.stage(DiaryStage.builder()
				.title("Win a Fight Pits round")
				.inside(TZHAAR)
				.at(Location.of("Fight Pits, TzHaar city", 2399, 5173, 0, 3))
				.line("1. Go into the Fight Pits waiting room in the north-west of the city.")
				.line("2. Be the last player standing in a round. Quiet worlds with one other player are easiest.")
				.build())
			.build());
	}

	private static void elite(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.ELITE;
		out.add(elite(t, 4, "Antivenom in the Horseshoe mine", "Create an antivenom potion whilst standing in the horse shoe mine.")
			.skill(Skill.HERBLORE, 87)
			.item(DiaryItem.of("Antidote++(4)", ItemID.ANTIDOTE__4))
			.item(DiaryItem.of("Zulrah's scales", 20, ItemID.SNAKEBOSS_SCALE))
			.stage(DiaryStage.builder()
				.title("Mix antivenom in the Horseshoe mine")
				.at(Location.of("Horseshoe mine, north of Brimhaven", 2734, 3224, 0, 2))
				.inside(Zone.of(2731, 3222, 2736, 3227, 0))
				.use(ItemID.SNAKEBOSS_SCALE).use(ItemID.ANTIDOTE__4)
				.line("1. Stand inside the small Horseshoe mine north of Brimhaven.")
				.line("2. Use the Zulrah's scales on the Antidote++ (both outlined).")
				.build())
			.build());
		out.add(elite(t, 3, "Palm tree health", "Check the health of a palm tree in Brimhaven.")
			.skill(Skill.FARMING, 68)
			.item(DiaryItem.of("Palm sapling", ItemID.PLANTPOT_PALM_SAPLING))
			.item(DiaryItem.of("Spade", ItemID.SPADE))
			.item(DiaryItem.of("Rake", ItemID.RAKE))
			.item(DiaryItem.of("Papaya fruit (to pay the gardener)", 15, ItemID.PAPAYA))
			.stage(DiaryStage.builder()
				.title("Grow a palm tree in Brimhaven")
				.at(Location.of("Fruit tree patch, Brimhaven", 2765, 3213, 0, 3))
				.object("Fruit Tree Patch").object("Fruit tree patch")
				.line("1. Rake the patch and plant the Palm sapling. Pay the gardener the papayas to protect it.")
				.line("2. Come back when it's grown (about 16 hours) and click Check-health.")
				.build())
			.build());
		out.add(elite(t, 5, "Calquat health", "Check the health of your Calquat tree patch.")
			.skill(Skill.FARMING, 72)
			.item(DiaryItem.of("Calquat sapling", ItemID.PLANTPOT_CALQUAT_SAPLING))
			.item(DiaryItem.of("Spade", ItemID.SPADE))
			.item(DiaryItem.of("Rake", ItemID.RAKE))
			.item(DiaryItem.of("Poison ivy berries (to pay the gardener)", 8, ItemID.POISONIVY_BERRIES))
			.stage(DiaryStage.builder()
				.title("Grow a calquat tree in Tai Bwo Wannai")
				.at(Location.of("Calquat patch, Tai Bwo Wannai", 2796, 3101, 0, 3))
				.object("Calquat patch").object("Calquat Tree Patch")
				.line("1. The patch is next to Imiago in Tai Bwo Wannai. Rake it and plant the sapling.")
				.line("2. Come back when it's grown (about 21 hours) and click Check-health.")
				.build())
			.build());
		out.add(elite(t, 1, "56 nature runes", "Craft 56 Nature runes simultaneously from Essence without the use of Extracts.")
			.skill(Skill.RUNECRAFT, 91)
			.item(DiaryItem.anyOf("Pure essence", 28, ItemID.BLANKRUNE_HIGH, DiaryStages.PURE_ESSENCE))
			.item(NATURE_ACCESS)
			.stages(DiaryStages.runeAltar("north of Shilo Village, in the south Karamja jungle", NATURE_RUINS,
				NATURE_ALTAR, "Craft 56 nature runes at once", ItemID.NATURE_TALISMAN,
				"Wear the tiara so all 28 slots hold essence; at 91 each makes 2."))
			.build());
		out.add(elite(t, 2, "Fire cape in Mor Ul Rek", "Equip a Fire Cape or Infernal Cape in Mor Ul Rek.")
			.item(DiaryItem.owned("Fire cape or infernal cape", "Beat the TzHaar Fight Cave (all 63 waves) for a fire cape.",
				ItemID.INFERNAL_CAPE, ItemID.TZHAAR_CAPE_FIRE))
			.stage(volcanoDown())
			.stage(intoTzhaar())
			.stage(DiaryStage.builder()
				.title("Go into Mor Ul Rek and wear the cape")
				.inside(TZHAAR)
				.npc("TzHaar-Ket")
				.use(ItemID.TZHAAR_CAPE_FIRE).use(ItemID.INFERNAL_CAPE)
				.line("1. Show your fire cape to a guard (TzHaar-Ket, outlined) at the south of the city to get into Mor Ul Rek.")
				.line("2. Inside, take the cape off and wear it again (click it, outlined).")
				.build())
			.build());
	}

	private static DiaryTask.DiaryTaskBuilder task(DiaryTier tier, int number, int varbit, String name, String text)
	{
		return DiaryTask.builder()
			.region(DiaryRegion.KARAMJA)
			.tier(tier)
			.number(number)
			.name(name)
			.text(text)
			.varbit(varbit);
	}

	/**
	 * Elite tasks are bits 1 to 5 of one varp, in wiki order.
	 */
	private static DiaryTask.DiaryTaskBuilder elite(DiaryTier tier, int number, String name, String text)
	{
		return DiaryTask.builder()
			.region(DiaryRegion.KARAMJA)
			.tier(tier)
			.number(number)
			.name(name)
			.text(text)
			.varp(VarPlayerID.ATJUN_TASKS_4)
			.bit(number);
	}
}
