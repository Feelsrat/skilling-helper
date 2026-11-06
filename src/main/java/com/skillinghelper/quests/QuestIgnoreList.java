package com.skillinghelper.quests;

import com.skillinghelper.quests.data.OrderRow;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;

/**
 * Quests (and other route rows) the player never wants suggested, saved per account. Shared by the Quest Order
 * Guide and the Account Planner. Quests are stored by their wiki name, other rows by their row key.
 */
public final class QuestIgnoreList
{
	public static final String GROUP = "skillinghelper";
	public static final String KEY = "ignoredQuests";
	/**
	 * How the Account Planner reads it through its profile config.
	 */
	public static final String PROFILE_KEY = GROUP + "." + KEY;
	private static final String SEPARATOR = ";";

	private QuestIgnoreList()
	{
	}

	public static Set<String> load(ConfigManager configManager)
	{
		return parse(configManager.getRSProfileConfiguration(GROUP, KEY));
	}

	public static Set<String> parse(String saved)
	{
		Set<String> out = new LinkedHashSet<>();
		if (saved != null && !saved.isEmpty())
		{
			Collections.addAll(out, saved.split(SEPARATOR));
		}
		out.remove("");
		return out;
	}

	public static void set(ConfigManager configManager, String entry, boolean ignored)
	{
		Set<String> s = load(configManager);
		if (!(ignored ? s.add(entry) : s.remove(entry)))
		{
			return;
		}
		if (s.isEmpty())
		{
			configManager.unsetRSProfileConfiguration(GROUP, KEY);
		}
		else
		{
			configManager.setRSProfileConfiguration(GROUP, KEY, String.join(SEPARATOR, s));
		}
	}

	/**
	 * The entry a route row is ignored under.
	 */
	public static String entry(OrderRow row)
	{
		return row.getKind() == OrderRow.Kind.QUEST || row.getKind() == OrderRow.Kind.PARTIAL
			? row.getRef() : row.key();
	}

	public static boolean isSettingChange(ConfigChanged e)
	{
		return GROUP.equals(e.getGroup()) && e.getKey() != null && e.getKey().endsWith(KEY);
	}
}
