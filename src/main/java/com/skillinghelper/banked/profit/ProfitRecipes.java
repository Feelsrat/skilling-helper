package com.skillinghelper.banked.profit;

import com.skillinghelper.banked.data.BankedActivities;
import com.skillinghelper.banked.data.BankedActivity;
import com.skillinghelper.data.Banks;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Every bank item the profit finder knows how to process into something that sells: the Banked XP Guide's
 * activities that make a product, plus a few that give no xp but are classic money makers (unfinished potions).
 * Burying, burning and house building are left out because nothing comes out of them to sell.
 */
public final class ProfitRecipes
{
	private static final String BURN_NOTE = "Some will burn at low Cooking levels, so the real profit is a bit lower.";
	private static final String CRUSH_NOTE = "Some gems get crushed while cutting, so the real profit is a bit lower.";

	private static final List<ProfitRecipe> ALL = build();
	private static final Map<String, ProfitRecipe> BY_ID = index(ALL);

	private ProfitRecipes()
	{
	}

	public static List<ProfitRecipe> all()
	{
		return ALL;
	}

	/**
	 * The recipe for an activity id, or null.
	 */
	public static ProfitRecipe byId(String id)
	{
		return BY_ID.get(id);
	}

	private static Map<String, ProfitRecipe> index(List<ProfitRecipe> all)
	{
		Map<String, ProfitRecipe> byId = new LinkedHashMap<>();
		for (ProfitRecipe r : all)
		{
			if (byId.put(r.getId(), r) != null)
			{
				throw new IllegalStateException("Duplicate profit recipe " + r.getId());
			}
		}
		return Collections.unmodifiableMap(byId);
	}

	private static List<ProfitRecipe> build()
	{
		List<ProfitRecipe> out = new ArrayList<>();
		Map<String, Object[]> products = products();
		for (BankedActivity a : BankedActivities.all())
		{
			Object[] p = products.remove(a.getId());
			if (p != null)
			{
				out.add(new ProfitRecipe(a, (int) p[0], (String) p[1], ((Number) p[2]).doubleValue(), (String) p[3]));
			}
			else if (!a.getMethod().getProducts().isEmpty() && a.getMethod().getProducts().get(0) != ItemID.COINS)
			{
				// Skilling Helper's own methods already name what they make
				int product = a.getMethod().getProducts().get(0);
				out.add(new ProfitRecipe(a, product, productName(a), 1, null));
			}
		}
		if (!products.isEmpty())
		{
			throw new IllegalStateException("No banked activity for " + products.keySet());
		}
		unfinishedPotions(out);
		return Collections.unmodifiableList(out);
	}

