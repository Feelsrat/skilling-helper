package com.skillinghelper.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * Sailing moorings, where a boat can be docked and boarded. Coordinates and levels match RuneLite's world map
 * (MooringLocation).
 */
@Getter
public enum Mooring
{
	PORT_SARIM("Port Sarim", 1, 3050, 3192, 0),
	THE_PANDEMONIUM("The Pandemonium", 1, 3069, 2986, 0),
	LANDS_END("Land's End", 5, 1506, 3402, 0),
	HOSIDIUS("Hosidius", 5, 1726, 3452, 0),
	MUSA_POINT("Musa Point", 10, 2960, 3147, 0),
	PORT_PISCARILLIUS("Port Piscarillius", 15, 1845, 3687, 0),
	RIMMINGTON("Rimmington", 18, 2905, 3226, 0),
	CATHERBY("Catherby", 20, 2796, 3412, 0),
	BRIMHAVEN("Brimhaven", 25, 2757, 3229, 0),
	ARDOUGNE("Ardougne", 28, 2671, 3265, 0),
	PORT_KHAZARD("Port Khazard", 30, 2685, 3161, 0),
	WITCHAVEN("Witchaven", 34, 2746, 3304, 0),
	ENTRANA("Entrana", 36, 2878, 3335, 0),
	CIVITAS_ILLA_FORTIS("Civitas illa Fortis", 38, 1774, 3141, 0),
	CORSAIR_COVE("Corsair Cove", 40, 2579, 2843, 0),
	DOGNOSE_ISLAND("Dognose Island", 40, 3061, 2639, 0),
	CAIRN_ISLE("Cairn Isle", 42, 2749, 2951, 0),
	CHINCHOMPA_ISLAND("Chinchompa Island", 42, 1892, 3429, 0),
	SUNSET_COAST("Sunset Coast", 44, 1511, 2975, 0),
	REMOTE_ISLAND("Remote Island", 45, 2971, 2603, 0),
	THE_SUMMER_SHORE("The Summer Shore", 45, 3174, 2367, 0),
	THE_LITTLE_PEARL("The Little Pearl", 45, 3354, 2216, 0),
	ALDARIN("Aldarin", 46, 1452, 2970, 0),
	VATRACHOS_ISLAND("Vatrachos Island", 46, 1872, 2985, 0),
	THE_ONYX_CREST("The Onyx Crest", 47, 2997, 2288, 0),
	RUINS_OF_UNKAH("Ruins of Unkah", 48, 3143, 2824, 0),
	SHIMMERING_ATOLL("Shimmering Atoll", 49, 1557, 2771, 0),
	VOID_KNIGHTS_OUTPOST("Void Knights' Outpost", 50, 2651, 2678, 0),
	PORT_ROBERTS("Port Roberts", 50, 1860, 3306, 0),
	ANGLERS_RETREAT("Anglers' Retreat", 51, 2467, 2721, 0),
	LAST_LIGHT("Last Light", 52, 2848, 2327, 0),
	RED_ROCK("Red Rock", 52, 2808, 2510, 0),
	MINOTAURS_REST("Minotaurs' Rest", 54, 1958, 3117, 0),
	ISLE_OF_SOULS("Isle of Souls", 55, 2282, 2823, 0),
	ISLE_OF_BONES("Isle of Bones", 56, 2532, 2531, 0),
	LAGUNA_AURORAE("Laguna Aurorae", 58, 1202, 2733, 0),
	CHARRED_ISLAND("Charred Island", 60, 2660, 2395, 0),
	TEAR_OF_THE_SOUL("Tear of the Soul", 61, 2318, 2774, 0),
	RELLEKKA("Rellekka", 62, 2630, 3705, 0),
	WYRMSCRAIG("Wyrmscraig", 62, 2567, 2297, 0),
	WYRMSCRAIG_CAVERN("Wyrmscraig Cavern", 62, 2773, 8607, 0),
	WINTUMBER_ISLAND("Wintumber Island", 63, 2058, 2606, 0),
	THE_CROWN_JEWEL("The Crown Jewel", 64, 1765, 2659, 0),
	ETCETERIA("Etceteria", 65, 2611, 3840, 0),
	PORT_TYRAS("Port Tyras", 66, 2144, 3120, 0),
	LLEDRITH_ISLAND("Lledrith Island", 66, 2097, 3188, 0),
	DEEPFIN_POINT("Deepfin Point", 67, 1923, 2758, 0),
	JATIZSO("Jatizso", 68, 2412, 3780, 0),
	NEITIZNOT("Neitiznot", 68, 2308, 3783, 0),
	RAINBOWS_END("Rainbow's End", 69, 2344, 2270, 0),
	PRIFDDINAS("Prifddinas", 70, 2158, 3324, 0),
	SUNBLEAK_ISLAND("Sunbleak Island", 72, 2189, 2327, 0),
	YNYSDAIL("Ynysdail", 73, 2222, 3466, 0),
	WATERBIRTH_ISLAND("Waterbirth Island", 74, 2543, 3765, 0),
	PISCATORIS("Piscatoris", 75, 2303, 3690, 0),
	LUNAR_ISLE("Lunar Isle", 76, 2151, 3880, 0),
	BUCCANEERS_HAVEN("Buccaneers' Haven", 76, 2080, 3690, 0),
	DRUMSTICK_ISLE("Drumstick Isle", 79, 2150, 3530, 0),
	WEISS("Weiss", 80, 2860, 3972, 0),
	BRITTLE_ISLE("Brittle Isle", 81, 1954, 4056, 0),
	GRIMSTONE("Grimstone", 87, 2927, 4056, 0);

	/**
	 * How close counts as standing at the mooring.
	 */
	private static final int RADIUS = 3;

	private final String displayName;
	private final int level;
	private final WorldPoint point;

	Mooring(String displayName, int level, int x, int y, int plane)
	{
		this.displayName = displayName;
		this.level = level;
		this.point = new WorldPoint(x, y, plane);
	}

	public Location location()
	{
		return Location.of(displayName + " dock", point.getX(), point.getY(), point.getPlane(), RADIUS);
	}

	/**
	 * Moorings a player with this Sailing level can use, nearest to {@code near} first.
	 */
	public static List<Mooring> usable(int sailingLevel, WorldPoint near)
	{
		List<Mooring> out = new ArrayList<>();
		for (Mooring m : values())
		{
			if (m.level <= sailingLevel && m.point.getY() < 6400)
			{
				out.add(m);
			}
		}
		if (near != null)
		{
			out.sort(Comparator.comparingInt(m -> distance(m.point, near)));
		}
		return out;
	}

	/**
	 * The mooring within {@code maxDistance} tiles of {@code wp}, or null.
	 */
	public static Mooring at(WorldPoint wp, int maxDistance)
	{
		if (wp == null)
		{
			return null;
		}
		Mooring best = null;
		for (Mooring m : values())
		{
			int d = distance(m.point, wp);
			if (d <= maxDistance && (best == null || d < distance(best.point, wp)))
			{
				best = m;
			}
		}
		return best;
	}

	private static int distance(WorldPoint a, WorldPoint b)
	{
		return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
	}
}
