package com.skillinghelper.pvm;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(PvmProgressionPlugin.GROUP)
public interface PvmProgressionConfig extends Config
{
	@ConfigSection(name = "Guide", description = "How a running guide shows the way", position = 0)
	String guideSection = "guide";

	@ConfigSection(name = "Colours", description = "Highlight colours", position = 1)
	String colourSection = "colours";

	@ConfigItem(
		keyName = "fightHelp",
		name = "Fight highlights",
		description = "During a fight, highlight the boss and the adds it has spawned, and show your progress "
			+ "(e.g. Barrows brothers) and supply reminders. Never predicts attacks, prayers or safe tiles.",
		section = guideSection,
		position = 0
	)
	default boolean fightHelp()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shortestPath",
		name = "Shortest Path route",
		description = "Draw the route to the next step with the Shortest Path plugin, if it's installed",
		section = guideSection,
		position = 1
	)
	default boolean shortestPath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hintArrow",
		name = "Hint arrow",
		description = "Point the game's hint arrow at where the next step is",
		section = guideSection,
		position = 2
	)
	default boolean hintArrow()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "targetColor",
		name = "Click target",
		description = "Colour of the boss, gates and other things to click",
		section = colourSection,
		position = 0
	)
	default Color targetColor()
	{
		return new Color(0, 255, 255, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "addColor",
		name = "Adds",
		description = "Colour of the adds a boss spawns, like growthlings or Jad's healers",
		section = colourSection,
		position = 1
	)
	default Color addColor()
	{
		return new Color(255, 140, 0, 220);
	}

	@Alpha
	@ConfigItem(
		keyName = "bankColor",
		name = "Bank",
		description = "Colour of banks when it's time to bank",
		section = colourSection,
		position = 2
	)
	default Color bankColor()
	{
		return new Color(255, 215, 0, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "itemColor",
		name = "Items",
		description = "Outline of inventory items to use and bank items to withdraw",
		section = colourSection,
		position = 3
	)
	default Color itemColor()
	{
		return new Color(0, 255, 0, 220);
	}

	@Alpha
	@ConfigItem(
		keyName = "equipColor",
		name = "Wear",
		description = "Outline of inventory gear to put on",
		section = colourSection,
		position = 4
	)
	default Color equipColor()
	{
		return new Color(255, 0, 255, 220);
	}

	@Alpha
	@ConfigItem(
		keyName = "priorityColor",
		name = "Attack first",
		description = "Colour of the monster to attack first by the guide's usual kill order (Fight Cave waves)",
		section = colourSection,
		position = 5
	)
	default Color priorityColor()
	{
		return new Color(255, 40, 40, 230);
	}
}