	/**
	 * Product id, name, products per action and a note, by banked activity id.
	 */
	private static Map<String, Object[]> products()
	{
		Map<String, Object[]> m = new LinkedHashMap<>();
		// Herblore
		herb(m, "guam_leaf", ItemID.GUAM_LEAF, "Guam leaf");
		herb(m, "marrentill", ItemID.MARENTILL, "Marrentill");
		herb(m, "tarromin", ItemID.TARROMIN, "Tarromin");
		herb(m, "harralander", ItemID.HARRALANDER, "Harralander");
		herb(m, "ranarr_weed", ItemID.RANARR_WEED, "Ranarr weed");
		herb(m, "toadflax", ItemID.TOADFLAX, "Toadflax");
		herb(m, "irit_leaf", ItemID.IRIT_LEAF, "Irit leaf");
		herb(m, "avantoe", ItemID.AVANTOE, "Avantoe");
		herb(m, "kwuarm", ItemID.KWUARM, "Kwuarm");
		herb(m, "snapdragon", ItemID.SNAPDRAGON, "Snapdragon");
		herb(m, "cadantine", ItemID.CADANTINE, "Cadantine");
		herb(m, "lantadyme", ItemID.LANTADYME, "Lantadyme");
		herb(m, "dwarf_weed", ItemID.DWARF_WEED, "Dwarf weed");
		herb(m, "torstol", ItemID.TORSTOL, "Torstol");
		potion(m, "attack_potions", ItemID._3DOSE1ATTACK, "Attack potion(3)");
		potion(m, "antipoisons", ItemID._3DOSEANTIPOISON, "Antipoison(3)");
		potion(m, "strength_potions", ItemID._3DOSE1STRENGTH, "Strength potion(3)");
		potion(m, "restore_potions", ItemID._3DOSESTATRESTORE, "Restore potion(3)");
		potion(m, "energy_potions", ItemID._3DOSE1ENERGY, "Energy potion(3)");
		potion(m, "agility_potions", ItemID._3DOSE1AGILITY, "Agility potion(3)");
		potion(m, "prayer_potions", ItemID._3DOSEPRAYERRESTORE, "Prayer potion(3)");
		potion(m, "super_attack_potions", ItemID._3DOSE2ATTACK, "Super attack(3)");
		potion(m, "superantipoisons", ItemID._3DOSE2ANTIPOISON, "Superantipoison(3)");
		potion(m, "fishing_potions", ItemID._3DOSEFISHERSPOTION, "Fishing potion(3)");
		potion(m, "super_energy_potions", ItemID._3DOSE2ENERGY, "Super energy(3)");
		potion(m, "super_strength_potions", ItemID._3DOSE2STRENGTH, "Super strength(3)");
		potion(m, "super_restores", ItemID._3DOSE2RESTORE, "Super restore(3)");
		potion(m, "super_defence_potions", ItemID._3DOSE2DEFENSE, "Super defence(3)");
		potion(m, "antifire_potions", ItemID._3DOSE1ANTIDRAGON, "Antifire potion(3)");
		potion(m, "ranging_potions", ItemID._3DOSERANGERSPOTION, "Ranging potion(3)");
		potion(m, "magic_potions", ItemID._3DOSE1MAGIC, "Magic potion(3)");
		potion(m, "zamorak_brews", ItemID._3DOSEPOTIONOFZAMORAK, "Zamorak brew(3)");
		potion(m, "saradomin_brews", ItemID._3DOSEPOTIONOFSARADOMIN, "Saradomin brew(3)");

		// Cooking
		cook(m, "raw_shrimps", ItemID.SHRIMP, "Shrimps");
		cook(m, "raw_anchovies", ItemID.ANCHOVIES, "Anchovies");
		cook(m, "raw_sardine", ItemID.SARDINE, "Sardine");
		cook(m, "raw_beef", ItemID.COOKED_MEAT, "Cooked meat");
		cook(m, "raw_chicken", ItemID.COOKED_CHICKEN, "Cooked chicken");
		cook(m, "raw_herring", ItemID.HERRING, "Herring");
		cook(m, "raw_trout", ItemID.TROUT, "Trout");
		cook(m, "raw_cod", ItemID.COD, "Cod");
		cook(m, "raw_pike", ItemID.PIKE, "Pike");
		cook(m, "raw_salmon", ItemID.SALMON, "Salmon");
		cook(m, "raw_tuna", ItemID.TUNA, "Tuna");
		cook(m, "raw_cave_eel", ItemID.CAVE_EEL, "Cave eel");
		cook(m, "raw_lobster", ItemID.LOBSTER, "Lobster");
		cook(m, "raw_bass", ItemID.BASS, "Bass");
		cook(m, "raw_swordfish", ItemID.SWORDFISH, "Swordfish");
		cook(m, "raw_monkfish", ItemID.MONKFISH, "Monkfish");
		cook(m, "raw_shark", ItemID.SHARK, "Shark");
		cook(m, "raw_sea_turtle", ItemID.SEATURTLE, "Sea turtle");
		cook(m, "raw_anglerfish", ItemID.ANGLERFISH, "Anglerfish");
		cook(m, "raw_dark_crab", ItemID.DARK_CRAB, "Dark crab");
		cook(m, "raw_manta_ray", ItemID.MANTARAY, "Manta ray");
		put(m, "banked_cook_karambwan", ItemID.TBWT_COOKED_KARAMBWAN, "Cooked karambwan", 1, BURN_NOTE);
		put(m, "banked_cook_wine", ItemID.JUG_WINE, "Jug of wine", 1,
			"Some wine goes bad at low Cooking levels, so the real profit is a bit lower.");

		// Fletching
		bows(m, "", ItemID.UNSTRUNG_SHORTBOW, ItemID.UNSTRUNG_LONGBOW, 15, "");
		bows(m, "oak_", ItemID.UNSTRUNG_OAK_SHORTBOW, ItemID.UNSTRUNG_OAK_LONGBOW, 30, "Oak ");
		bows(m, "willow_", ItemID.UNSTRUNG_WILLOW_SHORTBOW, ItemID.UNSTRUNG_WILLOW_LONGBOW, 45, "Willow ");
		bows(m, "maple_", ItemID.UNSTRUNG_MAPLE_SHORTBOW, ItemID.UNSTRUNG_MAPLE_LONGBOW, 60, "Maple ");
		bows(m, "yew_", ItemID.UNSTRUNG_YEW_SHORTBOW, ItemID.UNSTRUNG_YEW_LONGBOW, 75, "Yew ");
		bows(m, "magic_", ItemID.UNSTRUNG_MAGIC_SHORTBOW, ItemID.UNSTRUNG_MAGIC_LONGBOW, 90, "Magic ");
		strung(m, "shortbow_u", ItemID.SHORTBOW, "Shortbow");
		strung(m, "longbow_u", ItemID.LONGBOW, "Longbow");
		strung(m, "oak_shortbow_u", ItemID.OAK_SHORTBOW, "Oak shortbow");
		strung(m, "oak_longbow_u", ItemID.OAK_LONGBOW, "Oak longbow");
		strung(m, "willow_shortbow_u", ItemID.WILLOW_SHORTBOW, "Willow shortbow");
		strung(m, "willow_longbow_u", ItemID.WILLOW_LONGBOW, "Willow longbow");
		strung(m, "maple_shortbow_u", ItemID.MAPLE_SHORTBOW, "Maple shortbow");
		strung(m, "maple_longbow_u", ItemID.MAPLE_LONGBOW, "Maple longbow");
		strung(m, "yew_shortbow_u", ItemID.YEW_SHORTBOW, "Yew shortbow");
		strung(m, "yew_longbow_u", ItemID.YEW_LONGBOW, "Yew longbow");
		strung(m, "magic_shortbow_u", ItemID.MAGIC_SHORTBOW, "Magic shortbow");
		strung(m, "magic_longbow_u", ItemID.MAGIC_LONGBOW, "Magic longbow");
		put(m, "banked_fletch_headless", ItemID.HEADLESS_ARROW, "Headless arrow", 15, null);
		arrows(m, "bronze", ItemID.BRONZE_ARROW, "Bronze arrow");
		arrows(m, "iron", ItemID.IRON_ARROW, "Iron arrow");
		arrows(m, "steel", ItemID.STEEL_ARROW, "Steel arrow");
		arrows(m, "mithril", ItemID.MITHRIL_ARROW, "Mithril arrow");
		arrows(m, "adamant", ItemID.ADAMANT_ARROW, "Adamant arrow");
		arrows(m, "rune", ItemID.RUNE_ARROW, "Rune arrow");
		arrows(m, "amethyst", ItemID.AMETHYST_ARROW, "Amethyst arrow");
		arrows(m, "dragon", ItemID.DRAGON_ARROW, "Dragon arrow");
		darts(m, "bronze", ItemID.BRONZE_DART, "Bronze dart");
		darts(m, "iron", ItemID.IRON_DART, "Iron dart");
		darts(m, "steel", ItemID.STEEL_DART, "Steel dart");
		darts(m, "mithril", ItemID.MITHRIL_DART, "Mithril dart");
		darts(m, "adamant", ItemID.ADAMANT_DART, "Adamant dart");
		darts(m, "rune", ItemID.RUNE_DART, "Rune dart");
		darts(m, "amethyst", ItemID.AMETHYST_DART, "Amethyst dart");
		darts(m, "dragon", ItemID.DRAGON_DART, "Dragon dart");

		// Crafting
		gem(m, "opal", ItemID.OPAL, "Opal", CRUSH_NOTE);
		gem(m, "jade", ItemID.JADE, "Jade", CRUSH_NOTE);
		gem(m, "red_topaz", ItemID.RED_TOPAZ, "Red topaz", CRUSH_NOTE);
		gem(m, "sapphire", ItemID.SAPPHIRE, "Sapphire", null);
		gem(m, "emerald", ItemID.EMERALD, "Emerald", null);
		gem(m, "ruby", ItemID.RUBY, "Ruby", null);
		gem(m, "diamond", ItemID.DIAMOND, "Diamond", null);
		gem(m, "dragonstone", ItemID.DRAGONSTONE, "Dragonstone", null);
		gem(m, "onyx", ItemID.ONYX, "Onyx", null);
		gem(m, "zenyte", ItemID.ZENYTE, "Zenyte", null);
		craft(m, "leather_gloves", ItemID.LEATHER_GLOVES, "Leather gloves");
		craft(m, "leather_bodies", ItemID.LEATHER_ARMOUR, "Leather body");
		craft(m, "leather_chaps", ItemID.LEATHER_CHAPS, "Leather chaps");
		craft(m, "coifs", ItemID.COIF, "Coif");
		craft(m, "hardleather_bodies", ItemID.HARDLEATHER_BODY, "Hardleather body");
		craft(m, "green_d_hide_vambraces", ItemID.DRAGON_VAMBRACES, "Green d'hide vambraces");
		craft(m, "green_d_hide_chaps", ItemID.DRAGONHIDE_CHAPS, "Green d'hide chaps");
		craft(m, "green_d_hide_bodies", ItemID.DRAGONHIDE_BODY, "Green d'hide body");
		craft(m, "blue_d_hide_vambraces", ItemID.BLUE_DRAGON_VAMBRACES, "Blue d'hide vambraces");
		craft(m, "blue_d_hide_chaps", ItemID.BLUE_DRAGONHIDE_CHAPS, "Blue d'hide chaps");
		craft(m, "blue_d_hide_bodies", ItemID.BLUE_DRAGONHIDE_BODY, "Blue d'hide body");
		craft(m, "red_d_hide_vambraces", ItemID.RED_DRAGON_VAMBRACES, "Red d'hide vambraces");
		craft(m, "red_d_hide_chaps", ItemID.RED_DRAGONHIDE_CHAPS, "Red d'hide chaps");
		craft(m, "red_d_hide_bodies", ItemID.RED_DRAGONHIDE_BODY, "Red d'hide body");
		craft(m, "black_d_hide_vambraces", ItemID.BLACK_DRAGON_VAMBRACES, "Black d'hide vambraces");
		craft(m, "black_d_hide_chaps", ItemID.BLACK_DRAGONHIDE_CHAPS, "Black d'hide chaps");
		craft(m, "black_d_hide_bodies", ItemID.BLACK_DRAGONHIDE_BODY, "Black d'hide body");
		glass(m, "beer_glasses", ItemID.BEER_GLASS, "Beer glass");
		glass(m, "vials", ItemID.VIAL_EMPTY, "Vial");
		glass(m, "unpowered_orbs", ItemID.STAFFORB, "Unpowered orb");
		glass(m, "lantern_lenses", ItemID.BULLSEYE_LANTERN_LENS, "Lantern lens");
		put(m, "banked_staff_water", ItemID.WATER_BATTLESTAFF, "Water battlestaff", 1, null);
		put(m, "banked_staff_earth", ItemID.EARTH_BATTLESTAFF, "Earth battlestaff", 1, null);
		put(m, "banked_staff_fire", ItemID.FIRE_BATTLESTAFF, "Fire battlestaff", 1, null);
		put(m, "banked_staff_air", ItemID.AIR_BATTLESTAFF, "Air battlestaff", 1, null);

		// Smithing
		put(m, "banked_smelt_bronze", ItemID.BRONZE_BAR, "Bronze bar", 1, null);
		put(m, "banked_smelt_iron", ItemID.IRON_BAR, "Iron bar", 0.5,
			"Iron only smelts half the time without a ring of forging; wear one and it's every time.");
		put(m, "banked_smelt_silver", ItemID.SILVER_BAR, "Silver bar", 1, null);
		put(m, "banked_smelt_steel", ItemID.STEEL_BAR, "Steel bar", 1, null);
		put(m, "banked_smelt_gold", ItemID.GOLD_BAR, "Gold bar", 1, null);
		put(m, "banked_smelt_mithril", ItemID.MITHRIL_BAR, "Mithril bar", 1, null);
		put(m, "banked_smelt_adamantite", ItemID.ADAMANTITE_BAR, "Adamantite bar", 1, null);
		put(m, "banked_smelt_runite", ItemID.RUNITE_BAR, "Runite bar", 1, null);
		smith(m, "bronze", ItemID.BRONZE_DAGGER, ItemID.BRONZE_PLATEBODY, "Bronze");
		smith(m, "iron", ItemID.IRON_DAGGER, ItemID.IRON_PLATEBODY, "Iron");
		smith(m, "steel", ItemID.STEEL_DAGGER, ItemID.STEEL_PLATEBODY, "Steel");
		smith(m, "mithril", ItemID.MITHRIL_DAGGER, ItemID.MITHRIL_PLATEBODY, "Mithril");
		smith(m, "adamant", ItemID.ADAMANT_DAGGER, ItemID.ADAMANT_PLATEBODY, "Adamant");
		smith(m, "rune", ItemID.RUNE_DAGGER, ItemID.RUNE_PLATEBODY, "Rune");

		// Runecraft and Magic, from Skilling Helper's guides
		String moreRunes = "You get more than one rune per essence at higher Runecraft levels, so this is the least "
			+ "you'll make.";
		put(m, "banked_rc_air", ItemID.AIRRUNE, "Air rune", 1, moreRunes);
		put(m, "banked_rc_earth", ItemID.EARTHRUNE, "Earth rune", 1, moreRunes);
		put(m, "banked_rc_fire", ItemID.FIRERUNE, "Fire rune", 1, moreRunes);
		put(m, "banked_magic_superheat_iron", ItemID.IRON_BAR, "Iron bar", 1, null);
		put(m, "banked_magic_plank_make_oak", ItemID.PLANK_OAK, "Oak plank", 1, null);
		put(m, "banked_magic_plank_make_teak", ItemID.PLANK_TEAK, "Teak plank", 1, null);
		put(m, "banked_magic_plank_make_mahogany", ItemID.PLANK_MAHOGANY, "Mahogany plank", 1, null);
		put(m, "banked_magic_enchant_recoil", ItemID.RING_OF_RECOIL, "Ring of recoil", 1, null);
		put(m, "banked_magic_enchant_power", ItemID.AMULET_OF_POWER, "Amulet of power", 1, null);
		put(m, "banked_magic_enchant_glory", ItemID.AMULET_OF_GLORY, "Amulet of glory", 1, null);
		return m;
	}

