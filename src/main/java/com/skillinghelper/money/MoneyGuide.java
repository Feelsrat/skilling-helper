package com.skillinghelper.money;

import com.skillinghelper.data.Location;
import com.skillinghelper.money.data.MoneyItem;
import com.skillinghelper.money.data.MoneyMethod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/**
 * Turns a wiki money making method into the next step, Quest Helper style: get the supplies, walk to the spot, then
 * do it with the wiki's own instructions. Recomputed every tick from what the player carries and where they stand.
 */
public final class MoneyGuide
{
	/**
	 * At most this many supplies are listed; the rest are on the wiki page.
	 */
	static final int MAX_SUPPLIES = 6;
	/**
	 * At most this many sentences of the wiki's instructions on the overlay; the panel shows them all.
	 */
	static final int MAX_SENTENCES = 6;
	static final Set<String> BANK_NPCS = Set.of("Banker", "Gnome banker", "Ghost banker", "Emerald Benedict");
	static final Set<String> BANK_OBJECTS = Set.of("Bank booth", "Bank chest", "Grand Exchange booth");

	private MoneyGuide()
	{
	}

	/**
	 * @param carried      item id to count across the inventory and worn equipment
	 * @param bankOpen     whether the bank interface is open
	 * @param suppliesDone the player has reached the spot once or said they have everything, so stop asking
	 */
	public static MoneyStep resolve(MoneyOption option, WorldPoint me, Map<Integer, Integer> carried, boolean bankOpen,
		boolean suppliesDone)
	{
		MoneyMethod m = option.getMethod();
		if (!option.isAvailable())
		{
			MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.LOCKED)
				.title("You can't do this one yet");
			option.getBlockers().forEach(b::line);
			return b.line("Pick another method in the Money Making panel, or work towards these first.").build();
		}

		List<MoneyItem> missing = suppliesDone ? List.of() : missingSupplies(m, carried);
		if (!missing.isEmpty())
		{
			return suppliesStep(m, missing, bankOpen);
		}

		Location spot = m.getSpot();
		if (spot != null && !spot.contains(me))
		{
			MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.TRAVEL)
				.title("Go to " + m.getLocationName())
				.destination(spot);
			if (me != null && me.getPlane() == spot.getPoint().getPlane())
			{
				b.line(me.distanceTo2D(spot.getPoint()) + " tiles away. Follow the arrow or the Shortest Path line.");
			}
			else
			{
				b.line("Follow the arrow or the Shortest Path line.");
			}
			return b.build();
		}
		return workStep(m);
	}

	/**
	 * Inputs and supplies worth bringing that the player has none of, most valuable first.
	 */
	static List<MoneyItem> missingSupplies(MoneyMethod m, Map<Integer, Integer> carried)
	{
		List<MoneyItem> out = new ArrayList<>();
		for (MoneyItem item : supplies(m))
		{
			if (carried.getOrDefault(itemId(item), 0) <= 0)
			{
				out.add(item);
			}
		}
		return out;
	}

	/**
	 * The inputs worth bringing: real items (or coins to spend), most gold per hour first.
	 */
	static List<MoneyItem> supplies(MoneyMethod m)
	{
		List<MoneyItem> out = new ArrayList<>();
		for (MoneyItem item : m.getInputs())
		{
			if (itemId(item) > 0 && item.getQuantity() > 0)
			{
				out.add(item);
			}
		}
		out.sort(Comparator.comparingDouble((MoneyItem i) -> i.getQuantity() * i.getWikiValue()).reversed());
		return out.size() > MAX_SUPPLIES ? out.subList(0, MAX_SUPPLIES) : out;
	}

	static int itemId(MoneyItem item)
	{
		return item.isCoins() ? ItemID.COINS : item.getId();
	}

	private static MoneyStep suppliesStep(MoneyMethod m, List<MoneyItem> missing, boolean bankOpen)
	{
		String per = m.isRecurring() ? " per run" : " an hour";
		MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.SUPPLIES)
			.title(bankOpen ? "Take out your supplies" : "Get your supplies from a bank");
		Set<Integer> withdraw = new LinkedHashSet<>();
		for (MoneyItem item : missing)
		{
			b.line("- " + item.getName() + " (about " + String.format("%,d", Math.max(1, Math.round(item.getQuantity())))
				+ per + ")");
			withdraw.add(itemId(item));
		}
		b.line(bankOpen ? "They're outlined in your bank. Buy any you don't have on the Grand Exchange."
			: "Use any bank, or the Grand Exchange to buy what you don't have.");
		b.line("Already set? Press \"I have everything\" in the panel.");
		return b.withdraw(withdraw).npcs(BANK_NPCS).objects(BANK_OBJECTS).build();
	}

	private static MoneyStep workStep(MoneyMethod m)
	{
		MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.WORK).title(m.getName());
		if (m.getSpot() == null && m.getLocationName() != null)
		{
			b.line("Where: " + m.getLocationName());
		}
		if (m.getNpc() != null)
		{
			b.line("Trade with " + m.getNpc() + " (outlined).");
		}
		List<String> sentences = sentences(m.getDetails());
		for (int i = 0; i < Math.min(MAX_SENTENCES, sentences.size()); i++)
		{
			b.line(sentences.get(i));
		}
		if (sentences.size() > MAX_SENTENCES)
		{
			b.line("The rest of the wiki's tips are in the panel.");
		}
		String sell = sellLine(m);
		if (sell != null)
		{
			b.line(sell);
		}
		if (m.getNpc() != null)
		{
			b.npcs(Set.of(m.getNpc()));
		}
		return b.build();
	}

	private static String sellLine(MoneyMethod m)
	{
		List<String> names = new ArrayList<>();
		List<MoneyItem> outputs = new ArrayList<>(m.getOutputs());
		outputs.sort(Comparator.comparingDouble((MoneyItem i) -> i.getQuantity() * i.getWikiValue()).reversed());
		for (MoneyItem out : outputs)
		{
			if (out.getId() > 0 && !names.contains(out.getName()) && names.size() < 3)
			{
				names.add(out.getName());
			}
		}
		return names.isEmpty() ? null : "Sell the " + String.join(", ", names) + " on the Grand Exchange.";
	}

	/**
	 * The wiki's details split into sentences and bullet points.
	 */
	public static List<String> sentences(String details)
	{
		List<String> out = new ArrayList<>();
		if (details == null)
		{
			return out;
		}
		for (String part : details.split("(?<=[.!?])\\s+(?=[A-Z\"(-])|\\s+(?=- )"))
		{
			String s = part.trim();
			if (s.startsWith("- "))
			{
				s = s.substring(2).trim();
			}
			if (s.length() > 1)
			{
				out.add(s);
			}
		}
		return out;
	}
}
