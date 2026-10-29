package com.skillinghelper.pvm.data;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * One boss, raid or challenge on the progression ladder.
 */
@Value
@Builder
public class PvmContent
{
	String id;
	String name;
	Tier tier;
	/**
	 * Why it's worth doing at this point: what it teaches or drops.
	 */
	String why;
	String wikiPage;
	@Singular
	List<QuestReq> quests;
	@Singular
	List<StatReq> stats;
	@Singular
	List<ItemNeed> items;
	/**
	 * How to get there, in words.
	 */
	String travel;
	/**
	 * Where to path to with Shortest Path, or null when only {@link #travel} is given.
	 */
	WorldPoint location;
	boolean wilderness;
	/**
	 * The names the game uses in its "Your ... count is: N" messages for this content.
	 */
	@Singular
	List<String> kcNames;

	/**
	 * The highest recommended level, used to sort content from easiest to hardest.
	 */
	public int getLevel()
	{
		return stats.stream().mapToInt(StatReq::getLevel).max().orElse(1);
	}

	int levelTotal()
	{
		return stats.stream().mapToInt(StatReq::getLevel).sum();
	}
}
