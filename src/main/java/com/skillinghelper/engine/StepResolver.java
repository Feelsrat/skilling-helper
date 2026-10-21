package com.skillinghelper.engine;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.BoostItem;
import com.skillinghelper.data.CombatGear;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.SkillOutfit;
import com.skillinghelper.data.Stop;
import com.skillinghelper.data.ToolType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Turns a method and the player's current state into the single next step, Quest Helper style. Steps are
 * recomputed every tick from scratch, so the guide adapts as soon as the inventory, bank or position changes.
 */
public final class StepResolver
{
	private static final Location GRAND_EXCHANGE = Banks.GRAND_EXCHANGE;
	private static final String BANK_TIP = "Tip: press the Skilling Helper button in the bank to show only these items.";

	private final PriceLookup prices;

	public StepResolver(PriceLookup prices)
	{
		this.prices = prices;
	}

	/**
	 * The "what you need" checklist for a method.
	 *
	 * @param actions actions still needed to reach the goal, used to size input requirements
	 */
	public List<Requirement> requirements(Method m, PlayerState s, int actions)
	{
		List<Requirement> reqs = new ArrayList<>();

		ToolType toolType = m.getToolType();
		if (toolType != null)
		{
			ToolType.Tier usable = ToolAdvisor.bestUsable(toolType, s);
			ToolAdvisor.OwnedTool owned = ToolAdvisor.bestOwned(toolType, s);
			if (owned == null)
			{
				int price = prices.price(usable.getIds()[0]);
				reqs.add(new Requirement(usable.getName() + " (best " + toolType.getNoun() + " for your level, ~"
					+ gp(price) + ")", Requirement.Status.MISSING, usable.getIds(), 1, price));
			}
			else
			{
				String name = owned.getTier().getName();
				ToolAdvisor.OwnedTool carried = ToolAdvisor.bestCarried(toolType, s);
				if (owned.getWhere() != ToolAdvisor.Where.BANK)
				{
					reqs.add(new Requirement(name, Requirement.Status.READY, new int[]{owned.getItemId()}, 1, 0));
				}
				else if (carried != null)
				{
					// A usable tool is already carried, so the better one in the bank is a recommendation only
					reqs.add(new Requirement(carried.getTier().getName(), Requirement.Status.READY,
						new int[]{carried.getItemId()}, 1, 0));
					reqs.add(new Requirement("Better: " + name + " is in your bank", Requirement.Status.OPTIONAL,
						new int[]{owned.getItemId()}, 1, 0));
				}
				else
				{
					reqs.add(new Requirement(name + " (in bank)", Requirement.Status.IN_BANK,
						new int[]{owned.getItemId()}, 1, 0));
				}
				if (owned.getTier() != usable)
				{
					int price = prices.price(usable.getIds()[0]);
					reqs.add(new Requirement("Upgrade: " + usable.getName() + " is the best you can use (~" + gp(price) + ")",
						Requirement.Status.OPTIONAL, new int[0], 0, price));
				}
			}
		}

		for (ItemReq tool : m.getTools())
		{
			reqs.add(itemRequirement(tool, s, 1, 1));
		}

		for (ItemReq input : m.getInputs())
		{
			if (input.hasSubstitute() && s.owned(input.getSubstituteIds()) > 0)
			{
				ItemReq sub = ItemReq.tool(input.getSubstituteName(), input.getSubstituteIds());
				Requirement r = itemRequirement(sub, s, 1, 1);
				reqs.add(new Requirement(r.getText() + " (instead of " + input.getName().toLowerCase() + ")",
					r.getStatus(), r.getItemIds(), r.getQuantity(), r.getBuyCost()));
				continue;
			}
			long needed = (long) Math.max(actions, 1) * input.getPerAction();
			int trip = (int) Math.min(input.isWholeStack() ? Integer.MAX_VALUE : input.getQuantity(), Math.max(needed, 1));
			Requirement r = itemRequirement(input, s, needed, trip);
			if (input.hasSubstitute() && r.getStatus() == Requirement.Status.MISSING)
			{
				r = new Requirement(r.getText() + ", or use a " + input.getSubstituteName().toLowerCase(), r.getStatus(),
					r.getItemIds(), r.getQuantity(), r.getBuyCost());
			}
			reqs.add(r);
		}

		double goalHours = m.getXpPerHour() <= 0 ? 0 : (double) actions * m.getXpPerAction() / m.getXpPerHour();
		for (SkillOutfit outfit : SkillOutfit.values())
		{
			if (outfit.getSkill() != m.getSkill())
			{
				continue;
			}
			int missingPieces = 0;
			for (int[] slot : outfit.getSlots())
			{
				if (s.getEquipment().has(slot))
				{
					continue;
				}
				int id = firstOwned(slot, s);
				if (id == -1)
				{
					missingPieces++;
					continue;
				}
				boolean carried = s.getInventory().has(id);
				reqs.add(new Requirement(outfit.getName() + " piece: wear it (" + outfit.getBonus() + ")"
					+ (carried ? "" : " (in bank)"), Requirement.Status.OPTIONAL, new int[]{id}, 1, 0));
			}
			if (missingPieces > 0 && s.bankKnown())
			{
				reqs.add(new Requirement(unlockText(outfit, missingPieces, goalHours, m), Requirement.Status.UNLOCK,
					new int[0], 0, 0));
			}
		}

		for (BoostItem boost : BoostItem.values())
		{
			if (!boost.appliesTo(m))
			{
				continue;
			}
			if (s.getEquipment().has(boost.getIds()))
			{
				reqs.add(new Requirement(boost.getName() + " (" + boost.getBonus() + ")", Requirement.Status.READY,
					boost.getIds(), 1, 0));
			}
			else if (s.owned(boost.getIds()) > 0)
			{
				boolean carried = s.getInventory().has(boost.getIds());
				reqs.add(new Requirement(boost.getName() + ": wear it (" + boost.getBonus() + ")"
					+ (carried ? "" : " (in bank)"), Requirement.Status.OPTIONAL, boost.getIds(), 1, 0));
			}
			else
			{
				int price = prices.price(boost.getIds()[0]);
				reqs.add(new Requirement("Optional buy: " + boost.getName() + " (~" + gp(price) + " on the GE): "
					+ boost.getBonus(), Requirement.Status.SUGGESTED, new int[0], 0, 0));
			}
		}

		gearRequirements(m, s, reqs);
		for (ItemReq supply : m.getSupplies())
		{
			reqs.add(supplyRequirement(supply, s));
		}

		for (Quest q : m.getQuests())
		{
			boolean done = s.getCompletedQuests().contains(q);
			reqs.add(new Requirement("Quest: " + q.getName(), done ? Requirement.Status.READY : Requirement.Status.MISSING,
				new int[0], 0, 0));
		}
		if (m.getRequirementText() != null)
		{
			reqs.add(new Requirement(m.getRequirementText(), Requirement.Status.INFO, new int[0], 0, 0));
		}
		return reqs;
	}

