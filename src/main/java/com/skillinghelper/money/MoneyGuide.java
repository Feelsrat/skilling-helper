package com.skillinghelper.money;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.Location;
import com.skillinghelper.engine.Format;
import com.skillinghelper.money.data.MoneyItem;
import com.skillinghelper.money.data.MoneyMethod;
import com.skillinghelper.shops.ShopPlan;
import com.skillinghelper.shops.ShopPlanner;
import com.skillinghelper.travel.TravelPick;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/**
 * Turns a wiki money making method into the next step, Quest Helper style, written for players who don't know the
 * game well: buy what's missing at the Grand Exchange, open a bank and take out exact amounts, walk to the spot, do
 * it with the wiki's own instructions, and bank the results when the inventory fills up. Recomputed every tick from
 * what the player carries, what's in their bank and where they stand.
 */
public final class MoneyGuide
{
	/**
	 * At most this many supplies are listed; the rest are on the wiki page.
	 */
	static final int MAX_SUPPLIES = 6;
	/**
	 * At most this many sentences of the wiki's instructions on the step; the panel shows them all.
	 */
	static final int INVENTORY_SLOTS = 28;
	static final Set<String> BANK_NPCS = Set.of("Banker", "Gnome banker", "Ghost banker", "Emerald Benedict");
	static final Set<String> BANK_OBJECTS = Set.of("Bank booth", "Bank chest", "Grand Exchange booth");
	static final Set<String> GE_CLERKS = Set.of("Grand Exchange Clerk");

	private MoneyGuide()
	{
	}

	/**
	 * What the guide reads about the player each tick.
	 */
	@Value
	@Builder
	public static class Player
	{
		WorldPoint location;
		/**
		 * Item id to count across the inventory and worn equipment.
		 */
		@Builder.Default
		Map<Integer, Integer> carried = Collections.emptyMap();
		/**
		 * Item id to count in the bank, or null when the bank hasn't been opened yet.
		 */
		Map<Integer, Integer> bank;
		boolean bankOpen;
		boolean inventoryFull;
		boolean members;
		/**
		 * Item ids that stack in one inventory slot (runes, coins, noted items).
		 */
		@Builder.Default
		Set<Integer> stackable = Collections.emptySet();
		/**
		 * The player reached the spot once, or said they have everything: only ask for supplies again when the main
		 * input runs out.
		 */
		boolean suppliesDone;
		/**
		 * Buy from an NPC shop instead of the Grand Exchange when it's cheaper.
		 */
		boolean preferShops;
		/**
		 * Today's price to buy one on the GE.
		 */
		@Builder.Default
		IntUnaryOperator buyPrice = id -> 0;
		/**
		 * What to bring to the spot: a travel item and/or coins for a paid transport.
		 */
		@Builder.Default
		List<TravelPick> travel = Collections.emptyList();
	}

	public static MoneyStep resolve(MoneyOption option, Player p)
	{
		MoneyMethod m = option.getMethod();
		if (!option.isAvailable())
		{
			MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.LOCKED)
				.title("You can't do this one yet");
			option.getBlockers().forEach(b::line);
			return b.line("Pick another method in the Money Making panel, or work towards these first.").build();
		}

		Map<MoneyItem, Integer> trip = tripAmounts(m, p.getStackable());
		MoneyItem main = mainInput(m, p.getStackable());
		boolean outOfMain = main != null && p.getCarried().getOrDefault(itemId(main), 0) <= 0;
		if (!p.isSuppliesDone() || outOfMain)
		{
			Map<MoneyItem, Integer> missing = missing(trip, p.getCarried());
			if (!missing.isEmpty())
			{
				return suppliesStep(option, missing, p);
			}
		}

		if (p.isInventoryFull() && carriesProducts(m, p.getCarried()) && main == null)
		{
			return depositStep(m, trip, p);
		}

