package com.skillinghelper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.skillinghelper.collection.data.ClogData;
import com.skillinghelper.collection.data.ClogEntry;
import com.skillinghelper.collection.data.ClogItem;
import com.skillinghelper.collection.data.ClogTab;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.pvm.data.PvmLadder;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import org.junit.Test;

public class ClogPlannerTest
{
	private static final ClogData DATA = ClogData.get();
	private static final ClogRequirements REQS = new ClogRequirements(PvmLadder.ALL, new GuideRepository().all());

	private static ClogPlanner planner(int level, Set<Integer> obtained, Set<Integer> missing, Set<Integer> owned,
		boolean wild, boolean locked)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, level);
		}
		Map<Quest, QuestState> quests = new HashMap<>();
		return new ClogPlanner(DATA, new ClogProgress(obtained, missing, owned),
			(e, link) -> REQS.check(e, link, levels, quests), wild, locked);
	}

	private static ClogPlanner fresh(int level)
	{
		return planner(level, Set.of(), Set.of(), Set.of(), false, false);
	}

	@Test
	public void quickestComesMostCommonFirst()
	{
		List<ClogView.SlotRow> rows = fresh(99).quickest();
		assertEquals(ClogPlanner.QUICKEST_ROWS, rows.size());
		for (int i = 1; i < rows.size(); i++)
		{
			assertTrue(rows.get(i - 1).getPercent() >= rows.get(i).getPercent());
		}
		Set<Integer> ids = new HashSet<>();
		for (ClogView.SlotRow r : rows)
		{
			assertTrue("listed twice: " + r.getName(), ids.add(r.getId()));
			assertFalse(r.getName() + " is in the Wilderness", r.isWilderness());
		}
	}

	@Test
	public void filledSlotsLeaveTheList()
	{
		ClogView.SlotRow top = fresh(99).quickest().get(0);
		List<ClogView.SlotRow> after = planner(99, Set.of(top.getId()), Set.of(), Set.of(), false, false).quickest();
		assertTrue(after.stream().noneMatch(r -> r.getId() == top.getId()));
		// Owned items count as filled until the log says otherwise
		after = planner(99, Set.of(), Set.of(), Set.of(top.getId()), false, false).quickest();
		assertTrue(after.stream().noneMatch(r -> r.getId() == top.getId()));
		after = planner(99, Set.of(), Set.of(top.getId()), Set.of(top.getId()), false, false).quickest();
		assertTrue(after.stream().anyMatch(r -> r.getId() == top.getId()));
	}

	@Test
	public void lockedPagesAreHiddenUnlessAsked()
	{
		// A level 1 account can't fight the Abyssal Sire (85 Slayer)
		ClogEntry sire = DATA.entry("Abyssal Sire");
		Set<Integer> some = new HashSet<>();
		some.add(sire.getItems().get(sire.getItems().size() - 1).getId());
		assertTrue(planner(1, some, Set.of(), Set.of(), false, false).closest().stream()
			.noneMatch(r -> r.getName().equals("Abyssal Sire")));
		ClogView.EntryRow row = planner(1, some, Set.of(), Set.of(), false, true).closest().stream()
			.filter(r -> r.getName().equals("Abyssal Sire")).findFirst().orElse(null);
		assertNotNull(row);
		assertTrue(row.getMissing().contains("Level 85 Slayer"));
		assertTrue(planner(90, some, Set.of(), Set.of(), false, false).closest().stream()
			.anyMatch(r -> r.getName().equals("Abyssal Sire")));
	}

	@Test
	public void closestPutsFewestLeftFirst()
	{
		ClogEntry mole = DATA.entry("Giant Mole");
		ClogEntry barrows = DATA.entry("Barrows Chests");
		Set<Integer> have = new HashSet<>();
		for (int i = 0; i < mole.getItems().size() - 1; i++)
		{
			have.add(mole.getItems().get(i).getId());
		}
		have.add(barrows.getItems().get(0).getId());
		List<ClogView.EntryRow> rows = planner(99, have, Set.of(), Set.of(), false, false).closest();
		assertEquals("Giant Mole", rows.get(0).getName());
		assertEquals(1, rows.get(0).getTotal() - rows.get(0).getHave());
	}

	@Test
	public void wildernessNeedsTheSharedSetting()
	{
		ClogEntry chaos = DATA.entry("Chaos Elemental");
		Set<Integer> some = Set.of(chaos.getItems().get(0).getId());
		assertTrue(planner(99, some, Set.of(), Set.of(), false, false).closest().stream()
			.noneMatch(r -> r.getName().equals("Chaos Elemental")));
		assertTrue(planner(99, some, Set.of(), Set.of(), true, false).closest().stream()
			.anyMatch(r -> r.getName().equals("Chaos Elemental") && r.isWilderness()));
	}

	@Test
	public void petsListEveryPetWithItsSource()
	{
		List<ClogView.SlotRow> pets = fresh(99).pets();
		assertEquals(DATA.entry("All Pets").getItems().size(), pets.size());
		ClogView.SlotRow mole = pets.stream().filter(p -> p.getName().equals("Baby Mole")).findFirst().orElseThrow();
		assertEquals("Giant Mole", mole.getEntry());
		assertEquals("PvM Progression", mole.getGuide());
		ClogView.SlotRow heron = pets.stream().filter(p -> p.getName().equals("Heron")).findFirst().orElseThrow();
		assertEquals("Skilling Pets", heron.getEntry());
		assertEquals("Skilling Helper (Fishing)", heron.getGuide());
		ClogView.SlotRow chaos = pets.stream().filter(p -> p.getName().equals("Pet chaos elemental")).findFirst()
			.orElseThrow();
		assertTrue(chaos.isWilderness());
	}

	@Test
	public void everyPageHasBeginnerSteps()
	{
		ClogPlanner p = fresh(1);
		for (ClogEntry e : DATA.entries())
		{
			ClogView.EntryDetail d = p.detail(e, false, List.of());
			assertTrue(e.getName() + " has too few steps", d.getSteps().size() >= 2);
			assertEquals(e.getItems().size(), d.getSlots().size());
			if (e.isWilderness())
			{
				assertTrue(e.getName(), d.getSteps().stream().anyMatch(s -> s.contains("Wilderness")));
			}
		}
		ClogView.EntryDetail mole = p.detail(DATA.entry("Giant Mole"), false, List.of());
		assertEquals("PvM Progression", mole.getGuide());
		assertTrue(mole.getSteps().stream().anyMatch(s -> s.contains("Open full guide")));
	}

	@Test
	public void browseFollowsTheGameOrder()
	{
		List<ClogView.EntryRow> bosses = fresh(99).browse(ClogTab.BOSSES);
		assertEquals("Abyssal Sire", bosses.get(0).getName());
		assertTrue(bosses.stream().allMatch(r -> r.getTab() == ClogTab.BOSSES));
	}

	@Test
	public void viewsCompareByValue()
	{
		Set<Integer> have = new HashSet<>();
		for (ClogItem i : DATA.entry("Giant Mole").getItems().subList(0, 2))
		{
			have.add(i.getId());
		}
		assertEquals(fresh(50).quickest(), fresh(50).quickest());
		assertEquals(planner(50, have, Set.of(), Set.of(), false, false).closest(),
			planner(50, new HashSet<>(have), Set.of(), Set.of(), false, false).closest());
		ClogEntry mole = DATA.entry("Giant Mole");
		assertEquals(fresh(50).detail(mole, false, List.of()), fresh(50).detail(mole, false, List.of()));
		assertNotEquals(fresh(50).detail(mole, false, List.of()), fresh(50).detail(mole, true, List.of()));
	}
}
