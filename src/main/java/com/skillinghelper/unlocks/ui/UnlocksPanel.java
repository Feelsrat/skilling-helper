package com.skillinghelper.unlocks.ui;

import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import com.skillinghelper.unlocks.UnlockStep;
import com.skillinghelper.unlocks.UnlockView;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
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
	private final Set<String> openSections = new HashSet<>();

	public UnlocksPanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		redraws = new HiddenRedraws(this);
		JPanel header = GuideUi.header("Account Unlocks", "/com/skillinghelper/unlocks/icon.png");
		GuideUi.options(header, GuideUi.toggle(hideDone, GuideUi.TEXT, "Leave out the unlocks you already have",
			() ->
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
		UnlockStep step = v.getStep();
		if (v.isRunning() && step != null)
		{
			// Guiding: just the step, everything else folded away under it
			body.add(stepCard(step, v.getStepWiki()));
			body.add(GuideUi.more());
		}
		body.add(GuideUi.progress("Unlocked", v.getDone(), v.getTotal()));
		body.add(Box.createVerticalStrut(8));
		if (v.isRunning() && step == null)
		{
			body.add(GuideUi.button("Stop guide", listener::onStopGuide));
		}
		else if (!v.isRunning() && v.getDone() < v.getTotal())
		{
			body.add(GuideUi.primary("▶ Start guide", listener::onStartGuide));
			body.add(GuideUi.hint("Guides you to the next unlock, one at a time."));
		}

		body.add(GuideUi.section("All unlocks"));
		body.add(GuideUi.hint("Click one to go for it next."));
		for (UnlockView.Row r : v.getRows())
		{
			body.add(row(r));
			body.add(Box.createVerticalStrut(3));
		}
		GuideUi.foldSections(body, GuideUi.MORE::equals, openSections);
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
		if (!r.getGives().isEmpty())
		{
			row.add(GuideUi.text(r.getGives().get(0), GuideUi.MUTED, CARD - 12));
		}
		if (r.getState() != UnlockView.RowState.DONE)
		{
			for (String m : r.getMissing())
			{
				row.add(GuideUi.check(false, m, CARD));
			}
			if (r.getState() == UnlockView.RowState.UNKNOWN)
			{
				row.add(GuideUi.text("Open your bank once so the plugin can see if you have it.", GuideUi.WARN,
					CARD - 12));
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
