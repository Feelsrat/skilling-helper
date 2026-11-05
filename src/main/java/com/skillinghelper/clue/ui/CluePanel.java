package com.skillinghelper.clue.ui;

import com.skillinghelper.clue.ClueHoldings;
import com.skillinghelper.clue.ClueRewards;
import com.skillinghelper.clue.ClueView;
import com.skillinghelper.clue.data.ClueData;
import com.skillinghelper.clue.data.ClueItem;
import com.skillinghelper.clue.data.ClueTier;
import com.skillinghelper.clue.data.ClueWays;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.engine.Format;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.HiddenRedraws;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: the clues, scroll boxes and caskets you have; per tier how to get one (click to be guided), what to
 * bring, what the caskets can give, its STASH units and every other drop source.
 */
public class CluePanel extends PluginPanel
{
	private static final int WIDTH = GuideUi.WIDTH;
	private static final int CARD = GuideUi.CARD_WIDTH;
	private static final int DROPS_SHOWN = 12;

	public interface Listener
	{
		void onSelectTier(ClueTier tier);

		void onStartGuide();

		void onStopGuide();

		void onPickWay(String key);

		void onPickStash(String key);

		void onAllowWilderness(boolean allow);

		void onTurnOnCluePlugin();

		void onOpenWiki();
	}

	private final Listener listener;
	private final HiddenRedraws redraws;
	private final JButton[] tierButtons = new JButton[ClueTier.values().length];
	private final JCheckBox wilderness = new JCheckBox(Wilderness.TOGGLE_LABEL);
	private final JPanel body = new JPanel();
	/**
	 * Reference sections the player unfolded.
	 */
	private final Set<String> openSections = new HashSet<>();

	private ClueView view;
	private boolean updating;

	public CluePanel(Listener listener)
	{
		super(false);
		this.listener = listener;
		redraws = new HiddenRedraws(this);
		JPanel header = GuideUi.header("Clue Scrolls", "Get, carry and finish clues of every tier.");
		JPanel tiers = new JPanel(new GridLayout(2, 3, 3, 3));
		tiers.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (ClueTier t : ClueTier.values())
		{
			JButton b = new JButton(t.getLabel());
			b.setFont(FontManager.getRunescapeSmallFont());
			b.setMargin(new Insets(2, 1, 2, 1));
			b.addActionListener(e -> listener.onSelectTier(t));
			tierButtons[t.ordinal()] = b;
			tiers.add(b);
		}
		header.add(GuideUi.full(tiers));
		header.add(Box.createVerticalStrut(2));
		header.add(GuideUi.wilderness(wilderness, "Off: Wilderness ways and STASH units are skipped.", () ->
		{
			if (!updating)
			{
				listener.onAllowWilderness(wilderness.isSelected());
			}
		}));
		GuideUi.frame(this, header, body);
		body.add(GuideUi.empty("Not logged in", "Log in to see your clues."));
	}

	/**
	 * Shows the latest view. Swing thread.
	 */
	public void update(ClueView view)
	{
		redraws.run("view", () -> apply(view));
	}

	private void apply(ClueView v)
	{
		view = v;
		updating = true;
		wilderness.setSelected(v.isWildernessAllowed());
		updating = false;
		for (ClueTier t : ClueTier.values())
		{
			JButton b = tierButtons[t.ordinal()];
			int have = have(v.getHoldings(), t);
			b.setText(t.getLabel() + (have > 0 ? " (" + have + ")" : ""));
			GuideUi.select(b, t == v.getTier(), have > 0 ? GuideUi.GOLD : GuideUi.TEXT);
		}
		rebuild();
	}

	private static int have(ClueHoldings h, ClueTier t)
	{
		int n = 0;
		for (ClueItem.Kind k : ClueItem.Kind.values())
		{
			n += h.get(t, k).total();
		}
		return n;
	}

