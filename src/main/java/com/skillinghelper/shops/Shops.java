package com.skillinghelper.shops;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every NPC shop the guides know, from {@code shops.tsv} (built from the OSRS Wiki by tools/shops/build_shops.py).
 * Only shops without an entry requirement are listed, and only items they sell for less than the GE did when the
 * list was built.
 */
public final class Shops
{
	private static final String RESOURCE = "/com/skillinghelper/data/shops.tsv";
	private static final int COLUMNS = 10;
	/**
	 * Tiles from the middle of the shop that count as being there.
	 */
	private static final int RADIUS = 5;
	private static volatile Map<Integer, List<ShopOffer>> byItem;

	private Shops()
	{
	}

	/**
	 * The shops that sell this item for coins, or an empty list.
	 */
	public static List<ShopOffer> selling(int itemId)
	{
		return all().getOrDefault(itemId, Collections.emptyList());
	}

	/**
	 * Every shopkeeper to click, so the scene trackers know their names before a step asks for them.
	 */
	public static List<Target> targets()
	{
		Set<Shop> shops = new LinkedHashSet<>();
		all().values().forEach(offers -> offers.forEach(o -> shops.add(o.getShop())));
		List<Target> targets = new ArrayList<>();
		for (Shop shop : shops)
		{
			targets.add(shop.target());
		}
		return targets;
	}

	static Map<Integer, List<ShopOffer>> all()
	{
		Map<Integer, List<ShopOffer>> loaded = byItem;
		if (loaded == null)
		{
			synchronized (Shops.class)
			{
				if (byItem == null)
				{
					byItem = load();
				}
				loaded = byItem;
			}
		}
		return loaded;
	}

	private static Map<Integer, List<ShopOffer>> load()
	{
		try (InputStream in = Shops.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IllegalStateException("Missing " + RESOURCE);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			Map<String, Shop> shops = new HashMap<>();
			Map<Integer, List<ShopOffer>> offers = new LinkedHashMap<>();
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isEmpty() || line.startsWith("#"))
				{
					continue;
				}
				String[] c = line.split("\t", -1);
				if (c.length != COLUMNS)
				{
					throw new IllegalStateException("Bad shop line: " + line);
				}
				Shop shop = shops.computeIfAbsent(c[0], name -> shop(name, c[1], c[2], c[3]));
				int itemId = Integer.parseInt(c[4]);
				offers.computeIfAbsent(itemId, id -> new ArrayList<>()).add(new ShopOffer(shop, itemId, c[5],
					Integer.parseInt(c[6]), Integer.parseInt(c[7]), Integer.parseInt(c[8]), Integer.parseInt(c[9])));
			}
			offers.replaceAll((id, list) -> Collections.unmodifiableList(list));
			return Collections.unmodifiableMap(offers);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	private static Shop shop(String name, String npcs, String point, String members)
	{
		String[] xyz = point.split(",");
		Location where = Location.of(name, Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]),
			Integer.parseInt(xyz[2]), RADIUS);
		return new Shop(name, List.of(npcs.split(",")), where, "1".equals(members));
	}
}