	private Requirement itemRequirement(ItemReq req, PlayerState s, long needed, int trip)
	{
		int carried = s.carried(req.getIds());
		int banked = s.bankCount(req.getIds());
		String name = req.getName();
		boolean consumed = req.getPerAction() > 0;
		if (!consumed && req.getQuantity() > 1)
		{
			return toolSet(req, carried, banked);
		}
		String counts = consumed
			? String.format(" (%,d carried, %,d in bank, %,d needed for goal)", carried, banked, needed)
			: "";
		if (carried > 0)
		{
			return new Requirement(name + counts, Requirement.Status.READY, req.getIds(), trip, 0);
		}
		if (banked > 0)
		{
			return new Requirement(name + (consumed ? counts : " (in bank)"), Requirement.Status.IN_BANK, req.getIds(),
				trip, 0);
		}
		long toBuy = consumed ? Math.max(0, needed - banked) : 1;
		int unit = prices.price(req.primaryId());
		long cost = toBuy * unit;
		String buy = toBuy > 1
			? String.format(" - buy %,d x ~%s = ~%s", toBuy, gp(unit), gp(cost))
			: " - buy for ~" + gp(cost);
		return new Requirement(name + counts + buy, Requirement.Status.MISSING, req.getIds(), trip, cost);
	}

