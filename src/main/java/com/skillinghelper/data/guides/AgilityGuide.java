package com.skillinghelper.data.guides;

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
 * Based on the OSRS wiki "Agility training" guide. Lap xp matches RuneLite's Agility plugin; course start points match
 * RuneLite's world map. Obstacles are highlighted by name, so every obstacle on the course lights up.
 */
final class AgilityGuide
{
	private static final String WIKI = "Agility_training";
	private static final String MARKS = "Pick up marks of grace as they appear and trade them to the Grace for the "
		+ "Graceful outfit (lighter weight, faster run energy).";

	static List<Method> methods()
	{
		return List.of(
			course("agility_gnome", "Gnome Stronghold Agility Course", 1, 86.5, 9_000, 2474, 3436,
				"Log balance", "Obstacle net", "Tree branch", "Balancing rope", "Obstacle pipe")
				.notes("No marks of grace here, but it's the only course before level 10.")
				.build(),
			course("agility_draynor", "Draynor Village Rooftop", 10, 120, 10_000, 3103, 3279,
				"Rough wall", "Tightrope", "Narrow wall", "Gap", "Crate")
				.build(),
			course("agility_al_kharid", "Al Kharid Rooftop", 20, 216, 13_000, 3272, 3195,
				"Rough wall", "Tightrope", "Cable", "Zip line", "Tropical tree", "Roof top beams", "Gap")
				.build(),
			Method.builder()
				.id("agility_brimhaven")
				.skill(Skill.AGILITY)
				.type(MethodType.ACTIVITY)
				.name("Brimhaven Agility Arena")
				.minLevel(20)
				.xpPerAction(345)
				.xpPerHour(25_000)
				.rateAt(40, 40_000).rateAt(80, 45_000).rateAt(99, 55_000)
				.members(true)
				.tag(MethodTag.MINIGAME).tag(MethodTag.CHEAP)
				.location(Location.area("Brimhaven Agility Arena", 2809, 3194, 12))
				.alsoAt(Location.area("Brimhaven Agility Arena (inside)", 2781, 9568, 30))
				.target(Target.object(null, "Ticket dispenser", "Floor spikes", "Pressure pad", "Rope swing",
					"Monkey bars", "Log balance", "Hand holds", "Plank", "Balancing rope", "Balancing ledge", "Low wall"))
				.target(Target.npc(null, "Cap'n Izzy No-Beard", "Pirate Jackie the Fruit"))
				.supply(ItemReq.supply("Coins (200 entry fee)", 200, ItemID.COINS))
				.supply(Supplies.food(5))
				.supply(Supplies.STAMINA)
				.instruction("Pay Cap'n Izzy No-Beard 200 coins and climb down into the arena. Every minute, tag the "
					+ "ticket dispenser with the yellow arrow over it, then cross the floor spikes back and forth until "
					+ "the next one")
				.notes("Trade arena tickets to Pirate Jackie the Fruit at the entrance for 345 Agility xp each. "
					+ "Floor spikes only need 20 Agility; you can pass every obstacle from 40. "
					+ "Karamja gloves 2 or better give 10% more xp here.")
				.wikiPage("Brimhaven_Agility_Arena")
				.getThere("Take the boat from Ardougne docks (or Rimmington) to Brimhaven on Karamja. The arena "
					+ "is under the building just west of Brimhaven: follow the arrow. Bring at least 200 coins.")
				.howTo("Talk to Cap'n Izzy No-Beard at the entrance, pay the 200 coins and climb down the ladder "
					+ "into the arena.")
				.howTo("The arena is a grid of pillars joined by obstacles. One ticket dispenser at a time has a "
					+ "flashing yellow arrow over it: make your way to it across the obstacles and click it to "
					+ "tag it.")
				.howTo("Then go to the nearest floor spikes and cross them back and forth for xp until a new "
					+ "dispenser gets the arrow (about every minute). Tag that one too.")
				.howTo("If an obstacle trips you, you take a little damage; eat when you're low.")
				.howTo("When you're done, climb out and trade your tickets to Pirate Jackie the Fruit at the "
					+ "entrance: choose Agility xp (345 xp per ticket).")
				.build(),
			course("agility_varrock", "Varrock Rooftop", 30, 270, 14_000, 3219, 3414,
				"Rough wall", "Clothes line", "Gap", "Ledge", "Edge")
				.build(),
			Method.builder()
				.id("agility_pyramid")
				.skill(Skill.AGILITY)
				.type(MethodType.ACTIVITY)
				.name("Agility Pyramid")
				.minLevel(30)
				// Obstacles give 722 xp, plus a top bonus of 300 + 8 per Agility level (capped at 1,000 from 88).
				.xpPerAction(1_262)
				.xpAt(new int[]{40, 1_342}).xpAt(new int[]{50, 1_422}).xpAt(new int[]{60, 1_502})
				.xpAt(new int[]{70, 1_582}).xpAt(new int[]{80, 1_662}).xpAt(new int[]{88, 1_722})
				.xpPerHour(18_000)
				.rateAt(50, 25_000).rateAt(60, 30_000).rateAt(70, 36_000).rateAt(75, 42_100).rateAt(88, 44_700)
				.coinsPerAction(10_000)
				.members(true)
				.tag(MethodTag.PROFIT)
				.location(Location.area("Agility Pyramid", 3360, 2838, 30))
				.alsoAt(Location.area("Agility Pyramid (top floor)", 3043, 4697, 15))
				.target(Target.object(null, "Stairs", "Low wall", "Ledge", "Plank", "Gap", "Climbing rocks", "Doorway"))
				.target(Target.npc(null, "Simon Templeton"))
				.supply(ItemReq.supply("Waterskins (desert heat)", 4, ItemID.WATER_SKIN4, ItemID.WATER_SKIN3,
					ItemID.WATER_SKIN2, ItemID.WATER_SKIN1).bestFirst(Supplies.WATERSKIN_NAMES))
				.supply(Supplies.food(5))
				.supply(Supplies.STAMINA)
				.instruction("Climb the pyramid floor by floor, clicking each highlighted obstacle. At the top, climb the "
					+ "rocks to take the pyramid top before you enter the doorway, then sell it to Simon Templeton")
				.notes("Each pyramid top sells to Simon Templeton for 10,000 gp. "
					+ "Grab the top first: the doorway sends you back down with nothing. "
					+ "Sell every top straight away, it weighs 11 kg. "
					+ "Time the moving pyramid blocks or they push you down a floor. "
					+ "Expect lots of falls below 60 Agility; nothing fails from 75. "
					+ "Bring waterskins for the desert heat, or wear a desert amulet 4 or a charged circlet of water.")
				.wikiPage("Agility_Pyramid")
				.getThere("The Agility Pyramid is in the desert south of Pollnivneach. Take the magic carpet from "
					+ "the Shantay Pass to Pollnivneach and run south, or follow the arrow. Bring waterskins and "
					+ "food.")
				.howTo("Click the stairs at the front of the pyramid to start. On each floor, click the "
					+ "highlighted obstacles one after the other (low walls, ledges, planks, gaps) as you go "
					+ "round, then take the stairs up to the next floor.")
				.howTo("Moving stone blocks slide across some floors. Wait until the block slides away before you "
					+ "walk past, or it knocks you down a floor.")
				.howTo("If you fail an obstacle you drop to the floor below. Just carry on from there.")
				.howTo("On the top floor, click the climbing rocks to grab the pyramid top FIRST. Then go through "
					+ "the doorway, which takes you back to the ground.")
				.howTo("Sell the pyramid top to Simon Templeton at the base of the pyramid for 10,000 coins "
					+ "straight away (it's heavy), then go again.")
				.howTo("Drink from a waterskin when the heat starts hurting you.")
				.build(),
			course("agility_barbarian", "Barbarian Outpost Agility Course", 35, 153.2, 14_000, 2552, 3556,
				"Rope swing", "Log balance", "Obstacle net", "Balancing ledge", "Ladder", "Crumbling wall")
				.quest(Quest.ALFRED_GRIMHANDS_BARCRAWL)
				.notes("No marks of grace, but it's an option near a bank (Barbarian Outpost) before Canifis. "
					+ "A games necklace teleports you right next to it.")
				.build(),
			course("agility_canifis", "Canifis Rooftop", 40, 240, 19_000, 3506, 3490,
				"Tall tree", "Gap", "Pole-vault")
				.quest(Quest.PRIEST_IN_PERIL)
				.notes("Lots of marks of grace; the best place to get the Graceful outfit. " + MARKS)
				.build(),
			course("agility_shayzien_advanced", "Shayzien Advanced Agility Course", 45, 507.5, 30_000, 1554, 3632,
				"Ladder", "Monkeybars", "Tightrope", "Beam", "Edge", "Zipline")
				.tool(ItemReq.tool("Any crossbow", ItemID.CROSSBOW, ItemID.XBOWS_CROSSBOW_BRONZE,
					ItemID.XBOWS_CROSSBOW_IRON, ItemID.XBOWS_CROSSBOW_STEEL, ItemID.XBOWS_CROSSBOW_MITHRIL,
					ItemID.XBOWS_CROSSBOW_ADAMANTITE, ItemID.XBOWS_CROSSBOW_RUNITE, ItemID.XBOWS_CROSSBOW_DRAGON)
					// Any one will do, so the lowest one owned (which needs the least Ranged to wield) is named
					.bestFirst("Phoenix crossbow", "Bronze crossbow", "Iron crossbow", "Steel crossbow",
						"Mithril crossbow", "Adamant crossbow", "Rune crossbow", "Dragon crossbow"))
				.tool(ItemReq.tool("Mith grapple", ItemID.XBOWS_GRAPPLE_TIP_BOLT_MITHRIL_ROPE))
				.notes("Wield the crossbow and keep the mith grapple in your inventory for the grapple obstacles. "
					+ "Good if you can't use Ape Atoll and want to stay out of the Wilderness. " + MARKS)
				.getThere("Wield any crossbow and carry a mith grapple: the course has grapple obstacles you "
					+ "can't cross without them.")
				.build(),
			course("agility_ape_atoll", "Ape Atoll Agility Course", 48, 580, 35_000, 2755, 2742,
				"Stepping stone", "Tropical tree", "Monkeybars", "Skull slope", "Rope")
				.rateAt(60, 45_000).rateAt(75, 55_000)
				.requirementText("Wear a small or medium ninja monkey greegree, or the Kruk greegree (Monkey Madness I)")
				.notes("You fail every obstacle without the right greegree on. Fairy ring C-L-R lands on the course. "
					+ "Nothing fails from 75 Agility. " + MARKS)
				.getThere("Wear a small or medium ninja monkey greegree (or the Kruk greegree) the whole time: "
					+ "without it every obstacle fails. Fairy ring C-L-R lands right on the course.")
				.build(),
			course("agility_falador", "Falador Rooftop", 50, 586, 27_000, 3035, 3342,
				"Rough wall", "Tightrope", "Hand holds", "Gap", "Ledge", "Edge")
				.build(),
			course("agility_wyrm_basic", "Colossal Wyrm Agility Course (basic)", 50, 601.6, 31_000, 1648, 2929,
				"Ladder", "Tightrope", "Edge", "Rope", "Zipline")
				.notes("Low effort: obstacles never fail and refill run energy. You also get termites to trade to Worm "
					+ "Tongue and blessed bone shards for Prayer. Fairy ring A-J-P is close by. " + MARKS)
				.build(),
			course("agility_wilderness", "Wilderness Agility Course", 52, 571.4, 50_000, 3004, 3937,
				"Obstacle pipe", "Ropeswing", "Stepping stone", "Log balance", "Rocks")
				.target(Target.object(null, "Agility dispenser"))
				.rateAt(62, 60_000)
				.notes("Deep Wilderness: player killers can attack you here, so bring only what you can afford to lose. "
					+ "Tag the Agility dispenser after each lap for a ticket worth extra xp and a run energy refill. "
					+ "Paying it 150,000 coins also gives loot, but you lose that fee if you die or leave. "
					+ "Ice Plateau Teleport tablets land just north of the course. "
					+ "Summer pies boost you in from 47.")
				.getThere("This course is deep in the Wilderness: other players can attack you there. Only bring "
					+ "food and stamina potions you can afford to lose.")
				.howTo("After each lap, click the Agility dispenser near the start for a ticket and a run energy "
					+ "refill.")
				.build(),
			Method.builder()
				.id("agility_sepulchre")
				.skill(Skill.AGILITY)
				.type(MethodType.ACTIVITY)
				.name("Hallowed Sepulchre")
				.minLevel(52)
				// Floor xp, run through every floor the player can enter (52, 62, 72, 77, 87 Agility).
				.xpPerAction(575)
				.xpAt(new int[]{62, 1_500}).xpAt(new int[]{72, 3_100}).xpAt(new int[]{77, 5_975})
				.xpAt(new int[]{87, 11_700})
				.xpPerHour(45_000)
				.rateAt(62, 56_300).rateAt(72, 68_900).rateAt(77, 79_700).rateAt(87, 98_500)
				.members(true)
				.tag(MethodTag.MINIGAME).tag(MethodTag.FAST)
				.quest(Quest.SINS_OF_THE_FATHER)
				.location(Location.area("Hallowed Sepulchre (Darkmeyer)", 3654, 3387, 10))
				.alsoAt(Location.area("Hallowed Sepulchre floors", 2368, 5856, 200))
				.target(Target.object(null, "Stairs", "Coffin", "Grand Hallowed Coffin", "Magical obelisk"))
				.supply(Supplies.STAMINA)
				.supply(Supplies.food(5))
				.instruction("Enter the Sepulchre in Darkmeyer and take the stairs down. Run each floor, dodging the "
					+ "arrows, swords and flames, then take the stairs at the end down to the next floor")
				.notes("Floor xp is only given when you take the stairs down or leave by a magical obelisk. "
					+ "Wear a Saradomin item so the obelisks refill your run energy. "
					+ "Loot coffins for hallowed marks; the Grand Hallowed Coffin on floor 5 needs 66 Thieving. "
					+ "200 marks buy a private instance with the same trap pattern every time.")
				.wikiPage("Hallowed_Sepulchre")
				.getThere("The Hallowed Sepulchre is in the city of Darkmeyer (you need Sins of the Father). "
					+ "Drakan's medallion teleports you to Darkmeyer; then follow the arrow to the Sepulchre "
					+ "entrance.")
				.getThere("Wear a Saradomin item if you have one: the obelisks at the end of each floor then "
					+ "refill your run energy. Bring stamina potions and some food.")
				.howTo("Go in and take the stairs down to floor 1.")
				.howTo("Each floor is a path with traps: statues that shoot arrows, swinging swords and flame "
					+ "jets. Stop before a trap, watch it once, and run past when it's clear. If you get hit, "
					+ "eat.")
				.howTo("At the end of the floor, click the highlighted stairs down to the next floor. That's when "
					+ "the floor's xp comes in. Floor 2 opens at 62 Agility, 3 at 72, 4 at 77 and 5 at 87.")
				.howTo("On the last floor you can do, click a highlighted magical obelisk to leave (you still get "
					+ "that floor's xp), then start again from floor 1.")
				.howTo("Optional: click the coffins on the way for hallowed marks to spend in the shop.")
				.build(),
			course("agility_seers", "Seers' Village Rooftop", 60, 570, 45_000, 2728, 3488,
				"Gap", "Tightrope", "Edge")
				.target(Target.object("Climb-up", "Wall"))
				.notes("Teleport back to the start with Camelot Teleport (Kandarin hard diary sends you to Seers' bank). "
					+ MARKS)
				.howTo("The course starts at the wall on the bank building in Seers' Village (\"Climb-up\"). "
					+ "After the last edge, use Camelot Teleport to get back to the start quickly.")
				.build(),
			course("agility_werewolf", "Werewolf Agility Course", 60, 730, 55_000, 3540, 9880,
				"Stepping stone", "Hurdle", "Pipe", "Skull slope", "Zip line")
				.target(Target.npc(null, "Agility Trainer"))
				.quest(Quest.CREATURE_OF_FENKENSTRAIN)
				.requirementText("Wear a Ring of Charos to enter (trapdoor in Canifis)")
				.rateAt(80, 62_000).rateAt(90, 68_000)
				.notes("Pick up the stick after the pipes and hand it to the Agility Trainer at the end for 380 bonus "
					+ "xp. Keep a free inventory slot for it. "
					+ "Only the zip line can fail, and it stops failing at 80 Agility and Strength. " + MARKS)
				.getThere("Wear a Ring of Charos and climb down the trapdoor in Canifis to "
					+ "reach the course.")
				.howTo("After the pipes, pick up the stick that appears and hand it to the Agility Trainer at the "
					+ "end of the lap for bonus xp. Keep one inventory slot free for it.")
				.build(),
			course("agility_wyrm_advanced", "Colossal Wyrm Agility Course (advanced)", 62, 1053.6, 42_000, 1648, 2929,
				"Ladder", "Tightrope", "Edge", "Rope", "Zipline")
				.notes("Climb the second ladder after the first tightrope for the advanced route. Only about six clicks "
					+ "a lap and obstacles never fail. Amylase packs sell to Grace for 8 marks of grace each. " + MARKS)
				.howTo("After the first tightrope, climb the second ladder (the one up the higher scaffolding) to "
					+ "take the advanced route.")
				.build(),
			course("agility_pollnivneach", "Pollnivneach Rooftop", 70, 890, 52_000, 3350, 2963,
				"Basket", "Market stall", "Banner", "Gap", "Tree", "Rough wall", "Monkeybars", "Drying line")
				.build(),
			course("agility_prifddinas", "Prifddinas Agility Course", 75, 1337, 60_000, 3253, 6109,
				"Ladder", "Tightrope", "Chimney", "Roof edge", "Dark hole", "Rope bridge")
				.quest(Quest.SONG_OF_THE_ELVES)
				.rateAt(90, 65_000)
				.notes("Take the portals that sometimes appear on the course: they're shortcuts and give a crystal "
					+ "shard each. Low click effort, like the rooftop courses. " + MARKS)
				.build(),
			course("agility_rellekka", "Rellekka Rooftop", 80, 780, 55_000, 2624, 3677,
				"Rough wall", "Gap", "Tightrope", "Pile of fish")
				.build(),
			course("agility_ardougne", "Ardougne Rooftop", 90, 889, 62_000, 2673, 3298,
				"Wooden Beams", "Gap", "Plank", "Steep roof")
				.build()
		);
	}

