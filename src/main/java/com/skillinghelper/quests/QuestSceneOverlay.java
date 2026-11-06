package com.skillinghelper.quests;

import com.skillinghelper.overlay.NameTargets;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines the NPC who starts the next quest once the player is near the start.
 */
class QuestSceneOverlay extends Overlay
{
	private static final int MAX_DISTANCE = 24;

	private final Client client;
	private final QuestOrderPlugin plugin;
	private final QuestOrderConfig config;
	private final NameTargets targets;
	private final ModelOutlineRenderer outlines;

	QuestSceneOverlay(Client client, QuestOrderPlugin plugin, QuestOrderConfig config, NameTargets targets,
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
		QuestStep step = plugin.getStep();
		Player player = client.getLocalPlayer();
		if (step == null || step.getNpc() == null || player == null)
		{
			return null;
		}
		WorldPoint me = player.getWorldLocation();
		for (Map.Entry<NPC, String> e : targets.getNpcs())
		{
			WorldPoint wp = e.getKey().getWorldLocation();
			if (step.getNpc().equals(e.getValue()) && wp != null && wp.distanceTo2D(me) <= MAX_DISTANCE)
			{
				outlines.drawOutline(e.getKey(), 2, config.targetColor(), 4);
			}
		}
		return null;
	}
}
