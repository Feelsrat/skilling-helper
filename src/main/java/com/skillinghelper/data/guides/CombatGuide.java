package com.skillinghelper.data.guides;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import java.util.ArrayList;
import java.util.List;
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

		Monster(String key, String name, String[] npcs, int hitpoints, int level, int meleeXpHr, boolean members,
			Location location, String notes, String requirement, boolean afk)
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
		}
	}

	private static final List<Monster> MONSTERS = List.of(
		new Monster("chickens", "Chickens (Lumbridge)", new String[]{"Chicken"}, 3, 1, 4_000, false,
			Location.of("Lumbridge chicken pen", 3231, 3297, 8),
			"Chickens can't really hurt you. Pick up feathers for Fishing if you like.", null, false),
		new Monster("cows", "Cows (Lumbridge)", new String[]{"Cow", "Cow calf"}, 8, 10, 8_000, false,
			Location.of("Lumbridge cow field", 3257, 3270, 12),
			"Bank the cowhides for Crafting, or sell them.", null, false),
		new Monster("al_kharid_warriors", "Al Kharid warriors", new String[]{"Al Kharid warrior"}, 19, 20, 15_000,
			false, Location.of("Al Kharid palace", 3293, 3173, 10),
			"They hit back, so bring some food.", null, false),
		new Monster("hill_giants", "Hill giants (Edgeville Dungeon)", new String[]{"Hill Giant"}, 35, 30, 25_000, false,
			Location.of("Edgeville Dungeon hill giants", 3117, 9845, 12),
			"Bring food. Big bones are worth burying or selling.",
			"A brass key opens the hill giant room door", false),
		new Monster("sand_crabs", "Sand crabs (Hosidius)", new String[]{"Sand Crab"}, 60, 20, 45_000, true,
			Location.of("Hosidius sand crab beach", 1740, 3468, 30),
			"Very AFK: stand next to a few crabs and they wake up. Run away and back every 10 minutes when they "
				+ "stop attacking you.", null, true),
		new Monster("nmz", "Nightmare Zone", new String[]{}, 400, 60, 80_000, true,
			Location.of("Nightmare Zone", 2606, 3115, 8),
			"Customisable rumble with absorption and overload potions is the classic AFK setup.",
			"Nightmare Zone needs 5 quest bosses unlocked (e.g. The Grand Tree, Fight Arena, Tree Gnome Village, "
				+ "Waterfall Quest, Witch's House)", true)
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
		if ("nmz".equals(mon.key))
		{
			b.tag(MethodTag.MINIGAME).target(Target.object(null, "Potion")).target(Target.npc(null, "Dominic Onion"));
		}
		return b;
	}

	private static Method.MethodBuilder melee(Monster mon, Skill skill, String style)
	{
		return base(mon, skill, skill.getName().toLowerCase())
			.minLevel(mon.level)
			.xpPerAction(4.0 * mon.hitpoints)
			.xpPerHour(mon.meleeXpHr)
			.tag(MethodTag.CHEAP)
			.instruction("Set your combat style to " + style + " (" + skill.getName() + " xp) and attack the "
				+ targetNoun(mon));
	}

	private static Method.MethodBuilder hitpoints(Monster mon)
	{
		return base(mon, Skill.HITPOINTS, "hitpoints")
			// Hitpoints starts at 10, so the level-1 monster stays level 1 to keep the plan simple
			.minLevel(mon.level)
			.xpPerAction(1.33 * mon.hitpoints)
			.xpPerHour(mon.meleeXpHr / 3)
			.tag(MethodTag.CHEAP)
			.instruction("Attack the " + targetNoun(mon) + " with any combat style; Hitpoints xp comes with every hit");
	}

	private static Method.MethodBuilder ranged(Monster mon)
	{
		boolean cheapAmmo = mon.level < 20;
		ItemReq ammo = cheapAmmo
			? ItemReq.stack("Iron arrows", 1, ItemID.IRON_ARROW)
			: ItemReq.stack("Mithril arrows", 1, ItemID.MITHRIL_ARROW);
		return base(mon, Skill.RANGED, "ranged")
			.minLevel(mon.level)
			.xpPerAction(4.0 * mon.hitpoints)
			.xpPerHour((int) (mon.meleeXpHr * 1.1))
			// Roughly one arrow lost per kill once you pick arrows back up (or wear Ava's device)
			.input(ammo)
			.instruction("Equip your best bow with the arrows, set Rapid and attack the " + targetNoun(mon))
			.wikiPage(RANGED_WIKI);
	}

	private static String targetNoun(Monster mon)
	{
		return mon.npcs.length == 0 ? "monsters" : mon.npcs[0].toLowerCase() + "s";
	}

	private CombatGuide()
	{
	}
}
