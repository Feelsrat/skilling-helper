package com.skillinghelper.ca.ui;

import com.skillinghelper.ca.CaRanker;
import com.skillinghelper.ca.CaView;
import com.skillinghelper.ca.data.CaReward;
import com.skillinghelper.ca.data.CaTasks;
import com.skillinghelper.ca.data.CaTier;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.Skin;
import com.skillinghelper.ui.HiddenRedraws;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: points and every tier's progress, the picked tier's reward and perks, the running guide's step, and
 * the tasks not done yet, easiest for you first.
 */
public class CaPanel extends PluginPanel
{
	private static final int WIDTH = GuideUi.WIDTH;
	private static final int CARD = GuideUi.CARD_WIDTH;

	public interface Listener
	{
		/**
		 * Show one tier, or every tier when {@code tier} is null.
		 */
		void onScope(CaTier tier);

		void onClearFocus();

		void onShowMore();

		void onStartGuide();

		void onStopGuide();

		void onPickTask(String key);

		void onSkipTask(String key);

		void onAllowWilderness(boolean allow);

		void onOpenWiki(String page);

		void onBossGuide(String pvmId);
	}

	private final Listener listener;
	private final HiddenRedraws redraws;
	private final JButton allButton = new Skin.Pill("All");
	private final JButton[] tierButtons = new JButton[CaTier.values().length];
	private final JCheckBox wilderness = new JCheckBox(Wilderness.TOGGLE_LABEL);
	private final JPanel body = new JPanel();
	/**
	 * Reward sections the player unfolded.
	 */
	private final Set<String> openSections = new HashSet<>();

	private CaView view;
	private boolean updating;

	public CaPanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		redraws = new HiddenRedraws(this);
		JPanel header = GuideUi.header("Combat Achievements", "/com/skillinghelper/ca/icon.png");

