package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
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
 * Based on the OSRS wiki "Herblore training" guide. Every potion is made from a bought unfinished potion and a
 * secondary, so costs come straight from GE prices.
 */
final class HerbloreGuide
{
	private static final String WIKI = "Herblore_training";
	// 14 potions per trip, about 2,500 an hour
	private static final int POTIONS_PER_HOUR = 2_500;

	static List<Method> methods()
	{
		List<Method> m = new ArrayList<>();
		m.add(Method.builder()
			.id("herb_druidic_ritual")
			.skill(Skill.HERBLORE)
			.type(MethodType.ACTIVITY)
			.name("Druidic Ritual (quest)")
			.minLevel(1)
			.xpPerAction(250)
			.xpPerHour(1_000)
			.members(true)
			.location(Location.of("Taverley stone circle", 2925, 3486, 10))
			.target(Target.npc("Talk-to", "Kaqemeex", "Sanfew"))
			.instruction("Talk to Kaqemeex at the Taverley stone circle to start Druidic Ritual")
			.notes("The quest unlocks Herblore and takes you straight to level 3. Use Quest Helper for the steps.")
			.wikiPage("Druidic_Ritual")
			.build());
		m.add(potion("herb_attack", "Attack potions", 3, 25, ItemID.GUAMVIAL, "Guam potions (unf)", ItemID.GUAM_LEAF,
			ItemID.EYE_OF_NEWT, "Eyes of newt", ItemID._3DOSE1ATTACK).tag(MethodTag.CHEAP).build());
		m.add(potion("herb_antipoison", "Antipoisons", 5, 37.5, ItemID.MARRENTILLVIAL, "Marrentill potions (unf)",
			ItemID.MARENTILL, ItemID.UNICORN_HORN_DUST, "Unicorn horn dust", ItemID._3DOSEANTIPOISON).build());
		m.add(potion("herb_strength", "Strength potions", 12, 50, ItemID.TARROMINVIAL, "Tarromin potions (unf)",
			ItemID.TARROMIN, ItemID.LIMPWURT_ROOT, "Limpwurt roots", ItemID._3DOSE1STRENGTH).build());
		m.add(potion("herb_energy", "Energy potions", 26, 67.5, ItemID.HARRALANDERVIAL, "Harralander potions (unf)",
			ItemID.HARRALANDER, ItemID.CHOCOLATE_DUST, "Chocolate dust", ItemID._3DOSE1ENERGY).build());
		m.add(potion("herb_prayer", "Prayer potions", 38, 87.5, ItemID.RANARRVIAL, "Ranarr potions (unf)",
			ItemID.RANARR_WEED, ItemID.SNAPE_GRASS, "Snape grass", ItemID._3DOSEPRAYERRESTORE)
			.tag(MethodTag.PROFIT).build());
		m.add(potion("herb_super_attack", "Super attack potions", 45, 100, ItemID.IRITVIAL, "Irit potions (unf)",
			ItemID.IRIT_LEAF, ItemID.EYE_OF_NEWT, "Eyes of newt", ItemID._3DOSE2ATTACK).build());
		m.add(potion("herb_super_strength", "Super strength potions", 55, 125, ItemID.KWUARMVIAL,
			"Kwuarm potions (unf)", ItemID.KWUARM, ItemID.LIMPWURT_ROOT, "Limpwurt roots", ItemID._3DOSE2STRENGTH)
			.build());
		m.add(potion("herb_super_restore", "Super restores", 63, 142.5, ItemID.SNAPDRAGONVIAL,
			"Snapdragon potions (unf)", ItemID.SNAPDRAGON, ItemID.RED_SPIDERS_EGGS, "Red spiders' eggs",
			ItemID._3DOSE2RESTORE).build());
		m.add(potion("herb_super_defence", "Super defence potions", 66, 150, ItemID.CADANTINEVIAL,
			"Cadantine potions (unf)", ItemID.CADANTINE, ItemID.WHITE_BERRIES, "White berries", ItemID._3DOSE2DEFENSE)
			.build());
		m.add(potion("herb_ranging", "Ranging potions", 72, 162.5, ItemID.DWARFWEEDVIAL, "Dwarf weed potions (unf)",
			ItemID.DWARF_WEED, ItemID.WINE_OF_ZAMORAK, "Wines of Zamorak", ItemID._3DOSERANGERSPOTION).build());
		m.add(potion("herb_magic", "Magic potions", 76, 172.5, ItemID.LANTADYMEVIAL, "Lantadyme potions (unf)",
			ItemID.LANTADYME, ItemID.CACTUS_POTATO, "Potato cacti", ItemID._3DOSE1MAGIC).build());
		m.add(potion("herb_saradomin_brew", "Saradomin brews", 81, 180, ItemID.TOADFLAXVIAL, "Toadflax potions (unf)",
			ItemID.TOADFLAX, ItemID.CRUSHED_BIRD_NEST, "Crushed nests", ItemID._3DOSEPOTIONOFSARADOMIN).build());
		return m;
	}

	/**
	 * Secondaries the player can grind themselves from a cheaper item, e.g. unicorn horns into dust.
	 */
	private static ItemReq ground(String name, int secondary)
	{
		ItemReq req = ItemReq.consumed(name, 14, 1, secondary);
		switch (secondary)
		{
			case ItemID.UNICORN_HORN_DUST:
				return grind(req, "Unicorn horns", ItemID.UNICORN_HORN);
			case ItemID.CHOCOLATE_DUST:
				return grind(req, "Chocolate bars", ItemID.CHOCOLATE_BAR);
			case ItemID.CRUSHED_BIRD_NEST:
				return grind(req, "Empty bird nests", ItemID.BIRD_NEST_EMPTY);
			default:
				return req;
		}
	}

	private static ItemReq grind(ItemReq req, String fromName, int from)
	{
		return req.madeFrom("Use the pestle and mortar on the " + fromName.toLowerCase(), false,
			ItemReq.part(fromName, 1, from), ItemReq.tool("Pestle and mortar", ItemID.PESTLE_AND_MORTAR));
	}

	private static Method.MethodBuilder potion(String id, String name, int level, double xp, int unf, String unfName,
		int herb, int secondary, String secondaryName, int product)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.HERBLORE)
			.type(MethodType.PROCESS)
			.name("Make " + name.toLowerCase())
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * POTIONS_PER_HOUR))
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.FAST)
			.quest(Quest.DRUIDIC_RITUAL)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.consumed(unfName, 14, 1, unf).madeFrom("Use the clean herbs on vials of water (clean any "
					+ "grimy herbs first)", false,
				ItemReq.part("Clean " + unfName.substring(0, unfName.indexOf(' ')).toLowerCase(), 1, herb),
				ItemReq.part("Vials of water", 1, ItemID.VIAL_WATER)))
			.input(ground(secondaryName, secondary))
			.product(product)
			.useItem(secondary).useItem(unf)
			.instruction("Use the " + secondaryName.toLowerCase() + " on the " + unfName.toLowerCase()
				+ " and press Space to make all")
			.notes("If you have the herbs and vials, or they're cheaper than unfinished potions, the guide has you make "
				+ "the unfinished potions first. An amulet of chemistry gives a chance of 4-dose potions.")
			.wikiPage(WIKI);
	}

	private HerbloreGuide()
	{
	}
}
