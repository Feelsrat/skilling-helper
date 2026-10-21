package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import java.util.List;
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

	private static final int[] AIR_STAVES = {ItemID.STAFF_OF_AIR, ItemID.AIR_BATTLESTAFF, ItemID.MYSTIC_AIR_STAFF,
		ItemID.SMOKE_BATTLESTAFF, ItemID.MIST_BATTLESTAFF, ItemID.DUST_BATTLESTAFF};
	private static final int[] FIRE_STAVES = {ItemID.STAFF_OF_FIRE, ItemID.FIRE_BATTLESTAFF, ItemID.MYSTIC_FIRE_STAFF,
		ItemID.LAVA_BATTLESTAFF, ItemID.SMOKE_BATTLESTAFF, ItemID.STEAM_BATTLESTAFF, ItemID.TOME_OF_FIRE};
	private static final int[] WATER_STAVES = {ItemID.STAFF_OF_WATER, ItemID.WATER_BATTLESTAFF,
		ItemID.MYSTIC_WATER_STAFF, ItemID.MUD_BATTLESTAFF, ItemID.STEAM_BATTLESTAFF, ItemID.MIST_BATTLESTAFF};

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
		Location chickens = Location.of("Lumbridge chicken pen", 3231, 3297, 8);
		Location cows = Location.of("Lumbridge cow field", 3257, 3270, 12);
		return List.of(
			Method.builder()
				.id("magic_wind_strike")
				.skill(Skill.MAGIC)
				.type(MethodType.ACTIVITY)
				.name("Wind Strike on chickens")
				.minLevel(1)
				.xpPerAction(7.5)
				.xpPerHour(4_500)
				.tag(MethodTag.CHEAP)
				.location(chickens)
				.bank(Banks.LUMBRIDGE)
				.input(ItemReq.stack("Mind runes", 1, ItemID.MINDRUNE))
				.input(air(1))
				.target(Target.npc("Attack", "Chicken"))
				.instruction("Set Wind Strike to autocast (or click it each time) and attack the chickens")
				.notes("Every cast gives 5.5 xp plus 2 per damage. Splashing still gives the base xp.")
				.wikiPage(F2P_WIKI)
				.build(),
			Method.builder()
				.id("magic_fire_strike")
				.skill(Skill.MAGIC)
				.type(MethodType.ACTIVITY)
				.name("Fire Strike on cows")
				.minLevel(13)
				.xpPerAction(15)
				.xpPerHour(9_000)
				.location(cows)
				.bank(Banks.LUMBRIDGE)
				.input(ItemReq.stack("Mind runes", 1, ItemID.MINDRUNE))
				.input(air(2))
				.input(fire(3))
				.target(Target.npc("Attack", "Cow", "Cow calf"))
				.instruction("Autocast Fire Strike and attack the cows")
				.notes("Wield a staff of fire or air to save runes; the guide stops asking for those runes when you do.")
				.wikiPage(F2P_WIKI)
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
				.xpPerHour(30_000)
				.members(true)
				.tag(MethodTag.BANKSTANDING).tag(MethodTag.PROFIT)
				.location(Banks.GRAND_EXCHANGE)
				.bank(Banks.GRAND_EXCHANGE)
				.input(ItemReq.stack("Cosmic runes", 1, ItemID.COSMICRUNE))
				.input(water(1))
				.input(ItemReq.consumed("Sapphire rings", 27, 1, ItemID.SAPPHIRE_RING))
				.product(ItemID.RING_OF_RECOIL)
				.useItem(ItemID.SAPPHIRE_RING)
				.instruction("Cast Lvl-1 Enchant on each sapphire ring")
				.wikiPage(P2P_WIKI)
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
			.notes("Teleport spells are some of the fastest early Magic xp. Wear a staff to save the elemental runes.")
			.wikiPage(members ? P2P_WIKI : F2P_WIKI);
		for (ItemReq r : runes)
		{
			b.input(r);
		}
		return b;
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
			.notes("Each cast turns the item into " + String.format("%,d", alchValue) + " coins. Buy the items noted; "
				+ "profit or loss depends on today's GE prices, which the guide shows.")
			.wikiPage("High_Level_Alchemy");
	}

	private MagicGuide()
	{
	}
}
