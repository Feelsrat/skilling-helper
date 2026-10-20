package com.skillinghelper.data;

/**
 * Shape of the training loop, which decides how guide steps are generated.
 */
public enum MethodType
{
	/**
	 * Go somewhere, click a resource until the inventory is full, then bank or drop.
	 */
	GATHER,
	/**
	 * Withdraw inputs, turn them into products (optionally at a station such as a range), repeat.
	 */
	PROCESS,
	/**
	 * Minigames and other activities that are explained with text and highlights only.
	 */
	ACTIVITY
}
