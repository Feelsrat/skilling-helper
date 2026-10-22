package com.skillinghelper.ui;

import com.skillinghelper.ActiveGuide;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.SkillOutfit;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.engine.Format;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.PlanSegment;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Requirement;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: pick a skill and goal, see the current step, what you need, the plan and alternative methods.
 */
public class SkillingHelperPanel extends PluginPanel
{
	private static final int TEXT_WIDTH = 165;
	private static final Color GOOD = new Color(80, 200, 80);
	private static final Color WARN = new Color(255, 170, 0);
	private static final Color BAD = new Color(230, 80, 80);
	private static final Color HINT = new Color(110, 170, 255);
	private static final Color GOLD = new Color(255, 215, 90);

	public interface Listener
	{
		void onSkillSelected(Skill skill);

		void onTargetChanged(int target);

		void onTargetReset();

		void onPreferenceChanged(TrainingPreference preference);

		void onMethodChosen(String methodId);

		void onToggleBankFilter();

		void onOpenWiki(String page);
	}

	private final Listener listener;
	private final SkillIconManager skillIcons;
	private final JComboBox<Skill> skillBox = new JComboBox<>();
	private final SpinnerNumberModel targetModel = new SpinnerNumberModel(2, 2, Experience.MAX_REAL_LEVEL, 1);
	private final JSpinner targetSpinner = new JSpinner(targetModel);
	private final JComboBox<TrainingPreference> prefBox = new JComboBox<>(TrainingPreference.values());
	private final JLabel levelLabel = new JLabel();
	private final JPanel content = new JPanel();
	private boolean updating;
	private String lastSignature;

	public SkillingHelperPanel(Listener listener, SkillIconManager skillIcons)
	{
		super(false);
		this.listener = listener;
		this.skillIcons = skillIcons;
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(header(), BorderLayout.NORTH);
		add(scrollingContent(), BorderLayout.CENTER);
		showIntro();
	}

	// ---- Layout ----

