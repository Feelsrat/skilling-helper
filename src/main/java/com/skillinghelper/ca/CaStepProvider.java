package com.skillinghelper.ca;

import com.skillinghelper.OpenCombatAchievementsRequest;
import com.skillinghelper.ca.data.CaReward;
import com.skillinghelper.ca.data.CaTask;
import com.skillinghelper.ca.data.CaTasks;
import com.skillinghelper.ca.data.CaTier;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.planner.api.StepKind;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.gameval.VarbitID;

/**
 * Combat Achievement Guides' steps for the Account Planner: one per reward tier, done once the player has its
 * points. The time is a rough guess from the points still missing and how long tasks of that tier take.
 */
public class CaStepProvider implements PlanStepProvider
{
	/**
	 * Rough minutes per task, by tier (bosses get longer and need more practice further up).
	 */
	private static final int[] TASK_MINUTES = {8, 15, 25, 40, 60, 90};
	private static final int[] VALUE = {5, 5, 6, 6, 5, 4};
	/**
	 * Combat level that tier's tasks mostly expect, used only as a note.
	 */
	private static final int[] COMBAT = {40, 70, 90, 100, 115, 120};

	@Override
	public String name()
	{
		return "Combat Achievement Guides";
	}

	public static String stepId(CaTier tier)
	{
		return "ca:" + tier.name();
	}

	@Override
	public List<PlanStep> steps(AccountState a)
	{
		int points = a.getVarbit().applyAsInt(VarbitID.CA_POINTS);
		int[] done = new int[CaTier.values().length];
		int[] total = new int[CaTier.values().length];
		int sum = 0;
		for (CaTask t : CaTasks.all())
		{
			total[t.getTier().ordinal()]++;
			int varp = t.varp();
			if (varp > 0 && (a.getVarp().applyAsInt(varp) >>> t.bit() & 1) != 0)
			{
				done[t.getTier().ordinal()]++;
				sum += t.getTier().getPoints();
			}
		}
		if (points <= 0)
		{
			points = sum;
		}
		List<PlanStep> out = new ArrayList<>();
		int previous = 0;
		for (CaTier tier : CaTier.values())
		{
			int v = a.getVarbit().applyAsInt(tier.getThresholdVarbit());
			int threshold = v > previous ? v : tier.getFallbackThreshold();
			previous = threshold;
			int missing = Math.max(0, threshold - points);
			// Points mostly come from the tier being worked on, so that tier's task size sets the pace
			int tasks = (missing + tier.getPoints() - 1) / tier.getPoints();
			CaReward reward = CaTasks.reward(tier);
			String why = reward == null || reward.getPerks().isEmpty() ? null
				: String.join("; ", reward.getPerks().subList(0, Math.min(3, reward.getPerks().size()))) + ".";
			PlanStep.PlanStepBuilder b = PlanStep.builder()
				.id(stepId(tier))
				.kind(StepKind.COMBAT_ACHIEVEMENT)
				.title(tier.getLabel() + " Combat Achievements reward")
				.why(why)
				.done(points >= threshold)
				.minutes(Math.max(1, tasks) * TASK_MINUTES[tier.ordinal()])
				.value(VALUE[tier.ordinal()])
				.open(OpenCombatAchievementsRequest.tier(tier.name()))
				.openIn("Combat Achievement Guides")
				.wikiPage("Combat_Achievements/" + tier.getLabel())
				.howTo("Press Open guide: Combat Achievement Guides opens on the " + tier.getLabel() + " tier ("
					+ done[tier.ordinal()] + "/" + total[tier.ordinal()] + " tasks done) and starts with the "
					+ "easiest task for your stats")
				.howTo("You need " + threshold + " points (you have " + points + "); tasks of any tier count")
				.howTo("Then claim the reward from Ghommal at the Warriors' Guild entrance in Burthorpe");
			if (a.getCombatLevel() < COMBAT[tier.ordinal()])
			{
				b.note("Most of these tasks expect about combat level " + COMBAT[tier.ordinal()]);
			}
			if (tier.ordinal() > 0)
			{
				b.after(stepId(CaTier.values()[tier.ordinal() - 1]));
			}
			out.add(b.build());
		}
		return out;
	}
}
