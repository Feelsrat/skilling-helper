package com.skillinghelper.diary.data;

import lombok.Getter;

@Getter
public enum DiaryTier
{
	EASY("Easy"),
	MEDIUM("Medium"),
	HARD("Hard"),
	ELITE("Elite");

	private final String label;

	DiaryTier(String label)
	{
		this.label = label;
	}

	/**
	 * The tier before this one, whose reward has to be claimed first, or null for Easy.
	 */
	public DiaryTier previous()
	{
		return ordinal() == 0 ? null : values()[ordinal() - 1];
	}
}
