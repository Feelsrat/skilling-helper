package com.skillinghelper.engine;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Stop;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class RunsTest
{
	private static final Stop A = Stop.of("a", "Spot A", 3200, 3200, null);
	private static final Stop B = Stop.of("b", "Spot B", 3300, 3300, null);
	private static final Stop C = Stop.of("c", "Spot C", 3400, 3400, null);
	private static final Method RUN = Method.builder()
		.id("run").skill(Skill.HUNTER).name("Run").type(MethodType.ACTIVITY)
		.location(Location.of("Island", 3300, 3300, 200))
		.stop(A).stop(B).stop(C)
		.instruction("Check each spot")
		.build();

	private static PlayerState at(WorldPoint where, Map<String, Integer> waits)
	{
		return PlayerState.builder().location(where).stopWaits(waits).build();
	}

	@Test
	public void startsWithTheFirstStopInRunOrder()
	{
		assertEquals(A, Runs.nextStop(RUN, at(new WorldPoint(1000, 1000, 0), Map.of())));
	}

	@Test
	public void prefersTheStopThePlayerIsStandingAt()
	{
		assertEquals(C, Runs.nextStop(RUN, at(C.getLocation().getPoint(), Map.of())));
	}

	@Test
	public void skipsStopsThatAreDone()
	{
		assertEquals(B, Runs.nextStop(RUN, at(A.getLocation().getPoint(), Map.of("a", 30))));
	}

	@Test
	public void noStopWhenEveryStopIsDone()
	{
		assertNull(Runs.nextStop(RUN, at(null, Map.of("a", 30, "b", 10, "c", 20))));
	}

	@Test
	public void progressCountsTheStopsLeft()
	{
		PlayerState s = at(null, Map.of("a", 30));
		assertEquals("Stop 2 of 3: Spot B (2 still to do this run)", Runs.progress(RUN, s, B));
	}

	@Test
	public void waitStepNamesTheSoonestStop()
	{
		GuideStep step = Runs.waitStep(RUN, at(null, Map.of("a", 30, "b", 10, "c", 20)));
		assertEquals(StepType.WAIT, step.getType());
		assertEquals("Run done: next one ready in ~10 min", step.getTitle());
		assertEquals("First up then: Spot B.", step.getDetails().get(1));
	}
}
