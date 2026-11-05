package com.skillinghelper.diary;

import com.skillinghelper.OpenDiaryRequest;
import com.skillinghelper.diary.data.Diaries;
import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryReward;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.diary.data.DiaryTier;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.planner.api.StepKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * Achievement Diary Guides' steps for the Account Planner: one per diary tier, needing the highest level of each
 * skill (and every quest) its unfinished tasks ask for. Done when the tier's reward is claimed.
 */
public class DiaryStepProvider implements PlanStepProvider
{
	/**
	 * Rough minutes per task, by tier: later tiers send you further and need more supplies.
	 */
	private static final int[] TASK_MINUTES = {4, 7, 12, 20};
	private static final int[] VALUE = {4, 5, 6, 6};

	@Override
	public String name()
	{
		return "Achievement Diary Guides";
	}

	@Override
	public List<PlanStep> steps(AccountState a)
	{
		Set<String> done = doneTasks(a.getVarp(), a.getVarbit());
		DiaryPlayer player = DiaryPlayer.builder()
			.levels(a.getLevels())
			.combatLevel(a.getCombatLevel())
			.quests(a.getQuests())
			.done(done)
			.varbit(a.getVarbit())
			.varp(a.getVarp())
			.carried(a.getOwned())
			.bank(Collections.emptyMap())
			.members(true)
			.allowWilderness(a.isAllowWilderness())
			.buyPrice(a.getPrice())
			.build();
		List<PlanStep> out = new ArrayList<>();
		for (DiaryRegion r : DiaryRegion.values())
		{
			if (!Diaries.guided(r))
			{
				continue;
			}
			for (DiaryTier tier : DiaryTier.values())
			{
				out.add(step(r, tier, a, player));
			}
		}
		return out;
	}

	public static String stepId(DiaryRegion r, DiaryTier tier)
	{
		return "diary:" + r.name() + ":" + tier.name();
	}

	private static PlanStep step(DiaryRegion r, DiaryTier tier, AccountState a, DiaryPlayer player)
	{
		List<DiaryTask> tasks = Diaries.tasks(r, tier);
		Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
		Set<Quest> quests = new LinkedHashSet<>();
		Set<Quest> questsStarted = new LinkedHashSet<>();
		Set<String> notes = new LinkedHashSet<>();
		boolean wilderness = false;
		long gp = 0;
		int left = 0;
		for (DiaryTask t : tasks)
		{
			if (player.getDone().contains(t.key()))
			{
				continue;
			}
			left++;
			t.getSkills().forEach((s, lvl) -> skills.merge(s, lvl, Math::max));
			quests.addAll(t.getQuests());
			questsStarted.addAll(t.getQuestsStarted());
			wilderness |= t.isWilderness();
			if (t.getCombat() > 0 && a.getCombatLevel() < t.getCombat())
			{
				notes.add("Combat level " + t.getCombat());
			}
			for (DiaryTask.VarReq v : t.getVarReqs())
			{
				int have = v.getVarp() > 0 ? a.getVarp().applyAsInt(v.getVarp()) : a.getVarbit().applyAsInt(v.getVarbit());
				if (have < v.getMin())
				{
					notes.add(v.getMin() + " " + v.getLabel() + " (you have " + have + ")");
				}
			}
			notes.addAll(t.getNotes());
			if (a.isBankKnown())
			{
				gp += DiaryGuide.cost(t, player);
			}
		}
		questsStarted.removeAll(quests);
		for (Quest q : new ArrayList<>(questsStarted))
		{
			if (a.quest(q) != QuestState.NOT_STARTED)
			{
				questsStarted.remove(q);
			}
		}
		boolean claimed = a.getVarbit().applyAsInt(r.rewardVarbit(tier)) > 0;
		String title = r.getLabel() + " " + tier.getLabel() + " diary";
		DiaryReward reward = Diaries.reward(r, tier);
		String why = reward == null ? null : reward.getItem()
			+ (reward.getPerks().isEmpty() ? "." : ": " + String.join("; ", reward.getPerks().subList(0,
			Math.min(2, reward.getPerks().size()))) + (reward.getPerks().size() > 2 ? "; and more." : "."));
		String taskmaster = r.getTaskmasters().isEmpty() ? "the taskmaster" : r.getTaskmasters().get(0);
		PlanStep.PlanStepBuilder b = PlanStep.builder()
			.id(stepId(r, tier))
			.kind(StepKind.DIARY)
			.title(title)
			.why(why)
			.done(claimed)
			.skills(skills)
			.quests(quests)
			.questsStarted(questsStarted)
			.notes(notes)
			.minutes(Math.max(left, 1) * TASK_MINUTES[tier.ordinal()])
			.value(VALUE[tier.ordinal()])
			.gpCost(gp)
			.wilderness(wilderness)
			.open(new OpenDiaryRequest(r.name(), tier.name()))
			.openIn("Achievement Diary Guides")
			.wikiPage(r.getWikiPage())
			.howTo("Press Open guide: Achievement Diary Guides opens on this tier and starts its guide ("
				+ (tasks.size() - left) + "/" + tasks.size() + " tasks done)")
			.howTo("Follow its steps: it takes you through the tasks in walking order, with what to bring")
			.howTo("When every task is ticked, talk to " + taskmaster + " (" + r.getTaskmasterSpot().getName()
				+ ") to claim the reward");
		if (tier.previous() != null)
		{
			b.after(stepId(r, tier.previous()));
		}
		return b.build();
	}

	/**
	 * Keys of the diary tasks the game says are done.
	 */
	static Set<String> doneTasks(IntUnaryOperator varp, IntUnaryOperator varbit)
	{
		Set<String> done = new HashSet<>();
		Map<Integer, Integer> varps = new HashMap<>();
		for (DiaryTask t : Diaries.all())
		{
			boolean isDone;
			if (t.getVarp() > 0)
			{
				int value = varps.computeIfAbsent(t.getVarp(), varp::applyAsInt);
				isDone = (value >>> t.getBit() & 1) != 0;
			}
			else
			{
				isDone = varbit.applyAsInt(t.getVarbit()) >= t.getVarbitDone();
			}
			if (isDone)
			{
				done.add(t.key());
			}
		}
		return done;
	}
}
