package com.skillinghelper.quests;

import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryTier;
import com.skillinghelper.quests.data.OrderRow;
import com.skillinghelper.quests.data.QuestData;
import com.skillinghelper.quests.data.QuestInfo;
import com.skillinghelper.quests.data.Route;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class QuestDataTest
{
	@Test
	public void everyQuestIsAGameQuest()
	{
		assertTrue(QuestData.quests().size() > 190);
		for (QuestInfo q : QuestData.quests().values())
		{
			assertNotNull("No game quest for " + q.getName(), QuestData.gameQuest(q.getName()));
		}
	}

	@Test
	public void prerequisitesAreKnownQuests()
	{
		for (QuestInfo q : QuestData.quests().values())
		{
			for (String pre : q.getQuests())
			{
				assertNotNull(q.getName() + " needs unknown " + pre, QuestData.quest(pre));
			}
			for (String pre : q.getQuestsStarted())
			{
				assertNotNull(q.getName() + " needs unknown " + pre, QuestData.quest(pre));
			}
		}
	}

	@Test
	public void routesAreWhole()
	{
		for (Route r : Route.values())
		{
			List<OrderRow> rows = QuestData.route(r);
			assertTrue(r + " is short: " + rows.size(), rows.size() > 200);
			Set<String> keys = new HashSet<>();
			for (OrderRow row : rows)
			{
				assertTrue(r + " repeats " + row.key(), keys.add(row.key()));
				switch (row.getKind())
				{
					case QUEST:
					case PARTIAL:
						assertNotNull(row.getRef(), QuestData.quest(row.getRef()));
						break;
					case DIARY:
						String[] parts = row.getRef().split("\\.");
						if (!"ALL".equals(parts[0]))
						{
							DiaryRegion.valueOf(parts[0]);
						}
						DiaryTier.valueOf(parts[1]);
						break;
					default:
						assertTrue(row.getLabel(), !row.getLabel().isEmpty());
				}
			}
		}
	}

	@Test
	public void requirementsParsed()
	{
		QuestInfo dt = QuestData.quest("Desert Treasure I");
		assertTrue(dt.getQuests().contains("The Dig Site"));
		assertTrue(dt.getSkills().stream().anyMatch(s -> s.label().equals("50 Firemaking") && s.isBoostable()));
		assertEquals(32, QuestData.quest("Dragon Slayer I").getQuestPoints());
		QuestInfo cook = QuestData.quest("Cook's Assistant");
		assertNotNull(cook.getStartPoint());
		assertEquals("Cook", cook.getStartNpc());
		assertEquals(QuestData.RFD, QuestData.quest("Recipe for Disaster/Freeing Evil Dave").listName());
	}
}
