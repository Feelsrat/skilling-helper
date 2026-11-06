package com.skillinghelper.minigame;

import com.skillinghelper.data.Location;
import com.skillinghelper.diary.DiaryGuide;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.diary.data.DiaryStage;
import com.skillinghelper.minigame.data.Minigame;
import com.skillinghelper.minigame.data.Points;
import com.skillinghelper.minigame.data.Reward;
import com.skillinghelper.travel.TravelPick;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * Requirements, owned rewards and the next step for a minigame. The step logic matches the diary guides (same
 * player snapshot, supplies and step type): get what to bring, walk to the start, then follow the stages.
 */
public final class MinigameGuide
{
	private MinigameGuide()
	{
	}

	// ---- Requirements ----

	/**
	 * What the player still needs before they can play, e.g. "50 Mining (you have 43)". Empty when ready.
	 */
	public static List<String> missing(Minigame m, DiaryPlayer p)
	{
		List<String> out = new ArrayList<>();
		if (!p.isMembers())
		{
			out.add("A members world");
		}
		for (Map.Entry<Skill, Integer> e : m.getSkills().entrySet())
		{
			Skill s = e.getKey();
			boolean boostable = !m.getUnboostable().contains(s);
			int have = boostable ? p.boostedLevel(s) : p.level(s);
			if (have < e.getValue())
			{
				out.add(e.getValue() + " " + s.getName() + " (you have " + p.level(s) + ")"
					+ (boostable ? "" : ", no boosts"));
			}
		}
		if (m.getCombat() > 0 && p.getCombatLevel() < m.getCombat())
		{
			out.add("Combat level " + m.getCombat() + " (you have " + p.getCombatLevel() + ")");
		}
		if (m.getTotalLevel() > 0)
		{
			int total = 0;
			for (Skill s : Skill.values())
			{
				// Skip the old OVERALL entry without naming it (it's deprecated)
				total += "Overall".equals(s.getName()) ? 0 : p.level(s);
			}
			if (total < m.getTotalLevel())
			{
				out.add("Total level " + m.getTotalLevel() + " (you have " + total + ")");
			}
		}
		for (Quest q : m.getQuests())
		{
			if (p.quest(q) != QuestState.FINISHED)
			{
				out.add("Quest: " + q.getName());
			}
		}
		for (Quest q : m.getQuestsStarted())
		{
			if (p.quest(q) == QuestState.NOT_STARTED)
			{
				out.add("Start the quest " + q.getName());
			}
		}
		if (m.isWilderness() && !p.isAllowWilderness())
		{
			out.add("Wilderness turned off in the panel");
		}
		return out;
	}

	// ---- Rewards ----

