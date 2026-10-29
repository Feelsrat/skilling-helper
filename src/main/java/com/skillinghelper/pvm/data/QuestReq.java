package com.skillinghelper.pvm.data;

import lombok.Value;
import net.runelite.api.Quest;

/**
 * A quest needed to reach a piece of content.
 */
@Value
public class QuestReq
{
	Quest quest;
	/**
	 * True when having started the quest is enough (e.g. Regicide's first part opens the way to Zul-Andra).
	 */
	boolean startedIsEnough;

	public static QuestReq done(Quest quest)
	{
		return new QuestReq(quest, false);
	}

	public static QuestReq started(Quest quest)
	{
		return new QuestReq(quest, true);
	}
}
