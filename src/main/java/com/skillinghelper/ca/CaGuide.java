package com.skillinghelper.ca;

import com.skillinghelper.ca.data.CaMonster;
import com.skillinghelper.ca.data.CaMonsters;
import com.skillinghelper.ca.data.CaTask;
import com.skillinghelper.ca.data.CaTier;
import com.skillinghelper.data.Location;
import com.skillinghelper.diary.DiaryGuide;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.diary.data.DiaryStage;
import com.skillinghelper.diary.data.DiaryTask;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * Turns a Combat Achievement task into the next thing to do, Quest Helper style, for someone who has never fought
 * the boss: get what it needs (bank, Grand Exchange, shops), walk to the entrance, go in, then the fight with the
 * task's rule and the wiki's tips. Recomputed every tick. Banking, buying and walking reuse the diary guide's steps.
 */
public final class CaGuide
{
	/**
	 * Ghommal hands out the rewards, by the Warriors' Guild entrance in Burthorpe.
	 */
	static final Location GHOMMAL = Location.of("Ghommal at the Warriors' Guild entrance", 2877, 3546, 4);
	static final int MAX_TIER_BLOCKERS = 6;

	private static final Map<Integer, DiaryTask> DIARY_TASKS = new ConcurrentHashMap<>();

	/**
	 * What the guide knows besides the player: kill counts, what's around them and where they've been.
	 */
	@Value
	@Builder
	public static class Context
	{
		DiaryPlayer player;
		CaRanker.Power power;
		@Builder.Default
		Map<String, Integer> kc = Map.of();
		/**
		 * The task's boss (or someone it names) is on screen close by.
		 */
		boolean nearMonster;
		/**
		 * The player reached the entrance for this task and hasn't wandered off since.
		 */
		boolean arrived;
		/**
		 * Points the player has, and what each tier's reward needs.
		 */
		int points;
		@Builder.Default
		Map<CaTier, Integer> thresholds = Map.of();
		/**
		 * Tiers whose hilt the player owns (rewards claimed). Empty until the bank was seen.
		 */
		@Builder.Default
		Set<CaTier> claimed = Set.of();
	}

	private CaGuide()
	{
	}

	/**
	 * The task as a diary-style task, so the diary guide's requirement, banking, buying and walking steps work on it.
	 */
	public static DiaryTask diaryTask(CaTask t)
	{
		return DIARY_TASKS.computeIfAbsent(t.getId(), id -> buildDiaryTask(t));
	}

	private static DiaryTask buildDiaryTask(CaTask t)
	{
		CaMonster m = CaMonsters.get(t.getMonster());
		DiaryTask.DiaryTaskBuilder b = DiaryTask.builder()
			.id(t.key())
			.number(t.getId())
			.name(t.getName())
			.text(t.getText())
			.varp(t.varp())
			.bit(t.bit())
			.freeToPlay(!t.isMembers())
			.freeWorld(t.freeWorld());
		Map<Skill, Integer> levels = new java.util.EnumMap<>(Skill.class);
		levels.putAll(t.getLevels());
		if (m != null)
		{
			m.getSkills().forEach((s, l) -> levels.merge(s, l, Math::max));
			m.getQuests().forEach(b::quest);
			m.getQuestsStarted().forEach(b::questStarted);
			m.getNotes().forEach(b::note);
			m.getItems().forEach(b::item);
			b.wilderness(m.isWilderness());
		}
		levels.forEach(b::skill);
		for (net.runelite.api.Quest q : t.getQuests())
		{
			if (m == null || !m.getQuests().contains(q))
			{
				b.quest(q);
			}
		}
		for (com.skillinghelper.diary.data.DiaryItem item : CaExtras.items(t.getId()))
		{
			b.item(item);
		}
		CaExtras.notes(t.getId()).forEach(b::note);
		DiaryStage.DiaryStageBuilder stage = DiaryStage.builder()
			.title(m == null || "None".equals(m.getName()) ? t.getName() : "Go to " + m.getName());
		if (m != null)
		{
			stage.at(m.getAt());
			m.getEnter().forEach(stage::line);
			m.getObjects().forEach(stage::object);
		}
		b.stage(stage.build());
		return b.build();
	}

	// ---- Choosing a task ----

	/**
	 * The task to guide next: the picked one, else the easiest the player can do now in {@code ranked}.
	 */
	public static CaRanker.Ranked next(List<CaRanker.Ranked> ranked, Set<String> skipped, String picked)
	{
		for (CaRanker.Ranked r : ranked)
		{
			if (r.getTask().key().equals(picked) && r.getFit() != CaRanker.Fit.DONE)
			{
				return r;
			}
		}
		for (CaRanker.Ranked r : ranked)
		{
			boolean doable = r.getFit() == CaRanker.Fit.READY || r.getFit() == CaRanker.Fit.STRETCH;
			if (doable && !skipped.contains(r.getTask().key()) && r.getMonster() != null)
			{
				return r;
			}
		}
		return null;
	}

	/**
	 * A tier whose reward the player has the points for but hasn't claimed (no hilt in the bank), lowest first.
	 * Null when there's none, or before the bank has been seen.
	 */
	static CaTier unclaimed(Context c)
	{
		if (c.getPlayer().getBank() == null)
		{
			return null;
		}
		for (CaTier t : CaTier.values())
		{
			int need = c.getThresholds().getOrDefault(t, t.getFallbackThreshold());
			if (c.getPoints() >= need && !c.getClaimed().contains(t))
			{
				return t;
			}
		}
		return null;
	}

	// ---- Steps ----

