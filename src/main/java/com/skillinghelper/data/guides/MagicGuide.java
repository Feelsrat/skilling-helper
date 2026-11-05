package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.CombatSpell;
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
 * Based on the OSRS wiki "Free-to-play Magic training" and "Pay-to-play Magic training" guides. Elemental runes can be
 * replaced by the matching staff, which the guide picks up on automatically.
 */
final class MagicGuide
{
	private static final String F2P_WIKI = "Free-to-play_Magic_training";
	private static final String P2P_WIKI = "Pay-to-play_Magic_training";

	private static final int[] AIR_STAVES = CombatSpell.AIR_STAVES;
	private static final int[] FIRE_STAVES = CombatSpell.FIRE_STAVES;
	private static final int[] WATER_STAVES = CombatSpell.WATER_STAVES;
	private static final int[] EARTH_STAVES = CombatSpell.EARTH_STAVES;

	private static ItemReq earth(int perCast)
	{
		return ItemReq.stack("Earth runes", perCast, ItemID.EARTHRUNE).orUse("Staff of earth", EARTH_STAVES);
	}

	private static ItemReq air(int perCast)
	{
		return ItemReq.stack("Air runes", perCast, ItemID.AIRRUNE).orUse("Staff of air", AIR_STAVES);
	}

	private static ItemReq fire(int perCast)
	{
		return ItemReq.stack("Fire runes", perCast, ItemID.FIRERUNE).orUse("Staff of fire", FIRE_STAVES);
	}

	private static ItemReq water(int perCast)
	{
		return ItemReq.stack("Water runes", perCast, ItemID.WATERRUNE).orUse("Staff of water", WATER_STAVES);
	}

