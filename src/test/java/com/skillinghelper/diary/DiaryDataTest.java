package com.skillinghelper.diary;

import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.data.Diaries;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryReward;
import com.skillinghelper.diary.data.DiaryStage;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.diary.data.DiaryTier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.VarPlayerID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The diary data is complete and consistent: every task of a guided diary is there once, with its own done bit, a
 * guide, and items the guide can get.
 */
public class DiaryDataTest
{
	/**
	 * Tasks per tier, from the wiki.
	 */
	private static final Map<DiaryRegion, int[]> COUNTS = Map.ofEntries(
		Map.entry(DiaryRegion.LUMBRIDGE, new int[]{12, 12, 11, 6}),
		Map.entry(DiaryRegion.VARROCK, new int[]{14, 13, 10, 5}),
		Map.entry(DiaryRegion.FALADOR, new int[]{11, 14, 11, 6}),
		Map.entry(DiaryRegion.KARAMJA, new int[]{10, 19, 10, 5}),
		Map.entry(DiaryRegion.ARDOUGNE, new int[]{10, 12, 12, 8}),
		Map.entry(DiaryRegion.DESERT, new int[]{11, 12, 10, 6}),
		Map.entry(DiaryRegion.KANDARIN, new int[]{11, 14, 11, 7}),
		Map.entry(DiaryRegion.MORYTANIA, new int[]{11, 11, 10, 6}),
		Map.entry(DiaryRegion.FREMENNIK, new int[]{10, 9, 9, 6}),
		Map.entry(DiaryRegion.KOUREND, new int[]{12, 13, 10, 8}));

	@Test
	public void everyTaskOfAGuidedDiaryIsThere()
	{
		for (DiaryRegion r : DiaryRegion.values())
		{
			if (!Diaries.guided(r))
			{
				continue;
			}
			int[] counts = COUNTS.get(r);
			assertNotNull("add " + r + " to COUNTS", counts);
			for (DiaryTier t : DiaryTier.values())
			{
				Set<Integer> numbers = new HashSet<>();
				for (DiaryTask task : Diaries.tasks(r, t))
				{
					assertTrue(task.key() + " numbered twice", numbers.add(task.getNumber()));
				}
				assertEquals(r + " " + t, counts[t.ordinal()], numbers.size());
				for (int n = 1; n <= counts[t.ordinal()]; n++)
				{
					assertTrue(r + " " + t + " misses task " + n, numbers.contains(n));
				}
			}
		}
	}

	@Test
	public void everyTaskHasItsOwnDoneFlag()
	{
		Set<String> flags = new HashSet<>();
		for (DiaryTask t : Diaries.all())
		{
			String flag = t.getVarp() > 0 ? "varp " + t.getVarp() + " bit " + t.getBit() : "varbit " + t.getVarbit();
			assertTrue(t.key() + " has no done flag", t.getVarp() > 0 || t.getVarbit() > 0);
			assertTrue(t.key() + " shares " + flag, flags.add(flag));
			assertTrue(t.key(), t.getBit() >= 0 && t.getBit() < 32);
		}
	}

	@Test
	public void lumbridgeBitsMatchTheGame()
	{
		assertFlag(DiaryTier.EASY, 1, VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 1);
		assertFlag(DiaryTier.EASY, 12, VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 12);
		assertFlag(DiaryTier.MEDIUM, 1, VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 13);
		assertFlag(DiaryTier.HARD, 7, VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 31);
		assertFlag(DiaryTier.HARD, 8, VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 0);
		assertFlag(DiaryTier.ELITE, 1, VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 4);
		assertFlag(DiaryTier.ELITE, 6, VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 9);
	}

	@Test
	public void varrockDoneBitsSkipTheUnusedOne()
	{
		assertFlag(DiaryRegion.VARROCK, DiaryTier.EASY, 1, VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 1);
		assertFlag(DiaryRegion.VARROCK, DiaryTier.MEDIUM, 2, VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 16);
		assertFlag(DiaryRegion.VARROCK, DiaryTier.MEDIUM, 3, VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 18);
		assertFlag(DiaryRegion.VARROCK, DiaryTier.HARD, 3, VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 31);
		assertFlag(DiaryRegion.VARROCK, DiaryTier.HARD, 4, VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 0);
		assertFlag(DiaryRegion.VARROCK, DiaryTier.ELITE, 5, VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 11);
	}

	@Test
	public void faladorDoneBitsSkipTheUnusedOne()
	{
		assertFlag(DiaryRegion.FALADOR, DiaryTier.EASY, 1, VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 0);
		assertFlag(DiaryRegion.FALADOR, DiaryTier.MEDIUM, 8, VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 18);
		assertFlag(DiaryRegion.FALADOR, DiaryTier.MEDIUM, 9, VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 20);
		assertFlag(DiaryRegion.FALADOR, DiaryTier.HARD, 6, VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 31);
		assertFlag(DiaryRegion.FALADOR, DiaryTier.HARD, 7, VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 0);
		assertFlag(DiaryRegion.FALADOR, DiaryTier.ELITE, 6, VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 10);
	}

	private static void assertFlag(DiaryTier tier, int number, int varp, int bit)
	{
		assertFlag(DiaryRegion.LUMBRIDGE, tier, number, varp, bit);
	}

	private static void assertFlag(DiaryRegion region, DiaryTier tier, int number, int varp, int bit)
	{
		for (DiaryTask t : Diaries.tasks(region, tier))
		{
			if (t.getNumber() == number)
			{
				assertEquals(t.key(), varp, t.getVarp());
				assertEquals(t.key(), bit, t.getBit());
				return;
			}
		}
		throw new AssertionError("no task " + tier + " " + number);
	}

	@Test
	public void everyTaskHasABeginnerGuide()
	{
		for (DiaryTask t : Diaries.all())
		{
			assertFalse(t.key() + " has no stages", t.getStages().isEmpty());
			assertNotNull(t.key(), t.getText());
			for (DiaryStage s : t.getStages())
			{
				assertNotNull(t.key() + " stage title", s.getTitle());
				assertFalse(t.key() + " '" + s.getTitle() + "' has no how-to", s.getLines().isEmpty());
				if (s.getAt() != null && Wilderness.contains(s.getAt()))
				{
					assertTrue(t.key() + " is in the Wilderness but not flagged", t.isWilderness());
				}
			}
		}
	}

	@Test
	public void everyItemCanBeGot()
	{
		for (DiaryTask t : Diaries.all())
		{
			for (DiaryItem item : t.getItems())
			{
				assertTrue(t.key() + " " + item.getName(), item.getIds().length > 0);
				assertTrue(t.key() + " " + item.getName() + ": buy id, tool or how-to needed",
					item.getBuyId() > 0 || item.getTool() != null || item.getHowToGet() != null);
			}
		}
	}

	@Test
	public void everyTierHasItsReward()
	{
		for (DiaryRegion r : DiaryRegion.values())
		{
			for (DiaryTier t : DiaryTier.values())
			{
				DiaryReward reward = Diaries.reward(r, t);
				assertNotNull(r + " " + t, reward);
				assertFalse(r + " " + t + " perks", reward.getPerks().isEmpty());
				for (String perk : reward.getPerks())
				{
					assertFalse(r + " " + t + " leftover wiki markup: " + perk, perk.contains("{{") || perk.contains("]]"));
				}
			}
		}
	}
}
