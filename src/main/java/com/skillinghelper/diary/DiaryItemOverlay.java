package com.skillinghelper.diary;

import com.skillinghelper.travel.TravelSettings;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.function.Supplier;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Outlines what to take out of the bank for the next step (a travel item in its own colour), and the inventory
 * items to use. Shared by the guides that run on {@link DiaryStep}s (diaries, account unlocks).
 */
public class DiaryItemOverlay extends WidgetItemOverlay
{
	private final Supplier<DiaryStep> step;
	private final Supplier<Color> color;
	private final ItemManager itemManager;
	private final ConfigManager configManager;

	public DiaryItemOverlay(Supplier<DiaryStep> step, Supplier<Color> color, ItemManager itemManager,
		ConfigManager configManager)
	{
		this.step = step;
		this.color = color;
		this.itemManager = itemManager;
		this.configManager = configManager;
		showOnBank();
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D g, int itemId, WidgetItem item)
	{
		DiaryStep step = this.step.get();
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
		Color outline = travel ? TravelSettings.color(configManager) : color.get();
		Rectangle bounds = item.getCanvasBounds();
		g.drawImage(itemManager.getItemOutline(itemId, item.getQuantity(), outline), bounds.x, bounds.y, null);
	}
}
