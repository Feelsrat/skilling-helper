package com.skillinghelper.pvm;

import com.skillinghelper.pvm.data.ItemNeed;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.QuestReq;
import com.skillinghelper.pvm.data.StatReq;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.runelite.api.QuestState;

/**
 * How ready the player is for one piece of content, and what's still missing.
 */
@Value
public class Readiness
{
	/**
	 * Levels within this many of every recommendation count as "almost ready".
	 */
	static final int ALMOST_LEVELS = 5;

	@Getter
	@RequiredArgsConstructor
	public enum Status
	{
		DONE("Done"),
		READY("Ready"),
		ALMOST("Almost ready"),
		TRAIN("Train first"),
		LOCKED("Locked");

		private final String label;
	}

	PvmContent content;
	Status status;
	List<QuestReq> missingQuests;
	/**
	 * Required levels the player doesn't have yet: these keep them out.
	 */
	List<StatReq> missingLevels;
	/**
	 * Recommended levels the player doesn't have yet.
	 */
	List<StatReq> levelGaps;
	/**
	 * Required items the player doesn't own.
	 */
	List<ItemNeed> missingItems;
	int killCount;

	public static Readiness of(PvmContent c, PlayerView p)
	{
		List<QuestReq> quests = new ArrayList<>();
		for (QuestReq q : c.getQuests())
		{
			if (!questMet(q, p.quest(q.getQuest())))
			{
				quests.add(q);
			}
		}

		List<StatReq> locked = new ArrayList<>();
		List<StatReq> gaps = new ArrayList<>();
		int worstGap = 0;
		for (StatReq s : c.getStats())
		{
			int gap = s.getLevel() - p.level(s.getSkill());
			if (gap <= 0)
			{
				continue;
			}
			if (s.isRequired())
			{
				locked.add(s);
			}
			else
			{
				gaps.add(s);
				worstGap = Math.max(worstGap, gap);
			}
		}

		List<ItemNeed> items = new ArrayList<>();
		for (ItemNeed i : c.getItems())
		{
			if (i.isRequired() && i.isCheckable() && i.getIds().stream().noneMatch(p.getOwned()::contains))
			{
				items.add(i);
			}
		}

		int kc = p.killCount(c.getId());
		Status status;
		if (kc > 0 || p.getMarkedDone().contains(c.getId()))
		{
			status = Status.DONE;
		}
		else if (!quests.isEmpty() || !locked.isEmpty())
		{
			status = Status.LOCKED;
		}
		else if (worstGap > ALMOST_LEVELS)
		{
			status = Status.TRAIN;
		}
		else if (worstGap > 0 || !items.isEmpty())
		{
			status = Status.ALMOST;
		}
		else
		{
			status = Status.READY;
		}
		return new Readiness(c, status, quests, locked, gaps, items, kc);
	}

	static boolean questMet(QuestReq q, QuestState state)
	{
		return state == QuestState.FINISHED || (q.isStartedIsEnough() && state == QuestState.IN_PROGRESS);
	}

	/**
	 * The next thing to work on: the first piece of content on the ladder that isn't done yet, or null when
	 * everything is.
	 */
	public static Readiness next(List<Readiness> ladder)
	{
		return ladder.stream().filter(r -> r.getStatus() != Status.DONE).findFirst().orElse(null);
	}
}
