package com.skillinghelper.quests;

import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryTier;
import com.skillinghelper.quests.data.OrderRow;
import com.skillinghelper.quests.data.QuestData;
import com.skillinghelper.quests.data.QuestInfo;
import com.skillinghelper.quests.data.Route;
import com.skillinghelper.travel.TravelPick;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;

/**
 * Works out where the player is on the wiki's optimal quest route: what's done, what they can do now, what's
 * locked and why, and the next thing to do. Pure logic, so tests can run it.
 */
public final class QuestPlanner
{
	/**
	 * Quests that take you into the Wilderness even though they start outside it.
	 */
	private static final Set<String> WILDERNESS_QUESTS = Set.of("Enter the Abyss", "Curse of the Empty Lord");
	private static final int MAX_REWARD_LINES = 4;
	private static final int MAX_REWARD_LENGTH = 110;

	public enum Status
	{
		DONE,
		/**
		 * A quest that's started but not finished.
		 */
		STARTED,
		READY,
		/**
		 * Requirements missing (listed).
		 */
		LOCKED,
		/**
		 * In the Wilderness and the player hasn't opted in.
		 */
		WILDERNESS,
		/**
		 * Skipped by the player this session.
		 */
		SKIPPED
	}

	@Value
	public static class RowState
	{
		OrderRow row;
		Status status;
		List<String> missing;
		boolean wilderness;
	}

	private QuestPlanner()
	{
	}

	public static List<RowState> evaluate(Route route, QuestPlayer p, Set<String> skipped)
	{
		List<RowState> out = new ArrayList<>();
		for (OrderRow row : QuestData.route(route))
		{
			out.add(evaluate(row, p, skipped));
		}
		return out;
	}

	static RowState evaluate(OrderRow row, QuestPlayer p, Set<String> skipped)
	{
		boolean wild = isWilderness(row);
		if (done(row, p))
		{
			return new RowState(row, Status.DONE, List.of(), wild);
		}
		List<String> missing = missing(row, p);
		Status status;
		if (!missing.isEmpty())
		{
			status = Status.LOCKED;
		}
		else if (wild && !p.isAllowWilderness())
		{
			status = Status.WILDERNESS;
		}
		else if (skipped.contains(row.key()))
		{
			status = Status.SKIPPED;
		}
		else if ((row.getKind() == OrderRow.Kind.QUEST || row.getKind() == OrderRow.Kind.PARTIAL)
			&& p.state(row.getRef()) == QuestState.IN_PROGRESS)
		{
			status = Status.STARTED;
		}
		else
		{
			status = Status.READY;
		}
		return new RowState(row, status, missing, wild);
	}

	public static boolean done(OrderRow row, QuestPlayer p)
	{
		switch (row.getKind())
		{
			case QUEST:
				return p.state(row.getRef()) == QuestState.FINISHED;
			case PARTIAL:
				return p.state(row.getRef()) != QuestState.NOT_STARTED;
			case DIARY:
				return diaryDone(row.getRef(), p);
			default:
				return p.getTicked() != null && p.getTicked().contains(row.getRef());
		}
	}

	private static boolean diaryDone(String ref, QuestPlayer p)
	{
		Set<String> claimed = p.getClaimedDiaries();
		if (claimed == null)
		{
			return false;
		}
		String[] parts = ref.split("\\.");
		if ("ALL".equals(parts[0]))
		{
			for (DiaryRegion r : DiaryRegion.values())
			{
				if (!claimed.contains(r.name() + "." + parts[1]))
				{
					return false;
				}
			}
			return true;
		}
		return claimed.contains(ref);
	}

