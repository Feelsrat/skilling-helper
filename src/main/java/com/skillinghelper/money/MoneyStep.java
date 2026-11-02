package com.skillinghelper.money;

import com.skillinghelper.data.Location;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * What to do right now for the running money making guide. Rebuilt every tick and read by the overlays and panel.
 */
@Value
@Builder
public class MoneyStep
{
	public enum Kind
	{
		/**
		 * The player can't do the method yet.
		 */
		LOCKED,
		/**
		 * Fetch the inputs and supplies from a bank or the Grand Exchange.
		 */
		SUPPLIES,
		/**
		 * Walk to where it's done.
		 */
		TRAVEL,
		/**
		 * At the spot: do the method.
		 */
		WORK
	}

	Kind kind;
	String title;
	@Singular
	List<String> lines;
	/**
	 * Where to walk to, or null when the step doesn't move the player.
	 */
	Location destination;
	/**
	 * NPC names to outline (a shopkeeper, bankers).
	 */
	@Builder.Default
	Set<String> npcs = Collections.emptySet();
	/**
	 * Object names to outline (bank booths and chests).
	 */
	@Builder.Default
	Set<String> objects = Collections.emptySet();
	/**
	 * Items to outline in the bank, to take out.
	 */
	@Builder.Default
	Set<Integer> withdraw = Collections.emptySet();
}
