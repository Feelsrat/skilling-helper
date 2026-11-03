package com.skillinghelper.plunder;

import com.skillinghelper.SkillingHelperConfig;
import com.skillinghelper.overlay.SceneHighlightOverlay;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * At and in the pyramid: outlines the one object to click next, with the menu option to pick above it.
 */
public class PlunderOverlay extends Overlay
{
	private static final int MAX_DISTANCE = 24;

	private final Client client;
	private final PlunderTracker tracker;
	private final SkillingHelperConfig config;
	private final ModelOutlineRenderer outlines;

	@Inject
	PlunderOverlay(Client client, PlunderTracker tracker, SkillingHelperConfig config, ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.tracker = tracker;
		this.config = config;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		TileObject obj = tracker.getHighlight();
		Player me = client.getLocalPlayer();
		if (obj == null || me == null || obj.getPlane() != client.getTopLevelWorldView().getPlane()
			|| obj.getWorldLocation().distanceTo2D(me.getWorldLocation()) > MAX_DISTANCE)
		{
			return null;
		}
		Color c = config.targetColor();
		SceneHighlightOverlay.drawObject(g, obj, c, config.highlightStyle(), outlines);
		if (config.plunderUrnNumbers() && tracker.isUrnsNext())
		{
			drawRouteNumbers(g, tracker.getUrnRoute(), c);
		}
		String option = tracker.getHighlightOption();
		if (config.plunderOptionText() && option != null)
		{
			Point at = obj.getCanvasTextLocation(g, option, 150);
			if (at != null)
			{
				OverlayUtil.renderTextLocation(g, at, option, new Color(c.getRed(), c.getGreen(), c.getBlue()));
			}
		}
		return null;
	}

	/**
	 * The urns after the next one, numbered in walking order, without outlines so only the next click stands out.
	 */
	private void drawRouteNumbers(Graphics2D g, List<TileObject> route, Color c)
	{
		Color dim = new Color(c.getRed(), c.getGreen(), c.getBlue(), 170);
		for (int i = 1; i < route.size(); i++)
		{
			String n = String.valueOf(i + 1);
			Point at = route.get(i).getCanvasTextLocation(g, n, 60);
			if (at != null)
			{
				OverlayUtil.renderTextLocation(g, at, n, dim);
			}
		}
	}
}
