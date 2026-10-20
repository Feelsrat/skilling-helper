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

	public static Target object(String action, String... names)
	{
		return new Target(Kind.OBJECT, lower(names), action);
	}

	public static Target npc(String action, String... names)
	{
		return new Target(Kind.NPC, lower(names), action);
	}

	private static Set<String> lower(String... names)
	{
		return Stream.of(names).map(n -> n.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
	}

	public boolean matches(String name, String[] actions)
	{
		if (name == null || !names.contains(name.toLowerCase(Locale.ROOT)))
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
}
