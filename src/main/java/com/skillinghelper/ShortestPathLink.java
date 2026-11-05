package com.skillinghelper;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/**
 * The one place every plugin in this jar talks to the Shortest Path plugin through. Shortest Path shows one path at a
 * time, so it remembers which guide set it last: a guide clearing its own path doesn't wipe another guide's path.
 * Also warns once per session when Shortest Path isn't there. Client thread only.
 */
public final class ShortestPathLink
{
	public static final String NAMESPACE = "shortestpath";
	static final String PLUGIN_CLASS = "shortestpath.ShortestPathPlugin";
	static final String PLUGIN_NAME = "Shortest Path";
	static final String WARNING = "The Shortest Path plugin isn't installed or isn't turned on, so guides can't draw "
		+ "the way. Get it from the Plugin Hub (or turn it on) to see paths. Hint arrows still work without it.";

	/**
	 * The guide whose path Shortest Path is showing, or null.
	 */
	private static Object owner;
	private static boolean warned;

	private ShortestPathLink()
	{
	}

	/**
	 * Shows the path to {@code data}'s target for {@code who}, replacing whatever path Shortest Path had.
	 */
	public static void path(EventBus eventBus, Object who, Map<String, Object> data)
	{
		owner = who;
		eventBus.post(new PluginMessage(NAMESPACE, "path", data));
	}

	/**
	 * Clears the path, but only if it's {@code who}'s: another guide may have set a newer one since.
	 */
	public static void clear(EventBus eventBus, Object who)
	{
		if (owner != who)
		{
			return;
		}
		owner = null;
		eventBus.post(new PluginMessage(NAMESPACE, "clear", new HashMap<>()));
	}

	static boolean ownedBy(Object who)
	{
		return owner == who;
	}

	/**
	 * Says in the chat, once per session, that Shortest Path is missing or off. Call when logged in, from a guide
	 * that has its Shortest Path setting on.
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

	static boolean isActive(PluginManager pluginManager)
	{
		for (Plugin p : pluginManager.getPlugins())
		{
			if (isShortestPath(p.getClass().getName(), p.getName()))
			{
				return pluginManager.isPluginActive(p);
			}
		}
		return false;
	}

	static boolean isShortestPath(String className, String name)
	{
		return PLUGIN_CLASS.equals(className) || PLUGIN_NAME.equalsIgnoreCase(name);
	}

	/**
	 * For tests.
	 */
	static void reset()
	{
		owner = null;
		warned = false;
	}
}
