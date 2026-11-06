package com.skillinghelper.ca.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.diary.data.DiaryItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * A boss, raid or monster that Combat Achievement tasks are about: how strong you should be, how to get there and
 * in, and what it needs. Levels and entrances are judgement calls from the OSRS Wiki's guides.
 */
@Value
@Builder
public class CaMonster
{
	/**
	 * The name the wiki's task list uses, e.g. "Giant Mole".
	 */
	String name;
	String wikiPage;
	/**
	 * Fighting strength that makes it comfortable, on the same 1-99 scale as the player's: roughly the combat stats
	 * the wiki's guides suggest, with fitting gear.
	 */
	int level;
	/**
	 * How many players it's usually done with; 1 for solo content.
	 */
	@Builder.Default
	int team = 1;
	boolean wilderness;
	@Builder.Default
	boolean members = true;
	/**
	 * Where to walk to first (the entrance), or null when the way there can't be pathed (the steps say how).
	 */
	Location at;
	/**
	 * How to get from {@link #at} to the fight, one line each, for someone who has never been.
	 */
	@Singular("enter")
	List<String> enter;
	/**
	 * How to start the fight and what to watch for, beyond the task itself.
	 */
	@Singular("fight")
	List<String> fight;
	/**
	 * NPC names to outline: the boss and anyone to talk to.
	 */
	@Singular
	Set<String> npcs;
	/**
	 * Object names to outline: entrances, ladders, chests.
	 */
	@Singular
	Set<String> objects;
	/**
	 * Hard requirements, e.g. Slayer 85.
	 */
	@Singular
	Map<Skill, Integer> skills;
	@Singular
	List<Quest> quests;
	@Singular("questStarted")
	List<Quest> questsStarted;
	/**
	 * Requirements the plugin can't check, e.g. "Only on a Slayer task".
	 */
	@Singular
	List<String> notes;
	/**
	 * What to bring that the fight can't be done without (keys, shields, light sources).
	 */
	@Singular
	List<DiaryItem> items;
	/**
	 * Not one boss to click Attack on (raids, minigames, skilling bosses, groups of monsters).
	 */
	boolean noAttack;
	/**
	 * What to bring, when it isn't "your best gear, food and potions", or null.
	 */
	String bring;
	/**
	 * The PvM Progression guide with a full loadout for it, or null.
	 */
	String pvmId;
	/**
	 * Names the game uses in "Your ... count is: N" messages, lower case.
	 */
	@Singular
	List<String> kcNames;

	public String wiki()
	{
		return wikiPage != null ? wikiPage : name;
	}
}
