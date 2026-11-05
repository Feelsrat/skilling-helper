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
			anvil("smith_rune_battleaxes", "Rune battleaxes", 95, 225, 3, ItemID.RUNITE_BAR, "Runite bars",
				ItemID.RUNE_BATTLEAXE).tag(MethodTag.PROFIT).build(),
			anvil("smith_mithril_dart_tips", "Mithril dart tips", 54, 50, 1, ItemID.MITHRIL_BAR, "Mithril bars",
				ItemID.MITHRIL_DART_TIP).members(true).build(),
			anvil("smith_adamant_dart_tips", "Adamant dart tips", 74, 62.5, 1, ItemID.ADAMANTITE_BAR, "Adamantite bars",
				ItemID.ADAMANT_DART_TIP).members(true).build(),
			anvil("smith_rune_dart_tips", "Rune dart tips", 89, 75, 1, ItemID.RUNITE_BAR, "Runite bars",
				ItemID.RUNE_DART_TIP).members(true).build(),
			Method.builder()
				.id("smith_cannonballs")
				.skill(Skill.SMITHING)
				.type(MethodType.PROCESS)
				.name("Steel cannonballs (Edgeville)")
				.minLevel(35)
				.xpPerAction(25.6)
				.xpPerHour(9_000)
				.members(true)
				.tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.quest(Quest.DWARF_CANNON)
				.location(EDGEVILLE_FURNACE)
				.bank(Banks.EDGEVILLE)
				.tool(ItemReq.tool("Ammo mould", ItemID.AMMO_MOULD))
				.input(ItemReq.consumed("Steel bars", 27, 1, ItemID.STEEL_BAR))
				.product(ItemID.MCANNONBALL)
				.target(Target.object("Smelt", "Furnace"))
				.useItem(ItemID.STEEL_BAR)
				.instruction("Use the steel bars on the furnace to make cannonballs")
				.notes("Slow xp but very AFK: one inventory takes about 2.5 minutes. Each bar makes 4 cannonballs.")
				.wikiPage(P2P_WIKI)
				.getThere("You need Dwarf Cannon done; the quest gives you the ammo mould (Nulodion at the dwarf "
					+ "camp gives a new one if you lose it).")
				.howTo("The furnace is in the building a few steps east of Edgeville bank: follow the arrow.")
				.howTo("At the bank, take out the ammo mould and 27 steel bars.")
				.howTo("Click a steel bar, then the highlighted furnace. Press Space to make cannonballs from "
					+ "every bar.")
				.howTo("This takes about 2.5 minutes per inventory and keeps going on its own. Then bank the "
					+ "cannonballs and repeat.")
				.build(),
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
				.getThere("You need The Giant Dwarf (to enter Keldagrim) and Family Crest (goldsmith gauntlets). "
					+ "Take the Keldagrim minecart or the Minigame Teleport to the Blast Furnace. Get ice gloves "
					+ "from Bree on top of Ice Mountain (kill her for them).")
				.getThere("Bring coins: below 60 Smithing you pay the Blast Furnace foreman 2,500 gp for every 10 "
					+ "minutes, and the coffer needs coins (put about 72,000 in for an hour) to pay the stokers. "
					+ "Bring stamina potions.")
				.howTo("Wear the goldsmith gauntlets. At the bank chest, take out a full inventory of gold ore.")
				.howTo("Click the highlighted conveyor belt (\"Put-ore-on\") to load all the ore.")
				.howTo("Swap to your ice gloves, walk to the bar dispenser and click it (\"Take\") when the bars "
					+ "are ready. Without ice gloves, cool the bars with a bucket of water first.")
				.howTo("Swap back to the goldsmith gauntlets (they only give the bonus xp if you wear them when "
					+ "the ore goes in), bank the bars, take more ore and repeat.")
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
				.getThere("You need Sleeping Giants done. The Giants' Foundry is in the desert east of Al Kharid; "
					+ "an amulet of glory to Al Kharid gets you close.")
				.getThere("Bring bars for the crucible (28 bars' worth, e.g. 14 mithril and 14 adamantite bars) "
					+ "and a bucket of water, or wear ice gloves.")
				.howTo("Talk to the highlighted Kovac and ask for a commission: he names two words, like \"Narrow "
					+ "Spiked\".")
				.howTo("Click the mould jig and pick the forte, blade and tip moulds whose bars are green for "
					+ "your two words. Then click \"Set mould\".")
				.howTo("Put your bars into the crucible (use them on it) until it holds 28 bars' worth, then "
					+ "click the crucible to pour it into the mould jig. Pick up the finished preform from the "
					+ "jig.")
				.howTo("Watch the bars at the top of the screen: each tool (trip hammer, grindstone, polishing "
					+ "wheel) only works in one heat range. Dip the preform in the lava pool to heat it or in the "
					+ "waterfall to cool it until the arrow is in the right colour, then click that tool.")
				.howTo("Keep working it until the progress bar is full, then hand the sword to Kovac for xp and "
					+ "reputation. Spend reputation at Kovac's shop on the Smiths' uniform.")
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
			.howTo(HowTo.atObject(furnace == EDGEVILLE_FURNACE
					? "The furnace is in the building a few steps east of Edgeville bank"
					: "The furnace is in the building just north of Al Kharid bank",
				"the ore", "the highlighted furnace (\"Smelt\")",
				"click the " + name.toLowerCase().replace("bars", "bar")
					+ " (or press its number key) to smelt them all",
				name.toLowerCase()))
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
			.howTo(HowTo.atObject("The anvils are in the house just south of Varrock West bank",
				"your hammer and the " + barName.toLowerCase(), "a bar, then the highlighted anvil",
				"click the " + name.toLowerCase() + " picture (make sure \"All\" is picked at the top) to make as many "
					+ "as your bars allow", name.toLowerCase()))
			.wikiPage(barsEach == 1 || level < 50 ? F2P_WIKI : P2P_WIKI);
	}

	private SmithingGuide()
	{
	}
}
