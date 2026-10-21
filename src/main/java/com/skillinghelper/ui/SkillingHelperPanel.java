package com.skillinghelper.ui;

import com.skillinghelper.ActiveGuide;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.SkillOutfit;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.PlanSegment;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.StepResolver;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
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

		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Skilling Helper");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(left(title));
		header.add(Box.createVerticalStrut(6));

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
					setIcon(new javax.swing.ImageIcon(skillIcons.getSkillImage(s, true)));
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
		header.add(full(skillBox));
		header.add(Box.createVerticalStrut(6));

		JPanel goalRow = new JPanel(new BorderLayout(4, 0));
		goalRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		levelLabel.setForeground(Color.WHITE);
		goalRow.add(levelLabel, BorderLayout.WEST);
		targetSpinner.addChangeListener(e ->
		{
			if (!updating)
			{
				listener.onTargetChanged(targetModel.getNumber().intValue());
			}
		});
		targetSpinner.setToolTipText("Target level");
		goalRow.add(targetSpinner, BorderLayout.CENTER);
		JButton reset = new JButton("Reset");
		reset.setToolTipText("Reset the goal to your level plus the default offset");
		reset.addActionListener(e -> listener.onTargetReset());
		goalRow.add(reset, BorderLayout.EAST);
		header.add(full(goalRow));
		header.add(Box.createVerticalStrut(6));

		JPanel prefRow = new JPanel(new BorderLayout(4, 0));
		prefRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel styleLabel = new JLabel("Style");
		styleLabel.setForeground(Color.WHITE);
		prefRow.add(styleLabel, BorderLayout.WEST);
		prefBox.addActionListener(e ->
		{
			if (!updating)
			{
				listener.onPreferenceChanged((TrainingPreference) prefBox.getSelectedItem());
			}
		});
		prefRow.add(prefBox, BorderLayout.CENTER);
		header.add(full(prefRow));
		header.add(Box.createVerticalStrut(8));

		add(header, BorderLayout.NORTH);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrap.add(content, BorderLayout.NORTH);
		javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(wrap);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(scroll, BorderLayout.CENTER);

		showIntro();
	}

	/**
	 * Refreshes the panel. Must be called on the Swing thread.
	 */
	public void update(ActiveGuide guide, TrainingPreference preference, boolean bankFilterOn)
	{
		String signature = signature(guide, preference, bankFilterOn);
		if (signature.equals(lastSignature))
		{
			return;
		}
		lastSignature = signature;

		updating = true;
		try
		{
			skillBox.setSelectedItem(guide.getSkill());
			prefBox.setSelectedItem(preference);
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

		content.removeAll();
		if (!guide.isActive())
		{
			showIntro();
		}
		else if (!guide.isHasGuide())
		{
			section("No built-in guide yet");
			content.add(text("There's no step-by-step guide for " + guide.getSkill().getName()
				+ " yet. The wiki training guide covers every method.", Color.LIGHT_GRAY));
			content.add(Box.createVerticalStrut(6));
			content.add(button("Open wiki training guide", () -> listener.onOpenWiki(guide.getWikiPage())));
		}
		else
		{
			buildGuide(guide, bankFilterOn);
		}
		content.revalidate();
		content.repaint();
	}

	private void showIntro()
	{
		content.removeAll();
		content.add(text("Pick a skill to get a step-by-step guide. It adapts to your level, items, bank and gold, "
			+ "and highlights what to click.", Color.LIGHT_GRAY));
		content.add(Box.createVerticalStrut(6));
		content.add(text("Open your bank once so the guide knows what you own.", HINT));
	}

	private void buildGuide(ActiveGuide guide, boolean bankFilterOn)
	{
		MethodOption option = guide.getOption();
		Method method = guide.getMethod();
		GuideStep step = guide.getStep();

		if (method != null && option != null)
		{
			double planHours = guide.getPlan().isEmpty()
				? option.hours(guide.getXpRemaining())
				: guide.getPlan().stream().mapToDouble(seg -> seg.getOption().hours(seg.getXp())).sum();
			JLabel eta = text("Time to level " + guide.getTargetLevel() + ": " + duration(planHours), Color.WHITE);
			eta.setFont(FontManager.getRunescapeBoldFont());
			content.add(eta);
			String progress = String.format("%,d xp to go · ~%,d actions with this method", guide.getXpRemaining(),
				option.getActions());
			content.add(text(progress, Color.LIGHT_GRAY));
			long gpHr = option.gpPerHour();
			if (gpHr != 0)
			{
				content.add(text((gpHr > 0 ? "Profit ~" : "Costs ~") + StepResolver.gp(Math.abs(gpHr)) + "/hr ("
					+ gpPerXp(option) + ")", gpHr > 0 ? GOOD : WARN));
			}
			if (!method.getInputs().isEmpty() || !method.getProducts().isEmpty())
			{
				long goalCost = Math.round(-option.getGpPerAction() * option.getActions());
				if (goalCost > 0)
				{
					content.add(text("Whole goal costs ~" + StepResolver.gp(goalCost) + " net (after selling products)",
						WARN));
				}
				else if (goalCost < 0)
				{
					content.add(text("Whole goal earns ~" + StepResolver.gp(-goalCost) + " after GE tax", GOOD));
				}
			}
			if (guide.getPlayer() != null)
			{
				content.add(text("Your GP: " + StepResolver.gp(guide.getPlayer().gp())
					+ (guide.getPlayer().bankKnown() ? "" : " (open your bank to include it)"), Color.LIGHT_GRAY));
			}
			if (guide.getPriceSource() != null && (!method.getInputs().isEmpty() || !method.getProducts().isEmpty()))
			{
				content.add(text(guide.getPriceSource(), HINT));
			}
		}

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

		if (!guide.getRequirements().isEmpty())
		{
			section("What you need");
			for (Requirement r : guide.getRequirements())
			{
				content.add(requirementLine(r));
			}
		}

		if (guide.getPlan().size() > 1 || (guide.getPlan().size() == 1 && !guide.isForced()))
		{
			section("Plan to level " + guide.getTargetLevel());
			for (PlanSegment seg : guide.getPlan())
			{
				content.add(text(String.format("%d–%d: %s (%s)", seg.getFromLevel(), seg.getToLevel(),
					seg.getOption().getMethod().getName(), duration(seg.getOption().hours(seg.getXp()))),
					seg.getOption().getMethod() == method ? Color.WHITE : Color.LIGHT_GRAY));
			}
		}

		section("Methods");
		if (guide.isForced())
		{
			content.add(button("Let the guide choose for me", () -> listener.onMethodChosen(null)));
			content.add(Box.createVerticalStrut(4));
		}
		for (MethodOption o : guide.getOptions())
		{
			content.add(methodRow(o, o.getMethod() == method, guide.getXpRemaining(), guide.getTargetLevel(),
				guide.getPlayer()));
			content.add(Box.createVerticalStrut(4));
		}

		if (method != null && method.getWikiPage() != null)
		{
			content.add(Box.createVerticalStrut(6));
			content.add(button("Open wiki guide", () -> listener.onOpenWiki(method.getWikiPage())));
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
		String tags = m.getTags().stream().map(MethodTag::getLabel).collect(Collectors.joining(", "));
		String info = String.format("Lvl %d · ~%dk xp/hr%s%s", m.getMinLevel(), m.getXpPerHour() / 1000,
			m.isMembers() ? " · Members" : "", tags.isEmpty() ? "" : " · " + tags);
		row.add(text(info, Color.LIGHT_GRAY));
		if (xpRemaining > 0)
		{
			row.add(text(duration(o.hours(xpRemaining)) + " to level " + targetLevel + " using only this",
				Color.LIGHT_GRAY));
		}
		if (earns != null)
		{
			row.add(text("★ Earns the " + earns.getName() + " (" + earns.getBonus() + "), which makes the rest of "
				+ "the grind faster", GOLD));
		}
		long gpHr = o.gpPerHour();
		if (gpHr != 0)
		{
			row.add(text((gpHr > 0 ? "+" : "-") + StepResolver.gp(Math.abs(gpHr)) + "/hr · " + gpPerXp(o),
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
			JButton use = new JButton("Use this method");
			use.setAlignmentX(Component.LEFT_ALIGNMENT);
			use.addActionListener(e -> listener.onMethodChosen(m.getId()));
			JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
			b.setBackground(row.getBackground());
			b.add(use);
			row.add(left(b));
		}
		return full(row);
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
		String icon;
		Color color;
		switch (r.getStatus())
		{
			case READY:
				icon = "✔ ";
				color = GOOD;
				break;
			case IN_BANK:
				icon = "▣ ";
				color = WARN;
				break;
			case MISSING:
				icon = "✖ ";
				color = BAD;
				break;
			case OPTIONAL:
				icon = "☆ ";
				color = HINT;
				break;
			case SUGGESTED:
				icon = "$ ";
				color = HINT;
				break;
			case UNLOCK:
				icon = "★ ";
				color = GOLD;
				break;
			default:
				icon = "ℹ ";
				color = Color.LIGHT_GRAY;
				break;
		}
		return text(icon + r.getText(), color);
	}

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

	private static <T extends javax.swing.JComponent> T left(T c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		return c;
	}

	private static <T extends javax.swing.JComponent> T full(T c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
		return c;
	}

	static String duration(double hours)
	{
		if (hours <= 0)
		{
			return "-";
		}
		long minutes = Math.round(hours * 60);
		if (minutes < 60)
		{
			return "~" + Math.max(1, minutes) + "m";
		}
		return String.format("~%dh %02dm", minutes / 60, minutes % 60);
	}

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
			sb.append('|').append(o.getMethod().getId()).append(o.isAvailable()).append(o.gpPerHour() / 1000).append(o.isAffordable());
		}
		if (g.getPlayer() != null)
		{
			sb.append('|').append(g.getPlayer().gp() / 1000).append(g.getPlayer().bankKnown());
		}
		return sb.toString();
	}
}
