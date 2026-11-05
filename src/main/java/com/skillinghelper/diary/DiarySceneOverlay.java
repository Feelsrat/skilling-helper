package com.skillinghelper.diary;

import com.skillinghelper.overlay.NameTargets;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import java.util.function.Supplier;
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
 * Outlines what the running guide says to click: the NPC to talk to, the obstacle or rock, the bank booth. Shared
 * by the guides that run on {@link DiaryStep}s (diaries, account unlocks).
 */
public class DiarySceneOverlay extends Overlay
{
	private static final int MAX_DISTANCE = 24;

	private final Client client;
	private final Supplier<DiaryStep> step;
	private final Supplier<Color> color;
	private final NameTargets targets;
	private final ModelOutlineRenderer outlines;

	public DiarySceneOverlay(Client client, Supplier<DiaryStep> step, Supplier<Color> color, NameTargets targets,
		ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.step = step;
		this.color = color;
		this.targets = targets;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		DiaryStep step = this.step.get();
		Player player = client.getLocalPlayer();
		if (step == null || player == null)
		{
			return null;
		}
		Color c = color.get();
		WorldPoint me = player.getWorldLocation();
		for (Map.Entry<NPC, String> e : targets.getNpcs())
		{
			if (step.getNpcs().contains(e.getValue()) && near(e.getKey().getWorldLocation(), me))
			{
				outlines.drawOutline(e.getKey(), 2, c, 4);
			}
		}
		for (Map.Entry<TileObject, String> e : targets.getObjects())
		{
			if (step.getObjects().contains(e.getValue()) && near(e.getKey().getWorldLocation(), me))
			{
				outlines.drawOutline(e.getKey(), 2, c, 4);
			}
		}
		return null;
	}

	private static boolean near(WorldPoint wp, WorldPoint me)
	{
		return wp != null && wp.distanceTo2D(me) <= MAX_DISTANCE;
	}
}
