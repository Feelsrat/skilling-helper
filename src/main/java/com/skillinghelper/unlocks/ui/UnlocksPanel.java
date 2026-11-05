package com.skillinghelper.unlocks.ui;

import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.engine.Format;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import com.skillinghelper.unlocks.UnlockStep;
import com.skillinghelper.unlocks.UnlockView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
 * Side panel: how many key unlocks the account has, the running guide's step, and every unlock in the suggested
 * order with what it gives, how it's got and what's still missing.
 */
public class UnlocksPanel extends PluginPanel
{
	private static final int WIDTH = GuideUi.WIDTH;
	private static final int CARD = GuideUi.CARD_WIDTH;

	public interface Listener
	{
		void onStartGuide();

		void onStopGuide();

		/**
		 * Guide this unlock next, whatever the suggested order says.
		 */
		void onPick(String id);

		void onSkip(String id);

		void onHideDone(boolean hide);

		/**
		 * Hand the unlock's current part to the plugin that guides it (diary guide, Skilling Helper).
		 */
		void onAction(String id);

		void onOpenWiki(String page);
	}

	private final Listener listener;
	private final HiddenRedraws redraws;
	private final JCheckBox hideDone = new JCheckBox("Hide unlocked");
	private final JPanel body = new JPanel();
	private boolean updating;

	public UnlocksPanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		redraws = new HiddenRedraws(this);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = column();
		header.setBorder(new EmptyBorder(0, 0, 6, 0));
		JLabel title = new JLabel("Account Unlocks");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(GuideUi.left(title));
		header.add(Box.createVerticalStrut(4));
		header.add(GuideUi.text("The unlocks that make the game smoother, in a good order: what each gives, which "
			+ "you have, and a step-by-step guide to the next.", GuideUi.MUTED, WIDTH));
		header.add(Box.createVerticalStrut(4));
		hideDone.setBackground(ColorScheme.DARK_GRAY_COLOR);
		hideDone.setForeground(GuideUi.TEXT);
		hideDone.setFont(FontManager.getRunescapeSmallFont());
		hideDone.addActionListener(e ->
		{
			if (!updating)
			{
				listener.onHideDone(hideDone.isSelected());
			}
		});
		header.add(GuideUi.left(hideDone));
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
		body.add(GuideUi.text("Log in to see your unlocks.", GuideUi.MUTED, WIDTH));
	}

	/**
	 * Shows the latest view. Swing thread.
	 */
	public void update(UnlockView view)
	{
		redraws.run("view", () -> apply(view));
	}

	private void apply(UnlockView v)
	{
		updating = true;
		hideDone.setSelected(v.isHideDone());
		updating = false;
		body.removeAll();
		if (!v.isLoggedIn())
		{
			body.add(GuideUi.text("Log in to see your unlocks.", GuideUi.MUTED, WIDTH));
			finish();
			return;
		}
		body.add(GuideUi.headline(v.getDone() + " of " + v.getTotal() + " unlocked",
			v.getDone() == v.getTotal() ? GuideUi.GOOD : Color.WHITE, WIDTH));
		body.add(Box.createVerticalStrut(6));
		if (v.isRunning())
		{
			body.add(GuideUi.button("Stop guide", listener::onStopGuide));
		}
		else if (v.getDone() < v.getTotal())
		{
			body.add(GuideUi.button("Start guide", listener::onStartGuide));
		}

		UnlockStep step = v.getStep();
		if (v.isRunning() && step != null)
		{
			body.add(Box.createVerticalStrut(8));
			body.add(stepCard(step, v.getStepWiki()));
		}

		body.add(GuideUi.section("Unlocks"));
		body.add(GuideUi.text("In the suggested order. Click one to be guided to it next.", GuideUi.MUTED, WIDTH));
		body.add(Box.createVerticalStrut(4));
		for (UnlockView.Row r : v.getRows())
		{
			body.add(row(r));
			body.add(Box.createVerticalStrut(3));
		}
		finish();
	}

	private JComponent stepCard(UnlockStep step, String wiki)
	{
		DiaryStep s = step.getStep();
		JPanel card = GuideUi.card(s.getKind() == DiaryStep.Kind.LOCKED ? GuideUi.WARN : GuideUi.HINT);
		GuideUi.fillStep(card, "Now", GuideUi.HINT, s.getTitle(), s.getLines());
		String id = step.getUnlockId();
		if (step.getAction() == UnlockStep.Action.DIARY)
		{
			card.add(Box.createVerticalStrut(6));
			card.add(GuideUi.button("Open diary guide", () -> listener.onAction(id)));
		}
		else if (step.getAction() == UnlockStep.Action.TRAIN)
		{
			card.add(Box.createVerticalStrut(6));
			card.add(GuideUi.button("Open Skilling Helper guide", () -> listener.onAction(id)));
		}
		if (wiki != null)
		{
			card.add(Box.createVerticalStrut(3));
			card.add(GuideUi.button("Open wiki page", () -> listener.onOpenWiki(wiki)));
		}
		if (id != null && s.getKind() != DiaryStep.Kind.DONE)
		{
			card.add(Box.createVerticalStrut(3));
			card.add(GuideUi.button("Skip this for now", () -> listener.onSkip(id)));
		}
		return GuideUi.full(card);
	}

	private JComponent row(UnlockView.Row r)
	{
		JPanel row = column();
		row.setOpaque(true);
		boolean current = r.getState() == UnlockView.RowState.CURRENT;
		row.setBackground(current ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
		String marker;
		Color color;
		switch (r.getState())
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
			case UNKNOWN:
				marker = "?";
				color = GuideUi.WARN;
				break;
			case SKIPPED:
				marker = "•";
				color = GuideUi.MUTED;
				break;
			default:
				marker = "•";
				color = GuideUi.TEXT;
		}
		row.add(GuideUi.line(marker + " " + r.getName(), color, CARD));
		row.add(GuideUi.text(r.getCategory(), GuideUi.MUTED, CARD - 12));
		for (String g : r.getGives())
		{
			row.add(GuideUi.text("+ " + g, GuideUi.TEXT, CARD - 12));
		}
		if (r.getState() != UnlockView.RowState.DONE)
		{
			if (!r.getHow().isEmpty())
			{
				row.add(GuideUi.text("How: " + r.getHow(), GuideUi.HINT, CARD - 12));
			}
			for (String m : r.getMissing())
			{
				row.add(GuideUi.check(false, m, CARD));
			}
			if (r.getCost() > 0)
			{
				row.add(GuideUi.text("Costs ~" + Format.gp(r.getCost()), GuideUi.GOLD, CARD - 12));
			}
			if (r.getState() == UnlockView.RowState.UNKNOWN)
			{
				row.add(GuideUi.text("Open your bank once so the plugin can see if you have it.", GuideUi.WARN,
					CARD - 12));
			}
			if (r.getState() == UnlockView.RowState.SKIPPED)
			{
				row.add(GuideUi.text("Skipped for now.", GuideUi.MUTED, CARD));
			}
			row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			row.setToolTipText("Guide me to this unlock");
			row.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					listener.onPick(r.getId());
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
