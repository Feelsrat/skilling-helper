package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
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
 * Based on the OSRS wiki "Free-to-play Smithing training" and "Pay-to-play Smithing training" guides. Platebodies
 * use five bars at once, which is the fastest way to turn bars into xp at an anvil.
 */
final class SmithingGuide
{
	private static final String F2P_WIKI = "Free-to-play_Smithing_training";
	private static final String P2P_WIKI = "Pay-to-play_Smithing_training";
	private static final ItemReq HAMMER = ItemReq.tool("Hammer", ItemID.HAMMER);
	private static final Location AL_KHARID_FURNACE = Location.of("Al Kharid furnace", 3275, 3186, 4);
	private static final Location EDGEVILLE_FURNACE = Location.of("Edgeville furnace", 3109, 3499, 4);
	private static final Location VARROCK_ANVIL = Location.of("Varrock West anvils", 3188, 3425, 4);
	// Bars smelted per hour at a furnace next to a bank
	private static final int SMELTS_PER_HOUR = 1_300;

	static List<Method> methods()
	{
		return List.of(
			smelt("smith_bronze_bars", "Bronze bars", 1, 6.2, AL_KHARID_FURNACE, Banks.AL_KHARID, ItemID.BRONZE_BAR)
				.input(ItemReq.consumed("Copper ore", 14, 1, ItemID.COPPER_ORE))
				.input(ItemReq.consumed("Tin ore", 14, 1, ItemID.TIN_ORE))
				.tag(MethodTag.CHEAP)
				.notes("Mine the ore yourself for a free start, or buy it cheaply.")
				.build(),
			smelt("smith_iron_bars", "Iron bars", 15, 12.5, EDGEVILLE_FURNACE, Banks.EDGEVILLE, ItemID.IRON_BAR)
				.input(ItemReq.consumed("Iron ore", 28, 1, ItemID.IRON_ORE))
				.notes("Only half of iron ore smelts into bars unless you wear a ring of forging (from the GE).")
				.build(),
			smelt("smith_steel_bars", "Steel bars", 30, 17.5, EDGEVILLE_FURNACE, Banks.EDGEVILLE, ItemID.STEEL_BAR)
				.input(ItemReq.consumed("Iron ore", 9, 1, ItemID.IRON_ORE))
				.input(ItemReq.consumed("Coal", 18, 2, ItemID.COAL))
				.notes("Each steel bar needs 1 iron ore and 2 coal.")
				.build(),
			smelt("smith_gold_bars", "Gold bars", 40, 22.5, EDGEVILLE_FURNACE, Banks.EDGEVILLE, ItemID.GOLD_BAR)
				.input(ItemReq.consumed("Gold ore", 28, 1, ItemID.GOLD_ORE))
				.tag(MethodTag.FAST)
				.notes("Members: goldsmith gauntlets (Family Crest) raise this to 56.2 xp per bar.")
				.build(),
			anvil("smith_bronze_daggers", "Bronze daggers", 1, 12.5, 1, ItemID.BRONZE_BAR, "Bronze bars",
				ItemID.BRONZE_DAGGER).tag(MethodTag.CHEAP).build(),
			anvil("smith_bronze_platebodies", "Bronze platebodies", 18, 62.5, 5, ItemID.BRONZE_BAR, "Bronze bars",
				ItemID.BRONZE_PLATEBODY).build(),
			anvil("smith_iron_platebodies", "Iron platebodies", 33, 125, 5, ItemID.IRON_BAR, "Iron bars",
				ItemID.IRON_PLATEBODY).build(),
			anvil("smith_steel_platebodies", "Steel platebodies", 48, 187.5, 5, ItemID.STEEL_BAR, "Steel bars",
				ItemID.STEEL_PLATEBODY).build(),
			anvil("smith_mithril_platebodies", "Mithril platebodies", 68, 250, 5, ItemID.MITHRIL_BAR, "Mithril bars",
				ItemID.MITHRIL_PLATEBODY).members(true).build(),
			anvil("smith_adamant_platebodies", "Adamant platebodies", 88, 312.5, 5, ItemID.ADAMANTITE_BAR,
				"Adamantite bars", ItemID.ADAMANT_PLATEBODY).members(true).build(),
			Method.builder()
				.id("smith_blast_furnace_gold")
				.skill(Skill.SMITHING)
				.type(MethodType.ACTIVITY)
				.name("Blast Furnace gold bars")
				.minLevel(40)
				.xpPerAction(56.2)
				.xpPerHour(330_000)
				.members(true)
				.tag(MethodTag.FAST).tag(MethodTag.MINIGAME)
				.quest(Quest.THE_GIANT_DWARF).quest(Quest.FAMILY_CREST)
				.location(Location.of("Blast Furnace", 1940, 4962, 15))
				.input(ItemReq.consumed("Gold ore", 27, 1, ItemID.GOLD_ORE))
				.product(ItemID.GOLD_BAR)
				.target(Target.object("Put-ore-on", "Conveyor belt"))
				.target(Target.object("Take", "Bar dispenser"))
				.target(Target.object(null, "Bank chest"))
				.instruction("Put gold ore on the conveyor belt, swap to ice gloves and take the bars from the dispenser, "
					+ "then swap back to goldsmith gauntlets")
				.notes("Below 60 Smithing you pay the foreman 2,500 gp per 10 minutes, and the coffer needs coins "
					+ "to pay the stokers.")
				.requirementText("Goldsmith gauntlets (Family Crest) and ice gloves (Bree, Ice Mountain)")
				.supply(Supplies.COINS)
				.supply(Supplies.STAMINA)
				.wikiPage("Blast_Furnace")
				.build(),
			Method.builder()
				.id("smith_giants_foundry")
				.skill(Skill.SMITHING)
				.type(MethodType.ACTIVITY)
				.name("Giants' Foundry")
				.minLevel(15)
				.xpPerAction(1_000)
				.xpPerHour(35_000)
				.members(true)
				.tag(MethodTag.MINIGAME).tag(MethodTag.AFK)
				.quest(Quest.SLEEPING_GIANTS)
				.supply(ItemReq.supply("Bars for the sword (e.g. 14 mithril + 14 adamantite)", 28, ItemID.MITHRIL_BAR,
					ItemID.ADAMANTITE_BAR, ItemID.STEEL_BAR, ItemID.IRON_BAR, ItemID.RUNITE_BAR))
				.location(Location.of("Giants' Foundry", 3360, 3150, 20))
				.target(Target.object(null, "Trip hammer", "Grindstone", "Polishing wheel", "Crucible (empty)",
					"Crucible (partially full)", "Crucible (full)", "Mould jig (Empty)", "Lava pool", "Waterfall"))
				.target(Target.npc(null, "Kovac"))
				.instruction("Fill the crucible with bars, pour it into the mould and work the sword at the trip hammer, "
					+ "grindstone and polishing wheel while keeping the heat in range")
				.notes("Uses bars you already have. Earns the Smiths' uniform, which makes smithing faster.")
				.wikiPage("Giants%27_Foundry")
				.rateAt(50, 60_000).rateAt(70, 80_000).rateAt(90, 100_000)
				.build()
		);
	}

