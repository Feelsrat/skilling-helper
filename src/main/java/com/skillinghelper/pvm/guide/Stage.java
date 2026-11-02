package com.skillinghelper.pvm.guide;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.runelite.api.coords.WorldPoint;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * One step of a guide before the fight: get a key, go to the entrance, dig into a mound... The guide shows the
 * first stage whose {@link #when} holds once the loadout is ready.
 */
@Value
@Builder
public class Stage
{
	String title;
	@Singular
	List<String> lines;
	@Builder.Default
	Predicate<GuideState> when = s -> true;
	/**
	 * Where to go for this stage; the step walks there first. Null when it's done where the player stands.
	 */
	Location place;
	/**
	 * Where to go when it depends on the player's progress (e.g. the next door in the Barrows tunnels). Used when
	 * {@link #place} is null; null means stay put.
	 */
	@Builder.Default
	Function<GuideState, Location> goTo = s -> null;
	/**
	 * The way to {@link #goTo} is shown on the ground and by the hint arrow only (Shortest Path doesn't know it).
	 */
	boolean local;
	/**
	 * Tiles to mark on the ground, e.g. the way through the Barrows tunnels or the spot to dig.
	 */
	@Builder.Default
	Function<GuideState, List<WorldPoint>> tiles = s -> List.of();
	/**
	 * A single object to outline, by where it stands (e.g. the one Barrows door to open out of several).
	 */
	@Builder.Default
	Function<GuideState, WorldPoint> objectAt = s -> null;
	/**
	 * Text of the dialogue option to click when it comes up, e.g. "Yeah I'm fearless!".
	 */
	String dialogOption;
	/**
	 * Objects and NPCs to click.
	 */
	@Singular
	List<Target> highlights;
	/**
	 * Inventory items to click or use, e.g. a spade.
	 */
	@Singular("use")
	List<Integer> useItems;
	/**
	 * The player is in the middle of the run (inside a crypt, in the lair): don't send them off to bank.
	 */
	boolean committed;
	/**
	 * Lines worked out from the player's progress, e.g. which Barrows brothers are left.
	 */
	@Builder.Default
	Function<GuideState, List<String>> live = s -> List.of();
	/**
	 * Which of the monsters on screen to attack first, by a fixed kill order (never by what they're doing).
	 */
	@Builder.Default
	Function<GuideState, List<Target>> priority = s -> List.of();
	/**
	 * The wave that's up, for wave minigames; null otherwise.
	 */
	@Builder.Default
	Function<GuideState, WaveCard> wave = s -> null;
}
