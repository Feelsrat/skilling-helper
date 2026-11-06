package com.skillinghelper.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The painted look shared by every panel: rounded cards, pill buttons and section headings. {@link GuideUi}
 * builds everything out of these, so a panel never paints on its own.
 */
public final class Skin
{
	/**
	 * Panel background, behind every card.
	 */
	public static final Color BG = new Color(30, 30, 32);
	/**
	 * A card: the step, a list row, the header.
	 */
	public static final Color CARD = new Color(42, 42, 45);
	public static final Color CARD_HOVER = new Color(54, 54, 58);
	/**
	 * Hairline round a card, so it lifts off the background.
	 */
	public static final Color EDGE = new Color(60, 60, 64);
	/**
	 * Inset wells inside a card: an options box, a bar's track.
	 */
	public static final Color WELL = new Color(24, 24, 26);
	public static final Color BUTTON = new Color(58, 58, 62);
	public static final Color ACCENT = new Color(232, 140, 30);
	public static final Color ACCENT_DARK = new Color(186, 96, 14);

	static final int RADIUS = 10;

	private Skin()
	{
	}

	static Graphics2D smooth(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		return g2;
	}

	static Color mix(Color a, Color b, double t)
	{
		return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
			(int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
			(int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}

	/**
	 * A rounded card. It fills with its background colour (so {@code setBackground} still works for hover), can
	 * have a coloured strip down its left edge, a hairline round it and a soft top-to-bottom shade.
	 */
	public static class Surface extends JPanel
	{
		private final int radius;
		private Color accent;
		private Color edge = EDGE;
		private Color shadeTo;

		public Surface(LayoutManager layout, int radius)
		{
			super(layout);
			this.radius = radius;
			setOpaque(false);
			setBackground(CARD);
		}

		/**
		 * The coloured strip down the left edge, or null for none.
		 */
		public Surface accent(Color accent)
		{
			this.accent = accent;
			repaint();
			return this;
		}

		public Surface edge(Color edge)
		{
			this.edge = edge;
			return this;
		}

		/**
		 * Shades the fill from its background at the top to {@code to} at the bottom.
		 */
		public Surface shade(Color to)
		{
			this.shadeTo = to;
			return this;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = smooth(g);
			int w = getWidth();
			int h = getHeight();
			Shape shape = new RoundRectangle2D.Float(0, 0, w, h, radius, radius);
			g2.setPaint(shadeTo == null ? getBackground() : new GradientPaint(0, 0, getBackground(), 0, h, shadeTo));
			g2.fill(shape);
			if (accent != null)
			{
				g2.clip(shape);
				g2.setColor(accent);
				g2.fillRect(0, 0, 3, h);
				g2.setClip(null);
			}
			if (edge != null)
			{
				g2.setColor(edge);
				g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, radius, radius));
			}
			g2.dispose();
		}
	}

	/**
	 * A rounded button. Its colour is its background, a little lighter under the mouse and darker while pressed;
	 * {@link #primary} ones are a warm orange with a soft shine.
	 */
	public static class Pill extends JButton
	{
		private boolean primary;

		public Pill(String label)
		{
			super(label);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setRolloverEnabled(true);
			setBackground(BUTTON);
			setForeground(GuideUi.TEXT);
			setBorder(new EmptyBorder(4, 3, 4, 3));
		}

		Pill primary()
		{
			primary = true;
			setBackground(ACCENT);
			setForeground(Color.WHITE);
			setBorder(new EmptyBorder(7, 8, 7, 8));
			return this;
		}

		@Override
		public void setBackground(Color bg)
		{
			// select(tab, false, ...) passes null: back to the plain button colour
			super.setBackground(bg == null ? BUTTON : bg);
		}

		@Override
		public void setMargin(Insets m)
		{
			// Padding comes from the border; keep old callers' margins from fighting it
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = smooth(g);
			ButtonModel m = getModel();
			Color base = getBackground();
			if (!isEnabled())
			{
				base = mix(base, BG, 0.5);
			}
			else if (m.isPressed())
			{
				base = mix(base, Color.BLACK, 0.15);
			}
			else if (m.isRollover())
			{
				base = mix(base, Color.WHITE, 0.12);
			}
			int w = getWidth();
			int h = getHeight();
			int r = Math.min(h, 12);
			if (primary)
			{
				g2.setPaint(new GradientPaint(0, 0, mix(base, Color.WHITE, 0.12), 0, h, mix(base, ACCENT_DARK, 0.6)));
			}
			else
			{
				g2.setColor(base);
			}
			g2.fillRoundRect(0, 0, w, h, r, r);
			if (primary)
			{
				g2.setColor(new Color(255, 255, 255, 40));
				g2.drawLine(r / 2, 1, w - r / 2, 1);
			}
			g2.dispose();
			super.paintComponent(g);
		}
	}

	/**
	 * A section heading: bold text after a short orange bar, with a hairline running off to the right.
	 */
	public static class Heading extends JLabel
	{
		public Heading(String title)
		{
			super(title);
			setForeground(GuideUi.BRIGHT);
			setBorder(new EmptyBorder(14, 9, 6, 0));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = smooth(g);
			Insets in = getInsets();
			int textH = getHeight() - in.top - in.bottom;
			g2.setColor(ACCENT);
			g2.fillRoundRect(0, in.top + 1, 3, Math.max(4, textH - 2), 3, 3);
			FontMetrics fm = getFontMetrics(getFont());
			String plain = getText().replaceAll("<[^>]*>", "").replace("&nbsp;", " ");
			int end = in.left + fm.stringWidth(plain) + 10;
			if (end < getWidth())
			{
				g2.setColor(EDGE);
				g2.drawLine(end, in.top + textH / 2, getWidth(), in.top + textH / 2);
			}
			g2.dispose();
			super.paintComponent(g);
		}
	}
}