	static List<Method> methods()
	{
		Location cows = Location.of("Lumbridge cow field", 3257, 3270, 12);
		return List.of(
			combatSpells("magic_combat_spells_cows", "Cows (autocast your best spell)", false, 1, 700, 8)
				.location(cows)
				.bank(Banks.LUMBRIDGE)
				.tag(MethodTag.CHEAP)
				.target(Target.npc("Attack", "Cow", "Cow calf"))
				.instruction("Autocast the spell named next to the runes and attack the cows")
				.notes("The guide picks the strongest Strike, Bolt or Blast spell you can cast; an air staff saves "
					+ "the air runes every spell uses. Splashing still gives the spell's base xp.")
				.wikiPage(F2P_WIKI)
				.howTo("The cows are in the big fenced field east of Lumbridge, across the river: follow the "
					+ "arrow, then click the gate to go in.")
				.howTo("Open the combat options tab (the crossed swords icon), click \"Choose spell\" (autocast) "
					+ "and pick the spell the guide names next to the runes. Wear the staff the guide suggests.")
				.howTo("Keep the runes for that spell in your inventory. An air staff counts as unlimited air "
					+ "runes.")
				.howTo("Click a highlighted target (\"Attack\"). Your character casts the spell again and again "
					+ "by themselves.")
				.howTo("If the runes run out, the casting stops: go to the bank for more.")
				.build(),
			teleport("magic_varrock_tele", "Varrock Teleport", 25, 35, false,
				ItemReq.stack("Law runes", 1, ItemID.LAWRUNE), air(3), fire(1)).build(),
			teleport("magic_falador_tele", "Falador Teleport", 37, 48, false,
				ItemReq.stack("Law runes", 1, ItemID.LAWRUNE), air(3), water(1)).build(),
			teleport("magic_camelot_tele", "Camelot Teleport", 45, 55.5, true,
				ItemReq.stack("Law runes", 1, ItemID.LAWRUNE), air(5)).build(),
			Method.builder()
				.id("magic_superheat_iron")
				.skill(Skill.MAGIC)
				.type(MethodType.PROCESS)
				.name("Superheat Item on iron ore")
				.minLevel(43)
				.xpPerAction(53)
				.xpPerHour(80_000)
				.members(true)
				.tag(MethodTag.BANKSTANDING)
				.location(Banks.GRAND_EXCHANGE)
				.bank(Banks.GRAND_EXCHANGE)
				.input(ItemReq.stack("Nature runes", 1, ItemID.NATURERUNE))
				.input(fire(4))
				.input(ItemReq.consumed("Iron ore", 27, 1, ItemID.IRON_ORE))
				.product(ItemID.IRON_BAR)
				.useItem(ItemID.IRON_ORE)
				.instruction("Cast Superheat Item on each iron ore")
				.notes("Also gives 12.5 Smithing xp per bar, and every bar succeeds unlike a furnace.")
				.wikiPage(P2P_WIKI)
				.howTo("Stand at any bank (the Grand Exchange is handy). Wear a fire staff, or carry fire runes.")
				.howTo("Take out the nature runes and 27 iron ore.")
				.howTo("Open your spellbook (the blue book icon), click Superheat Item, then click an iron ore: "
					+ "it turns into an iron bar.")
				.howTo("The spellbook opens again by itself: click Superheat Item and the next ore. Repeat until "
					+ "all the ore is bars, then bank and take more.")
				.build(),
			Method.builder()
				.id("magic_enchant_opal_bolts")
				.skill(Skill.MAGIC)
				.type(MethodType.PROCESS)
				.name("Enchant opal bolts")
				.minLevel(4)
				.xpPerAction(9)
				.xpPerHour(18_000)
				.members(true)
				.tag(MethodTag.FAST)
				.location(Banks.GRAND_EXCHANGE)
				.bank(Banks.GRAND_EXCHANGE)
				.input(ItemReq.stack("Cosmic runes", 1, ItemID.COSMICRUNE))
				.input(air(2))
				.input(ItemReq.stack("Opal bolts (10 per cast)", 10, ItemID.OPAL_BOLTS))
				.instruction("Cast Enchant Crossbow Bolt and pick opal bolts; each cast enchants 10")
				.notes("Bolts and runes stack, so you can do this while running or questing. Higher gem bolts give "
					+ "more xp per cast but cost a lot more.")
				.wikiPage(P2P_WIKI)
				.howTo("Take out cosmic runes, air runes (or wear an air staff) and your opal bolts. Everything "
					+ "stacks, so you can do this anywhere.")
				.howTo("Open your spellbook (the blue book icon) and click Enchant Crossbow Bolt.")
				.howTo("A menu opens with the bolt types: click opal. Each cast enchants 10 bolts.")
				.howTo("Keep casting until the bolts or runes run out.")
				.build(),
			Method.builder()
				.id("magic_low_alch")
				.skill(Skill.MAGIC)
				.type(MethodType.PROCESS)
				.name("Low Level Alchemy (yew longbows)")
				.minLevel(21)
				.xpPerAction(31)
				.xpPerHour(60_000)
				.coinsPerAction(512)
				.tag(MethodTag.AFK)
				.location(Banks.GRAND_EXCHANGE)
				.bank(Banks.GRAND_EXCHANGE)
				.input(ItemReq.stack("Nature runes", 1, ItemID.NATURERUNE))
				.input(fire(3))
				.input(ItemReq.stack("Yew longbows (noted is fine)", 1, ItemID.YEW_LONGBOW, ItemID.Cert.YEW_LONGBOW))
				.useItem(ItemID.YEW_LONGBOW).useItem(ItemID.Cert.YEW_LONGBOW)
				.instruction("Cast Low Level Alchemy on the yew longbows")
				.notes("Low alching turns items into coins for 31 xp a cast. It loses a little money, but you can do "
					+ "it while walking or questing.")
				.wikiPage("Low_Level_Alchemy")
				.howTo("Wear a fire staff so you don't need fire runes. Take out the nature runes and the yew "
					+ "longbows (noted is fine: in the bank click \"Note\" at the bottom before you withdraw).")
				.howTo("Open your spellbook (the blue book icon), click Low Level Alchemy, then click a yew "
					+ "longbow. You get the coins.")
				.howTo("The spellbook opens again by itself: click Low Level Alchemy and the bow again. Keep "
					+ "going until they run out.")
				.build(),
			alch("magic_high_alch_yew", "High Alchemy (yew longbows)", "Yew longbows", ItemID.YEW_LONGBOW,
				ItemID.Cert.YEW_LONGBOW, 768).build(),
			alch("magic_high_alch_magic", "High Alchemy (magic longbows)", "Magic longbows", ItemID.MAGIC_LONGBOW,
				ItemID.Cert.MAGIC_LONGBOW, 1_536)
				.build(),
			Method.builder()
				.id("magic_enchant_recoil")
				.skill(Skill.MAGIC)
				.type(MethodType.PROCESS)
				.name("Enchant sapphire rings (rings of recoil)")
				.minLevel(7)
				.xpPerAction(17.5)
				// The spell repeats by itself every 7 ticks: about 850 rings an hour
				.xpPerHour(15_000)
				.members(true)
				.tag(MethodTag.BANKSTANDING).tag(MethodTag.PROFIT).tag(MethodTag.AFK)
				.location(Banks.GRAND_EXCHANGE)
				.bank(Banks.GRAND_EXCHANGE)
				.input(ItemReq.stack("Cosmic runes", 1, ItemID.COSMICRUNE))
				.input(water(1))
				.input(ItemReq.consumed("Sapphire rings", 27, 1, ItemID.SAPPHIRE_RING))
				.product(ItemID.RING_OF_RECOIL)
				.useItem(ItemID.SAPPHIRE_RING)
				.instruction("Cast Lvl-1 Enchant on a sapphire ring; the rest of the rings enchant by themselves")
				.wikiPage(P2P_WIKI)
				.howTo("Stand at any bank. Wear a water staff, or carry water runes, and take out the cosmic "
					+ "runes and 27 sapphire rings.")
				.howTo("Open your spellbook (the blue book icon), click Lvl-1 Enchant, then click a sapphire "
					+ "ring: it becomes a ring of recoil.")
				.howTo("Your character carries on with the next rings by themselves until the whole inventory is "
					+ "done (casting again by hand on each ring is a bit faster but much more clicking).")
				.howTo("Bank the rings of recoil, take out more sapphire rings and repeat.")
				.build(),
			enchant("magic_enchant_power", "Lvl-4 Enchant", "Diamond amulets", 57, 67, ItemID.STRUNG_DIAMOND_AMULET,
				ItemID.AMULET_OF_POWER, earth(10)).build(),
			enchant("magic_enchant_glory", "Lvl-5 Enchant", "Dragonstone amulets", 68, 78,
				ItemID.STRUNG_DRAGONSTONE_AMULET, ItemID.AMULET_OF_GLORY, earth(15), water(15)).build(),
			gemstoneCrab().build(),
			plankMake("magic_plank_make_oak", "Oak", ItemID.OAK_LOGS, ItemID.PLANK_OAK, 175).build(),
			plankMake("magic_plank_make_teak", "Teak", ItemID.TEAK_LOGS, ItemID.PLANK_TEAK, 350).build(),
			plankMake("magic_plank_make_mahogany", "Mahogany", ItemID.MAHOGANY_LOGS, ItemID.PLANK_MAHOGANY, 1_050)
				.build(),
			Method.builder()
				.id("magic_string_jewellery")
				.skill(Skill.MAGIC)
				.type(MethodType.PROCESS)
				.name("String Jewellery (gold amulets)")
				.minLevel(80)
				.xpPerAction(83)
				.xpPerHour(140_000)
				.members(true)
				.tag(MethodTag.BANKSTANDING).tag(MethodTag.AFK)
				.quest(Quest.LUNAR_DIPLOMACY)
				.location(Banks.GRAND_EXCHANGE)
				.bank(Banks.GRAND_EXCHANGE)
				.input(ItemReq.stack("Astral runes (2 per inventory)", 1, ItemID.ASTRALRUNE))
				.input(earth(1))
				.input(water(1))
				.input(ItemReq.consumed("Unstrung gold amulets", 25, 1, ItemID.UNSTRUNG_GOLD_AMULET))
				.product(ItemID.STRUNG_GOLD_AMULET)
				.instruction("Cast String Jewellery once; it strings every amulet in your inventory")
				.notes("Lunar spellbook. One cast per inventory, so it's very low effort. Also gives a little "
					+ "Crafting xp. Each cast uses 2 astral, 10 earth and 5 water runes.")
				.wikiPage("String_Jewellery")
				.howTo("You must be on the Lunar spellbook (after Lunar Diplomacy, pray at the altar in the "
					+ "Astral altar's temple on Lunar Isle to switch).")
				.howTo("Stand at any bank. Take out the astral, earth and water runes and 25 unstrung gold "
					+ "amulets.")
				.howTo("Open your spellbook and click String Jewellery once: your character strings every amulet "
					+ "in the inventory by themselves.")
				.howTo("Bank the strung amulets, take out more and cast again.")
				.build()
		);
	}

