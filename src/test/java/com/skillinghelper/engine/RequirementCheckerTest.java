package com.skillinghelper.engine;

import com.skillinghelper.data.CombatGear;
import com.skillinghelper.data.CombatStyle;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

	private static Method ranged()
	{
		return Method.builder()
			.id("ranged").skill(Skill.RANGED).name("Ranged").type(MethodType.ACTIVITY)
			.location(Location.of("Spot", 3200, 3200, 5))
			.input(CombatGear.AMMO_INPUT).gearStyle(CombatStyle.RANGED)
			.instruction("Train")
			.build();
	}

	private static PlayerState rangedAt(int ranged, ItemCounts bank)
	{
		return PlayerState.builder().inventory(ItemCounts.EMPTY).bank(bank).freeInventorySlots(28).membersWorld(true)
			.levels(Map.of(Skill.RANGED, ranged, Skill.DEFENCE, 70)).build();
	}

	private static List<String> texts(List<Requirement> reqs)
	{
		return reqs.stream().map(Requirement::getText).collect(Collectors.toList());
	}

	@Test
	public void rangedGearFollowsTheRangedLevelNotTheMonster()
	{
		List<String> at70 = texts(checker.check(ranged(), rangedAt(70, ItemCounts.of(ItemID.COINS, 1)), 100));
		assertEquals("Dragon bolts (0 carried, 0 in bank, 100 needed for goal) - buy 100 x ~50 gp = ~5,000 gp",
			at70.get(0));
		// Pricey crossbows (Armadyl, dragon hunter) aren't suggested without the coins for them
		assertTrue(at70.contains("Gear upgrade (weapon): Dragon crossbow (~50 gp on the GE)"));
		assertTrue(at70.contains("Gear upgrade (ammo): Dragon bolts (~50 gp on the GE)"));

		List<String> at75 = texts(checker.check(ranged(), rangedAt(75, ItemCounts.of(ItemID.COINS, 1)), 100));
		assertTrue(at75.contains("Gear upgrade (weapon): Toxic blowpipe (~50 gp on the GE)"));
		assertTrue(at75.contains("Gear upgrade (ammo): Dragon darts (~50 gp on the GE, loaded into the blowpipe with "
			+ "Zulrah's scales)"));
	}

	@Test
	public void bestOwnedGearIsWornEvenIfItIsNotSoldOnTheGe()
	{
		List<String> bofa = texts(checker.check(ranged(),
			rangedAt(85, ItemCounts.of(ItemID.BOW_OF_FAERDHINEN_INFINITE, 1, ItemID.ACB, 1)), 100));
		assertTrue(bofa.contains("Gear: wear your Bow of faerdhinen (in bank)"));
		// It makes its own arrows, and both hands are taken
		assertTrue(bofa.stream().noneMatch(t -> t.contains("Ammo") || t.contains("bolts") || t.contains("arrows")));
		assertTrue(bofa.stream().noneMatch(t -> t.contains("(shield)")));
	}

	@Test
	public void pricyGearIsSuggestedWhenYouCanAffordIt()
	{
		List<String> rich = texts(checker.check(ranged(), rangedAt(75, ItemCounts.of(ItemID.COINS, 1_000_000)), 100));
		assertTrue(rich.contains("Gear upgrade (weapon): Twisted bow (~50 gp on the GE)"));
		assertTrue(rich.contains("Gear upgrade (amulet): Necklace of anguish (~50 gp on the GE)"));
	}

	@Test
	public void crossbowsGetAnOffhand()
	{
		List<String> rcb = texts(checker.check(ranged(),
			rangedAt(70, ItemCounts.of(ItemID.XBOWS_CROSSBOW_RUNITE, 1, ItemID.COINS, 1)), 100));
		assertTrue(rcb.contains("Gear upgrade (shield): Odium ward (~50 gp on the GE)"));

		List<String> owned = texts(checker.check(ranged(),
			rangedAt(80, ItemCounts.of(ItemID.ACB, 1, ItemID.ODIUM_WARD, 1)), 100));
		assertTrue(owned.contains("Gear: wear your Armadyl crossbow (in bank)"));
		assertTrue(owned.contains("Gear: wear your Odium ward (in bank)"));

		List<String> bow = texts(checker.check(ranged(),
			rangedAt(70, ItemCounts.of(ItemID.MAGIC_SHORTBOW, 1, ItemID.COINS, 1)), 100));
		assertTrue(bow.stream().noneMatch(t -> t.contains("(shield)")));
	}

	@Test
	public void ammoMatchesTheBowYouOwn()
	{
		// A willow shortbow can't fire anything better than mithril arrows
		List<String> willow = texts(checker.check(ranged(),
			rangedAt(45, ItemCounts.of(ItemID.WILLOW_SHORTBOW, 1, ItemID.IRON_ARROW, 100)), 100));
		assertTrue(willow.contains("Gear upgrade (ammo): Mithril arrows (~50 gp on the GE)"));
		assertTrue(willow.contains("Gear upgrade (weapon): Yew shortbow (~50 gp on the GE)"));
		assertTrue(willow.get(0).startsWith("Arrows (0 carried, 100 in bank"));

		List<String> msb = texts(checker.check(ranged(), rangedAt(55, ItemCounts.of(ItemID.MAGIC_SHORTBOW, 1)), 100));
		assertTrue(msb.contains("Gear upgrade (ammo): Amethyst arrows (~50 gp on the GE)"));
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
