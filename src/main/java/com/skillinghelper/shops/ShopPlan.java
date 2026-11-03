package com.skillinghelper.shops;

import com.skillinghelper.engine.Format;
import java.util.ArrayList;
import java.util.List;
import lombok.Value;

/**
 * Which shop to visit, what to buy there, and what's left for the Grand Exchange.
 */
@Value
public class ShopPlan
{
	/**
	 * One item to buy at the shop.
	 */
	@Value
	public static class Line
	{
		ShopPlanner.Want want;
		ShopOffer offer;
		/**
		 * Gold for the whole amount, hopping worlds as needed.
		 */
		long cost;
		/**
		 * How many to buy in one world before the shop gets dearer than the GE (or runs out).
		 */
		int perWorld;
		int worlds;
		/**
		 * The dearest one is worth buying at in a world.
		 */
		int highestPrice;

		public long getGeCost()
		{
			return (long) want.getQuantity() * want.getGePrice();
		}

		public long getSaving()
		{
			return getGeCost() - cost;
		}
	}

	Shop shop;
	List<Line> lines;
	/**
	 * What the shop doesn't sell cheaper: still bought on the GE.
	 */
	List<ShopPlanner.Want> rest;
	long saving;

	public long getCost()
	{
		return lines.stream().mapToLong(Line::getCost).sum();
	}

	public String title()
	{
		return "Buy from " + shop.getName();
	}

	/**
	 * The step text: what to buy, what it saves, and how to buy it.
	 */
	public List<String> describe()
	{
		List<String> out = new ArrayList<>();
		out.add("Cheaper here than on the Grand Exchange (saves ~" + Format.gp(saving) + "):");
		for (Line l : lines)
		{
			out.add(String.format("• %,d x %s for ~%s (GE ~%s)", l.getWant().getQuantity(), l.getOffer().getItemName(),
				Format.gp(l.getCost()), Format.gp(l.getGeCost())));
		}
		out.add("Click " + shop.npcNames() + " (outlined), choose Trade and buy them from the shop window.");
		for (Line l : lines)
		{
			if (l.getWorlds() > 1)
			{
				out.add(String.format("The price climbs as the stock runs down: buy %,d %s, then hop worlds and buy"
						+ " the rest (%d worlds).", l.getPerWorld(), l.getOffer().getItemName().toLowerCase(),
					l.getWorlds()));
			}
			else if (l.getHighestPrice() > l.getOffer().getPrice())
			{
				out.add(String.format("The price of %s climbs as you buy; it's still cheaper than the GE up to"
					+ " %,d gp each.", l.getOffer().getItemName().toLowerCase(), l.getHighestPrice()));
			}
		}
		if (!rest.isEmpty())
		{
			StringBuilder names = new StringBuilder();
			for (ShopPlanner.Want w : rest)
			{
				if (w.getName() == null)
				{
					names.setLength(0);
					break;
				}
				names.append(names.length() > 0 ? ", " : "").append(w.getName().toLowerCase());
			}
			out.add(names.length() == 0 ? "Then buy the rest on the Grand Exchange."
				: "Then buy the rest on the Grand Exchange: " + names + ".");
		}
		return out;
	}
}
