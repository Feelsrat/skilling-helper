package com.skillinghelper.pvm;

import com.skillinghelper.engine.ItemHighlight;
import com.skillinghelper.pvm.guide.PvmStep;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Outlines inventory items to use or wear, and bank items to take out, for the running guide.
 */
class PvmItemOverlay extends WidgetItemOverlay
{
	private final PvmProgressionPlugin plugin;
	private final PvmProgressionConfig config;
	private final ItemManager itemManager;

	@Inject
	PvmItemOverlay(PvmProgressionPlugin plugin, PvmProgressionConfig config, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		showOnInventory();
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D g, int itemId, WidgetItem item)
	{
		PvmStep step = plugin.getStep();
		if (step == null)
		{
			return;
		}
		boolean bank = item.getWidget().getId() >>> 16 == InterfaceID.BANKMAIN;
		ItemHighlight kind = step.getItemHighlights().get(bank ? itemManager.canonicalize(itemId) : itemId);
		// Withdraw outlines only belong in the bank, the rest only in the inventory
		if (kind == null || bank != (kind == ItemHighlight.WITHDRAW))
		{
			return;
		}
		Color color = kind == ItemHighlight.EQUIP ? config.equipColor() : config.itemColor();
		Rectangle bounds = item.getCanvasBounds();
		g.drawImage(itemManager.getItemOutline(itemId, item.getQuantity(), color), bounds.x, bounds.y, null);
	}
}
