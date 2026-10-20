package com.skillinghelper;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.MethodPlanner;
import com.skillinghelper.engine.PlanSegment;
import com.skillinghelper.engine.PlayerState;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PlannerTest
{
	private final GuideRepository guides = new GuideRepository();
	private final MethodPlanner planner = new MethodPlanner(id -> 100);

	static PlayerState state(Skill skill, int level, boolean members, ItemCounts bank)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		levels.put(skill, level);
		xp.put(skill, Experience.getXpForLevel(level));
		return PlayerState.builder()
			.levels(levels)
			.xp(xp)
			.bank(bank)
			.freeInventorySlots(28)
			.membersWorld(members)
			.build();
	}

	@Test
	public void everyGuidedSkillHasALevelOneMethod()
	{
		for (Skill skill : Skill.values())
		{
			List<Method> methods = guides.methods(skill);
			if (methods.isEmpty())
			{
				continue;
			}
			boolean hasStart = methods.stream().anyMatch(m -> m.getMinLevel() == 1);
			assertTrue(skill + " needs a level 1 method", hasStart);
		}
	}

	@Test
	public void methodIdsAreUnique()
	{
		List<Method> all = guides.all();
		assertEquals(all.size(), all.stream().map(Method::getId).distinct().count());
	}

	@Test
	public void lockedMethodsRankLast()
	{
		PlayerState s = state(Skill.WOODCUTTING, 20, true, ItemCounts.EMPTY);
		List<MethodOption> ranked = planner.rank(guides.methods(Skill.WOODCUTTING), s, TrainingPreference.FAST, 30);
		assertTrue(ranked.get(0).isAvailable());
		assertTrue(ranked.get(0).getMethod().getMinLevel() <= 20);
		assertFalse(ranked.get(ranked.size() - 1).isAvailable());
	}

	@Test
	public void freeToPlayWorldHidesMembersMethods()
	{
		PlayerState s = state(Skill.WOODCUTTING, 50, false, ItemCounts.EMPTY);
		List<MethodOption> ranked = planner.rank(guides.methods(Skill.WOODCUTTING), s, TrainingPreference.FAST, 60);
		assertFalse(ranked.get(0).getMethod().isMembers());
	}

	@Test
	public void planSwitchesMethodWhenBetterOneUnlocks()
	{
		PlayerState s = state(Skill.WOODCUTTING, 25, false, ItemCounts.EMPTY);
		List<PlanSegment> plan = planner.plan(guides.methods(Skill.WOODCUTTING), s, TrainingPreference.FAST, 35, null);
		assertEquals(2, plan.size());
		assertEquals(25, plan.get(0).getFromLevel());
		assertEquals(30, plan.get(0).getToLevel());
		assertEquals("wc_willows_draynor", plan.get(1).getOption().getMethod().getId());
		assertEquals(35, plan.get(plan.size() - 1).getToLevel());
	}

	@Test
	public void forcedMethodIsUsedForWholeGoal()
	{
		PlayerState s = state(Skill.WOODCUTTING, 25, false, ItemCounts.EMPTY);
		Method oaks = guides.byId("wc_oaks_varrock");
		List<PlanSegment> plan = planner.plan(guides.methods(Skill.WOODCUTTING), s, TrainingPreference.FAST, 45, oaks);
		assertEquals(1, plan.size());
		assertEquals(oaks, plan.get(0).getOption().getMethod());
	}

	@Test
	public void cheapPreferenceUsesLogsAlreadyInBank()
	{
		// 40 Firemaking with a bank full of willow logs and no gold
		PlayerState s = state(Skill.FIREMAKING, 40, false, ItemCounts.of(ItemID.WILLOW_LOGS, 5000, ItemID.TINDERBOX, 1));
		List<MethodOption> ranked = planner.rank(guides.methods(Skill.FIREMAKING), s, TrainingPreference.CHEAP, 50);
		assertEquals("fm_willow", ranked.get(0).getMethod().getId());
		assertEquals(0, ranked.get(0).getBuyCost());
	}

	@Test
	public void noPlanOnceGoalReached()
	{
		PlayerState s = state(Skill.MINING, 40, true, ItemCounts.EMPTY);
		assertTrue(planner.plan(guides.methods(Skill.MINING), s, TrainingPreference.RECOMMENDED, 40, null).isEmpty());
	}
}
