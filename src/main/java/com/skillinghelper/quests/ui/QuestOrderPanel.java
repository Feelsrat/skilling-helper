package com.skillinghelper.quests.ui;

import com.skillinghelper.data.Wilderness;
import com.skillinghelper.quests.QuestOrderConfig;
import com.skillinghelper.quests.QuestPlanner;
import com.skillinghelper.quests.QuestStep;
import com.skillinghelper.quests.QuestView;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: the route picker, whether Quest Helper is there, progress, what to do now, and the next rows on the
 * route (click one to do it next).
 */
public class QuestOrderPanel extends PluginPanel
{
	private static final int WIDTH = GuideUi.WIDTH;
	private static final int CARD = GuideUi.CARD_WIDTH;
	private static final String IGNORED = "Ignored";

	public interface Listener
	{
		void onStartGuide();

		void onStopGuide();

		/**
		 * Do this route row next, whatever the order says.
		 */
		void onPick(String key);

		void onSkip(String key);

		void onMarkDone(String key);

		void onRoute(QuestOrderConfig.RouteChoice choice);

		void onAllowWilderness(boolean allow);

		void onShowDiary(String diary);

		void onOpenWiki(String page);

		void onOpenRoutePage();

		void onGetQuestHelper();

		/**
		 * Add a route row to the ignore list (shared with the Account Planner), or take it off.
		 */
		void onIgnore(String key, boolean ignore);

		void onQuestHelperAutoStart();

		void onSortQuestHelper();
	}

	private final Listener listener;
	private final HiddenRedraws redraws;
	private final JComboBox<QuestOrderConfig.RouteChoice> routes = new JComboBox<>(QuestOrderConfig.RouteChoice.values());
	private final JCheckBox wilderness = new JCheckBox(Wilderness.TOGGLE_LABEL);
	private final JPanel body = new JPanel();
	private boolean updating;
	private final Set<String> openSections = new HashSet<>();