	/**
	 * What the player still lacks to do this row, empty when they can do it now.
	 */
	public static List<String> missing(OrderRow row, QuestPlayer p)
	{
		List<String> out = new ArrayList<>();
		if (row.getKind() != OrderRow.Kind.QUEST && row.getKind() != OrderRow.Kind.PARTIAL)
		{
			if (!p.isMembers() && row.getKind() == OrderRow.Kind.DIARY)
			{
				out.add("A members world");
			}
			return out;
		}
		QuestInfo q = QuestData.quest(row.getRef());
		if (q == null)
		{
			return out;
		}
		if (q.isMembers() && !p.isMembers())
		{
			out.add("A members world");
		}
		if (p.state(q.getName()) == QuestState.IN_PROGRESS)
		{
			// Already started: the game let them in, carry on
			return out;
		}
		for (String pre : q.getQuests())
		{
			if (p.state(pre) != QuestState.FINISHED)
			{
				out.add("Finish " + shortName(pre));
			}
		}
		for (String pre : q.getQuestsStarted())
		{
			if (p.state(pre) == QuestState.NOT_STARTED)
			{
				out.add("Start " + shortName(pre));
			}
		}
		// The first part of a quest often needs less than the whole thing: only prerequisites count for those
		if (row.getKind() == OrderRow.Kind.PARTIAL)
		{
			return out;
		}
		for (QuestInfo.SkillReq r : q.getSkills())
		{
			if (r.getSkill() == null)
			{
				if (p.getCombatLevel() < r.getLevel())
				{
					out.add(r.getLevel() + " Combat (you're " + p.getCombatLevel() + ")");
				}
				continue;
			}
			int real = p.level(r.getSkill());
			if (real >= r.getLevel() || r.isBoostable() && p.boostedLevel(r.getSkill()) >= r.getLevel())
			{
				continue;
			}
			out.add(r.label() + " (you have " + real + (r.isBoostable() ? ", boostable" : "") + ")");
		}
		if (q.getQuestPoints() > 0 && p.getQuestPoints() < q.getQuestPoints())
		{
			out.add(q.getQuestPoints() + " Quest points (you have " + p.getQuestPoints() + ")");
		}
		return out;
	}

	public static boolean isWilderness(OrderRow row)
	{
		if (row.getKind() == OrderRow.Kind.DIARY)
		{
			return row.getRef().startsWith(DiaryRegion.WILDERNESS.name() + ".");
		}
		if (WILDERNESS_QUESTS.contains(row.getRef()))
		{
			return true;
		}
		QuestInfo q = QuestData.quest(row.getRef());
		return q != null && q.getStartPoint() != null && Wilderness.contains(q.getStartPoint());
	}

	/**
	 * The row to do next: the picked one if it isn't done, else a started quest, else the first row the player can
	 * do, in route order. Null when nothing can be done right now.
	 */
	public static RowState next(List<RowState> states, String picked)
	{
		if (picked != null)
		{
			for (RowState s : states)
			{
				if (s.getRow().key().equals(picked) && s.getStatus() != Status.DONE)
				{
					return s;
				}
			}
		}
		for (RowState s : states)
		{
			if (s.getStatus() == Status.STARTED || s.getStatus() == Status.READY)
			{
				return s;
			}
		}
		return null;
	}

	/**
	 * The first row on the route that's still locked, the thing to train or unlock for. Null if none.
	 */
	public static RowState firstLocked(List<RowState> states)
	{
		for (RowState s : states)
		{
			if (s.getStatus() == Status.LOCKED)
			{
				return s;
			}
		}
		return null;
	}

	// ---- The step ----

	public static QuestStep step(List<RowState> states, String picked, QuestPlayer p, boolean drawRoute)
	{
		RowState next = next(states, picked);
		if (next == null)
		{
			RowState locked = firstLocked(states);
			if (locked == null)
			{
				return QuestStep.builder()
					.kind(QuestStep.Kind.FINISHED)
					.title("Route done")
					.line("✔ You've done everything on this route you can do. Turn on Wilderness rows, or "
						+ "un-skip rows, to see what's left.")
					.build();
			}
			QuestStep.QuestStepBuilder b = QuestStep.builder()
				.kind(QuestStep.Kind.TRAIN)
				.rowKey(locked.getRow().key())
				.title("Train for " + label(locked.getRow()))
				.line("Nothing on the route can be done right now. The next row needs:");
			for (String m : locked.getMissing())
			{
				b.line("• " + m);
			}
			b.line("Tip: Skilling Helper (the pickaxe icon) can train a skill step by step. Boosts count for "
				+ "skills marked boostable.");
			return b.build();
		}
		OrderRow row = next.getRow();
		switch (row.getKind())
		{
			case DIARY:
				return diaryStep(next);
			case ACTIVITY:
				return activityStep(next);
			default:
				return questStep(next, p, drawRoute);
		}
	}

