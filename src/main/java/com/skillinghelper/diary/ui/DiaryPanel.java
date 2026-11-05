package com.skillinghelper.diary.ui;

import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.diary.DiaryView;
import com.skillinghelper.diary.data.Diaries;
import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryReward;
import com.skillinghelper.diary.data.DiaryTier;
import com.skillinghelper.engine.Format;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: pick a diary and tier, see its reward and perks, what's still missing, every task (done ticked),
 * and the running guide's current step.
 */
public class DiaryPanel extends PluginPanel
{
	private static final int WIDTH = GuideUi.WIDTH;
	private static final int CARD = GuideUi.CARD_WIDTH;

	public interface Listener
	{
		void onSelect(DiaryRegion region, DiaryTier tier);

		void onStartGuide();

		void onStopGuide();

		/**
		 * Guide this task next, whatever the walking order says.
		 */
		void onPickTask(String key);

		void onSkipTask(String key);

		void onAllowWilderness(boolean allow);

		void onOpenWiki();
	}

	private final Listener listener;
	private final HiddenRedraws redraws;
	private final JComboBox<DiaryRegion> regions = new JComboBox<>(DiaryRegion.values());
	private final JButton[] tierButtons = new JButton[DiaryTier.values().length];
	private final JCheckBox wilderness = new JCheckBox(Wilderness.TOGGLE_LABEL);
	private final JPanel body = new JPanel();

	private DiaryView view;
	private DiaryTier tier = DiaryTier.EASY;
	private boolean updating;

	public DiaryPanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		redraws = new HiddenRedraws(this);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = column();
		header.setBorder(new EmptyBorder(0, 0, 6, 0));
		JLabel title = new JLabel("Achievement Diaries");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(GuideUi.left(title));
		header.add(Box.createVerticalStrut(4));
		header.add(GuideUi.text("Step-by-step guides for every task, and what each tier's reward gives you.",
			GuideUi.MUTED, WIDTH));
		header.add(Box.createVerticalStrut(6));

