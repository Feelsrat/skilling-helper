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
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Based on the OSRS wiki "Free-to-play Crafting training" and "Pay-to-play Crafting training" guides. Almost every
 * method turns bought materials into products, so the planner compares material cost with the product's GE price
 * (after tax) to show what each level costs or earns.
 */
final class CraftingGuide
{
	private static final String F2P_WIKI = "Free-to-play_Crafting_training";
	private static final String P2P_WIKI = "Pay-to-play_Crafting_training";
	private static final ItemReq NEEDLE = ItemReq.tool("Needle", ItemID.NEEDLE);
	private static final ItemReq THREAD = ItemReq.tool("Thread", ItemID.THREAD);
	private static final ItemReq CHISEL = ItemReq.tool("Chisel", ItemID.CHISEL);
	private static final ItemReq PIPE = ItemReq.tool("Glassblowing pipe", ItemID.GLASSBLOWINGPIPE);
	private static final ItemReq RING_MOULD = ItemReq.tool("Ring mould", ItemID.RING_MOULD);
	private static final Location EDGEVILLE_FURNACE = Location.of("Edgeville furnace", 3109, 3499, 4);
	private static final Target FURNACE = Target.object("Smelt", "Furnace");
	private static final Location TANNER = Location.of("Al Kharid tanner (Ellis)", 3274, 3191, 4);
	private static final ItemReq MOLTEN_GLASS = ItemReq.consumed("Molten glass", 27, 1, ItemID.MOLTEN_GLASS)
		.madeFrom("Use the buckets of sand on the furnace with soda ash in your inventory", false,
			ItemReq.part("Buckets of sand", 1, ItemID.BUCKET_SAND),
			ItemReq.part("Soda ash", 1, ItemID.SODA_ASH))
		.madeAt(EDGEVILLE_FURNACE, FURNACE);

	static List<Method> methods()
	{
		List<Method> m = new ArrayList<>();

		m.add(leather("craft_leather_gloves", "Leather gloves", 1, 13.8, 1, ItemID.LEATHER, "Leather",
			ItemID.LEATHER_GLOVES).tag(MethodTag.CHEAP).build());
		m.add(leather("craft_leather_body", "Leather bodies", 14, 25, 1, ItemID.LEATHER, "Leather",
			ItemID.LEATHER_ARMOUR).build());
		m.add(leather("craft_hard_leather_body", "Hard leather bodies", 28, 35, 1, ItemID.HARD_LEATHER, "Hard leather",
			ItemID.HARDLEATHER_BODY).build());

		m.add(furnace("craft_gold_rings", "Gold rings", 5, 15, RING_MOULD, null, 0, ItemID.GOLD_RING).build());
		m.add(furnace("craft_gold_amulets", "Gold amulets (u)", 8, 30, ItemReq.tool("Amulet mould", ItemID.AMULET_MOULD),
			null, 0, ItemID.UNSTRUNG_GOLD_AMULET).build());
		m.add(furnace("craft_sapphire_rings", "Sapphire rings", 20, 40, RING_MOULD, "Sapphires", ItemID.SAPPHIRE,
			ItemID.SAPPHIRE_RING).build());
		m.add(furnace("craft_emerald_rings", "Emerald rings", 27, 55, RING_MOULD, "Emeralds", ItemID.EMERALD,
			ItemID.EMERALD_RING).build());
		m.add(furnace("craft_ruby_rings", "Ruby rings", 34, 70, RING_MOULD, "Rubies", ItemID.RUBY,
			ItemID.RUBY_RING).build());
		m.add(furnace("craft_diamond_rings", "Diamond rings", 43, 85, RING_MOULD, "Diamonds", ItemID.DIAMOND,
			ItemID.DIAMOND_RING).build());

		m.add(gem("craft_cut_sapphire", "Sapphires", 20, 50, ItemID.UNCUT_SAPPHIRE, ItemID.SAPPHIRE, false).build());
		m.add(gem("craft_cut_emerald", "Emeralds", 27, 67.5, ItemID.UNCUT_EMERALD, ItemID.EMERALD, false).build());
		m.add(gem("craft_cut_ruby", "Rubies", 34, 85, ItemID.UNCUT_RUBY, ItemID.RUBY, false).build());
		m.add(gem("craft_cut_diamond", "Diamonds", 43, 107.5, ItemID.UNCUT_DIAMOND, ItemID.DIAMOND, false).build());
		m.add(gem("craft_cut_dragonstone", "Dragonstones", 55, 137.5, ItemID.UNCUT_DRAGONSTONE, ItemID.DRAGONSTONE, true)
			.build());

		m.add(glass("craft_glass_beer", "Beer glasses", 1, 17.5, ItemID.BEER_GLASS).tag(MethodTag.CHEAP).build());
		m.add(glass("craft_glass_lantern", "Candle lanterns", 4, 19, ItemID.CANDLE_LANTERN_EMPTY).build());
		m.add(glass("craft_glass_oil_lamp", "Oil lamps", 12, 25, ItemID.OIL_LAMP_EMPTY).build());
		m.add(glass("craft_glass_vial", "Vials", 33, 35, ItemID.VIAL_EMPTY).build());
		m.add(glass("craft_glass_orb", "Unpowered orbs", 46, 52.5, ItemID.STAFFORB).build());
		m.add(glass("craft_glass_lens", "Lantern lenses", 49, 55, ItemID.BULLSEYE_LANTERN_LENS).build());
		m.add(glass("craft_glass_light_orb", "Empty light orbs", 87, 70, ItemID.DORGESH_LIGHTBULB_NOFILAMENT).build());

		m.add(battlestaff("craft_water_battlestaff", "Water battlestaves", 54, 100, ItemID.WATER_ORB,
			ItemID.WATER_BATTLESTAFF).build());
		m.add(battlestaff("craft_earth_battlestaff", "Earth battlestaves", 58, 112.5, ItemID.EARTH_ORB,
			ItemID.EARTH_BATTLESTAFF).build());
		m.add(battlestaff("craft_fire_battlestaff", "Fire battlestaves", 62, 125, ItemID.FIRE_ORB,
			ItemID.FIRE_BATTLESTAFF).build());
		m.add(battlestaff("craft_air_battlestaff", "Air battlestaves", 66, 137.5, ItemID.AIR_ORB,
			ItemID.AIR_BATTLESTAFF).build());

		m.add(dhide("craft_green_dhide_body", "Green d'hide bodies", 63, 186, ItemID.DRAGON_LEATHER,
			"Green dragon leather", ItemID.DRAGONHIDE_BODY).build());
		m.add(dhide("craft_blue_dhide_body", "Blue d'hide bodies", 71, 210, ItemID.DRAGON_LEATHER_BLUE,
			"Blue dragon leather", ItemID.BLUE_DRAGONHIDE_BODY).build());
		m.add(dhide("craft_red_dhide_body", "Red d'hide bodies", 77, 234, ItemID.DRAGON_LEATHER_RED,
			"Red dragon leather", ItemID.RED_DRAGONHIDE_BODY).build());
		m.add(dhide("craft_black_dhide_body", "Black d'hide bodies", 84, 258, ItemID.DRAGON_LEATHER_BLACK,
			"Black dragon leather", ItemID.BLACK_DRAGONHIDE_BODY).build());
		return m;
	}

