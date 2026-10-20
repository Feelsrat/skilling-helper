package com.skillinghelper.engine;

/**
 * Grand Exchange price per item. Backed by RuneLite's ItemManager in game and by fixed values in tests.
 */
public interface PriceLookup
{
	int price(int itemId);
}
