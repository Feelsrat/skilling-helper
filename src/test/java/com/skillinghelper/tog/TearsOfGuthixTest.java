package com.skillinghelper.tog;

import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.StepType;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
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

public class TearsOfGuthixTest
{
	private static final long DAY = 24 * 60 * 60;
	private static final long T0 = 1_800_000_000L;

	@Test
	public void lockedWithoutQuest()
	{
		assertEquals(TearsOfGuthix.State.LOCKED,
			TearsOfGuthix.status(false, TearsOfGuthix.Record.EMPTY, T0, 0, 0).getState());
	}

	@Test
	public void unknownBeforeAnyVisit()
	{
		assertEquals(TearsOfGuthix.State.UNKNOWN,
			TearsOfGuthix.status(true, TearsOfGuthix.Record.EMPTY, T0, 0, 50).getState());
	}

	@Test
	public void needsSevenDaysAndXp()
	{
		TearsOfGuthix.Record r = TearsOfGuthix.visited(T0, 5_000_000, 100);

		TearsOfGuthix.Status s = TearsOfGuthix.status(true, r, T0 + DAY, 5_000_000, 100);
		assertEquals(TearsOfGuthix.State.WAIT, s.getState());
		assertEquals(T0 + 7 * DAY, s.getReadyAt());
		assertEquals(100_000, s.getXpToGo());

		// A week later but not enough xp
		s = TearsOfGuthix.status(true, r, T0 + 8 * DAY, 5_060_000, 100);
		assertEquals(TearsOfGuthix.State.WAIT, s.getState());
		assertEquals(0, s.getReadyAt());
		assertEquals(40_000, s.getXpToGo());

		assertTrue(TearsOfGuthix.status(true, r, T0 + 8 * DAY, 5_100_000, 100).isReady());
		// A quest point counts instead of the xp
		assertTrue(TearsOfGuthix.status(true, r, T0 + 8 * DAY, 5_000_000, 101).isReady());
		// But not instead of the week
		assertFalse(TearsOfGuthix.status(true, r, T0 + 6 * DAY, 6_000_000, 105).isReady());
	}

	@Test
	public void junaSaysDays()
	{
		TearsOfGuthix.Record r = TearsOfGuthix.heard(TearsOfGuthix.Record.EMPTY,
			"It has not been long since your last visit. Come back in 3 days.", T0, 1_000_000, 80);
		TearsOfGuthix.Status s = TearsOfGuthix.status(true, r, T0, 1_000_000, 80);
		assertEquals(TearsOfGuthix.State.WAIT, s.getState());
		assertEquals(T0 + 3 * DAY, s.getReadyAt());
		assertTrue(s.isEstimated());
		assertEquals(0, s.getXpToGo());

		r = TearsOfGuthix.heard(TearsOfGuthix.Record.EMPTY,
			"It has not been long since your last visit. Come back<br>tomorrow.", T0, 1_000_000, 80);
		assertEquals(T0 + DAY, r.getHintReadyAt());
	}

	@Test
	public void junaSaysXp()
	{
		TearsOfGuthix.Record r = TearsOfGuthix.heard(TearsOfGuthix.Record.EMPTY,
			"You cannot enter the cave again until you have gained either one quest point or 12,345 total XP.",
			T0, 2_000_000, 90);
		TearsOfGuthix.Status s = TearsOfGuthix.status(true, r, T0, 2_000_000, 90);
		assertEquals(TearsOfGuthix.State.WAIT, s.getState());
		assertEquals(12_345, s.getXpToGo());
		assertEquals(0, s.getReadyAt());
		assertTrue(TearsOfGuthix.status(true, r, T0, 2_012_345, 90).isReady());
		assertTrue(TearsOfGuthix.status(true, r, T0, 2_000_000, 91).isReady());
	}

	@Test
	public void unrelatedDialogueChangesNothing()
	{
		TearsOfGuthix.Record r = TearsOfGuthix.visited(T0, 1, 2);
		assertTrue(r == TearsOfGuthix.heard(r, "Guthix reclaimed that which had been stolen from Him.", T0, 1, 2));
	}

	@Test
	public void newVisitClearsHints()
	{
		TearsOfGuthix.Record r = TearsOfGuthix.heard(TearsOfGuthix.Record.EMPTY, "Come back in 2 days.", T0, 0, 0);
		assertTrue(r.getHintReadyAt() > 0);
		assertEquals(0, TearsOfGuthix.visited(T0, 0, 0).getHintReadyAt());
	}

