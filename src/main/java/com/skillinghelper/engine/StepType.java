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
	 * Nothing to do on this run until something grows or fills up.
	 */
	WAIT,
	DONE
}
