package com.skillinghelper.pvm.ui;

import com.skillinghelper.pvm.PlayerView;
import com.skillinghelper.pvm.Readiness;
import com.skillinghelper.pvm.data.ItemNeed;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.QuestReq;
import com.skillinghelper.pvm.data.StatReq;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: a dropdown of all combat content sorted by the level it needs, and a checklist for the one picked.
 */
public class PvmProgressionPanel extends PluginPanel
{
	private static final int TEXT_WIDTH = 165;
	private static final Color GOOD = new Color(80, 200, 80);
	private static final Color WARN = new Color(255, 170, 0);
	private static final Color BAD = new Color(230, 80, 80);
	private static final Color HINT = new Color(110, 170, 255);
	private static final Color MUTED = new Color(150, 150, 150);

	public interface Listener
	{
		void onTrain(Skill skill, int level);

		void onShowPath(PvmContent content);

		void onClearPath();

		void onOpenWiki(String page);

		void onToggleDone(PvmContent content);
	}

	private final Listener listener;
	private final JPanel content = new JPanel();
	private final JComboBox<Readiness> picker = new JComboBox<>();
	/**
	 * The content the player picked in the dropdown, or null for the lowest-level one not done yet.
	 */
	private String pickedId;
	private boolean updating;
	private List<Readiness> ladder;
	private PlayerView view;
	private String pathId;

	public PvmProgressionPanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("PvM Progression");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(new EmptyBorder(0, 0, 8, 0));
		header.add(title);
		header.add(Box.createVerticalStrut(6));
		header.add(text("Sorted by the level each one needs:", MUTED));
		header.add(Box.createVerticalStrut(2));
		picker.setRenderer(new PickerRenderer());
		picker.setMaximumRowCount(20);
		picker.addActionListener(e ->
		{
			Readiness r = (Readiness) picker.getSelectedItem();
			if (!updating && r != null)
			{
				Readiness first = Readiness.next(ladder);
				pickedId = first != null && first.getContent().getId().equals(r.getContent().getId())
					? null : r.getContent().getId();
				showDetail();
			}
		});
		header.add(full(picker));
		add(header, BorderLayout.NORTH);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(content, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(wrapper);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);

