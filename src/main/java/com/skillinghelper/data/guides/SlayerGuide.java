package com.skillinghelper.data.guides;

import com.skillinghelper.data.CombatStyle;
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
 * Based on the OSRS wiki "Slayer training" guide. Slayer is trained by killing assigned monsters, so each method is
 * a Slayer master; the guide points you at the master and the task itself is shown by the enchanted gem.
 */
final class SlayerGuide
{
	private static final String WIKI = "Slayer_training";

	static List<Method> methods()
	{
		return List.of(
			master("slayer_turael", "Turael", 1, 3, 5_000, Location.of("Burthorpe (Turael)", 2931, 3536, 8))
				.notes("Tasks are short and easy. Later, Turael can also reset tasks you don't want from other masters.")
				.build(),
			master("slayer_mazchna", "Mazchna", 1, 20, 8_000, Location.of("Canifis (Mazchna)", 3510, 3507, 8))
				.quest(Quest.PRIEST_IN_PERIL)
				.build(),
			master("slayer_vannaka", "Vannaka", 1, 40, 15_000,
				Location.of("Edgeville Dungeon (Vannaka)", 3146, 9914, 8))
				.build(),
			master("slayer_chaeldar", "Chaeldar", 1, 70, 25_000, Location.of("Zanaris (Chaeldar)", 2445, 4431, 8))
				.quest(Quest.LOST_CITY)
				.build(),
			master("slayer_konar", "Konar quo Maten", 1, 75, 30_000,
				Location.of("Mount Karuulm (Konar)", 1308, 3786, 8))
				.notes("Tasks are locked to one area, but Konar drops brimstone keys.")
				.build(),
			master("slayer_nieve", "Nieve", 1, 85, 35_000,
				Location.of("Tree Gnome Stronghold (Nieve/Steve)", 2432, 3423, 8))
				.build(),
			master("slayer_duradel", "Duradel", 50, 100, 40_000, Location.of("Shilo Village (Duradel)", 2869, 2982, 8))
				.quest(Quest.SHILO_VILLAGE)
				.build()
		);
	}

	private static Method.MethodBuilder master(String id, String name, int level, int combat, int xpHr,
		Location where)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.SLAYER)
			.type(MethodType.ACTIVITY)
			.name("Tasks from " + name)
			.minLevel(level)
			// Slayer xp equals the monster's hitpoints, which grows with the master's tasks
			.xpPerAction(Math.max(10, combat / 2.0))
			.combatLevel(combat > 3 ? combat : 0)
			.xpPerHour(xpHr)
			.members(true)
			.tag(MethodTag.PROFIT)
			.gearStyle(CombatStyle.MELEE)
			.supply(Supplies.food(combat >= 40 ? 20 : 10))
			.tool(ItemReq.tool("Enchanted gem", ItemID.SLAYER_GEM)
				.orUse("Slayer ring", ItemID.SLAYER_RING_ETERNAL, ItemID.SLAYER_RING_8, ItemID.SLAYER_RING_7,
					ItemID.SLAYER_RING_6, ItemID.SLAYER_RING_5, ItemID.SLAYER_RING_4, ItemID.SLAYER_RING_3,
					ItemID.SLAYER_RING_2, ItemID.SLAYER_RING_1)
				.from("Buy one from " + name + " for 1 gp", null, Target.npc("Trade", name)))
			.location(where)
			.target(Target.npc("Assignment", name, "Steve"))
			.instruction("Get a task from " + name + ", then check it with your enchanted gem and go kill the monsters")
			.notes("Use the best master your combat level allows: harder masters give much more xp per task.")
			.wikiPage(WIKI);
	}

	private SlayerGuide()
	{
	}
}
