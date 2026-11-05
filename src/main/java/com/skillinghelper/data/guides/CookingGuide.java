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
 * Based on the OSRS wiki "Free-to-play Cooking training" and "Pay-to-play Cooking training" guides.
 */
final class CookingGuide
{
	private static final String F2P_WIKI = "Free-to-play_Cooking_training";
	private static final String P2P_WIKI = "Pay-to-play_Cooking_training";
	private static final Location AL_KHARID_RANGE = Location.of("Al Kharid range", 3273, 3180, 3);
	private static final Location CATHERBY_RANGE = Location.of("Catherby range", 2817, 3443, 3);
	// Roughly 1,300 fish an hour on a range including banking
	private static final int FISH_PER_HOUR = 1_300;

	static List<Method> methods()
	{
		List<Method> methods = new ArrayList<>();
		methods.add(fish("cook_shrimp", "Shrimp", 1, 30, ItemID.RAW_SHRIMP, ItemID.SHRIMP, false).build());
		methods.add(fish("cook_trout", "Trout", 15, 70, ItemID.RAW_TROUT, ItemID.TROUT, false).build());
		methods.add(fish("cook_salmon", "Salmon", 25, 90, ItemID.RAW_SALMON, ItemID.SALMON, false).build());
		methods.add(fish("cook_tuna", "Tuna", 30, 100, ItemID.RAW_TUNA, ItemID.TUNA, false).build());
		methods.add(fish("cook_lobster", "Lobsters", 40, 120, ItemID.RAW_LOBSTER, ItemID.LOBSTER, false)
			.tag(MethodTag.PROFIT).build());
		methods.add(fish("cook_swordfish", "Swordfish", 45, 140, ItemID.RAW_SWORDFISH, ItemID.SWORDFISH, false).build());
		methods.add(fish("cook_monkfish", "Monkfish", 62, 150, ItemID.RAW_MONKFISH, ItemID.MONKFISH, true)
			.quest(Quest.SWAN_SONG).build());
		methods.add(fish("cook_shark", "Sharks", 80, 210, ItemID.RAW_SHARK, ItemID.SHARK, true)
			.tag(MethodTag.PROFIT).build());
		methods.add(fish("cook_anglerfish", "Anglerfish", 84, 230, ItemID.RAW_ANGLERFISH, ItemID.ANGLERFISH, true)
			.tag(MethodTag.PROFIT).build());
		methods.add(fish("cook_dark_crab", "Dark crabs", 90, 215, ItemID.RAW_DARK_CRAB, ItemID.DARK_CRAB, true)
			.tag(MethodTag.PROFIT).build());
		methods.add(Method.builder()
			.id("cook_pineapple_pizza")
			.skill(Skill.COOKING)
			.type(MethodType.PROCESS)
			.name("Pineapple pizzas (bank-standing)")
			.minLevel(65)
			.xpPerAction(45)
			.xpPerHour(45 * 2_400)
			.members(true)
			.tag(MethodTag.BANKSTANDING).tag(MethodTag.PROFIT)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.consumed("Plain pizzas", 14, 1, ItemID.PLAIN_PIZZA))
			.input(ItemReq.consumed("Pineapple rings (or chunks)", 14, 1, ItemID.PINEAPPLE_RING,
				ItemID.PINEAPPLE_CHUNKS))
			.product(ItemID.PINEAPPLE_PIZZA)
			.useItem(ItemID.PINEAPPLE_RING).useItem(ItemID.PINEAPPLE_CHUNKS).useItem(ItemID.PLAIN_PIZZA)
			.instruction("Use the pineapple rings on the plain pizzas and press Space to make them all")
			.notes("A wiki money maker: no range needed and every pizza succeeds.")
			.wikiPage("Money_making_guide/Making_pineapple_pizzas")
			.howTo("Stand next to any banker or bank booth at the Grand Exchange.")
			.howTo("Take out 14 plain pizzas and 14 pineapple rings (the guide highlights them). If you only have "
				+ "pineapples, use a knife on them first to cut them into rings.")
			.howTo("Click a pineapple ring, then click a plain pizza. Press Space to make all 14.")
			.howTo("When they're done, open the bank, click \"Deposit inventory\" and take out 14 of each again.")
			.build());
		methods.add(Method.builder()
			.id("cook_wine")
			.skill(Skill.COOKING)
			.type(MethodType.PROCESS)
			.name("Jugs of wine (bank-standing)")
			.minLevel(35)
			.xpPerAction(200)
			.xpPerHour(400_000)
			.members(true)
			.tag(MethodTag.FAST).tag(MethodTag.BANKSTANDING)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.consumed("Grapes", 14, 1, ItemID.GRAPES))
			.input(ItemReq.consumed("Jugs of water", 14, 1, ItemID.JUG_WATER))
			.product(ItemID.JUG_UNFERMENTED_WINE)
			.useItem(ItemID.GRAPES).useItem(ItemID.JUG_WATER)
			.instruction("Use grapes on the jugs of water, then bank and repeat")
			.notes("The fastest Cooking xp in the game. Xp is granted when the wine ferments a few seconds later.")
			.wikiPage(P2P_WIKI)
			.howTo("Stand next to any banker or bank booth at the Grand Exchange.")
			.howTo("Take out 14 grapes and 14 jugs of water (the guide highlights them).")
			.howTo("Click the grapes, then click a jug of water. Press Space to make all 14.")
			.howTo("The xp comes a few seconds later when the wine ferments, even if you're already banking. "
				+ "Deposit the unfermented wine and repeat.")
			.build());
		methods.add(Method.builder()
			.id("cook_karambwan")
			.skill(Skill.COOKING)
			.type(MethodType.PROCESS)
			.name("Karambwans (Catherby)")
			.minLevel(30)
			.xpPerAction(190)
			.xpPerHour(250_000)
			.members(true)
			.tag(MethodTag.FAST)
			.location(CATHERBY_RANGE)
			.bank(Banks.CATHERBY)
			.quest(Quest.TAI_BWO_WANNAI_TRIO)
			.input(ItemReq.consumed("Raw karambwan", 28, 1, ItemID.TBWT_RAW_KARAMBWAN))
			.product(ItemID.TBWT_COOKED_KARAMBWAN)
			.target(Target.object("Cook", "Range"))
			.instruction("Use raw karambwan on the range. 1-tick cooking gives much faster xp")
			.wikiPage("1-tick_karambwan")
			.getThere("First time only: finish Tai Bwo Wannai Trio and talk to Tinsay in Tai Bwo Wannai, or every "
				+ "karambwan comes out poisoned.")
			.howTo("The range is in the house just east of Catherby bank: follow the arrow. Take out 28 raw "
				+ "karambwan.")
			.howTo("Slow way: use a raw karambwan on the range and press Space to cook them all.")
			.howTo("Fast way (1-tick): hold down the 1 key and keep using raw karambwan on the range, one after "
				+ "another. Each one cooks instantly.")
			.build());
		return methods;
	}

	private static Method.MethodBuilder fish(String id, String fishName, int level, double xp, int raw, int cooked,
		boolean members)
	{
		Location range = members ? CATHERBY_RANGE : AL_KHARID_RANGE;
		return Method.builder()
			.id(id)
			.skill(Skill.COOKING)
			.type(MethodType.PROCESS)
			.name(fishName + " (" + range.getName() + ")")
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * FISH_PER_HOUR))
			.members(members)
			.tag(MethodTag.AFK)
			.location(range)
			.bank(members ? Banks.CATHERBY : Banks.AL_KHARID)
			.input(ItemReq.consumed("Raw " + fishName.toLowerCase(), 28, 1, raw))
			.product(cooked)
			.target(Target.object("Cook", "Range"))
			.instruction("Use the raw " + fishName.toLowerCase() + " on the range and press Space to cook all")
			.notes("Cooking fish you caught yourself is free xp. Buying raw fish usually costs a little per xp.")
			.howTo(members ? "The range is in the house just east of Catherby bank: follow the arrow."
				: "The range is in the small house just north of Al Kharid bank: follow the arrow.")
			.howTo("At the bank, take out 28 raw " + fishName.toLowerCase() + " (the guide highlights them).")
			.howTo("Click a raw " + fishName.toLowerCase() + " in your inventory, then click the highlighted range. A "
				+ "menu pops up: press Space (or click the fish picture) to cook them all.")
			.howTo("Wait while your character cooks the whole inventory. Some fish burn at first; that's normal and "
				+ "happens less as you level.")
			.howTo("When everything is cooked, the guide sends you back to the bank to put the fish away and take out "
				+ "more.")
			.wikiPage(members ? P2P_WIKI : F2P_WIKI);
	}

	private CookingGuide()
	{
	}
}
