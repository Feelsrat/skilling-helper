package com.skillinghelper.clue.data;

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
import java.util.function.Consumer;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * The wiki data bundled with the plugin (built by tools/clue/build_clue_data.py): every reward a casket can give,
 * every STASH unit and every drop source of each tier. Loaded once, on first use.
 */
public final class ClueData
{
	private static final String DIR = "/com/skillinghelper/clue/";

	/**
	 * One line of a reward casket's drop table.
	 */
	@Value
	public static class Reward
	{
		ClueTier tier;
		/**
		 * The wiki's table, e.g. "Easy clue uniques" or "Standard table / Runes".
		 */
		String table;
		String name;
		/**
		 * Item id, or 0 when the wiki has none (e.g. emotes, or items with many versions).
		 */
		int id;
		String quantity;
		/**
		 * E.g. "1/1404", per loot roll.
		 */
		String rarity;
		double chance;

		public boolean isUnique()
		{
			return table.contains("uniques");
		}

		public boolean isMimicOnly()
		{
			return table.contains("Mimic");
		}

		public boolean isTertiary()
		{
			return table.startsWith("Tertiary");
		}

		/**
		 * The average amount, e.g. 10 for "5-15" (noted or not).
		 */
		public double averageQuantity()
		{
			String q = quantity.replace("(noted)", "").replace(",", "").trim();
			try
			{
				int dash = q.indexOf('-');
				if (dash > 0)
				{
					return (Double.parseDouble(q.substring(0, dash).trim()) + Double.parseDouble(q.substring(dash + 1).trim())) / 2;
				}
				return Double.parseDouble(q);
			}
			catch (NumberFormatException e)
			{
				return 1;
			}
		}
	}

	/**
	 * How many rewards a casket of a tier gives, and the wiki's summary of its odds.
	 */
	@Value
	public static class Casket
	{
		int minRolls;
		int maxRolls;
		String summary;

		public double averageRolls()
		{
			return (minRolls + maxRolls) / 2.0;
		}
	}

	/**
	 * A STASH unit: where it is and the emote clue items it holds.
	 */
	@Value
	public static class Stash
	{
		ClueTier tier;
		WorldPoint point;
		String name;
		String where;
		List<String> items;
		/**
		 * Ids matching {@link #items}; 0 when the item has several versions or the wiki has no id.
		 */
		List<Integer> ids;

		public String key()
		{
			return tier.name() + "@" + point.getX() + "," + point.getY() + "," + point.getPlane();
		}
	}

	/**
	 * A monster, activity or reward that can give a clue of a tier.
	 */
	@Value
	public static class Drop
	{
		ClueTier tier;
		String source;
		String rarity;
		double chance;
		/**
		 * Combat level(s) for monsters, the skill level for skilling sources, or empty.
		 */
		String level;
		/**
		 * The wiki's drop type: combat, thieving, hunter, reward, sailing...
		 */
		String type;
		/**
		 * Only found in the Wilderness.
		 */
		boolean wilderness;
	}

	private static Map<ClueTier, List<Reward>> rewards;
	private static Map<ClueTier, Casket> caskets;
	private static Map<ClueTier, List<Stash>> stashes;
	private static Map<ClueTier, List<Drop>> drops;

	private ClueData()
	{
	}

	public static synchronized List<Reward> rewards(ClueTier tier)
	{
		loadRewards();
		return rewards.getOrDefault(tier, Collections.emptyList());
	}

	public static synchronized Casket casket(ClueTier tier)
	{
		loadRewards();
		return caskets.get(tier);
	}

	public static synchronized List<Stash> stashes(ClueTier tier)
	{
		if (stashes == null)
		{
			stashes = new EnumMap<>(ClueTier.class);
			read("clue-stash.tsv", f ->
			{
				ClueTier t = ClueTier.fromKey(f[0]);
				if (t == null || f.length < 8)
				{
					return;
				}
				List<String> items = List.of(f[6].split("\\|"));
				List<Integer> ids = new ArrayList<>();
				for (String id : f[7].split("\\|"))
				{
					ids.add(parse(id));
				}
				stashes.computeIfAbsent(t, k -> new ArrayList<>()).add(new Stash(t,
					new WorldPoint(parse(f[1]), parse(f[2]), parse(f[3])), f[4], f[5], items, ids));
			});
		}
		return stashes.getOrDefault(tier, Collections.emptyList());
	}

	/**
	 * Every source of the tier, best rate first.
	 */
	public static synchronized List<Drop> drops(ClueTier tier)
	{
		if (drops == null)
		{
			drops = new EnumMap<>(ClueTier.class);
			read("clue-drops.tsv", f ->
			{
				ClueTier t = ClueTier.fromKey(f[0]);
				if (t == null || f.length < 7)
				{
					return;
				}
				drops.computeIfAbsent(t, k -> new ArrayList<>()).add(new Drop(t, f[1], f[2],
					Double.parseDouble(f[3]), f[4], f[5], "1".equals(f[6])));
			});
			drops.values().forEach(l -> l.sort((a, b) -> Double.compare(b.getChance(), a.getChance())));
		}
		return drops.getOrDefault(tier, Collections.emptyList());
	}

	private static void loadRewards()
	{
		if (rewards != null)
		{
			return;
		}
		rewards = new EnumMap<>(ClueTier.class);
		caskets = new EnumMap<>(ClueTier.class);
		read("clue-rewards.tsv", f ->
		{
			if (f[0].startsWith("#"))
			{
				ClueTier tier = ClueTier.fromKey(f[0].substring(1));
				if (tier != null && f.length >= 4)
				{
					caskets.put(tier, new Casket(parse(f[1]), parse(f[2]), f[3]));
				}
				return;
			}
			ClueTier tier = ClueTier.fromKey(f[0]);
			if (tier == null || f.length < 7)
			{
				return;
			}
			rewards.computeIfAbsent(tier, k -> new ArrayList<>()).add(new Reward(tier, f[1], f[2], parse(f[3]), f[4],
				f[5], Double.parseDouble(f[6])));
		});
	}

	private static int parse(String s)
	{
		try
		{
			return Integer.parseInt(s.trim());
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	private static void read(String file, Consumer<String[]> row)
	{
		try (InputStream in = ClueData.class.getResourceAsStream(DIR + file))
		{
			if (in == null)
			{
				return;
			}
			BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = r.readLine()) != null)
			{
				if (!line.isEmpty())
				{
					row.accept(line.split("\t", -1));
				}
			}
		}
		catch (IOException e)
		{
			// The data is only shown, so carry on without it
		}
	}
}
