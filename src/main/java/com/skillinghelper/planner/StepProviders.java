package com.skillinghelper.planner;

import com.skillinghelper.SkillingStepProvider;
import com.skillinghelper.diary.DiaryStepProvider;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.pvm.PvmStepProvider;
import java.util.List;

/**
 * Every plugin in this jar that hands the Account Planner steps. A new plugin adds its provider here (see
 * {@link PlanStepProvider} for how). Earlier providers win when two make a step with the same id, so the
 * plugins with the most specific guides (quests, unlocks) go first.
 */
public final class StepProviders
{
	public static final List<PlanStepProvider> ALL = List.of(
		new DiaryStepProvider(),
		new PvmStepProvider(),
		new SkillingStepProvider());

	private StepProviders()
	{
	}
}
