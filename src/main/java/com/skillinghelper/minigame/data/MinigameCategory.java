package com.skillinghelper.minigame.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MinigameCategory
{
	SKILLING("Skilling"),
	COMBAT("Combat");

	private final String label;
}
