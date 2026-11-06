package com.skillinghelper.collection;

import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.overlay.NameTargets;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines what the running guide says to click: the boss or monster, and the entrance.
 */
class ClogSceneOverlay extends Overlay
{
	private static final int MAX_DISTANCE = 24;

	private final Client client;
	private final CollectionLogPlugin plugin;
	private final CollectionLogConfig config;
	private final NameTargets targets;
	private final ModelOutlineRenderer outlines;

	ClogSceneOverlay(Client client, CollectionLogPlugin plugin, CollectionLogConfig config, NameTargets targets,
		ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.targets = targets;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		DiaryStep step = plugin.getStep();
		Player player = client.getLocalPlayer();
		if (step == null || player == null || !config.outlineTargets())
		{
			return null;
		}
		WorldPoint me = player.getWorldLocation();
		for (Map.Entry<NPC, String> e : targets.getNpcs())
		{
			if (step.getNpcs().contains(e.getValue()) && near(e.getKey().getWorldLocation(), me))
			{
				outlines.drawOutline(e.getKey(), 2, config.targetColor(), 4);
			}
		}
		for (Map.Entry<TileObject, String> e : targets.getObjects())
		{
			if (step.getObjects().contains(e.getValue()) && near(e.getKey().getWorldLocation(), me))
			{
				outlines.drawOutline(e.getKey(), 2, config.targetColor(), 4);
			}
		}
		return null;
	}

	private static boolean near(WorldPoint wp, WorldPoint me)
	{
		return wp != null && wp.distanceTo2D(me) <= MAX_DISTANCE;
	}
}
