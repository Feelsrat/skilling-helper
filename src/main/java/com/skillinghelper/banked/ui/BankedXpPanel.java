package com.skillinghelper.banked.ui;

import com.skillinghelper.banked.BankedPlanner;
import com.skillinghelper.banked.BankedView;
import com.skillinghelper.banked.data.BankedActivity;
import com.skillinghelper.engine.Format;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.Requirement;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;
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
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: the xp in the bank per skill, each banked item with a choice of how to use it, and the running guide's
 * current step.
 */
public class BankedXpPanel extends PluginPanel
{
	private static final int TEXT_WIDTH = 165;
	private static final Color GOOD = new Color(80, 200, 80);
	private static final Color WARN = new Color(255, 170, 0);
	private static final Color BAD = new Color(230, 80, 80);
	private static final Color MUTED = new Color(150, 150, 150);
	private static final String SKIP_LABEL = "Don't use these";

	public interface Listener
	{
		void onStart();

		void onStop();

		/**
		 * @param choice an activity id, or {@link BankedPlanner#SKIP}
		 */
		void onChoice(String group, String choice);

		void onToggleBankFilter();

		void onOpenWiki(String page);
	}

	private final Listener listener;
	private final ItemManager itemManager;
	private final JPanel guideBox = column();
	private final JPanel totalsBox = column();
	private final JPanel list = column();

	private List<BankedPlanner.Group> shownGroups;
	private boolean updating;

	public BankedXpPanel(Listener listener, ItemManager itemManager)
	{
		super(false);
		this.listener = listener;
		this.itemManager = itemManager;
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = column();
		header.setBorder(new EmptyBorder(0, 0, 6, 0));
		JLabel title = new JLabel("Banked XP Guide");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(left(title));
		header.add(Box.createVerticalStrut(4));
		header.add(text("The xp in your bank, and a guide that uses it up: what to take out, where to go and what "
			+ "to click.", MUTED));
		add(header, BorderLayout.NORTH);

		JPanel content = column();
		content.add(guideBox);
		content.add(Box.createVerticalStrut(8));
		content.add(totalsBox);
		content.add(Box.createVerticalStrut(8));
		content.add(list);
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(content, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(wrapper);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);

		update(BankedView.LOGGED_OUT);
	}

	/**
	 * Shows a new view. Swing thread.
	 */
	public void update(BankedView view)
	{
		updating = true;
		try
		{
			showGuide(view);
			showTotals(view);
			if (!Objects.equals(shownGroups, view.getGroups()))
			{
				shownGroups = view.getGroups();
				showGroups(view.getGroups());
			}
		}
		finally
		{
			updating = false;
		}
		revalidate();
		repaint();
	}

	// ---- The guide ----

