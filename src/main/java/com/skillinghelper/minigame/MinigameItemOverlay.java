package com.skillinghelper.minigame;

import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.travel.TravelSettings;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Outlines what to take out of the bank for the minigame (a travel item in its own colour), and the
 * inventory items to use.
 */
class MinigameItemOverlay extends WidgetItemOverlay
{
	private final MinigameGuidesPlugin plugin;
	private final MinigameGuidesConfig config;
	private final ItemManager itemManager;
	private final ConfigManager configManager;

	@Inject
	MinigameItemOverlay(MinigameGuidesPlugin plugin, MinigameGuidesConfig config, ItemManager itemManager,
		ConfigManager configManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		this.configManager = configManager;
		showOnBank();
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D g, int itemId, WidgetItem item)
	{
		DiaryStep step = plugin.getStep();
		if (step == null)
		{
			return;
		}
		boolean bank = item.getWidget().getId() >>> 16 == InterfaceID.BANKMAIN;
		int id = itemManager.canonicalize(itemId);
		boolean travel = bank && step.getTravel().contains(id);
		if (!travel && (bank ? !step.getWithdraw().containsKey(id) : !step.getUse().contains(id)))
		{
			return;
		}
		Color color = travel ? TravelSettings.color(configManager) : config.itemColor();
		Rectangle bounds = item.getCanvasBounds();
		g.drawImage(itemManager.getItemOutline(itemId, item.getQuantity(), color), bounds.x, bounds.y, null);
	}
}
