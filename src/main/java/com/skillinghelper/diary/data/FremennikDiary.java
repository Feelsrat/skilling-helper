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
 * Fremennik diary, every tier. Task wording and requirements from the OSRS Wiki, listed in walking order.
 */
final class FremennikDiary
{
	/**
	 * Done bit per wiki task number across both Fremennik diary varps (32 on is the second). Bits 16 and 22 are unused.
	 */
	private static final Map<DiaryTier, int[]> BITS = Map.of(
		DiaryTier.EASY, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
		DiaryTier.MEDIUM, new int[]{11, 12, 13, 14, 15, 17, 18, 19, 20},
		DiaryTier.HARD, new int[]{21, 23, 24, 25, 26, 27, 28, 29, 30},
		DiaryTier.ELITE, new int[]{31, 32, 33, 34, 35, 36});

	private static final Location RELLEKKA = Location.of("Rellekka", 2660, 3660, 0, 8);
	private static final Location RELLEKKA_HUNTER = Location.of("Rellekka hunter area", 2725, 3770, 0, 6);
	private static final Location RELLEKKA_MINE = Location.of("Rellekka mine", 2683, 3702, 0, 4);
	private static final Location KELDAGRIM_TRAPDOOR = Location.of("Keldagrim trapdoor, Grand Exchange", 3140, 3504, 0, 2);
	private static final Zone KELDAGRIM = Zone.of(2816, 10158, 2943, 10238, 0);
	private static final Location GWD = Location.of("God Wars Dungeon entrance, Trollheim", 2919, 3747, 0, 2);
	private static final Zone GWD_ZONE = Zone.anyFloor(2819, 5252, 2943, 5375);
	private static final Location NEITIZNOT = Location.of("Neitiznot", 2336, 3805, 0, 6);
	private static final Location WATERBIRTH_BOAT = Location.of("Jarvald, Rellekka docks", 2620, 3686, 0, 3);
	private static final Location TROLL_STRONGHOLD = Location.of("Troll Stronghold entrance", 2828, 3647, 0, 2);

	private FremennikDiary()
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

	private static DiaryStage keldagrimEntrance()
	{
		return simple("Take the trapdoor to Keldagrim", KELDAGRIM_TRAPDOOR, null, "Trapdoor",
			"1. Go down the Trapdoor (outlined) in the house just west of the Grand Exchange, then take the minecart.",
			"Or use fairy ring DKS / walk through the Ice Mountain tunnel.");
	}

