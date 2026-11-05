package com.skillinghelper.pvm.ui;

import com.skillinghelper.data.Wilderness;
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
import com.skillinghelper.pvm.guide.WaveCard;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
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
	private static final int TEXT_WIDTH = GuideUi.WIDTH;
	private static final Color GOOD = GuideUi.GOOD;
	private static final Color WARN = GuideUi.WARN;
	private static final Color BAD = GuideUi.BAD;
	private static final Color HINT = GuideUi.HINT;
	private static final Color MUTED = GuideUi.MUTED;
	/**
	 * Matches the default "Attack first" highlight.
	 */
	private static final Color FIRST = new Color(255, 90, 90);

	public interface Listener
	{
		void onTrain(Skill skill, int level);

		void onShowPath(PvmContent content);

		void onClearPath();

		void onOpenWiki(String page);

		void onToggleDone(PvmContent content);

		void onStartGuide(PvmContent content);

		void onStopGuide();

		/**
		 * Let every guide pick Wilderness bosses and methods, or leave them out again.
		 */
		void onAllowWilderness(boolean allow);

		/**
		 * Switches the shared "Hide outleveled guides" setting.
		 */
		void onHideOutleveled(boolean hide);
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
	private boolean wildernessAllowed;
	private boolean hideOutleveled = true;
	private PlayerView view;
	private String pathId;
	private GuideView guides = GuideView.NONE;
	/**
	 * Holds the running guide's current step, so it can change every tick without rebuilding the panel.
	 */
	private final JPanel stepBox = new JPanel();

	/**
	 * Holds back redraws while the panel is closed.
	 */
	private final HiddenRedraws redraws;

	public PvmProgressionPanel(Listener listener)
	{
		super(false);
		redraws = new HiddenRedraws(this);
		this.listener = listener;
		JPanel header = GuideUi.header("PvM Progression", "Bosses in the order to learn them, step by step.");
		picker.setRenderer(new PickerRenderer());
		picker.setMaximumRowCount(20);
		picker.addActionListener(e ->
		{
			Readiness r = (Readiness) picker.getSelectedItem();
			if (!updating && r != null)
			{
				Readiness first = first();
				pickedId = first != null && first.getContent().getId().equals(r.getContent().getId())
					? null : r.getContent().getId();
				showDetail();
			}
		});
		header.add(full(picker));
		GuideUi.frame(this, header, content);

		content.add(GuideUi.empty("Not logged in", "Log in to see which boss to work towards next."));

		stepBox.setLayout(new BoxLayout(stepBox, BoxLayout.Y_AXIS));
		stepBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		stepBox.setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, HINT), new EmptyBorder(6, 6, 7, 6)));
		stepBox.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

	/**
	 * Shows the latest state. Runs on the Swing thread.
	 *
	 * @param pathId id of the content Shortest Path is showing the way to, or null
	 */
	public void update(List<Readiness> ladder, PlayerView view, String pathId, GuideView guides)
	{
		redraws.run("update", () -> applyUpdate(ladder, view, pathId, guides));
	}

	private void applyUpdate(List<Readiness> ladder, PlayerView view, String pathId, GuideView guides)
	{
		this.ladder = ladder;
		this.view = view;
		this.pathId = pathId;
		String shownGuideId = this.guides.getActiveId();
		this.guides = guides;
		if (guides.getActiveId() != null && !guides.getActiveId().equals(shownGuideId))
		{
			// Jump to a guide when it starts
			Readiness first = first();
			pickedId = first != null && first.getContent().getId().equals(guides.getActiveId())
				? null : guides.getActiveId();
		}
		rebuild();
	}

	/**
	 * Follows the shared Wilderness setting. Runs on the Swing thread.
	 */
	public void setWildernessAllowed(boolean allowed)
	{
		redraws.run("wilderness", () -> applySetWildernessAllowed(allowed));
	}

	private void applySetWildernessAllowed(boolean allowed)
	{
		if (wildernessAllowed != allowed)
		{
			wildernessAllowed = allowed;
			if (ladder != null)
			{
				rebuild();
			}
		}
	}

	/**
	 * Follows the shared "Hide outleveled guides" setting. Runs on the Swing thread.
	 */
	public void setHideOutleveled(boolean hide)
	{
		redraws.run("outleveled", () -> applySetHideOutleveled(hide));
	}

	private void applySetHideOutleveled(boolean hide)
	{
		if (hideOutleveled != hide)
		{
			hideOutleveled = hide;
			if (ladder != null)
			{
				rebuild();
			}
		}
	}

	/**
	 * The entry shown when the player hasn't picked one: the lowest-level one not done, skipping the Wilderness
	 * (unless included) and outgrown ones (unless shown).
	 */
	private Readiness first()
	{
		return Readiness.next(ladder, wildernessAllowed, hideOutleveled);
	}

	/**
	 * Hidden from the dropdown: outgrown while the setting hides those, and not the one being shown or guided.
	 */
	private boolean hidden(Readiness r)
	{
		String id = r.getContent().getId();
		return hideOutleveled && r.isOutgrown() && !id.equals(pickedId) && !id.equals(guides.getActiveId());
	}

	/**
	 * Shows the running guide's new step. Runs on the Swing thread.
	 */
	public void updateStep(PvmStep step)
	{
		redraws.run("step", () -> applyUpdateStep(step));
	}

	private void applyUpdateStep(PvmStep step)
	{
		guides = new GuideView(guides.getActiveId(), step, guides.getPlans(), guides.isBankKnown());
		fillStep(step);
	}

	private void fillStep(PvmStep step)
	{
		stepBox.removeAll();
		if (step != null)
		{
			GuideUi.fillStep(stepBox, "Now", HINT, step.getTitle(), Collections.emptyList());
			GuideUi.addStop(stepBox, "Now", HINT, listener::onStopGuide);
			if (step.getWave() != null)
			{
				stepBox.add(Box.createVerticalStrut(5));
				stepBox.add(waveCard(step.getWave()));
			}
			if (!step.getLines().isEmpty())
			{
				stepBox.add(Box.createVerticalStrut(5));
				GuideUi.lines(stepBox, step.getLines(), GuideUi.CARD_WIDTH);
			}
		}
		stepBox.revalidate();
		stepBox.repaint();
	}

	private void rebuild()
	{
		updating = true;
		picker.removeAllItems();
		Readiness shown = pickedId == null ? null : find(pickedId);
		if (shown == null)
		{
			pickedId = null;
			shown = first();
		}
		for (Readiness r : ladder)
		{
			if (r == shown || !hidden(r))
			{
				picker.addItem(r);
			}
		}
		picker.setSelectedItem(shown);
		updating = false;
		showDetail();
	}

	private void showDetail()
	{
		content.removeAll();
		Readiness shown = (Readiness) picker.getSelectedItem();
		if (Readiness.next(ladder, wildernessAllowed) == null)
		{
			content.add(text("You've done everything on the list. Nice work!", GOOD));
			content.add(Box.createVerticalStrut(8));
		}
		wildernessNote();
		outleveledNote();
		if (shown != null)
		{
			detail(shown);
		}
		content.revalidate();
		content.repaint();
	}

	/**
	 * Says which Wilderness entries the list skipped over, with the switch to include them.
	 */
	private void wildernessNote()
	{
		if (wildernessAllowed)
		{
			return;
		}
		Readiness next = Readiness.next(ladder, false);
		List<String> skipped = new ArrayList<>();
		for (Readiness r : ladder)
		{
			if (r == next)
			{
				break;
			}
			if (r.getContent().isWilderness() && r.getStatus() != Readiness.Status.DONE)
			{
				skipped.add(r.getContent().getName());
			}
		}
		if (skipped.isEmpty())
		{
			return;
		}
		content.add(text("☠ Skipped because " + (skipped.size() == 1 ? "it's" : "they're") + " in the Wilderness: "
			+ String.join(", ", skipped) + ". Pick " + (skipped.size() == 1 ? "it" : "one")
			+ " in the list to do it anyway.", Wilderness.COLOR));
		content.add(Box.createVerticalStrut(3));
		content.add(GuideUi.actions(GuideUi.small("Include Wilderness bosses",
			() -> listener.onAllowWilderness(true))));
		content.add(Box.createVerticalStrut(8));
	}

	/**
	 * Says how many outgrown entries the dropdown leaves out (or could), with the switch to change it.
	 */
	private void outleveledNote()
	{
		long outgrown = ladder.stream().filter(Readiness::isOutgrown).count();
		if (outgrown == 0)
		{
			return;
		}
		content.add(text(hideOutleveled
			? outgrown + (outgrown == 1 ? " boss you've outgrown is" : " bosses you've outgrown are")
			+ " hidden from the list."
			: outgrown + (outgrown == 1 ? " boss is" : " bosses are") + " past your level range.", MUTED));
		content.add(Box.createVerticalStrut(3));
		content.add(GuideUi.actions(GuideUi.small(hideOutleveled ? "Show outgrown bosses" : "Hide outgrown bosses",
			() -> listener.onHideOutleveled(!hideOutleveled))));
		content.add(Box.createVerticalStrut(8));
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
		content.add(text(r.getStatus().getLabel() + " · " + c.levelRange() + " · " + c.getTier().getLabel(),
			statusColor(r.getStatus())));
		if (r.isOutgrown())
		{
			content.add(text("You've outgrown this one: your combat stats are past its range.", MUTED));
		}
		if (c.isWilderness())
		{
			content.add(text(Wilderness.TAG + ": " + Wilderness.WARNING, Wilderness.COLOR));
			if (!wildernessAllowed && r != Readiness.next(ladder, true))
			{
				content.add(text("The list skips Wilderness bosses unless you include them.", MUTED));
			}
		}
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
			}
			else
			{
				content.add(GuideUi.primary("▶ Start guide", () -> listener.onStartGuide(c)));
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
			content.add(GuideUi.actions(c.getId().equals(pathId) ? GuideUi.small("Clear path", listener::onClearPath)
				: GuideUi.small("Show path", () -> listener.onShowPath(c))));
		}

		content.add(section(step++ + ". Learn the fight"));
		Fight fight = guide == null ? null : guide.getFight();
		if (fight != null)
		{
			for (String tip : fight.getTips())
			{
				content.add(text("• " + tip, Color.LIGHT_GRAY));
				content.add(Box.createVerticalStrut(3));
			}
			content.add(Box.createVerticalStrut(2));
		}
		content.add(text("Read the strategy guide before your first try.", Color.LIGHT_GRAY));
		content.add(Box.createVerticalStrut(4));
		content.add(GuideUi.actions(GuideUi.small("Wiki strategy guide", () -> listener.onOpenWiki(c.getWikiPage()))));

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
			content.add(GuideUi.actions(GuideUi.small(marked ? "Mark as not done" : "Mark as done",
				() -> listener.onToggleDone(c))));
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
		content.add(Box.createVerticalStrut(4));
		content.add(GuideUi.text("Money: " + verdict.getLabel(), color, TEXT_WIDTH));
		if (profit.getGpPerHour() > 0)
		{
			content.add(GuideUi.stat("Profit", "~" + gp(profit.getGpPerHour()) + "/hr", color));
		}
		if (plan != null && plan.getTripCost() > 0)
		{
			content.add(GuideUi.stat("Supplies a trip", "~" + gp(plan.getTripCost()), GuideUi.TEXT));
		}
		content.add(GuideUi.text(profit.getNote(), MUTED, TEXT_WIDTH));
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
			content.add(GuideUi.subheading("Wear"));
			for (LoadoutPlan.GearLine g : plan.getGear())
			{
				content.add(Box.createVerticalStrut(2));
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
			content.add(GuideUi.subheading("Inventory"));
			for (LoadoutPlan.SupplyLine l : plan.getSupplies())
			{
				content.add(Box.createVerticalStrut(2));
				content.add(supplyLine(l));
				if (l.getSupply().getNote() != null)
				{
					JLabel note = GuideUi.text(l.getSupply().getNote(), MUTED, TEXT_WIDTH - 14);
					note.setBorder(new EmptyBorder(0, 14, 0, 0));
					content.add(note);
				}
			}
		}
		if (plan.getBuyCost() > 0 && guides.isBankKnown())
		{
			content.add(text("To buy: ~" + gp(plan.getBuyCost()) + " in total.", WARN));
		}
	}

	private JComponent gearLine(LoadoutPlan.GearLine g)
	{
		String name = g.getGear().getName();
		switch (g.getHave())
		{
			case WORN:
				return text("✔ " + name, GOOD);
			case CARRIED:
				return g.isSwap() ? text("✔ " + name + " (swap)", GOOD) : text("➜ Wear your " + name, WARN);
			case IN_BANK:
				return text("• " + name + " (in bank)", Color.LIGHT_GRAY);
			case BUY:
			default:
				return guides.isBankKnown()
					? text("✘ " + name + ": buy" + priceNote(g.getPrice()) + " (" + g.getSlot() + ")", BAD)
					: text("? " + name + " (" + g.getSlot() + ")", MUTED);
		}
	}

	private JComponent supplyLine(LoadoutPlan.SupplyLine l)
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
		Color color = met ? GOOD : (s.isRequired() || s.getLevel() - have > 5 ? BAD : WARN);
		if (met)
		{
			return text(label, color);
		}
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.add(text(label, color, TEXT_WIDTH - 55), BorderLayout.CENTER);
		JButton train = new JButton("Train");
		train.setToolTipText("Open the Skilling Helper guide for " + s.getSkill().getName() + " to level "
			+ s.getLevel());
		train.addActionListener(e -> listener.onTrain(s.getSkill(), s.getLevel()));
		row.add(train, BorderLayout.EAST);
		return full(row);
	}

	private JComponent itemLine(ItemNeed i)
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
	 * Dropdown rows: "70→99  Barrows (ready)", coloured by status.
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
				setText(mark + r.getContent().getLevel() + "→" + r.getContent().topLevel() + "  "
					+ r.getContent().getName() + " ("
					+ r.getStatus().getLabel().toLowerCase() + ")"
					+ (r.getContent().isWilderness() ? " " + Wilderness.TAG : ""));
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

	private static JComponent section(String s)
	{
		return GuideUi.section(s);
	}

	/**
	 * The wave that's up, readable at a glance: a big wave number, one row per monster with how it attacks, the
	 * wiki's tip, then drink reminders and doses left.
	 */
	private static JComponent waveCard(WaveCard card)
	{
		int width = TEXT_WIDTH - 25;
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		box.setBorder(new EmptyBorder(6, 6, 6, 6));
		box.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel header = new JLabel(card.getWave() > 0
			? "<html><span style='font-size:110%'>Wave " + card.getWave() + "</span>"
				+ "<span style='color:#969696'> / " + card.getTotal() + "</span></html>"
			: "Wave ?");
		header.setFont(FontManager.getRunescapeBoldFont());
		header.setForeground(Color.WHITE);
		box.add(left(header));
		if (card.getWave() <= 0)
		{
			box.add(text("Shows when the next wave starts (\"Wave: N\" in chat).", MUTED, width));
		}

		for (WaveCard.Row row : card.getRows())
		{
			box.add(Box.createVerticalStrut(5));
			JPanel line = new JPanel(new BorderLayout(4, 0));
			line.setOpaque(false);
			line.setAlignmentX(Component.LEFT_ALIGNMENT);
			JLabel name = new JLabel((row.getCount() > 1 ? row.getCount() + "× " : "") + row.getName()
				+ " (" + row.getLevel() + ")");
			name.setFont(FontManager.getRunescapeBoldFont());
			name.setForeground(row.isFirst() ? FIRST : Color.WHITE);
			JLabel style = new JLabel(row.getStyle().toUpperCase());
			style.setFont(FontManager.getRunescapeSmallFont());
			style.setForeground(styleColor(row.getStyle()));
			line.add(name, BorderLayout.WEST);
			line.add(style, BorderLayout.EAST);
			line.setMaximumSize(new Dimension(width + 10, name.getPreferredSize().height));
			box.add(line);
			box.add(text((row.isFirst() ? "▶ Attack first. " : "") + row.getNote(), row.isFirst() ? FIRST : MUTED,
				width));
		}

		if (card.getTip() != null)
		{
			box.add(Box.createVerticalStrut(6));
			JPanel tip = new JPanel();
			tip.setLayout(new BoxLayout(tip, BoxLayout.Y_AXIS));
			tip.setBackground(ColorScheme.DARK_GRAY_COLOR);
			tip.setBorder(new CompoundBorder(new MatteBorder(0, 2, 0, 0, HINT), new EmptyBorder(3, 5, 3, 3)));
			tip.setAlignmentX(Component.LEFT_ALIGNMENT);
			JLabel heading = new JLabel("Wiki tip");
			heading.setFont(FontManager.getRunescapeSmallFont());
			heading.setForeground(HINT);
			tip.add(left(heading));
			tip.add(text(card.getTip(), Color.LIGHT_GRAY, width - 12));
			box.add(tip);
		}

		if (!card.getAlerts().isEmpty())
		{
			box.add(Box.createVerticalStrut(6));
			for (String alert : card.getAlerts())
			{
				box.add(text("⚠ " + alert, WARN, width));
			}
		}
		box.add(Box.createVerticalStrut(6));
		box.add(text(card.getSupplies(), Color.LIGHT_GRAY, width));
		return box;
	}

	/**
	 * Wiki-style colours for the combat triangle: red melee, green ranged, blue magic.
	 */
	private static Color styleColor(String style)
	{
		String s = style.toLowerCase();
		if (s.contains("+"))
		{
			return Color.LIGHT_GRAY;
		}
		if (s.contains("magic"))
		{
			return HINT;
		}
		if (s.contains("ranged"))
		{
			return GOOD;
		}
		return BAD;
	}

	private static JComponent text(String s, Color color)
	{
		return text(s, color, TEXT_WIDTH);
	}

	/**
	 * A guide line in the shared look: split into short lines with the key bits highlighted. Light grey and green
	 * lines get inline highlights (green just colours the tick); other colours draw the whole line in that colour.
	 */
	private static JComponent text(String s, Color color, int width)
	{
		if (color == Color.LIGHT_GRAY || color == GuideUi.TEXT)
		{
			return GuideUi.line(s, width);
		}
		if (color == GOOD)
		{
			return GuideUi.line(s, GOOD, GuideUi.TEXT, width);
		}
		return GuideUi.line(s, color, width);
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
