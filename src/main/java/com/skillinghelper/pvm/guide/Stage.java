package com.skillinghelper.pvm.guide;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
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
}
