package com.skillinghelper.engine;

import lombok.Value;
import lombok.With;

/**
 * One line of the "what you need" checklist in the panel.
 */
@Value
public class Requirement
{
	public enum Status
	{
		/**
		 * Carried or worn.
		 */
		READY,
		/**
		 * Owned but in the bank.
		 */
		IN_BANK,
		/**
		 * Not owned (or bank unknown and not carried).
		 */
		MISSING,
		/**
		 * Recommended but not required.
		 */
		OPTIONAL,
		/**
		 * A GE-buyable item that helps but isn't needed.
		 */
		SUGGESTED,
		/**
		 * Gear the player doesn't own yet that's earned by playing, with the time it would save.
		 */
		UNLOCK,
		/**
		 * Informational only.
		 */
		INFO
	}

	@With
	String text;
	Status status;
	/**
	 * Item ids this line is about, for bank filtering. Empty for non-item lines.
	 */
	int[] itemIds;
	/**
	 * How many to withdraw for one trip.
	 */
	int quantity;
	/**
	 * GP needed to buy what's missing, 0 if nothing to buy.
	 */
	long buyCost;
	/**
	 * How many to buy, 0 if nothing to buy.
	 */
	long buyCount;

	public Requirement(String text, Status status, int[] itemIds, int quantity, long buyCost)
	{
		this(text, status, itemIds, quantity, buyCost, 0);
	}

	public Requirement(String text, Status status, int[] itemIds, int quantity, long buyCost, long buyCount)
	{
		this.text = text;
		this.status = status;
		this.itemIds = itemIds;
		this.quantity = quantity;
		this.buyCost = buyCost;
		this.buyCount = buyCount;
	}
}
