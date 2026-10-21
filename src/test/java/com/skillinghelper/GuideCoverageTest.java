package com.skillinghelper;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.SkillOutfit;
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
}
