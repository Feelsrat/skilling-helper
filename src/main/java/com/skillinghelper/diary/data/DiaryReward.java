package com.skillinghelper.diary.data;

import java.util.List;
import lombok.Value;

/**
 * What finishing a tier gives: the reward item and everything it and the tier unlock, as the wiki lists them.
 */
@Value
public class DiaryReward
{
	String item;
	List<String> perks;
}
