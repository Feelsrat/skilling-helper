package com.skillinghelper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.skillinghelper.ca.CaGuideTest;
import com.skillinghelper.collection.data.ClogData;
import com.skillinghelper.collection.data.ClogEntry;
import com.skillinghelper.collection.data.ClogLinks;
import com.skillinghelper.collection.data.GuideLink;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ClogGuideTest
{
	private static final ClogData DATA = ClogData.get();

	private static DiaryStep step(ClogEntry e, DiaryPlayer p, boolean arrived)
	{
		GuideLink link = ClogLinks.of(e);
		return ClogGuide.resolve(ClogGuide.Context.builder()
			.player(p)
			.entry(e)
			.slot(e.getItems().get(0))
			.left(new ArrayList<>(e.getItems()))
			.link(link)
			.check(ClogRequirements.Check.NONE)
			.arrived(arrived)
			.build());
	}

	@Test
	public void everyPageHasAStep()
	{
		DiaryPlayer p = CaGuideTest.player(99).allowWilderness(true).build();
		for (ClogEntry e : DATA.entries())
		{
			if (e.isPets())
			{
				continue;
			}
			for (boolean arrived : new boolean[]{false, true})
			{
				DiaryStep s = step(e, p, arrived);
				assertNotNull(e.getName(), s.getTitle());
				assertFalse(e.getName() + " has no lines", s.getLines().isEmpty());
				if (ClogGuide.guidable(e))
				{
					assertTrue(e.getName() + ": " + s.getLines(),
						s.getLines().stream().anyMatch(l -> l.startsWith("Going for: " + e.getItems().get(0).getName())));
				}
			}
		}
	}

	@Test
	public void atTheBossItSaysWhatToClick()
	{
		DiaryPlayer p = CaGuideTest.player(99).build();
		ClogEntry boss = DATA.entries().stream().filter(x -> ClogLinks.of(x) == null && ClogLinks.monster(x) != null
			&& !ClogLinks.monster(x).isNoAttack()).findFirst().orElseThrow();
		assertTrue(ClogGuide.guidable(boss));
		DiaryStep s = step(boss, p, true);
		assertEquals(DiaryStep.Kind.DO, s.getKind());
		List<String> lines = s.getLines();
		assertTrue(lines.toString(), lines.stream().anyMatch(l -> l.contains("choose Attack")));
		assertTrue(lines.toString(), lines.stream().anyMatch(l -> l.startsWith("Keep going")));
		assertFalse(s.getNpcs().isEmpty());
	}

	@Test
	public void linkedPagesAreHandedOver()
	{
		DiaryPlayer p = CaGuideTest.player(99).build();
		DiaryStep s = step(DATA.entry("Giant Mole"), p, false);
		assertTrue(s.getTitle(), s.getTitle().startsWith("Follow the"));
	}

	@Test
	public void missingLevelsLockTheStep()
	{
		ClogEntry e = DATA.entries().stream().filter(x -> x.getSlayerLevel() > 50 && ClogLinks.of(x) == null
			&& ClogGuide.guidable(x)).findFirst().orElseThrow();
		DiaryStep s = step(e, CaGuideTest.player(1).build(), false);
		assertEquals(e.getName(), DiaryStep.Kind.LOCKED, s.getKind());
	}
}
