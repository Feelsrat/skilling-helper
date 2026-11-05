package com.skillinghelper.minigame.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Every minigame the plugin knows, in list order: skilling first, then combat, each by the level it starts at.
 */
public final class Minigames
{
	private static final List<Minigame> ALL;

	static
	{
		List<Minigame> all = new ArrayList<>();
		all.addAll(LinkedMinigames.all());
		all.addAll(GuidedMinigames.all());
		all.sort(Comparator.comparing(Minigame::getCategory)
			.thenComparingInt(Minigame::minLevel)
			.thenComparing(Minigame::getName));
		ALL = Collections.unmodifiableList(all);
	}

	private Minigames()
	{
	}

	public static List<Minigame> all()
	{
		return ALL;
	}

	public static Minigame byId(String id)
	{
		for (Minigame m : ALL)
		{
			if (m.getId().equals(id))
			{
				return m;
			}
		}
		return null;
	}
}
