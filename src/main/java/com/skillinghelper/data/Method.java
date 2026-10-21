package com.skillinghelper.data;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * One way of training a skill, e.g. "Willow trees at Draynor". Rates and requirements come from the
 * OSRS wiki training guides (see {@link #wikiPage}).
 */
@Value
@Builder
public class Method
{
	@NonNull
	String id;
	@NonNull
	Skill skill;
	@NonNull
	String name;
	@NonNull
	MethodType type;
	int minLevel;
	/**
	 * Combat level needed, e.g. for Slayer masters. 0 when there's no requirement.
	 */
	int combatLevel;
	/**
	 * Approximate xp per hour at {@link #minLevel} with a typical setup.
	 */
	int xpPerHour;
	/**
	 * Xp for one action, used to work out how many actions (and items) the goal needs.
	 */
	double xpPerAction;
	boolean members;
	@Singular
	Set<MethodTag> tags;

	/**
	 * Where the training happens. For bank-standing methods this is the bank itself.
	 */
	@NonNull
	Location location;
	/**
	 * Other places that count as being at the training spot, e.g. the inside of a runecrafting altar.
	 */
	@Singular("alsoAt")
	List<Location> alsoAt;
	/**
	 * Training happens in an instance (e.g. the player's house), so being in any instance counts as being there.
	 */
	boolean instanced;
	/**
	 * Nearest bank, or null if the method never banks.
	 */
	Location bank;
	/**
	 * Drop produce instead of banking when the inventory is full (power-training).
	 */
	boolean dropByDefault;

	/**
	 * Tool family the method needs; the best usable tier is recommended.
	 */
	ToolType toolType;
	/**
	 * Other items that must be in the inventory (fishing net, tinderbox, knife...).
	 */
	@Singular
	List<ItemReq> tools;
	/**
	 * Items consumed by the method (logs, raw fish, feathers...).
	 */
	@Singular
	List<ItemReq> inputs;
	/**
	 * Items the method produces. Used for drop/deposit highlights and profit estimates.
	 */
	@Singular
	List<Integer> products;
	/**
	 * Coins gained (or spent, if negative) per action that aren't an item, e.g. High Alchemy or pickpocketing.
	 */
	int coinsPerAction;

	/**
	 * Things to click in the world (trees, fishing spots, ranges...).
	 */
	@Singular
	List<Target> targets;
	/**
	 * Inventory items to use on each other for processing, e.g. knife then logs.
	 */
	@Singular("useItem")
	List<Integer> useItems;

	/**
	 * The instruction shown while training, e.g. "Chop the willow trees".
	 */
	@NonNull
	String instruction;
	String notes;
	@Singular
	List<Quest> quests;
	/**
	 * Free text for requirements that can't be checked automatically.
	 */
	String requirementText;
	String wikiPage;

	/**
	 * Whether the player is at the training spot (or one of its {@link #alsoAt} areas).
	 */
	public boolean isAt(WorldPoint wp, boolean inInstance)
	{
		if ((instanced && inInstance) || location.contains(wp))
		{
			return true;
		}
		for (Location l : alsoAt)
		{
			if (l.contains(wp))
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasTag(MethodTag tag)
	{
		return tags.contains(tag);
	}

	public Set<MethodTag> tagSet()
	{
		return tags.isEmpty() ? EnumSet.noneOf(MethodTag.class) : EnumSet.copyOf(tags);
	}
}