	private JPanel header()
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Skilling Helper");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(left(title));
		header.add(Box.createVerticalStrut(6));
		header.add(full(skillPicker()));
		header.add(Box.createVerticalStrut(6));
		header.add(full(goalRow()));
		header.add(Box.createVerticalStrut(6));
		header.add(full(styleRow()));
		header.add(Box.createVerticalStrut(8));
		return header;
	}

	private JComboBox<Skill> skillPicker()
	{
		skillBox.addItem(null);
		for (Skill s : Skill.values())
		{
			skillBox.addItem(s);
		}
		skillBox.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
				boolean focus)
			{
				super.getListCellRendererComponent(list, value, index, selected, focus);
				if (value == null)
				{
					setText("Choose a skill...");
					setIcon(null);
				}
				else
				{
					Skill s = (Skill) value;
					setText(s.getName());
					setIcon(new ImageIcon(skillIcons.getSkillImage(s, true)));
				}
				return this;
			}
		});
		skillBox.addActionListener(e ->
		{
			if (!updating)
			{
				listener.onSkillSelected((Skill) skillBox.getSelectedItem());
			}
		});
		return skillBox;
	}

	private JPanel goalRow()
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		levelLabel.setForeground(Color.WHITE);
		row.add(levelLabel, BorderLayout.WEST);
		targetSpinner.addChangeListener(e ->
		{
			if (!updating)
			{
				listener.onTargetChanged(targetModel.getNumber().intValue());
			}
		});
		targetSpinner.setToolTipText("Target level");
		row.add(targetSpinner, BorderLayout.CENTER);
		JButton reset = new JButton("Reset");
		reset.setToolTipText("Reset the goal to your level plus the default offset");
		reset.addActionListener(e -> listener.onTargetReset());
		row.add(reset, BorderLayout.EAST);
		return row;
	}

	private JPanel styleRow()
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel styleLabel = new JLabel("Style");
		styleLabel.setForeground(Color.WHITE);
		row.add(styleLabel, BorderLayout.WEST);
		prefBox.addActionListener(e ->
		{
			if (!updating)
			{
				listener.onPreferenceChanged((TrainingPreference) prefBox.getSelectedItem());
			}
		});
		row.add(prefBox, BorderLayout.CENTER);
		return row;
	}

	private JScrollPane scrollingContent()
	{
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrap.add(content, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(wrap);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		return scroll;
	}

	// ---- Updating ----

	/**
	 * Refreshes the panel. Must be called on the Swing thread.
	 */
	public void update(ActiveGuide guide, TrainingPreference preference, boolean bankFilterOn)
	{
		// Rebuilding the panel while a dropdown is open closes it, so wait until it's closed (updates come every tick)
		if (skillBox.isPopupVisible() || prefBox.isPopupVisible())
		{
			return;
		}
		String signature = signature(guide, preference, bankFilterOn);
		if (signature.equals(lastSignature))
		{
			return;
		}
		lastSignature = signature;

		syncControls(guide, preference);
		content.removeAll();
		if (!guide.isActive())
		{
			showIntro();
		}
		else if (!guide.isHasGuide())
		{
			showNoGuide(guide);
		}
		else
		{
			showGuide(guide, bankFilterOn);
		}
		content.revalidate();
		content.repaint();
	}

	/**
	 * Updates the skill, goal and style controls without firing their listeners.
	 */
	private void syncControls(ActiveGuide guide, TrainingPreference preference)
	{
		updating = true;
		try
		{
			if (!Objects.equals(skillBox.getSelectedItem(), guide.getSkill()))
			{
				skillBox.setSelectedItem(guide.getSkill());
			}
			if (prefBox.getSelectedItem() != preference)
			{
				prefBox.setSelectedItem(preference);
			}
			if (guide.isActive())
			{
				int min = Math.min(guide.getLevel() + 1, Experience.MAX_REAL_LEVEL);
				targetModel.setMinimum(min);
				targetModel.setValue(Math.max(min, guide.getTargetLevel()));
				levelLabel.setText("Level " + guide.getLevel() + "  →");
			}
			else
			{
				levelLabel.setText("Goal");
			}
		}
		finally
		{
			updating = false;
		}
	}

	private void showIntro()
	{
		content.removeAll();
		content.add(text("Pick a skill to get a step-by-step guide. It adapts to your level, items, bank and gold, "
			+ "and highlights what to click.", Color.LIGHT_GRAY));
		content.add(Box.createVerticalStrut(6));
		content.add(text("Open your bank once so the guide knows what you own.", HINT));
	}

	private void showNoGuide(ActiveGuide guide)
	{
		section("No built-in guide yet");
		content.add(text("There's no step-by-step guide for " + guide.getSkill().getName()
			+ " yet. The wiki training guide covers every method.", Color.LIGHT_GRAY));
		content.add(Box.createVerticalStrut(6));
		content.add(button("Open wiki training guide", () -> listener.onOpenWiki(guide.getWikiPage())));
	}

	private void showGuide(ActiveGuide guide, boolean bankFilterOn)
	{
		Method method = guide.getMethod();
		if (method != null && guide.getOption() != null)
		{
			addSummary(guide);
		}
		addCurrentStep(guide.getStep(), bankFilterOn);
		addRequirements(guide);
		addPlan(guide);
		addMethods(guide);
		if (method != null && method.getWikiPage() != null)
		{
			content.add(Box.createVerticalStrut(6));
			content.add(button("Open wiki guide", () -> listener.onOpenWiki(method.getWikiPage())));
		}
	}

	// ---- Sections ----

	/**
	 * Time to the goal, xp rate, cost or profit, and the player's gold.
	 */
	private void addSummary(ActiveGuide guide)
	{
		MethodOption option = guide.getOption();
		Method method = guide.getMethod();
		addTimeToGoal(guide);
		content.add(text(String.format("%,d xp to go · ~%,d actions with this method", guide.getXpRemaining(),
			option.getActions()), Color.LIGHT_GRAY));
		long gpHr = option.gpPerHour();
		if (gpHr != 0)
		{
			content.add(text((gpHr > 0 ? "Profit ~" : "Costs ~") + Format.gp(Math.abs(gpHr)) + "/hr ("
				+ gpPerXp(option) + ")", gpHr > 0 ? GOOD : WARN));
		}
		boolean usesItems = !method.getInputs().isEmpty() || !method.getProducts().isEmpty();
		if (usesItems)
		{
			addGoalCost(guide);
		}
		PlayerState player = guide.getPlayer();
		if (player != null)
		{
			content.add(text("Your GP: " + Format.gp(player.gp())
				+ (player.bankKnown() ? "" : " (open your bank to include it)"), Color.LIGHT_GRAY));
		}
		if (guide.getPriceSource() != null && usesItems)
		{
			content.add(text(guide.getPriceSource(), HINT));
		}
	}

	private void addTimeToGoal(ActiveGuide guide)
	{
		MethodOption option = guide.getOption();
		double planHours = guide.getPlan().isEmpty()
			? option.hours(guide.getXpRemaining())
			: guide.getPlan().stream().mapToDouble(seg -> seg.getOption().hours(seg.getXp())).sum();
		JLabel eta = text("Time to level " + guide.getTargetLevel() + ": " + duration(planHours)
			+ (option.isRun() ? " of play" : ""), Color.WHITE);
		eta.setFont(FontManager.getRunescapeBoldFont());
		content.add(eta);
		if (option.isRun())
		{
			content.add(text(String.format("~%,d runs, one every %s while they grow", option.runs(
				guide.getXpRemaining()), duration(guide.getMethod().getCycleMinutes() / 60.0)), Color.LIGHT_GRAY));
		}
		content.add(text(option.isMeasured()
			? String.format("Based on your own rate: ~%,d xp/hr", Math.round(option.getXpPerHour()))
			: String.format("Guide estimate ~%,d xp/hr; switches to your own rate after 10 minutes of training",
				Math.round(option.getXpPerHour())), HINT));
	}

	/**
	 * What the whole goal costs or earns, across every method the plan switches through.
	 */
	private void addGoalCost(ActiveGuide guide)
	{
		MethodOption option = guide.getOption();
		long goalCost = guide.getPlan().isEmpty()
			? Math.round(-option.getGpPerAction() * option.getActions())
			: Math.round(guide.getPlan().stream()
			.mapToDouble(seg -> -seg.getOption().getGpPerAction() * seg.getOption().getActions()).sum());
		if (goalCost > 0)
		{
			content.add(text("Whole goal costs ~" + Format.gp(goalCost) + " net (after selling products)", WARN));
		}
		else if (goalCost < 0)
		{
			content.add(text("Whole goal earns ~" + Format.gp(-goalCost) + " after GE tax", GOOD));
		}
	}

	private void addCurrentStep(GuideStep step, boolean bankFilterOn)
	{
		section("Current step");
		JPanel stepBox = new JPanel();
		stepBox.setLayout(new BoxLayout(stepBox, BoxLayout.Y_AXIS));
		stepBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		stepBox.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, ColorScheme.BRAND_ORANGE),
			new EmptyBorder(6, 6, 6, 6)));
		if (step != null)
		{
			JLabel stepTitle = text(step.getTitle(), Color.WHITE);
			stepTitle.setFont(FontManager.getRunescapeBoldFont());
			stepBox.add(stepTitle);
			for (String line : step.getDetails())
			{
				stepBox.add(Box.createVerticalStrut(3));
				stepBox.add(text(line, Color.LIGHT_GRAY));
			}
			if (step.getDestinationName() != null)
			{
				stepBox.add(Box.createVerticalStrut(3));
				stepBox.add(text("Follow the arrow on the minimap / world map to " + step.getDestinationName() + ".",
					HINT));
			}
		}
		content.add(full(stepBox));
		content.add(Box.createVerticalStrut(6));
		content.add(button(bankFilterOn ? "Show all bank items" : "Show only needed items in bank",
			listener::onToggleBankFilter));
	}

	private void addRequirements(ActiveGuide guide)
	{
		if (guide.getRequirements().isEmpty())
		{
			return;
		}
		section("What you need");
		for (Requirement r : guide.getRequirements())
		{
			content.add(requirementLine(r));
		}
	}

	/**
	 * The plan's legs, unless it's just the one method the player picked.
	 */
	private void addPlan(ActiveGuide guide)
	{
		if (guide.getPlan().isEmpty() || (guide.getPlan().size() == 1 && guide.isForced()))
		{
			return;
		}
		section("Plan to level " + guide.getTargetLevel());
		for (PlanSegment seg : guide.getPlan())
		{
			content.add(text(String.format("%d–%d: %s (%s)", seg.getFromLevel(), seg.getToLevel(),
				seg.getOption().getMethod().getName(), duration(seg.getOption().hours(seg.getXp()))),
				seg.getOption().getMethod() == guide.getMethod() ? Color.WHITE : Color.LIGHT_GRAY));
		}
	}

	private void addMethods(ActiveGuide guide)
	{
		section("Methods");
		if (guide.isForced())
		{
			content.add(button("Let the guide choose for me", () -> listener.onMethodChosen(null)));
			content.add(Box.createVerticalStrut(4));
		}
		for (MethodOption o : guide.getOptions())
		{
			content.add(methodRow(o, o.getMethod() == guide.getMethod(), guide.getXpRemaining(),
				guide.getTargetLevel(), guide.getPlayer()));
			content.add(Box.createVerticalStrut(4));
		}
	}

	private JPanel methodRow(MethodOption o, boolean current, long xpRemaining, int targetLevel, PlayerState player)
	{
		Method m = o.getMethod();
		SkillOutfit earns = unownedOutfitEarnedBy(m, player);
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setBackground(current ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(earns == null
			? new EmptyBorder(4, 6, 4, 6)
			: BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, GOLD),
			new EmptyBorder(4, 3, 4, 6)));

		JLabel name = text(m.getName() + (current ? "  (current)" : ""), o.isAvailable() ? Color.WHITE : Color.GRAY);
		name.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		row.add(name);
		row.add(text(methodInfo(o), Color.LIGHT_GRAY));
		if (xpRemaining > 0)
		{
			row.add(text(duration(o.hours(xpRemaining)) + (o.isRun() ? " of play (" + String.format("%,d", o.runs(
				xpRemaining)) + " runs)" : "") + " to level " + targetLevel + " using only this", Color.LIGHT_GRAY));
		}
		if (earns != null)
		{
			row.add(text("★ Earns the " + earns.getName() + " (" + earns.getBonus() + "), which makes the rest of "
				+ "the grind faster", GOLD));
		}
		long gpHr = o.gpPerHour();
		if (gpHr != 0)
		{
			row.add(text((gpHr > 0 ? "+" : "-") + Format.gp(Math.abs(gpHr)) + "/hr · " + gpPerXp(o),
				gpHr > 0 ? GOOD : WARN));
		}
		if (o.isAvailable() && !o.isAffordable())
		{
			row.add(text("Can't afford the materials for your goal yet", WARN));
		}
		if (!o.isAvailable())
		{
			row.add(text(String.join(", ", o.getBlockers()), BAD));
		}
		else if (!current)
		{
			row.add(left(useButton(m, row.getBackground())));
		}
		return full(row);
	}

	/**
	 * "Lvl 30 · ~45k xp/hr · Members · AFK, Profit".
	 */
	private static String methodInfo(MethodOption o)
	{
		Method m = o.getMethod();
		String tags = m.getTags().stream().map(MethodTag::getLabel).collect(Collectors.joining(", "));
		return String.format("Lvl %d · ~%dk xp/hr%s%s%s", m.getMinLevel(), Math.round(o.getXpPerHour() / 1000),
			o.isMeasured() ? " (yours)" : "",
			m.isMembers() ? " · Members" : "", tags.isEmpty() ? "" : " · " + tags);
	}

	private JPanel useButton(Method m, Color background)
	{
		JButton use = new JButton("Use this method");
		use.setAlignmentX(Component.LEFT_ALIGNMENT);
		use.addActionListener(e -> listener.onMethodChosen(m.getId()));
		JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
		wrap.setBackground(background);
		wrap.add(use);
		return wrap;
	}

	/**
	 * The outfit this method earns, if the player is still missing pieces of it.
	 */
	private static SkillOutfit unownedOutfitEarnedBy(Method m, PlayerState player)
	{
		if (player == null)
		{
			return null;
		}
		for (SkillOutfit outfit : SkillOutfit.values())
		{
			if (!m.getId().equals(outfit.getSourceMethodId()))
			{
				continue;
			}
			for (int[] slot : outfit.getSlots())
			{
				if (player.owned(slot) == 0)
				{
					return outfit;
				}
			}
		}
		return null;
	}

	private static String gpPerXp(MethodOption o)
	{
		double v = o.gpPerXp();
		return String.format("%s%.1f gp/xp", v > 0 ? "+" : "", v);
	}

	private JLabel requirementLine(Requirement r)
	{
		switch (r.getStatus())
		{
			case READY:
				return text("✔ " + r.getText(), GOOD);
			case IN_BANK:
				return text("▣ " + r.getText(), WARN);
			case MISSING:
				return text("✖ " + r.getText(), BAD);
			case OPTIONAL:
				return text("☆ " + r.getText(), HINT);
			case SUGGESTED:
				return text("+ " + r.getText(), HINT);
			case UNLOCK:
				return text("★ " + r.getText(), GOLD);
			default:
				return text("ℹ " + r.getText(), Color.LIGHT_GRAY);
		}
	}

	// ---- Building blocks ----

	private void section(String title)
	{
		content.add(Box.createVerticalStrut(10));
		JLabel l = new JLabel(title);
		l.setFont(FontManager.getRunescapeBoldFont());
		l.setForeground(ColorScheme.BRAND_ORANGE);
		content.add(left(l));
		content.add(Box.createVerticalStrut(4));
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
		b.setAlignmentX(Component.LEFT_ALIGNMENT);
		b.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.getPreferredSize().height));
		b.addActionListener(e -> action.run());
		return b;
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

	/**
	 * "~3h 05m", or "-" when there's no estimate.
	 */
	static String duration(double hours)
	{
		return hours <= 0 ? "-" : "~" + Format.hours(hours);
	}

	/**
	 * Everything the panel shows, so it's only rebuilt when something visible changed.
	 */
	private static String signature(ActiveGuide g, TrainingPreference pref, boolean bankFilterOn)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(g.getSkill()).append('|').append(g.getLevel()).append('|').append(g.getTargetLevel()).append('|')
			.append(g.getXpRemaining() / 500).append('|').append(pref).append('|').append(bankFilterOn).append('|')
			.append(g.isForced()).append('|').append(g.getMethod() == null ? "" : g.getMethod().getId()).append('|')
			.append(g.getPriceSource());
		GuideStep s = g.getStep();
		if (s != null)
		{
			sb.append('|').append(s.getTitle()).append(s.getDetails());
		}
		for (Requirement r : g.getRequirements())
		{
			sb.append('|').append(r.getText()).append(r.getStatus()).append(Arrays.toString(r.getItemIds()));
		}
		for (PlanSegment p : g.getPlan())
		{
			sb.append('|').append(p.getFromLevel()).append(p.getOption().getMethod().getId());
		}
		for (MethodOption o : g.getOptions())
		{
			sb.append('|').append(o.getMethod().getId()).append(o.isAvailable()).append(o.gpPerHour() / 1000)
				.append(o.isAffordable()).append(Math.round(o.getXpPerHour() / 1000));
		}
		if (g.getPlayer() != null)
		{
			sb.append('|').append(g.getPlayer().gp() / 1000).append(g.getPlayer().bankKnown());
		}
		return sb.toString();
	}
}
