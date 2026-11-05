package com.skillinghelper.money.ui;

import com.skillinghelper.data.Wilderness;
import com.skillinghelper.engine.Format;
import com.skillinghelper.money.MoneyGuide;
import com.skillinghelper.money.MoneyOption;
import com.skillinghelper.money.MoneyStep;
import com.skillinghelper.money.data.MoneyItem;
import com.skillinghelper.money.data.MoneyMethod;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import com.skillinghelper.ui.GuideUi;

/**
 * Side panel: the wiki's money making methods the player can do, best gp/hr first, with filters. Clicking one shows
 * its requirements, supplies and the wiki's instructions, and starts its guide.
 */
public class MoneyMakingPanel extends PluginPanel
{
	private static final int TEXT_WIDTH = GuideUi.WIDTH;
	private static final int PAGE_SIZE = 40;
	private static final Color GOOD = GuideUi.GOOD;
	private static final Color WARN = GuideUi.WARN;
	private static final Color BAD = GuideUi.BAD;
	private static final Color HINT = GuideUi.HINT;
	private static final Color MUTED = GuideUi.MUTED;
	private static final String ALL = "All categories";
	private static final String ANY_EFFORT = "Any effort";

	public interface Listener
	{
		void onStartGuide(String page);

		void onStopGuide();

		void onHaveSupplies();

		void onOpenWiki(String path);

		/**
		 * Let every guide pick Wilderness methods, or leave them out again.
		 */
		void onAllowWilderness(boolean allow);
	}

	private final Listener listener;
	private final JComboBox<String> category = new JComboBox<>();
	private final JComboBox<String> effort = new JComboBox<>(new String[]{ANY_EFFORT, "Low", "Moderate", "High"});
	private final JCheckBox showLocked = new JCheckBox("Show ones I can't do or afford yet");
	private final JCheckBox showLosing = new JCheckBox("Show ones losing money or not selling");
	private final JCheckBox showWilderness = new JCheckBox(Wilderness.TOGGLE_LABEL);
	private final JTextField search = new JTextField();
	private final JPanel guideBox = new JPanel();
	private final JPanel list = new JPanel();

	private List<MoneyOption> options = Collections.emptyList();
	private Map<String, String> fullGuides = Collections.emptyMap();
	private String selected;
	private String running;
	private MoneyStep step;
	private int shown = PAGE_SIZE;
	private boolean updating;

	public MoneyMakingPanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(new EmptyBorder(0, 0, 6, 0));
		JLabel title = new JLabel("Money Making");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(left(title));
		header.add(Box.createVerticalStrut(4));
		header.add(text("OSRS Wiki money making guides, best gp/hr at today's prices first.", MUTED));
		header.add(Box.createVerticalStrut(6));

