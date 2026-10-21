package com.skillinghelper;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.PatchType;
import com.skillinghelper.data.Stop;
import com.skillinghelper.data.guides.GuideRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;

/**
 * Remembers the state of every farming patch that guides run through. A patch's varbit is only valid near the
 * patch, so it's read whenever the player is close and the value is saved per account with the time it was seen,
 * which is enough to tell how long the crop has left. Runs on the client thread.
 */
@Singleton
class PatchTracker
{
	private static final String KEY = "patch.";
	/**
	 * How close the player must be for the varbit to belong to the patch.
	 */
	private static final int READ_DISTANCE = 20;

	private final Client client;
	private final ConfigManager configManager;
	private final List<Stop> patches = new ArrayList<>();
	/**
	 * Last seen varbit value and when, in epoch seconds, by stop id.
	 */
	private final Map<String, long[]> seen = new HashMap<>();
	private boolean loaded;
	/**
	 * The region the player was in last tick: right after arriving, the varbits still describe the old region.
	 */
	private int lastRegion = -1;

	@Inject
	PatchTracker(Client client, ConfigManager configManager, GuideRepository guides)
	{
		this.client = client;
		this.configManager = configManager;
		Map<String, Stop> unique = new LinkedHashMap<>();
		for (Method m : guides.all())
		{
			for (Stop stop : m.getStops())
			{
				if (stop.getPatch() != null)
				{
					unique.putIfAbsent(stop.getId(), stop);
				}
			}
		}
		patches.addAll(unique.values());
	}

	void load()
	{
		seen.clear();
		for (Stop stop : patches)
		{
			String stored = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY + stop.getId());
			if (stored == null)
			{
				continue;
			}
			String[] parts = stored.split(":");
			try
			{
				seen.put(stop.getId(), new long[]{Integer.parseInt(parts[0]), Long.parseLong(parts[1])});
			}
			catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored)
			{
				// corrupt entry: treat as unknown
			}
		}
		loaded = true;
	}

	void reset()
	{
		loaded = false;
		lastRegion = -1;
		seen.clear();
	}

	/**
	 * Reads the varbits of patches the player is standing near. Call every game tick.
	 */
	void tick(WorldPoint player)
	{
		if (!loaded || player == null)
		{
			return;
		}
		int region = player.getRegionID();
		boolean settled = region == lastRegion;
		lastRegion = region;
		if (!settled)
		{
			return;
		}
		for (Stop stop : patches)
		{
			WorldPoint p = stop.getLocation().getPoint();
			if (player.getRegionID() != p.getRegionID() || player.getPlane() != p.getPlane()
				|| player.distanceTo2D(p) > READ_DISTANCE)
			{
				continue;
			}
			int value = client.getVarbitValue(stop.getVarbit());
			long[] old = seen.get(stop.getId());
			if (old == null || old[0] != value)
			{
				long now = Instant.now().getEpochSecond();
				seen.put(stop.getId(), new long[]{value, now});
				configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY + stop.getId(), value + ":" + now);
			}
		}
	}

	/**
	 * Minutes until each growing patch needs a visit, by stop id. Patches that need a visit now, or that haven't been
	 * seen yet, are left out so the guide sends the player there.
	 */
	Map<String, Integer> waits()
	{
		Map<String, Integer> waits = new HashMap<>();
		long now = Instant.now().getEpochSecond();
		for (Stop stop : patches)
		{
			long left = readyAt(stop) - now;
			if (left > 0)
			{
				waits.put(stop.getId(), (int) ((left + 59) / 60));
			}
		}
		return waits;
	}

	/**
	 * When every growing patch of this type will be ready, in epoch seconds, or 0 if none are growing.
	 */
	long readyAt(PatchType type)
	{
		long latest = 0;
		for (Stop stop : patches)
		{
			if (stop.getPatch() == type)
			{
				latest = Math.max(latest, readyAt(stop));
			}
		}
		return latest;
	}

	private long readyAt(Stop stop)
	{
		long[] s = seen.get(stop.getId());
		if (s == null)
		{
			return 0;
		}
		int minutes = stop.getPatch().minutesLeft((int) s[0]);
		return minutes == 0 ? 0 : s[1] + minutes * 60L;
	}
}
