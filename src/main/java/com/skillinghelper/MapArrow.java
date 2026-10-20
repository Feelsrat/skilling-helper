package com.skillinghelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * The world map marker image, drawn once in code so no extra resource is needed.
 */
final class MapArrow
{
	private static BufferedImage image;

	static synchronized BufferedImage image()
	{
		if (image == null)
		{
			BufferedImage img = new BufferedImage(20, 24, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Polygon arrow = new Polygon(new int[]{10, 19, 13, 13, 7, 7, 1}, new int[]{23, 12, 12, 1, 1, 12, 12}, 7);
			g.setColor(new Color(0, 255, 255));
			g.fillPolygon(arrow);
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(1.5f));
			g.drawPolygon(arrow);
			g.dispose();
			image = img;
		}
		return image;
	}

	private MapArrow()
	{
	}
}
