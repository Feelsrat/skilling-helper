package com.skillinghelper.clue;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(ClueScrollGuideConfig.GROUP)
public interface ClueScrollGuideConfig extends Config
{
	String GROUP = "clue-scroll-guide";

	@ConfigSection(name = "Guide", description = "How a running guide shows the way", position = 0)
	String guideSection = "guide";

	@ConfigSection(name = "Colours", description = "Highlight colours", position = 1)
	String colourSection = "colours";

	@ConfigItem(
		keyName = "shortestPath",
		name = "Shortest Path route",
		description = "Draw the route to where the guide sends you with the Shortest Path plugin, if it's installed",
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
		description = "Point the game's hint arrow at where the guide sends you",
		section = guideSection,
		position = 1
	)
	default boolean hintArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "preferShops",
		name = "Buy from shops when cheaper",
		description = "When an NPC shop sells what the guide needs for less than the Grand Exchange, send you there "
			+ "instead. Only shops anyone can enter are used",
		section = guideSection,
		position = 2
	)
	default boolean preferShops()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "targetColor",
		name = "Click target",
		description = "Colour of the NPCs and objects to click",
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
		name = "Items",
		description = "Colour of bank items to take out and inventory items to click (clues, caskets, jars)",
		section = colourSection,
		position = 1
	)
	default Color itemColor()
	{
		return new Color(0, 255, 0, 220);
	}
}
