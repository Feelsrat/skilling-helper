package com.skillinghelper.diary.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.ToolType;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;

/**
 * Kandarin diary, every tier. Task wording and requirements from the OSRS Wiki, listed in walking order.
 */
final class KandarinDiary
{
	private static final Location CATHERBY_FISH = Location.of("Catherby fishing spots", 2837, 3431, 0, 4);
	private static final Location CATHERBY_RANGE = Location.of("Range in Catherby", 2817, 3444, 0, 2);
	private static final Location OTTO = Location.of("Otto's Grotto, Baxtorian Falls", 2501, 3490, 0, 4);
	private static final Location BA = Location.of("Barbarian Assault, Barbarian Outpost", 2534, 3572, 0, 3);
	private static final Zone SEERS = Zone.of(2682, 3455, 2742, 3510, 0);
	private static final Zone SEERS_BANK = Zone.of(2721, 3490, 2730, 3495, 0);
	private static final DiaryItem RAKE = DiaryItem.of("Rake", ItemID.RAKE);
	private static final DiaryItem DIBBER = DiaryItem.of("Seed dibber", ItemID.DIBBER);
	private static final int[] BOWS = {ItemID.MAGIC_LONGBOW, ItemID.MAGIC_SHORTBOW, ItemID.YEW_LONGBOW,
		ItemID.YEW_SHORTBOW, ItemID.MAPLE_LONGBOW, ItemID.MAPLE_SHORTBOW, ItemID.WILLOW_LONGBOW, ItemID.WILLOW_SHORTBOW,
		ItemID.OAK_LONGBOW, ItemID.OAK_SHORTBOW, ItemID.LONGBOW, ItemID.SHORTBOW};

