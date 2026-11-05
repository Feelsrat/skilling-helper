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
import com.skillinghelper.travel.TravelOption;
import com.skillinghelper.travel.TravelPick;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class StepResolverTest
{
	private final GuideRepository guides = new GuideRepository();
	private final StepResolver resolver = new StepResolver(id -> 1000);
	private final Method willows = guides.byId("wc_willows_draynor");
	private final WorldPoint atWillows = willows.getLocation().getPoint();
	private final WorldPoint lumbridge = new WorldPoint(3222, 3218, 0);

	private PlayerState.PlayerStateBuilder base(int wcLevel, int attack)
	{
		PlayerState s = PlannerTest.state(Skill.WOODCUTTING, wcLevel, false, null);
		java.util.Map<Skill, Integer> levels = new java.util.EnumMap<>(s.getLevels());
		levels.put(Skill.ATTACK, attack);
		return PlayerState.builder().levels(levels).xp(s.getXp()).freeInventorySlots(28);
	}

	private GuideStep step(Method m, PlayerState s)
	{
		List<Requirement> reqs = resolver.requirements(m, s, 100);
		return resolver.resolve(m, s, 60, m.isDropByDefault(), reqs);
	}

	@Test
	public void unknownBankSendsPlayerToBankFirst()
	{
		PlayerState s = base(30, 1).location(lumbridge).build();
		GuideStep step = step(willows, s);
		assertEquals(StepType.BANK, step.getType());
		assertNotNull(step.getDestination());
	}

	@Test
	public void missingToolRecommendsBestUsableAndCost()
	{
		PlayerState s = base(45, 1).location(lumbridge).bank(ItemCounts.EMPTY).build();
		List<Requirement> reqs = resolver.requirements(willows, s, 100);
		assertTrue(reqs.get(0).getText().startsWith("Rune axe"));
		assertEquals(Requirement.Status.MISSING, reqs.get(0).getStatus());
		assertEquals(StepType.GET_ITEMS, step(willows, s).getType());
	}

	@Test
	public void toolInBankMeansBankStepWithWithdrawHighlight()
	{
		PlayerState s = base(30, 1).location(lumbridge).bank(ItemCounts.of(ItemID.MITHRIL_AXE, 1, ItemID.BRONZE_AXE, 1, ItemID.RUNE_AXE, 1))
			.build();
		GuideStep step = step(willows, s);
		assertEquals(StepType.BANK, step.getType());
		assertEquals(ItemHighlight.WITHDRAW, step.getItemHighlights().get(ItemID.MITHRIL_AXE));
		assertNull(step.getItemHighlights().get(ItemID.BRONZE_AXE));
	}

	@Test
	public void walksToSpotThenTrains()
	{
		PlayerState away = base(30, 1).location(lumbridge).bank(ItemCounts.EMPTY)
			.inventory(ItemCounts.of(ItemID.MITHRIL_AXE, 1)).build();
		GuideStep walk = step(willows, away);
		assertEquals(StepType.WALK, walk.getType());
		assertEquals(atWillows, walk.getDestination());

		PlayerState there = base(30, 1).location(atWillows).bank(ItemCounts.EMPTY)
			.inventory(ItemCounts.of(ItemID.MITHRIL_AXE, 1)).build();
		GuideStep train = step(willows, there);
		assertEquals(StepType.TRAIN, train.getType());
		assertNull(train.getDestination());
		assertEquals(1, train.getHighlights().size());
	}

	@Test
	public void suggestsWieldingAxeWhenAttackAllows()
	{
		PlayerState there = base(35, 20).location(atWillows).bank(ItemCounts.EMPTY)
			.inventory(ItemCounts.of(ItemID.MITHRIL_AXE, 1)).build();
		assertEquals(ItemHighlight.EQUIP, step(willows, there).getItemHighlights().get(ItemID.MITHRIL_AXE));
	}

	@Test
	public void fullInventoryDropsWhenPowerChopping()
	{
		PlayerState full = base(30, 1).location(atWillows).bank(ItemCounts.EMPTY).freeInventorySlots(0)
			.inventory(ItemCounts.of(ItemID.MITHRIL_AXE, 1, ItemID.WILLOW_LOGS, 27)).build();
		GuideStep step = resolver.resolve(willows, full, 40, true, resolver.requirements(willows, full, 100));
		assertEquals(StepType.DROP, step.getType());
		assertEquals(ItemHighlight.DROP, step.getItemHighlights().get(ItemID.WILLOW_LOGS));

		GuideStep bank = resolver.resolve(willows, full, 40, false, resolver.requirements(willows, full, 100));
		assertEquals(StepType.BANK, bank.getType());
		assertEquals(ItemHighlight.DEPOSIT, bank.getItemHighlights().get(ItemID.WILLOW_LOGS));
	}

	private static TravelPick tablet(boolean carried)
	{
		TravelOption o = new TravelOption(TravelOption.Kind.ITEM, "Varrock tablet", "Varrock tablet",
			new WorldPoint(3213, 3424, 0), new int[]{ItemID.POH_TABLET_VARROCKTELEPORT}, true, List.of(), Map.of(),
			List.of(), List.of(), null, 0);
		return new TravelPick(o, ItemID.POH_TABLET_VARROCKTELEPORT, carried, 10, "Willows");
	}

	@Test
	public void travelItemInBankIsTakenOutInItsOwnColour()
	{
		PlayerState s = base(30, 1).location(lumbridge).travel(List.of(tablet(false)))
			.bank(ItemCounts.of(ItemID.MITHRIL_AXE, 1, ItemID.POH_TABLET_VARROCKTELEPORT, 5)).build();
		GuideStep step = step(willows, s);
		assertEquals(StepType.BANK, step.getType());
		assertEquals(ItemHighlight.TRAVEL, step.getItemHighlights().get(ItemID.POH_TABLET_VARROCKTELEPORT));
		assertEquals(Integer.valueOf(1), step.getWithdrawAmounts().get(ItemID.POH_TABLET_VARROCKTELEPORT));
		assertTrue(step.getDetails().stream().anyMatch(l -> l.contains("Travel: take your Varrock tablet")));
		Map<Integer, ItemHighlight> bank = StepResolver.bankItems(List.of(), step, s.getTravel());
		assertEquals(ItemHighlight.TRAVEL, bank.get(ItemID.POH_TABLET_VARROCKTELEPORT));
	}

	@Test
	public void carriedTravelItemIsKeptWhileHeadingOut()
	{
		PlayerState full = base(30, 1).location(atWillows).bank(ItemCounts.EMPTY).freeInventorySlots(0)
			.travel(List.of(tablet(true)))
			.inventory(ItemCounts.of(ItemID.MITHRIL_AXE, 1, ItemID.WILLOW_LOGS, 26, ItemID.POH_TABLET_VARROCKTELEPORT, 1))
			.build();
		GuideStep bank = resolver.resolve(willows, full, 40, false, resolver.requirements(willows, full, 100));
		assertEquals(StepType.BANK, bank.getType());
		assertNull(bank.getItemHighlights().get(ItemID.POH_TABLET_VARROCKTELEPORT));

		// Once there the guide no longer suggests it, so the next bank trip puts it back to free the slot
		PlayerState arrived = full.toBuilder().travel(List.of()).build();
		GuideStep next = resolver.resolve(willows, arrived, 40, false, resolver.requirements(willows, arrived, 100));
		assertEquals(ItemHighlight.DEPOSIT, next.getItemHighlights().get(ItemID.POH_TABLET_VARROCKTELEPORT));
	}

	@Test
	public void ownedOutfitPieceIsWornBeforeTraining()
	{
		PlayerState s = base(30, 1).location(atWillows).bank(ItemCounts.EMPTY)
			.inventory(ItemCounts.of(ItemID.MITHRIL_AXE, 1, ItemID.RAMBLE_LUMBERJACK_HAT, 1)).build();
		GuideStep step = step(willows, s);
		assertEquals(StepType.EQUIP, step.getType());
		assertEquals(ItemHighlight.EQUIP, step.getItemHighlights().get(ItemID.RAMBLE_LUMBERJACK_HAT));
	}

	@Test
	public void processingWithdrawsInputsFromBank()
	{
		Method fletch = guides.byId("fletch_willow_long");
		PlayerState s = base(1, 1).location(fletch.getLocation().getPoint())
			.bank(ItemCounts.of(ItemID.WILLOW_LOGS, 500))
			.inventory(ItemCounts.of(ItemID.KNIFE, 1)).build();
		GuideStep step = step(fletch, s);
		assertEquals(StepType.BANK, step.getType());
		assertNull(step.getDestination());
		assertEquals(ItemHighlight.WITHDRAW, step.getItemHighlights().get(ItemID.WILLOW_LOGS));
	}

	@Test
	public void goalReached()
	{
		PlayerState s = base(60, 1).location(atWillows).build();
		assertEquals(StepType.DONE, step(willows, s).getType());
	}
}
