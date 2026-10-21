package com.skillinghelper.data;

import java.util.Collections;
import java.util.List;
import lombok.Value;
import lombok.With;

/**
 * An item the method needs. Any of {@link #ids} satisfies it (e.g. noted variants or upgraded versions).
 */
@Value
@With
public class ItemReq
{
	String name;
	int[] ids;
	/**
	 * How many to carry in the inventory for one trip. 0 means the whole stack (runes, arrows, coins...).
	 */
	int quantity;
	/**
	 * How many are used up per action. 0 for tools that are never consumed.
	 */
	int perAction;
	/**
	 * Something that replaces this item entirely when carried or worn, e.g. a staff of air instead of air runes.
	 * Null when there's no substitute.
	 */
	String substituteName;
	int[] substituteIds;
	/**
	 * How to make this item from {@link #recipe}, e.g. "Fill an empty basket with 5 cooking apples". Null when it
	 * can only be bought or gathered.
	 */
	String recipeText;
	/**
	 * What one of this item is made from; each part's {@link #perAction} is the amount per item made.
	 */
	List<ItemReq> recipe;
	/**
	 * Make it even when it has a GE price, e.g. because it's hardly ever sold there.
	 */
	boolean alwaysMake;
	/**
	 * Where the item is made, e.g. a furnace, the sawmill or a tanner, or null if it can be made anywhere.
	 */
	Location makeAt;
	/**
	 * What to click there to make it.
	 */
	Target makeTarget;
	/**
	 * For items the GE doesn't sell: where to get one, e.g. "Buy one from Turael for 1 gp". Null for GE items.
	 */
	String source;
	/**
	 * Where that is, or null for the method's own training spot.
	 */
	Location sourceAt;
	Target sourceTarget;

	private static ItemReq base(String name, int[] ids, int quantity, int perAction)
	{
		return new ItemReq(name, ids, quantity, perAction, null, null, null, Collections.emptyList(), false, null, null,
			null, null, null);
	}

	public static ItemReq tool(String name, int... ids)
	{
		return base(name, ids, 1, 0);
	}

	/**
	 * Several copies of a tool that isn't used up, e.g. bird snares or box traps.
	 */
	public static ItemReq tools(String name, int count, int... ids)
	{
		return base(name, ids, count, 0);
	}

	/**
	 * An optional supply to bring, e.g. 10 food. {@code carry} 0 means the whole stack.
	 */
	public static ItemReq supply(String name, int carry, int... ids)
	{
		return base(name, ids, carry, 0);
	}

	public static ItemReq consumed(String name, int carry, int perAction, int... ids)
	{
		return base(name, ids, carry, perAction);
	}

	/**
	 * A stackable item (runes, arrows) used {@code perAction} at a time. The whole stack is taken each trip.
	 */
	public static ItemReq stack(String name, int perAction, int... ids)
	{
		return base(name, ids, 0, perAction);
	}

	/**
	 * Copy of this requirement that is also satisfied by carrying or wearing one of {@code ids}.
	 */
	public ItemReq orUse(String name, int... ids)
	{
		return withSubstituteName(name).withSubstituteIds(ids);
	}

	/**
	 * Copy of this requirement that the guide can have the player make from {@code parts} when they don't have it,
	 * e.g. baskets of apples from cooking apples and empty baskets. Making is suggested when the player already has
	 * the parts, when it's cheaper than buying, or always if {@code alwaysMake}.
	 */
	public ItemReq madeFrom(String how, boolean alwaysMake, ItemReq... parts)
	{
		return withRecipeText(how).withRecipe(List.of(parts)).withAlwaysMake(alwaysMake);
	}

	/**
	 * Copy of this recipe requirement that has to be made at a particular place, by clicking {@code target}.
	 */
	public ItemReq madeAt(Location where, Target target)
	{
		return withMakeAt(where).withMakeTarget(target);
	}

	/**
	 * Copy of this requirement for an item the GE doesn't sell, with where to get it.
	 *
	 * @param where null for the method's training spot
	 */
	public ItemReq from(String how, Location where, Target target)
	{
		return withSource(how).withSourceAt(where).withSourceTarget(target);
	}

	/**
	 * One part of a recipe: {@code perItem} of these go into each item made.
	 */
	public static ItemReq part(String name, int perItem, int... ids)
	{
		return consumed(name, perItem, perItem, ids);
	}

	public boolean hasRecipe()
	{
		return !recipe.isEmpty();
	}

	public boolean hasSubstitute()
	{
		return substituteIds != null && substituteIds.length > 0;
	}

	public boolean isWholeStack()
	{
		return quantity <= 0;
	}

	public int primaryId()
	{
		return ids[0];
	}

	public boolean matches(int itemId)
	{
		for (int id : ids)
		{
			if (id == itemId)
			{
				return true;
			}
		}
		return false;
	}
}
