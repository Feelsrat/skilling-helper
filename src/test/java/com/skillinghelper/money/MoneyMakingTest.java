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
import net.runelite.api.gameval.ItemID;
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
	public void wikiProfitWithoutPricesLessGeTax()
	{
		MoneyMethod snape = method("Collecting snape grass");
		MoneyOption o = MoneyRanker.evaluate(snape, player(99, true, "the fremennik trials", "lunar diplomacy"), null);
		assertTrue(o.getGeTax() > 0);
		// The wiki's figure doesn't take off the tax
		assertEquals((double) snape.getWikiProfit() - o.getGeTax(), o.getProfit(), 2);
		assertEquals(o.getOutputValue() - o.getGeTax() - o.getInputCost(), o.getProfit(), 2);
		assertTrue(o.isGood());
	}

	@Test
	public void todaysPricesDecideProfit()
	{
		MoneyMethod herbs = method("Cleaning grimy ranarr weed");
		MoneyItem grimy = herbs.getInputs().get(0);
		MoneyItem clean = herbs.getOutputs().get(0);
		MoneyPlayer p = player(99, true, "druidic ritual");
		// Grimy dearer than clean: losing money, so it sorts after the ones that make money
		MoneyOption losing = MoneyRanker.evaluate(herbs, p, prices(grimy.getId(), 5000, clean.getId(), 4900));
		assertFalse(losing.isProfitable());
		assertFalse(losing.isGood());
		assertTrue(losing.isAvailable());

		MoneyOption making = MoneyRanker.evaluate(herbs, p, prices(grimy.getId(), 4000, clean.getId(), 5000));
		long expected = Math.round(clean.getQuantity() * (5000 - 100) - grimy.getQuantity() * 4000);
		assertEquals(expected, making.getProfit());
	}

	@Test
	public void cantAffordATrip()
	{
		MoneyMethod herbs = method("Cleaning grimy ranarr weed");
		MoneyItem grimy = herbs.getInputs().get(0);
		MoneyPlayer broke = player(99, true, "druidic ritual").toBuilder().coins(1000).owned(Map.of()).build();
		MoneyOption o = MoneyRanker.evaluate(herbs, broke, prices(grimy.getId(), 5000, 0, 0));
		assertEquals(28L * 5000, o.getTripCost());
		assertFalse(o.isAvailable());

		// Owning the herbs already means nothing to buy
		MoneyPlayer stocked = broke.toBuilder().owned(Map.of(grimy.getId(), 100)).build();
		MoneyOption ok = MoneyRanker.evaluate(herbs, stocked, prices(grimy.getId(), 5000, 0, 0));
		assertEquals(0, ok.getTripCost());
		assertTrue(ok.isAvailable());
	}

	@Test
	public void productsThatDontTradeAreFlagged()
	{
		MoneyMethod herbs = method("Cleaning grimy ranarr weed");
		MoneyItem clean = herbs.getOutputs().get(0);
		MoneyPlayer p = player(99, true, "druidic ritual");

		MoneyOption unknown = MoneyRanker.evaluate(herbs, p, null);
		assertFalse(unknown.isVolumesKnown());
		assertFalse(unknown.isHardToSell());

		MoneyOption dead = MoneyRanker.evaluate(herbs, p, null, Map.of(clean.getId(), 10L));
		assertTrue(dead.isHardToSell());
		assertFalse(dead.isGood());

		MoneyOption slow = MoneyRanker.evaluate(herbs, p, null, Map.of(clean.getId(), 20_000L));
		assertFalse(slow.isHardToSell());
		assertTrue(slow.getMarketWarnings().toString(), slow.getMarketWarnings().stream()
			.anyMatch(w -> w.startsWith("Sells slowly")));

		MoneyOption liquid = MoneyRanker.evaluate(herbs, p, null, Map.of(clean.getId(), 5_000_000L));
		assertTrue(liquid.getMarketWarnings().stream().noneMatch(w -> w.startsWith("Sells slowly")));
	}

	@Test
	public void buyLimitWarns()
	{
		// 5,000 grimy ranarr an hour is far over the 4-hour buy limit
		MoneyOption o = MoneyRanker.evaluate(method("Cleaning grimy ranarr weed"), player(99, true, "druidic ritual"),
			null);
		assertTrue(o.getMarketWarnings().toString(), o.getMarketWarnings().stream()
			.anyMatch(w -> w.startsWith("GE buy limit")));
	}

	private static com.skillinghelper.engine.PriceLookup prices(int a, int pa, int b, int pb)
	{
		return id -> id == a ? pa : id == b ? pb : 0;
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
			if (o.isGood())
			{
				assertTrue(o.getProfitPerHour() <= last);
				last = o.getProfitPerHour();
			}
		}
		long f2p = ranked.stream().filter(MoneyOption::isAvailable).count();
		assertTrue("a level 30 F2P account should have some methods: " + f2p, f2p > 5);
		assertTrue(ranked.stream().filter(MoneyOption::isAvailable).noneMatch(o -> o.getMethod().isMembers()));
	}

	private static MoneyGuide.Player.PlayerBuilder at(WorldPoint wp)
	{
		return MoneyGuide.Player.builder().location(wp).members(true);
	}

	@Test
	public void guideBuysThenBanksThenTravelsThenWorks()
	{
		MoneyMethod snape = method("Collecting snape grass");
		MoneyOption o = MoneyRanker.evaluate(snape, player(99, true, "the fremennik trials", "lunar diplomacy"), null);
		WorldPoint ge = new WorldPoint(3165, 3487, 0);
		Set<Integer> runes = Set.of(ItemID.ASTRALRUNE, ItemID.LAWRUNE);

		// Bank not seen yet: open it first
		MoneyStep open = MoneyGuide.resolve(o, at(ge).stackable(runes).build());
		assertEquals(MoneyStep.Kind.OPEN_BANK, open.getKind());
		assertTrue(open.getObjects().contains("Bank booth"));

		// Bank seen without the runes: buy them at the GE
		MoneyStep buy = MoneyGuide.resolve(o, at(ge).stackable(runes).bank(Map.of()).build());
		assertEquals(MoneyStep.Kind.BUY, buy.getKind());
		assertTrue(buy.getNpcs().contains("Grand Exchange Clerk"));

		// Runes in the bank and the bank open: take out an hour's worth, exactly
		MoneyStep withdraw = MoneyGuide.resolve(o, at(ge).stackable(runes).bankOpen(true)
			.bank(Map.of(ItemID.ASTRALRUNE, 1000, ItemID.LAWRUNE, 1000)).build());
		assertEquals(MoneyStep.Kind.WITHDRAW, withdraw.getKind());
		assertEquals(Integer.valueOf(76), withdraw.getWithdraw().get(ItemID.ASTRALRUNE));
		assertEquals(Integer.valueOf(152), withdraw.getWithdraw().get(ItemID.LAWRUNE));

		Map<Integer, Integer> carried = new HashMap<>(withdraw.getWithdraw());
		MoneyStep travel = MoneyGuide.resolve(o, at(ge).stackable(runes).carried(carried).build());
		assertEquals(MoneyStep.Kind.TRAVEL, travel.getKind());
		assertEquals(snape.getSpot(), travel.getDestination());

		WorldPoint spot = snape.getSpot().getPoint();
		MoneyStep work = MoneyGuide.resolve(o, at(spot).stackable(runes).carried(Map.of(ItemID.LAWRUNE, 10))
			.suppliesDone(true).build());
		assertEquals(MoneyStep.Kind.WORK, work.getKind());
		assertTrue(work.getLines().size() > 1);

		// Full of snape grass: bank it
		MoneyStep deposit = MoneyGuide.resolve(o, at(spot).stackable(runes).inventoryFull(true)
			.carried(Map.of(ItemID.SNAPE_GRASS, 27)).suppliesDone(true).build());
		assertEquals(MoneyStep.Kind.DEPOSIT, deposit.getKind());
	}

	@Test
	public void processingFillsTheInventoryAndRestocks()
	{
		MoneyMethod herbs = method("Cleaning grimy ranarr weed");
		MoneyOption o = MoneyRanker.evaluate(herbs, player(99, true, "druidic ritual"), null);
		WorldPoint ge = herbs.getSpot().getPoint();
		Map<Integer, Integer> bank = Map.of(ItemID.UNIDENTIFIED_RANARR, 5000);

		MoneyStep withdraw = MoneyGuide.resolve(o, at(ge).bankOpen(true).bank(bank).build());
		assertEquals(MoneyStep.Kind.WITHDRAW, withdraw.getKind());
		assertEquals(Integer.valueOf(28), withdraw.getWithdraw().get(ItemID.UNIDENTIFIED_RANARR));

		MoneyStep work = MoneyGuide.resolve(o, at(ge).bank(bank).suppliesDone(true)
			.carried(Map.of(ItemID.UNIDENTIFIED_RANARR, 28)).build());
		assertEquals(MoneyStep.Kind.WORK, work.getKind());
		assertTrue(work.getUse().contains(ItemID.UNIDENTIFIED_RANARR));

		// All cleaned: back to the bank for more, even though the supplies were done once
		MoneyStep restock = MoneyGuide.resolve(o, at(ge).bank(bank).suppliesDone(true).inventoryFull(true)
			.carried(Map.of(ItemID.RANARR_WEED, 28)).build());
		assertEquals(MoneyStep.Kind.OPEN_BANK, restock.getKind());
	}

	@Test
	public void shopMethodsOutlineTheShopkeeper()
	{
		MoneyMethod zaff = method("Buying battlestaves from Zaff");
		assertEquals("Zaff", zaff.getNpc());
		assertTrue(zaff.isRecurring());
		MoneyOption o = MoneyRanker.evaluate(zaff, player(99, true), null);
		MoneyStep work = MoneyGuide.resolve(o, at(zaff.getSpot().getPoint()).suppliesDone(true).build());
		assertTrue(work.getNpcs().contains("Zaff"));

		// Needs the coins first
		MoneyStep coins = MoneyGuide.resolve(o, at(zaff.getSpot().getPoint()).bankOpen(true)
			.bank(Map.of(ItemID.COINS, 2_000_000)).build());
		assertEquals(Integer.valueOf(840_000), coins.getWithdraw().get(ItemID.COINS));
	}

	@Test
	public void lockedMethodSaysWhy()
	{
		MoneyOption o = MoneyRanker.evaluate(method("Collecting snape grass"), player(1, true), null);
		MoneyStep step = MoneyGuide.resolve(o, at(null).build());
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
