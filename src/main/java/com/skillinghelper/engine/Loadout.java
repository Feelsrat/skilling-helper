package com.skillinghelper.engine;

import com.skillinghelper.data.ItemReq;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/**
 * How much of each input to take out of the bank for one trip of a processing method: as many actions' worth as fit
 * in the free inventory slots, split by how many of each one action uses (14 buckets of sand + 14 soda ash, 9 iron
 * ore + 18 coal, 13 gold bars + 13 gems...). Capped by what the player owns and by what the goal still needs.
 */
@Value
public class Loadout
{
	public static final int INVENTORY_SIZE = 28;

	/**
	 * Actions one trip covers.
	 */
	int actions;
	/**
	 * How many of each input to carry when leaving the bank.
	 */
	Map<ItemReq, Integer> amounts;

	public int amount(ItemReq input)
	{
		return amounts.getOrDefault(input, 0);
	}

	/**
	 * @param inputs      inputs that fill the inventory: used up per action, one slot each
	 * @param keptIds     everything else the trip keeps in the inventory (tools, runes, supplies...)
	 * @param toWithdraw  slots still needed for kept items that are in the bank, e.g. a chisel not yet withdrawn
	 * @param actionsLeft actions the goal still needs, or a huge number when unknown
	 */
	public static Loadout of(List<ItemReq> inputs, Set<Integer> keptIds, int toWithdraw, PlayerState s, long actionsLeft)
	{
		if (inputs.isEmpty())
		{
			return new Loadout(0, Collections.emptyMap());
		}
		int kept = 0;
		for (int id : keptIds)
		{
			kept += s.slots(id);
		}
		int free = Math.max(0, INVENTORY_SIZE - kept - toWithdraw);
		int slotsPerAction = 0;
		long cap = Math.max(1, actionsLeft);
		for (ItemReq input : inputs)
		{
			slotsPerAction += input.getPerAction();
			// Don't plan for more than the player owns, so the other inputs don't end up left over
			cap = Math.min(cap, s.owned(input.getIds()) / input.getPerAction());
		}
		int actions = (int) Math.max(1, Math.min(cap, free / Math.max(1, slotsPerAction)));
		Map<ItemReq, Integer> amounts = new LinkedHashMap<>();
		for (ItemReq input : inputs)
		{
			amounts.put(input, actions * input.getPerAction());
		}
		return new Loadout(actions, amounts);
	}
}
