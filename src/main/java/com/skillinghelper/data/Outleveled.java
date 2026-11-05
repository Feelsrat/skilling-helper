package com.skillinghelper.data;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;

/**
 * One shared setting for every guide in this jar: hide the methods and bosses the player has outgrown (past the top
 * of their level range). On by default.
 */
public final class Outleveled
{
	/**
	 * Config group of the shared setting: the Skilling Helper's, so it shows up in its settings.
	 */
	public static final String GROUP = Wilderness.GROUP;
	public static final String KEY = "hideOutleveled";

	private Outleveled()
	{
	}

	/**
	 * Whether outgrown methods are hidden. On unless the player turned it off.
	 */
	public static boolean hidden(ConfigManager configManager)
	{
		Boolean hidden = configManager.getConfiguration(GROUP, KEY, Boolean.class);
		return hidden == null || hidden;
	}

	public static void setHidden(ConfigManager configManager, boolean hidden)
	{
		configManager.setConfiguration(GROUP, KEY, hidden);
	}

	/**
	 * Whether this config change switched the shared setting.
	 */
	public static boolean isSettingChange(ConfigChanged e)
	{
		return GROUP.equals(e.getGroup()) && KEY.equals(e.getKey()) && e.getProfile() == null;
	}
}
