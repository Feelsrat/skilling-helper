package com.skillinghelper.travel;

import java.awt.Color;
import net.runelite.client.config.ConfigManager;

/**
 * The shared travel item settings. They live in the Skilling Helper's config group, so they show up in its settings,
 * and every plugin in this jar reads them from there.
 */
public final class TravelSettings
{
	public static final String GROUP = "skillinghelper";
	public static final String KEY = "travelItems";
	public static final String COLOR_KEY = "travelColor";
	public static final Color DEFAULT_COLOR = new Color(0, 190, 255);

	private TravelSettings()
	{
	}

	/**
	 * Whether guides suggest travel items. On unless the player turned it off.
	 */
	public static boolean enabled(ConfigManager configManager)
	{
		Boolean on = configManager.getConfiguration(GROUP, KEY, Boolean.class);
		return on == null || on;
	}

	/**
	 * The outline colour for travel items in the bank.
	 */
	public static Color color(ConfigManager configManager)
	{
		Color c = configManager.getConfiguration(GROUP, COLOR_KEY, Color.class);
		return c == null ? DEFAULT_COLOR : c;
	}
}
