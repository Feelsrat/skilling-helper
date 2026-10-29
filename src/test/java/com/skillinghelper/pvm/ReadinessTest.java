package com.skillinghelper.pvm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.PvmLadder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class ReadinessTest
{
	private static PvmContent content(String id)
	{
		return PvmLadder.ALL.stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow();
	}

	private static PlayerView.PlayerViewBuilder allAt(int level)
	{
		PlayerView.PlayerViewBuilder b = PlayerView.builder();
		for (Skill s : Skill.values())
		{
			b.level(s, level);
		}
		return b;
	}

	@Test
	public void ladderIdsAndKillCountNamesAreUnique()
	{
		Set<String> ids = new HashSet<>();
		Set<String> kcNames = new HashSet<>();
		for (PvmContent c : PvmLadder.ALL)
		{
			assertTrue("duplicate id " + c.getId(), ids.add(c.getId()));
			assertNotNull(c.getName());
			assertNotNull(c.getWikiPage());
			assertNotNull(c.getTravel());
			for (String n : c.getKcNames())
			{
				assertTrue("duplicate kill count name " + n, kcNames.add(n.toLowerCase()));
			}
		}
	}

	@Test
	public void ladderIsInTierOrder()
	{
		for (int i = 1; i < PvmLadder.ALL.size(); i++)
		{
			assertTrue(PvmLadder.ALL.get(i - 1).getId() + " is after " + PvmLadder.ALL.get(i).getId(),
				PvmLadder.ALL.get(i - 1).getTier().compareTo(PvmLadder.ALL.get(i).getTier()) <= 0);
		}
	}

	@Test
	public void missingQuestLocks()
	{
		Readiness r = Readiness.of(content("barrows"), allAt(99).build());
		assertEquals(Readiness.Status.LOCKED, r.getStatus());
		assertEquals(Quest.PRIEST_IN_PERIL, r.getMissingQuests().get(0).getQuest());

		r = Readiness.of(content("barrows"), allAt(99).quest(Quest.PRIEST_IN_PERIL, QuestState.FINISHED)
			.owned(ItemID.SPADE).build());
		assertEquals(Readiness.Status.READY, r.getStatus());
	}

	@Test
	public void startedIsEnoughForZulrah()
	{
		Readiness r = Readiness.of(content("zulrah"), allAt(99).quest(Quest.REGICIDE, QuestState.IN_PROGRESS).build());
		assertEquals(Readiness.Status.READY, r.getStatus());
	}

	@Test
	public void levelGapsDecideAlmostOrTrain()
	{
		PvmContent mole = content("giant_mole");
		Set<Integer> gear = Set.of(ItemID.SPADE, ItemID.LIT_CANDLE);
		assertEquals(Readiness.Status.ALMOST, Readiness.of(mole, allAt(56).owned(gear).build()).getStatus());
		Readiness train = Readiness.of(mole, allAt(50).owned(gear).build());
		assertEquals(Readiness.Status.TRAIN, train.getStatus());
		assertEquals(4, train.getLevelGaps().size());
	}

	@Test
	public void missingRequiredItemIsAlmost()
	{
		Readiness r = Readiness.of(content("king_black_dragon"), allAt(99).build());
		assertEquals(Readiness.Status.ALMOST, r.getStatus());
		assertEquals(1, r.getMissingItems().size());
		r = Readiness.of(content("king_black_dragon"), allAt(99).owned(ItemID.DRAGONFIRE_WARD).build());
		assertEquals(Readiness.Status.READY, r.getStatus());
	}

	@Test
	public void killOrMarkMakesDoneAndNextMovesOn()
	{
		PlayerView p = allAt(30).killCount("obor", 1).markedDone("bryophyta").build();
		List<Readiness> ladder = PvmLadder.ALL.stream().map(c -> Readiness.of(c, p)).collect(Collectors.toList());
		assertEquals(Readiness.Status.DONE, ladder.get(0).getStatus());
		assertEquals("scurrius", Readiness.next(ladder).getContent().getId());

		PlayerView all = allAt(99).markedDone(PvmLadder.ALL.stream().map(PvmContent::getId)
			.collect(Collectors.toList())).build();
		assertNull(Readiness.next(PvmLadder.ALL.stream().map(c -> Readiness.of(c, all)).collect(Collectors.toList())));
	}
}
