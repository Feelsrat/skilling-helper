package com.skillinghelper.pvm;

import com.skillinghelper.TargetTracker;
import com.skillinghelper.data.Target;
import com.skillinghelper.pvm.guide.PvmStep;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
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
 * Outlines what the running guide says to click: the boss and its adds, gates, banks.
 */
class PvmSceneOverlay extends Overlay
{
	private static final int MAX_DISTANCE = 24;

	private final Client client;
	private final PvmProgressionPlugin plugin;
	private final PvmProgressionConfig config;
	private final GuideTargets targets;
	private final ModelOutlineRenderer outlines;

	@Inject
	PvmSceneOverlay(Client client, PvmProgressionPlugin plugin, PvmProgressionConfig config, GuideTargets targets,
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
		PvmStep step = plugin.getStep();
		Player player = client.getLocalPlayer();
		if (step == null || player == null)
		{
			return null;
		}
		boolean fight = step.getKind() == PvmStep.Kind.FIGHT;
		if (fight && !config.fightHelp())
		{
			return null;
		}
		WorldPoint me = player.getWorldLocation();
		for (GuideTargets.Tracked<NPC> t : targets.getNpcs())
		{
			if (inRange(t.getEntity().getWorldLocation(), me))
			{
				Color c = colorFor(Target.Kind.NPC, t.getName(), t.getActions(), step);
				if (c != null)
				{
					outlines.drawOutline(t.getEntity(), 2, c, 4);
				}
			}
		}
		for (GuideTargets.Tracked<TileObject> t : targets.getObjects())
		{
			if (inRange(t.getEntity().getWorldLocation(), me))
			{
				Color c = colorFor(Target.Kind.OBJECT, t.getName(), t.getActions(), step);
				if (c != null)
				{
					outlines.drawOutline(t.getEntity(), 2, c, 4);
				}
			}
		}
		return null;
	}

	private Color colorFor(Target.Kind kind, String name, String[] actions, PvmStep step)
	{
		if (matches(step.getHighlights(), kind, name, actions))
		{
			return config.targetColor();
		}
		if (matches(step.getAddHighlights(), kind, name, actions))
		{
			return config.addColor();
		}
		if (step.isHighlightBank() && matches(TargetTracker.BANK_TARGETS, kind, name, actions))
		{
			return config.bankColor();
		}
		return null;
	}

	private static boolean matches(List<Target> targets, Target.Kind kind, String name, String[] actions)
	{
		for (Target t : targets)
		{
			if (t.getKind() == kind && t.matches(name, actions))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean inRange(WorldPoint wp, WorldPoint me)
	{
		return wp != null && wp.getPlane() == me.getPlane() && wp.distanceTo2D(me) <= MAX_DISTANCE;
	}
}
