package com.skillinghelper.banked.ui;

import com.skillinghelper.banked.BankedPlanner;
import com.skillinghelper.banked.BankedView;
import com.skillinghelper.banked.data.BankedActivity;
import com.skillinghelper.engine.Format;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.ui.GuideUi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
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
import javax.swing.border.EmptyBorder;
import net.runelite.api.Skill;
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
	private static final int TEXT_WIDTH = GuideUi.WIDTH;
	private static final Color GOOD = GuideUi.GOOD;
	private static final Color WARN = GuideUi.WARN;
	private static final Color BAD = GuideUi.BAD;
	private static final Color MUTED = GuideUi.MUTED;
	private static final String SKIP_LABEL = "Don't use these";
	private static final String ALL_SKILLS = "All skills";

	public interface Listener
	{
		void onStart();

		void onStop();

		/**
		 * @param choice an activity id, or {@link BankedPlanner#SKIP}
		 */
		void onChoice(String group, String choice);

		/**
		 * @param skill the skill to train, or null for all of them
		 */
		void onFocus(Skill skill);

		void onToggleBankFilter();

		void onOpenWiki(String page);
	}

	private final Listener listener;
	private final ItemManager itemManager;
	private final JPanel guideBox = column();
	private final JPanel totalsBox = column();
	private final JPanel list = column();

	private List<BankedPlanner.Group> shownGroups;
	private final JPanel focusBox = column();
	private final JComboBox<Object> skillPicker = new JComboBox<>();
	private List<Skill> shownSkills;
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
		focusBox.add(GuideUi.subheading("Train"));
		skillPicker.setToolTipText("Pick one skill to keep the rest of your bank for other skills, e.g. logs for "
			+ "Construction instead of burning them");
		skillPicker.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected,
				boolean focus)
			{
				super.getListCellRendererComponent(l, value, index, selected, focus);
				setText(value instanceof Skill ? ((Skill) value).getName() + " only" : ALL_SKILLS);
				return this;
			}
		});
		skillPicker.addActionListener(e ->
		{
			if (!updating)
			{
				Object picked = skillPicker.getSelectedItem();
				listener.onFocus(picked instanceof Skill ? (Skill) picked : null);
			}
		});
		focusBox.add(full(skillPicker));
		focusBox.add(Box.createVerticalStrut(8));
		content.add(focusBox);
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
			showFocus(view);
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

	// ---- Skill picker ----

	private void showFocus(BankedView view)
	{
		focusBox.setVisible(view.isBankKnown() && (!view.getSkills().isEmpty() || view.getFocus() != null));
		if (!Objects.equals(shownSkills, view.getSkills()))
		{
			shownSkills = view.getSkills();
			skillPicker.removeAllItems();
			skillPicker.addItem(ALL_SKILLS);
			for (Skill skill : view.getSkills())
			{
				skillPicker.addItem(skill);
			}
		}
		if (view.getFocus() != null && !view.getSkills().contains(view.getFocus()))
		{
			skillPicker.addItem(view.getFocus());
			shownSkills = null;
		}
		skillPicker.setSelectedItem(view.getFocus() == null ? ALL_SKILLS : view.getFocus());
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
				guideBox.add(text(view.getFocus() == null
					? "Nothing in your bank can be used for xp at your levels right now."
					: "Nothing in your bank trains " + view.getFocus().getName() + " at your level right now.", MUTED));
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
		JPanel card = GuideUi.card(GOOD);
		GuideStep step = view.getStep();
		List<String> lines = step == null ? new ArrayList<>() : new ArrayList<>(step.getDetails());
		GuideUi.fillStep(card, "Now: " + a.getName(), GOOD, step == null ? a.getName() : step.getTitle(), lines);
		card.add(Box.createVerticalStrut(5));
		card.add(GuideUi.stat("Left", String.format("%,d", current.getActions()), GuideUi.BRIGHT));
		card.add(GuideUi.stat("Xp", String.format("%,d", current.getXp()), GOOD));
		card.add(GuideUi.stat("Time", "about " + Format.hours(hours(List.of(current))), GuideUi.TEXT));
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
					color = null;
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
				card.add(GuideUi.subheading("You need"));
				header = true;
			}
			card.add(color == BAD ? GuideUi.line(mark + r.getText(), BAD, GuideUi.CARD_WIDTH)
				: GuideUi.line(mark + r.getText(), color, GuideUi.TEXT, GuideUi.CARD_WIDTH));
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
		totalsBox.add(GuideUi.section("Banked xp per skill"));
		for (BankedPlanner.SkillTotal t : view.getTotals())
		{
			String levels = t.getLevelAfter() > t.getLevelNow()
				? t.getLevelNow() + " → " + t.getLevelAfter() : "lvl " + t.getLevelNow();
			totalsBox.add(GuideUi.stat(t.getSkill().getName(), String.format("+%,d xp (%s)", t.getXp(), levels),
				t.getLevelAfter() > t.getLevelNow() ? GOOD : GuideUi.TEXT));
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
		list.add(GuideUi.section("Your banked items"));
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
		JPanel row = GuideUi.card(ColorScheme.BRAND_ORANGE);
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
			row.add(Box.createVerticalStrut(3));
			row.add(GuideUi.stat(String.format("%,d xp", chosen.getXp()), "about " + Format.hours(hours(List.of(chosen))),
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

	private static JLabel text(String s, Color color)
	{
		return GuideUi.text(s, color, TEXT_WIDTH);
	}

	private static JButton button(String label, Runnable action)
	{
		return GuideUi.button(label, action);
	}

	private static <T extends JComponent> T left(T c)
	{
		return GuideUi.left(c);
	}

	private static <T extends JComponent> T full(T c)
	{
		return GuideUi.full(c);
	}
}
