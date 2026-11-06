package com.skillinghelper.unlocks.data;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * Points or tokens to earn before an unlock can be bought, e.g. 260 marks of grace or 750 Slayer points. Counted
 * from an item the player owns ({@link #itemId}) or a varbit ({@link #varbit}).
 */
@Value
@Builder
public class Grind
{
	/**
	 * What's earned, plural, e.g. "marks of grace".
	 */
	String label;
	/**
	 * The item that's earned (counted in the bank and inventory), or 0.
	 */
	int itemId;
	/**
	 * The varbit holding the points, or 0.
	 */
	int varbit;
	/**
	 * How many are needed in all. For unlocks with several pieces see {@link Unlock#getPieceCosts()}.
	 */
	int amount;
	/**
	 * The skill trained to earn them, for the Skilling Helper hand-off.
	 */
	Skill skill;
	/**
	 * Level needed in {@link #skill} to start earning, or 0.
	 */
	int minLevel;
	/**
	 * A Skilling Helper method that earns them (opened as is), or null to open the skill's guide.
	 */
	String methodId;
	/**
	 * How to earn them, for someone who has never done it.
	 */
	@Singular
	List<String> lines;
}
