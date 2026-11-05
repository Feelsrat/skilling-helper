package com.skillinghelper.planner;

import com.skillinghelper.SkillingStepProvider;
import com.skillinghelper.ca.CaStepProvider;
import com.skillinghelper.clue.ClueStepProvider;
import com.skillinghelper.diary.DiaryStepProvider;
import com.skillinghelper.minigame.MinigameStepProvider;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.pvm.PvmStepProvider;
import com.skillinghelper.quests.QuestStepProvider;
import com.skillinghelper.unlocks.UnlockStepProvider;
import java.util.List;

/**
 * Every plugin in this jar that hands the Account Planner steps. A new plugin adds its provider here (see
 * {@link PlanStepProvider} for how). Earlier providers win when two make a step with the same id, so the
 * plugins with the most specific guides (quests, unlocks) go first.
 */
public final class StepProviders
{
	public static final List<PlanStepProvider> ALL = List.of(
		new QuestStepProvider(),
		new UnlockStepProvider(),
		new DiaryStepProvider(),
		new MinigameStepProvider(),
		new ClueStepProvider(),
		new PvmStepProvider(),
		new CaStepProvider(),
		new SkillingStepProvider());

	private StepProviders()
	{
	}
}
