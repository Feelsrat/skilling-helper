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
@Builder(toBuilder = true)
public class GuideStep
{
	StepType type;
	String title;
	@Singular("line")
	List<String> details;
	/**
	 * Where to go, or null when the player is already in the right spot.
	 */
	Route route;
	@Singular
	List<Target> highlights;
	boolean highlightBank;
	/**
	 * Inventory/bank items to highlight and why.
	 */
	@Singular
	Map<Integer, ItemHighlight> itemHighlights;
	/**
	 * In the bank: how many of each item to take out right now, 0 meaning all of them. Used to put the matching
	 * Withdraw option on left-click.
	 */
	@Singular
	Map<Integer, Integer> withdrawAmounts;
	/**
	 * In the bank: how many of each carried item to put back, 0 meaning all of them.
	 */
	@Singular
	Map<Integer, Integer> depositAmounts;

	public static GuideStep empty(String title, String detail)
	{
		return GuideStep.builder().type(StepType.DONE).title(title).line(detail).build();
	}

	/**
	 * Where the arrows point, or null when the player is already in the right spot.
	 */
	public WorldPoint getDestination()
	{
		return route == null ? null : route.getPoint();
	}

	public String getDestinationName()
	{
		return route == null ? null : route.getStopName();
	}

	public List<String> getDetails()
	{
		return details == null ? Collections.emptyList() : details;
	}
}
