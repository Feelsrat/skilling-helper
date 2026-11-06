package com.skillinghelper.collection.ui;

import com.skillinghelper.collection.ClogPlanner;
import com.skillinghelper.collection.ClogProgress;
import com.skillinghelper.collection.ClogView;
import com.skillinghelper.collection.CollectionLogPlugin;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.collection.data.ClogTab;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.Skin;
import com.skillinghelper.ui.HiddenRedraws;
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
import javax.swing.JPanel;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: how full the log is and whether it's synced, then either the running guide's step (with the page's
 * slots under it) or a Start button and one of four lists (quickest slots, closest pages, pets, browse by tab);
 * clicking a row starts the guide on it.
 */
public class CollectionLogPanel extends PluginPanel
{
	private static final int WIDTH = GuideUi.WIDTH;
	private static final int CARD = GuideUi.CARD_WIDTH;

	public interface Listener
	{
		void onMode(ClogView.Mode mode);

		void onTab(ClogTab tab);

		/**
		 * Starts the guide on the quickest slot anywhere.
		 */
		void onStart();

		void onStop();

		/**
		 * Starts the guide on a page, working through its slots.
		 */
		void onPickEntry(String name);

		/**
		 * Starts the guide on one slot, at the page given.
		 */
		void onPickSlot(String entry, int itemId);

		void onSkip();

		/**
		 * Opens the current page's full guide again, in its own plugin.
		 */
		void onOpenGuide();

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
		JPanel header = GuideUi.header("Collection Log", "/com/skillinghelper/collection/icon.png");
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
		header.add(Box.createVerticalStrut(4));
		GuideUi.options(header,
			GuideUi.wilderness(wilderness, "Off: Wilderness bosses and pets are left out.", () ->
			{
				if (!updating)
				{
					listener.onAllowWilderness(wilderness.isSelected());
				}
			}),
			GuideUi.toggle(locked, GuideUi.TEXT, "Also list what you don't have the quests or levels for yet", () ->
			{
				if (!updating)
				{
					listener.onShowLocked(locked.isSelected());
				}
			}));
		GuideUi.frame(this, header, body);
		body.add(GuideUi.empty("Not logged in", "Log in to see your collection log."));
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
		modes.setVisible(v.getGuide() == null);
		tabs.setVisible(v.getMode() == ClogView.Mode.BROWSE && v.getGuide() == null);
		wilderness.setSelected(v.isWildernessAllowed());
		locked.setSelected(v.isShowLocked());
		updating = false;

		body.removeAll();
		if (!v.isLoggedIn())
		{
			body.add(GuideUi.empty("Not logged in", "Log in to see your collection log."));
			finish();
			return;
		}
		summary(v);
		if (v.getGuide() != null)
		{
			guide(v.getGuide());
		}
		else
		{
			body.add(GuideUi.primary("▶ Start guide (quickest slot first)", listener::onStart));
			body.add(GuideUi.hint("Or click any slot or page below to be guided to it, step by step."));
			body.add(Box.createVerticalStrut(6));
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
		boolean game = v.getGameTotal() > 0;
		body.add(GuideUi.progress("Slots filled", game ? v.getGameCount() : v.getHave(),
			game ? v.getGameTotal() : v.getTotal()));
		body.add(Box.createVerticalStrut(3));
		if (v.getSyncedAt() == 0)
		{
			List<String> lines = new ArrayList<>();
			lines.add("1. Open your collection log: the Character Summary tab (green quest list tab), then "
				+ "Collection Log. Or read the collection log book.");
			lines.add("2. Click the log's Search button at the top (outlined in yellow). Don't type anything.");
			lines.add("That's all: the whole log is read in one go, and this box turns into \"✔ Synced\".");
			lines.add("Until then, items in your bank count as filled slots.");
			body.add(GuideUi.stepCard(GuideUi.WARN, "Sync", "Sync your log once", lines, null));
		}
		else if (v.getUnsynced() > 0)
		{
			List<String> lines = new ArrayList<>();
			lines.add("The game counts " + v.getUnsynced() + " more filled slot" + (v.getUnsynced() == 1 ? "" : "s")
				+ " than the plugin knows. Open the log and click Search again (outlined in yellow).");
			lines.add("Tip: turn on the game's \"collection log\" new-slot chat message (Settings, search "
				+ "\"collection\"), then new slots are ticked off as you get them.");
			body.add(GuideUi.stepCard(GuideUi.WARN, "Sync", "New slots to sync", lines, null));
		}
		else
		{
			body.add(GuideUi.line("✔ Synced " + CollectionLogPlugin.syncLabel(v.getSyncedAt()), GuideUi.GOOD, WIDTH));
		}
		body.add(Box.createVerticalStrut(6));
	}

