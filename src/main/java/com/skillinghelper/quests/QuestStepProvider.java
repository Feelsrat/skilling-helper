package com.skillinghelper.quests;

import com.skillinghelper.OpenQuestRequest;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.planner.api.StepKind;
import com.skillinghelper.quests.data.OrderRow;
import com.skillinghelper.quests.data.QuestData;
import com.skillinghelper.quests.data.QuestInfo;
import com.skillinghelper.quests.data.Route;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Quest Order Guide's steps for the Account Planner: every quest with its real requirements from the wiki, in the
 * wiki's optimal order (which breaks ties in the planner). These replace the planner's "do this quest" stand-ins.
 */
public class QuestStepProvider implements PlanStepProvider
{
	private static final int VALUE = 2;

	@Override
	public String name()
	{
		return "Quest Order Guide";
	}

	@Override
	public List<PlanStep> steps(AccountState a)
	{
		Route route = a.getVarbit().applyAsInt(VarbitID.IRONMAN) > 0 ? Route.IRONMAN : Route.MAIN;
		Set<String> names = new LinkedHashSet<>();
		for (OrderRow row : QuestData.route(route))
		{
			if (row.getKind() == OrderRow.Kind.QUEST)
			{
				names.add(row.getRef());
			}
		}
		names.addAll(QuestData.quests().keySet());
		int qp = a.getVarp().applyAsInt(VarPlayerID.QP);
		Set<String> ignored = QuestIgnoreList.parse(a.getProfileConfig().apply(QuestIgnoreList.PROFILE_KEY));
		List<PlanStep> out = new ArrayList<>();
		Set<Quest> seen = new LinkedHashSet<>();
		for (String name : names)
		{
			QuestInfo info = QuestData.quest(name);
			Quest q = QuestData.gameQuest(name);
			if (info == null || q == null || !seen.add(q))
			{
				continue;
			}
			out.add(step(route, info, q, a, qp, ignored.contains(info.getName())));
		}
		return out;
	}

	private static PlanStep step(Route route, QuestInfo info, Quest q, AccountState a, int qp, boolean ignored)
	{
		PlanStep.PlanStepBuilder b = PlanStep.builder()
			.id(PlanStep.questId(q))
			.kind(StepKind.QUEST)
			.title(info.shortName())
			.finishes(q)
			.done(a.quest(q) == QuestState.FINISHED)
			.ignored(ignored)
			.why(why(info))
			.minutes(minutes(info.getLength()))
			.value(VALUE)
			.wilderness(QuestPlanner.isWilderness(new OrderRow(route, 0, OrderRow.Kind.QUEST, info.getName(),
				info.getName(), List.of())))
			.open(new OpenQuestRequest(info.getName()))
			.openIn("Quest Order Guide")
			.wikiPage(info.getName().replace(' ', '_') + "/Quick_guide")
			.howTo("Press Open guide: Quest Order Guide picks this quest and walks you to where it starts"
				+ (info.getStart() == null || info.getStart().isEmpty() ? "" : " (" + info.getStart() + ")"))
			.howTo("Press Start Quest Helper there: Quest Helper (a Plugin Hub plugin) guides every step of the quest");
		for (QuestInfo.SkillReq s : info.getSkills())
		{
			if (s.getSkill() == null)
			{
				b.note("Combat level " + s.getLevel());
			}
			else
			{
				b.skill(s.getSkill(), s.getLevel());
			}
		}
		for (String name : info.getQuests())
		{
			Quest need = QuestData.gameQuest(name);
			if (need != null && need != q)
			{
				b.quest(need);
			}
		}
		for (String name : info.getQuestsStarted())
		{
			Quest need = QuestData.gameQuest(name);
			if (need != null && need != q)
			{
				b.questStarted(need);
			}
		}
		if (info.getQuestPoints() > qp)
		{
			b.note(info.getQuestPoints() + " quest points (you have " + qp + ")");
		}
		if (info.getCombat() > 0)
		{
			b.note("Recommended combat level " + info.getCombat());
		}
		return b.build();
	}

	private static String why(QuestInfo info)
	{
		if (info.getRewards().isEmpty())
		{
			return null;
		}
		List<String> r = info.getRewards().subList(0, Math.min(3, info.getRewards().size()));
		return "Rewards: " + String.join("; ", r) + (info.getRewards().size() > 3 ? "; and more." : ".");
	}

	static int minutes(String length)
	{
		switch (length == null ? "" : length)
		{
			case "Very Short":
				return 15;
			case "Short":
				return 30;
			case "Long":
				return 120;
			case "Very Long":
				return 240;
			default:
				return 60;
		}
	}
}
