package com.skillinghelper.planner.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What sort of thing a plan step is. Decides the tag and colour in the planner panel and lets the player hide
 * whole kinds of steps.
 */
@Getter
@RequiredArgsConstructor
public enum StepKind
{
	QUEST("Quest"),
	SKILL("Skill"),
	DIARY("Diary"),
	BOSS("Boss"),
	MINIGAME("Minigame"),
	UNLOCK("Unlock"),
	COMBAT_ACHIEVEMENT("Combat task"),
	CLUE("Clues"),
	COLLECTION("Collection log"),
	MONEY("Gold"),
	OTHER("Other");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