		content.add(text("Log in to see which boss to work towards next.", MUTED));
	}

	/**
	 * Shows the latest state. Runs on the Swing thread.
	 *
	 * @param pathId id of the content Shortest Path is showing the way to, or null
	 */
	public void update(List<Readiness> ladder, PlayerView view, String pathId)
	{
		this.ladder = ladder;
		this.view = view;
		this.pathId = pathId;
		rebuild();
	}

	private void rebuild()
	{
		updating = true;
		picker.removeAllItems();
		for (Readiness r : ladder)
		{
			picker.addItem(r);
		}
		Readiness shown = pickedId == null ? null : find(pickedId);
		if (shown == null)
		{
			pickedId = null;
			shown = Readiness.next(ladder);
		}
		picker.setSelectedItem(shown);
		updating = false;
		showDetail();
	}

	private void showDetail()
	{
		content.removeAll();
		Readiness shown = (Readiness) picker.getSelectedItem();
		if (Readiness.next(ladder) == null)
		{
			content.add(text("You've done everything on the list. Nice work!", GOOD));
			content.add(Box.createVerticalStrut(8));
		}
		if (shown != null)
		{
			detail(shown);
		}
		content.revalidate();
		content.repaint();
	}

	private Readiness find(String id)
	{
		return ladder.stream().filter(r -> r.getContent().getId().equals(id)).findFirst().orElse(null);
	}

	// ---- The checklist for one boss ----

	private void detail(Readiness r)
	{
		PvmContent c = r.getContent();
		JLabel name = new JLabel(c.getName());
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(Color.WHITE);
		content.add(left(name));
		content.add(text(r.getStatus().getLabel() + " · needs about level " + c.getLevel() + " · "
			+ c.getTier().getLabel(), statusColor(r.getStatus())));
		content.add(Box.createVerticalStrut(4));
		content.add(text(c.getWhy(), Color.LIGHT_GRAY));

		int step = 1;
		if (!c.getQuests().isEmpty())
		{
			content.add(section(step++ + ". Quests"));
			for (QuestReq q : c.getQuests())
			{
				boolean done = !r.getMissingQuests().contains(q);
				String what = q.getQuest().getName() + (q.isStartedIsEnough() ? " (started is enough)" : "");
				content.add(text((done ? "✔ " : "✘ ") + what, done ? GOOD : BAD));
			}
		}

		if (!c.getStats().isEmpty())
		{
			content.add(section(step++ + ". Levels"));
			for (StatReq s : c.getStats())
			{
				content.add(levelRow(s));
			}
		}

		if (!c.getItems().isEmpty())
		{
			content.add(section(step++ + ". Bring"));
			for (ItemNeed i : c.getItems())
			{
				content.add(itemLine(i));
			}
		}

		content.add(section(step++ + ". Get there"));
		content.add(text(c.getTravel(), Color.LIGHT_GRAY));
		if (c.getLocation() != null)
		{
			content.add(Box.createVerticalStrut(4));
			if (c.getId().equals(pathId))
			{
				content.add(button("Clear path", listener::onClearPath));
			}
			else
			{
				content.add(button("Show path", () -> listener.onShowPath(c)));
			}
		}

		content.add(section(step++ + ". Learn the fight"));
		content.add(text("Read the strategy guide before your first try.", Color.LIGHT_GRAY));
		content.add(Box.createVerticalStrut(4));
		content.add(button("Open wiki strategy guide", () -> listener.onOpenWiki(c.getWikiPage())));

		content.add(section(step + ". Get the kill"));
		if (r.getKillCount() > 0)
		{
			content.add(text("✔ Kill count: " + r.getKillCount(), GOOD));
		}
		else
		{
			content.add(text("Your first kill marks this done and moves the list on to the next one.", Color.LIGHT_GRAY));
		}
		boolean marked = view.getMarkedDone().contains(c.getId());
		if (r.getKillCount() == 0 || marked)
		{
			content.add(Box.createVerticalStrut(4));
			content.add(button(marked ? "Mark as not done" : "Mark as done", () -> listener.onToggleDone(c)));
		}
	}

	private JComponent levelRow(StatReq s)
	{
		int have = view.level(s.getSkill());
		boolean met = have >= s.getLevel();
		String label = (met ? "✔ " : "✘ ") + s.getSkill().getName() + " " + have + "/" + s.getLevel()
			+ (s.isRequired() ? " (required)" : "");
		JLabel line = text(label, met ? GOOD : (s.isRequired() || s.getLevel() - have > 5 ? BAD : WARN));
		if (met)
		{
			return line;
		}
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.add(text(label, line.getForeground(), TEXT_WIDTH - 55), BorderLayout.CENTER);
		JButton train = new JButton("Train");
		train.setToolTipText("Open the Skilling Helper guide for " + s.getSkill().getName() + " to level "
			+ s.getLevel());
		train.addActionListener(e -> listener.onTrain(s.getSkill(), s.getLevel()));
		row.add(train, BorderLayout.EAST);
		return full(row);
	}

	private JLabel itemLine(ItemNeed i)
	{
		if (!i.isCheckable())
		{
			return text("• " + i.getName(), Color.LIGHT_GRAY);
		}
		boolean owned = i.getIds().stream().anyMatch(view.getOwned()::contains);
		if (owned)
		{
			return text("✔ " + i.getName(), GOOD);
		}
		return text("✘ " + i.getName() + (i.isRequired() ? "" : " (optional)"), i.isRequired() ? BAD : WARN);
	}

	/**
	 * Dropdown rows: "70  Barrows (ready)", coloured by status.
	 */
	private static class PickerRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
			boolean focused)
		{
			super.getListCellRendererComponent(list, value, index, selected, focused);
			if (value instanceof Readiness)
			{
				Readiness r = (Readiness) value;
				String mark = r.getStatus() == Readiness.Status.DONE ? "✔ " : "";
				setText(mark + r.getContent().getLevel() + "  " + r.getContent().getName() + " ("
					+ r.getStatus().getLabel().toLowerCase() + ")");
				if (!selected)
				{
					setForeground(statusColor(r.getStatus()));
				}
			}
			return this;
		}
	}

	// ---- Helpers ----

	private static Color statusColor(Readiness.Status status)
	{
		switch (status)
		{
			case DONE:
			case READY:
				return GOOD;
			case ALMOST:
				return WARN;
			case TRAIN:
				return HINT;
			default:
				return BAD;
		}
	}

	private static JLabel section(String s)
	{
		JLabel l = new JLabel(s);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(Color.WHITE);
		l.setBorder(new EmptyBorder(8, 0, 2, 0));
		return left(l);
	}

	private static JLabel text(String s, Color color)
	{
		return text(s, color, TEXT_WIDTH);
	}

	private static JLabel text(String s, Color color, int width)
	{
		JLabel l = new JLabel("<html><body style='width:" + width + "px'>" + escape(s) + "</body></html>");
		l.setForeground(color);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static JButton button(String label, Runnable action)
	{
		JButton b = new JButton(label);
		b.addActionListener(e -> action.run());
		return full(b);
	}

	private static <T extends JComponent> T left(T c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		return c;
	}

	private static <T extends JComponent> T full(T c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
		return c;
	}
}
