package com.skillinghelper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.skillinghelper.collection.data.ClogData;
import com.skillinghelper.collection.data.ClogEntry;
import com.skillinghelper.collection.data.ClogItem;
import com.skillinghelper.collection.data.ClogLinks;
import com.skillinghelper.collection.data.ClogTab;
import com.skillinghelper.collection.data.GuideLink;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.pvm.data.PvmLadder;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class ClogDataTest
{
	private final ClogData data = ClogData.get();

	@Test
	public void everyTabAndPageIsThere()
	{
		Map<ClogTab, Integer> pages = new EnumMap<>(ClogTab.class);
		for (ClogEntry e : data.entries())
		{
			pages.merge(e.getTab(), 1, Integer::sum);
			assertFalse(e.getName() + " has no slots", e.getItems().isEmpty());
			assertFalse(e.getName() + " has no wiki page", e.getWikiPage().isEmpty());
		}
		assertEquals(ClogTab.values().length, pages.size());
		assertTrue("expected 120+ pages, got " + data.entries().size(), data.entries().size() >= 120);
		assertTrue("expected 1,650+ unique slots, got " + data.items().size(), data.items().size() >= 1650);
	}

	@Test
	public void slotsAreUniqueWithinAPage()
	{
		for (ClogEntry e : data.entries())
		{
			Set<Integer> ids = new HashSet<>();
			for (ClogItem i : e.getItems())
			{
				assertTrue(e.getName() + " lists " + i.getName() + " twice", ids.add(i.getId()));
				assertTrue(i.getName() + " has a bad percentage", i.getPercent() <= 100);
			}
		}
	}

	@Test
	public void petsAreThere()
	{
		ClogEntry pets = data.entry("All Pets");
		assertNotNull(pets);
		assertTrue(pets.getItems().size() >= 65);
		ClogEntry skilling = data.entry("Skilling Pets");
		assertNotNull(skilling);
		for (ClogItem pet : skilling.getItems())
		{
			GuideLink link = ClogLinks.of(pet, data);
			assertNotNull(pet.getName() + " has no skill guide", link);
			assertEquals(GuideLink.Kind.SKILL, link.getKind());
		}
	}

	@Test
	public void everyGuideLinkExists()
	{
		GuideRepository guides = new GuideRepository();
		Set<String> pvm = new HashSet<>();
		PvmLadder.ALL.forEach(c -> pvm.add(c.getId()));
		for (ClogEntry e : data.entries())
		{
			check(ClogLinks.of(e), e.getName(), pvm, guides);
			for (ClogItem i : e.getItems())
			{
				check(ClogLinks.of(i, data), i.getName(), pvm, guides);
			}
		}
		// The table must name real pages too, or a renamed wiki page would silently lose its link
		for (String name : new String[]{"Barrows Chests", "Giant Mole", "Tempoross", "Wintertodt", "Rooftop Agility",
			"Guardians of the Rift", "The Fight Caves", "General Graardor"})
		{
			assertNotNull(name + " lost its guide", ClogLinks.of(data.entry(name)));
		}
	}

	private static void check(GuideLink link, String what, Set<String> pvm, GuideRepository guides)
	{
		if (link == null)
		{
			return;
		}
		switch (link.getKind())
		{
			case PVM:
				assertTrue(what + " links to missing PvM entry " + link.getTarget(), pvm.contains(link.getTarget()));
				break;
			case METHOD:
				assertNotNull(what + " links to missing method " + link.getTarget(), guides.byId(link.getTarget()));
				break;
			default:
				assertNotNull(link.getSkill());
		}
	}

	@Test
	public void wildernessPagesAreFlagged()
	{
		for (String name : new String[]{"Callisto and Artio", "Chaos Elemental", "Scorpia", "Revenants",
			"Vet'ion and Calvar'ion", "Venenatis and Spindel"})
		{
			assertTrue(name, data.entry(name).isWilderness());
		}
		for (String name : new String[]{"Giant Mole", "Barrows Chests", "Wintertodt", "Kalphite Queen"})
		{
			assertFalse(name, data.entry(name).isWilderness());
		}
	}

	@Test
	public void bossNamesForOutlines()
	{
		assertEquals(java.util.List.of("Callisto", "Artio"), data.entry("Callisto and Artio").npcNames());
		assertEquals(java.util.List.of("Giant Mole"), data.entry("Giant Mole").npcNames());
		assertEquals(java.util.List.of("Hueycoatl"), data.entry("The Hueycoatl").npcNames());
		assertTrue(data.entry("Easy Treasure Trails").npcNames().isEmpty());
	}

	@Test
	public void savedIdsRoundTrip()
	{
		Set<Integer> ids = Set.of(12646, 7981, 7982);
		assertEquals(ids, CollectionLogPlugin.parseIds(CollectionLogPlugin.joinIds(ids)));
		assertTrue(CollectionLogPlugin.parseIds(null).isEmpty());
		assertEquals(Set.of(1), CollectionLogPlugin.parseIds("1,x,"));
	}
}
