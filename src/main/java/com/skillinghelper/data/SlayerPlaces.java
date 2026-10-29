package com.skillinghelper.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * Where each Slayer task can be done, keyed by the game's task id (the SLAYER_TARGET varp). Loaded from
 * {@code slayer-places.tsv}, which tools/slayer/build_slayer_places.py builds from wiki-sourced data: see its header
 * for the sources.
 */
public final class SlayerPlaces
{
	/**
	 * One place a task's monsters spawn.
	 */
	@Value
	public static class Place
	{
		String key;
		String label;
		/**
		 * Konar's area id for the place (the SLAYER_AREA varp), or 0 when she never assigns it.
		 */
		int areaId;
		Set<Quest> questsFinished;
		Set<Quest> questsStarted;
		Map<Skill, Integer> skills;
		/**
		 * Access needs the guide can't check, e.g. "Needs 10 Woodcutting to cut the vines". Null if none.
		 */
		String note;
		/**
		 * Spawn tiles, one per cluster of monsters. Empty when only the entrance is known.
		 */
		List<WorldPoint> spots;
		/**
		 * The world map entrance, or null.
		 */
		WorldPoint entrance;
	}

	private static final SlayerPlaces INSTANCE = load();

	private final Map<Integer, String> taskNames = new HashMap<>();
	private final Map<Integer, List<Place>> places = new HashMap<>();
	private final Map<Integer, List<Place>> wildernessPlaces = new HashMap<>();
	private final Map<String, List<String>> npcNames = new HashMap<>();
	private final Set<Quest> quests = EnumSet.noneOf(Quest.class);

	public static SlayerPlaces get()
	{
		return INSTANCE;
	}

	/**
	 * The task's name, e.g. "Aberrant spectres", or null for a task id that isn't known.
	 */
	public String taskName(int taskId)
	{
		return taskNames.get(taskId);
	}

	/**
	 * Every place the task can be done, best first.
	 *
	 * @param wilderness Krystilia's version of the task, which only counts in the Wilderness
	 */
	public List<Place> places(int taskId, boolean wilderness)
	{
		List<Place> list = (wilderness ? wildernessPlaces : places).get(taskId);
		return list == null ? Collections.emptyList() : list;
	}

	/**
	 * Lower case NPC names that count for a task, besides its own name: "chicken" for Birds and so on.
	 */
	public List<String> extraNpcNames(String taskName)
	{
		List<String> names = taskName == null ? null : npcNames.get(taskName.toLowerCase(Locale.ROOT));
		return names == null ? Collections.emptyList() : names;
	}

	/**
	 * Every quest some place needs, so the plugin knows which quest states to read.
	 */
	public Set<Quest> quests()
	{
		return Collections.unmodifiableSet(quests);
	}

	private static SlayerPlaces load()
	{
		SlayerPlaces p = new SlayerPlaces();
		try (InputStream in = SlayerPlaces.class.getResourceAsStream("slayer-places.tsv"))
		{
			if (in == null)
			{
				throw new IllegalStateException("slayer-places.tsv is missing");
			}
			BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = r.readLine()) != null)
			{
				if (!line.isEmpty() && !line.startsWith("#"))
				{
					p.parse(line.split("\t", -1));
				}
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return p;
	}

	private void parse(String[] f)
	{
		switch (f[0])
		{
			case "task":
				taskNames.put(Integer.parseInt(f[1]), f[2]);
				break;
			case "place":
			case "wild":
				Place place = new Place(f[2], f[3], Integer.parseInt(f[4]), parseQuests(f[5]), parseQuests(f[6]),
					parseSkills(f[7]), dash(f[8]), parsePoints(f[9]), f[10].equals("-") ? null : parsePoint(f[10]));
				(f[0].equals("wild") ? wildernessPlaces : places)
					.computeIfAbsent(Integer.parseInt(f[1]), k -> new ArrayList<>()).add(place);
				break;
			case "names":
				npcNames.put(f[1], List.of(f[2].split(";")));
				break;
			default:
				break;
		}
	}

	private static String dash(String s)
	{
		return s.equals("-") ? null : s;
	}

	private Set<Quest> parseQuests(String s)
	{
		Set<Quest> out = EnumSet.noneOf(Quest.class);
		if (!s.equals("-"))
		{
			for (String name : s.split(","))
			{
				try
				{
					out.add(Quest.valueOf(name));
				}
				catch (IllegalArgumentException e)
				{
					// A quest this RuneLite version doesn't know yet: can't check it, so don't require it
				}
			}
		}
		quests.addAll(out);
		return out;
	}

	private static Map<Skill, Integer> parseSkills(String s)
	{
		Map<Skill, Integer> out = new EnumMap<>(Skill.class);
		if (!s.equals("-"))
		{
			for (String part : s.split(","))
			{
				String[] kv = part.split(":");
				out.put(Skill.valueOf(kv[0]), Integer.parseInt(kv[1]));
			}
		}
		return out;
	}

	private static List<WorldPoint> parsePoints(String s)
	{
		if (s.equals("-"))
		{
			return Collections.emptyList();
		}
		List<WorldPoint> out = new ArrayList<>();
		for (String p : s.split(";"))
		{
			out.add(parsePoint(p));
		}
		return out;
	}

	private static WorldPoint parsePoint(String s)
	{
		String[] xyz = s.split(",");
		return new WorldPoint(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
	}

	private SlayerPlaces()
	{
	}
}
