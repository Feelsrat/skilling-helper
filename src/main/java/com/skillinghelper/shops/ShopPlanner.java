package com.skillinghelper.shops;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.Wilderness;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * Decides whether a shopping list is better bought from an NPC shop than on the Grand Exchange.
 * <p>
 * A shop gets an item when its price stays below the GE's for the whole amount, hopping worlds at most
 * {@link #MAX_WORLDS} times as its price climbs with each one bought. The one shop worth visiting is the one that
 * saves the most after allowing for the walk: it has to save at least {@link #MIN_SAVING}, or sell everything on the
 * list and be closer than the GE. Whatever the shop doesn't cover is still bought on the GE.
 */
public final class ShopPlanner
{
	/**
	 * Smallest saving that's worth a detour to a shop.
	 */
	public static final int MIN_SAVING = 1_000;
	/**
	 * Most worlds to buy one item in. More hopping than this isn't worth the gold.
	 */
	public static final int MAX_WORLDS = 3;
	/**
	 * What a tile of walking to the shop is worth, so a far-away shop has to save more.
	 */
	static final int GP_PER_TILE = 2;
	/**
	 * Underground areas are mapped this far north of the surface above them.
	 */
	private static final int UNDERGROUND_OFFSET = 6400;

	/**
	 * Something to buy: how many, and what one costs on the GE. The name may be null, and then the shop's name for
	 * the item is shown.
	 */
	@Value
	public static class Want
	{
		int itemId;
		String name;
		int quantity;
		int gePrice;
	}

	private ShopPlanner()
	{
	}

	/**
	 * The shop to buy (some of) the list from, or null to buy it all on the GE.
	 *
	 * @param from    where the player is, or null if unknown
	 * @param members on a members world, so members-only shops are open
	 */
	public static ShopPlan plan(List<Want> wants, WorldPoint from, boolean members)
	{
		Map<Shop, List<ShopPlan.Line>> byShop = new LinkedHashMap<>();
		int wanted = 0;
		for (Want w : wants)
		{
			if (w.getQuantity() <= 0)
			{
				continue;
			}
			wanted++;
			for (ShopOffer offer : Shops.selling(w.getItemId()))
			{
				// Never send anyone into the Wilderness just to save a few coins
				if ((offer.getShop().isMembers() && !members) || Wilderness.contains(offer.getShop().getLocation()))
				{
					continue;
				}
				ShopPlan.Line line = line(offer, w);
				if (line != null)
				{
					byShop.computeIfAbsent(offer.getShop(), s -> new ArrayList<>()).add(line);
				}
			}
		}

		int geDistance = distance(from, Banks.GRAND_EXCHANGE.getPoint());
		ShopPlan best = null;
		long bestScore = Long.MIN_VALUE;
		for (Map.Entry<Shop, List<ShopPlan.Line>> e : byShop.entrySet())
		{
			List<ShopPlan.Line> lines = e.getValue();
			long saving = lines.stream().mapToLong(ShopPlan.Line::getSaving).sum();
			int distance = distance(from, e.getKey().getLocation().getPoint());
			long score = saving - (long) GP_PER_TILE * distance;
			boolean coversAll = lines.size() == wanted;
			boolean worth = score >= MIN_SAVING || (coversAll && saving > 0 && distance < geDistance);
			if (worth && score > bestScore)
			{
				best = new ShopPlan(e.getKey(), lines, rest(wants, lines), saving);
				bestScore = score;
			}
		}
		return best;
	}

	/**
	 * What buying {@code want} from this shop costs, or null if the shop can't beat the GE for all of it within
	 * {@link #MAX_WORLDS} worlds.
	 */
	static ShopPlan.Line line(ShopOffer offer, Want want)
	{
		int ge = want.getGePrice();
		if (ge <= 0)
		{
			return null;
		}
		// Per world: buy while the shop is still cheaper than the GE and has stock
		long worldCost = 0;
		int perWorld = 0;
		int highest = offer.getPrice();
		while (perWorld < want.getQuantity() && offer.inStock(perWorld))
		{
			int price = offer.unitPrice(perWorld);
			if (price >= ge)
			{
				break;
			}
			worldCost += price;
			highest = price;
			perWorld++;
		}
		if (perWorld == 0)
		{
			return null;
		}
		int worlds = (want.getQuantity() + perWorld - 1) / perWorld;
		if (worlds > MAX_WORLDS)
		{
			return null;
		}
		int lastWorld = want.getQuantity() - (worlds - 1) * perWorld;
		long cost = (worlds - 1) * worldCost;
		for (int i = 0; i < lastWorld; i++)
		{
			cost += offer.unitPrice(i);
		}
		return new ShopPlan.Line(want, offer, cost, perWorld, worlds, highest);
	}

	private static List<Want> rest(List<Want> wants, List<ShopPlan.Line> lines)
	{
		List<Want> rest = new ArrayList<>();
		for (Want w : wants)
		{
			if (w.getQuantity() > 0 && lines.stream().noneMatch(l -> l.getWant() == w))
			{
				rest.add(w);
			}
		}
		return rest;
	}

	/**
	 * Tiles between two points, counting an underground spot as the surface above it. 0 when either is unknown.
	 */
	static int distance(WorldPoint a, WorldPoint b)
	{
		if (a == null || b == null)
		{
			return 0;
		}
		return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(surfaceY(a) - surfaceY(b)));
	}

	private static int surfaceY(WorldPoint p)
	{
		return p.getY() >= UNDERGROUND_OFFSET ? p.getY() - UNDERGROUND_OFFSET : p.getY();
	}
}