	private static QuestStep questStep(RowState s, QuestPlayer p, boolean drawRoute)
	{
		OrderRow row = s.getRow();
		QuestInfo q = QuestData.quest(row.getRef());
		boolean partial = row.getKind() == OrderRow.Kind.PARTIAL;
		boolean started = s.getStatus() == Status.STARTED;
		String name = q.shortName();
		String listName = q.listName();
		boolean rfd = !listName.equals(q.getName());
		QuestStep.QuestStepBuilder b = QuestStep.builder()
			.kind(started ? QuestStep.Kind.CONTINUE : QuestStep.Kind.START)
			.rowKey(row.key())
			.title((partial ? "Start " : "") + name)
			.wikiPage(q.getName() + "/Quick guide")
			.questListName(listName);
		if (partial)
		{
			b.line("Only the first part for now: the rest of " + name + " comes later on the route. The wiki's "
				+ "tips below say how far to go.");
		}
		b.line(q.getDifficulty() + ", " + q.getLength().toLowerCase() + (q.isMembers() ? ", members" : ", free")
			+ (q.getCombat() > 0 ? ". Combat " + q.getCombat() + " recommended" : "") + ".");
		for (String m : s.getMissing())
		{
			b.line("✘ Still needed: " + m);
		}
		if (s.isWilderness())
		{
			b.line("⚠ " + Wilderness.WARNING);
		}
		String startQh = rfd
			? "Right-click " + QuestData.RFD + " and choose \"Start Quest Helper\" on the " + name.substring(5)
			+ " line."
			: "Right-click " + listName + " and choose \"Start Quest Helper\".";
		if (p.isQuestHelper())
		{
			if (started)
			{
				b.line("1. You've started this quest. Keep following Quest Helper's steps.");
				b.line("2. If its steps aren't showing, open the Quest list (the blue quest icon in your side tabs), "
					+ "find " + listName + " (outlined) and " + Character.toLowerCase(startQh.charAt(0))
					+ startQh.substring(1));
				b.line("3. This guide moves on by itself when the quest is done.");
			}
			else
			{
				b.line("1. Open the Quest list: click the blue quest icon in your side tabs. " + listName
					+ " is outlined there.");
				b.line("2. " + startQh);
				b.line("3. Quest Helper's panel opens with every step and a list of the items to bring, ticked "
					+ "when you have them. Bank for those first.");
				b.line("4. Follow Quest Helper from there. This guide moves on by itself when the quest is done.");
			}
		}
		else
		{
			b.line("⚠ Quest Helper isn't installed or is turned off, so there are no click-by-click steps. Get "
				+ "\"Quest Helper\" from the Plugin Hub (the wrench icon, then Plugin Hub) for them.");
			if (!started)
			{
				b.line("Until then: get the items below, go to the start (route drawn), then follow the wiki guide "
					+ "(button below).");
			}
			else
			{
				b.line("Until then: follow the wiki guide (button below). This guide moves on when the quest is done.");
			}
		}
		if (!started)
		{
			if (!q.getStart().isEmpty())
			{
				b.line("Where: " + q.getStart());
			}
			if (q.getStartPoint() != null)
			{
				b.destination(q.getStartPoint());
				if (drawRoute)
				{
					b.line("The way to the start is drawn on your screen and minimap.");
				}
			}
			if (!q.getStartNpc().isEmpty())
			{
				b.npc(q.getStartNpc());
			}
			if (!q.getItems().isEmpty() && !partial)
			{
				b.line("Bring: " + String.join(", ", q.getItems()));
			}
			for (TravelPick t : p.getTravel())
			{
				b.line(t.isCarried() ? t.carriedLine() : t.tipLine());
				if (!t.isCarried() && !t.isCoins())
				{
					b.travelItem(t.getItemId());
				}
			}
		}
		if (!q.getKills().isEmpty())
		{
			b.line("Fights: " + String.join(", ", q.getKills()));
		}
		notes(b, row);
		int shown = 0;
		for (String r : q.getRewards())
		{
			if (shown++ >= MAX_REWARD_LINES)
			{
				break;
			}
			b.line("+ " + (r.length() > MAX_REWARD_LENGTH ? r.substring(0, MAX_REWARD_LENGTH - 1).trim() + "…" : r));
		}
		return b.build();
	}

	private static QuestStep diaryStep(RowState s)
	{
		OrderRow row = s.getRow();
		String[] parts = row.getRef().split("\\.");
		DiaryTier tier = DiaryTier.valueOf(parts[1]);
		QuestStep.QuestStepBuilder b = QuestStep.builder()
			.kind(QuestStep.Kind.DIARY)
			.rowKey(row.key())
			.title(label(row))
			.wikiPage("Achievement_Diary");
		if ("ALL".equals(parts[0]))
		{
			b.line("1. Finish and claim every region's " + tier.getLabel() + " diary.");
			b.line("2. Open Achievement Diary Guides (the book icon): pick a region, pick " + tier.getLabel()
				+ " and press \"Start guide\". It walks you through every task.");
		}
		else
		{
			DiaryRegion region = DiaryRegion.valueOf(parts[0]);
			b.diary(row.getRef());
			b.wikiPage(region.getWikiPage());
			b.line("1. Press \"Show in Achievement Diary Guides\" below: it picks " + region.getLabel() + " "
				+ tier.getLabel() + " there.");
			b.line("2. Open Achievement Diary Guides (the book icon) and press \"Start guide\". It walks you "
				+ "through every task, then to the person who hands out the reward.");
			b.line("3. This guide moves on once you've claimed the reward.");
		}
		if (s.isWilderness())
		{
			b.line("⚠ " + Wilderness.WARNING);
		}
		notes(b, row);
		return b.build();
	}

