package com.skillinghelper.travel;

import com.skillinghelper.data.Wilderness;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/**
 * Picks the one travel item worth bringing to a destination: the teleport (or fairy ring) the player owns and has
 * unlocked that lands closest to it, if that beats walking by enough. Distances are as the crow flies, with
 * dungeons counted from the place above them, so this is a rough guess; Shortest Path still works out the actual
 * route, and only uses the item if it's quicker.
 */
public final class TravelPlanner
{
	/**
	 * Below this many tiles to walk, a teleport isn't worth the bank slot.
	 */
	static final int MIN_WALK = 40;
	/**
	 * A teleport has to save at least this many tiles over walking.
	 */
	static final int MIN_SAVING = 30;
	/**
	 * Teleports landing further than this from the destination aren't suggested: Shortest Path would likely take
	 * another way from there anyway.
	 */
	static final int MAX_LANDING = 90;
	/**
	 * Tiles a teleport "costs": the cast, the animation, and finding the item.
	 */
	static final int TELEPORT_COST = 6;
	/**
	 * Extra cost for a teleport that spends a charge or a tablet, so a free one wins a close race.
	 */
	static final int CONSUMABLE_COST = 4;
	/**
	 * An item already carried wins over a slightly better one still in the bank.
	 */
	static final int CARRIED_BONUS = 15;
	private static final int UNDERGROUND_OFFSET = 6400;

	private TravelPlanner()
	{
	}

	/**
	 * What to bring for heading to {@code destinations}: the best travel item (see {@link #pick}) and, after it,
	 * coins for a paid transport (boat, charter ship, magic carpet, dungeon entrance) that gets there quicker from
	 * where the item lands. Empty when walking is fine.
	 *
	 * @param carried how many of an item id are in the inventory or worn
	 * @param inBank  how many of an item id are in the bank (placeholders don't count)
	 */
	public static List<TravelPick> plan(List<TravelOption> options, TravelUnlocks unlocks, IntUnaryOperator carried,
		IntUnaryOperator inBank, WorldPoint from, List<WorldPoint> destinations, String destinationName,
		boolean allowWild)
	{
		List<TravelPick> out = new ArrayList<>();
		TravelPick item = pick(options, unlocks, id -> carried.applyAsInt(id) > 0, id -> inBank.applyAsInt(id) > 0,
			from, destinations, destinationName, allowWild);
		if (item != null)
		{
			out.add(item);
		}
		// After the item, the player stands where it lands (a fairy ring option is the ring it lands at)
		WorldPoint feeFrom = item != null ? item.getOption().getPoint() : from;
		TravelPick fee = fee(options, unlocks, carried.applyAsInt(ItemID.COINS), inBank.applyAsInt(ItemID.COINS),
			feeFrom, destinations, destinationName, allowWild);
		if (fee != null)
		{
			out.add(fee);
		}
		return out;
	}

	/**
	 * Coins for the paid transport that gets from {@code from} to a destination quickest, if it beats walking by
	 * enough, or the destination is in a dungeon that a paid entrance leads into. Null otherwise, or when the
	 * player doesn't have the coins.
	 */
	static TravelPick fee(List<TravelOption> options, TravelUnlocks unlocks, int carriedCoins, int bankCoins,
		WorldPoint from, List<WorldPoint> destinations, String destinationName, boolean allowWild)
	{
		if (from == null || destinations.isEmpty())
		{
			return null;
		}
		int walk = Integer.MAX_VALUE;
		boolean underground = false;
		for (WorldPoint d : destinations)
		{
			walk = Math.min(walk, distance(from, d));
			underground |= isUnderground(d);
		}
		TravelOption best = null;
		int bestCost = Integer.MAX_VALUE;
		int bestLand = 0;
		for (int i = 0; i < options.size(); i++)
		{
			TravelOption o = options.get(i);
			if (o.getKind() != TravelOption.Kind.FEE || !unlocks.usable(i)
				|| (!allowWild && (Wilderness.contains(o.getPoint()) || Wilderness.contains(o.getOrigin())))
				|| o.getCoins() > carriedCoins + bankCoins)
			{
				continue;
			}
			int land = Integer.MAX_VALUE;
			for (WorldPoint d : destinations)
			{
				land = Math.min(land, distance(o.getPoint(), d));
			}
			int approach = distance(from, o.getOrigin());
			if (land > MAX_LANDING || approach > MAX_LANDING * 3)
			{
				continue;
			}
			int cost = approach + land + TELEPORT_COST;
			// A dungeon behind a paid entrance can't be walked into as the crow flies suggests
			boolean entrance = underground && isUnderground(o.getPoint()) && !isUnderground(o.getOrigin())
				&& approach <= MAX_LANDING;
			if (!entrance && (walk < MIN_WALK || cost + MIN_SAVING > walk))
			{
				continue;
			}
			if (cost < bestCost)
			{
				best = o;
				bestCost = cost;
				bestLand = land;
			}
		}
		if (best == null)
		{
			return null;
		}
		// Boats and carpets are paid both ways; a dungeon entrance only going in
		int want = isUnderground(best.getPoint()) ? best.getCoins() : best.getCoins() * 2;
		want = Math.min(want, carriedCoins + bankCoins);
		boolean enough = carriedCoins >= want;
		return new TravelPick(best, ItemID.COINS, enough, bestLand, destinationName,
			enough ? 0 : want - carriedCoins);
	}

