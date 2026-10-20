package com.skillinghelper.overlay;

import com.skillinghelper.ActiveGuide;
import com.skillinghelper.SkillingHelperConfig;
import com.skillinghelper.SkillingHelperPlugin;
import com.skillinghelper.TargetTracker;
import com.skillinghelper.data.Target;
import com.skillinghelper.engine.GuideStep;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Highlights the objects and NPCs to click for the current step, banks when banking, and the destination tile.
 */
public class SceneHighlightOverlay extends Overlay
{
	private static final int MAX_DISTANCE = 24;

	private final Client client;
	private final SkillingHelperPlugin plugin;
	private final SkillingHelperConfig config;
	private final TargetTracker tracker;
	private final ModelOutlineRenderer outlines;

	@Inject
	SceneHighlightOverlay(Client client, SkillingHelperPlugin plugin, SkillingHelperConfig config, TargetTracker tracker,
		ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.tracker = tracker;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		ActiveGuide guide = plugin.getGuide();
		GuideStep step = guide.getStep();
		Player player = client.getLocalPlayer();
		if (step == null || player == null)
		{
			return null;
		}
		WorldPoint me = player.getWorldLocation();
		List<Target> targets = step.getHighlights();
		boolean bank = step.isHighlightBank();

		if (!targets.isEmpty() || bank)
		{
			for (TargetTracker.Tracked<TileObject> t : tracker.getObjects())
			{
				TileObject obj = t.getEntity();
				if (!inRange(obj.getWorldLocation(), me))
				{
					continue;
				}
				Color c = colorFor(Target.Kind.OBJECT, t.getName(), t.getActions(), targets, bank);
				if (c != null)
				{
					drawObject(g, obj, c);
				}
			}
			for (TargetTracker.Tracked<NPC> t : tracker.getNpcs())
			{
				NPC npc = t.getEntity();
				if (!inRange(npc.getWorldLocation(), me))
				{
					continue;
				}
				Color c = colorFor(Target.Kind.NPC, t.getName(), t.getActions(), targets, bank);
				if (c != null)
				{
					drawNpc(g, npc, c);
				}
			}
		}

		WorldPoint dest = step.getDestination();
		if (config.destinationTile() && dest != null && dest.getPlane() == me.getPlane())
		{
			LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), dest);
			if (lp != null)
			{
				Polygon poly = Perspective.getCanvasTilePoly(client, lp);
				if (poly != null)
				{
					OverlayUtil.renderPolygon(g, poly, config.targetColor(), new BasicStroke(2));
				}
			}
		}
		return null;
	}

	private static boolean inRange(WorldPoint wp, WorldPoint me)
	{
		return wp != null && wp.getPlane() == me.getPlane() && wp.distanceTo2D(me) <= MAX_DISTANCE;
	}

	private Color colorFor(Target.Kind kind, String name, String[] actions, List<Target> targets, boolean bank)
	{
		for (Target t : targets)
		{
			if (t.getKind() == kind && t.matches(name, actions))
			{
				return config.targetColor();
			}
		}
		if (bank)
		{
			for (Target t : TargetTracker.BANK_TARGETS)
			{
				if (t.getKind() == kind && t.matches(name, actions))
				{
					return config.bankColor();
				}
			}
		}
		return null;
	}

	private void drawObject(Graphics2D g, TileObject obj, Color c)
	{
		switch (config.highlightStyle())
		{
			case CLICKBOX:
				Shape box = obj.getClickbox();
				if (box != null)
				{
					OverlayUtil.renderPolygon(g, box, c, fill(c), new BasicStroke(2));
				}
				break;
			case TILE:
				Polygon tile = obj.getCanvasTilePoly();
				if (tile != null)
				{
					OverlayUtil.renderPolygon(g, tile, c, fill(c), new BasicStroke(2));
				}
				break;
			case OUTLINE:
			default:
				outlines.drawOutline(obj, 2, c, 4);
				break;
		}
	}

	private void drawNpc(Graphics2D g, NPC npc, Color c)
	{
		switch (config.highlightStyle())
		{
			case CLICKBOX:
				Shape hull = npc.getConvexHull();
				if (hull != null)
				{
					OverlayUtil.renderPolygon(g, hull, c, fill(c), new BasicStroke(2));
				}
				break;
			case TILE:
				Polygon tile = npc.getCanvasTilePoly();
				if (tile != null)
				{
					OverlayUtil.renderPolygon(g, tile, c, fill(c), new BasicStroke(2));
				}
				break;
			case OUTLINE:
			default:
				outlines.drawOutline(npc, 2, c, 4);
				break;
		}
	}

	private static Color fill(Color c)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(c.getAlpha(), 40));
	}
}
