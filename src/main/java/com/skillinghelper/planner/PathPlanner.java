package com.skillinghelper.planner;

import com.skillinghelper.OpenMoneyMakingRequest;
import com.skillinghelper.TrainSkillRequest;
import com.skillinghelper.engine.Format;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.StepKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * Puts every provider's steps in order on the way to a maxed account.
 * <p>
 * Greedy: it keeps picking the step that gives the most for the time it costs, counting the training (and any
 * quest or earlier step) needed to reach it, and a bonus for steps other steps wait on. Picking a step plays it out:
 * whatever it needs comes first in the plan ("Train Agility to 42", a quest), then the step, then the levels and
 * quests it gives count as had for everything after. Pure, so it can be tested without the client.
 */
public final class PathPlanner
{
	/**
	 * Hours a step of unknown length counts as, and the floor under every step's cost.
	 */
	static final double SHORT_STEP_HOURS = 0.25;
	/**
	 * How many dependants count towards a step's bonus.
	 */
	static final int MAX_FANOUT = 4;
	/**
	 * Value bonus per step that waits on this one.
	 */
	static final int FANOUT_BONUS = 1;
	/**
	 * How deep the cost of reaching a step looks through what it needs.
	 */
	private static final int COST_DEPTH = 3;
	/**
	 * Stops a broken provider from planning forever.
	 */
	private static final int MAX_PLAN = 2000;
	/**
	 * Extra hours a training step counts as when weighing it: getting set up and there. Stops the planner from
	 * filling the start of the plan with ten-minute bits of training.
	 */
	static final double SKILL_SETUP_HOURS = 0.5;
	static final int QUEST_STAND_IN_MINUTES = 60;
	static final String QUEST_HELPER = "Quest Helper";

	@Value
	public static class Planned
	{
		PlanStep step;
		/**
		 * Why it's here at this point, e.g. "Needed for Varrock Medium diary", or null when it's here for itself.
		 */
		String reason;
		/**
		 * Rough hours it takes, counting from the levels the plan reaches before it.
		 */
		double hours;
	}

	@Value
	public static class Plan
	{
		List<Planned> steps;
		double totalHours;
		/**
		 * Per kind: {done, total} of the steps the providers know about.
		 */
		Map<StepKind, int[]> progress;
		/**
		 * Steps left out because they're in the Wilderness (or wait on one that is) and it's turned off.
		 */
		List<String> unreachable;
	}

	@Value
	public static class Settings
	{
		Set<String> skipped;
		Set<StepKind> hiddenKinds;
	}

	private final AccountState account;
	private final SkillHours skillHours;
	private final Settings settings;

	private final Map<String, PlanStep> byId = new LinkedHashMap<>();
	private final Map<String, Integer> order = new HashMap<>();
	private final Set<String> remaining = new HashSet<>();
	private final Map<String, Integer> fanout = new HashMap<>();
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private final Set<Quest> finished = new HashSet<>();
	private final Set<Quest> started = new HashSet<>();
	private final Set<String> doneIds = new HashSet<>();
	private final List<Planned> out = new ArrayList<>();
	private final Set<String> emitting = new HashSet<>();
	/**
	 * Quest steps the planner made up because no provider has them.
	 */
	private final Set<String> standIns = new HashSet<>();
	private Set<String> excluded = new HashSet<>();
	private long coins;
	private int moneySteps;

	private PathPlanner(AccountState account, SkillHours skillHours, Settings settings)
	{
		this.account = account;
		this.skillHours = skillHours;
		this.settings = settings;
	}

	public static Plan plan(Collection<PlanStep> provided, AccountState account, SkillHours hours, Settings settings)
	{
		return new PathPlanner(account, hours, settings).run(provided);
	}

