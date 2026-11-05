package com.skillinghelper.planner;

import com.skillinghelper.planner.api.StepKind;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

/**
 * What the planner panel shows. Compared by value so the panel only redraws when something changed.
 */
@Value
@Builder
public class PlannerView
{
	boolean loggedIn;
	@Builder.Default
	List<Row> rows = Collections.emptyList();
	/**
	 * Steps in the whole plan, of which {@link #rows} are the first.
	 */
	int planSize;
	double totalHours;
	@Builder.Default
	Map<StepKind, int[]> progress = Collections.emptyMap();
	@Builder.Default
	Set<StepKind> hiddenKinds = Collections.emptySet();
	int skipped;
	@Builder.Default
	List<String> unreachable = Collections.emptyList();
	boolean wildernessAllowed;
	@Builder.Default
	List<String> providers = Collections.emptyList();

	@Value
	public static class Row
	{
		String id;
		StepKind kind;
		String title;
		String why;
		String reason;
		double hours;
		List<String> howTo;
		/**
		 * Needs nobody can check, shown with a "?".
		 */
		List<String> notes;
		String openIn;
		boolean canOpen;
		String wikiPage;
		boolean wilderness;
	}

	/**
	 * Progress arrays compare by content, so the view compares by value.
	 */
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof PlannerView))
		{
			return false;
		}
		PlannerView v = (PlannerView) o;
		return loggedIn == v.loggedIn && planSize == v.planSize && Double.compare(totalHours, v.totalHours) == 0
			&& skipped == v.skipped && unreachable.equals(v.unreachable) && wildernessAllowed == v.wildernessAllowed
			&& rows.equals(v.rows) && hiddenKinds.equals(v.hiddenKinds) && providers.equals(v.providers)
			&& progressKey(progress).equals(progressKey(v.progress));
	}

	@Override
	public int hashCode()
	{
		return rows.hashCode() * 31 + progressKey(progress).hashCode();
	}

	private static String progressKey(Map<StepKind, int[]> progress)
	{
		StringBuilder sb = new StringBuilder();
		progress.forEach((k, v) -> sb.append(k).append(v[0]).append('/').append(v[1]).append(';'));
		return sb.toString();
	}
}
