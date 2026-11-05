package com.skillinghelper.quests.ui;

import com.skillinghelper.data.Wilderness;
import com.skillinghelper.quests.QuestOrderConfig;
import com.skillinghelper.quests.QuestPlanner;
import com.skillinghelper.quests.QuestStep;
import com.skillinghelper.quests.QuestView;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
 * Side panel: the route picker, whether Quest Helper is there, progress, what to do now, and the next rows on the
 * route (click one to do it next).
 */
public class QuestOrderPanel extends PluginPanel
{
	private static final int WIDTH = GuideUi.WIDTH;
	private static final int CARD = GuideUi.CARD_WIDTH;

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
	}

	private final Listener listener;
	private final HiddenRedraws redraws;
	private final JComboBox<QuestOrderConfig.RouteChoice> routes = new JComboBox<>(QuestOrderConfig.RouteChoice.values());
	private final JCheckBox wilderness = new JCheckBox(Wilderness.TOGGLE_LABEL);
	private final JPanel body = new JPanel();
	private boolean updating;

	public QuestOrderPanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		redraws = new HiddenRedraws(this);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = column();
		header.setBorder(new EmptyBorder(0, 0, 6, 0));
		JLabel title = new JLabel("Quest Order Guide");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(GuideUi.left(title));
		header.add(Box.createVerticalStrut(4));
		header.add(GuideUi.text("The wiki's optimal quest order for your account. Quest Helper does each quest's "
			+ "steps.", GuideUi.MUTED, WIDTH));
		header.add(Box.createVerticalStrut(6));
		routes.setToolTipText("Which of the wiki's optimal quest guides to follow");
		routes.addActionListener(e ->
		{
			if (!updating && routes.getSelectedItem() != null)
			{
				listener.onRoute((QuestOrderConfig.RouteChoice) routes.getSelectedItem());
			}
		});
		header.add(GuideUi.full(routes));
		wilderness.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wilderness.setForeground(Wilderness.COLOR);
		wilderness.setFont(FontManager.getRunescapeSmallFont());
		wilderness.setToolTipText("Off: quests and diaries in the Wilderness are skipped. Shared with the other "
			+ "guides in this plugin pack.");
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
		body.add(GuideUi.text("Log in to see your quest order.", GuideUi.MUTED, WIDTH));
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
			body.add(GuideUi.text("Log in to see your quest order.", GuideUi.MUTED, WIDTH));
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

		body.add(GuideUi.headline(v.getRoute().getLabel() + " route", Color.WHITE, WIDTH));
		body.add(GuideUi.line(v.getDone() + " of " + v.getTotal() + " done · " + v.getQuestPoints() + " Quest points",
			v.getDone() == v.getTotal() ? GuideUi.GOOD : GuideUi.TEXT, WIDTH));
		body.add(Box.createVerticalStrut(3));
		if (v.isQuestHelper())
		{
			body.add(GuideUi.line("✔ Quest Helper found: it gives the click-by-click steps for each quest.",
				GuideUi.GOOD, GuideUi.TEXT, WIDTH));
		}
		else
		{
			body.add(GuideUi.line("⚠ Quest Helper isn't installed or is off. This guide can only show where each "
				+ "quest starts without it.", GuideUi.WARN, GuideUi.TEXT, WIDTH));
			body.add(Box.createVerticalStrut(3));
			body.add(GuideUi.button("Get Quest Helper", listener::onGetQuestHelper));
		}
		body.add(Box.createVerticalStrut(6));
		body.add(GuideUi.button(v.isRunning() ? "Stop guide" : "Start guide",
			v.isRunning() ? listener::onStopGuide : listener::onStartGuide));
		body.add(Box.createVerticalStrut(3));
		body.add(GuideUi.button("Open the wiki's route", listener::onOpenRoutePage));

		QuestStep step = v.getStep();
		if (step != null)
		{
			body.add(Box.createVerticalStrut(8));
			Color accent = step.getKind() == QuestStep.Kind.TRAIN ? GuideUi.WARN
				: step.getKind() == QuestStep.Kind.FINISHED ? GuideUi.GOOD : GuideUi.HINT;
			JPanel card = GuideUi.card(accent);
			String kicker = v.isRunning() ? "Now" : "Next (press Start guide to be shown the way)";
			GuideUi.fillStep(card, kicker, GuideUi.HINT, step.getTitle(), step.getLines());
			if (step.getDiary() != null)
			{
				String d = step.getDiary();
				card.add(Box.createVerticalStrut(6));
				card.add(GuideUi.button("Show in Achievement Diary Guides", () -> listener.onShowDiary(d)));
			}
			if (step.getWikiPage() != null)
			{
				String page = step.getWikiPage();
				card.add(Box.createVerticalStrut(4));
				card.add(GuideUi.button("Open wiki guide", () -> listener.onOpenWiki(page)));
			}
			if (step.getKind() == QuestStep.Kind.ACTIVITY && step.getRowKey() != null)
			{
				String key = step.getRowKey();
				card.add(Box.createVerticalStrut(4));
				card.add(GuideUi.button("Mark as done", () -> listener.onMarkDone(key)));
			}
			if (step.getRowKey() != null && step.getKind() != QuestStep.Kind.TRAIN)
			{
				String key = step.getRowKey();
				card.add(Box.createVerticalStrut(4));
				card.add(GuideUi.button("Skip for now", () -> listener.onSkip(key)));
			}
			body.add(GuideUi.full(card));
		}

		if (!v.getUpcoming().isEmpty())
		{
			body.add(GuideUi.section("Up next on the route"));
			body.add(GuideUi.text("Click one to do it next.", GuideUi.MUTED, WIDTH));
			body.add(Box.createVerticalStrut(4));
			for (QuestView.Row r : v.getUpcoming())
			{
				body.add(row(r));
				body.add(Box.createVerticalStrut(3));
			}
		}
		finish();
	}

	private JComponent row(QuestView.Row r)
	{
		JPanel row = column();
		row.setOpaque(true);
		row.setBackground(r.isCurrent() ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
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
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText("Do this next");
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				listener.onPick(r.getKey());
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
