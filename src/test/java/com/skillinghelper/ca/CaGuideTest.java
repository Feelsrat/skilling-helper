package com.skillinghelper.ca;

import com.skillinghelper.ca.data.CaTask;
import com.skillinghelper.ca.data.CaTasks;
import com.skillinghelper.ca.data.CaTier;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CaGuideTest
{
	private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);

	public static DiaryPlayer.DiaryPlayerBuilder player(int level)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, s == Skill.HITPOINTS ? Math.max(10, level) : level);
		}
		Map<Quest, QuestState> quests = new HashMap<>();
		for (Quest q : Quest.values())
		{
			quests.put(q, QuestState.FINISHED);
		}
		return DiaryPlayer.builder()
			.location(LUMBRIDGE)
			.levels(levels)
			.combatLevel(level)
			.quests(quests)
			.members(true)
			.bank(new HashMap<>());
	}

	private static List<CaRanker.Ranked> rank(CaTier tier, DiaryPlayer p)
	{
		return CaRanker.rank(CaTasks.tier(tier), p, CaRanker.power(p), Map.of(), Set.of());
	}

	@Test
	public void gearRaisesFightingStrength()
	{
		DiaryPlayer bare = player(75).build();
		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(ItemID.ABYSSAL_WHIP, 1);
		bank.put(ItemID.DRAGON_CHAINBODY, 1);
		DiaryPlayer geared = player(75).bank(bank).build();
		assertTrue(CaRanker.power(geared).getValue() > CaRanker.power(bare).getValue());
		assertEquals("Abyssal whip", CaRanker.power(geared).getWeapon());
	}

	@Test
	public void lowLevelPlayerGetsEasyBossesFirstAndNoWilderness()
	{
		DiaryPlayer p = player(45).build();
		List<CaRanker.Ranked> ranked = rank(CaTier.EASY, p);
		CaRanker.Ranked first = ranked.get(0);
		assertTrue(first.getFit() == CaRanker.Fit.READY || first.getFit() == CaRanker.Fit.STRETCH);
		assertTrue(first.getTask().getName() + " " + first.getMonster().getLevel(), first.getMonster().getLevel() <= 50);
		for (CaRanker.Ranked r : ranked)
		{
			if (r.getMonster() != null && r.getMonster().isWilderness())
			{
				assertEquals(r.getTask().getName(), CaRanker.Fit.LOCKED, r.getFit());
			}
		}
	}

	@Test
	public void tasksAtTheSameBossStayTogether()
	{
		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(ItemID.MOSSY_KEY, 1);
		DiaryPlayer p = player(70).bank(bank).build();
		List<CaRanker.Ranked> ranked = rank(CaTier.EASY, p);
		// Bryophyta's easier tasks come one after another, so one trip does them
		int first = -1;
		for (int i = 0; i < ranked.size() && first < 0; i++)
		{
			if ("Bryophyta".equals(ranked.get(i).getTask().getMonster()))
			{
				first = i;
			}
		}
		assertTrue(first >= 0);
		for (int i = first; i < first + 3; i++)
		{
			assertEquals("Bryophyta", ranked.get(i).getTask().getMonster());
		}
	}

	@Test
	public void guideGoesBankThenTravelThenFight()
	{
		DiaryPlayer p = player(60).build();
		CaTask obor = CaDataTest.byName("Obor Novice");
		CaRanker.Ranked r = CaRanker.rankOne(obor, p, CaRanker.power(p), Map.of(), false);
		// No giant key: the guide says how to get one
		assertEquals(CaRanker.Fit.LOCKED, r.getFit());

		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(ItemID.HILLGIANT_BOSS_KEY, 1);
		p = player(60).bank(bank).build();
		r = CaRanker.rankOne(obor, p, CaRanker.power(p), Map.of(), false);
		assertEquals(CaRanker.Fit.READY, r.getFit());
		CaGuide.Context c = CaGuide.Context.builder().player(p).power(CaRanker.power(p)).build();
		DiaryStep step = CaGuide.resolve(r, c);
		assertEquals(DiaryStep.Kind.OPEN_BANK, step.getKind());

		Map<Integer, Integer> carried = new HashMap<>();
		carried.put(ItemID.HILLGIANT_BOSS_KEY, 1);
		p = player(60).bank(new HashMap<>()).carried(carried).build();
		c = CaGuide.Context.builder().player(p).power(CaRanker.power(p)).build();
		step = CaGuide.resolve(r, c);
		assertEquals(DiaryStep.Kind.TRAVEL, step.getKind());
		assertNotNull(step.getDestination());

		p = player(60).carried(carried).location(new WorldPoint(3097, 3468, 0)).build();
		c = CaGuide.Context.builder().player(p).power(CaRanker.power(p)).build();
		step = CaGuide.resolve(r, c);
		assertEquals(DiaryStep.Kind.DO, step.getKind());
		assertTrue(step.getNpcs().contains("Obor"));
		assertTrue(String.join("\n", step.getLines()).contains("trapdoor"));

		// Once in, the steps stay on the fight even away from the entrance
		p = player(60).carried(carried).location(new WorldPoint(3095, 9850, 0)).build();
		c = CaGuide.Context.builder().player(p).power(CaRanker.power(p)).arrived(true).build();
		assertEquals(DiaryStep.Kind.DO, CaGuide.resolve(r, c).getKind());
	}

	@Test
	public void freeWorldTaskNeedsAFreeWorld()
	{
		CaTask t = CaDataTest.byName("Fighting as Intended");
		DiaryPlayer p = player(60).build();
		CaRanker.Ranked r = CaRanker.rankOne(t, p, CaRanker.power(p), Map.of(), false);
		assertTrue(r.getMissing().toString(), r.getMissing().stream().anyMatch(m -> m.contains("free-to-play")));
	}

	@Test
	public void claimStepWhenARewardIsReachedButNoHilt()
	{
		DiaryPlayer p = player(60).build();
		Map<CaTier, Integer> thresholds = new EnumMap<>(CaTier.class);
		for (CaTier t : CaTier.values())
		{
			thresholds.put(t, t.getFallbackThreshold());
		}
		CaGuide.Context c = CaGuide.Context.builder().player(p).power(CaRanker.power(p)).points(50)
			.thresholds(thresholds).build();
		DiaryStep step = CaGuide.resolve(rank(CaTier.EASY, p), c, new HashSet<>(), null);
		assertEquals(DiaryStep.Kind.CLAIM, step.getKind());
		assertTrue(step.getTitle().contains("Easy"));
		c = CaGuide.Context.builder().player(p).power(CaRanker.power(p)).points(50).thresholds(thresholds)
			.claimed(Set.of(CaTier.EASY)).build();
		assertFalse(CaGuide.resolve(rank(CaTier.EASY, p), c, new HashSet<>(), null).getKind() == DiaryStep.Kind.CLAIM);
	}

	@Test
	public void everyTaskResolvesForAStrongPlayer()
	{
		Map<Integer, Integer> bank = new HashMap<>();
		DiaryPlayer p = player(99).bank(bank).allowWilderness(true).build();
		CaRanker.Power power = CaRanker.power(p);
		for (CaTask t : CaTasks.all())
		{
			CaRanker.Ranked r = CaRanker.rankOne(t, p, power, Map.of(), false);
			DiaryStep step = CaGuide.resolve(r, CaGuide.Context.builder().player(p).power(power).build());
			assertNotNull(t.getName(), step);
			assertFalse(t.getName(), step.getLines().isEmpty());
		}
	}
}
