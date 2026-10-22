package com.skillinghelper.engine;

import com.skillinghelper.data.ItemReq;
import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RecipesTest
{
	private static final ItemReq SAND = ItemReq.part("Bucket of sand", 1, ItemID.BUCKET_SAND);
	private static final ItemReq SODA = ItemReq.part("Soda ash", 1, ItemID.SODA_ASH);
	private static final ItemReq GLASS = ItemReq.consumed("Molten glass", 27, 1, ItemID.MOLTEN_GLASS)
		.madeFrom("Smelt sand and soda ash", false, SAND, SODA);

	private static Recipes recipes(int glassPrice, int partPrice)
	{
		return new Recipes(id -> id == ItemID.MOLTEN_GLASS ? glassPrice : partPrice);
	}

	private static PlayerState state(ItemCounts inventory, ItemCounts bank)
	{
		return PlayerState.builder().inventory(inventory).bank(bank).freeInventorySlots(28).build();
	}

	@Test
	public void buysWhenBuyingIsCheaperAndPartsArentOwned()
	{
		assertFalse(recipes(100, 80).making(GLASS, state(ItemCounts.EMPTY, ItemCounts.EMPTY), Long.MAX_VALUE));
	}

	@Test
	public void makesWhenPartsAreCheaper()
	{
		assertTrue(recipes(200, 80).making(GLASS, state(ItemCounts.EMPTY, ItemCounts.EMPTY), Long.MAX_VALUE));
	}

	@Test
	public void makesWhenThePartsAreAlreadyOwned()
	{
		ItemCounts bank = ItemCounts.of(ItemID.BUCKET_SAND, 10, ItemID.SODA_ASH, 10);
		assertTrue(recipes(100, 80).making(GLASS, state(ItemCounts.EMPTY, bank), Long.MAX_VALUE));
	}

	@Test
	public void makesWhenTheItemHasNoPrice()
	{
		assertTrue(recipes(0, 80).making(GLASS, state(ItemCounts.EMPTY, ItemCounts.EMPTY), Long.MAX_VALUE));
	}

	@Test
	public void doesntMakeWhatsAlreadyOwned()
	{
		ItemCounts bank = ItemCounts.of(ItemID.MOLTEN_GLASS, 27, ItemID.BUCKET_SAND, 10, ItemID.SODA_ASH, 10);
		assertFalse(recipes(200, 80).making(GLASS, state(ItemCounts.EMPTY, bank), Long.MAX_VALUE));
	}

	@Test
	public void alwaysMakeIgnoresPrices()
	{
		ItemReq always = GLASS.madeFrom("Smelt sand and soda ash", true, SAND, SODA);
		assertTrue(recipes(1, 1_000).making(always, state(ItemCounts.EMPTY, ItemCounts.EMPTY), Long.MAX_VALUE));
	}

	@Test
	public void itemsWithoutARecipeAreNeverMade()
	{
		ItemReq logs = ItemReq.consumed("Logs", 27, 1, ItemID.LOGS);
		assertFalse(recipes(1_000, 1).making(logs, state(ItemCounts.EMPTY, ItemCounts.EMPTY), Long.MAX_VALUE));
	}

	@Test
	public void perTripSplitsAnEmptyInventoryBetweenTheParts()
	{
		ItemCounts bank = ItemCounts.of(ItemID.BUCKET_SAND, 100, ItemID.SODA_ASH, 100);
		assertEquals(14, Recipes.perTrip(GLASS, state(ItemCounts.EMPTY, bank), Long.MAX_VALUE));
	}

	@Test
	public void perTripIsCappedByThePartsOwned()
	{
		ItemCounts bank = ItemCounts.of(ItemID.BUCKET_SAND, 5, ItemID.SODA_ASH, 100);
		assertEquals(5, Recipes.perTrip(GLASS, state(ItemCounts.EMPTY, bank), Long.MAX_VALUE));
	}

	@Test
	public void keepsMakingUntilTheGoalIsCovered()
	{
		// 27 glass is a trip's worth, but the goal needs 200: use up the parts first instead of glassblowing now
		ItemCounts bank = ItemCounts.of(ItemID.MOLTEN_GLASS, 27, ItemID.BUCKET_SAND, 100, ItemID.SODA_ASH, 100);
		assertTrue(recipes(200, 80).making(GLASS, state(ItemCounts.EMPTY, bank), 200));
		assertFalse(recipes(200, 80).making(GLASS, state(ItemCounts.EMPTY, bank), 20));
	}

	@Test
	public void makesOnlyWhatTheGoalNeeds()
	{
		ItemCounts bank = ItemCounts.of(ItemID.BUCKET_SAND, 100, ItemID.SODA_ASH, 100);
		assertEquals(6, Recipes.perTrip(GLASS, state(ItemCounts.EMPTY, bank), 6));
		assertEquals(6, Recipes.wanted(GLASS, 6));
		assertEquals(27, Recipes.wanted(GLASS, Long.MAX_VALUE));
	}

	@Test
	public void partsCarriedNeedsEveryPart()
	{
		assertFalse(Recipes.partsCarried(GLASS, state(ItemCounts.of(ItemID.BUCKET_SAND, 14), ItemCounts.EMPTY)));
		assertTrue(Recipes.partsCarried(GLASS,
			state(ItemCounts.of(ItemID.BUCKET_SAND, 14, ItemID.SODA_ASH, 1), ItemCounts.EMPTY)));
	}

	@Test
	public void makeTripsKeepOnlyThePartsAndCash()
	{
		assertEquals(Set.of(ItemID.BUCKET_SAND, ItemID.SODA_ASH, ItemID.COINS, ItemID.PLATINUM),
			Recipes.keptIds(List.of(GLASS)));
	}
}
