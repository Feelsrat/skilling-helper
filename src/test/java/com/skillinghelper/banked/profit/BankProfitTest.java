package com.skillinghelper.banked.profit;

import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.engine.StepType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BankProfitTest
{
	private static final WorldPoint GRAND_EXCHANGE = new WorldPoint(3164, 3486, 0);
	private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);

	private final Map<Integer, Integer> priceTable = new HashMap<>();
	private final PriceLookup prices = id -> priceTable.getOrDefault(id, 1);

	@Test
	public void everyRecipeMakesSomethingElse()
	{
		assertTrue(ProfitRecipes.all().size() > 100);
		for (ProfitRecipe r : ProfitRecipes.all())
		{
			assertTrue(r.getId(), r.getProductId() > 0);
			assertTrue(r.getId(), r.getPerAction() > 0);
			assertFalse(r.getId(), r.getActivity().keyInput().matches(r.getProductId()));
			assertNotNull(r.getId(), r.getProductName());
			assertTrue(r.getId(), r == ProfitRecipes.byId(r.getId()));
		}
	}

	@Test
	public void picksTheBestUseOfEachItemAndListsBetterOnesAFewLevelsAway()
	{
		priceTable.put(ItemID.YEW_LOGS, 300);
		priceTable.put(ItemID.UNSTRUNG_YEW_SHORTBOW, 400);
		priceTable.put(ItemID.UNSTRUNG_YEW_LONGBOW, 600);
		PlayerState s = state(Map.of(ItemID.YEW_LOGS, 1_000), 66, GRAND_EXCHANGE);

		BankProfit.Result r = BankProfit.find(s, prices, 5, Collections.emptyMap());
		assertEquals(1, r.getNow().size());
		ProfitPick now = r.getNow().get(0);
		assertEquals("banked_fletch_yew_shortbow", now.getId());
		// 1,000 x (400 - 8 tax) - 1,000 x (300 - 6 tax)
		assertEquals(392_000 - 294_000, now.getProfit());
		assertEquals(1, r.getSoon().size());
		ProfitPick soon = r.getSoon().get(0);
		assertEquals("banked_fletch_yew_longbow", soon.getId());
		assertEquals(4, soon.getLevelsAway());
		assertEquals("Needs Fletching 70 (you're 66)", soon.getLocked());

		// At 70 the longbows are the pick and nothing is left for later
		r = BankProfit.find(state(Map.of(ItemID.YEW_LOGS, 1_000), 70, GRAND_EXCHANGE), prices, 5,
			Collections.emptyMap());
		assertEquals("banked_fletch_yew_longbow", r.getNow().get(0).getId());
		assertTrue(r.getSoon().isEmpty());

		// Too far away to mention
		r = BankProfit.find(state(Map.of(ItemID.YEW_LOGS, 1_000), 60, GRAND_EXCHANGE), prices, 2,
			Collections.emptyMap());
		assertTrue(r.getNow().isEmpty());
		assertTrue(r.getSoon().isEmpty());
	}

	@Test
	public void missingSecondariesAreBoughtAndLosingRecipesLeftOut()
	{
		priceTable.put(ItemID.RANARRVIAL, 7_000);
		priceTable.put(ItemID.SNAPE_GRASS, 300);
		priceTable.put(ItemID._3DOSEPRAYERRESTORE, 8_000);
		PlayerState s = state(Map.of(ItemID.RANARRVIAL, 100, ItemID.SNAPE_GRASS, 40, ItemID.COINS, 1_000_000), 99,
			GRAND_EXCHANGE);
		ProfitPick p = BankProfit.evaluate(ProfitRecipes.byId("banked_potion_prayer_potions"), s, prices, 5,
			Collections.emptyMap());
		assertNotNull(p);
		assertEquals(100, p.getActions());
		assertEquals(60 * 300, p.getBuyCost());
		assertTrue(p.getInputs().toString(), p.getInputs().get(1).contains("buy 60"));
		assertTrue(p.getProfit() > 0);

		priceTable.put(ItemID._3DOSEPRAYERRESTORE, 7_000);
		BankProfit.Result r = BankProfit.find(s, prices, 5, Collections.emptyMap());
		assertTrue(r.getNow().stream().noneMatch(x -> x.getId().equals("banked_potion_prayer_potions")));
	}

	@Test
	public void cleanHerbsCountForPotionsWithBoughtVials()
	{
		priceTable.put(ItemID.RANARR_WEED, 6_000);
		priceTable.put(ItemID.VIAL_WATER, 5);
		priceTable.put(ItemID.RANARRVIAL, 7_000);
		PlayerState s = state(Map.of(ItemID.RANARR_WEED, 10), 99, GRAND_EXCHANGE);
		ProfitPick unf = BankProfit.evaluate(ProfitRecipes.byId("profit_unf_ranarr_weed"), s, prices, 5,
			Collections.emptyMap());
		assertNotNull(unf);
		assertEquals(10 * 5, unf.getBuyCost());
		assertEquals(10 * (7_000 - 140) - 10 * (6_000 - 120) - 10 * 5, unf.getProfit());
	}

	@Test
	public void sameBankGivesAnEqualResult()
	{
		priceTable.put(ItemID.UNCUT_RUBY, 900);
		priceTable.put(ItemID.RUBY, 1_200);
		Map<Integer, Integer> bank = Map.of(ItemID.UNCUT_RUBY, 50);
		BankProfit.Result a = BankProfit.find(state(bank, 50, GRAND_EXCHANGE), prices, 5, Collections.emptyMap());
		BankProfit.Result b = BankProfit.find(state(bank, 50, GRAND_EXCHANGE), prices, 5, Collections.emptyMap());
		assertEquals(a, b);
		assertFalse(a.getNow().isEmpty());
		BankProfit.Result c = BankProfit.find(state(Map.of(ItemID.UNCUT_RUBY, 51), 50, GRAND_EXCHANGE), prices, 5,
			Collections.emptyMap());
		assertNotEquals(a, c);
	}

	@Test
	public void sellStepsGoBankThenClerkThenDone()
	{
		int bow = ItemID.UNSTRUNG_YEW_LONGBOW;
		int noted = 99_999;
		GuideStep step = SellStep.next(state(Map.of(bow, 500), 70, LUMBRIDGE), bow, noted, "Yew longbow (u)", 500);
		assertEquals(StepType.BANK, step.getType());
		assertNotNull(step.getRoute());
		assertEquals(Integer.valueOf(0), step.getWithdrawAmounts().get(bow));

		PlayerState carrying = state(Map.of(), 70, GRAND_EXCHANGE).toBuilder()
			.inventory(new ItemCounts(Map.of(noted, 500))).build();
		step = SellStep.next(carrying, bow, noted, "Yew longbow (u)", 500);
		assertEquals(StepType.GET_ITEMS, step.getType());
		assertNull(step.getRoute());
		assertTrue(step.getHighlights().contains(SellStep.CLERK));
		assertTrue(step.getItemHighlights().containsKey(noted));

		assertNull(SellStep.next(state(Map.of(), 70, GRAND_EXCHANGE), bow, noted, "Yew longbow (u)", 500));
	}

	private static PlayerState state(Map<Integer, Integer> bank, int level, WorldPoint where)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill skill : Skill.values())
		{
			levels.put(skill, level);
			xp.put(skill, Experience.getXpForLevel(level));
		}
		return PlayerState.builder()
			.levels(levels)
			.xp(xp)
			.bank(new ItemCounts(bank))
			.freeInventorySlots(28)
			.location(where)
			.membersWorld(true)
			.completedQuests(EnumSet.allOf(Quest.class))
			.build();
	}
}
