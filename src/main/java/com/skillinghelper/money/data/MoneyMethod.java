package com.skillinghelper.money.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Wilderness;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * One OSRS Wiki money making guide, e.g. "Collecting snape grass". Built from the wiki by
 * tools/money/build_money_methods.py.
 */
@Value
@Builder
public class MoneyMethod
{
	/**
	 * Wiki page title, e.g. "Money making guide/Collecting snape grass". Also the method's id.
	 */
	String page;
	String name;
	/**
	 * The wiki's category: Skilling, Combat/Mid, Collecting, Processing, Recurring...
	 */
	String category;
	/**
	 * The skill a skilling method trains, or null.
	 */
	String skillCategory;
	boolean members;
	/**
	 * Low, Moderate or High attention, or null when the wiki doesn't say.
	 */
	String intensity;
	/**
	 * For recurring methods (herb runs, daily shop stock): hours one run takes, else 0.
	 */
	double runHours;
	/**
	 * For recurring methods: hours between runs, else 0.
	 */
	double everyHours;
	/**
	 * Profit per hour (per run for recurring methods) at the wiki's prices when the list was built.
	 */
	long wikiProfit;
	/**
	 * Skill levels the wiki lists as needed. Recommended and optional levels are left out.
	 */
	Map<Skill, Integer> skills;
	/**
	 * Quests the wiki lists, by name.
	 */
	List<String> quests;
	/**
	 * Other requirements as text (diaries, items, unlocks), which the plugin can't check.
	 */
	String other;
	/**
	 * Where it's done, as the wiki words it.
	 */
	String locationName;
	/**
	 * Spot to walk to, from the wiki map of the location, or null.
	 */
	Location spot;
	/**
	 * Shopkeeper to click for shop-buying methods, or null.
	 */
	String npc;
	List<MoneyItem> inputs;
	List<MoneyItem> outputs;
	/**
	 * The wiki's "Details" text: how to do it.
	 */
	String details;

	/**
	 * Whether the method is done in the Wilderness: its spot is there, or the wiki's location names it (Revenant
	 * Caves, Lava Maze...), or it runs through the Abyss, whose way in is in the Wilderness.
	 */
	public boolean isWilderness()
	{
		if ((spot != null && Wilderness.contains(spot)) || page.contains("through the Abyss"))
		{
			return true;
		}
		if (spot != null || SAFE_PAGES.contains(page))
		{
			// The wiki's spot is outside: other places named in the text are alternatives
			return false;
		}
		String where = (locationName == null ? "" : locationName.toLowerCase(Locale.ROOT));
		String title = page.toLowerCase(Locale.ROOT);
		return (where.contains("wilderness") && !where.contains("except the wilderness"))
			|| where.contains("revenant") || where.contains("lava maze") || where.contains("hunter's end")
			|| where.contains("king black dragon") || where.contains("earth obelisk") || where.contains("fire obelisk")
			|| title.contains("wilderness") || title.contains("revenant")
			|| title.contains("callisto") || title.contains("chaos elemental");
	}

	/**
	 * Pages whose location mentions the Wilderness but that are done somewhere else.
	 */
	private static final Set<String> SAFE_PAGES = Set.of("Money making guide/Killing Bryophyta (free-to-play)");

	public boolean isRecurring()
	{
		return runHours > 0;
	}

	public String wikiPath()
	{
		return page.replace(' ', '_');
	}

	/**
	 * The category's first word: "Combat/High" is "Combat".
	 */
	public String mainCategory()
	{
		int slash = category == null ? -1 : category.indexOf('/');
		return category == null ? "Other" : slash < 0 ? category : category.substring(0, slash);
	}
}
