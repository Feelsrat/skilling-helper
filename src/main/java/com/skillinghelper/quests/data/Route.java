package com.skillinghelper.quests.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Which of the wiki's optimal quest guides to follow.
 */
@Getter
@RequiredArgsConstructor
public enum Route
{
	MAIN("Main account", "Optimal_quest_guide"),
	IRONMAN("Ironman", "Optimal_quest_guide/Ironman");

	private final String label;
	private final String wikiPage;

	@Override
	public String toString()
	{
		return label;
	}
}