	private static void put(Map<String, Object[]> m, String id, int product, String name, double perAction,
		String note)
	{
		m.put(id, new Object[]{product, name, perAction, note});
	}

	private static void herb(Map<String, Object[]> m, String key, int clean, String name)
	{
		put(m, "banked_clean_" + key, clean, name, 1, null);
	}

	private static void potion(Map<String, Object[]> m, String key, int potion, String name)
	{
		put(m, "banked_potion_" + key, potion, name, 1, null);
	}

	private static void cook(Map<String, Object[]> m, String key, int cooked, String name)
	{
		put(m, "banked_cook_" + key, cooked, name, 1, BURN_NOTE);
	}

	private static void bows(Map<String, Object[]> m, String wood, int shortbow, int longbow, int shafts,
		String name)
	{
		put(m, "banked_fletch_" + wood + "shortbow", shortbow, name + "shortbow (u)", 1, null);
		put(m, "banked_fletch_" + wood + "longbow", longbow, name + "longbow (u)", 1, null);
		put(m, "banked_fletch_" + wood + "shafts", ItemID.ARROW_SHAFT, "Arrow shaft", shafts, null);
	}

	private static void strung(Map<String, Object[]> m, String key, int bow, String name)
	{
		put(m, "banked_string_" + key, bow, name, 1, null);
	}

