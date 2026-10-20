package com.skillinghelper.engine;

/**
 * Why an inventory/bank item is highlighted. Each kind gets its own colour.
 */
public enum ItemHighlight
{
	/**
	 * Click or use this item to train.
	 */
	USE,
	/**
	 * Wear/wield this item.
	 */
	EQUIP,
	/**
	 * Drop this item.
	 */
	DROP,
	/**
	 * Withdraw this item from the bank.
	 */
	WITHDRAW,
	/**
	 * Deposit this item into the bank.
	 */
	DEPOSIT
}
