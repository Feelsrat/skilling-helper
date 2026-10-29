package com.skillinghelper.engine;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.SlayerPlaces;
import com.skillinghelper.data.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * Leads the player to their current Slayer task's monsters, so a task they can't (or don't want to) cancel never
 * leaves them without a next step. Masters are only visited once the task is done.
 */
public final class SlayerTaskStep
{
	/**
	 * Slayer points it costs to cancel a task at a master.
	 */
	static final int SKIP_COST = 30;
	/**
	 * How close to a spawn tile counts as being at the monsters.
	 */
	private static final int SPOT_RADIUS = 8;
	private static final int ENTRANCE_RADIUS = 3;
	private static final int ALSO_AT_LIMIT = 3;

	static boolean applies(Method m, PlayerState s)
	{
		return m.getSkill() == Skill.SLAYER && s.getSlayerTask() != null && s.getSlayerTask().getRemaining() > 0;
	}

	static GuideStep step(PlayerState s)
	{
		SlayerTask task = s.getSlayerTask();
		String name = task.getName() != null ? task.getName() : SlayerPlaces.get().taskName(task.getTaskId());
		String noun = name == null ? "task monsters" : name.toLowerCase(Locale.ROOT);
		String left = String.format("Slayer task: %,d %s left.", task.getRemaining(), noun);

		GuideStep.GuideStepBuilder b = GuideStep.builder();
		if (name != null)
		{
			b.highlight(target(name));
		}

		List<SlayerPlaces.Place> places = candidates(task);
		List<SlayerPlaces.Place> open = filter(places, p -> canEnter(p, s));
		if (task.isMonstersNearby() || isAtAny(places, s.getLocation()))
		{
			b.type(StepType.TRAIN)
				.title(String.format("Kill %,d more %s", task.getRemaining(), noun))
				.line("The highlighted monsters count for your task.");
			konarLine(b, task);
			skipLine(b, task, false);
			return b.build();
		}
		if (places.isEmpty())
		{
			b.type(StepType.TRAIN)
				.title(String.format("Kill %,d more %s", task.getRemaining(), noun))
				.line("I don't have a location for this task yet: check its Slayer task page on the wiki. I'll "
					+ "highlight the monsters once they're in view.");
			skipLine(b, task, true);
			return b.build();
		}

		List<SlayerPlaces.Place> choices = open.isEmpty() ? places : open;
		SlayerPlaces.Place best = choices.stream().filter(p -> !p.getSpots().isEmpty()).findFirst()
			.orElse(choices.get(0));
		Route route = Route.to(s, destinations(best));
		b.type(StepType.WALK).title(route.title()).line(left);
		konarLine(b, task);
		if (best.getSpots().isEmpty())
		{
			b.line("The " + noun + " are inside. I'll highlight them once they're in view.");
		}
		String missing = missingText(best, s);
		if (!missing.isEmpty())
		{
			b.line("You may need " + missing + " to get in.");
		}
		if (best.getNote() != null)
		{
			b.line(best.getNote());
		}
		List<String> others = choices.stream().filter(p -> p != best).limit(ALSO_AT_LIMIT)
			.map(SlayerPlaces.Place::getLabel).collect(Collectors.toList());
		if (!others.isEmpty())
		{
			b.line("Also found at: " + String.join(", ", others) + ".");
		}
		skipLine(b, task, open.isEmpty());
		return b.travel(route)
			.line("Then: kill the highlighted " + noun + ".")
			.build();
	}

	/**
	 * Every NPC that counts for the task, matched like RuneLite's Slayer plugin: the task name without its plural
	 * "s" (so "Aberrant spectres" matches "Aberrant spectre"), plus names such as "Chicken" for Birds.
	 */
	public static Target target(String taskName)
	{
		List<String> words = new ArrayList<>(SlayerPlaces.get().extraNpcNames(taskName));
		words.add(taskName.replaceAll("s$", ""));
		return Target.npcContaining("Attack", words.toArray(new String[0]));
	}

	/**
	 * The places that count for this assignment: Krystilia's are in the Wilderness, Konar picks one area, and other
	 * masters' tasks are never sent into the Wilderness unless there's nowhere else.
	 */
	private static List<SlayerPlaces.Place> candidates(SlayerTask task)
	{
		SlayerPlaces data = SlayerPlaces.get();
		List<SlayerPlaces.Place> places;
		if (task.isWilderness())
		{
			places = data.places(task.getTaskId(), true);
			if (places.isEmpty())
			{
				places = filter(data.places(task.getTaskId(), false), SlayerTaskStep::inWilderness);
			}
		}
		else
		{
			places = data.places(task.getTaskId(), false);
			List<SlayerPlaces.Place> safe = filter(places, p -> !inWilderness(p));
			if (!safe.isEmpty())
			{
				places = safe;
			}
		}
		if (task.getAreaId() > 0)
		{
			List<SlayerPlaces.Place> konar = filter(places, p -> p.getAreaId() == task.getAreaId());
			if (!konar.isEmpty())
			{
				places = konar;
			}
		}
		return places;
	}

	private static List<Location> destinations(SlayerPlaces.Place p)
	{
		List<Location> out = new ArrayList<>();
		for (WorldPoint spot : p.getSpots())
		{
			out.add(Location.of(p.getLabel(), spot.getX(), spot.getY(), spot.getPlane(), SPOT_RADIUS));
		}
		if (out.isEmpty())
		{
			WorldPoint e = p.getEntrance();
			out.add(Location.of(p.getLabel(), e.getX(), e.getY(), e.getPlane(), ENTRANCE_RADIUS));
		}
		return out;
	}

	private static boolean isAtAny(List<SlayerPlaces.Place> places, WorldPoint me)
	{
		if (me == null)
		{
			return false;
		}
		for (SlayerPlaces.Place p : places)
		{
			for (WorldPoint spot : p.getSpots())
			{
				if (spot.getPlane() == me.getPlane() && Math.max(Math.abs(spot.getX() - me.getX()),
					Math.abs(spot.getY() - me.getY())) <= SPOT_RADIUS)
				{
					return true;
				}
			}
		}
		return false;
	}

	private static boolean inWilderness(SlayerPlaces.Place p)
	{
		for (WorldPoint spot : p.getSpots())
		{
			if (!Route.inWilderness(spot))
			{
				return false;
			}
		}
		return p.getEntrance() == null ? !p.getSpots().isEmpty() : Route.inWilderness(p.getEntrance());
	}

	static boolean canEnter(SlayerPlaces.Place p, PlayerState s)
	{
		return missingText(p, s).isEmpty();
	}

	/**
	 * The quests and levels the place needs that the player doesn't have, e.g. "Priest in Peril", or "" if none.
	 */
	private static String missingText(SlayerPlaces.Place p, PlayerState s)
	{
		List<String> missing = new ArrayList<>();
		for (Quest q : p.getQuestsFinished())
		{
			if (!s.getCompletedQuests().contains(q))
			{
				missing.add(q.getName());
			}
		}
		for (Quest q : p.getQuestsStarted())
		{
			if (!s.getStartedQuests().contains(q) && !s.getCompletedQuests().contains(q))
			{
				missing.add(q.getName() + " (started)");
			}
		}
		for (Map.Entry<Skill, Integer> e : p.getSkills().entrySet())
		{
			if (s.level(e.getKey()) < e.getValue())
			{
				missing.add(e.getValue() + " " + e.getKey().getName());
			}
		}
		return String.join(", ", missing);
	}

	private static void konarLine(GuideStep.GuideStepBuilder b, SlayerTask task)
	{
		if (task.getAreaName() != null)
		{
			b.line("Konar's task only counts in " + task.getAreaName() + ".");
		}
	}

	/**
	 * Offers to cancel the task only when the player has the points for it. Turael can always swap a task for an easier
	 * one, at the cost of the streak, which matters when nowhere the task is done can be reached.
	 */
	private static void skipLine(GuideStep.GuideStepBuilder b, SlayerTask task, boolean stuck)
	{
		if (task.getPoints() >= SKIP_COST)
		{
			b.line(String.format("%s Any Slayer master can cancel it for %d points (you have %,d).",
				stuck ? "Can't do this task yet?" : "Don't want this task?", SKIP_COST, task.getPoints()));
		}
		else if (stuck)
		{
			b.line(String.format("You can't reach any spot for this task yet and need %d Slayer points to cancel it "
				+ "(you have %,d). Turael can give you an easier task instead, but that resets your task streak.",
				SKIP_COST, task.getPoints()));
		}
	}

	private static <T> List<T> filter(List<T> list, Predicate<T> keep)
	{
		return list.stream().filter(keep).collect(Collectors.toList());
	}

	private SlayerTaskStep()
	{
	}
}
