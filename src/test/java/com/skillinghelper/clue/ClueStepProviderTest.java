package com.skillinghelper.clue;

import com.skillinghelper.OpenClueGuideRequest;
import com.skillinghelper.clue.data.ClueTier;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ClueStepProviderTest
{
	@Test
	public void oneStepPerTierDoneFromSavedCounts()
	{
		AccountState a = AccountState.builder().members(true)
			.profileConfig(k -> k.equals("clue-scroll-guide.completed.easy") ? "12" : null).build();
		List<PlanStep> steps = new ClueStepProvider().steps(a);
		assertEquals(ClueTier.values().length, steps.size());
		for (PlanStep s : steps)
		{
			ClueTier t = ClueTier.valueOf(s.getId().substring("clue:".length()));
			assertEquals(t == ClueTier.EASY, s.isDone());
			assertEquals(new OpenClueGuideRequest(t.key()), s.getOpen());
			assertFalse(s.isWilderness());
		}
		PlanStep master = steps.get(ClueTier.MASTER.ordinal());
		assertTrue(master.getAfter().contains(ClueStepProvider.stepId(ClueTier.ELITE)));
		assertEquals(4, master.getAfter().size());
	}
}
