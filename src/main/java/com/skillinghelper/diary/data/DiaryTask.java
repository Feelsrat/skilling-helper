package com.skillinghelper.diary.data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * One Achievement Diary task: what the game asks for, how the game records it as done, what it needs, and the
 * step-by-step guide to do it.
 */
@Value
@Builder
public class DiaryTask
{
	DiaryRegion region;
	DiaryTier tier;
	/**
	 * The task's number in its tier, as on the wiki and in the in-game diary.
	 */
	int number;
	/**
	 * The task as the diary words it.
	 */
	String text;
	/**
	 * Short name for lists, e.g. "Draynor rooftops".
	 */
	String name;

	/**
	 * Done when bit {@link #bit} of this varp is set. 0 when the task uses {@link #varbit} instead.
	 */
	int varp;
	int bit;
	/**
	 * Done when this varbit is at least {@link #varbitDone}, for diaries that store each task in its own varbit.
	 */
	int varbit;
	@Builder.Default
	int varbitDone = 1;

	/**
	 * Skill levels needed. Boosts count unless the skill is in {@link #unboostable}.
	 */
	@Singular
	Map<Skill, Integer> skills;
	@Singular("unboostable")
	Set<Skill> unboostable;
	@Singular
	List<Quest> quests;
	/**
	 * Quests that only need to be started.
	 */
	@Singular("questStarted")
	List<Quest> questsStarted;
	/**
	 * Combat level needed, or 0.
	 */
	int combat;
	/**
	 * Other requirements the plugin can't check, shown with a "?".
	 */
	@Singular
	List<String> notes;
	@Singular
	List<DiaryItem> items;
	/**
	 * Done in the Wilderness: only guided to when the player opted in.
	 */
	boolean wilderness;
	@Singular
	List<DiaryStage> stages;

	public String key()
	{
		return region.name() + "." + tier.name() + "." + number;
	}

	public String label()
	{
		return name != null ? name : text;
	}
}