	/**
	 * Whether the player has the reward (carried, worn, in the bank, or unlocked). False when it can't be checked.
	 */
	public static boolean owned(Reward r, DiaryPlayer p)
	{
		if (r.getUnlockVarbit() > 0 && p.getVarbit().applyAsInt(r.getUnlockVarbit()) > 0)
		{
			return true;
		}
		for (int id : r.getIds())
		{
			if (p.owned(id) > 0)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * How many of the minigame's rewards worth chasing the player has, out of the ones that can be checked.
	 */
	public static int[] ownedCount(Minigame m, DiaryPlayer p)
	{
		int have = 0;
		int total = 0;
		for (Reward r : m.getRewards())
		{
			if (r.isWorth() && r.checkable())
			{
				total++;
				have += owned(r, p) ? 1 : 0;
			}
		}
		return new int[]{have, total};
	}

	/**
	 * The player's balance in each of the minigame's currencies, e.g. "120 golden nuggets". Item currencies count
	 * the bank only once it's been seen.
	 */
	public static List<String> balances(Minigame m, DiaryPlayer p)
	{
		List<String> out = new ArrayList<>();
		for (Points pts : m.getPoints())
		{
			out.add(String.format("%,d", balance(pts, p)) + " " + pts.getLabel());
		}
		return out;
	}

	public static int balance(Points pts, DiaryPlayer p)
	{
		if (pts.getItemId() > 0)
		{
			return p.owned(pts.getItemId());
		}
		if (pts.getVarp() > 0)
		{
			return p.getVarp().applyAsInt(pts.getVarp());
		}
		return p.getVarbit().applyAsInt(pts.getVarbit());
	}

	/**
	 * Rewards worth chasing the player doesn't have yet and can afford now (only when the minigame has one
	 * currency, so the price is clear).
	 */
	public static List<Reward> affordable(Minigame m, DiaryPlayer p)
	{
		List<Reward> out = new ArrayList<>();
		if (m.getPoints().size() != 1)
		{
			return out;
		}
		int have = balance(m.getPoints().get(0), p);
		for (Reward r : m.getRewards())
		{
			if (r.isWorth() && r.getPrice() > 0 && have >= r.getPrice() && !owned(r, p))
			{
				out.add(r);
			}
		}
		return out;
	}

	// ---- The guide ----

	/**
	 * The next step for a minigame guided here.
	 *
	 * @param inInstance the player is in an instanced area (most minigame games are): never send them walking
	 */
	public static DiaryStep resolve(Minigame m, DiaryPlayer p, boolean inInstance)
	{
		List<String> missing = missing(m, p);
		if (!missing.isEmpty())
		{
			DiaryStep.DiaryStepBuilder b = step(DiaryStep.Kind.LOCKED, m).title("You can't play " + m.getName() + " yet");
			b.line("Still needed:");
			missing.forEach(x -> b.line("✘ " + x));
			return b.build();
		}

		WorldPoint me = p.getLocation();
		boolean inGame = inInstance || (m.getArea() != null && m.getArea().contains(me));
		int index = stageIndex(m, p, inGame);
		DiaryStage stage = m.getStages().get(index);
		if (!stage.isStarted() && !inGame)
		{
			DiaryStep supplies = DiaryGuide.supplies(m.getId(), m.getName(), m.getItems(), p);
			if (supplies != null)
			{
				return reword(supplies);
			}
		}

		boolean inside = stage.getInside() != null && stage.getInside().contains(me);
		Location at = stage.getAt();
		if (at != null && !inside && !inGame && !at.contains(me))
		{
			DiaryStep.DiaryStepBuilder b = step(DiaryStep.Kind.TRAVEL, m)
				.title("Go to " + at.getName())
				.destination(at);
			// No "N tiles away" line: it would change every tick and redraw the panel the whole walk
			if (m.getGetThere() != null)
			{
				b.line(m.getGetThere());
			}
			b.line("Follow the yellow arrow, or the line on the ground if you have the Shortest Path plugin.");
			for (TravelPick travel : p.getTravel())
			{
				b.line(travel.isCarried() ? travel.carriedLine() : travel.tipLine());
			}
			b.line("Then: " + stage.getTitle());
			return b.build();
		}

		DiaryStep.DiaryStepBuilder b = step(DiaryStep.Kind.DO, m)
			.title(stage.getTitle())
			.npcs(stage.getNpcs())
			.objects(stage.getObjects())
			.use(stage.getUse());
		stage.getLines().forEach(b::line);
		if (m.getStages().size() > 1)
		{
			b.line("Part " + (index + 1) + " of " + m.getStages().size() + ".");
		}
		for (Reward r : affordable(m, p))
		{
			b.line("★ You can afford the " + r.getName() + " (" + r.getCost() + "): spend your points on it.");
		}
		return b.build();
	}

	/**
	 * Which stage the player is on: the last one whose condition holds; in a game area the last stage (the game
	 * itself); otherwise the first.
	 */
	static int stageIndex(Minigame m, DiaryPlayer p, boolean inGame)
	{
		List<DiaryStage> stages = m.getStages();
		for (int i = stages.size() - 1; i > 0; i--)
		{
			DiaryStage s = stages.get(i);
			if ((s.getInside() != null && s.getInside().contains(p.getLocation()))
				|| (s.getCarrying() > 0 && p.getCarried().getOrDefault(s.getCarrying(), 0) > 0)
				|| (s.getVarbit() > 0 && p.getVarbit().applyAsInt(s.getVarbit()) >= s.getVarbitValue()))
			{
				return i;
			}
		}
		if (inGame)
		{
			return stages.size() - 1;
		}
		return 0;
	}

	/**
	 * The shared supply steps talk about "this task" (they come from the diary guides).
	 */
	private static DiaryStep reword(DiaryStep s)
	{
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder()
			.kind(s.getKind())
			.taskKey(s.getTaskKey())
			.title(s.getTitle().replace("this task", "this minigame"))
			.destination(s.getDestination())
			.npcs(s.getNpcs())
			.objects(s.getObjects())
			.withdraw(s.getWithdraw())
			.use(s.getUse())
			.travel(s.getTravel());
		for (String line : s.getLines())
		{
			b.line(line.replace("Or pick another task in the panel.", "Or pick another minigame in the panel."));
		}
		return b.build();
	}

	private static DiaryStep.DiaryStepBuilder step(DiaryStep.Kind kind, Minigame m)
	{
		return DiaryStep.builder().kind(kind).taskKey(m.getId());
	}
}
