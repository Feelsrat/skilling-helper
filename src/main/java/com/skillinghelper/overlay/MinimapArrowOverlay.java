package com.skillinghelper.overlay;

import com.skillinghelper.SkillingHelperConfig;
import com.skillinghelper.SkillingHelperPlugin;
import com.skillinghelper.engine.GuideStep;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Points from the player to the destination on the minimap, or marks it when it's close enough to be shown.
 */
public class MinimapArrowOverlay extends Overlay
{
	// Stay inside the 20 tile radius Perspective.localToMinimap allows
	private static final int ARROW_TILES = 15;

	private final Client client;
	private final SkillingHelperPlugin plugin;
	private final SkillingHelperConfig config;

	@Inject
	MinimapArrowOverlay(Client client, SkillingHelperPlugin plugin, SkillingHelperConfig config)
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
		if (!config.minimapArrow())
		{
			return null;
		}
		GuideStep step = plugin.getGuide().getStep();
		Player player = client.getLocalPlayer();
		if (step == null || step.getDestination() == null || player == null)
		{
			return null;
		}
		WorldView wv = client.getTopLevelWorldView();
		WorldPoint dest = step.getDestination();
		WorldPoint me = player.getWorldLocation();
		LocalPoint myLocal = player.getLocalLocation();
		int dx = dest.getX() - me.getX();
		int dy = dest.getY() - me.getY();
		double dist = Math.hypot(dx, dy);
		Color color = config.targetColor();

		if (dist <= ARROW_TILES)
		{
			LocalPoint lp = LocalPoint.fromWorld(wv, dest);
			Point mini = lp == null ? null : Perspective.localToMinimap(client, lp);
			if (mini != null)
			{
				OverlayUtil.renderMinimapLocation(g, mini, color);
			}
			return null;
		}

		double scale = ARROW_TILES / dist * Perspective.LOCAL_TILE_SIZE;
		LocalPoint tip = new LocalPoint(myLocal.getX() + (int) (dx * scale), myLocal.getY() + (int) (dy * scale), wv);
		Point from = Perspective.localToMinimap(client, myLocal);
		Point to = Perspective.localToMinimap(client, tip);
		if (from == null || to == null)
		{
			return null;
		}
		drawArrow(g, from, to, color);
		return null;
	}

	private static void drawArrow(Graphics2D g, Point from, Point to, Color color)
	{
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		double angle = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
		// Start the shaft a little away from the player dot so it doesn't cover it
		int sx = (int) (from.getX() + Math.cos(angle) * 12);
		int sy = (int) (from.getY() + Math.sin(angle) * 12);
		g.setStroke(new BasicStroke(3));
		g.setColor(Color.BLACK);
		g.drawLine(sx, sy, to.getX(), to.getY());
		g.setStroke(new BasicStroke(2));
		g.setColor(color);
		g.drawLine(sx, sy, to.getX(), to.getY());

		Polygon head = new Polygon();
		head.addPoint(to.getX(), to.getY());
		head.addPoint((int) (to.getX() - 9 * Math.cos(angle - 0.5)), (int) (to.getY() - 9 * Math.sin(angle - 0.5)));
		head.addPoint((int) (to.getX() - 9 * Math.cos(angle + 0.5)), (int) (to.getY() - 9 * Math.sin(angle + 0.5)));
		g.fillPolygon(head);
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(1));
		g.drawPolygon(head);
	}
}
