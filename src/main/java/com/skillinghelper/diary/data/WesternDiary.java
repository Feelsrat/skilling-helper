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
 * Western Provinces diary, every tier. Task wording and requirements from the OSRS Wiki, listed in walking order.
 */
final class WesternDiary
{
	/**
	 * Done bit per wiki task number across both Western diary varps (32 on is the second). Bits 42 and 43 are unused.
	 */
	private static final Map<DiaryTier, int[]> BITS = Map.of(
		DiaryTier.EASY, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
		DiaryTier.MEDIUM, new int[]{12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24},
		DiaryTier.HARD, new int[]{25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37},
		DiaryTier.ELITE, new int[]{38, 39, 40, 41, 44, 45, 46});

	private static final Location GRAND_TREE = Location.of("Grand Tree, Gnome Stronghold", 2465, 3495, 0, 6);
	private static final Location PEST_CONTROL = Location.of("Squire, Port Sarim docks", 3041, 3202, 0, 3);
	private static final Location PEST_LANDERS = Location.of("Pest Control landers, Void Knights' Outpost", 2657, 2639, 0, 6);
	private static final Location RANTZ = Location.of("Rantz, Feldip Hills", 2631, 2981, 0, 4);
	private static final Location PISCATORIS = Location.of("Piscatoris Fishing Colony", 2340, 3690, 0, 6);
	private static final Location LLETYA = Location.of("Lletya", 2330, 3170, 0, 8);

	private WesternDiary()
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

	private static DiaryTask.DiaryTaskBuilder chompyHat(DiaryTier tier, int number, int kills)
	{
		return task(tier, number, "Chompy hat (" + kills + " kills)",
			"Claim a Chompy bird hat from Rantz after registering at least " + kills + " kills.")
			.quest(Quest.BIG_CHOMPY_BIRD_HUNTING)
			.varReq(DiaryTask.VarReq.varp("chompy kills", VarPlayerID.CHOMPYBIRD, kills))
			.item(DiaryItem.owned("Ogre bow", "Made during Big Chompy Bird Hunting; Zogre Flesh Eaters gives the composite bow.",
				ItemID.OGRE_BOW, ItemID.ZOGRE_BOW))
			.stage(simple("Claim the hat from Rantz", RANTZ, "Rantz", null,
				"1. With the ogre bow in your pack, talk to Rantz (outlined) in the Feldip Hills and ask about hats.",
				"Kill more chompies (bloat toads with the bellows, place them, shoot the bird) until you have " + kills + "."));
	}

	private static DiaryTask.DiaryTaskBuilder pestControl(DiaryTier tier, int number, String lander, int combat)
	{
		return task(tier, number, lander + " Pest Control", "Complete a " + lander.toLowerCase() + " game of Pest Control.")
			.combat(combat)
			.stage(simple("Sail to the Void Knights' Outpost", PEST_CONTROL, "Squire", null,
				"1. Talk to the Squire (outlined) on the Port Sarim docks, or use the Pest Control minigame teleport."))
			.stage(DiaryStage.builder()
				.title("Win a " + lander.toLowerCase() + " game")
				.inside(Zone.anyFloor(2620, 2560, 2690, 2690))
				.at(PEST_LANDERS)
				.object("Gangplank")
				.line("1. Cross the " + lander.toLowerCase() + " lander's Gangplank (outlined) and wait for the game.")
				.line("2. Help kill the portals and earn points; you must get commendation points for it to count.")
				.build());
	}

