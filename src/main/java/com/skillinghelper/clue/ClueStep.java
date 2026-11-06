package com.skillinghelper.clue;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * The clue step the player is on, as read from RuneLite's Clue Scroll plugin, in a shape the guide can turn into
 * beginner steps: where to go, what to bring and wear, and what to click there. Plain data so the guide can be
 * tested without the client.
 */
@Value
@Builder
public class ClueStep
{
	public enum Type
	{
		CRYPTIC,
		ANAGRAM,
		CIPHER,
		COORDINATE,
		MAP,
		EMOTE,
		MUSIC,
		FAIRY_RING,
		HOT_COLD,
		CHALLENGE,
		FALO,
		THREE_STEP,
		OTHER
	}

	/**
	 * An item the step needs: whether it's already carried or worn, else a bank item id that fulfils it (0 = none in
	 * the bank).
	 */
	@Value
	public static class Need
	{
		String name;
		boolean have;
		int bankId;
	}

	Type type;
	/**
	 * The clue's own text, e.g. the anagram or the cryptic riddle.
	 */
	String text;
	/**
	 * What the clue means, e.g. "Talk to Hans in Lumbridge Castle courtyard.", or null.
	 */
	String solution;
	/**
	 * Answer to the challenge question the NPC asks, or null.
	 */
	String answer;
	/**
	 * Where to go: the dig tile, the NPC's spot, the emote spot. Null when unknown (e.g. hot/cold before narrowing).
	 */
	WorldPoint location;
	/**
	 * All the spots it can still be (hot/cold, three-step cryptic), nearest first.
	 */
	@Singular
	List<WorldPoint> spots;
	@Singular
	Set<String> npcs;
	@Singular
	Set<String> objects;
	boolean spade;
	boolean light;
	/**
	 * What attacks you first, e.g. "Zamorak wizard", or null.
	 */
	String enemy;
	@Singular
	List<String> emotes;
	@Singular
	List<Need> items;
	/**
	 * The STASH unit for this emote step, or null.
	 */
	String stash;
	String song;
}