		JPanel tiers = new JPanel(new GridLayout(2, 4, 3, 3));
		tiers.setBackground(Skin.BG);
		style(allButton);
		allButton.setFocusPainted(false);
		allButton.addActionListener(e -> listener.onScope(null));
		tiers.add(allButton);
		for (CaTier t : CaTier.values())
		{
			JButton b = new Skin.Pill(shortLabel(t));
			style(b);
			b.setToolTipText(t.getLabel() + " tasks (" + t.getPoints() + " point" + (t.getPoints() > 1 ? "s" : "")
				+ " each)");
			b.addActionListener(e -> listener.onScope(t));
			tierButtons[t.ordinal()] = b;
			tiers.add(b);
		}
		GuideUi.options(header, GuideUi.subheading("Show tasks of"), GuideUi.full(tiers),
			GuideUi.wilderness(wilderness, "Off: Wilderness bosses are skipped.", () ->
			{
				if (!updating)
				{
					listener.onAllowWilderness(wilderness.isSelected());
				}
			}));
		GuideUi.frame(this, header, body);
		body.add(GuideUi.empty("Not logged in", "Log in to see the easiest Combat Achievements for you."));
	}

	private static String shortLabel(CaTier t)
	{
		return t == CaTier.GRANDMASTER ? "GM" : t.getLabel();
	}

	private static void style(JButton b)
	{
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setMargin(new Insets(2, 1, 2, 1));
	}

	/**
	 * Shows the latest view. Swing thread.
	 */
	public void update(CaView view)
	{
		redraws.run("view", () -> apply(view));
	}

	private void apply(CaView v)
	{
		view = v;
		updating = true;
		wilderness.setSelected(v.isWildernessAllowed());
		updating = false;
		GuideUi.select(allButton, v.getScope() == null && v.getFocus() == null, GuideUi.TEXT);
		for (CaTier t : CaTier.values())
		{
			JButton b = tierButtons[t.ordinal()];
			CaView.TierRow row = v.getTiers().isEmpty() ? null : v.getTiers().get(t.ordinal());
			boolean claimed = row != null && Boolean.TRUE.equals(row.getClaimed());
			b.setText((claimed ? "✔ " : "") + shortLabel(t));
			GuideUi.select(b, t == v.getScope(), claimed ? GuideUi.GOOD
				: row != null && row.isReached() ? GuideUi.GOLD : GuideUi.TEXT);
		}
		rebuild();
	}

	private void rebuild()
	{
		body.removeAll();
		CaView v = view;
		if (v == null || !v.isLoggedIn())
		{
			body.add(GuideUi.empty("Not logged in", "Log in to see the easiest Combat Achievements for you."));
			finish();
			return;
		}
		DiaryStep step = v.getStep();
		if (v.isRunning() && step != null)
		{
			String id = v.getPvmId();
			JButton boss = id == null ? null : GuideUi.small("Boss guide", () -> listener.onBossGuide(id));
			if (boss != null)
			{
				boss.setToolTipText("Opens this boss in PvM Progression: exact gear and supplies from your bank, "
					+ "step by step (turn that plugin on in the plugin list)");
			}
			String page = v.getWikiPage();
			String key = step.getTaskKey();
			body.add(GuideUi.stepCard(step.getKind() == DiaryStep.Kind.LOCKED ? GuideUi.WARN : GuideUi.HINT, "Now",
				step.getTitle(), step.getLines(), listener::onStopGuide, boss,
				page == null ? null : GuideUi.small("Wiki", () -> listener.onOpenWiki(page)),
				key != null && step.getKind() != DiaryStep.Kind.CLAIM
					? GuideUi.small("Skip task", () -> listener.onSkipTask(key)) : null));
		}
		else if (v.isRunning())
		{
			body.add(GuideUi.button("Stop guide", listener::onStopGuide));
		}
		else if (v.getLeft() > 0)
		{
			body.add(GuideUi.primary("▶ Start guide (easiest first)", listener::onStartGuide));
		}
		if (v.isRunning() && step != null)
		{
			body.add(GuideUi.more());
		}
		body.add(Box.createVerticalStrut(8));
		points(v);

		reward(v);

		body.add(GuideUi.section(v.getFocus() != null ? v.getFocus() + " tasks"
			: v.getScope() == null ? "Easiest tasks for you" : "Easiest " + v.getScope().getLabel() + " tasks"));
		if (v.getFocus() != null)
		{
			body.add(GuideUi.button("Show every boss", listener::onClearFocus));
			body.add(Box.createVerticalStrut(3));
		}
		body.add(GuideUi.hint(v.getDoneInScope() + " of " + v.getTotalInScope() + " done. Click a task to do it next."));
		body.add(Box.createVerticalStrut(4));
		for (CaView.TaskRow t : v.getTasks())
		{
			body.add(taskRow(t, v.getScope() == null));
			body.add(Box.createVerticalStrut(3));
		}
		if (v.getLeft() > v.getTasks().size())
		{
			body.add(GuideUi.button("Show more (" + (v.getLeft() - v.getTasks().size()) + " left)",
				listener::onShowMore));
		}
		else if (v.getLeft() == 0)
		{
			body.add(GuideUi.line("✔ Every task here is done.", GuideUi.GOOD, WIDTH));
		}
		GuideUi.foldSections(body, t -> t.equals(GuideUi.MORE) || t.equals("Tiers") || t.endsWith(" reward"),
			openSections);
		finish();
	}

	private void points(CaView v)
	{
		CaView.TierRow next = null;
		for (CaView.TierRow row : v.getTiers())
		{
			if (!row.isReached())
			{
				next = row;
				break;
			}
		}
		if (next == null)
		{
			body.add(GuideUi.headline(v.getPoints() + " points", Color.WHITE, WIDTH));
		}
		else
		{
			body.add(GuideUi.progress("Points", v.getPoints(), next.getThreshold()));
			body.add(GuideUi.line((next.getThreshold() - v.getPoints()) + " more for the " + next.getTier().getLabel()
				+ " reward (" + next.getThreshold() + ")", GuideUi.GOLD, GuideUi.TEXT, WIDTH));
		}
		body.add(GuideUi.section("Tiers"));
		for (CaView.TierRow row : v.getTiers())
		{
			String state;
			Color color;
			if (Boolean.TRUE.equals(row.getClaimed()))
			{
				state = "✔ claimed";
				color = GuideUi.GOOD;
			}
			else if (row.isReached())
			{
				state = row.getClaimed() == null ? "★ reached" : "★ claim from Ghommal";
				color = GuideUi.GOLD;
			}
			else
			{
				state = row.getThreshold() + " pts";
				color = GuideUi.TEXT;
			}
			body.add(GuideUi.stat(row.getTier().getLabel() + "  " + row.getDone() + "/" + row.getTotal(), state, color));
		}
	}

	private void reward(CaView v)
	{
		CaTier tier = v.getScope();
		if (tier == null)
		{
			for (CaView.TierRow row : v.getTiers())
			{
				if (!Boolean.TRUE.equals(row.getClaimed()))
				{
					tier = row.getTier();
					break;
				}
			}
		}
		CaReward reward = tier == null ? null : CaTasks.reward(tier);
		if (reward == null)
		{
			return;
		}
		CaView.TierRow row = v.getTiers().get(tier.ordinal());
		body.add(GuideUi.section(tier.getLabel() + " reward"));
		body.add(GuideUi.text("Needs " + row.getThreshold() + " points from tasks of any tier (you have "
			+ v.getPoints() + "). Claim it from Ghommal at the Warriors' Guild.", GuideUi.MUTED, WIDTH));
		body.add(Box.createVerticalStrut(2));
		for (String perk : reward.getPerks())
		{
			boolean sub = perk.startsWith("- ");
			body.add(GuideUi.line((sub ? "   – " + perk.substring(2) : "• " + perk), WIDTH));
		}
	}

	private JComponent taskRow(CaView.TaskRow t, boolean showTier)
	{
		JPanel row = GuideUi.row(t.isCurrent());
		String marker;
		Color color;
		if (t.isCurrent())
		{
			marker = "▶";
			color = GuideUi.OPTION;
		}
		else if (t.getFit() == CaRanker.Fit.LOCKED)
		{
			marker = "✘";
			color = GuideUi.BAD;
		}
		else if (t.getFit() == CaRanker.Fit.TOO_HARD)
		{
			marker = "▲";
			color = GuideUi.WARN;
		}
		else
		{
			marker = "•";
			color = t.isSkipped() ? GuideUi.MUTED : GuideUi.TEXT;
		}
		row.add(GuideUi.line(marker + " " + t.getName(), color, CARD));
		row.add(GuideUi.text((showTier ? t.getTier().getLabel() + " · " : "") + t.getMonster() + " · " + t.getType(),
			GuideUi.HINT, CARD - 12));
		row.add(GuideUi.text(t.getText(), GuideUi.MUTED, CARD - 12));
		if (t.getFit() == CaRanker.Fit.LOCKED)
		{
			for (String m : t.getMissing())
			{
				row.add(GuideUi.check(false, m, CARD));
			}
		}
		else
		{
			Color reasonColor = t.getFit() == CaRanker.Fit.READY ? GuideUi.GOOD : GuideUi.WARN;
			row.add(GuideUi.text(t.getReason(), reasonColor, CARD - 12));
		}
		if (t.isWilderness())
		{
			row.add(GuideUi.line(Wilderness.TAG, Wilderness.COLOR, CARD));
		}
		if (t.isSkipped())
		{
			row.add(GuideUi.text("Skipped for now.", GuideUi.MUTED, CARD));
		}
		GuideUi.clickable(row, "Guide me through this task", () -> listener.onPickTask(t.getKey()));
		return GuideUi.full(row);
	}

	private void finish()
	{
		body.revalidate();
		body.repaint();
	}
}
