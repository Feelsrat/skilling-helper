package com.skillinghelper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.config.ConfigManager;

/**
 * Follows the four Fossil Island bird houses through their varps, the same way RuneLite's Time Tracking plugin does,
 * and remembers when each one was seeded so the guide knows which still need a visit and when they'll be full.
 * Everything runs on the client thread.
 */
@Singleton
class BirdhouseTracker
{
	static final int FILL_MINUTES = 50;
	private static final String KEY = "birdhouse.";
	/**
	 * RuneLite's Time Tracking plugin keeps "value:epoch" per bird house varp under this group, as birdhouse.(varp).
	 */
	private static final String TIME_TRACKING_GROUP = "timetracking";
	private static final int SETTLE_TICKS = 3;

	/**
	 * The bird house spaces, keyed by the stop ids the Hunter guide uses.
	 */
	@AllArgsConstructor
	@Getter
	enum Space
	{
		VALLEY_NE("birdhouse_valley_ne", VarPlayerID.BIRDHOUSE_TRANSMIT_C),
		VALLEY_SW("birdhouse_valley_sw", VarPlayerID.BIRDHOUSE_TRANSMIT_D),
		MEADOW_S("birdhouse_meadow_s", VarPlayerID.BIRDHOUSE_TRANSMIT_B),
		MEADOW_N("birdhouse_meadow_n", VarPlayerID.BIRDHOUSE_TRANSMIT_A);

		private final String stopId;
		private final int varp;
	}

	@Inject
	private Client client;
	@Inject
	private ConfigManager configManager;

	private final int[] values = new int[Space.values().length];
	/**
	 * When each space last changed, in epoch seconds, or 0 if unknown.
	 */
	private final long[] changed = new long[Space.values().length];
	private boolean loaded;
	private int loadedTick;
	/**
	 * When every seeded bird house will be full, in epoch seconds, or 0 if none are seeded.
	 */
	@Getter
	private long readyAt;

	/**
	 * Loads the saved timestamps for the logged-in account and catches up on changes made while it wasn't tracked.
	 * A space whose state changed while we weren't watching gets its time from RuneLite's Time Tracking plugin if
	 * that saw it, otherwise its age is unknown and the guide sends the player there to check it.
	 */
	void load()
	{
		Space[] spaces = Space.values();
		for (int i = 0; i < spaces.length; i++)
		{
			values[i] = -1;
			changed[i] = 0;
			parse(configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY + spaces[i].name()), i);
			catchUp(i, client.getVarpValue(spaces[i].getVarp()));
		}
		loaded = true;
		loadedTick = client.getTickCount();
		updateReadyAt();
	}

	private void parse(String stored, int i)
	{
		if (stored == null)
		{
			return;
		}
		String[] parts = stored.split(":");
		try
		{
			values[i] = Integer.parseInt(parts[0]);
			changed[i] = Long.parseLong(parts[1]);
		}
		catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored)
		{
			// corrupt entry: treat as unknown
			values[i] = -1;
			changed[i] = 0;
		}
	}

	/**
	 * Takes in a value seen without watching it change, so we can't say it changed just now.
	 */
	private void catchUp(int i, int value)
	{
		if (values[i] == value && changed[i] > 0)
		{
			return;
		}
		Space space = Space.values()[i];
		values[i] = -1;
		changed[i] = 0;
		parse(configManager.getRSProfileConfiguration(TIME_TRACKING_GROUP, KEY + space.getVarp()), i);
		if (values[i] != value)
		{
			values[i] = value;
			changed[i] = 0;
		}
		save(i);
	}

	void reset()
	{
		loaded = false;
		readyAt = 0;
	}

	void onVarpChanged(int varpId, int value)
	{
		if (!loaded)
		{
			return;
		}
		Space[] spaces = Space.values();
		for (int i = 0; i < spaces.length; i++)
		{
			if (spaces[i].getVarp() == varpId)
			{
				// Varps arrive in a burst right after login: those are the saved state, not a fresh change
				if (client.getTickCount() - loadedTick < SETTLE_TICKS)
				{
					catchUp(i, value);
				}
				else
				{
					record(i, value);
				}
				updateReadyAt();
			}
		}
	}

	/**
	 * Minutes until each seeded bird house is full, by stop id. Spaces that are empty, built without seeds, full, or
	 * of unknown age are left out so the guide sends the player there.
	 */
	Map<String, Integer> waits()
	{
		Map<String, Integer> waits = new HashMap<>();
		if (!loaded)
		{
			return waits;
		}
		long now = now();
		Space[] spaces = Space.values();
		for (int i = 0; i < spaces.length; i++)
		{
			long left = fullAt(i) - now;
			if (left > 0)
			{
				waits.put(spaces[i].getStopId(), (int) ((left + 59) / 60));
			}
		}
		return waits;
	}

	private void record(int i, int value)
	{
		if (values[i] == value)
		{
			return;
		}
		values[i] = value;
		changed[i] = now();
		save(i);
	}

	private void save(int i)
	{
		configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY + Space.values()[i].name(),
			values[i] + ":" + changed[i]);
	}

	/**
	 * When a seeded bird house will be full, or 0 if it isn't seeded or its age is unknown.
	 */
	private long fullAt(int i)
	{
		return seeded(values[i]) && changed[i] > 0 ? changed[i] + FILL_MINUTES * 60L : 0;
	}

	private void updateReadyAt()
	{
		readyAt = 0;
		for (int i = 0; i < values.length; i++)
		{
			readyAt = Math.max(readyAt, fullAt(i));
		}
	}

	/**
	 * Varp values are 0 for an empty space, then three per bird house type: a multiple of 3 once seeds are added.
	 */
	static boolean seeded(int value)
	{
		return value > 0 && value % 3 == 0;
	}

	private static long now()
	{
		return Instant.now().getEpochSecond();
	}
}