	private Plan run(Collection<PlanStep> provided)
	{
		for (Skill s : Skill.values())
		{
			levels.put(s, account.level(s));
		}
		for (Map.Entry<Quest, QuestState> e : account.getQuests().entrySet())
		{
			if (e.getValue() == QuestState.FINISHED)
			{
				finished.add(e.getKey());
			}
			if (e.getValue() != QuestState.NOT_STARTED)
			{
				started.add(e.getKey());
			}
		}
		coins = account.isBankKnown() ? account.coins() : -1;

		Map<StepKind, int[]> progress = new EnumMap<>(StepKind.class);
		Set<String> excluded = new HashSet<>();
		for (PlanStep s : provided)
		{
			if (byId.containsKey(s.getId()))
			{
				continue;
			}
			byId.put(s.getId(), s);
			order.put(s.getId(), order.size());
			boolean done = isDone(s);
			int[] p = progress.computeIfAbsent(s.getKind(), k -> new int[2]);
			p[1]++;
			if (done)
			{
				p[0]++;
				doneIds.add(s.getId());
			}
			else if (settings.getSkipped().contains(s.getId()) || settings.getHiddenKinds().contains(s.getKind()))
			{
				// Left to the player: counts as had for whatever comes after it
				doneIds.add(s.getId());
			}
			else if (s.isWilderness() && !account.isAllowWilderness())
			{
				excluded.add(s.getId());
			}
			else
			{
				remaining.add(s.getId());
			}
		}
		this.excluded = excluded;
		List<String> unreachable = dropUnreachable(excluded);
		addQuestStandIns();
		countFanout();

		while (!remaining.isEmpty() && out.size() < MAX_PLAN)
		{
			String best = null;
			double bestScore = -1;
			Map<Skill, Integer> lowestSkillStep = lowestSkillSteps();
			for (String id : remaining)
			{
				PlanStep s = byId.get(id);
				if (standIns.contains(id))
				{
					// Planned only as part of a step that needs it: without its own requirements its time is a guess
					continue;
				}
				if (s.getKind() == StepKind.SKILL && s.getTrainSkill() != null
					&& lowestSkillStep.get(s.getTrainSkill()) < s.getTrainLevel() && s.getValue() <= 1)
				{
					// A later chunk of the same skill: never better than the next one
					continue;
				}
				double score = score(s);
				if (score > bestScore || (score == bestScore && order(id) < order(best)))
				{
					best = id;
					bestScore = score;
				}
			}
			if (best == null)
			{
				break;
			}
			emit(byId.get(best), null);
		}

		double total = 0;
		for (Planned p : out)
		{
			total += p.getHours();
		}
		return new Plan(out, total, progress, unreachable);
	}

	// ---- Setting up ----

	private boolean isDone(PlanStep s)
	{
		if (s.isDone())
		{
			return true;
		}
		if (s.getKind() == StepKind.SKILL && s.getTrainSkill() != null)
		{
			return levels.get(s.getTrainSkill()) >= s.getTrainLevel();
		}
		return s.getFinishes() != null && finished.contains(s.getFinishes());
	}

	/**
	 * Drops every remaining step that waits on a left-out one (e.g. a later tier of a Wilderness diary, or a quest
	 * that needs a Wilderness quest).
	 */
	private List<String> dropUnreachable(Set<String> excluded)
	{
		boolean changed = true;
		while (changed)
		{
			changed = false;
			for (String id : new ArrayList<>(remaining))
			{
				PlanStep s = byId.get(id);
				List<String> deps = new ArrayList<>(s.getAfter());
				s.getQuests().forEach(q -> deps.add(PlanStep.questId(q)));
				s.getQuestsStarted().forEach(q -> deps.add(PlanStep.questId(q)));
				for (String dep : deps)
				{
					if (excluded.contains(dep))
					{
						remaining.remove(id);
						excluded.add(id);
						changed = true;
						break;
					}
				}
			}
		}
		List<String> titles = new ArrayList<>();
		for (Map.Entry<String, PlanStep> e : byId.entrySet())
		{
			if (excluded.contains(e.getKey()))
			{
				titles.add(e.getValue().getTitle());
			}
		}
		return titles;
	}

	/**
	 * Adds a step for every quest a remaining step needs that no provider has a step for.
	 */
	private void addQuestStandIns()
	{
		for (String id : new ArrayList<>(remaining))
		{
			PlanStep s = byId.get(id);
			for (Quest q : s.getQuests())
			{
				questStandIn(q);
			}
			for (Quest q : s.getQuestsStarted())
			{
				if (!started.contains(q))
				{
					questStandIn(q);
				}
			}
		}
	}

