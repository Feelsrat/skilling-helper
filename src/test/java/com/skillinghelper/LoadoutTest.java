package com.skillinghelper;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.ItemHighlight;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import java.util.List;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class LoadoutTest
{
	private final GuideRepository guides = new GuideRepository();
	private final StepResolver resolver = new StepResolver(id -> 1000);

	private GuideStep step(Method m, PlayerState s)
	{
		List<Requirement> reqs = resolver.requirements(m, s, 1000);
		return resolver.resolve(m, s, Math.min(99, s.level(m.getSkill()) + 10), m.isDropByDefault(), reqs);
	}

	private static PlayerState at(Method m, Skill skill, int level, ItemCounts bank, ItemCounts inventory)
	{
		return PlannerTest.state(skill, level, true, bank).toBuilder()
			.location(m.getLocation().getPoint())
			.inventory(inventory)
			.build();
	}

	@Test
	public void sandAndSodaAshSplitTheInventory()
	{
		Method m = guides.byId("craft_glass_beer");
		PlayerState s = at(m, Skill.CRAFTING, 10,
			ItemCounts.of(ItemID.BUCKET_SAND, 500, ItemID.SODA_ASH, 500, ItemID.GLASSBLOWINGPIPE, 1), ItemCounts.EMPTY);
		GuideStep step = step(m, s);
		assertEquals(StepType.BANK, step.getType());
		assertEquals(Integer.valueOf(14), step.getWithdrawAmounts().get(ItemID.BUCKET_SAND));
		assertEquals(Integer.valueOf(14), step.getWithdrawAmounts().get(ItemID.SODA_ASH));
		// Making glass doesn't need the pipe, so it stays in the bank
		assertNull(step.getWithdrawAmounts().get(ItemID.GLASSBLOWINGPIPE));
	}

	@Test
	public void carriedToolTakesASlotAndGetsPutBackWhenNotNeeded()
	{
		Method m = guides.byId("craft_glass_beer");
		PlayerState s = at(m, Skill.CRAFTING, 10,
			ItemCounts.of(ItemID.BUCKET_SAND, 500, ItemID.SODA_ASH, 500),
			ItemCounts.of(ItemID.GLASSBLOWINGPIPE, 1, ItemID.BUCKET_SAND, 2));
		GuideStep step = step(m, s);
		assertEquals(ItemHighlight.DEPOSIT, step.getItemHighlights().get(ItemID.GLASSBLOWINGPIPE));
		assertEquals(Integer.valueOf(12), step.getWithdrawAmounts().get(ItemID.BUCKET_SAND));
		assertEquals(Integer.valueOf(14), step.getWithdrawAmounts().get(ItemID.SODA_ASH));
	}

	@Test
	public void steelBarsTakeOneIronToTwoCoal()
	{
		Method m = guides.byId("smith_steel_bars");
		PlayerState s = at(m, Skill.SMITHING, 30,
			ItemCounts.of(ItemID.IRON_ORE, 500, ItemID.COAL, 1000), ItemCounts.EMPTY);
		GuideStep step = step(m, s);
		assertEquals(StepType.BANK, step.getType());
		assertEquals(Integer.valueOf(9), step.getWithdrawAmounts().get(ItemID.IRON_ORE));
		assertEquals(Integer.valueOf(18), step.getWithdrawAmounts().get(ItemID.COAL));
	}

	@Test
	public void unrelatedItemsAndExtrasGetDeposited()
	{
		Method m = guides.byId("smith_steel_bars");
		PlayerState s = at(m, Skill.SMITHING, 30,
			ItemCounts.of(ItemID.IRON_ORE, 500, ItemID.COAL, 1000),
			ItemCounts.of(ItemID.LOBSTER, 3, ItemID.IRON_ORE, 12));
		GuideStep step = step(m, s);
		assertEquals(Integer.valueOf(0), step.getDepositAmounts().get(ItemID.LOBSTER));
		// The lobsters go back, so the trip is still 9 + 18 and the 3 extra iron ore go back too
		assertEquals(Integer.valueOf(3), step.getDepositAmounts().get(ItemID.IRON_ORE));
		assertEquals(Integer.valueOf(18), step.getWithdrawAmounts().get(ItemID.COAL));
	}

	@Test
	public void tripIsCappedByWhatTheBankHas()
	{
		Method m = guides.byId("smith_steel_bars");
		PlayerState s = at(m, Skill.SMITHING, 30,
			ItemCounts.of(ItemID.IRON_ORE, 500, ItemID.COAL, 10), ItemCounts.EMPTY);
		GuideStep step = step(m, s);
		assertEquals(Integer.valueOf(5), step.getWithdrawAmounts().get(ItemID.IRON_ORE));
		assertEquals(Integer.valueOf(10), step.getWithdrawAmounts().get(ItemID.COAL));
	}
}
