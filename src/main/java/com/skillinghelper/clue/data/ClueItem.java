package com.skillinghelper.clue.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;
import net.runelite.api.gameval.ItemID;

/**
 * What a clue-related item is, worked out from its name: every clue scroll step is its own item id (hundreds per
 * tier), but they all share the name "Clue scroll (easy)" and so on.
 */
@Value
public class ClueItem
{
	public enum Kind
	{
		/**
		 * A clue in progress: the scroll itself, or the casket, puzzle box, light box or challenge scroll a step gives.
		 * Only one per tier can be held (inventory, bank and Death's office together).
		 */
		CLUE,
		/**
		 * A stackable scroll box, opened for a clue once you hold none of its tier.
		 */
		SCROLL_BOX,
		/**
		 * A clue bottle, nest or geode from skilling: open it for a clue.
		 */
		CONTAINER,
		/**
		 * The reward casket at the end of a trail.
		 */
		REWARD_CASKET,
		/**
		 * A casket that turned into the Mimic: take it to Watson's house.
		 */
		MIMIC
	}

	private static final Pattern NAME = Pattern.compile(
		"^(Clue scroll|Challenge scroll|Puzzle box|Light box|Casket|Scroll box|Reward casket|Clue bottle|Clue nest"
			+ "|Clue geode|Key) \\((beginner|easy|medium|hard|elite|master)\\)$");

	ClueTier tier;
	Kind kind;

	/**
	 * Classifies an item by its id and name, or returns null when it isn't a clue item.
	 */
	public static ClueItem of(int id, String name)
	{
		if (id == ItemID.TRAIL_MIMIC_CASKET)
		{
			return new ClueItem(ClueTier.MASTER, Kind.MIMIC);
		}
		if (name == null)
		{
			return null;
		}
		Matcher m = NAME.matcher(name);
		if (!m.matches())
		{
			return null;
		}
		ClueTier tier = ClueTier.fromKey(m.group(2));
		switch (m.group(1))
		{
			case "Scroll box":
				return new ClueItem(tier, Kind.SCROLL_BOX);
			case "Reward casket":
				return new ClueItem(tier, Kind.REWARD_CASKET);
			case "Clue bottle":
			case "Clue nest":
			case "Clue geode":
				return new ClueItem(tier, Kind.CONTAINER);
			default:
				return new ClueItem(tier, Kind.CLUE);
		}
	}
}