	private void guide(ClogView.Guide g)
	{
		DiaryStep step = g.getStep();
		Color accent = step.getKind() == DiaryStep.Kind.LOCKED ? GuideUi.WARN
			: step.getKind() == DiaryStep.Kind.DONE ? GuideUi.GOOD : GuideUi.HINT;
		String ca = g.getCombatAchievements();
		String page = g.getEntry();
		body.add(GuideUi.stepCard(accent, "Now", step.getTitle(), step.getLines(), listener::onStop,
			g.getSlot() == null ? null : GuideUi.small("Skip slot", listener::onSkip),
			g.getHandOff() == null ? null : GuideUi.small("Open guide", listener::onOpenGuide),
			ca == null ? null : GuideUi.small("Combat tasks", () -> listener.onOpenCombatAchievements(ca)),
			page == null ? null : GuideUi.small("Wiki", () -> listener.onOpenWiki(page))));
		if (page == null)
		{
			return;
		}
		body.add(GuideUi.more());
		body.add(GuideUi.section(page + "  " + g.getHave() + "/" + g.getTotal()));
		boolean pets = page.endsWith("Pets");
		for (ClogView.SlotRow s : g.getSlots())
		{
			JComponent line = GuideUi.line((s.getName().equals(g.getSlot()) ? "▶ " : marker(s.getState()) + " ")
				+ s.getName() + "  " + ClogPlanner.percent(s.getPercent()) + stateNote(s.getState()),
				s.getName().equals(g.getSlot()) ? GuideUi.GOLD : color(s.getState()), WIDTH);
			if (!has(s.getState()) && !s.getName().equals(g.getSlot()) && (!pets || s.isGuided()))
			{
				GuideUi.clickable(line, "Go for this slot instead", () -> listener.onPickSlot(pets ? s.getEntry()
					: page, s.getId()));
			}
			body.add(line);
		}
	}

	private void quickest(ClogView v)
	{
		body.add(GuideUi.section("Quickest new slots"));
		body.add(GuideUi.text("Slots you don't have yet that most players have: usually the fastest to get. "
			+ "% = share of players with it. Click one to be guided to it.", GuideUi.MUTED, WIDTH));
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
		body.add(GuideUi.text("The ones most players have come first. Click one to be guided to it.",
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
		body.add(GuideUi.text("Pages you've started, fewest slots left first. Click one to be guided through it.",
			GuideUi.MUTED, WIDTH));
		body.add(Box.createVerticalStrut(4));
		if (v.getEntries().isEmpty())
		{
			body.add(GuideUi.text("No pages started yet that you can do. Sync your log (above) so the plugin "
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
		if (s.isGuided() && !s.getEntry().isEmpty() && !has(s.getState()))
		{
			clickable(row, "Guide me to it, step by step", () -> listener.onPickSlot(s.getEntry(), s.getId()));
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
		if (!done)
		{
			clickable(row, "Guide me through this page, slot by slot", () -> listener.onPickEntry(e.getName()));
		}
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
		row.setBackground(Skin.CARD);
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
				row.setBackground(Skin.CARD_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(Skin.CARD);
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
		p.setBackground(Skin.BG);
		p.setAlignmentX(LEFT_ALIGNMENT);
		return p;
	}
}
