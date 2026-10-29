package com.skillinghelper.pvm.guide;

import com.skillinghelper.pvm.guide.Supply.Option;
import net.runelite.api.gameval.ItemID;

/**
 * Supplies guides bring, with every dose or charge counted.
 */
public final class Supplies
{
	public static final Option SHARKS = Option.of("Sharks", ItemID.SHARK);
	public static final Option MONKFISH = Option.of("Monkfish", ItemID.MONKFISH);
	public static final Option SWORDFISH = Option.of("Swordfish", ItemID.SWORDFISH);
	public static final Option LOBSTERS = Option.of("Lobsters", ItemID.LOBSTER);
	public static final Option TUNA = Option.of("Tuna", ItemID.TUNA);
	public static final Option ANGLERFISH = Option.of("Anglerfish", ItemID.ANGLERFISH);
	public static final Option MANTA_RAYS = Option.of("Manta rays", ItemID.MANTARAY);
	public static final Option KARAMBWANS = Option.of("Cooked karambwans", ItemID.TBWT_COOKED_KARAMBWAN);

	private static final Option PRAYER = Option.of("Prayer potions", ItemID._4DOSEPRAYERRESTORE,
		ItemID._3DOSEPRAYERRESTORE, ItemID._2DOSEPRAYERRESTORE, ItemID._1DOSEPRAYERRESTORE);
	private static final Option RESTORE = Option.of("Super restores", ItemID._4DOSE2RESTORE, ItemID._3DOSE2RESTORE,
		ItemID._2DOSE2RESTORE, ItemID._1DOSE2RESTORE);
	private static final Option BREW = Option.of("Saradomin brews", ItemID._4DOSEPOTIONOFSARADOMIN,
		ItemID._3DOSEPOTIONOFSARADOMIN, ItemID._2DOSEPOTIONOFSARADOMIN, ItemID._1DOSEPOTIONOFSARADOMIN);
	private static final Option SUPER_COMBAT = Option.of("Super combat potions", ItemID._4DOSE2COMBAT,
		ItemID._3DOSE2COMBAT, ItemID._2DOSE2COMBAT, ItemID._1DOSE2COMBAT);
	private static final Option DIVINE_COMBAT = Option.of("Divine super combat potions", ItemID._4DOSEDIVINECOMBAT,
		ItemID._3DOSEDIVINECOMBAT, ItemID._2DOSEDIVINECOMBAT, ItemID._1DOSEDIVINECOMBAT);
	private static final Option SUPER_STRENGTH = Option.of("Super strength potions", ItemID._4DOSE2STRENGTH,
		ItemID._3DOSE2STRENGTH, ItemID._2DOSE2STRENGTH, ItemID._1DOSE2STRENGTH);
	private static final Option STRENGTH = Option.of("Strength potions", ItemID.STRENGTH4, ItemID._3DOSE1STRENGTH,
		ItemID._2DOSE1STRENGTH, ItemID._1DOSE1STRENGTH);
	private static final Option RANGING = Option.of("Ranging potions", ItemID._4DOSERANGERSPOTION,
		ItemID._3DOSERANGERSPOTION, ItemID._2DOSERANGERSPOTION, ItemID._1DOSERANGERSPOTION);
	private static final Option DIVINE_RANGING = Option.of("Divine ranging potions", ItemID._4DOSEDIVINERANGE,
		ItemID._3DOSEDIVINERANGE, ItemID._2DOSEDIVINERANGE, ItemID._1DOSEDIVINERANGE);
	private static final Option ANTI_VENOM = Option.of("Anti-venom+", ItemID.ANTIVENOM_4, ItemID.ANTIVENOM_3,
		ItemID.ANTIVENOM_2, ItemID.ANTIVENOM_1);
	private static final Option ANTIDOTE = Option.of("Antidote++", ItemID.ANTIDOTE__4, ItemID.ANTIDOTE__3,
		ItemID.ANTIDOTE__2, ItemID.ANTIDOTE__1);
	private static final Option SUPER_ANTIPOISON = Option.of("Superantipoisons", ItemID._4DOSE2ANTIPOISON,
		ItemID._3DOSE2ANTIPOISON, ItemID._2DOSE2ANTIPOISON, ItemID._1DOSE2ANTIPOISON);
	private static final Option EXT_SUPER_ANTIFIRE = Option.of("Extended super antifires", ItemID._4DOSE4ANTIDRAGON,
		ItemID._3DOSE4ANTIDRAGON, ItemID._2DOSE4ANTIDRAGON, ItemID._1DOSE4ANTIDRAGON);
	private static final Option SUPER_ANTIFIRE = Option.of("Super antifires", ItemID._4DOSE3ANTIDRAGON,
		ItemID._3DOSE3ANTIDRAGON, ItemID._2DOSE3ANTIDRAGON, ItemID._1DOSE3ANTIDRAGON);
	private static final Option ANTIFIRE = Option.of("Antifire potions", ItemID._4DOSE1ANTIDRAGON,
		ItemID._3DOSE1ANTIDRAGON, ItemID._2DOSE1ANTIDRAGON, ItemID._1DOSE1ANTIDRAGON);
	private static final Option STAMINA = Option.of("Stamina potions", ItemID._4DOSESTAMINA, ItemID._3DOSESTAMINA,
		ItemID._2DOSESTAMINA, ItemID._1DOSESTAMINA);

	/**
	 * Food that fills the rest of the inventory, best first. The guide buys the first option.
	 */
	public static Supply food(Option buy)
	{
		return new Supply("Food", foodOptions(buy), Supply.FILL, 1, false, false, true, true, null);
	}