	private void questStandIn(Quest q)
	{
		String id = PlanStep.questId(q);
		if (settings.getSkipped().contains(id) || settings.getHiddenKinds().contains(StepKind.QUEST))
		{
			// The player does quests their own way: count it as had
			finished.add(q);
			started.add(q);
			return;
		}
		if (finished.contains(q) || byId.containsKey(id))
		{
			if (byId.containsKey(id) && !doneIds.contains(id) && !finished.contains(q) && !excluded.contains(id))
			{
				remaining.add(id);
			}
			return;
		}
		PlanStep s = PlanStep.builder()
			.id(id)
			.kind(StepKind.QUEST)
			.title(q.getName())
			.finishes(q)
			.why("Needed by later steps.")
			.howTo(QUEST_HELPER + " (a Plugin Hub plugin) guides every quest step by step: install it, open its panel, "
				+ "search \"" + q.getName() + "\" and press Start")
			.howTo("Or follow the wiki's quick guide (Wiki button)")
			.note("Its own quest and level requirements aren't planned yet; " + QUEST_HELPER + " lists them")
			.minutes(QUEST_STAND_IN_MINUTES)
			.value(2)
			.wikiPage(q.getName().replace(' ', '_') + "/Quick_guide")
			.build();
		byId.put(id, s);
		order.put(id, order.size());
		remaining.add(id);
		standIns.add(id);
	}

	private void countFanout()
	{
		for (String id : remaining)
		{
			PlanStep s = byId.get(id);
			for (String dep : s.getAfter())
			{
				fanout.merge(dep, 1, Integer::sum);
			}
			for (Quest q : s.getQuests())
			{
				fanout.merge(PlanStep.questId(q), 1, Integer::sum);
			}
			for (Quest q : s.getQuestsStarted())
			{
				fanout.merge(PlanStep.questId(q), 1, Integer::sum);
			}
		}
	}

	private int order(String id)
	{
		return id == null ? Integer.MAX_VALUE : order.getOrDefault(id, Integer.MAX_VALUE);
	}

	private Map<Skill, Integer> lowestSkillSteps()
	{
		Map<Skill, Integer> lowest = new EnumMap<>(Skill.class);
		for (String id : remaining)
		{
			PlanStep s = byId.get(id);
			if (s.getKind() == StepKind.SKILL && s.getTrainSkill() != null)
			{
				lowest.merge(s.getTrainSkill(), s.getTrainLevel(), Math::min);
			}
		}
		return lowest;
	}

	// ---- Weighing ----

	double score(PlanStep s)
	{
		int bonus = Math.min(MAX_FANOUT, fanout.getOrDefault(s.getId(), 0)) * FANOUT_BONUS;
		double setup = s.getKind() == StepKind.SKILL ? SKILL_SETUP_HOURS : 0;
		return (s.getValue() + bonus) / (cost(s, COST_DEPTH, new HashSet<>()) + setup + SHORT_STEP_HOURS);
	}

	/**
	 * Hours to reach and do a step from where the plan is now.
	 */
	private double cost(PlanStep s, int depth, Set<String> seen)
	{
		if (!seen.add(s.getId()))
		{
			return 0;
		}
		double hours = ownHours(s);
		for (Map.Entry<Skill, Integer> e : s.getSkills().entrySet())
		{
			hours += skillHours.hours(e.getKey(), levels.get(e.getKey()), e.getValue());
		}
		if (depth > 0)
		{
			for (PlanStep dep : unmetDependencies(s))
			{
				hours += cost(dep, depth - 1, seen);
			}
		}
		return hours;
	}

	private double ownHours(PlanStep s)
	{
		if (s.getKind() == StepKind.SKILL && s.getTrainSkill() != null)
		{
			return skillHours.hours(s.getTrainSkill(), levels.get(s.getTrainSkill()), s.getTrainLevel());
		}
		return s.getMinutes() > 0 ? s.getMinutes() / 60.0 : SHORT_STEP_HOURS;
	}

	private List<PlanStep> unmetDependencies(PlanStep s)
	{
		List<PlanStep> deps = new ArrayList<>();
		for (String id : s.getAfter())
		{
			if (remaining.contains(id))
			{
				deps.add(byId.get(id));
			}
		}
		for (Quest q : s.getQuests())
		{
			addQuestDependency(q, deps);
		}
		for (Quest q : s.getQuestsStarted())
		{
			if (!started.contains(q))
			{
				addQuestDependency(q, deps);
			}
		}
		return deps;
	}

	private void addQuestDependency(Quest q, List<PlanStep> deps)
	{
		String id = PlanStep.questId(q);
		if (!finished.contains(q) && remaining.contains(id))
		{
			deps.add(byId.get(id));
		}
	}

	// ---- Playing it out ----

