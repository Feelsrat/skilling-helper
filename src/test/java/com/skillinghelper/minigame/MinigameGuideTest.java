package com.skillinghelper.minigame;

import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.minigame.data.Minigame;
import com.skillinghelper.minigame.data.Minigames;
import com.skillinghelper.minigame.data.Reward;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MinigameGuideTest
{
	private static final WorldPoint VARROCK = new WorldPoint(3212, 3428, 0);

	private static DiaryPlayer.DiaryPlayerBuilder player(int level)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, level);
		}
		Map<Quest, QuestState> quests = new HashMap<>();
		for (Quest q : Quest.values())
		{
			quests.put(q, QuestState.FINISHED);
		}
		return DiaryPlayer.builder()
			.location(VARROCK)
			.levels(levels)
			.combatLevel(level)
			.quests(quests)
			.members(true)
			.bank(new HashMap<>());
	}

	@Test
	public void lockedUntilRequirementsAreMet()
	{
		Minigame vm = Minigames.byId("volcanic_mine");
		DiaryStep s = MinigameGuide.resolve(vm, player(40).build(), false);
		assertEquals(DiaryStep.Kind.LOCKED, s.getKind());
		assertTrue(s.getLines().stream().anyMatch(l -> l.contains("50 Mining (you have 40)")));
	}

	@Test
	public void soulWarsChecksTotalAndCombatLevel()
	{
		Minigame sw = Minigames.byId("soul_wars");
		assertFalse(MinigameGuide.missing(sw, player(20).build()).isEmpty());
		assertTrue(MinigameGuide.missing(sw, player(40).build()).isEmpty());
	}

	@Test
	public void suppliesThenTravelThenPlay()
	{
		Minigame vm = Minigames.byId("volcanic_mine");
		DiaryStep bank = MinigameGuide.resolve(vm, player(70).build(), false);
		assertTrue(bank.getKind() == DiaryStep.Kind.BUY || bank.getKind() == DiaryStep.Kind.OPEN_BANK);
		assertFalse(bank.getTitle().contains("task"));

		Map<Integer, Integer> carried = Map.of(ItemID.RUNE_PICKAXE, 1, ItemID.FOSSIL_NUMULITE, 30);
		DiaryStep walk = MinigameGuide.resolve(vm, player(70).carried(carried).build(), false);
		assertEquals(DiaryStep.Kind.TRAVEL, walk.getKind());
		assertEquals(vm.getLocation(), walk.getDestination());

		DiaryStep there = MinigameGuide.resolve(vm, player(70).carried(carried)
			.location(vm.getLocation().getPoint()).build(), false);
		assertEquals(DiaryStep.Kind.DO, there.getKind());
		assertTrue(there.getNpcs().contains("Petrified Pete"));
	}

	@Test
	public void inAnInstanceTheGameStageShowsWithoutWalking()
	{
		Minigame vm = Minigames.byId("volcanic_mine");
		DiaryStep s = MinigameGuide.resolve(vm, player(70).build(), true);
		assertEquals(DiaryStep.Kind.DO, s.getKind());
		assertEquals("Mine until the boulder", s.getTitle());
	}

	@Test
	public void castleWarsArenaIsTheGameStage()
	{
		Minigame cw = Minigames.byId("castle_wars");
		DiaryStep s = MinigameGuide.resolve(cw, player(50).location(new WorldPoint(2400, 3100, 0)).build(), false);
		assertEquals("Play the game", s.getTitle());
	}

	@Test
	public void rewardsInTheBankCountAsOwned()
	{
		Minigame mlm = Minigames.byId("motherlode");
		Reward coalBag = mlm.getRewards().stream().filter(r -> r.getName().equals("Coal bag")).findFirst().get();
		assertFalse(MinigameGuide.owned(coalBag, player(50).build()));
		assertTrue(MinigameGuide.owned(coalBag, player(50).bank(Map.of(ItemID.COAL_BAG, 1)).build()));
	}

	@Test
	public void unlocksAreReadFromVarbits()
	{
		Minigame mta = Minigames.byId("mage_training_arena");
		Reward b2p = mta.getRewards().get(0);
		assertFalse(MinigameGuide.owned(b2p, player(50).build()));
		assertTrue(MinigameGuide.owned(b2p, player(50)
			.varbit(id -> id == VarbitID.MAGICTRAINING_BONESPEACHES ? 1 : 0).build()));
	}

	@Test
	public void affordableRewardsUseTheBalance()
	{
		Minigame mlm = Minigames.byId("motherlode");
		DiaryPlayer rich = player(50).bank(Map.of(ItemID.MOTHERLODE_NUGGET, 100)).build();
		assertTrue(MinigameGuide.affordable(mlm, rich).stream().anyMatch(r -> r.getName().equals("Gem bag")));
		DiaryPlayer owns = player(50).bank(Map.of(ItemID.MOTHERLODE_NUGGET, 100, ItemID.GEM_BAG, 1)).build();
		assertFalse(MinigameGuide.affordable(mlm, owns).stream().anyMatch(r -> r.getName().equals("Gem bag")));
		assertEquals(java.util.List.of("100 golden nuggets"), MinigameGuide.balances(mlm, rich));
	}
}
