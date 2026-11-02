package com.skillinghelper.money;

import com.skillinghelper.money.data.MoneyItem;
import com.skillinghelper.money.data.MoneyMethod;
import com.skillinghelper.money.data.MoneyMethods;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.PvmLadder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MoneyMakingTest
{
	private static MoneyMethod method(String suffix)
	{
		for (MoneyMethod m : MoneyMethods.all())
		{
			if (m.getPage().equals("Money making guide/" + suffix))
			{
				return m;
			}
		}
		throw new AssertionError("No method " + suffix);
	}

	private static MoneyPlayer player(int level, boolean members, String... questsDone)
	{
		MoneyPlayer.MoneyPlayerBuilder b = MoneyPlayer.builder().members(members)
			.knownQuests(Set.of("priest in peril", "the fremennik trials", "lunar diplomacy", "druidic ritual"))
			.questsDone(Set.of(questsDone));
		for (Skill s : Skill.values())
		{
			b.level(s, level);
		}
		return b.build();
	}

	@Test
	public void loadsEveryWikiMethod()
	{
		List<MoneyMethod> all = MoneyMethods.all();
		assertTrue("expected the full wiki list, got " + all.size(), all.size() > 500);
		Set<String> pages = new HashSet<>();
		for (MoneyMethod m : all)
		{
			assertTrue("duplicate " + m.getPage(), pages.add(m.getPage()));
			assertTrue(m.getPage(), m.getPage().startsWith("Money making guide/"));
			assertNotNull(m.getName());
		}
		assertTrue(all.stream().anyMatch(m -> !m.isMembers()));
		assertTrue(all.stream().anyMatch(MoneyMethod::isRecurring));
		assertTrue("most methods should have a spot", all.stream().filter(m -> m.getSpot() != null).count() > 300);
	}

	@Test
	public void wikiProfitWithoutPrices()
	{
		MoneyMethod snape = method("Collecting snape grass");
		MoneyOption o = MoneyRanker.evaluate(snape, player(99, true, "the fremennik trials", "lunar diplomacy"), null);
		assertEquals(snape.getWikiProfit(), o.getProfit());
		assertTrue(o.isAvailable());
	}

	@Test
	public void livePricesMoveProfit()
	{
		MoneyMethod herbs = method("Cleaning grimy ranarr weed");
		MoneyItem clean = herbs.getOutputs().get(0);
		// Clean ranarr up 100 gp each, everything else as the wiki had it
		MoneyOption o = MoneyRanker.evaluate(herbs, player(99, true, "druidic ritual"),
			id -> id == clean.getId() ? (int) clean.getWikiValue() + 100 : 0);
		assertEquals(herbs.getWikiProfit() + Math.round(clean.getQuantity() * 100), o.getProfit());
	}

	@Test
	public void requirementsBlock()
	{
		MoneyMethod snape = method("Collecting snape grass");
		MoneyOption low = MoneyRanker.evaluate(snape, player(50, true), null);
		assertFalse(low.isAvailable());
		assertTrue(low.getBlockers().toString(), low.getBlockers().stream().anyMatch(b -> b.contains("Magic")));
		assertTrue(low.getBlockers().contains("Needs Lunar Diplomacy"));

		MoneyOption f2p = MoneyRanker.evaluate(snape, player(99, false, "the fremennik trials", "lunar diplomacy"),
			null);
		assertTrue(f2p.getBlockers().contains("Members only"));
	}

	@Test
	public void rankingPutsAvailableFirstThenProfit()
	{
		List<MoneyOption> ranked = MoneyRanker.rank(MoneyMethods.all(), player(30, false), null);
		boolean seenLocked = false;
		long last = Long.MAX_VALUE;
		for (MoneyOption o : ranked)
		{
			if (!o.isAvailable())
			{
				seenLocked = true;
				continue;
			}
			assertFalse("available after locked: " + o.getMethod().getName(), seenLocked);
			assertTrue(o.getProfitPerHour() <= last);
			last = o.getProfitPerHour();
		}
		long f2p = ranked.stream().filter(MoneyOption::isAvailable).count();
		assertTrue("a level 30 F2P account should have some methods: " + f2p, f2p > 5);
		assertTrue(ranked.stream().filter(MoneyOption::isAvailable).noneMatch(o -> o.getMethod().isMembers()));
	}

	@Test
	public void guideGoesSuppliesThenTravelThenWork()
	{
		MoneyMethod snape = method("Collecting snape grass");
		MoneyOption o = MoneyRanker.evaluate(snape, player(99, true, "the fremennik trials", "lunar diplomacy"), null);
		WorldPoint ge = new WorldPoint(3165, 3487, 0);

		MoneyStep supplies = MoneyGuide.resolve(o, ge, Map.of(), false, false);
		assertEquals(MoneyStep.Kind.SUPPLIES, supplies.getKind());
		assertFalse(supplies.getWithdraw().isEmpty());
		assertNull(supplies.getDestination());

		Map<Integer, Integer> carried = new HashMap<>();
		for (int id : supplies.getWithdraw())
		{
			carried.put(id, 100);
		}
		MoneyStep travel = MoneyGuide.resolve(o, ge, carried, false, false);
		assertEquals(MoneyStep.Kind.TRAVEL, travel.getKind());
		assertEquals(snape.getSpot(), travel.getDestination());

		MoneyStep work = MoneyGuide.resolve(o, snape.getSpot().getPoint(), Map.of(), false, true);
		assertEquals(MoneyStep.Kind.WORK, work.getKind());
		assertTrue(work.getLines().size() > 1);
	}

	@Test
	public void shopMethodsOutlineTheShopkeeper()
	{
		MoneyMethod zaff = method("Buying battlestaves from Zaff");
		assertEquals("Zaff", zaff.getNpc());
		assertTrue(zaff.isRecurring());
		MoneyOption o = MoneyRanker.evaluate(zaff, player(99, true), null);
		MoneyStep work = MoneyGuide.resolve(o, zaff.getSpot().getPoint(), Map.of(), false, true);
		assertTrue(work.getNpcs().contains("Zaff"));
	}

	@Test
	public void lockedMethodSaysWhy()
	{
		MoneyOption o = MoneyRanker.evaluate(method("Collecting snape grass"), player(1, true), null);
		MoneyStep step = MoneyGuide.resolve(o, null, Map.of(), false, false);
		assertEquals(MoneyStep.Kind.LOCKED, step.getKind());
	}

	@Test
	public void pvmHandOffsPointAtRealGuides()
	{
		Set<String> ids = PvmLadder.ALL.stream().map(PvmContent::getId).collect(Collectors.toSet());
		for (Map.Entry<String, String> e : FullGuides.PVM_GUIDES.entrySet())
		{
			assertTrue(e.getValue(), ids.contains(e.getValue()));
			assertTrue("no wiki page starts with " + e.getKey(),
				MoneyMethods.all().stream().anyMatch(m -> m.getPage().startsWith(e.getKey())));
		}
		assertEquals("barrows", FullGuides.pvmContent("Money making guide/Barrows"));
		assertNull(FullGuides.pvmContent("Money making guide/Creating Barrows teleport tablets"));
	}

	@Test
	public void sentencesSplit()
	{
		List<String> s = MoneyGuide.sentences("Go there. Click the thing! - First bullet - Second bullet");
		assertEquals(List.of("Go there.", "Click the thing!", "First bullet", "Second bullet"), s);
	}
}
