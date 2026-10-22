package com.skillinghelper;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.ItemHighlight;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Plays every gathering and processing guide like an obedient player, starting with nothing but cash at the Grand
 * Exchange: buy what the guide says, bank as it says, make what it says, train, and repeat for three levels. Catches
 * loops (the same bank step forever), overfilled inventories, training without the inputs, and buying over and over.
 */
public class GuidePlaythroughTest
{
	private static final int MAX_STEPS = 400;
	private static final int MAX_BUY_STEPS = 3;
	private static final WorldPoint GRAND_EXCHANGE = new WorldPoint(3164, 3486, 0);
	private static final PriceLookup PRICES = id -> 100;

	@Test
	public void everyGuidePlaysThroughWithoutGettingStuck()
	{
		StepResolver resolver = new StepResolver(PRICES);
		List<String> problems = new ArrayList<>();
		for (Method m : new GuideRepository().all())
		{
			if (m.getType() != MethodType.ACTIVITY && m.getStops().isEmpty())
			{
				String problem = new Player(m, resolver).play();
				if (problem != null)
				{
					problems.add(m.getId() + ": " + problem);
				}
			}
		}
		assertTrue(String.join("\n", problems), problems.isEmpty());
	}

	/**
	 * A simulated player following one method's guide.
	 */
	private static final class Player
	{
		private final Method method;
		private final StepResolver resolver;
		private final Set<Integer> stackable = new HashSet<>();
		private final Map<Integer, Integer> inventory = new HashMap<>();
		private final Map<Integer, Integer> bank = new HashMap<>();
		private final Map<Integer, Integer> worn = new HashMap<>();
		private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		private final Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		private WorldPoint location = GRAND_EXCHANGE;
		private int buySteps;

		Player(Method method, StepResolver resolver)
		{
			this.method = method;
			this.resolver = resolver;
			stackable.add(ItemID.COINS);
			for (ItemReq r : allItems(method))
			{
				if (r.isWholeStack())
				{
					for (int id : r.getIds())
					{
						stackable.add(id);
					}
				}
			}
			bank.put(ItemID.COINS, 50_000_000);
			for (Skill s : Skill.values())
			{
				int level = s == method.getSkill() ? Math.max(1, method.getMinLevel()) : 70;
				levels.put(s, level);
				xp.put(s, Experience.getXpForLevel(level));
			}
		}

		/**
		 * @return what went wrong, or null if the guide got the player to the goal (or kept them busy until the step
		 * limit, for long goals)
		 */
		String play()
		{
			Skill skill = method.getSkill();
			int target = Math.min(99, levels.get(skill) + 3);
			String lastStep = null;
			int repeats = 0;
			for (int i = 0; i < MAX_STEPS; i++)
			{
				levels.put(skill, Experience.getLevelForXp(xp.get(skill)));
				PlayerState s = state();
				if (s.level(skill) >= target)
				{
					return null;
				}
				List<Requirement> reqs = resolver.requirements(method, s, actionsLeft(s, target));
				GuideStep step = resolver.resolve(method, s, target, method.isDropByDefault(), reqs);

				String key = step.getType() + " " + step.getTitle() + " " + inventory + " " + worn + " " + location;
				repeats = key.equals(lastStep) ? repeats + 1 : 0;
				if (repeats >= 2 && step.getType() != StepType.TRAIN)
				{
					return "stuck on " + step.getType() + " '" + step.getTitle() + "' " + step.getDetails()
						+ " carrying " + inventory;
				}
				lastStep = key;

				String problem = follow(step, s, reqs);
				if (problem != null)
				{
					return problem;
				}
				if (slots(inventory) > 28)
				{
					return "inventory overfilled after " + step.getType() + ": " + inventory;
				}
			}
			return null;
		}

		private String follow(GuideStep step, PlayerState s, List<Requirement> reqs)
		{
			switch (step.getType())
			{
				case DONE:
				case WAIT:
					return null;
				case WALK:
					location = step.getDestination();
					return null;
				case BANK:
					if (step.getRoute() != null)
					{
						location = step.getDestination();
					}
					else
					{
						bank(step);
					}
					return null;
				case GET_ITEMS:
					if (step.getRoute() != null)
					{
						location = step.getDestination();
						return null;
					}
					return ++buySteps > MAX_BUY_STEPS ? "kept asking to buy: " + step.getDetails() : buy(reqs);
				case MAKE:
					make();
					return null;
				case EQUIP:
					step.getItemHighlights().forEach((id, h) ->
					{
						if (h == ItemHighlight.EQUIP)
						{
							move(inventory, worn, id, 1);
						}
					});
					return null;
				case DROP:
					method.getProducts().forEach(inventory::remove);
					return null;
				case TRAIN:
					return train(s);
				default:
					return null;
			}
		}

		private void bank(GuideStep step)
		{
			step.getDepositAmounts().forEach((id, amount) ->
				move(inventory, bank, id, amount == 0 ? inventory.getOrDefault(id, 0) : amount));
			step.getWithdrawAmounts().forEach((id, amount) ->
			{
				// Withdrawing more than a full inventory only makes sense for something that stacks (feathers, runes)
				if (amount > 28)
				{
					stackable.add(id);
				}
				int have = bank.getOrDefault(id, 0);
				int want = amount == 0 ? have : Math.min(have, amount);
				if (!stackable.contains(id))
				{
					want = Math.min(want, 28 - slots(inventory));
				}
				move(bank, inventory, id, want);
			});
		}