	private static Method.MethodBuilder bankstanding(String id, String name, int level, double xp, int perHour,
		boolean members)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.CRAFTING)
			.type(MethodType.PROCESS)
			.name(name)
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * perHour))
			.members(members)
			.tag(MethodTag.BANKSTANDING)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.wikiPage(members ? P2P_WIKI : F2P_WIKI);
	}

	private static Method.MethodBuilder leather(String id, String name, int level, double xp, int perItem,
		int leather, String leatherName, int product)
	{
		return bankstanding(id, "Craft " + name.toLowerCase(), level, xp, 1_400, false)
			.tool(NEEDLE).tool(THREAD)
			.input(tanned(leatherName, 26, perItem, leather))
			.product(product)
			.useItem(ItemID.NEEDLE).useItem(leather)
			.instruction("Use the needle on the " + leatherName.toLowerCase() + " and make " + name.toLowerCase())
			.notes("Thread runs out every 5 items; keep a stack of it in your inventory.");
	}

	/**
	 * Leather, which Ellis the tanner in Al Kharid makes from hides for a small fee.
	 */
	private static ItemReq tanned(String name, int carry, int perItem, int leather)
	{
		int hide;
		String hideName;
		int fee;
		switch (leather)
		{
			case ItemID.HARD_LEATHER:
				hide = ItemID.COW_HIDE;
				hideName = "Cowhides";
				fee = 3;
				break;
			case ItemID.DRAGON_LEATHER:
				hide = ItemID.DRAGONHIDE_GREEN;
				hideName = "Green dragonhides";
				fee = 20;
				break;
			case ItemID.DRAGON_LEATHER_BLUE:
				hide = ItemID.DRAGONHIDE_BLUE;
				hideName = "Blue dragonhides";
				fee = 20;
				break;
			case ItemID.DRAGON_LEATHER_RED:
				hide = ItemID.DRAGONHIDE_RED;
				hideName = "Red dragonhides";
				fee = 20;
				break;
			case ItemID.DRAGON_LEATHER_BLACK:
				hide = ItemID.DRAGONHIDE_BLACK;
				hideName = "Black dragonhides";
				fee = 20;
				break;
			default:
				hide = ItemID.COW_HIDE;
				hideName = "Cowhides";
				fee = 1;
				break;
		}
		return ItemReq.consumed(name, carry, perItem, leather)
			.madeFrom("Trade Ellis the tanner and tan your " + hideName.toLowerCase() + " (" + fee + " gp each)", false,
				ItemReq.part(hideName, 1, hide), ItemReq.stack("Coins", fee, ItemID.COINS))
			.madeAt(TANNER, Target.npc("Trade", "Ellis"));
	}

	private static Method.MethodBuilder furnace(String id, String name, int level, double xp, ItemReq mould,
		String gemName, int gem, int product)
	{
		Method.MethodBuilder b = Method.builder()
			.id(id)
			.skill(Skill.CRAFTING)
			.type(MethodType.PROCESS)
			.name(name + " (Edgeville furnace)")
			.minLevel(level)
			.xpPerAction(xp)
			.location(EDGEVILLE_FURNACE)
			.bank(Banks.EDGEVILLE)
			.tool(mould)
			.input(ItemReq.consumed("Gold bars", gem == 0 ? 27 : 13, 1, ItemID.GOLD_BAR)
				.madeFrom("Use the gold ore on the furnace to smelt gold bars", false,
					ItemReq.part("Gold ore", 1, ItemID.GOLD_ORE))
				.madeAt(EDGEVILLE_FURNACE, FURNACE))
			.product(product)
			.target(FURNACE)
			.useItem(ItemID.GOLD_BAR)
			.instruction("Use a gold bar on the furnace and make " + name.toLowerCase())
			.wikiPage(F2P_WIKI);
		if (gem != 0)
		{
			b.input(ItemReq.consumed(gemName, 13, 1, gem))
				.xpPerHour((int) (xp * 1_300));
		}
		else
		{
			b.tag(MethodTag.AFK).xpPerHour((int) (xp * 1_600));
		}
		return b;
	}

	private static Method.MethodBuilder gem(String id, String name, int level, double xp, int uncut, int cut,
		boolean members)
	{
		return bankstanding(id, "Cut " + name.toLowerCase(), level, xp, 2_300, members)
			.tag(MethodTag.FAST)
			.tool(CHISEL)
			.input(ItemReq.consumed("Uncut " + name.toLowerCase(), 27, 1, uncut))
			.product(cut)
			.useItem(ItemID.CHISEL).useItem(uncut)
			.instruction("Use the chisel on the uncut gems and press Space to cut them all");
	}

	private static Method.MethodBuilder glass(String id, String name, int level, double xp, int product)
	{
		return bankstanding(id, "Blow " + name.toLowerCase(), level, xp, 1_700, true)
			.tag(MethodTag.AFK)
			.tool(PIPE)
			.input(MOLTEN_GLASS)
			.product(product)
			.useItem(ItemID.GLASSBLOWINGPIPE).useItem(ItemID.MOLTEN_GLASS)
			.instruction("Use the glassblowing pipe on the molten glass and choose " + name.toLowerCase());
	}

	private static Method.MethodBuilder battlestaff(String id, String name, int level, double xp, int orb, int product)
	{
		return bankstanding(id, "Make " + name.toLowerCase(), level, xp, 2_300, true)
			.tag(MethodTag.FAST)
			.input(ItemReq.consumed("Battlestaves", 14, 1, ItemID.BATTLESTAFF))
			.input(ItemReq.consumed("Orbs", 14, 1, orb))
			.product(product)
			.useItem(orb).useItem(ItemID.BATTLESTAFF)
			.instruction("Use the orbs on the battlestaves")
			.notes("Zaff in Varrock sells battlestaves cheaper than the GE if you've done the Varrock diary.");
	}

	private static Method.MethodBuilder dhide(String id, String name, int level, double xp, int leather,
		String leatherName, int product)
	{
		return bankstanding(id, "Craft " + name.toLowerCase(), level, xp, 1_700, true)
			.tag(MethodTag.FAST)
			.tool(NEEDLE).tool(THREAD)
			.input(tanned(leatherName, 24, 3, leather))
			.product(product)
			.useItem(ItemID.NEEDLE).useItem(leather)
			.instruction("Use the needle on the " + leatherName.toLowerCase() + " and make bodies (3 leather each)");
	}

	private CraftingGuide()
	{
	}
}
