package com.skillinghelper.pvm.ui;

import com.skillinghelper.pvm.PlayerView;
import com.skillinghelper.pvm.Readiness;
import com.skillinghelper.pvm.data.ItemNeed;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.QuestReq;
import com.skillinghelper.pvm.data.StatReq;
import com.skillinghelper.pvm.guide.Fight;
import com.skillinghelper.pvm.guide.LoadoutPlan;
import com.skillinghelper.pvm.guide.Profit;
import com.skillinghelper.pvm.guide.PvmGuide;
import com.skillinghelper.pvm.guide.PvmGuides;
import com.skillinghelper.pvm.guide.PvmStep;
import com.skillinghelper.pvm.guide.Supply;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import lombok.Value;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: a dropdown of all combat content sorted by the level it needs, and a checklist for the one picked with
 * its exact loadout. A running guide shows its current step at the top of the checklist.
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

		void onStartGuide(PvmContent content);

		void onStopGuide();
	}

	/**
	 * The running guide (if any) and every guide's loadout for the player.
	 */
	@Value
	public static class GuideView
	{
		public static final GuideView NONE = new GuideView(null, null, Collections.emptyMap(), false);

		/**
		 * Content id of the running guide, or null.
		 */
		String activeId;
		PvmStep step;
		Map<String, LoadoutPlan> plans;
		boolean bankKnown;
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
	private GuideView guides = GuideView.NONE;
	/**
	 * Holds the running guide's current step, so it can change every tick without rebuilding the panel.
	 */
	private final JPanel stepBox = new JPanel();

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

		stepBox.setLayout(new BoxLayout(stepBox, BoxLayout.Y_AXIS));
		stepBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		stepBox.setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, HINT), new EmptyBorder(6, 6, 6, 6)));
		stepBox.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

	/**
	 * Shows the latest state. Runs on the Swing thread.
	 *
	 * @param pathId id of the content Shortest Path is showing the way to, or null
	 */
	public void update(List<Readiness> ladder, PlayerView view, String pathId, GuideView guides)
	{
		this.ladder = ladder;
		this.view = view;
		this.pathId = pathId;
		this.guides = guides;
		if (guides.getActiveId() != null)
		{
			// Keep showing the guide that's running
			Readiness first = Readiness.next(ladder);
			pickedId = first != null && first.getContent().getId().equals(guides.getActiveId())
				? null : guides.getActiveId();
		}
		rebuild();
	}

	/**
	 * Shows the running guide's new step. Runs on the Swing thread.
	 */
	public void updateStep(PvmStep step)
	{
		guides = new GuideView(guides.getActiveId(), step, guides.getPlans(), guides.isBankKnown());
		fillStep(step);
	}

	private void fillStep(PvmStep step)
	{
		stepBox.removeAll();
		if (step != null)
		{
			JLabel title = new JLabel("<html><body style='width:" + (TEXT_WIDTH - 15) + "px'>" + escape(step.getTitle())
				+ "</body></html>");
			title.setFont(FontManager.getRunescapeBoldFont());
			title.setForeground(Color.WHITE);
			stepBox.add(left(title));
			for (String line : step.getLines())
			{
				stepBox.add(text(line, Color.LIGHT_GRAY, TEXT_WIDTH - 15));
			}
		}
		stepBox.revalidate();
		stepBox.repaint();
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

		PvmGuide guide = PvmGuides.forContent(c.getId());
		LoadoutPlan plan = guides.getPlans().get(c.getId());
		if (guide != null)
		{
			money(guide, plan);
			content.add(Box.createVerticalStrut(6));
			if (c.getId().equals(guides.getActiveId()))
			{
				fillStep(guides.getStep());
				content.add(stepBox);
				content.add(Box.createVerticalStrut(4));
				content.add(button("Stop guide", listener::onStopGuide));
			}
			else
			{
				content.add(button("Start guide", () -> listener.onStartGuide(c)));
			}
		}

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

		if (plan != null)
		{
			content.add(section(step++ + ". Loadout"));
			loadout(plan);
		}
		else if (!c.getItems().isEmpty())
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
		Fight fight = guide == null ? null : guide.getFight();
		if (fight != null)
		{
			for (String tip : fight.getTips())
			{
				content.add(text("• " + tip, Color.LIGHT_GRAY));
			}
			content.add(Box.createVerticalStrut(2));
		}
		content.add(text("Read the strategy guide before your first try.", Color.LIGHT_GRAY));
		content.add(Box.createVerticalStrut(4));
		content.add(button("Open wiki strategy guide", () -> listener.onOpenWiki(c.getWikiPage())));

		content.add(section(step + ". Get the kill"));
		if (r.getKillCount() > 0)
		{
			content.add(text("★ Kill count: " + r.getKillCount(), GOOD));
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

	private void money(PvmGuide guide, LoadoutPlan plan)
	{
		Profit profit = guide.getProfit();
		if (profit == null)
		{
			return;
		}
		Profit.Verdict verdict = profit.verdict();
		Color color = verdict == Profit.Verdict.GOOD ? GOOD : verdict == Profit.Verdict.SOME ? WARN : MUTED;
		String line = "Money: " + verdict.getLabel()
			+ (profit.getGpPerHour() > 0 ? " · ~" + gp(profit.getGpPerHour()) + "/hr" : "");
		content.add(Box.createVerticalStrut(4));
		content.add(text(line, color));
		content.add(text(profit.getNote(), MUTED));
		if (plan != null && plan.getTripCost() > 0)
		{
			content.add(text("Supplies cost ~" + gp(plan.getTripCost()) + " a trip.", MUTED));
		}
	}

	private void loadout(LoadoutPlan plan)
	{
		if (plan.getGear().isEmpty() && plan.getSupplies().isEmpty())
		{
			content.add(text("Nothing to bring.", Color.LIGHT_GRAY));
			return;
		}
		if (!guides.isBankKnown())
		{
			content.add(text("Open your bank once so this can count what you own.", WARN));
		}
		if (!plan.getGear().isEmpty())
		{
			content.add(text("Wear:", Color.WHITE));
			for (LoadoutPlan.GearLine g : plan.getGear())
			{
				content.add(gearLine(g));
				if (g.getUpgrade() != null && guides.isBankKnown())
				{
					content.add(text("   ↑ Upgrade: " + g.getUpgrade().getName() + priceNote(g.getUpgradePrice()), HINT));
				}
			}
		}
		if (!plan.getSupplies().isEmpty())
		{
			content.add(Box.createVerticalStrut(4));
			content.add(text("Inventory:", Color.WHITE));
			for (LoadoutPlan.SupplyLine l : plan.getSupplies())
			{
				content.add(supplyLine(l));
				if (l.getSupply().getNote() != null)
				{
					content.add(text("   " + l.getSupply().getNote(), MUTED));
				}
			}
		}
		if (plan.getBuyCost() > 0 && guides.isBankKnown())
		{
			content.add(text("To buy: ~" + gp(plan.getBuyCost()) + " in total.", WARN));
		}
	}

	private JLabel gearLine(LoadoutPlan.GearLine g)
	{
		String name = g.getGear().getName();
		switch (g.getHave())
		{
			case WORN:
				return text("✔ " + name, GOOD);
			case CARRIED:
				return text("➜ Wear your " + name, WARN);
			case IN_BANK:
				return text("• " + name + " (in bank)", Color.LIGHT_GRAY);
			case BUY:
			default:
				return guides.isBankKnown()
					? text("✘ " + name + ": buy" + priceNote(g.getPrice()) + " (" + g.getSlot() + ")", BAD)
					: text("? " + name + " (" + g.getSlot() + ")", MUTED);
		}
	}

	private JLabel supplyLine(LoadoutPlan.SupplyLine l)
	{
		Supply sup = l.getSupply();
		String what = l.getAmount() + " x " + (sup.getOptions().size() == 1 ? l.buyName()
			: sup.getName() + " (" + l.buyName().toLowerCase() + " or better)");
		if (l.getCarried() >= l.getAmount())
		{
			return text("✔ " + what, GOOD);
		}
		int inBank = l.getWithdraw().values().stream().mapToInt(Integer::intValue).sum();
		StringBuilder status = new StringBuilder();
		if (l.getCarried() > 0)
		{
			status.append(l.getCarried()).append(" carried");
		}
		if (inBank > 0)
		{
			status.append(status.length() > 0 ? ", " : "").append(inBank).append(" from bank");
		}
		if (l.getToBuy() > 0 && guides.isBankKnown())
		{
			status.append(status.length() > 0 ? ", " : "").append("buy ").append(l.getToBuy())
				.append(priceNote((long) l.getToBuy() * l.getPrice()));
		}
		else if (!sup.isBuyable() && l.getCarried() + inBank < l.getAmount())
		{
			status.append(status.length() > 0 ? ", " : "").append("get it in game");
		}
		boolean missing = l.getToBuy() > 0 || (!sup.isBuyable() && l.getCarried() + inBank < l.getAmount());
		return text((missing ? "✘ " : "• ") + what + (status.length() > 0 ? ": " + status : ""),
			missing && guides.isBankKnown() ? (sup.isRequired() ? BAD : WARN) : Color.LIGHT_GRAY);
	}

	private static String priceNote(long price)
	{
		return price > 0 ? " (~" + gp(price) + ")" : "";
	}

	private static String gp(long gp)
	{
		if (gp >= 1_000_000)
		{
			return String.format("%.1fm", gp / 1_000_000.0);
		}
		if (gp >= 10_000)
		{
			return (gp / 1000) + "k";
		}
		return String.format("%,d gp", gp);
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
				// A star for anything killed at least once, a tick for things only marked done by hand
				String mark = r.getKillCount() > 0 ? "★ " : r.getStatus() == Readiness.Status.DONE ? "✔ " : "";
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
