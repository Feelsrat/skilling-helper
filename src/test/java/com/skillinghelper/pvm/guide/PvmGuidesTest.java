package com.skillinghelper.pvm.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.skillinghelper.engine.ItemHighlight;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.PvmLadder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

public class PvmGuidesTest
{
	private static final PriceLookup PRICES = id -> 1_000;
	private static final WorldPoint VARROCK_SQUARE = new WorldPoint(3212, 3428, 0);

	private static GuideState.GuideStateBuilder player(int level)
	{
		GuideState.GuideStateBuilder b = GuideState.builder()
			.location(VARROCK_SQUARE)
			.members(true)
			.hitpoints(level)
			.maxHitpoints(level);
		for (Skill s : Skill.values())
		{
			b.level(s, level);
		}
		return b;
	}

	private static PvmStep step(String id, GuideState s)
	{
		PvmGuide g = PvmGuides.forContent(id);
		return GuideResolver.resolve(g, id, s, LoadoutPlan.of(g, s, PRICES));
	}

	@Test
	public void everyLadderEntryHasAGuide()
	{
		Set<String> ladder = PvmLadder.ALL.stream().map(PvmContent::getId).collect(Collectors.toSet());
		Set<String> guides = PvmGuides.ALL.stream().map(PvmGuide::getContentId).collect(Collectors.toSet());
		assertEquals(ladder, guides);
		assertEquals(PvmGuides.ALL.size(), guides.size());
	}

	@Test
	public void foodFillsTheRestOfTheInventory()
	{
		GuideState s = player(50).bank(Map.of()).build();
		LoadoutPlan plan = LoadoutPlan.of(PvmGuides.forContent("bryophyta"), s, PRICES);
		LoadoutPlan.SupplyLine food = plan.getSupplies().stream()
			.filter(l -> l.getSupply().getName().equals("Food")).findFirst().orElseThrow();
		// Key, secateurs and two strength potions leave 24 slots
		assertEquals(24, food.getAmount());
		assertEquals(24, food.getToBuy());
	}

	@Test
	public void unknownBankAsksToOpenIt()
	{
		PvmStep step = step("barrows", player(70).build());
		assertEquals(PvmStep.Kind.BANK, step.getKind());
		assertTrue(step.isHighlightBank());
	}

	@Test
	public void emptyBankSendsToTheGrandExchange()
	{
		PvmStep step = step("giant_mole", player(60).bank(Map.of()).build());
		assertEquals(PvmStep.Kind.BUY, step.getKind());
		assertTrue(step.getLines().stream().anyMatch(l -> l.contains("Monkfish")));
	}

	@Test
	public void bankStepWithdrawsExactAmounts()
	{
		Map<Integer, Integer> bank = new HashMap<>();
		bank.put(ItemID.SHARK, 100);
		bank.put(ItemID._4DOSEPRAYERRESTORE, 50);
		bank.put(ItemID.SPADE, 1);
		bank.put(ItemID.TELETAB_BARROWS, 20);
		bank.put(ItemID.ABYSSAL_WHIP, 1);
		GuideState s = player(70).bank(bank).bankOpen(true).build();
		PvmStep step = step("barrows", s);
		assertEquals(PvmStep.Kind.BANK, step.getKind());
		assertEquals(ItemHighlight.WITHDRAW, step.getItemHighlights().get(ItemID.ABYSSAL_WHIP));
		// Spade, 6 prayer potions and a stack of teleports leave 20 slots for food; monkfish missing, so sharks
		assertTrue(step.getLines().toString(), step.getLines().contains("• 20 x Sharks"));
		assertTrue(step.getLines().contains("• 6 x Prayer potions"));
		assertTrue(step.getLines().contains("• 1 x Barrows teleport tablets"));
	}

	@Test
	public void readyLoadoutGoesToTheKeyStage()
	{
		Map<Integer, Integer> inv = new HashMap<>();
		inv.put(ItemID.SECATEURS, 1);
		inv.put(ItemID.STRENGTH4, 2);
		inv.put(ItemID.SWORDFISH, 24);
		Map<Integer, Integer> worn = new HashMap<>();
		for (int id : new int[]{ItemID.RUNE_SCIMITAR, ItemID.RUNE_FULL_HELM, ItemID.RUNE_CHAINBODY,
			ItemID.RUNE_PLATELEGS, ItemID.RUNE_KITESHIELD, ItemID.AMULET_OF_POWER, ItemID.LEATHER_GLOVES,
			ItemID.LEATHER_BOOTS})
		{
			worn.put(id, 1);
		}
		GuideState s = player(45).members(false).bank(Map.of()).inventory(inv).worn(worn).build();
		PvmStep step = step("bryophyta", s);
		assertEquals(step.getTitle(), PvmStep.Kind.STAGE, step.getKind());
		assertEquals("Get a mossy key", step.getTitle());
		assertNotNull(step.getDestination());
	}

	@Test
	public void bossInViewIsAFightWithAdds()
	{
		GuideState s = player(50).bank(Map.of()).npc("bryophyta", 1).npc("growthling", 3).build();
		PvmStep step = step("bryophyta", s);
		assertEquals(PvmStep.Kind.FIGHT, step.getKind());
		assertEquals(1, step.getAddHighlights().size());
		assertTrue(step.getLines().get(0).startsWith("Growthlings are up"));
	}