	/**
	 * Adds the step to the plan, after everything it needs.
	 */
	private void emit(PlanStep s, String reason)
	{
		if (!remaining.contains(s.getId()) || !emitting.add(s.getId()))
		{
			return;
		}
		String forThis = "Needed for " + s.getTitle();
		for (PlanStep dep : unmetDependencies(s))
		{
			emit(dep, forThis);
		}
		for (Map.Entry<Skill, Integer> e : s.getSkills().entrySet())
		{
			if (levels.get(e.getKey()) >= e.getValue())
			{
				continue;
			}
			if (settings.getSkipped().contains(PlanStep.skillId(e.getKey(), e.getValue())))
			{
				// The player skipped this training: count the level as had
				raise(e.getKey(), e.getValue());
			}
			else
			{
				emit(trainingStep(e.getKey(), e.getValue()), forThis);
			}
		}
		if (s.getGpCost() > 0 && coins >= 0 && coins < s.getGpCost())
		{
			out.add(new Planned(moneyStep(s.getGpCost() - coins, s), forThis, 0));
			coins = s.getGpCost();
		}
		double hours = ownHours(s);
		out.add(new Planned(s, reason, hours));
		remaining.remove(s.getId());
		emitting.remove(s.getId());
		doneIds.add(s.getId());
		if (coins >= 0)
		{
			coins = Math.max(0, coins - s.getGpCost());
		}
		if (s.getKind() == StepKind.SKILL && s.getTrainSkill() != null)
		{
			raise(s.getTrainSkill(), s.getTrainLevel());
		}
		if (s.getFinishes() != null)
		{
			finished.add(s.getFinishes());
			started.add(s.getFinishes());
		}
	}

	/**
	 * The plan reached this level: skill steps at or below it are done on the way.
	 */
	private void raise(Skill skill, int level)
	{
		levels.merge(skill, level, Math::max);
		remaining.removeIf(id ->
		{
			PlanStep s = byId.get(id);
			return s.getKind() == StepKind.SKILL && s.getTrainSkill() == skill && s.getTrainLevel() <= levels.get(skill);
		});
	}

	/**
	 * A provider's step for this level if there is one, else a plain "Train X to N" that opens Skilling Helper.
	 */
	private PlanStep trainingStep(Skill skill, int level)
	{
		String id = PlanStep.skillId(skill, level);
		PlanStep s = byId.get(id);
		if (s == null)
		{
			s = trainStep(skill, level, null, 0);
			byId.put(id, s);
			order.put(id, order.size());
		}
		remaining.add(id);
		return s;
	}

	/**
	 * A step that trains a skill to a level with the Skilling Helper guide.
	 */
	public static PlanStep trainStep(Skill skill, int level, String why, int value)
	{
		PlanStep.PlanStepBuilder b = PlanStep.builder()
			.id(PlanStep.skillId(skill, level))
			.kind(StepKind.SKILL)
			.title("Train " + skill.getName() + " to " + level)
			.why(why)
			.trainSkill(skill)
			.trainLevel(level)
			.value(value)
			.wikiPage(skill.getName() + "_training");
		if (skill == Skill.HITPOINTS)
		{
			return b.howTo("Hitpoints goes up by itself while you train Attack, Strength, Defence, Ranged or Magic")
				.howTo("Press Open guide for the Attack guide in Skilling Helper, or train any of the others")
				.open(new TrainSkillRequest(Skill.ATTACK, level))
				.openIn("Skilling Helper")
				.build();
		}
		return b.howTo("Press Open guide: Skilling Helper opens on " + skill.getName() + " with the goal set to " + level)
			.howTo("Pick a method from its list (sorted by your setting there), press Start and follow the steps")
			.open(new TrainSkillRequest(skill, level))
			.openIn("Skilling Helper")
			.build();
	}

	private PlanStep moneyStep(long shortBy, PlanStep forStep)
	{
		moneySteps++;
		return PlanStep.builder()
			.id("money:" + forStep.getId() + ":" + moneySteps)
			.kind(StepKind.MONEY)
			.title("Make about " + Format.gp(shortBy))
			.why(forStep.getTitle() + " costs about " + Format.gp(forStep.getGpCost())
				+ " in items and fees, more than you have.")
			.howTo("Press Open guide: Money Making lists the money makers you can do, best gp/hr first")
			.howTo("Pick one, press Start and follow the steps until you have the gold")
			.open(new OpenMoneyMakingRequest(shortBy))
			.openIn("Money Making")
			.value(0)
			.build();
	}
}
