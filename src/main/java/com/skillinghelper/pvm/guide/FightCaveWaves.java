package com.skillinghelper.pvm.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Which monsters each Fight Cave wave has. The waves are fixed (the same list the wiki has), so this is reference
 * information like a wave table, not a reaction to the fight: nothing here looks at what a monster is doing.
 */
public final class FightCaveWaves
{
	public static final int JAD_WAVE = 63;
	/**
	 * Guide flag holding the wave the player is on, e.g. "fight-cave-wave:12".
	 */
	public static final String WAVE_FLAG = "fight-cave-wave:";

	private static final Pattern WAVE_MESSAGE = Pattern.compile("^Wave: (\\d+)$");

	/**
	 * Listed in the usual kill order, which is also the order wave lines show them in.
	 */
	@Getter
	@RequiredArgsConstructor
	public enum Monster
	{
		KET_ZEK("Ket-Zek", 360, "magic from range, melee up close"),
		TOK_XIL("Tok-Xil", 90, "ranged from range, melee up close"),
		YT_MEJKOT("Yt-MejKot", 180, "melee, and heals monsters near it"),
		TZ_KEK("Tz-Kek", 45, "melee; splits into two small Tz-Kek when it dies"),
		TZ_KIH("Tz-Kih", 22, "melee; every hit drains your prayer"),
		JAD("TzTok-Jad", 702, "magic, ranged and melee; summons healers at half health");

		private final String npcName;
		private final int level;
		private final String attacks;
	}

	/**
	 * The monsters that build the normal waves, smallest first.
	 */
	private static final Monster[] BUILDING = {
		Monster.TZ_KIH, Monster.TZ_KEK, Monster.TOK_XIL, Monster.YT_MEJKOT, Monster.KET_ZEK,
	};

	private static final List<List<Monster>> WAVES = build();

	private FightCaveWaves()
	{
	}

	/**
	 * Waves 1 to 62 count up like this: the next monster up joins once two of the previous size have had a wave
	 * together, and the smaller ones start again under it. Wave 63 is Jad on his own.
	 */
	private static List<List<Monster>> build()
	{
		List<List<Monster>> waves = new ArrayList<>();
		waves.add(List.of());
		for (Monster m : BUILDING)
		{
			List<List<Monster>> before = new ArrayList<>(waves);
			for (List<Monster> smaller : before)
			{
				List<Monster> wave = new ArrayList<>();
				wave.add(m);
				wave.addAll(smaller);
				waves.add(wave);
			}
			waves.add(List.of(m, m));
		}
		waves.add(List.of(Monster.JAD));
		List<List<Monster>> sorted = new ArrayList<>();
		for (List<Monster> wave : waves)
		{
			List<Monster> w = new ArrayList<>(wave);
			Collections.sort(w);
			sorted.add(Collections.unmodifiableList(w));
		}
		// Index 0 is the empty starting point, so wave n is at index n
		return Collections.unmodifiableList(sorted);
	}

	/**
	 * The monsters of wave {@code n} in kill order, or an empty list outside 1 to 63.
	 */
	public static List<Monster> wave(int n)
	{
		return n >= 1 && n <= JAD_WAVE ? WAVES.get(n) : List.of();
	}

	/**
	 * e.g. "Tok-Xil (90), Tz-Kih x2 (22)".
	 */
	public static String describe(List<Monster> wave)
	{
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < wave.size(); )
		{
			Monster m = wave.get(i);
			int n = 0;
			while (i < wave.size() && wave.get(i) == m)
			{
				n++;
				i++;
			}
			if (b.length() > 0)
			{
				b.append(", ");
			}
			b.append(m.getNpcName()).append(n > 1 ? " x" + n : "").append(" (").append(m.getLevel()).append(')');
		}
		return b.toString();
	}

	/**
	 * The wave number from the cave's "Wave: 12" chat message (tags already stripped), or -1.
	 */
	public static int parseWave(String message)
	{
		Matcher m = WAVE_MESSAGE.matcher(message.trim());
		if (!m.matches())
		{
			return -1;
		}
		int n = Integer.parseInt(m.group(1));
		return n >= 1 && n <= JAD_WAVE ? n : -1;
	}

	/**
	 * The wave the plugin last saw a message for, or -1 when it hasn't seen one this trip.
	 */
	public static int currentWave(GuideState s)
	{
		for (String f : s.getFlags())
		{
			if (f.startsWith(WAVE_FLAG))
			{
				return Integer.parseInt(f.substring(WAVE_FLAG.length()));
			}
		}
		return -1;
	}

	/**
	 * Panel lines while working through the waves: this wave and how each of its monsters attacks.
	 */
	public static List<String> lines(GuideState s)
	{
		List<String> lines = new ArrayList<>();
		int n = currentWave(s);
		if (n < 0)
		{
			lines.add("This wave's monsters show up once the next wave starts (the cave says \"Wave: N\" in chat).");
			return lines;
		}
		List<Monster> now = wave(n);
		// Only the wave that's already up: warning about future waves counts as advance warning of a hazard
		lines.add("Wave " + n + " of " + JAD_WAVE + ": " + describe(now));
		List<Monster> seen = new ArrayList<>();
		for (Monster m : now)
		{
			if (!seen.contains(m))
			{
				seen.add(m);
				lines.add("• " + m.getNpcName() + ": " + m.getAttacks());
			}
		}
		return lines;
	}
}