	/**
	 * A set amount of food rather than filling the inventory.
	 */
	public static Supply food(Option buy, int amount)
	{
		return food(buy).amount(amount);
	}

	private static java.util.List<Option> foodOptions(Option buy)
	{
		java.util.List<Option> all = new java.util.ArrayList<>();
		all.add(buy);
		for (Option o : new Option[]{ANGLERFISH, MANTA_RAYS, SHARKS, KARAMBWANS, MONKFISH, SWORDFISH, LOBSTERS, TUNA})
		{
			if (o != buy)
			{
				all.add(o);
			}
		}
		return all;
	}

	public static Supply prayerPotions(int amount)
	{
		return Supply.of("Prayer potions", amount, PRAYER, RESTORE);
	}

	public static Supply superRestores(int amount)
	{
		return Supply.of("Super restores", amount, RESTORE);
	}

	public static Supply brews(int amount)
	{
		return Supply.of("Saradomin brews", amount, BREW);
	}

	public static Supply meleeBoost(int amount)
	{
		return Supply.of("Combat boost", amount, SUPER_COMBAT, DIVINE_COMBAT);
	}

	public static Supply strengthPotions(int amount)
	{
		return Supply.of("Strength boost", amount, STRENGTH, SUPER_STRENGTH);
	}

	public static Supply superStrength(int amount)
	{
		return Supply.of("Strength boost", amount, SUPER_STRENGTH);
	}

	public static Supply rangingPotions(int amount)
	{
		return Supply.of("Ranging potions", amount, RANGING, DIVINE_RANGING);
	}

	public static Supply antiVenom(int amount)
	{
		return Supply.of("Anti-venom", amount, ANTI_VENOM, ANTIDOTE);
	}

	public static Supply antidote(int amount)
	{
		return Supply.of("Antipoison", amount, ANTIDOTE, SUPER_ANTIPOISON, ANTI_VENOM);
	}

	public static Supply antifire(int amount)
	{
		return Supply.of("Antifire", amount, EXT_SUPER_ANTIFIRE, SUPER_ANTIFIRE, ANTIFIRE);
	}

	public static Supply cheapAntifire(int amount)
	{
		return Supply.of("Antifire", amount, ANTIFIRE, SUPER_ANTIFIRE, EXT_SUPER_ANTIFIRE);
	}

	public static Supply stamina(int amount)
	{
		return Supply.of("Stamina potions", amount, STAMINA);
	}

	public static Supply spade()
	{
		return Supply.tool("Spade", Option.of("Spade", ItemID.SPADE));
	}

	public static Supply rope()
	{
		return Supply.tool("Rope", Option.of("Rope", ItemID.ROPE));
	}

	public static Supply lightSource()
	{
		return Supply.tool("Light source", Option.of("Lit candle", ItemID.LIT_CANDLE),
			Option.of("Lantern", ItemID.CANDLE_LANTERN_LIT, ItemID.CANDLE_LANTERN_BLACK_LIT, ItemID.OIL_LANTERN_LIT,
				ItemID.BULLSEYE_LANTERN_LIT));
	}

	public static Supply secateurs()
	{
		return Supply.tool("Secateurs", Option.of("Secateurs", ItemID.SECATEURS),
			Option.of("Magic secateurs", ItemID.FAIRY_ENCHANTED_SECATEURS));
	}

	public static Supply teleport(String name, int amount, Option... options)
	{
		return Supply.stack(name, amount, options);
	}

	/**
	 * A teleport or other item the guide asks for that can't be bought, such as a key.
	 */
	public static Supply untradeable(String name, Option... options)
	{
		return Supply.tool(name, options).withBuyable(false);
	}

	/**
	 * Ammo for a ranged weapon that fires this kind, or null for none.
	 */
	public static Supply ammo(Gear.Ammo kind)
	{
		switch (kind)
		{
			case ARROWS:
				return Supply.stack("Arrows", 500, Option.of("Rune arrows", ItemID.RUNE_ARROW),
					Option.of("Amethyst arrows", ItemID.AMETHYST_ARROW), Option.of("Dragon arrows", ItemID.DRAGON_ARROW),
					Option.of("Adamant arrows", ItemID.ADAMANT_ARROW), Option.of("Mithril arrows", ItemID.MITHRIL_ARROW),
					Option.of("Iron arrows", ItemID.IRON_ARROW)).withRestockBelow(50)
					.note("Wear them in the ammo slot; use the best your bow can fire");
			case BOLTS:
				return Supply.stack("Bolts", 300, Option.of("Diamond bolts (e)",
						ItemID.XBOWS_CROSSBOW_BOLTS_ADAMANTITE_TIPPED_DIAMOND_ENCHANTED),
					Option.of("Diamond dragon bolts (e)", ItemID.DRAGON_BOLTS_ENCHANTED_DIAMOND),
					Option.of("Ruby dragon bolts (e)", ItemID.DRAGON_BOLTS_ENCHANTED_RUBY),
					Option.of("Ruby bolts (e)", ItemID.XBOWS_CROSSBOW_BOLTS_ADAMANTITE_TIPPED_RUBY_ENCHANTED),
					Option.of("Runite bolts", ItemID.XBOWS_CROSSBOW_BOLTS_RUNITE)).withRestockBelow(50)
					.note("Wear them in the ammo slot");
			case DARTS:
				return Supply.stack("Darts and scales", 1_000, Option.of("Zulrah's scales", ItemID.SNAKEBOSS_SCALE))
					.withRestockBelow(200)
					.note("Load the blowpipe with these and 300+ dragon, amethyst or adamant darts");
			default:
				return null;
		}
	}

	private Supplies()
	{
	}
}
