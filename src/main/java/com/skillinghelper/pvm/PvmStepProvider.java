package com.skillinghelper.pvm;

import com.skillinghelper.StartPvmGuideRequest;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.planner.api.StepKind;
import com.skillinghelper.pvm.data.ItemNeed;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.PvmLadder;
import com.skillinghelper.pvm.data.QuestReq;
import com.skillinghelper.pvm.data.StatReq;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PvM Progression's steps for the Account Planner: the first kill of each boss and raid on its ladder, needing
 * the levels the ladder recommends. Done when PvM Progression has seen a kill count or it was marked done there.
 */
public class PvmStepProvider implements PlanStepProvider
{
	@Override
	public String name()
	{
		return "PvM Progression";
	}

	@Override
	public List<PlanStep> steps(AccountState a)
	{
		Set<String> killed = new HashSet<>();
		for (String entry : split(a.getProfileConfig().apply(PvmProgressionPlugin.GROUP + "."
			+ PvmProgressionPlugin.KEY_KILL_COUNTS)))
		{
			String[] parts = entry.split(":");
			if (parts.length == 2 && !parts[1].trim().equals("0"))
			{
				killed.add(parts[0]);
			}
		}
		killed.addAll(split(a.getProfileConfig().apply(PvmProgressionPlugin.GROUP + "."
			+ PvmProgressionPlugin.KEY_MARKED_DONE)));

		List<PlanStep> out = new ArrayList<>();
		for (PvmContent c : PvmLadder.ALL)
		{
			boolean done = killed.contains(c.getId());
			if (!done && a.isHideOutleveled() && c.isOutgrownBy(a::level))
			{
				// Better bosses give more by now (the shared "hide outleveled" setting)
				continue;
			}
			out.add(step(c, done));
		}
		return out;
	}

	public static String stepId(PvmContent c)
	{
		return "boss:" + c.getId();
	}

	private static PlanStep step(PvmContent c, boolean done)
	{
		PlanStep.PlanStepBuilder b = PlanStep.builder()
			.id(stepId(c))
			.kind(StepKind.BOSS)
			.title(c.getName())
			.why(c.getWhy())
			.done(done)
			.minutes(minutes(c))
			.value(value(c))
			.wilderness(c.isWilderness())
			.open(new StartPvmGuideRequest(c.getId()))
			.openIn("PvM Progression")
			.wikiPage(c.getWikiPage())
			.howTo("Press Open guide: PvM Progression starts its guide for " + c.getName())
			.howTo("It lays out the gear and supplies to bring (and what to buy), the way there and what to do in "
				+ "the fight, step by step")
			.howTo("Done once you get your first kill");
		for (StatReq s : c.getStats())
		{
			b.skill(s.getSkill(), s.getLevel());
		}
		for (QuestReq q : c.getQuests())
		{
			if (q.isStartedIsEnough())
			{
				b.questStarted(q.getQuest());
			}
			else
			{
				b.quest(q.getQuest());
			}
		}
		for (ItemNeed i : c.getItems())
		{
			if (i.isRequired())
			{
				b.note("Needs: " + i.getName());
			}
		}
		return b.build();
	}

	private static int minutes(PvmContent c)
	{
		switch (c.getTier())
		{
			case FIRST_BOSSES:
				return 30;
			case EARLY:
				return 45;
			case MID:
				return 60;
			case LATE:
				return 90;
			default:
				return 180;
		}
	}

	private static int value(PvmContent c)
	{
		switch (c.getTier())
		{
			case FIRST_BOSSES:
				return 3;
			case EARLY:
			case ENDGAME:
				return 4;
			case RAIDS:
				return 6;
			default:
				return 5;
		}
	}

	private static List<String> split(String s)
	{
		List<String> out = new ArrayList<>();
		if (s == null)
		{
			return out;
		}
		for (String part : s.split(","))
		{
			if (!part.trim().isEmpty())
			{
				out.add(part.trim());
			}
		}
		return out;
	}
}
