package com.skillinghelper.planner.api;

import java.util.List;

/**
 * Hands the Account Planner the steps one plugin in this jar knows about. Each plugin keeps its provider in its own
 * package (e.g. {@code diary.DiaryStepProvider}) and adds it to {@code planner.StepProviders}. Providers are
 * plain classes: they work from their plugin's data and the {@link AccountState}, so the planner works whether or
 * not the providing plugin is switched on (its guide only opens when it is).
 * <p>
 * To plug a new plugin in:
 * <ol>
 * <li>Write a class implementing this interface. Return every step, done ones too (with {@code done(true)}), so
 * the planner can show progress.</li>
 * <li>Give each step a namespaced id ({@code "ca:<task id>"}), its requirements (skills, quests, {@code after}
 * other step ids), a rough {@code minutes}, a {@code value} 1-10, {@code wilderness} when it is, and an
 * {@code open} request event that your plugin {@code @Subscribe}s to and opens its guide on.</li>
 * <li>Quest steps use {@link PlanStep#questId} and set {@code finishes}; skill milestones use
 * {@link PlanStep#skillId} and set {@code trainSkill}/{@code trainLevel}. Those replace the planner's stand-ins.</li>
 * <li>Add it to {@code StepProviders.ALL}. Keep {@code steps()} fast (a few milliseconds): it runs on the client
 * thread whenever the plan is worked out again.</li>
 * </ol>
 */
public interface PlanStepProvider
{
	/**
	 * The plugin's name as shown on buttons, e.g. "Achievement Diary Guides".
	 */
	String name();

	/**
	 * Every step this plugin knows about, done or not. Client thread.
	 */
	List<PlanStep> steps(AccountState account);
}
