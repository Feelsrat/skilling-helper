package com.skillinghelper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.skillinghelper.collection.data.ClogData;
import com.skillinghelper.collection.data.ClogItem;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class ClogStepProviderTest
{
	private static PlanStep mole(List<PlanStep> steps)
	{
		return steps.stream().filter(s -> s.getId().equals("clog:Giant Mole")).findFirst().orElseThrow();
	}

	@Test
	public void quickPagesBecomeSteps()
	{
		List<PlanStep> steps = new ClogStepProvider().steps(AccountState.builder().build());
		assertFalse(steps.isEmpty());
		Set<String> ids = new HashSet<>();
		for (PlanStep s : steps)
		{
			assertTrue("duplicate " + s.getId(), ids.add(s.getId()));
			assertTrue(s.getId().startsWith("clog:"));
		}
		PlanStep mole = mole(steps);
		assertFalse(mole.isDone());
		assertEquals(List.of("boss:giant_mole"), mole.getAfter());
		assertTrue(steps.stream().filter(s -> s.getId().equals("clog:Chaos Elemental")).allMatch(PlanStep::isWilderness));
	}

	@Test
	public void filledQuickSlotsFinishTheStep()
	{
		Set<Integer> have = new HashSet<>();
		for (ClogItem i : ClogData.get().entry("Giant Mole").getItems())
		{
			if (i.getPercent() >= ClogStepProvider.QUICK_PERCENT)
			{
				have.add(i.getId());
			}
		}
		String saved = CollectionLogPlugin.joinIds(have);
		AccountState a = AccountState.builder()
			.profileConfig(k -> k.equals(CollectionLogConfig.GROUP + ".obtained") ? saved : null).build();
		assertTrue(mole(new ClogStepProvider().steps(a)).isDone());
	}
}
