package com.skillinghelper.planner;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(AccountPlannerConfig.GROUP)
public interface AccountPlannerConfig extends Config
{
	String GROUP = "account-planner";
	/**
	 * Hidden: comma-separated step kinds left out of the plan, toggled from the panel.
	 */
	String KEY_HIDDEN_KINDS = "hiddenKinds";
	/**
	 * Hidden, per RuneScape profile: comma-separated ids of skipped steps.
	 */
	String KEY_SKIPPED = "skipped";
	/**
	 * Hidden, per RuneScape profile: set while the guided route runs, to the id of its current step.
	 */
	String KEY_ROUTE = "route";

	@Range(min = 5, max = 200)
	@ConfigItem(
		keyName = "planLength",
		name = "Steps shown",
		description = "How many steps of the path to show in the panel",
		position = 0
	)
	default int planLength()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "routeBox",
		name = "Route box",
		description = "While the route runs, show its current and next step in a small box on the game screen",
		position = 1
	)
	default boolean routeBox()
	{
		return true;
	}
}
