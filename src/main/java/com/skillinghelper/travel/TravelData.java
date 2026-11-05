package com.skillinghelper.travel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/**
 * Every teleport item and fairy ring, loaded once from {@code travel.tsv}. That file is built from the Shortest Path
 * plugin's transport data by {@code tools/travel/build_travel.py}, so whatever the guide suggests is something Shortest
 * Path can route with.
 */
@Slf4j
public final class TravelData
{
	private static final String RESOURCE = "/com/skillinghelper/data/travel.tsv";

	/**
	 * Staffs that open fairy rings, best first: the plain Dramen staff, then the Lunar staff.
	 */
	public static final int[] FAIRY_STAFFS = {
		ItemID.DRAMEN_STAFF, ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF,
		ItemID.DRAMEN_STAFF_AIR, ItemID.DRAMEN_STAFF_FIRE, ItemID.DRAMEN_STAFF_WATER,
	};

	private static List<TravelOption> all;

	private TravelData()
	{
	}

	public static synchronized List<TravelOption> all()
	{
		if (all == null)
		{
			all = load();
		}
		return all;
	}

	public static String staffName(int id)
	{
		return id == ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF ? "Lunar staff" : "Dramen staff";
	}

	private static List<TravelOption> load()
	{
		List<TravelOption> out = new ArrayList<>();
		try (InputStream in = TravelData.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("Missing {}", RESOURCE);
				return Collections.emptyList();
			}
			BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = r.readLine()) != null)
			{
				if (line.isEmpty() || line.startsWith("#"))
				{
					continue;
				}
				TravelOption o = parse(line);
				if (o != null)
				{
					out.add(o);
				}
			}
		}
		catch (IOException e)
		{
			log.warn("Couldn't read {}", RESOURCE, e);
		}
		return Collections.unmodifiableList(out);
	}

	static TravelOption parse(String line)
	{
		String[] c = line.split("\t", -1);
		if (c.length < 10)
		{
			return null;
		}
		String[] xyz = c[3].trim().split(" ");
		if (xyz.length != 3)
		{
			return null;
		}
		WorldPoint point = new WorldPoint(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]),
			Integer.parseInt(xyz[2]));
		int[] ids = c[4].isEmpty() ? new int[0]
			: java.util.Arrays.stream(c[4].split("\\|")).mapToInt(Integer::parseInt).toArray();
		List<String> quests = new ArrayList<>();
		for (String q : c[6].split(";"))
		{
			if (!q.trim().isEmpty())
			{
				quests.add(q.trim());
			}
		}
		Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
		for (String s : c[7].split(";"))
		{
			int eq = s.indexOf('=');
			if (eq > 0)
			{
				try
				{
					skills.put(Skill.valueOf(s.substring(0, eq)), Integer.parseInt(s.substring(eq + 1)));
				}
				catch (IllegalArgumentException e)
				{
					// A skill this client doesn't know: the option can't be checked, so leave it out
					return null;
				}
			}
		}
		return new TravelOption("F".equals(c[0]) ? TravelOption.Kind.FAIRY_RING : TravelOption.Kind.ITEM,
			c[1], c[2], point, ids, "T".equals(c[5]), quests, skills, checks(c[8]), checks(c[9]));
	}

	private static List<TravelOption.Check> checks(String text)
	{
		List<TravelOption.Check> out = new ArrayList<>();
		for (String part : text.split(";"))
		{
			if (part.isEmpty())
			{
				continue;
			}
			int at = -1;
			for (int i = 0; i < part.length(); i++)
			{
				char ch = part.charAt(i);
				if (ch == '=' || ch == '<' || ch == '>')
				{
					at = i;
					break;
				}
			}
			out.add(new TravelOption.Check(Integer.parseInt(part.substring(0, at)), part.charAt(at),
				Integer.parseInt(part.substring(at + 1))));
		}
		return out;
	}
}
