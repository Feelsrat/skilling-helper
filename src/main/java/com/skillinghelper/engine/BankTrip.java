package com.skillinghelper.engine;

import com.skillinghelper.data.BoostItem;
import com.skillinghelper.data.CombatGear;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.SkillOutfit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * What one bank trip takes out and puts back: how many of each input fit in the inventory, what the trip keeps, and
 * the "withdraw N x" lines for the bank step.
 */
final class BankTrip
{
	private final Recipes recipes;

	BankTrip(Recipes recipes)
	{
		this.recipes = recipes;
	}

	/**
	 * How many of each input to carry out of the bank on a processing trip, sized to the free inventory slots (see
	 * {@link Loadout}). Empty for gathering, where the room is for what's gathered and inputs keep their set amount.
	 */
	Map<ItemReq, Integer> tripAmounts(Method m, PlayerState s, long actionsLeft)
	{
		if (m.getType() != MethodType.PROCESS)
		{
			return Collections.emptyMap();
		}
		List<ItemReq> fill = new ArrayList<>();
		for (ItemReq input : m.getInputs())
		{
			if (fillsInventory(input, s, actionsLeft))
			{
				fill.add(input);
			}
		}
		Set<Integer> kept = keptIds(m);
		for (ItemReq input : fill)
		{
			ItemIds.removeAll(kept, input.getIds());
		}
		return Loadout.of(fill, kept, slotsToWithdraw(m, s), s, actionsLeft).getAmounts();
	}

	/**
	 * Whether an input shares out the free inventory slots, rather than being a single stack, made on the way, or
	 * replaced by a substitute.
	 */
	private boolean fillsInventory(ItemReq input, PlayerState s, long actionsLeft)
	{
		return !input.isWholeStack() && input.getPerAction() > 0 && !recipes.making(input, s, actionsLeft)
			&& !s.ownsSubstitute(input);
	}

	/**
	 * Inventory slots the trip's tools and stacks will take once they're out of the bank.
	 */
	private static int slotsToWithdraw(Method m, PlayerState s)
	{
		int slots = 0;
		if (m.getToolType() != null && ToolAdvisor.bestCarried(m.getToolType(), s) == null
			&& ToolAdvisor.bestOwned(m.getToolType(), s) != null)
		{
			slots++;
		}
		for (ItemReq tool : m.getTools())
		{
			if (s.carriesSubstitute(tool))
			{
				continue;
			}
			int want = tool.isWholeStack() ? 1 : tool.getQuantity();
			int have = tool.isWholeStack() ? Math.min(1, s.carried(tool.getIds())) : s.carried(tool.getIds());
			slots += Math.min(Math.max(0, want - have), s.bankCount(tool.getIds()));
		}
		for (ItemReq input : m.getInputs())
		{
			if (input.isWholeStack() && s.carried(input.getIds()) == 0 && s.bankCount(input.getIds()) > 0
				&& !s.carriesSubstitute(input))
			{
				slots++;
			}
		}
		return slots;
	}

	/**
	 * Every item a trip of this method keeps in the inventory: tools, inputs and their parts, supplies and gear.
	 * Anything else is put back in the bank.
	 */
	static Set<Integer> keptIds(Method m)
	{
		Set<Integer> ids = new HashSet<>();
		if (m.getToolType() != null)
		{
			ItemIds.addAll(ids, m.getToolType().allIds());
		}
		for (ItemReq tool : m.getTools())
		{
			ItemIds.addReq(ids, tool);
		}
		for (ItemReq input : m.getInputs())
		{
			ItemIds.addReq(ids, input);
			for (ItemReq part : input.getRecipe())
			{
				ItemIds.addReq(ids, part);
			}
		}
		for (ItemReq supply : m.getSupplies())
		{
			ItemIds.addReq(ids, supply);
		}
		addGearIds(ids, m);
		// Cash is handy to keep on you
		ids.add(ItemID.COINS);
		ids.add(ItemID.PLATINUM);
		return ids;
	}

