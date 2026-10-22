package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import com.skillinghelper.data.ToolType;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Based on the OSRS wiki "Free-to-play Firemaking training" and "Pay-to-play Firemaking training" guides.
 */
final class FiremakingGuide
{
	// Around 1,100 logs an hour lighting lines at the Grand Exchange
	private static final int LOGS_PER_HOUR = 1_100;
	private static final ItemReq TINDERBOX = ItemReq.tool("Tinderbox", ItemID.TINDERBOX);

	static List<Method> methods()
	{
		List<Method> methods = new ArrayList<>();
		methods.add(logs("fm_logs", "Logs", 1, 40, ItemID.LOGS).build());
		methods.add(logs("fm_oak", "Oak logs", 15, 60, ItemID.OAK_LOGS).build());
		methods.add(logs("fm_willow", "Willow logs", 30, 90, ItemID.WILLOW_LOGS).build());
		methods.add(logs("fm_maple", "Maple logs", 45, 135, ItemID.MAPLE_LOGS).build());
		methods.add(logs("fm_yew", "Yew logs", 60, 202.5, ItemID.YEW_LOGS).build());
		methods.add(logs("fm_magic", "Magic logs", 75, 303.8, ItemID.MAGIC_LOGS).build());
		methods.add(Method.builder()
			.id("fm_wintertodt")
			.skill(Skill.FIREMAKING)
			.type(MethodType.ACTIVITY)
			.name("Wintertodt")
			.minLevel(50)
			.xpPerAction(100)
			.xpPerHour(150_000)
			.members(true)
			.tag(MethodTag.MINIGAME).tag(MethodTag.PROFIT).tag(MethodTag.FAST)
			.location(Location.of("Wintertodt camp", 1630, 3965, 15))
			.toolType(ToolType.AXE)
			.tool(TINDERBOX)
			.tool(ItemReq.tool("Knife", ItemID.KNIFE))
			.tool(ItemReq.tool("Hammer", ItemID.HAMMER))
			.target(Target.object("Chop", "Bruma roots"))
			.target(Target.object("Light", "Brazier"))
			.target(Target.object("Feed", "Burning brazier"))
			.instruction("Chop bruma roots, fletch them into kindling with your knife, and feed the burning brazier. "
				+ "Bring food or warm clothing; relight and fix the brazier when needed.")
			.notes("Supply crates give logs, ores, herbs and the Pyromancer outfit.")
			.wikiPage("Wintertodt")
			.rateAt(70, 220_000).rateAt(90, 280_000).rateAt(99, 300_000)
			.build());
		return methods;
	}

	private static Method.MethodBuilder logs(String id, String logName, int level, double xp, int logId)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.FIREMAKING)
			.type(MethodType.PROCESS)
			.name(logName + " (Grand Exchange)")
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * LOGS_PER_HOUR))
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.FAST)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.tool(TINDERBOX)
			.input(ItemReq.consumed(logName, 27, 1, logId))
			.useItem(ItemID.TINDERBOX).useItem(logId)
			.instruction("Stand on a free east-facing lane and use the tinderbox on the " + logName.toLowerCase()
				+ ". You step west after each fire, so keep lighting until the inventory is empty")
			.notes("The Grand Exchange has long clear lanes and a bank in the middle.")
			.wikiPage("Pay-to-play_Firemaking_training");
	}

	private FiremakingGuide()
	{
	}
}