	private static void easy(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.EASY;
		out.add(task(t, 8, "Rellekka well bucket", "Fill a bucket with water at the Rellekka well.")
			.item(DiaryItem.of("Bucket", ItemID.BUCKET_EMPTY))
			.stage(simple("Fill a bucket at the well", Location.of("Rellekka well", 2669, 3661, 0, 2), null, "Well",
				"1. Use the Bucket on the Well (outlined) in the middle of Rellekka."))
			.build());
		out.add(task(t, 2, "Yrsa's shop", "Change your boots at Yrsa's Shoe Store.")
			.stage(simple("Browse Yrsa's clothes shop", Location.of("Yrsa's shop, Rellekka", 2625, 3674, 0, 3), "Yrsa", null,
				"1. Right-click Yrsa (outlined) in west Rellekka and choose Trade. Browsing is enough."))
			.build());
		out.add(task(t, 10, "Oak logs in Fremennik", "Chop and burn some oak logs in the Fremennik Province.")
			.skill(Skill.WOODCUTTING, 15).skill(Skill.FIREMAKING, 15)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DiaryItem.of("Tinderbox", ItemID.TINDERBOX))
			.stage(simple("Chop an oak by Rellekka", Location.of("Oak trees, south-east Rellekka", 2714, 3664, 0, 3), null,
				"Oak tree", "1. Chop an Oak tree (outlined) near the Rellekka house portal until you get Oak logs."))
			.stage(DiaryStage.builder()
				.title("Burn the oak logs here")
				.carrying(ItemID.OAK_LOGS)
				.use(ItemID.TINDERBOX).use(ItemID.OAK_LOGS)
				.line("1. Use the Tinderbox on the Oak logs (both outlined).")
				.build())
			.build());
		out.add(task(t, 3, "5 rock crabs", "Kill 5 Rock crabs.")
			.stage(simple("Kill 5 rock crabs", Location.of("Rock crabs, north of Rellekka", 2707, 3723, 0, 6), "Rock crab", "Rocks",
				"1. Stand next to the small rocks (outlined) on the beach north of Rellekka; they wake up as Rock crabs.",
				"2. Kill 5 of them."))
			.build());
		out.add(task(t, 4, "Tiara from scratch", "Craft a tiara from scratch in Rellekka.")
			.quest(Quest.THE_FREMENNIK_TRIALS)
			.skill(Skill.CRAFTING, 23).skill(Skill.MINING, 20).skill(Skill.SMITHING, 20)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.item(DiaryItem.of("Tiara mould", ItemID.TIARA_MOULD))
			.stage(simple("Mine silver ore in Rellekka", RELLEKKA_MINE, null, "Silver rocks",
				"1. Mine the Silver rocks (outlined) in the Rellekka mine until you get one ore.",
				"Doing any other diary task before the tiara is made resets this one."))
			.stage(DiaryStage.builder()
				.title("Smelt the silver and make the tiara")
				.carrying(ItemID.SILVER_ORE)
				.at(Location.of("Furnace, west Rellekka", 2617, 3667, 0, 2))
				.object("Furnace")
				.use(ItemID.SILVER_ORE)
				.line("1. Use the Silver ore on the Furnace (outlined) in Rellekka's westernmost building.")
				.line("2. Use the Silver bar on the furnace again and pick the Tiara.")
				.build())
			.stage(DiaryStage.builder()
				.title("Make the tiara")
				.carrying(ItemID.SILVER_BAR)
				.at(Location.of("Furnace, west Rellekka", 2617, 3667, 0, 2))
				.object("Furnace")
				.use(ItemID.SILVER_BAR)
				.line("1. Use the Silver bar on the Furnace (outlined) and pick the Tiara.")
				.build())
			.build());
		out.add(task(t, 1, "Cerulean twitch", "Catch a Cerulean twitch.")
			.skill(Skill.HUNTER, 11)
			.item(DiaryItem.of("Bird snare", ItemID.HUNTING_OJIBWAY_BIRD_SNARE))
			.stage(simple("Catch a cerulean twitch", RELLEKKA_HUNTER, "Cerulean twitch", null,
				"1. The twitches live in the snowy hunter area north-east of Rellekka.",
				"2. Lay the Bird snare and wait for a Cerulean twitch to get caught, then check it."))
			.build());
		out.add(task(t, 6, "Waterbirth snape grass", "Collect 5 Snape grass on Waterbirth Island.")
			.item(DiaryItem.of("Coins (boat fare)", 1_000, ItemID.COINS))
			.stage(simple("Sail to Waterbirth Island", WATERBIRTH_BOAT, "Jarvald", null,
				"1. Talk to Jarvald (outlined) on the Rellekka docks and sail to Waterbirth Island (free after the Fremennik Trials)."))
			.stage(DiaryStage.builder()
				.title("Pick up snape grass 5 times")
				.inside(Zone.of(2499, 3713, 2557, 3770, 0))
				.at(Location.of("Snape grass, Waterbirth Island", 2551, 3754, 0, 4))
				.line("1. Pick up Snape grass on the island; drop and pick up the same one until you've done it 5 times.")
				.build())
			.build());
		out.add(task(t, 9, "Troll Stronghold", "Enter the Troll Stronghold.")
			.questStarted(Quest.TROLL_STRONGHOLD)
			.stage(simple("Walk into the Troll Stronghold", TROLL_STRONGHOLD, null, "Stronghold",
				"1. Climb Death Plateau and walk into the Troll Stronghold (outlined).",
				"With Ghommal's hilt 1 you can teleport straight there."))
			.build());
		out.add(task(t, 5, "Keldagrim stonemason", "Browse the Stonemasons shop.")
			.questStarted(Quest.THE_GIANT_DWARF)
			.stage(keldagrimEntrance())
			.stage(DiaryStage.builder()
				.title("Browse the Stonemason's shop")
				.inside(KELDAGRIM)
				.at(Location.of("Stonemason, west Keldagrim", 2848, 10185, 0, 3))
				.npc("Stonemason")
				.line("1. Right-click the Stonemason (outlined) in west Keldagrim and choose Trade.")
				.build())
			.build());
		out.add(task(t, 7, "Keldagrim stall", "Steal from the Keldagrim crafting or baker's stall.")
			.questStarted(Quest.THE_GIANT_DWARF)
			.skill(Skill.THIEVING, 5)
			.stage(keldagrimEntrance())
			.stage(DiaryStage.builder()
				.title("Steal from the bakery stall")
				.inside(KELDAGRIM)
				.at(Location.of("Bakery stall, Keldagrim", 2892, 10211, 0, 2))
				.object("Bakery Stall").object("Crafting stall")
				.line("1. Right-click the Bakery stall (outlined) east of the palace and choose Steal-from.")
				.build())
			.build());
	}

	private static void medium(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.MEDIUM;
		out.add(task(t, 3, "Rellekka coal", "Mine some coal in Rellekka.")
			.quest(Quest.THE_FREMENNIK_TRIALS)
			.skill(Skill.MINING, 30)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Mine coal in the Rellekka mine", RELLEKKA_MINE, null, "Coal rocks",
				"1. Mine the Coal rocks (outlined) until you get one coal."))
			.build());
		out.add(task(t, 4, "Rellekka fish stall", "Steal from the Rellekka Fish stalls.")
			.quest(Quest.THE_FREMENNIK_TRIALS)
			.skill(Skill.THIEVING, 42)
			.stage(simple("Steal from the fish stall", Location.of("Fish market, Rellekka", 2648, 3677, 0, 2), null, "Fish stall",
				"1. Right-click the Fish stall (outlined) in the Rellekka market and choose Steal-from."))
			.build());
		out.add(task(t, 6, "Snowy knight", "Catch a Snowy knight.")
			.skill(Skill.HUNTER, 35)
			.item(DiaryItem.of("Butterfly net", ItemID.HUNTING_BUTTERFLY_NET))
			.item(DiaryItem.of("Butterfly jar", ItemID.BUTTERFLY_JAR))
			.stage(simple("Catch a snowy knight", RELLEKKA_HUNTER, "Snowy knight", null,
				"1. Wield the Butterfly net with a jar in your inventory.",
				"2. Click Catch on a Snowy knight (outlined) in the hunter area north-east of Rellekka."))
			.build());
		out.add(task(t, 5, "Fairy ring to Miscellania", "Travel to Miscellania by Fairy ring.")
			.quest(Quest.THE_FREMENNIK_TRIALS)
			.questStarted(Quest.FAIRYTALE_II__CURE_A_QUEEN)
			.item(DiaryStages.FAIRY_STAFF)
			.stages(DiaryStages.fairyRing("CIP", "You land on Miscellania, which finishes it."))
			.build());
		out.add(task(t, 1, "Brine rat", "Slay a Brine rat.")
			.questStarted(Quest.OLAFS_QUEST)
			.skill(Skill.SLAYER, 47)
			.item(DiaryItem.of("Spade", ItemID.SPADE))
			.stage(simple("Dig into the brine rat cavern", Location.of("Brine rat cavern, east of Rellekka", 2749, 3732, 0, 1),
				null, null, "1. Stand on the rocky patch east of the Rellekka hunter area and Dig with the spade."))
			.stage(DiaryStage.builder()
				.title("Kill a brine rat")
				.inside(Zone.of(2688, 10116, 2751, 10175, 0))
				.npc("Brine rat")
				.line("1. Kill a Brine rat (outlined).")
				.build())
			.build());
		out.add(task(t, 2, "Eagle to the snowy area", "Travel to the Snowy Hunter Area via Eagle.")
			.quest(Quest.EAGLES_PEAK)
			.item(DiaryItem.of("Rope", ItemID.ROPE))
			.stage(simple("Go into the Eagles' Peak cave", Location.of("Eagles' Peak cave entrance", 2329, 3495, 0, 2), null,
				"Cave entrance", "1. Enter the cave (outlined) at the top of Eagles' Peak."))
			.stage(DiaryStage.builder()
				.title("Ride the polar eagle")
				.inside(Zone.anyFloor(1986, 4944, 2030, 4985))
				.at(Location.of("Polar eagle, Eagles' Peak", 2027, 4964, 3, 3))
				.npc("Polar eagle")
				.line("1. Use the Rope on the Polar eagle (outlined) to fly to the snowy hunter area.")
				.build())
			.build());
		out.add(task(t, 7, "Pet rock in the menagerie", "Pick up your Pet Rock from your POH Menagerie.")
			.questStarted(Quest.THE_FREMENNIK_TRIALS)
			.skill(Skill.CONSTRUCTION, 37)
			.item(DiaryItem.owned("Pet rock", "Askeladden in Rellekka gives it during The Fremennik Trials.",
				ItemID.VT_USELESS_ROCK))
			.note("A menagerie with a pet house in your house")
			.stage(DiaryStage.builder()
				.title("Put the pet rock in your menagerie and pick it up")
				.line("1. In your house (not in build mode), use the Pet rock on the pet house in the menagerie.")
				.line("2. Pick the rock back up off the floor.")
				.build())
			.build());
		out.add(task(t, 9, "Arzinian mine gold", "Mine some gold at the Arzinian mine.")
			.questStarted(Quest.BETWEEN_A_ROCK)
			.skill(Skill.MINING, 40).skill(Skill.DEFENCE, 30)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.item(DiaryItem.owned("Gold helmet", "Made during Between a Rock...", ItemID.DWARF_GOLDROCK_HELMET))
			.stage(simple("Take the boat to the Arzinian mine", Location.of("Dwarven boatman, Keldagrim river", 2842, 10129, 0, 3),
				"Boatman", null, "1. Wear the Gold helmet and talk to the dwarven Boatman (outlined) south of Keldagrim."))
			.stage(DiaryStage.builder()
				.title("Mine gold")
				.inside(Zone.of(2497, 4928, 2691, 4989, 0))
				.object("Gold rocks")
				.line("1. Mine the Gold rocks (outlined) in the Arzinian mine.")
				.build())
			.build());
		out.add(task(t, 8, "Lighthouse via Waterbirth", "Visit the Lighthouse from Waterbirth island.")
			.quest(Quest.HORROR_FROM_THE_DEEP)
			.note("Another player, a pet rock with rune thrownaxes, or 85 Agility for the two-player doors; protection prayers")
			.stage(simple("Sail to Waterbirth Island", WATERBIRTH_BOAT, "Jarvald", null,
				"1. Talk to Jarvald (outlined) and sail to Waterbirth Island."))
			.stage(DiaryStage.builder()
				.title("Go through the Waterbirth dungeon to the lighthouse")
				.inside(Zone.of(2499, 3713, 2557, 3770, 0))
				.at(Location.of("Waterbirth dungeon entrance", 2521, 3740, 0, 2))
				.object("Cave Entrance")
				.line("1. Enter the cave (outlined) and get past the doors.")
				.line("2. Go down through the sublevels and leave by the exit on level 5, which comes out at the lighthouse.")
				.build())
			.build());
	}

	private static void hard(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.HARD;
		out.add(task(t, 1, "Trollheim Teleport", "Teleport to Trollheim.")
			.quest(Quest.EADGARS_RUSE)
			.skill(Skill.MAGIC, 61)
			.item(DiaryItem.of("Law rune", 2, ItemID.LAWRUNE))
			.item(DiaryItem.of("Fire rune", 2, ItemID.FIRERUNE))
			.stage(DiaryStage.builder()
				.title("Cast Trollheim Teleport")
				.line("1. Open the spellbook and click Trollheim Teleport.")
				.build())
			.build());
		out.add(task(t, 3, "Super defence in Fremennik", "Mix a super defence potion in the Fremennik Province.")
			.skill(Skill.HERBLORE, 66)
			.item(DiaryItem.of("Cadantine potion (unf)", ItemID.CADANTINEVIAL))
			.item(DiaryItem.of("White berries", ItemID.WHITE_BERRIES))
			.stage(DiaryStage.builder()
				.title("Mix a super defence in Rellekka")
				.at(RELLEKKA)
				.use(ItemID.WHITE_BERRIES).use(ItemID.CADANTINEVIAL)
				.line("1. In or near Rellekka, use the White berries on the Cadantine potion (unf) (both outlined).")
				.build())
			.build());
		out.add(task(t, 2, "Sabre-toothed kyatt", "Catch a Sabre-toothed Kyatt.")
			.skill(Skill.HUNTER, 55)
			.item(DiaryItem.of("Teasing stick", ItemID.HUNTING_TEASING_STICK))
			.item(DiaryItem.of("Logs", ItemID.LOGS))
			.item(DiaryItem.of("Knife", ItemID.KNIFE))
			.stage(simple("Catch a kyatt in a pit", RELLEKKA_HUNTER, "Sabre-toothed kyatt", "Pit",
				"1. Click Trap on a Pit (outlined) with the logs and knife.",
				"2. Tease a Sabre-toothed kyatt, then jump over the pit so it follows you in."))
			.build());
		out.add(task(t, 9, "Free Blast Furnace", "Obtain the Blast Furnace Foreman's permission to use the Blast Furnace for free.")
			.questStarted(Quest.THE_GIANT_DWARF)
			.skill(Skill.SMITHING, 60).unboostable(Skill.SMITHING)
			.stage(keldagrimEntrance())
			.stage(DiaryStage.builder()
				.title("Go down to the Blast Furnace")
				.inside(KELDAGRIM)
				.at(Location.of("Blast Furnace stairs, Keldagrim", 2930, 10197, 0, 2))
				.object("Stairs")
				.line("1. Go down the Stairs (outlined) in the east of Keldagrim.")
				.build())
			.stage(DiaryStage.builder()
				.title("Ask the Foreman for free use")
				.inside(Zone.of(1933, 4955, 1958, 4975, 0))
				.npc("Blast Furnace Foreman")
				.line("1. Talk to the Blast Furnace Foreman (outlined).")
				.line("2. Choose \"What?\", then \"Can I use the furnace to smelt ore?\", then \"I have level 60!\".")
				.line("If he asks for money, hop worlds and try again.")
				.build())
			.build());
		out.add(task(t, 4, "Keldagrim gem stall", "Steal from the Keldagrim Gem Stall.")
			.questStarted(Quest.THE_GIANT_DWARF)
			.skill(Skill.THIEVING, 75)
			.stage(keldagrimEntrance())
			.stage(DiaryStage.builder()
				.title("Steal from the gem stall")
				.inside(KELDAGRIM)
				.at(Location.of("Gem stall, Keldagrim", 2888, 10211, 0, 2))
				.object("Gem stall")
				.line("1. Right-click the Gem stall (outlined) east of the palace and choose Steal-from.")
				.build())
			.build());
		out.add(task(t, 7, "100% Miscellania support", "Obtain 100% support from your kingdom subjects.")
			.questStarted(Quest.THRONE_OF_MISCELLANIA)
			.item(DiaryItem.of("Rake", ItemID.RAKE))
			.stage(simple("Rake the Miscellania herb patches to 100%", Location.of("Herb patches, Miscellania", 2527, 3849, 0, 4),
				null, "Herbs",
				"1. On Miscellania, rake the herb patches (outlined) north of the castle.",
				"2. Keep going until approval reaches 100% (check with Advisor Ghrim). Mining, chopping or fishing also work."))
			.build());
		out.add(task(t, 8, "Waterbirth Teleport", "Teleport to Waterbirth Island.")
			.quest(Quest.LUNAR_DIPLOMACY)
			.skill(Skill.MAGIC, 72)
			.note("Lunar spellbook selected")
			.item(DiaryItem.of("Law rune", 1, ItemID.LAWRUNE))
			.item(DiaryItem.of("Astral rune", 2, ItemID.ASTRALRUNE))
			.item(DiaryItem.of("Water rune", 1, ItemID.WATERRUNE))
			.stage(DiaryStage.builder()
				.title("Cast Waterbirth Teleport")
				.line("1. Open the Lunar spellbook and click Waterbirth Teleport.")
				.build())
			.build());
		out.add(task(t, 5, "Neitiznot shield", "Craft a Neitiznot shield on Neitiznot.")
			.questStarted(Quest.THE_FREMENNIK_ISLES)
			.skill(Skill.WOODCUTTING, 56)
			.item(DiaryItem.of("Arctic pine logs", 2, ItemID.ARCTIC_PINE_LOG))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.item(DiaryItem.of("Rope", ItemID.ROPE))
			.item(DiaryItem.of("Bronze nails", 15, ItemID.NAILS_BRONZE))
			.stage(simple("Make the shield on Neitiznot", Location.of("Woodcutting stump, Neitiznot", 2342, 3807, 0, 2), null,
				"Woodcutting stump",
				"1. Sail to Neitiznot with Mord Gunnars in north Rellekka.",
				"2. Split the logs on the Woodcutting stump (outlined), then use the planks on it again to make the shield."))
			.build());
		out.add(task(t, 6, "5 adamantite on Jatizso", "Mine 5 Adamantite ores on Jatizso.")
			.questStarted(Quest.THE_FREMENNIK_ISLES)
			.skill(Skill.MINING, 70)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Go into the Jatizso mine", Location.of("Mine entrance, Jatizso", 2398, 3813, 0, 2), null, "Mine",
				"1. Sail to Jatizso with Mord Gunnars and go into the mine (outlined)."))
			.stage(DiaryStage.builder()
				.title("Mine 5 adamantite ore")
				.inside(Zone.of(2373, 10179, 2429, 10234, 0))
				.at(Location.of("Adamantite rocks, Jatizso mine", 2402, 10189, 0, 3))
				.object("Adamantite rocks")
				.line("1. Mine the Adamantite rocks (outlined) until you have 5 ore. Ice trolls attack, so bring food.")
				.build())
			.build());
	}

	private static void elite(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.ELITE;
		out.add(task(t, 4, "Rellekka rooftops", "Complete a lap of the Rellekka agility course.")
			.skill(Skill.AGILITY, 80)
			.stage(simple("Run a lap of the Rellekka rooftops", Location.of("Rellekka rooftop course start", 2625, 3677, 0, 2), null,
				"Rough wall", "1. Climb the Rough wall (outlined) in west Rellekka, then cross each obstacle to the end."))
			.build());
		out.add(task(t, 3, "Dragonstone amulet on Neitiznot", "Create a dragonstone amulet in the Neitiznot furnace.")
			.questStarted(Quest.THE_FREMENNIK_ISLES)
			.skill(Skill.CRAFTING, 80)
			.item(DiaryItem.of("Dragonstone", ItemID.DRAGONSTONE))
			.item(DiaryItem.of("Gold bar", ItemID.GOLD_BAR))
			.item(DiaryItem.of("Amulet mould", ItemID.AMULET_MOULD))
			.stage(simple("Make the amulet at the Neitiznot furnace", Location.of("Clay forge, Neitiznot", 2344, 3811, 0, 2), null,
				"Clay forge", "1. Use the Gold bar on the Clay forge (outlined) on Neitiznot and make a Dragonstone amulet (u)."))
			.build());
		out.add(task(t, 2, "56 astral runes", "Craft 56 astral runes simultaneously from Essence without the use of Extracts.")
			.quest(Quest.LUNAR_DIPLOMACY)
			.skill(Skill.RUNECRAFT, 82)
			.item(DiaryItem.anyOf("Pure essence", 28, ItemID.BLANKRUNE_HIGH, DiaryStages.PURE_ESSENCE))
			.stage(simple("Craft astrals on Lunar Isle", Location.of("Astral altar, Lunar Isle", 2158, 3864, 0, 3), null, "Altar",
				"1. Wear your seal of passage, get to Lunar Isle (Lunar spellbook teleport or the pirate ship from Rellekka).",
				"2. Click Craft-rune on the Altar (outlined) with 28 essence; at 82 each makes 2."))
			.build());
		out.add(task(t, 6, "Spiritual mage in God Wars", "Slay a Spiritual mage within the Godwars Dungeon.")
			.questStarted(Quest.TROLL_STRONGHOLD)
			.skill(Skill.SLAYER, 83)
			.item(DiaryItem.of("Rope", ItemID.ROPE))
			.note("A god item for the area you fight in, so the followers don't attack")
			.stage(simple("Enter the God Wars Dungeon", GWD, null, "Hole",
				"1. Climb up past Trollheim to the hole (outlined), tie the rope and climb down."))
			.stage(DiaryStage.builder()
				.title("Kill a spiritual mage")
				.inside(GWD_ZONE)
				.npc("Spiritual mage")
				.line("1. Kill a Spiritual mage (outlined) of any god.")
				.build())
			.build());
		out.add(task(t, 5, "God Wars generals", "Kill the generals of Armadyl, Bandos, Saradomin and Zamorak in the God Wars Dungeon.")
			.questStarted(Quest.TROLL_STRONGHOLD)
			.skill(Skill.AGILITY, 70).skill(Skill.STRENGTH, 70).skill(Skill.HITPOINTS, 70).skill(Skill.RANGED, 70)
			.item(DiaryItem.of("Rope", ItemID.ROPE))
			.note("Boss gear and 40 killcount per god (progress isn't lost between trips)")
			.stage(simple("Enter the God Wars Dungeon", GWD, null, "Hole",
				"1. Climb up past Trollheim to the hole (outlined) and climb down with a rope."))
			.stage(DiaryStage.builder()
				.title("Kill all four generals")
				.inside(GWD_ZONE)
				.npc("Kree'arra").npc("General Graardor").npc("Commander Zilyana").npc("K'ril Tsutsaroth")
				.line("1. Get 40 killcount in a god's area, enter its boss room and kill the general.")
				.line("2. Repeat for Armadyl, Bandos, Saradomin and Zamorak. The bodyguards don't have to die.")
				.build())
			.build());
		out.add(task(t, 1, "Dagannoth Kings", "Kill each of the Dagannoth Kings.")
			.note("Gear for the three kings and a way past the two-player doors; another diary task in between resets it")
			.stage(simple("Sail to Waterbirth Island", WATERBIRTH_BOAT, "Jarvald", null,
				"1. Talk to Jarvald (outlined) and sail to Waterbirth Island."))
			.stage(DiaryStage.builder()
				.title("Go down to the kings and kill all three")
				.inside(Zone.of(2499, 3713, 2557, 3770, 0))
				.at(Location.of("Waterbirth dungeon entrance", 2521, 3740, 0, 2))
				.object("Cave Entrance")
				.npc("Dagannoth Rex").npc("Dagannoth Prime").npc("Dagannoth Supreme")
				.line("1. Enter the cave (outlined) and work down the ladders to the kings' lair.")
				.line("2. Kill Dagannoth Rex, Prime and Supreme.")
				.build())
			.build());
	}

	private static DiaryTask.DiaryTaskBuilder task(DiaryTier tier, int number, String name, String text)
	{
		int index = BITS.get(tier)[number - 1];
		return DiaryTask.builder()
			.region(DiaryRegion.FREMENNIK)
			.tier(tier)
			.number(number)
			.name(name)
			.text(text)
			.varp(index < 32 ? VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY : VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2)
			.bit(index % 32);
	}
}
