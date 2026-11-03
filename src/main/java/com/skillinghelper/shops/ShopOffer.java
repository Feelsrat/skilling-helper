package com.skillinghelper.shops;

import lombok.Value;

/**
 * One item an NPC shop sells for coins.
 */
@Value
public class ShopOffer
{
	Shop shop;
	int itemId;
	String itemName;
	/**
	 * Price of one when the shop has its full stock.
	 */
	int price;
	/**
	 * How many the shop holds when fully stocked, or -1 when it never runs out.
	 */
	int stock;
	/**
	 * How much dearer each one gets as the stock runs down, in thousandths of the item's value per item bought.
	 */
	int delta;
	/**
	 * The shop's price at full stock, in thousandths of the item's value.
	 */
	int multiplier;

	/**
	 * Price of the next one after {@code bought} have been bought from a fully stocked shop. Shops charge more the
	 * lower their stock gets, so buying a lot in one world gets expensive; hopping worlds starts again at full stock.
	 */
	public int unitPrice(int bought)
	{
		if (delta <= 0 || multiplier <= 0)
		{
			return price;
		}
		double value = price * 1000.0 / multiplier;
		return Math.max(price, (int) Math.floor(value * (multiplier + (double) delta * bought) / 1000.0));
	}

	/**
	 * Whether the shop has another one after {@code bought} have been bought in one world.
	 */
	public boolean inStock(int bought)
	{
		return stock < 0 || bought < stock;
	}
}
