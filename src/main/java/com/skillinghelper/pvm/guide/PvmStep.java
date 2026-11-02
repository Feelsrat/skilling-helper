package com.skillinghelper.pvm.guide;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import com.skillinghelper.engine.ItemHighlight;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * What to do right now, and what the overlays should show for it.
 */
@Value
@Builder
public class PvmStep
{
	public enum Kind
	{
		BUY,
		BANK,
		EQUIP,
		TRAVEL,
		STAGE,
		FIGHT,
		LOOT
	}

	Kind kind;
	String title;
	@Singular
	List<String> lines;
	/**
	 * Where to go, or null when the player is already there.
	 */
	Location destination;
	@Singular
	List<Target> highlights;
	/**
	 * Adds the boss spawned, in their own colour.
	 */
	@Singular("addHighlight")
	List<Target> addHighlights;
	/**
	 * The monster to attack first by the guide's fixed kill order, in its own colour.
	 */
	@Singular("priorityHighlight")
	List<Target> priorityHighlights;
	/**
	 * The wave that's up, shown as a card above the lines; null outside wave minigames.
	 */
	WaveCard wave;
	boolean highlightBank;
	/**
	 * Only point the hint arrow at {@link #destination}; don't ask Shortest Path for a route (it can't path there).
	 */
	boolean localRoute;
	/**
	 * Tiles to mark on the ground, e.g. the way through the Barrows tunnels.
	 */
	@Singular
	List<WorldPoint> tiles;
	/**
	 * Outline the object standing here (and its other half, for double doors).
	 */
	WorldPoint objectAt;
	/**
	 * Dialogue option to outline when it's on screen.
	 */
	String dialogOption;
	/**
	 * Inventory and bank items to outline and why.
	 */
	@Singular
	Map<Integer, ItemHighlight> itemHighlights;
}
