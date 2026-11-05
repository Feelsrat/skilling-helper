package com.skillinghelper.engine;

import com.skillinghelper.data.ItemReq;

/**
 * Narrows an item that has several versions (any pickaxe, food, compost...) to the single best one the player owns
 * and can use, so lower tiers aren't suggested or highlighted. Bank placeholders never count as owned (see
 * {@code BankItems}).
 */
final class BestOwned
{
	/**
	 * The best version the player owns. Unchanged when the item has no versions or the player owns none.
	 */
	static ItemReq of(ItemReq req, PlayerState s)
	{
		ItemReq tool = ToolAdvisor.bestOf(req, s);
		if (tool != req || req.getTierNames() == null)
		{
			return tool;
		}
		int[] ids = req.getIds();
		int best = -1;
		for (int i = 0; i < ids.length && best == -1; i++)
		{
			if (s.owned(ids[i]) > 0)
			{
				best = i;
			}
		}
		if (best == -1)
		{
			return req;
		}
		return req.withName(req.getTierNames().get(best)).withIds(new int[]{ids[best]}).withTierNames(null);
	}

	private BestOwned()
	{
	}
}
