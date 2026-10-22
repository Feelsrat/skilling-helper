package com.skillinghelper.engine;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import java.util.List;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RequirementCheckerTest
{
	private static final ItemReq TRAPS = ItemReq.tools("Box trap", 4, ItemID.HUNTING_BOX_TRAP);
	private static final ItemReq FOOD = ItemReq.supply("Trout", 5, ItemID.TROUT);
	private static final ItemReq AIR = ItemReq.stack("Air rune", 1, ItemID.AIRRUNE)
		.orUse("Staff of air", ItemID.STAFF_OF_AIR);
	private static final ItemReq MIND = ItemReq.stack("Mind rune", 1, ItemID.MINDRUNE);

	private final RequirementChecker checker = new RequirementChecker(id -> 50, new Recipes(id -> 50),
		new BankTrip(new Recipes(id -> 50)));

	private static Method method(ItemReq... reqs)
	{
		Method.MethodBuilder b = Method.builder()
			.id("test").skill(Skill.HUNTER).name("Test").type(MethodType.GATHER)
			.location(Location.of("Spot", 3200, 3200, 5))
			.instruction("Train");
		for (ItemReq r : reqs)
		{
			if (r.getPerAction() > 0)
			{
				b.input(r);
			}
			else if (r == FOOD)
			{
				b.supply(r);
			}
			else
			{
				b.tool(r);
			}
		}
		return b.build();
	}

	private static PlayerState state(ItemCounts inventory, ItemCounts bank)
	{
		return PlayerState.builder().inventory(inventory).bank(bank).freeInventorySlots(28).build();
	}

	@Test
	public void toolSetCountsWhatsCarriedAndBanked()
	{
		List<Requirement> reqs = checker.check(method(TRAPS),
			state(ItemCounts.of(ItemID.HUNTING_BOX_TRAP, 1), ItemCounts.of(ItemID.HUNTING_BOX_TRAP, 3)), 10);
		assertEquals("4 x Box trap (1 carried, 3 in bank)", reqs.get(0).getText());
		assertEquals(Requirement.Status.IN_BANK, reqs.get(0).getStatus());
		assertEquals(3, reqs.get(0).getQuantity());
	}

	@Test
	public void toolSetPricesTheMissingCopies()
	{
		List<Requirement> reqs = checker.check(method(TRAPS), state(ItemCounts.EMPTY, ItemCounts.of(ItemID.HUNTING_BOX_TRAP, 1)),
			10);
		assertEquals("4 x Box trap (you have 1) - buy 3 x ~50 gp", reqs.get(0).getText());
		assertEquals(150, reqs.get(0).getBuyCost());
	}

	@Test
	public void substituteReplacesTheItem()
	{
		List<Requirement> reqs = checker.check(method(AIR, MIND),
			state(ItemCounts.of(ItemID.STAFF_OF_AIR, 1, ItemID.MINDRUNE, 100), ItemCounts.EMPTY), 10);
		assertEquals("Staff of air (instead of air rune)", reqs.get(0).getText());
		assertEquals(Requirement.Status.READY, reqs.get(0).getStatus());
		assertEquals("Mind rune (100 carried, 0 in bank, 10 needed for goal)", reqs.get(1).getText());
	}

	@Test
	public void missingItemWithASubstituteMentionsIt()
	{
		List<Requirement> reqs = checker.check(method(AIR), state(ItemCounts.EMPTY, ItemCounts.EMPTY), 10);
		assertEquals("Air rune (0 carried, 0 in bank, 10 needed for goal) - buy 10 x ~50 gp = ~500 gp, "
			+ "or use a staff of air", reqs.get(0).getText());
		assertEquals(Requirement.Status.MISSING, reqs.get(0).getStatus());
	}

	@Test
	public void suppliesAreOnlyEverSuggestions()
	{
		Method m = method(FOOD);
		assertEquals(Requirement.Status.SUGGESTED,
			checker.check(m, state(ItemCounts.EMPTY, ItemCounts.EMPTY), 10).get(0).getStatus());
		assertEquals("Optional: 5 x Trout (20 in bank)",
			checker.check(m, state(ItemCounts.EMPTY, ItemCounts.of(ItemID.TROUT, 20)), 10).get(0).getText());
		assertEquals(Requirement.Status.READY,
			checker.check(m, state(ItemCounts.of(ItemID.TROUT, 5), ItemCounts.EMPTY), 10).get(0).getStatus());
	}
}
