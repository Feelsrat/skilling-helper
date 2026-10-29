package com.skillinghelper.pvm.guide;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import java.util.List;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * The step-by-step guide for one piece of PvM content: the loadout to bring, the steps to get to the fight, and what
 * to highlight once there.
 */
@Value
@Builder
public class PvmGuide
{
	/**
	 * The {@link com.skillinghelper.pvm.data.PvmContent} id this guide is for.
	 */
	String contentId;
	/**
	 * Equipment slots, each with its gear best first.
	 */
	List<GearSlot> gear;
	@Singular
	List<Supply> supplies;
	/**
	 * Steps before the fight, in order.
	 */
	@Singular
	List<Stage> stages;
	/**
	 * Where the fight is (or the entrance to it), once every stage is done.
	 */
	Location place;
	/**
	 * What to click there, e.g. the gate or the boat.
	 */
	@Singular("enter")
	List<Target> entrance;
	/**
	 * Lines for the last step, at the place.
	 */
	@Singular("arrival")
	List<String> arrival;
	Fight fight;
	Profit profit;
	/**
	 * Most one piece of gear the guide suggests buying should cost.
	 */
	int budget;
	/**
	 * Whether the player is inside the content's area, past the entrance (in the lair, in the crypts). Never sent to
	 * the bank from there.
	 */
	@Builder.Default
	Predicate<GuideState> inside = s -> false;
	/**
	 * Varbits the guide's stages read, e.g. which moons are dead.
	 */
	@Singular("watch")
	List<Integer> watched;
	/**
	 * NPC names the stages check for besides the ones they highlight.
	 */
	@Singular("track")
	List<String> tracked;

	public List<Gear> allGear()
	{
		return gear.stream().flatMap(g -> g.getOptions().stream()).collect(java.util.stream.Collectors.toList());
	}
}
