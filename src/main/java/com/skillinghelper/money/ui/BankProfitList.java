package com.skillinghelper.money.ui;

import com.skillinghelper.banked.profit.BankProfit;
import com.skillinghelper.banked.profit.ProfitPick;
import com.skillinghelper.engine.Format;
import com.skillinghelper.ui.GuideUi;
import com.skillinghelper.ui.Skin;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;

/**
 * The "From my bank" list: things in the bank the player can process and sell for more, best total profit first,
 * then ones a few levels away. Swing thread only.
 */
class BankProfitList
{
	private static final int WIDTH = GuideUi.WIDTH - 15;

	private final JPanel list;
	private final Consumer<String> onStart;
	private final Consumer<String> onWiki;
	private BankProfit.Result result = BankProfit.Result.NONE;
	private boolean loggedIn;
	private boolean bankKnown;
	private boolean guideOn = true;
	private int soonLevels;
	private String selected;

	BankProfitList(JPanel list, Consumer<String> onStart, Consumer<String> onWiki)
	{
		this.list = list;
		this.onStart = onStart;
		this.onWiki = onWiki;
	}

	/**
	 * @param guideOn whether the Banked XP Guide plugin is on to run the step-by-step guide
	 */
	void update(BankProfit.Result result, boolean loggedIn, boolean bankKnown, boolean guideOn, int soonLevels)
	{
		this.result = result;
		this.loggedIn = loggedIn;
		this.bankKnown = bankKnown;
		this.guideOn = guideOn;
		this.soonLevels = soonLevels;
	}

	void rebuild()
	{
		list.removeAll();
		if (!loggedIn)
		{
			list.add(GuideUi.text("Log in to see what your bank can make.", GuideUi.MUTED, GuideUi.WIDTH));
			return;
		}
		if (!bankKnown)
		{
			list.add(GuideUi.text("Open your bank once so I can see what's in it.", GuideUi.WARN, GuideUi.WIDTH));
			return;
		}
		List<ProfitPick> now = result.getNow();
		if (now.isEmpty())
		{
			list.add(GuideUi.text("Nothing in your bank makes a profit to process at your levels right now.",
				GuideUi.MUTED, GuideUi.WIDTH));
		}
		else
		{
			list.add(GuideUi.text(String.format("%d thing%s in your bank sell for about %s more once processed. Each "
					+ "one is worth more than selling the items as they are:", now.size(), now.size() == 1 ? "" : "s",
				Format.gp(result.totalNow())), GuideUi.MUTED, GuideUi.WIDTH));
			list.add(Box.createVerticalStrut(4));
			for (ProfitPick p : now)
			{
				list.add(row(p));
				list.add(Box.createVerticalStrut(3));
			}
		}
		if (!result.getSoon().isEmpty())
		{
			list.add(Box.createVerticalStrut(6));
			list.add(GuideUi.section("Soon (up to " + soonLevels + " levels away)"));
			list.add(GuideUi.text("These would make more from the same items once you have the level or quest:",
				GuideUi.MUTED, GuideUi.WIDTH));
			list.add(Box.createVerticalStrut(4));
			for (ProfitPick p : result.getSoon())
			{
				list.add(row(p));
				list.add(Box.createVerticalStrut(3));
			}
		}
		list.add(Box.createVerticalStrut(6));
		list.add(GuideUi.text("Profit = what the products sell for after the 2% GE tax, minus what your items would "
			+ "sell for as they are and anything bought.", GuideUi.MUTED, GuideUi.WIDTH));
	}

	private JComponent row(ProfitPick p)
	{
		boolean open = p.getId().equals(selected);
		Color edge = p.isDoable() ? GuideUi.GOOD : GuideUi.WARN;
		Skin.Surface row = new Skin.Surface(null, 10).accent(edge);
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setBackground(open ? Skin.CARD_HOVER : Skin.CARD);
		row.setBorder(new EmptyBorder(6, 10, 6, 7));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel name = GuideUi.text(p.getName(), Color.WHITE, WIDTH);
		name.setFont(FontManager.getRunescapeBoldFont());
		row.add(name);
		row.add(GuideUi.text("+" + Format.gp(p.getProfit()) + " profit, ~" + Format.gp(p.getProfitPerHour())
			+ "/hr", GuideUi.GOOD, WIDTH));
		row.add(GuideUi.text(String.format("%,d %s, about %s", p.getProducts(),
			p.getRecipe().getProductName().toLowerCase(), Format.hours(p.getHours())), GuideUi.MUTED, WIDTH));
		if (!p.isDoable())
		{
			row.add(GuideUi.text(p.getLocked(), GuideUi.WARN, WIDTH));
		}
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				selected = Objects.equals(selected, p.getId()) ? null : p.getId();
				rebuild();
				list.revalidate();
				list.repaint();
			}
		});
		if (open)
		{
			detail(row, p);
		}
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	private void detail(JPanel box, ProfitPick p)
	{
		box.add(Box.createVerticalStrut(4));
		box.add(GuideUi.subheading("Uses"));
		GuideUi.lines(box, p.getInputs(), WIDTH);
		box.add(Box.createVerticalStrut(3));
		box.add(GuideUi.stat("Sells for", Format.gp(p.getProductEach()) + " each", GuideUi.BRIGHT));
		box.add(GuideUi.stat("Products", Format.gp(p.getProductValue()), GuideUi.TEXT));
		box.add(GuideUi.stat("Items + buys", Format.gp(p.getInputValue()), GuideUi.TEXT));
		box.add(GuideUi.stat("Profit", "+" + Format.gp(p.getProfit()), GuideUi.GOOD));
		for (String w : p.getWarnings())
		{
			box.add(GuideUi.line("⚠ " + w, GuideUi.WARN, WIDTH));
		}
		box.add(Box.createVerticalStrut(4));
		box.add(GuideUi.subheading("What the guide does"));
		GuideUi.lines(box, List.of(
			"1. Takes the " + p.getRecipe().getActivity().getItemName().toLowerCase() + " out of the bank, a "
				+ "trip at a time" + (p.getBuyCost() > 0 ? ", after buying what's missing on the GE" : ""),
			"2. Shows where to go and what to click to " + lowerFirst(p.getName()),
			"3. Takes the " + p.getRecipe().getProductName().toLowerCase() + " to the Grand Exchange and walks "
				+ "you through selling them"), WIDTH);
		box.add(Box.createVerticalStrut(4));
		if (p.isDoable())
		{
			if (guideOn)
			{
				box.add(GuideUi.primary("▶ Start guide", () -> onStart.accept(p.getId())));
			}
			else
			{
				box.add(GuideUi.line("Turn on the Banked XP Guide plugin (it comes with this one) for the "
					+ "step-by-step guide.", GuideUi.WARN, WIDTH));
			}
		}
		String wiki = p.getRecipe().getActivity().getMethod().getWikiPage();
		if (wiki != null)
		{
			box.add(Box.createVerticalStrut(3));
			box.add(GuideUi.actions(GuideUi.small("Wiki", () -> onWiki.accept(wiki))));
		}
	}

	private static String lowerFirst(String s)
	{
		return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}
}
