package com.skillinghelper.engine;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Stop;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Experience;
import static com.skillinghelper.engine.Format.gp;

/**
 * Turns a method and the player's current state into the single next step, Quest Helper style. Steps are
 * recomputed every tick from scratch, so the guide adapts as soon as the inventory, bank or position changes.
 * <p>
 * The step is the first of these that applies: goal reached, do the current Slayer task, wait for the next run, a
 * requirement banking can't fix, fetch an item the GE doesn't sell, buy what's missing, make an input, bank, wear
 * gear, drop produce, walk to the spot, and finally train.
 */
public final class StepResolver
{
	private static final Location GRAND_EXCHANGE = Banks.GRAND_EXCHANGE;
	private static final String BANK_TIP = "Tip: press the Skilling Helper button in the bank to show only these items.";

	private final PriceLookup prices;
	private final Recipes recipes;
	private final BankTrip trips;
	private final RequirementChecker checker;

	public StepResolver(PriceLookup prices)
	{
		this.prices = prices;
		this.recipes = new Recipes(prices);
		this.trips = new BankTrip(recipes);
		this.checker = new RequirementChecker(prices, recipes, trips);
	}

	/**
	 * The "what you need" checklist for a method.
	 *
	 * @param actions actions still needed to reach the goal, used to size input requirements
	 */
	public List<Requirement> requirements(Method m, PlayerState s, int actions)
	{
		return checker.check(m, s, actions);
	}

	/**
	 * Picks the next step.
	 *
	 * @param targetLevel where this method stops: the goal, or the end of this leg of the plan
	 * @param drop        whether to drop produce when the inventory is full instead of banking
	 */
	public GuideStep resolve(Method m, PlayerState s, int targetLevel, boolean drop, List<Requirement> reqs)
	{
		if (s.level(m.getSkill()) >= targetLevel)
		{
			return goalReachedStep(m, targetLevel);
		}

		// With a Slayer task, the task is the training: the master is only needed again once it's done
		if (SlayerTaskStep.applies(m, s))
		{
			return SlayerTaskStep.step(m, s);
		}

		// On a run, once every stop is done there's nothing to fetch or do until the next one is ready
		Stop stop = null;
		if (!m.getStops().isEmpty())
		{
			stop = Runs.nextStop(m, s);
			if (stop == null)
			{
				return Runs.waitStep(m, s);
			}
		}

		Needs needs = Needs.of(reqs, s);
		Requirement unmet = needs.firstNonItem();
		if (unmet != null)
		{
			return unmetRequirementStep(unmet);
		}

		ItemReq unsourced = s.bankKnown() ? firstUnowned(sourcedItems(m), s) : null;
		if (unsourced != null)
		{
			return sourceStep(m, unsourced, s);
		}

		Location bank = bankFor(m, s);
		if (!needs.missing.isEmpty())
		{
			return s.bankKnown() ? buyStep(m, s, needs.missing) : checkBankStep(m, s, bank, needs);
		}

		// Inputs made from parts (baskets of apples, unfinished potions): make them as soon as the parts are carried,
		// and otherwise fetch only the parts so they fit in the inventory
		long actionsLeft = actionsLeft(m, s, targetLevel);
		List<ItemReq> toMake = recipes.toMake(m, s, actionsLeft);
		for (ItemReq input : toMake)
		{
			if (Recipes.partsCarried(input, s))
			{
				return makeStep(input, s);
			}
		}

		boolean gather = m.getType() != MethodType.PROCESS;
		boolean fullOfProduce = gather && s.inventoryFull() && carriesProduct(m, s);
		if (!needs.inBank.isEmpty() || needsInputs(m, s, gather, toMake) || (fullOfProduce && !drop))
		{
			return bankTripStep(m, s, bank, needs, toMake, gather, actionsLeft);
		}
		if (!needs.wearNow.isEmpty())
		{
			return equipStep(needs.wearNow);
		}
		if (fullOfProduce)
		{
			return dropStep(m);
		}
		return trainStep(m, s, stop);
	}

	/**
	 * The requirement lines sorted by what the guide does about them.
	 */
	private static final class Needs
	{
		final List<Requirement> missing = new ArrayList<>();
		final List<Requirement> inBank = new ArrayList<>();
		/**
		 * Optional items to mention whenever the player is banking anyway.
		 */
		final List<Requirement> optionalInBank = new ArrayList<>();
		/**
		 * Gear worth wearing that's already in the inventory.
		 */
		final List<Requirement> wearNow = new ArrayList<>();

