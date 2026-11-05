package com.skillinghelper.unlocks.ui;

import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.engine.Format;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import com.skillinghelper.unlocks.UnlockStep;
import com.skillinghelper.unlocks.UnlockView;
import java.awt.Color;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
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
		JPanel header = GuideUi.header("Account Unlocks", "Key unlocks in a good order, guided one by one.");
		header.add(GuideUi.toggle(hideDone, GuideUi.TEXT, "Leave out the unlocks you already have", () ->
		{
			if (!updating)
			{
				listener.onHideDone(hideDone.isSelected());
			}
		}));
		GuideUi.frame(this, header, body);
		body.add(loggedOut());
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
			body.add(loggedOut());
			finish();
			return;
		}
		body.add(GuideUi.progress("Unlocked", v.getDone(), v.getTotal()));
		body.add(Box.createVerticalStrut(8));
		UnlockStep step = v.getStep();
		if (v.isRunning() && step != null)
		{
			body.add(stepCard(step, v.getStepWiki()));
		}
		else if (v.isRunning())
		{
			body.add(GuideUi.button("Stop guide", listener::onStopGuide));
		}
		else if (v.getDone() < v.getTotal())
		{
			body.add(GuideUi.primary("▶ Start guide", listener::onStartGuide));
		}

		body.add(GuideUi.section("Unlocks"));
		body.add(GuideUi.hint("In the suggested order. Click one to be guided to it next."));
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
		String id = step.getUnlockId();
		JButton open = null;
		if (step.getAction() == UnlockStep.Action.DIARY)
		{
			open = GuideUi.small("Diary guide", () -> listener.onAction(id));
			open.setToolTipText("Open this in Achievement Diary Guides");
		}
		else if (step.getAction() == UnlockStep.Action.TRAIN)
		{
			open = GuideUi.small("Train it", () -> listener.onAction(id));
			open.setToolTipText("Open the Skilling Helper guide for this");
		}
		return GuideUi.stepCard(s.getKind() == DiaryStep.Kind.LOCKED ? GuideUi.WARN : GuideUi.HINT, "Now",
			s.getTitle(), s.getLines(), listener::onStopGuide, open,
			wiki == null ? null : GuideUi.small("Wiki", () -> listener.onOpenWiki(wiki)),
			id == null || s.getKind() == DiaryStep.Kind.DONE ? null
				: GuideUi.small("Skip", () -> listener.onSkip(id)));
	}

	private JComponent row(UnlockView.Row r)
	{
		boolean current = r.getState() == UnlockView.RowState.CURRENT;
		JPanel row = GuideUi.row(current);
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
			GuideUi.clickable(row, "Guide me to this unlock", () -> listener.onPick(r.getId()));
		}
		return GuideUi.full(row);
	}

	private static JComponent loggedOut()
	{
		return GuideUi.empty("Not logged in", "Log in to see which unlocks you have and get guided to the next one.");
	}

	private void finish()
	{
		body.revalidate();
		body.repaint();
	}
}
