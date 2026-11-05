package com.skillinghelper.ca.ui;

import com.skillinghelper.ca.CaRanker;
import com.skillinghelper.ca.CaView;
import com.skillinghelper.ca.data.CaReward;
import com.skillinghelper.ca.data.CaTasks;
import com.skillinghelper.ca.data.CaTier;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
	private final JButton allButton = new JButton("All");
	private final JButton[] tierButtons = new JButton[CaTier.values().length];
	private final JCheckBox wilderness = new JCheckBox(Wilderness.TOGGLE_LABEL);
	private final JPanel body = new JPanel();

	private CaView view;
	private boolean updating;

	public CaPanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		redraws = new HiddenRedraws(this);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = column();
		header.setBorder(new EmptyBorder(0, 0, 6, 0));
		JLabel title = new JLabel("Combat Achievements");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(GuideUi.left(title));
		header.add(Box.createVerticalStrut(4));
		header.add(GuideUi.text("The easiest tasks for you first, step by step, and what each tier's reward gives.",
			GuideUi.MUTED, WIDTH));
		header.add(Box.createVerticalStrut(6));

		JPanel tiers = new JPanel(new GridLayout(2, 4, 3, 3));
		tiers.setBackground(ColorScheme.DARK_GRAY_COLOR);
		style(allButton);
		allButton.addActionListener(e -> listener.onScope(null));
		tiers.add(allButton);
		for (CaTier t : CaTier.values())
		{
			JButton b = new JButton(shortLabel(t));
			style(b);
			b.setToolTipText(t.getLabel() + " tasks (" + t.getPoints() + " point" + (t.getPoints() > 1 ? "s" : "")
				+ " each)");
			b.addActionListener(e -> listener.onScope(t));
			tierButtons[t.ordinal()] = b;
			tiers.add(b);
		}
		header.add(GuideUi.full(tiers));
		wilderness.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wilderness.setForeground(Wilderness.COLOR);
		wilderness.setFont(FontManager.getRunescapeSmallFont());
		wilderness.setToolTipText("Off: Wilderness bosses are skipped. Shared with the other guides in this plugin "
			+ "pack.");
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
		body.add(GuideUi.text("Log in to see your Combat Achievements.", GuideUi.MUTED, WIDTH));
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
		allButton.setForeground(v.getScope() == null && v.getFocus() == null ? GuideUi.OPTION : GuideUi.TEXT);
		for (CaTier t : CaTier.values())
		{
			JButton b = tierButtons[t.ordinal()];
			CaView.TierRow row = v.getTiers().isEmpty() ? null : v.getTiers().get(t.ordinal());
			boolean claimed = row != null && Boolean.TRUE.equals(row.getClaimed());
			b.setText((claimed ? "✔ " : "") + shortLabel(t));
			b.setForeground(t == v.getScope() ? GuideUi.OPTION : claimed ? GuideUi.GOOD
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
			body.add(GuideUi.text("Log in to see your Combat Achievements.", GuideUi.MUTED, WIDTH));
			finish();
			return;
		}
		points(v);
		body.add(Box.createVerticalStrut(6));
		if (v.isRunning())
		{
			body.add(GuideUi.button("Stop guide", listener::onStopGuide));
		}
		else if (v.getLeft() > 0)
		{
			body.add(GuideUi.button("Start guide (easiest first)", listener::onStartGuide));
		}

		DiaryStep step = v.getStep();
		if (v.isRunning() && step != null)
		{
			body.add(Box.createVerticalStrut(8));
			JPanel card = GuideUi.card(step.getKind() == DiaryStep.Kind.LOCKED ? GuideUi.WARN : GuideUi.HINT);
			GuideUi.fillStep(card, "Now", GuideUi.HINT, step.getTitle(), step.getLines());
			if (v.getPvmId() != null)
			{
				String id = v.getPvmId();
				card.add(Box.createVerticalStrut(6));
				JButton boss = GuideUi.button("Full boss guide", () -> listener.onBossGuide(id));
				boss.setToolTipText("Opens this boss in PvM Progression: exact gear and supplies from your bank, "
					+ "step by step (turn that plugin on in the plugin list)");
				card.add(boss);
			}
			if (v.getWikiPage() != null)
			{
				String page = v.getWikiPage();
				card.add(Box.createVerticalStrut(3));
				card.add(GuideUi.button("Open task on the wiki", () -> listener.onOpenWiki(page)));
			}
			if (step.getTaskKey() != null && step.getKind() != DiaryStep.Kind.CLAIM)
			{
				String key = step.getTaskKey();
				card.add(Box.createVerticalStrut(3));
				card.add(GuideUi.button("Skip this task for now", () -> listener.onSkipTask(key)));
			}
			body.add(GuideUi.full(card));
		}

		reward(v);

		body.add(GuideUi.section(v.getFocus() != null ? v.getFocus() + " tasks"
			: v.getScope() == null ? "Easiest tasks for you" : "Easiest " + v.getScope().getLabel() + " tasks"));
		if (v.getFocus() != null)
		{
			body.add(GuideUi.button("Show every boss", listener::onClearFocus));
			body.add(Box.createVerticalStrut(3));
		}
		body.add(GuideUi.text(v.getDoneInScope() + " of " + v.getTotalInScope() + " done. Your fighting strength is about "
			+ v.getPower() + " (" + v.getPowerStyle() + (v.getWeapon() != null ? ", " + v.getWeapon() : "")
			+ "). Click a task to be guided through it.", GuideUi.MUTED, WIDTH));
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
		finish();
	}

	private void points(CaView v)
	{
		body.add(GuideUi.headline(v.getPoints() + " points", Color.WHITE, WIDTH));
		CaView.TierRow next = null;
		for (CaView.TierRow row : v.getTiers())
		{
			if (!row.isReached())
			{
				next = row;
				break;
			}
		}
		if (next != null)
		{
			body.add(GuideUi.line((next.getThreshold() - v.getPoints()) + " more for the " + next.getTier().getLabel()
				+ " reward (" + next.getThreshold() + ")", GuideUi.GOLD, GuideUi.TEXT, WIDTH));
		}
		body.add(Box.createVerticalStrut(3));
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
		JPanel row = column();
		row.setOpaque(true);
		row.setBackground(t.isCurrent() ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
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
