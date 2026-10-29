package com.skillinghelper.data.guides;

import com.skillinghelper.data.CombatGear;
import com.skillinghelper.data.CombatStyle;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Melee, Ranged and Hitpoints training, based on the OSRS wiki "Free-to-play melee training", "Pay-to-play melee
 * training" and "Ranged training" guides. Combat xp is 4 per damage in the trained style (1.33 for Hitpoints), so
 * xp per kill is worked out from the monster's hitpoints. Rates assume gear and stats typical for the level range.
 */
final class CombatGuide
{
	private static final String MELEE_F2P_WIKI = "Free-to-play_melee_training";
	private static final String MELEE_P2P_WIKI = "Pay-to-play_melee_training";
	private static final String RANGED_WIKI = "Ranged_training";
	private static final String GEMSTONE_CRAB = "gemstone_crab";
	/**
	 * The Gemstone Crab burrows every 10 minutes and pops up at the next of these three spots, from the wiki.
	 */
	private static final Location[] GEMSTONE_CRAB_SPOTS = {
		Location.of("Gemstone Crab (north)", 1275, 3160, 12),
		Location.of("Gemstone Crab (east)", 1351, 3110, 12),
		Location.of("Gemstone Crab (south)", 1237, 3042, 12),
	};

	/**
	 * A monster worth training on.
	 */
	private static final class Monster
	{
		final String key;
		final String name;
		final String[] npcs;
		final int hitpoints;
		final int level;
		final int meleeXpHr;
		final boolean members;
		final Location location;
		final String notes;
		final String requirement;
		final boolean afk;
		/**
		 * Melee xp per hour at higher levels, as level, xp per hour pairs.
		 */
		final int[] meleeRates;

		Monster(String key, String name, String[] npcs, int hitpoints, int level, int meleeXpHr, boolean members,
			Location location, String notes, String requirement, boolean afk, int... meleeRates)
		{
			this.key = key;
			this.name = name;
			this.npcs = npcs;
			this.hitpoints = hitpoints;
			this.level = level;
			this.meleeXpHr = meleeXpHr;
			this.members = members;
			this.location = location;
			this.notes = notes;
			this.requirement = requirement;
			this.afk = afk;
			this.meleeRates = meleeRates;
		}
	}

	private static final List<Monster> MONSTERS = List.of(
		new Monster("chickens", "Chickens (Lumbridge)", new String[]{"Chicken"}, 3, 1, 2_500, false,
			Location.of("Lumbridge chicken pen", 3231, 3297, 8),
			"Chickens can't really hurt you. Pick up feathers for Fishing if you like.", null, false, 10, 5_000),
		new Monster("cows", "Cows (Lumbridge)", new String[]{"Cow", "Cow calf"}, 8, 10, 6_000, false,
			Location.of("Lumbridge cow field", 3257, 3270, 12),
			"Bank the cowhides for Crafting, or sell them.", null, false, 20, 10_000),
		new Monster("al_kharid_warriors", "Al Kharid warriors", new String[]{"Al Kharid warrior"}, 19, 20, 12_000,
			false, Location.of("Al Kharid palace", 3293, 3173, 10),
			"They hit back, so bring some food.", null, false, 30, 18_000, 40, 22_000),
		new Monster("hill_giants", "Hill giants (Edgeville Dungeon)", new String[]{"Hill Giant"}, 35, 30, 18_000, false,
			Location.of("Edgeville Dungeon hill giants", 3117, 9845, 12),
			"Bring food. Big bones are worth burying or selling.",
			"A brass key opens the hill giant room door", false, 50, 30_000, 70, 40_000),
		new Monster("sand_crabs", "Sand crabs (Hosidius)", new String[]{"Sand Crab"}, 60, 20, 15_000, true,
			Location.of("Hosidius sand crab beach", 1740, 3468, 30),
			"Very AFK: stand next to a few crabs and they wake up. Run away and back every 10 minutes when they "
				+ "stop attacking you.", null, true, 40, 30_000, 60, 45_000, 80, 60_000, 99, 70_000),
		// The crab can't die and takes any damage, so a "kill" here is ~100 damage at the reduced 3.5 xp per damage
		new Monster(GEMSTONE_CRAB, "Gemstone Crab (Varlamore)", new String[]{"Gemstone Crab"}, 88, 1, 5_000, true,
			GEMSTONE_CRAB_SPOTS[0],
			"Very AFK and it never hits back. Every 10 minutes it burrows and moves to one of the other two spots: "
				+ "crawl through the cave next to the old spot to come out at the new one. The top 16 damage dealers "
				+ "can mine the shell afterwards for gems (bring any pickaxe).", null, true,
			20, 18_000, 40, 35_000, 60, 52_000, 80, 70_000, 99, 85_000),
		new Monster("nmz", "Nightmare Zone", new String[]{}, 400, 60, 55_000, true,
			Location.of("Nightmare Zone", 2606, 3115, 8),
			"Customisable rumble with absorption and overload potions is the classic AFK setup.",
			"Nightmare Zone needs 5 quest bosses unlocked (e.g. The Grand Tree, Fight Arena, Tree Gnome Village, "
				+ "Waterfall Quest, Witch's House)", true, 80, 85_000, 99, 110_000)
	);

