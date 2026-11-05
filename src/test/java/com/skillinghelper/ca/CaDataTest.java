package com.skillinghelper.ca;

import com.skillinghelper.ca.data.CaMonster;
import com.skillinghelper.ca.data.CaMonsters;
import com.skillinghelper.ca.data.CaReward;
import com.skillinghelper.ca.data.CaTask;
import com.skillinghelper.ca.data.CaTasks;
import com.skillinghelper.ca.data.CaTier;
import com.skillinghelper.data.Wilderness;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CaDataTest
{
	@Test
	public void everyTaskLoadsWithAUniqueIdAndADoneFlag()
	{
		Set<Integer> ids = new HashSet<>();
		assertTrue(CaTasks.all().size() > 600);
		for (CaTask t : CaTasks.all())
		{
			assertTrue("duplicate id " + t.getId(), ids.add(t.getId()));
			assertTrue("no varp for task " + t.getId() + ": add CA_TASK_COMPLETED_" + t.getId() / 32
				+ " to CaTask.COMPLETED", t.varp() > 0);
			assertFalse(t.getName().isEmpty());
			assertFalse(t.getText().isEmpty());
		}
	}

	@Test
	public void everyBossHasDataSoTheGuideCanTakeYouThere()
	{
		for (CaTask t : CaTasks.all())
		{
			CaMonster m = CaMonsters.get(t.getMonster());
			assertNotNull("No CaMonsters entry for \"" + t.getMonster() + "\" (task " + t.getName() + ")", m);
			assertTrue(m.getName() + " has no level", m.getLevel() > 0);
		}
		for (CaMonster m : CaMonsters.all())
		{
			assertFalse(m.getName() + " has no way in", m.getEnter().isEmpty() && m.getAt() == null
				&& m.getNotes().isEmpty());
			if (m.getAt() != null && Wilderness.contains(m.getAt()))
			{
				assertTrue(m.getName() + " is in the Wilderness but not flagged", m.isWilderness());
			}
		}
	}

	@Test
	public void everyTierHasItsRewards()
	{
		int previous = 0;
		for (CaTier tier : CaTier.values())
		{
			CaReward r = CaTasks.reward(tier);
			assertNotNull(tier + " has no reward", r);
			assertFalse(r.getPerks().isEmpty());
			assertTrue(r.getPerks().get(0).startsWith("Antique lamp"));
			assertEquals(r.getPoints(), tier.getFallbackThreshold());
			assertTrue(r.getPoints() > previous);
			previous = r.getPoints();
		}
	}

	@Test
	public void kindsAndCountsReadFromTheText()
	{
		CaTask moleNovice = byName("Giant Mole Novice");
		assertEquals(10, moleNovice.count());
		CaTask f2p = byName("Fighting as Intended");
		assertTrue(f2p.freeWorld());
		assertFalse(f2p.isMembers());
		CaTask spectre = byName("Noxious Foe");
		assertEquals(Integer.valueOf(60), spectre.getLevels().get(net.runelite.api.Skill.SLAYER));
	}

	@Test
	public void wikiTipsLeavePrayerAdviceOut()
	{
		for (CaTask t : CaTasks.all())
		{
			if (t.getText().toLowerCase().contains("pray"))
			{
				continue;
			}
			for (String tip : CaTips.wiki(t))
			{
				assertFalse(t.getName() + ": " + tip, tip.toLowerCase().matches(".*\\b(pray|protect from)\\b.*"));
			}
		}
	}

	static CaTask byName(String name)
	{
		for (CaTask t : CaTasks.all())
		{
			if (t.getName().equals(name))
			{
				return t;
			}
		}
		throw new AssertionError(name);
	}
}
