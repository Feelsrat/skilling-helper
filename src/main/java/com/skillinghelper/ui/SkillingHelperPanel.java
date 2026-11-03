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
import com.skillinghelper.tog.TearsOfGuthix;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
	private static final int TEXT_WIDTH = GuideUi.WIDTH;
	private static final Color TEXT = GuideUi.TEXT;
	private static final Color MUTED = GuideUi.MUTED;
	private static final Color GOOD = GuideUi.GOOD;
	private static final Color WARN = GuideUi.WARN;
	private static final Color BAD = GuideUi.BAD;
	private static final Color HINT = GuideUi.HINT;
	private static final Color GOLD = GuideUi.GOLD;

	/**
	 * Dropdown entries that pick the skill for the player.
	 */
	@Getter
	@RequiredArgsConstructor
	public enum QuickPick
	{
		BEST_GOLD("Best gold/hr", "Opens the guide that earns the most gold per hour with your current stats"),
		LOWEST_SKILL("Lowest skill", "Opens the guide for your lowest-level skill"),
		TEARS_OF_GUTHIX("Tears of Guthix (weekly)",
			"Opens the guide to the weekly Tears of Guthix minigame, which gives xp in your lowest skill");

		private final String label;
		private final String description;
	}

	public interface Listener
	{
		void onSkillSelected(Skill skill);

		/**
		 * Let the plugin choose the skill (and for gold, the method) to train.
		 */
		void onQuickPick(QuickPick pick);

		/**
		 * Turn the guide off: no steps, paths or highlights until a skill is picked again.
		 */
		void onStopGuide();

		void onTargetChanged(int target);

		void onTargetReset();

		void onPreferenceChanged(TrainingPreference preference);

		void onMethodChosen(String methodId);

		void onToggleBankFilter();

		void onOpenWiki(String page);
	}

	private final Listener listener;
	private final SkillIconManager skillIcons;
	private final JComboBox<Object> skillBox = new JComboBox<>();
	private final JButton stopButton = new JButton("Stop");
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
		header.add(full(skillRow()));
		header.add(Box.createVerticalStrut(6));
		header.add(full(goalRow()));
		header.add(Box.createVerticalStrut(6));
		header.add(full(styleRow()));
		header.add(Box.createVerticalStrut(8));
		return header;
	}

	private JPanel skillRow()
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.add(skillPicker(), BorderLayout.CENTER);
		stopButton.setToolTipText("Turn the guide off: hides its steps, path and highlights");
		stopButton.setEnabled(false);
		stopButton.addActionListener(e -> listener.onStopGuide());
		row.add(stopButton, BorderLayout.EAST);
		return row;
	}

	private JComboBox<Object> skillPicker()
	{
		skillBox.addItem(null);
		for (QuickPick pick : QuickPick.values())
		{
			skillBox.addItem(pick);
		}
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
				setToolTipText(null);
				if (value == null)
				{
					setText("Choose a skill...");
					setIcon(null);
				}
				else if (value instanceof QuickPick)
				{
					QuickPick pick = (QuickPick) value;
					setText(pick.getLabel());
					setIcon(null);
					setToolTipText(pick.getDescription());
					if (!selected)
					{
						setForeground(GOLD);
					}
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
			if (updating)
			{
				return;
			}
			Object item = skillBox.getSelectedItem();
			if (item instanceof QuickPick)
			{
				// The plugin opens the skill it picks; resync the dropdown to it on the next update
				lastSignature = null;
				listener.onQuickPick((QuickPick) item);
			}
			else
			{
				listener.onSkillSelected((Skill) item);
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
		if (guide.getActivity() != null)
		{
			showActivity(guide, bankFilterOn);
		}
		else if (!guide.isActive())
		{
			showIntro();
			addWeekly(guide);
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
			Object selected = guide.getActivity() != null ? QuickPick.TEARS_OF_GUTHIX : guide.getSkill();
			if (!Objects.equals(skillBox.getSelectedItem(), selected))
			{
				skillBox.setSelectedItem(selected);
			}
			stopButton.setEnabled(guide.isActive());
			if (prefBox.getSelectedItem() != preference)
			{
				prefBox.setSelectedItem(preference);
			}
			if (guide.getSkill() != null)
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
		content.add(GuideUi.line("Pick a skill to get a step-by-step guide.", TEXT_WIDTH));
		content.add(Box.createVerticalStrut(3));
		content.add(GuideUi.line("It adapts to your level, items, bank and gold, and highlights what to click.",
			MUTED, TEXT_WIDTH));
		content.add(Box.createVerticalStrut(6));
		content.add(GuideUi.line("➜ Open your bank once so the guide knows what you own.", HINT, TEXT, TEXT_WIDTH));
	}

	/**
	 * Whether the weekly Tears of Guthix is up, with a button to open its guide.
	 */
	private void addWeekly(ActiveGuide guide)
	{
		if (guide.getWeeklyState() == null || guide.getWeeklyState() == TearsOfGuthix.State.LOCKED)
		{
			return;
		}
		section("Weekly");
		content.add(GuideUi.stat("Tears of Guthix", guide.getWeeklyState() == TearsOfGuthix.State.READY ? "Ready"
			: guide.getWeeklyState() == TearsOfGuthix.State.UNKNOWN ? "?" : "Not yet", weeklyColor(guide)));
		content.add(Box.createVerticalStrut(2));
		content.add(GuideUi.line(guide.getWeeklyStatus(), MUTED, TEXT_WIDTH));
		content.add(Box.createVerticalStrut(6));
		content.add(button("Open Tears of Guthix guide", () -> listener.onQuickPick(QuickPick.TEARS_OF_GUTHIX)));
	}

	private static Color weeklyColor(ActiveGuide guide)
	{
		if (guide.getWeeklyState() == TearsOfGuthix.State.READY)
		{
			return GOOD;
		}
		return guide.getWeeklyState() == TearsOfGuthix.State.UNKNOWN ? GOLD : WARN;
	}

	/**
	 * A guide that isn't about one skill: Tears of Guthix.
	 */
	private void showActivity(ActiveGuide guide, boolean bankFilterOn)
	{
		addCurrentStep(guide.getStep(), bankFilterOn);
		section(guide.getActivity());
		if (guide.getWeeklyState() != null && guide.getWeeklyState() != TearsOfGuthix.State.LOCKED)
		{
			content.add(GuideUi.stat("Status", guide.getWeeklyState() == TearsOfGuthix.State.READY ? "Ready"
				: guide.getWeeklyState() == TearsOfGuthix.State.UNKNOWN ? "Unknown" : "Not yet", weeklyColor(guide)));
			content.add(Box.createVerticalStrut(2));
			content.add(GuideUi.line(guide.getWeeklyStatus(), MUTED, TEXT_WIDTH));
			content.add(Box.createVerticalStrut(6));
		}
		GuideUi.lines(content, guide.getActivityInfo(), TEXT_WIDTH);
		if (guide.getWikiPage() != null)
		{
			content.add(Box.createVerticalStrut(6));
			content.add(button("Open wiki guide", () -> listener.onOpenWiki(guide.getWikiPage())));
		}
	}

	private void showNoGuide(ActiveGuide guide)
	{
		section("No built-in guide yet");
		content.add(GuideUi.line("There's no step-by-step guide for " + guide.getSkill().getName() + " yet.",
			TEXT_WIDTH));
		content.add(Box.createVerticalStrut(3));
		content.add(GuideUi.line("The wiki training guide covers every method.", MUTED, TEXT_WIDTH));
		content.add(Box.createVerticalStrut(6));
		content.add(button("Open wiki training guide", () -> listener.onOpenWiki(guide.getWikiPage())));
	}

	private void showGuide(ActiveGuide guide, boolean bankFilterOn)
	{
		Method method = guide.getMethod();
		addCurrentStep(guide.getStep(), bankFilterOn);
		if (method != null && guide.getOption() != null)
		{
			addSummary(guide);
		}
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
	 * Time to the goal, xp rate, cost or profit, and the player's gold, as label/number rows.
	 */
	private void addSummary(ActiveGuide guide)
	{
		MethodOption option = guide.getOption();
		Method method = guide.getMethod();
		section("Progress to level " + guide.getTargetLevel());
		addTimeToGoal(guide);
		content.add(Box.createVerticalStrut(4));
		content.add(GuideUi.stat("XP to go", String.format("%,d", guide.getXpRemaining()), TEXT));
		content.add(GuideUi.stat("Actions", String.format("~%,d", option.getActions()), TEXT));
		long gpHr = option.gpPerHour();
		if (gpHr != 0)
		{
			content.add(GuideUi.stat(gpHr > 0 ? "Profit" : "Cost", "~" + Format.gp(Math.abs(gpHr)) + "/hr",
				gpHr > 0 ? GOOD : WARN));
			content.add(GuideUi.stat("Per xp", gpPerXp(option), MUTED));
		}
		boolean usesItems = !method.getInputs().isEmpty() || !method.getProducts().isEmpty();
		if (usesItems)
		{
			addGoalCost(guide);
		}
		PlayerState player = guide.getPlayer();
		if (player != null)
		{
			content.add(GuideUi.stat("Your GP", Format.gp(player.gp()), GOLD));
			if (!player.bankKnown())
			{
				content.add(GuideUi.text("Open your bank to include it.", MUTED, TEXT_WIDTH));
			}
		}
		if (guide.getPriceSource() != null && usesItems)
		{
			content.add(Box.createVerticalStrut(2));
			content.add(GuideUi.text(guide.getPriceSource(), MUTED, TEXT_WIDTH));
		}
	}

	private void addTimeToGoal(ActiveGuide guide)
	{
		MethodOption option = guide.getOption();
		double planHours = guide.getPlan().isEmpty()
			? option.hours(guide.getXpRemaining())
			: guide.getPlan().stream().mapToDouble(seg -> seg.getOption().hours(seg.getXp())).sum();
		JComponent eta = GuideUi.stat("Time left" + (option.isRun() ? " (play)" : ""), duration(planHours),
			Color.WHITE);
		content.add(eta);
		if (option.isRun())
		{
			content.add(GuideUi.stat("Runs", String.format("~%,d", option.runs(guide.getXpRemaining())), TEXT));
			content.add(GuideUi.stat("One every", duration(guide.getMethod().getCycleMinutes() / 60.0), TEXT));
		}
		content.add(GuideUi.stat("Guide rate", String.format("~%,d xp/hr", Math.round(option.getXpPerHour())),
			HINT));
		addSessionRate(guide);
	}

	/**
	 * The xp/hr the player is really getting this session, and the time to the goal if they keep it up.
	 */
	private void addSessionRate(ActiveGuide guide)
	{
		int rate = guide.getSessionXpPerHour();
		if (rate < 0)
		{
			content.add(GuideUi.stat("Your rate", "measuring...", MUTED));
		}
		else if (rate > 0)
		{
			content.add(GuideUi.stat("Your rate", String.format("~%,d xp/hr", rate), Color.WHITE));
			PlayerState player = guide.getPlayer();
			int next = guide.getLevel() + 1;
			if (player != null && next < guide.getTargetLevel() && next <= Experience.MAX_REAL_LEVEL)
			{
				long toNext = Experience.getXpForLevel(next) - (long) player.xp(guide.getSkill());
				content.add(GuideUi.stat("  to level " + next, duration((double) Math.max(0, toNext) / rate), TEXT));
			}
			content.add(GuideUi.stat("  to level " + guide.getTargetLevel(),
				duration((double) guide.getXpRemaining() / rate), TEXT));
		}
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
			content.add(GuideUi.stat("Whole goal costs", "~" + Format.gp(goalCost), WARN));
			content.add(GuideUi.text("Net, after selling what you make.", MUTED, TEXT_WIDTH));
		}
		else if (goalCost < 0)
		{
			content.add(GuideUi.stat("Whole goal earns", "~" + Format.gp(-goalCost), GOOD));
			content.add(GuideUi.text("After GE tax.", MUTED, TEXT_WIDTH));
		}
	}

	private void addCurrentStep(GuideStep step, boolean bankFilterOn)
	{
		content.add(Box.createVerticalStrut(2));
		JPanel stepBox = GuideUi.card(ColorScheme.BRAND_ORANGE);
		if (step != null)
		{
			List<String> lines = new ArrayList<>(step.getDetails());
			if (step.getDestinationName() != null)
			{
				lines.add("➜ Follow the arrow on the minimap or world map to " + step.getDestinationName() + ".");
			}
			GuideUi.fillStep(stepBox, "Current step", ColorScheme.BRAND_ORANGE, step.getTitle(), lines);
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
		boolean first = true;
		for (Requirement r : guide.getRequirements())
		{
			if (!first)
			{
				content.add(Box.createVerticalStrut(3));
			}
			first = false;
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
			boolean current = seg.getOption().getMethod() == guide.getMethod();
			content.add(GuideUi.html("<font color='" + GuideUi.hex(current ? GOLD : Color.WHITE) + "'>"
				+ seg.getFromLevel() + "–" + seg.getToLevel() + "</font>&nbsp; "
				+ GuideText.escape(seg.getOption().getMethod().getName())
				+ " <font color='" + GuideUi.hex(MUTED) + "'>" + duration(seg.getOption().hours(seg.getXp()))
				+ "</font>", current ? Color.WHITE : TEXT, TEXT_WIDTH));
			content.add(Box.createVerticalStrut(2));
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
		Color edge = current ? ColorScheme.BRAND_ORANGE : earns != null ? GOLD : null;
		row.setBorder(edge == null
			? new EmptyBorder(5, 6, 5, 6)
			: BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, edge),
			new EmptyBorder(5, 3, 5, 6)));

		int w = GuideUi.CARD_WIDTH;
		JLabel name = GuideUi.text(m.getName(), o.isAvailable() ? Color.WHITE : Color.GRAY, w);
		name.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		row.add(name);
		if (current)
		{
			row.add(GuideUi.text("Current method", ColorScheme.BRAND_ORANGE, w));
		}
		row.add(GuideUi.text(methodInfo(o), MUTED, w));
		row.add(Box.createVerticalStrut(2));
		row.add(GuideUi.stat("XP/hr", String.format("~%,d", Math.round(o.getXpPerHour())), TEXT));
		if (xpRemaining > 0)
		{
			row.add(GuideUi.stat("To level " + targetLevel, duration(o.hours(xpRemaining)), TEXT));
			if (o.isRun())
			{
				row.add(GuideUi.stat("Runs", String.format("%,d", o.runs(xpRemaining)), TEXT));
			}
		}
		long gpHr = o.gpPerHour();
		if (gpHr != 0)
		{
			row.add(GuideUi.stat(gpHr > 0 ? "Profit" : "Cost", Format.gp(Math.abs(gpHr)) + "/hr",
				gpHr > 0 ? GOOD : WARN));
		}
		if (earns != null)
		{
			row.add(Box.createVerticalStrut(2));
			row.add(GuideUi.line("★ Earns the " + earns.getName() + " (" + earns.getBonus() + ")", w));
		}
		if (o.isAvailable() && !o.isAffordable())
		{
			row.add(GuideUi.line("⚠ Can't afford the materials for your goal yet", WARN, w));
		}
		if (!o.isAvailable())
		{
			row.add(Box.createVerticalStrut(2));
			for (String blocker : o.getBlockers())
			{
				row.add(GuideUi.check(false, blocker, w));
			}
		}
		else if (!current)
		{
			row.add(left(useButton(m, row.getBackground())));
		}
		return full(row);
	}

	/**
	 * "Lvl 30 · Members · AFK, Profit".
	 */
	private static String methodInfo(MethodOption o)
	{
		Method m = o.getMethod();
		String tags = m.getTags().stream().map(MethodTag::getLabel).collect(Collectors.joining(", "));
		return String.format("Lvl %d%s%s", m.getMinLevel(), m.isMembers() ? " · Members" : " · F2P",
			tags.isEmpty() ? "" : " · " + tags);
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

	private JComponent requirementLine(Requirement r)
	{
		switch (r.getStatus())
		{
			case READY:
				return GuideUi.line("✔ " + r.getText(), GOOD, TEXT, TEXT_WIDTH);
			case IN_BANK:
				return GuideUi.line("▣ " + r.getText(), WARN, TEXT, TEXT_WIDTH);
			case MISSING:
				return GuideUi.line("✘ " + r.getText(), BAD, TEXT, TEXT_WIDTH);
			case OPTIONAL:
				return GuideUi.line("☆ " + r.getText(), HINT, TEXT, TEXT_WIDTH);
			case SUGGESTED:
				return GuideUi.line("+ " + r.getText(), HINT, TEXT, TEXT_WIDTH);
			case UNLOCK:
				return GuideUi.line("★ " + r.getText(), GOLD, TEXT, TEXT_WIDTH);
			default:
				return GuideUi.line("ℹ " + r.getText(), MUTED, TEXT, TEXT_WIDTH);
		}
	}

	// ---- Building blocks ----

	private void section(String title)
	{
		content.add(GuideUi.section(title));
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
			.append(g.getPriceSource()).append('|').append(g.getSessionXpPerHour()).append('|')
			.append(g.getActivity()).append(g.getActivityInfo()).append(g.getWeeklyState()).append(g.getWeeklyStatus());
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
