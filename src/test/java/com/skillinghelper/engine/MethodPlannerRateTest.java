package com.skillinghelper.engine;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MethodPlannerRateTest
{
	private static Method method(int... rates)
	{
		Method.MethodBuilder b = Method.builder().id("test").skill(Skill.MINING).name("Test").type(MethodType.GATHER)
			.location(Location.of("Mine", 3000, 3000, 0, 5)).instruction("Mine").minLevel(30).xpPerAction(10).xpPerHour(20_000);
		for (int i = 0; i + 1 < rates.length; i += 2)
		{
			b.rateAt(rates[i], rates[i + 1]);
		}
		return b.build();
	}

	@Test
	public void flatRateStaysFlat()
	{
		assertEquals(20_000, MethodPlanner.averageXpPerHour(method(), 30, 1_000_000), 1);
	}

	@Test
	public void climbingRateAveragesOverTheClimb()
	{
		Method m = method(60, 50_000);
		long xp = Experience.getXpForLevel(60) - Experience.getXpForLevel(30);
		double average = MethodPlanner.averageXpPerHour(m, 30, xp);
		assertTrue("faster than the starting rate: " + average, average > 20_000);
		assertTrue("slower than the end rate: " + average, average < 50_000);
	}

	@Test
	public void shortClimbUsesTheCurrentRate()
	{
		assertEquals(20_000, MethodPlanner.averageXpPerHour(method(60, 50_000), 30, 10), 1);
	}
}
