package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Based on the OSRS wiki "Free-to-play Fletching training" and "Pay-to-play Fletching training" guides.
 * All Fletching is bank-standing, so every method happens at the Grand Exchange.
 */
final class FletchingGuide
{
	private static final String WIKI = "Pay-to-play_Fletching_training";
	private static final ItemReq KNIFE = ItemReq.tool("Knife", ItemID.KNIFE);
	// Rough actions per hour, including banking
	private static final int CUTS_PER_HOUR = 1_700;
	private static final int STRINGS_PER_HOUR = 2_400;

	static List<Method> methods()
	{
		List<Method> m = new ArrayList<>();
		m.add(cut("fletch_arrow_shafts", "Arrow shafts", 1, 5, "Logs", ItemID.LOGS, ItemID.ARROW_SHAFT)
			.tag(MethodTag.CHEAP).build());
		m.add(cut("fletch_shortbow", "Shortbows (u)", 5, 5, "Logs", ItemID.LOGS, ItemID.UNSTRUNG_SHORTBOW).build());
		m.add(cut("fletch_longbow", "Longbows (u)", 10, 10, "Logs", ItemID.LOGS, ItemID.UNSTRUNG_LONGBOW).build());
		m.add(cut("fletch_oak_short", "Oak shortbows (u)", 20, 16.5, "Oak logs", ItemID.OAK_LOGS, ItemID.UNSTRUNG_OAK_SHORTBOW).build());
		m.add(cut("fletch_oak_long", "Oak longbows (u)", 25, 25, "Oak logs", ItemID.OAK_LOGS, ItemID.UNSTRUNG_OAK_LONGBOW).build());
		m.add(cut("fletch_willow_short", "Willow shortbows (u)", 35, 33.3, "Willow logs", ItemID.WILLOW_LOGS, ItemID.UNSTRUNG_WILLOW_SHORTBOW).build());
		m.add(cut("fletch_willow_long", "Willow longbows (u)", 40, 41.5, "Willow logs", ItemID.WILLOW_LOGS, ItemID.UNSTRUNG_WILLOW_LONGBOW).build());
		m.add(cut("fletch_maple_short", "Maple shortbows (u)", 50, 50, "Maple logs", ItemID.MAPLE_LOGS, ItemID.UNSTRUNG_MAPLE_SHORTBOW).build());
		m.add(cut("fletch_maple_long", "Maple longbows (u)", 55, 58.3, "Maple logs", ItemID.MAPLE_LOGS, ItemID.UNSTRUNG_MAPLE_LONGBOW).build());
		m.add(cut("fletch_yew_short", "Yew shortbows (u)", 65, 67.5, "Yew logs", ItemID.YEW_LOGS, ItemID.UNSTRUNG_YEW_SHORTBOW).build());
		m.add(cut("fletch_yew_long", "Yew longbows (u)", 70, 75, "Yew logs", ItemID.YEW_LOGS, ItemID.UNSTRUNG_YEW_LONGBOW).build());
		m.add(cut("fletch_magic_short", "Magic shortbows (u)", 80, 83.3, "Magic logs", ItemID.MAGIC_LOGS, ItemID.UNSTRUNG_MAGIC_SHORTBOW).build());
		m.add(cut("fletch_magic_long", "Magic longbows (u)", 85, 91.5, "Magic logs", ItemID.MAGIC_LOGS, ItemID.UNSTRUNG_MAGIC_LONGBOW).build());

		m.add(string("fletch_string_willow_long", "Willow longbows", 40, 41.5, ItemID.UNSTRUNG_WILLOW_LONGBOW, ItemID.WILLOW_LONGBOW).build());
		m.add(string("fletch_string_maple_long", "Maple longbows", 55, 58.3, ItemID.UNSTRUNG_MAPLE_LONGBOW, ItemID.MAPLE_LONGBOW).build());
		m.add(string("fletch_string_yew_long", "Yew longbows", 70, 75, ItemID.UNSTRUNG_YEW_LONGBOW, ItemID.YEW_LONGBOW).build());
		m.add(string("fletch_string_magic_long", "Magic longbows", 85, 91.5, ItemID.UNSTRUNG_MAGIC_LONGBOW, ItemID.MAGIC_LONGBOW).build());

		m.add(ammo("fletch_headless_arrows", "Headless arrows", 1, 15, "Arrow shafts", ItemID.ARROW_SHAFT, 15,
			ItemID.HEADLESS_ARROW).tag(MethodTag.CHEAP).build());
		m.add(ammo("fletch_adamant_arrows", "Adamant arrows", 60, 150, "Adamant arrowheads", ItemID.ADAMANT_ARROWHEADS,
			15, ItemID.ADAMANT_ARROW, ItemID.HEADLESS_ARROW, "Headless arrows").build());
		m.add(ammo("fletch_rune_arrows", "Rune arrows", 75, 187.5, "Rune arrowheads", ItemID.RUNE_ARROWHEADS, 15,
			ItemID.RUNE_ARROW, ItemID.HEADLESS_ARROW, "Headless arrows").build());
		m.add(ammo("fletch_steel_darts", "Steel darts", 37, 75, "Steel dart tips", ItemID.STEEL_DART_TIP, 10,
			ItemID.STEEL_DART).build());
		m.add(ammo("fletch_mithril_darts", "Mithril darts", 52, 112, "Mithril dart tips", ItemID.MITHRIL_DART_TIP, 10,
			ItemID.MITHRIL_DART).build());
		m.add(ammo("fletch_adamant_darts", "Adamant darts", 67, 150, "Adamant dart tips", ItemID.ADAMANT_DART_TIP, 10,
			ItemID.ADAMANT_DART).build());
		m.add(ammo("fletch_rune_darts", "Rune darts", 81, 188, "Rune dart tips", ItemID.RUNE_DART_TIP, 10,
			ItemID.RUNE_DART).build());
		m.add(ammo("fletch_adamant_bolts", "Adamant bolts", 61, 70, "Adamant bolts (unf)",
			ItemID.XBOWS_CROSSBOW_BOLTS_ADAMANTITE_UNFEATHERED, 10, ItemID.XBOWS_CROSSBOW_BOLTS_ADAMANTITE).build());
		m.add(ammo("fletch_runite_bolts", "Runite bolts", 69, 100, "Runite bolts (unf)",
			ItemID.XBOWS_CROSSBOW_BOLTS_RUNITE_UNFEATHERED, 10, ItemID.XBOWS_CROSSBOW_BOLTS_RUNITE).build());

		// From the wiki's AFK guide: one click does a whole inventory
		m.add(cut("fletch_battlestaves", "Battlestaves", 40, 80, "Celastrus bark", ItemID.CELASTRUS_WOOD,
			ItemID.BATTLESTAFF)
			.notes("The best Fletching xp from 40 to 80, and a fletching knife makes it faster. Celastrus bark comes "
				+ "from the Farming Guild's celastrus tree or the GE.")
			.wikiPage("Fletching_training#Level_40–80:_Battlestaves")
			.build());
		m.add(Method.builder()
			.id("fletch_redwood_shields")
			.skill(Skill.FLETCHING)
			.type(MethodType.PROCESS)
			.name("Cut redwood shields")
			.minLevel(92)
			.xpPerAction(216)
			// About 780 shields an hour (wiki: 168k xp/hr, 193k with a fletching knife)
			.xpPerHour(216 * 780)
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.AFK)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.tool(KNIFE)
			.input(ItemReq.consumed("Redwood logs", 26, 2, ItemID.REDWOOD_LOGS))
			.product(ItemID.REDWOOD_SHIELD)
			.useItem(ItemID.KNIFE).useItem(ItemID.REDWOOD_LOGS)
			.instruction("Use the knife on the redwood logs and choose the redwood shield")
			.notes("Each shield takes two redwood logs. A fletching knife makes it faster.")
			.wikiPage("Fletching_training#Level_92–99:_Redwood_shields")
			.howTo(HowTo.bankStanding("your knife and 26 redwood logs",
				"the knife, then a log (in the menu pick the redwood shield)", "redwood shields"))
			.build());
		m.add(Method.builder()
			.id("fletch_diamond_bolt_tips")
			.skill(Skill.FLETCHING)
			.type(MethodType.PROCESS)
			.name("Cut diamond bolt tips")
			.minLevel(65)
			.xpPerAction(7)
			// About 1,200 diamonds an hour (wiki AFK guide: 8,400 xp/hr)
			.xpPerHour(7 * 1_200)
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.AFK)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.tool(ItemReq.tool("Chisel", ItemID.CHISEL))
			.input(ItemReq.consumed("Diamonds (cut)", 27, 1, ItemID.DIAMOND))
			.product(ItemID.XBOWS_BOLT_TIPS_DIAMOND)
			.useItem(ItemID.CHISEL).useItem(ItemID.DIAMOND)
			.instruction("Use the chisel on the diamonds and pick the bolt tips; each diamond makes 12")
			.notes("Low xp, but over a minute between clicks. Any cut gem works the same way (opal from 11, sapphire "
				+ "56, ruby 63, dragonstone 71, onyx 73).")
			.wikiPage("Bolt_tips")
			.howTo(HowTo.bankStanding("your chisel and 27 cut diamonds",
				"the chisel, then a diamond (in the menu pick the bolt tips)", "diamond bolt tips"))
			.build());
		return m;
	}

	/**
	 * Stackable ammo made 10 or 15 at a time from two stacks (tips and feathers, or tips and headless arrows). The
	 * wiki counts these as zero-time methods: about one set per game tick when clicking steadily.
	 */
	private static Method.MethodBuilder ammo(String id, String name, int level, double xpPerSet, String partName,
		int part, int perSet, int product)
	{
		return ammo(id, name, level, xpPerSet, partName, part, perSet, product, ItemID.FEATHER, "Feathers");
	}

	private static Method.MethodBuilder ammo(String id, String name, int level, double xpPerSet, String partName,
		int part, int perSet, int product, int other, String otherName)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.FLETCHING)
			.type(MethodType.PROCESS)
			.name(name)
			.minLevel(level)
			.xpPerAction(xpPerSet)
			// About 1,500 sets an hour clicking at a steady pace
			.xpPerHour((int) (xpPerSet * 1_500))
			.members(level > 1)
			.tag(MethodTag.FAST).tag(MethodTag.BANKSTANDING)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.stack(otherName, perSet, other))
			.input(ItemReq.stack(partName, perSet, part))
			.product(product)
			.useItem(part)
			.instruction("Use the " + partName.toLowerCase() + " on the " + otherName.toLowerCase()
				+ " to make " + perSet + " " + name.toLowerCase() + " at a time")
			.notes("Everything stacks, so you never bank and can do this anywhere, even while running. "
				+ "Very fast xp, but the cost depends on today's prices.")
			.howTo("Take the " + partName.toLowerCase() + " and " + otherName.toLowerCase() + " out of the bank (the "
				+ "guide highlights them).")
			.howTo(HowTo.stackable(partName.toLowerCase(), otherName.toLowerCase(), name.toLowerCase()))
			.wikiPage(WIKI);
	}

	private static Method.MethodBuilder cut(String id, String productName, int level, double xp, String logName, int log,
		int product)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.FLETCHING)
			.type(MethodType.PROCESS)
			.name("Cut " + productName.toLowerCase())
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * CUTS_PER_HOUR))
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.AFK)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.tool(KNIFE)
			.input(ItemReq.consumed(logName, 27, 1, log))
			.product(product)
			.useItem(ItemID.KNIFE).useItem(log)
			.instruction("Use the knife on the " + logName.toLowerCase() + " and choose " + productName.toLowerCase())
			.howTo(HowTo.bankStanding("your knife and 27 " + logName.toLowerCase(),
				"the knife, then a log (in the menu pick " + productName.toLowerCase() + ")",
				productName.toLowerCase()))
			.wikiPage(WIKI);
	}

	private static Method.MethodBuilder string(String id, String bowName, int level, double xp, int unstrung, int strung)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.FLETCHING)
			.type(MethodType.PROCESS)
			.name("String " + bowName.toLowerCase())
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * STRINGS_PER_HOUR))
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.FAST)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.consumed("Bow strings", 14, 1, ItemID.BOW_STRING))
			.input(ItemReq.consumed("Unstrung " + bowName.toLowerCase(), 14, 1, unstrung))
			.product(strung)
			.useItem(ItemID.BOW_STRING).useItem(unstrung)
			.instruction("Use the bow strings on the unstrung bows")
			.howTo(HowTo.bankStanding("14 bow strings and 14 unstrung " + bowName.toLowerCase(),
				"a bow string, then an unstrung bow", bowName.toLowerCase()))
			.notes("Fast xp; usually close to break-even if you buy the materials.")
			.wikiPage(WIKI);
	}

	private FletchingGuide()
	{
	}
}
