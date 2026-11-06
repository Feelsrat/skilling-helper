package com.skillinghelper.quests;

import com.skillinghelper.quests.data.OrderRow;
import com.skillinghelper.quests.data.QuestData;
import com.skillinghelper.quests.data.Route;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class QuestPlannerTest
{
	private static QuestPlayer.QuestPlayerBuilder player(int level, Map<String, QuestState> quests)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, level);
		}
		return QuestPlayer.builder()
			.quests(quests)
			.levels(levels)
			.boosted(levels)
			.combatLevel(level)
			.members(true)
			.claimedDiaries(Set.of())
			.ticked(Set.of())
			.questHelper(true)
			.questHelperAutoStart(true)
			.ignored(Set.of());
	}

	@Test
	public void newAccountStartsWithCooksAssistant()
	{
		Map<String, QuestState> q = new HashMap<>();
		q.put("Learning the Ropes", QuestState.FINISHED);
		QuestPlayer p = player(1, q).build();
		List<QuestPlanner.RowState> states = QuestPlanner.evaluate(Route.MAIN, p, Set.of());
		QuestStep step = QuestPlanner.step(states, null, p, true);
		assertEquals("Cook's Assistant", step.getTitle());
		assertEquals(QuestStep.Kind.START, step.getKind());
		assertEquals("Cook's Assistant", step.getQuestListName());
		assertNotNull(step.getDestination());
		assertTrue(step.getLines().stream().anyMatch(l -> l.contains("Start Quest Helper")));
	}

	@Test
	public void lockedQuestsListWhatsMissing()
	{
		QuestPlayer p = player(1, new HashMap<>()).build();
		OrderRow mm = QuestData.route(Route.MAIN).stream().filter(r -> r.getRef().equals("Monkey Madness I"))
			.findFirst().orElseThrow();
		List<String> missing = QuestPlanner.missing(mm, p);
		assertTrue(missing.contains("Finish The Grand Tree"));
		OrderRow dt = QuestData.route(Route.MAIN).stream().filter(r -> r.getRef().equals("Desert Treasure I"))
			.findFirst().orElseThrow();
		assertTrue(QuestPlanner.missing(dt, p).stream().anyMatch(m -> m.startsWith("53 Thieving")));
	}

	@Test
	public void freeWorldSkipsMembersQuests()
	{
		Map<String, QuestState> q = new HashMap<>();
		q.put("Learning the Ropes", QuestState.FINISHED);
		QuestPlayer p = player(1, q).members(false).build();
		List<QuestPlanner.RowState> states = QuestPlanner.evaluate(Route.MAIN, p, Set.of());
		for (QuestPlanner.RowState s : states)
		{
			if (s.getStatus() == QuestPlanner.Status.READY && s.getRow().getKind() != OrderRow.Kind.ACTIVITY)
			{
				assertTrue(s.getRow().getRef(), !QuestData.quest(s.getRow().getRef()).isMembers());
			}
		}
	}

	@Test
	public void startedQuestComesFirstAndNoQuestHelperWarns()
	{
		Map<String, QuestState> q = new HashMap<>();
		q.put("Learning the Ropes", QuestState.FINISHED);
		q.put("Sheep Shearer", QuestState.IN_PROGRESS);
		QuestPlayer p = player(1, q).questHelper(false).build();
		QuestStep step = QuestPlanner.step(QuestPlanner.evaluate(Route.MAIN, p, Set.of()), null, p, true);
		// Cook's Assistant is first on the route and ready, so it stays first; Sheep Shearer shows as started
		assertEquals("Cook's Assistant", step.getTitle());
		assertTrue(step.getLines().stream().anyMatch(l -> l.contains("isn't installed")));
		QuestStep picked = QuestPlanner.step(QuestPlanner.evaluate(Route.MAIN, p, Set.of()), "QUEST:Sheep Shearer",
			p, true);
		assertEquals(QuestStep.Kind.CONTINUE, picked.getKind());
	}

	@Test
	public void ignoredQuestsAreNeverNext()
	{
		Map<String, QuestState> q = new HashMap<>();
		q.put("Learning the Ropes", QuestState.FINISHED);
		QuestPlayer p = player(1, q).ignored(Set.of("Cook's Assistant")).build();
		List<QuestPlanner.RowState> states = QuestPlanner.evaluate(Route.MAIN, p, Set.of());
		assertTrue(states.stream().anyMatch(s -> s.getRow().getRef().equals("Cook's Assistant")
			&& s.getStatus() == QuestPlanner.Status.IGNORED));
		assertEquals("Sheep Shearer", QuestPlanner.step(states, null, p, true).getTitle());
	}

	@Test
	public void questHelperTakesOverOrOffersAutoStart()
	{
		Map<String, QuestState> q = new HashMap<>();
		q.put("Learning the Ropes", QuestState.FINISHED);
		QuestPlayer auto = player(1, q).build();
		QuestStep a = QuestPlanner.step(QuestPlanner.evaluate(Route.MAIN, auto, Set.of()), null, auto, true);
		assertTrue(a.getLines().stream().anyMatch(l -> l.contains("takes over by itself")));
		assertTrue(!a.isOfferAutoStart());
		assertEquals("Cook", a.getNpc());
		QuestPlayer manual = player(1, q).questHelperAutoStart(false).build();
		QuestStep m = QuestPlanner.step(QuestPlanner.evaluate(Route.MAIN, manual, Set.of()), null, manual, true);
		assertTrue(m.isOfferAutoStart());
	}

	@Test
	public void everythingDoneFinishes()
	{
		Map<String, QuestState> q = new HashMap<>();
		QuestData.quests().keySet().forEach(n -> q.put(n, QuestState.FINISHED));
		java.util.Set<String> claimed = new java.util.HashSet<>();
		for (com.skillinghelper.diary.data.DiaryRegion r : com.skillinghelper.diary.data.DiaryRegion.values())
		{
			for (com.skillinghelper.diary.data.DiaryTier t : com.skillinghelper.diary.data.DiaryTier.values())
			{
				claimed.add(r.name() + "." + t.name());
			}
		}
		java.util.Set<String> ticked = new java.util.HashSet<>();
		QuestData.route(Route.MAIN).forEach(r -> ticked.add(r.getRef()));
		QuestPlayer p = player(99, q).claimedDiaries(claimed).ticked(ticked).build();
		QuestStep step = QuestPlanner.step(QuestPlanner.evaluate(Route.MAIN, p, Set.of()), null, p, true);
		assertEquals(QuestStep.Kind.FINISHED, step.getKind());
	}

	@Test
	public void everyRowMakesAStep()
	{
		QuestPlayer p = player(99, new HashMap<>()).allowWilderness(true).build();
		for (Route r : Route.values())
		{
			List<QuestPlanner.RowState> states = QuestPlanner.evaluate(r, p, Set.of());
			for (OrderRow row : QuestData.route(r))
			{
				QuestStep s = QuestPlanner.step(states, row.key(), p, true);
				assertEquals(row.key(), s.getRowKey());
				assertTrue(!s.getLines().isEmpty());
			}
		}
	}
}
