package com.skillinghelper.money;

import com.skillinghelper.data.Location;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
		 * Buy what's missing at the Grand Exchange.
		 */
		BUY,
		/**
		 * Walk to a bank and open it.
		 */
		OPEN_BANK,
		/**
		 * Bank is open: take out the supplies.
		 */
		WITHDRAW,
		/**
		 * Inventory is full of what the method makes: put it in the bank.
		 */
		DEPOSIT,
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
	 * NPC names to outline (a shopkeeper, bankers, Grand Exchange clerks).
	 */
	@Builder.Default
	Set<String> npcs = Collections.emptySet();
	/**
	 * Object names to outline (bank booths and chests).
	 */
	@Builder.Default
	Set<String> objects = Collections.emptySet();
	/**
	 * Items to outline in the bank, with how many to take out.
	 */
	@Builder.Default
	Map<Integer, Integer> withdraw = Collections.emptyMap();
	/**
	 * Items to outline in the inventory, to use.
	 */
	@Builder.Default
	Set<Integer> use = Collections.emptySet();

	/**
	 * Whether the step is about getting the supplies, so the panel offers "I have everything".
	 */
	public boolean isSupplyStep()
	{
		return kind == Kind.BUY || kind == Kind.OPEN_BANK || kind == Kind.WITHDRAW;
	}
}
