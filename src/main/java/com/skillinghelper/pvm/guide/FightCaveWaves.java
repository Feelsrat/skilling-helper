package com.skillinghelper.pvm.guide;

import com.skillinghelper.data.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	 * Listed in the wiki's kill order, which is also the order wave lines show them in.
	 */
	@Getter
	@RequiredArgsConstructor
	public enum Monster
	{
		TZ_KIH("Tz-Kih", 22, "melee; every hit drains your prayer"),
		TOK_XIL("Tok-Xil", 90, "accurate ranged that hits hard; weaker melee up close"),
		KET_ZEK("Ket-Zek", 360, "magic from range, magic and a very hard melee up close"),
		YT_MEJKOT("Yt-MejKot", 180, "slow melee that hits high; heals itself and monsters near it"),
		TZ_KEK("Tz-Kek", 45, "weak melee; 1 recoil damage when you melee it; splits into two small Tz-Kek"),
		JAD("TzTok-Jad", 702, "magic and ranged from range, melee only next to you");

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

	private static final Monster[] BUILDING_KILL_ORDER = {
		Monster.TZ_KIH, Monster.TOK_XIL, Monster.KET_ZEK, Monster.YT_MEJKOT, Monster.TZ_KEK,
	};

	private static final String HIT_AND_RUN = "Kill the Tz-Kih first, hit-and-run so it can't drain your prayer.";
	private static final String BLOCK_MEJKOT = "Block the Yt-MejKot behind a rock (Italy or Dragon rock) or other "
		+ "monsters.";
	private static final String BLOCK_WITH_KET_ZEK = "Use the Ket-Zek and other monsters to block the Yt-MejKot.";

	/**
	 * The OSRS Wiki's per-wave notes (Template:TzHaar Fight Cave Waves), for the wave that's up only. Left out on
	 * purpose: the wiki's prayer advice (combat prayer recommendations are banned) and its notes about where later
	 * waves and Jad will spawn (advance warning of future hazards is banned).
	 */
	private static final Map<Integer, String> NOTES = notes();

	private static Map<Integer, String> notes()
	{
		Map<Integer, String> n = new HashMap<>();
		n.put(1, "Hit-and-run the Tz-Kih so it can't drain your prayer.");
		n.put(2, "Again, hit and run.");
		for (int w : new int[]{4, 5, 8, 9, 11, 16, 19, 23, 26, 27, 32, 35, 39, 42, 47, 50, 54, 57})
		{
			n.put(w, HIT_AND_RUN);
		}
		n.put(15, BLOCK_MEJKOT + " Guthan's can heal off it.");
		for (int w : new int[]{18, 21, 28})
		{
			n.put(w, BLOCK_MEJKOT);
		}
		n.put(31, "Keep your distance: the Ket-Zek's melee hits very hard. Hit-and-run works but isn't for beginners.");
		n.put(43, "Halfway: wave 43 is the midpoint of the damage from waves 1 to 62.");
		n.put(45, "Take out the rangers first.");
		n.put(46, "Block the Yt-MejKot with the Ket-Zek, Italy rock, Dragon rock or other monsters.");
		for (int w : new int[]{49, 52, 56, 59})
		{
			n.put(w, BLOCK_WITH_KET_ZEK);
		}
		n.put(53, "The Tok-Xil can block the Yt-MejKot if needed. " + BLOCK_WITH_KET_ZEK);
		n.put(60, "Kill the rangers first.");
		n.put(61, "Last wave to heal with Guthan's, or to regenerate in a safespot once the Ket-Zek is dead. To take a "
			+ "break before Jad, log out at the end of this wave, not the next one.");
		n.put(62, "Before you kill the last Ket-Zek, top up your hitpoints and prayer.");
		n.put(63, "Stay out of his melee range. At half health four healers appear: hit each from long range to drag "
			+ "them off him. They don't have to die; if they heal him to full and you kill them, they come back.");
		return Collections.unmodifiableMap(n);
	}

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
	 * The wiki's tip for wave {@code n}, or null.
	 */
	public static String note(int n)
	{
		return NOTES.get(n);
	}

	/**
	 * e.g. "Tz-Kih x2 (22), Tok-Xil (90)".
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
	 * The first monster in the usual kill order that's on screen. A fixed order by monster type, the same for every
	 * player and wave; it never looks at what a monster is doing or who it's attacking.
	 */
	public static List<Target> firstTarget(GuideState s)
	{
		for (Monster m : BUILDING_KILL_ORDER)
		{
			if (s.npc(m.getNpcName()) > 0)
			{
				return List.of(Target.npc(null, m.getNpcName()));
			}
		}
		return List.of();
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
		String note = NOTES.get(n);
		if (note != null)
		{
			lines.add("Wiki tip: " + note);
		}
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
