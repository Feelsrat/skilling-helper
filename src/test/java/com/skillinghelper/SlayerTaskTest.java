package com.skillinghelper;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.SlayerPlaces;
import com.skillinghelper.data.Target;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.SlayerTask;
import com.skillinghelper.engine.SlayerTaskStep;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SlayerTaskTest
{
	private static final int GOBLINS = 2;
	private static final int ABERRANT_SPECTRES = 41;
	private static final int ABYSSAL_DEMONS = 42;
	private static final int KONAR = 8;

	private final GuideRepository guides = new GuideRepository();
	private final StepResolver resolver = new StepResolver(id -> 1000);
	private final Method turael = guides.byId("slayer_turael");
	private final WorldPoint burthorpe = new WorldPoint(2931, 3536, 0);

	private static PlayerState.PlayerStateBuilder player(WorldPoint at, SlayerTask task)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, s == Skill.HITPOINTS ? 10 : 1);
		}
		levels.put(Skill.SLAYER, 5);
		// Bank seen and empty: no enchanted gem, no food, no gold
		return PlayerState.builder().levels(levels).freeInventorySlots(28).bank(ItemCounts.EMPTY).location(at)
			.slayerTask(task);
	}

	private static SlayerTask task(int id, String name, int remaining, int points)
	{
		return SlayerTask.builder().taskId(id).name(name).remaining(remaining).masterId(1).points(points).build();
	}

	private GuideStep step(Method m, PlayerState s)
	{
		List<Requirement> reqs = resolver.requirements(m, s, 100);
		return resolver.resolve(m, s, 20, false, reqs);
	}

	private static boolean near(WorldPoint a, WorldPoint b, int tiles)
	{
		return a.getPlane() == b.getPlane() && Math.abs(a.getX() - b.getX()) <= tiles
			&& Math.abs(a.getY() - b.getY()) <= tiles;
	}

	/**
	 * The soft lock: with a task and too few points to cancel it, the guide kept pointing at the Slayer master (or at
	 * buying a gem) and never said where the task's monsters are.
	 */
	@Test
	public void taskWithoutPointsToCancelLeadsToTheMonsters()
	{
		PlayerState s = player(burthorpe, task(GOBLINS, "Goblins", 15, 0)).build();
		GuideStep step = step(turael, s);

		assertEquals(StepType.WALK, step.getType());
		assertEquals("Go to Lumbridge", step.getTitle());
		assertNotNull(step.getDestination());
		assertFalse("must not send the player back to the master",
			turael.getLocation().contains(step.getDestination()));
		assertTrue(step.getDetails().contains("Slayer task: 15 goblins left."));
		assertTrue(step.getRoute().pathTargets().contains(new WorldPoint(3156, 3293, 0)));
		assertTrue(step.getHighlights().stream().anyMatch(t -> t.matches("Goblin", new String[]{"Attack"})));
		// Not enough points, so no cancel offer
		assertTrue(step.getDetails().stream().noneMatch(l -> l.contains("cancel")));
	}

	@Test
	public void atTheSpawnTheGuideSaysToKillThem()
	{
		PlayerState s = player(new WorldPoint(3158, 3290, 0), task(GOBLINS, "Goblins", 15, 0)).build();
		GuideStep step = step(turael, s);
		assertEquals(StepType.TRAIN, step.getType());
		assertEquals("Kill 15 more goblins", step.getTitle());
		assertNotEquals(0, step.getHighlights().size());
	}

	@Test
	public void monstersInViewCountAsArrivedAnywhere()
	{
		SlayerTask t = task(GOBLINS, "Goblins", 3, 0).toBuilder().monstersNearby(true).build();
		assertEquals(StepType.TRAIN, step(turael, player(burthorpe, t).build()).getType());
	}

	@Test
	public void noTaskStillGoesToTheMaster()
	{
		GuideStep none = step(turael, player(new WorldPoint(3222, 3218, 0), null).build());
		assertNotEquals(StepType.TRAIN, none.getType());
		GuideStep done = step(turael, player(new WorldPoint(3222, 3218, 0), task(GOBLINS, "Goblins", 0, 0)).build());
		assertEquals(none, done);
	}

	@Test
	public void cancelIsOfferedOnlyWhenAffordable()
	{
		GuideStep rich = step(turael, player(burthorpe, task(GOBLINS, "Goblins", 15, 45)).build());
		assertEquals(StepType.WALK, rich.getType());
		assertTrue(rich.getDetails().stream().anyMatch(l -> l.contains("cancel it for 30 points (you have 45)")));
	}

	@Test
	public void konarsAreaPicksThePlace()
	{
		SlayerTask t = task(ABERRANT_SPECTRES, "Aberrant spectres", 100, 0).toBuilder().masterId(KONAR).areaId(2)
			.areaName("the Stronghold Slayer Cave").build();
		GuideStep step = step(turael, player(burthorpe, t).build());
		assertEquals("Go to Stronghold Slayer Dungeon", step.getTitle());
		assertTrue(step.getRoute().pathTargets().contains(new WorldPoint(2456, 9790, 0)));
		assertTrue(step.getDetails().contains("Konar's task only counts in the Stronghold Slayer Cave."));
	}

	@Test
	public void wildernessOnlyForKrystilia()
	{
		GuideStep normal = step(turael, player(burthorpe, task(ABYSSAL_DEMONS, "Abyssal demons", 100, 0)).build());
		assertFalse(normal.getRoute().entersWilderness());

		SlayerTask wild = task(ABYSSAL_DEMONS, "Abyssal demons", 100, 0).toBuilder().masterId(SlayerTask.KRYSTILIA)
			.build();
		GuideStep step = step(turael, player(burthorpe, wild).build());
		assertEquals("Go to Wilderness Slayer Cave", step.getTitle());
		assertTrue(step.getRoute().entersWilderness());
	}

	@Test
	public void lockedPlacesAreSkippedForOpenOnes()
	{
		// Aberrant spectres: the Catacombs are open to everyone, the Slayer Tower needs Priest in Peril
		GuideStep step = step(turael, player(burthorpe, task(ABERRANT_SPECTRES, "Aberrant spectres", 50, 0)).build());
		assertEquals("Go to Catacombs of Kourend", step.getTitle());
		for (WorldPoint p : step.getRoute().pathTargets())
		{
			assertTrue(near(p, new WorldPoint(1630, 10000, 0), 40));
		}
	}

	@Test
	public void unknownTaskStillHighlightsWithoutAPath()
	{
		GuideStep step = step(turael, player(burthorpe, task(9999, "Mystery beasts", 10, 0)).build());
		assertEquals(StepType.TRAIN, step.getType());
		assertTrue(step.getHighlights().get(0).matches("Mystery beast", new String[]{"Attack"}));
	}

	@Test
	public void taskNamesMatchLikeTheSlayerPlugin()
	{
		Target goblins = SlayerTaskStep.target("Goblins");
		assertTrue(goblins.matches("Goblin", new String[]{"Attack"}));
		assertTrue(goblins.matches("Goblin guard", new String[]{"Attack"}));
		assertFalse(goblins.matches("Hobgoblin", new String[]{"Attack"}));
		assertFalse(goblins.matches("Goblin", new String[]{"Talk-to"}));
		Target birds = SlayerTaskStep.target("Birds");
		assertTrue(birds.matches("Chicken", new String[]{"Attack"}));
	}

	/**
	 * Tasks the source data has no reachable spot or entrance for. They get the "no location yet" step.
	 */
	private static final Set<String> NO_PLACES = Set.of("Killerwatts", "Warped creatures", "Magic Axes", "Mammoths");

	@Test
	public void everyTaskHasSomewhereToGo()
	{
		SlayerPlaces data = SlayerPlaces.get();
		for (int id = 1; id < 200; id++)
		{
			if (data.taskName(id) == null || NO_PLACES.contains(data.taskName(id)))
			{
				continue;
			}
			boolean wildOnly = data.places(id, false).isEmpty();
			for (SlayerPlaces.Place p : data.places(id, wildOnly))
			{
				assertTrue(p.getLabel(), !p.getSpots().isEmpty() || p.getEntrance() != null);
			}
			assertFalse(data.taskName(id), data.places(id, wildOnly).isEmpty());
		}
	}

	@Test
	public void outOfFoodBanksAtTheClosestBankThenGoesBack()
	{
		// Fighting at the goblins in Lumbridge with no food left and sharks in the bank
		WorldPoint atGoblins = new WorldPoint(3215, 3276, 0);
		PlayerState out = player(atGoblins, task(GOBLINS, "Goblins", 15, 0))
			.bank(ItemCounts.of(ItemID.SHARK, 100, ItemID.TROUT, 50)).freeInventorySlots(20).build();
		GuideStep bank = step(turael, out);
		assertEquals(StepType.BANK, bank.getType());
		assertTrue(bank.getTitle(), bank.getTitle().startsWith("Out of food: bank at "));
		assertTrue(bank.isHighlightBank());
		assertEquals(Integer.valueOf(20), bank.getWithdrawAmounts().get(ItemID.SHARK));
		assertFalse(bank.getWithdrawAmounts().containsKey(ItemID.TROUT));
		// Lumbridge's bank is the closest one, not the GE or the master
		assertTrue(near(bank.getDestination(), new WorldPoint(3208, 3220, 2), 10));

		PlayerState fed = out.toBuilder().inventory(ItemCounts.of(ItemID.SHARK, 20)).build();
		assertEquals(StepType.TRAIN, step(turael, fed).getType());
	}

	@Test
	public void noFoodAnywhereDoesNotBlockTheTask()
	{
		GuideStep step = step(turael, player(burthorpe, task(GOBLINS, "Goblins", 15, 0)).build());
		assertEquals(StepType.WALK, step.getType());
	}
}
