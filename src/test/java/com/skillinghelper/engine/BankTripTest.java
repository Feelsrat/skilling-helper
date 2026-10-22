package com.skillinghelper.engine;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BankTripTest
{
	private static final ItemReq LOGS = ItemReq.consumed("Logs", 27, 1, ItemID.LOGS);
	private static final ItemReq KNIFE = ItemReq.tool("Knife", ItemID.KNIFE);
	private static final ItemReq FEATHERS = ItemReq.stack("Feathers", 1, ItemID.FEATHER);
	private static final Method FLETCH = Method.builder()
		.id("fletch").skill(Skill.FLETCHING).name("Fletch").type(MethodType.PROCESS)
		.location(Location.of("Bank", 3200, 3200, 5))
		.tool(KNIFE).input(LOGS)
		.instruction("Fletch")
		.build();

	private final BankTrip trips = new BankTrip(new Recipes(id -> 100));

	private static PlayerState state(ItemCounts inventory, ItemCounts bank)
	{
		return PlayerState.builder().inventory(inventory).bank(bank).freeInventorySlots(28).build();
	}

	@Test
	public void processingFillsTheInventoryAroundTheTools()
	{
		PlayerState s = state(ItemCounts.of(ItemID.KNIFE, 1), ItemCounts.of(ItemID.LOGS, 1_000));
		assertEquals(Map.of(LOGS, 27), trips.tripAmounts(FLETCH, s, 1_000));
	}

	@Test
	public void processingLeavesRoomForToolsStillInTheBank()
	{
		PlayerState s = state(ItemCounts.EMPTY, ItemCounts.of(ItemID.LOGS, 1_000, ItemID.KNIFE, 1));
		assertEquals(Map.of(LOGS, 27), trips.tripAmounts(FLETCH, s, 1_000));
	}

	@Test
	public void tripIsCappedByWhatTheGoalNeeds()
	{
		PlayerState s = state(ItemCounts.of(ItemID.KNIFE, 1), ItemCounts.of(ItemID.LOGS, 1_000));
		assertEquals(Map.of(LOGS, 5), trips.tripAmounts(FLETCH, s, 5));
	}

	@Test
	public void gatheringHasNoTripAmounts()
	{
		Method gather = Method.builder()
			.id("fish").skill(Skill.FISHING).name("Fish").type(MethodType.GATHER)
			.location(Location.of("Spot", 3200, 3200, 5))
			.input(FEATHERS)
			.instruction("Fish")
			.build();
		assertTrue(trips.tripAmounts(gather, state(ItemCounts.EMPTY, ItemCounts.EMPTY), 100).isEmpty());
	}

	@Test
	public void gatheringOnlyBanksWhenConsumablesRunOut()
	{
		PlayerState worn = PlayerState.builder().equipment(ItemCounts.of(ItemID.FEATHER, 10)).build();
		assertFalse(BankTrip.needsWithdraw(FEATHERS, worn, true));
		assertTrue(BankTrip.needsWithdraw(FEATHERS, PlayerState.builder().build(), true));
	}

	@Test
	public void withdrawLinesTopUpToTheTrip()
	{
		PlayerState s = state(ItemCounts.of(ItemID.KNIFE, 1, ItemID.LOGS, 7), ItemCounts.of(ItemID.LOGS, 1_000));
		GuideStep.GuideStepBuilder b = GuideStep.builder();
		trips.withdrawInputs(b, FLETCH, s, false, 1_000);
		GuideStep step = b.build();
		assertEquals("• Withdraw 20 more logs (27 for this trip)", step.getDetails().get(0));
		assertEquals("This trip: 27 x logs.", step.getDetails().get(1));
		assertEquals(Integer.valueOf(20), step.getWithdrawAmounts().get(ItemID.LOGS));
	}

	@Test
	public void withdrawLinesPutBackExtra()
	{
		PlayerState s = state(ItemCounts.of(ItemID.KNIFE, 1, ItemID.LOGS, 27), ItemCounts.of(ItemID.LOGS, 1_000));
		GuideStep.GuideStepBuilder b = GuideStep.builder();
		trips.withdrawInputs(b, FLETCH, s, false, 10);
		GuideStep step = b.build();
		assertEquals("• Deposit 17 x Logs (10 is enough for this trip)", step.getDetails().get(0));
		assertEquals(Integer.valueOf(17), step.getDepositAmounts().get(ItemID.LOGS));
		assertEquals(ItemHighlight.DEPOSIT, step.getItemHighlights().get(ItemID.LOGS));
	}

	@Test
	public void keptIdsCoverToolsInputsAndCash()
	{
		assertTrue(BankTrip.keptIds(FLETCH).containsAll(Set.of(ItemID.KNIFE, ItemID.LOGS, ItemID.COINS)));
		assertFalse(BankTrip.keptIds(FLETCH).contains(ItemID.FEATHER));
	}
}
