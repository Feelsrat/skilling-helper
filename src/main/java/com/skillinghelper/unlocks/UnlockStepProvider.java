package com.skillinghelper.unlocks;

import com.skillinghelper.OpenUnlockRequest;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStepProvider;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.planner.api.StepKind;
import com.skillinghelper.unlocks.data.QuestPart;
import com.skillinghelper.unlocks.data.Unlock;
import com.skillinghelper.unlocks.data.Unlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Account Unlocks' steps for the Account Planner: graceful, the rune pouch, fairy rings... with the quests and
 * diary tier on the way as requirements, so the planner fits them in. Unlocks that are only a quest or only a diary
 * reward are left out: the quest or diary step already covers them.
 */
public class UnlockStepProvider implements PlanStepProvider
{
	private static final int VALUE = 6;

	@Override
	public String name()
	{
		return "Account Unlocks";
	}

	@Override
	public List<PlanStep> steps(AccountState a)
	{
		Set<String> claimed = DiaryStepProvider.claimedTiers(a.getVarbit());
		DiaryPlayer player = DiaryPlayer.builder()
			.levels(a.getLevels())
			.combatLevel(a.getCombatLevel())
			.quests(a.getQuests())
			.claimed(claimed)
			.varbit(a.getVarbit())
			.varp(a.getVarp())
			.carried(a.getOwned())
			.bank(a.isBankKnown() ? Collections.emptyMap() : null)
			.members(true)
			.allowWilderness(a.isAllowWilderness())
			.buyPrice(a.getPrice())
			.build();
		UnlockPlayer up = UnlockPlayer.builder().player(player).build();
		List<PlanStep> out = new ArrayList<>();
		for (Unlock u : Unlocks.all())
		{
			if (u.getGuide() == null && u.getGrind() == null)
			{
				continue;
			}
			out.add(step(u, up));
		}
		return out;
	}

	private static PlanStep step(Unlock u, UnlockPlayer up)
	{
		PlanStep.PlanStepBuilder b = PlanStep.builder()
			.id("unlock:" + u.getId())
			.kind(StepKind.UNLOCK)
			.title(u.getName())
			.done(UnlockGuide.done(u, up))
			.value(VALUE)
			.minutes(u.getGrind() != null ? 120 : 30)
			.open(new OpenUnlockRequest(u.getId()))
			.openIn("Account Unlocks")
			.wikiPage(u.getWikiPage())
			.howTo("Press Open guide: Account Unlocks picks " + u.getName() + " and starts its guide")
			.howTo("It walks you through what's left: quests go to Quest Helper, the rest it guides step by step");
		List<String> gives = u.getGives();
		if (!gives.isEmpty())
		{
			b.why(String.join("; ", gives.subList(0, Math.min(2, gives.size()))) + (gives.size() > 2 ? "; and more."
				: "."));
		}
		for (QuestPart q : u.getQuests())
		{
			if (q.getVarbit() > 0 && up.getPlayer().getVarbit().applyAsInt(q.getVarbit()) >= q.getVarbitMin())
			{
				continue;
			}
			b.quest(q.getQuest());
			q.getSkills().forEach(b::skill);
		}
		if (u.isDiary())
		{
			b.after(DiaryStepProvider.stepId(u.getDiaryRegion(), u.getDiaryTier()));
		}
		if (u.getGrind() != null && u.getGrind().getSkill() != null && u.getGrind().getMinLevel() > 0)
		{
			b.skill(u.getGrind().getSkill(), u.getGrind().getMinLevel());
		}
		DiaryTask guide = u.getGuide();
		if (guide != null)
		{
			guide.getSkills().forEach(b::skill);
			guide.getQuests().forEach(b::quest);
			guide.getQuestsStarted().forEach(b::questStarted);
			guide.getNotes().forEach(b::note);
			b.wilderness(guide.isWilderness());
		}
		return b.build();
	}
}
