package com.skillinghelper.collection;

import com.skillinghelper.ca.CaRanker;
import com.skillinghelper.ca.data.CaMonster;
import com.skillinghelper.collection.data.ClogEntry;
import com.skillinghelper.collection.data.ClogItem;
import com.skillinghelper.collection.data.ClogLinks;
import com.skillinghelper.collection.data.ClogTab;
import com.skillinghelper.collection.data.GuideLink;
import com.skillinghelper.data.Location;
import com.skillinghelper.diary.DiaryGuide;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.diary.data.DiaryStage;
import com.skillinghelper.diary.data.DiaryTask;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * Turns "get this collection log slot" into the next thing to do, for someone who has never been there: what's
 * missing, the bank trip for what to bring, the walk (with travel items), how to get in, and what to click, then
 * "keep going" until the slot is logged. Recomputed every tick.
 * <p>
 * Pages another plugin in this jar has a full guide for are handed over to it (the plugin opens it); the rest are
 * guided here, with the Combat Achievement Guides' boss data (entrances, items, outlines) and the diary guide's
 * banking, buying and walking steps.
 */
public final class ClogGuide
{
	private static final Map<String, DiaryTask> TASKS = new ConcurrentHashMap<>();

	/**
	 * What the guide knows besides the player.
	 */
	@Value
	@Builder
	public static class Context
	{
		DiaryPlayer player;
		ClogEntry entry;
		ClogItem slot;
		/**
		 * The page's slots still to get, quickest first (the target slot first).
		 */
		List<ClogItem> left;
		/**
		 * The page's guide elsewhere in the jar, or null.
		 */
		GuideLink link;
		/**
		 * What keeps the player from a linked page (the linked guide's own requirements).
		 */
		ClogRequirements.Check check;
		/**
		 * The boss is on screen close by.
		 */
		boolean near;
		/**
		 * The player reached the place for this page and hasn't wandered off since.
		 */
		boolean arrived;
	}

	private ClogGuide()
	{
	}

	/**
	 * Whether this plugin can take a player to the page: a full guide elsewhere, boss data, or a place on the map.
	 */
	public static boolean guidable(ClogEntry e)
	{
		return !e.isPets() && (ClogLinks.of(e) != null || ClogLinks.monster(e) != null || e.getLocation() != null);
	}

	/**
	 * The page as a diary-style task, so the diary guide's requirement, banking, buying and walking steps work on it.
	 */
	public static DiaryTask task(ClogEntry e)
	{
		return TASKS.computeIfAbsent(e.getName(), k -> buildTask(e));
	}

	private static DiaryTask buildTask(ClogEntry e)
	{
		CaMonster m = ClogLinks.monster(e);
		DiaryTask.DiaryTaskBuilder b = DiaryTask.builder()
			.id("clog:" + e.getName())
			.name(e.getName())
			.text(e.getIntro())
			.freeToPlay(m != null && !m.isMembers())
			.wilderness(e.isWilderness() || m != null && m.isWilderness());
		if (e.getSlayerLevel() > 0)
		{
			b.skill(Skill.SLAYER, e.getSlayerLevel());
			b.unboostable(Skill.SLAYER);
		}
		DiaryStage.DiaryStageBuilder stage = DiaryStage.builder().title(doTitle(e, m));
		if (m != null)
		{
			m.getSkills().forEach((s, l) -> b.skill(s, Math.max(l, s == Skill.SLAYER ? e.getSlayerLevel() : 0)));
			m.getQuests().forEach(b::quest);
			m.getQuestsStarted().forEach(b::questStarted);
			m.getNotes().forEach(b::note);
			m.getItems().forEach(b::item);
			stage.at(m.getAt() != null ? m.getAt() : place(e));
			m.getObjects().forEach(stage::object);
		}
		else
		{
			stage.at(place(e));
			e.npcNames().forEach(stage::npc);
		}
		b.stage(stage.build());
		return b.build();
	}

	private static Location place(ClogEntry e)
	{
		return e.getLocation() == null ? null : Location.of(e.getName(), e.getLocation().getX(),
			e.getLocation().getY(), e.getLocation().getPlane(), 6);
	}

	private static String doTitle(ClogEntry e, CaMonster m)
	{
		if (m != null && !m.isNoAttack())
		{
			return "Kill " + m.getName();
		}
		return e.getTab() == ClogTab.BOSSES || e.getTab() == ClogTab.RAIDS ? "Do " + e.getName() : e.getName();
	}

	// ---- Steps ----

	public static DiaryStep resolve(Context c)
	{
		ClogEntry e = c.getEntry();
		if (c.getLink() != null)
		{
			return handOff(c);
		}
		if (!guidable(e))
		{
			return DiaryStep.builder().kind(DiaryStep.Kind.LOCKED)
				.title("No route for " + e.getName() + " yet")
				.line("This page has no place on the map to send you to (" + e.getIntro() + ")")
				.line("Press \"Wiki\" to read how its slots are found, or \"Skip slot\" to go for the next one.")
				.build();
		}
		DiaryTask t = task(e);
		DiaryPlayer p = c.getPlayer();
		if (!DiaryGuide.missing(t, p).isEmpty())
		{
			return locked(DiaryGuide.missing(t, p), c);
		}
		if (c.isNear() || c.isArrived())
		{
			return doStep(c, c.isNear());
		}
		DiaryStep s = DiaryGuide.resolve(t, p);
		if (s.getKind() == DiaryStep.Kind.DO)
		{
			return doStep(c, false);
		}
		List<String> lines = new ArrayList<>(s.getLines());
		lines.add(goingFor(c));
		return s.toBuilder().clearLines().lines(lines).build();
	}

