package com.skillinghelper.travel;

import java.util.List;
import java.util.Map;
import lombok.Value;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * One way to teleport somewhere with an item: a jewellery or cape teleport, a tablet, or a fairy ring (which needs a
 * fairy ring staff). From the Shortest Path plugin's own transport data, see {@link TravelData}.
 */
@Value
public class TravelOption
{
	public enum Kind
	{
		ITEM,
		/**
		 * A fairy ring code: needs a Dramen or Lunar staff unless the Lumbridge elite diary is done.
		 */
		FAIRY_RING,
	}

	Kind kind;
	/**
	 * The item's name, e.g. "Skills necklace", or "Fairy ring".
	 */
	String group;
	/**
	 * Where it goes, e.g. "Fishing Guild", or the fairy ring code ("B K R").
	 */
	String place;
	WorldPoint point;
	/**
	 * Item ids that can do it. Empty for fairy rings.
	 */
	int[] itemIds;
	/**
	 * Using it spends a charge, a tablet or a scroll.
	 */
	boolean consumable;
	/**
	 * Quests that must be finished, by name.
	 */
	List<String> quests;
	Map<Skill, Integer> skills;
	List<Check> varbits;
	List<Check> varps;

	/**
	 * A game variable that must be equal to, below or above a value.
	 */
	@Value
	public static class Check
	{
		int id;
		char op;
		int value;

		public boolean test(int actual)
		{
			switch (op)
			{
				case '<':
					return actual < value;
				case '>':
					return actual > value;
				default:
					return actual == value;
			}
		}
	}

	/**
	 * How it reads in a guide: "Fishing Guild teleport", "fairy ring B-K-R", or just "teleport" for a tablet that is
	 * named after where it goes.
	 */
	public String how()
	{
		if (kind == Kind.FAIRY_RING)
		{
			return "ZANARIS".equalsIgnoreCase(place) ? "fairy ring to Zanaris" : "fairy ring " + place.replace(' ', '-');
		}
		return place.equals(group) ? "teleport" : place + " teleport";
	}
}