	/**
	 * Several copies of a tool, e.g. 4 box traps: all of them should be carried.
	 */
	private Requirement toolSet(ItemReq req, int carried, int banked)
	{
		int want = req.getQuantity();
		String name = String.format("%d x %s", want, req.getName());
		if (carried >= want)
		{
			return new Requirement(name, Requirement.Status.READY, req.getIds(), want, 0);
		}
		if (carried + banked >= want)
		{
			return new Requirement(name + String.format(" (%d carried, %d in bank)", carried, banked),
				Requirement.Status.IN_BANK, req.getIds(), want - carried, 0);
		}
		int toBuy = want - carried - banked;
		int unit = prices.price(req.primaryId());
		return new Requirement(name + String.format(" (you have %d) - buy %d x ~%s", carried + banked, toBuy, gp(unit)),
			Requirement.Status.MISSING, req.getIds(), want - carried, (long) toBuy * unit);
	}

	/**
	 * An optional supply such as food: fine to skip, so it's only ever a suggestion.
	 */
	private Requirement supplyRequirement(ItemReq req, PlayerState s)
	{
		int carried = s.carried(req.getIds());
		int banked = s.bankCount(req.getIds());
		String amount = req.isWholeStack() ? "" : req.getQuantity() + " x ";
		if (carried > 0)
		{
			return new Requirement("Optional: " + req.getName() + " (carried)", Requirement.Status.READY, req.getIds(),
				Math.max(1, req.getQuantity()), 0);
		}
		if (banked > 0)
		{
			return new Requirement("Optional: " + amount + req.getName() + " (" + String.format("%,d", banked)
				+ " in bank)", Requirement.Status.SUGGESTED, req.getIds(), Math.max(1, req.getQuantity()), 0);
		}
		int unit = prices.price(req.primaryId());
		String price = unit > 0 ? " (~" + gp(unit) + " each on the GE)" : "";
		return new Requirement("Optional: bring " + amount + req.getName().toLowerCase() + price,
			Requirement.Status.SUGGESTED, new int[0], 0, 0);
	}

	/**
	 * Suggests the best weapon and armour per slot: wear what you own, or buy an upgrade from the GE.
	 */
	private void gearRequirements(Method m, PlayerState s, List<Requirement> reqs)
	{
		if (m.getGearStyle() == null)
		{
			return;
		}
		for (CombatGear slot : CombatGear.values())
		{
			if (slot.getStyle() != m.getGearStyle())
			{
				continue;
			}
			int bestUsable = -1;
			int worn = -1;
			int owned = -1;
			int ownedId = -1;
			List<CombatGear.Tier> tiers = slot.getTiers();
			for (int i = 0; i < tiers.size(); i++)
			{
				CombatGear.Tier t = tiers.get(i);
				if (!canWear(t, s))
				{
					continue;
				}
				if (bestUsable == -1)
				{
					bestUsable = i;
				}
				if (worn == -1 && s.getEquipment().has(t.getIds()))
				{
					worn = i;
				}
				if (owned == -1 && s.owned(t.getIds()) > 0)
				{
					owned = i;
					ownedId = firstOwned(t.getIds(), s);
				}
			}
			if (bestUsable == -1)
			{
				continue;
			}
			if (owned != -1 && ownedId != -1 && (worn == -1 || owned < worn))
			{
				boolean carried = s.getInventory().has(ownedId);
				reqs.add(new Requirement("Gear: wear your " + tiers.get(owned).getName() + (carried ? "" : " (in bank)"),
					Requirement.Status.OPTIONAL, new int[]{ownedId}, 1, 0));
			}
			int have = worn != -1 && (owned == -1 || worn < owned) ? worn : owned;
			if (s.bankKnown() && (have == -1 || bestUsable < have))
			{
				CombatGear.Tier best = tiers.get(bestUsable);
				reqs.add(new Requirement("Gear upgrade (" + slot.getSlotName() + "): " + best.getName() + " (~"
					+ gp(prices.price(best.getIds()[0])) + " on the GE)", Requirement.Status.SUGGESTED, new int[0], 0, 0));
			}
		}
	}

