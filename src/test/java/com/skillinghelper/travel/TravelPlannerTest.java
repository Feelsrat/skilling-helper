package com.skillinghelper.travel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TravelPlannerTest
{
	private static final WorldPoint GE = new WorldPoint(3165, 3487, 0);
	private static final WorldPoint FISHING_GUILD = new WorldPoint(2604, 3410, 0);
	private static final WorldPoint MORT_MYRE = new WorldPoint(3472, 3436, 0);
	private static final WorldPoint CHAOS_TEMPLE = new WorldPoint(3236, 3636, 0);
	private static final int SKILLS_NECKLACE_4 = 11105;
	private static final int BURNING_AMULET_5 = 21166;

	private static final List<TravelOption> ALL = TravelData.all();
	private static final TravelUnlocks EVERYTHING = TravelUnlocks.everything(ALL);
	private static final IntPredicate NONE = id -> false;

	private static IntPredicate has(Integer... ids)
	{
		Set<Integer> set = Set.of(ids);
		return set::contains;
	}

	private static TravelPick pick(IntPredicate carried, IntPredicate bank, WorldPoint from, WorldPoint to,
		boolean wild)
	{
		return TravelPlanner.pick(ALL, EVERYTHING, carried, bank, from, List.of(to), "there", wild);
	}

	@Test
	public void dataLoads()
	{
		assertTrue(ALL.size() > 400);
		assertTrue(ALL.stream().anyMatch(o -> o.getKind() == TravelOption.Kind.FAIRY_RING));
	}

	@Test
	public void everyQuestNameIsAQuest()
	{
		Map<String, Quest> byName = new HashMap<>();
		for (Quest q : Quest.values())
		{
			byName.put(q.getName(), q);
		}
		for (TravelOption o : ALL)
		{
			for (String q : o.getQuests())
			{
				assertTrue("Unknown quest " + q, byName.containsKey(q));
			}
		}
	}

	@Test
	public void suggestsTheNecklaceInTheBank()
	{
		TravelPick p = pick(NONE, has(SKILLS_NECKLACE_4), GE, FISHING_GUILD, false);
		assertNotNull(p);
		assertEquals("Skills necklace", p.itemName());
		assertEquals(SKILLS_NECKLACE_4, p.getItemId());
		assertFalse(p.isCarried());
		assertTrue(p.label(), p.label().contains("Fishing Guild"));
	}

	@Test
	public void nothingOwnedNothingSuggested()
	{
		assertNull(pick(NONE, NONE, GE, FISHING_GUILD, false));
	}

	@Test
	public void nothingWhenItsAShortWalk()
	{
		WorldPoint near = new WorldPoint(GE.getX() + 20, GE.getY(), 0);
		assertNull(pick(NONE, has(ItemID.POH_TABLET_VARROCKTELEPORT), GE, near, false));
	}

	@Test
	public void fairyRingStaff()
	{
		TravelPick p = pick(NONE, has(ItemID.DRAMEN_STAFF), GE, MORT_MYRE, false);
		assertNotNull(p);
		assertEquals(TravelOption.Kind.FAIRY_RING, p.getOption().getKind());
		assertEquals("Dramen staff", p.itemName());
		assertTrue(p.label(), p.label().contains("B-K-R"));
	}

	@Test
	public void noStaffNeededAfterTheEliteDiary()
	{
		TravelUnlocks diary = TravelUnlocks.read(ALL, new Unlocked(), true, false);
		assertNull(TravelPlanner.pick(ALL, diary, NONE, has(ItemID.DRAMEN_STAFF), GE, List.of(MORT_MYRE), "there",
			false));
	}

	@Test
	public void fairyRingsNeedTheQuest()
	{
		TravelUnlocks noRings = TravelUnlocks.read(ALL, new Unlocked(), false, true);
		assertNull(TravelPlanner.pick(ALL, noRings, NONE, has(ItemID.DRAMEN_STAFF), GE, List.of(MORT_MYRE), "there",
			false));
	}

	@Test
	public void wildernessTeleportsOnlyWhenAllowed()
	{
		TravelPick off = pick(NONE, has(BURNING_AMULET_5), GE, CHAOS_TEMPLE, false);
		assertTrue(off == null || !"Burning amulet".equals(off.itemName()));
		TravelPick on = pick(NONE, has(BURNING_AMULET_5), GE, CHAOS_TEMPLE, true);
		assertNotNull(on);
		assertEquals("Burning amulet", on.itemName());
	}

	@Test
	public void carriedItemWinsACloseRace()
	{
		// Owned twice, carried and banked: the carried one is suggested, so nothing needs taking out
		TravelPick p = pick(has(SKILLS_NECKLACE_4), has(SKILLS_NECKLACE_4), GE, FISHING_GUILD, false);
		assertNotNull(p);
		assertTrue(p.isCarried());
	}

	@Test
	public void lockedOptionsAreSkipped()
	{
		// No quests and no diaries: the skills necklace needs neither, so it still works
		TravelUnlocks none = TravelUnlocks.read(ALL, new Unlocked()
		{
			@Override
			public int varbit(int id)
			{
				return -1;
			}

			@Override
			public boolean questDone(String name)
			{
				return false;
			}
		}, false, true);
		TravelPick p = TravelPlanner.pick(ALL, none, NONE, has(SKILLS_NECKLACE_4), GE, List.of(FISHING_GUILD), "there",
			false);
		assertNotNull(p);
		// Every option behind a quest is locked
		for (int i = 0; i < ALL.size(); i++)
		{
			if (!ALL.get(i).getQuests().isEmpty())
			{
				assertFalse(none.usable(i));
			}
		}
	}

	@Test
	public void dungeonsCountFromAbove()
	{
		WorldPoint mine = new WorldPoint(3049, 9763, 0);
		WorldPoint above = new WorldPoint(3049, 3363, 0);
		assertEquals(0, TravelPlanner.distance(mine, above));
	}

	private static class Unlocked implements TravelUnlocks.Source
	{
		@Override
		public boolean questDone(String name)
		{
			return true;
		}

		@Override
		public int level(Skill skill)
		{
			return 99;
		}

		@Override
		public int varbit(int id)
		{
			return 0;
		}

		@Override
		public int varp(int id)
		{
			return 0;
		}
	}
}
