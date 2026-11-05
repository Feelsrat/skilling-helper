package com.skillinghelper.diary.data;

import com.skillinghelper.data.Location;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * One part of a task, e.g. "climb down the trapdoor" then "talk to Sedridor". A task's stages are in order, and the
 * guide shows the last one whose condition holds ({@link #inside}, {@link #carrying} or {@link #varbit}), or the
 * first one when none does.
 */
@Value
@Builder
public class DiaryStage
{
	/**
	 * What to do, short, e.g. "Talk to Hans".
	 */
	String title;
	/**
	 * Numbered steps for someone who has never done it: what to click, which option, where to stand.
	 */
	@Singular
	List<String> lines;
	/**
	 * Where to walk to first, or null when it can be done anywhere (or the player is already where it happens).
	 */
	Location at;
	/**
	 * This stage applies while the player is in here (e.g. inside a cave), so walking to {@link #at} is skipped.
	 */
	Zone inside;
	/**
	 * This stage applies while the player carries this item (e.g. the logs they just chopped), or 0.
	 */
	int carrying;
	/**
	 * This stage applies once this varbit is at least {@link #varbitValue}, or 0.
	 */
	int varbit;
	int varbitValue;
	/**
	 * The task's supplies are used up by now (e.g. the gold bar became an amulet), so stop asking for them.
	 */
	boolean started;
	/**
	 * NPC names to outline.
	 */
	@Singular
	Set<String> npcs;
	/**
	 * Object names to outline.
	 */
	@Singular
	Set<String> objects;
	/**
	 * Inventory items to outline, to use or click.
	 */
	@Singular("use")
	Set<Integer> use;
}
