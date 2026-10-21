package com.skillinghelper;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.Mooring;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Route;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RouteTest
{
	private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);

	private static PlayerState.PlayerStateBuilder player(int sailing)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		levels.put(Skill.SAILING, sailing);
		return PlayerState.builder().levels(levels).location(LUMBRIDGE).freeInventorySlots(28);
	}

	@Test
	public void everyTileOfAnAreaIsCloseToATarget()
	{
		for (int radius : new int[]{1, 3, 4, 5, 8, 12, 25, 60})
		{
			Location l = Location.of("Test", 3000, 3000, radius);
			Route route = Route.to(player(1).build(), l);
			List<WorldPoint> targets = route.pathTargets();
			assertTrue("radius " + radius + " sends " + targets.size(), targets.size() <= 49);
			assertTrue(targets.contains(l.getPoint()));
			for (int x = -radius; x <= radius; x++)
			{
				for (int y = -radius; y <= radius; y++)
				{
					int best = Integer.MAX_VALUE;
					for (WorldPoint t : targets)
					{
						assertTrue(l.contains(t));
						best = Math.min(best, Math.max(Math.abs(3000 + x - t.getX()), Math.abs(3000 + y - t.getY())));
					}
					assertTrue("radius " + radius, best <= route.reachDistance());
				}
			}
		}
	}

	@Test
	public void seaSpotSendsPlayerToTheirBoatFirst()
	{
		Location wreck = Location.sea("Small shipwrecks", 3110, 2948, 25);
		Route unknown = Route.to(player(20).build(), wreck);
		assertEquals(Route.Kind.BOARD_BOAT, unknown.getKind());
		assertEquals(Mooring.PORT_SARIM.getPoint(), unknown.getPoint());
		for (Location dock : unknown.getStops())
		{
			assertFalse(dock.isAtSea());
		}

		Route known = Route.to(player(20).boatMooring(Mooring.MUSA_POINT).build(), wreck);
		assertEquals(1, known.getStops().size());
		assertEquals(Mooring.MUSA_POINT.getPoint(), known.getPoint());
		assertTrue(known.title().contains("Musa Point"));

		Route sailing = Route.to(player(20).onBoat(true).build(), wreck);
		assertEquals(Route.Kind.TRAVEL, sailing.getKind());
		assertTrue(sailing.bySea());
	}

	@Test
	public void sailingToALandSpotDocksNearIt()
	{
		Route route = Route.to(player(30).onBoat(true).build(), Location.of("Catherby bank", 2809, 3440, 5));
		assertEquals(Route.Kind.DOCK, route.getKind());
		assertEquals(Mooring.CATHERBY.getPoint(), route.getPoint());
	}

	@Test
	public void wildernessSpotsAllowWildernessPaths()
	{
		assertTrue(Route.to(player(1).build(), Location.of("Chaos Temple", 2948, 3820, 8)).entersWilderness());
		assertFalse(Route.to(player(1).build(), Location.of("Lumbridge", 3222, 3218, 8)).entersWilderness());
	}

	@Test
	public void salvageWalksToBoatAndHeadsForNearestWreck()
	{
		GuideRepository guides = new GuideRepository();
		Method salvage = guides.byId("sail_salvage_small");
		StepResolver resolver = new StepResolver(id -> 1000);
		PlayerState onFoot = player(20).bank(ItemCounts.EMPTY)
			.completedQuests(EnumSet.of(Quest.PANDEMONIUM)).build();
		GuideStep step = resolver.resolve(salvage, onFoot, 30, false, resolver.requirements(salvage, onFoot, 30));
		assertEquals(StepType.WALK, step.getType());
		assertEquals(Route.Kind.BOARD_BOAT, step.getRoute().getKind());

		PlayerState aboard = player(20).bank(ItemCounts.EMPTY).onBoat(true).location(new WorldPoint(1650, 3250, 0))
			.completedQuests(EnumSet.of(Quest.PANDEMONIUM)).build();
		step = resolver.resolve(salvage, aboard, 30, false, resolver.requirements(salvage, aboard, 30));
		assertEquals(StepType.WALK, step.getType());
		// The Shipwreck Cove site is much nearer than the Kharidian Sea one
		assertEquals(new WorldPoint(1589, 3323, 0), step.getDestination());
	}
}
