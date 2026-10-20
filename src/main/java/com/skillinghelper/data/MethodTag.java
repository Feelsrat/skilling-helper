package com.skillinghelper.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Descriptive tags shown next to a method and used when ranking methods for a {@link TrainingPreference}.
 */
@Getter
@RequiredArgsConstructor
public enum MethodTag
{
	FAST("Fast"),
	AFK("AFK"),
	CHEAP("Cheap"),
	PROFIT("Profit"),
	BANKSTANDING("Bank-standing"),
	POWER("Power-training"),
	MINIGAME("Minigame");

	private final String label;
}
