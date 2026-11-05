package com.skillinghelper.planner.ui;

import com.skillinghelper.data.Wilderness;
import com.skillinghelper.engine.Format;
import com.skillinghelper.planner.PlannerView;
import com.skillinghelper.planner.api.StepKind;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * Side panel: the next step of the path to max, the steps after it (click one to see it in full), progress per
 * kind of step, and which kinds to plan.
 */
public class PlannerPanel extends PluginPanel
{
	private static final int WIDTH = GuideUi.WIDTH;
	private static final int CARD = GuideUi.CARD_WIDTH;
	private static final Map<StepKind, Color> KIND_COLORS = new EnumMap<>(StepKind.class);

	static
	{
		KIND_COLORS.put(StepKind.QUEST, new Color(110, 170, 255));
		KIND_COLORS.put(StepKind.SKILL, new Color(80, 200, 80));
		KIND_COLORS.put(StepKind.DIARY, new Color(255, 215, 90));
		KIND_COLORS.put(StepKind.BOSS, new Color(230, 80, 80));
		KIND_COLORS.put(StepKind.MINIGAME, new Color(200, 120, 255));
		KIND_COLORS.put(StepKind.UNLOCK, new Color(0, 220, 220));
		KIND_COLORS.put(StepKind.COMBAT_ACHIEVEMENT, new Color(255, 140, 60));
		KIND_COLORS.put(StepKind.CLUE, new Color(220, 180, 140));
		KIND_COLORS.put(StepKind.COLLECTION, new Color(255, 120, 200));
		KIND_COLORS.put(StepKind.MONEY, new Color(255, 200, 0));
	}

	public interface Listener
	{
		/**
		 * Open the step's guide in the plugin that has it.
		 */
		void onOpen(String stepId);

		void onSkip(String stepId);

		void onUnskipAll();

		void onShowKind(StepKind kind, boolean show);

		void onAllowWilderness(boolean allow);

		void onWiki(String page);
	}

	private final Listener listener;
	private final HiddenRedraws redraws;
	private final JCheckBox wilderness = new JCheckBox(Wilderness.TOGGLE_LABEL);
	private final JPanel body = new JPanel();

	private PlannerView view;
	/**
	 * The later step the player clicked open, or null.
	 */
	private String expanded;
	private boolean updating;

