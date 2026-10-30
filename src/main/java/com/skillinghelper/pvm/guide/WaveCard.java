package com.skillinghelper.pvm.guide;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * The wave that's up in a wave minigame, laid out for the panel: one row per monster type, the wiki's tip for this
 * wave, and reminders about the player's own supplies. Only the current wave, never the next one.
 */
@Value
@Builder
public class WaveCard
{
	/**
	 * The wave number, or -1 when it isn't known yet.
	 */
	int wave;
	int total;
	@Singular
	List<Row> rows;
	/**
	 * The wiki's tip for this wave, or null.
	 */
	String tip;
	/**
	 * When to drink, from the player's own stats.
	 */
	@Singular
	List<String> alerts;
	/**
	 * Doses left, e.g. "Prayer 31 · Restore 8 · Brew 20".
	 */
	String supplies;

	@Value
	public static class Row
	{
		int count;
		String name;
		int level;
		/**
		 * How it attacks: "Melee", "Ranged", "Magic" or a mix.
		 */
		String style;
		String note;
		/**
		 * First in the kill order of the monsters on screen right now.
		 */
		boolean first;
	}
}
