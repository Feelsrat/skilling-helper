package com.skillinghelper.minigame;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.diary.data.DiaryStage;
import com.skillinghelper.minigame.data.Minigame;
import com.skillinghelper.minigame.data.Minigames;
import com.skillinghelper.minigame.data.Reward;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.PvmLadder;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MinigameDataTest
{
	@Test
	public void idsAreUnique()
	{
		Set<String> ids = new HashSet<>();
		for (Minigame m : Minigames.all())
		{
			assertTrue("duplicate id " + m.getId(), ids.add(m.getId()));
			assertEquals(m, Minigames.byId(m.getId()));
		}
	}

	@Test
	public void everyMinigameHasAGuideSomewhere()
	{
		Set<String> methods = new HashSet<>();
		for (Method method : new GuideRepository().all())
		{
			methods.add(method.getId());
		}
		Set<String> pvm = new HashSet<>();
		for (PvmContent c : PvmLadder.ALL)
		{
			pvm.add(c.getId());
		}
		for (Minigame m : Minigames.all())
		{
			int guides = (m.guidedHere() ? 1 : 0) + (m.getSkillingMethod() != null ? 1 : 0)
				+ (m.getPvmContent() != null ? 1 : 0);
			assertEquals(m.getId() + " needs exactly one guide", 1, guides);
			if (m.getSkillingMethod() != null)
			{
				assertTrue(m.getId() + ": no Skilling Helper method " + m.getSkillingMethod(),
					methods.contains(m.getSkillingMethod()));
			}
			if (m.getPvmContent() != null)
			{
				assertTrue(m.getId() + ": no PvM entry " + m.getPvmContent(), pvm.contains(m.getPvmContent()));
			}
		}
	}

	@Test
	public void guidesHereAreBeginnerComplete()
	{
		for (Minigame m : Minigames.all())
		{
			if (!m.guidedHere())
			{
				continue;
			}
			assertNotNull(m.getId() + " needs a start location", m.getLocation());
			assertNotNull(m.getId() + " needs how to get there", m.getGetThere());
			assertEquals(m.getId() + ": the first stage starts at the minigame", m.getLocation(),
				m.getStages().get(0).getAt());
			for (DiaryStage s : m.getStages())
			{
				assertFalse(m.getId() + ": stage without steps", s.getLines().isEmpty());
			}
			for (DiaryItem item : m.getItems())
			{
				assertTrue(m.getId() + ": " + item.getName() + " can't be bought",
					item.getBuyId() > 0 || item.getTool() != null);
			}
		}
	}

	@Test
	public void everyMinigameSaysWhatsWorthChasing()
	{
		for (Minigame m : Minigames.all())
		{
			assertNotNull(m.getId(), m.getSummary());
			assertNotNull(m.getId(), m.getWikiPage());
			assertTrue(m.getId() + " has no reward worth chasing", m.getRewards().stream().anyMatch(Reward::isWorth));
			for (Reward r : m.getRewards())
			{
				assertNotNull(m.getId() + ": " + r.getName(), r.getWhy());
				if (r.getPrice() > 0)
				{
					assertEquals(m.getId() + ": a price needs exactly one currency", 1, m.getPoints().size());
				}
			}
		}
	}

	@Test
	public void wildernessIsFlagged()
	{
		for (Minigame m : Minigames.all())
		{
			if (m.getLocation() != null)
			{
				assertEquals(m.getId(), Wilderness.contains(m.getLocation()), m.isWilderness());
			}
		}
	}
}
