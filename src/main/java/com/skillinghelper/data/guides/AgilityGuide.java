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
					ItemID.WATER_SKIN2, ItemID.WATER_SKIN1))
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
				.build(),
			course("agility_canifis", "Canifis Rooftop", 40, 240, 19_000, 3506, 3490,
				"Tall tree", "Gap", "Pole-vault")
				.quest(Quest.PRIEST_IN_PERIL)
				.notes("Lots of marks of grace; the best place to get the Graceful outfit. " + MARKS)
				.build(),
			course("agility_falador", "Falador Rooftop", 50, 586, 27_000, 3035, 3342,
				"Rough wall", "Tightrope", "Hand holds", "Gap", "Ledge", "Edge")
				.build(),
			course("agility_seers", "Seers' Village Rooftop", 60, 570, 45_000, 2728, 3488,
				"Gap", "Tightrope", "Edge")
				.target(Target.object("Climb-up", "Wall"))
				.notes("Teleport back to the start with Camelot Teleport (Kandarin hard diary sends you to Seers' bank). "
					+ MARKS)
				.build(),
			course("agility_pollnivneach", "Pollnivneach Rooftop", 70, 890, 52_000, 3350, 2963,
				"Basket", "Market stall", "Banner", "Gap", "Tree", "Rough wall", "Monkeybars", "Drying line")
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
			.wikiPage(WIKI);
	}

	private AgilityGuide()
	{
	}
}
