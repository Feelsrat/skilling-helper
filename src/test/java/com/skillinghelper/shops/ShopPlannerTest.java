package com.skillinghelper.shops;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ShopPlannerTest
{
	private static final WorldPoint GE = Banks.GRAND_EXCHANGE.getPoint();
	private static final WorldPoint FALADOR_FARM = new WorldPoint(3040, 3290, 0);
	private static final WorldPoint AL_KHARID = new WorldPoint(3300, 3190, 0);

	private static ShopPlanner.Want want(int id, int quantity, int gePrice)
	{
		return new ShopPlanner.Want(id, null, quantity, gePrice);
	}

	@Test
	public void everyShopLineLoads()
	{
		assertFalse(Shops.all().isEmpty());
		for (List<ShopOffer> offers : Shops.all().values())
		{
			for (ShopOffer o : offers)
			{
				assertTrue(o.getItemName(), o.getPrice() > 0);
				assertFalse(o.getShop().getName(), o.getShop().getNpcs().isEmpty());
				assertTrue(o.getShop().getName(), o.getStock() != 0);
			}
		}
		assertFalse(Shops.selling(ItemID.SPADE).isEmpty());
	}

	@Test
	public void priceClimbsAsStockRunsDown()
	{
		ShopOffer vials = Shops.selling(ItemID.VIAL_WATER).stream()
			.filter(o -> o.getShop().getName().startsWith("Jatix")).findFirst().orElseThrow();
		assertEquals(vials.getPrice(), vials.unitPrice(0));
		assertTrue(vials.unitPrice(100) > vials.unitPrice(10));
		assertTrue(vials.unitPrice(10) >= vials.unitPrice(0));
	}

	@Test
	public void cheapToolsNearbyComeFromTheShop()
	{
		ShopPlan plan = ShopPlanner.plan(List.of(want(ItemID.SPADE, 1, 166), want(ItemID.RAKE, 1, 187)),
			FALADOR_FARM, true);
		assertNotNull(plan);
		assertEquals("Sarah's Farming shop", plan.getShop().getName());
		assertEquals(2, plan.getLines().size());
		assertTrue(plan.getRest().isEmpty());
		assertEquals(9, plan.getCost());
		assertTrue(plan.describe().get(0).contains("saves"));
	}

	@Test
	public void membersShopsAreSkippedOnFreeWorlds()
	{
		ShopPlan plan = ShopPlanner.plan(List.of(want(ItemID.SPADE, 1, 166), want(ItemID.RAKE, 1, 187)),
			FALADOR_FARM, false);
		// Falador's general store has a spade but no rake, and saves too little to be a detour
		assertNull(plan);
	}

	@Test
	public void smallSavingsDontBeatTheGeWhenItsOnTheWay()
	{
		// At the GE already: a 165 gp saving on a spade isn't worth the walk
		assertNull(ShopPlanner.plan(List.of(want(ItemID.SPADE, 1, 166)), GE, true));
	}

	@Test
	public void bigSavingsAreWorthAFarTrip()
	{
		ShopPlan plan = ShopPlanner.plan(List.of(want(ItemID.RAW_SHARK, 50, 473), want(ItemID.COAL, 100, 150)),
			GE, true);
		assertNotNull(plan);
		assertEquals("Frankie's Fishing Emporium", plan.getShop().getName());
		ShopPlan.Line sharks = plan.getLines().get(0);
		assertEquals(2, sharks.getWorlds());
		assertTrue(sharks.getSaving() > ShopPlanner.MIN_SAVING);
		assertEquals(1, plan.getRest().size());
		assertTrue(String.join("\n", plan.describe()).contains("hop worlds"));
		assertTrue(String.join("\n", plan.describe()).contains("Grand Exchange"));
	}

	@Test
	public void tooMuchHoppingFallsBackToTheGe()
	{
		// Feathers get dearer than the GE after about a hundred in each world
		assertNull(ShopPlanner.line(Shops.selling(ItemID.FEATHER).get(0), want(ItemID.FEATHER, 50_000, 4)));
	}

	@Test
	public void skillingGuideSendsYouToTheShopWhenTheSettingIsOn()
	{
		GuideRepository guides = new GuideRepository();
		Method gems = guides.byId("craft_cut_sapphire");
		StepResolver resolver = new StepResolver(id -> 1_000);
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, s == Skill.HITPOINTS ? 10 : 20);
			xp.put(s, Experience.getXpForLevel(levels.get(s)));
		}
		PlayerState.PlayerStateBuilder b = PlayerState.builder().levels(levels).xp(xp).freeInventorySlots(28)
			.membersWorld(false).location(AL_KHARID)
			.bank(new ItemCounts(Map.of(ItemID.COINS, 100_000, ItemID.UNCUT_SAPPHIRE, 1_000)));

		PlayerState off = b.build();
		List<Requirement> reqs = resolver.requirements(gems, off, 100);
		GuideStep ge = resolver.resolve(gems, off, 30, false, reqs);
		assertEquals("Buy the items you're missing", ge.getTitle());

		PlayerState on = b.preferShops(true).build();
		GuideStep shop = resolver.resolve(gems, on, 30, false, resolver.requirements(gems, on, 100));
		assertEquals(StepType.GET_ITEMS, shop.getType());
		assertTrue(shop.getTitle(), shop.getTitle().startsWith("Buy from "));
		assertEquals(1, shop.getHighlights().size());
		assertNotNull(shop.getDestination());
	}
}