		static Needs of(List<Requirement> reqs, PlayerState s)
		{
			Needs needs = new Needs();
			for (Requirement r : reqs)
			{
				boolean isItem = r.getItemIds().length > 0;
				switch (r.getStatus())
				{
					case MISSING:
						needs.missing.add(r);
						break;
					case IN_BANK:
						needs.inBank.add(r);
						break;
					case OPTIONAL:
						if (isItem && s.getInventory().has(r.getItemIds()))
						{
							needs.wearNow.add(r);
						}
						else if (isItem)
						{
							needs.optionalInBank.add(r);
						}
						break;
					case SUGGESTED:
						// Supplies like food that are in the bank
						if (isItem)
						{
							needs.optionalInBank.add(r);
						}
						break;
					default:
						break;
				}
			}
			return needs;
		}

		/**
		 * A missing requirement that isn't an item, such as a quest: banking can't fix it.
		 */
		Requirement firstNonItem()
		{
			for (Requirement r : missing)
			{
				if (r.getItemIds().length == 0)
				{
					return r;
				}
			}
			return null;
		}
	}

	// ---- Steps, in the order they're checked ----

	private static GuideStep goalReachedStep(Method m, int targetLevel)
	{
		return GuideStep.builder()
			.type(StepType.DONE)
			.title("Goal reached: " + m.getSkill().getName() + " " + targetLevel + "!")
			.line("Set a new target level in the panel to keep going.")
			.build();
	}

	private static GuideStep unmetRequirementStep(Requirement r)
	{
		return GuideStep.builder()
			.type(StepType.GET_ITEMS)
			.title("This method needs a requirement you don't have")
			.line(r.getText())
			.line("Pick another method in the panel, or complete the requirement first.")
			.build();
	}

	/**
	 * Sends the player to get an item the GE doesn't sell, e.g. a barbarian rod from Otto.
	 */
	private static GuideStep sourceStep(Method m, ItemReq req, PlayerState s)
	{
		Location where = req.getSourceAt() != null ? req.getSourceAt() : m.getLocation();
		GuideStep.GuideStepBuilder b = GuideStep.builder();
		if (req.getSourceTarget() != null)
		{
			b.highlight(req.getSourceTarget());
		}
		if (!where.contains(s.getLocation()))
		{
			Route route = Route.to(s, where);
			return b.type(StepType.WALK).title(route.title())
				.line("You need a " + req.getName().toLowerCase() + ", which isn't sold on the GE. " + req.getSource()
					+ ".")
				.travel(route)
				.build();
		}
		return b.type(StepType.GET_ITEMS)
			.title("Get a " + req.getName().toLowerCase())
			.line(req.getSource() + ".")
			.build();
	}

	/**
	 * The bank hasn't been seen yet, so the player may already own what looks missing.
	 */
	private GuideStep checkBankStep(Method m, PlayerState s, Location bank, Needs needs)
	{
		return bankStep(s, bank, "Open your bank so I can check what you own", needs.missing, needs.optionalInBank, m,
			null)
			.line("I haven't seen your bank yet, so I can't tell if you already have these.")
			.line(BANK_TIP)
			.build();
	}

	private GuideStep buyStep(Method m, PlayerState s, List<Requirement> missing)
	{
		long total = missing.stream().mapToLong(Requirement::getBuyCost).sum();
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.type(StepType.GET_ITEMS)
			.title("Buy the items you're missing")
			.highlightBank(false);
		for (Requirement r : missing)
		{
			b.line("• " + r.getText());
		}
		b.line(String.format("Total ~%s at current GE prices. You have %s.", gp(total), gp(s.gp())));
		if (total > s.gp())
		{
			affordabilityHint(m, s, b);
		}
		if (!GRAND_EXCHANGE.contains(s.getLocation()))
		{
			b.travel(Route.to(s, GRAND_EXCHANGE));
		}
		return b.build();
	}