	@Test
	public void barrowsDigsTheNextBrotherAndSkipsTheTunnel()
	{
		Map<Integer, Integer> inv = new HashMap<>();
		inv.put(ItemID.SPADE, 1);
		inv.put(ItemID._4DOSEPRAYERRESTORE, 6);
		inv.put(ItemID.TELETAB_BARROWS, 1);
		inv.put(ItemID.MONKFISH, 20);
		GuideState.GuideStateBuilder b = player(70).bank(Map.of()).inventory(inv)
			.location(new WorldPoint(3565, 3289, 0))
			.varbit(VarbitID.BARROWS_KILLED_AHRIM, 1)
			.varbit(VarbitID.BARROWS_KILLED_DHAROK, 1)
			.flag(PvmGuides.Barrows.TUNNEL_FLAG + 2);
		PvmStep step = step("barrows", wearAll(b).build());
		assertEquals("Dig into Karil's mound", step.getTitle());
		assertEquals(ItemHighlight.USE, step.getItemHighlights().get(ItemID.SPADE));

		b.varbit(VarbitID.BARROWS_KILLED_KARIL, 1).varbit(VarbitID.BARROWS_KILLED_TORAG, 1)
			.varbit(VarbitID.BARROWS_KILLED_VERAC, 1);
		assertEquals("Enter the tunnel from Guthan's mound", step("barrows", wearAll(b).build()).getTitle());
	}

	@Test
	public void insideACryptNeverSendsToTheBank()
	{
		GuideState s = player(70).bank(Map.of()).location(new WorldPoint(3557, 9703, 3)).build();
		PvmStep step = step("barrows", s);
		assertEquals("Search the sarcophagus", step.getTitle());
		assertFalse(step.getLines().isEmpty());
	}

	/**
	 * Wears the best melee gear a level 70 can buy so the loadout counts as ready.
	 */
	private static GuideState.GuideStateBuilder wearAll(GuideState.GuideStateBuilder b)
	{
		GuideState probe = b.build();
		LoadoutPlan plan = LoadoutPlan.of(PvmGuides.forContent("barrows"), probe, PRICES);
		for (LoadoutPlan.GearLine g : plan.getGear())
		{
			b.wearing(g.getItemId(), 1);
		}
		return b;
	}

	@Test
	public void everyGuideHasStepsBeyondTravel()
	{
		for (PvmGuide g : PvmGuides.ALL)
		{
			assertFalse(g.getContentId(), g.getStages().isEmpty());
		}
	}

	@Test
	public void moonsGoThroughEachMoonThenTheChest()
	{
		GuideState.GuideStateBuilder b = player(75).bank(Map.of()).location(new WorldPoint(1440, 9600, 0))
			.inv(ItemID._4DOSEMOONLIGHTPOTION, 4);
		assertEquals("Kill the Blood Moon", step("moons_of_peril", ready("moons_of_peril", b).build()).getTitle());
		b.varbit(VarbitID.PMOON_BOSS_BLOOD_DEAD, 1).varbit(VarbitID.PMOON_BOSS_BLUE_DEAD, 1)
			.varbit(VarbitID.PMOON_BOSS_ECLIPSE_DEAD, 1);
		assertEquals("Open the Lunar Chest", step("moons_of_peril", ready("moons_of_peril", b).build()).getTitle());
	}

	@Test
	public void gauntletCountsResourcesInside()
	{
		GuideState s = player(80).bank(Map.of()).location(new WorldPoint(1900, 5700, 1))
			.inv(ItemID.GAUNTLET_CRYSTAL_SHARD, 120).npc("crystalline bat", 2).build();
		PvmStep step = step("gauntlet", s);
		assertEquals("Gather resources", step.getTitle());
		assertTrue(step.getLines().contains("• Shards: 120/400"));
	}

	@Test
	public void zulrahStartsWithTheTripThere()
	{
		GuideState.GuideStateBuilder b = player(85).bank(Map.of()).location(VARROCK_SQUARE);
		PvmStep step = step("zulrah", ready("zulrah", b).build());
		assertEquals("Go to Zul-Andra", step.getTitle());
		assertNotNull(step.getDestination());
	}

	/**
	 * Carries and wears the whole loadout, as if the player had just banked.
	 */
	private static GuideState.GuideStateBuilder ready(String id, GuideState.GuideStateBuilder b)
	{
		LoadoutPlan plan = LoadoutPlan.of(PvmGuides.forContent(id), b.build(), PRICES);
		for (LoadoutPlan.GearLine g : plan.getGear())
		{
			b.wearing(g.getItemId(), 1);
		}
		for (LoadoutPlan.SupplyLine l : plan.getSupplies())
		{
			b.inv(l.getSupply().getOptions().get(0).getIds()[0], l.getAmount());
		}
		return b;
	}

	@Test
	public void missingPotionsStopTheGuideBeforeSettingOff()
	{
		GuideState.GuideStateBuilder b = ready("fight_caves", player(75).bank(Map.of()));
		b.inv(ItemID._4DOSEPOTIONOFSARADOMIN, 4);
		PvmStep step = step("fight_caves", b.build());
		assertEquals(PvmStep.Kind.BUY, step.getKind());
		assertTrue(step.getLines().toString(), step.getLines().stream().anyMatch(l -> l.startsWith("• 4 x Saradomin brews")));

		// Once the trip is under way, drinking some isn't a reason to go back
		PvmStep underWay = step("fight_caves", b.tripStocked(true).build());
		assertEquals(underWay.getTitle(), PvmStep.Kind.STAGE, underWay.getKind());
	}

	@Test
	public void missingPotionsInTheBankAreWithdrawn()
	{
		GuideState.GuideStateBuilder b = ready("fight_caves", player(75)
			.bank(Map.of(ItemID._4DOSEPOTIONOFSARADOMIN, 10)));
		b.inv(ItemID._4DOSEPOTIONOFSARADOMIN, 4);
		PvmStep step = step("fight_caves", b.build());
		assertEquals(PvmStep.Kind.BANK, step.getKind());
		assertTrue(step.getLines().toString(), step.getLines().contains("• 4 x Saradomin brews"));
	}
}
