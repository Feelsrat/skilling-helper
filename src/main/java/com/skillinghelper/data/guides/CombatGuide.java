package com.skillinghelper.data.guides;

import com.skillinghelper.data.CombatGear;
import com.skillinghelper.data.CombatStyle;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

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
	static final Location[] GEMSTONE_CRAB_SPOTS = {
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
		/**
		 * Lowest level for melee and Hitpoints training here, or 0 when it's a ranged-only spot.
		 */
		int meleeLevel;
		/**
		 * Lowest level for Ranged training here, or 0 when it isn't a ranged spot.
		 */
		int rangedLevel;
		/**
		 * The Ranged method throws chinchompas into groups of monsters instead of firing a bow.
		 */
		boolean chinning;
		/**
		 * The Ranged method uses a dwarf multicannon as well.
		 */
		boolean cannon;
		String rangedName;
		int rangedXpHr;
		/**
		 * Ranged xp per hour at higher levels, when it isn't just the melee rate scaled.
		 */
		int[] rangedRates;
		Quest quest;
		/**
		 * A quest that only has to be started (or partly done) to reach the spot.
		 */
		Quest questStarted;
		ItemReq tool;
		int slayerLevel;

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
			this.meleeLevel = level;
			this.rangedLevel = level;
		}

		Monster meleeOnly()
		{
			rangedLevel = 0;
			return this;
		}

		Monster rangedOnly()
		{
			meleeLevel = 0;
			return this;
		}

		Monster rangedFrom(int level)
		{
			rangedLevel = level;
			return this;
		}

		/**
		 * Ranged xp per hour from {@code level}, then as level, xp per hour pairs.
		 */
		Monster rangedRates(int xpHr, int... rates)
		{
			rangedXpHr = xpHr;
			rangedRates = rates;
			return this;
		}

		Monster chinning(String name)
		{
			chinning = true;
			rangedName = name;
			return this;
		}

		Monster cannon(String name)
		{
			cannon = true;
			rangedName = name;
			return this;
		}

		Monster quest(Quest quest)
		{
			this.quest = quest;
			return this;
		}

		Monster questStarted(Quest quest)
		{
			this.questStarted = quest;
			return this;
		}

		Monster slayer(int level)
		{
			slayerLevel = level;
			return this;
		}

		Monster tool(ItemReq tool)
		{
			this.tool = tool;
			return this;
		}
	}

	private static final List<ItemReq> CANNON = List.of(
		ItemReq.tool("Cannon base", ItemID.TWPART1),
		ItemReq.tool("Cannon stand", ItemID.TWPART2),
		ItemReq.tool("Cannon barrels", ItemID.TWPART3),
		ItemReq.tool("Cannon furnace", ItemID.TWPART4));
	/**
	 * About 6 cannonballs per ogre (60 hitpoints, the cannon hits up to 30).
	 */
	private static final ItemReq CANNONBALLS = ItemReq.stack("Cannonballs", 6, ItemID.MCANNONBALL,
		ItemID.GRANITE_CANNONBALL);

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
			// Ranged NMZ is slower than melee there; the wiki starts it at 70 with a magic shortbow or blowpipe
			.rangedFrom(70).rangedRates(60_000, 80, 85_000, 99, 120_000),
		new Monster("rock_crabs", "Rock crabs (Rellekka)", new String[]{"Rock Crab"}, 50, 10, 12_000, true,
			Location.of("Rellekka rock crabs", 2706, 3722, 15),
			"AFK like sand crabs: stand between a few rocks and the crabs wake up. When they stop attacking you "
				+ "(about 10 minutes), walk well away and come back.", null, true, 25, 20_000, 40, 28_000),
		new Monster("ammonite_crabs", "Ammonite crabs (Fossil Island)", new String[]{"Ammonite Crab"}, 100, 30,
			25_000, true, Location.of("Fossil Island north coast ammonite crabs", 3718, 3870, 30),
			"The best crabs: 100 hitpoints and low Defence. They wake up when you stand next to them; when they "
				+ "stop attacking you, walk away and come back. The Museum Camp bank chest is close by.",
			null, true, 50, 40_000, 70, 55_000, 85, 70_000, 99, 80_000)
			.quest(Quest.BONE_VOYAGE),
		new Monster("ogres_cannon", "Ogres (Castle Wars)", new String[]{"Ogre"}, 60, 1, 40_000, true,
			Location.of("Ogres east of Castle Wars", 2505, 3100, 15),
			"Set the cannon up among the ogres and attack them too. The cannon does most of the damage, so this "
				+ "is the fastest Ranged xp until chinchompas at 45, but cannonballs cost money.",
			null, false, 30, 45_000, 45, 50_000)
			.cannon("Cannon ogres (Castle Wars)").quest(Quest.DWARF_CANNON).rangedOnly(),
		new Monster("maniacal_monkeys", "Maniacal monkeys (Kruk's Dungeon)", new String[]{"Maniacal monkey"}, 65,
			75, 140_000, true, Location.of("Kruk's Dungeon maniacal monkeys", 2660, 9151, 20),
			"Fastest melee and Ranged xp. They drop 1-dose prayer potions, so use Protect from Melee and stay. "
				+ "Bring a light source.",
			"Needs Monkey Madness II started up to chapter II (to enter Kruk's Dungeon)", false,
			85, 170_000, 99, 200_000)
			.questStarted(Quest.MONKEY_MADNESS_II)
			.chinning("Chinning maniacal monkeys (Kruk's Dungeon)").rangedFrom(45)
			.rangedRates(200_000, 55, 260_000, 65, 350_000, 75, 420_000, 85, 480_000, 99, 550_000),
		new Monster("skeletal_monkeys", "Skeletal monkeys (Ape Atoll Dungeon)", new String[]{"Skeleton"}, 77, 45,
			170_000, true, Location.of("Ape Atoll Dungeon skeletal monkeys", 2740, 9114, 15),
			"Only worth it if you can't get to the maniacal monkeys. They are undead, so a salve amulet helps. "
				+ "Bring prayer potions and a light source.",
			"Needs Monkey Madness I done far enough to enter the Ape Atoll Dungeon", false)
			.questStarted(Quest.MONKEY_MADNESS_I)
			.chinning("Chinning skeletal monkeys (Ape Atoll Dungeon)")
			.rangedRates(170_000, 55, 220_000, 65, 300_000, 75, 360_000, 85, 410_000, 99, 470_000)
			.rangedOnly(),
		new Monster("sulphur_naguas", "Sulphur naguas (Neypotzli)", new String[]{"Sulphur Nagua"}, 100, 55, 60_000,
			true, Location.of("Neypotzli Ancient Prison", 1360, 9563, 15),
			"Very fast melee xp thanks to their negative armour, best with a glacial temotli or sulphur blades. "
				+ "Moonlight potions made inside keep your prayer up.",
			"Needs Perilous Moons done far enough to enter Neypotzli", false, 65, 80_000, 75, 110_000)
			.questStarted(Quest.PERILOUS_MOONS).slayer(48).meleeOnly(),
		new Monster("gargoyles", "Gargoyles (Slayer Tower)", new String[]{"Gargoyle"}, 105, 80, 70_000, true,
			Location.of("Slayer Tower top floor gargoyles", 3440, 3542, 2, 12),
			"Good melee xp and alchables. Finish each gargoyle with a rock hammer when it's low.",
			null, false, 90, 80_000, 99, 90_000)
			.slayer(75).meleeOnly().tool(ItemReq.tool("Rock hammer", ItemID.SLAYER_ROCK_HAMMER, ItemID.GRANITE_HAMMER,
				ItemID.SLAYER_ROCK_THROWNHAMMER))
	);

	static List<Method> methods()
	{
		List<Method> m = new ArrayList<>();
		for (Monster mon : MONSTERS)
		{
			if (mon.meleeLevel > 0)
			{
				m.add(melee(mon, Skill.ATTACK, "Accurate").build());
				m.add(melee(mon, Skill.STRENGTH, "Aggressive").build());
				m.add(melee(mon, Skill.DEFENCE, "Defensive").build());
				m.add(hitpoints(mon).build());
			}
			if (mon.rangedLevel > 0)
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
		if (mon.quest != null)
		{
			b.quest(mon.quest);
		}
		if (mon.questStarted != null)
		{
			b.questStarted(mon.questStarted);
		}
		if (mon.slayerLevel > 0)
		{
			b.skillLevel(Skill.SLAYER, mon.slayerLevel);
		}
		if (mon.tool != null)
		{
			b.tool(mon.tool);
		}
		if (GEMSTONE_CRAB.equals(mon.key))
		{
			for (int i = 1; i < GEMSTONE_CRAB_SPOTS.length; i++)
			{
				b.alsoAt(GEMSTONE_CRAB_SPOTS[i]);
			}
			b.quest(Quest.CHILDREN_OF_THE_SUN).target(Target.npc("Mine", "Gemstone Crab Shell")).supply(Supplies.PICKAXE);
		}
		return b;
	}

	private static Method.MethodBuilder melee(Monster mon, Skill skill, String style)
	{
		return withRates(base(mon, skill, skill.getName().toLowerCase()), mon, 1.0)
			.minLevel(mon.meleeLevel)
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
			.minLevel(mon.meleeLevel)
			.xpPerAction(1.33 * mon.hitpoints)
			.xpPerHour(mon.meleeXpHr / 3)
			.tag(MethodTag.CHEAP)
			.gearStyle(CombatStyle.MELEE)
			.instruction("Attack the " + targetNoun(mon) + " with any combat style; Hitpoints xp comes with every hit");
	}

	private static Method.MethodBuilder ranged(Monster mon)
	{
		Method.MethodBuilder b = base(mon, Skill.RANGED, "ranged")
			.minLevel(mon.rangedLevel)
			.xpPerAction(4.0 * mon.hitpoints)
			.gearStyle(CombatStyle.RANGED)
			.wikiPage(RANGED_WIKI);
		if (mon.rangedName != null)
		{
			b.name(mon.rangedName);
		}
		if (mon.rangedRates != null)
		{
			b.xpPerHour(mon.rangedXpHr);
			for (int i = 0; i + 1 < mon.rangedRates.length; i += 2)
			{
				b.rateAt(mon.rangedRates[i], mon.rangedRates[i + 1]);
			}
		}
		else
		{
			withRates(b, mon, 1.1).xpPerHour((int) (mon.meleeXpHr * 1.1));
		}
		if (mon.chinning)
		{
			// The chinchompa is the weapon, so no bow or arrows; which kind depends on Ranged level and the bank
			return b.input(CombatGear.CHINCHOMPA_INPUT)
				.instruction("Wield your chinchompas, set the Long fuse style (Ranged and Defence xp) or Medium fuse "
					+ "(Ranged only), and throw them at the biggest group of " + targetNoun(mon));
		}
		// Roughly one arrow lost per kill once you pick arrows back up (or wear Ava's device). Which ammo depends
		// on the player's weapon and Ranged level, not the monster, so the guide picks it at runtime
		b.input(CombatGear.AMMO_INPUT);
		if (mon.cannon)
		{
			return b.tools(CANNON)
				.input(CANNONBALLS)
				.target(Target.object("Fire", "Dwarf multicannon"))
				.instruction("Set up your cannon among the " + targetNoun(mon) + ", load it with cannonballs, then "
					+ "equip your best ranged weapon and ammo, set Rapid and attack them too");
		}
		return b.instruction("Equip your best ranged weapon and ammo, set Rapid and attack the " + targetNoun(mon));
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