	/**
	 * The next step of the running guide over {@code ranked} (one tier, or all tasks).
	 */
	public static DiaryStep resolve(List<CaRanker.Ranked> ranked, Context c, Set<String> skipped, String picked)
	{
		CaTier claim = unclaimed(c);
		if (claim != null)
		{
			return claimStep(claim, c);
		}
		CaRanker.Ranked next = next(ranked, skipped, picked);
		if (next == null)
		{
			return nothingLeft(ranked, skipped);
		}
		return resolve(next, c);
	}

	/**
	 * The next step for one task.
	 */
	public static DiaryStep resolve(CaRanker.Ranked r, Context c)
	{
		CaTask t = r.getTask();
		DiaryTask dt = diaryTask(t);
		DiaryPlayer p = c.getPlayer();
		if (!DiaryGuide.missing(dt, p).isEmpty())
		{
			return DiaryGuide.resolve(dt, p);
		}
		if (c.isNearMonster() || c.isArrived())
		{
			return fightStep(r, c, c.isNearMonster());
		}
		DiaryStep s = DiaryGuide.resolve(dt, p);
		if (s.getKind() == DiaryStep.Kind.DO)
		{
			return fightStep(r, c, false);
		}
		if (s.getKind() == DiaryStep.Kind.TRAVEL && r.getMonster() != null)
		{
			List<String> lines = new ArrayList<>(s.getLines());
			lines.add("Task: " + t.getText());
			return s.toBuilder().clearLines().lines(lines).build();
		}
		return s;
	}

	/**
	 * At the entrance or the boss: how to get in, the task's rule, how this kind of task works, and the wiki's tips.
	 */
	static DiaryStep fightStep(CaRanker.Ranked r, Context c, boolean atBoss)
	{
		CaTask t = r.getTask();
		CaMonster m = r.getMonster();
		boolean general = m == null || "None".equals(m.getName());
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder()
			.kind(DiaryStep.Kind.DO)
			.taskKey(t.key())
			.title(general ? t.getName() : t.getName() + " (" + m.getName() + ")");
		int n = 1;
		if (m != null && !atBoss)
		{
			for (String line : m.getEnter())
			{
				b.line(n++ + ". " + line);
			}
		}
		if (m != null && !general && !m.isNoAttack())
		{
			b.line(n + ". Click " + m.getName() + " (outlined) and choose Attack. Eat when your Hitpoints drop "
				+ "below half.");
		}
		b.line("Task: " + t.getText());
		int need = t.count();
		int kc = CaRanker.kcFor(m, t, c.getKc());
		if (need > 1 && kc > 0)
		{
			b.line("Your kill count so far: " + kc + " of " + need + ".");
		}
		CaTips.forType(t).forEach(b::line);
		for (String tip : CaTips.wiki(t))
		{
			b.line("Tip: " + tip);
		}
		if (m != null && m.getBring() != null)
		{
			b.line(m.getBring() + ".");
		}
		else if (m != null && m.getPvmId() != null)
		{
			b.line("For an exact gear and supply list from your bank, press \"Full boss guide\" below.");
		}
		else if (!general)
		{
			b.line("Bring your best gear, food and potions.");
		}
		if (r.getFit() == CaRanker.Fit.STRETCH)
		{
			b.line("⚠ " + r.getReason() + ": bring extra food and leave if it goes badly.");
		}
		b.line("When the game says the task is complete, the guide moves on by itself.");
		if (m != null)
		{
			b.npcs(new HashSet<>(m.getNpcs()));
			b.objects(new HashSet<>(m.getObjects()));
		}
		return b.build();
	}

	static DiaryStep claimStep(CaTier tier, Context c)
	{
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder()
			.kind(DiaryStep.Kind.CLAIM)
			.title("Claim your " + tier.getLabel() + " Combat Achievements reward")
			.npcs(Set.of("Ghommal", "Laidee Gnonock"));
		if (!GHOMMAL.contains(c.getPlayer().getLocation()))
		{
			b.destination(GHOMMAL).line("Walk to Ghommal by the Warriors' Guild entrance in Burthorpe (follow the "
				+ "arrow).");
		}
		b.line("1. Click Ghommal (outlined) and choose Talk-to (or Rewards).");
		b.line("2. Claim the " + tier.getLabel() + " rewards: Ghommal's hilt and an antique lamp.");
		b.line("Have two free inventory slots. You have " + c.getPoints() + " points; "
			+ tier.getLabel() + " needs " + c.getThresholds().getOrDefault(tier, tier.getFallbackThreshold()) + ".");
		b.line("Keep the hilt in your bank: the guide uses it to know you've claimed this tier.");
		return b.build();
	}

	static DiaryStep nothingLeft(List<CaRanker.Ranked> ranked, Set<String> skipped)
	{
		boolean anyLeft = false;
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder().kind(DiaryStep.Kind.LOCKED);
		int shown = 0;
		for (CaRanker.Ranked r : ranked)
		{
			if (r.getFit() == CaRanker.Fit.DONE)
			{
				continue;
			}
			anyLeft = true;
			if (shown++ >= MAX_TIER_BLOCKERS)
			{
				continue;
			}
			if (skipped.contains(r.getTask().key()))
			{
				b.line("• " + r.getTask().getName() + ": skipped (click it in the list to do it)");
			}
			else
			{
				b.line("✘ " + r.getTask().getName() + ": " + r.getReason());
			}
		}
		if (!anyLeft)
		{
			return b.title("Every task here is done").line("✔ Pick another tier in the panel.").build();
		}
		b.title("Nothing here you're ready for yet");
		b.line("Click a task in the list to be guided through it anyway, or train first.");
		return b.build();
	}
}
