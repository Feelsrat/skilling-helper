package com.skillinghelper.planner;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * A small box on the game screen while the Account Planner's route runs: the step being guided now, which plugin
 * guides it, and what comes after. The lines are set on the client thread when the plan changes, never worked out
 * per frame.
 */
class RouteOverlay extends OverlayPanel
{
	private static final Color NOW = new Color(255, 215, 90);
	private static final Color MUTED = new Color(170, 170, 170);

	private volatile String now;
	private volatile String guide;
	private volatile String next;
	private volatile int done;

	@Inject
	RouteOverlay()
	{
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setPreferredSize(new Dimension(180, 0));
	}

	/**
	 * Null {@code now} hides the box.
	 */
	void setLines(String now, String guide, String next, int done)
	{
		this.now = now;
		this.guide = guide;
		this.next = next;
		this.done = done;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		String n = now;
		if (n == null)
		{
			return null;
		}
		panelComponent.getChildren().add(TitleComponent.builder().text("Account route").color(NOW).build());
		panelComponent.getChildren().add(LineComponent.builder().left("Now: " + n).leftColor(Color.WHITE).build());
		String g = guide;
		if (g != null)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Follow " + g).leftColor(MUTED).build());
		}
		String x = next;
		if (x != null)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Then: " + x).leftColor(MUTED).build());
		}
		if (done > 0)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Done this session").right(
				String.valueOf(done)).leftColor(MUTED).build());
		}
		return super.render(graphics);
	}
}
