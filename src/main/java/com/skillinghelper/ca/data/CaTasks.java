package com.skillinghelper.ca.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Every Combat Achievement task and tier reward, read once from the bundled wiki data
 * ({@code tools/ca/build_ca_tasks.py} builds it).
 */
public final class CaTasks
{
	private static final String TASKS = "/com/skillinghelper/ca/ca-tasks.tsv";
	private static final String REWARDS = "/com/skillinghelper/ca/ca-rewards.tsv";

	private static List<CaTask> all;
	private static Map<CaTier, CaReward> rewards;

	private CaTasks()
	{
	}

	public static synchronized List<CaTask> all()
	{
		if (all == null)
		{
			List<CaTask> out = new ArrayList<>();
			for (String[] f : read(TASKS))
			{
				out.add(parse(f));
			}
			all = Collections.unmodifiableList(out);
		}
		return all;
	}

	public static List<CaTask> tier(CaTier tier)
	{
		List<CaTask> out = new ArrayList<>();
		for (CaTask t : all())
		{
			if (t.getTier() == tier)
			{
				out.add(t);
			}
		}
		return out;
	}

	public static synchronized CaReward reward(CaTier tier)
	{
		if (rewards == null)
		{
			Map<CaTier, CaReward> out = new EnumMap<>(CaTier.class);
			for (String[] f : read(REWARDS))
			{
				CaTier t = CaTier.fromLabel(f[0]);
				out.put(t, new CaReward(t, Integer.parseInt(f[1].trim()),
					Collections.unmodifiableList(Arrays.asList(f).subList(2, f.length))));
			}
			rewards = out;
		}
		return rewards.get(tier);
	}

	static CaTask parse(String[] f)
	{
		Map<Skill, Integer> levels = new LinkedHashMap<>();
		for (String s : split(field(f, 7), ";"))
		{
			int space = s.lastIndexOf(' ');
			Skill skill = skill(s.substring(0, space));
			int level = Integer.parseInt(s.substring(space + 1));
			if (skill != null && level > 1)
			{
				levels.put(skill, level);
			}
		}
		List<Quest> quests = new ArrayList<>();
		for (String name : split(field(f, 8), ";"))
		{
			Quest q = quest(name);
			if (q != null && !quests.contains(q))
			{
				quests.add(q);
			}
		}
		return new CaTask(Integer.parseInt(f[0]), CaTier.fromLabel(f[1]), CaType.fromLabel(f[2]), f[3], f[4], f[5],
			!"0".equals(f[6]), levels, quests, split(field(f, 9), " | "));
	}

	/**
	 * The quest with this wiki name, e.g. "Desert Treasure II" for Desert Treasure II - The Fallen Empire.
	 */
	static Quest quest(String wikiName)
	{
		String want = normal(wikiName);
		if (want.isEmpty())
		{
			return null;
		}
		for (Quest q : Quest.values())
		{
			String have = normal(q.getName());
			if (have.equals(want) || (have.startsWith(want + " ") && want.length() > 8))
			{
				return q;
			}
		}
		return null;
	}

	private static String normal(String s)
	{
		return s.toLowerCase().replace('_', ' ').replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
	}

	private static Skill skill(String name)
	{
		for (Skill s : Skill.values())
		{
			if (s.getName().equalsIgnoreCase(name.trim()))
			{
				return s;
			}
		}
		return null;
	}

	private static String field(String[] f, int i)
	{
		return i < f.length ? f[i] : "";
	}

	private static List<String> split(String s, String sep)
	{
		List<String> out = new ArrayList<>();
		if (s == null || s.trim().isEmpty())
		{
			return out;
		}
		for (String part : s.split(java.util.regex.Pattern.quote(sep)))
		{
			if (!part.trim().isEmpty())
			{
				out.add(part.trim());
			}
		}
		return out;
	}

	private static List<String[]> read(String resource)
	{
		InputStream in = CaTasks.class.getResourceAsStream(resource);
		if (in == null)
		{
			throw new IllegalStateException("Missing " + resource);
		}
		List<String[]> rows = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = r.readLine()) != null)
			{
				if (!line.isEmpty() && !line.startsWith("#"))
				{
					rows.add(line.split("\t", -1));
				}
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return rows;
	}
}
