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
			.wikiPage(members ? P2P_WIKI : F2P_WIKI);
	}

	private CookingGuide()
	{
	}
}