	private static Method.MethodBuilder course(String id, String name, int level, double lapXp, int xpHr, int x, int y,
		String... obstacles)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.AGILITY)
			.type(MethodType.ACTIVITY)
			.name(name)
			.minLevel(level)
			.xpPerAction(lapXp)
			.xpPerHour(xpHr)
			.members(true)
			.tag(MethodTag.CHEAP)
			.location(Location.area(name, x, y, 40))
			.target(Target.object(null, obstacles))
			.supply(Supplies.STAMINA)
			.supply(Supplies.food(5))
			.instruction("Run laps of the course, clicking each highlighted obstacle in turn")
			.notes(MARKS)
			.howTo("Turn on run (click the boot icon next to the minimap) and keep some food with you: falling off "
				+ "an obstacle costs a few hitpoints.")
			.howTo("Start at the highlighted " + obstacles[0] + " where the arrow points and click it. Your "
				+ "character crosses it by themselves.")
			.howTo("Then click the next highlighted obstacle ahead of you. The obstacles go: "
				+ String.join(" → ", obstacles) + ". Some names repeat (like \"Gap\"), so always take the one in "
				+ "front of you.")
			.howTo("The last obstacle puts you back on the ground near the start: that's one lap. Go straight back to "
				+ "the " + obstacles[0] + " and repeat.")
			.howTo("If you fail an obstacle you drop to the ground and lose a little health. Walk back to the "
				+ obstacles[0] + " and start the lap again; eat if your hitpoints get low.")
			.wikiPage(WIKI);
	}

	private AgilityGuide()
	{
	}
}
