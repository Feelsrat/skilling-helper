package com.skillinghelper.clue;

import com.skillinghelper.OpenClueGuideRequest;
import com.skillinghelper.clue.data.ClueTier;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.planner.api.StepKind;
import java.util.ArrayList;
import java.util.List;

/**
 * Clue Scroll Guide's steps for the Account Planner: finish a first clue of each tier. Done once the Clue Scroll
 * Guide has seen the game's "You have completed N ... Treasure Trails" message for that tier on this account (saved
 * per account), so tiers done before the plugin was installed show as done after the next casket of that tier.
 */
public class ClueStepProvider implements PlanStepProvider
{
	/**
	 * Rough minutes to get a clue and do it, by tier.
	 */
	private static final int[] MINUTES = {15, 25, 35, 50, 70, 120};
	private static final int[] VALUE = {3, 3, 4, 5, 5, 6};

	@Override
	public String name()
	{
		return "Clue Scroll Guide";
	}

	@Override
	public List<PlanStep> steps(AccountState a)
	{
		List<PlanStep> out = new ArrayList<>();
		for (ClueTier t : ClueTier.values())
		{
			String lower = t.getLabel().toLowerCase();
			PlanStep.PlanStepBuilder b = PlanStep.builder()
				.id(stepId(t))
				.kind(StepKind.CLUE)
				.title("Do a " + lower + " clue scroll")
				.why(t.getRewardKinds().get(0) + (t.getMasterChance() != null
					? " Each casket also has a " + t.getMasterChance() + " chance of a master clue." : ""))
				.done(ClueScrollGuidePlugin.completed(a.getProfileConfig(), t) > 0)
				.minutes(MINUTES[t.ordinal()])
				.value(VALUE[t.ordinal()])
				.open(new OpenClueGuideRequest(t.key()))
				.openIn("Clue Scroll Guide")
				.wikiPage(t.getWikiPage())
				.howTo("Press Open guide: Clue Scroll Guide opens on " + lower + " clues; press Start guide")
				.howTo("It guides you to get a clue (or opens one you already have), then RuneLite's Clue Scroll plugin "
					+ "shows each step")
				.howTo("Open the reward casket at the end");
			if (t.isMembers() && !a.isMembers())
			{
				b.note("A members world");
			}
			if (t.isWildernessSteps())
			{
				b.note("Some steps can be in the Wilderness (you can drop the clue instead)");
			}
			if (t == ClueTier.MASTER)
			{
				for (ClueTier lowerTier : List.of(ClueTier.EASY, ClueTier.MEDIUM, ClueTier.HARD, ClueTier.ELITE))
				{
					b.after(stepId(lowerTier));
				}
			}
			out.add(b.build());
		}
		return out;
	}

	public static String stepId(ClueTier t)
	{
		return "clue:" + t.name();
	}
}