		regions.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof DiaryRegion)
				{
					DiaryRegion r = (DiaryRegion) value;
					Map<DiaryRegion, Integer> claimed = view == null ? null : view.getClaimedTiers();
					int n = claimed == null ? 0 : claimed.getOrDefault(r, 0);
					setText(r.getLabel() + (claimed == null || claimed.isEmpty() ? "" : "  " + n + "/4")
						+ (Diaries.guided(r) ? "" : "  (soon)"));
				}
				return this;
			}
		});
		regions.addActionListener(e ->
		{
			if (!updating && regions.getSelectedItem() != null)
			{
				listener.onSelect((DiaryRegion) regions.getSelectedItem(), tier);
			}
		});
		header.add(GuideUi.full(regions));
		header.add(Box.createVerticalStrut(4));
		JPanel tiers = new JPanel(new GridLayout(1, 4, 3, 0));
		tiers.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (DiaryTier t : DiaryTier.values())
		{
			JButton b = new JButton(t.getLabel());
			b.setFont(FontManager.getRunescapeSmallFont());
			b.setMargin(new java.awt.Insets(2, 1, 2, 1));
			b.addActionListener(e ->
			{
				tier = t;
				listener.onSelect((DiaryRegion) regions.getSelectedItem(), t);
			});
			tierButtons[t.ordinal()] = b;
			tiers.add(b);
		}
		header.add(GuideUi.full(tiers));
		wilderness.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wilderness.setForeground(Wilderness.COLOR);
		wilderness.setFont(FontManager.getRunescapeSmallFont());
		wilderness.setToolTipText("Off: Wilderness tasks are skipped. Shared with the other guides in this plugin pack.");
		wilderness.addActionListener(e ->
		{
			if (!updating)
			{
				listener.onAllowWilderness(wilderness.isSelected());
			}
		});
		header.add(GuideUi.left(wilderness));
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
		body.add(GuideUi.text("Log in to see your diaries.", GuideUi.MUTED, WIDTH));
	}

	/**
	 * Shows the latest view. Swing thread.
	 */
	public void update(DiaryView view)
	{
		redraws.run("view", () -> apply(view));
	}

	private void apply(DiaryView v)
	{
		view = v;
		tier = v.getTier();
		updating = true;
		regions.setSelectedItem(v.getRegion());
		wilderness.setSelected(v.isWildernessAllowed());
		updating = false;
		regions.repaint();
		for (DiaryTier t : DiaryTier.values())
		{
			JButton b = tierButtons[t.ordinal()];
			DiaryView.TierRow row = v.getTiers().isEmpty() ? null : v.getTiers().get(t.ordinal());
			String mark = row == null ? "" : row.isClaimed() ? "✔ " : "";
			b.setText(mark + t.getLabel());
			b.setForeground(t == tier ? GuideUi.OPTION : row != null && row.isClaimed() ? GuideUi.GOOD : GuideUi.TEXT);
		}
		rebuild();
	}

	private void rebuild()
	{
		body.removeAll();
		DiaryView v = view;
		if (v == null || !v.isLoggedIn())
		{
			body.add(GuideUi.text("Log in to see your diaries.", GuideUi.MUTED, WIDTH));
			finish();
			return;
		}
		DiaryView.TierRow row = v.getTiers().get(tier.ordinal());
		String headline = v.getRegion().getLabel() + " " + tier.getLabel();
		body.add(GuideUi.headline(headline, Color.WHITE, WIDTH));
		if (v.isGuided())
		{
			Color c = row.isClaimed() ? GuideUi.GOOD : row.getDone() == row.getTotal() ? GuideUi.GOLD : GuideUi.TEXT;
			String status = row.isClaimed() ? "✔ Done and reward claimed"
				: row.getDone() == row.getTotal() ? "★ All tasks done: claim your reward"
				: row.getDone() + " of " + row.getTotal() + " tasks done";
			body.add(GuideUi.line(status, c, WIDTH));
			if (!row.isClaimed() && row.getDone() < row.getTotal())
			{
				body.add(GuideUi.line(v.getTierCost() > 0
					? "Costs about " + Format.gp(v.getTierCost()) + " more to finish (Grand Exchange prices, what you own counts)"
					: "Costs nothing more to finish", GuideUi.GOLD, GuideUi.TEXT, WIDTH));
			}
		}
		else
		{
			body.add(GuideUi.line(row.isClaimed() ? "✔ Reward claimed" : "Not finished yet",
				row.isClaimed() ? GuideUi.GOOD : GuideUi.TEXT, WIDTH));
			body.add(Box.createVerticalStrut(3));
			body.add(GuideUi.line("⚠ Step-by-step guides for this diary are coming soon. The reward and its perks are below.",
				GuideUi.WARN, GuideUi.TEXT, WIDTH));
		}
		body.add(Box.createVerticalStrut(6));
		if (v.isGuided())
		{
			if (v.isRunning())
			{
				body.add(GuideUi.button("Stop guide", listener::onStopGuide));
			}
			else if (!row.isClaimed())
			{
				body.add(GuideUi.button("Start guide", listener::onStartGuide));
			}
		}
		body.add(Box.createVerticalStrut(3));
		body.add(GuideUi.button("Open wiki page", listener::onOpenWiki));

		DiaryStep step = v.getStep();
		if (v.isRunning() && step != null)
		{
			body.add(Box.createVerticalStrut(8));
			JPanel card = GuideUi.card(step.getKind() == DiaryStep.Kind.LOCKED ? GuideUi.WARN : GuideUi.HINT);
			GuideUi.fillStep(card, "Now", GuideUi.HINT, step.getTitle(), step.getLines());
			if (step.getTaskKey() != null && step.getKind() != DiaryStep.Kind.CLAIM)
			{
				String key = step.getTaskKey();
				card.add(Box.createVerticalStrut(6));
				card.add(GuideUi.button("Skip this task for now", () -> listener.onSkipTask(key)));
			}
			body.add(GuideUi.full(card));
		}

		DiaryReward reward = Diaries.reward(v.getRegion(), tier);
		if (reward != null)
		{
			body.add(GuideUi.section("Reward"));
			body.add(GuideUi.headline(reward.getItem(), GuideUi.GOLD, WIDTH));
			for (String perk : reward.getPerks())
			{
				body.add(Box.createVerticalStrut(2));
				body.add(GuideUi.line("• " + perk, WIDTH));
			}
		}

		if (v.isGuided() && !v.getStillNeeded().isEmpty())
		{
			body.add(GuideUi.section("Still needed"));
			for (String need : v.getStillNeeded())
			{
				body.add(GuideUi.check(false, need, WIDTH));
			}
		}

		if (v.isGuided())
		{
			body.add(GuideUi.section("Tasks"));
			body.add(GuideUi.text("Click a task to be guided through it next.", GuideUi.MUTED, WIDTH));
			body.add(Box.createVerticalStrut(4));
			for (DiaryView.TaskRow t : v.getTasks())
			{
				body.add(taskRow(t));
				body.add(Box.createVerticalStrut(3));
			}
		}
		finish();
	}

	private JComponent taskRow(DiaryView.TaskRow t)
	{
		JPanel row = column();
		row.setOpaque(true);
		boolean current = t.getState() == DiaryView.TaskState.CURRENT;
		row.setBackground(current ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
		String marker;
		Color color;
		switch (t.getState())
		{
			case DONE:
				marker = "✔";
				color = GuideUi.GOOD;
				break;
			case CURRENT:
				marker = "▶";
				color = GuideUi.OPTION;
				break;
			case LOCKED:
				marker = "✘";
				color = GuideUi.BAD;
				break;
			case SKIPPED:
				marker = "•";
				color = GuideUi.MUTED;
				break;
			default:
				marker = "•";
				color = GuideUi.TEXT;
		}
		// The number goes after the name: a leading "1." would be read as a numbered step
		row.add(GuideUi.line(marker + " " + t.getName(), color, CARD));
		if (t.getState() != DiaryView.TaskState.DONE)
		{
			row.add(GuideUi.text("Task " + t.getNumber() + ": " + t.getText(), GuideUi.MUTED, CARD - 12));
			for (String m : t.getMissing())
			{
				row.add(GuideUi.check(false, m, CARD));
			}
			for (String n : t.getNotes())
			{
				row.add(GuideUi.line("? " + n, GuideUi.WARN, GuideUi.TEXT, CARD));
			}
			if (!t.getItems().isEmpty())
			{
				row.add(GuideUi.text("Bring: " + String.join(", ", t.getItems()), GuideUi.TEXT, CARD - 12));
			}
			if (t.getCost() > 0)
			{
				row.add(GuideUi.text("Costs ~" + Format.gp(t.getCost()), GuideUi.GOLD, CARD - 12));
			}
			if (t.isWilderness())
			{
				row.add(GuideUi.line(Wilderness.TAG, Wilderness.COLOR, CARD));
			}
			if (t.getState() == DiaryView.TaskState.SKIPPED)
			{
				row.add(GuideUi.text("Skipped for now.", GuideUi.MUTED, CARD));
			}
			row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			row.setToolTipText("Guide me through this task");
			row.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					listener.onPickTask(t.getKey());
				}
			});
		}
		return GuideUi.full(row);
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
