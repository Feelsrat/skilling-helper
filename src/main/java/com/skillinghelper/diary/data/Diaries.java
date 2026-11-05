package com.skillinghelper.diary.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Every diary task with a guide, by region and tier, and the rewards of every tier.
 */
public final class Diaries
{
	private static final Map<DiaryRegion, List<DiaryTask>> TASKS = new EnumMap<>(DiaryRegion.class);
	private static Map<String, DiaryReward> rewards;

	static
	{
		TASKS.put(DiaryRegion.LUMBRIDGE, LumbridgeDiary.tasks());
		TASKS.put(DiaryRegion.VARROCK, VarrockDiary.tasks());
		TASKS.put(DiaryRegion.FALADOR, FaladorDiary.tasks());
		TASKS.put(DiaryRegion.KARAMJA, KaramjaDiary.tasks());
		TASKS.put(DiaryRegion.ARDOUGNE, ArdougneDiary.tasks());
		TASKS.put(DiaryRegion.DESERT, DesertDiary.tasks());
		TASKS.put(DiaryRegion.KANDARIN, KandarinDiary.tasks());
		TASKS.put(DiaryRegion.MORYTANIA, MorytaniaDiary.tasks());
		TASKS.put(DiaryRegion.FREMENNIK, FremennikDiary.tasks());
		TASKS.put(DiaryRegion.KOUREND, KourendDiary.tasks());
		TASKS.put(DiaryRegion.WESTERN, WesternDiary.tasks());
	}

	private Diaries()
	{
	}

	/**
	 * Whether this region has step-by-step guides yet.
	 */
	public static boolean guided(DiaryRegion region)
	{
		return TASKS.containsKey(region);
	}

	public static List<DiaryTask> tasks(DiaryRegion region)
	{
		return TASKS.getOrDefault(region, Collections.emptyList());
	}

	public static List<DiaryTask> tasks(DiaryRegion region, DiaryTier tier)
	{
		List<DiaryTask> out = new ArrayList<>();
		for (DiaryTask t : tasks(region))
		{
			if (t.getTier() == tier)
			{
				out.add(t);
			}
		}
		return out;
	}

	public static List<DiaryTask> all()
	{
		List<DiaryTask> out = new ArrayList<>();
		TASKS.values().forEach(out::addAll);
		return out;
	}

	/**
	 * The tier's reward as the wiki lists it, or null if it's missing from the data.
	 */
	public static synchronized DiaryReward reward(DiaryRegion region, DiaryTier tier)
	{
		if (rewards == null)
		{
			rewards = loadRewards();
		}
		return rewards.get(region.name() + "." + tier.name());
	}

	private static Map<String, DiaryReward> loadRewards()
	{
		Map<String, DiaryReward> out = new java.util.HashMap<>();
		try (InputStream in = Diaries.class.getResourceAsStream("/com/skillinghelper/diary/diary-rewards.tsv"))
		{
			if (in == null)
			{
				return out;
			}
			BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = r.readLine()) != null)
			{
				if (line.isEmpty() || line.startsWith("#"))
				{
					continue;
				}
				String[] f = line.split("\t");
				if (f.length < 3)
				{
					continue;
				}
				out.put(f[0] + "." + f[1], new DiaryReward(f[2],
					Collections.unmodifiableList(Arrays.asList(Arrays.copyOfRange(f, 3, f.length)))));
			}
		}
		catch (IOException e)
		{
			// Rewards are only shown, so carry on without them
		}
		return out;
	}
}