	static List<Method> methods()
	{
		List<Method> m = new ArrayList<>();
		for (Monster mon : MONSTERS)
		{
			m.add(melee(mon, Skill.ATTACK, "Accurate").build());
			m.add(melee(mon, Skill.STRENGTH, "Aggressive").build());
			m.add(melee(mon, Skill.DEFENCE, "Defensive").build());
			m.add(hitpoints(mon).build());
			if (!"nmz".equals(mon.key))
			{
				m.add(ranged(mon).build());
			}
		}
		return m;
	}

	private static Method.MethodBuilder base(Monster mon, Skill skill, String prefix)
	{
		Method.MethodBuilder b = Method.builder()
			.id(prefix + "_" + mon.key)
			.skill(skill)
			.type(MethodType.ACTIVITY)
			.name(mon.name)
			.members(mon.members)
			.location(mon.location)
			.notes(mon.notes)
			.requirementText(mon.requirement)
			.wikiPage(mon.members ? MELEE_P2P_WIKI : MELEE_F2P_WIKI);
		if (mon.npcs.length > 0)
		{
			b.target(Target.npc("Attack", mon.npcs));
		}
		if (mon.afk)
		{
			b.tag(MethodTag.AFK);
		}
		if (mon.hitpoints > 3 && !GEMSTONE_CRAB.equals(mon.key))
		{
			b.supply(Supplies.food(mon.hitpoints >= 60 ? 20 : 10));
		}
		if ("nmz".equals(mon.key))
		{
			b.tag(MethodTag.MINIGAME).target(Target.object(null, "Potion")).target(Target.npc(null, "Dominic Onion"));
		}
		if (GEMSTONE_CRAB.equals(mon.key))
		{
			for (int i = 1; i < GEMSTONE_CRAB_SPOTS.length; i++)
			{
				b.alsoAt(GEMSTONE_CRAB_SPOTS[i]);
			}
			b.quest(Quest.CHILDREN_OF_THE_SUN).target(Target.npc("Mine", "Gemstone Crab Shell"));
		}
		return b;
	}

	private static Method.MethodBuilder melee(Monster mon, Skill skill, String style)
	{
		return withRates(base(mon, skill, skill.getName().toLowerCase()), mon, 1.0)
			.minLevel(mon.level)
			.xpPerAction(4.0 * mon.hitpoints)
			.xpPerHour(mon.meleeXpHr)
			.tag(MethodTag.CHEAP)
			.gearStyle(CombatStyle.MELEE)
			.instruction("Set your combat style to " + style + " (" + skill.getName() + " xp) and attack the "
				+ targetNoun(mon));
	}

	private static Method.MethodBuilder hitpoints(Monster mon)
	{
		return withRates(base(mon, Skill.HITPOINTS, "hitpoints"), mon, 1 / 3.0)
			// Hitpoints starts at 10, so the level-1 monster stays level 1 to keep the plan simple
			.minLevel(mon.level)
			.xpPerAction(1.33 * mon.hitpoints)
			.xpPerHour(mon.meleeXpHr / 3)
			.tag(MethodTag.CHEAP)
			.gearStyle(CombatStyle.MELEE)
			.instruction("Attack the " + targetNoun(mon) + " with any combat style; Hitpoints xp comes with every hit");
	}

	private static Method.MethodBuilder ranged(Monster mon)
	{
		return withRates(base(mon, Skill.RANGED, "ranged"), mon, 1.1)
			.minLevel(mon.level)
			.xpPerAction(4.0 * mon.hitpoints)
			.xpPerHour((int) (mon.meleeXpHr * 1.1))
			// Roughly one arrow lost per kill once you pick arrows back up (or wear Ava's device). Which ammo depends
			// on the player's weapon and Ranged level, not the monster, so the guide picks it at runtime
			.input(CombatGear.AMMO_INPUT)
			.gearStyle(CombatStyle.RANGED)
			.instruction("Equip your best ranged weapon and ammo, set Rapid and attack the " + targetNoun(mon))
			.wikiPage(RANGED_WIKI);
	}

	/**
	 * The monster's melee rates scaled for another skill: Hitpoints gets a third of the xp, Ranged a bit more.
	 */
	private static Method.MethodBuilder withRates(Method.MethodBuilder b, Monster mon, double scale)
	{
		for (int i = 0; i + 1 < mon.meleeRates.length; i += 2)
		{
			b.rateAt(mon.meleeRates[i], (int) (mon.meleeRates[i + 1] * scale));
		}
		return b;
	}

	private static String targetNoun(Monster mon)
	{
		return mon.npcs.length == 0 ? "monsters" : mon.npcs[0].toLowerCase() + "s";
	}

	private CombatGuide()
	{
	}
}
