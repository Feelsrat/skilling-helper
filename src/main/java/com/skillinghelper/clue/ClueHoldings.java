package com.skillinghelper.clue;

import com.skillinghelper.clue.data.ClueItem;
import com.skillinghelper.clue.data.ClueTier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntFunction;
import lombok.Value;

/**
 * Every clue scroll, scroll box, clue bottle/nest/geode and reward casket the player has, per tier, split into what
 * they carry and what's in the bank. Compared by value, so the panel only redraws when something changed.
 */
@Value
public class ClueHoldings
{
	/**
	 * How many of one kind of clue item of one tier the player has, and their item ids (carried first).
	 */
	@Value
	public static class Stack
	{
		int carried;
		int banked;
		List<Integer> carriedIds;
		List<Integer> bankIds;

		public int total()
		{
			return carried + banked;
		}

		/**
		 * Every id seen, carried ones first.
		 */
		public int[] ids()
		{
			List<Integer> all = new ArrayList<>(carriedIds);
			for (int id : bankIds)
			{
				if (!all.contains(id))
				{
					all.add(id);
				}
			}
			return all.stream().mapToInt(Integer::intValue).toArray();
		}
	}

	private static final Stack NONE = new Stack(0, 0, Collections.emptyList(), Collections.emptyList());

	Map<ClueTier, Map<ClueItem.Kind, Stack>> stacks;
	/**
	 * The bank has been opened this session, so {@link Stack#getBanked()} can be trusted.
	 */
	boolean bankKnown;

	public Stack get(ClueTier tier, ClueItem.Kind kind)
	{
		Map<ClueItem.Kind, Stack> byKind = stacks.get(tier);
		Stack s = byKind == null ? null : byKind.get(kind);
		return s == null ? NONE : s;
	}

	public boolean isEmpty()
	{
		return stacks.isEmpty();
	}

	/**
	 * Sorts what's carried and what's banked into tiers and kinds.
	 *
	 * @param classify item id to what it is, or null when it isn't a clue item
	 */
	public static ClueHoldings of(Map<Integer, Integer> carried, Map<Integer, Integer> bank,
		IntFunction<ClueItem> classify)
	{
		Map<ClueTier, Map<ClueItem.Kind, int[]>> counts = new EnumMap<>(ClueTier.class);
		Map<ClueTier, Map<ClueItem.Kind, List<List<Integer>>>> ids = new EnumMap<>(ClueTier.class);
		add(new TreeMap<>(carried), 0, classify, counts, ids);
		if (bank != null)
		{
			add(new TreeMap<>(bank), 1, classify, counts, ids);
		}
		Map<ClueTier, Map<ClueItem.Kind, Stack>> stacks = new EnumMap<>(ClueTier.class);
		counts.forEach((tier, byKind) ->
		{
			Map<ClueItem.Kind, Stack> out = new EnumMap<>(ClueItem.Kind.class);
			byKind.forEach((kind, n) ->
			{
				List<List<Integer>> idLists = ids.get(tier).get(kind);
				out.put(kind, new Stack(n[0], n[1], idLists.get(0), idLists.get(1)));
			});
			stacks.put(tier, out);
		});
		return new ClueHoldings(stacks, bank != null);
	}

	private static void add(Map<Integer, Integer> items, int where, IntFunction<ClueItem> classify,
		Map<ClueTier, Map<ClueItem.Kind, int[]>> counts, Map<ClueTier, Map<ClueItem.Kind, List<List<Integer>>>> ids)
	{
		items.forEach((id, n) ->
		{
			ClueItem c = n > 0 ? classify.apply(id) : null;
			if (c == null)
			{
				return;
			}
			counts.computeIfAbsent(c.getTier(), k -> new EnumMap<>(ClueItem.Kind.class))
				.computeIfAbsent(c.getKind(), k -> new int[2])[where] += n;
			List<List<Integer>> lists = ids.computeIfAbsent(c.getTier(), k -> new EnumMap<>(ClueItem.Kind.class))
				.computeIfAbsent(c.getKind(), k -> List.of(new ArrayList<>(), new ArrayList<>()));
			lists.get(where).add(id);
		});
	}
}