	private static boolean isUnderground(WorldPoint p)
	{
		return p.getY() >= UNDERGROUND_OFFSET && p.getY() < 2 * UNDERGROUND_OFFSET && p.getX() < 4000;
	}

	/**
	 * The best travel item for getting from {@code from} to any of {@code destinations}, or null when walking is
	 * fine or the player has nothing that helps.
	 *
	 * @param carried       whether an item id is in the inventory or worn
	 * @param inBank        whether an item id is in the bank (placeholders don't count)
	 * @param allowWild     whether a teleport may land in the Wilderness: only when the destination is there and the
	 *                      player opted in
	 */
	public static TravelPick pick(List<TravelOption> options, TravelUnlocks unlocks, IntPredicate carried,
		IntPredicate inBank, WorldPoint from, List<WorldPoint> destinations, String destinationName,
		boolean allowWild)
	{
		if (from == null || destinations.isEmpty())
		{
			return null;
		}
		int walk = Integer.MAX_VALUE;
		for (WorldPoint d : destinations)
		{
			walk = Math.min(walk, distance(from, d));
		}
		if (walk < MIN_WALK)
		{
			return null;
		}
		int staff = unlocks.fairyRings() && unlocks.staffNeeded() ? owned(TravelData.FAIRY_STAFFS, carried, inBank) : -1;
		int ringFromHere = staff > 0 ? nearestRing(options, unlocks, from) : Integer.MAX_VALUE;

		Candidate best = null;
		Candidate bestCarried = null;
		for (int i = 0; i < options.size(); i++)
		{
			TravelOption o = options.get(i);
			if (!unlocks.usable(i) || (!allowWild && Wilderness.contains(o.getPoint())))
			{
				continue;
			}
			if (o.getKind() == TravelOption.Kind.FEE)
			{
				continue;
			}
			int id;
			int cost;
			if (o.getKind() == TravelOption.Kind.FAIRY_RING)
			{
				if (staff <= 0 || ringFromHere == Integer.MAX_VALUE)
				{
					continue;
				}
				id = staff;
				cost = ringFromHere;
			}
			else
			{
				id = owned(o.getItemIds(), carried, inBank);
				if (id <= 0)
				{
					continue;
				}
				cost = o.isConsumable() ? CONSUMABLE_COST : 0;
			}
			int land = Integer.MAX_VALUE;
			for (WorldPoint d : destinations)
			{
				land = Math.min(land, distance(o.getPoint(), d));
			}
			if (land > MAX_LANDING)
			{
				continue;
			}
			cost += land + TELEPORT_COST;
			if (cost + MIN_SAVING > walk)
			{
				continue;
			}
			Candidate c = new Candidate(o, id, carried.test(id), land, cost);
			if (best == null || c.cost < best.cost)
			{
				best = c;
			}
			if (c.carried && (bestCarried == null || c.cost < bestCarried.cost))
			{
				bestCarried = c;
			}
		}
		if (best == null)
		{
			return null;
		}
		Candidate chosen = bestCarried != null && bestCarried.cost <= best.cost + CARRIED_BONUS ? bestCarried : best;
		return new TravelPick(chosen.option, chosen.id, chosen.carried, chosen.land, destinationName);
	}

	/**
	 * The first of these ids the player carries, else the first in the bank, else -1.
	 */
	private static int owned(int[] ids, IntPredicate carried, IntPredicate inBank)
	{
		for (int id : ids)
		{
			if (carried.test(id))
			{
				return id;
			}
		}
		for (int id : ids)
		{
			if (inBank.test(id))
			{
				return id;
			}
		}
		return -1;
	}

	private static int nearestRing(List<TravelOption> options, TravelUnlocks unlocks, WorldPoint from)
	{
		int best = Integer.MAX_VALUE;
		for (int i = 0; i < options.size(); i++)
		{
			TravelOption o = options.get(i);
			if (o.getKind() == TravelOption.Kind.FAIRY_RING && unlocks.usable(i) && !Wilderness.contains(o.getPoint()))
			{
				best = Math.min(best, distance(from, o.getPoint()));
			}
		}
		return best;
	}

	/**
	 * Tiles between two points as the crow flies, counting a dungeon as the place above it.
	 */
	static int distance(WorldPoint a, WorldPoint b)
	{
		return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(surfaceY(a) - surfaceY(b)));
	}

	private static int surfaceY(WorldPoint p)
	{
		return isUnderground(p) ? p.getY() - UNDERGROUND_OFFSET : p.getY();
	}

	private static final class Candidate
	{
		final TravelOption option;
		final int id;
		final boolean carried;
		final int land;
		final int cost;

		Candidate(TravelOption option, int id, boolean carried, int land, int cost)
		{
			this.option = option;
			this.id = id;
			this.carried = carried;
			this.land = land;
			this.cost = cost;
		}
	}
}
