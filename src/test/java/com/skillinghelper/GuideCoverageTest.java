package com.skillinghelper;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.PatchType;
import com.skillinghelper.data.SkillOutfit;
import com.skillinghelper.data.Stop;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.MethodPlanner;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GuideCoverageTest
{
	private final GuideRepository guides = new GuideRepository();

	private static PlayerState.PlayerStateBuilder player(Skill skill, int level, ItemCounts bank)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, s == Skill.HITPOINTS ? 10 : 1);
			xp.put(s, Experience.getXpForLevel(levels.get(s)));
		}
		levels.put(skill, level);
		xp.put(skill, Experience.getXpForLevel(level));
		return PlayerState.builder().levels(levels).xp(xp).bank(bank).freeInventorySlots(28).membersWorld(true);
	}

	@Test
	public void everySkillHasAGuide()
	{
		for (Skill skill : Skill.values())
		{
			assertTrue(skill + " has no guide", guides.hasGuide(skill));
		}
	}

	@Test
	public void everySkillProducesAPlanAndAStep()
	{
		MethodPlanner planner = new MethodPlanner(id -> 50);
		StepResolver resolver = new StepResolver(id -> 50);
		for (Skill skill : Skill.values())
		{
			for (int level : new int[]{1, 30, 60, 90})
			{
				int start = skill == Skill.HITPOINTS ? Math.max(10, level) : level;
				PlayerState s = player(skill, start, new ItemCounts(Map.of(ItemID.COINS, 1_000_000))).build();
				List<Method> methods = guides.methods(skill);
				List<MethodOption> ranked = planner.rank(methods, s, TrainingPreference.RECOMMENDED, start + 5);
				assertFalse(skill + " " + level, ranked.isEmpty());
				planner.plan(methods, s, TrainingPreference.RECOMMENDED, start + 5, null);
				MethodOption best = ranked.get(0);
				List<Requirement> reqs = resolver.requirements(best.getMethod(), s, best.getActions());
				assertNotNull(resolver.resolve(best.getMethod(), s, start + 5, false, reqs));
			}
		}
	}

	@Test
	public void everyOutfitSourceMethodExists()
	{
		for (SkillOutfit outfit : SkillOutfit.values())
		{
			if (outfit.getSourceMethodId() != null)
			{
				Method m = guides.byId(outfit.getSourceMethodId());
				assertNotNull(outfit + " source method missing", m);
				assertEquals(outfit.getSkill(), m.getSkill());
			}
		}
	}

	@Test
	public void staffReplacesElementalRunes()
	{
		Method camelot = guides.byId("magic_camelot_tele");
		StepResolver resolver = new StepResolver(id -> 5);
		PlayerState s = player(Skill.MAGIC, 50, new ItemCounts(Map.of()))
			.equipment(new ItemCounts(Map.of(ItemID.STAFF_OF_AIR, 1)))
			.inventory(new ItemCounts(Map.of(ItemID.LAWRUNE, 500)))
			.location(camelot.getLocation().getPoint())
			.build();
		List<Requirement> reqs = resolver.requirements(camelot, s, 100);
		assertTrue(reqs.stream().noneMatch(r -> r.getStatus() == Requirement.Status.MISSING));
		assertTrue(reqs.stream().anyMatch(r -> r.getText().contains("instead of air runes")));
		GuideStep step = resolver.resolve(camelot, s, 60, false, reqs);
		assertEquals(StepType.TRAIN, step.getType());
	}

	@Test
	public void geTaxIsTakenOffProductValue()
	{
		PriceLookup prices = id -> 1_000;
		assertEquals(980, prices.sellPriceAfterTax(1));
		assertEquals(0, PriceLookup.geTax(49));
	}

	@Test
	public void cantAffordWholeGoalExplainsHowFarGoldGoes()
	{
		Method gems = guides.byId("craft_cut_sapphire");
		StepResolver resolver = new StepResolver(id -> id == ItemID.UNCUT_SAPPHIRE ? 500 : 600);
		PlayerState s = player(Skill.CRAFTING, 20, new ItemCounts(Map.of(ItemID.COINS, 10_000, ItemID.CHISEL, 1)))
			.inventory(new ItemCounts(Map.of(ItemID.CHISEL, 1)))
			.location(gems.getLocation().getPoint())
			.build();
		List<Requirement> reqs = resolver.requirements(gems, s, 1_000);
		GuideStep step = resolver.resolve(gems, s, 40, false, reqs);
		assertEquals(StepType.GET_ITEMS, step.getType());
		assertTrue(step.getDetails().stream().anyMatch(l -> l.startsWith("You can afford about 20 actions")));
		assertTrue(step.getDetails().stream().anyMatch(l -> l.contains("sell the products and repeat")));
	}

	@Test
	public void unownedOutfitIsSuggestedWithTimeSaved()
	{
		Method lobsters = guides.byId("fish_lobster_catherby");
		StepResolver resolver = new StepResolver(id -> 100);
		PlayerState s = player(Skill.FISHING, 40, new ItemCounts(Map.of())).build();
		List<Requirement> reqs = resolver.requirements(lobsters, s, 2_000);
		assertTrue(reqs.stream().anyMatch(r -> r.getStatus() == Requirement.Status.UNLOCK
			&& r.getText().contains("Angler outfit") && r.getText().contains("save ~")));
	}

	@Test
	public void geBoostItemIsSuggestedWithPrice()
	{
		Method knights = guides.byId("thieve_ardougne_knights");
		StepResolver resolver = new StepResolver(id -> 1_234);
		PlayerState s = player(Skill.THIEVING, 60, new ItemCounts(Map.of())).build();
		List<Requirement> reqs = resolver.requirements(knights, s, 100);
		assertTrue(reqs.stream().anyMatch(r -> r.getStatus() == Requirement.Status.SUGGESTED
			&& r.getText().contains("Dodgy necklace") && r.getText().contains("1,234 gp")));
	}

	@Test
	public void slayerMastersNeedCombatLevel()
	{
		MethodPlanner planner = new MethodPlanner(id -> 0);
		PlayerState s = player(Skill.SLAYER, 60, null).build();
		List<MethodOption> ranked = planner.rank(guides.methods(Skill.SLAYER), s, TrainingPreference.FAST, 70);
		MethodOption duradel = ranked.stream().filter(o -> o.getMethod().getId().equals("slayer_duradel"))
			.findFirst().orElseThrow();
		assertFalse(duradel.isAvailable());
		assertTrue(duradel.getBlockers().contains("Needs combat level 100"));
	}

	@Test
	public void combatGuideSuggestsGearToWearAndBuy()
	{
		Method cows = guides.byId("attack_cows");
		StepResolver resolver = new StepResolver(id -> 100);
		PlayerState s = player(Skill.ATTACK, 45, new ItemCounts(Map.of(ItemID.ADAMANT_SCIMITAR, 1))).build();
		List<Requirement> reqs = resolver.requirements(cows, s, 100);
		assertTrue(reqs.stream().anyMatch(r -> r.getStatus() == Requirement.Status.OPTIONAL
			&& r.getText().equals("Gear: wear your Adamant scimitar (in bank)")));
		assertTrue(reqs.stream().anyMatch(r -> r.getStatus() == Requirement.Status.SUGGESTED
			&& r.getText().startsWith("Gear upgrade (weapon): Rune scimitar")));
		assertTrue(reqs.stream().anyMatch(r -> r.getText().startsWith("Optional: bring 10 x food")));
	}

	@Test
	public void toolSetsNeedEveryCopy()
	{
		Method chins = guides.byId("hunter_red_chinchompas");
		StepResolver resolver = new StepResolver(id -> 30);
		PlayerState s = player(Skill.HUNTER, 70, new ItemCounts(Map.of(ItemID.HUNTING_BOX_TRAP, 2))).build();
		List<Requirement> reqs = resolver.requirements(chins, s, 100);
		Requirement traps = reqs.stream().filter(r -> r.getText().startsWith("5 x Box traps")).findFirst().orElseThrow();
		assertEquals(Requirement.Status.MISSING, traps.getStatus());
		assertEquals(90, traps.getBuyCost());
	}

	@Test
	public void birdhouseRunsAskForSupplies()
	{
		Method birdhouses = guides.byId("hunter_birdhouses");
		StepResolver resolver = new StepResolver(id -> 30);
		PlayerState s = player(Skill.HUNTER, 30, new ItemCounts(Map.of(ItemID.HAMMER, 1, ItemID.CHISEL, 1,
			ItemID.OAK_LOGS, 20, ItemID.POH_CLOCKWORK_MECHANISM, 10, ItemID.BARLEY_SEED, 100)))
			.completedQuests(Set.of(Quest.BONE_VOYAGE))
			.location(birdhouses.getLocation().getPoint())
			.build();
		GuideStep step = resolver.resolve(birdhouses, s, 40, false, resolver.requirements(birdhouses, s, 8));
		assertEquals(StepType.BANK, step.getType());
		assertTrue(step.getDetails().stream().anyMatch(l -> l.contains("Clockworks")));
		assertTrue(step.getDetails().stream().anyMatch(l -> l.contains("seeds")));
	}

	private PlayerState.PlayerStateBuilder birdhouser(ItemCounts inventory, Map<String, Integer> waits)
	{
		return player(Skill.HUNTER, 30, new ItemCounts(Map.of(ItemID.COINS, 10_000)))
			.inventory(inventory)
			.completedQuests(Set.of(Quest.BONE_VOYAGE))
			.stopWaits(waits);
	}

	@Test
	public void craftedBirdHousesReplaceLogsAndClockworks()
	{
		Method birdhouses = guides.byId("hunter_birdhouses");
		StepResolver resolver = new StepResolver(id -> 30);
		PlayerState s = birdhouser(new ItemCounts(Map.of(ItemID.HAMMER, 1, ItemID.CHISEL, 1,
			ItemID.BIRDHOUSE_OAK, 4, ItemID.BARLEY_SEED, 40)), Map.of()).build();
		GuideStep step = resolver.resolve(birdhouses, s, 40, false, resolver.requirements(birdhouses, s, 8));
		assertEquals(StepType.WALK, step.getType());
		assertEquals("Go to Verdant Valley bird house (northeast)", step.getTitle());
	}

	@Test
	public void birdhouseRunGoesToTheNextUnseededSpot()
	{
		Method birdhouses = guides.byId("hunter_birdhouses");
		StepResolver resolver = new StepResolver(id -> 30);
		PlayerState.PlayerStateBuilder b = birdhouser(new ItemCounts(Map.of(ItemID.HAMMER, 1, ItemID.CHISEL, 1,
			ItemID.BIRDHOUSE_OAK, 2, ItemID.BARLEY_SEED, 20)),
			Map.of("birdhouse_valley_ne", 49, "birdhouse_valley_sw", 50));
		PlayerState s = b.build();
		GuideStep step = resolver.resolve(birdhouses, s, 40, false, resolver.requirements(birdhouses, s, 8));
		assertEquals("Go to Mushroom Meadow bird house (south)", step.getTitle());
		assertTrue(step.getDetails().contains("Stop 3 of 4: Mushroom Meadow bird house (south) (2 still to do this run)"));

		PlayerState there = b.location(birdhouses.getStops().get(2).getLocation().getPoint()).build();
		step = resolver.resolve(birdhouses, there, 40, false, resolver.requirements(birdhouses, there, 8));
		assertEquals(StepType.TRAIN, step.getType());
	}

	@Test
	public void finishedBirdhouseRunWaitsWithCountdown()
	{
		Method birdhouses = guides.byId("hunter_birdhouses");
		StepResolver resolver = new StepResolver(id -> 30);
		PlayerState s = birdhouser(ItemCounts.EMPTY, Map.of("birdhouse_valley_ne", 12, "birdhouse_valley_sw", 13,
			"birdhouse_meadow_s", 15, "birdhouse_meadow_n", 16)).build();
		GuideStep step = resolver.resolve(birdhouses, s, 40, false, resolver.requirements(birdhouses, s, 8));
		assertEquals(StepType.WAIT, step.getType());
		assertEquals("Run done: next one ready in ~12 min", step.getTitle());
	}

	@Test
	public void patchStatesTellGrowingFromReady()
	{
		// Ranarr just planted, ranarr ready, weeds
		assertEquals(80, PatchType.HERB.minutesLeft(32));
		assertEquals(20, PatchType.HERB.minutesLeft(35));
		assertEquals(0, PatchType.HERB.minutesLeft(36));
		assertEquals(0, PatchType.HERB.minutesLeft(3));
		// Oak waiting for a health check
		assertEquals(0, PatchType.TREE.minutesLeft(12));
		assertEquals(160, PatchType.TREE.minutesLeft(8));
	}

	@Test
	public void herbRunSkipsGrowingPatchesAndSaysHowToGetThere()
	{
		Method herbs = guides.byId("farm_herb_runs");
		StepResolver resolver = new StepResolver(id -> 100);
		PlayerState s = player(Skill.FARMING, 40, new ItemCounts(Map.of(ItemID.COINS, 10_000)))
			.inventory(new ItemCounts(Map.of(ItemID.SPADE, 1, ItemID.RAKE, 1, ItemID.DIBBER, 1, ItemID.RANARR_SEED, 5)))
			.stopWaits(Map.of("herb_falador", 30, "herb_port_phasmatys", 40))
			.build();
		GuideStep step = resolver.resolve(herbs, s, 50, false, resolver.requirements(herbs, s, 10));
		assertEquals(StepType.WALK, step.getType());
		assertEquals("Go to Catherby herb patch", step.getTitle());
		assertTrue(step.getDetails().stream().anyMatch(l -> l.startsWith("Fastest: Camelot Teleport")));
	}

	@Test
	public void everyRunStopHasAUniqueIdAndPatchVarbit()
	{
		Set<String> ids = new java.util.HashSet<>();
		for (Method m : guides.all())
		{
			Set<String> inMethod = new java.util.HashSet<>();
			for (Stop stop : m.getStops())
			{
				assertTrue(m.getId() + " repeats " + stop.getId(), inMethod.add(stop.getId()));
				ids.add(stop.getId());
				if (stop.getPatch() != null)
				{
					assertTrue(stop.getId(), stop.getVarbit() > 0);
				}
			}
		}
		assertTrue(ids.size() >= 16);
	}

	@Test
	public void basketsOfApplesAreMadeFromCookingApples()
	{
		Method willows = guides.byId("farm_tree_willow");
		StepResolver resolver = new StepResolver(id -> 100);
		ItemCounts bank = new ItemCounts(Map.of(ItemID.COINS, 100_000, ItemID.PLANTPOT_WILLOW_SAPLING, 5,
			ItemID.COOKING_APPLE, 25, ItemID.BASKET_EMPTY, 5));
		PlayerState s = player(Skill.FARMING, 35, bank)
			.inventory(new ItemCounts(Map.of(ItemID.SPADE, 1)))
			.build();
		List<Requirement> reqs = resolver.requirements(willows, s, 5);
		assertTrue(reqs.stream().anyMatch(r -> r.getText().startsWith("Baskets of apples (gardener payment): make")));
		GuideStep step = resolver.resolve(willows, s, 40, false, reqs);
		assertEquals(StepType.BANK, step.getType());
		assertTrue(step.getDetails().contains("• Withdraw 20 x Cooking apples (to make baskets of apples "
			+ "(gardener payment))"));

		PlayerState carrying = player(Skill.FARMING, 35, bank)
			.inventory(new ItemCounts(Map.of(ItemID.SPADE, 1, ItemID.COOKING_APPLE, 20, ItemID.BASKET_EMPTY, 4)))
			.build();
		step = resolver.resolve(willows, carrying, 40, false, resolver.requirements(willows, carrying, 5));
		assertEquals(StepType.MAKE, step.getType());
	}

	@Test
	public void unfinishedPotionsAreMadeWhenYouHaveTheHerbs()
	{
		Method prayer = guides.byId("herb_prayer");
		StepResolver resolver = new StepResolver(id -> 500);
		PlayerState s = player(Skill.HERBLORE, 40, new ItemCounts(Map.of(ItemID.RANARR_WEED, 50,
			ItemID.VIAL_WATER, 50, ItemID.SNAPE_GRASS, 50)))
			.completedQuests(Set.of(Quest.DRUIDIC_RITUAL))
			.location(prayer.getLocation().getPoint())
			.build();
		GuideStep step = resolver.resolve(prayer, s, 45, false, resolver.requirements(prayer, s, 100));
		assertEquals(StepType.BANK, step.getType());
		assertTrue(step.getDetails().contains("• Withdraw 14 x Clean ranarr (to make ranarr potions (unf))"));
		assertTrue(step.getDetails().stream().noneMatch(l -> l.contains("Snape grass")));
	}

	@Test
	public void itemsTheGeDoesntSellSendYouToTheirSource()
	{
		Method otto = guides.byId("fish_barbarian_otto");
		StepResolver resolver = new StepResolver(id -> 10);
		PlayerState s = player(Skill.FISHING, 50, new ItemCounts(Map.of(ItemID.FEATHER, 1_000))).build();
		GuideStep step = resolver.resolve(otto, s, 55, false, resolver.requirements(otto, s, 100));
		assertEquals(StepType.WALK, step.getType());
		assertTrue(step.getDetails().get(0).contains("Search the bed in Otto's hut"));

		// A slayer ring replaces the enchanted gem
		Method turael = guides.byId("slayer_turael");
		PlayerState ringed = player(Skill.SLAYER, 5, new ItemCounts(Map.of(ItemID.SLAYER_RING_8, 1))).build();
		List<Requirement> reqs = resolver.requirements(turael, ringed, 10);
		assertTrue(reqs.stream().anyMatch(r -> r.getText().startsWith("Slayer ring (in bank) (instead of enchanted gem)")));
	}

	@Test
	public void planksAreMadeAtTheSawmillWhenCheaper()
	{
		Method larders = guides.byId("con_oak_larders");
		StepResolver resolver = new StepResolver(id -> id == ItemID.PLANK_OAK ? 600 : id == ItemID.OAK_LOGS ? 50 : 1);
		PlayerState s = player(Skill.CONSTRUCTION, 40, new ItemCounts(Map.of(ItemID.COINS, 1_000_000,
			ItemID.OAK_LOGS, 500)))
			.inventory(new ItemCounts(Map.of(ItemID.HAMMER, 1, ItemID.POH_SAW, 1, ItemID.OAK_LOGS, 24,
				ItemID.COINS, 10_000)))
			.build();
		GuideStep step = resolver.resolve(larders, s, 45, false, resolver.requirements(larders, s, 100));
		assertEquals(StepType.WALK, step.getType());
		assertEquals("Go to Varrock sawmill", step.getTitle());
	}

	@Test
	public void ownRateReplacesTheGuideEstimate()
	{
		MethodPlanner planner = new MethodPlanner(id -> 0);
		Method willows = guides.byId("farm_tree_willow");
		Method birdhouses = guides.byId("hunter_birdhouses");
		PlayerState s = player(Skill.HUNTER, 30, new ItemCounts(Map.of()))
			.measuredXpPerHour(Map.of(birdhouses.getId(), 12_345))
			.completedQuests(Set.of(Quest.BONE_VOYAGE))
			.build();
		MethodOption o = planner.rank(List.of(birdhouses), s, TrainingPreference.RECOMMENDED, 35).get(0);
		assertTrue(o.isMeasured());
		assertEquals(12_345, o.getXpPerHour(), 0.1);
		// Willow bird houses (level 24+) give 560 xp each, so 4 per run
		assertEquals(560, o.getXpPerAction(), 0.1);
		assertTrue(o.isRun());
		assertEquals(3, o.runs(5_000));
		assertFalse(willows.getStops().isEmpty());
	}

	@Test
	public void banksAtTheClosestBank()
	{
		Method glass = guides.byId("craft_glass_beer");
		StepResolver resolver = new StepResolver(id -> 10);
		// Standing next to Varrock East bank with the materials banked: no trip to the GE
		PlayerState s = player(Skill.CRAFTING, 5, new ItemCounts(Map.of(ItemID.MOLTEN_GLASS, 100,
			ItemID.GLASSBLOWINGPIPE, 1)))
			.location(new net.runelite.api.coords.WorldPoint(3255, 3420, 0))
			.build();
		GuideStep step = resolver.resolve(glass, s, 10, false, resolver.requirements(glass, s, 50));
		assertEquals(StepType.BANK, step.getType());
		assertEquals("Bank", step.getTitle());
	}
}
