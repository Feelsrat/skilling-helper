package com.skillinghelper.engine;

import com.skillinghelper.data.Target;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * What the player should do right now, plus everything the overlays need to show it.
 */
@Value
@Builder
public class GuideStep
{
	StepType type;
	String title;
	@Singular("line")
	List<String> details;
	/**
	 * Where to go, or null when the player is already in the right spot.
	 */
	WorldPoint destination;
	String destinationName;
	@Singular
	List<Target> highlights;
	boolean highlightBank;
	/**
	 * Inventory/bank items to highlight and why.
	 */
	@Singular
	Map<Integer, ItemHighlight> itemHighlights;

	public static GuideStep empty(String title, String detail)
	{
		return GuideStep.builder().type(StepType.DONE).title(title).line(detail).build();
	}

	public List<String> getDetails()
	{
		return details == null ? Collections.emptyList() : details;
	}
}
