package com.skillinghelper.pvm.data;

import java.util.List;
import java.util.function.ToIntFunction;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Skill;
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
	/**
	 * Combat stat level where it's outgrown: better bosses give more by then. 99 (or 0, the same) when it stays worth
	 * doing.
	 */
	int maxLevel;
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

	/**
	 * {@link #maxLevel}, with 0 read as 99.
	 */
	public int topLevel()
	{
		return maxLevel <= 0 ? 99 : Math.min(maxLevel, 99);
	}

	/**
	 * "Lvl 40 → 70": the recommended level to the level where it's outgrown.
	 */
	public String levelRange()
	{
		return "Lvl " + getLevel() + " → " + topLevel();
	}

	/**
	 * Whether every combat stat it recommends (Prayer aside) is at the top of the range or higher.
	 */
	public boolean isOutgrownBy(ToIntFunction<Skill> level)
	{
		if (topLevel() >= 99)
		{
			return false;
		}
		boolean any = false;
		for (StatReq s : stats)
		{
			if (s.getSkill() == Skill.PRAYER)
			{
				continue;
			}
			any = true;
			if (level.applyAsInt(s.getSkill()) < topLevel())
			{
				return false;
			}
		}
		return any;
	}

	int levelTotal()
	{
		return stats.stream().mapToInt(StatReq::getLevel).sum();
	}
}
