package com.skillinghelper.clue;

import com.skillinghelper.clue.data.ClueData;
import com.skillinghelper.clue.data.ClueTier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import lombok.Value;

/**
 * What a tier's reward casket is worth at today's Grand Exchange prices, and its most valuable uniques.
 */
public final class ClueRewards
{
	@Value
	public static class Pick
	{
		String name;
		long price;
		String rarity;
	}

	private ClueRewards()
	{
	}

	public static int maxRolls(ClueTier tier)
	{
		ClueData.Casket c = ClueData.casket(tier);
		return c == null ? 7 : c.getMaxRolls();
	}

	/**
	 * Average value of one casket: average rolls x the chance-weighted price of every reward. Untradeable items
	 * count as nothing; the master clue and Mimic chances aren't included.
	 */
	public static long averageValue(ClueTier tier, IntUnaryOperator price)
	{
		ClueData.Casket c = ClueData.casket(tier);
		if (c == null)
		{
			return 0;
		}
		double perRoll = 0;
		for (ClueData.Reward r : ClueData.rewards(tier))
		{
			if (r.getId() > 0 && !r.isTertiary() && !r.isMimicOnly())
			{
				perRoll += r.getChance() * r.averageQuantity() * Math.max(0, price.applyAsInt(r.getId()));
			}
		}
		return Math.round(perRoll * c.averageRolls());
	}

	/**
	 * The tier's most valuable uniques right now, most valuable first.
	 */
	public static List<Pick> topUniques(ClueTier tier, IntUnaryOperator price, int n)
	{
		List<Pick> all = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (ClueData.Reward r : ClueData.rewards(tier))
		{
			if (r.getId() <= 0 || !r.isUnique() || !seen.add(r.getName()))
			{
				continue;
			}
			long p = price.applyAsInt(r.getId());
			if (p > 0)
			{
				all.add(new Pick(r.getName(), p, r.getRarity()));
			}
		}
		all.sort((a, b) -> Long.compare(b.getPrice(), a.getPrice()));
		return all.size() > n ? new ArrayList<>(all.subList(0, n)) : all;
	}
}
