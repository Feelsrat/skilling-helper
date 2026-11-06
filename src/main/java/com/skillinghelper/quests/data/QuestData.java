package com.skillinghelper.quests.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * The wiki's optimal quest routes and every quest they name, read from {@code order.tsv} and {@code quests.tsv}
 * (built by tools/quests/build_quests.py).
 */
public final class QuestData
{
	public static final String RFD = "Recipe for Disaster";

	/**
	 * Wiki subpage name to the game's name for Recipe for Disaster's parts.
	 */
	private static final Map<String, String> RFD_PARTS = Map.of(
		"Another Cook's Quest", "Another Cook's Quest",
		"Freeing the Mountain Dwarf", "Mountain Dwarf",
		"Freeing the Goblin generals", "Wartface & Bentnoze",
		"Freeing Pirate Pete", "Pirate Pete",
		"Freeing the Lumbridge Guide", "Lumbridge Guide",
		"Freeing Evil Dave", "Evil Dave",
		"Freeing Skrach Uglogwee", "Skrach Uglogwee",
		"Freeing Sir Amik Varze", "Sir Amik Varze",
		"Freeing King Awowogei", "King Awowogei",
		"Defeating the Culinaromancer", "Culinaromancer");

	private static Map<String, QuestInfo> quests;
	private static Map<Route, List<OrderRow>> routes;
	private static Map<String, Quest> gameQuests;

	private QuestData()
	{
	}

	public static synchronized Map<String, QuestInfo> quests()
	{
		if (quests == null)
		{
			Map<String, QuestInfo> m = new LinkedHashMap<>();
			for (String[] c : read("quests.tsv"))
			{
				QuestInfo q = new QuestInfo(c[0], "Y".equals(c[1]), c[2], c[3], c[4], point(c[5]), c[6], skills(c[7]),
					list(c[8]), list(c[9]), c[10].isEmpty() ? 0 : Integer.parseInt(c[10]), combat(c[11]), list(c[12]),
					list(c[13]), list(c[14]));
				m.put(q.getName(), q);
			}
			quests = Collections.unmodifiableMap(m);
		}
		return quests;
	}

	public static QuestInfo quest(String name)
	{
		return quests().get(name);
	}

	public static synchronized List<OrderRow> route(Route route)
	{
		if (routes == null)
		{
			Map<Route, List<OrderRow>> m = new EnumMap<>(Route.class);
			for (Route r : Route.values())
			{
				m.put(r, new ArrayList<>());
			}
			for (String[] c : read("order.tsv"))
			{
				Route r = Route.valueOf(c[0]);
				List<OrderRow> rows = m.get(r);
				List<String> notes = c[4].isEmpty() ? List.of() : Arrays.asList(c[4].split("\\|"));
				rows.add(new OrderRow(r, rows.size(), OrderRow.Kind.valueOf(c[1]), c[2], c[3], notes));
			}
			for (Route r : Route.values())
			{
				m.put(r, Collections.unmodifiableList(m.get(r)));
			}
			routes = m;
		}
		return routes.get(route);
	}

	/**
	 * The game's quest for a wiki quest name, or null.
	 */
	public static synchronized Quest gameQuest(String name)
	{
		if (gameQuests == null)
		{
			Map<String, Quest> byName = new HashMap<>();
			for (Quest q : Quest.values())
			{
				byName.put(q.getName(), q);
			}
			gameQuests = byName;
		}
		if (name.startsWith(RFD + "/"))
		{
			String part = RFD_PARTS.get(name.substring(RFD.length() + 1));
			return part == null ? null : gameQuests.get(RFD + " - " + part);
		}
		return gameQuests.get(name);
	}

	private static List<String[]> read(String file)
	{
		List<String[]> rows = new ArrayList<>();
		try (InputStream in = QuestData.class.getResourceAsStream("/com/skillinghelper/quests/" + file))
		{
			if (in == null)
			{
				throw new IllegalStateException("Missing " + file);
			}
			BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = r.readLine()) != null)
			{
				if (line.isEmpty() || line.startsWith("#"))
				{
					continue;
				}
				rows.add(line.split("\t", -1));
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Can't read " + file, e);
		}
		return rows;
	}

	private static List<String> list(String s)
	{
		return s.isEmpty() ? List.of() : List.of(s.split(";"));
	}

	private static WorldPoint point(String s)
	{
		String[] p = s.split(",");
		if (p.length < 2 || p[0].isEmpty() || p[1].isEmpty())
		{
			return null;
		}
		int plane = p.length > 2 && !p[2].isEmpty() ? Integer.parseInt(p[2]) : 0;
		return new WorldPoint(Integer.parseInt(p[0]), Integer.parseInt(p[1]), plane);
	}

	static List<QuestInfo.SkillReq> skills(String s)
	{
		List<QuestInfo.SkillReq> out = new ArrayList<>();
		for (String part : list(s))
		{
			String[] w = part.split(" ");
			if (w.length < 2)
			{
				continue;
			}
			boolean boostable = w.length > 2 && "b".equals(w[2]);
			int level = Integer.parseInt(w[1]);
			if ("Combat".equals(w[0]))
			{
				out.add(new QuestInfo.SkillReq(null, level, false));
				continue;
			}
			try
			{
				out.add(new QuestInfo.SkillReq(Skill.valueOf(w[0].toUpperCase()), level, boostable));
			}
			catch (IllegalArgumentException e)
			{
				// A skill this client doesn't know yet: leave it out
			}
		}
		return out;
	}

	private static int combat(String s)
	{
		for (QuestInfo.SkillReq r : skills(s))
		{
			if (r.getSkill() == null)
			{
				return r.getLevel();
			}
		}
		return 0;
	}
}
