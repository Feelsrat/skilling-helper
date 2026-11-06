package com.skillinghelper.minigame.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.diary.data.DiaryStage;
import com.skillinghelper.diary.data.Zone;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * One minigame: what it is, what it needs, which rewards are worth chasing, and either a step-by-step guide here
 * ({@link #stages}) or the full guide another plugin in this pack has ({@link #skillingMethod} or
 * {@link #pvmContent}).
 */
@Value
@Builder
public class Minigame
{
	String id;
	String name;
	MinigameCategory category;
	/**
	 * What you do there, in a sentence or two, for someone who has never seen it.
	 */
	String summary;
	/**
	 * The skill it trains, for the "Lvl X → 99" line, or null.
	 */
	Skill trains;
	String wikiPage;
	/**
	 * Where it is (the lobby or start), for the route.
	 */
	Location location;
	/**
	 * The rest of the minigame (waiting rooms, battlefield) where the guide never says "walk to" the start, or null.
	 * Instanced areas count too, see MinigameGuide.
	 */
	Zone area;
	/**
	 * How to get there, beginner wording.
	 */
	String getThere;
	/**
	 * Team or solo, e.g. "Team game (needs other players on the world)", or null for solo.
	 */
	String team;
	/**
	 * Dying there loses items (not a safe minigame).
	 */
	boolean dangerous;
	boolean wilderness;

	/**
	 * Skill levels needed; boosts count unless listed in {@link #unboostable}.
	 */
	@Singular
	Map<Skill, Integer> skills;
	@Singular("unboostable")
	List<Skill> unboostable;
	@Singular
	List<Quest> quests;
	@Singular("questStarted")
	List<Quest> questsStarted;
	int combat;
	int totalLevel;
	/**
	 * Requirements the plugin can't check, shown with a "?".
	 */
	@Singular
	List<String> notes;

	@Singular
	List<DiaryItem> items;
	/**
	 * Step-by-step guide, in order. Empty when the full guide lives in another plugin.
	 */
	@Singular
	List<DiaryStage> stages;

	@Singular
	List<Reward> rewards;
	@Singular("points")
	List<Points> points;

	/**
	 * Skilling Helper method with the full click-by-click guide, or null.
	 */
	String skillingMethod;
	/**
	 * PvM Progression entry with the full guide, or null.
	 */
	String pvmContent;

	public boolean guidedHere()
	{
		return !stages.isEmpty();
	}

	/**
	 * The lowest level of {@link #trains} it can be done at.
	 */
	public int minLevel()
	{
		return trains == null ? 1 : skills.getOrDefault(trains, 1);
	}
}
