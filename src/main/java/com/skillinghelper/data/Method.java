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

	public boolean hasTag(MethodTag tag)
	{
		return tags.contains(tag);
	}

	public Set<MethodTag> tagSet()
	{
		return tags.isEmpty() ? EnumSet.noneOf(MethodTag.class) : EnumSet.copyOf(tags);
	}
}
