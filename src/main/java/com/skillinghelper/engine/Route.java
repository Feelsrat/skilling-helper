package com.skillinghelper.engine;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Mooring;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.Value;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * How to get to a place from where the player is now: walk there, or first board or dock a boat when the place is
 * out at sea (or the player is). Also builds the targets handed to the Shortest Path plugin.
 */
@Value
public class Route
{
	public enum Kind
	{
		/**
		 * Walk (or teleport, or take transports) to one of the stops.
		 */
		TRAVEL,
		/**
		 * The place is at sea: go to the boat's mooring and board it first.
		 */
		BOARD_BOAT,
		/**
		 * The player is sailing but the place is on land: dock at the mooring nearest to it first.
		 */
		DOCK,
	}

	/**
	 * Most targets sent to Shortest Path per stop, per axis.
	 */
	private static final int GRID = 7;

	// The Wilderness, above ground and in its dungeons
	private static final int WILDERNESS_MIN_X = 2944;
	private static final int WILDERNESS_MAX_X = 3391;
	private static final int WILDERNESS_MIN_Y = 3520;
	private static final int WILDERNESS_MAX_Y = 3967;
	private static final int WILDERNESS_UNDERGROUND_MIN_Y = 9920;
	private static final int WILDERNESS_UNDERGROUND_MAX_Y = 10367;

	Kind kind;
	/**
	 * The place the player is ultimately heading for.
	 */
	String placeName;
	/**
	 * Where to go next, nearest to the player first. The path may end at whichever is quickest to reach.
	 */
	List<Location> stops;
	/**
	 * Whether the player's boat mooring is known, for {@link Kind#BOARD_BOAT}.
	 */
	boolean mooringKnown;

	/**
	 * A route to the first of {@code places} the player can reach quickest. All places share the name of the first.
	 */
	public static Route to(PlayerState s, List<Location> places)
	{
		Location first = places.get(0);
		Route boatLeg = null;
		if (first.isAtSea() && !s.isOnBoat())
		{
			boatLeg = boardBoat(s, first);
		}
		else if (!first.isAtSea() && s.isOnBoat())
		{
			boatLeg = dock(s, first);
		}
		return boatLeg != null ? boatLeg
			: new Route(Kind.TRAVEL, first.getName(), nearestFirst(places, s.getLocation()), true);
	}

	/**
	 * To the boat's mooring, or every dock the player could have left it at. Null if there's none.
	 */
	private static Route boardBoat(PlayerState s, Location place)
	{
		Mooring known = s.getBoatMooring();
		List<Location> docks = new ArrayList<>();
		if (known != null)
		{
			docks.add(known.location());
		}
		else
		{
			for (Mooring m : Mooring.usable(s.level(Skill.SAILING), s.getLocation()))
			{
				docks.add(m.location());
			}
		}
		return docks.isEmpty() ? null : new Route(Kind.BOARD_BOAT, place.getName(), docks, known != null);
	}

	/**
	 * Dock where the walk to the place is shortest; the boat stays there for next time. Null if there's no dock.
	 */
	private static Route dock(PlayerState s, Location place)
	{
		List<Mooring> near = Mooring.usable(s.level(Skill.SAILING), place.getPoint());
		return near.isEmpty() ? null : new Route(Kind.DOCK, place.getName(), List.of(near.get(0).location()), true);
	}

	public static Route to(PlayerState s, Location place)
	{
		return to(s, List.of(place));
	}

	private static List<Location> nearestFirst(List<Location> places, WorldPoint me)
	{
		if (places.size() < 2 || me == null)
		{
			return places;
		}
		List<Location> sorted = new ArrayList<>(places);
		sorted.sort(Comparator.comparingInt(l -> distance(l.getPoint(), me)));
		return Collections.unmodifiableList(sorted);
	}

	/**
	 * Where the arrows point: the nearest stop.
	 */
	public WorldPoint getPoint()
	{
		return stops.get(0).getPoint();
	}