	private void showGuide(BankedView view)
	{
		guideBox.removeAll();
		if (!view.isLoggedIn())
		{
			guideBox.add(text("Log in to see the xp in your bank.", MUTED));
			return;
		}
		if (!view.isBankKnown())
		{
			guideBox.add(text("Open your bank once so I can see what's in it.", WARN));
			return;
		}
		if (!view.isRunning() || view.getCurrent() == null)
		{
			List<BankedPlanner.Option> queue = view.getQueue();
			if (queue.isEmpty())
			{
				guideBox.add(text("Nothing in your bank can be used for xp at your levels right now.", MUTED));
				return;
			}
			long xp = queue.stream().mapToLong(BankedPlanner.Option::getXp).sum();
			guideBox.add(text(String.format("%d item%s to use up for %,d xp (about %s).", queue.size(),
				queue.size() == 1 ? "" : "s", xp, Format.hours(hours(queue))), Color.WHITE));
			guideBox.add(Box.createVerticalStrut(6));
			guideBox.add(button("Start guide", listener::onStart));
			return;
		}

		BankedPlanner.Option current = view.getCurrent();
		BankedActivity a = current.getActivity();
		JPanel card = card();
		card.add(text("Now: " + a.getName(), GOOD));
		card.add(text(String.format("%,d left · %,d xp · about %s", current.getActions(), current.getXp(),
			Format.hours(hours(List.of(current)))), MUTED));
		GuideStep step = view.getStep();
		if (step != null)
		{
			card.add(Box.createVerticalStrut(6));
			JLabel stepTitle = text(step.getTitle(), Color.WHITE);
			stepTitle.setFont(FontManager.getRunescapeBoldFont());
			card.add(stepTitle);
			for (String line : step.getDetails())
			{
				card.add(text(line, Color.LIGHT_GRAY));
			}
		}
		addRequirements(card, view.getRequirements());
		guideBox.add(card);
		guideBox.add(Box.createVerticalStrut(6));

		JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
		buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttons.add(new JButton("Stop")
		{
			{
				addActionListener(e -> listener.onStop());
			}
		});
		buttons.add(new JButton("Skip item")
		{
			{
				setToolTipText("Don't use this item: the guide moves on to the next one");
				addActionListener(e -> listener.onChoice(a.getGroup(), BankedPlanner.SKIP));
			}
		});
		guideBox.add(full(buttons));
		guideBox.add(Box.createVerticalStrut(4));
		JPanel more = new JPanel(new GridLayout(1, 2, 4, 0));
		more.setBackground(ColorScheme.DARK_GRAY_COLOR);
		more.add(new JButton("Bank filter")
		{
			{
				setToolTipText("Show only this guide's items in the bank (or click the Banked XP button there)");
				addActionListener(e -> listener.onToggleBankFilter());
			}
		});
		more.add(new JButton("Wiki")
		{
			{
				addActionListener(e -> listener.onOpenWiki(a.getMethod().getWikiPage()));
			}
		});
		guideBox.add(full(more));
		if (view.getQueue().size() > 1)
		{
			guideBox.add(Box.createVerticalStrut(4));
			BankedPlanner.Option next = view.getQueue().get(1);
			guideBox.add(text("Then: " + next.getActivity().getName() + " (" + (view.getQueue().size() - 1)
				+ " more after this)", MUTED));
		}
	}

	private static void addRequirements(JPanel card, List<Requirement> reqs)
	{
		boolean header = false;
		for (Requirement r : reqs)
		{
			Color color;
			String mark;
			switch (r.getStatus())
			{
				case READY:
					color = GOOD;
					mark = "✔ ";
					break;
				case IN_BANK:
					color = Color.LIGHT_GRAY;
					mark = "• ";
					break;
				case MISSING:
					color = BAD;
					mark = "✘ ";
					break;
				default:
					continue;
			}
			if (!header)
			{
				card.add(Box.createVerticalStrut(6));
				card.add(text("You need:", Color.WHITE));
				header = true;
			}
			card.add(text(mark + r.getText(), color));
		}
	}

	// ---- Totals ----

	private void showTotals(BankedView view)
	{
		totalsBox.removeAll();
		if (view.getTotals().isEmpty())
		{
			return;
		}
		totalsBox.add(heading("Banked xp per skill"));
		for (BankedPlanner.SkillTotal t : view.getTotals())
		{
			String levels = t.getLevelAfter() > t.getLevelNow()
				? t.getLevelNow() + " → " + t.getLevelAfter() : "level " + t.getLevelNow();
			totalsBox.add(text(String.format("%s: +%,d xp (%s)", t.getSkill().getName(), t.getXp(), levels),
				Color.LIGHT_GRAY));
		}
	}

	// ---- Banked items ----

	private void showGroups(List<BankedPlanner.Group> groups)
	{
		list.removeAll();
		if (groups.isEmpty())
		{
			return;
		}
		list.add(heading("Your banked items"));
		list.add(text("Pick how to use each one. The guide does them top to bottom in the order that makes sense "
			+ "(clean herbs before potions, smelt before smithing).", MUTED));
		list.add(Box.createVerticalStrut(4));
		for (BankedPlanner.Group g : groups)
		{
			list.add(groupRow(g));
			list.add(Box.createVerticalStrut(4));
		}
	}

