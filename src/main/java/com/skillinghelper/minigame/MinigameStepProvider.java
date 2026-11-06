package com.skillinghelper.minigame;

import com.skillinghelper.OpenMinigameRequest;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.minigame.data.Minigame;
import com.skillinghelper.minigame.data.Minigames;
import com.skillinghelper.minigame.data.Reward;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.planner.api.StepKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Minigame Guides' steps for the Account Planner: one per minigame, "get its rewards worth chasing", done once
 * every one of them that can be checked is owned. Minigames whose rewards can't be checked (xp only) and the
 * combat ones PvM Progression already plans are left out.
 */
public class MinigameStepProvider implements PlanStepProvider
{
	/**
	 * How much the rewards move an account on (1-10) and rough minutes to earn them all, per minigame id.
	 */
	private static final Map<String, int[]> WEIGHT = Map.ofEntries(
		Map.entry("tempoross", new int[]{7, 300}),
		Map.entry("motherlode", new int[]{6, 360}),
		Map.entry("tithe_farm", new int[]{6, 300}),
		Map.entry("gotr", new int[]{6, 900}),
		Map.entry("wintertodt", new int[]{5, 600}),
		Map.entry("mage_training_arena", new int[]{6, 600}),
		Map.entry("hallowed_sepulchre", new int[]{5, 900}),
		Map.entry("rogues_den", new int[]{5, 120}),
		Map.entry("mahogany_homes", new int[]{5, 300}),
		Map.entry("giants_foundry", new int[]{4, 900}),
		Map.entry("fishing_trawler", new int[]{3, 240}),
		Map.entry("soul_wars", new int[]{4, 300}),
		Map.entry("mastering_mixology", new int[]{4, 900}),
		Map.entry("volcanic_mine", new int[]{3, 1200}),
		Map.entry("vale_totems", new int[]{3, 300}),
		Map.entry("castle_wars", new int[]{1, 1800}),
		Map.entry("pyramid_plunder", new int[]{3, 300}),
		Map.entry("brimhaven_arena", new int[]{1, 600}));

	@Override
	public String name()
	{
		return "Minigame Guides";
	}

	@Override
	public List<PlanStep> steps(AccountState a)
	{
		DiaryPlayer player = DiaryPlayer.builder()
			.levels(a.getLevels())
			.combatLevel(a.getCombatLevel())
			.quests(a.getQuests())
			.varbit(a.getVarbit())
			.varp(a.getVarp())
			.carried(a.getOwned())
			.bank(Collections.emptyMap())
			.members(true)
			.allowWilderness(a.isAllowWilderness())
			.build();
		List<PlanStep> out = new ArrayList<>();
		for (Minigame m : Minigames.all())
		{
			int[] owned = MinigameGuide.ownedCount(m, player);
			if (owned[1] == 0 || m.getPvmContent() != null)
			{
				continue;
			}
			int[] weight = WEIGHT.getOrDefault(m.getId(), new int[]{3, 300});
			List<String> wanted = new ArrayList<>();
			for (Reward r : m.getRewards())
			{
				if (r.isWorth() && r.checkable() && !MinigameGuide.owned(r, player))
				{
					wanted.add(r.getName());
				}
			}
			PlanStep.PlanStepBuilder b = PlanStep.builder()
				.id(stepId(m))
				.kind(StepKind.MINIGAME)
				.title(m.getName() + " rewards")
				.why(wanted.isEmpty() ? "All its rewards worth chasing owned"
					: "Still to get: " + String.join(", ", wanted))
				.howTo("Open the Minigame Guides panel on " + m.getName() + " and press Start guide.")
				.done(owned[0] == owned[1])
				.skills(m.getSkills())
				.quests(m.getQuests())
				.questsStarted(m.getQuestsStarted())
				.notes(m.getNotes())
				.minutes(weight[1])
				.value(weight[0])
				.wilderness(m.isWilderness())
				.open(new OpenMinigameRequest(m.getId()))
				.openIn("Minigame Guides")
				.wikiPage(m.getWikiPage());
			if (m.getCombat() > 0)
			{
				b.note("Combat level " + m.getCombat());
			}
			if (m.getTotalLevel() > 0)
			{
				b.note("Total level " + m.getTotalLevel());
			}
			out.add(b.build());
		}
		return out;
	}

	public static String stepId(Minigame m)
	{
		return "minigame:" + m.getId();
	}
}