	private static void arrows(Map<String, Object[]> m, String metal, int arrow, String name)
	{
		put(m, "banked_arrows_" + metal + "_arrowtips", arrow, name, 15, null);
	}

	private static void darts(Map<String, Object[]> m, String metal, int dart, String name)
	{
		put(m, "banked_darts_" + metal + "_dart_tip", dart, name, 10, null);
	}

	private static void gem(Map<String, Object[]> m, String key, int cut, String name, String note)
	{
		put(m, "banked_cut_uncut_" + key, cut, name, 1, note);
	}

	private static void craft(Map<String, Object[]> m, String key, int product, String name)
	{
		put(m, "banked_craft_" + key, product, name, 1, null);
	}

	private static void glass(Map<String, Object[]> m, String key, int product, String name)
	{
		put(m, "banked_glass_" + key, product, name, 1, null);
	}

	private static void smith(Map<String, Object[]> m, String metal, int dagger, int platebody, String name)
	{
		put(m, "banked_smith_" + metal + "_daggers", dagger, name + " dagger", 1, null);
		put(m, "banked_smith_" + metal + "_platebodies", platebody, name + " platebody", 1, null);
	}

	private static String productName(BankedActivity a)
	{
		String name = a.getName();
		int space = name.indexOf(' ');
		return space < 0 ? name : Character.toUpperCase(name.charAt(space + 1)) + name.substring(space + 2);
	}

