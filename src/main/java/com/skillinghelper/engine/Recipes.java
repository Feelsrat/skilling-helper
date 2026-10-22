package com.skillinghelper.engine;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * Decides when an input is better made from its parts than bought (baskets of apples, unfinished potions, molten
 * glass...) and how many to make per bank trip.
 */
final class Recipes
{
	private final PriceLookup prices;

	Recipes(PriceLookup prices)
	{
		this.prices = prices;
	}

	/**
	 * Whether the player should make this input from its recipe instead of buying it: they don't have enough of it,
	 * and they already own the parts, making is cheaper, or it's hardly ever sold.
	 */
	boolean making(ItemReq input, PlayerState s)
	{
		if (!input.hasRecipe() || s.owned(input.getIds()) >= Math.max(1, input.getQuantity()))
		{
			return false;
		}
		if (input.isAlwaysMake())
		{
			return true;
		}
		int price = prices.price(input.primaryId());
		return ownsParts(input, s) || price <= 0 || partsCost(input) < price;
	}

	/**
	 * The method's inputs the player should make this trip.
	 */
	List<ItemReq> toMake(Method m, PlayerState s)
	{
		List<ItemReq> toMake = new ArrayList<>();
		for (ItemReq input : m.getInputs())
		{
			if (making(input, s))
			{
				toMake.add(input);
			}
		}
		return toMake;
	}

	private static boolean ownsParts(ItemReq input, PlayerState s)
	{
		for (ItemReq part : input.getRecipe())
		{
			if (s.owned(part.getIds()) < part.getPerAction())
			{
				return false;
			}
		}
		return true;
	}

	private long partsCost(ItemReq input)
	{
		long cost = 0;
		for (ItemReq part : input.getRecipe())
		{
			cost += (long) prices.price(part.primaryId()) * part.getPerAction();
		}
		return cost;
	}

	/**
	 * How many to make per bank trip: as many as the parts fit in the inventory (14 buckets of sand + 14 soda ash),
	 * up to what the method needs.
	 */
	static int perTrip(ItemReq input, PlayerState s)
	{
		List<ItemReq> fill = new ArrayList<>();
		int toWithdraw = 0;
		for (ItemReq part : input.getRecipe())
		{
			if (takesOneSlot(part))
			{
				if (s.getInventory().count(part.getIds()) == 0)
				{
					toWithdraw++;
				}
			}
			else
			{
				fill.add(part);
			}
		}
		Set<Integer> kept = keptIds(List.of(input));
		for (ItemReq part : fill)
		{
			ItemIds.removeAll(kept, part.getIds());
		}
		int want = Math.max(1, Math.max(1, input.getQuantity()) - s.owned(input.getIds()));
		return Math.max(1, Loadout.of(fill, kept, toWithdraw, s, want).getActions());
	}

	/**
	 * Coins or a tool such as a pestle and mortar: one slot whatever the amount.
	 */
	private static boolean takesOneSlot(ItemReq part)
	{
		return part.isWholeStack() || part.getPerAction() == 0;
	}

	/**
	 * What a trip to make these inputs keeps in the inventory: just the parts (and cash).
	 */
	static Set<Integer> keptIds(List<ItemReq> toMake)
	{
		Set<Integer> ids = new HashSet<>();
		for (ItemReq input : toMake)
		{
			for (ItemReq part : input.getRecipe())
			{
				ItemIds.addReq(ids, part);
			}
		}
		ids.add(ItemID.COINS);
		ids.add(ItemID.PLATINUM);
		return ids;
	}

	static boolean partsCarried(ItemReq input, PlayerState s)
	{
		for (ItemReq part : input.getRecipe())
		{
			if (s.getInventory().count(part.getIds()) < Math.max(1, part.getPerAction()))
			{
				return false;
			}
		}
		return true;
	}
}
