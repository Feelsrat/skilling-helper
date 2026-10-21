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
	/**
	 * When every seeded bird house will be full, in epoch seconds, or 0 if none are seeded.
	 */
	@Getter
	private long readyAt;

	/**
	 * Loads the saved timestamps for the logged-in account and catches up on changes made while it wasn't tracked.
	 */
	void load()
	{
		Space[] spaces = Space.values();
		for (int i = 0; i < spaces.length; i++)
		{
			values[i] = -1;
			changed[i] = 0;
			String stored = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY + spaces[i].name());
			if (stored != null)
			{
				String[] parts = stored.split(":");
				try
				{
					values[i] = Integer.parseInt(parts[0]);
					changed[i] = Long.parseLong(parts[1]);
				}
				catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored)
				{
					// corrupt entry: treat as unknown
				}
			}
			record(i, client.getVarpValue(spaces[i].getVarp()));
		}
		loaded = true;
		updateReadyAt();
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
				record(i, value);
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
		configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY + Space.values()[i].name(),
			value + ":" + changed[i]);
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