	// ---- Unfinished potions: no xp, but a classic money maker ----

	private static void unfinishedPotions(List<ProfitRecipe> out)
	{
		unf(out, "Guam leaf", ItemID.GUAM_LEAF, ItemID.GUAMVIAL, "Guam potion (unf)", 3);
		unf(out, "Marrentill", ItemID.MARENTILL, ItemID.MARRENTILLVIAL, "Marrentill potion (unf)", 5);
		unf(out, "Tarromin", ItemID.TARROMIN, ItemID.TARROMINVIAL, "Tarromin potion (unf)", 12);
		unf(out, "Harralander", ItemID.HARRALANDER, ItemID.HARRALANDERVIAL, "Harralander potion (unf)", 22);
		unf(out, "Ranarr weed", ItemID.RANARR_WEED, ItemID.RANARRVIAL, "Ranarr potion (unf)", 30);
		unf(out, "Toadflax", ItemID.TOADFLAX, ItemID.TOADFLAXVIAL, "Toadflax potion (unf)", 34);
		unf(out, "Irit leaf", ItemID.IRIT_LEAF, ItemID.IRITVIAL, "Irit potion (unf)", 45);
		unf(out, "Avantoe", ItemID.AVANTOE, ItemID.AVANTOEVIAL, "Avantoe potion (unf)", 50);
		unf(out, "Kwuarm", ItemID.KWUARM, ItemID.KWUARMVIAL, "Kwuarm potion (unf)", 55);
		unf(out, "Snapdragon", ItemID.SNAPDRAGON, ItemID.SNAPDRAGONVIAL, "Snapdragon potion (unf)", 63);
		unf(out, "Cadantine", ItemID.CADANTINE, ItemID.CADANTINEVIAL, "Cadantine potion (unf)", 66);
		unf(out, "Lantadyme", ItemID.LANTADYME, ItemID.LANTADYMEVIAL, "Lantadyme potion (unf)", 69);
		unf(out, "Dwarf weed", ItemID.DWARF_WEED, ItemID.DWARFWEEDVIAL, "Dwarf weed potion (unf)", 72);
		unf(out, "Torstol", ItemID.TORSTOL, ItemID.TORSTOLVIAL, "Torstol potion (unf)", 78);
	}

