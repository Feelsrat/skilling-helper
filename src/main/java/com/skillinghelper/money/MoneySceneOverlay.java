package com.skillinghelper.money;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines what the running money guide says to click: the shopkeeper, or bankers and booths for supplies.
 */
class MoneySceneOverlay extends Overlay
{
	private static final int MAX_DISTANCE = 24;

	private final Client client;
	private final MoneyMakingPlugin plugin;
	private final MoneyMakingConfig config;
	private final MoneyTargets targets;
	private final ModelOutlineRenderer outlines;

	@Inject
	MoneySceneOverlay(Client client, MoneyMakingPlugin plugin, MoneyMakingConfig config, MoneyTargets targets,
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
		MoneyStep step = plugin.getStep();
		Player player = client.getLocalPlayer();
		if (step == null || player == null)
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
		for (Map.Entry<GameObject, String> e : targets.getObjects())
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
		return wp != null && wp.getPlane() == me.getPlane() && wp.distanceTo2D(me) <= MAX_DISTANCE;
	}
}
