package com.skillinghelper;

import static org.junit.Assert.assertEquals;
import net.runelite.api.Skill;
import org.junit.Test;

public class SessionXpTrackerTest
{
	@Test
	public void measuresOnlyTrainingTime()
	{
		SessionXpTracker t = new SessionXpTracker();
		int xp = 1_000;
		t.tick(Skill.FISHING, xp);
		assertEquals(0, t.xpPerHour(Skill.FISHING));
		// 50 xp every 10 ticks (6 seconds) = 30k xp/hr
		for (int i = 1; i <= 50; i++)
		{
			xp += i % 10 == 0 ? 50 : 0;
			t.tick(Skill.FISHING, xp);
		}
		assertEquals(-1, t.xpPerHour(Skill.FISHING));
		for (int i = 51; i <= 600; i++)
		{
			xp += i % 10 == 0 ? 50 : 0;
			t.tick(Skill.FISHING, xp);
		}
		int rate = t.xpPerHour(Skill.FISHING);
		// A long break after the last drop only counts its first minute
		for (int i = 0; i < 5_000; i++)
		{
			t.tick(Skill.FISHING, xp);
		}
		int afterBreak = t.xpPerHour(Skill.FISHING);
		assertEquals(30_000, rate, 1_000);
		// At most a minute of idle time is added to ten minutes of training
		org.junit.Assert.assertTrue(afterBreak >= rate * 590 / 690.0 - 500);
	}

	@Test
	public void holdsTheShownRateBetweenRefreshes()
	{
		SessionXpTracker t = new SessionXpTracker();
		int xp = 0;
		t.tick(Skill.FISHING, xp);
		for (int i = 1; i <= 200; i++)
		{
			xp += i % 10 == 0 ? 50 : 0;
			t.tick(Skill.FISHING, xp);
		}
		int shown = t.xpPerHour(Skill.FISHING);
		// A slightly faster stretch doesn't change the number until a minute of training has passed
		for (int i = 1; i <= 50; i++)
		{
			xp += i % 8 == 0 ? 50 : 0;
			t.tick(Skill.FISHING, xp);
			assertEquals(shown, t.xpPerHour(Skill.FISHING));
		}
		for (int i = 1; i <= 60; i++)
		{
			xp += i % 8 == 0 ? 50 : 0;
			t.tick(Skill.FISHING, xp);
		}
		org.junit.Assert.assertNotEquals(shown, t.xpPerHour(Skill.FISHING));
		// A big jump in pace shows straight away
		int before = t.xpPerHour(Skill.FISHING);
		for (int i = 0; i < 20; i++)
		{
			xp += 400;
			t.tick(Skill.FISHING, xp);
		}
		org.junit.Assert.assertTrue(t.xpPerHour(Skill.FISHING) > before * 1.15);
	}

	@Test
	public void ignoresLampsAndSkillSwitches()
	{
		SessionXpTracker t = new SessionXpTracker();
		t.tick(Skill.MINING, 0);
		t.tick(Skill.MINING, 100_000);
		assertEquals(0, t.xpPerHour(Skill.MINING));
		t.tick(Skill.WOODCUTTING, 500);
		t.tick(Skill.MINING, 200_000);
		assertEquals(0, t.xpPerHour(Skill.MINING));
		t.reset();
		assertEquals(0, t.xpPerHour(Skill.WOODCUTTING));
	}
}
