package com.skillinghelper;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/**
 * Finds the Quest Helper plugin, which the Quest Order Guide hands each quest over to for the click-by-click steps.
 * Quest Helper has no way for other plugins to start a quest in it, so the guide shows where to right-click it.
 * Warns once per session when it's missing, like {@link ShortestPathLink}. Client thread only.
 */
public final class QuestHelperLink
{
	static final String PLUGIN_CLASS = "com.questhelper.QuestHelperPlugin";
	static final String PLUGIN_NAME = "Quest Helper";
	static final String WARNING = "The Quest Helper plugin isn't installed or isn't turned on, so the Quest Order "
		+ "Guide can only show where each quest starts. Get Quest Helper from the Plugin Hub (or turn it on) for "
		+ "click-by-click quest steps.";

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
