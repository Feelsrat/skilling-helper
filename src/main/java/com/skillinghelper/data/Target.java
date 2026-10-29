package com.skillinghelper.data;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;

/**
 * Something in the game world to highlight, matched by name and (optionally) menu action. Matching by name
 * keeps the guide data readable and covers every variant of e.g. "Willow tree" without listing object ids.
 */
@Value
public class Target
{
	public enum Kind
	{
		OBJECT,
		NPC
	}

	Kind kind;
	Set<String> names;
	/**
	 * Menu action the player should click, e.g. "Chop down". Null matches any action.
	 */
	String action;
	/**
	 * Match any name that contains one of {@link #names} as whole words, like RuneLite's Slayer plugin does: "goblin"
	 * matches "Goblin guard" but not "Hobgoblin".
	 */
	boolean anyContaining;

	public static Target object(String action, String... names)
	{
		return new Target(Kind.OBJECT, lower(names), action, false);
	}

	public static Target npc(String action, String... names)
	{
		return new Target(Kind.NPC, lower(names), action, false);
	}

	/**
	 * NPCs whose name contains one of these words, e.g. "spectre" for both aberrant and deviant spectres.
	 */
	public static Target npcContaining(String action, String... words)
	{
		return new Target(Kind.NPC, lower(words), action, true);
	}

	private static Set<String> lower(String... names)
	{
		return Stream.of(names).map(n -> n.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
	}

	public boolean matches(String name, String[] actions)
	{
		if (name == null || !matchesName(name))
		{
			return false;
		}
		if (action == null)
		{
			return true;
		}
		if (actions == null)
		{
			return false;
		}
		for (String a : actions)
		{
			if (action.equalsIgnoreCase(a))
			{
				return true;
			}
		}
		return false;
	}

	public boolean matchesName(String name)
	{
		String lower = name.replace('\u00A0', ' ').toLowerCase(Locale.ROOT);
		if (!anyContaining)
		{
			return names.contains(lower);
		}
		for (String word : names)
		{
			if (containsWords(lower, word))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean containsWords(String name, String words)
	{
		for (int i = name.indexOf(words); i >= 0; i = name.indexOf(words, i + 1))
		{
			int end = i + words.length();
			if ((i == 0 || name.charAt(i - 1) == ' ') && (end == name.length() || name.charAt(end) == ' '))
			{
				return true;
			}
		}
		return false;
	}
}