	private void rebuild()
	{
		body.removeAll();
		ClueView v = view;
		if (v == null || !v.isLoggedIn())
		{
			body.add(GuideUi.empty("Not logged in", "Log in to see your clues."));
			finish();
			return;
		}
		ClueTier tier = v.getTier();

		if (!v.isCluePluginOn())
		{
			body.add(GuideUi.line("⚠ RuneLite's Clue Scroll plugin is off. It shows where every clue step is and what to "
				+ "do there; this guide relies on it.", GuideUi.WARN, GuideUi.TEXT, WIDTH));
			body.add(Box.createVerticalStrut(3));
			body.add(GuideUi.button("Turn on the Clue Scroll plugin", listener::onTurnOnCluePlugin));
			body.add(Box.createVerticalStrut(6));
		}

		yourClues(v);

		body.add(GuideUi.section(tier.getLabel() + " clues"));
		body.add(GuideUi.line(tier.getSteps() + (tier.isMembers() ? ", members only" : ", works on free worlds")
			+ (tier.isWildernessSteps() ? "" : ", never in the Wilderness"), GuideUi.TEXT, WIDTH));
		if (v.getCompleted() > 0)
		{
			body.add(GuideUi.line("✔ You've done " + String.format("%,d", v.getCompleted()) + " " + tier.getLabel()
				.toLowerCase() + " clues", GuideUi.GOOD, WIDTH));
		}
		if (tier.isMembers() && !v.isMembers())
		{
			body.add(GuideUi.line("✘ You're on a free world: these clues need a members world.", GuideUi.BAD, WIDTH));
		}
		body.add(Box.createVerticalStrut(8));
		DiaryStep step = v.getStep();
		JButton wiki = GuideUi.small("Wiki", listener::onOpenWiki);
		if (v.isRunning() && step != null)
		{
			body.add(GuideUi.stepCard(step.getKind() == DiaryStep.Kind.LOCKED ? GuideUi.WARN : GuideUi.HINT, "Now",
				step.getTitle(), step.getLines(), listener::onStopGuide, wiki));
		}
		else if (v.isRunning())
		{
			body.add(GuideUi.actions(GuideUi.small("Stop guide", listener::onStopGuide), wiki));
		}
		else
		{
			body.add(GuideUi.primary("▶ Start guide", listener::onStartGuide));
			body.add(Box.createVerticalStrut(3));
			body.add(GuideUi.actions(wiki));
		}

		getOne(v);
		bring(v);
		rewards(v);
		stash(v);
		drops(v);
		GuideUi.foldSections(body, t -> t.equals("Rewards") || t.equals("STASH units") || t.equals("Best drop rates"),
			openSections);
		finish();
	}

	private void yourClues(ClueView v)
	{
		ClueHoldings h = v.getHoldings();
		body.add(GuideUi.section("Your clues"));
		if (!h.isBankKnown())
		{
			body.add(GuideUi.text("Open your bank once so the guide can see the clues and caskets in it.",
				GuideUi.WARN, WIDTH));
			body.add(Box.createVerticalStrut(3));
		}
		boolean any = false;
		for (ClueTier t : ClueTier.values())
		{
			List<String> parts = new ArrayList<>();
			ClueHoldings.Stack clue = h.get(t, ClueItem.Kind.CLUE);
			if (clue.total() > 0)
			{
				parts.add("clue " + where(clue));
			}
			ClueHoldings.Stack boxes = h.get(t, ClueItem.Kind.SCROLL_BOX);
			if (boxes.total() > 0)
			{
				parts.add(count(boxes.total(), "scroll box", "scroll boxes") + " " + where(boxes));
			}
			ClueHoldings.Stack containers = h.get(t, ClueItem.Kind.CONTAINER);
			if (containers.total() > 0)
			{
				parts.add(count(containers.total(), "bottle/nest/geode", "bottles/nests/geodes") + " " + where(containers));
			}
			ClueHoldings.Stack caskets = h.get(t, ClueItem.Kind.REWARD_CASKET);
			if (caskets.total() > 0)
			{
				parts.add(count(caskets.total(), "reward casket", "reward caskets") + " " + where(caskets));
			}
			if (h.get(t, ClueItem.Kind.MIMIC).total() > 0)
			{
				parts.add("a Mimic casket");
			}
			if (parts.isEmpty())
			{
				continue;
			}
			any = true;
			JComponent row = GuideUi.line("• " + t.getLabel() + ": " + String.join(", ", parts), GuideUi.GOLD,
				GuideUi.TEXT, WIDTH);
			clickable(row, "Show " + t.getLabel() + " clues", () -> listener.onSelectTier(t));
			body.add(row);
			if (clue.total() > 0 && boxes.total() >= 2)
			{
				body.add(GuideUi.line("⚠ You can only hold one " + t.getLabel().toLowerCase() + " clue, and scroll boxes "
					+ "stack to 2 (up to 5 with scroll cases). At your limit, new " + t.getLabel().toLowerCase()
					+ " clue drops are lost: do this clue soon.", GuideUi.WARN, GuideUi.TEXT, WIDTH));
			}
		}
		if (!any)
		{
			body.add(GuideUi.text(h.isBankKnown() ? "None yet. Pick a tier to see how to get one."
				: "None carried.", GuideUi.MUTED, WIDTH));
		}
	}

