package com.skillinghelper.ca.data;

import java.util.List;
import lombok.Value;

/**
 * What a tier's reward gives, from the OSRS Wiki. Lines starting "- " belong to the line above.
 */
@Value
public class CaReward
{
	CaTier tier;
	/**
	 * Points the wiki listed when the data was built.
	 */
	int points;
	List<String> perks;
}
