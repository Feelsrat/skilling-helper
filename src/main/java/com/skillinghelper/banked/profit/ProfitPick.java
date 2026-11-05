package com.skillinghelper.banked.profit;

import java.util.List;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * One thing the player could make from their bank to sell, with what it makes at today's prices.
 */
@Value
public class ProfitPick
{
	ProfitRecipe recipe;
	/**
	 * Actions the banked items allow (secondaries that run short are bought).
	 */
	int actions;
	/**
	 * Products made, to sell.
	 */
	long products;
	/**
	 * Gold one product sells for after the GE tax.
	 */
	int productEach;
	/**
	 * What the products sell for after tax.
	 */
	long productValue;
	/**
	 * What the banked items would sell for as they are (after tax), plus the cost of anything bought.
	 */
	long inputValue;
	/**
	 * Gold to spend on the GE for missing secondaries.
	 */
	long buyCost;
	/**
	 * Extra gold made by processing instead of selling the items as they are.
	 */
	long profit;
	double hours;
	/**
	 * What goes in, one line each: "1,000 yew logs (yours)", "Buy 600 bow strings (~90K)".
	 */
	List<String> inputs;
	/**
	 * Why it can't be done yet ("Needs Fletching 70, you're 66"), or null when it can.
	 */
	String locked;
	/**
	 * Levels still to gain, 0 when the level is already there.
	 */
	int levelsAway;
	/**
	 * Selling worries (barely trades, not enough gold for the secondaries), may be empty.
	 */
	List<String> warnings;

	public boolean isDoable()
	{
		return locked == null;
	}

	public String getId()
	{
		return recipe.getId();
	}

	public String getName()
	{
		return recipe.getActivity().getName();
	}

	public Skill getSkill()
	{
		return recipe.getActivity().getSkill();
	}

	public long getProfitPerHour()
	{
		return hours <= 0 ? profit : (long) (profit / hours);
	}
}