	private static Method.MethodBuilder smelt(String id, String name, int level, double xp, Location furnace,
		Location bank, int bar)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.SMITHING)
			.type(MethodType.PROCESS)
			.name("Smelt " + name.toLowerCase() + " (" + furnace.getName() + ")")
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * SMELTS_PER_HOUR))
			.location(furnace)
			.bank(bank)
			.product(bar)
			.target(Target.object("Smelt", "Furnace"))
			.instruction("Click the furnace and choose " + name.toLowerCase() + ", then press Space to smelt them all")
			.wikiPage(F2P_WIKI);
	}

	private static Method.MethodBuilder anvil(String id, String name, int level, double xp, int barsEach, int bar,
		String barName, int product)
	{
		// About 1,800 bars an hour including banking
		int actionsPerHour = 1_800 / barsEach;
		return Method.builder()
			.id(id)
			.skill(Skill.SMITHING)
			.type(MethodType.PROCESS)
			.name("Smith " + name.toLowerCase() + " (Varrock)")
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * actionsPerHour))
			.tag(MethodTag.AFK)
			.location(VARROCK_ANVIL)
			.bank(Banks.VARROCK_WEST)
			.tool(HAMMER)
			.input(ItemReq.consumed(barName, barsEach == 1 ? 27 : 25, barsEach, bar))
			.product(product)
			.target(Target.object("Smith", "Anvil"))
			.useItem(bar)
			.instruction("Use the bars on the anvil and make " + name.toLowerCase())
			.wikiPage(barsEach == 1 || level < 50 ? F2P_WIKI : P2P_WIKI);
	}

	private SmithingGuide()
	{
	}
}
