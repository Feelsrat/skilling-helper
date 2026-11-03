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
	public void urnsOnlyInTheLastTwoRooms()
	{
		// Level 75: rooms 1-6, so urns in 5 and 6
		assertFalse(PyramidPlunder.plan(3, 6, false).isUrns());
		assertFalse(PyramidPlunder.plan(3, 6, false).isChest());
		assertTrue(PyramidPlunder.plan(4, 6, false).isChest());
		assertFalse(PyramidPlunder.plan(4, 6, false).isUrns());
		assertTrue(PyramidPlunder.plan(5, 6, false).isUrns());
		assertTrue(PyramidPlunder.plan(6, 6, false).isUrns());
		assertTrue(PyramidPlunder.plan(6, 6, false).isLast());
		assertFalse(PyramidPlunder.plan(5, 6, false).isSarcophagus());
		assertTrue(PyramidPlunder.plan(5, 6, true).isSarcophagus());
		assertTrue(PyramidPlunder.plan(1, 6, true).isChest());
	}

	@Test
	public void lowLevelsLootEveryChest()
	{
		assertTrue(PyramidPlunder.plan(1, 3, false).isChest());
		assertFalse(PyramidPlunder.plan(1, 3, false).isUrns());
		assertTrue(PyramidPlunder.plan(1, 1, false).isUrns());
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
		assertEquals(PyramidPlunder.Action.PICK_DOOR, PyramidPlunder.decide(at(75, 2)
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
	public void keepNinetySecondsForTheLastRoom()
	{
		assertEquals(PyramidPlunder.Action.OPEN_CHEST, PyramidPlunder.decide(at(75, 5).ticksLeft(100)
			.urnsLeft(5).nextUrn(PyramidPlunder.UrnState.CLOSED).chestClosed(true).build()));
		// In the last room there's no later room to save time for
		assertEquals(PyramidPlunder.Action.SEARCH_URN, PyramidPlunder.decide(at(75, 6).ticksLeft(30)
			.urnsLeft(5).nextUrn(PyramidPlunder.UrnState.CLOSED).build()));
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
		GuideStep without = PyramidPlunder.step(at(60, 3).build(), PyramidPlunder.Action.PICK_DOOR);
		assertTrue(String.join(" ", without.getDetails()).contains("bring a lockpick"));
	}

	@Test
	public void roundPlanGroupsEarlyRooms()
	{
		List<String> plan = PyramidPlunder.roundPlan(6, 5, false);
		assertEquals(List.of(
			"✔ Rooms 1-3: trap, then door.",
			"✔ Room 4: trap, then chest, then door.",
			"➜ Room 5: trap, then every urn, then chest, then door. Keep 90 seconds for room 6.",
			"• Room 6: trap, then every urn, then chest, then leave."), plan);
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