	/**
	 * Boost items, skilling outfit pieces and combat gear the method suggests wearing.
	 */
	private static void addGearIds(Set<Integer> ids, Method m)
	{
		for (BoostItem boost : BoostItem.values())
		{
			if (boost.appliesTo(m))
			{
				ItemIds.addAll(ids, boost.getIds());
			}
		}
		for (SkillOutfit outfit : SkillOutfit.values())
		{
			if (outfit.getSkill() == m.getSkill())
			{
				for (int[] slot : outfit.getSlots())
				{
					ItemIds.addAll(ids, slot);
				}
			}
		}
		if (m.getGearStyle() == null)
		{
			return;
		}
		for (CombatGear slot : CombatGear.values())
		{
			if (slot.getStyle() == m.getGearStyle())
			{
				for (CombatGear.Tier t : slot.getTiers())
				{
					ItemIds.addAll(ids, t.getIds());
				}
			}
		}
	}

	/**
	 * Whether the player has to fetch more of an input before training. Processing needs it in the inventory;
	 * gathering with consumables (feathers, bait, runes) only banks once they've run out.
	 */
	static boolean needsWithdraw(ItemReq input, PlayerState s, boolean gather)
	{
		if (s.carriesSubstitute(input))
		{
			return false;
		}
		int have = gather || input.isWholeStack() ? s.carried(input.getIds()) : s.getInventory().count(input.getIds());
		return have == 0;
	}

	/**
	 * Lines for the inputs to take out. Processing tops every input up to this trip's loadout (and puts back any
	 * extra); gathering takes a fresh set of whatever ran out.
	 */
	void withdrawInputs(GuideStep.GuideStepBuilder b, Method m, PlayerState s, boolean gather, long actionsLeft)
	{
		Map<ItemReq, Integer> trip = tripAmounts(m, s, actionsLeft);
		List<String> plan = new ArrayList<>();
		for (ItemReq listed : m.getInputs())
		{
			ItemReq input = CombatGearAdvisor.resolve(listed, s);
			if (s.carriesSubstitute(input))
			{
				continue;
			}
			Integer target = trip.get(input);
			if (target == null)
			{
				withdrawFreshSet(b, input, s, gather);
				continue;
			}
			plan.add(String.format("%,d x %s", target, input.getName().toLowerCase()));
			topUp(b, input, s, target);
		}
		if (!plan.isEmpty())
		{
			b.line("This trip: " + String.join(" + ", plan) + ".");
		}
	}

	private static void withdrawFreshSet(GuideStep.GuideStepBuilder b, ItemReq input, PlayerState s, boolean gather)
	{
		if (!needsWithdraw(input, s, gather))
		{
			return;
		}
		b.line(input.isWholeStack()
			? "• Withdraw all your " + input.getName().toLowerCase()
			: String.format("• Withdraw %,d x %s", input.getQuantity(), input.getName()));
		b.withdraw(input.getIds(), input.getQuantity());
	}

	/**
	 * Brings a carried input to exactly the trip's amount: withdraw what's short, or deposit the extra.
	 */
	private static void topUp(GuideStep.GuideStepBuilder b, ItemReq input, PlayerState s, int target)
	{
		int carried = s.getInventory().count(input.getIds());
		if (carried < target)
		{
			int more = target - carried;
			b.line(carried == 0
				? String.format("• Withdraw %,d x %s", more, input.getName())
				: String.format("• Withdraw %,d more %s (%,d for this trip)", more, input.getName().toLowerCase(),
					target));
			b.withdraw(input.getIds(), more);
		}
		else if (carried > target)
		{
			int extra = carried - target;
			b.line(String.format("• Deposit %,d x %s (%,d is enough for this trip)", extra, input.getName(), target));
			for (int id : input.getIds())
			{
				if (s.getInventory().has(id))
				{
					b.deposit(id, extra);
				}
			}
		}
	}

	/**
	 * Lines for the parts to take out on a trip that makes inputs (sand and soda ash for molten glass...).
	 */
	static void withdrawParts(GuideStep.GuideStepBuilder b, List<ItemReq> toMake, PlayerState s, long actionsLeft)
	{
		for (ItemReq input : toMake)
		{
			int perTrip = Recipes.perTrip(input, s, actionsLeft);
			for (ItemReq part : input.getRecipe())
			{
				int amount = part.getPerAction() == 0 ? 1 : perTrip * part.getPerAction();
				int more = Math.max(0, amount - s.getInventory().count(part.getIds()));
				if (more == 0)
				{
					continue;
				}
				b.line(part.getPerAction() == 0
					? String.format("• Withdraw a %s (to make %s)", part.getName().toLowerCase(),
						input.getName().toLowerCase())
					: String.format("• Withdraw %,d x %s (to make %s)", more, part.getName(),
						input.getName().toLowerCase()));
				b.withdraw(part.getIds(), more);
			}
		}
	}
}
