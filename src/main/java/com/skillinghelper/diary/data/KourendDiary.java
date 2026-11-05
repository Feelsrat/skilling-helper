package com.skillinghelper.diary.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.ToolType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;

/**
 * Kourend &amp; Kebos diary, every tier. Task wording and requirements from the OSRS Wiki, listed in walking order.
 */
final class KourendDiary
{
	/**
	 * Done bit per wiki task number across both Kourend diary varps (32 on is the second).
	 */
	private static final Map<DiaryTier, int[]> BITS = Map.of(
		DiaryTier.EASY, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12},
		DiaryTier.MEDIUM, new int[]{25, 13, 14, 15, 21, 16, 17, 18, 19, 22, 20, 23, 24},
		DiaryTier.HARD, new int[]{26, 27, 28, 29, 31, 30, 32, 33, 34, 35},
		DiaryTier.ELITE, new int[]{36, 37, 38, 39, 40, 41, 42, 43});

	private static final Location HOSIDIUS = Location.of("Hosidius", 1761, 3600, 0, 8);
	private static final Location PISCARILIUS = Location.of("Port Piscarilius", 1803, 3740, 0, 8);
	private static final Location WOODCUTTING_GUILD = Location.of("Woodcutting Guild entrance", 1657, 3505, 0, 2);
	private static final Location KARUULM_ELEVATOR = Location.of("Mount Karuulm dungeon elevator", 1311, 3807, 0, 2);
	private static final Zone KARUULM_DUNGEON = Zone.anyFloor(1240, 10140, 1390, 10290);
	private static final Location FARMING_GUILD = Location.of("Farming Guild door", 1249, 3725, 0, 2);
	private static final Location DARK_ALTAR = Location.of("Dark altar, Arceuus", 1716, 3883, 0, 3);
	private static final Location ARCEUUS_RUNESTONE = Location.of("Dense runestone, Arceuus", 1764, 3858, 0, 4);

	private KourendDiary()
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

	private static DiaryStage simple(String title, Location at, String npcOrNull, String objectOrNull, String... lines)
	{
		DiaryStage.DiaryStageBuilder b = DiaryStage.builder().title(title).at(at);
		if (npcOrNull != null)
		{
			b.npc(npcOrNull);
		}
		if (objectOrNull != null)
		{
			b.object(objectOrNull);
		}
		for (String line : lines)
		{
			b.line(line);
		}
		return b.build();
	}

	private static void easy(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.EASY;
		out.add(task(t, 6, "Boat to Land's End", "Take a boat to Land's End.")
			.stage(simple("Sail to Land's End", Location.of("Veos, Port Sarim docks", 3054, 3245, 0, 3), "Veos", null,
				"1. Talk to Veos (outlined) on the Port Sarim docks and travel to Land's End.",
				"Already in Kourend? Cabin Boy Herbert at Port Piscarilius sails there too."))
			.build());
		out.add(task(t, 2, "Sand crab", "Kill a Sand Crab.")
			.stage(simple("Kill a sand crab", Location.of("Sand crabs, Hosidius beach", 1739, 3468, 0, 6), "Sand crab", "Sandy rocks",
				"1. Stand next to the Sandy rocks (outlined) on the beach south of Hosidius; they wake up as Sand crabs.",
				"2. Kill one."))
			.build());
		out.add(task(t, 8, "Dig saltpetre", "Dig some saltpetre.")
			.item(DiaryItem.of("Spade", ItemID.SPADE))
			.stage(simple("Dig saltpetre in Hosidius", Location.of("Saltpetre, Hosidius", 1703, 3526, 0, 4), null, "Saltpetre",
				"1. Use the Spade on a Saltpetre deposit (outlined) west of the Hosidius farms."))
			.build());
		out.add(task(t, 9, "House in Hosidius", "Move your player-owned house portal to a different location using the Hosidius House Portal.")
			.skill(Skill.CONSTRUCTION, 25).unboostable(Skill.CONSTRUCTION)
			.item(DiaryItem.of("Coins (move fee)", 8_750, ItemID.COINS))
			.stage(simple("Move your house to Hosidius", Location.of("Estate agent, Hosidius", 1779, 3625, 0, 3), "Estate agent", null,
				"1. Talk to the Estate agent (outlined) in Hosidius and move your house to Hosidius for 8,750 coins."))
			.stage(simple("Move it with the Hosidius portal", Location.of("House portal, Hosidius", 1742, 3517, 0, 3), null, "Portal",
				"1. Now right-click the House portal (outlined) south of Hosidius to move it somewhere else.",
				"If the wiki step is already ticked, ignore this one."))
			.build());
		out.add(task(t, 4, "Hosidius fruit stall", "Steal from a Hosidius Food Stall.")
			.skill(Skill.THIEVING, 25)
			.stage(simple("Steal from the fruit stall", Location.of("Fruit stall, Hosidius", 1767, 3597, 0, 2), null, "Fruit Stall",
				"1. Right-click a Fruit stall (outlined) in the Hosidius market and choose Steal-from."))
			.build());
		out.add(task(t, 5, "Warrens General Store", "Browse the Warrens General Store.")
			.questStarted(Quest.THE_QUEEN_OF_THIEVES)
			.stage(simple("Go down the manhole in Piscarilius", Location.of("Manhole, Port Piscarilius", 1813, 3745, 0, 2), null, "Manhole",
				"1. Open and climb down the Manhole (outlined) in Port Piscarilius."))
			.stage(DiaryStage.builder()
				.title("Browse the general store")
				.inside(Zone.of(1730, 10110, 1810, 10180, 0))
				.at(Location.of("Warrens General Store", 1775, 10148, 0, 3))
				.npc("Shop keeper")
				.line("1. Right-click the Shop keeper (outlined) in the Warrens and choose Trade.")
				.build())
			.build());
		out.add(task(t, 10, "Shayzien agility course", "Complete a lap of the Shayzien Agility Course.")
			.stage(simple("Run a lap of the Shayzien course", Location.of("Shayzien agility course start", 1554, 3631, 0, 3), null,
				"Ladder", "1. Climb the first obstacle (outlined) south-east of Shayzien and finish each obstacle to the end.",
				"The basic route needs no Agility level."))
			.build());
		out.add(task(t, 7, "Kourend Castle altar", "Pray at the altar in Kourend Castle.")
			.stage(simple("Climb Kourend Castle", Location.of("Kourend Castle stairs", 1616, 3680, 0, 2), null, "Staircase",
				"1. Climb the Staircase (outlined) inside Kourend Castle to the top floor."))
			.stage(DiaryStage.builder()
				.title("Pray at the castle altar")
				.inside(Zone.of(1592, 3655, 1627, 3691, 1))
				.at(Location.of("Kourend Castle stairs, 1st floor", 1616, 3687, 1, 2))
				.object("Staircase")
				.line("1. Keep climbing the Staircase (outlined) to the 2nd floor.")
				.build())
			.stage(DiaryStage.builder()
				.title("Pray at the castle altar")
				.inside(Zone.of(1592, 3655, 1627, 3691, 2))
				.at(Location.of("Altar, Kourend Castle", 1617, 3673, 2, 2))
				.object("Altar")
				.line("1. Click Pray-at on the Altar (outlined).")
				.build())
			.build());
		out.add(task(t, 3, "Arceuus library book", "Fetch a book for a customer in the Arceuus Library.")
			.stage(simple("Find a book for a library customer", Location.of("Arceuus Library", 1625, 3801, 0, 6), "Sam", null,
				"1. Talk to a customer in the Arceuus Library (Sam, Professor Gracklebone or Villia) and ask what they need.",
				"2. Search the bookcases until you find it (Tips from Biblia shows where), then hand it over."))
			.build());
		out.add(task(t, 11, "Strength potion in the Lovakengj pub", "Create a Strength potion in the Lovakengj Pub.")
			.quest(Quest.DRUIDIC_RITUAL)
			.skill(Skill.HERBLORE, 12)
			.item(DiaryItem.of("Tarromin potion (unf)", ItemID.TARROMINVIAL))
			.item(DiaryItem.of("Limpwurt root", ItemID.LIMPWURT_ROOT))
			.stage(simple("Walk to the Lovakengj pub", Location.of("Deeper Lode pub, Lovakengj", 1565, 3758, 0, 3), null, null,
				"1. Walk into the Deeper Lode pub in south Lovakengj."))
			.stage(DiaryStage.builder()
				.title("Mix the strength potion here")
				.inside(Zone.of(1562, 3752, 1569, 3765, 0))
				.use(ItemID.LIMPWURT_ROOT).use(ItemID.TARROMINVIAL)
				.line("1. Use the Limpwurt root on the Tarromin potion (unf) (both outlined) inside the pub.")
				.build())
			.build());
		out.add(task(t, 1, "Iron at Mount Karuulm", "Mine some iron ore at the Mount Karuulm mine.")
			.skill(Skill.MINING, 15)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Mine iron at Mount Karuulm", Location.of("Iron rocks, Mount Karuulm", 1275, 3817, 0, 3), null, "Iron rocks",
				"1. Mine the Iron rocks (outlined) at the foot of Mount Karuulm. Fairy ring CIR lands next to it."))
			.build());
		out.add(task(t, 12, "Trout in the River Molch", "Catch a Trout in the River Molch.")
			.skill(Skill.FISHING, 20)
			.item(DiaryItem.of("Fly fishing rod", ItemID.FLY_FISHING_ROD))
			.item(DiaryItem.of("Feathers", 10, ItemID.FEATHER))
			.stage(simple("Fly fish a trout in the Molch", Location.of("Fishing spot, River Molch", 1267, 3706, 0, 4), "Rod Fishing spot",
				null, "1. Click Lure on a Rod Fishing spot (outlined) in the River Molch until you catch a Trout."))
			.build());
	}

	private static void medium(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.MEDIUM;
		out.add(task(t, 8, "Intelligence to Captain Ginea", "Deliver an intelligence report to Captain Ginea.")
			.stage(simple("Deliver a report to Captain Ginea", Location.of("Captain Ginea, Shayzien", 1504, 3632, 0, 3), "Captain Ginea",
				null, "1. Kill lizardmen near Shayzien until one drops an Intelligence report, or take one from the Shayzien camp.",
				"2. Give it to Captain Ginea (outlined) in Shayzien."))
			.build());
		out.add(task(t, 2, "Lizardman", "Kill a Lizardman.")
			.stage(simple("Kill a lizardman", Location.of("Lizardman Canyon, west of Shayzien", 1507, 3683, 0, 6), "Lizardman", null,
				"1. Kill a Lizardman (outlined) north-west of Shayzien. They poison, so bring antipoison or food."))
			.build());
		out.add(task(t, 7, "Piscarilius crane", "Repair a Piscarilius crane.")
			.skill(Skill.CRAFTING, 30).skill(Skill.CONSTRUCTION, 30)
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.item(DiaryItem.of("Plank", 3, ItemID.WOODPLANK))
			.item(DiaryItem.of("Steel nails", 50, ItemID.NAILS))
			.stage(simple("Fix a crane in Port Piscarilius", Location.of("Cranes, Port Piscarilius", 1830, 3735, 0, 3), null,
				"Broken crane", "1. Click Repair on a broken crane (outlined) on the Piscarilius docks."))
			.build());
		out.add(task(t, 11, "Wintertodt", "Subdue the Wintertodt.")
			.skill(Skill.FIREMAKING, 50)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DiaryItem.of("Tinderbox", ItemID.TINDERBOX))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.stage(simple("Go in to the Wintertodt", Location.of("Wintertodt camp doors", 1631, 3962, 0, 2), null, "Doors of Dinh",
				"1. Walk north from the Wintertodt camp through the Doors of Dinh (outlined)."))
			.stage(DiaryStage.builder()
				.title("Help subdue the Wintertodt")
				.inside(Zone.of(1600, 3968, 1660, 4030, 0))
				.object("Burning brazier").npc("Incapacitated Pyromancer")
				.line("1. Chop bruma roots, fletch them with a knife or just burn them in a Brazier (outlined).")
				.line("2. Earn at least 500 points before the Wintertodt's energy hits 0.")
				.build())
			.build());
		out.add(task(t, 10, "Boulder leap", "Use the boulder leap in the Arceuus essence mine.")
			.skill(Skill.AGILITY, 49)
			.stage(simple("Leap the boulder", Location.of("Boulder, Arceuus essence mine", 1776, 3883, 0, 2), null, "Boulder",
				"1. Click Climb on the Boulder (outlined) in the north-east of the Arceuus essence mine."))
			.build());
		out.add(task(t, 6, "Tyss spellbook", "Use the Necromancy spellbook in Arceuus, or switch to it at Tyss.")
			.stage(simple("Switch spellbooks at Tyss", Location.of("Tyss, Dark altar", 1712, 3882, 0, 3), "Tyss", null,
				"1. Talk to Tyss (outlined) by the Dark Altar and ask to switch to the Arceuus spellbook."))
			.build());
		out.add(task(t, 4, "Volcanic sulphur", "Mine some volcanic sulphur.")
			.skill(Skill.MINING, 42)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.item(DiaryItem.owned("Face mask", "Any face mask or gas mask, or a slayer helmet.", ItemID.SLAYER_FACEMASK,
				ItemID.GASMASK))
			.stage(simple("Mine volcanic sulphur in Lovakengj", Location.of("Sulphur, Lovakengj", 1444, 3860, 0, 4), null,
				"Volcanic sulphur", "1. Wear the face mask and mine the Volcanic sulphur (outlined) in north-west Lovakengj."))
			.build());
		out.add(task(t, 12, "Chinchompa in Kourend Woodland", "Catch a Chinchompa in the Kourend Woodland.")
			.questStarted(Quest.EAGLES_PEAK)
			.skill(Skill.HUNTER, 53)
			.item(DiaryItem.of("Box trap", ItemID.HUNTING_BOX_TRAP))
			.stage(simple("Trap a chinchompa", Location.of("Chinchompas, Kourend Woodland", 1555, 3440, 0, 6), "Chinchompa", null,
				"1. Lay a Box trap near the grey Chinchompas in the woodland south of Shayzien, wait, then check it."))
			.build());
		out.add(task(t, 9, "Bluegill on Molch Island", "Catch a Bluegill on Molch Island.")
			.skill(Skill.FISHING, 43).skill(Skill.HUNTER, 35)
			.item(DiaryItem.of("King worm or fish chunks", ItemID.KING_WORM))
			.stage(simple("Row to Molch Island", Location.of("Boat to Molch Island, Lake Molch", 1405, 3611, 0, 3), null, "Boat",
				"1. Take the boat (outlined) at Lake Molch to Molch Island."))
			.stage(simple("Catch a bluegill with the cormorant", Location.of("Aerial fishing spots, Molch Island", 1379, 3627, 0, 4), "Alry the Angler",
				null, "1. Talk to Alry the Angler (outlined) to borrow a cormorant and glove.",
				"2. With bait in your pack, click Catch on a shimmering spot on the lake until you get a Bluegill."))
			.build());
		out.add(task(t, 13, "Mahogany north of the Farming Guild", "Chop some Mahogany logs north of the Farming Guild.")
			.skill(Skill.WOODCUTTING, 50)
			.item(DiaryItem.tool(ToolType.AXE))
			.stage(simple("Chop a mahogany tree", Location.of("Mahogany trees, north of the Farming Guild", 1238, 3771, 0, 4), null,
				"Mahogany tree", "1. Chop a Mahogany tree (outlined) north of the Farming Guild until you get a log."))
			.build());
		out.add(task(t, 5, "Farming Guild", "Enter the Farming Guild.")
			.skill(Skill.FARMING, 45)
			.stage(simple("Walk into the Farming Guild", FARMING_GUILD, null, "Door",
				"1. Open the Door (outlined) on the south side of the Farming Guild."))
			.build());
		out.add(task(t, 1, "Fairy ring to Mount Karuulm", "Travel to the fairy ring south of Mount Karuulm.")
			.questStarted(Quest.FAIRYTALE_II__CURE_A_QUEEN)
			.item(DiaryStages.FAIRY_STAFF)
			.stages(DiaryStages.fairyRing("CIR", "You land south of Mount Karuulm, which finishes it."))
			.build());
		out.add(task(t, 3, "Kharedst's memoirs", "Use Kharedst's memoirs to teleport to all five cities in Great Kourend.")
			.quest(Quest.THE_QUEEN_OF_THIEVES).quest(Quest.THE_DEPTHS_OF_DESPAIR).quest(Quest.TALE_OF_THE_RIGHTEOUS)
			.quest(Quest.THE_FORSAKEN_TOWER).quest(Quest.THE_ASCENT_OF_ARCEUUS)
			.item(DiaryItem.owned("Kharedst's memoirs or the Book of the dead",
				"Veos at Port Sarim gives the memoirs after Client of Kourend; add each city's page after its quest.",
				ItemID.VEOS_KHAREDSTS_MEMOIRS, ItemID.BOOK_OF_THE_DEAD))
			.stage(DiaryStage.builder()
				.title("Teleport to each of the five cities")
				.line("1. Rub the memoirs and teleport to Hosidius, Port Piscarilius, Shayzien, Lovakengj and Arceuus in turn.")
				.line("It needs a charge per teleport; recharge it with a law, body, mind and soul rune each.")
				.build())
			.build());
	}

	private static void hard(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.HARD;
		out.add(task(t, 1, "Woodcutting Guild", "Enter the Woodcutting Guild.")
			.skill(Skill.WOODCUTTING, 60)
			.stage(simple("Walk into the Woodcutting Guild", WOODCUTTING_GUILD, null, "Gate",
				"1. Walk through the gate (outlined) south of Hosidius into the Woodcutting Guild."))
			.build());
		out.add(task(t, 5, "Logavano at Tithe Farm", "Plant some Logavano seeds at the Tithe Farm.")
			.skill(Skill.FARMING, 74)
			.item(DiaryItem.of("Seed dibber", ItemID.DIBBER))
			.stage(simple("Plant logavano at the Tithe Farm", Location.of("Seed table, Tithe Farm", 1802, 3503, 0, 3), null, "Seed table",
				"1. Take Logavano seeds from the Seed table (outlined) at the Tithe Farm east of Hosidius.",
				"2. Go in and plant one in a patch with the dibber."))
			.build());
		out.add(task(t, 8, "Artefact to Captain Khaled", "Deliver an artefact to Captain Khaled.")
			.skill(Skill.THIEVING, 49)
			.item(DiaryItem.of("Lockpick", ItemID.LOCKPICK))
			.stage(simple("Get a job from Captain Khaled", Location.of("Captain Khaled, Port Piscarilius", 1845, 3753, 0, 3),
				"Captain Khaled", null,
				"1. Talk to Captain Khaled (outlined) in Port Piscarilius and take a stealing job.",
				"2. Pick the house's door, steal the artefact and bring it back to him."))
			.build());
		out.add(task(t, 6, "Zombie in the Shayzien crypts", "Kill a Zombie in the Shayzien Crypts.")
			.item(DiaryStages.LIGHT_SOURCE)
			.stage(simple("Go into the Shayzien crypts", Location.of("Crypt entrance, Shayzien", 1483, 3548, 0, 2), null, "Tomb",
				"1. Enter the tomb (outlined) south of Shayzien. Bring a light source."))
			.stage(DiaryStage.builder()
				.title("Kill a zombie")
				.inside(Zone.of(1473, 9928, 1516, 9974, 3))
				.npc("Zombie")
				.line("1. Kill a Zombie (outlined) in the crypts.")
				.build())
			.build());
		out.add(task(t, 3, "Lizardman shaman", "Kill a Lizardman Shaman in Molch.")
			.stage(simple("Go into the Molch temple", Location.of("Lizardman temple, Molch", 1312, 3686, 0, 3), null, "Lizard dwelling",
				"1. Go into the lizardman temple (outlined) in Molch. Bring antipoison and ranged or melee gear."))
			.stage(DiaryStage.builder()
				.title("Kill a lizardman shaman")
				.inside(Zone.of(1289, 10067, 1333, 10100, 0))
				.npc("Lizardman shaman")
				.line("1. Kill a Lizardman shaman (outlined). Step away from its jumps and green spit.")
				.build())
			.build());
		out.add(task(t, 10, "Monster Examine a troll", "Cast Monster Examine on a Troll south of Mount Quidamortem.")
			.quest(Quest.DREAM_MENTOR)
			.skill(Skill.MAGIC, 66)
			.note("Lunar spellbook selected")
			.item(DiaryItem.of("Astral rune", 1, ItemID.ASTRALRUNE))
			.item(DiaryItem.of("Cosmic rune", 1, ItemID.COSMICRUNE))
			.item(DiaryItem.of("Mind rune", 1, ItemID.MINDRUNE))
			.stage(simple("Examine a troll", Location.of("Trolls, south of Mount Quidamortem", 1240, 3515, 0, 6), "Mountain troll", null,
				"1. Cast Monster Examine on any troll (outlined) south of Mount Quidamortem."))
			.build());
		out.add(task(t, 7, "Xeric's Heart", "Teleport to Xeric's Heart using Xeric's Talisman.")
			.note("Architectural Alliance miniquest done (unlocks the Heart teleport)")
			.item(DiaryItem.owned("Xeric's talisman", "Lizardmen drop the inert talisman; charge it with lizardman fangs.",
				ItemID.XERIC_TALISMAN))
			.stage(DiaryStage.builder()
				.title("Teleport to Xeric's Heart")
				.line("1. Rub Xeric's talisman and pick Xeric's Heart.")
				.build())
			.build());
		out.add(task(t, 4, "Lovakite", "Mine some Lovakite.")
			.skill(Skill.MINING, 65)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Mine lovakite", Location.of("Lovakite rocks, Lovakengj", 1438, 3816, 0, 4), null, "Lovakite rocks",
				"1. Mine the Lovakite rocks (outlined) in the Lovakengj mine."))
			.build());
		out.add(task(t, 2, "Forsaken Tower adamantite bar", "Smelt an Adamantite bar in The Forsaken Tower.")
			.questStarted(Quest.THE_FORSAKEN_TOWER)
			.skill(Skill.SMITHING, 70)
			.item(DiaryItem.of("Adamantite ore", ItemID.ADAMANTITE_ORE))
			.item(DiaryItem.of("Coal", 6, ItemID.COAL))
			.stage(simple("Walk into the Forsaken Tower", Location.of("The Forsaken Tower", 1382, 3822, 0, 3), null, "Door",
				"1. Walk into The Forsaken Tower west of Lovakengj."))
			.stage(DiaryStage.builder()
				.title("Smelt the adamantite bar")
				.inside(Zone.of(1377, 3817, 1388, 3829, 0))
				.object("Furnace")
				.use(ItemID.ADAMANTITE_ORE)
				.line("1. Use the Adamantite ore on the Furnace (outlined) in the tower.")
				.build())
			.build());
		out.add(task(t, 9, "Wyrm in the Karuulm dungeon", "Kill a Wyrm in the Karuulm Slayer Dungeon.")
			.skill(Skill.SLAYER, 62)
			.item(DiaryItem.owned("Boots of stone", "Buy them from the Grand Exchange or make them; any of brimstone or granite boots work.",
				ItemID.BOOTS_OF_STONE))
			.stage(simple("Take the Karuulm elevator down", KARUULM_ELEVATOR, null, "Elevator",
				"1. Wear the boots and take the Elevator (outlined) inside Mount Karuulm."))
			.stage(DiaryStage.builder()
				.title("Kill a wyrm")
				.inside(KARUULM_DUNGEON)
				.at(Location.of("Wyrms, Karuulm dungeon", 1282, 10189, 0, 6))
				.npc("Wyrm")
				.line("1. Kill a Wyrm (outlined). They hit with magic, so wear something to help with that.")
				.build())
			.build());
	}

	private static void elite(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.ELITE;
		out.add(task(t, 2, "Redwood", "Chop some Redwood logs.")
			.skill(Skill.WOODCUTTING, 90)
			.item(DiaryItem.tool(ToolType.AXE))
			.stage(simple("Walk into the Woodcutting Guild", WOODCUTTING_GUILD, null, "Gate",
				"1. Walk through the gate (outlined) into the Woodcutting Guild."))
			.stage(DiaryStage.builder()
				.title("Chop a redwood")
				.inside(Zone.anyFloor(1560, 3470, 1660, 3510))
				.at(Location.of("Redwood trees, Woodcutting Guild", 1569, 3487, 1, 4))
				.object("Redwood")
				.line("1. Climb the rope ladders in the west of the guild and chop a Redwood (outlined) until you get a log.")
				.build())
			.build());
		out.add(task(t, 4, "Anglerfish", "Catch an Anglerfish and cook it whilst in Great Kourend.")
			.skill(Skill.FISHING, 82).skill(Skill.COOKING, 84)
			.item(DiaryItem.of("Fishing rod", ItemID.FISHING_ROD))
			.item(DiaryItem.of("Sandworms", 10, ItemID.PISCARILIUS_SANDWORMS))
			.stage(simple("Catch an anglerfish", Location.of("Anglerfish spots, Port Piscarilius", 1832, 3772, 0, 4), "Rod Fishing spot", null,
				"1. Click Bait on a Rod Fishing spot (outlined) north-east of Port Piscarilius until you catch a Raw anglerfish."))
			.stage(DiaryStage.builder()
				.title("Cook the anglerfish in Kourend")
				.carrying(ItemID.RAW_ANGLERFISH)
				.at(Location.of("Range, Port Piscarilius", 1795, 3754, 0, 3))
				.object("Range")
				.use(ItemID.RAW_ANGLERFISH)
				.line("1. Use the Raw anglerfish on the Range (outlined) in Port Piscarilius. If it burns, catch another.")
				.build())
			.build());
		out.add(task(t, 8, "Battlestaff in the Farming Guild", "Create your own Battlestaff from scratch within the Farming Guild.")
			.skill(Skill.FARMING, 85).skill(Skill.FLETCHING, 40)
			.item(DiaryItem.of("Celastrus sapling", ItemID.PLANTPOT_CELASTRUS_TREE_SAPLING))
			.item(DiaryItem.of("Spade", ItemID.SPADE))
			.item(DiaryItem.of("Rake", ItemID.RAKE))
			.item(DiaryItem.of("Knife", ItemID.KNIFE))
			.item(DiaryItem.tool(ToolType.AXE))
			.stage(simple("Walk into the Farming Guild", FARMING_GUILD, null, "Door",
				"1. Open the Door (outlined) of the Farming Guild."))
			.stage(DiaryStage.builder()
				.title("Grow and cut a battlestaff")
				.inside(Zone.anyFloor(1220, 3715, 1275, 3770))
				.at(Location.of("Celastrus patch, Farming Guild", 1244, 3750, 0, 2))
				.object("Celastrus patch")
				.line("1. Plant the Celastrus sapling in the patch (outlined) in the guild's top tier and let it grow (about 13 hours).")
				.line("2. Pick a Battlestaff from it, then use the Knife on it there. Every step must be done inside the guild.")
				.build())
			.build());
		out.add(task(t, 6, "Ape Atoll teleport tablet", "Create an Ape Atoll teleport tablet.")
			.skill(Skill.MAGIC, 90)
			.note("Arceuus spellbook")
			.item(DiaryItem.owned("Dark essence block", "Chisel dark essence from a block made at the dark altar.",
				ItemID.ARCEUUS_ESSENCE_BLOCK_DARK))
			.item(DiaryItem.of("Soul rune", 2, ItemID.SOULRUNE))
			.item(DiaryItem.of("Blood rune", 2, ItemID.BLOODRUNE))
			.item(DiaryItem.of("Law rune", 2, ItemID.LAWRUNE))
			.stage(simple("Make the tablet at a lectern", Location.of("Lectern, Arceuus Library", 1679, 3765, 0, 3), null, "Lectern",
				"1. On the Arceuus spellbook, click Study on a Lectern (outlined) in Arceuus and make an Ape Atoll teleport."))
			.build());
		out.add(task(t, 1, "Blood runes from dark essence", "Craft one or more Blood runes from Dark essence.")
			.skill(Skill.RUNECRAFT, 77).skill(Skill.MINING, 38).skill(Skill.CRAFTING, 38)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.item(DiaryItem.of("Chisel", ItemID.CHISEL))
			.stage(simple("Mine a dense essence block", ARCEUUS_RUNESTONE, null, "Dense runestone",
				"1. Mine the Dense runestone (outlined) until you have a few blocks."))
			.stage(DiaryStage.builder()
				.title("Darken the blocks at the dark altar")
				.carrying(ItemID.ARCEUUS_ESSENCE_BLOCK)
				.at(DARK_ALTAR)
				.object("Dark Altar")
				.line("1. Click Venerate on the Dark Altar (outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Chisel them and craft at the blood altar")
				.carrying(ItemID.ARCEUUS_ESSENCE_BLOCK_DARK)
				.at(Location.of("Blood altar, Arceuus", 1718, 3828, 0, 3))
				.object("Blood Altar")
				.use(ItemID.CHISEL).use(ItemID.ARCEUUS_ESSENCE_BLOCK_DARK)
				.line("1. Use the Chisel on the Dark essence blocks to get fragments.")
				.line("2. Walk to the Blood Altar (outlined) south of the mine and click Bind.")
				.build())
			.build());
		out.add(task(t, 5, "Alchemical Hydra", "Kill a Hydra in the Karuulm Slayer Dungeon.")
			.skill(Skill.SLAYER, 95)
			.item(DiaryItem.owned("Boots of stone", "Any of boots of stone, brimstone or granite boots.", ItemID.BOOTS_OF_STONE))
			.stage(simple("Take the Karuulm elevator down", KARUULM_ELEVATOR, null, "Elevator",
				"1. Wear the boots and take the Elevator (outlined) inside Mount Karuulm."))
			.stage(DiaryStage.builder()
				.title("Kill a hydra")
				.inside(KARUULM_DUNGEON)
				.at(Location.of("Hydras, Karuulm dungeon", 1312, 10232, 0, 6))
				.npc("Hydra")
				.line("1. On a hydra slayer task, kill a Hydra (outlined). The normal hydras count, not only the boss.")
				.build())
			.build());
		out.add(task(t, 3, "Skotizo", "Kill Skotizo in the Catacombs of Kourend.")
			.item(DiaryItem.owned("Dark totem", "Join the three totem pieces dropped in the Catacombs of Kourend.", ItemID.CATA_TOTEM))
			.stage(simple("Go into the Catacombs of Kourend", Location.of("Catacombs entrance, Kourend Castle statue", 1636, 3673, 0, 2),
				null, "Statue", "1. Investigate the statue (outlined) in the middle of Kourend and climb down."))
			.stage(DiaryStage.builder()
				.title("Use the totem and kill Skotizo")
				.inside(Zone.anyFloor(1590, 9980, 1730, 10115))
				.at(Location.of("Altar, Catacombs centre", 1665, 10050, 0, 3))
				.object("Altar")
				.npc("Skotizo")
				.line("1. Use the Dark totem on the Altar (outlined) in the middle of the catacombs.")
				.line("2. Kill Skotizo. Destroy the Awakened altars to stop him healing. Demonbane weapons help.")
				.build())
			.build());
		out.add(task(t, 7, "Chambers of Xeric", "Complete a raid in the Chambers of Xeric.")
			.note("Raid gear; the wiki and a group are the best way to learn it")
			.stage(simple("Start a raid at Mount Quidamortem", Location.of("Chambers of Xeric entrance, Mount Quidamortem", 1232, 3573, 0, 3),
				null, "Chambers of Xeric", "1. Enter the Chambers of Xeric (outlined) at Mount Quidamortem.",
				"2. Finish the raid; any party size counts."))
			.build());
	}

	private static DiaryTask.DiaryTaskBuilder task(DiaryTier tier, int number, String name, String text)
	{
		int index = BITS.get(tier)[number - 1];
		return DiaryTask.builder()
			.region(DiaryRegion.KOUREND)
			.tier(tier)
			.number(number)
			.name(name)
			.text(text)
			.varp(index < 32 ? VarPlayerID.KOUREND_ACHIEVEMENT_DIARY : VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2)
			.bit(index % 32);
	}
}