		Location spot = m.getSpot();
		if (spot != null && !spot.contains(p.getLocation()))
		{
			MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.TRAVEL)
				.title("Walk to " + m.getLocationName())
				.destination(spot);
			WorldPoint me = p.getLocation();
			if (me != null && me.getPlane() == spot.getPoint().getPlane())
			{
				b.line("About " + me.distanceTo2D(spot.getPoint()) + " tiles away.");
			}
			b.line("Follow the yellow arrow, or the line on the ground if you have the Shortest Path plugin.");
			for (TravelPick travel : p.getTravel())
			{
				b.line(travel.isCarried() ? travel.carriedLine() : travel.tipLine());
			}
			if (p.getTravel().isEmpty())
			{
				b.line("Teleports save time: check the wiki page (button in the panel) for the fastest way.");
			}
			return b.build();
		}
		return workStep(m, main, p);
	}

	// ---- Supplies ----

	/**
	 * How many of each supply to take out for one trip: an hour's (or one run's) worth of anything that stacks, and
	 * the inventory shared out between the rest in proportion to how many get used.
	 */
	static Map<MoneyItem, Integer> tripAmounts(MoneyMethod m, Set<Integer> stackable)
	{
		Map<MoneyItem, Integer> out = new LinkedHashMap<>();
		List<MoneyItem> loose = new ArrayList<>();
		int stackSlots = 0;
		for (MoneyItem item : supplies(m))
		{
			if (item.isCoins() || stackable.contains(itemId(item)))
			{
				out.put(item, (int) Math.min(Integer.MAX_VALUE, Math.max(1, Math.round(Math.ceil(item.getQuantity())))));
				stackSlots++;
			}
			else
			{
				loose.add(item);
			}
		}
		int free = Math.max(loose.size(), INVENTORY_SLOTS - stackSlots);
		double wanted = 0;
		for (MoneyItem item : loose)
		{
			wanted += Math.max(1, Math.ceil(item.getQuantity()));
		}
		for (MoneyItem item : loose)
		{
			double want = Math.max(1, Math.ceil(item.getQuantity()));
			int amount = wanted <= free ? (int) want : (int) Math.max(1, Math.floor(want * free / wanted));
			out.put(item, amount);
		}
		return out;
	}

	/**
	 * The input the method turns into money (grimy herbs, raw fish), which, once used up, means it's time to restock.
	 * Null for methods whose inputs are only runes, coins or a few consumables.
	 */
	static MoneyItem mainInput(MoneyMethod m, Set<Integer> stackable)
	{
		for (MoneyItem item : supplies(m))
		{
			if (!item.isCoins() && !stackable.contains(itemId(item)) && item.getQuantity() >= 100)
			{
				return item;
			}
		}
		return null;
	}

	private static Map<MoneyItem, Integer> missing(Map<MoneyItem, Integer> trip, Map<Integer, Integer> carried)
	{
		Map<MoneyItem, Integer> out = new LinkedHashMap<>();
		for (Map.Entry<MoneyItem, Integer> e : trip.entrySet())
		{
			int have = carried.getOrDefault(itemId(e.getKey()), 0);
			if (have < e.getValue())
			{
				out.put(e.getKey(), e.getValue() - have);
			}
		}
		return out;
	}

	private static MoneyStep suppliesStep(MoneyOption option, Map<MoneyItem, Integer> missing, Player p)
	{
		MoneyMethod m = option.getMethod();
		String warning = null;
		if (!option.isProfitable())
		{
			warning = "Heads up: at today's GE prices this loses about " + Format.gp(-option.getProfit())
				+ (m.isRecurring() ? " a run" : " an hour") + ". Consider picking another method.";
		}
		else if (option.isHardToSell())
		{
			warning = "Heads up: " + option.getMarketWarnings().get(0) + " Consider picking another method.";
		}
		Map<MoneyItem, Integer> toBuy = new LinkedHashMap<>();
		if (p.getBank() != null)
		{
			for (Map.Entry<MoneyItem, Integer> e : missing.entrySet())
			{
				int inBank = p.getBank().getOrDefault(itemId(e.getKey()), 0);
				if (inBank < e.getValue() && !e.getKey().isCoins())
				{
					toBuy.put(e.getKey(), e.getValue() - inBank);
				}
			}
		}
		Map<Integer, Integer> withdraw = new LinkedHashMap<>();
		missing.forEach((item, n) -> withdraw.put(itemId(item), n));

		ShopPlan shop = toBuy.isEmpty() || !p.isPreferShops() ? null : shopPlan(toBuy, p);
		if (shop != null && !p.isBankOpen())
		{
			return shopStep(shop, withdraw, p, warning);
		}
		if (!toBuy.isEmpty() && !p.isBankOpen())
		{
			return buyStep(toBuy, withdraw, p, warning, option.getTripCost());
		}
		if (!p.isBankOpen())
		{
			Location bank = Banks.nearest(p.getLocation(), p.isMembers());
			MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.OPEN_BANK)
				.title("Open your bank")
				.withdraw(withdraw)
				.npcs(BANK_NPCS)
				.objects(BANK_OBJECTS);
			if (bank != null && !bank.contains(p.getLocation()))
			{
				b.destination(bank).line("Walk to " + bank.getName() + " (follow the arrow).");
			}
			b.line("Click a bank booth or banker (outlined) and choose Bank.");
			b.line("You'll need:");
			missing.forEach((item, n) -> b.line("- " + amount(item, n)));
			travelLine(b, p);
			if (warning != null)
			{
				b.line(warning);
			}
			return b.build();
		}

		MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.WITHDRAW)
			.title("Take out your supplies")
			.withdraw(withdraw);
		b.line("1. If your inventory has other things, click \"Deposit inventory\" at the bottom of the bank.");
		b.line("2. Take out these (outlined in the bank):");
		missing.forEach((item, n) -> b.line("- " + amount(item, n)));
		travelLine(b, p);
		b.line("To take an exact number, right-click the item and choose Withdraw-X, then type it.");
		b.line("Tip: the coin button at the top of the bank shows only these items.");
		if (shop != null)
		{
			b.line("You don't have enough " + names(toBuy) + ". Take out ~" + Format.gp(shop.getCost())
				+ " in coins: " + shop.getShop().getName() + " sells " + (shop.getRest().isEmpty() ? "them" : "some")
				+ " for less than the Grand Exchange.");
		}
		else if (!toBuy.isEmpty())
		{
			b.line("You don't have enough " + names(toBuy) + ". Take out coins and buy them at the Grand Exchange.");
		}
		if (warning != null)
		{
			b.line(warning);
		}
		return b.build();
	}

	/**
	 * Adds the travel item still in the bank to a step that opens or uses the bank.
	 */
	private static void travelLine(MoneyStep.MoneyStepBuilder b, Player p)
	{
		Set<Integer> ids = new HashSet<>();
		for (TravelPick travel : p.getTravel())
		{
			if (!travel.isCarried())
			{
				b.line("- " + travel.bankLine());
				ids.add(travel.getItemId());
			}
		}
		if (!ids.isEmpty())
		{
			b.travel(ids);
		}
	}

	private static ShopPlan shopPlan(Map<MoneyItem, Integer> toBuy, Player p)
	{
		List<ShopPlanner.Want> wants = new ArrayList<>();
		toBuy.forEach((item, n) -> wants.add(new ShopPlanner.Want(itemId(item), item.getName(), n,
			p.getBuyPrice().applyAsInt(itemId(item)))));
		return ShopPlanner.plan(wants, p.getLocation(), p.isMembers());
	}

	private static MoneyStep shopStep(ShopPlan plan, Map<Integer, Integer> withdraw, Player p, String warning)
	{
		Location shop = plan.getShop().getLocation();
		MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.BUY)
			.title(plan.title())
			.withdraw(withdraw)
			.npcs(new HashSet<>(plan.getShop().getNpcs()));
		if (!shop.contains(p.getLocation()))
		{
			b.destination(shop).line("Walk to " + shop.getName() + " (follow the arrow).");
		}
		if (warning != null)
		{
			b.line(warning);
		}
		plan.describe().forEach(b::line);
		b.line("Have the coins in your inventory (take them out of a bank first).");
		b.line("Then open the bank and the guide moves on.");
		return b.build();
	}

	private static MoneyStep buyStep(Map<MoneyItem, Integer> toBuy, Map<Integer, Integer> withdraw, Player p,
		String warning, long cost)
	{
		Location ge = Banks.GRAND_EXCHANGE;
		MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.BUY)
			.title("Buy your supplies at the Grand Exchange")
			.withdraw(withdraw)
			.npcs(union(GE_CLERKS, BANK_NPCS))
			.objects(BANK_OBJECTS);
		if (!ge.contains(p.getLocation()))
		{
			b.destination(ge).line("Walk to the Grand Exchange in north-west Varrock (follow the arrow).");
		}
		if (warning != null)
		{
			b.line(warning);
		}
		b.line("Buy" + (cost > 0 ? " (about " + Format.gp(cost) + " in total)" : "") + ":");
		toBuy.forEach((item, n) -> b.line("- " + amount(item, n)));
		b.line("1. Have the coins in your inventory (take them out of a bank first).");
		b.line("2. Click a Grand Exchange Clerk (outlined) and choose Exchange.");
		b.line("3. Click the Buy arrow on an empty slot and type the item's name.");
		b.line("4. Set the quantity, press +5% once or twice so it buys fast, then Confirm.");
		b.line("5. When it's done, click the slot and Collect to bank.");
		b.line("6. Then open the bank (booths outlined) and the guide moves on.");
		return b.build();
	}

	private static MoneyStep depositStep(MoneyMethod m, Map<MoneyItem, Integer> trip, Player p)
	{
		Location bank = p.isBankOpen() ? null : Banks.nearest(p.getLocation(), p.isMembers());
		MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.DEPOSIT)
			.title("Inventory full: bank it")
			.npcs(BANK_NPCS)
			.objects(BANK_OBJECTS);
		if (p.isBankOpen())
		{
			b.line("Click the " + productNames(m) + " in your inventory to deposit them (or Deposit inventory).");
			if (!trip.isEmpty())
			{
				b.line("Keep your " + names(trip) + ", or take them out again.");
			}
			b.line("Then close the bank and go back.");
			return b.build();
		}
		if (bank != null && !bank.contains(p.getLocation()))
		{
			b.destination(bank).line("Walk to " + bank.getName() + " (follow the arrow).");
		}
		b.line("Click a bank booth or banker (outlined) and choose Bank.");
		return b.build();
	}

	// ---- Doing it ----

	private static MoneyStep workStep(MoneyMethod m, MoneyItem main, Player p)
	{
		MoneyStep.MoneyStepBuilder b = MoneyStep.builder().kind(MoneyStep.Kind.WORK).title(m.getName());
		if (m.getSpot() == null && m.getLocationName() != null)
		{
			b.line("Where: " + m.getLocationName());
		}
		if (m.getNpc() != null)
		{
			b.line("Click " + m.getNpc() + " (outlined) and choose Trade, then buy from the shop window.");
			b.npcs(Set.of(m.getNpc()));
		}
		Set<String> named = npcsFromName(m.getName());
		if (m.getNpc() == null && !named.isEmpty())
		{
			boolean kill = m.getName().startsWith("Killing ");
			b.line("Click the outlined " + named.iterator().next() + " to " + (kill ? "attack it. Eat when your "
				+ "hitpoints drop below half." : "pickpocket them."));
			b.npcs(named);
		}
		if (main != null && p.getCarried().getOrDefault(itemId(main), 0) > 0)
		{
			b.line("Use the " + main.getName() + " in your inventory (outlined).");
			b.use(Set.of(itemId(main)));
		}
		// The wiki's own how-to, every sentence, numbered: written for someone who has never done this before
		List<String> sentences = sentences(m.getDetails());
		for (int i = 0; i < sentences.size(); i++)
		{
			b.line((i + 1) + ". " + sentences.get(i));
		}
		if (sentences.size() < 3)
		{
			b.line("The wiki page (button in the panel) has the full strategy, with pictures.");
		}
		if (main != null)
		{
			b.line("When you run out, the guide takes you back to the bank for more.");
		}
		else if (!m.getOutputs().isEmpty() && !m.isRecurring())
		{
			b.line("When your inventory is full, the guide takes you to the bank.");
		}
		String products = productNames(m);
		if (!products.isEmpty())
		{
			b.line("To cash in: at the Grand Exchange click a clerk, Exchange, then click the " + products
				+ " in your inventory, set a price (-5% sells fast) and Confirm.");
		}
		return b.build();
	}

	/**
	 * The NPC a "Killing blue dragons" or "Pickpocketing master farmers" method is about, as the game spells its
	 * name: both as written and in sentence case, since the wiki's capitals don't always match. Empty for others.
	 */
	static Set<String> npcsFromName(String name)
	{
		String rest;
		if (name.startsWith("Killing "))
		{
			rest = name.substring("Killing ".length());
		}
		else if (name.startsWith("Pickpocketing "))
		{
			rest = name.substring("Pickpocketing ".length());
		}
		else
		{
			return Collections.emptySet();
		}
		for (String cut : new String[]{" (", " in ", " and ", " using ", " for ", " off-task", " on-task", ".",
			" boss"})
		{
			int i = rest.indexOf(cut);
			if (i > 0)
			{
				rest = rest.substring(0, i);
			}
		}
		if (rest.startsWith("the "))
		{
			rest = rest.substring(4);
		}
		int of = rest.indexOf(" of ");
		rest = of > 0 ? singular(rest.substring(0, of)) + rest.substring(of) : singular(rest);
		rest = rest.trim();
		if (rest.isEmpty())
		{
			return Collections.emptySet();
		}
		Set<String> names = new java.util.LinkedHashSet<>();
		names.add(Character.toUpperCase(rest.charAt(0)) + rest.substring(1));
		names.add(Character.toUpperCase(rest.charAt(0)) + rest.substring(1).toLowerCase(java.util.Locale.ROOT));
		return names;
	}

	private static String singular(String words)
	{
		if (words.endsWith("ves"))
		{
			return words.substring(0, words.length() - 3) + "f";
		}
		if (words.endsWith("s") && !words.endsWith("ss") && !words.endsWith("us") && !words.endsWith("is"))
		{
			return words.substring(0, words.length() - 1);
		}
		return words;
	}

	private static boolean carriesProducts(MoneyMethod m, Map<Integer, Integer> carried)
	{
		for (MoneyItem out : m.getOutputs())
		{
			if (out.getId() > 0 && carried.getOrDefault(out.getId(), 0) > 0)
			{
				return true;
			}
		}
		return false;
	}

	// ---- Helpers ----

	/**
	 * The inputs worth bringing: real items (or coins to spend), most gold per hour first.
	 */
	static List<MoneyItem> supplies(MoneyMethod m)
	{
		List<MoneyItem> out = new ArrayList<>();
		for (MoneyItem item : m.getInputs())
		{
			if (itemId(item) > 0 && item.getQuantity() > 0 && out.stream().noneMatch(o -> itemId(o) == itemId(item)))
			{
				out.add(item);
			}
		}
		out.sort(Comparator.comparingDouble((MoneyItem i) -> i.getQuantity() * i.getWikiValue()).reversed());
		return out.size() > MAX_SUPPLIES ? out.subList(0, MAX_SUPPLIES) : out;
	}

	private static Set<String> union(Set<String> a, Set<String> b)
	{
		Set<String> out = new HashSet<>(a);
		out.addAll(b);
		return out;
	}

	static int itemId(MoneyItem item)
	{
		return item.isCoins() ? ItemID.COINS : item.getId();
	}

	private static String amount(MoneyItem item, int n)
	{
		return String.format("%,d", n) + " x " + item.getName();
	}

	private static String names(Map<MoneyItem, Integer> items)
	{
		List<String> names = new ArrayList<>();
		items.keySet().forEach(i -> names.add(i.getName()));
		return String.join(", ", names);
	}

	private static String productNames(MoneyMethod m)
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
		return String.join(", ", names);
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
