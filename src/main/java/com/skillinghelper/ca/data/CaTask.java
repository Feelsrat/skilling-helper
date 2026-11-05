package com.skillinghelper.ca.data;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarPlayerID;

/**
 * One Combat Achievement task, as the game and the OSRS Wiki list it.
 */
@Value
public class CaTask
{
	/**
	 * The varps holding which tasks are done, 32 tasks each: task id N is bit N % 32 of COMPLETED[N / 32].
	 */
	public static final int[] COMPLETED = {
		VarPlayerID.CA_TASK_COMPLETED_0, VarPlayerID.CA_TASK_COMPLETED_1, VarPlayerID.CA_TASK_COMPLETED_2,
		VarPlayerID.CA_TASK_COMPLETED_3, VarPlayerID.CA_TASK_COMPLETED_4, VarPlayerID.CA_TASK_COMPLETED_5,
		VarPlayerID.CA_TASK_COMPLETED_6, VarPlayerID.CA_TASK_COMPLETED_7, VarPlayerID.CA_TASK_COMPLETED_8,
		VarPlayerID.CA_TASK_COMPLETED_9, VarPlayerID.CA_TASK_COMPLETED_10, VarPlayerID.CA_TASK_COMPLETED_11,
		VarPlayerID.CA_TASK_COMPLETED_12, VarPlayerID.CA_TASK_COMPLETED_13, VarPlayerID.CA_TASK_COMPLETED_14,
		VarPlayerID.CA_TASK_COMPLETED_15, VarPlayerID.CA_TASK_COMPLETED_16, VarPlayerID.CA_TASK_COMPLETED_17,
		VarPlayerID.CA_TASK_COMPLETED_18, VarPlayerID.CA_TASK_COMPLETED_19, VarPlayerID.CA_TASK_COMPLETED_20,
	};

	private static final Pattern TIMES = Pattern.compile("(\\d[\\d,]*) (?:times|Moons|kills)");
	private static final Pattern ONCE = Pattern.compile("\\b(?:once|a single|an? [A-Z])");

	/**
	 * The game's task id.
	 */
	int id;
	CaTier tier;
	CaType type;
	/**
	 * The boss or monster, as the wiki names it ("None" for a few general tasks).
	 */
	String monster;
	String name;
	/**
	 * What to do, as the game words it.
	 */
	String text;
	boolean members;
	/**
	 * Levels the wiki says the task needs (e.g. 60 Slayer for aberrant spectres).
	 */
	Map<Skill, Integer> levels;
	List<Quest> quests;
	/**
	 * The wiki's strategy notes, one sentence each.
	 */
	List<String> strategy;

	public String key()
	{
		return "CA." + id;
	}

	public int varp()
	{
		return id / 32 < COMPLETED.length ? COMPLETED[id / 32] : 0;
	}

	public int bit()
	{
		return id % 32;
	}

	/**
	 * How many kills a kill-count or stamina task asks for, 1 when it's a single kill.
	 */
	public int count()
	{
		Matcher m = TIMES.matcher(text);
		if (m.find())
		{
			return Integer.parseInt(m.group(1).replace(",", ""));
		}
		return 1;
	}

	/**
	 * Has to be done on a free-to-play world.
	 */
	public boolean freeWorld()
	{
		return text.toLowerCase().contains("free to play world");
	}

	public String wikiPage()
	{
		return name.replace(' ', '_');
	}
}