	private static Method.MethodBuilder teleport(String id, String spell, int level, double xp, boolean members,
		ItemReq... runes)
	{
		Method.MethodBuilder b = Method.builder()
			.id(id)
			.skill(Skill.MAGIC)
			.type(MethodType.ACTIVITY)
			.name(spell + " (repeat casting)")
			.minLevel(level)
			.xpPerAction(xp)
			// About 1,450 casts an hour including the occasional bank run for law runes
			.xpPerHour((int) (xp * 1_450))
			.members(members)
			.tag(MethodTag.FAST)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.instruction("Cast " + spell + " over and over")
			.howTo("Wear a staff of the element the spell uses most (an air staff for most teleports), so you don't "
				+ "need those runes. Take the law runes and other runes out of the bank.")
			.howTo("Open your spellbook (the blue book icon in the side panel) and click " + spell + ".")
			.howTo("You land at the destination after a few seconds. Click " + spell + " again straight away, and "
				+ "keep going until your law runes run out.")
			.howTo("Then go to the nearest bank (or buy law runes from a magic shop) and carry on.")
			.notes("Teleport spells are some of the fastest early Magic xp. Wear a staff to save the elemental runes.")
			.wikiPage(members ? P2P_WIKI : F2P_WIKI);
		for (ItemReq r : runes)
		{
			b.input(r);
		}
		return b;
	}

