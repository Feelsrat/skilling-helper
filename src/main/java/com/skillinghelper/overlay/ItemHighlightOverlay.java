package com.skillinghelper.overlay;

import com.skillinghelper.ActiveGuide;
import com.skillinghelper.SkillingHelperConfig;
import com.skillinghelper.SkillingHelperPlugin;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemHighlight;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Outlines inventory items to use, wear or drop, and bank items to withdraw.
 */
public class ItemHighlightOverlay extends WidgetItemOverlay
{
	private final SkillingHelperPlugin plugin;
	private final SkillingHelperConfig config;
	private final ItemManager itemManager;

	@Inject
	ItemHighlightOverlay(SkillingHelperPlugin plugin, SkillingHelperConfig config, ItemManager itemManager)
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
		ActiveGuide guide = plugin.getGuide();
		GuideStep step = guide.getStep();
		if (step == null)
		{
			return;
		}
		int group = item.getWidget().getId() >>> 16;
		ItemHighlight kind;
		if (group == InterfaceID.BANKMAIN)
		{
			kind = guide.getBankItems().get(itemManager.canonicalize(itemId));
		}
		else
		{
			kind = step.getItemHighlights().get(itemId);
			if (kind == ItemHighlight.WITHDRAW)
			{
				kind = null;
			}
			else if (kind == ItemHighlight.DEPOSIT && group != InterfaceID.BANKSIDE)
			{
				kind = null;
			}
		}
		if (kind == null)
		{
			return;
		}
		Color color = colorFor(kind);
		Rectangle bounds = item.getCanvasBounds();
		BufferedImage outline = itemManager.getItemOutline(itemId, item.getQuantity(), color);
		g.drawImage(outline, (int) bounds.getX(), (int) bounds.getY(), null);
	}

	private Color colorFor(ItemHighlight kind)
	{
		switch (kind)
		{
			case EQUIP:
				return config.equipColor();
			case DROP:
			case DEPOSIT:
				return config.dropColor();
			case USE:
			case WITHDRAW:
			default:
				return config.useColor();
		}
	}
}
