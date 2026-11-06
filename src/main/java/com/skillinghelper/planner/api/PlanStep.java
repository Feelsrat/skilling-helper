package com.skillinghelper.planner.api;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * One thing on the way to a maxed account: a quest, a skill level, a diary tier, a boss, an unlock... Made by a
 * {@link PlanStepProvider}; the planner puts the steps in order and fills in what's missing in between (training
 * for a level, a quest nobody provides a step for).
 * <p>
 * Ids are namespaced by provider, e.g. {@code diary:VARROCK:EASY}. Quests and skill levels have fixed ids
 * ({@link #questId}, {@link #skillId}) so a provider's step replaces the planner's stand-in and other steps can
 * depend on it.
 */
@Value
@Builder(toBuilder = true)
public class PlanStep
{
	@NonNull
	String id;
	@NonNull
	StepKind kind;
	/**
	 * Short name for the list, e.g. "Varrock Easy diary".
	 */
	@NonNull
	String title;
	/**
	 * What doing it gives the account, in one or two sentences, e.g. "Varrock armour 1: chance of an extra ore".
	 */
	String why;
	/**
	 * First-timer steps on how to start it, e.g. "Open the Achievement Diary Guides panel and press Start".
	 * Usually short: the opened guide carries the click-by-click detail.
	 */
	@Singular("howTo")
	List<String> howTo;

	/**
	 * Done already: counted for progress, never planned.
	 */
	boolean done;
	/**
	 * The player chose not to do it, in its own plugin (e.g. the Quest Order Guide's ignore list): never planned,
	 * and counted as had for whatever waits on it, like a step skipped in the planner.
	 */
	boolean ignored;

	// ---- What it needs ----

	/**
	 * Real (unboosted) skill levels needed. The planner adds training steps for any the player lacks.
	 */
	@Singular
	Map<Skill, Integer> skills;
	/**
	 * Quests that must be finished first.
	 */
	@Singular
	List<Quest> quests;
	/**
	 * Quests that only have to be started.
	 */
	@Singular("questStarted")
	List<Quest> questsStarted;
	/**
	 * Ids of other steps that must come first, e.g. the Easy tier before the Medium one. An id no provider makes is
	 * ignored, so a step can point at a plugin that isn't there yet.
	 */
	@Singular("after")
	List<String> after;
	/**
	 * Requirements nothing can check, shown with a "?" (e.g. "32 quest points").
	 */
	@Singular
	List<String> notes;

	// ---- What doing it gives ----

	/**
	 * For a {@link StepKind#SKILL} step: the skill and the level it trains to.
	 */
	Skill trainSkill;
	int trainLevel;
	/**
	 * For a {@link StepKind#QUEST} step: the quest it finishes.
	 */
	Quest finishes;

	// ---- How the planner weighs it ----

	/**
	 * Rough time it takes, in minutes, not counting training for its levels (the planner adds that). 0 = unknown,
	 * counted as a short step. Skill steps leave it 0: their time comes from the Skilling Helper xp rates.
	 */
	int minutes;
	/**
	 * How much it moves the account on, 1 (nice to have) to 10 (opens up the game). Steps that other steps
	 * depend on get a bonus on top, so leave that out.
	 */
	@Builder.Default
	int value = 3;
	/**
	 * Gold it costs (items to buy, fees), 0 when free or unknown. The planner puts a money-making step in front of
	 * it when the player can't afford it.
	 */
	long gpCost;
	/**
	 * In the Wilderness: only planned when the player opted in.
	 */
	boolean wilderness;

	// ---- Opening its guide ----

	/**
	 * Event posted on the RuneLite event bus to open this step's guide in its own plugin, e.g. a
	 * {@code TrainSkillRequest}. Must compare by value (a Lombok {@code @Value}). Null when there's no guide.
	 */
	Object open;
	/**
	 * Name of the plugin {@link #open} goes to, for the button: "Open in PvM Progression".
	 */
	String openIn;
	/**
	 * Wiki page name for a "Wiki" button, e.g. "Varrock_Diary", or null.
	 */
	String wikiPage;

	public static String questId(Quest quest)
	{
		return "quest:" + quest.name();
	}

	public static String skillId(Skill skill, int level)
	{
		return "skill:" + skill.name() + ":" + level;
	}
}
