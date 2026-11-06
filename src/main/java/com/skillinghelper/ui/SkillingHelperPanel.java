package com.skillinghelper.ui;

import com.skillinghelper.ActiveGuide;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.SkillOutfit;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.data.Wilderness;
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
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
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
		BEST_GOLD("Best gold/hr", "Lists the guides that earn the most gold per hour with your current stats"),
		FASTEST_LEVELS("Fastest +10 levels",
			"Lists the guides where you'd gain 10 levels the quickest with your current stats, gold and pace"),
		MOST_AFK("Most AFK", "Lists the guides that need the fewest clicks per hour, leaving out ones you've outleveled"),
		LOWEST_SKILL("Lowest skill", "Lists your skills from the lowest level up"),
		TEARS_OF_GUTHIX("Tears of Guthix (weekly)",
			"Opens the guide to the weekly Tears of Guthix minigame, which gives xp in your lowest skill");

		private final String label;
		private final String description;
	}

	/**
	 * Skills in the order of the game's skills tab, three to a row.
	 */
	public static final List<Skill> GRID_ORDER = List.of(
		Skill.ATTACK, Skill.HITPOINTS, Skill.MINING,
		Skill.STRENGTH, Skill.AGILITY, Skill.SMITHING,
		Skill.DEFENCE, Skill.HERBLORE, Skill.FISHING,
		Skill.RANGED, Skill.THIEVING, Skill.COOKING,
		Skill.PRAYER, Skill.CRAFTING, Skill.FIREMAKING,
		Skill.MAGIC, Skill.FLETCHING, Skill.WOODCUTTING,
		Skill.RUNECRAFT, Skill.SLAYER, Skill.FARMING,
		Skill.CONSTRUCTION, Skill.HUNTER, Skill.SAILING);

	public interface Listener
	{
		void onSkillSelected(Skill skill);

		/**
		 * Let the plugin choose the skill (and for gold, the method) to train.
		 */
		void onQuickPick(QuickPick pick);

		/**
		 * Open one guide from a quick pick's ranked list.
		 */
		void onPickChosen(PickList.Row row);

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

		/**
		 * Opens a link in the browser, e.g. the Tears of Guthix calendar reminder.
		 */
		void onOpenLink(String url);

		/**
		 * Let every guide pick Wilderness methods, or leave them out again.
		 */
		void onAllowWilderness(boolean allow);

		/**
		 * Switches the shared "Hide outleveled guides" setting.
		 */
		void onHideOutleveled(boolean hide);
	}

	private final Listener listener;
	private final SkillIconManager skillIcons;
	private final JComboBox<Object> skillBox = new JComboBox<>();
	private final SpinnerNumberModel targetModel = new SpinnerNumberModel(2, 2, Experience.MAX_REAL_LEVEL, 1);
	private final JSpinner targetSpinner = new JSpinner(targetModel);
	private final JComboBox<TrainingPreference> prefBox = new JComboBox<>(TrainingPreference.values());
	private final JLabel levelLabel = new JLabel();
	private final JPanel goalRow = new JPanel();
	private final JPanel content = new JPanel();
	private boolean updating;
	private final Set<String> openSections = new HashSet<>();
	private String lastSignature;

	/**
	 * Holds back redraws while the panel is closed.
	 */
	private final HiddenRedraws redraws;

	public SkillingHelperPanel(Listener listener, SkillIconManager skillIcons)
	{
		super(false);
		redraws = new HiddenRedraws(this);
		this.listener = listener;
		this.skillIcons = skillIcons;
		GuideUi.frame(this, header(), content);
		showIntro(ActiveGuide.NONE);
	}

	// ---- Layout ----

	private JPanel header()
	{
		JPanel header = GuideUi.header("Skilling Helper", "/com/skillinghelper/icon.png");
		header.add(full(skillRow()));
		header.add(Box.createVerticalStrut(4));
		buildGoalRow();
		goalRow.setVisible(false);
		header.add(full(goalRow));
		header.add(Box.createVerticalStrut(4));
		GuideUi.options(header, full(styleRow()));
		return header;
	}

	private JPanel skillRow()
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(Skin.BG);
		row.add(skillPicker(), BorderLayout.CENTER);
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

	private void buildGoalRow()
	{
		JPanel row = goalRow;
		row.setLayout(new BorderLayout(4, 0));
		row.setBackground(Skin.BG);
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
		JButton reset = GuideUi.small("Reset", listener::onTargetReset);
		reset.setToolTipText("Reset the goal to your level plus the default offset");
		row.add(reset, BorderLayout.EAST);
	}

	private JPanel styleRow()
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
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

	// ---- Updating ----

	/**
	 * Refreshes the panel. Must be called on the Swing thread.
	 */
	public void update(ActiveGuide guide, TrainingPreference preference, boolean bankFilterOn)
	{
		redraws.run("update", () -> applyUpdate(guide, preference, bankFilterOn));
	}

	private void applyUpdate(ActiveGuide guide, TrainingPreference preference, boolean bankFilterOn)
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
		else if (guide.getPicks() != null)
		{
			showPicks(guide.getPicks());
		}
		else if (!guide.isActive())
		{
			showIntro(guide);
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
			Object selected = guide.getActivity() != null ? QuickPick.TEARS_OF_GUTHIX
				: guide.getPicks() != null ? guide.getPicks().getPick() : guide.getSkill();
			if (!Objects.equals(skillBox.getSelectedItem(), selected))
			{
				skillBox.setSelectedItem(selected);
			}
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
			// The goal only means something once a skill is picked
			goalRow.setVisible(guide.getSkill() != null);
		}
		finally
		{
			updating = false;
		}
	}

	/**
	 * No guide open: one click on a skill (or a quick pick) starts its guide.
	 */
	private void showIntro(ActiveGuide guide)
	{
		content.removeAll();
		content.add(GuideUi.subheading("Train a skill"));
		JPanel grid = new JPanel(new GridLayout(0, 3, 3, 3));
		grid.setOpaque(false);
		for (Skill s : GRID_ORDER)
		{
			grid.add(skillCell(s, guide.getLevels().get(s)));
		}
		content.add(full(grid));
		content.add(Box.createVerticalStrut(4));
		content.add(GuideUi.subheading("Or let it choose"));
		JButton[] picks = new JButton[4];
		QuickPick[] quick = {QuickPick.BEST_GOLD, QuickPick.FASTEST_LEVELS, QuickPick.MOST_AFK, QuickPick.LOWEST_SKILL};
		for (int i = 0; i < quick.length; i++)
		{
			QuickPick pick = quick[i];
			picks[i] = GuideUi.small(pick.getLabel(), () ->
			{
				lastSignature = null;
				listener.onQuickPick(pick);
			});
			picks[i].setToolTipText(pick.getDescription());
		}
		content.add(GuideUi.actions(picks));
		content.add(Box.createVerticalStrut(8));
		content.add(GuideUi.line("It adapts to your level, items, bank and gold, and highlights what to click.",
			MUTED, TEXT_WIDTH));
		content.add(Box.createVerticalStrut(3));
		content.add(GuideUi.line("➜ Open your bank once so the guide knows what you own.", HINT, TEXT, TEXT_WIDTH));
	}

	/**
	 * A skill's icon and level, like the game's skills tab; clicking it opens the skill's guide.
	 */
	private JComponent skillCell(Skill s, Integer level)
	{
		JPanel cell = new Skin.Surface(new BorderLayout(4, 0), 8).edge(null);
		cell.setBorder(new EmptyBorder(4, 5, 4, 6));
		JLabel icon = new JLabel(new ImageIcon(skillIcons.getSkillImage(s, true)));
		cell.add(icon, BorderLayout.WEST);
		JLabel lvl = new JLabel(level == null || level == 0 ? "" : String.valueOf(level));
		lvl.setFont(FontManager.getRunescapeSmallFont());
		lvl.setForeground(level != null && level >= Experience.MAX_REAL_LEVEL ? GOLD : Color.WHITE);
		lvl.setHorizontalAlignment(SwingConstants.RIGHT);
		cell.add(lvl, BorderLayout.CENTER);
		return GuideUi.clickable(cell, "Train " + s.getName(), () -> listener.onSkillSelected(s));
	}

	/**
	 * A quick pick's ranked guides, each with its skill icon; clicking one opens it.
	 */
	private void showPicks(PickList picks)
	{
		section(picks.getPick().getLabel());
		content.add(GuideUi.line(picks.getNote(), MUTED, TEXT_WIDTH));
		content.add(Box.createVerticalStrut(6));
		if (picks.getRows().isEmpty())
		{
			content.add(GuideUi.line("⚠ " + picks.getEmptyText(), WARN, TEXT, TEXT_WIDTH));
			return;
		}
		int rank = 1;
		for (PickList.Row row : picks.getRows())
		{
			content.add(pickRow(rank++, row));
			content.add(Box.createVerticalStrut(4));
		}
	}

	private JPanel pickRow(int rank, PickList.Row r)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(Skin.CARD);
		row.setBorder(new EmptyBorder(5, 6, 5, 6));
		JLabel icon = new JLabel(new ImageIcon(skillIcons.getSkillImage(r.getSkill(), true)));
		icon.setVerticalAlignment(SwingConstants.TOP);
		row.add(icon, BorderLayout.WEST);

		int w = GuideUi.CARD_WIDTH - 24;
		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		JLabel title = GuideUi.text(rank + ". " + r.getTitle(), Color.WHITE, w);
		title.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		text.add(title);
		text.add(GuideUi.text(r.getValue(), GOLD, w));
		if (r.getDetail() != null && !r.getDetail().isEmpty())
		{
			text.add(GuideUi.text(r.getDetail(), MUTED, w));
		}
		row.add(text, BorderLayout.CENTER);
		row.setToolTipText("Open the " + r.getSkill().getName() + " guide" + (r.getMethodId() != null
			? " with this method" : ""));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				lastSignature = null;
				listener.onPickChosen(r);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(Skin.CARD_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(Skin.CARD);
			}
		});
		return full(row);
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
		String url = guide.getWeeklyCalendarUrl();
		content.add(GuideUi.actions(GuideUi.small("Open guide", () -> listener.onQuickPick(QuickPick.TEARS_OF_GUTHIX)),
			url == null ? null : GuideUi.small("Remind me", () -> listener.onOpenLink(url))));
	}

	/**
	 * While Tears of Guthix is on cooldown: a button that opens a Google Calendar event for when it's up.
	 */
	private void addCalendarButton(ActiveGuide guide)
	{
		String url = guide.getWeeklyCalendarUrl();
		if (url == null)
		{
			return;
		}
		content.add(Box.createVerticalStrut(4));
		JButton remind = GuideUi.small("Remind me (Google Calendar)", () -> listener.onOpenLink(url));
		content.add(GuideUi.actions(remind));
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
			addCalendarButton(guide);
			content.add(Box.createVerticalStrut(6));
		}
		GuideUi.lines(content, guide.getActivityInfo(), TEXT_WIDTH);
		if (guide.getWikiPage() != null)
		{
			content.add(Box.createVerticalStrut(6));
			content.add(GuideUi.actions(GuideUi.small("Open wiki guide",
				() -> listener.onOpenWiki(guide.getWikiPage()))));
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
		content.add(GuideUi.actions(GuideUi.small("Open wiki training guide",
			() -> listener.onOpenWiki(guide.getWikiPage()))));
	}

	private void showGuide(ActiveGuide guide, boolean bankFilterOn)
	{
		Method method = guide.getMethod();
		addCurrentStep(guide.getStep(), bankFilterOn);
		MethodOption o = guide.getOption();
		if (method != null && o != null)
		{
			// The one line most people want: what they're doing and how long it takes
			content.add(Box.createVerticalStrut(6));
			content.add(GuideUi.stat(method.getName(), guide.getXpRemaining() > 0
				? duration(o.hours(guide.getXpRemaining())) + " to " + guide.getTargetLevel() : "", TEXT));
			content.add(GuideUi.stat("XP/hr", String.format("~%,d", Math.round(o.getXpPerHour())), HINT));
		}
		content.add(Box.createVerticalStrut(4));
		content.add(GuideUi.more());
		if (method != null && o != null)
		{
			addSummary(guide);
		}
		addRequirements(guide);
		addPlan(guide);
		addMethods(guide);
		if (method != null && method.getWikiPage() != null)
		{
			content.add(Box.createVerticalStrut(6));
			content.add(GuideUi.actions(GuideUi.small("Open wiki guide",
				() -> listener.onOpenWiki(method.getWikiPage()))));
		}
		GuideUi.foldSections(content, GuideUi.MORE::equals, openSections);
	}

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
		content.add(GuideUi.stat("Clicks", String.format("~%,d/hr", guide.getMethod().getClicksPerHour()), HINT));
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
			GuideUi.fillStep(stepBox, "Now", ColorScheme.BRAND_ORANGE, step.getTitle(), lines);
			GuideUi.addStop(stepBox, "Now", ColorScheme.BRAND_ORANGE, listener::onStopGuide);
			if (step.getRoute() != null && step.getRoute().entersWilderness())
			{
				stepBox.add(Box.createVerticalStrut(4));
				stepBox.add(GuideUi.line(Wilderness.TAG + ": " + Wilderness.WARNING, Wilderness.COLOR,
					GuideUi.CARD_WIDTH));
			}
		}
		JButton bank = GuideUi.small(bankFilterOn ? "Show whole bank" : "Bank: only what I need",
			listener::onToggleBankFilter);
		bank.setToolTipText(bankFilterOn ? "Show every bank item again" : "Show only this guide's items in the bank");
		stepBox.add(Box.createVerticalStrut(6));
		stepBox.add(GuideUi.actions(bank));
		content.add(full(stepBox));
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
		addWildernessChoice(guide.getOptions());
		int outleveled = 0;
		for (MethodOption o : guide.getOptions())
		{
			if (isOutleveled(guide, o.getMethod()))
			{
				outleveled++;
				if (guide.isHideOutleveled())
				{
					continue;
				}
			}
			content.add(methodRow(o, o.getMethod() == guide.getMethod(), guide.getXpRemaining(),
				guide.getTargetLevel(), guide.getPlayer()));
			content.add(Box.createVerticalStrut(4));
		}
		if (outleveled > 0)
		{
			boolean hidden = guide.isHideOutleveled();
			content.add(GuideUi.line(hidden
				? outleveled + (outleveled == 1 ? " method you've outleveled is" : " methods you've outleveled are")
				+ " hidden: something better has opened up."
				: outleveled + (outleveled == 1 ? " method is" : " methods are") + " past your level range.",
				MUTED, TEXT_WIDTH));
			content.add(Box.createVerticalStrut(3));
			content.add(GuideUi.actions(GuideUi.small(hidden ? "Show outleveled methods" : "Hide outleveled methods",
				() -> listener.onHideOutleveled(!hidden))));
			content.add(Box.createVerticalStrut(4));
		}
	}

	/**
	 * Past the top of its level range, and not the method the guide is on or plans to use.
	 */
	private static boolean isOutleveled(ActiveGuide guide, Method m)
	{
		if (m == guide.getMethod() || guide.getPlan().stream().anyMatch(seg -> seg.getOption().getMethod() == m))
		{
			return false;
		}
		int combat = guide.getPlayer() == null ? 3 : guide.getPlayer().combatLevel();
		return m.isOutleveled(guide.getLevel(), combat);
	}

	/**
	 * When the skill has Wilderness methods: says whether the guide may pick them, with a switch to change it.
	 */
	private void addWildernessChoice(List<MethodOption> options)
	{
		long wild = options.stream().filter(o -> o.getMethod().isWilderness()).count();
		if (wild == 0)
		{
			return;
		}
		boolean skipped = options.stream().anyMatch(MethodOption::isWildernessSkipped);
		content.add(GuideUi.line(skipped
			? "☠ " + wild + (wild == 1 ? " method is" : " methods are") + " in the Wilderness, so the guide won't "
			+ "pick " + (wild == 1 ? "it" : "them") + " for you. You can still choose one below."
			: "☠ Wilderness methods can be picked (the setting is on for all guides).", Wilderness.COLOR,
			TEXT_WIDTH));
		content.add(Box.createVerticalStrut(3));
		content.add(GuideUi.actions(GuideUi.small(skipped ? "Include Wilderness methods"
			: "Leave out Wilderness methods",
			() -> listener.onAllowWilderness(skipped))));
		content.add(Box.createVerticalStrut(4));
	}

	private JPanel methodRow(MethodOption o, boolean current, long xpRemaining, int targetLevel, PlayerState player)
	{
		Method m = o.getMethod();
		SkillOutfit earns = unownedOutfitEarnedBy(m, player);
		Color edge = current ? Skin.ACCENT : earns != null ? GOLD : null;
		Skin.Surface row = new Skin.Surface(null, 10).accent(edge);
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setBackground(current ? Skin.mix(Skin.CARD, Skin.ACCENT, 0.08) : Skin.CARD);
		row.setBorder(new EmptyBorder(6, edge == null ? 8 : 10, 6, 7));

		int w = GuideUi.CARD_WIDTH;
		JLabel name = GuideUi.text(m.getName(), o.isAvailable() ? Color.WHITE : Color.GRAY, w);
		name.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		row.add(name);
		if (current)
		{
			row.add(GuideUi.text("Current method", ColorScheme.BRAND_ORANGE, w));
		}
		row.add(GuideUi.text(methodInfo(o), MUTED, w));
		if (m.isWilderness())
		{
			row.add(GuideUi.text(Wilderness.TAG + (o.isWildernessSkipped() ? " · only if you choose it" : ""),
				Wilderness.COLOR, w));
		}
		row.add(Box.createVerticalStrut(2));
		row.add(GuideUi.stat("XP/hr", String.format("~%,d", Math.round(o.getXpPerHour())), TEXT));
		if (!o.isAvailable())
		{
			// Locked: just what it gives and what's missing, so the ones you can use stand out
			for (String blocker : o.getBlockers())
			{
				row.add(GuideUi.check(false, blocker, w));
			}
			return full(row);
		}
		row.add(GuideUi.stat("Clicks/hr", String.format("~%,d", m.getClicksPerHour()), TEXT));
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
		if (!current)
		{
			row.add(Box.createVerticalStrut(4));
			row.add(GuideUi.actions(GuideUi.small("Use this method", () -> listener.onMethodChosen(m.getId()))));
		}
		return full(row);
	}

	/**
	 * "Lvl 30 → 60 · Members · AFK, Profit".
	 */
	private static String methodInfo(MethodOption o)
	{
		Method m = o.getMethod();
		String tags = m.getTags().stream().map(MethodTag::getLabel).collect(Collectors.joining(", "));
		return String.format("%s%s%s", m.levelRange(), m.isMembers() ? " · Members" : " · F2P",
			tags.isEmpty() ? "" : " · " + tags);
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
			.append(g.isHideOutleveled()).append('|').append(g.isForced()).append('|')
			.append(g.getMethod() == null ? "" : g.getMethod().getId()).append('|')
			.append(g.getPriceSource()).append('|').append(g.getSessionXpPerHour()).append('|')
			.append(g.getActivity()).append(g.getActivityInfo()).append(g.getWeeklyState()).append(g.getWeeklyStatus())
			.append(g.getLevels());
		if (g.getPicks() != null)
		{
			sb.append('|').append(g.getPicks());
		}
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
			sb.append('|').append(o.getMethod().getId()).append(o.isAvailable()).append(o.isWildernessSkipped())
				.append(o.gpPerHour() / 1000)
				.append(o.isAffordable()).append(Math.round(o.getXpPerHour() / 1000));
		}
		if (g.getPlayer() != null)
		{
			sb.append('|').append(g.getPlayer().gp() / 1000).append(g.getPlayer().bankKnown());
		}
		return sb.toString();
	}
}