	private void getOne(ClueView v)
	{
		ClueTier tier = v.getTier();
		body.add(GuideUi.section("How to get one"));
		if (!v.getWays().isEmpty())
		{
			body.add(GuideUi.hint("Click a way to be guided through it. The guide picks the first one you can do."));
		}
		for (ClueView.WayRow w : v.getWays())
		{
			JPanel row = column();
			row.setOpaque(true);
			row.setBackground(w.isCurrent() ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			row.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
			boolean ready = w.getMissing().isEmpty();
			row.add(GuideUi.line((w.isCurrent() ? "▶ " : ready ? "✔ " : "✘ ") + w.getName(),
				w.isCurrent() ? GuideUi.OPTION : ready ? GuideUi.GOOD : GuideUi.BAD, CARD));
			for (String m : w.getMissing())
			{
				row.add(GuideUi.check(false, m, CARD));
			}
			if (w.getCost() > 0)
			{
				row.add(GuideUi.text("Costs ~" + Format.gp(w.getCost()) + " per clue on average", GuideUi.GOLD, CARD - 12));
			}
			if (w.isWilderness())
			{
				row.add(GuideUi.line(Wilderness.TAG, Wilderness.COLOR, CARD));
			}
			clickable(row, "Guide me through this", () -> listener.onPickWay(w.getKey()));
			body.add(GuideUi.full(row));
			body.add(Box.createVerticalStrut(3));
		}
		for (String tip : ClueWays.tips(tier))
		{
			body.add(Box.createVerticalStrut(2));
			body.add(GuideUi.line("• " + tip, WIDTH));
		}
	}

	private void bring(ClueView v)
	{
		ClueTier tier = v.getTier();
		body.add(GuideUi.section("What to bring"));
		for (ClueView.KitRow k : v.getKit())
		{
			body.add(GuideUi.check(k.isOk(), k.getLabel() + (k.isOk() ? "" : " (you don't have one)"), WIDTH));
		}
		if (tier.ordinal() >= ClueTier.HARD.ordinal())
		{
			body.add(GuideUi.line("• Weapon, armour and food for the fights some steps start.", WIDTH));
		}
		body.add(GuideUi.line("• Teleports save a lot of walking: the guide's travel tips and Shortest Path use the ones "
			+ "you carry.", WIDTH));
		body.add(Box.createVerticalStrut(4));
		body.add(GuideUi.subheading("What the steps are like"));
		for (String s : tier.getWhatToExpect())
		{
			body.add(Box.createVerticalStrut(2));
			body.add(GuideUi.line("• " + s, WIDTH));
		}
	}

	private void rewards(ClueView v)
	{
		ClueTier tier = v.getTier();
		body.add(GuideUi.section("Rewards"));
		ClueData.Casket casket = ClueData.casket(tier);
		if (casket != null)
		{
			body.add(GuideUi.line(casket.getMinRolls() + " to " + casket.getMaxRolls() + " items per casket", GuideUi.GOLD,
				WIDTH));
		}
		if (v.getAverageCasket() > 0)
		{
			body.add(GuideUi.line("Worth about " + Format.gp(v.getAverageCasket()) + " on average at today's Grand "
				+ "Exchange prices (untradeable items count as nothing)", GuideUi.TEXT, WIDTH));
		}
		if (tier.getMasterChance() != null)
		{
			body.add(GuideUi.line("• " + tier.getMasterChance() + " chance of a master clue in every casket", WIDTH));
		}
		body.add(Box.createVerticalStrut(4));
		body.add(GuideUi.subheading("What it can give"));
		for (String s : tier.getRewardKinds())
		{
			body.add(Box.createVerticalStrut(2));
			body.add(GuideUi.line("• " + s, WIDTH));
		}
		if (!v.getTopUniques().isEmpty())
		{
			body.add(Box.createVerticalStrut(4));
			body.add(GuideUi.subheading("Most valuable right now"));
			for (ClueRewards.Pick pick : v.getTopUniques())
			{
				body.add(GuideUi.line("• " + pick.getName() + ": " + Format.gp(pick.getPrice()) + " (" + pick.getRarity()
					+ " per item rolled)", WIDTH));
			}
		}
		body.add(Box.createVerticalStrut(4));
		body.add(GuideUi.line("• " + tier.getMilestone(), GuideUi.GOLD, GuideUi.TEXT, WIDTH));
		body.add(GuideUi.line("• Milestones also give a minor and a major scroll case: each lets you stack one "
			+ "more scroll box of this tier.", WIDTH));
	}

	private void stash(ClueView v)
	{
		ClueTier tier = v.getTier();
		List<ClueView.StashRow> rows = v.getStashes();
		body.add(GuideUi.section("STASH units"));
		body.add(GuideUi.text("A STASH unit holds the items an emote clue asks for, right where the clue sends you, so "
			+ "you don't carry them around.", GuideUi.MUTED, WIDTH));
		body.add(Box.createVerticalStrut(3));
		body.add(GuideUi.check(v.getConstructionLevel() >= tier.getStashLevel(), tier.getStashLevel()
			+ " Construction to build (you have " + v.getConstructionLevel() + ", boosts work)", WIDTH));
		body.add(GuideUi.line("• Each needs 2 " + tier.getPlankName().toLowerCase() + "s, 10 nails"
			+ (tier.getGoldLeaf() > 0 ? ", a gold leaf" : "") + ", a hammer and a saw. All " + rows.size() + ": "
			+ rows.size() * 2 + " planks and " + rows.size() * 10 + " nails.", WIDTH));
		body.add(GuideUi.line("• The S.T.A.S.H. chart in your house study, or the noticeboard in Watson's house, shows which "
			+ "you've built and filled.", WIDTH));
		body.add(Box.createVerticalStrut(3));
		body.add(GuideUi.text("Click one to be guided there. ✔ = you own every item for it.", GuideUi.MUTED, WIDTH));
		body.add(Box.createVerticalStrut(3));
		for (ClueView.StashRow s : rows)
		{
			if (s.isWilderness() && !v.isWildernessAllowed())
			{
				continue;
			}
			JPanel row = column();
			row.setOpaque(true);
			row.setBackground(s.isCurrent() ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			row.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
			String mark = s.getOwnAll() == null ? "• " : s.getOwnAll() ? "✔ " : "✘ ";
			Color c = s.getOwnAll() == null ? GuideUi.TEXT : s.getOwnAll() ? GuideUi.GOOD : GuideUi.MUTED;
			row.add(GuideUi.line((s.isCurrent() ? "▶ " : mark) + s.getName(), s.isCurrent() ? GuideUi.OPTION : c, CARD));
			row.add(GuideUi.text(String.join(", ", s.getItems()), GuideUi.MUTED, CARD - 12));
			if (s.isWilderness())
			{
				row.add(GuideUi.line(Wilderness.TAG, Wilderness.COLOR, CARD));
			}
			clickable(row, s.getWhere(), () -> listener.onPickStash(s.getKey()));
			body.add(GuideUi.full(row));
			body.add(Box.createVerticalStrut(2));
		}
	}

	private void drops(ClueView v)
	{
		ClueTier tier = v.getTier();
		List<ClueData.Drop> drops = ClueData.drops(tier);
		if (drops.isEmpty())
		{
			return;
		}
		body.add(GuideUi.section("Best drop rates"));
		body.add(GuideUi.text("From the wiki, per kill, pickpocket or loot. " + drops.size() + " sources in all.",
			GuideUi.MUTED, WIDTH));
		body.add(Box.createVerticalStrut(3));
		int shown = 0;
		for (ClueData.Drop d : drops)
		{
			if (shown >= DROPS_SHOWN)
			{
				break;
			}
			if (d.isWilderness() && !v.isWildernessAllowed())
			{
				continue;
			}
			String level = d.getLevel().isEmpty() ? "" : ("combat".equals(d.getType()) ? ", level " : ", "
				+ d.getType() + " ") + d.getLevel();
			body.add(GuideUi.line("• " + d.getSource() + ": " + d.getRarity() + level
				+ (d.isWilderness() ? "  " + Wilderness.TAG : ""), WIDTH));
			shown++;
		}
	}

	private static String where(ClueHoldings.Stack s)
	{
		if (s.getCarried() > 0 && s.getBanked() > 0)
		{
			return "(carried and banked)";
		}
		return s.getCarried() > 0 ? "(carried)" : "(bank)";
	}

	private static String count(int n, String one, String many)
	{
		return n == 1 ? "1 " + one : n + " " + many;
	}

	private static void clickable(JComponent row, String tip, Runnable action)
	{
		GuideUi.clickable(row, tip, () -> action.run());
	}

	private void finish()
	{
		body.revalidate();
		body.repaint();
	}

	private static JPanel column()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(ColorScheme.DARK_GRAY_COLOR);
		p.setAlignmentX(LEFT_ALIGNMENT);
		return p;
	}
}
