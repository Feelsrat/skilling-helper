package com.skillinghelper.banked;

import com.skillinghelper.banked.data.BankedActivities;
import com.skillinghelper.banked.data.BankedActivity;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.ItemHighlight;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Requirement;
import java.util.ArrayList;
import java.util.Collections;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BankedPlannerTest
{
	private static final WorldPoint GRAND_EXCHANGE = new WorldPoint(3164, 3486, 0);
	private static final int MAX_STEPS = 600;
	private static final int MAX_BUY_STEPS = 3;

	private final BankedPlanner planner = new BankedPlanner(id -> 100);

	@Test
	public void everyActivityCountsItsOwnFirstInput()
	{
		for (BankedActivity a : BankedActivities.all())
		{
			ItemReq key = a.keyInput();
			boolean counted = false;
			for (int id : a.getCountIds())
			{
				counted |= key.matches(id);
			}
			assertTrue(a.getId(), counted);
			assertTrue(a.getId(), a.getPerAction() >= 1);
			assertEquals(a.getId(), key.getPerAction(), a.getPerAction());
		}
	}

	@Test
	public void picksTheMostXpPerItemAndRespectsChoices()
	{
		PlayerState s = state(Map.of(ItemID.YEW_LOGS, 1_000), 99);
		BankedPlanner.Group logs = only(planner.groups(s, Collections.emptyMap()));
		assertEquals("banked_burn_yew_logs", logs.getChosen().getActivity().getId());
		assertEquals(202_500, logs.getChosen().getXp());

		logs = only(planner.groups(s, Map.of(logs.getKey(), "banked_fletch_yew_longbow")));
		assertEquals("banked_fletch_yew_longbow", logs.getChosen().getActivity().getId());

		logs = only(planner.groups(s, Map.of(logs.getKey(), BankedPlanner.SKIP)));
		assertNull(logs.getChosen());
		assertTrue(BankedPlanner.queue(List.of(logs)).isEmpty());
	}

	@Test
	public void aSkillFocusKeepsItemsForThatSkill()
	{
		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(ItemID.OAK_LOGS, 80);
		bank.put(ItemID.PLANK_OAK, 16);
		bank.put(ItemID.DRAGON_BONES, 10);
		PlayerState s = state(bank, 99);

		List<BankedPlanner.Group> construction = planner.groups(s, Collections.emptyMap(), Skill.CONSTRUCTION);
		BankedPlanner.Group oak = only(construction);
		assertEquals(96, oak.getCount());
		assertEquals("banked_con_oak_larders", oak.getChosen().getActivity().getId());
		assertEquals(12, oak.getChosen().getActions());

		List<BankedPlanner.Group> firemaking = planner.groups(s, Collections.emptyMap(), Skill.FIREMAKING);
		assertEquals("banked_burn_oak_logs", only(firemaking).getChosen().getActivity().getId());

		// A saved choice for another skill doesn't apply while focused
		Map<String, String> choices = Map.of(oak.getKey(), "banked_burn_oak_logs");
		assertEquals("banked_con_oak_larders",
			only(planner.groups(s, choices, Skill.CONSTRUCTION)).getChosen().getActivity().getId());
		assertEquals(2, planner.groups(s, choices, null).size());
	}

	@Test
	public void lockedActivitiesAreNotChosen()
	{
		PlayerState s = state(Map.of(ItemID.YEW_LOGS, 10), 30);
		BankedPlanner.Group logs = only(planner.groups(s, Collections.emptyMap()));
		// Firemaking 30 can't burn yews, Fletching 30 can't cut them: nothing to do yet
		assertNull(logs.getChosen());
		for (BankedPlanner.Option o : logs.getOptions())
		{
			assertNotNull(o.getActivity().getId(), o.getLocked());
		}
	}

	@Test
	public void herbsAreCleanedBeforePotionsAndCountTowardsThem()
	{
		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(ItemID.UNIDENTIFIED_RANARR, 50);
		bank.put(ItemID.RANARR_WEED, 20);
		bank.put(ItemID.RANARRVIAL, 30);
		PlayerState s = state(bank, 99);
		List<BankedPlanner.Option> queue = BankedPlanner.queue(planner.groups(s, Collections.emptyMap()));
		assertEquals("banked_clean_ranarr_weed", queue.get(0).getActivity().getId());
		assertEquals("banked_potion_prayer_potions", queue.get(1).getActivity().getId());
		assertEquals(50, queue.get(1).getActions());
	}

	@Test
	public void totalsGiveTheLevelAfter()
	{
		PlayerState s = state(Map.of(ItemID.DRAGON_BONES, 1_000), 1);
		List<BankedPlanner.SkillTotal> totals = BankedPlanner.totals(
			BankedPlanner.queue(planner.groups(s, Collections.emptyMap())), s);
		assertEquals(1, totals.size());
		assertEquals(72_000, totals.get(0).getXp());
		assertEquals(Experience.getLevelForXp(Experience.getXpForLevel(1) + 72_000), totals.get(0).getLevelAfter());
	}

	/**
	 * Plays every activity like an obedient player, from a bank holding only the banked items and coins, until the
	 * items are used up.
	 */
	@Test
	public void everyActivityUsesUpTheBankWithoutGettingStuck()
	{
		List<String> problems = new ArrayList<>();
		for (BankedActivity a : BankedActivities.all())
		{
			for (int countId : a.getCountIds())
			{
				String problem = new Player(a, countId, 60 * a.getPerAction()).play();
				if (problem != null)
				{
					problems.add(a.getId() + " (from " + countId + "): " + problem);
				}
			}
		}
		assertTrue(String.join("\n", problems), problems.isEmpty());
	}

	private static BankedPlanner.Group only(List<BankedPlanner.Group> groups)
	{
		assertEquals(1, groups.size());
		return groups.get(0);
	}

	private static PlayerState state(Map<Integer, Integer> bank, int level)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill skill : Skill.values())
		{
			levels.put(skill, level);
			xp.put(skill, Experience.getXpForLevel(level));
		}
		return PlayerState.builder()
			.levels(levels)
			.xp(xp)
			.bank(new ItemCounts(bank))
			.freeInventorySlots(28)
			.location(GRAND_EXCHANGE)
			.membersWorld(true)
			.completedQuests(EnumSet.allOf(Quest.class))
			.build();
	}

	/**
	 * A simulated player following one activity's guide.
	 */
	private final class Player
	{
		private final BankedActivity activity;
		private final Set<Integer> stackable = new HashSet<>();
		private final Map<Integer, Integer> inventory = new HashMap<>();
		private final Map<Integer, Integer> bank = new HashMap<>();
		private final Map<Integer, Integer> worn = new HashMap<>();
		private final Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		private WorldPoint location = GRAND_EXCHANGE;
		private int buySteps;
		private Method method;

		Player(BankedActivity activity, int itemId, int amount)
		{
			this.activity = activity;
			stackable.add(ItemID.COINS);
			for (ItemReq r : allItems(activity.getMethod()))
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
			bank.put(itemId, amount);
			for (Skill s : Skill.values())
			{
				xp.put(s, Experience.getXpForLevel(99));
			}
		}

		String play()
		{
			String lastStep = null;
			int repeats = 0;
			for (int i = 0; i < MAX_STEPS; i++)
			{
				PlayerState s = state();
				if (activity.actions(s) == 0)
				{
					return null;
				}
				BankedPlanner.Plan plan = planner.plan(activity, s);
				method = plan.getMethod();
				GuideStep step = plan.getStep();
				String key = step.getType() + " " + step.getTitle() + " " + inventory + " " + worn + " " + location;
				repeats = key.equals(lastStep) ? repeats + 1 : 0;
				if (repeats >= 2)
				{
					return "stuck on " + step.getType() + " '" + step.getTitle() + "' " + step.getDetails()
						+ " carrying " + inventory;
				}
				lastStep = key;
				String problem = follow(step, s, plan.getRequirements());
				if (problem != null)
				{
					return problem;
				}
				if (slots(inventory) > 28)
				{
					return "inventory overfilled after " + step.getType() + ": " + inventory;
				}
			}
			return "not done after " + MAX_STEPS + " steps, " + activity.actions(state()) + " actions left";
		}

		private String follow(GuideStep step, PlayerState s, List<Requirement> reqs)
		{
			switch (step.getType())
			{
				case DONE:
					return "guide said done: " + step.getTitle() + " " + step.getDetails();
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
					if (reqs.stream().anyMatch(r -> r.getStatus() == Requirement.Status.MISSING
						&& activity.keyInput().matches(r.getItemIds().length == 0 ? -1 : r.getItemIds()[0])))
					{
						return "asked to buy the banked item itself: " + step.getDetails();
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
				case TRAIN:
					return train(s);
				default:
					return "unexpected step " + step.getType();
			}
		}

		private void bank(GuideStep step)
		{
			step.getDepositAmounts().forEach((id, amount) ->
				move(inventory, bank, id, amount == 0 ? inventory.getOrDefault(id, 0) : amount));
			step.getWithdrawAmounts().forEach((id, amount) ->
			{
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
			if (!method.isAt(location, false))
			{
				return "told to train away from the spot";
			}
			int actions = Integer.MAX_VALUE;
			for (ItemReq input : method.getInputs())
			{
				if (input.getPerAction() > 0)
				{
					actions = Math.min(actions, count(inventory, input.getIds()) / input.getPerAction());
				}
			}
			if (actions == 0 || actions == Integer.MAX_VALUE)
			{
				return "told to train without the inputs, carrying " + inventory;
			}
			for (ItemReq tool : method.getTools())
			{
				if (count(inventory, tool.getIds()) == 0)
				{
					return "told to train without a " + tool.getName();
				}
			}
			for (ItemReq input : method.getInputs())
			{
				take(inventory, input.getIds(), actions * input.getPerAction());
			}
			if (!method.getProducts().isEmpty())
			{
				inventory.merge(method.getProducts().get(0), actions, Integer::sum);
			}
			else
			{
				// Something to deposit, like the real product, which stacks when the inputs do (arrows, darts)
				if (method.getInputs().stream().allMatch(ItemReq::isWholeStack))
				{
					stackable.add(ItemID.BURNT_MONKFISH);
				}
				inventory.merge(ItemID.BURNT_MONKFISH, actions, Integer::sum);
			}
			xp.merge(method.getSkill(), (int) Math.round(actions * method.getXpPerAction()), Integer::sum);
			return null;
		}

		private PlayerState state()
		{
			Map<Integer, Integer> slotCounts = new HashMap<>();
			inventory.forEach((id, n) -> slotCounts.put(id, stackable.contains(id) ? 1 : n));
			Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
			xp.forEach((skill, x) -> levels.put(skill, Experience.getLevelForXp(x)));
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
