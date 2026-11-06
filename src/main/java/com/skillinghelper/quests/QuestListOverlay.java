package com.skillinghelper.quests;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;

/**
 * Outlines the next quest's line in the in-game Quest list, so it's easy to find and right-click for Quest Helper.
 */
class QuestListOverlay extends Overlay
{
	private final Client client;
	private final QuestOrderPlugin plugin;
	private final QuestOrderConfig config;
	/**
	 * Index of the matching line last frame, tried first.
	 */
	private int lastIndex = -1;

	@Inject
	QuestListOverlay(Client client, QuestOrderPlugin plugin, QuestOrderConfig config)
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
		QuestStep step = plugin.getStep();
		String name = step == null ? null : step.getQuestListName();
		if (name == null)
		{
			return null;
		}
		Widget list = client.getWidget(InterfaceID.Questlist.LIST);
		Widget box = client.getWidget(InterfaceID.Questlist.LIST_CONTAINER);
		if (list == null || list.isHidden() || box == null)
		{
			return null;
		}
		Widget[] lines = list.getDynamicChildren();
		if (lines == null)
		{
			return null;
		}
		Widget line = null;
		if (lastIndex >= 0 && lastIndex < lines.length && matches(lines[lastIndex], name))
		{
			line = lines[lastIndex];
		}
		else
		{
			for (int i = 0; i < lines.length; i++)
			{
				if (matches(lines[i], name))
				{
					line = lines[i];
					lastIndex = i;
					break;
				}
			}
		}
		if (line == null)
		{
			return null;
		}
		Rectangle r = line.getBounds();
		Rectangle visible = box.getBounds();
		if (r == null || visible == null || !visible.intersects(r))
		{
			return null;
		}
		Shape old = g.getClip();
		g.clip(visible);
		g.setColor(config.targetColor());
		g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
		g.setClip(old);
		return null;
	}

	private static boolean matches(Widget w, String name)
	{
		String text = w == null ? null : w.getText();
		return text != null && !text.isEmpty() && name.equalsIgnoreCase(Text.removeTags(text).trim());
	}
}
