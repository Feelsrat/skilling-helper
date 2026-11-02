package com.skillinghelper.money;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Outlines the supplies to take out of the bank, and the inventory items to use, for the running money guide.
 */
class MoneyItemOverlay extends WidgetItemOverlay
{
	private final MoneyMakingPlugin plugin;
	private final MoneyMakingConfig config;
	private final ItemManager itemManager;

	@Inject
	MoneyItemOverlay(MoneyMakingPlugin plugin, MoneyMakingConfig config, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
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
		if (bank ? !step.getWithdraw().containsKey(id) : !step.getUse().contains(id))
		{
			return;
		}
		Rectangle bounds = item.getCanvasBounds();
		g.drawImage(itemManager.getItemOutline(itemId, item.getQuantity(), config.itemColor()), bounds.x, bounds.y,
			null);
	}
}
