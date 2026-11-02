package com.skillinghelper.money;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(MoneyMakingConfig.GROUP)
public interface MoneyMakingConfig extends Config
{
	String GROUP = "money-making-guides";

	@ConfigSection(name = "Guide", description = "How a running guide shows the way", position = 0)
	String guideSection = "guide";

	@ConfigSection(name = "Colours", description = "Highlight colours", position = 1)
	String colourSection = "colours";

	@ConfigItem(
		keyName = "shortestPath",
		name = "Shortest Path route",
		description = "Draw the route to the method's spot with the Shortest Path plugin, if it's installed",
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
		description = "Point the game's hint arrow at the method's spot",
		section = guideSection,
		position = 1
	)
	default boolean hintArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "handOff",
		name = "Use full guides",
		description = "When Skilling Helper or PvM Progression has a click-by-click guide for a method, start that "
			+ "one instead of the simple guide here",
		section = guideSection,
		position = 2
	)
	default boolean handOff()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "targetColor",
		name = "Click target",
		description = "Colour of shopkeepers, bankers and bank booths to click",
		section = colourSection,
		position = 0
	)
	default Color targetColor()
	{
		return new Color(0, 255, 255, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "itemColor",
		name = "Items to take",
		description = "Colour of bank items to take out",
		section = colourSection,
		position = 1
	)
	default Color itemColor()
	{
		return new Color(0, 255, 0, 220);
	}
}