	/**
	 * The name of where the arrows point.
	 */
	public String getStopName()
	{
		return kind == Kind.TRAVEL ? placeName : stops.get(0).getName();
	}

	/**
	 * The step title for walking there.
	 */
	public String title()
	{
		switch (kind)
		{
			case BOARD_BOAT:
				return mooringKnown ? "Board your boat at " + stops.get(0).getName() : "Go to your boat and board it";
			case DOCK:
				return "Dock at " + stops.get(0).getName();
			case TRAVEL:
			default:
				return "Go to " + placeName;
		}
	}

	/**
	 * Extra lines explaining a boat leg, empty for a plain walk.
	 */
	public List<String> hints()
	{
		switch (kind)
		{
			case BOARD_BOAT:
				List<String> lines = new ArrayList<>();
				if (!mooringKnown)
				{
					lines.add("Your boat is moored where you last docked it. The path goes to the nearest dock you can "
						+ "use; I'll remember your boat's dock once I see you leave it.");
				}
				lines.add("Then sail to " + placeName + ". The path switches to a sailing route once you're aboard.");
				return lines;
			case DOCK:
				return List.of("Dock at " + stops.get(0).getName() + " first, then walk to " + placeName
					+ ". Or teleport there and leave the boat where it is.");
			case TRAVEL:
			default:
				return Collections.emptyList();
		}
	}

	/**
	 * Tiles to hand to Shortest Path. Ending the path on any of them counts as arriving, so a spot whose centre is a
	 * tree, a rock or water still has walkable goals, and big areas are reached at their nearest edge.
	 */
	public List<WorldPoint> pathTargets()
	{
		List<WorldPoint> out = new ArrayList<>();
		for (Location l : stops)
		{
			out.addAll(areaTargets(l));
		}
		return out;
	}

	/**
	 * How far from the nearest target the path may end and still count as arrived.
	 */
	public int reachDistance()
	{
		int reach = 2;
		for (Location l : stops)
		{
			reach = Math.max(reach, spacing(l.getRadius()));
		}
		return reach;
	}

	/**
	 * Whether this leg is sailed rather than walked.
	 */
	public boolean bySea()
	{
		return kind == Kind.DOCK || stops.get(0).isAtSea();
	}

	public boolean entersWilderness()
	{
		for (Location l : stops)
		{
			if (inWilderness(l.getPoint()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * A grid of points covering the area, at most {@value #GRID} per axis, always including the centre.
	 */
	static List<WorldPoint> areaTargets(Location l)
	{
		WorldPoint c = l.getPoint();
		int r = l.getRadius();
		if (r <= 3)
		{
			return List.of(c);
		}
		int step = spacing(r);
		int max = r / step * step;
		List<WorldPoint> out = new ArrayList<>();
		out.add(c);
		for (int dx = -max; dx <= max; dx += step)
		{
			for (int dy = -max; dy <= max; dy += step)
			{
				if (dx != 0 || dy != 0)
				{
					out.add(new WorldPoint(c.getX() + dx, c.getY() + dy, c.getPlane()));
				}
			}
		}
		return out;
	}

	/**
	 * Grid spacing for an area of this radius. Every tile of the area is within this many tiles of a grid point.
	 */
	static int spacing(int radius)
	{
		if (radius <= 3)
		{
			return radius;
		}
		return Math.max(2, (2 * radius + GRID - 2) / (GRID - 1));
	}

	static boolean inWilderness(WorldPoint wp)
	{
		int x = wp.getX();
		int y = wp.getY();
		return x >= WILDERNESS_MIN_X && x <= WILDERNESS_MAX_X
			&& ((y >= WILDERNESS_MIN_Y && y <= WILDERNESS_MAX_Y)
			|| (y >= WILDERNESS_UNDERGROUND_MIN_Y && y <= WILDERNESS_UNDERGROUND_MAX_Y));
	}

	private static int distance(WorldPoint a, WorldPoint b)
	{
		return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
	}
}