	private static GuideStep makeStep(ItemReq input, PlayerState s)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder();
		Location where = input.getMakeAt();
		if (where != null && !where.contains(s.getLocation()))
		{
			Route route = Route.to(s, where);
			return b.type(StepType.WALK).title(route.title())
				.line("To make " + input.getName().toLowerCase() + ": " + input.getRecipeText() + ".")
				.travel(route)
				.build();
		}
		b.type(StepType.MAKE)
			.title("Make " + input.getName().toLowerCase())
			.line(input.getRecipeText() + ".")
			.line("Make as many as your parts allow; the guide carries on once you're done.");
		if (input.getMakeTarget() != null)
		{
			b.highlight(input.getMakeTarget());
		}
		for (ItemReq part : input.getRecipe())
		{
			for (int id : part.getIds())
			{
				b.itemHighlight(id, ItemHighlight.USE);
			}
		}
		return b.build();
	}

	/**
	 * A bank visit: put back what the trip doesn't use and take out tools, inputs (or the parts to make them) and
	 * optional extras.
	 */
	private GuideStep bankTripStep(Method m, PlayerState s, Location bank, Needs needs, List<ItemReq> toMake,
		boolean gather, long actionsLeft)
	{
		// A trip to make inputs (molten glass, unfinished potions) only needs the parts, so the method's own tools
		// stay in the bank to make room
		Set<Integer> kept = toMake.isEmpty() ? BankTrip.keptIds(m) : Recipes.keptIds(toMake);
		// Inputs get their own "withdraw N x" lines below, so leave them out of the generic list
		List<Requirement> withdraw = new ArrayList<>();
		for (Requirement r : needs.inBank)
		{
			if (!isInput(m, r) && (toMake.isEmpty() || ItemIds.containsAny(kept, r.getItemIds())))
			{
				withdraw.add(r);
			}
		}
		GuideStep.GuideStepBuilder b = bankStep(s, bank, "Bank", withdraw, needs.optionalInBank, m, kept);
		if (toMake.isEmpty())
		{
			trips.withdrawInputs(b, m, s, gather, actionsLeft);
		}
		else
		{
			BankTrip.withdrawParts(b, toMake, s, actionsLeft);
		}
		return b.line(BANK_TIP).build();
	}

	private static GuideStep equipStep(List<Requirement> wearNow)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.type(StepType.EQUIP)
			.title("Wear your skilling gear");
		for (Requirement r : wearNow)
		{
			b.line("• " + r.getText().replace(" (in bank)", ""));
			for (int id : r.getItemIds())
			{
				b.itemHighlight(id, ItemHighlight.EQUIP);
			}
		}
		return b.build();
	}

	private static GuideStep dropStep(Method m)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.type(StepType.DROP)
			.title("Inventory full: drop your " + productNoun(m))
			.line("Shift-click drop is fastest (enable it in RuneLite's Menu Entry Swapper).");
		for (int id : m.getProducts())
		{
			b.itemHighlight(id, ItemHighlight.DROP);
		}
		return b.build();
	}

	/**
	 * Train at the spot, or walk there first.
	 */
	private static GuideStep trainStep(Method m, PlayerState s, Stop stop)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.highlights(m.getTargets());
		for (int id : m.getUseItems())
		{
			b.itemHighlight(id, ItemHighlight.USE);
		}
		toolWieldTip(m, s, b);

		String progress = stop == null ? null : Runs.progress(m, s, stop);
		boolean there = stop != null ? stop.getLocation().contains(s.getLocation())
			: m.isAt(s.getLocation(), s.isInInstance());
		if (!there)
		{
			Route route = Route.to(s, trainingSpots(m, stop));
			b.type(StepType.WALK).title(route.title());
			if (progress != null)
			{
				b.line(progress);
			}
			if (stop != null && stop.getHint() != null)
			{
				b.line(stop.getHint() + ".");
			}
			return b.travel(route)
				.line("Then: " + m.getInstruction() + ".")
				.build();
		}

		b.type(StepType.TRAIN).title(m.getInstruction());
		if (progress != null)
		{
			b.line(progress);
		}
		if (m.getNotes() != null)
		{
			b.line(m.getNotes());
		}
		return b.build();
	}

	private static List<Location> trainingSpots(Method m, Stop stop)
	{
		List<Location> spots = new ArrayList<>();
		if (stop != null)
		{
			spots.add(stop.getLocation());
		}
		else
		{
			spots.add(m.getLocation());
			spots.addAll(m.getAlsoAt());
		}
		return spots;
	}

	// ---- Banking ----

	/**
	 * The bank to use: the closest one to the player. A method's own bank (picked for the route, e.g. not across a
	 * river, or a bank chest inside the activity) wins unless another is much closer as the crow flies. Falls back
	 * to the method's bank, or the GE, when the player's position doesn't help (in an instance or underground).
	 */
	static Location bankFor(Method m, PlayerState s)
	{
		Location own = m.getBank() != null ? m.getBank() : GRAND_EXCHANGE;
		if (s.isInInstance() || s.getLocation() == null)
		{
			return own;
		}
		Location nearest = Banks.nearest(s.getLocation(), s.isMembersWorld());
		if (nearest == null)
		{
			return own;
		}
		int ownDistance = Banks.distance(s.getLocation(), own.getPoint());
		int nearestDistance = Banks.distance(s.getLocation(), nearest.getPoint());
		if (m.getBank() == null)
		{
			// No preferred bank (the GE is only a fallback), so just take the closest
			return ownDistance <= nearestDistance ? own : nearest;
		}
		return ownDistance <= nearestDistance * 3 / 2 + 10 ? own : nearest;
	}

	/**
	 * The start of a bank step: walk there, deposit what the trip doesn't use (when {@code kept} is given), then
	 * withdraw these requirements and any optional extras that are in the bank.
	 */
	private static GuideStep.GuideStepBuilder bankStep(PlayerState s, Location bank, String title,
		List<Requirement> withdraw, List<Requirement> optional, Method m, Set<Integer> kept)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.type(StepType.BANK)
			.title(title + (bank.contains(s.getLocation()) ? "" : " at " + bank.getName()))
			.highlightBank(true);
		if (!bank.contains(s.getLocation()))
		{
			b.travel(Route.to(s, bank));
		}
		if (kept != null)
		{
			depositUnused(b, m, s, kept);
		}
		for (Requirement r : withdraw)
		{
			b.line("• " + (r.getStatus() == Requirement.Status.MISSING ? "Check for " : "Withdraw ")
				+ r.getText().replace(" (in bank)", ""));
			b.withdraw(r.getItemIds(), r.getQuantity());
		}
		for (Requirement r : optional)
		{
			if (s.bankCount(r.getItemIds()) > 0)
			{
				b.line("• Optional: " + r.getText().replace(" (in bank)", "").replaceFirst("^Optional: ", ""));
				b.withdraw(r.getItemIds(), r.getQuantity());
			}
		}
		return b;
	}

	/**
	 * Deposits the method's products and, outside activities, anything else the trip doesn't use.
	 */
	private static void depositUnused(GuideStep.GuideStepBuilder b, Method m, PlayerState s, Set<Integer> kept)
	{
		if (carriesProduct(m, s))
		{
			b.line("• Deposit your " + productNoun(m));
			for (int id : m.getProducts())
			{
				b.deposit(id, 0);
			}
		}
		if (m.getType() == MethodType.ACTIVITY)
		{
			return;
		}
		// Anything else the trip doesn't use only takes up room
		boolean other = false;
		for (int id : s.getInventory().asMap().keySet())
		{
			if (!kept.contains(id) && !m.getProducts().contains(id))
			{
				b.deposit(id, 0);
				other = true;
			}
		}
		if (other)
		{
			b.line("• Deposit the other outlined items: this trip doesn't use them");
		}
	}

	private static boolean needsInputs(Method m, PlayerState s, boolean gather, List<ItemReq> toMake)
	{
		if (!toMake.isEmpty())
		{
			return true;
		}
		for (ItemReq input : m.getInputs())
		{
			if (BankTrip.needsWithdraw(input, s, gather))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Actions the goal still needs, or {@link Long#MAX_VALUE} when the method has no xp per action.
	 */
	static long actionsLeft(Method m, PlayerState s, int targetLevel)
	{
		double perAction = m.xpPerAction(s.level(m.getSkill()));
		if (perAction <= 0 || targetLevel < 1 || targetLevel > Experience.MAX_VIRT_LEVEL)
		{
			return Long.MAX_VALUE;
		}
		long xpLeft = Experience.getXpForLevel(targetLevel) - s.xp(m.getSkill());
		return Math.max(1, (long) Math.ceil(xpLeft / perAction));
	}

	// ---- Helpers ----

	private static void toolWieldTip(Method m, PlayerState s, GuideStep.GuideStepBuilder b)
	{
		if (m.getToolType() == null)
		{
			return;
		}
		ToolAdvisor.OwnedTool owned = ToolAdvisor.bestOwned(m.getToolType(), s);
		if (owned != null && owned.getWhere() == ToolAdvisor.Where.INVENTORY && ToolAdvisor.canWield(owned.getTier(), s))
		{
			b.line("Tip: wield your " + owned.getTier().getName().toLowerCase() + " to free an inventory slot.");
			b.itemHighlight(owned.getItemId(), ItemHighlight.EQUIP);
		}
	}

	/**
	 * Explains how far the player's gold goes when they can't buy everything for the goal at once.
	 */
	private void affordabilityHint(Method m, PlayerState s, GuideStep.GuideStepBuilder b)
	{
		double costPerAction = inputCostPerAction(m, s);
		if (costPerAction <= 0)
		{
			b.line("You can't afford all of this yet: try the Cheapest preference or a gathering method.");
			return;
		}
		double earnedPerAction = earnedPerAction(m);
		long affordable = (long) (s.gp() / costPerAction);
		b.line(String.format("You can afford about %,d actions (~%,d xp) right now.", affordable,
			(long) (affordable * m.getXpPerAction())));
		if (earnedPerAction >= costPerAction)
		{
			b.line(String.format("Each action sells for ~%s after GE tax vs ~%s in materials, so buy what you can, "
				+ "sell the products and repeat.", gp(Math.round(earnedPerAction)), gp(Math.round(costPerAction))));
		}
		else if (earnedPerAction > 0)
		{
			b.line(String.format("Selling the products gets ~%s of the ~%s per action back, so sell them as you go "
				+ "to stretch your gold.", gp(Math.round(earnedPerAction)), gp(Math.round(costPerAction))));
		}
		else
		{
			b.line("Try the Cheapest preference or a gathering method to fund this.");
		}
	}

	private double inputCostPerAction(Method m, PlayerState s)
	{
		double cost = 0;
		for (ItemReq input : m.getInputs())
		{
			if (!s.ownsSubstitute(input))
			{
				cost += (double) prices.price(input.primaryId()) * input.getPerAction();
			}
		}
		return cost;
	}

	/**
	 * Coins per action plus the average product's price after GE tax, for methods that keep their products.
	 */
	private double earnedPerAction(Method m)
	{
		double earned = m.getCoinsPerAction();
		if (!m.getProducts().isEmpty() && !m.isDropByDefault())
		{
			double products = 0;
			for (int id : m.getProducts())
			{
				products += prices.sellPriceAfterTax(id);
			}
			earned += products / m.getProducts().size();
		}
		return earned;
	}

	private static boolean isInput(Method m, Requirement r)
	{
		for (ItemReq input : m.getInputs())
		{
			for (int id : r.getItemIds())
			{
				if (input.matches(id) || partOf(input, id))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static boolean partOf(ItemReq input, int id)
	{
		for (ItemReq part : input.getRecipe())
		{
			if (part.matches(id))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Tools and inputs the GE doesn't sell, which the player has to fetch from somewhere.
	 */
	private static List<ItemReq> sourcedItems(Method m)
	{
		List<ItemReq> sourced = new ArrayList<>();
		for (ItemReq req : m.getTools())
		{
			if (req.getSource() != null)
			{
				sourced.add(req);
			}
		}
		for (ItemReq req : m.getInputs())
		{
			if (req.getSource() != null)
			{
				sourced.add(req);
			}
		}
		return sourced;
	}

	private static ItemReq firstUnowned(List<ItemReq> reqs, PlayerState s)
	{
		for (ItemReq req : reqs)
		{
			if (s.owned(req.getIds()) == 0 && !s.ownsSubstitute(req))
			{
				return req;
			}
		}
		return null;
	}

	private static boolean carriesProduct(Method m, PlayerState s)
	{
		for (int id : m.getProducts())
		{
			if (s.getInventory().has(id))
			{
				return true;
			}
		}
		return false;
	}

	private static String productNoun(Method m)
	{
		switch (m.getSkill())
		{
			case WOODCUTTING:
				return "logs";
			case MINING:
				return "ore";
			case FISHING:
				return "fish";
			case HUNTER:
				return "catches";
			case THIEVING:
				return "loot";
			default:
				return "products";
		}
	}

	/**
	 * Every item id the bank filter should show for these requirements and step.
	 */
	public static Map<Integer, ItemHighlight> bankItems(List<Requirement> reqs, GuideStep step)
	{
		Map<Integer, ItemHighlight> items = new LinkedHashMap<>();
		for (Requirement r : reqs)
		{
			if (r.getStatus() == Requirement.Status.INFO)
			{
				continue;
			}
			for (int id : r.getItemIds())
			{
				items.put(id, ItemHighlight.WITHDRAW);
			}
		}
		if (step != null)
		{
			step.getItemHighlights().forEach((id, h) ->
			{
				if (h == ItemHighlight.WITHDRAW)
				{
					items.put(id, h);
				}
			});
		}
		return items;
	}
}
