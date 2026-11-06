package com.skillinghelper.quests;

import com.skillinghelper.travel.TravelSettings;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Outlines in the bank the travel item that gets the player to the next quest's start, in the travel colour.
 */
class QuestItemOverlay extends WidgetItemOverlay
{
	private final QuestOrderPlugin plugin;
	private final ItemManager itemManager;
	private final ConfigManager configManager;

	@Inject
	QuestItemOverlay(QuestOrderPlugin plugin, ItemManager itemManager, ConfigManager configManager)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;
		this.configManager = configManager;
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D g, int itemId, WidgetItem item)
	{
		QuestStep step = plugin.getStep();
		if (step == null || step.getTravel().isEmpty() || item.getWidget().getId() >>> 16 != InterfaceID.BANKMAIN)
		{
			return;
		}
		if (!step.getTravel().contains(itemManager.canonicalize(itemId)))
		{
			return;
		}
		Rectangle bounds = item.getCanvasBounds();
		g.drawImage(itemManager.getItemOutline(itemId, item.getQuantity(), TravelSettings.color(configManager)),
			bounds.x, bounds.y, null);
	}
}
