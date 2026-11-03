package com.skillinghelper.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The look every plugin in this jar uses for its side panel text, so they all read the same way: short lines with a
 * marker gutter, key bits highlighted (see {@link GuideText}), quiet asides, label/value rows for numbers, and the
 * current step in a card.
 * <p>
 * Widths are in the same units as the old {@code TEXT_WIDTH}: {@link #WIDTH} for the panel body and
 * {@link #CARD_WIDTH} inside a card or list row.
 */
public final class GuideUi
{
	public static final int WIDTH = 165;
	public static final int CARD_WIDTH = WIDTH - 15;

	public static final Color TEXT = new Color(200, 200, 200);
	public static final Color BRIGHT = Color.WHITE;
	public static final Color MUTED = new Color(150, 150, 150);
	public static final Color GOOD = new Color(80, 200, 80);
	public static final Color WARN = new Color(255, 170, 0);
	public static final Color BAD = new Color(230, 80, 80);
	public static final Color HINT = new Color(110, 170, 255);
	public static final Color GOLD = new Color(255, 215, 90);
	/**
	 * Buttons and menu options, close to the game's own orange.
	 */
	public static final Color OPTION = new Color(255, 150, 60);

	private static final GuideText.Palette PALETTE = new GuideText.Palette(hex(BRIGHT), hex(GOLD), hex(OPTION));
	private static final Font MARKER_FONT = new Font(Font.DIALOG, Font.PLAIN, 11);
	private static final int GUTTER = 11;
	private static final int NUMBER_GUTTER = 14;

	private GuideUi()
	{
	}

	// ---- Guide lines ----

	/**
	 * One guide line in normal text colour, split up and highlighted.
	 */
	public static JComponent line(String raw, int width)
	{
		return line(raw, null, TEXT, width);
	}

	/**
	 * One guide line drawn in a status colour (no inline highlights, so the colour reads clearly).
	 */
	public static JComponent line(String raw, Color color, int width)
	{
		return line(raw, color, color, width);
	}

	/**
	 * One guide line.
	 *
	 * @param markerColor colour for the leading "✔" / "•", or null to pick one from the marker
	 * @param textColor   {@link #TEXT} to get inline highlights, or a status colour for plain coloured text
	 */
	public static JComponent line(String raw, Color markerColor, Color textColor, int width)
	{
		GuideText.Line line = GuideText.parse(raw);
		JPanel body = column();
		boolean highlight = TEXT.equals(textColor);
		int textWidth = line.getMarker() == null ? width : width - gutter(line.getMarker()) - 3;
		List<GuideText.Part> parts = line.getParts();
		for (int i = 0; i < parts.size(); i++)
		{
			GuideText.Part part = parts.get(i);
			String html;
			Color color;
			if (part.isAside())
			{
				html = GuideText.escape(part.getText());
				color = MUTED;
			}
			else
			{
				html = highlight ? GuideText.highlight(part.getText(), PALETTE) : GuideText.escape(part.getText());
				color = textColor;
			}
			if (i == 0 && line.getLabel() != null)
			{
				html = "<font color='" + hex(labelColor(line.getLabel())) + "'>"
					+ GuideText.escape(line.getLabel().toUpperCase(Locale.ROOT)) + "</font>&nbsp;&nbsp;" + html;
			}
			if (i > 0)
			{
				body.add(Box.createVerticalStrut(1));
			}
			body.add(html(html, color, textWidth));
		}
		if (line.getMarker() == null)
		{
			return full(body);
		}
		JPanel row = new JPanel(new BorderLayout(3, 0));
		row.setOpaque(false);
		// "- item" lists get the same dot as "• item" ones
		JLabel marker = new JLabel(line.getMarker().equals("-") ? "•" : line.getMarker());
		// The game fonts have no ✔ / ✘ / ➜, so markers use a normal font
		marker.setFont(Character.isLetterOrDigit(line.getMarker().charAt(0)) ? FontManager.getRunescapeSmallFont()
			: MARKER_FONT);
		marker.setForeground(markerColor != null ? markerColor : markerColor(line.getMarker()));
		marker.setVerticalAlignment(SwingConstants.TOP);
		marker.setPreferredSize(new Dimension(gutter(line.getMarker()), marker.getPreferredSize().height));
		row.add(marker, BorderLayout.WEST);
		row.add(body, BorderLayout.CENTER);
		return full(row);
	}

	/**
	 * Adds each line with a small gap between them.
	 */
	public static void lines(JComponent to, List<String> lines, int width)
	{
		for (int i = 0; i < lines.size(); i++)
		{
			if (i > 0)
			{
				to.add(Box.createVerticalStrut(3));
			}
			to.add(line(lines.get(i), width));
		}
	}

	/**
	 * "✔ Members" in green or "✘ 70 Agility" in red.
	 */
	public static JComponent check(boolean ok, String text, int width)
	{
		return line((ok ? "✔ " : "✘ ") + text, ok ? GOOD : BAD, ok ? TEXT : BAD, width);
	}

	// ---- The current step ----

	/**
	 * A dark card with a coloured edge, for the current step or a picked list entry. Add to it with
	 * {@link #fillStep} or directly.
	 */
	public static JPanel card(Color accent)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, accent), new EmptyBorder(6, 6, 7, 6)));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		return card;
	}

	/**
	 * Fills a {@link #card} with a step: a small heading ("NOW"), the step's title in bold, then its lines.
	 * A long title keeps its first sentence bold and drops the rest into the lines.
	 *
	 * @param kicker small heading above the title, or null
	 */
	public static void fillStep(JPanel card, String kicker, Color kickerColor, String title, List<String> lines)
	{
		card.removeAll();
		if (kicker != null)
		{
			JLabel k = new JLabel(kicker.toUpperCase(Locale.ROOT));
			k.setFont(FontManager.getRunescapeSmallFont());
			k.setForeground(kickerColor);
			card.add(left(k));
			card.add(Box.createVerticalStrut(2));
		}
		GuideText.Line t = GuideText.parse(title);
		JLabel heading = html(GuideText.highlight(t.main(), PALETTE), BRIGHT, CARD_WIDTH);
		heading.setFont(FontManager.getRunescapeBoldFont());
		card.add(heading);
		for (int i = 1; i < t.getParts().size(); i++)
		{
			GuideText.Part part = t.getParts().get(i);
			card.add(Box.createVerticalStrut(2));
			card.add(part.isAside() ? html(GuideText.escape(part.getText()), MUTED, CARD_WIDTH)
				: line(part.getText(), CARD_WIDTH));
		}
		if (!lines.isEmpty())
		{
			card.add(Box.createVerticalStrut(5));
			lines(card, lines, CARD_WIDTH);
		}
		card.revalidate();
		card.repaint();
	}

	// ---- Headings and numbers ----

	/**
	 * A section heading with a thin rule under it.
	 */
	public static JComponent section(String title)
	{
		JLabel l = new JLabel(title);
		l.setFont(FontManager.getRunescapeBoldFont());
		l.setForeground(ColorScheme.BRAND_ORANGE);
		l.setBorder(new CompoundBorder(new EmptyBorder(10, 0, 4, 0),
			new CompoundBorder(new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(0, 0, 2, 0))));
		return full(l);
	}

	/**
	 * A smaller heading inside a card or a section, e.g. "Wear:" / "Inventory:".
	 */
	public static JLabel subheading(String title)
	{
		JLabel l = new JLabel(title.toUpperCase(Locale.ROOT));
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(MUTED);
		l.setBorder(new EmptyBorder(4, 0, 1, 0));
		return left(l);
	}

	/**
	 * "Profit ............ ~120K gp/hr": a quiet label on the left and the number on the right.
	 */
	public static JComponent stat(String label, String value, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		JLabel l = new JLabel(label);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(MUTED);
		JLabel v = new JLabel(value);
		v.setFont(FontManager.getRunescapeSmallFont());
		v.setForeground(valueColor);
		v.setHorizontalAlignment(SwingConstants.RIGHT);
		row.add(l, BorderLayout.WEST);
		row.add(v, BorderLayout.CENTER);
		// Never ask for more width than the text column, so a long value is cut short instead of widening the panel
		Dimension pref = row.getPreferredSize();
		row.setPreferredSize(new Dimension(Math.min(pref.width, WIDTH), pref.height));
		return full(row);
	}

	/**
	 * A big bold line, e.g. the headline number of a section.
	 */
	public static JLabel headline(String s, Color color, int width)
	{
		JLabel l = text(s, color, width);
		l.setFont(FontManager.getRunescapeBoldFont());
		return l;
	}

	// ---- Plain building blocks ----

	/**
	 * Wrapped plain text, nothing split or highlighted.
	 */
	public static JLabel text(String s, Color color, int width)
	{
		return html(GuideText.escape(s), color, width);
	}

	/**
	 * Wrapped text from an HTML fragment that is already escaped.
	 */
	public static JLabel html(String fragment, Color color, int width)
	{
		JLabel l = new JLabel("<html><body style='width:" + width + "px'>" + fragment + "</body></html>");
		l.setForeground(color);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	public static JButton button(String label, Runnable action)
	{
		JButton b = new JButton(label);
		b.addActionListener(e -> action.run());
		return full(b);
	}

	public static <T extends JComponent> T left(T c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		return c;
	}

	public static <T extends JComponent> T full(T c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
		return c;
	}

	public static String hex(Color c)
	{
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	private static JPanel column()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setOpaque(false);
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		return p;
	}

	private static int gutter(String marker)
	{
		return Character.isDigit(marker.charAt(0)) ? NUMBER_GUTTER : GUTTER;
	}

	private static Color markerColor(String marker)
	{
		switch (marker)
		{
			case "✔":
				return GOOD;
			case "✘":
			case "✖":
				return BAD;
			case "⚠":
			case "▣":
			case "?":
				return WARN;
			case "★":
				return GOLD;
			case "▶":
				return OPTION;
			case "•":
			case "-":
				return MUTED;
			default:
				return HINT;
		}
	}

	private static Color labelColor(String label)
	{
		String l = label.toLowerCase(Locale.ROOT);
		if (l.equals("warning"))
		{
			return WARN;
		}
		if (l.equals("optional") || l.equals("where") || l.equals("why"))
		{
			return MUTED;
		}
		return HINT;
	}
}
