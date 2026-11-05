package com.skillinghelper;

import com.skillinghelper.planner.PathPlanner;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.PlanStepProvider;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Skilling Helper's steps for the Account Planner: a few levels that open up a lot early on, then every skill to
 * 99 in chunks of ten levels, so the planner levels the account evenly once the content runs out.
 */
public class SkillingStepProvider implements PlanStepProvider
{
	static final int CHUNK = 10;
	static final int MILESTONE_VALUE = 5;
	static final int MAX_CHUNK_VALUE = 1;

	private static final class Milestone
	{
		final Skill skill;
		final int level;
		final String why;
		final Quest quest;

		Milestone(Skill skill, int level, String why, Quest quest)
		{
			this.skill = skill;
			this.level = level;
			this.why = why;
			this.quest = quest;
		}
	}

	private static final List<Milestone> MILESTONES = List.of(
		new Milestone(Skill.ATTACK, 40, "Rune weapons, the best you can wield for a long while without quests.", null),
		new Milestone(Skill.DEFENCE, 40, "Rune armour: a big step up in defence for every kind of combat.", null),
		new Milestone(Skill.PRAYER, 43, "Protect from Melee, Missiles and Magic, the prayers most bosses and Slayer "
			+ "monsters are built around.", null),
		new Milestone(Skill.MAGIC, 55, "High Level Alchemy: turn items into gold anywhere, with Magic xp on top.",
			null),
		new Milestone(Skill.HERBLORE, 38, "Prayer potions, the main supply for bossing and Slayer.",
			Quest.DRUIDIC_RITUAL));

	@Override
	public String name()
	{
		return "Skilling Helper";
	}

	@Override
	public List<PlanStep> steps(AccountState account)
	{
		List<PlanStep> out = new ArrayList<>();
		for (Milestone m : MILESTONES)
		{
			PlanStep.PlanStepBuilder b = PathPlanner.trainStep(m.skill, m.level, m.why, MILESTONE_VALUE).toBuilder()
				.done(account.level(m.skill) >= m.level);
			if (m.quest != null)
			{
				b.quest(m.quest);
			}
			out.add(b.build());
		}
		for (Skill skill : Skill.values())
		{
			if (skill == Skill.HITPOINTS)
			{
				// Comes with combat training
				continue;
			}
			int have = account.level(skill);
			for (int level = CHUNK; level < Experience.MAX_REAL_LEVEL; level += CHUNK)
			{
				out.add(PathPlanner.trainStep(skill, level, "On the way to 99 " + skill.getName() + ".",
					MAX_CHUNK_VALUE).toBuilder().done(have >= level).build());
			}
			out.add(PathPlanner.trainStep(skill, Experience.MAX_REAL_LEVEL,
				"99 " + skill.getName() + ": the skill cape, and one step closer to max.", MAX_CHUNK_VALUE)
				.toBuilder().done(have >= Experience.MAX_REAL_LEVEL).build());
		}
		return out;
	}
}
