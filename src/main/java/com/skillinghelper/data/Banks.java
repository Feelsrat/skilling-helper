package com.skillinghelper.data;

import java.util.List;
import net.runelite.api.coords.WorldPoint;

/**
 * Bank locations used by guides. Coordinates point at the bank booths / chest.
 */
public final class Banks
{
	public static final Location DRAYNOR = Location.of("Draynor Village bank", 3092, 3245, 6);
	public static final Location VARROCK_WEST = Location.of("Varrock West bank", 3185, 3438, 6);
	public static final Location VARROCK_EAST = Location.of("Varrock East bank", 3253, 3421, 6);
	public static final Location GRAND_EXCHANGE = Location.of("Grand Exchange", 3164, 3487, 8);
	public static final Location EDGEVILLE = Location.of("Edgeville bank", 3094, 3493, 6);
	public static final Location FALADOR_EAST = Location.of("Falador East bank", 3013, 3356, 6);
	public static final Location AL_KHARID = Location.of("Al Kharid bank", 3269, 3167, 6);
	public static final Location SEERS = Location.of("Seers' Village bank", 2725, 3492, 6);
	public static final Location CATHERBY = Location.of("Catherby bank", 2809, 3440, 5);
	public static final Location FISHING_GUILD = Location.of("Fishing Guild bank", 2586, 3420, 5);
	public static final Location FALADOR_WEST = Location.of("Falador West bank", 2946, 3368, 6);
	public static final Location ARDOUGNE_SOUTH = Location.of("Ardougne South bank", 2655, 3286, 6);
	public static final Location CASTLE_WARS = Location.of("Castle Wars bank chest", 2443, 3083, 5);
	public static final Location CANIFIS = Location.of("Canifis bank", 3512, 3480, 5);
	public static final Location HOSIDIUS_KITCHEN = Location.of("Hosidius kitchen bank", 1676, 3617, 5);
	public static final Location LUMBRIDGE = Location.of("Lumbridge Castle bank", 3208, 3220, 2, 5);

	/**
	 * Banks open to free-to-play players.
	 */
	private static final List<Location> F2P = List.of(LUMBRIDGE, DRAYNOR, VARROCK_WEST, VARROCK_EAST, GRAND_EXCHANGE,
		EDGEVILLE, FALADOR_EAST, FALADOR_WEST, AL_KHARID,
		Location.of("Ferox Enclave bank", 3130, 3631, 6));

	/**
	 * Members' banks. Guild banks with entry requirements are left out.
	 */
	private static final List<Location> MEMBERS = List.of(SEERS, CATHERBY, FISHING_GUILD, ARDOUGNE_SOUTH, CASTLE_WARS,
		CANIFIS, HOSIDIUS_KITCHEN,
		Location.of("Ardougne North bank", 2616, 3332, 6),
		Location.of("Yanille bank", 2613, 3093, 5),
		Location.of("Gnome Stronghold bank", 2449, 3482, 1, 5),
		Location.of("Barbarian Outpost bank", 2536, 3573, 5),
		Location.of("Shilo Village bank", 2852, 2954, 5),
		Location.of("Port Phasmatys bank", 3688, 3467, 5),
		Location.of("Burgh de Rott bank", 3496, 3211, 5),
		Location.of("Nardah bank", 3428, 2892, 5),
		Location.of("Shantay Pass bank chest", 3308, 3120, 4),
		Location.of("Neitiznot bank", 2337, 3807, 5),
		Location.of("Jatizso bank", 2416, 3801, 5),
		Location.of("Museum Camp bank (Fossil Island)", 3739, 3804, 5),
		Location.of("Piscatoris bank", 2330, 3689, 5),
		Location.of("Lletya bank", 2353, 3163, 5),
		Location.of("Zanaris bank", 2383, 4458, 5),
		Location.of("Arceuus bank", 1625, 3745, 5),
		Location.of("Piscarilius bank", 1803, 3790, 5),
		Location.of("Lovakengj bank", 1533, 3740, 5),
		Location.of("Wintertodt bank chest", 1640, 3944, 5));

	/**
	 * The closest bank to a point as the crow flies, or null if none is on the same part of the map (e.g. deep
	 * underground).
	 */
	public static Location nearest(WorldPoint from, boolean members)
	{
		if (from == null)
		{
			return null;
		}
		Location best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (List<Location> banks : members ? List.of(F2P, MEMBERS) : List.of(F2P))
		{
			for (Location bank : banks)
			{
				// Upstairs banks (Lumbridge Castle, Gnome Stronghold) take a while to reach
				int d = distance(from, bank.getPoint()) + (bank.getPoint().getPlane() != from.getPlane() ? 15 : 0);
				if (d < bestDistance)
				{
					best = bank;
					bestDistance = d;
				}
			}
		}
		// Underground areas and instances are thousands of tiles away from the surface banks
		return bestDistance <= 600 ? best : null;
	}

	public static int distance(WorldPoint a, WorldPoint b)
	{
		return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
	}

	private Banks()
	{
	}
}
