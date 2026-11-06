package com.skillinghelper.collection.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/**
 * Every collection log page and slot, from {@code collection-log.tsv} (built from the OSRS Wiki by
 * tools/collection/build_collection_log.py).
 */
public final class ClogData
{
	private static volatile ClogData instance;

	private final List<ClogEntry> entries;
	private final Map<Integer, ClogItem> items;
	private final Map<Integer, List<ClogEntry>> entriesOf;
	private final Map<String, Integer> idsByName;

	private ClogData(List<ClogEntry> entries)
	{
		this.entries = Collections.unmodifiableList(entries);
		Map<Integer, ClogItem> items = new LinkedHashMap<>();
		Map<Integer, List<ClogEntry>> entriesOf = new HashMap<>();
		Map<String, Integer> names = new HashMap<>();
		for (ClogEntry e : entries)
		{
			for (ClogItem i : e.getItems())
			{
				items.putIfAbsent(i.getId(), i);
				entriesOf.computeIfAbsent(i.getId(), k -> new ArrayList<>()).add(e);
				names.putIfAbsent(i.getName().toLowerCase(), i.getId());
			}
		}
		this.items = Collections.unmodifiableMap(items);
		this.entriesOf = entriesOf;
		this.idsByName = names;
	}

	public static ClogData get()
	{
		ClogData d = instance;
		if (d == null)
		{
			synchronized (ClogData.class)
			{
				if (instance == null)
				{
					try (InputStream in = ClogData.class.getResourceAsStream("/com/skillinghelper/collection/collection-log.tsv"))
					{
						if (in == null)
						{
							throw new IllegalStateException("collection-log.tsv is missing");
						}
						instance = parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
					}
					catch (IOException e)
					{
						throw new UncheckedIOException(e);
					}
				}
				d = instance;
			}
		}
		return d;
	}

	static ClogData parse(BufferedReader reader) throws IOException
	{
		List<ClogEntry> entries = new ArrayList<>();
		String[] head = null;
		List<ClogItem> items = null;
		String line;
		while ((line = reader.readLine()) != null)
		{
			if (line.isEmpty() || line.startsWith("#"))
			{
				continue;
			}
			String[] f = line.split("\t", -1);
			if ("E".equals(f[0]))
			{
				if (head != null)
				{
					entries.add(entry(head, items));
				}
				head = f;
				items = new ArrayList<>();
			}
			else if ("I".equals(f[0]) && items != null)
			{
				items.add(new ClogItem(Integer.parseInt(f[1]), f[2], Double.parseDouble(f[3])));
			}
		}
		if (head != null)
		{
			entries.add(entry(head, items));
		}
		return new ClogData(entries);
	}

	private static ClogEntry entry(String[] f, List<ClogItem> items)
	{
		WorldPoint at = null;
		if (!"-".equals(f[4]))
		{
			String[] p = f[4].split(" ");
			at = new WorldPoint(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
		}
		String intro = "-".equals(f[6]) ? "" : f[6];
		return new ClogEntry(ClogTab.of(f[1]), f[2], f[3], at, Integer.parseInt(f[5]), intro,
			Collections.unmodifiableList(items));
	}

	public List<ClogEntry> entries()
	{
		return entries;
	}

	public ClogEntry entry(String name)
	{
		for (ClogEntry e : entries)
		{
			if (e.getName().equals(name))
			{
				return e;
			}
		}
		return null;
	}

	/**
	 * Every slot once (an item on several pages fills all of them), keyed by item id.
	 */
	public Map<Integer, ClogItem> items()
	{
		return items;
	}

	public boolean isSlot(int itemId)
	{
		return items.containsKey(itemId);
	}

	/**
	 * The pages a slot is on, e.g. a dragon pickaxe is on Chaos Elemental, Callisto, KBD and more.
	 */
	public List<ClogEntry> entriesOf(int itemId)
	{
		return entriesOf.getOrDefault(itemId, List.of());
	}

	/**
	 * The slot with this name (as the game writes it in its "New item added" message), or -1.
	 */
	public int idByName(String name)
	{
		return idsByName.getOrDefault(name.toLowerCase(), -1);
	}
}
