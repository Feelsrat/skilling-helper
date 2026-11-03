package com.skillinghelper;

import java.util.EnumMap;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Measures the xp per hour the player is getting this session in the guide's skill, shown under the guide's estimate.
 * Only time spent training counts (up to a minute after the last xp drop, which covers banking), so breaks don't
 * drag the rate down. Kept in memory only and forgotten on logout; runs on the client thread.
 */
@Singleton
class SessionXpTracker
{
	private static final double TICK_SECONDS = 0.6;
	/**
	 * Ticks after the last xp drop that still count as training (one minute).
	 */
	private static final int ACTIVE_TICKS = 100;
	/**
	 * Training time needed before the rate is shown (one minute), so a single drop doesn't read as a huge rate.
	 */
	private static final long MIN_TICKS = 100;
	/**
	 * Bigger single jumps are lamps, quest rewards and the like, not training.
	 */
	private static final int MAX_DROP = 50_000;
	/**
	 * How often the shown rate is refreshed (one minute), unless it drifts by more than {@link #BIG_CHANGE}.
	 */
	private static final long SHOW_EVERY_TICKS = 100;
	private static final double BIG_CHANGE = 0.15;

	/**
	 * Xp gained and training ticks this session, by skill.
	 */
	private final Map<Skill, long[]> totals = new EnumMap<>(Skill.class);
	/**
	 * The rate last shown and the training tick it was shown at, by skill.
	 */
	private final Map<Skill, long[]> shown = new EnumMap<>(Skill.class);
	private Skill skill;
	private int lastXp = -1;
	private int ticksSinceXp = ACTIVE_TICKS;

	void reset()
	{
		totals.clear();
		shown.clear();
		skill = null;
		lastXp = -1;
		ticksSinceXp = ACTIVE_TICKS;
	}

	/**
	 * Call every game tick with the guide's skill and the player's xp in it, or a null skill when no guide is open.
	 */
	void tick(Skill current, int xp)
	{
		if (current == null || current != skill || lastXp < 0)
		{
			skill = current;
			lastXp = current == null ? -1 : xp;
			ticksSinceXp = ACTIVE_TICKS;
			return;
		}
		int gained = xp - lastXp;
		lastXp = xp;
		if (gained > 0 && gained < MAX_DROP)
		{
			totals.computeIfAbsent(skill, k -> new long[2])[0] += gained;
			ticksSinceXp = 0;
		}
		else if (ticksSinceXp < ACTIVE_TICKS)
		{
			ticksSinceXp++;
		}
		long[] t = totals.get(skill);
		if (t != null && ticksSinceXp < ACTIVE_TICKS)
		{
			t[1]++;
		}
	}

	/**
	 * This session's xp per hour in the skill, 0 before any xp, or -1 while there's xp but too little time to tell.
	 * The number holds steady between refreshes (once a minute of training, or sooner if it moves a lot), so the
	 * panel isn't rebuilt for every small wobble.
	 */
	int xpPerHour(Skill s)
	{
		long[] t = s == null ? null : totals.get(s);
		if (t == null || t[0] <= 0)
		{
			return 0;
		}
		if (t[1] < MIN_TICKS)
		{
			return -1;
		}
		long[] last = shown.get(s);
		if (last != null && t[1] - last[1] < SHOW_EVERY_TICKS)
		{
			int live = rate(t);
			if (Math.abs(live - last[0]) <= last[0] * BIG_CHANGE)
			{
				return (int) last[0];
			}
		}
		int live = rate(t);
		shown.put(s, new long[]{live, t[1]});
		return live;
	}

	private static int rate(long[] t)
	{
		return (int) Math.round(t[0] / (t[1] * TICK_SECONDS) * 3600);
	}
}
