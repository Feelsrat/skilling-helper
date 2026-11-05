package com.skillinghelper.minigame.ui;

import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.minigame.MinigameView;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: pick a minigame, see what it needs (ticked), which rewards are worth chasing and which you own, start
 * the guide (here or in the plugin that has it), and the running guide's current step. Below that, the other
 * minigames you can do that still have rewards you don't own.
 */
public class MinigamePanel extends PluginPanel
{
	private static final int WIDTH = GuideUi.WIDTH;
	private static final int CARD = GuideUi.CARD_WIDTH;

	public interface Listener
	{
		void onSelect(String id);

		void onStartGuide();

		void onStopGuide();

		void onAllowWilderness(boolean allow);

		void onOpenWiki();
	}

	private final Listener listener;
	private final HiddenRedraws redraws;
	private final JComboBox<MinigameView.Row> picker = new JComboBox<>();
	private final JCheckBox wilderness = new JCheckBox(Wilderness.TOGGLE_LABEL);
	private final JPanel body = new JPanel();

	private MinigameView view;
	private boolean updating;
	private final Set<String> openSections = new HashSet<>();

	public MinigamePanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		redraws = new HiddenRedraws(this);
		JPanel header = GuideUi.header("Minigames", null);

		picker.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof MinigameView.Row)
				{
					setText(label((MinigameView.Row) value));
				}
				return this;
			}
		});
		picker.addActionListener(e ->
		{
			Object item = picker.getSelectedItem();
			if (!updating && item instanceof MinigameView.Row)
			{
				listener.onSelect(((MinigameView.Row) item).getId());
			}
		});
		header.add(GuideUi.full(picker));
		header.add(Box.createVerticalStrut(4));
		GuideUi.options(header, GuideUi.wilderness(wilderness, "Off: Wilderness minigames are left out.", () ->
		{
			if (!updating)
			{
				listener.onAllowWilderness(wilderness.isSelected());
			}
		}));
		GuideUi.frame(this, header, body);
		body.add(GuideUi.empty("Not logged in", "Log in to see the minigames."));
	}

	/**
	 * "✔ Tithe Farm  3/7": ✔ = every reward worth chasing owned, ✘ = can't play yet.
	 */
	private static String label(MinigameView.Row r)
	{
		String mark = !r.isReady() ? "✘ " : r.getCheckable() > 0 && r.getOwned() == r.getCheckable() ? "✔ " : "";
		String count = r.getCheckable() > 0 ? "  " + r.getOwned() + "/" + r.getCheckable() : "";
		return mark + r.getName() + count;
	}

	/**
	 * Shows the latest view. Swing thread.
	 */
	public void update(MinigameView view)
	{
		redraws.run("view", () -> apply(view));
	}

	private void apply(MinigameView v)
	{
		MinigameView old = view;
		view = v;
		updating = true;
		if (old == null || !Objects.equals(old.getRows(), v.getRows()))
		{
			picker.removeAllItems();
			v.getRows().forEach(picker::addItem);
		}
		for (int i = 0; i < picker.getItemCount(); i++)
		{
			if (picker.getItemAt(i).getId().equals(v.getId()))
			{
				picker.setSelectedIndex(i);
			}
		}
		wilderness.setSelected(v.isWildernessAllowed());
		updating = false;
		rebuild();
	}

	private void rebuild()
	{
		body.removeAll();
		MinigameView v = view;
		if (v == null || !v.isLoggedIn() || v.getId() == null)
		{
			body.add(GuideUi.empty("Not logged in", "Log in to see the minigames."));
			finish();
			return;
		}
		DiaryStep step = v.getStep();
		boolean guiding = v.isRunning() && step != null;
		if (guiding)
		{
			body.add(GuideUi.stepCard(step.getKind() == DiaryStep.Kind.LOCKED ? GuideUi.WARN : GuideUi.HINT, "Now",
				step.getTitle(), step.getLines(), listener::onStopGuide,
				GuideUi.small("Wiki", listener::onOpenWiki)));
			body.add(GuideUi.more());
		}
		body.add(GuideUi.headline(v.getName(), Color.WHITE, WIDTH));
		if (v.getLevelRange() != null)
		{
			body.add(GuideUi.text(v.getLevelRange(), GuideUi.MUTED, WIDTH));
		}
		body.add(Box.createVerticalStrut(3));
		body.add(GuideUi.text(v.getSummary(), GuideUi.TEXT, WIDTH));
		if (v.getTeam() != null)
		{
			body.add(Box.createVerticalStrut(2));
			body.add(GuideUi.line("• " + v.getTeam(), GuideUi.MUTED, GuideUi.TEXT, WIDTH));
		}
		if (v.isDangerous())
		{
			body.add(GuideUi.line("⚠ Dangerous: you lose items if you die", GuideUi.WARN, GuideUi.TEXT, WIDTH));
		}
		if (v.isWilderness())
		{
			body.add(GuideUi.line(Wilderness.TAG, Wilderness.COLOR, WIDTH));
		}

		body.add(Box.createVerticalStrut(8));
		if (!guiding)
		{
			startButtons(v);
		}

		body.add(GuideUi.section("Requirements"));
		if (v.getMissing().isEmpty() && v.getMet().isEmpty() && v.getNotes().isEmpty())
		{
			body.add(GuideUi.line("✔ None, anyone can play", GuideUi.GOOD, GuideUi.TEXT, WIDTH));
		}
		for (String m : v.getMet())
		{
			body.add(GuideUi.check(true, m, WIDTH));
		}
		for (String m : v.getMissing())
		{
			body.add(GuideUi.check(false, m, WIDTH));
		}
		for (String n : v.getNotes())
		{
			body.add(GuideUi.line("? " + n, GuideUi.WARN, GuideUi.TEXT, WIDTH));
		}
		if (!v.getBring().isEmpty())
		{
			body.add(Box.createVerticalStrut(3));
			body.add(GuideUi.text("Bring: " + String.join(", ", v.getBring()), GuideUi.TEXT, WIDTH));
		}
		if (v.getGetThere() != null)
		{
			body.add(Box.createVerticalStrut(3));
			body.add(GuideUi.text("Getting there: " + v.getGetThere(), GuideUi.MUTED, WIDTH));
		}

		rewards(v);
		others(v);
		GuideUi.foldSections(body, t -> t.equals(GuideUi.MORE) || t.equals("Still worth doing"), openSections);
		finish();
	}

	private void startButtons(MinigameView v)
	{
		switch (v.getHandoff())
		{
			case SKILLING_HELPER:
			case PVM_PROGRESSION:
				String plugin = v.getHandoff() == MinigameView.Handoff.SKILLING_HELPER ? "Skilling Helper" : "PvM Progression";
				if (v.isHandoffActive())
				{
					body.add(GuideUi.primary("▶ Start guide (in " + plugin + ")", listener::onStartGuide));
					body.add(GuideUi.text("The full click-by-click guide for this one is in " + plugin + ".",
						GuideUi.MUTED, WIDTH));
				}
				else
				{
					body.add(GuideUi.line("⚠ The full guide for this one is in " + plugin + ": turn that plugin on "
						+ "in the plugin list to use it.", GuideUi.WARN, GuideUi.TEXT, WIDTH));
				}
				break;
			default:
				if (v.isRunning())
				{
					body.add(GuideUi.button("Stop guide", listener::onStopGuide));
				}
				else if (v.isGuidedHere())
				{
					body.add(GuideUi.primary("▶ Start guide", listener::onStartGuide));
				}
		}
		body.add(Box.createVerticalStrut(3));
		body.add(GuideUi.actions(GuideUi.small("Wiki", listener::onOpenWiki)));
	}

	private void rewards(MinigameView v)
	{
		List<MinigameView.RewardRow> worth = new ArrayList<>();
		List<MinigameView.RewardRow> also = new ArrayList<>();
		for (MinigameView.RewardRow r : v.getRewards())
		{
			(r.isWorth() ? worth : also).add(r);
		}
		body.add(GuideUi.section("Rewards worth chasing"));
		for (String balance : v.getBalances())
		{
			body.add(GuideUi.line("You have " + balance, GuideUi.GOLD, GuideUi.TEXT, WIDTH));
		}
		if (!v.isBankKnown())
		{
			body.add(GuideUi.text("Open your bank once so rewards in it count as owned.", GuideUi.MUTED, WIDTH));
		}
		for (MinigameView.RewardRow r : worth)
		{
			body.add(rewardRow(r));
			body.add(Box.createVerticalStrut(3));
		}
		if (!also.isEmpty())
		{
			body.add(GuideUi.subheading("Also"));
			for (MinigameView.RewardRow r : also)
			{
				String owned = Boolean.TRUE.equals(r.getOwned()) ? "✔ " : "• ";
				body.add(GuideUi.line(owned + r.getName() + ": " + r.getWhy(), GuideUi.MUTED, GuideUi.TEXT, WIDTH));
			}
		}
	}

	private JComponent rewardRow(MinigameView.RewardRow r)
	{
		JPanel row = column();
		row.setOpaque(true);
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
		String marker;
		Color color;
		if (Boolean.TRUE.equals(r.getOwned()))
		{
			marker = "✔";
			color = GuideUi.GOOD;
		}
		else if (r.isAffordable())
		{
			marker = "★";
			color = GuideUi.GOLD;
		}
		else
		{
			marker = "•";
			color = GuideUi.TEXT;
		}
		row.add(GuideUi.line(marker + " " + r.getName(), color, CARD));
		row.add(GuideUi.text(r.getWhy(), GuideUi.MUTED, CARD - 12));
		if (Boolean.TRUE.equals(r.getOwned()))
		{
			row.add(GuideUi.text("You have it.", GuideUi.GOOD, CARD - 12));
		}
		else
		{
			if (r.getCost() != null)
			{
				row.add(GuideUi.text("Costs: " + r.getCost(), GuideUi.GOLD, CARD - 12));
			}
			if (r.isAffordable())
			{
				row.add(GuideUi.text("You can afford it now.", GuideUi.GOLD, CARD - 12));
			}
		}
		return GuideUi.full(row);
	}

	/**
	 * Other minigames you can play that still have rewards worth chasing you don't own.
	 */
	private void others(MinigameView v)
	{
		List<MinigameView.Row> todo = new ArrayList<>();
		for (MinigameView.Row r : v.getRows())
		{
			if (r.isReady() && r.getOwned() < r.getCheckable() && !r.getId().equals(v.getId()))
			{
				todo.add(r);
			}
		}
		if (todo.isEmpty())
		{
			return;
		}
		body.add(GuideUi.section("Still worth doing"));
		for (MinigameView.Row r : todo)
		{
			JComponent line = GuideUi.line("• " + r.getName() + " (" + r.getCategory() + "), " + r.getOwned() + " of "
				+ r.getCheckable() + " owned", GuideUi.OPTION, GuideUi.TEXT, WIDTH);
			GuideUi.clickable(line, "Show " + r.getName(), () -> listener.onSelect(r.getId()));
			body.add(line);
		}
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
