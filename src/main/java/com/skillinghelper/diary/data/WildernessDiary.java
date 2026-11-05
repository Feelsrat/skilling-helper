package com.skillinghelper.diary.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.ToolType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;

/**
 * Wilderness diary, every tier. Task wording and requirements from the OSRS Wiki, listed in walking order. Every task
 * but the Emblem Trader chat is in the Wilderness, so they only show up when the Wilderness opt-in is on.
 */
final class WildernessDiary
{
	/**
	 * Done bit per wiki task number across both Wilderness diary varps (32 on is the second).
	 */
	private static final Map<DiaryTier, int[]> BITS = Map.of(
		DiaryTier.EASY, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12},
		DiaryTier.MEDIUM, new int[]{13, 14, 15, 16, 18, 19, 20, 21, 22, 23, 24},
		DiaryTier.HARD, new int[]{25, 26, 27, 28, 29, 30, 31, 32, 33, 34},
		DiaryTier.ELITE, new int[]{35, 37, 39, 40, 41, 42, 43});

	/**
	 * Team-1 to Team-50 capes, which are every odd id from Team-1 on.
	 */
	private static final int[] TEAM_CAPES = IntStream.rangeClosed(0, 49).map(i -> ItemID.WILDERNESS_CAPE_1 + 2 * i).toArray();

	private static final Location EDGEVILLE_LEVER = Location.of("Lever, Edgeville", 3090, 3475, 0, 2);
	private static final Location WILDY_GWD = Location.of("Wilderness God Wars Dungeon entrance", 3017, 3740, 0, 2);
	private static final Zone WILDY_GWD_ZONE = Zone.anyFloor(3010, 10110, 3075, 10170);
	private static final Location RESOURCE_AREA = Location.of("Wilderness Resource Area gate", 3184, 3944, 0, 2);
	private static final Zone RESOURCE_AREA_ZONE = Zone.of(3174, 3924, 3196, 3944, 0);
	private static final String RESOURCE_FEE = "Coins (Resource Area entry; less with the Wilderness diary)";
	private static final String SAME_TRIP = "Do it on the same Resource Area trip as the other tasks there, so you only pay once.";
	private static final Location LAVA_MAZE = Location.of("Lava Maze, north Wilderness", 3070, 3850, 0, 6);

	private WildernessDiary()
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

	private static DiaryStage enterResourceArea()
	{
		return simple("Pay into the Resource Area", RESOURCE_AREA, null, "Gate",
			"1. Open the Gate (outlined) of the Resource Area in the far north-east and pay the fee.");
	}

	private static DiaryStage wildyGwd()
	{
		return simple("Enter the Wilderness God Wars Dungeon", WILDY_GWD, null, "Cave",
			"1. Go into the cave (outlined) north-west of the Dark Warriors' Fortress. You need 60 Agility or 60 Strength inside.");
	}

	private static void easy(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.EASY;
		out.add(task(t, 2, "Wilderness lever", "Enter the Wilderness from the Ardougne or Edgeville lever.")
			.stage(simple("Pull the Edgeville lever", EDGEVILLE_LEVER, null, "Lever",
				"1. Pull the Lever (outlined) in the ruined building north of Edgeville. It drops you deep in the Wilderness, so bring nothing you'd mind losing."))
			.build());
		out.add(task(t, 12, "Team cape", "Equip any team cape in the Wilderness.")
			.item(DiaryItem.anyOf("Any team cape", 1, ItemID.WILDERNESS_CAPE_1, TEAM_CAPES))
			.stage(DiaryStage.builder()
				.title("Wear a team cape in the Wilderness")
				.at(Location.of("Wilderness ditch, Edgeville", 3087, 3523, 0, 4))
				.line("1. Cross the Wilderness ditch north of Edgeville with the cape in your pack.")
				.line("2. Put the team cape on once you're inside (wearing it in already doesn't count).")
				.build())
			.build());
		out.add(task(t, 11, "Mage of Zamorak to the Abyss", "Have the Mage of Zamorak teleport you to the Abyss.")
			.quest(Quest.ENTER_THE_ABYSS)
			.stage(simple("Ask the Mage of Zamorak for the Abyss", Location.of("Mage of Zamorak, Wilderness level 5", 3106, 3557, 0, 3),
				"Mage of Zamorak", null, "1. Right-click the Mage of Zamorak (outlined) just north of the Edgeville ditch and choose Teleport."))
			.build());
		out.add(task(t, 4, "Chaos altar temple", "Enter the Chaos Runecrafting temple.")
			.item(DiaryItem.owned("Chaos talisman or tiara", "Any chaos or catalytic talisman or tiara works, or use the Abyss.",
				ItemID.CHAOS_TALISMAN, ItemID.TIARA_CHAOS))
			.stage(simple("Enter the chaos ruins", Location.of("Chaos altar ruins, Wilderness level 9", 3060, 3591, 0, 2), null,
				"Mysterious ruins", "1. Use the talisman on (or wear the tiara and click) the Mysterious ruins (outlined)."))
			.build());
		out.add(task(t, 9, "Red spiders' eggs", "Collect 5 Red spiders' eggs from the Wilderness.")
			.stage(DiaryStages.edgevilleTrapdoor())
			.stage(DiaryStage.builder()
				.title("Pick up 5 red spiders' eggs")
				.inside(DiaryStages.EDGEVILLE_DUNGEON)
				.at(Location.of("Red spiders' eggs, north Edgeville Dungeon", 3120, 9957, 0, 5))
				.line("1. Walk north through the Wilderness gate in the dungeon and pick up Red spiders' eggs 5 times.")
				.build())
			.build());
		out.add(task(t, 6, "Earth warrior", "Kill an Earth Warrior in the Wilderness beneath Edgeville.")
			.skill(Skill.AGILITY, 15)
			.stage(DiaryStages.edgevilleTrapdoor())
			.stage(DiaryStage.builder()
				.title("Kill an earth warrior")
				.inside(DiaryStages.EDGEVILLE_DUNGEON)
				.at(Location.of("Earth warriors, north Edgeville Dungeon", 3120, 9990, 0, 5))
				.npc("Earth warrior")
				.line("1. Go north past the Wilderness gate, cross the monkey bars and kill an Earth warrior (outlined).")
				.build())
			.build());
		out.add(task(t, 5, "Mammoth", "Kill a Mammoth in the Wilderness.")
			.stage(simple("Kill a mammoth", Location.of("Mammoths, south-east of Ferox Enclave", 3165, 3600, 0, 6), "Mammoth", null,
				"1. Kill a Mammoth (outlined) south-east of the Ferox Enclave."))
			.build());
		out.add(task(t, 10, "Wilderness iron", "Mine some Iron ore in the Wilderness.")
			.skill(Skill.MINING, 15)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Mine iron at the hobgoblin mine", Location.of("Hobgoblin mine, Wilderness", 3081, 3760, 0, 4), null, "Iron rocks",
				"1. Mine the Iron rocks (outlined) at the hobgoblin mine, Wilderness level 30."))
			.build());
		out.add(task(t, 3, "Chaos Altar prayer", "Pray at the Chaos Altar in the Western Wilderness.")
			.stage(simple("Pray at the Chaos Temple", Location.of("Chaos Temple, west Wilderness", 2947, 3820, 0, 3), null, "Chaos altar",
				"1. Click Pray-at on the Chaos altar (outlined) in the temple, Wilderness level 38."))
			.build());
		out.add(task(t, 8, "King Black Dragon's lair", "Enter the King Black Dragon's lair.")
			.stage(simple("Go down into the Lava Maze dungeon", LAVA_MAZE, null, "Ladder",
				"1. Climb down the Ladder (outlined) in the Lava Maze."))
			.stage(DiaryStage.builder()
				.title("Pull the lever to the lair")
				.inside(Zone.anyFloor(3010, 10240, 3070, 10270))
				.at(Location.of("KBD lever", 3016, 10249, 0, 2))
				.object("Lever")
				.line("1. Walk to the Lever (outlined) at the end of the dungeon and pull it. Leave straight away.")
				.build())
			.build());
		out.add(task(t, 1, "Low Alchemy at the Fountain of Rune", "Cast Low Alchemy at the Fountain of Rune.")
			.skill(Skill.MAGIC, 21)
			.item(DiaryItem.of("Nature rune", 1, ItemID.NATURERUNE))
			.item(DiaryItem.of("Fire rune", 3, ItemID.FIRERUNE))
			.stage(simple("Low alch at the fountain", Location.of("Fountain of Rune, north-east Wilderness", 3374, 3893, 0, 3), null,
				"Fountain of Rune", "1. Stand by the Fountain of Rune (outlined) and cast Low Level Alchemy on any item."))
			.build());
		out.add(task(t, 7, "Demonic ruins prayer", "Restore some prayer points at the demonic ruins.")
			.stage(simple("Restore prayer at the demonic ruins", Location.of("Demonic ruins, north-east Wilderness", 3288, 3886, 0, 3), null,
				"Demonic ruins", "1. Use some prayer first, then click the ruins (outlined) to restore it."))
			.build());
	}

	private static void medium(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.MEDIUM;
		out.add(task(t, 9, "Emblem Trader", "Talk to the Emblem Trader in Edgeville about emblems.")
			.stage(simple("Talk to the Emblem Trader", Location.of("Emblem Trader, Edgeville", 3097, 3504, 0, 3), "Emblem Trader", null,
				"1. Talk to the Emblem Trader (outlined) just north of the Edgeville bank and ask about emblems."))
			.build());
		out.add(task(t, 7, "Charge an earth orb", "Charge an Earth Orb.")
			.skill(Skill.MAGIC, 60)
			.item(DiaryItem.of("Unpowered orb", ItemID.STAFFORB))
			.item(DiaryItem.of("Cosmic rune", 3, ItemID.COSMICRUNE))
			.item(DiaryItem.of("Earth rune", 30, ItemID.EARTHRUNE))
			.stage(DiaryStages.edgevilleTrapdoor())
			.stage(DiaryStage.builder()
				.title("Charge the orb on the earth obelisk")
				.inside(DiaryStages.EDGEVILLE_DUNGEON)
				.at(Location.of("Earth obelisk, Edgeville Dungeon", 3088, 9933, 0, 2))
				.object("Obelisk of Earth")
				.line("1. Go north past the Wilderness gate and cast Charge Earth Orb on the Obelisk of Earth (outlined).")
				.build())
			.build());
		out.add(task(t, 5, "Green dragon", "Kill a Green Dragon.")
			.item(DiaryStages.DRAGONFIRE_SHIELD)
			.stage(simple("Kill a green dragon", Location.of("Green dragons, west of the Graveyard of Shadows", 2980, 3615, 0, 6),
				"Green dragon", null, "1. Kill a Green dragon (outlined) in the Wilderness. An anti-dragon shield blocks most dragonfire."))
			.build());
		out.add(task(t, 2, "Ent yew logs", "Chop some yew logs from a fallen Ent.")
			.skill(Skill.WOODCUTTING, 61).unboostable(Skill.WOODCUTTING)
			.item(DiaryItem.tool(ToolType.AXE))
			.stage(simple("Kill an ent and chop its trunk", Location.of("Ents, by the Chaos Temple (east)", 3240, 3625, 0, 6), "Ent", "Ent trunk",
				"1. Kill an Ent (outlined) near the elder chaos druids' temple.",
				"2. Chop the Ent trunk it leaves until you get yew logs (needs at least a mithril axe)."))
			.build());
		out.add(task(t, 6, "Ankou", "Kill an Ankou in the Wilderness.")
			.stage(simple("Kill an ankou", Location.of("Forgotten Cemetery, Wilderness", 2977, 3757, 0, 6), "Ankou", null,
				"1. Kill an Ankou (outlined) in the Forgotten Cemetery, or in the Wilderness Slayer Cave."))
			.build());
		out.add(task(t, 3, "Wilderness God Wars", "Enter the Wilderness Godwars Dungeon.")
			.stage(wildyGwd())
			.build());
		out.add(task(t, 8, "Bloodveld in the Wilderness God Wars", "Kill a Bloodveld in the Wilderness Godwars Dungeon.")
			.skill(Skill.SLAYER, 50)
			.note("60 Agility or 60 Strength, and a Saradomin and Zamorak item so the followers leave you alone")
			.stage(wildyGwd())
			.stage(DiaryStage.builder()
				.title("Kill a bloodveld")
				.inside(WILDY_GWD_ZONE)
				.npc("Bloodveld")
				.line("1. Kill a Bloodveld (outlined); the south-west corner is a safe spot.")
				.build())
			.build());
		out.add(task(t, 1, "Wilderness mithril", "Mine some Mithril ore in the wilderness.")
			.skill(Skill.MINING, 55)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Mine mithril in the Lava Maze", LAVA_MAZE, null, "Mithril rocks",
				"1. Mine the Mithril rocks (outlined) in the north-east of the Lava Maze, or in the Resource Area."))
			.build());
		out.add(task(t, 11, "Muddy chest", "Open the Muddy Chest in the lava maze.")
			.item(DiaryItem.of("Muddy key", ItemID.MUDDY_KEY))
			.item(DiaryItem.of("Knife (for the webs)", ItemID.KNIFE))
			.stage(simple("Open the muddy chest", Location.of("Muddy chest, Lava Maze", 3089, 3859, 0, 2), null, "Muddy chest",
				"1. Slash through the webs in the Lava Maze and use the Muddy key on the Muddy chest (outlined)."))
			.build());
		out.add(task(t, 4, "Wilderness agility course", "Complete a lap of the Wilderness Agility course.")
			.skill(Skill.AGILITY, 52)
			.stage(simple("Run a lap of the Wilderness course", Location.of("Wilderness agility course gate", 3004, 3936, 0, 3), null,
				"Gate", "1. Go through the course gate (outlined) and do each obstacle to the end. Pkers often wait here."))
			.build());
		out.add(task(t, 10, "Gold helmet in the Resource Area", "Smith a Gold helmet in the Resource Area.")
			.questStarted(Quest.BETWEEN_A_ROCK)
			.skill(Skill.SMITHING, 50)
			.item(DiaryItem.of("Gold bar", 3, ItemID.GOLD_BAR))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.item(DiaryItem.of(RESOURCE_FEE, 7_500, ItemID.COINS))
			.note("No gold helmet in your bank or bags")
			.stage(enterResourceArea())
			.stage(DiaryStage.builder()
				.title("Smith the gold helmet")
				.inside(RESOURCE_AREA_ZONE)
				.object("Anvil")
				.use(ItemID.GOLD_BAR)
				.line("1. Use a Gold bar on the Anvil (outlined) and make the Gold helmet.")
				.build())
			.build());
	}

	private static void hard(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.HARD;
		out.add(task(t, 2, "Charge an air orb", "Charge an Air Orb.")
			.skill(Skill.MAGIC, 66)
			.item(DiaryItem.of("Unpowered orb", ItemID.STAFFORB))
			.item(DiaryItem.of("Cosmic rune", 3, ItemID.COSMICRUNE))
			.item(DiaryItem.of("Air rune", 30, ItemID.AIRRUNE))
			.stage(simple("Charge the orb on the air obelisk", Location.of("Air obelisk, Wilderness level 7", 3088, 3570, 0, 2), null,
				"Obelisk of Air", "1. Cast Charge Air Orb on the Obelisk of Air (outlined) north of Edgeville."))
			.build());
		out.add(task(t, 8, "Trollheim to the Wilderness", "Take the agility shortcut from Trollheim into the Wilderness.")
			.questStarted(Quest.TROLL_STRONGHOLD)
			.skill(Skill.AGILITY, 64)
			.stage(DiaryStage.builder()
				.title("Climb the Trollheim Wilderness shortcut")
				.line("1. On Trollheim, take the rocky agility shortcut on the east side down into the Wilderness (it only goes one way).")
				.build())
			.build());
		out.add(task(t, 3, "Black salamander", "Catch a Black Salamander in the Wilderness.")
			.skill(Skill.HUNTER, 67)
			.item(DiaryItem.of("Small fishing net", ItemID.NET))
			.item(DiaryItem.of("Rope", ItemID.ROPE))
			.stage(simple("Set a net trap for a black salamander", Location.of("Black salamanders, east of the Bone Yard", 3293, 3673, 0, 4),
				null, "Young tree", "1. Set a net trap on a Young tree (outlined) and wait for a Black salamander."))
			.build());
		out.add(task(t, 5, "Lava dragon", "Kill a Lava Dragon.")
			.item(DiaryStages.DRAGONFIRE_SHIELD)
			.stage(simple("Kill a lava dragon", Location.of("Lava Dragon Isle", 3200, 3830, 0, 6), "Lava dragon", null,
				"1. Kill a Lava dragon (outlined) on Lava Dragon Isle. An anti-dragon shield blocks most dragonfire."))
			.build());
		out.add(task(t, 6, "Chaos Elemental", "Kill the Chaos Elemental.")
			.note("Boss gear; it unequips your items, so keep spare inventory space")
			.stage(simple("Kill the Chaos Elemental", Location.of("Chaos Elemental, deep Wilderness", 3260, 3920, 0, 8), "Chaos Elemental",
				null, "1. Kill the Chaos Elemental (outlined) west of the Rogues' Castle. It's in multi-combat, so watch for pkers."))
			.build());
		out.add(task(t, 7, "Crazy archaeologist, Chaos Fanatic and Scorpia", "Kill the Crazy Arc., Chaos Fanatic & Scorpia.")
			.note("Boss gear; doing another diary task in between resets it")
			.stage(simple("Kill the three bosses", Location.of("Crazy archaeologist, Ruins", 2975, 3700, 0, 6), "Crazy archaeologist",
				null, "1. Kill the Crazy archaeologist in the ruins west of the Forgotten Cemetery.",
				"2. Kill the Chaos Fanatic west of the Lava Maze.",
				"3. Kill Scorpia in the scorpion pit cave north of the Demonic ruins."))
			.build());
		out.add(task(t, 9, "Spiritual warrior in the Wilderness God Wars", "Kill a Spiritual warrior in the Wilderness Godwars Dungeon.")
			.skill(Skill.SLAYER, 68)
			.note("60 Agility or 60 Strength, and god items")
			.stage(wildyGwd())
			.stage(DiaryStage.builder()
				.title("Kill a spiritual warrior")
				.inside(WILDY_GWD_ZONE)
				.npc("Spiritual warrior")
				.line("1. Kill a Spiritual warrior (outlined).")
				.build())
			.build());
		out.add(task(t, 10, "Raw lava eel", "Fish some Raw Lava Eel in the Wilderness.")
			.skill(Skill.FISHING, 53).skill(Skill.HERBLORE, 25)
			.item(DiaryItem.owned("Oily fishing rod", "Use blamish oil (Herblore 25) on a fishing rod.", ItemID.OILY_FISHING_ROD))
			.item(DiaryItem.of("Fishing bait", 10, ItemID.FISHING_BAIT))
			.item(DiaryItem.of("Knife (for the webs)", ItemID.KNIFE))
			.stage(simple("Fish a lava eel in the Lava Maze", LAVA_MAZE, "Fishing spot", null,
				"1. Slash through the webs and Bait the Fishing spot (outlined) in the lava in the Lava Maze."))
			.build());
		out.add(task(t, 4, "Adamant scimitar in the Resource Area", "Smith an Adamant scimitar in the Resource Area.")
			.skill(Skill.SMITHING, 75)
			.item(DiaryItem.of("Adamantite bar", 2, ItemID.ADAMANTITE_BAR))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.item(DiaryItem.of(RESOURCE_FEE, 7_500, ItemID.COINS))
			.stage(enterResourceArea())
			.stage(DiaryStage.builder()
				.title("Smith the adamant scimitar")
				.inside(RESOURCE_AREA_ZONE)
				.object("Anvil")
				.use(ItemID.ADAMANTITE_BAR)
				.line("1. Use an Adamantite bar on the Anvil (outlined) and make a scimitar.")
				.build())
			.build());
		out.add(task(t, 1, "God spell on a player", "Cast any of the 3 God spells against another player in the Wilderness.")
			.quest(Quest.MAGE_ARENA_I)
			.skill(Skill.MAGIC, 60)
			.note("The god spell needs 100 casts in the Mage Arena first; it must hit another player")
			.stage(DiaryStage.builder()
				.title("Hit another player with a god spell")
				.line("1. Wield your god staff and cast its god spell on a willing player in the Wilderness until one hits.")
				.line("The Mage Arena itself counts too, and friends often help each other with this.")
				.build())
			.build());
	}

	private static void elite(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.ELITE;
		out.add(task(t, 2, "Ghorrock Teleport", "Teleport to Ghorrock.")
			.quest(Quest.DESERT_TREASURE_I)
			.skill(Skill.MAGIC, 96)
			.note("Ancient spellbook selected")
			.item(DiaryItem.of("Law rune", 2, ItemID.LAWRUNE))
			.item(DiaryItem.of("Water rune", 8, ItemID.WATERRUNE))
			.stage(DiaryStage.builder()
				.title("Cast Ghorrock Teleport")
				.line("1. Open the Ancient spellbook and click Ghorrock Teleport.")
				.build())
			.build());
		out.add(task(t, 6, "Spiritual mage in the Wilderness God Wars", "Slay a spiritual mage inside the wilderness Godwars Dungeon.")
			.skill(Skill.SLAYER, 83)
			.note("60 Agility or 60 Strength, and god items")
			.stage(wildyGwd())
			.stage(DiaryStage.builder()
				.title("Kill a spiritual mage")
				.inside(WILDY_GWD_ZONE)
				.npc("Spiritual mage")
				.line("1. Kill a Spiritual mage (outlined).")
				.build())
			.build());
		out.add(task(t, 5, "Rogues' chest", "Steal from the Rogues' chest.")
			.skill(Skill.THIEVING, 84)
			.stage(simple("Steal from the rogues' chest", Location.of("Rogues' Castle, deep Wilderness", 3297, 3940, 0, 3), null, "Chest",
				"1. Right-click a Chest (outlined) in the Rogues' Castle and choose Search for traps. The rogues will attack you."))
			.build());
		out.add(task(t, 4, "Rune scimitar from scratch", "Smith a rune scimitar from scratch in the Resource Area.")
			.skill(Skill.MINING, 85).skill(Skill.SMITHING, 90)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.item(DiaryItem.of("Coal", 16, ItemID.COAL))
			.item(DiaryItem.of(RESOURCE_FEE, 7_500, ItemID.COINS))
			.stage(enterResourceArea())
			.stage(DiaryStage.builder()
				.title("Kill and mine two runite golems")
				.inside(RESOURCE_AREA_ZONE)
				.npc("Runite Golem").object("Furnace").object("Anvil")
				.line("1. Kill a Runite Golem (outlined) and mine the rocks it leaves; do it twice for 2 runite ore.")
				.line("2. Smelt both ores at the Furnace (no Superheat), then smith the scimitar on the Anvil.")
				.build())
			.build());
		out.add(task(t, 3, "Dark crab", "Fish and Cook a Dark Crab in the Resource Area.")
			.skill(Skill.FISHING, 85).skill(Skill.COOKING, 90)
			.item(DiaryItem.of("Lobster pot", ItemID.LOBSTER_POT))
			.item(DiaryItem.of("Dark fishing bait", 10, ItemID.WILDERNESS_FISHING_BAIT))
			.note(SAME_TRIP)
			.stage(enterResourceArea())
			.stage(DiaryStage.builder()
				.title("Catch and cook a dark crab")
				.inside(RESOURCE_AREA_ZONE)
				.npc("Fishing spot").object("Fire")
				.line("1. Cage the Fishing spot (outlined) inside the Resource Area until you get a Raw dark crab.")
				.line("2. Cook it on the fire in the Resource Area. If it burns, catch another.")
				.build())
			.build());
		out.add(task(t, 7, "Magic logs in the Resource Area", "Cut and burn some magic logs in the Resource Area.")
			.skill(Skill.WOODCUTTING, 75).skill(Skill.FIREMAKING, 75)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DiaryItem.of("Tinderbox", ItemID.TINDERBOX))
			.note(SAME_TRIP)
			.stage(enterResourceArea())
			.stage(DiaryStage.builder()
				.title("Chop and burn magic logs")
				.inside(RESOURCE_AREA_ZONE)
				.object("Magic tree")
				.use(ItemID.TINDERBOX).use(ItemID.MAGIC_LOGS)
				.line("1. Chop the Magic tree (outlined) for a log, then use the Tinderbox on it. No infernal axe.")
				.build())
			.build());
		out.add(task(t, 1, "Callisto, Venenatis and Vet'ion", "Kill Callisto, Venenatis & Vet'ion.")
			.note("Boss gear; Artio, Spindel and Calvar'ion count too; another diary task in between resets it")
			.stage(DiaryStage.builder()
				.title("Kill the three wilderness bosses")
				.line("1. Kill Callisto (or Artio), Venenatis (or Spindel) and Vet'ion (or Calvar'ion) in any order.")
				.line("The lesser versions are safer and need the hard Wilderness diary reward claimed.")
				.build())
			.build());
	}

	private static DiaryTask.DiaryTaskBuilder task(DiaryTier tier, int number, String name, String text)
	{
		int index = BITS.get(tier)[number - 1];
		return DiaryTask.builder()
			.region(DiaryRegion.WILDERNESS)
			.tier(tier)
			.number(number)
			.name(name)
			.text(text)
			.wilderness(!(tier == DiaryTier.MEDIUM && number == 9))
			.varp(index < 32 ? VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY : VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2)
			.bit(index % 32);
	}
}