	private KandarinDiary()
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
		out.add(task(t, 10, "Speak to Sherlock", "Speak to Sherlock.")
			.stage(simple("Talk to Sherlock", Location.of("Sherlock's house, south of Seers' Village", 2735, 3413, 0, 3),
				"Sherlock", null, "1. Sherlock lives south of Seers' Village, by the Camelot road. Talk to him (outlined)."))
			.build());
		out.add(task(t, 3, "5 flax", "Collect five Flax from the Seers' flax fields.")
			.stage(simple("Pick 5 flax", Location.of("Flax field, Seers' Village", 2742, 3446, 0, 5), null, "Flax",
				"1. Pick the Flax (outlined) in the field south-east of Seers' Village 5 times."))
			.build());
		out.add(task(t, 4, "Seers' church organ", "Play the Organ in Seers' church.")
			.stage(simple("Play the church organ", Location.of("Seers' Village church", 2692, 3463, 0, 2), null, "Organ",
				"1. Click Play on the Organ (outlined) in the church in south Seers' Village."))
			.build());
		out.add(task(t, 9, "Seers' pub stew", "Buy a Stew from the Seers' pub.")
			.item(DiaryItem.of("Coins", 20, ItemID.COINS))
			.stage(simple("Buy a stew from the bartender", Location.of("Forester's Arms, Seers' Village", 2691, 3494, 0, 3),
				"Bartender", null, "1. Talk to the Bartender (outlined) in the Forester's Arms and buy a stew."))
			.build());
		out.add(task(t, 6, "Galahad's tea", "Have Galahad make you a cup of tea.")
			.stage(simple("Ask Galahad for tea", Location.of("Galahad's house, west of McGrubor's Wood", 2612, 3474, 0, 3),
				"Brother Galahad", null,
				"1. Galahad's house is west of Seers' Village, near the Fishing Guild road.",
				"2. Talk to Brother Galahad (outlined) and accept the tea. Drink it after."))
			.build());
		out.add(task(t, 5, "Plant jute", "Plant some Jute seeds in the patch north of McGrubor's Wood.")
			.skill(Skill.FARMING, 13)
			.item(DiaryItem.of("Jute seed", 3, ItemID.JUTE_SEED))
			.item(RAKE).item(DIBBER)
			.stage(simple("Plant jute in the hops patch", Location.of("Hops patch north of McGrubor's Wood", 2669, 3523, 0, 3),
				"Rhonen", "Hops Patch",
				"1. Rake the Hops patch (outlined) next to Rhonen.",
				"2. Use the 3 Jute seeds on it. Planting is enough."))
			.build());
		out.add(task(t, 11, "Coal truck log", "Cross the Coal truck log shortcut.")
			.skill(Skill.AGILITY, 20)
			.stage(simple("Walk across the log", Location.of("Log by the coal trucks", 2602, 3477, 0, 2), null, "Log balance",
				"1. Click Walk-across on the Log balance (outlined) over the river at the coal trucks."))
			.build());
		out.add(task(t, 7, "Workshop elementals", "Defeat one of each elemental in the workshop.")
			.questStarted(Quest.ELEMENTAL_WORKSHOP_I)
			.item(DiaryItem.owned("Battered key", "Found in Elemental Workshop I; search the bookcase in Seers' "
				+ "Village's north-east house for another.", ItemID.ELEMENTAL_WORKSHOP_KEY))
			.stage(simple("Climb down into the workshop", Location.of("Elemental Workshop staircase, Seers' Village", 2711, 3498, 0, 2),
				null, "Staircase", "1. Go through the odd-looking wall in the house east of the bank, then down the Staircase (outlined)."))
			.stage(DiaryStage.builder()
				.title("Kill an air, earth, fire and water elemental")
				.inside(Zone.of(2682, 9862, 2747, 9927, 0))
				.npc("Air elemental").npc("Earth elemental").npc("Fire elemental").npc("Water elemental")
				.line("1. Kill one of each elemental (outlined) in the workshop rooms.")
				.line("Only the roaming earth elementals count, not the ones in the rocks.")
				.build())
			.build());
		out.add(task(t, 2, "Catherby candle", "Buy a candle from the Chandler in Catherby.")
			.item(DiaryItem.of("Coins", 3, ItemID.COINS))
			.stage(simple("Buy a candle", Location.of("Candle shop, Catherby", 2799, 3439, 0, 3), "Candle maker", null,
				"1. Trade the Candle maker (outlined) in west Catherby and buy one candle."))
			.build());
		out.add(task(t, 1, "Catherby mackerel", "Catch a Mackerel at Catherby.")
			.skill(Skill.FISHING, 16)
			.item(DiaryItem.of("Big fishing net", ItemID.BIG_NET))
			.stage(simple("Catch a mackerel", CATHERBY_FISH, "Fishing spot", null,
				"1. Click Net on a Fishing spot (outlined) on Catherby beach with the big net until you get a Mackerel."))
			.build());
		out.add(task(t, 8, "Pet fish from Harry", "Get a pet fish from Harry in Catherby.")
			.item(DiaryItem.owned("Fishbowl", "Blow one at 42 Crafting from molten glass, or buy one on the Grand "
				+ "Exchange.", ItemID.FISHBOWL_WATER_WEED, ItemID.FISHBOWL_WATER, ItemID.FISHBOWL_EMPTY))
			.item(DiaryItem.of("Seaweed", ItemID.SEAWEED))
			.item(DiaryItem.of("Coins", 10, ItemID.COINS))
			.stage(simple("Get a fish from Harry", Location.of("Harry's fishing shop, Catherby", 2833, 3443, 0, 3), "Harry",
				"Aquarium",
				"1. Fill the bowl at the Sink in Harry's shop, then use the Seaweed on it.",
				"2. Talk to Harry (outlined) and ask for a fish (10 coins), then use the bowl on the Aquarium (outlined)."))
			.build());
	}

	private static void medium(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.MEDIUM;
		out.add(task(t, 6, "Camelot Teleport", "Teleport to Camelot.")
			.skill(Skill.MAGIC, 45)
			.item(DiaryItem.of("Law rune", 1, ItemID.LAWRUNE))
			.item(DiaryItem.of("Air rune", 5, ItemID.AIRRUNE))
			.stage(DiaryStage.builder()
				.title("Cast Camelot Teleport")
				.line("1. Open the spellbook and click Camelot Teleport.")
				.build())
			.build());
		out.add(task(t, 13, "Fairy ring to McGrubor's Wood", "Travel to McGrubor's Wood by Fairy Ring.")
			.questStarted(Quest.FAIRYTALE_II__CURE_A_QUEEN)
			.item(DiaryStages.FAIRY_STAFF)
			.stages(DiaryStages.fairyRing("ALS", "You arrive at McGrubor's Wood, which finishes it."))
			.build());
		out.add(task(t, 7, "String a maple shortbow", "String a Maple shortbow in Seers' Village bank.")
			.skill(Skill.FLETCHING, 50)
			.item(DiaryItem.of("Maple shortbow (u)", ItemID.UNSTRUNG_MAPLE_SHORTBOW))
			.item(DiaryItem.of("Bow string", ItemID.BOW_STRING))
			.stage(DiaryStage.builder()
				.title("String the bow inside Seers' bank")
				.at(Location.of("Seers' Village bank", 2725, 3492, 0, 2))
				.inside(SEERS_BANK)
				.use(ItemID.BOW_STRING).use(ItemID.UNSTRUNG_MAPLE_SHORTBOW)
				.line("1. Stand inside Seers' Village bank.")
				.line("2. Use the Bow string on the Maple shortbow (u) (both outlined).")
				.build())
			.build());
		out.add(task(t, 2, "Super antipoison from scratch", "Create a Super Antipoison potion from scratch in the Seers/Catherby Area.")
			.skill(Skill.HERBLORE, 48)
			.item(DiaryItem.of("Vial of water", ItemID.VIAL_WATER))
			.item(DiaryItem.of("Irit leaf", ItemID.IRIT_LEAF))
			.item(DiaryItem.of("Unicorn horn dust", ItemID.UNICORN_HORN_DUST))
			.stage(DiaryStage.builder()
				.title("Make the unfinished potion in Seers' Village")
				.at(Location.of("Seers' Village", 2725, 3485, 0, 6))
				.use(ItemID.IRIT_LEAF).use(ItemID.VIAL_WATER)
				.line("1. Between Seers' Village and Catherby, use the Irit leaf on the Vial of water.")
				.line("Doing another diary task before you finish resets this one.")
				.build())
			.stage(DiaryStage.builder()
				.title("Add the unicorn horn dust")
				.carrying(ItemID.IRITVIAL)
				.started(true)
				.use(ItemID.UNICORN_HORN_DUST).use(ItemID.IRITVIAL)
				.line("1. Use the Unicorn horn dust on the Irit potion (unf) (both outlined).")
				.build())
			.build());
		out.add(task(t, 14, "Coal by the trucks", "Mine some coal near the coal trucks.")
			.skill(Skill.MINING, 30)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Mine coal at the coal trucks", Location.of("Coal rocks by the coal trucks", 2590, 3476, 0, 3), null,
				"Coal rocks", "1. Mine the Coal rocks (outlined) west of Seers' Village until you get one coal."))
			.build());
		out.add(task(t, 3, "Ranging Guild", "Enter the Ranging guild.")
			.skill(Skill.RANGED, 40)
			.stage(simple("Walk into the Ranging Guild", Location.of("Ranging Guild door", 2658, 3438, 0, 2), null, "Guild door",
				"1. Open the Guild door (outlined) and walk in."))
			.build());
		out.add(task(t, 12, "Hemenster chest", "Steal from the chest in Hemenster.")
			.skill(Skill.THIEVING, 47)
			.item(DiaryItem.of("Lockpick", ItemID.LOCKPICK))
			.stage(simple("Picklock the Hemenster chest", Location.of("Chest house, Hemenster", 2639, 3424, 0, 2), null, "Chest",
				"1. The chest is in the house between the range and the anvil in Hemenster.",
				"2. Right-click the Chest (outlined) and choose Pick-lock."))
			.build());
		out.add(task(t, 8, "Catherby limpwurt", "Pick some Limpwurt root from the farming patch in Catherby.")
			.skill(Skill.FARMING, 26)
			.item(DiaryItem.of("Limpwurt seed", ItemID.LIMPWURT_SEED))
			.item(RAKE).item(DIBBER)
			.stage(simple("Grow limpwurt in the Catherby flower patch", Location.of("Flower patch, Catherby", 2810, 3464, 0, 3),
				null, "Flower Patch",
				"1. Rake the Flower patch (outlined) north of Catherby and plant the Limpwurt seed.",
				"2. Come back in about 20 minutes and click Pick."))
			.build());
		out.add(task(t, 5, "Catch and cook a bass", "Catch and cook a Bass in Catherby.")
			.skill(Skill.FISHING, 46).skill(Skill.COOKING, 43)
			.item(DiaryItem.of("Big fishing net", ItemID.BIG_NET))
			.stage(simple("Catch a bass", CATHERBY_FISH, "Fishing spot", null,
				"1. Net at the big net Fishing spots (outlined) until you catch a Raw bass.",
				"Doing another diary task before you cook it resets this one."))
			.stage(DiaryStage.builder()
				.title("Cook the bass in Catherby")
				.carrying(ItemID.RAW_BASS)
				.at(CATHERBY_RANGE)
				.object("Range")
				.use(ItemID.RAW_BASS)
				.line("1. Use the Raw bass on the Range (outlined) in Catherby. Catch another if it burns.")
				.build())
			.build());
		out.add(task(t, 4, "Obelisk grapple shortcut", "Use the grapple shortcut to get from the water obelisk to Catherby shore.")
			.skill(Skill.AGILITY, 36).skill(Skill.STRENGTH, 22).skill(Skill.RANGED, 39)
			.item(DiaryStages.MITH_GRAPPLE)
			.item(DiaryStages.ANY_CROSSBOW)
			.note("To reach the island: the Dusty key (Taverley Dungeon) or 70 Agility")
			.stage(simple("Go down into Taverley Dungeon", Location.of("Taverley Dungeon ladder", 2884, 3397, 0, 2), null,
				"Ladder", "1. Climb-down the Ladder (outlined) south of Taverley."))
			.stage(DiaryStage.builder()
				.title("Climb up to the water obelisk island")
				.inside(Zone.of(2813, 9669, 2972, 9857, 0))
				.at(Location.of("Ladder to the water obelisk", 2842, 9824, 0, 2))
				.object("Ladder")
				.line("1. Go through the gate with the Dusty key (or the pipe at 70 Agility) past the blue dragons.")
				.line("2. Climb-up the Ladder (outlined) to the obelisk island.")
				.build())
			.stage(DiaryStage.builder()
				.title("Grapple to the Catherby shore")
				.inside(Zone.of(2833, 3415, 2849, 3427, 0))
				.at(Location.of("Tree on the obelisk island", 2842, 3435, 0, 2))
				.object("Tree")
				.line("1. Wield the crossbow and Mith grapple.")
				.line("2. Click Grapple on the Tree (outlined) on the north shore.")
				.build())
			.build());
		out.add(task(t, 1, "Barbarian rooftops", "Complete a lap of the Barbarian agility course.")
			.quest(Quest.ALFRED_GRIMHANDS_BARCRAWL)
			.skill(Skill.AGILITY, 35)
			.stage(simple("Run a lap of the Barbarian Outpost course", Location.of("Barbarian Outpost agility course", 2552, 3560, 0, 2),
				null, "Obstacle pipe",
				"1. Squeeze through the Obstacle pipe (outlined), then cross each obstacle in turn.",
				"2. Finishing over the crumbling walls completes a lap."))
			.build());
		out.add(task(t, 11, "Barbarian Assault wave", "Complete a wave of Barbarian Assault.")
			.note("Barbarian Assault tutorial done, and a team (join a minigame world)")
			.stage(simple("Play a wave of Barbarian Assault", BA, "Captain Cain", "Ladder",
				"1. Go to world 309 or another Barbarian Assault world.",
				"2. Join a team in the lobby (climb the ladder, outlined) and finish one wave."))
			.build());
		out.add(task(t, 10, "Waterfall fire giant", "Kill a Fire Giant inside Baxtorian Waterfall.")
			.questStarted(Quest.WATERFALL_QUEST)
			.item(DiaryItem.of("Rope", ItemID.ROPE))
			.note("Glarial's amulet if Waterfall Quest isn't finished; food for level 86 fire giants")
			.stage(simple("Get into the waterfall dungeon", Location.of("Baxtorian Falls", 2511, 3464, 0, 3), null, "Ledge",
				"1. Raft or rope your way to the ledge behind the falls and open the door (outlined)."))
			.stage(DiaryStage.builder()
				.title("Kill a fire giant")
				.inside(Zone.of(2534, 9860, 2613, 9918, 0))
				.npc("Fire giant")
				.line("1. Walk to the fire giants (outlined) at the back and kill one.")
				.build())
			.build());
		out.add(task(t, 9, "Mind helmet", "Create a Mind helmet.")
			.questStarted(Quest.ELEMENTAL_WORKSHOP_II)
			.stage(DiaryStage.builder()
				.title("Make the mind helmet in Elemental Workshop II")
				.line("1. This happens during Elemental Workshop II: follow the quest until you make the Mind helmet.")
				.build())
			.build());
	}

	private static void hard(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.HARD;
		out.add(task(t, 10, "Seers' fancy stone", "Have the Seers' estate agent decorate your house with Fancy Stone.")
			.skill(Skill.CONSTRUCTION, 50).unboostable(Skill.CONSTRUCTION)
			.item(DiaryItem.of("Coins", 25_000, ItemID.COINS))
			.stage(simple("Ask the Seers' estate agent for Fancy Stone", Location.of("Estate agent, Seers' Village", 2735, 3500, 0, 3),
				"Estate agent", null,
				"1. Talk to the Estate agent (outlined) and choose to redecorate in Fancy stone.",
				"2. Finish the whole conversation. Already Fancy stone? Change style and back (5,000 more)."))
			.build());
		out.add(task(t, 4, "Piety in the courthouse", "Enter the Seers' Village courthouse with piety turned on.")
			.quest(Quest.KINGS_RANSOM)
			.skill(Skill.PRAYER, 70).unboostable(Skill.PRAYER)
			.skill(Skill.DEFENCE, 70).unboostable(Skill.DEFENCE)
			.note("Piety unlocked (Knight Waves Training Grounds)")
			.stage(simple("Walk into the courthouse with Piety on", Location.of("Seers' courthouse", 2735, 3470, 0, 3), null, "Door",
				"1. Turn on Piety, then open the courthouse Door (outlined) in south Seers' Village and step in."))
			.build());
		out.add(task(t, 2, "Seers' rooftops", "Complete a lap of the Seers' Village agility course.")
			.skill(Skill.AGILITY, 60)
			.stage(simple("Run a lap of the Seers' rooftops", Location.of("Seers' rooftop course start", 2729, 3489, 0, 2), null,
				"Wall", "1. Climb the Wall (outlined) by the bank, then cross each obstacle to the end."))
			.build());
		out.add(task(t, 3, "Yew longbow from scratch", "Create a Yew Longbow from scratch around Seers' Village.")
			.skill(Skill.WOODCUTTING, 60).skill(Skill.FLETCHING, 70).skill(Skill.CRAFTING, 10)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DiaryItem.of("Knife", ItemID.KNIFE))
			.item(DiaryItem.of("Flax", ItemID.FLAX))
			.stage(simple("Chop a yew in Seers' Village", Location.of("Yew tree, Seers' Village", 2715, 3460, 0, 3), null,
				"Yew tree",
				"1. Chop the Yew tree (outlined) south of the church until you get Yew logs.",
				"Doing any other diary task before the bow is done resets this one."))
			.stage(DiaryStage.builder()
				.title("Cut the logs into a longbow")
				.carrying(ItemID.YEW_LOGS)
				.started(true)
				.use(ItemID.KNIFE).use(ItemID.YEW_LOGS)
				.line("1. Use the Knife on the Yew logs and make a Yew longbow (u).")
				.build())
			.stage(DiaryStage.builder()
				.title("Spin a bow string and string the bow")
				.carrying(ItemID.UNSTRUNG_YEW_LONGBOW)
				.started(true)
				.at(Location.of("Spinning wheel, Seers' Village", 2711, 3471, 1, 2))
				.object("Spinning wheel")
				.use(ItemID.FLAX)
				.line("1. Spin the Flax into a Bow string on the Spinning wheel (outlined) upstairs in the house by the church.")
				.line("2. Use the Bow string on the Yew longbow (u).")
				.build())
			.build());
		out.add(task(t, 6, "Burn maple with a bow", "Burn some Maple logs with a bow in Seers' Village.")
			.skill(Skill.FIREMAKING, 65)
			.note("Barbarian Firemaking learned (Otto's Grotto)")
			.item(DiaryItem.of("Maple logs", ItemID.MAPLE_LOGS))
			.item(DiaryItem.anyOf("Any bow", 1, ItemID.SHORTBOW, BOWS))
			.stage(DiaryStage.builder()
				.title("Light maple logs with a bow in Seers' Village")
				.at(Location.of("Seers' Village", 2725, 3485, 0, 6))
				.inside(SEERS)
				.use(ItemID.MAPLE_LOGS)
				.line("1. Stand in Seers' Village and use the bow on the Maple logs.")
				.build())
			.build());
		out.add(task(t, 5, "Charge a water orb", "Charge a Water Orb.")
			.skill(Skill.MAGIC, 56)
			.item(DiaryItem.of("Unpowered orb", ItemID.STAFFORB))
			.item(DiaryItem.of("Cosmic rune", 3, ItemID.COSMICRUNE))
			.item(DiaryItem.of("Water rune", 30, ItemID.WATERRUNE))
			.note("Dusty key or 70 Agility to reach the obelisk; anti-dragon shield recommended")
			.stage(simple("Go down into Taverley Dungeon", Location.of("Taverley Dungeon ladder", 2884, 3397, 0, 2), null,
				"Ladder", "1. Climb-down the Ladder (outlined) south of Taverley."))
			.stage(DiaryStage.builder()
				.title("Climb up to the water obelisk")
				.inside(Zone.of(2813, 9669, 2972, 9857, 0))
				.at(Location.of("Ladder to the water obelisk", 2842, 9824, 0, 2))
				.object("Ladder")
				.line("1. Past the blue dragons, Climb-up the Ladder (outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Charge the orb")
				.inside(Zone.of(2833, 3415, 2849, 3427, 0))
				.object("Obelisk of Water")
				.line("1. Cast Charge Water Orb on the Obelisk of Water (outlined).")
				.build())
			.build());
		out.add(task(t, 1, "Leaping sturgeon", "Catch a Leaping Sturgeon.")
			.skill(Skill.FISHING, 70).skill(Skill.AGILITY, 45).skill(Skill.STRENGTH, 45)
			.note("Barbarian Fishing learned from Otto")
			.item(DiaryItem.owned("Barbarian rod", "Search Otto's bed in his grotto after talking to him about Barbarian Fishing.",
				ItemID.BRUT_FISHING_ROD))
			.item(DiaryItem.of("Feather", 30, ItemID.FEATHER))
			.stage(simple("Catch a leaping sturgeon at Otto's Grotto", Location.of("Fishing spots, Otto's Grotto", 2501, 3504, 0, 4),
				"Fishing spot", null, "1. Use-rod on the Fishing spots (outlined) by Otto's Grotto until you get a sturgeon."))
			.build());
		out.add(task(t, 11, "Adamant spear at Otto's", "Smith an Adamant spear at Otto's Grotto.")
			.skill(Skill.SMITHING, 75)
			.note("Barbarian Smithing learned from Otto")
			.item(DiaryItem.of("Adamantite bar", ItemID.ADAMANTITE_BAR))
			.item(DiaryItem.of("Yew logs", ItemID.YEW_LOGS))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.stage(simple("Smith the spear on Otto's anvil", OTTO, null, "Barbarian anvil",
				"1. Use the Adamantite bar on the Barbarian anvil (outlined) by Otto's Grotto and pick the spear."))
			.build());
		out.add(task(t, 8, "Mithril dragon", "Kill a Mithril Dragon.")
			.note("Barbarian Firemaking learned (to enter the Ancient Cavern); gear for mithril dragons")
			.item(DiaryStages.DRAGONFIRE_SHIELD)
			.stage(simple("Dive into the whirlpool", Location.of("Whirlpool, Baxtorian Falls lake", 2512, 3508, 0, 2), null,
				"Whirlpool", "1. Click Dive-in on the Whirlpool (outlined) near Otto's Grotto."))
			.stage(DiaryStage.builder()
				.title("Kill a mithril dragon")
				.inside(Zone.anyFloor(1733, 5279, 1792, 5373))
				.at(Location.of("Mithril dragons, Ancient Cavern", 1779, 5344, 1, 4))
				.object("Stairs")
				.npc("Mithril dragon")
				.line("1. Climb the stairs (outlined) up to the mithril dragons.")
				.line("2. Wear your anti-dragon shield and kill one (outlined).")
				.build())
			.build());
		out.add(task(t, 7, "Shadow hound", "Kill a Shadow Hound in the Shadow dungeon.")
			.questStarted(Quest.DESERT_TREASURE_I)
			.skill(Skill.THIEVING, 53)
			.item(DiaryItem.owned("Ring of visibility", "From Desert Treasure I (Rasolo); or a Ring of shadows.",
				ItemID.RING_OF_SHADOWS, ItemID.RING_OF_SHADOWS_UNCHARGED, ItemID.FD_RING_VISIBILITY))
			.stage(simple("Climb into the Shadow dungeon", Location.of("Shadow dungeon ladder, south of Baxtorian Falls", 2547, 3421, 0, 2),
				null, "Ladder", "1. Wear the ring and Climb-down the Ladder (outlined)."))
			.stage(DiaryStage.builder()
				.title("Kill a shadow hound")
				.inside(Zone.of(2621, 5054, 2754, 5121, 0))
				.npc("Shadow hound")
				.line("1. Keep the ring on and kill a Shadow hound (outlined).")
				.build())
			.build());
		out.add(task(t, 9, "Granite body", "Purchase and equip a granite body from Barbarian Assault.")
			.note("Penance Queen killed; 95,000 coins and honour points for the body")
			.item(DiaryItem.of("Coins", 95_000, ItemID.COINS))
			.stage(simple("Buy and wear a granite body", BA, "Commander Connad", null,
				"1. Talk to Commander Connad (outlined) under the Barbarian Assault lobby and buy the Granite body.",
				"2. Wear it right away; doing another diary task in between resets this one."))
			.build());
	}

	private static void elite(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.ELITE;
		out.add(task(t, 7, "Catherby Teleport", "Teleport to Catherby.")
			.quest(Quest.LUNAR_DIPLOMACY)
			.skill(Skill.MAGIC, 87)
			.note("Lunar spellbook selected")
			.item(DiaryItem.of("Law rune", 3, ItemID.LAWRUNE))
			.item(DiaryItem.of("Astral rune", 3, ItemID.ASTRALRUNE))
			.item(DiaryItem.of("Water rune", 10, ItemID.WATERRUNE))
			.stage(DiaryStage.builder()
				.title("Cast Catherby Teleport")
				.line("1. Open the Lunar spellbook and click Catherby Teleport.")
				.build())
			.build());
		out.add(task(t, 2, "Catherby dwarf weed", "Pick some Dwarf weed from the herb patch at Catherby.")
			.skill(Skill.FARMING, 79)
			.item(DiaryItem.of("Dwarf weed seed", ItemID.DWARF_WEED_SEED))
			.item(DiaryItem.of("Spade", ItemID.SPADE)).item(RAKE).item(DIBBER)
			.stage(simple("Grow dwarf weed in the Catherby herb patch", Location.of("Herb patch, Catherby", 2814, 3464, 0, 3),
				null, "Herb patch",
				"1. Rake the Herb patch (outlined) north of Catherby and plant the Dwarf weed seed. Ultracompost helps.",
				"2. Come back in about 80 minutes and click Pick."))
			.build());
		out.add(task(t, 3, "5 sharks in Catherby", "Fish and Cook 5 Sharks in Catherby using the Cooking gauntlets.")
			.skill(Skill.FISHING, 76).skill(Skill.COOKING, 80)
			.quest(Quest.FAMILY_CREST)
			.item(DiaryItem.of("Harpoon", ItemID.HARPOON))
			.item(DiaryItem.owned("Cooking gauntlets", "Reward from Family Crest; Avan's family sells the gauntlets back.",
				ItemID.GAUNTLETS_OF_COOKING))
			.stage(simple("Catch 5 sharks in Catherby", CATHERBY_FISH, "Fishing spot", null,
				"1. Harpoon at the Fishing spots (outlined) on Catherby beach until you have 5 Raw sharks."))
			.stage(DiaryStage.builder()
				.title("Cook the sharks in Catherby")
				.carrying(ItemID.RAW_SHARK)
				.at(CATHERBY_RANGE)
				.object("Range")
				.use(ItemID.RAW_SHARK)
				.line("1. Wear the Cooking gauntlets.")
				.line("2. Use the sharks on the Range (outlined) between the bank and the archery shop.")
				.build())
			.build());
		out.add(task(t, 4, "Stamina mix on the bank roof", "Mix a Stamina Mix on top of the Seers' Village bank.")
			.skill(Skill.HERBLORE, 86).skill(Skill.AGILITY, 60)
			.note("Barbarian Herblore learned from Otto")
			.item(DiaryItem.of("Stamina potion(2)", ItemID._2DOSESTAMINA))
			.item(DiaryItem.of("Caviar", ItemID.BRUT_CAVIAR))
			.stage(simple("Climb onto the bank roof", Location.of("Seers' rooftop course start", 2729, 3489, 0, 2), null, "Wall",
				"1. Climb the Wall (outlined) that starts the Seers' rooftop course; it puts you on the bank roof."))
			.stage(DiaryStage.builder()
				.title("Mix the stamina mix")
				.inside(Zone.of(2721, 3490, 2730, 3495, 3))
				.use(ItemID.BRUT_CAVIAR).use(ItemID._2DOSESTAMINA)
				.line("1. Use the Caviar on the Stamina potion(2) (both outlined).")
				.build())
			.build());
		out.add(task(t, 5, "Rune hasta at Otto's", "Smith a Rune Hasta at Otto's Grotto.")
			.skill(Skill.SMITHING, 90)
			.note("Barbarian Smithing learned from Otto")
			.item(DiaryItem.of("Runite bar", ItemID.RUNITE_BAR))
			.item(DiaryItem.of("Magic logs", ItemID.MAGIC_LOGS))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.stage(simple("Smith the hasta on Otto's anvil", OTTO, null, "Barbarian anvil",
				"1. Use the Runite bar on the Barbarian anvil (outlined) and pick the hasta."))
			.build());
		out.add(task(t, 6, "Magic pyre ship", "Construct a Pyre ship from Magic Logs.")
			.skill(Skill.FIREMAKING, 85).skill(Skill.CRAFTING, 85)
			.note("Barbarian Firemaking learned from Otto")
			.item(DiaryItem.of("Magic logs", ItemID.MAGIC_LOGS))
			.item(DiaryItem.owned("Chewed bones", "Dropped by monsters in the Ancient Cavern (through the whirlpool).",
				ItemID.BRUT_BARBARIAN_BONES))
			.item(DiaryItem.of("Tinderbox", ItemID.TINDERBOX))
			.item(DiaryItem.tool(ToolType.AXE))
			.stage(simple("Build and light a pyre ship", Location.of("Burnt ground by the lake, Otto's Grotto", 2519, 3519, 0, 3),
				null, "Burnt ground",
				"1. Use the Magic logs on the burnt ground (outlined) by the lake.",
				"2. Use the Chewed bones on the pyre and light it."))
			.build());
		out.add(task(t, 1, "Barbarian Assault level 5", "Read the Blackboard at Barbarian Assault after reaching level 5 in every role.")
			.note("Level 5 in all four roles (about 5,600 honour points)")
			.stage(simple("Read the Barbarian Assault blackboard", BA, null, "Blackboard",
				"1. Click Read on the Blackboard (outlined) at the Barbarian Assault entrance."))
			.build());
	}

	private static DiaryTask.DiaryTaskBuilder task(DiaryTier tier, int number, String name, String text)
	{
		int[] offsets = {0, 11, 25, 36};
		int index = offsets[tier.ordinal()] + number;
		return DiaryTask.builder()
			.region(DiaryRegion.KANDARIN)
			.tier(tier)
			.number(number)
			.name(name)
			.text(text)
			.varp(index < 32 ? VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY : VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2)
			.bit(index % 32);
	}
}
