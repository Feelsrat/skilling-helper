package com.skillinghelper.plunder;

import com.skillinghelper.engine.GuideStep;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PyramidPlunderTest
{
	private static PyramidPlunder.Situation.SituationBuilder at(int level, int room)
	{
		return PyramidPlunder.Situation.builder()
			.level(level)
			.room(room)
			.ticksLeft(400)
			.doorsClosed(4)
			.prayerLevel(1);
	}

	@Test
	public void deepestRoomByLevel()
	{
		assertEquals(0, PyramidPlunder.maxRoom(20));
		assertEquals(1, PyramidPlunder.maxRoom(21));
		assertEquals(1, PyramidPlunder.maxRoom(30));
		assertEquals(2, PyramidPlunder.maxRoom(31));
		assertEquals(6, PyramidPlunder.maxRoom(71));
		assertEquals(8, PyramidPlunder.maxRoom(91));
		assertEquals(8, PyramidPlunder.maxRoom(99));
	}

	@Test
	public void trapFirst()
	{
		assertEquals(PyramidPlunder.Action.PASS_TRAP, PyramidPlunder.decide(at(75, 5).trapActive(true)
			.urnsLeft(13).nextUrn(PyramidPlunder.UrnState.CLOSED).chestClosed(true).build()));
	}

	@Test
	public void earlyRoomsGoStraightToTheDoor()
	{
		// Early rooms' urns are never worth it, and their chests only with time to spare
		assertEquals(PyramidPlunder.Action.PICK_DOOR, PyramidPlunder.decide(at(75, 2).ticksLeft(180)
			.urnsLeft(13).nextUrn(PyramidPlunder.UrnState.CLOSED).chestClosed(true).build()));
		assertEquals(PyramidPlunder.Action.OPEN_CHEST, PyramidPlunder.decide(at(75, 4)
			.urnsLeft(13).nextUrn(PyramidPlunder.UrnState.CLOSED).chestClosed(true).build()));
	}

	@Test
	public void urnsThenChestThenDoor()
	{
		PyramidPlunder.Situation.SituationBuilder room5 = at(75, 5).chestClosed(true);
		assertEquals(PyramidPlunder.Action.SEARCH_URN, PyramidPlunder.decide(room5
			.urnsLeft(3).nextUrn(PyramidPlunder.UrnState.CLOSED).build()));
		assertEquals(PyramidPlunder.Action.OPEN_CHEST, PyramidPlunder.decide(room5
			.urnsLeft(0).nextUrn(null).build()));
		assertEquals(PyramidPlunder.Action.PICK_DOOR, PyramidPlunder.decide(room5
			.chestClosed(false).build()));
	}

	@Test
	public void lastRoomEndsByLeaving()
	{
		assertEquals(PyramidPlunder.Action.LEAVE, PyramidPlunder.decide(at(75, 6).urnsLeft(0).build()));
	}

	@Test
	public void snakeCharmBelow51()
	{
		PyramidPlunder.Situation.SituationBuilder low = at(45, 3).carriesCharm(true).urnsLeft(5);
		assertEquals(PyramidPlunder.Action.CHECK_URN,
			PyramidPlunder.decide(low.nextUrn(PyramidPlunder.UrnState.CLOSED).build()));
		assertEquals(PyramidPlunder.Action.CHARM_URN,
			PyramidPlunder.decide(low.nextUrn(PyramidPlunder.UrnState.SNAKE).build()));
		assertEquals(PyramidPlunder.Action.SEARCH_URN,
			PyramidPlunder.decide(low.nextUrn(PyramidPlunder.UrnState.CHARMED).build()));
		// Without the charm, or at 51+, just search
		assertEquals(PyramidPlunder.Action.SEARCH_URN, PyramidPlunder.decide(low.carriesCharm(false)
			.nextUrn(PyramidPlunder.UrnState.CLOSED).build()));
		assertEquals(PyramidPlunder.Action.SEARCH_URN, PyramidPlunder.decide(at(55, 4).carriesCharm(true)
			.urnsLeft(5).nextUrn(PyramidPlunder.UrnState.CLOSED).build()));
	}

	@Test
	public void sceptreHuntOpensSarcophagi()
	{
		PyramidPlunder.Situation.SituationBuilder s = at(75, 4).sceptreHunt(true).sarcophagusClosed(true);
		assertEquals(PyramidPlunder.Action.OPEN_SARCOPHAGUS, PyramidPlunder.decide(s.build()));
		assertEquals(PyramidPlunder.Action.SEARCH_SARCOPHAGUS, PyramidPlunder.decide(s.sarcophagusClosed(false)
			.sarcophagusOpen(true).build()));
		assertEquals(PyramidPlunder.Action.PICK_DOOR, PyramidPlunder.decide(at(75, 4).sarcophagusClosed(true)
			.build()));
	}

	@Test
	public void lockpickHalvesDoorXp()
	{
		GuideStep with = PyramidPlunder.step(at(75, 3).carriesLockpick(true).build(),
			PyramidPlunder.Action.PICK_DOOR);
		assertTrue(with.getDetails().get(0).contains("50 xp with your lockpick"));
		assertTrue(String.join(" ", with.getDetails()).contains("works by itself"));
		GuideStep without = PyramidPlunder.step(at(60, 3).build(), PyramidPlunder.Action.PICK_DOOR);
		assertTrue(String.join(" ", without.getDetails()).contains("bring a lockpick"));
	}

	@Test
	public void freshRoundAtSeventyFive()
	{
		List<PyramidPlunder.RoomPlan> plan = PyramidPlunder.freshPlan(75, false);
		assertEquals(6, plan.size());
		assertEquals(0, plan.get(3).getUrns());
		assertEquals(13, plan.get(4).getUrns());
		assertEquals(13, plan.get(5).getUrns());
		assertTrue(plan.get(3).isChest());
		assertTrue(plan.get(5).isLast());
		assertEquals(List.of(
			"• Rooms 1-4: trap, then chest, then door.",
			"• Room 5: trap, then every urn, then chest, then door.",
			"• Room 6: trap, then every urn, then chest, then leave."), PyramidPlunder.roundPlan(plan, 0));
	}

	@Test
	public void slowRoomShortensItsUrns()
	{
		// 2 minutes left in room 5: the deepest room's urns come first, so room 5 gets only a few
		List<PyramidPlunder.RoomPlan> plan = PyramidPlunder.plan(75, 5, true, 13, false, true, 200, false);
		assertEquals(13, plan.get(1).getUrns());
		assertTrue(plan.get(0).getUrns() < 13);
		assertEquals(PyramidPlunder.Action.SEARCH_URN, PyramidPlunder.decide(at(75, 5).ticksLeft(200)
			.urnsLeft(13).nextUrn(PyramidPlunder.UrnState.CLOSED).chestClosed(true).build()));
		// With under a minute left, room 5's urns are dropped for room 6
		assertEquals(PyramidPlunder.Action.OPEN_CHEST, PyramidPlunder.decide(at(75, 5).ticksLeft(100)
			.urnsLeft(13).nextUrn(PyramidPlunder.UrnState.CLOSED).chestClosed(true).build()));
		// In the last room every second goes to its urns
		assertEquals(PyramidPlunder.Action.SEARCH_URN, PyramidPlunder.decide(at(75, 6).ticksLeft(40)
			.urnsLeft(5).nextUrn(PyramidPlunder.UrnState.CLOSED).build()));
	}

	@Test
	public void lowLevelsLootEverything()
	{
		List<PyramidPlunder.RoomPlan> one = PyramidPlunder.freshPlan(25, false);
		assertEquals(1, one.size());
		assertEquals(13, one.get(0).getUrns());
		assertTrue(one.get(0).isChest());
	}

	@Test
	public void sceptreHuntPlansEveryChestAndLateSarcophagi()
	{
		List<PyramidPlunder.RoomPlan> plan = PyramidPlunder.freshPlan(95, true);
		for (PyramidPlunder.RoomPlan p : plan)
		{
			assertTrue(p.isChest());
			assertEquals(p.getRoom() >= 4, p.isSarcophagus());
		}
	}

	@Test
	public void shortestUrnWalk()
	{
		// Three urns in a row: from the left end, walk them left to right
		int[] xs = {10, 2, 6};
		int[] ys = {0, 0, 0};
		assertEquals(List.of(1, 2, 0), UrnWalk.order(0, 0, xs, ys, 3));
		assertEquals(List.of(1), UrnWalk.order(0, 0, xs, ys, 1));
		assertEquals(List.of(), UrnWalk.order(0, 0, xs, ys, 0));
	}

	@Test
	public void threatComesFirst()
	{
		GuideStep step = PyramidPlunder.step(at(75, 5).threat(PyramidPlunder.Threat.SWARM).prayerLevel(70)
			.urnsLeft(3).nextUrn(PyramidPlunder.UrnState.CLOSED).build(), PyramidPlunder.Action.SEARCH_URN);
		assertTrue(step.getDetails().get(0).contains("Protect from Melee"));
	}

	@Test
	public void timeLeft()
	{
		assertEquals("5:00", PyramidPlunder.timeLeft(500));
		assertEquals("1:30", PyramidPlunder.timeLeft(150));
		assertEquals("0:00", PyramidPlunder.timeLeft(-3));
	}

	@Test
	public void charmOnlyWhenUseful()
	{
		assertTrue(PyramidPlunder.needsCharm(30, true, 0));
		assertFalse(PyramidPlunder.needsCharm(30, true, 1));
		assertFalse(PyramidPlunder.needsCharm(30, false, 0));
		assertFalse(PyramidPlunder.needsCharm(51, true, 0));
	}

	@Test
	public void sceptreCharging()
	{
		assertTrue(PyramidPlunder.canCharge(true, 6, 0, 0));
		assertTrue(PyramidPlunder.canCharge(true, 0, 12, 0));
		assertTrue(PyramidPlunder.canCharge(true, 0, 0, 24));
		assertFalse(PyramidPlunder.canCharge(true, 5, 11, 23));
		assertFalse(PyramidPlunder.canCharge(false, 6, 12, 24));
	}

	@Test
	public void fullInventoryWarning()
	{
		GuideStep step = PyramidPlunder.step(at(75, 5).inventoryFull(true).urnsLeft(3)
			.nextUrn(PyramidPlunder.UrnState.CLOSED).build(), PyramidPlunder.Action.SEARCH_URN);
		assertTrue(String.join(" ", step.getDetails()).contains("inventory is full"));
	}
}