	private static QuestStep activityStep(RowState s)
	{
		OrderRow row = s.getRow();
		QuestStep.QuestStepBuilder b = QuestStep.builder()
			.kind(QuestStep.Kind.ACTIVITY)
			.rowKey(row.key())
			.title(label(row))
			.wikiPage(row.getRef());
		Activity how = ACTIVITIES.get(row.getRef());
		if (how != null)
		{
			int n = 1;
			for (String l : how.steps)
			{
				b.line(n++ + ". " + l);
			}
			if (how.at != null)
			{
				b.destination(how.at);
			}
		}
		b.line("The game doesn't track this one as a quest: press \"Mark as done\" when you've done it.");
		notes(b, row);
		return b.build();
	}

	/**
	 * Beginner steps for the route's non-quest rows that need more than the wiki's route tips.
	 */
	private static final class Activity
	{
		final WorldPoint at;
		final List<String> steps;

		Activity(WorldPoint at, String... steps)
		{
			this.at = at;
			this.steps = List.of(steps);
		}
	}

	private static final Map<String, Activity> ACTIVITIES = Map.of(
		"Stronghold of Security", new Activity(new WorldPoint(3081, 3421, 0),
			"Bring food. Go to the hole in the middle of Barbarian Village (route drawn) and climb down.",
			"Each door asks an account security question: the safe answer is never to share your password, bank "
				+ "PIN or account. Answer it to go through.",
			"Each of the 4 floors has a treasure room at the far end: search the Gift of Peace, Grain of Plenty and "
				+ "Box of Health for 2,000, 3,000 and 5,000 coins plus 2 emotes.",
			"Search the Cradle of Life on the bottom floor and pick Fancy boots or Fighting boots.",
			"Climb the ladders back up, or teleport out."),
		"Natural history quiz", new Activity(new WorldPoint(3256, 3448, 0),
			"Go to the Varrock Museum (route drawn) and take the stairs down to the basement.",
			"Talk to Orlando Smith, then go round the display cases. Click each plaque and answer its 3 questions "
				+ "(the answers are on the plaque).",
			"Talk to Orlando Smith again when all are done: 1,000 Slayer and Hunter xp, and Museum kudos."),
		"Knight Waves Training Grounds", new Activity(new WorldPoint(2758, 3504, 0),
			"Go to Camelot Castle (route drawn) and climb to the top floor.",
			"Talk to the Squire, then fight the 8 knights one after another. Only melee works: bring a weapon, "
				+ "armour and food.",
			"Beat all 8 to unlock the Chivalry and Piety prayers (they also need 65/70 Defence and 60/70 Prayer)."));

	private static void notes(QuestStep.QuestStepBuilder b, OrderRow row)
	{
		for (String n : row.getNotes())
		{
			b.line(n.startsWith("  ") ? "- " + n.trim() : n.startsWith("First: ") ? n : "Tip: " + n);
		}
	}

	/**
	 * How a row is named in the panel.
	 */
	public static String label(OrderRow row)
	{
		switch (row.getKind())
		{
			case DIARY:
			{
				String[] parts = row.getRef().split("\\.");
				DiaryTier tier = DiaryTier.valueOf(parts[1]);
				if ("ALL".equals(parts[0]))
				{
					return "All " + tier.getLabel() + " diaries";
				}
				return DiaryRegion.valueOf(parts[0]).getLabel() + " " + tier.getLabel() + " diary";
			}
			case PARTIAL:
				return shortName(row.getRef()) + " (first part)";
			case QUEST:
				return shortName(row.getRef());
			default:
				return row.getLabel().replaceFirst("^Unlock: ", "");
		}
	}

	private static String shortName(String quest)
	{
		QuestInfo q = QuestData.quest(quest);
		return q == null ? quest : q.shortName();
	}
}
