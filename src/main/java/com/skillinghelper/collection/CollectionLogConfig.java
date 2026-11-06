package com.skillinghelper.collection;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(CollectionLogConfig.GROUP)
public interface CollectionLogConfig extends Config
{
	String GROUP = "collection-log-guide";

	@ConfigSection(name = "Guide", description = "How a page you picked shows the way", position = 0)
	String guideSection = "guide";

	@ConfigItem(
		keyName = "countOwned",
		name = "Count items I own",
		description = "A slot not seen in your log yet counts as filled when the item is in your bank, inventory "
			+ "or worn. Open the log page to correct any you bought rather than got",
		position = 0
	)
	default boolean countOwned()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLocked",
		name = "Show locked pages",
		description = "List slots and pages you don't have the quests or levels for yet (marked with what's missing)",
		position = 1
	)
	default boolean showLocked()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shortestPath",
		name = "Shortest Path route",
		description = "Draw the route to the page you picked with the Shortest Path plugin, if it's installed",
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
		description = "Point the game's hint arrow at where to go",
		section = guideSection,
		position = 1
	)
	default boolean hintArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "outlineTargets",
		name = "Outline the boss",
		description = "Outline the boss or monster of the page you picked",
		section = guideSection,
		position = 2
	)
	default boolean outlineTargets()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "targetColor",
		name = "Outline colour",
		description = "Colour of the boss or monster outline",
		section = guideSection,
		position = 3
	)
	default Color targetColor()
	{
		return new Color(0, 255, 255, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "itemColor",
		name = "Item colour",
		description = "Colour of the items to take out of the bank and the ones to use",
		section = guideSection,
		position = 4
	)
	default Color itemColor()
	{
		return new Color(255, 215, 90, 220);
	}

	@ConfigItem(
		keyName = "preferShops",
		name = "Buy from shops when cheaper",
		description = "Send you to an NPC shop instead of the Grand Exchange when it sells what you need for less",
		section = guideSection,
		position = 5
	)
	default boolean preferShops()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightSearch",
		name = "Highlight the log's Search button",
		description = "While the log hasn't been synced (or the game counts more slots than the plugin knows), "
			+ "outline the collection log's Search button: clicking it once reads the whole log",
		position = 2
	)
	default boolean highlightSearch()
	{
		return true;
	}
}