	private static void easy(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.EASY;
		out.add(task(t, 4, "Gnome agility course", "Complete a lap of the Gnome agility course.")
			.stage(simple("Run a lap of the gnome course", Location.of("Gnome agility course start", 2474, 3436, 0, 3), null,
				"Log balance", "1. Walk across the Log balance (outlined) and finish each obstacle to the end."))
			.build());
		out.add(task(t, 10, "Oak shortbow in the Stronghold", "Fletch an Oak Shortbow in the Gnome Stronghold.")
			.skill(Skill.FLETCHING, 20)
			.item(DiaryItem.of("Oak shortbow (u)", ItemID.UNSTRUNG_OAK_SHORTBOW))
			.item(DiaryItem.of("Bow string", ItemID.BOW_STRING))
			.stage(DiaryStage.builder()
				.title("String the bow in the Stronghold")
				.at(GRAND_TREE)
				.use(ItemID.BOW_STRING).use(ItemID.UNSTRUNG_OAK_SHORTBOW)
				.line("1. Inside the Gnome Stronghold, use the Bow string on the Oak shortbow (u) (both outlined).")
				.build())
			.build());
		out.add(task(t, 8, "Swamp toad", "Collect a swamp toad at the Gnome Stronghold.")
			.stage(simple("Pick up a swamp toad", Location.of("Swamp toad pen, north-west of the Grand Tree", 2420, 3509, 0, 4), null,
				"Swamp toad", "1. Pick up a Swamp toad in the swampy pen north-west of the Grand Tree."))
			.build());
		out.add(task(t, 5, "Gnomeball goal", "Score a goal in a Gnomeball match.")
			.stage(simple("Score a gnomeball goal", Location.of("Gnomeball field", 2394, 3487, 0, 6), "Gnome ball referee", null,
				"1. Talk to the referee by the Gnomeball field to get the ball.",
				"2. Run towards the goal and throw the ball in before the gnomes tackle you."))
			.build());
		out.add(task(t, 11, "Terrorbird", "Kill a Terrorbird in the Terrorbird enclosure.")
			.stage(simple("Kill a terrorbird", Location.of("Terrorbird enclosure, Gnome Stronghold", 2380, 3433, 0, 5), "Terrorbird", null,
				"1. Cross the river in the south-west of the Stronghold and kill a Terrorbird (outlined)."))
			.build());
		out.add(task(t, 9, "Brimstail essence teleport", "Have Brimstail teleport you to the Essence Mine.")
			.quest(Quest.RUNE_MYSTERIES)
			.stage(simple("Go into Brimstail's cave", Location.of("Brimstail's cave, Gnome Stronghold", 2403, 3419, 0, 2), null,
				"Cave entrance", "1. Enter the cave (outlined) south-west of the Gnome Stronghold."))
			.stage(DiaryStage.builder()
				.title("Ask Brimstail for the essence mine")
				.inside(Zone.anyFloor(2370, 9800, 2420, 9840))
				.npc("Brimstail")
				.line("1. Right-click Brimstail (outlined) and choose Teleport.")
				.build())
			.build());
		out.add(task(t, 3, "Piscatoris iron", "Mine some Iron Ore near Piscatoris.")
			.skill(Skill.MINING, 15)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Mine iron at Piscatoris", Location.of("Piscatoris mine", 2339, 3640, 0, 4), null, "Iron rocks",
				"1. Mine the Iron rocks (outlined) south of the Piscatoris Fishing Colony. Fairy ring AKQ lands nearby."))
			.build());
		out.add(task(t, 1, "Copper longtail", "Catch a Copper Longtail.")
			.skill(Skill.HUNTER, 9)
			.item(DiaryItem.of("Bird snare", ItemID.HUNTING_OJIBWAY_BIRD_SNARE))
			.stage(simple("Snare a copper longtail", Location.of("Piscatoris hunter area", 2335, 3585, 0, 6), "Copper longtail", null,
				"1. Lay the Bird snare in the Piscatoris hunter area and wait for a Copper longtail, then check it."))
			.build());
		out.add(pestControl(t, 2, "Novice", 40).build());
		out.add(task(t, 7, "Pest Control teleport", "Teleport to Pest Control using the Minigame Teleport.")
			.combat(40)
			.stage(DiaryStage.builder()
				.title("Use the Pest Control minigame teleport")
				.line("1. Open the Grouping tab (smiley face icon), pick Pest Control and click Teleport.")
				.line("It has a 20 minute cooldown.")
				.build())
			.build());
		out.add(chompyHat(t, 6, 30).build());
	}

	private static void medium(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.MEDIUM;
		out.add(task(t, 2, "Spirit tree to the Stronghold", "Travel to the Gnome Stronghold by Spirit Tree.")
			.quest(Quest.TREE_GNOME_VILLAGE)
			.stage(DiaryStage.builder()
				.title("Ride a spirit tree to the Stronghold")
				.object("Spirit tree")
				.line("1. Click Travel on any Spirit tree (Grand Exchange, Tree Gnome Village, Battlefield of Khazard) and pick the Gnome Stronghold.")
				.build())
			.build());
		out.add(task(t, 10, "Chocolate bomb", "Make a Chocolate Bomb at the Grand Tree.")
			.skill(Skill.COOKING, 42)
			.item(DiaryItem.of("Gnomebowl mould", ItemID.GNOMEBOWL_MOULD))
			.note("Gianne dough, 4 chocolate bars, an equa leaf, 2 pots of cream and chocolate dust (all sold at Grand Tree Groceries)")
			.stage(simple("Make a chocolate bomb at the Grand Tree", Location.of("Grand Tree Groceries", 2490, 3488, 1, 3), "Heckel Funch",
				null, "1. Buy the ingredients from Heckel Funch (outlined) on the Grand Tree's 1st floor.",
				"2. Shape the dough in the mould, bake it, add 4 chocolate bars and the leaf, bake again, then add the cream and dust."))
			.build());
		out.add(task(t, 11, "Gnome Restaurant delivery", "Complete a delivery for the Gnome Restaurant.")
			.skill(Skill.COOKING, 29)
			.stage(simple("Take a delivery job", Location.of("Aluft Gianne snr., Grand Tree", 2449, 3501, 1, 3), "Aluft Gianne snr.", null,
				"1. Finish the cooking tutorial with Aluft Gianne snr. (outlined) if you haven't, then ask him for an easy delivery.",
				"2. Cook the dish he names and hand it to the gnome he tells you about before time runs out."))
			.build());
		out.add(task(t, 13, "Gold under the Grand Tree", "Mine some Gold ore underneath the Grand Tree.")
			.quest(Quest.THE_GRAND_TREE)
			.skill(Skill.MINING, 40)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Go down under the Grand Tree", Location.of("Trapdoor, Grand Tree", 2464, 3497, 0, 2), null, "Trapdoor",
				"1. Climb down the Trapdoor (outlined) in the Grand Tree's ground floor."))
			.stage(DiaryStage.builder()
				.title("Mine gold")
				.inside(Zone.anyFloor(2440, 9880, 2500, 9930))
				.object("Gold rocks")
				.line("1. Mine the Gold rocks (outlined) in the mine.")
				.build())
			.build());
		out.add(task(t, 1, "Shortcut to Otto's Grotto", "Take the agility shortcut from the Grand Tree to Otto's Grotto.")
			.quest(Quest.TREE_GNOME_VILLAGE).quest(Quest.THE_GRAND_TREE)
			.skill(Skill.AGILITY, 37)
			.stage(simple("Take the shortcut to Otto's Grotto", Location.of("Agility shortcut, north of the Grand Tree", 2486, 3515, 0, 3),
				null, "Rocks", "1. Climb down the rocks (outlined) north-east of the Grand Tree towards Otto's Grotto."))
			.build());
		out.add(task(t, 12, "Crystal saw", "Turn your small crystal seed into a Crystal saw.")
			.quest(Quest.THE_EYES_OF_GLOUPHRIE)
			.item(DiaryItem.owned("Crystal saw seed", "Given during The Eyes of Glouphrie.", ItemID.CRYSTAL_SEED_OLD_SMALL))
			.stage(simple("Go into Brimstail's cave", Location.of("Brimstail's cave, Gnome Stronghold", 2403, 3419, 0, 2), null,
				"Cave entrance", "1. Enter the cave (outlined) south-west of the Gnome Stronghold."))
			.stage(DiaryStage.builder()
				.title("Sing the seed into a saw")
				.inside(Zone.anyFloor(2370, 9800, 2420, 9840))
				.object("Singing bowl")
				.use(ItemID.CRYSTAL_SEED_OLD_SMALL)
				.line("1. Use the crystal saw seed on the Singing bowl (outlined).")
				.build())
			.build());
		out.add(task(t, 7, "Glider to the Feldip Hills", "Travel to the Feldip Hills by Gnome Glider.")
			.quest(Quest.THE_GRAND_TREE)
			.questStarted(Quest.ONE_SMALL_FAVOUR)
			.stage(simple("Fly to the Feldip Hills", Location.of("Captain Errdo, top of the Grand Tree", 2465, 3501, 3, 3), "Captain Errdo",
				null, "1. Talk to Captain Errdo (outlined) at the top of the Grand Tree, or any glider captain, and fly to Lemantolly Undri."))
			.build());
		out.add(task(t, 9, "Eagle to the Feldip Hills", "Travel from Eagles' Peak to the Feldip Hills by Eagle.")
			.quest(Quest.EAGLES_PEAK)
			.item(DiaryItem.of("Rope", ItemID.ROPE))
			.stage(simple("Go into the Eagles' Peak cave", Location.of("Eagles' Peak cave entrance", 2329, 3495, 0, 2), null,
				"Cave entrance", "1. Enter the cave (outlined) at the top of Eagles' Peak."))
			.stage(DiaryStage.builder()
				.title("Ride the jungle eagle")
				.inside(Zone.anyFloor(1986, 4944, 2030, 4985))
				.npc("Jungle eagle")
				.line("1. Use the Rope on the Jungle eagle (outlined) to fly to the Feldip Hills.")
				.build())
			.build());
		out.add(task(t, 3, "Spined larupia", "Trap a Spined Larupia.")
			.skill(Skill.HUNTER, 31)
			.item(DiaryItem.of("Teasing stick", ItemID.HUNTING_TEASING_STICK))
			.item(DiaryItem.of("Logs", ItemID.LOGS))
			.item(DiaryItem.of("Knife", ItemID.KNIFE))
			.stage(simple("Catch a larupia in a pit", Location.of("Feldip hunter area", 2551, 2908, 0, 6), "Spined larupia", "Pit",
				"1. Click Trap on a Pit (outlined) with the logs and knife.",
				"2. Tease a Spined larupia, then jump over the pit so it follows you in."))
			.build());
		out.add(pestControl(t, 6, "Intermediate", 70).build());
		out.add(chompyHat(t, 8, 125).build());
		out.add(task(t, 4, "Bass on Ape Atoll", "Fish some Bass on Ape Atoll.")
			.questStarted(Quest.MONKEY_MADNESS_I)
			.skill(Skill.FISHING, 46)
			.item(DiaryItem.of("Big fishing net", ItemID.BIG_NET))
			.stage(simple("Net a bass on Ape Atoll", Location.of("Fishing spots, south Ape Atoll", 2700, 2701, 0, 4), "Fishing spot", null,
				"1. Get to Ape Atoll (glider or Ape Atoll teleport) and use the Big net on a Fishing spot (outlined) until you get a Bass."))
			.build());
		out.add(task(t, 5, "Teak logs on Ape Atoll", "Chop and burn some teak logs on Ape Atoll.")
			.questStarted(Quest.MONKEY_MADNESS_I)
			.skill(Skill.WOODCUTTING, 35).skill(Skill.FIREMAKING, 35)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DiaryItem.of("Tinderbox", ItemID.TINDERBOX))
			.stage(simple("Chop a teak on Ape Atoll", Location.of("Teak trees, Ape Atoll", 2773, 2697, 0, 4), null, "Teak tree",
				"1. Chop a Teak tree (outlined) on Ape Atoll. Don't do another diary task before you burn it."))
			.stage(DiaryStage.builder()
				.title("Burn the teak logs here")
				.carrying(ItemID.TEAK_LOGS)
				.use(ItemID.TINDERBOX).use(ItemID.TEAK_LOGS)
				.line("1. Use the Tinderbox on the Teak logs (both outlined), still on Ape Atoll.")
				.build())
			.build());
	}

	private static void hard(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.HARD;
		out.add(task(t, 13, "Pickpocket a gnome", "Pickpocket a Gnome.")
			.skill(Skill.THIEVING, 75)
			.stage(simple("Pickpocket a gnome", GRAND_TREE, "Gnome", null,
				"1. Right-click any Gnome (outlined) in the Gnome Stronghold and choose Pickpocket."))
			.build());
		out.add(task(t, 4, "Dashing kebbit", "Catch a Dashing Kebbit.")
			.skill(Skill.HUNTER, 69)
			.item(DiaryItem.of("Coins (falconry fee)", 500, ItemID.COINS))
			.stage(simple("Hunt a dashing kebbit with a falcon", Location.of("Piscatoris falconry area", 2375, 3605, 0, 4), "Matthias",
				null, "1. Pay Matthias (outlined) 500 coins for a falcon, with no weapon or shield worn.",
				"2. Send the falcon at a Dashing kebbit and pick up the catch."))
			.build());
		out.add(task(t, 2, "Monkfish in Piscatoris", "Catch and cook a Monkfish in Piscatoris.")
			.quest(Quest.SWAN_SONG)
			.skill(Skill.FISHING, 62).skill(Skill.COOKING, 62)
			.item(DiaryItem.of("Small fishing net", ItemID.NET))
			.stage(simple("Net a monkfish at the colony", PISCATORIS, "Fishing spot", null,
				"1. Use the Small fishing net on a Fishing spot (outlined) at the Piscatoris Fishing Colony."))
			.stage(DiaryStage.builder()
				.title("Cook the monkfish in the colony")
				.carrying(ItemID.RAW_MONKFISH)
				.at(Location.of("Range, Piscatoris Fishing Colony", 2324, 3692, 0, 3))
				.object("Range")
				.use(ItemID.RAW_MONKFISH)
				.line("1. Use the Raw monkfish on the Range (outlined) in the colony. If it burns, catch another.")
				.build())
			.build());
		out.add(pestControl(t, 3, "Veteran", 100).build());
		out.add(task(t, 12, "Ape Atoll Teleport", "Teleport to Ape Atoll.")
			.quest(Quest.RECIPE_FOR_DISASTER__KING_AWOWOGEI)
			.skill(Skill.MAGIC, 64)
			.item(DiaryItem.of("Law rune", 2, ItemID.LAWRUNE))
			.item(DiaryItem.of("Fire rune", 2, ItemID.FIRERUNE))
			.item(DiaryItem.of("Water rune", 2, ItemID.WATERRUNE))
			.item(DiaryItem.of("Banana", 1, ItemID.BANANA))
			.stage(DiaryStage.builder()
				.title("Cast Ape Atoll Teleport")
				.line("1. Open the spellbook and click Ape Atoll Teleport.")
				.build())
			.build());
		out.add(task(t, 5, "Ape Atoll agility course", "Complete a lap of the Ape Atoll agility course.")
			.questStarted(Quest.MONKEY_MADNESS_I)
			.skill(Skill.AGILITY, 48)
			.item(DiaryItem.owned("Ninja or Kruk monkey greegree", "From Zooknock during Monkey Madness; Kruk's comes from MM II.",
				ItemID.MM_MONKEY_GREEGREE_FOR_SMALL_NINJA_MONKEY, ItemID.MM_MONKEY_GREEGREE_FOR_MEDIUM_NINJA_MONKEY,
				ItemID.MM2_KRUK_GREEGREE))
			.stage(simple("Run a lap of the Ape Atoll course", Location.of("Ape Atoll agility course start", 2754, 2742, 0, 3), null,
				"Stepping stone", "1. Wear the ninja greegree and jump on the Stepping stone (outlined), then finish each obstacle."))
			.build());
		out.add(task(t, 6, "Mahogany logs on Ape Atoll", "Chop and burn some Mahogany logs on Ape Atoll.")
			.questStarted(Quest.MONKEY_MADNESS_I)
			.skill(Skill.WOODCUTTING, 50).skill(Skill.FIREMAKING, 50)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DiaryItem.of("Tinderbox", ItemID.TINDERBOX))
			.stage(simple("Chop a mahogany on Ape Atoll", Location.of("Mahogany trees, Ape Atoll", 2773, 2697, 0, 4), null,
				"Mahogany tree", "1. Chop a Mahogany tree (outlined) on Ape Atoll. Don't use an infernal axe."))
			.stage(DiaryStage.builder()
				.title("Burn the mahogany logs here")
				.carrying(ItemID.MAHOGANY_LOGS)
				.use(ItemID.TINDERBOX).use(ItemID.MAHOGANY_LOGS)
				.line("1. Use the Tinderbox on the Mahogany logs (both outlined), still on Ape Atoll.")
				.build())
			.build());
		out.add(chompyHat(t, 9, 300).build());
		out.add(task(t, 10, "Isafdar painting", "Build an Isafdar painting in your POH Quest hall.")
			.quest(Quest.ROVING_ELVES)
			.skill(Skill.CONSTRUCTION, 65)
			.item(DiaryItem.of("Mahogany plank", 3, ItemID.PLANK_MAHOGANY))
			.item(DiaryItem.of("Saw", ItemID.POH_SAW))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.item(DiaryItem.of("Coins (painting from Sir Renitee)", 2_000, ItemID.COINS))
			.note("A quest hall in your house")
			.stage(simple("Buy the Isafdar painting", Location.of("Sir Renitee, Falador castle", 2982, 3341, 1, 3), "Sir Renitee", null,
				"1. Buy the Isafdar painting from Sir Renitee (outlined) in Falador castle."))
			.stage(DiaryStage.builder()
				.title("Hang it in your quest hall")
				.line("1. In build mode, build the Isafdar painting on the landscape space in your quest hall.")
				.build())
			.build());
		out.add(task(t, 1, "Elf with a crystal bow", "Kill an Elf with a Crystal bow.")
			.quest(Quest.ROVING_ELVES)
			.skill(Skill.RANGED, 70).unboostable(Skill.RANGED)
			.item(DiaryItem.owned("Crystal bow or Bow of Faerdhinen", "Made from a crystal weapon seed at Ilfeen or Prifddinas.",
				ItemID.CRYSTAL_BOW, ItemID.BOW_OF_FAERDHINEN, ItemID.BOW_OF_FAERDHINEN_INFINITE, ItemID.ROVING_CRYSTAL_BOW_NEW))
			.stage(simple("Shoot an elf with the crystal bow", LLETYA, "Elf warrior", null,
				"1. Wield the crystal bow and kill an Elf warrior (outlined) in Tirannwn, for example around Lletya."))
			.build());
		out.add(task(t, 8, "Palm tree in Lletya", "Check the health of your Palm tree in Lletya.")
			.questStarted(Quest.MOURNINGS_END_PART_I)
			.skill(Skill.FARMING, 68)
			.item(DiaryItem.owned("Palm tree sapling", "Plant a palm seed in a plant pot and water it.", ItemID.PLANTPOT_PALM_SAPLING))
			.item(DiaryItem.of("Rake", ItemID.RAKE))
			.item(DiaryItem.of("Spade", ItemID.SPADE))
			.item(DiaryItem.of("Papaya fruit (to pay the gardener)", 15, ItemID.PAPAYA))
			.stage(simple("Grow a palm in Lletya", Location.of("Fruit tree patch, Lletya", 2346, 3162, 0, 2), null, "Fruit tree patch",
				"1. Rake the Fruit tree patch (outlined) and plant the sapling. Pay the gardener the papayas to protect it.",
				"2. Come back in about 16 hours and click Check-health on the grown tree."))
			.build());
		out.add(task(t, 7, "Adamantite in Tirannwn", "Mine some Adamantite ore in Tirannwn.")
			.questStarted(Quest.REGICIDE)
			.skill(Skill.MINING, 70)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Mine adamantite in the Isafdar mine", Location.of("Mine, south-east Isafdar", 2276, 3160, 0, 4), null,
				"Adamantite rocks", "1. Mine the Adamantite rocks (outlined) in the mine just outside Lletya, or in Prifddinas."))
			.build());
		out.add(task(t, 11, "Zulrah", "Kill Zulrah.")
			.questStarted(Quest.REGICIDE)
			.note("Ranged and magic gear, antivenom and food; see the wiki strategy guide")
			.stage(simple("Take the boat to Zulrah's shrine", Location.of("Zul-Andra pier", 2213, 3056, 0, 3), null, "Sacrificial boat",
				"1. Get to Zul-Andra (Zul-andra teleport scroll or fairy ring BJS) and board the Sacrificial boat (outlined)."))
			.stage(DiaryStage.builder()
				.title("Kill Zulrah")
				.inside(Zone.anyFloor(2250, 3060, 2280, 3085))
				.npc("Zulrah")
				.line("1. Kill Zulrah (outlined), switching between ranged and magic as it changes colour.")
				.build())
			.build());
	}

	private static void elite(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.ELITE;
		out.add(task(t, 5, "Full void set", "Equip any complete void set.")
			.combat(40)
			.skill(Skill.PRAYER, 22).skill(Skill.ATTACK, 42).skill(Skill.STRENGTH, 42).skill(Skill.DEFENCE, 42)
			.skill(Skill.HITPOINTS, 42).skill(Skill.RANGED, 42).skill(Skill.MAGIC, 42)
			.item(DiaryItem.owned("Void knight top", "Bought with Pest Control points.", ItemID.PEST_VOID_KNIGHT_TOP))
			.item(DiaryItem.owned("Void knight robe", "Bought with Pest Control points.", ItemID.PEST_VOID_KNIGHT_ROBES))
			.item(DiaryItem.owned("Void knight gloves", "Bought with Pest Control points.", ItemID.PEST_VOID_KNIGHT_GLOVES))
			.item(DiaryItem.owned("Any void helm", "Bought with Pest Control points.", ItemID.GAME_PEST_MELEE_HELM,
				ItemID.GAME_PEST_ARCHER_HELM, ItemID.GAME_PEST_MAGE_HELM))
			.stage(DiaryStage.builder()
				.title("Wear the full void set")
				.line("1. Wear the void top, robe, gloves and any void helm at the same time.")
				.build())
			.build());
		out.add(task(t, 3, "Prissy Scilla's magic tree", "Have Prissy Scilla protect your Magic tree.")
			.skill(Skill.FARMING, 75)
			.item(DiaryItem.of("Magic sapling", ItemID.PLANTPOT_MAGIC_TREE_SAPLING))
			.item(DiaryItem.of("Coconut", 25, ItemID.COCONUT))
			.item(DiaryItem.of("Rake", ItemID.RAKE))
			.item(DiaryItem.of("Spade", ItemID.SPADE))
			.stage(simple("Plant a magic tree in the Stronghold", Location.of("Tree patch, Gnome Stronghold", 2435, 3415, 0, 2),
				"Prissy Scilla", "Tree patch",
				"1. Rake and plant the Magic sapling in the Tree patch (outlined) in the Gnome Stronghold.",
				"2. Pay Prissy Scilla (outlined) the 25 coconuts to protect it."))
			.build());
		out.add(task(t, 2, "Thermonuclear smoke devil", "Kill the Thermonuclear Smoke Devil.")
			.skill(Skill.SLAYER, 93).unboostable(Skill.SLAYER)
			.note("A facemask or slayer helmet, and boss gear; no task needed for the first kill")
			.stage(simple("Enter the Smoke Devil Dungeon", Location.of("Smoke Devil Dungeon entrance, north of Castle Wars", 2412, 3061, 0, 2),
				null, "Dungeon entrance", "1. Enter the Smoke Devil Dungeon (outlined) north-west of Castle Wars."))
			.stage(DiaryStage.builder()
				.title("Kill the thermonuclear smoke devil")
				.inside(Zone.anyFloor(2340, 9400, 2430, 9460))
				.npc("Thermonuclear smoke devil")
				.line("1. Crawl into the boss room and kill the Thermonuclear smoke devil (outlined).")
				.line("Stay in the room until its death animation ends.")
				.build())
			.build());
		out.add(task(t, 4, "Advanced Elven overpass shortcut", "Use the Elven overpass advanced cliffside shortcut.")
			.quest(Quest.UNDERGROUND_PASS)
			.skill(Skill.AGILITY, 85)
			.stage(simple("Climb the advanced cliffside", Location.of("Elven overpass, Arandar", 2337, 3253, 0, 3), null, "Rocks",
				"1. On the Elven overpass in Arandar, climb the advanced cliffside rocks (outlined)."))
			.build());
		out.add(task(t, 1, "Magic longbow in Tirannwn", "Fletch a Magic Longbow in Tirannwn.")
			.questStarted(Quest.REGICIDE)
			.skill(Skill.FLETCHING, 85)
			.item(DiaryItem.of("Magic longbow (u)", ItemID.UNSTRUNG_MAGIC_LONGBOW))
			.item(DiaryItem.of("Bow string", ItemID.BOW_STRING))
			.stage(DiaryStage.builder()
				.title("String the bow in Tirannwn")
				.at(LLETYA)
				.use(ItemID.BOW_STRING).use(ItemID.UNSTRUNG_MAGIC_LONGBOW)
				.line("1. Anywhere in Tirannwn, use the Bow string on the Magic longbow (u) (both outlined).")
				.build())
			.build());
		out.add(task(t, 7, "Pickpocket an elf", "Pickpocket an Elf.")
			.questStarted(Quest.MOURNINGS_END_PART_I)
			.skill(Skill.THIEVING, 85)
			.stage(simple("Pickpocket an elf", LLETYA, "Elf", null,
				"1. Right-click an elf (outlined) in Lletya or Prifddinas and choose Pickpocket."))
			.build());
		out.add(chompyHat(t, 6, 1000).build());
	}

	private static DiaryTask.DiaryTaskBuilder task(DiaryTier tier, int number, String name, String text)
	{
		int index = BITS.get(tier)[number - 1];
		return DiaryTask.builder()
			.region(DiaryRegion.WESTERN)
			.tier(tier)
			.number(number)
			.name(name)
			.text(text)
			.varp(index < 32 ? VarPlayerID.WESTERN_ACHIEVEMENT_DIARY : VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2)
			.bit(index % 32);
	}
}
