package com.skillinghelper.clue;

import com.skillinghelper.clue.data.ClueData;
import com.skillinghelper.clue.data.ClueItem;
import com.skillinghelper.clue.data.ClueTier;
import com.skillinghelper.clue.data.ClueWays;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.DiaryGuide;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ClueGuideTest
{
	private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);
	// Made-up ids: the classifier below decides what they are
	private static final int EASY_CLUE = 90001;
	private static final int MEDIUM_CLUE = 90002;
	private static final int HARD_CLUE = 90003;
	private static final int ELITE_CLUE = 90004;
	private static final int EASY_BOX = 90005;

	private static ClueItem classify(int id)
	{
		switch (id)
		{
			case EASY_CLUE:
				return new ClueItem(ClueTier.EASY, ClueItem.Kind.CLUE);
			case MEDIUM_CLUE:
				return new ClueItem(ClueTier.MEDIUM, ClueItem.Kind.CLUE);
			case HARD_CLUE:
				return new ClueItem(ClueTier.HARD, ClueItem.Kind.CLUE);
			case ELITE_CLUE:
				return new ClueItem(ClueTier.ELITE, ClueItem.Kind.CLUE);
			case EASY_BOX:
				return new ClueItem(ClueTier.EASY, ClueItem.Kind.SCROLL_BOX);
			case ItemID.TRAIL_REWARD_CASKET_EASY:
				return new ClueItem(ClueTier.EASY, ClueItem.Kind.REWARD_CASKET);
			default:
				return null;
		}
	}

	private static DiaryPlayer.DiaryPlayerBuilder player(int level, Map<Integer, Integer> carried,
		Map<Integer, Integer> bank)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, level);
		}
		return DiaryPlayer.builder().location(LUMBRIDGE).levels(levels).combatLevel(level).members(true)
			.carried(carried).bank(bank);
	}

	private static DiaryStep resolve(ClueTier tier, DiaryPlayer p)
	{
		return ClueGuide.resolve(tier, ClueHoldings.of(p.getCarried(), p.getBank(), ClueGuideTest::classify), p, null,
			true);
	}

	@Test
	public void freshPlayerIsSentToGoblinsEvenOnAFreeWorld()
	{
		DiaryStep step = resolve(ClueTier.BEGINNER, player(1, Map.of(), new HashMap<>()).members(false).build());
		assertEquals(DiaryStep.Kind.TRAVEL, step.getKind());
		assertEquals("clue.BEGINNER.goblins", step.getTaskKey());
	}

	@Test
	public void membersTiersNeedAMembersWorld()
	{
		DiaryStep step = resolve(ClueTier.EASY, player(50, Map.of(), new HashMap<>()).members(false).build());
		assertEquals(DiaryStep.Kind.LOCKED, step.getKind());
	}

	@Test
	public void noClueMeansGettingOneBestWayFirst()
	{
		DiaryStep step = resolve(ClueTier.EASY, player(50, Map.of(ItemID.COINS, 1_000_000), new HashMap<>()).build());
		assertEquals("clue.EASY.gourmet", step.getTaskKey());
		assertEquals(DiaryStep.Kind.BUY, step.getKind());
	}

	@Test
	public void lootingJarsKeepsGoingUntilTheyRunOut()
	{
		int jar = ItemID.II_CAPTURED_IMPLING_3;
		DiaryStep step = resolve(ClueTier.EASY, player(50, Map.of(jar, 3), new HashMap<>()).build());
		assertEquals(DiaryStep.Kind.DO, step.getKind());
		assertTrue(step.getUse().contains(jar));
	}

	@Test
	public void casketsAreOpenedFirst()
	{
		DiaryStep step = resolve(ClueTier.EASY, player(50,
			Map.of(ItemID.TRAIL_REWARD_CASKET_EASY, 2, EASY_CLUE, 1), new HashMap<>()).build());
		assertEquals(DiaryStep.Kind.DO, step.getKind());
		assertTrue(step.getUse().contains(ItemID.TRAIL_REWARD_CASKET_EASY));
	}

	@Test
	public void carriedClueWithSpadeIsRead()
	{
		DiaryStep step = resolve(ClueTier.EASY, player(50, Map.of(EASY_CLUE, 1, ItemID.SPADE, 1), new HashMap<>())
			.build());
		assertEquals(DiaryStep.Kind.DO, step.getKind());
		assertTrue(step.getUse().contains(EASY_CLUE));
		assertTrue(step.getLines().get(0).contains("Read"));
	}

	private static DiaryStep resolve(ClueTier tier, DiaryPlayer p, ClueStep clueStep)
	{
		return ClueGuide.resolve(tier, ClueHoldings.of(p.getCarried(), p.getBank(), ClueGuideTest::classify), p, null,
			true, clueStep);
	}

	private static final WorldPoint DIG = new WorldPoint(3230, 3209, 0);

	private static ClueStep dig()
	{
		return ClueStep.builder().type(ClueStep.Type.COORDINATE).location(DIG).spade(true).build();
	}

	@Test
	public void digStepWithoutSpadeBuysOne()
	{
		DiaryStep step = resolve(ClueTier.EASY, player(50, Map.of(EASY_CLUE, 1), new HashMap<>()).build(), dig());
		assertEquals(DiaryStep.Kind.BUY, step.getKind());
		assertTrue(step.getWithdraw().containsKey(ItemID.SPADE));
	}

	@Test
	public void digStepWalksToTheTileThenDigs()
	{
		Map<Integer, Integer> kit = Map.of(EASY_CLUE, 1, ItemID.SPADE, 1);
		DiaryStep walk = resolve(ClueTier.EASY, player(50, kit, new HashMap<>()).build(), dig());
		assertEquals(DiaryStep.Kind.TRAVEL, walk.getKind());
		assertEquals(DIG, walk.getDestination().getPoint());
		DiaryStep there = resolve(ClueTier.EASY, player(50, kit, new HashMap<>()).location(DIG).build(), dig());
		assertEquals(DiaryStep.Kind.DO, there.getKind());
		assertTrue(there.getUse().contains(ItemID.SPADE));
		assertTrue(there.getLines().stream().anyMatch(l -> l.contains("Dig")));
		assertEquals(DIG, there.getDestination().getPoint());
	}

	@Test
	public void npcStepOutlinesTheNpcAndGivesTheAnswer()
	{
		ClueStep talk = ClueStep.builder().type(ClueStep.Type.ANAGRAM).npc("Hans").answer("5")
			.location(LUMBRIDGE).build();
		DiaryStep step = resolve(ClueTier.MEDIUM, player(50, Map.of(MEDIUM_CLUE, 1), new HashMap<>()).build(), talk);
		assertEquals(DiaryStep.Kind.DO, step.getKind());
		assertTrue(step.getNpcs().contains("Hans"));
		assertTrue(step.getLines().stream().anyMatch(l -> l.contains("type 5")));
	}

	@Test
	public void emoteItemsComeOutOfTheBank()
	{
		ClueStep emote = ClueStep.builder().type(ClueStep.Type.EMOTE).location(LUMBRIDGE).emote("Cheer")
			.item(new ClueStep.Need("Bronze axe", false, ItemID.BRONZE_AXE)).build();
		Map<Integer, Integer> bank = new HashMap<>(Map.of(ItemID.BRONZE_AXE, 1));
		DiaryStep step = resolve(ClueTier.EASY, player(50, Map.of(EASY_CLUE, 1), bank).build(), emote);
		assertEquals(DiaryStep.Kind.OPEN_BANK, step.getKind());
		assertTrue(step.getWithdraw().containsKey(ItemID.BRONZE_AXE));
	}

	@Test
	public void wildernessStepNeedsOptIn()
	{
		ClueStep wildy = ClueStep.builder().type(ClueStep.Type.COORDINATE).spade(true)
			.location(new WorldPoint(3100, 3700, 0)).build();
		DiaryStep step = resolve(ClueTier.HARD, player(80, Map.of(HARD_CLUE, 1, ItemID.SPADE, 1), new HashMap<>())
			.build(), wildy);
		assertEquals(DiaryStep.Kind.LOCKED, step.getKind());
	}

	@Test
	public void bankedClueIsTakenOut()
	{
		Map<Integer, Integer> bank = new HashMap<>(Map.of(EASY_CLUE, 1, ItemID.SPADE, 1));
		DiaryStep step = resolve(ClueTier.EASY, player(50, Map.of(), bank).build());
		assertEquals(DiaryStep.Kind.OPEN_BANK, step.getKind());
		assertTrue(step.getWithdraw().containsKey(EASY_CLUE));
	}

	@Test
	public void scrollBoxIsOpenedWhenNoClueIsHeld()
	{
		DiaryStep step = resolve(ClueTier.EASY, player(50, Map.of(EASY_BOX, 2), new HashMap<>()).build());
		assertEquals(DiaryStep.Kind.DO, step.getKind());
		assertTrue(step.getUse().contains(EASY_BOX));
	}

	@Test
	public void masterNeedsAllFourForWatson()
	{
		DiaryStep missing = resolve(ClueTier.MASTER, player(90, Map.of(EASY_CLUE, 1, MEDIUM_CLUE, 1), new HashMap<>())
			.build());
		assertEquals(DiaryStep.Kind.LOCKED, missing.getKind());
		DiaryStep ready = resolve(ClueTier.MASTER, player(90,
			Map.of(EASY_CLUE, 1, MEDIUM_CLUE, 1, HARD_CLUE, 1, ELITE_CLUE, 1), new HashMap<>()).build());
		assertEquals(DiaryStep.Kind.TRAVEL, ready.getKind());
		assertEquals(ClueWays.WATSON, ready.getDestination());
	}

	@Test
	public void wildernessStashNeedsOptIn()
	{
		ClueData.Stash wild = ClueData.stashes(ClueTier.HARD).stream().filter(s -> Wilderness.contains(s.getPoint()))
			.findFirst().orElseThrow(AssertionError::new);
		DiaryPlayer p = player(90, Map.of(), new HashMap<>()).build();
		assertTrue(DiaryGuide.missing(ClueGuide.stashTask(wild), p).contains("Wilderness turned off in the panel"));
		assertTrue(DiaryGuide.missing(ClueGuide.stashTask(wild), p.toBuilder().allowWilderness(true).build()).isEmpty());
	}

	@Test
	public void holdingsCompareByValue()
	{
		Map<Integer, Integer> carried = Map.of(EASY_CLUE, 1, EASY_BOX, 2);
		assertEquals(ClueHoldings.of(carried, Map.of(), ClueGuideTest::classify),
			ClueHoldings.of(carried, Map.of(), ClueGuideTest::classify));
		ClueHoldings h = ClueHoldings.of(carried, Map.of(ItemID.TRAIL_REWARD_CASKET_EASY, 3), ClueGuideTest::classify);
		assertEquals(1, h.get(ClueTier.EASY, ClueItem.Kind.CLUE).getCarried());
		assertEquals(2, h.get(ClueTier.EASY, ClueItem.Kind.SCROLL_BOX).getCarried());
		assertEquals(3, h.get(ClueTier.EASY, ClueItem.Kind.REWARD_CASKET).getBanked());
	}
}
