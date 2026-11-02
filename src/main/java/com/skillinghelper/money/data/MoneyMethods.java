package com.skillinghelper.money.data;

import com.skillinghelper.data.Location;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Reads the bundled money-methods.tsv, built from the OSRS Wiki by tools/money/build_money_methods.py.
 */
public final class MoneyMethods
{
	private static final String RESOURCE = "/com/skillinghelper/money/money-methods.tsv";
	private static final int COLUMNS = 17;
	private static volatile List<MoneyMethod> all;

	private MoneyMethods()
	{
	}

	/**
	 * Every method, loaded once.
	 */
	public static List<MoneyMethod> all()
	{
		List<MoneyMethod> loaded = all;
		if (loaded == null)
		{
			synchronized (MoneyMethods.class)
			{
				if (all == null)
				{
					all = Collections.unmodifiableList(load());
				}
				loaded = all;
			}
		}
		return loaded;
	}

	private static List<MoneyMethod> load()
	{
		try (InputStream in = MoneyMethods.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IllegalStateException("Missing " + RESOURCE);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			List<MoneyMethod> methods = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (!line.isEmpty() && !line.startsWith("#"))
				{
					methods.add(parse(line));
				}
			}
			return methods;
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	static MoneyMethod parse(String line)
	{
		String[] c = line.split("\t", -1);
		if (c.length != COLUMNS)
		{
			throw new IllegalArgumentException("Expected " + COLUMNS + " columns: " + line);
		}
		MoneyMethod.MoneyMethodBuilder b = MoneyMethod.builder()
			.page(c[0])
			.name(c[1])
			.category(text(c[2]))
			.skillCategory(text(c[3]))
			.members("1".equals(c[4]))
			.intensity(text(c[5]))
			.wikiProfit(Long.parseLong(c[7]))
			.skills(skills(c[8]))
			.quests(list(c[9], ";"))
			.other(text(c[10]))
			.locationName(text(c[11]))
			.spot(spot(c[11], c[12]))
			.npc(text(c[13]))
			.inputs(items(c[14]))
			.outputs(items(c[15]))
			.details(text(c[16]));
		if (!"-".equals(c[6]))
		{
			String[] hours = c[6].split("/");
			b.runHours(Double.parseDouble(hours[0])).everyHours(Double.parseDouble(hours[1]));
		}
		return b.build();
	}

	private static String text(String s)
	{
		return "-".equals(s) || s.isEmpty() ? null : s;
	}

	private static List<String> list(String s, String sep)
	{
		if ("-".equals(s))
		{
			return Collections.emptyList();
		}
		List<String> out = new ArrayList<>();
		for (String part : s.split(sep))
		{
			if (!part.trim().isEmpty())
			{
				out.add(part.trim());
			}
		}
		return out;
	}

	private static Map<Skill, Integer> skills(String s)
	{
		Map<Skill, Integer> out = new EnumMap<>(Skill.class);
		for (String part : list(s, ","))
		{
			String[] kv = part.split(":");
			out.put(Skill.valueOf(kv[0].toUpperCase(Locale.ROOT)), Integer.parseInt(kv[1]));
		}
		return out;
	}

	private static Location spot(String name, String s)
	{
		if ("-".equals(s))
		{
			return null;
		}
		String[] p = s.split(",");
		return Location.of(name, Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]),
			Integer.parseInt(p[3]));
	}

	private static List<MoneyItem> items(String s)
	{
		List<MoneyItem> out = new ArrayList<>();
		for (String part : list(s, ";"))
		{
			String[] f = part.split(":", 5);
			out.add(new MoneyItem(Integer.parseInt(f[0]), Double.parseDouble(f[1]), Double.parseDouble(f[2]),
				Integer.parseInt(f[3]), f[4]));
		}
		return out;
	}
}