	private static boolean canWear(CombatGear.Tier t, PlayerState s)
	{
		return s.level(t.getSkill()) >= t.getLevel()
			&& s.level(Skill.DEFENCE) >= t.getDefenceLevel()
			&& (!t.isMembers() || s.isMembersWorld());
	}

	private static String unlockText(SkillOutfit outfit, int missingPieces, double goalHours, Method m)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(missingPieces == outfit.getSlots().length ? "Worth chasing: " : "Missing " + missingPieces
			+ " piece" + (missingPieces == 1 ? "" : "s") + " of the ").append(outfit.getName())
			.append(" (").append(outfit.getBonus()).append("). Earn it from ").append(outfit.getSource());
		if (m.getId().equals(outfit.getSourceMethodId()))
		{
			sb.append(", which this method does");
		}
		sb.append('.');
		if (outfit.getXpBonus() > 0 && goalHours > 0)
		{
			double saved = goalHours * outfit.getXpBonus() / (1 + outfit.getXpBonus());
			sb.append(" The full set would save ~").append(hoursText(saved)).append(" on this goal.");
		}
		return sb.toString();
	}

	public static String hoursText(double hours)
	{
		long minutes = Math.round(hours * 60);
		if (minutes < 60)
		{
			return Math.max(1, minutes) + "m";
		}
		return String.format("%dh %02dm", minutes / 60, minutes % 60);
	}

	private static int firstOwned(int[] ids, PlayerState s)
	{
		for (int id : ids)
		{
			if (s.getInventory().has(id))
			{
				return id;
			}
		}
		for (int id : ids)
		{
			if (s.bankCount(id) > 0)
			{
				return id;
			}
		}
		return -1;
	}

	/**
	 * Picks the next step.
	 *
	 * @param drop whether to drop produce when the inventory is full instead of banking
	 */
	public GuideStep resolve(Method m, PlayerState s, int targetLevel, boolean drop, List<Requirement> reqs)
	{
		if (s.level(m.getSkill()) >= targetLevel)
		{
			return GuideStep.builder()
				.type(StepType.DONE)
				.title("Goal reached: " + m.getSkill().getName() + " " + targetLevel + "!")
				.line("Set a new target level in the panel to keep going.")
				.build();
		}

		// On a run, once every stop is done there's nothing to fetch or do until the next one is ready
		Stop stop = null;
		if (!m.getStops().isEmpty())
		{
			stop = nextStop(m, s);
			if (stop == null)
			{
				return waitStep(m, s);
			}
		}

		Location bank = m.getBank() != null ? m.getBank() : GRAND_EXCHANGE;

		List<Requirement> missing = new ArrayList<>();
		List<Requirement> inBank = new ArrayList<>();
		List<Requirement> optionalInBank = new ArrayList<>();
		List<Requirement> wearNow = new ArrayList<>();
		for (Requirement r : reqs)
		{
			boolean isItem = r.getItemIds().length > 0;
			switch (r.getStatus())
			{
				case MISSING:
					missing.add(r);
					break;
				case IN_BANK:
					inBank.add(r);
					break;
				case OPTIONAL:
					if (isItem && s.getInventory().has(r.getItemIds()))
					{
						wearNow.add(r);
					}
					else if (isItem)
					{
						optionalInBank.add(r);
					}
					break;
				case SUGGESTED:
					// Supplies like food that are in the bank: mention them whenever we're banking anyway
					if (isItem)
					{
						optionalInBank.add(r);
					}
					break;
				default:
					break;
			}
		}

		// Quests can't be fixed by banking, so stop here with a clear message.
		for (Requirement r : missing)
		{
			if (r.getItemIds().length == 0)
			{
				return GuideStep.builder()
					.type(StepType.GET_ITEMS)
					.title("This method needs a requirement you don't have")
					.line(r.getText())
					.line("Pick another method in the panel, or complete the requirement first.")
					.build();
			}
		}

		if (!missing.isEmpty())
		{
			if (!s.bankKnown())
			{
				return bankStep(s, bank, "Open your bank so I can check what you own", missing, optionalInBank, m, false)
					.line("I haven't seen your bank yet, so I can't tell if you already have these.")
					.line(BANK_TIP)
					.build();
			}
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
				travel(b, Route.to(s, GRAND_EXCHANGE));
			}
			return b.build();
		}

		boolean gather = m.getType() != MethodType.PROCESS;
		boolean needsInputs = false;
		for (ItemReq input : m.getInputs())
		{
			if (needsWithdraw(input, s, gather))
			{
				needsInputs = true;
				break;
			}
		}

		boolean fullOfProduce = gather && s.inventoryFull() && carriesProduct(m, s);
		if (!inBank.isEmpty() || needsInputs || (fullOfProduce && !drop && m.getBank() != null))
		{
			// Inputs get their own "withdraw N x" lines below, so leave them out of the generic list
			List<Requirement> withdraw = new ArrayList<>();
			for (Requirement r : inBank)
			{
				if (!isInput(m, r))
				{
					withdraw.add(r);
				}
			}
			GuideStep.GuideStepBuilder b = bankStep(s, bank, "Bank", withdraw, optionalInBank, m, true);
			if (needsInputs)
			{
				for (ItemReq input : m.getInputs())
				{
					if (needsWithdraw(input, s, gather))
					{
						b.line(input.isWholeStack()
							? "• Withdraw all your " + input.getName().toLowerCase()
							: String.format("• Withdraw %,d x %s", input.getQuantity(), input.getName()));
						for (int id : input.getIds())
						{
							b.itemHighlight(id, ItemHighlight.WITHDRAW);
						}
					}
				}
			}
			return b.line(BANK_TIP).build();
		}

		if (!wearNow.isEmpty())
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

		if (fullOfProduce)
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

		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.highlights(m.getTargets());
		for (int id : m.getUseItems())
		{
			b.itemHighlight(id, ItemHighlight.USE);
		}
		toolWieldTip(m, s, b);

		String progress = stop == null ? null : String.format("Stop %d of %d: %s (%d still to do this run)",
			m.getStops().indexOf(stop) + 1, m.getStops().size(), stop.getLocation().getName(), dueStops(m, s));
		boolean there = stop != null ? stop.getLocation().contains(s.getLocation())
			: m.isAt(s.getLocation(), s.isInInstance());
		if (!there)
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
			Route route = Route.to(s, spots);
			b.type(StepType.WALK).title(route.title());
			if (progress != null)
			{
				b.line(progress);
			}
			travel(b, route);
			return b.line("Then: " + m.getInstruction() + ".")
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

	/**
	 * The stop to visit next: the one the player is standing at if it needs a visit, otherwise the first one in run
	 * order that does. Null when every stop is done.
	 */
	private static Stop nextStop(Method m, PlayerState s)
	{
		Stop first = null;
		for (Stop stop : m.getStops())
		{
			if (s.getStopWaits().containsKey(stop.getId()))
			{
				continue;
			}
			if (stop.getLocation().contains(s.getLocation()))
			{
				return stop;
			}
			if (first == null)
			{
				first = stop;
			}
		}
		return first;
	}

	private static int dueStops(Method m, PlayerState s)
	{
		int due = 0;
		for (Stop stop : m.getStops())
		{
			if (!s.getStopWaits().containsKey(stop.getId()))
			{
				due++;
			}
		}
		return due;
	}

	private static GuideStep waitStep(Method m, PlayerState s)
	{
		int soonest = Integer.MAX_VALUE;
		for (Stop stop : m.getStops())
		{
			soonest = Math.min(soonest, s.getStopWaits().getOrDefault(stop.getId(), 0));
		}
		return GuideStep.builder()
			.type(StepType.WAIT)
			.title("Run done: next one ready in ~" + soonest + " min")
			.line("Every stop on this run is done. Train another skill meanwhile and come back when it's ready; "
				+ "this guide picks up again on its own.")
			.build();
	}

	private static void travel(GuideStep.GuideStepBuilder b, Route route)
	{
		b.route(route);
		for (String hint : route.hints())
		{
			b.line(hint);
		}
	}

	private GuideStep.GuideStepBuilder bankStep(PlayerState s, Location bank, String title, List<Requirement> withdraw,
		List<Requirement> optional, Method m, boolean depositProduce)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.type(StepType.BANK)
			.title(title + (bank.contains(s.getLocation()) ? "" : " at " + bank.getName()))
			.highlightBank(true);
		if (!bank.contains(s.getLocation()))
		{
			travel(b, Route.to(s, bank));
		}
		if (depositProduce && carriesProduct(m, s))
		{
			b.line("• Deposit your " + productNoun(m));
			for (int id : m.getProducts())
			{
				b.itemHighlight(id, ItemHighlight.DEPOSIT);
			}
		}
		for (Requirement r : withdraw)
		{
			b.line("• " + (r.getStatus() == Requirement.Status.MISSING ? "Check for " : "Withdraw ")
				+ r.getText().replace(" (in bank)", ""));
			for (int id : r.getItemIds())
			{
				b.itemHighlight(id, ItemHighlight.WITHDRAW);
			}
		}
		for (Requirement r : optional)
		{
			if (s.bankCount(r.getItemIds()) > 0)
			{
				b.line("• Optional: " + r.getText().replace(" (in bank)", "").replaceFirst("^Optional: ", ""));
				for (int id : r.getItemIds())
				{
					b.itemHighlight(id, ItemHighlight.WITHDRAW);
				}
			}
		}
		return b;
	}

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
	 * Whether the player has to fetch more of an input before training. Processing needs it in the inventory;
	 * gathering with consumables (feathers, bait, runes) only banks once they've run out.
	 */
	private static boolean needsWithdraw(ItemReq input, PlayerState s, boolean gather)
	{
		if (input.hasSubstitute() && s.carried(input.getSubstituteIds()) > 0)
		{
			return false;
		}
		int have = gather || input.isWholeStack() ? s.carried(input.getIds()) : s.getInventory().count(input.getIds());
		return have == 0;
	}

	/**
	 * Explains how far the player's gold goes when they can't buy everything for the goal at once.
	 */
	private void affordabilityHint(Method m, PlayerState s, GuideStep.GuideStepBuilder b)
	{
		double costPerAction = 0;
		for (ItemReq input : m.getInputs())
		{
			if (input.hasSubstitute() && s.owned(input.getSubstituteIds()) > 0)
			{
				continue;
			}
			costPerAction += (double) prices.price(input.primaryId()) * input.getPerAction();
		}
		double earnedPerAction = m.getCoinsPerAction();
		if (!m.getProducts().isEmpty() && !m.isDropByDefault())
		{
			double products = 0;
			for (int id : m.getProducts())
			{
				products += prices.sellPriceAfterTax(id);
			}
			earnedPerAction += products / m.getProducts().size();
		}
		if (costPerAction <= 0)
		{
			b.line("You can't afford all of this yet: try the Cheapest preference or a gathering method.");
			return;
		}
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

	private static boolean isInput(Method m, Requirement r)
	{
		for (ItemReq input : m.getInputs())
		{
			for (int id : r.getItemIds())
			{
				if (input.matches(id))
				{
					return true;
				}
			}
		}
		return false;
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

	public static String gp(long amount)
	{
		if (amount >= 10_000_000)
		{
			return (amount / 1_000_000) + "M gp";
		}
		if (amount >= 100_000)
		{
			return (amount / 1_000) + "K gp";
		}
		return String.format("%,d gp", amount);
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
