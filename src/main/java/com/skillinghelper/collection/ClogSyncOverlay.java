package com.skillinghelper.collection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * While the log needs a sync, outlines the collection log's Search button with "Click Search to sync": clicking it
 * makes the game list every filled slot, which the plugin reads in one go. Only draws; the player clicks.
 */
class ClogSyncOverlay extends Overlay
{
	private static final Color COLOR = new Color(255, 215, 90);
	private static final String LABEL = "Click Search to sync";

	private final Client client;
	private final CollectionLogPlugin plugin;
	private final CollectionLogConfig config;

	ClogSyncOverlay(Client client, CollectionLogPlugin plugin, CollectionLogConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!plugin.isNeedsSync() || !config.highlightSearch())
		{
			return null;
		}
		Widget search = client.getWidget(InterfaceID.Collection.SEARCH_TOGGLE);
		if (search == null || search.isHidden())
		{
			return null;
		}
		Rectangle b = search.getBounds();
		if (b == null || b.width <= 0)
		{
			return null;
		}
		g.setColor(COLOR);
		g.setStroke(new BasicStroke(2));
		g.drawRect(b.x - 1, b.y - 1, b.width + 2, b.height + 2);
		g.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = g.getFontMetrics();
		int x = b.x + b.width / 2 - fm.stringWidth(LABEL) / 2;
		int y = b.y - 4;
		g.setColor(Color.BLACK);
		g.drawString(LABEL, x + 1, y + 1);
		g.setColor(COLOR);
		g.drawString(LABEL, x, y);
		return null;
	}
}
