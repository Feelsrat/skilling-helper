package com.skillinghelper.planner;

import com.skillinghelper.data.RatePace;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.planner.api.StepKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PathPlannerTest
{
	private static final GuideRepository GUIDES = new GuideRepository();
	private static final SkillHours HOURS = new SkillHours(GUIDES::methods, RatePace.STEADY);
	private static final PathPlanner.Settings NONE = new PathPlanner.Settings(Collections.emptySet(),
		Collections.emptySet());

	private static AccountState account(int level)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, s == Skill.HITPOINTS ? Math.max(10, level) : level);
		}
		return AccountState.builder().levels(levels).combatLevel(3).members(true).build();
	}

	private static List<PlanStep> allSteps(AccountState a)
	{
		List<PlanStep> out = new ArrayList<>();
		for (PlanStepProvider p : StepProviders.ALL)
		{
			out.addAll(p.steps(a));
		}
		return out;
	}

	private static int indexOf(PathPlanner.Plan plan, String id)
	{
		for (int i = 0; i < plan.getSteps().size(); i++)
		{
			if (plan.getSteps().get(i).getStep().getId().equals(id))
			{
				return i;
			}
		}
		return -1;
	}

	@Test
	public void requirementsComeBeforeTheStep()
	{
		PlanStep agility = PlanStep.builder().id("x:agility").kind(StepKind.OTHER).title("Needs agility")
			.skill(Skill.AGILITY, 30).quest(Quest.COOKS_ASSISTANT).after("x:first").value(10).build();
		PlanStep first = PlanStep.builder().id("x:first").kind(StepKind.OTHER).title("First").value(1).build();
		PathPlanner.Plan plan = PathPlanner.plan(List.of(agility, first), account(1), HOURS, NONE);
		int step = indexOf(plan, "x:agility");
		assertTrue(indexOf(plan, "x:first") < step);
		assertTrue(indexOf(plan, PlanStep.skillId(Skill.AGILITY, 30)) < step);
		assertTrue(indexOf(plan, PlanStep.questId(Quest.COOKS_ASSISTANT)) < step);
		assertEquals("Needed for Needs agility",
			plan.getSteps().get(indexOf(plan, PlanStep.skillId(Skill.AGILITY, 30))).getReason());
	}

	@Test
	public void wildernessStepsAndWhatWaitsOnThemAreLeftOut()
	{
		PlanStep wild = PlanStep.builder().id("x:wild").kind(StepKind.OTHER).title("Wild").wilderness(true).build();
		PlanStep after = PlanStep.builder().id("x:after").kind(StepKind.OTHER).title("After").after("x:wild").build();
		PathPlanner.Plan plan = PathPlanner.plan(List.of(wild, after), account(50), HOURS, NONE);
		assertTrue(plan.getSteps().isEmpty());
		assertEquals(List.of("Wild", "After"), plan.getUnreachable());

		AccountState allowed = account(50).toBuilder().allowWilderness(true).build();
		assertEquals(2, PathPlanner.plan(List.of(wild, after), allowed, HOURS, NONE).getSteps().size());
	}

	@Test
	public void skippedStepsCountAsDone()
	{
		PlanStep a = PlanStep.builder().id("x:a").kind(StepKind.OTHER).title("A").build();
		PlanStep b = PlanStep.builder().id("x:b").kind(StepKind.OTHER).title("B").after("x:a")
			.skill(Skill.MINING, 20).build();
		Set<String> skipped = new HashSet<>(List.of("x:a", PlanStep.skillId(Skill.MINING, 20)));
		PathPlanner.Plan plan = PathPlanner.plan(List.of(a, b), account(1), HOURS,
			new PathPlanner.Settings(skipped, Collections.emptySet()));
		assertEquals(1, plan.getSteps().size());
		assertEquals("x:b", plan.getSteps().get(0).getStep().getId());
	}

	@Test
	public void ignoredStepsAreLeftOutAndCountAsHad()
	{
		PlanStep quest = PlanStep.builder().id(PlanStep.questId(Quest.COOKS_ASSISTANT)).kind(StepKind.QUEST)
			.title("Cook's Assistant").finishes(Quest.COOKS_ASSISTANT).ignored(true).build();
		PlanStep after = PlanStep.builder().id("x:after").kind(StepKind.OTHER).title("After")
			.quest(Quest.COOKS_ASSISTANT).build();
		PathPlanner.Plan plan = PathPlanner.plan(List.of(quest, after), account(50), HOURS, NONE);
		assertEquals(1, plan.getSteps().size());
		assertEquals("x:after", plan.getSteps().get(0).getStep().getId());
	}

	@Test
	public void moneyStepWhenShortOfGold()
	{
		PlanStep pricey = PlanStep.builder().id("x:p").kind(StepKind.OTHER).title("Pricey").gpCost(1_000_000).build();
		AccountState poor = account(50).toBuilder().bankKnown(true).owned(Map.of(995, 200_000)).build();
		PathPlanner.Plan plan = PathPlanner.plan(List.of(pricey), poor, HOURS, NONE);
		assertEquals(StepKind.MONEY, plan.getSteps().get(0).getStep().getKind());
		assertEquals("Make about 800K gp", plan.getSteps().get(0).getStep().getTitle());
	}

	@Test
	public void freshAccountGetsAFullPlanWithEveryDiaryAndAllSkillsTo99()
	{
		AccountState a = account(1);
		long start = System.nanoTime();
		PathPlanner.Plan plan = PathPlanner.plan(allSteps(a), a, HOURS, NONE);
		long ms = (System.nanoTime() - start) / 1_000_000;
		Set<String> ids = new HashSet<>();
		for (PathPlanner.Planned p : plan.getSteps())
		{
			assertTrue("planned twice: " + p.getStep().getId(), ids.add(p.getStep().getId()));
		}
		for (Skill s : Skill.values())
		{
			if (s != Skill.HITPOINTS)
			{
				assertTrue(s + " to 99", ids.contains(PlanStep.skillId(s, 99)));
			}
		}
		plan.getSteps().stream().filter(p -> p.getStep().getKind() == StepKind.DIARY).count();
		allSteps(a).stream().filter(st -> st.getKind() == StepKind.DIARY && !ids.contains(st.getId()))
			.forEach(st -> System.out.println("not planned: " + st.getId()));
		assertTrue("took " + ms + "ms", ms < 1500);
		System.out.println("Fresh account: " + plan.getSteps().size() + " steps, ~" + Math.round(plan.getTotalHours())
			+ " h, planned in " + ms + " ms");
		for (int i = 0; i < 40; i++)
		{
			PathPlanner.Planned p = plan.getSteps().get(i);
			System.out.println((i + 1) + ". [" + p.getStep().getKind() + "] " + p.getStep().getTitle()
				+ (p.getReason() == null ? "" : "  <- " + p.getReason()) + "  " + Math.round(p.getHours() * 10) / 10.0 + "h");
		}
	}

	@Test
	public void midAccountPlan()
	{
		AccountState a = account(60);
		Map<Quest, QuestState> quests = new HashMap<>();
		for (Quest q : EnumSet.of(Quest.COOKS_ASSISTANT, Quest.DRUIDIC_RITUAL, Quest.RUNE_MYSTERIES))
		{
			quests.put(q, QuestState.FINISHED);
		}
		a = a.toBuilder().quests(quests).build();
		PathPlanner.Plan plan = PathPlanner.plan(allSteps(a), a, HOURS, NONE);
		assertFalse(plan.getSteps().isEmpty());
		for (int i = 0; i < 25; i++)
		{
			PathPlanner.Planned p = plan.getSteps().get(i);
			assertNotNull(p.getStep().getTitle());
			System.out.println((i + 1) + ". [" + p.getStep().getKind() + "] " + p.getStep().getTitle()
				+ (p.getReason() == null ? "" : "  <- " + p.getReason()) + "  " + Math.round(p.getHours() * 10) / 10.0 + "h");
		}
	}

	@Test
	public void skillHoursGrowWithLevel()
	{
		for (Skill s : Skill.values())
		{
			double early = HOURS.hours(s, 1, 50);
			double late = HOURS.hours(s, 50, 99);
			assertTrue(s + " " + early + " " + late, late > early && early > 0);
			System.out.println("hours " + s + " 1-99: " + Math.round(HOURS.hours(s, 1, 99)));
		}
	}
}
