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
		return m;
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
			.notes("Fast xp; usually close to break-even if you buy the materials.")
			.wikiPage(WIKI);
	}

	private FletchingGuide()
	{
	}
}
