package com.skillinghelper;

import com.skillinghelper.data.TrainingPreference;
import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;

@ConfigGroup(SkillingHelperConfig.GROUP)
public interface SkillingHelperConfig extends Config
{
	String GROUP = "skillinghelper";

	enum InventoryFullAction
	{
		METHOD_DEFAULT,
		ALWAYS_BANK,
		ALWAYS_DROP
	}

	enum HighlightStyle
	{
		OUTLINE,
		CLICKBOX,
		TILE
	}

	@ConfigSection(name = "Guide", description = "How guides are chosen", position = 0)
	String guideSection = "guide";

	@ConfigSection(name = "Highlights", description = "What gets highlighted and how", position = 1)
	String highlightSection = "highlights";

	@ConfigSection(name = "Pathing", description = "How the way to the next spot is shown", position = 2)
	String pathingSection = "pathing";

	@ConfigSection(name = "Prices", description = "Where Grand Exchange prices come from", position = 3)
	String pricesSection = "prices";

	@ConfigSection(name = "Bird houses", description = "Fossil Island bird house timer", position = 4)
	String birdhouseSection = "birdhouses";

	@ConfigItem(
		keyName = "birdhouseTimer",
		name = "Bird house timer",
		description = "Show a countdown until your seeded bird houses are full",
		section = birdhouseSection,
		position = 0
	)
	default boolean birdhouseTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "birdhouseNotification",
		name = "Notify when full",
		description = "Send a notification when your bird houses are full and ready for a run",
		section = birdhouseSection,
		position = 1
	)
	default Notification birdhouseNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "livePrices",
		name = "Live OSRS Wiki prices",
		description = "Use real-time instant-buy and instant-sell prices from the OSRS Wiki (refreshed every 5 minutes)"
			+ " instead of RuneLite's Grand Exchange guide prices when costing materials and products",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		section = pricesSection,
		position = 0
	)
	default boolean livePrices()
	{
		return false;
	}

	@ConfigItem(
		keyName = "preference",
		name = "Default training style",
		description = "Which methods to recommend. You can also change this in the panel.",
		section = guideSection,
		position = 0
	)
	default TrainingPreference preference()
	{
		return TrainingPreference.RECOMMENDED;
	}

	@Range(min = 1, max = 98)
	@ConfigItem(
		keyName = "defaultTargetOffset",
		name = "Default goal (levels)",
		description = "New goals are this many levels above your current level",
		section = guideSection,
		position = 1
	)
	default int defaultTargetOffset()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "inventoryFullAction",
		name = "Full inventory",
		description = "When gathering, whether to bank or drop when the inventory is full",
		section = guideSection,
		position = 2
	)
	default InventoryFullAction inventoryFullAction()
	{
		return InventoryFullAction.METHOD_DEFAULT;
	}

	@ConfigItem(
		keyName = "autoSelectSkill",
		name = "Auto-select skill",
		description = "When no guide is open, open the guide for a skill as soon as you gain xp in it",
		section = guideSection,
		position = 3
	)
	default boolean autoSelectSkill()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightStyle",
		name = "Highlight style",
		description = "How objects and NPCs to click are highlighted",
		section = highlightSection,
		position = 0
	)
	default HighlightStyle highlightStyle()
	{
		return HighlightStyle.OUTLINE;
	}

	@Alpha
	@ConfigItem(
		keyName = "targetColor",
		name = "Click target",
		description = "Colour of trees, rocks, fishing spots and stations to click",
		section = highlightSection,
		position = 1
	)
	default Color targetColor()
	{
		return new Color(0, 255, 255, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "bankColor",
		name = "Bank",
		description = "Colour of bank booths and bankers when you need to bank",
		section = highlightSection,
		position = 2
	)
	default Color bankColor()
	{
		return new Color(255, 200, 0, 200);
	}

	@ConfigItem(
		keyName = "useColor",
		name = "Item to use",
		description = "Colour of inventory items to use or withdraw",
		section = highlightSection,
		position = 3
	)
	default Color useColor()
	{
		return new Color(0, 255, 0);
	}

	@ConfigItem(
		keyName = "equipColor",
		name = "Item to wear",
		description = "Colour of gear to equip",
		section = highlightSection,
		position = 4
	)
	default Color equipColor()
	{
		return new Color(255, 200, 0);
	}

	@ConfigItem(
		keyName = "dropColor",
		name = "Item to drop or deposit",
		description = "Colour of items to drop or deposit",
		section = highlightSection,
		position = 5
	)
	default Color dropColor()
	{
		return new Color(255, 80, 80);
	}

	@ConfigItem(
		keyName = "showBankButton",
		name = "Bank filter button",
		description = "Add a button to the bank that shows only the items the current guide needs",
		section = highlightSection,
		position = 6
	)
	default boolean showBankButton()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hintArrow",
		name = "Hint arrow",
		description = "Show the yellow game hint arrow over the destination",
		section = pathingSection,
		position = 0
	)
	default boolean hintArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "minimapArrow",
		name = "Minimap arrow",
		description = "Point to the destination on the minimap",
		section = pathingSection,
		position = 1
	)
	default boolean minimapArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "worldMapPoint",
		name = "World map marker",
		description = "Mark the destination on the world map",
		section = pathingSection,
		position = 2
	)
	default boolean worldMapPoint()
	{
		return true;
	}

	@ConfigItem(
		keyName = "destinationTile",
		name = "Destination tile",
		description = "Highlight the destination tile when it's in view",
		section = pathingSection,
		position = 3
	)
	default boolean destinationTile()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shortestPath",
		name = "Use Shortest Path plugin",
		description = "If the Shortest Path plugin is installed, draw the full walking route to the destination with it",
		section = pathingSection,
		position = 4
	)
	default boolean shortestPath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shortestPathTransports",
		name = "Allow all transports in path",
		description = "While Skilling Helper shows a path, let Shortest Path use boats, ships, charter ships, canoes, "
			+ "shortcuts, fairy rings, spirit trees, gliders and other transports you've unlocked, even if they're off "
			+ "in its own settings. Your Shortest Path settings come back when the path is cleared.",
		section = pathingSection,
		position = 5
	)
	default boolean shortestPathTransports()
	{
		return true;
	}
}
