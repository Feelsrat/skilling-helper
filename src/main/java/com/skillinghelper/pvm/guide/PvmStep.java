package com.skillinghelper.pvm.guide;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import com.skillinghelper.engine.ItemHighlight;
import java.util.List;
import java.util.Map;
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
	boolean highlightBank;
	/**
	 * Inventory and bank items to outline and why.
	 */
	@Singular
	Map<Integer, ItemHighlight> itemHighlights;
}
