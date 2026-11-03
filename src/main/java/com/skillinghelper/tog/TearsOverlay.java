package com.skillinghelper.tog;

import com.skillinghelper.SkillingHelperConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * In the Tears of Guthix cave: outlines the blue streams to collect (the one with the most time left brightest) and
 * the green ones to avoid, each with a pie of the time it has left, like RuneLite's own Tears of Guthix plugin.
 */
public class TearsOverlay extends Overlay
{
	private static final Color BLUE = new Color(0, 200, 255);
	private static final Color BLUE_DIM = new Color(0, 120, 170);
	private static final Color GREEN = new Color(230, 60, 60);

	private final Client client;
	private final TogTracker tracker;
	private final SkillingHelperConfig config;
	private final ModelOutlineRenderer outlines;

	@Inject
	TearsOverlay(Client client, TogTracker tracker, SkillingHelperConfig config, ModelOutlineRenderer outlines)
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
		if (!config.togStreams() || !tracker.isPlaying())
		{
			return null;
		}
		Map<DecorativeObject, Integer> streams = tracker.getStreams();
		int tick = client.getTickCount();
		DecorativeObject freshest = null;
		int freshestTick = Integer.MIN_VALUE;
		for (Map.Entry<DecorativeObject, Integer> e : streams.entrySet())
		{
			if (TogTracker.isBlue(e.getKey().getId()) && e.getValue() > freshestTick)
			{
				freshest = e.getKey();
				freshestTick = e.getValue();
			}
		}
		for (Map.Entry<DecorativeObject, Integer> e : streams.entrySet())
		{
			DecorativeObject obj = e.getKey();
			boolean blue = TogTracker.isBlue(obj.getId());
			Color c = !blue ? GREEN : obj == freshest ? BLUE : BLUE_DIM;
			outlines.drawOutline(obj, obj == freshest ? 3 : 2, c, 4);
			Point at = obj.getCanvasLocation(100);
			if (at != null)
			{
				double left = 1 - Math.min(1, (tick - e.getValue()) / (double) TearsOfGuthix.STREAM_TICKS);
				ProgressPieComponent pie = new ProgressPieComponent();
				pie.setDiameter(15);
				pie.setFill(new Color(c.getRed(), c.getGreen(), c.getBlue(), 120));
				pie.setBorderColor(c);
				pie.setPosition(at);
				pie.setProgress(left);
				pie.render(g);
			}
		}
		return null;
	}
}