		/**
		 * Buys exactly what the guide says is missing.
		 */
		private String buy(List<Requirement> reqs)
		{
			for (Requirement r : reqs)
			{
				if (r.getStatus() == Requirement.Status.MISSING && r.getItemIds().length > 0)
				{
					long amount = Math.max(Math.max(1, r.getQuantity()), r.getBuyCost() / 100);
					bank.merge(r.getItemIds()[0], (int) amount, Integer::sum);
				}
			}
			return null;
		}

		/**
		 * Makes every input it can from the parts carried.
		 */
		private void make()
		{
			for (ItemReq input : method.getInputs())
			{
				int made = Integer.MAX_VALUE;
				for (ItemReq part : input.getRecipe())
				{
					if (part.getPerAction() > 0)
					{
						made = Math.min(made, count(inventory, part.getIds()) / part.getPerAction());
					}
				}
				if (!input.hasRecipe() || made == Integer.MAX_VALUE || made == 0)
				{
					continue;
				}
				for (ItemReq part : input.getRecipe())
				{
					take(inventory, part.getIds(), made * part.getPerAction());
				}
				inventory.merge(input.primaryId(), made, Integer::sum);
			}
		}

		private String train(PlayerState s)
		{
			int actions = method.getType() == MethodType.GATHER ? 28 - slots(inventory) : Integer.MAX_VALUE;
			for (ItemReq input : method.getInputs())
			{
				boolean replaced = input.hasSubstitute() && s.carried(input.getSubstituteIds()) > 0;
				if (input.getPerAction() > 0 && !replaced)
				{
					actions = Math.min(actions, count(inventory, input.getIds()) / input.getPerAction());
				}
			}
			if (actions == Integer.MAX_VALUE)
			{
				actions = 27;
			}
			if (actions == 0 && (method.getType() == MethodType.PROCESS || !method.getProducts().isEmpty()))
			{
				return "told to train without the inputs or room for them, carrying " + inventory;
			}
			for (ItemReq input : method.getInputs())
			{
				take(inventory, input.getIds(), actions * input.getPerAction());
			}
			if (!method.getProducts().isEmpty())
			{
				inventory.merge(method.getProducts().get(0), actions, Integer::sum);
			}
			Skill skill = method.getSkill();
			xp.merge(skill, (int) Math.round(actions * method.xpPerAction(levels.get(skill))), Integer::sum);
			return null;
		}

		private PlayerState state()
		{
			Map<Integer, Integer> slotCounts = new HashMap<>();
			inventory.forEach((id, n) -> slotCounts.put(id, stackable.contains(id) ? 1 : n));
			return PlayerState.builder()
				.levels(levels)
				.xp(xp)
				.inventory(new ItemCounts(inventory))
				.inventorySlots(new ItemCounts(slotCounts))
				.equipment(new ItemCounts(worn))
				.bank(new ItemCounts(bank))
				.freeInventorySlots(28 - slots(inventory))
				.location(location)
				.membersWorld(true)
				.completedQuests(EnumSet.allOf(Quest.class))
				.build();
		}

		private int actionsLeft(PlayerState s, int target)
		{
			double perAction = method.xpPerAction(s.level(method.getSkill()));
			long xpLeft = Experience.getXpForLevel(target) - s.xp(method.getSkill());
			return (int) Math.max(1, Math.ceil(xpLeft / perAction));
		}

		private int slots(Map<Integer, Integer> items)
		{
			int slots = 0;
			for (Map.Entry<Integer, Integer> e : items.entrySet())
			{
				slots += stackable.contains(e.getKey()) ? 1 : e.getValue();
			}
			return slots;
		}
	}

	private static List<ItemReq> allItems(Method m)
	{
		List<ItemReq> items = new ArrayList<>(m.getTools());
		items.addAll(m.getInputs());
		for (ItemReq input : m.getInputs())
		{
			items.addAll(input.getRecipe());
		}
		items.addAll(m.getSupplies());
		return items;
	}

	private static int count(Map<Integer, Integer> items, int[] ids)
	{
		int n = 0;
		for (int id : ids)
		{
			n += items.getOrDefault(id, 0);
		}
		return n;
	}

	private static void take(Map<Integer, Integer> items, int[] ids, int amount)
	{
		for (int id : ids)
		{
			int taken = Math.min(items.getOrDefault(id, 0), amount);
			amount -= taken;
			move(items, new HashMap<>(), id, taken);
		}
	}

	private static void move(Map<Integer, Integer> from, Map<Integer, Integer> to, int id, int amount)
	{
		int have = from.getOrDefault(id, 0);
		amount = Math.min(amount, have);
		if (amount <= 0)
		{
			return;
		}
		if (have == amount)
		{
			from.remove(id);
		}
		else
		{
			from.put(id, have - amount);
		}
		to.merge(id, amount, Integer::sum);
	}
}
