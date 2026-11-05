package com.skillinghelper.collection.ui;

import com.skillinghelper.collection.ClogPlanner;
import com.skillinghelper.collection.ClogProgress;
import com.skillinghelper.collection.ClogView;
import com.skillinghelper.collection.data.ClogTab;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: how full the log is, then one of four lists (quickest slots, closest pages, pets, browse by tab);
 * picking a row opens that page with beginner steps, its slots and buttons to the full guide, the route or the wiki.
 */
public class CollectionLogPanel extends PluginPanel
{
	private static final int WIDTH = GuideUi.WIDTH;
	private static final int CARD = GuideUi.CARD_WIDTH;

	public interface Listener
	{
		void onMode(ClogView.Mode mode);

		void onTab(ClogTab tab);

		void onPickEntry(String name);

		void onBack();

		void onShowWay(boolean show);

		/**
		 * Opens the full guide for a page; for the pet pages, the guide of that pet.
		 */
		void onOpenGuide(String entry, int itemId);

		void onOpenCombatAchievements(String monster);

		void onAllowWilderness(boolean allow);

		void onShowLocked(boolean show);

		void onOpenWiki(String entry);
	}

	private final Listener listener;
	private final HiddenRedraws redraws;
	private final JComboBox<ClogView.Mode> modes = new JComboBox<>(ClogView.Mode.values());
	private final JComboBox<ClogTab> tabs = new JComboBox<>(ClogTab.values());
	private final JCheckBox wilderness = new JCheckBox(Wilderness.TOGGLE_LABEL);
	private final JCheckBox locked = new JCheckBox("Show locked pages and slots");
	private final JPanel body = new JPanel();
	private boolean updating;

