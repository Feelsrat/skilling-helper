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
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Map;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
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
		JPanel header = GuideUi.header("Achievement Diaries", "Every task step by step, with rewards.");
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
			JButton b = GuideUi.tab(t.getLabel(), () ->
			{
				tier = t;
				listener.onSelect((DiaryRegion) regions.getSelectedItem(), t);
			});
			tierButtons[t.ordinal()] = b;
			tiers.add(b);
		}
		header.add(GuideUi.full(tiers));
		header.add(Box.createVerticalStrut(2));
		header.add(GuideUi.wilderness(wilderness, "Off: Wilderness tasks are skipped.", () ->
		{
			if (!updating)
			{
				listener.onAllowWilderness(wilderness.isSelected());
			}
		}));
		GuideUi.frame(this, header, body);
		body.add(GuideUi.empty("Not logged in", "Log in to see your diaries and get guided through each task."));
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
			GuideUi.select(b, t == tier, row != null && row.isClaimed() ? GuideUi.GOOD : GuideUi.TEXT);
		}
		rebuild();
	}

	private void rebuild()
	{
		body.removeAll();
		DiaryView v = view;
		if (v == null || !v.isLoggedIn())
		{
			body.add(GuideUi.empty("Not logged in", "Log in to see your diaries and get guided through each task."));
			finish();
			return;
		}
		DiaryView.TierRow row = v.getTiers().get(tier.ordinal());
		String headline = v.getRegion().getLabel() + " " + tier.getLabel();
		if (v.isGuided())
		{
			body.add(GuideUi.progress(headline, row.getDone(),
				row.getTotal()));
			body.add(Box.createVerticalStrut(3));
			if (row.isClaimed())
			{
				body.add(GuideUi.line("✔ Done and reward claimed", GuideUi.GOOD, WIDTH));
			}
			else if (row.getDone() == row.getTotal())
			{
				body.add(GuideUi.line("★ All tasks done: claim your reward", GuideUi.GOLD, WIDTH));
			}
			else
			{
				body.add(GuideUi.stat("Left to buy", v.getTierCost() > 0 ? "~" + Format.gp(v.getTierCost()) : "nothing",
					GuideUi.GOLD));
			}
		}
		else
		{
			body.add(GuideUi.headline(headline, Color.WHITE, WIDTH));
			body.add(GuideUi.line(row.isClaimed() ? "✔ Reward claimed" : "Not finished yet",
				row.isClaimed() ? GuideUi.GOOD : GuideUi.TEXT, WIDTH));
			body.add(Box.createVerticalStrut(3));
			body.add(GuideUi.line("⚠ Step-by-step guides for this diary are coming soon. The reward and its perks are below.",
				GuideUi.WARN, GuideUi.TEXT, WIDTH));
		}
		body.add(Box.createVerticalStrut(8));

		DiaryStep step = v.getStep();
		JButton wiki = GuideUi.small("Wiki", listener::onOpenWiki);
		if (v.isGuided() && v.isRunning() && step != null)
		{
			String key = step.getTaskKey();
			body.add(GuideUi.stepCard(step.getKind() == DiaryStep.Kind.LOCKED ? GuideUi.WARN : GuideUi.HINT, "Now",
				step.getTitle(), step.getLines(), listener::onStopGuide, wiki,
				key != null && step.getKind() != DiaryStep.Kind.CLAIM
					? GuideUi.small("Skip task", () -> listener.onSkipTask(key)) : null));
		}
		else if (v.isGuided() && v.isRunning())
		{
			body.add(GuideUi.actions(GuideUi.small("Stop guide", listener::onStopGuide), wiki));
		}
		else if (v.isGuided() && !row.isClaimed())
		{
			body.add(GuideUi.primary("▶ Start guide", listener::onStartGuide));
			body.add(Box.createVerticalStrut(3));
			body.add(GuideUi.actions(wiki));
		}
		else
		{
			body.add(GuideUi.actions(wiki));
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
			body.add(GuideUi.hint("Click a task to be guided through it next."));
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
		JPanel row = GuideUi.row(t.getState() == DiaryView.TaskState.CURRENT);
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
			GuideUi.clickable(row, "Guide me through this task", () -> listener.onPickTask(t.getKey()));
		}
		return GuideUi.full(row);
	}

	private void finish()
	{
		body.revalidate();
		body.repaint();
	}
}
