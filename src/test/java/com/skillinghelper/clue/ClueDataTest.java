package com.skillinghelper.clue;

import com.skillinghelper.clue.data.ClueData;
import com.skillinghelper.clue.data.ClueItem;
import com.skillinghelper.clue.data.ClueTier;
import com.skillinghelper.clue.data.ClueWays;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.DiaryGuide;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.data.DiaryTask;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ClueDataTest
{
	@Test
	public void everyTierHasRewardsThatAddUpToOneRoll()
	{
		for (ClueTier t : ClueTier.values())
		{
			ClueData.Casket c = ClueData.casket(t);
			assertNotNull(t + " casket", c);
			assertTrue(c.getMinRolls() >= 1 && c.getMaxRolls() > c.getMinRolls());
			double sum = 0;
			int uniques = 0;
			for (ClueData.Reward r : ClueData.rewards(t))
			{
				if (!r.isTertiary() && !r.isMimicOnly())
				{
					sum += r.getChance();
				}
				uniques += r.isUnique() ? 1 : 0;
			}
			assertEquals(t + " chances per roll", 1.0, sum, 0.01);
			assertTrue(t + " uniques", uniques > 10);
		}
	}

	@Test
	public void stashUnitsAndDropsAreLoaded()
	{
		int[] expected = {3, 31, 25, 16, 19, 25};
		for (ClueTier t : ClueTier.values())
		{
			assertEquals(t + " STASH units", expected[t.ordinal()], ClueData.stashes(t).size());
			for (ClueData.Stash s : ClueData.stashes(t))
			{
				assertFalse(s.getItems().isEmpty());
				assertEquals(s.getItems().size(), s.getIds().size());
			}
			if (t != ClueTier.MASTER)
			{
				assertTrue(t + " drops", ClueData.drops(t).size() > 50);
			}
		}
		// The volcano unit in the north-east of the Wilderness is flagged
		assertTrue(ClueData.stashes(ClueTier.HARD).stream().anyMatch(s -> Wilderness.contains(s.getPoint())));
	}

	@Test
	public void itemsAreClassifiedByName()
	{
		assertEquals(new ClueItem(ClueTier.EASY, ClueItem.Kind.CLUE), ClueItem.of(2677, "Clue scroll (easy)"));
		assertEquals(new ClueItem(ClueTier.MEDIUM, ClueItem.Kind.CLUE), ClueItem.of(1, "Casket (medium)"));
		assertEquals(new ClueItem(ClueTier.HARD, ClueItem.Kind.CLUE), ClueItem.of(1, "Puzzle box (hard)"));
		assertEquals(new ClueItem(ClueTier.HARD, ClueItem.Kind.SCROLL_BOX), ClueItem.of(1, "Scroll box (hard)"));
		assertEquals(new ClueItem(ClueTier.BEGINNER, ClueItem.Kind.CONTAINER), ClueItem.of(1, "Clue nest (beginner)"));
		assertEquals(new ClueItem(ClueTier.MASTER, ClueItem.Kind.REWARD_CASKET),
			ClueItem.of(ItemID.TRAIL_REWARD_CASKET_MASTER, "Reward casket (master)"));
		assertEquals(ClueItem.Kind.MIMIC, ClueItem.of(ItemID.TRAIL_MIMIC_CASKET, "Casket").getKind());
		assertNull(ClueItem.of(1, "Rune scimitar"));
		assertNull(ClueItem.of(1, "Casket"));
	}

	@Test
	public void everyWayIsUniqueAndCanBeStarted()
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, 99);
		}
		DiaryPlayer p = DiaryPlayer.builder().location(new WorldPoint(3164, 3487, 0)).levels(levels).combatLevel(126)
			.members(true).bank(new HashMap<>()).build();
		Set<String> keys = new HashSet<>();
		for (ClueTier t : ClueTier.values())
		{
			assertFalse(t + " tips", ClueWays.tips(t).isEmpty());
			for (DiaryTask w : ClueWays.ways(t))
			{
				assertTrue("duplicate " + w.key(), keys.add(w.key()));
				assertFalse(w.getStages().isEmpty());
				assertTrue(w.key() + " can't be bought", DiaryGuide.obtainable(w, p));
				assertNotNull(DiaryGuide.resolve(w, p));
				assertEquals(w, ClueWays.way(w.key()));
			}
		}
	}
}
