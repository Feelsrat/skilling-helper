package com.skillinghelper.travel;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.PlayerState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;

/**
 * Works out which travel item a guide should suggest, for any plugin in this jar. Reads the player's unlocks (quests,
 * levels, diaries) now and then rather than every tick, since quest states are scripts. Client thread only.
 */
public class TravelAdvisor
{
	/**
	 * How long unlocks are trusted before they're read again.
	 */
	private static final long REFRESH_MILLIS = 60_000;
	private static final Map<String, Quest> QUESTS = new HashMap<>();

	static
	{
		for (Quest q : Quest.values())
		{
			QUESTS.put(q.getName(), q);
		}
	}

	private final Client client;
	private final ConfigManager configManager;
	private TravelUnlocks unlocks;
	private long readAt;

	@Inject
	public TravelAdvisor(Client client, ConfigManager configManager)
	{
		this.client = client;
		this.configManager = configManager;
	}

	/**
	 * Reads the unlocks again on the next pick, e.g. after logging in on another account.
	 */
	public void invalidate()
	{
		unlocks = null;
	}

	public boolean enabled()
	{
		return TravelSettings.enabled(configManager);
	}

	/**
	 * What to bring for heading to {@code destinations}: a travel item and/or coins for a paid transport. Empty when
	 * walking is fine.
	 *
	 * @param wildernessDestination whether the destination itself is in the Wilderness and the player opted in, so a
	 *                              teleport there is fine
	 */
	public List<TravelPick> plan(IntUnaryOperator carried, IntUnaryOperator inBank, WorldPoint from,
		List<WorldPoint> destinations, String destinationName, boolean wildernessDestination)
	{
		if (!enabled() || client.getGameState() != GameState.LOGGED_IN || destinations.isEmpty())
		{
			return Collections.emptyList();
		}
		long now = System.currentTimeMillis();
		if (unlocks == null || now - readAt > REFRESH_MILLIS)
		{
			unlocks = readUnlocks();
			readAt = now;
		}
		boolean allowWild = wildernessDestination && Wilderness.allowed(configManager);
		return TravelPlanner.plan(TravelData.all(), unlocks, carried, inBank, from, destinations, destinationName,
			allowWild);
	}

	private TravelUnlocks readUnlocks()
	{
		Map<String, Boolean> quests = new HashMap<>();
		TravelUnlocks.Source source = new TravelUnlocks.Source()
		{
			@Override
			public boolean questDone(String name)
			{
				return quests.computeIfAbsent(name, n ->
				{
					Quest q = QUESTS.get(n);
					return q != null && q.getState(client) == QuestState.FINISHED;
				});
			}

			@Override
			public int level(Skill skill)
			{
				return client.getRealSkillLevel(skill);
			}

			@Override
			public int varbit(int id)
			{
				return client.getVarbitValue(id);
			}

			@Override
			public int varp(int id)
			{
				return client.getVarpValue(id);
			}
		};
		boolean rings = client.getVarbitValue(VarbitID.FAIRY2_QUEENCURE_QUEST) > 39;
		boolean staff = client.getVarbitValue(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) != 1;
		return TravelUnlocks.read(TravelData.all(), source, rings, staff);
	}

	/**
	 * What to bring (a travel item, coins for a paid transport) for a Skilling Helper or Banked XP player heading
	 * for {@code places}. Nothing while sailing, in an instance or for places at sea.
	 */
	public List<TravelPick> plan(PlayerState s, List<Location> places)
	{
		if (places.isEmpty() || s.isOnBoat() || s.isInInstance() || places.get(0).isAtSea())
		{
			return Collections.emptyList();
		}
		List<WorldPoint> points = new ArrayList<>();
		boolean wild = false;
		for (Location l : places)
		{
			points.add(l.getPoint());
			wild |= Wilderness.contains(l);
		}
		ItemCounts bank = s.getBank();
		return plan(id -> s.getInventory().count(id) + s.getEquipment().count(id), id -> bank == null ? 0 : bank.count(id),
			s.getLocation(), points, places.get(0).getName(), wild);
	}
}