	private static DiaryStep locked(List<String> missing, Context c)
	{
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder().kind(DiaryStep.Kind.LOCKED)
			.taskKey(task(c.getEntry()).key())
			.title("First you need, for " + c.getEntry().getName() + ":");
		missing.forEach(m -> b.line("✘ " + m));
		b.line("Skilling Helper trains levels and Quest Helper does quests. Press \"Skip slot\" to go for the next "
			+ "slot instead; this one comes back when you have them.");
		return b.build();
	}

	/**
	 * A page with a full guide elsewhere: the plugin opens it, this card says what it's for and when it moves on.
	 */
	private static DiaryStep handOff(Context c)
	{
		GuideLink link = c.getLink();
		ClogRequirements.Check check = c.getCheck();
		if (check != null && check.isLocked())
		{
			return locked(check.getMissing(), c);
		}
		String plugin = link.getKind() == GuideLink.Kind.SKILL ? "Skilling Helper (" + link.getSkill().getName()
			+ ")" : link.getKind().getPlugin();
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder().kind(DiaryStep.Kind.DO)
			.taskKey("clog:" + c.getEntry().getName())
			.title("Follow the " + plugin + " guide");
		b.line("It's open in its own panel: it sends you to the bank for what to bring, draws the route and "
			+ "outlines what to click, step by step.");
		b.line(goingFor(c));
		b.line("When the slot is logged, this guide picks your next one and opens its guide by itself.");
		b.line("Closed it by mistake? Press \"Open guide\".");
		return b.build();
	}

	/**
	 * At the place: how to get in, what to click, what to bring, and how long to keep going.
	 */
	private static DiaryStep doStep(Context c, boolean atBoss)
	{
		ClogEntry e = c.getEntry();
		CaMonster m = ClogLinks.monster(e);
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder().kind(DiaryStep.Kind.DO)
			.taskKey(task(e).key())
			.title(doTitle(e, m));
		int n = 1;
		Set<String> npcs = new HashSet<>(e.npcNames());
		Set<String> objects = new HashSet<>();
		if (m != null)
		{
			if (!atBoss)
			{
				for (String line : m.getEnter())
				{
					b.line(n++ + ". " + line);
				}
			}
			if (!m.isNoAttack())
			{
				b.line(n++ + ". Click " + m.getName() + " (outlined) and choose Attack. Eat when your Hitpoints drop "
					+ "below half, and leave (teleport) when you run out of food.");
			}
			for (String line : m.getFight())
			{
				b.line(n++ + ". " + line);
			}
			b.line(m.getBring() != null ? m.getBring() + "." : "Bring your best gear, food and potions.");
			CaRanker.Power power = CaRanker.power(c.getPlayer());
			if (power.getValue() + 5 < m.getLevel())
			{
				b.line("⚠ This is a hard fight for your stats (comfortable from about level " + m.getLevel()
					+ "): bring extra food and leave if it goes badly.");
			}
			npcs.addAll(m.getNpcs());
			objects.addAll(m.getObjects());
		}
		else
		{
			b.line(n + ". " + what(e));
		}
		if (e.isWilderness())
		{
			b.line("☠ Wilderness: other players can attack you here. Bring only what you are OK to lose.");
		}
		b.line(goingFor(c));
		b.line("Keep going: each kill or completion rolls for the slots. When a new slot is logged, the guide moves on "
			+ "by itself.");
		return b.npcs(npcs).objects(objects).build();
	}

	private static String what(ClogEntry e)
	{
		switch (e.getTab())
		{
			case BOSSES:
				List<String> npcs = e.npcNames();
				return npcs.isEmpty() ? "Take part in " + e.getName() + "." : "Kill " + String.join(" or ", npcs)
					+ " (outlined): click it and choose Attack.";
			case RAIDS:
				return "Go in at the entrance (solo or with a group). The slots come from the reward chest at the end.";
			case MINIGAMES:
				return "Play " + e.getName() + ". The slots are its rewards: bought with its points from its reward "
					+ "shop, or won as drops. The wiki page lists the cost of each.";
			default:
				return e.getIntro().isEmpty() ? "The slots come from here: see the wiki page for how."
					: e.getIntro() + " See the wiki page for how its slots are found.";
		}
	}

	private static String goingFor(Context c)
	{
		ClogItem slot = c.getSlot();
		StringBuilder s = new StringBuilder("Going for: " + slot.getName());
		if (slot.getPercent() >= 0)
		{
			s.append(" (").append(ClogPlanner.percent(slot.getPercent())).append(" of players have it)");
		}
		List<String> others = new ArrayList<>();
		for (ClogItem i : c.getLeft())
		{
			if (i.getId() != slot.getId() && others.size() < 3)
			{
				others.add(i.getName());
			}
		}
		if (!others.isEmpty())
		{
			s.append(". Also still to get here: ").append(String.join(", ", others))
				.append(c.getLeft().size() > others.size() + 1 ? " and more" : "");
		}
		return s.append(".").toString();
	}
}