	public PlannerPanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		redraws = new HiddenRedraws(this);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = column();
		header.setBorder(new EmptyBorder(0, 0, 6, 0));
		JLabel title = new JLabel("Account Planner");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(GuideUi.left(title));
		header.add(Box.createVerticalStrut(4));
		header.add(GuideUi.text("Your path to max: what to do next, in order. Each step opens its own guide.",
			GuideUi.MUTED, WIDTH));
		header.add(Box.createVerticalStrut(6));
		wilderness.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wilderness.setForeground(Wilderness.COLOR);
		wilderness.setFont(FontManager.getRunescapeSmallFont());
		wilderness.setToolTipText("Off: steps in the Wilderness are left out. Shared with the other guides in this "
			+ "plugin pack.");
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
		body.add(GuideUi.text("Log in to see your path to max.", GuideUi.MUTED, WIDTH));
	}

	/**
	 * Shows the latest view. Swing thread.
	 */
	public void update(PlannerView view)
	{
		redraws.run("view", () -> apply(view));
	}

	private void apply(PlannerView v)
	{
		view = v;
		updating = true;
		wilderness.setSelected(v.isWildernessAllowed());
		updating = false;
		if (expanded != null && v.getRows().stream().noneMatch(r -> r.getId().equals(expanded)))
		{
			expanded = null;
		}
		rebuild();
	}

	private void rebuild()
	{
		body.removeAll();
		PlannerView v = view;
		if (v == null || !v.isLoggedIn())
		{
			body.add(GuideUi.text("Log in to see your path to max.", GuideUi.MUTED, WIDTH));
			refreshLayout();
			return;
		}
		List<PlannerView.Row> rows = v.getRows();
		if (rows.isEmpty())
		{
			body.add(GuideUi.headline("Nothing left to plan.", GuideUi.GOOD, WIDTH));
			body.add(GuideUi.text("Every step the plugins know about is done, skipped or switched off below.",
				GuideUi.MUTED, WIDTH));
		}
		else
		{
			body.add(GuideUi.stat("Rough time to max", hours(v.getTotalHours()), GuideUi.GOLD));
			body.add(GuideUi.stat("Steps in the plan", String.format("%,d", v.getPlanSize()), GuideUi.TEXT));
			body.add(Box.createVerticalStrut(6));
			body.add(card(rows.get(0), 1, "Next up"));
			if (rows.size() > 1)
			{
				body.add(GuideUi.section("Then"));
				for (int i = 1; i < rows.size(); i++)
				{
					PlannerView.Row r = rows.get(i);
					if (r.getId().equals(expanded))
					{
						body.add(Box.createVerticalStrut(3));
						body.add(card(r, i + 1, null));
						body.add(Box.createVerticalStrut(3));
					}
					else
					{
						body.add(row(r, i + 1));
					}
				}
				if (v.getPlanSize() > rows.size())
				{
					body.add(Box.createVerticalStrut(4));
					body.add(GuideUi.text("…and " + String.format("%,d", v.getPlanSize() - rows.size())
						+ " more. Show more with \"Steps shown\" in the plugin's settings.", GuideUi.MUTED, WIDTH));
				}
			}
		}

		body.add(GuideUi.section("Progress"));
		for (Map.Entry<StepKind, int[]> e : v.getProgress().entrySet())
		{
			int[] p = e.getValue();
			body.add(GuideUi.stat(plural(e.getKey()), p[0] + " / " + p[1], p[0] >= p[1] ? GuideUi.GOOD : GuideUi.TEXT));
		}

		body.add(GuideUi.section("Plan these"));
		List<StepKind> kinds = new ArrayList<>(v.getProgress().keySet());
		for (StepKind k : new StepKind[]{StepKind.QUEST, StepKind.MONEY})
		{
			if (!kinds.contains(k))
			{
				kinds.add(k);
			}
		}
		for (StepKind k : kinds)
		{
			JCheckBox box = new JCheckBox(plural(k), !v.getHiddenKinds().contains(k));
			box.setBackground(ColorScheme.DARK_GRAY_COLOR);
			box.setForeground(KIND_COLORS.getOrDefault(k, GuideUi.TEXT));
			box.setFont(FontManager.getRunescapeSmallFont());
			box.setToolTipText(k == StepKind.SKILL ? "Off: only the levels other steps need are planned"
				: "Off: left out, and counted as done for whatever needs them");
			box.addActionListener(e -> listener.onShowKind(k, box.isSelected()));
			body.add(GuideUi.left(box));
		}
		if (v.getSkipped() > 0)
		{
			body.add(Box.createVerticalStrut(4));
			body.add(GuideUi.text(v.getSkipped() + " step" + (v.getSkipped() == 1 ? "" : "s") + " skipped.",
				GuideUi.MUTED, WIDTH));
			body.add(GuideUi.button("Bring back skipped steps", listener::onUnskipAll));
		}
		if (!v.getUnreachable().isEmpty() && !v.isWildernessAllowed())
		{
			List<String> left = v.getUnreachable();
			int n = left.size();
			body.add(Box.createVerticalStrut(4));
			body.add(GuideUi.text(n + (n == 1 ? " step needs" : " steps need") + " the Wilderness, so "
				+ (n == 1 ? "it's" : "they're") + " left out until you tick the Wilderness box above: "
				+ String.join(", ", left.subList(0, Math.min(6, n))) + (n > 6 ? " and " + (n - 6) + " more." : "."),
				Wilderness.COLOR, WIDTH));
		}
		body.add(Box.createVerticalStrut(8));
		body.add(GuideUi.text("Steps come from: " + String.join(", ", v.getProviders()) + ". Times are rough: "
			+ "Skilling Helper's xp rates at your pace setting, plus a guess per diary task and boss.",
			GuideUi.MUTED, WIDTH));
		refreshLayout();
	}

	private void refreshLayout()
	{
		body.revalidate();
		body.repaint();
	}

	/**
	 * A step in full: what it gives, why it's here, how to start, and its buttons.
	 */
	private JComponent card(PlannerView.Row r, int number, String kicker)
	{
		Color color = KIND_COLORS.getOrDefault(r.getKind(), GuideUi.TEXT);
		JPanel card = GuideUi.card(color);
		List<String> lines = new ArrayList<>();
		if (r.getWhy() != null)
		{
			lines.add("Why: " + r.getWhy());
		}
		if (r.getReason() != null)
		{
			lines.add("↑ " + r.getReason());
		}
		if (r.getHours() > 0)
		{
			lines.add("ℹ About " + hours(r.getHours()));
		}
		if (r.isWilderness())
		{
			lines.add("⚠ " + Wilderness.TAG + ": " + Wilderness.WARNING);
		}
		for (int i = 0; i < r.getHowTo().size(); i++)
		{
			lines.add((i + 1) + ". " + r.getHowTo().get(i));
		}
		for (String n : r.getNotes())
		{
			lines.add("? " + n);
		}
		String head = (kicker != null ? kicker : "Step " + number) + "  ·  " + r.getKind().getLabel();
		GuideUi.fillStep(card, head, color, r.getTitle(), lines);
		card.add(Box.createVerticalStrut(6));
		JPanel buttons = new JPanel(new GridLayout(1, 0, 3, 0));
		buttons.setOpaque(false);
		if (r.isCanOpen())
		{
			JButton open = small("Open guide", () -> listener.onOpen(r.getId()));
			open.setToolTipText("Opens this step in " + r.getOpenIn());
			buttons.add(open);
		}
		JButton skip = small("Skip", () -> listener.onSkip(r.getId()));
		skip.setToolTipText("Leave it out of the plan and count it as done (bring it back below)");
		buttons.add(skip);
		if (r.getWikiPage() != null)
		{
			buttons.add(small("Wiki", () -> listener.onWiki(r.getWikiPage())));
		}
		card.add(GuideUi.full(buttons));
		if (kicker == null)
		{
			card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			card.setToolTipText("Click to fold it up again");
			card.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					expanded = null;
					rebuild();
				}
			});
		}
		return card;
	}

	/**
	 * A later step in one line: click it to see it in full.
	 */
	private JComponent row(PlannerView.Row r, int number)
	{
		Color color = KIND_COLORS.getOrDefault(r.getKind(), GuideUi.TEXT);
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 0, 2, 0));
		JLabel num = new JLabel(number + ".");
		num.setFont(FontManager.getRunescapeSmallFont());
		num.setForeground(GuideUi.MUTED);
		num.setVerticalAlignment(JLabel.TOP);
		row.add(num, BorderLayout.WEST);
		String time = r.getHours() > 0 ? "  <font color='" + GuideUi.hex(GuideUi.MUTED) + "'>" + hours(r.getHours())
			+ "</font>" : "";
		JLabel text = GuideUi.html("<font color='" + GuideUi.hex(color) + "'>"
			+ escape(r.getKind().getLabel()) + "</font>&nbsp; " + escape(r.getTitle()) + time, GuideUi.TEXT, WIDTH - 22);
		row.add(text, BorderLayout.CENTER);
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText(r.getReason() != null ? r.getReason() : "Click to see this step in full");
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				expanded = Objects.equals(expanded, r.getId()) ? null : r.getId();
				rebuild();
			}
		});
		return GuideUi.full(row);
	}

	private static JButton small(String label, Runnable action)
	{
		JButton b = new JButton(label);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setMargin(new Insets(2, 2, 2, 2));
		b.addActionListener(e -> action.run());
		return b;
	}

	private static String plural(StepKind k)
	{
		switch (k)
		{
			case QUEST:
				return "Quests";
			case SKILL:
				return "Skill goals";
			case DIARY:
				return "Diary tiers";
			case BOSS:
				return "Bosses";
			case MINIGAME:
				return "Minigames";
			case UNLOCK:
				return "Unlocks";
			case COMBAT_ACHIEVEMENT:
				return "Combat tasks";
			case CLUE:
				return "Clue scrolls";
			case COLLECTION:
				return "Collection log";
			case MONEY:
				return "Gold making";
			default:
				return "Other";
		}
	}

	/**
	 * "45m", "3h 05m", or "~1,234 h" for long stretches.
	 */
	static String hours(double h)
	{
		return h >= 100 ? String.format("~%,d h", Math.round(h)) : Format.hours(h);
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static JPanel column()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setOpaque(false);
		return p;
	}
}