	private static void unf(List<ProfitRecipe> out, String herb, int clean, int unf, String unfName, int level)
	{
		String key = herb.toLowerCase().replaceAll("[^a-z0-9]+", "_");
		Method m = Method.builder()
			.id("profit_unf_" + key)
			.skill(Skill.HERBLORE)
			.type(MethodType.PROCESS)
			.name("Make " + unfName.toLowerCase().replace("potion (unf)", "potions (unf)"))
			.minLevel(level)
			.xpPerAction(0)
			.xpPerHour(0)
			.members(true)
			.quest(Quest.DRUIDIC_RITUAL)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.consumed(herb, 14, 1, clean))
			.input(ItemReq.consumed("Vials of water", 14, 1, ItemID.VIAL_WATER))
			.product(unf)
			.useItem(clean).useItem(ItemID.VIAL_WATER)
			.instruction("Use a " + herb.toLowerCase() + " on a vial of water and press Space to make all 14")
			.notes("No xp, but unfinished potions often sell for more than the herb and vial. Missing vials of water "
				+ "are bought at the Grand Exchange (or the Port Sarim / Yanille herb shops).")
			.wikiPage("Unfinished_potion")
			.build();
		BankedActivity a = BankedActivity.builder()
			.method(m)
			.place(BankedActivity.Place.ANY_BANK)
			.group("profit+" + clean)
			.itemName(herb)
			.iconId(clean)
			.countIds(new int[]{clean})
			.perAction(1)
			.stage(1)
			.build();
		out.add(new ProfitRecipe(a, unf, unfName, 1, null));
	}
}
