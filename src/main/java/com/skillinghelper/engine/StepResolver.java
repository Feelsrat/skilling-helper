package com.skillinghelper.engine;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.SkillOutfit;
import com.skillinghelper.data.ToolType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;

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
			long needed = (long) Math.max(actions, 1) * input.getPerAction();
			int trip = (int) Math.min(input.getQuantity(), Math.max(needed, 1));
			reqs.add(itemRequirement(input, s, needed, trip));
		}

		for (SkillOutfit outfit : SkillOutfit.values())
		{
			if (outfit.getSkill() != m.getSkill())
			{
				continue;
			}
			for (int[] slot : outfit.getSlots())
			{
				if (s.getEquipment().has(slot))
				{
					continue;
				}
				int id = firstOwned(slot, s);
				if (id == -1)
				{
					continue;
				}
				boolean carried = s.getInventory().has(id);
				reqs.add(new Requirement(outfit.getName() + " piece: wear it (" + outfit.getBonus() + ")"
					+ (carried ? "" : " (in bank)"), Requirement.Status.OPTIONAL, new int[]{id}, 1, 0));
			}
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
		long cost = toBuy * prices.price(req.primaryId());
		return new Requirement(name + counts + " - buy for ~" + gp(cost), Requirement.Status.MISSING, req.getIds(), trip,
			cost);
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
			b.line(String.format("Total ~%s. You have %s.", gp(total), gp(s.gp())));
			if (total > s.gp())
			{
				b.line("You can't afford all of this yet: try the Cheapest preference or a gathering method.");
			}
			if (!GRAND_EXCHANGE.contains(s.getLocation()))
			{
				b.destination(GRAND_EXCHANGE.getPoint()).destinationName(GRAND_EXCHANGE.getName());
			}
			return b.build();
		}

		boolean gather = m.getType() != MethodType.PROCESS;
		boolean needsInputs = false;
		if (!gather)
		{
			for (ItemReq input : m.getInputs())
			{
				if (s.getInventory().count(input.getIds()) == 0)
				{
					needsInputs = true;
					break;
				}
			}
		}
		else
		{
			// Gathering with consumables (feathers, bait): only bank when they've run out.
			for (ItemReq input : m.getInputs())
			{
				if (s.carried(input.getIds()) == 0)
				{
					needsInputs = true;
					break;
				}
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
					int trip = Math.max(1, input.getQuantity());
					if (!gather || s.carried(input.getIds()) == 0)
					{
						b.line(String.format("• Withdraw %,d x %s", trip, input.getName()));
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

		if (!m.getLocation().contains(s.getLocation()))
		{
			return b.type(StepType.WALK)
				.title("Go to " + m.getLocation().getName())
				.destination(m.getLocation().getPoint())
				.destinationName(m.getLocation().getName())
				.line("Then: " + m.getInstruction() + ".")
				.build();
		}

		b.type(StepType.TRAIN).title(m.getInstruction());
		if (m.getNotes() != null)
		{
			b.line(m.getNotes());
		}
		return b.build();
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
			b.destination(bank.getPoint()).destinationName(bank.getName());
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
				b.line("• Optional: " + r.getText().replace(" (in bank)", ""));
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