	public QuestOrderPanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		redraws = new HiddenRedraws(this);
		JPanel header = GuideUi.header("Quest Order Guide", "/com/skillinghelper/quests/icon.png");
		routes.setToolTipText("Which of the wiki's optimal quest guides to follow");
		routes.addActionListener(e ->
		{
			if (!updating && routes.getSelectedItem() != null)
			{
				listener.onRoute((QuestOrderConfig.RouteChoice) routes.getSelectedItem());
			}
		});
		GuideUi.options(header, GuideUi.subheading("Route"), GuideUi.full(routes),
			GuideUi.wilderness(wilderness, "Off: quests and diaries in the Wilderness are skipped.", () ->
			{
				if (!updating)
				{
					listener.onAllowWilderness(wilderness.isSelected());
				}
			}));
		GuideUi.frame(this, header, body);
		body.add(GuideUi.empty("Not logged in", "Log in to see your quests in the best order."));
	}

	/**
	 * Shows the latest view. Swing thread.
	 */
	public void update(QuestView view)
	{
		redraws.run("view", () -> apply(view));
	}

	private void apply(QuestView v)
	{
		updating = true;
		wilderness.setSelected(v.isWildernessAllowed());
		updating = false;
		body.removeAll();
		if (!v.isLoggedIn())
		{
			body.add(GuideUi.empty("Not logged in", "Log in to see your quests in the best order."));
			finish();
			return;
		}
		updating = true;
		if (!v.isRouteAuto())
		{
			routes.setSelectedItem(v.getRoute() == com.skillinghelper.quests.data.Route.IRONMAN
				? QuestOrderConfig.RouteChoice.IRONMAN : QuestOrderConfig.RouteChoice.MAIN);
		}
		else
		{
			routes.setSelectedItem(QuestOrderConfig.RouteChoice.AUTO);
		}
		updating = false;

		if (!v.isQuestHelper())
		{
			body.add(GuideUi.line("⚠ Quest Helper isn't on. Without it this guide only shows where each quest "
				+ "starts.", GuideUi.WARN, GuideUi.TEXT, WIDTH));
			body.add(Box.createVerticalStrut(3));
			body.add(GuideUi.button("Get Quest Helper", listener::onGetQuestHelper));
			body.add(Box.createVerticalStrut(4));
		}

		QuestStep step = v.getStep();
		if (step != null)
		{
			Color accent = step.getKind() == QuestStep.Kind.TRAIN ? GuideUi.WARN
				: step.getKind() == QuestStep.Kind.FINISHED ? GuideUi.GOOD : GuideUi.HINT;
			String diary = step.getDiary();
			String page = step.getWikiPage();
			String key = step.getRowKey();
			JPanel card = GuideUi.stepCard(accent, v.isRunning() ? "Now" : "Next up", step.getTitle(),
				step.getLines(), v.isRunning() ? listener::onStopGuide : null,
				diary == null ? null : GuideUi.small("Diary guide", () -> listener.onShowDiary(diary)),
				page == null ? null : GuideUi.small("Wiki", () -> listener.onOpenWiki(page)),
				step.getKind() == QuestStep.Kind.ACTIVITY && key != null
					? GuideUi.small("Done", () -> listener.onMarkDone(key)) : null,
				step.isOfferAutoStart() ? GuideUi.small("Auto start", listener::onQuestHelperAutoStart) : null,
				key != null && step.getKind() != QuestStep.Kind.TRAIN
					? GuideUi.small("Skip", () -> listener.onSkip(key)) : null,
				key != null && step.getKind() != QuestStep.Kind.TRAIN
					? GuideUi.small("Ignore", () -> listener.onIgnore(key, true)) : null);
			if (!v.isRunning())
			{
				card.add(Box.createVerticalStrut(6));
				card.add(GuideUi.primary("▶ Start guide", listener::onStartGuide));
			}
			body.add(card);
		}
		else if (v.isRunning())
		{
			body.add(GuideUi.button("Stop guide", listener::onStopGuide));
		}
		else
		{
			body.add(GuideUi.primary("▶ Start guide", listener::onStartGuide));
		}

		if (v.isRunning())
		{
			body.add(GuideUi.more());
		}
		body.add(Box.createVerticalStrut(8));
		body.add(GuideUi.progress("Quests done", v.getDone(), v.getTotal()));
		body.add(GuideUi.text(v.getRoute().getLabel() + " route · " + v.getQuestPoints() + " Quest points",
			GuideUi.MUTED, WIDTH));
		body.add(Box.createVerticalStrut(4));

		if (!v.getUpcoming().isEmpty())
		{
			body.add(GuideUi.section("Up next"));
			body.add(GuideUi.hint("Click one to do it next."));
			for (QuestView.Row r : v.getUpcoming())
			{
				body.add(row(r));
				body.add(Box.createVerticalStrut(3));
			}
		}
		if (!v.getIgnored().isEmpty())
		{
			body.add(GuideUi.section(IGNORED + " (" + v.getIgnored().size() + ")"));
			body.add(GuideUi.hint("Never suggested here or in the Account Planner. Click one to bring it back."));
			for (QuestView.Ignored r : v.getIgnored())
			{
				JPanel row = GuideUi.row(false);
				row.add(GuideUi.line("• " + r.getName(), GuideUi.MUTED, CARD));
				GuideUi.clickable(row, "Stop ignoring this", () -> listener.onIgnore(r.getKey(), false));
				body.add(GuideUi.full(row));
				body.add(Box.createVerticalStrut(3));
			}
		}
		body.add(Box.createVerticalStrut(6));
		body.add(GuideUi.actions(GuideUi.small("The wiki's full route", listener::onOpenRoutePage),
			v.isQuestHelper() ? GuideUi.small("Sort Quest Helper the same", listener::onSortQuestHelper) : null));
		GuideUi.foldSections(body, t -> GuideUi.MORE.equals(t) || t.startsWith(IGNORED), openSections);
		finish();
	}

	private JComponent row(QuestView.Row r)
	{
		JPanel row = GuideUi.row(r.isCurrent());
		String marker;
		Color color;
		switch (r.getStatus())
		{
			case STARTED:
				marker = "▶";
				color = GuideUi.OPTION;
				break;
			case LOCKED:
				marker = "✘";
				color = GuideUi.BAD;
				break;
			case WILDERNESS:
			case SKIPPED:
				marker = "•";
				color = GuideUi.MUTED;
				break;
			default:
				marker = r.isCurrent() ? "▶" : "•";
				color = r.isCurrent() ? GuideUi.OPTION : GuideUi.TEXT;
		}
		// The route number goes after the name: a leading "12." would be read as a numbered step
		row.add(GuideUi.line(marker + " " + r.getName() + "  #" + r.getNumber(), color, CARD));
		if (!r.getDetail().isEmpty())
		{
			row.add(GuideUi.text(r.getDetail() + (r.getStatus() == QuestPlanner.Status.STARTED ? ", started" : ""),
				GuideUi.MUTED, CARD - 12));
		}
		for (String m : r.getMissing())
		{
			row.add(GuideUi.check(false, m, CARD));
		}
		if (r.isWilderness())
		{
			row.add(GuideUi.line(Wilderness.TAG, Wilderness.COLOR, CARD));
		}
		if (r.getStatus() == QuestPlanner.Status.SKIPPED)
		{
			row.add(GuideUi.text("Skipped for now.", GuideUi.MUTED, CARD));
		}
		GuideUi.clickable(row, "Do this next (right-click to ignore)", () -> listener.onPick(r.getKey()));
		JPopupMenu menu = new JPopupMenu();
		JMenuItem next = new JMenuItem("Do this next");
		next.addActionListener(e -> listener.onPick(r.getKey()));
		JMenuItem ignore = new JMenuItem("Ignore (never suggest)");
		ignore.addActionListener(e -> listener.onIgnore(r.getKey(), true));
		menu.add(next);
		menu.add(ignore);
		popup(row, menu);
		return GuideUi.full(row);
	}

	/**
	 * Right-click anywhere on the row (its labels too) opens the menu.
	 */
	private static void popup(JComponent c, JPopupMenu menu)
	{
		c.setComponentPopupMenu(menu);
		for (java.awt.Component k : c.getComponents())
		{
			if (k instanceof JComponent)
			{
				((JComponent) k).setInheritsPopupMenu(true);
				popup((JComponent) k, menu);
			}
		}
	}

	private void finish()
	{
		body.revalidate();
		body.repaint();
	}
}
