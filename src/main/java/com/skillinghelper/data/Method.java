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
@Builder(toBuilder = true)
public class Method
{
	public static final int MAX_LEVEL = 99;

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
	 * The level where the method stops being worth it because a better one has opened up. 99 (or 0, the same) when
	 * it's worth doing all the way. Set for every method in {@code LevelRanges}.
	 */
	int maxLevel;
	/**
	 * Combat level needed, e.g. for Slayer masters. 0 when there's no requirement.
	 */
	int combatLevel;
	/**
	 * Combat level where the method is outgrown, e.g. when a better Slayer master gives tasks. 0 when combat level
	 * never outgrows it.
	 */
	int maxCombatLevel;
	/**
	 * Approximate xp per hour at {@link #minLevel} for efficient play, as the wiki gives it. The planner scales it down
	 * by the player's {@link RatePace}.
	 */
	int xpPerHour;
	/**
	 * Xp for one action, used to work out how many actions (and items) the goal needs.
	 */
	double xpPerAction;
	/**
	 * Better xp per action unlocked at higher levels, as {level, xp} pairs, e.g. bird houses made from better logs.
	 * Xp per hour scales with it.
	 */
	@Singular("xpAt")
	List<int[]> xpAt;
	/**
	 * Xp per hour at higher levels, as {level, xp per hour} pairs in level order, for methods that speed up as the
	 * player levels (more successful catches, faster chopping). Rates in between are interpolated from
	 * {@link #xpPerHour} at {@link #minLevel}. Empty when the rate only changes with {@link #xpAt}.
	 */
	@Singular("rate")
	List<int[]> rates;
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
	 * For runs (bird houses...): the spots to visit in order. The guide sends the player to the next one that needs a
	 * visit and waits once they're all done.
	 */
	@Singular
	List<Stop> stops;
	/**
	 * For runs: minutes between runs while the crops or bird houses grow. {@link #xpPerHour} is then xp per hour of
	 * play, not of waiting.
	 */
	int cycleMinutes;
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
	 * Optional things worth bringing (food, stamina potions, compost...). Suggested with their GE price, never required.
	 */
	@Singular
	List<ItemReq> supplies;
	/**
	 * Combat style whose weapon and armour the guide should recommend, or null for non-combat methods.
	 */
	CombatStyle gearStyle;
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

	/**
	 * Xp per action at this level, counting {@link #xpAt} upgrades.
	 */
	public double xpPerAction(int level)
	{
		double best = xpPerAction;
		for (int[] tier : xpAt)
		{
			if (level >= tier[0])
			{
				best = Math.max(best, tier[1]);
			}
		}
		return best;
	}

	/**
	 * Estimated xp per hour at this level.
	 */
	public double xpPerHour(int level)
	{
		if (!rates.isEmpty())
		{
			return interpolatedRate(level);
		}
		return xpPerAction <= 0 ? xpPerHour : xpPerHour * xpPerAction(level) / xpPerAction;
	}

	private double interpolatedRate(int level)
	{
		int fromLevel = minLevel;
		double fromRate = xpPerHour;
		for (int[] point : rates)
		{
			if (level < point[0])
			{
				double progress = Math.max(0, (double) (level - fromLevel) / (point[0] - fromLevel));
				return fromRate + progress * (point[1] - fromRate);
			}
			fromLevel = point[0];
			fromRate = point[1];
		}
		return fromRate;
	}

	/**
	 * Whether training happens in the Wilderness: the spot, any stop or any other place it counts.
	 */
	public boolean isWilderness()
	{
		if (Wilderness.contains(location))
		{
			return true;
		}
		for (Location l : alsoAt)
		{
			if (Wilderness.contains(l))
			{
				return true;
			}
		}
		for (Stop s : stops)
		{
			if (Wilderness.contains(s.getLocation()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * {@link #maxLevel}, with 0 read as 99.
	 */
	public int topLevel()
	{
		return maxLevel <= 0 ? MAX_LEVEL : Math.min(maxLevel, MAX_LEVEL);
	}

	/**
	 * Whether the player has outgrown the method: they reached the level where something better takes over.
	 */
	public boolean isOutleveled(int level, int combat)
	{
		return (topLevel() < MAX_LEVEL && level >= topLevel()) || (maxCombatLevel > 0 && combat >= maxCombatLevel);
	}

	/**
	 * "Lvl 30 → 60", plus the combat range when combat level matters: "Lvl 1 → 99 · Combat 40 → 70".
	 */
	public String levelRange()
	{
		String s = "Lvl " + Math.max(1, minLevel) + " → " + topLevel();
		if (combatLevel > 0 || maxCombatLevel > 0)
		{
			s += " · Combat " + Math.max(3, combatLevel) + " → " + (maxCombatLevel > 0 ? maxCombatLevel : 126);
		}
		return s;
	}

	public boolean hasTag(MethodTag tag)
	{
		return tags.contains(tag);
	}

	public Set<MethodTag> tagSet()
	{
		return tags.isEmpty() ? EnumSet.noneOf(MethodTag.class) : EnumSet.copyOf(tags);
	}

	public static class MethodBuilder
	{
		/**
		 * Adds a point to {@link Method#rates}: the xp per hour at this level.
		 */
		public MethodBuilder rateAt(int level, int xpPerHour)
		{
			return rate(new int[]{level, xpPerHour});
		}
	}
}
