package com.skillinghelper.money;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(MoneyMakingConfig.GROUP)
public interface MoneyMakingConfig extends Config
{
	String GROUP = "money-making-guides";

	@ConfigSection(name = "Guide", description = "How a running guide shows the way", position = 0)
	String guideSection = "guide";

	@ConfigSection(name = "Prices", description = "Where profit numbers come from", position = 1)
	String pricesSection = "prices";

	@ConfigSection(name = "Colours", description = "Highlight colours", position = 2)
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

	@ConfigItem(
		keyName = "preferShops",
		name = "Buy from shops when cheaper",
		description = "When an NPC shop sells what you need for less than the Grand Exchange (e.g. feathers, vials of water, "
			+ "tools), send you there instead. Only shops anyone can enter are used",
		section = guideSection,
		position = 3
	)
	default boolean preferShops()
	{
		return true;
	}

	@ConfigItem(
		keyName = "livePrices",
		name = "Live OSRS Wiki prices",
		description = "Cost supplies at the real-time instant-buy price and products at the instant-sell price from the"
			+ " OSRS Wiki (refreshed every 5 minutes while logged in), instead of RuneLite's Grand Exchange guide prices",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		section = pricesSection,
		position = 0
	)
	default boolean livePrices()
	{
		return false;
	}

	@Range(max = 20)
	@ConfigItem(
		keyName = "bankSoonLevels",
		name = "\"Soon\" levels",
		description = "In the My bank list, also show recipes up to this many levels above yours when they'd make "
			+ "more from the same items",
		section = pricesSection,
		position = 1
	)
	default int bankSoonLevels()
	{
		return 5;
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
