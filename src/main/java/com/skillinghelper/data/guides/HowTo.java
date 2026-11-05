package com.skillinghelper.data.guides;

import java.util.List;

/**
 * Beginner steps shared by many methods, written for someone who has never done the activity before.
 */
final class HowTo
{
	/**
	 * Making something at a bank: take out the parts, use one on the other, pick the product, bank and repeat.
	 *
	 * @param take   what to take out, e.g. "your knife and 27 logs"
	 * @param use    what to click, in order, e.g. "the knife, then a log"
	 * @param result what comes out, e.g. "longbows (u)"
	 */
	static List<String> bankStanding(String take, String use, String result)
	{
		return List.of(
			"Stand next to any banker or bank booth (the Grand Exchange has lots in the middle).",
			"Open the bank and take out " + take + ". The guide highlights them and sets the right Withdraw "
				+ "amount.",
			"Close the bank and click " + use + ". A menu pops up: press Space (or click the picture of " + result
				+ ") to make as many as you can.",
			"Wait until your character stops. A level-up message interrupts you; just do it again.",
			"Open the bank, deposit the " + result + ", take out more and repeat.");
	}

	/**
	 * Making something at an object (furnace, anvil, range...) near a bank.
	 *
	 * @param where  where the object is, e.g. "The furnace is in the building just east of Edgeville bank"
	 * @param take   what to take out of the bank
	 * @param use    what to click, in order
	 * @param choose what to pick in the menu that opens
	 * @param result what comes out
	 */
	static List<String> atObject(String where, String take, String use, String choose, String result)
	{
		return List.of(
			where + ": follow the arrow.",
			"At the bank, take out " + take + ". The guide highlights them and sets the right Withdraw amount.",
			"Click " + use + ". In the menu that opens, " + choose + ".",
			"Wait until your character stops. A level-up message interrupts you; just do it again.",
			"Walk back to the bank, deposit the " + result + ", take out more and repeat.");
	}

	/**
	 * Making something that stacks, which never needs a bank: arrows, darts, bolts.
	 */
	static List<String> stackable(String first, String second, String result)
	{
		return List.of(
			"Everything stacks in one inventory slot, so you can do this anywhere, even while walking.",
			"Click the " + first + ", then the " + second + ". A menu pops up: press Space (or click the picture of "
				+ result + ") to make a batch.",
			"Each batch makes up to 10 or 15 at once. Keep clicking the two stacks together (pressing Space each "
				+ "time) until one of them runs out.");
	}

	private HowTo()
	{
	}
}
