package com.skillinghelper.quests;

import com.skillinghelper.quests.data.Route;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Everything the side panel shows, built on the client thread. Compared by value so the panel only redraws when
 * something it shows changed.
 */
@Value
@Builder
public class QuestView
{
	@Value
	public static class Row
	{
		String key;
		int number;
		String name;
		QuestPlanner.Status status;
		List<String> missing;
		boolean wilderness;
		boolean current;
		/**
		 * Short "Novice, short" style detail, or empty.
		 */
		String detail;
	}

	boolean loggedIn;
	Route route;
	boolean routeAuto;
	boolean questHelper;
	int done;
	int total;
	int questPoints;
	boolean running;
	QuestStep step;
	/**
	 * The next rows on the route that aren't done, in order.
	 */
	List<Row> upcoming;
	boolean wildernessAllowed;
}
