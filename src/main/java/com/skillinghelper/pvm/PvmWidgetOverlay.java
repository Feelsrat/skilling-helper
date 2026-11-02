package com.skillinghelper.pvm;

import com.skillinghelper.pvm.guide.PvmStep;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;

/**
 * Outlines what to click in the game's own windows: the dialogue option the step asks for and the answer to the
 * Barrows door puzzle.
 */
class PvmWidgetOverlay extends Overlay
{
	private final Client client;
	private final PvmProgressionPlugin plugin;
	private final PvmProgressionConfig config;

	@Inject
	PvmWidgetOverlay(Client client, PvmProgressionPlugin plugin, PvmProgressionConfig config)
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
		PvmStep step = plugin.getStep();
		if (step == null)
		{
			return null;
		}
		g.setStroke(new BasicStroke(2));
		Widget answer = plugin.getPuzzleAnswer();
		if (answer != null && !answer.isHidden())
		{
			g.setColor(Color.GREEN);
			g.draw(answer.getBounds());
		}
		String option = step.getDialogOption();
		if (option != null)
		{
			Widget options = client.getWidget(InterfaceID.Chatmenu.OPTIONS);
			if (options != null && !options.isHidden() && options.getDynamicChildren() != null)
			{
				String want = option.toLowerCase(Locale.ROOT);
				for (Widget o : options.getDynamicChildren())
				{
					String text = o.getText();
					if (text != null && Text.removeTags(text).toLowerCase(Locale.ROOT).contains(want))
					{
						g.setColor(config.targetColor());
						g.draw(o.getBounds());
					}
				}
			}
		}
		return null;
	}
}
