package com.skillinghelper.engine;

import lombok.Value;

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
		 * Informational only.
		 */
		INFO
	}

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
}
