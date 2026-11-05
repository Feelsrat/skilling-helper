package com.skillinghelper.travel;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Which travel options the player has unlocked (quests, levels, diaries), apart from owning the item. Read on the
 * client thread by {@link TravelAdvisor} and kept for a while, since quest states are scripts.
 */
public final class TravelUnlocks
{
	/**
	 * What the unlock checks read from the game.
	 */
	public interface Source
	{
		boolean questDone(String name);

		int level(Skill skill);

		int varbit(int id);

		int varp(int id);
	}

	private final BitSet usable;
	private final boolean fairyRings;
	private final boolean staffNeeded;

	private TravelUnlocks(BitSet usable, boolean fairyRings, boolean staffNeeded)
	{
		this.usable = usable;
		this.fairyRings = fairyRings;
		this.staffNeeded = staffNeeded;
	}

	/**
	 * @param fairyRings  whether fairy rings are open (far enough into Fairytale II)
	 * @param staffNeeded whether using one still needs a staff (no Lumbridge elite diary)
	 */
	public static TravelUnlocks read(List<TravelOption> options, Source source, boolean fairyRings, boolean staffNeeded)
	{
		BitSet usable = new BitSet(options.size());
		for (int i = 0; i < options.size(); i++)
		{
			if (meets(options.get(i), source))
			{
				usable.set(i);
			}
		}
		return new TravelUnlocks(usable, fairyRings, staffNeeded);
	}

	/**
	 * Everything unlocked and fairy rings needing a staff, for tests.
	 */
	public static TravelUnlocks everything(List<TravelOption> options)
	{
		BitSet usable = new BitSet(options.size());
		usable.set(0, options.size());
		return new TravelUnlocks(usable, true, true);
	}

	private static boolean meets(TravelOption o, Source s)
	{
		for (String q : o.getQuests())
		{
			if (!s.questDone(q))
			{
				return false;
			}
		}
		for (Map.Entry<Skill, Integer> e : o.getSkills().entrySet())
		{
			if (s.level(e.getKey()) < e.getValue())
			{
				return false;
			}
		}
		for (TravelOption.Check c : o.getVarbits())
		{
			if (!c.test(s.varbit(c.getId())))
			{
				return false;
			}
		}
		for (TravelOption.Check c : o.getVarps())
		{
			if (!c.test(s.varp(c.getId())))
			{
				return false;
			}
		}
		return true;
	}

	public boolean usable(int index)
	{
		return usable.get(index);
	}

	public boolean fairyRings()
	{
		return fairyRings;
	}

	public boolean staffNeeded()
	{
		return staffNeeded;
	}
}
