package com.skillinghelper.banked;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * Banked XP settings. Highlight colours, arrows and the Shortest Path route follow Skilling Helper's settings, so
 * every guide in this jar looks the same.
 */
@ConfigGroup(BankedXpConfig.GROUP)
public interface BankedXpConfig extends Config
{
	String GROUP = "banked-xp-guide";

	@ConfigItem(
		keyName = "chatMessages",
		name = "Chat messages",
		description = "Say in the chat box when one banked item is used up and what's next",
		position = 0
	)
	default boolean chatMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "buyMissing",
		name = "Buy missing secondaries",
		description = "Count only the banked item itself and have the guide buy runes and secondaries that run short "
			+ "(nature runes, snape grass, coal...). Off: only use what's already in your bank, and stop when anything "
			+ "runs out",
		position = 1
	)
	default boolean buyMissing()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showBankButton",
		name = "Bank button",
		description = "Add a button to the bank that shows only the items the Banked XP guide needs right now",
		position = 2
	)
	default boolean showBankButton()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankFilterOn",
		name = "",
		description = "",
		hidden = true
	)
	default boolean bankFilterOn()
	{
		return true;
	}
}