	@Test
	public void recordRoundTrips()
	{
		TearsOfGuthix.Record r = TearsOfGuthix.Record.builder().visitedAt(T0).xpAtVisit(123).qpAtVisit(45)
			.hintReadyAt(T0 + 5).hintXpTarget(999).hintQp(44).build();
		assertEquals(r, TearsOfGuthix.Record.parse(r.serialize()));
		assertEquals(TearsOfGuthix.Record.EMPTY, TearsOfGuthix.Record.parse("garbage"));
		assertEquals(TearsOfGuthix.Record.EMPTY, TearsOfGuthix.Record.parse(null));
	}

	@Test
	public void rewardGoesToLowestEligibleSkill()
	{
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			xp.put(s, 500_000);
		}
		xp.put(Skill.RUNECRAFT, 0);
		xp.put(Skill.CONSTRUCTION, 0);
		xp.put(Skill.AGILITY, 20_000);
		xp.put(Skill.THIEVING, 20_000);
		Set<Quest> done = EnumSet.noneOf(Quest.class);
		// Runecraft needs Rune Mysteries and Construction a house, so Agility wins the tie with Thieving
		assertEquals(Skill.AGILITY, TearsOfGuthix.rewardSkill(xp, done));
		done.add(Quest.RUNE_MYSTERIES);
		assertEquals(Skill.RUNECRAFT, TearsOfGuthix.rewardSkill(xp, done));
	}

	@Test
	public void xpPerTear()
	{
		assertEquals(10, TearsOfGuthix.xpPerTear(0), 0.001);
		assertEquals(60, TearsOfGuthix.xpPerTear(13_500), 0.001);
		assertEquals(39, TearsOfGuthix.goodTears(43));
		assertEquals(171, TearsOfGuthix.goodTears(200));
	}

	@Test
	public void timeLeft()
	{
		assertEquals("3d 4h", TearsOfGuthix.timeLeft(3 * DAY + 4 * 3600 + 10));
		assertEquals("5h 20m", TearsOfGuthix.timeLeft(5 * 3600 + 20 * 60));
		assertEquals("1m", TearsOfGuthix.timeLeft(5));
	}

	@Test
	public void steps()
	{
		TearsOfGuthix.Status ready = new TearsOfGuthix.Status(TearsOfGuthix.State.READY, 0, 0, false);
		PlayerState far = PlayerState.builder().location(new WorldPoint(2440, 3090, 0)).build();

		GuideStep walk = TearsOfGuthix.step(sit(ready).build(), far);
		assertEquals(StepType.WALK, walk.getType());
		assertNotNull(walk.getRoute());

		PlayerState withNecklace = far.toBuilder()
			.inventory(ItemCounts.of(ItemID.NECKLACE_OF_MINIGAMES_5, 1)).build();
		GuideStep tele = TearsOfGuthix.step(sit(ready).build(), withNecklace);
		assertEquals("Teleport to Tears of Guthix", tele.getTitle());
		assertNull(tele.getRoute());

		PlayerState banked = far.toBuilder().bank(ItemCounts.of(ItemID.NECKLACE_OF_MINIGAMES_2, 3)).build();
		assertEquals(StepType.BANK, TearsOfGuthix.step(sit(ready).build(), banked).getType());

		GuideStep juna = TearsOfGuthix.step(sit(ready).inChasm(true).build(), far);
		assertEquals("Tell Juna a story", juna.getTitle());
		assertFalse(juna.getHighlights().isEmpty());

		GuideStep play = TearsOfGuthix.step(sit(ready).inChasm(true).playing(true).tears(12).build(), far);
		assertEquals(StepType.TRAIN, play.getType());
		assertTrue(play.getDetails().get(0).contains("12"));

		TearsOfGuthix.Status wait = new TearsOfGuthix.Status(TearsOfGuthix.State.WAIT, T0 + 2 * DAY, 0, false);
		GuideStep w = TearsOfGuthix.step(sit(wait).build(), far);
		assertEquals(StepType.WAIT, w.getType());
		assertTrue(w.getTitle(), w.getTitle().startsWith("Not up yet: in 2d"));

		assertEquals(StepType.DONE, TearsOfGuthix.step(sit(TearsOfGuthix.Status.LOCKED_STATUS).build(), far)
			.getType());
	}

	private static TearsOfGuthix.Situation.SituationBuilder sit(TearsOfGuthix.Status s)
	{
		return TearsOfGuthix.Situation.builder().status(s).now(T0).reward("~2,340 xp in Agility (39 tears)");
	}
}
