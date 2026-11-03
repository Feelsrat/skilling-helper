package com.skillinghelper.data;

import java.awt.Color;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;

/**
 * The Wilderness, where other players can attack you and you lose your items when you die. Every guide in this jar
 * leaves out methods, bosses, banks and shops there unless the player opts in with one shared setting, and flags
 * them wherever they show up.
 */
public final class Wilderness
{
	/**
	 * Config group of the shared setting: the Skilling Helper's, so it shows up in its settings.
	 */
	public static final String GROUP = "skillinghelper";
	public static final String KEY = "allowWilderness";

	/**
	 * Short tag shown next to anything in the Wilderness.
	 */
	public static final String TAG = "☠ Wilderness";
	public static final Color COLOR = new Color(255, 110, 70);
	public static final String WARNING = "In the Wilderness: other players can attack you, and you lose most of "
		+ "your items if you die. Only bring what you can afford to lose.";
	/**
	 * The label of the visible switch each guide's panel shows.
	 */
	public static final String TOGGLE_LABEL = "Include Wilderness methods (all guides)";

	// Above ground and in its dungeons
	private static final int MIN_X = 2944;
	private static final int MAX_X = 3391;
	private static final int MIN_Y = 3520;
	private static final int MAX_Y = 3967;
	private static final int UNDERGROUND_MIN_Y = 9920;
	private static final int UNDERGROUND_MAX_Y = 10367;

	private Wilderness()
	{
	}

	public static boolean contains(WorldPoint wp)
	{
		if (wp == null)
		{
			return false;
		}
		int x = wp.getX();
		int y = wp.getY();
		return x >= MIN_X && x <= MAX_X
			&& ((y >= MIN_Y && y <= MAX_Y) || (y >= UNDERGROUND_MIN_Y && y <= UNDERGROUND_MAX_Y));
	}

	public static boolean contains(Location l)
	{
		return l != null && contains(l.getPoint());
	}

	/**
	 * Whether the player opted in to Wilderness methods. Off unless they turned it on.
	 */
	public static boolean allowed(ConfigManager configManager)
	{
		Boolean allowed = configManager.getConfiguration(GROUP, KEY, Boolean.class);
		return allowed != null && allowed;
	}

	public static void setAllowed(ConfigManager configManager, boolean allowed)
	{
		configManager.setConfiguration(GROUP, KEY, allowed);
	}

	/**
	 * Whether this config change switched the shared setting.
	 */
	public static boolean isSettingChange(ConfigChanged e)
	{
		return GROUP.equals(e.getGroup()) && KEY.equals(e.getKey()) && e.getProfile() == null;
	}
}
