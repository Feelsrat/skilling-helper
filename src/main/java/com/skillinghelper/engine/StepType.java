package com.skillinghelper.engine;

public enum StepType
{
	GET_ITEMS,
	BANK,
	EQUIP,
	WALK,
	TRAIN,
	DROP,
	/**
	 * Make an input from its parts, e.g. fill baskets with apples.
	 */
	MAKE,
	/**
	 * Nothing to do on this run until something grows or fills up.
	 */
	WAIT,
	DONE
}
