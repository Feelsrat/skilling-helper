package com.skillinghelper.engine;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.Stop;

/**
 * Bird house and farming runs: which stop to visit next, and what to say once every stop is done.
 */
final class Runs
{
	private Runs()
	{
	}

	/**
	 * The stop to visit next: the one the player is standing at if it needs a visit, otherwise the first one in run
	 * order that does. Null when every stop is done.
	 */
	static Stop nextStop(Method m, PlayerState s)
	{
		Stop first = null;
		for (Stop stop : m.getStops())
		{
			if (isDone(stop, s))
			{
				continue;
			}
			if (stop.getLocation().contains(s.getLocation()))
			{
				return stop;
			}
			if (first == null)
			{
				first = stop;
			}
		}
		return first;
	}

	/**
	 * "Stop 2 of 4: ..." for the step text.
	 */
	static String progress(Method m, PlayerState s, Stop stop)
	{
		return String.format("Stop %d of %d: %s (%d still to do this run)", m.getStops().indexOf(stop) + 1,
			m.getStops().size(), stop.getLocation().getName(), dueStops(m, s));
	}

	private static int dueStops(Method m, PlayerState s)
	{
		int due = 0;
		for (Stop stop : m.getStops())
		{
			if (!isDone(stop, s))
			{
				due++;
			}
		}
		return due;
	}

	/**
	 * A stop is done for this run while it's still growing (it has a wait).
	 */
	private static boolean isDone(Stop stop, PlayerState s)
	{
		return s.getStopWaits().containsKey(stop.getId());
	}

	static GuideStep waitStep(Method m, PlayerState s)
	{
		int soonest = Integer.MAX_VALUE;
		Stop next = null;
		for (Stop stop : m.getStops())
		{
			int wait = s.getStopWaits().getOrDefault(stop.getId(), 0);
			if (wait < soonest)
			{
				soonest = wait;
				next = stop;
			}
		}
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.type(StepType.WAIT)
			.title("Run done: next one ready in ~" + soonest + " min")
			.line("Every stop on this run is done. Train another skill meanwhile and come back when it's ready; "
				+ "this guide picks up again on its own.");
		if (next != null)
		{
			b.line("First up then: " + next.getLocation().getName() + ".");
		}
		return b.build();
	}
}