	public CollectionLogPanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		redraws = new HiddenRedraws(this);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = column();
		header.setBorder(new EmptyBorder(0, 0, 6, 0));
		JLabel title = new JLabel("Collection Log");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(GuideUi.left(title));
		header.add(Box.createVerticalStrut(4));
		header.add(GuideUi.text("The slots and pets that come quickest, the pages you're closest to finishing, and "
			+ "how to get them.", GuideUi.MUTED, WIDTH));
		header.add(Box.createVerticalStrut(6));
		modes.addActionListener(e ->
		{
			if (!updating && modes.getSelectedItem() != null)
			{
				listener.onMode((ClogView.Mode) modes.getSelectedItem());
			}
		});
		header.add(GuideUi.full(modes));
		tabs.addActionListener(e ->
		{
			if (!updating && tabs.getSelectedItem() != null)
			{
				listener.onTab((ClogTab) tabs.getSelectedItem());
			}
		});
		header.add(Box.createVerticalStrut(3));
		header.add(GuideUi.full(tabs));
		for (JCheckBox box : new JCheckBox[]{wilderness, locked})
		{
			box.setBackground(ColorScheme.DARK_GRAY_COLOR);
			box.setFont(FontManager.getRunescapeSmallFont());
			header.add(GuideUi.left(box));
		}
		wilderness.setForeground(Wilderness.COLOR);
		wilderness.setToolTipText("Off: Wilderness bosses and pets are left out. Shared with the other guides in this "
			+ "plugin pack.");
		wilderness.addActionListener(e ->
		{
			if (!updating)
			{
				listener.onAllowWilderness(wilderness.isSelected());
			}
		});
		locked.setForeground(GuideUi.TEXT);
		locked.setToolTipText("Also list what you don't have the quests or levels for yet");
		locked.addActionListener(e ->
		{
			if (!updating)
			{
				listener.onShowLocked(locked.isSelected());
			}
		});
		add(header, BorderLayout.NORTH);

		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(body, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(wrapper);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);
		body.add(GuideUi.text("Log in to see your collection log.", GuideUi.MUTED, WIDTH));
	}

	/**
	 * Shows the latest view. Swing thread.
	 */
	public void update(ClogView view)
	{
		redraws.run("view", () -> apply(view));
	}

	private void apply(ClogView v)
	{
		updating = true;
		modes.setSelectedItem(v.getMode());
		tabs.setSelectedItem(v.getTab());
		tabs.setVisible(v.getMode() == ClogView.Mode.BROWSE && v.getDetail() == null);
		wilderness.setSelected(v.isWildernessAllowed());
		locked.setSelected(v.isShowLocked());
		updating = false;

		body.removeAll();
		if (!v.isLoggedIn())
		{
			body.add(GuideUi.text("Log in to see your collection log.", GuideUi.MUTED, WIDTH));
			finish();
			return;
		}
		summary(v);
		if (v.getDetail() != null)
		{
			detail(v.getDetail());
		}
		else
		{
			switch (v.getMode())
			{
				case QUICKEST:
					quickest(v);
					break;
				case CLOSEST:
					closest(v);
					break;
				case PETS:
					pets(v);
					break;
				default:
					browse(v);
			}
		}
		finish();
	}

	private void summary(ClogView v)
	{
		String count = v.getGameTotal() > 0
			? v.getGameCount() + " / " + v.getGameTotal() + " slots filled"
			: v.getHave() + " / " + v.getTotal() + " slots filled";
		body.add(GuideUi.headline(count, GuideUi.GOLD, WIDTH));
		if (v.getSeen() < v.getTotal() / 2 && v.getDetail() == null)
		{
			body.add(Box.createVerticalStrut(3));
			JPanel card = GuideUi.card(GuideUi.WARN);
			List<String> lines = new ArrayList<>();
			lines.add("1. Open the Character Summary tab (the green \"quest list\" tab) and click Collection Log. "
				+ "Or read the collection log book.");
			lines.add("2. Click through the pages you care about. Each page you open is read and remembered.");
			lines.add("3. In the game's Settings, search \"collection log\" and turn on the new-slot chat message, "
				+ "so new slots are ticked off as you get them.");
			lines.add("Until then, items in your bank count as filled slots.");
			GuideUi.fillStep(card, "Sync", GuideUi.WARN, "Let the plugin see your log", lines);
			body.add(GuideUi.full(card));
		}
		body.add(Box.createVerticalStrut(6));
	}

	private void quickest(ClogView v)
	{
		body.add(GuideUi.section("Quickest new slots"));
		body.add(GuideUi.text("Slots you don't have yet that most players have: usually the fastest to get. "
			+ "% = share of players with it. Click one to see how.", GuideUi.MUTED, WIDTH));
		body.add(Box.createVerticalStrut(4));
		if (v.getSlots().isEmpty())
		{
			body.add(GuideUi.text("Nothing left that you can do right now.", GuideUi.MUTED, WIDTH));
		}
		for (ClogView.SlotRow s : v.getSlots())
		{
			body.add(slotRow(s));
			body.add(Box.createVerticalStrut(3));
		}
	}

	private void pets(ClogView v)
	{
		body.add(GuideUi.section("Pets"));
		long have = v.getSlots().stream().filter(s -> has(s.getState())).count();
		body.add(GuideUi.line(have + " of " + v.getSlots().size() + " pets", GuideUi.GOLD, GuideUi.TEXT, WIDTH));
		body.add(GuideUi.text("The ones most players have come first. Click one to see where it's from.",
			GuideUi.MUTED, WIDTH));
		body.add(Box.createVerticalStrut(4));
		for (ClogView.SlotRow s : v.getSlots())
		{
			body.add(slotRow(s));
			body.add(Box.createVerticalStrut(3));
		}
	}

	private void closest(ClogView v)
	{
		body.add(GuideUi.section("Closest to complete"));
		body.add(GuideUi.text("Pages you've started, fewest slots left first. Click one for its steps.",
			GuideUi.MUTED, WIDTH));
		body.add(Box.createVerticalStrut(4));
		if (v.getEntries().isEmpty())
		{
			body.add(GuideUi.text("No pages started yet that you can do. Open your collection log so the plugin "
				+ "can see what you have.", GuideUi.MUTED, WIDTH));
		}
		for (ClogView.EntryRow e : v.getEntries())
		{
			body.add(entryRow(e));
			body.add(Box.createVerticalStrut(3));
		}
	}

	private void browse(ClogView v)
	{
		body.add(GuideUi.section(v.getTab().getLabel()));
		for (ClogView.EntryRow e : v.getEntries())
		{
			body.add(entryRow(e));
			body.add(Box.createVerticalStrut(3));
		}
	}

	private void detail(ClogView.EntryDetail d)
	{
		body.add(GuideUi.button("← Back to the list", listener::onBack));
		body.add(Box.createVerticalStrut(6));
		body.add(GuideUi.headline(d.getName(), Color.WHITE, WIDTH));
		Color c = d.getHave() == d.getTotal() ? GuideUi.GOOD : GuideUi.TEXT;
		body.add(GuideUi.line((d.getHave() == d.getTotal() ? "✔ " : "") + d.getHave() + " of " + d.getTotal()
			+ " slots", c, WIDTH));
		if (d.isWilderness())
		{
			body.add(GuideUi.line(Wilderness.TAG, Wilderness.COLOR, WIDTH));
		}
		if (!d.getIntro().isEmpty())
		{
			body.add(Box.createVerticalStrut(3));
			body.add(GuideUi.text(d.getIntro(), GuideUi.MUTED, WIDTH));
		}
		for (String m : d.getMissing())
		{
			body.add(GuideUi.check(false, m, WIDTH));
		}
		for (String t : d.getTrain())
		{
			body.add(GuideUi.line("• " + t, GuideUi.WARN, GuideUi.TEXT, WIDTH));
		}

		body.add(Box.createVerticalStrut(6));
		JPanel card = GuideUi.card(GuideUi.HINT);
		List<String> steps = new ArrayList<>();
		for (int i = 0; i < d.getSteps().size(); i++)
		{
			steps.add((i + 1) + ". " + d.getSteps().get(i));
		}
		GuideUi.fillStep(card, "How to", GuideUi.HINT, "Getting these slots", steps);
		body.add(GuideUi.full(card));
		body.add(Box.createVerticalStrut(6));
		if (d.getGuide() != null)
		{
			body.add(GuideUi.button("Open full guide", () -> listener.onOpenGuide(d.getName(), -1)));
			body.add(Box.createVerticalStrut(3));
		}
		else if (d.isHasPlace())
		{
			body.add(GuideUi.button(d.isShowingWay() ? "Stop showing the way" : "Show the way",
				() -> listener.onShowWay(!d.isShowingWay())));
			body.add(Box.createVerticalStrut(3));
		}
		if (d.getCombatAchievements() != null)
		{
			body.add(GuideUi.button("Combat achievements here", () -> listener.onOpenCombatAchievements(
				d.getCombatAchievements())));
			body.add(Box.createVerticalStrut(3));
		}
		body.add(GuideUi.button("Open wiki page", () -> listener.onOpenWiki(d.getName())));

		body.add(GuideUi.section("Slots"));
		boolean pets = d.getName().endsWith("Pets");
		for (ClogView.SlotRow s : d.getSlots())
		{
			if (pets)
			{
				body.add(slotRow(s));
				body.add(Box.createVerticalStrut(3));
			}
			else
			{
				body.add(GuideUi.line(marker(s.getState()) + " " + s.getName() + "  " + ClogPlanner.percent(s.getPercent())
					+ stateNote(s.getState()), color(s.getState()), WIDTH));
			}
		}
	}

	private JComponent slotRow(ClogView.SlotRow s)
	{
		JPanel row = rowPanel();
		row.add(GuideUi.line(marker(s.getState()) + " " + s.getName() + "  " + ClogPlanner.percent(s.getPercent()),
			color(s.getState()), CARD));
		if (s.getEntry().endsWith("Pets") && s.getGuide() != null && s.getGuide().startsWith("Skilling Helper ("))
		{
			String skill = s.getGuide().substring("Skilling Helper (".length(), s.getGuide().length() - 1);
			row.add(GuideUi.text("From training " + skill + stateNote(s.getState()), GuideUi.MUTED, CARD - 12));
		}
		else if (!s.getEntry().isEmpty() && !s.getEntry().endsWith("Pets"))
		{
			row.add(GuideUi.text("From " + s.getEntry() + stateNote(s.getState()), GuideUi.MUTED, CARD - 12));
		}
		for (String m : s.getMissing())
		{
			row.add(GuideUi.check(false, m, CARD));
		}
		if (s.isWilderness())
		{
			row.add(GuideUi.line(Wilderness.TAG, Wilderness.COLOR, CARD));
		}
		if (s.getGuide() != null && !has(s.getState()))
		{
			row.add(GuideUi.text("Full guide: " + s.getGuide(), GuideUi.HINT, CARD - 12));
		}
		if (s.getEntry().endsWith("Pets") || s.getEntry().isEmpty())
		{
			// A skilling pet (or one with no page of its own): its guide is the skill's
			if (s.getGuide() != null)
			{
				clickable(row, "Open the guide for it", () -> listener.onOpenGuide(s.getEntry(), s.getId()));
			}
		}
		else
		{
			clickable(row, "See how to get it", () -> listener.onPickEntry(s.getEntry()));
		}
		return GuideUi.full(row);
	}

	private JComponent entryRow(ClogView.EntryRow e)
	{
		JPanel row = rowPanel();
		boolean done = e.getHave() == e.getTotal();
		row.add(GuideUi.line((done ? "✔ " : "• ") + e.getName() + "  " + e.getHave() + "/" + e.getTotal(),
			done ? GuideUi.GOOD : GuideUi.TEXT, CARD));
		if (!done && !e.getNext().isEmpty())
		{
			row.add(GuideUi.text("Next: " + String.join(", ", e.getNext()), GuideUi.MUTED, CARD - 12));
		}
		for (String m : e.getMissing())
		{
			row.add(GuideUi.check(false, m, CARD));
		}
		if (e.isWilderness())
		{
			row.add(GuideUi.line(Wilderness.TAG, Wilderness.COLOR, CARD));
		}
		if (e.getGuide() != null && !done)
		{
			row.add(GuideUi.text("Full guide: " + e.getGuide(), GuideUi.HINT, CARD - 12));
		}
		clickable(row, "See how to finish it", () -> listener.onPickEntry(e.getName()));
		return GuideUi.full(row);
	}

	private static boolean has(ClogProgress.State s)
	{
		return s == ClogProgress.State.OBTAINED || s == ClogProgress.State.OWNED;
	}

	private static String marker(ClogProgress.State s)
	{
		return has(s) ? "✔" : s == ClogProgress.State.MISSING ? "✘" : "•";
	}

	private static Color color(ClogProgress.State s)
	{
		return has(s) ? GuideUi.GOOD : s == ClogProgress.State.MISSING ? GuideUi.TEXT : GuideUi.MUTED;
	}

	private static String stateNote(ClogProgress.State s)
	{
		return s == ClogProgress.State.OWNED ? " (in your bank)" : "";
	}

	private static JPanel rowPanel()
	{
		JPanel row = column();
		row.setOpaque(true);
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
		return row;
	}

	private static void clickable(JPanel row, String tip, Runnable action)
	{
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText(tip);
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				action.run();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
	}

	private void finish()
	{
		body.revalidate();
		body.repaint();
	}

	private static JPanel column()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(ColorScheme.DARK_GRAY_COLOR);
		p.setAlignmentX(LEFT_ALIGNMENT);
		return p;
	}
}
