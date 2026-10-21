package com.skillinghelper.engine;

import com.skillinghelper.data.Mooring;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/**
 * Snapshot of everything the guide needs to know about the player, taken on the client thread each tick.
 * Kept free of client references so the planner and step logic can be unit tested.
 */
@Value
@Builder
public class PlayerState
{
	@Builder.Default
	Map<Skill, Integer> levels = Collections.emptyMap();
	@Builder.Default
	Map<Skill, Integer> xp = Collections.emptyMap();
	@Builder.Default
	ItemCounts inventory = ItemCounts.EMPTY;
	@Builder.Default
	ItemCounts equipment = ItemCounts.EMPTY;
	/**
	 * Last known bank contents, or null if the bank hasn't been opened yet.
	 */
	ItemCounts bank;
	int freeInventorySlots;
	WorldPoint location;
	boolean membersWorld;
	/**
	 * In an instanced area such as a player-owned house.
	 */
	boolean inInstance;
	/**
	 * Standing on a boat (sailing).
	 */
	boolean onBoat;
	/**
	 * Where the player last docked their boat, or null if not seen yet.
	 */
	Mooring boatMooring;
	/**
	 * Minutes until each finished stop of a run (e.g. a seeded bird house) needs another visit. Stops that aren't
	 * listed need a visit now.
	 */
	@Builder.Default
	Map<String, Integer> stopWaits = Collections.emptyMap();
	@Builder.Default
	Set<Quest> completedQuests = Collections.emptySet();

	public int level(Skill skill)
	{
		return levels.getOrDefault(skill, 1);
	}

	public int xp(Skill skill)
	{
		return xp.getOrDefault(skill, 0);
	}

	public int combatLevel()
	{
		return Experience.getCombatLevel(level(Skill.ATTACK), level(Skill.STRENGTH), level(Skill.DEFENCE),
			level(Skill.HITPOINTS), level(Skill.MAGIC), level(Skill.RANGED), level(Skill.PRAYER));
	}

	public boolean bankKnown()
	{
		return bank != null;
	}

	public int bankCount(int... ids)
	{
		return bank == null ? 0 : bank.count(ids);
	}

	/**
	 * Items carried or worn right now.
	 */
	public int carried(int... ids)
	{
		return inventory.count(ids) + equipment.count(ids);
	}

	public int owned(int... ids)
	{
		return carried(ids) + bankCount(ids);
	}

	/**
	 * Coins and platinum tokens, carried and banked.
	 */
	public long gp()
	{
		return owned(ItemID.COINS) + 1000L * owned(ItemID.PLATINUM);
	}

	public boolean inventoryFull()
	{
		return freeInventorySlots <= 0;
	}
}
