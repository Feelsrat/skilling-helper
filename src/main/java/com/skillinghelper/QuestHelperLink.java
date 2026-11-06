package com.skillinghelper;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/**
 * Finds the Quest Helper plugin, which the Quest Order Guide and Account Unlocks hand each quest over to for the
 * click-by-click steps.
 * Quest Helper has no way for other plugins to start a quest in it, so the guide shows where to right-click it.
 * Warns once per session when it's missing, like {@link ShortestPathLink}. Client thread only.
 */
public final class QuestHelperLink
{
	static final String PLUGIN_CLASS = "com.questhelper.QuestHelperPlugin";
	static final String PLUGIN_NAME = "Quest Helper";
	static final String WARNING = "The Quest Helper plugin isn't installed or isn't turned on, so the guides can "
		+ "only show where each quest starts. Get Quest Helper from the Plugin Hub (or turn it on) for "
		+ "click-by-click quest steps.";
	/**
	 * Shown on a guide step that sends the player through a quest while Quest Helper is missing.
	 */
	public static final String MISSING = "The Quest Helper plugin isn't installed or isn't turned on. Get it from "
		+ "the Plugin Hub (or turn it on) to be shown every step of the quest.";

	/**
	 * Quest Helper's config group and the settings this jar reads or (on the player's press) changes.
	 */
	public static final String CONFIG_GROUP = "questhelper";
	static final String AUTO_START_KEY = "autoStartQuests";
	static final String ORDER_KEY = "orderListBy";

	private static boolean warned;

	private QuestHelperLink()
	{
	}

	public static boolean isActive(PluginManager pluginManager)
	{
		for (Plugin p : pluginManager.getPlugins())
		{
			if (isQuestHelper(p.getClass().getName(), p.getName()))
			{
				return pluginManager.isPluginActive(p);
			}
		}
		return false;
	}

	/**
	 * Whether Quest Helper starts guiding a quest by itself when the game says it started (its default).
	 */
	public static boolean autoStarts(ConfigManager configManager)
	{
		String v = configManager.getConfiguration(CONFIG_GROUP, AUTO_START_KEY);
		return v == null || Boolean.parseBoolean(v);
	}

	/**
	 * Turns on Quest Helper's "Auto start helper" setting. Only on the player's press.
	 */
	public static void enableAutoStart(ConfigManager configManager)
	{
		configManager.setConfiguration(CONFIG_GROUP, AUTO_START_KEY, true);
	}

	/**
	 * Sorts Quest Helper's quest list by the wiki's optimal order (main or Ironman), the same route as this guide.
	 * Only on the player's press.
	 */
	public static void sortOptimal(ConfigManager configManager, boolean ironman)
	{
		configManager.setConfiguration(CONFIG_GROUP, ORDER_KEY, ironman ? "OPTIMAL_IRONMAN" : "OPTIMAL");
	}

	static boolean isQuestHelper(String className, String name)
	{
		return PLUGIN_CLASS.equals(className) || PLUGIN_NAME.equalsIgnoreCase(name);
	}

	/**
	 * Says in the chat, once per session, that Quest Helper is missing or off. Call when logged in.
	 */
	public static void warnIfMissing(Client client, PluginManager pluginManager)
	{
		if (warned || isActive(pluginManager))
		{
			return;
		}
		warned = true;
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Skilling Helper: " + WARNING, null);
	}
}