	private JComponent groupRow(BankedPlanner.Group g)
	{
		JPanel row = card();
		JLabel name = text(String.format("%s × %,d", g.getItemName(), g.getCount()), Color.WHITE);
		itemManager.getImage(g.getIconId()).addTo(name);
		row.add(name);

		JComboBox<Object> combo = new JComboBox<>();
		for (BankedPlanner.Option o : g.getOptions())
		{
			combo.addItem(o);
		}
		combo.addItem(SKIP_LABEL);
		combo.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected,
				boolean focus)
			{
				super.getListCellRendererComponent(l, value, index, selected, focus);
				if (value instanceof BankedPlanner.Option)
				{
					BankedPlanner.Option o = (BankedPlanner.Option) value;
					setText(o.getLocked() != null ? o.getActivity().getName() + " (" + o.getLocked() + ")"
						: String.format("%s · %,d xp", o.getActivity().getName(), o.getXp()));
					if (o.getLocked() != null && !selected)
					{
						setForeground(MUTED);
					}
				}
				setToolTipText(getText());
				return this;
			}
		});
		if (g.isSkipped())
		{
			combo.setSelectedItem(SKIP_LABEL);
		}
		else if (g.getChosen() != null)
		{
			combo.setSelectedItem(g.getChosen());
		}
		combo.addActionListener(e ->
		{
			if (updating)
			{
				return;
			}
			Object picked = combo.getSelectedItem();
			if (picked instanceof BankedPlanner.Option)
			{
				listener.onChoice(g.getKey(), ((BankedPlanner.Option) picked).getActivity().getId());
			}
			else
			{
				listener.onChoice(g.getKey(), BankedPlanner.SKIP);
			}
		});
		row.add(Box.createVerticalStrut(3));
		row.add(full(combo));

		BankedPlanner.Option chosen = g.getChosen();
		if (chosen != null)
		{
			row.add(text(String.format("%,d xp · about %s", chosen.getXp(), Format.hours(hours(List.of(chosen)))),
				GOOD));
		}
		else if (!g.isSkipped())
		{
			BankedPlanner.Option first = g.getOptions().get(0);
			row.add(text(first.getLocked() != null ? "Can't use yet: " + first.getLocked().toLowerCase()
				: "Not enough for one action", WARN));
		}
		return row;
	}

	// ---- Helpers ----

	/**
	 * Hours of play at the activities' rough rates.
	 */
	private static double hours(List<BankedPlanner.Option> options)
	{
		double hours = 0;
		for (BankedPlanner.Option o : options)
		{
			double xpPerHour = o.getActivity().getMethod().getXpPerHour();
			if (xpPerHour > 0)
			{
				hours += o.getXp() / xpPerHour;
			}
		}
		return hours;
	}

	private static JPanel column()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(ColorScheme.DARK_GRAY_COLOR);
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		return p;
	}

	private static JPanel card()
	{
		JPanel p = column();
		p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		p.setBorder(new CompoundBorder(new MatteBorder(0, 2, 0, 0, ColorScheme.BRAND_ORANGE),
			new EmptyBorder(6, 6, 6, 6)));
		return p;
	}

	private static JLabel heading(String s)
	{
		JLabel l = new JLabel(s);
		l.setFont(FontManager.getRunescapeBoldFont());
		l.setForeground(Color.WHITE);
		l.setBorder(new EmptyBorder(4, 0, 2, 0));
		return left(l);
	}

	private static JLabel text(String s, Color color)
	{
		JLabel l = new JLabel("<html><body style='width:" + TEXT_WIDTH + "px'>" + escape(s) + "</body></html>");
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