		category.addItem(ALL);
		category.addActionListener(e -> filtersChanged());
		effort.addActionListener(e -> filtersChanged());
		showLocked.setBackground(ColorScheme.DARK_GRAY_COLOR);
		showLocked.setForeground(Color.LIGHT_GRAY);
		showLocked.setFont(FontManager.getRunescapeSmallFont());
		showLocked.addActionListener(e -> filtersChanged());
		showLosing.setBackground(ColorScheme.DARK_GRAY_COLOR);
		showLosing.setForeground(Color.LIGHT_GRAY);
		showLosing.setFont(FontManager.getRunescapeSmallFont());
		showLosing.addActionListener(e -> filtersChanged());
		showWilderness.setBackground(ColorScheme.DARK_GRAY_COLOR);
		showWilderness.setForeground(Wilderness.COLOR);
		showWilderness.setFont(FontManager.getRunescapeSmallFont());
		showWilderness.setToolTipText("Off: Wilderness methods are hidden. Shared with the other guides in this plugin pack.");
		showWilderness.addActionListener(e ->
		{
			if (!updating)
			{
				listener.onAllowWilderness(showWilderness.isSelected());
				filtersChanged();
			}
		});
		search.setToolTipText("Search by name, item or skill");
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				filtersChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				filtersChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				filtersChanged();
			}
		});
		JPanel combos = new JPanel(new GridLayout(1, 2, 4, 0));
		combos.setBackground(ColorScheme.DARK_GRAY_COLOR);
		combos.add(category);
		combos.add(effort);
		header.add(full(combos));
		header.add(Box.createVerticalStrut(4));
		header.add(full(search));
		header.add(left(showLocked));
		header.add(left(showLosing));
		header.add(left(showWilderness));
		add(header, BorderLayout.NORTH);

		guideBox.setLayout(new BoxLayout(guideBox, BoxLayout.Y_AXIS));
		guideBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		guideBox.setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, HINT), new EmptyBorder(6, 6, 6, 6)));
		guideBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		guideBox.setVisible(false);

		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.add(guideBox);
		body.add(Box.createVerticalStrut(6));
		body.add(list);
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(body, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(wrapper);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);

		list.add(text("Log in to see the money makers you can do.", MUTED));
	}

	/**
	 * Shows the latest ranking. Runs on the Swing thread.
	 *
	 * @param fullGuides plugin name with a click-by-click guide, by wiki page
	 */
	public void update(List<MoneyOption> options, Map<String, String> fullGuides)
	{
		this.options = options;
		this.fullGuides = fullGuides;
		updating = true;
		Object picked = category.getSelectedItem();
		TreeSet<String> categories = new TreeSet<>();
		for (MoneyOption o : options)
		{
			categories.add(o.getMethod().mainCategory());
		}
		category.removeAllItems();
		category.addItem(ALL);
		categories.forEach(category::addItem);
		category.setSelectedItem(picked != null && categories.contains(picked) ? picked : ALL);
		updating = false;
		rebuild();
	}

	/**
	 * Follows the shared Wilderness setting. Runs on the Swing thread.
	 */
	public void setWildernessAllowed(boolean allowed)
	{
		if (showWilderness.isSelected() != allowed)
		{
			updating = true;
			showWilderness.setSelected(allowed);
			updating = false;
			filtersChanged();
		}
	}

	/**
	 * Shows the running guide's step, or hides the guide box when {@code step} is null. Runs on the Swing thread.
	 */
	public void updateStep(String page, MoneyStep step)
	{
		String nowRunning = step == null ? null : page;
		boolean changed = !Objects.equals(nowRunning, running);
		running = nowRunning;
		this.step = step;
		fillGuide();
		if (changed)
		{
			if (nowRunning != null)
			{
				selected = nowRunning;
			}
			rebuild();
		}
	}

	private void fillGuide()
	{
		guideBox.removeAll();
		guideBox.setVisible(step != null);
		if (step != null)
		{
			GuideUi.fillStep(guideBox, "Now", HINT, step.getTitle(), step.getLines());
			if (isWilderness(running))
			{
				guideBox.add(Box.createVerticalStrut(4));
				guideBox.add(GuideUi.line(Wilderness.TAG + ": " + Wilderness.WARNING, Wilderness.COLOR,
					GuideUi.CARD_WIDTH));
			}
			guideBox.add(Box.createVerticalStrut(6));
			if (step.isSupplyStep())
			{
				guideBox.add(button("I have everything", listener::onHaveSupplies));
			}
			guideBox.add(button("Stop guide", listener::onStopGuide));
		}
		guideBox.revalidate();
		guideBox.repaint();
	}

	private boolean isWilderness(String page)
	{
		return page != null && options.stream().anyMatch(o -> o.getMethod().getPage().equals(page)
			&& o.getMethod().isWilderness());
	}

	private void filtersChanged()
	{
		if (!updating)
		{
			shown = PAGE_SIZE;
			rebuild();
		}
	}

	private void rebuild()
	{
		list.removeAll();
		List<MoneyOption> visible = new ArrayList<>();
		for (MoneyOption o : options)
		{
			if (matches(o))
			{
				visible.add(o);
			}
		}
		if (options.isEmpty())
		{
			list.add(text("Log in to see the money makers you can do.", MUTED));
		}
		else if (visible.isEmpty())
		{
			list.add(text("Nothing matches. Try another category, or show the ones you can't do yet.", MUTED));
		}
		else
		{
			long good = visible.stream().filter(MoneyOption::isGood).count();
			list.add(text(good + " you can do that make money right now" + (visible.size() > good
				? ", " + (visible.size() - good) + " others" : "") + ":", MUTED));
			list.add(Box.createVerticalStrut(4));
		}
		if (!showWilderness.isSelected())
		{
			long hidden = options.stream().filter(o -> o.getMethod().isWilderness() && o.isGood()).count();
			if (hidden > 0)
			{
				list.add(text("☠ " + hidden + " more you can do " + (hidden == 1 ? "is" : "are")
					+ " in the Wilderness, hidden. Tick \"Include Wilderness methods\" above to see them.",
					Wilderness.COLOR));
				list.add(Box.createVerticalStrut(4));
			}
		}
		for (int i = 0; i < Math.min(shown, visible.size()); i++)
		{
			list.add(row(visible.get(i)));
			list.add(Box.createVerticalStrut(3));
		}
		if (visible.size() > shown)
		{
			list.add(button("Show more (" + (visible.size() - shown) + " left)", () ->
			{
				shown += PAGE_SIZE;
				rebuild();
			}));
		}
		list.revalidate();
		list.repaint();
	}

	private boolean matches(MoneyOption o)
	{
		MoneyMethod m = o.getMethod();
		if (!m.getPage().equals(running))
		{
			if (m.isWilderness() && !showWilderness.isSelected())
			{
				return false;
			}
			if (!o.isAvailable() && !showLocked.isSelected())
			{
				return false;
			}
			if ((!o.isProfitable() || o.isHardToSell()) && !showLosing.isSelected())
			{
				return false;
			}
		}
		Object cat = category.getSelectedItem();
		if (cat != null && !ALL.equals(cat) && !cat.equals(m.mainCategory()))
		{
			return false;
		}
		Object eff = effort.getSelectedItem();
		if (eff != null && !ANY_EFFORT.equals(eff) && !eff.equals(m.getIntensity()))
		{
			return false;
		}
		String q = search.getText().trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty())
		{
			return true;
		}
		if (m.getName().toLowerCase(Locale.ROOT).contains(q)
			|| (m.getSkillCategory() != null && m.getSkillCategory().toLowerCase(Locale.ROOT).contains(q)))
		{
			return true;
		}
		for (MoneyItem item : m.getOutputs())
		{
			if (item.getName().toLowerCase(Locale.ROOT).contains(q))
			{
				return true;
			}
		}
		return false;
	}

	private JComponent row(MoneyOption o)
	{
		MoneyMethod m = o.getMethod();
		boolean open = m.getPage().equals(selected);
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setBackground(open ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		Color edge = m.getPage().equals(running) ? HINT : o.isGood() ? GOOD : o.isAvailable() ? WARN : BAD;
		row.setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, edge), new EmptyBorder(4, 6, 4, 6)));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel name = GuideUi.text(m.getName(), Color.WHITE, TEXT_WIDTH - 15);
		name.setFont(FontManager.getRunescapeBoldFont());
		row.add(name);
		row.add(GuideUi.text(profitText(o), o.getProfit() > 0 ? GOOD : BAD, TEXT_WIDTH - 15));
		row.add(GuideUi.text(summary(m), MUTED, TEXT_WIDTH - 15));
		if (m.isWilderness())
		{
			row.add(text(Wilderness.TAG, Wilderness.COLOR, TEXT_WIDTH - 15));
		}
		if (o.isHardToSell())
		{
			row.add(text("⚠ Hard to sell", BAD, TEXT_WIDTH - 15));
		}
		else if (!o.getMarketWarnings().isEmpty())
		{
			row.add(text("⚠ " + (o.getMarketWarnings().get(0).startsWith("GE buy limit") ? "GE buy limit"
				: "Sells slowly"), WARN, TEXT_WIDTH - 15));
		}
		if (fullGuides.containsKey(m.getPage()))
		{
			row.add(text("Full guide in " + fullGuides.get(m.getPage()), HINT, TEXT_WIDTH - 15));
		}
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				selected = Objects.equals(selected, m.getPage()) ? null : m.getPage();
				rebuild();
			}
		});
		if (open)
		{
			detail(row, o);
		}
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	private void detail(JPanel box, MoneyOption o)
	{
		MoneyMethod m = o.getMethod();
		box.add(Box.createVerticalStrut(4));
		boolean isRunning = m.getPage().equals(running);
		String full = fullGuides.get(m.getPage());
		if (isRunning)
		{
			box.add(button("Stop guide", listener::onStopGuide));
		}
		else
		{
			box.add(button(full != null ? "Start (opens " + full + ")" : "Start guide",
				() -> listener.onStartGuide(m.getPage())));
		}
		box.add(button("Open wiki page", () -> listener.onOpenWiki(m.wikiPath())));
		box.add(Box.createVerticalStrut(4));

		if (m.isRecurring())
		{
			box.add(text("One run takes about " + Format.hours(m.getRunHours()) + ", every "
				+ Format.hours(m.getEveryHours()) + ".", MUTED));
		}
		box.add(section(m.isRecurring() ? "Money per run" : "Money per hour"));
		box.add(GuideUi.text("At today's GE prices", MUTED, TEXT_WIDTH - 15));
		box.add(GuideUi.stat("Sells for", Format.gp(o.getOutputValue()), GuideUi.TEXT));
		if (o.getGeTax() > 0)
		{
			box.add(GuideUi.stat("GE tax (2%)", "-" + Format.gp(o.getGeTax()), GuideUi.TEXT));
		}
		if (o.getInputCost() > 0)
		{
			box.add(GuideUi.stat("Supplies", "-" + Format.gp(o.getInputCost()), GuideUi.TEXT));
		}
		box.add(GuideUi.stat("Profit", Format.gp(o.getProfit()), o.isProfitable() ? GOOD : BAD));
		if (!o.isProfitable())
		{
			box.add(text("Loses money right now: the supplies cost more than the products sell for.", BAD,
				TEXT_WIDTH - 15));
		}
		for (String w : o.getMarketWarnings())
		{
			box.add(text("⚠ " + w, o.isHardToSell() && !w.startsWith("GE buy limit") ? BAD : WARN,
				TEXT_WIDTH - 15));
		}
		if (!o.isVolumesKnown() && !m.getOutputs().isEmpty())
		{
			box.add(text("Turn on \"Live OSRS Wiki prices\" in the plugin settings to check how fast it sells.",
				MUTED, TEXT_WIDTH - 15));
		}
		if (o.getTripCost() > 0)
		{
			box.add(text("Gold needed for one trip, after what you own: " + Format.gp(o.getTripCost()), MUTED,
				TEXT_WIDTH - 15));
		}
		else if (o.getTripCost() < 0)
		{
			box.add(text("Open your bank once so the plugin can check you can afford it.", MUTED, TEXT_WIDTH - 15));
		}

		box.add(section("Requirements"));
		if (m.isMembers())
		{
			box.add(check("Members", !o.getBlockers().contains("Members only")));
		}
		for (String b : o.getBlockers())
		{
			if (b.contains("one trip's supplies"))
			{
				box.add(check(b.substring("Needs ".length()), false));
			}
		}
		for (Map.Entry<Skill, Integer> e : m.getSkills().entrySet())
		{
			box.add(check(e.getValue() + " " + e.getKey().getName(), o.getBlockers().stream()
				.noneMatch(b -> b.startsWith("Needs " + e.getValue() + " " + e.getKey().getName()))));
		}
		for (String quest : m.getQuests())
		{
			if (o.getUnchecked().contains(quest))
			{
				box.add(text("? " + quest, WARN, TEXT_WIDTH - 15));
			}
			else
			{
				box.add(check(quest, !o.getBlockers().contains("Needs " + quest)));
			}
		}
		if (m.getOther() != null)
		{
			box.add(text("? " + m.getOther(), WARN, TEXT_WIDTH - 15));
		}
		if (m.getSkills().isEmpty() && m.getQuests().isEmpty() && m.getOther() == null && !m.isMembers())
		{
			box.add(text("None", MUTED));
		}

		if (m.getLocationName() != null)
		{
			box.add(section("Where"));
			box.add(text(m.getLocationName(), Color.LIGHT_GRAY, TEXT_WIDTH - 15));
		}
		items(box, "Bring" + (m.isRecurring() ? " (per run)" : " (per hour)"), m.getInputs());
		items(box, "Makes" + (m.isRecurring() ? " (per run)" : " (per hour)"), m.getOutputs());

		List<String> sentences = MoneyGuide.sentences(m.getDetails());
		if (!sentences.isEmpty())
		{
			box.add(section("How (from the wiki)"));
			for (String s : sentences)
			{
				box.add(text("• " + s, Color.LIGHT_GRAY, TEXT_WIDTH - 15));
				box.add(Box.createVerticalStrut(3));
			}
		}
	}

	private void items(JPanel box, String heading, List<MoneyItem> items)
	{
		if (items.isEmpty())
		{
			return;
		}
		box.add(section(heading));
		List<MoneyItem> sorted = new ArrayList<>(items);
		sorted.sort(Comparator.comparingDouble((MoneyItem i) -> i.getQuantity() * i.getWikiValue()).reversed());
		int n = Math.min(6, sorted.size());
		for (int i = 0; i < n; i++)
		{
			MoneyItem item = sorted.get(i);
			box.add(text("• " + quantity(item.getQuantity()) + " x " + item.getName(), Color.LIGHT_GRAY,
				TEXT_WIDTH - 15));
		}
		if (sorted.size() > n)
		{
			box.add(text("...and " + (sorted.size() - n) + " more on the wiki page", MUTED));
		}
	}

	private static String quantity(double q)
	{
		if (q >= 10 || q == Math.rint(q))
		{
			return String.format("%,d", Math.round(q));
		}
		return String.format("%.2f", q);
	}

	private static String profitText(MoneyOption o)
	{
		MoneyMethod m = o.getMethod();
		return m.isRecurring() ? Format.gp(o.getProfit()) + " / run" : Format.gp(o.getProfit()) + "/hr";
	}

	private static String summary(MoneyMethod m)
	{
		String s = m.levelRange() + " · " + (m.getSkillCategory() != null ? m.getSkillCategory() : m.mainCategory());
		if (m.getIntensity() != null)
		{
			s += " · " + m.getIntensity() + " effort";
		}
		return m.isMembers() ? s : s + " · F2P";
	}

	private static JComponent section(String s)
	{
		return GuideUi.section(s);
	}

	private static JComponent check(String s, boolean ok)
	{
		return text((ok ? "✔ " : "✘ ") + s, ok ? GOOD : BAD, TEXT_WIDTH - 15);
	}

	private static JComponent text(String s, Color color)
	{
		return text(s, color, TEXT_WIDTH);
	}

	/**
	 * A guide line in the shared look: split into short lines with the key bits highlighted. Light grey and green
	 * lines get inline highlights (green just colours the tick); other colours draw the whole line in that colour.
	 */
	private static JComponent text(String s, Color color, int width)
	{
		if (color == Color.LIGHT_GRAY || color == GuideUi.TEXT)
		{
			return GuideUi.line(s, width);
		}
		if (color == GOOD)
		{
			return GuideUi.line(s, GOOD, GuideUi.TEXT, width);
		}
		return GuideUi.line(s, color, width);
	}

	private static JButton button(String label, Runnable action)
	{
		JButton b = new JButton(label);
		b.addActionListener(e -> action.run());
		return full(b);
	}

	private static <T extends JComponent> T left(T c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		return c;
	}

	private static <T extends JComponent> T full(T c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
		return c;
	}
}
