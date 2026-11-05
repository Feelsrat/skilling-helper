package com.skillinghelper.banked.profit;

import com.skillinghelper.banked.data.BankedActivity;
import lombok.NonNull;
import lombok.Value;

/**
 * A banked activity that makes something worth selling: what it makes and how many per action.
 */
@Value
public class ProfitRecipe
{
	@NonNull
	BankedActivity activity;
	int productId;
	String productName;
	/**
	 * Products per action. Below 1 when it can fail (iron ore smelts half the time without a ring of forging).
	 */
	double perAction;
	/**
	 * A caveat shown with the pick, or null.
	 */
	String note;

	public String getId()
	{
		return activity.getId();
	}
}
