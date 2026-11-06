package com.skillinghelper.diary;

import com.skillinghelper.diary.data.Diaries;
import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.diary.data.DiaryTier;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DiaryGuideTest
{
	private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);
	private static final WorldPoint OAK = new WorldPoint(3219, 3206, 0);

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

	private static DiaryTask task(DiaryTier tier, int number)
	{
		for (DiaryTask t : Diaries.tasks(DiaryRegion.LUMBRIDGE, tier))
		{
			if (t.getNumber() == number)
			{
				return t;
			}
		}
		throw new AssertionError();
	}

	private static Set<String> keys(DiaryTier tier)
	{
		Set<String> out = new HashSet<>();
		Diaries.tasks(DiaryRegion.LUMBRIDGE, tier).forEach(t -> out.add(t.key()));
		return out;
	}

	@Test
	public void freshPlayerStartsWithHans()
	{
		DiaryStep step = DiaryGuide.resolve(DiaryRegion.LUMBRIDGE, DiaryTier.EASY, player(1).build(), Set.of(), null);
		assertEquals(task(DiaryTier.EASY, 5).key(), step.getTaskKey());
		assertEquals(DiaryStep.Kind.DO, step.getKind());
		assertTrue(step.getNpcs().contains("Hans"));
	}

	@Test
	public void lockedTasksAreSkippedInWalkingOrder()
	{
		Set<String> done = new HashSet<>();
		for (DiaryTask t : Diaries.tasks(DiaryRegion.LUMBRIDGE, DiaryTier.EASY))
		{
			if (t.getNumber() != 2)
			{
				done.add(t.key());
			}
		}
		// Only the cave bug is left and it needs 7 Slayer
		DiaryPlayer p = player(1).done(done).build();
		DiaryStep step = DiaryGuide.resolve(DiaryRegion.LUMBRIDGE, DiaryTier.EASY, p, Set.of(), null);
		assertEquals(DiaryStep.Kind.LOCKED, step.getKind());
		assertTrue(String.join("|", step.getLines()), String.join("|", step.getLines()).contains("7 Slayer"));
		// Picked by hand it still says what's missing
		DiaryStep picked = DiaryGuide.resolve(DiaryRegion.LUMBRIDGE, DiaryTier.EASY, p, Set.of(), task(DiaryTier.EASY, 2).key());
		assertEquals(DiaryStep.Kind.LOCKED, picked.getKind());
		assertEquals(task(DiaryTier.EASY, 2).key(), picked.getTaskKey());
	}

	@Test
	public void oakTaskGetsAnAxeThenChopsThenBurns()
	{
		DiaryTask oak = task(DiaryTier.EASY, 7);
		DiaryPlayer.DiaryPlayerBuilder b = player(25);

		DiaryStep buy = DiaryGuide.resolve(oak, b.build());
		assertEquals(DiaryStep.Kind.BUY, buy.getKind());
		assertTrue(buy.getWithdraw().containsKey(ItemID.TINDERBOX));
		assertTrue("best axe a level 25 can use: mithril", buy.getWithdraw().containsKey(ItemID.MITHRIL_AXE));

		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(ItemID.RUNE_AXE, 1);
		bank.put(ItemID.STEEL_AXE, 1);
		bank.put(ItemID.TINDERBOX, 1);
		DiaryStep open = DiaryGuide.resolve(oak, b.bank(bank).build());
		assertEquals(DiaryStep.Kind.OPEN_BANK, open.getKind());
		assertTrue("rune axe needs 41, so the steel one", open.getWithdraw().containsKey(ItemID.STEEL_AXE));
		assertFalse(open.getWithdraw().containsKey(ItemID.RUNE_AXE));

		DiaryStep withdraw = DiaryGuide.resolve(oak, b.bank(bank).bankOpen(true).build());
		assertEquals(DiaryStep.Kind.WITHDRAW, withdraw.getKind());

		Map<Integer, Integer> carried = new HashMap<>();
		carried.put(ItemID.STEEL_AXE, 1);
		carried.put(ItemID.TINDERBOX, 1);
		DiaryStep walk = DiaryGuide.resolve(oak, b.bankOpen(false).carried(carried).build());
		assertEquals(DiaryStep.Kind.TRAVEL, walk.getKind());
		assertNotNull(walk.getDestination());

		DiaryStep chop = DiaryGuide.resolve(oak, b.location(OAK).build());
		assertEquals(DiaryStep.Kind.DO, chop.getKind());
		assertTrue(chop.getObjects().contains("Oak tree"));

		carried.put(ItemID.OAK_LOGS, 1);
		DiaryStep burn = DiaryGuide.resolve(oak, b.location(LUMBRIDGE).carried(carried).build());
		assertEquals(DiaryStep.Kind.DO, burn.getKind());
		assertTrue(burn.getUse().contains(ItemID.OAK_LOGS));
	}

	@Test
	public void ropeIsOnlyNeededOnce()
	{
		DiaryTask bug = task(DiaryTier.EASY, 2);
		Map<Integer, Integer> carried = new HashMap<>();
		carried.put(ItemID.LIT_CANDLE, 1);
		carried.put(ItemID.TINDERBOX, 1);
		DiaryPlayer.DiaryPlayerBuilder b = player(10).carried(carried);
		assertTrue(DiaryGuide.resolve(bug, b.build()).getWithdraw().containsKey(ItemID.ROPE));
		DiaryStep roped = DiaryGuide.resolve(bug, b.varbit(id -> id == VarbitID.SWAMP_CAVES_ROPED_ENTRANCE ? 1 : 0).build());
		assertEquals(DiaryStep.Kind.TRAVEL, roped.getKind());
		assertEquals("Climb down the dark hole", roped.getLines().get(roped.getLines().size() - 1).substring(6));
	}

	@Test
	public void cantBuyAQuestCape()
	{
		DiaryStep step = DiaryGuide.resolve(task(DiaryTier.ELITE, 6), player(99).build());
		assertEquals(DiaryStep.Kind.GET_ITEM, step.getKind());
	}

	@Test
	public void questsAreChecked()
	{
		DiaryTask bread = task(DiaryTier.EASY, 10);
		assertFalse(DiaryGuide.missing(bread, player(1).build()).isEmpty());
		Map<Quest, QuestState> quests = Map.of(Quest.COOKS_ASSISTANT, QuestState.FINISHED);
		assertTrue(DiaryGuide.missing(bread, player(1).quests(quests).build()).isEmpty());
	}

	@Test
	public void claimsTheRewardWhenEveryTaskIsDone()
	{
		DiaryPlayer p = player(1).done(keys(DiaryTier.EASY)).build();
		DiaryStep step = DiaryGuide.resolve(DiaryRegion.LUMBRIDGE, DiaryTier.EASY, p, Set.of(), null);
		assertEquals(DiaryStep.Kind.CLAIM, step.getKind());
		assertTrue(step.getNpcs().contains("Mikasi"));
		assertNull(step.getTaskKey());

		DiaryPlayer claimed = player(1).done(keys(DiaryTier.EASY)).claimed(Set.of("LUMBRIDGE.EASY")).build();
		assertEquals(DiaryStep.Kind.DONE,
			DiaryGuide.resolve(DiaryRegion.LUMBRIDGE, DiaryTier.EASY, claimed, Set.of(), null).getKind());
	}

	@Test
	public void claimingMediumAsksForEasyFirst()
	{
		DiaryPlayer p = player(1).done(keys(DiaryTier.MEDIUM)).build();
		DiaryStep step = DiaryGuide.resolve(DiaryRegion.LUMBRIDGE, DiaryTier.MEDIUM, p, Set.of(), null);
		assertEquals(DiaryStep.Kind.CLAIM, step.getKind());
		assertTrue(String.join("|", step.getLines()).contains("Easy"));
	}

	@Test
	public void everyTaskResolvesForAMaxedPlayer()
	{
		Map<Quest, QuestState> quests = new HashMap<>();
		for (Quest q : Quest.values())
		{
			quests.put(q, QuestState.FINISHED);
		}
		for (DiaryTask t : Diaries.all())
		{
			DiaryStep step = DiaryGuide.resolve(t, player(99).combatLevel(126).allowWilderness(true).quests(quests)
				.varp(id -> 1000).varbit(id -> id == VarbitID.VM_KUDOS ? 1000 : 0).build());
			assertNotNull(t.key(), step);
			assertNotNull(t.key(), step.getTitle());
			assertTrue(t.key() + " " + step.getKind(), step.getKind() != DiaryStep.Kind.LOCKED);
		}
	}

	@Test
	public void costCountsFeesAndWhatStillHasToBeBought()
	{
		DiaryTask planks = null;
		for (DiaryTask t : Diaries.tasks(DiaryRegion.VARROCK, DiaryTier.MEDIUM))
		{
			if (t.getNumber() == 10)
			{
				planks = t;
			}
		}
		assertNotNull(planks);
		DiaryPlayer.DiaryPlayerBuilder b = player(99).buyPrice(id -> id == ItemID.MAHOGANY_LOGS ? 1000 : 0);
		assertEquals(30_000 + 20 * 1000, DiaryGuide.cost(planks, b.build()));

		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(ItemID.MAHOGANY_LOGS, 15);
		bank.put(ItemID.COINS, 1_000_000);
		assertEquals(30_000 + 5 * 1000, DiaryGuide.cost(planks, b.bank(bank).build()));
	}

	@Test
	public void oneBankTripAlsoGrabsTheNextTasksItems()
	{
		DiaryTask bucket = null;
		DiaryTask oak = null;
		for (DiaryTask t : Diaries.tasks(DiaryRegion.FREMENNIK, DiaryTier.EASY))
		{
			if (t.getNumber() == 8)
			{
				bucket = t;
			}
			if (t.getNumber() == 10)
			{
				oak = t;
			}
		}
		assertNotNull(bucket);
		assertNotNull(oak);
		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(ItemID.BUCKET_EMPTY, 1);
		bank.put(ItemID.TINDERBOX, 1);
		bank.put(ItemID.RUNE_AXE, 1);
		DiaryPlayer p = player(99).bank(bank).bankOpen(true).build();

		DiaryStep step = DiaryGuide.resolve(bucket, p, java.util.List.of(oak));
		assertEquals(DiaryStep.Kind.WITHDRAW, step.getKind());
		assertEquals(Integer.valueOf(1), step.getWithdraw().get(ItemID.BUCKET_EMPTY));
		assertEquals(Integer.valueOf(1), step.getWithdraw().get(ItemID.TINDERBOX));

		// Carrying the bucket already: the open bank still offers the oak task's things
		Map<Integer, Integer> carried = new HashMap<>();
		carried.put(ItemID.BUCKET_EMPTY, 1);
		DiaryStep extras = DiaryGuide.resolve(bucket, p.toBuilder().carried(carried).build(), java.util.List.of(oak));
		assertEquals(DiaryStep.Kind.WITHDRAW, extras.getKind());
		assertTrue(extras.getWithdraw().containsKey(ItemID.TINDERBOX));

		// No room: the extras are left out
		DiaryStep full = DiaryGuide.resolve(bucket, p.toBuilder().freeSlots(1).build(), java.util.List.of(oak));
		assertFalse(full.getWithdraw().containsKey(ItemID.TINDERBOX));
	}
}
