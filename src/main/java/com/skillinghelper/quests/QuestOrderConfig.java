package com.skillinghelper.quests;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(QuestOrderConfig.GROUP)
public interface QuestOrderConfig extends Config
{
	String GROUP = "quest-order-guide";

	enum RouteChoice
	{
		AUTO("Ironman accounts get the Ironman route"),
		MAIN("Main account"),
		IRONMAN("Ironman");

		private final String label;

		RouteChoice(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return this == AUTO ? "Auto (by account type)" : label;
		}
	}

	@ConfigSection(name = "Guide", description = "How the guide shows the way", position = 0)
	String guideSection = "guide";

	@ConfigSection(name = "Colours", description = "Highlight colours", position = 1)
	String colourSection = "colours";

	@ConfigItem(
		keyName = "route",
		name = "Quest route",
		description = "Which of the wiki's optimal quest guides to follow",
		position = 0
	)
	default RouteChoice route()
	{
		return RouteChoice.AUTO;
	}

	@ConfigItem(
		keyName = "shortestPath",
		name = "Shortest Path route",
		description = "Draw the way to where the next quest starts with the Shortest Path plugin, if it's installed",
		section = guideSection,
		position = 0
	)
	default boolean shortestPath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hintArrow",
		name = "Hint arrow",
		description = "Point the game's hint arrow at where the next quest starts",
		section = guideSection,
		position = 1
	)
	default boolean hintArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "warnQuestHelper",
		name = "Warn if Quest Helper is missing",
		description = "Say in the chat (once per session) when the Quest Helper plugin isn't installed or is off",
		section = guideSection,
		position = 2
	)
	default boolean warnQuestHelper()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "targetColor",
		name = "Highlight",
		description = "Colour of the quest's line in the Quest list and the NPC who starts it",
		section = colourSection,
		position = 0
	)
	default Color targetColor()
	{
		return new Color(0, 255, 255, 200);
	}
}
