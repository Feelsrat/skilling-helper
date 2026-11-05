package com.skillinghelper.money;

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
 * Outlines the supplies to take out of the bank (a travel item in its own colour), and the inventory items to use,
 * for the running money guide.
 */
class MoneyItemOverlay extends WidgetItemOverlay
{
	private final MoneyMakingPlugin plugin;
	private final MoneyMakingConfig config;
	private final ItemManager itemManager;
	private final ConfigManager configManager;

	@Inject
	MoneyItemOverlay(MoneyMakingPlugin plugin, MoneyMakingConfig config, ItemManager itemManager,
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
		MoneyStep step = plugin.getStep();
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
