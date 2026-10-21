package com.skillinghelper;

import java.awt.Color;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

/**
 * Follows the four Fossil Island bird houses through their varps, the same way RuneLite's Time Tracking plugin does,
 * and remembers when each one was seeded so the guide knows which still need a visit. Also shows a countdown until
 * they're full and sends a notification when it's time for a run. Everything runs on the client thread.
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
	@Inject
	private SkillingHelperConfig config;
	@Inject
	private InfoBoxManager infoBoxManager;
	@Inject
	private ItemManager itemManager;
	@Inject
	private Notifier notifier;

	private final int[] values = new int[Space.values().length];
	/**
	 * When each space last changed, in epoch seconds, or 0 if unknown.
	 */
	private final long[] changed = new long[Space.values().length];
	private boolean loaded;
	private boolean notified;
	/**
	 * When every seeded bird house will be full, in epoch seconds, or 0 if none are seeded.
	 */
	private long readyAt;
	private Countdown countdown;

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
		// Don't notify on login for houses that filled up while logged out; the countdown already says "Ready"
		notified = readyAt <= now();
	}

	void reset()
	{
		loaded = false;
		readyAt = 0;
		removeCountdown();
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
	 * Updates the countdown and notifies once the bird houses are full. Call every game tick.
	 */
	void tick(Plugin plugin)
	{
		if (!loaded)
		{
			return;
		}
		long now = now();
		if (readyAt > now)
		{
			notified = false;
		}
		else if (readyAt > 0 && !notified)
		{
			notified = true;
			notifier.notify(config.birdhouseNotification(), "Your bird houses are full: time for a bird house run!");
		}

		boolean show = readyAt > 0 && config.birdhouseTimer();
		if (show && countdown == null)
		{
			countdown = new Countdown(plugin);
			infoBoxManager.addInfoBox(countdown);
		}
		else if (!show)
		{
			removeCountdown();
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

	private void removeCountdown()
	{
		if (countdown != null)
		{
			infoBoxManager.removeInfoBox(countdown);
			countdown = null;
		}
	}

	private static long now()
	{
		return Instant.now().getEpochSecond();
	}

	private final class Countdown extends InfoBox
	{
		Countdown(Plugin plugin)
		{
			super(itemManager.getImage(ItemID.BIRDHOUSE_OAK), plugin);
		}

		private long secondsLeft()
		{
			return readyAt - now();
		}

		@Override
		public String getText()
		{
			long left = secondsLeft();
			return left <= 0 ? "Ready" : ((left + 59) / 60) + "m";
		}

		@Override
		public Color getTextColor()
		{
			return secondsLeft() <= 0 ? Color.GREEN : Color.WHITE;
		}

		@Override
		public String getTooltip()
		{
			long left = secondsLeft();
			return left <= 0 ? "Bird houses are full: time for a run"
				: "Bird houses full in ~" + ((left + 59) / 60) + " min";
		}
	}
}
