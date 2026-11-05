package com.skillinghelper.banked.ui;

import com.skillinghelper.banked.BankedPlanner;
import com.skillinghelper.banked.BankedView;
import com.skillinghelper.banked.data.BankedActivity;
import com.skillinghelper.banked.profit.BankProfit;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.engine.Format;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.border.EmptyBorder;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
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

	/**
	 * Holds back redraws while the panel is closed.
	 */
	private final HiddenRedraws redraws;

	public BankedXpPanel(Listener listener, ItemManager itemManager)
	{
		super(false);
		redraws = new HiddenRedraws(this);
		this.listener = listener;
		this.itemManager = itemManager;
		JPanel header = GuideUi.header("Banked XP Guide", "Turn what's in your bank into xp, step by step.");
		header.setBorder(new EmptyBorder(0, 0, 0, 0));

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
		GuideUi.frame(this, header, content);

		update(BankedView.LOGGED_OUT);
	}

	/**
	 * Shows a new view. Swing thread.
	 */
	public void update(BankedView view)
	{
		redraws.run("update", () -> applyUpdate(view));
	}

	private void applyUpdate(BankedView view)
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
			guideBox.add(GuideUi.empty("Not logged in", "Log in to see the xp in your bank."));
			return;
		}
		if (!view.isBankKnown())
		{
			guideBox.add(GuideUi.empty("Open your bank", "Open your bank once so the guide can see what's in it."));
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
			guideBox.add(GuideUi.primary("▶ Start guide", listener::onStart));
			return;
		}

		BankedPlanner.Option current = view.getCurrent();
		BankedActivity a = current.getActivity();
		if (view.getProfitRun() != null)
		{
			showProfitRun(view, current, a);
			return;
		}
		GuideStep step = view.getStep();
		List<String> lines = step == null ? new ArrayList<>() : new ArrayList<>(step.getDetails());
		if (step != null && step.getRoute() != null && step.getRoute().entersWilderness())
		{
			lines.add(Wilderness.TAG + ": " + Wilderness.WARNING);
		}
		JButton skip = GuideUi.small("Skip item", () -> listener.onChoice(a.getGroup(), BankedPlanner.SKIP));
		skip.setToolTipText("Don't use this item: the guide moves on to the next one");
		JPanel card = GuideUi.stepCard(GOOD, "Now: " + a.getName(), step == null ? a.getName() : step.getTitle(),
			lines, listener::onStop);
		card.add(Box.createVerticalStrut(5));
		card.add(GuideUi.stat("Left", String.format("%,d", current.getActions()), GuideUi.BRIGHT));
		if (current.getLimitedBy() != null)
		{
			card.add(GuideUi.line("Stops when your " + current.getLimitedBy().toLowerCase() + " run out", WARN,
				GuideUi.CARD_WIDTH));
		}
		card.add(GuideUi.stat("Xp", String.format("%,d", current.getXp()), GOOD));
		card.add(GuideUi.stat("Time", "about " + Format.hours(hours(List.of(current))), GuideUi.TEXT));
		addRequirements(card, view.getRequirements());
		card.add(Box.createVerticalStrut(6));
		card.add(GuideUi.actions(skip, bankFilter(), GuideUi.small("Wiki",
			() -> listener.onOpenWiki(a.getMethod().getWikiPage()))));
		guideBox.add(card);
		if (view.getQueue().size() > 1)
		{
			guideBox.add(Box.createVerticalStrut(4));
			BankedPlanner.Option next = view.getQueue().get(1);
			guideBox.add(text("Then: " + next.getActivity().getName() + " (" + (view.getQueue().size() - 1)
				+ " more after this)", MUTED));
		}
	}

	/**
	 * A profit run from Money Making: one item, then selling what it made.
	 */
	private void showProfitRun(BankedView view, BankedPlanner.Option current, BankedActivity a)
	{
		GuideStep step = view.getStep();
		List<String> lines = step == null ? new ArrayList<>() : new ArrayList<>(step.getDetails());
		if (step != null && step.getRoute() != null && step.getRoute().entersWilderness())
		{
			lines.add(Wilderness.TAG + ": " + Wilderness.WARNING);
		}
		boolean selling = current.getActions() <= 0;
		String heading = selling ? "Now: sell your " + view.getProfitRun().toLowerCase()
			: "Profit run: " + a.getName();
		JPanel card = GuideUi.stepCard(GOOD, heading, step == null ? a.getName() : step.getTitle(), lines,
			listener::onStop);
		card.add(Box.createVerticalStrut(5));
		if (!selling)
		{
			card.add(GuideUi.stat("Left", String.format("%,d", current.getActions()), GuideUi.BRIGHT));
			card.add(GuideUi.stat("Time", "about " + Format.hours(current.getActions()
				/ BankProfit.actionsPerHour(a.getMethod())), GuideUi.TEXT));
			card.add(GuideUi.line("Then: sell the " + view.getProfitRun().toLowerCase() + " at the Grand Exchange ("
				+ Format.gp(view.getProfitEach()) + " each after tax)", MUTED, GuideUi.CARD_WIDTH));
			addRequirements(card, view.getRequirements());
		}
		card.add(Box.createVerticalStrut(6));
		card.add(GuideUi.actions(bankFilter()));
		guideBox.add(card);
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
		if (itemManager != null)
		{
			itemManager.getImage(g.getIconId()).addTo(name);
		}
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
			if (chosen.getLimitedBy() != null)
			{
				row.add(text(String.format("Only %,d: that's all your %s cover", chosen.getActions(),
					chosen.getLimitedBy().toLowerCase()), WARN));
			}
		}
		else if (!g.isSkipped())
		{
			row.add(text(whyNotUsed(g), WARN));
		}
		return row;
	}

	private static String whyNotUsed(BankedPlanner.Group g)
	{
		for (BankedPlanner.Option o : g.getOptions())
		{
			if (o.getLocked() == null && o.getLimitedBy() != null)
			{
				return "You have none of the " + o.getLimitedBy().toLowerCase() + " this needs (or turn on \"Buy "
					+ "missing secondaries\" in the settings)";
			}
		}
		BankedPlanner.Option first = g.getOptions().get(0);
		return first.getLocked() != null ? "Can't use yet: " + first.getLocked().toLowerCase()
			: "Not enough for one action";
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

	private JButton bankFilter()
	{
		JButton b = GuideUi.small("Bank filter", listener::onToggleBankFilter);
		b.setToolTipText("Show only this guide's items in the bank (or click the Banked XP button there)");
		return b;
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
