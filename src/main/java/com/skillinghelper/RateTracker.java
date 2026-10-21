package com.skillinghelper;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.guides.GuideRepository;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Measures the xp per hour the player really gets with each method, so time estimates match how they play. Only
 * time spent actively gaining xp counts (up to a minute after the last drop, which covers banking), so breaks and
 * long walks don't drag the rate down. Saved per account; runs on the client thread.
 */
@Singleton
class RateTracker
{
	private static final String KEY = "rate.";
	private static final double TICK_SECONDS = 0.6;
	/**
	 * Ticks after the last xp drop that still count as training (one minute).
	 */
	private static final int ACTIVE_TICKS = 100;
	/**
	 * Measured time needed before the player's own rate replaces the guide's (ten minutes).
	 */
	private static final long MIN_TICKS = 1_000;
	/**
	 * Halve the history after about two hours, so the rate follows the player as they level up.
	 */
	private static final long MAX_TICKS = 12_000;
	private static final int SAVE_EVERY = 100;

	private final ConfigManager configManager;
	private final GuideRepository guides;
	/**
	 * Xp gained and active ticks, by method id.
	 */
	private final Map<String, long[]> totals = new HashMap<>();
	private String methodId;
	private int lastXp = -1;
	private int ticksSinceXp = ACTIVE_TICKS;
	private int unsaved;

	@Inject
	RateTracker(ConfigManager configManager, GuideRepository guides)
	{
		this.configManager = configManager;
		this.guides = guides;
	}

	void load()
	{
		reset();
		for (Method m : guides.all())
		{
			String stored = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY + m.getId());
			if (stored == null)
			{
				continue;
			}
			String[] parts = stored.split(":");
			try
			{
				totals.put(m.getId(), new long[]{Long.parseLong(parts[0]), Long.parseLong(parts[1])});
			}
			catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored)
			{
				// corrupt entry: start over
			}
		}
	}

	/**
	 * Forgets the loaded account without saving (the profile may already have changed).
	 */
	void reset()
	{
		unsaved = 0;
		totals.clear();
		methodId = null;
		lastXp = -1;
	}

	/**
	 * Call every game tick with the guide's current method and the player's xp in its skill, or a null method when
	 * no guide is open.
	 */
	void tick(Method method, int xp)
	{
		if (method == null || method.getCycleMinutes() > 0)
		{
			// Runs are timed by their stops, not by continuous play
			save();
			methodId = null;
			lastXp = -1;
			return;
		}
		if (!method.getId().equals(methodId) || lastXp < 0)
		{
			save();
			methodId = method.getId();
			lastXp = xp;
			ticksSinceXp = ACTIVE_TICKS;
			return;
		}
		int gained = xp - lastXp;
		lastXp = xp;
		long[] t = totals.computeIfAbsent(methodId, k -> new long[2]);
		// Ignore jumps such as xp lamps or quest rewards
		if (gained > 0 && gained < 20_000)
		{
			t[0] += gained;
			ticksSinceXp = 0;
		}
		else if (ticksSinceXp < ACTIVE_TICKS)
		{
			ticksSinceXp++;
		}
		if (ticksSinceXp < ACTIVE_TICKS)
		{
			t[1]++;
			if (t[1] > MAX_TICKS)
			{
				t[0] /= 2;
				t[1] /= 2;
			}
			if (++unsaved >= SAVE_EVERY)
			{
				save();
			}
		}
	}

	/**
	 * Measured xp per hour by method id, for methods with enough play.
	 */
	Map<String, Integer> rates()
	{
		Map<String, Integer> rates = new HashMap<>();
		totals.forEach((id, t) ->
		{
			if (t[1] >= MIN_TICKS && t[0] > 0)
			{
				rates.put(id, (int) Math.round(t[0] / (t[1] * TICK_SECONDS) * 3600));
			}
		});
		return rates;
	}

	void save()
	{
		if (unsaved == 0 || methodId == null)
		{
			unsaved = 0;
			return;
		}
		long[] t = totals.get(methodId);
		if (t != null)
		{
			configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY + methodId, t[0] + ":" + t[1]);
		}
		unsaved = 0;
	}
}
