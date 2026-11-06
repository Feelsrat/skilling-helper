package com.skillinghelper.unlocks.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What kind of thing an unlock helps with, shown as a tag on its row.
 */
@Getter
@RequiredArgsConstructor
public enum UnlockCategory
{
	TRAVEL("Travel"),
	GEAR("Gear"),
	SKILLING("Skilling"),
	MAGIC("Magic");

	private final String label;
}
