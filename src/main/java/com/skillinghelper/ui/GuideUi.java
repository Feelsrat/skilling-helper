package com.skillinghelper.ui;

import com.skillinghelper.data.Wilderness;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import javax.swing.ImageIcon;
import net.runelite.client.util.ImageUtil;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

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
	/**
	 * Fill of the one main button on a panel ("Start guide").
	 */
	public static final Color PRIMARY = Skin.ACCENT;
	public static final Color PRIMARY_HOVER = Skin.mix(Skin.ACCENT, Color.WHITE, 0.12);
	/**
	 * Background of a list row under the mouse.
	 */
	public static final Color ROW_HOVER = Skin.CARD_HOVER;

	private static final GuideText.Palette PALETTE = new GuideText.Palette(hex(BRIGHT), hex(GOLD), hex(OPTION));
	private static final Font MARKER_FONT = new Font(Font.DIALOG, Font.PLAIN, 11);
	private static final int GUTTER = 11;
	private static final String SECTION = "skillinghelper.section";
	/**
	 * Title of the fold that holds everything under the running guide's step (see {@link #more}).
	 */
	public static final String MORE = "More about this guide";
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
		Skin.Surface card = new Skin.Surface(null, Skin.RADIUS).accent(accent).shade(Skin.mix(Skin.CARD, accent, 0.06));
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBorder(new EmptyBorder(9, 11, 10, 9));
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
			card.add(left(chip(kicker, kickerColor)));
			card.add(Box.createVerticalStrut(5));
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
		JLabel l = new Skin.Heading(title);
		l.putClientProperty(SECTION, title);
		l.setFont(FontManager.getRunescapeBoldFont());
		return full(l);
	}

	/**
	 * Lets the player fold away reference sections (rewards, drop tables) of a panel that was just filled, so the
	 * guide part stays near the top. A section is every {@link #section} heading in {@code body} and what follows
	 * it up to the next one. Call after filling {@code body}.
	 *
	 * @param foldable which section titles can fold
	 * @param open     titles the player opened; kept up to date, so pass the same set on every rebuild
	 */
	public static void foldSections(JPanel body, java.util.function.Predicate<String> foldable,
		java.util.Set<String> open)
	{
		Component[] kids = body.getComponents();
		for (int i = 0; i < kids.length; i++)
		{
			if (!(kids[i] instanceof JLabel))
			{
				continue;
			}
			JLabel heading = (JLabel) kids[i];
			Object title = heading.getClientProperty(SECTION);
			if (!(title instanceof String) || !foldable.test((String) title))
			{
				continue;
			}
			boolean toEnd = MORE.equals(title);
			List<Component> inside = new java.util.ArrayList<>();
			for (int j = i + 1; j < kids.length; j++)
			{
				if (!toEnd && kids[j] instanceof JLabel && ((JLabel) kids[j]).getClientProperty(SECTION) != null)
				{
					break;
				}
				inside.add(kids[j]);
			}
			String t = (String) title;
			Runnable show = () ->
			{
				boolean isOpen = open.contains(t);
				heading.setText("<html><font face='Dialog' size='-1'>" + (isOpen ? "▾" : "▸") + "</font>&nbsp;"
					+ GuideText.escape(t) + "</html>");
				heading.setToolTipText(isOpen ? "Click to fold away" : "Click to show");
				inside.forEach(c -> c.setVisible(isOpen));
				body.revalidate();
				body.repaint();
			};
			heading.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			heading.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (!open.remove(t))
					{
						open.add(t);
					}
					show.run();
				}
			});
			show.run();
			if (toEnd)
			{
				// Everything after it is inside this one fold
				return;
			}
		}
	}

	/**
	 * The fold under a running guide's step that holds the rest of the panel (lists, rewards, stats), closed until
	 * the player opens it. Add it right after the step, then call {@link #foldSections} with a test that accepts
	 * {@link #MORE}.
	 */
	public static JComponent more()
	{
		return section(MORE);
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

	// ---- Panel frame ----

	/**
	 * Lays out a side panel the same way in every plugin: a fixed header on top and the body under it in a
	 * scroll pane, with the usual padding and background.
	 */
	public static JScrollPane frame(PluginPanel panel, JComponent header, JPanel body)
	{
		panel.setLayout(new BorderLayout());
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));
		panel.setBackground(Skin.BG);
		panel.add(header, BorderLayout.NORTH);
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(Skin.BG);
		JPanel wrapper = new WidthTracking();
		wrapper.setBackground(Skin.BG);
		wrapper.add(body, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(wrapper);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(Skin.BG);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		panel.add(scroll, BorderLayout.CENTER);
		return scroll;
	}

	/**
	 * A scroll pane view that is always as wide as the panel, so a long line (a dropdown with a long entry, a
	 * wide row) wraps or gets cut short instead of pushing everything past the right edge.
	 */
	private static final class WidthTracking extends JPanel implements Scrollable
	{
		WidthTracking()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction)
		{
			return Math.max(16, visible.height - 32);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}

	/**
	 * The top of a side panel: a banner card with the plugin's icon and name. Add the panel's own controls
	 * (pickers, filters) to the returned column.
	 *
	 * @param icon the plugin's sidebar icon as a resource path (e.g. {@code /com/skillinghelper/clue/icon.png}),
	 *             or null
	 */
	public static JPanel header(String title, String icon)
	{
		JPanel header = column();
		header.setBorder(new EmptyBorder(0, 0, 4, 0));
		Skin.Surface top = new Skin.Surface(new BorderLayout(8, 0), 12).shade(Skin.mix(Skin.CARD, Skin.ACCENT, 0.16));
		top.setBorder(new EmptyBorder(7, 9, 7, 7));
		JLabel t = new JLabel(title);
		t.setFont(FontManager.getRunescapeBoldFont());
		t.setForeground(BRIGHT);
		t.setIconTextGap(8);
		if (icon != null)
		{
			try
			{
				t.setIcon(new ImageIcon(ImageUtil.loadImageResource(GuideUi.class, icon)));
			}
			catch (RuntimeException e)
			{
				// No icon is fine; the name still shows
			}
		}
		top.add(t, BorderLayout.CENTER);
		header.add(full(top));
		header.add(Box.createVerticalStrut(8));
		return header;
	}

	/**
	 * Puts a panel's rarely changed settings (tick boxes, sort order) behind an "Options" button on the
	 * {@link #header}'s title line, so the panel opens on just the guide. Call after adding the header's own
	 * controls.
	 */
	public static void options(JPanel header, JComponent... controls)
	{
		JPanel box = new Skin.Surface(null, Skin.RADIUS).edge(null);
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setBackground(Skin.WELL);
		box.setBorder(new EmptyBorder(6, 8, 6, 8));
		box.setAlignmentX(Component.LEFT_ALIGNMENT);
		for (JComponent c : controls)
		{
			box.add(left(c));
		}
		box.setVisible(false);
		JButton toggle = small("⚙", () ->
		{
		});
		toggle.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
		toggle.setBackground(new Color(0, 0, 0, 60));
		toggle.addActionListener(e ->
		{
			box.setVisible(!box.isVisible());
			toggle.setForeground(box.isVisible() ? Skin.ACCENT : TEXT);
			header.revalidate();
			header.repaint();
		});
		toggle.setToolTipText("Settings for this panel");
		JPanel top = (JPanel) header.getComponent(0);
		top.add(toggle, BorderLayout.EAST);
		header.add(full(box));
		header.add(Box.createVerticalStrut(6));
	}

	/**
	 * Styles a shared on/off setting ticked in the header, e.g. the Wilderness toggle: small text, quiet while off
	 * and in {@code onColor} while on, so a switched-on risk stands out instead of a switched-off one.
	 */
	public static JCheckBox toggle(JCheckBox box, Color onColor, String tooltip, Runnable onChange)
	{
		box.setOpaque(false);
		box.setFont(FontManager.getRunescapeSmallFont());
		box.setFocusable(false);
		box.setToolTipText(tooltip);
		Runnable recolor = () -> box.setForeground(box.isSelected() ? onColor : MUTED);
		recolor.run();
		box.addItemListener(e -> recolor.run());
		if (onChange != null)
		{
			box.addActionListener(e -> onChange.run());
		}
		return left(box);
	}

	/**
	 * The shared "Include Wilderness methods (all guides)" tick box, styled like every other panel's.
	 *
	 * @param offTooltip what being off leaves out in this panel, e.g. "Off: Wilderness bosses are skipped."
	 */
	public static JCheckBox wilderness(JCheckBox box, String offTooltip, Runnable onChange)
	{
		return toggle(box, Wilderness.COLOR, offTooltip + " Shared with the other guides in this plugin pack.",
			onChange);
	}

	/**
	 * What a panel shows when there is nothing to show yet (logged out, bank not seen): a quiet box with a
	 * short title and what to do about it.
	 */
	public static JComponent empty(String title, String hint)
	{
		JPanel box = new Skin.Surface(null, Skin.RADIUS);
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setAlignmentX(Component.LEFT_ALIGNMENT);
		box.setBorder(new EmptyBorder(14, 12, 14, 10));
		JLabel t = new JLabel(title);
		t.setFont(FontManager.getRunescapeBoldFont());
		t.setForeground(TEXT);
		box.add(left(t));
		if (hint != null)
		{
			box.add(Box.createVerticalStrut(3));
			box.add(text(hint, MUTED, CARD_WIDTH));
		}
		return full(box);
	}

	/**
	 * "Unlocked ........ 9 / 32" in bold with a thin bar under it.
	 */
	public static JComponent progress(String title, int done, int total)
	{
		boolean complete = total > 0 && done >= total;
		JPanel top = new JPanel(new BorderLayout(6, 0));
		top.setOpaque(false);
		JLabel t = new JLabel(title);
		t.setFont(FontManager.getRunescapeSmallFont());
		t.setForeground(MUTED);
		JLabel pct = new JLabel(total > 0 ? Math.round(100.0 * Math.min(done, total) / total) + "%" : "");
		pct.setFont(FontManager.getRunescapeSmallFont());
		pct.setForeground(complete ? GOOD : Skin.ACCENT);
		top.add(t, BorderLayout.WEST);
		top.add(pct, BorderLayout.EAST);
		JLabel n = new JLabel("<html><font size='+0' color='" + hex(BRIGHT) + "'>" + String.format("%,d", done)
			+ "</font><font color='" + hex(MUTED) + "'> / " + String.format("%,d", total) + "</font></html>");
		n.setFont(FontManager.getRunescapeBoldFont());
		JPanel box = new Skin.Surface(null, Skin.RADIUS);
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setAlignmentX(Component.LEFT_ALIGNMENT);
		box.setBorder(new EmptyBorder(8, 10, 10, 10));
		box.add(full(top));
		box.add(Box.createVerticalStrut(1));
		box.add(left(n));
		box.add(Box.createVerticalStrut(6));
		box.add(bar(done, total, complete ? GOOD : Skin.ACCENT));
		return full(box);
	}

	/**
	 * A thin progress bar the width of the panel.
	 */
	public static JComponent bar(int done, int total, Color fill)
	{
		JPanel bar = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				java.awt.Graphics2D g2 = Skin.smooth(g);
				int h = getHeight();
				g2.setColor(Skin.WELL);
				g2.fillRoundRect(0, 0, getWidth(), h, h, h);
				if (total > 0 && done > 0)
				{
					int w = Math.max(h, (int) Math.round(getWidth() * Math.min(1.0, done / (double) total)));
					g2.setPaint(new java.awt.GradientPaint(0, 0, Skin.mix(fill, Color.WHITE, 0.25), 0, h,
						Skin.mix(fill, Color.BLACK, 0.15)));
					g2.fillRoundRect(0, 0, w, h, h, h);
				}
				g2.dispose();
			}
		};
		bar.setOpaque(false);
		bar.setPreferredSize(new Dimension(WIDTH, 6));
		bar.setMinimumSize(new Dimension(0, 6));
		bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
		bar.setAlignmentX(Component.LEFT_ALIGNMENT);
		return bar;
	}

	// ---- Buttons ----

	/**
	 * The one main action of a panel, e.g. "Start guide": filled in orange so it is the first thing seen.
	 */
	public static JButton primary(String label, Runnable action)
	{
		JButton b = new Skin.Pill(label).primary();
		b.setFont(FontManager.getRunescapeBoldFont());
		b.addActionListener(e -> action.run());
		return full(b);
	}

	/**
	 * A button in a row of filters (tiers, tabs). See {@link #select}.
	 */
	public static JButton tab(String label, Runnable action)
	{
		JButton b = small(label, action);
		b.setBorder(new EmptyBorder(4, 2, 4, 2));
		return b;
	}

	/**
	 * Marks a {@link #tab} as the chosen one (filled) or not, with {@code color} for its text when not chosen.
	 */
	public static void select(JButton tab, boolean selected, Color color)
	{
		tab.setBackground(selected ? PRIMARY : null);
		tab.setForeground(selected ? BRIGHT : color);
	}

	/**
	 * A small button for a row of actions.
	 */
	public static JButton small(String label, Runnable action)
	{
		JButton b = new Skin.Pill(label);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.addActionListener(e -> action.run());
		return b;
	}

	/**
	 * Small buttons side by side, up to three a row, instead of a tall stack of full-width ones. Null entries
	 * are left out.
	 */
	public static JComponent actions(JButton... buttons)
	{
		List<JButton> list = new java.util.ArrayList<>();
		for (JButton b : buttons)
		{
			if (b != null)
			{
				list.add(b);
			}
		}
		int cols = list.size() == 4 ? 2 : Math.min(3, Math.max(1, list.size()));
		JPanel row = new JPanel(new GridLayout(0, cols, 4, 4));
		row.setOpaque(false);
		for (JButton b : list)
		{
			row.add(b);
		}
		return full(row);
	}

	/**
	 * The running guide's current step: a {@link #card} with "NOW" and a small Stop button on top, the step,
	 * then any extra actions in a row under it.
	 *
	 * @param onStop  null for no Stop button
	 * @param actions buttons for the row under the step (see {@link #small}); nulls are skipped
	 */
	public static JPanel stepCard(Color accent, String kicker, String title, List<String> lines, Runnable onStop,
		JButton... actions)
	{
		JPanel card = card(accent);
		fillStep(card, kicker, accent, title, lines);
		if (onStop != null && kicker != null)
		{
			addStop(card, kicker, accent, onStop);
		}
		boolean any = false;
		for (JButton b : actions)
		{
			any |= b != null;
		}
		if (any)
		{
			card.add(Box.createVerticalStrut(6));
			card.add(actions(actions));
		}
		return full(card);
	}

	/**
	 * Swaps the kicker ("NOW") of a card filled by {@link #fillStep} for one with a small Stop button on the right.
	 */
	public static void addStop(JPanel card, String kicker, Color color, Runnable onStop)
	{
		card.remove(0);
		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		top.add(chip(kicker, color), BorderLayout.WEST);
		JButton stop = small("■ Stop", onStop);
		stop.setFont(MARKER_FONT);
		stop.setForeground(BAD);
		stop.setBorder(new EmptyBorder(2, 7, 2, 7));
		stop.setToolTipText("Stop the guide");
		top.add(stop, BorderLayout.EAST);
		card.add(full(top), 0);
	}

	// ---- Lists ----

	/**
	 * A list entry in its own dark box, see {@link #clickable} to make it open something.
	 */
	public static JPanel row(boolean current)
	{
		Skin.Surface row = new Skin.Surface(null, Skin.RADIUS).accent(current ? Skin.ACCENT : null);
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (current)
		{
			row.setBackground(Skin.mix(Skin.CARD, Skin.ACCENT, 0.08));
		}
		row.setBorder(new EmptyBorder(6, current ? 10 : 8, 6, 7));
		return row;
	}

	/**
	 * Makes a list row open something on click, with a hand cursor and a lighter background under the mouse so
	 * it reads as clickable.
	 */
	public static <T extends JComponent> T clickable(T row, String tooltip, Runnable action)
	{
		Color normal = row.getBackground();
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText(tooltip);
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				// Right-click is left for a row's own menu
				if (!javax.swing.SwingUtilities.isRightMouseButton(e))
				{
					action.run();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ROW_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(normal);
			}
		});
		return row;
	}

	/**
	 * A small grey hint line under a section heading, e.g. "Click one to be guided to it next."
	 */
	public static JComponent hint(String s)
	{
		JLabel l = text(s, MUTED, WIDTH);
		l.setBorder(new EmptyBorder(0, 0, 4, 0));
		return full(l);
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
		JButton b = new Skin.Pill(label);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setBorder(new EmptyBorder(6, 6, 6, 6));
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

	/**
	 * A small rounded tag, e.g. the "NOW" over the current step.
	 */
	public static JLabel chip(String text, Color color)
	{
		JLabel l = new JLabel(text.toUpperCase(Locale.ROOT))
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				java.awt.Graphics2D g2 = Skin.smooth(g);
				g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
				g2.dispose();
				super.paintComponent(g);
			}
		};
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(color);
		l.setBorder(new EmptyBorder(1, 7, 1, 7));
		l.setMaximumSize(l.getPreferredSize());
		return l;
	}

	public static JPanel column()
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