	private static Method.MethodBuilder enchant(String id, String spell, String itemName, int level, double xp,
		int item, int product, ItemReq... runes)
	{
		Method.MethodBuilder b = Method.builder()
			.id(id)
			.skill(Skill.MAGIC)
			.type(MethodType.PROCESS)
			.name(spell + " (" + itemName.toLowerCase() + ")")
			.minLevel(level)
			.xpPerAction(xp)
			// The spell repeats by itself every 7 ticks: about 850 an hour (wiki AFK guide: 63k xp/hr at Lvl-5)
			.xpPerHour((int) (xp * 850))
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.PROFIT).tag(MethodTag.AFK)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.stack("Cosmic runes", 1, ItemID.COSMICRUNE))
			.input(ItemReq.consumed(itemName, 27, 1, item))
			.product(product)
			.useItem(item)
			.instruction("Cast " + spell + " on one of the " + itemName.toLowerCase() + "; the rest enchant by "
				+ "themselves")
			.howTo("Stand at any bank (the Grand Exchange is handy). Wear the matching elemental staff if you have "
				+ "one, and take out the cosmic runes, the other runes and 27 " + itemName.toLowerCase() + ".")
			.howTo("Open your spellbook (the blue book icon), click " + spell + ", then click one of the "
				+ itemName.toLowerCase() + ".")
			.howTo("Your character carries on with the rest of the inventory by themselves (casting by hand on each "
				+ "one is a bit faster but much more clicking).")
			.howTo("Bank the enchanted items, take out more and repeat.")
			.notes("A wiki money maker when the enchanted item sells for more than the amulet and runes.")
			.wikiPage(P2P_WIKI);
		for (ItemReq r : runes)
		{
			b.input(r);
		}
		return b;
	}

	/**
	 * Autocasting the best standard combat spell the player has on the Gemstone Crab. It never dies or hits back, so
	 * it is AFK casting at one cast every 3 seconds. All its combat xp is cut to 87.5%.
	 */
	private static Method.MethodBuilder gemstoneCrab()
	{
		Method.MethodBuilder b = combatSpells("magic_gemstone_crab", "Gemstone Crab (autocast your best spell)", true,
			0.875, 1_200, 0)
			.tag(MethodTag.AFK)
			.quest(Quest.CHILDREN_OF_THE_SUN)
			.location(CombatGuide.GEMSTONE_CRAB_SPOTS[0])
			.target(Target.npc("Attack", "Gemstone Crab"))
			.target(Target.npc("Mine", "Gemstone Crab Shell"))
			.supply(Supplies.PICKAXE)
			.instruction("Autocast the spell named next to the runes and attack the Gemstone Crab")
			.howTo("The Gemstone Crab is in Varlamore, outside Civitas illa Fortis (you need Children of the "
				+ "Sun). It sits at one of three spots and moves every 10 minutes; the guide points to the spots.")
			.howTo("Open the combat options tab (the crossed swords icon), click \"Choose spell\" (autocast) and "
				+ "pick the spell the guide names next to the runes. Wear the staff the guide suggests.")
			.howTo("Keep the runes for that spell in your inventory. An air staff counts as unlimited air runes.")
			.howTo("Click a highlighted target (\"Attack\"). Your character casts the spell again and again by "
				+ "themselves.")
			.howTo("When the crab burrows, click the highlighted cave next to where it was and crawl through to "
				+ "its new spot. Attack it again. It never hits back, so no food is needed.")
			.notes("Very AFK and it never hits back. The guide picks the strongest Strike, Bolt, Blast, Wave or Surge "
				+ "spell you can cast; an air staff saves the air runes every spell uses. Every 10 minutes the crab "
				+ "burrows and moves to one of the other two spots: crawl through the cave next to the old spot to "
				+ "come out at the new one. The top 16 damage dealers can mine the shell afterwards for gems.")
			.wikiPage(P2P_WIKI);
		for (int i = 1; i < CombatGuide.GEMSTONE_CRAB_SPOTS.length; i++)
		{
			b.alsoAt(CombatGuide.GEMSTONE_CRAB_SPOTS[i]);
		}
		return b;
	}

	/**
	 * Combat Magic with the best standard spell for the player's level: the spell and its runes are worked out by
	 * the guide (see {@link CombatSpell}), and magic gear is suggested. Each cast is the spell's base xp plus 2 per
	 * damage (about 45% of the max hit on average), so xp per cast steps up with each new spell.
	 *
	 * @param xpModifier scales all xp, e.g. 0.875 on the Gemstone Crab
	 * @param hitpoints the target's hitpoints, which caps the damage a cast can do (0 if it never dies)
	 */
	private static Method.MethodBuilder combatSpells(String id, String name, boolean members, double xpModifier,
		int castsPerHour, int hitpoints)
	{
		Method.MethodBuilder b = Method.builder()
			.id(id)
			.skill(Skill.MAGIC)
			.type(MethodType.ACTIVITY)
			.name(name)
			.members(members)
			.input(CombatSpell.CATALYST_INPUT)
			.input(CombatSpell.AIR_INPUT)
			.input(CombatSpell.ELEMENT_INPUT)
			.gearStyle(CombatStyle.MAGIC);
		CombatSpell[] spells = CombatSpell.values();
		for (int i = spells.length - 1; i >= 0; i--)
		{
			CombatSpell spell = spells[i];
			if (spell.isMembers() && !members)
			{
				continue;
			}
			double damage = 0.45 * spell.getMaxHit();
			if (hitpoints > 0)
			{
				// Overkill is wasted: on average a cast does at most about half the target's hitpoints
				damage = Math.min(damage, hitpoints / 2.0);
			}
			double xp = xpModifier * (spell.getBaseXp() + 2 * damage);
			if (i == spells.length - 1)
			{
				b.minLevel(spell.getLevel()).xpPerAction(xp).xpPerHour((int) (xp * castsPerHour));
			}
			else
			{
				b.xpAt(new int[]{spell.getLevel(), (int) Math.round(xp)});
			}
		}
		return b;
	}

	/**
	 * Lunar Plank Make: turns one log into a plank for a coin fee, 70% of what the sawmill charges.
	 */
	private static Method.MethodBuilder plankMake(String id, String wood, int log, int plank, int fee)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.MAGIC)
			.type(MethodType.PROCESS)
			.name("Plank Make (" + wood.toLowerCase() + " logs)")
			.minLevel(86)
			.xpPerAction(90)
			// Letting the spell repeat by itself: about 1,000 planks an hour (wiki: up to 90k xp/hr)
			.xpPerHour(90 * 1_000)
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.PROFIT).tag(MethodTag.AFK)
			.quest(Quest.DREAM_MENTOR)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.stack("Astral runes", 2, ItemID.ASTRALRUNE))
			.input(ItemReq.stack("Nature runes", 1, ItemID.NATURERUNE))
			.input(earth(15))
			.input(ItemReq.stack("Coins", fee, ItemID.COINS))
			.input(ItemReq.consumed(wood + " logs", 25, 1, log))
			.product(plank)
			.useItem(log)
			.instruction("On the Lunar spellbook, cast Plank Make on one " + wood.toLowerCase() + " log; the rest turn "
				+ "into planks by themselves")
			.howTo("You must be on the Lunar spellbook: after Lunar Diplomacy, pray at the altar in the Astral "
				+ "altar's temple on Lunar Isle to switch.")
			.howTo("Stand at any bank. Take out the astral, nature and earth runes (or wear an earth staff), the coins "
				+ "and the " + wood.toLowerCase() + " logs.")
			.howTo("Open your spellbook, click Plank Make, then click a log. Your character keeps casting on the "
				+ "rest of the logs by themselves until every log is a plank.")
			.howTo("Bank the planks, take out more logs and repeat.")
			.notes("Needs the Lunar spellbook (Lunar Diplomacy) and Dream Mentor to cast. Each cast costs "
				+ String.format("%,d", fee) + " coins on top of the runes.")
			.wikiPage("Plank_Make");
	}

	private static Method.MethodBuilder alch(String id, String name, String itemName, int item, int noted,
		int alchValue)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.MAGIC)
			.type(MethodType.PROCESS)
			.name(name)
			.minLevel(55)
			.xpPerAction(65)
			.xpPerHour(78_000)
			.coinsPerAction(alchValue)
			.tag(MethodTag.AFK).tag(MethodTag.PROFIT)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.stack("Nature runes", 1, ItemID.NATURERUNE))
			.input(fire(5))
			.input(ItemReq.stack(itemName + " (noted is fine)", 1, item, noted))
			.useItem(item).useItem(noted)
			.instruction("Cast High Level Alchemy on the " + itemName.toLowerCase())
			.howTo("Wear a fire staff so you don't need fire runes. Take out the nature runes and the "
				+ itemName.toLowerCase() + " (noted is fine: in the bank click \"Note\" at the bottom before you "
				+ "withdraw, so they take one slot).")
			.howTo("Open your spellbook (the blue book icon), click High Level Alchemy, then click the "
				+ itemName.toLowerCase() + ". You get the coins.")
			.howTo("The spellbook opens again by itself: click High Level Alchemy and the item again. Keep going until "
				+ "the items or nature runes run out.")
			.notes("Each cast turns the item into " + String.format("%,d", alchValue) + " coins. Buy the items noted; "
				+ "profit or loss depends on today's GE prices, which the guide shows.")
			.wikiPage("High_Level_Alchemy");
	}

	private MagicGuide()
	{
	}
}
