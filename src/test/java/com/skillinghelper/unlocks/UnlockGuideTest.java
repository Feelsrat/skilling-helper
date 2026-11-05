package com.skillinghelper.unlocks;

import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.diary.data.Diaries;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.unlocks.data.Unlock;
import com.skillinghelper.unlocks.data.Unlocks;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class UnlockGuideTest
{
	private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);

	private static DiaryPlayer.DiaryPlayerBuilder player(int level)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, level);
		}
		return DiaryPlayer.builder()
			.location(LUMBRIDGE)
			.levels(levels)
			.combatLevel(level)
			.members(true)
			.bank(new HashMap<>());
	}

	private static UnlockPlayer up(DiaryPlayer p)
	{
		return UnlockPlayer.builder().player(p).questHelper(true).diaryGuides(true).skillingHelper(true).build();
	}

	@Test
	public void everyUnlockIsWellFormed()
	{
		Set<String> ids = new HashSet<>();
		for (Unlock u : Unlocks.all())
		{
			assertTrue("duplicate id " + u.getId(), ids.add(u.getId()));
			assertNotNull(u.getName());
			assertNotNull(u.getCategory());
			assertNotNull(u.getId() + " wiki", u.getWikiPage());
			boolean detectable = !u.getOwn().isEmpty() || u.getDoneVarbit() > 0 || u.isDiary() || u.getDoneQuest() != null;
			assertTrue(u.getId() + " has no way to tell it's unlocked", detectable);
			assertFalse(u.getId() + " says nothing about what it gives", UnlockGuide.gives(u).isEmpty());
			boolean way = !u.getQuests().isEmpty() || u.isDiary() || u.getGrind() != null || u.getGuide() != null;
			assertTrue(u.getId() + " has no way to get it", way);
			if (u.getPieceCosts() != null)
			{
				assertEquals(u.getOwn().size(), u.getPieceCosts().length);
			}
			if (u.isDiary())
			{
				assertNotNull(u.getId() + " reward", Diaries.reward(u.getDiaryRegion(), u.getDiaryTier()));
			}
			if (u.getGuide() != null)
			{
				assertFalse(u.getGuide().getStages().isEmpty());
				assertFalse(u.getGuide().isWilderness());
				for (DiaryItem item : u.getGuide().getItems())
				{
					assertTrue(u.getId() + " " + item.getName() + " can't be got", item.getBuyId() > 0 || item.getHowToGet() != null);
				}
			}
		}
	}

	@Test
	public void bankNotSeenAsksToOpenItFirst()
	{
		UnlockPlayer p = up(player(50).bank(null).build());
		UnlockStep step = UnlockGuide.resolve(Unlocks.all(), p, Set.of(), null);
		assertEquals("graceful", step.getUnlockId());
		assertEquals(DiaryStep.Kind.OPEN_BANK, step.getStep().getKind());
	}

	@Test
	public void gracefulCountsMarksForMissingPiecesOnly()
	{
		Unlock graceful = Unlocks.byId("graceful");
		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(ItemID.GRACE, 50);
		bank.put(ItemID.GRACEFUL_HOOD, 1);
		bank.put(ItemID.ZEAH_GRACEFUL_LEGS_ARCEUUS, 1);
		DiaryPlayer p = player(50).bank(bank).build();
		// 260 minus hood (35) and legs (60), minus the 50 marks owned
		assertEquals(115, UnlockGuide.stillToEarn(graceful, p));
		UnlockStep step = UnlockGuide.resolve(graceful, up(p));
		assertEquals(UnlockStep.Action.TRAIN, step.getAction());
		assertTrue(step.getStep().getTitle().contains("115"));

		bank.put(ItemID.GRACE, 165);
		step = UnlockGuide.resolve(graceful, up(player(50).bank(bank).build()));
		// Enough marks: take them out of the bank to go to Grace
		assertEquals(DiaryStep.Kind.OPEN_BANK, step.getStep().getKind());
		assertEquals(Integer.valueOf(165), step.getStep().getWithdraw().get(ItemID.GRACE));
	}

	@Test
	public void lowAgilityLocksGraceful()
	{
		UnlockPlayer p = up(player(1).build());
		assertEquals(UnlockGuide.State.LOCKED, UnlockGuide.state(Unlocks.byId("graceful"), p));
		// The guide moves on to the house (no level needed)
		assertEquals("house", UnlockGuide.resolve(Unlocks.all(), p, Set.of(), null).getUnlockId());
	}

	@Test
	public void fairyRingsGoQuestByQuestThenStopAtTheVarbit()
	{
		Unlock rings = Unlocks.byId("fairy_rings");
		Map<Quest, QuestState> quests = new HashMap<>();
		UnlockStep step = UnlockGuide.resolve(rings, up(player(50).quests(quests).build()));
		assertEquals(UnlockStep.Action.QUEST, step.getAction());
		assertTrue(step.getStep().getTitle().contains("Lost City"));

		quests.put(Quest.LOST_CITY, QuestState.FINISHED);
		quests.put(Quest.FAIRYTALE_I__GROWING_PAINS, QuestState.FINISHED);
		step = UnlockGuide.resolve(rings, up(player(50).quests(quests).build()));
		assertTrue(step.getStep().getTitle().startsWith("Start the quest"));

		DiaryPlayer far = player(50).quests(quests).varbit(id -> id == VarbitID.FAIRY2_QUEENCURE_QUEST ? 40 : 0).build();
		assertTrue(UnlockGuide.done(rings, up(far)));
	}

	@Test
	public void questHelperMissingIsFlagged()
	{
		UnlockPlayer p = UnlockPlayer.builder().player(player(50).build()).build();
		UnlockStep step = UnlockGuide.resolve(Unlocks.byId("dramen_staff"), p);
		assertTrue(String.join(" ", step.getStep().getLines()).contains("Quest Helper plugin isn't installed"));
	}

	@Test
	public void diaryRewardHandsOffToTheDiaryGuide()
	{
		Unlock cloak = Unlocks.byId("ardougne_cloak");
		UnlockPlayer p = up(player(50).build()).toBuilder().diaryProgress(Map.of("ARDOUGNE.EASY", new int[]{3, 10})).build();
		UnlockStep step = UnlockGuide.resolve(cloak, p);
		assertEquals(UnlockStep.Action.DIARY, step.getAction());
		assertTrue(String.join(" ", step.getStep().getLines()).contains("3 of 10"));
		UnlockPlayer claimed = up(player(50).claimed(Set.of("ARDOUGNE.EASY")).build());
		assertTrue(UnlockGuide.done(cloak, claimed));
	}

	@Test
	public void slayerPointsFromTheVarbit()
	{
		Unlock pouch = Unlocks.byId("rune_pouch");
		DiaryPlayer p = player(50).varbit(id -> id == VarbitID.SLAYER_POINTS ? 800 : 0).build();
		UnlockStep step = UnlockGuide.resolve(pouch, up(p));
		assertEquals(DiaryStep.Kind.TRAVEL, step.getStep().getKind());
		assertEquals("rune_pouch", step.getStep().getTaskKey());
		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(ItemID.DIVINE_RUNE_POUCH, 1);
		assertTrue(UnlockGuide.done(pouch, up(player(50).bank(bank).build())));
	}

	@Test
	public void everythingDoneSaysSo()
	{
		Map<Integer, Integer> bank = new HashMap<>();
		Map<Quest, QuestState> quests = new HashMap<>();
		Set<String> claimed = new HashSet<>();
		for (Unlock u : Unlocks.all())
		{
			u.getOwn().forEach(ids -> bank.put(ids[0], 1));
			if (u.getDoneQuest() != null)
			{
				quests.put(u.getDoneQuest(), QuestState.FINISHED);
			}
			if (u.isDiary())
			{
				claimed.add(u.getDiaryRegion().name() + "." + u.getDiaryTier().name());
			}
		}
		DiaryPlayer p = player(99).bank(bank).quests(quests).claimed(claimed).varbit(id -> 100).build();
		UnlockStep step = UnlockGuide.resolve(Unlocks.all(), up(p), Set.of(), null);
		assertEquals("Every unlock on the list is done", step.getStep().getTitle());
	}

	@Test
	public void sameStateGivesAnEqualStep()
	{
		UnlockPlayer a = up(player(50).build());
		UnlockPlayer b = up(player(50).build());
		assertEquals(UnlockGuide.resolve(Unlocks.all(), a, Set.of(), null),
			UnlockGuide.resolve(Unlocks.all(), b, Set.of(), null));
	}
}
