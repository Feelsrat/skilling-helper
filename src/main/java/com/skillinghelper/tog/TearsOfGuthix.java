package com.skillinghelper.tog;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemHighlight;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Route;
import com.skillinghelper.engine.StepType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/**
 * The weekly Tears of Guthix minigame: when Juna lets the player in again, which skill the xp goes to, and the step
 * to do next. Kept free of client references so it can be unit tested. Rules from the OSRS wiki "Tears of Guthix
 * (minigame)" and "Transcript:Juna" pages.
 */
public final class TearsOfGuthix
{
	public static final String NAME = "Tears of Guthix";
	public static final String WIKI_PAGE = "Tears_of_Guthix_(minigame)";
	/**
	 * The Chasm of Tears: Juna and the weeping walls.
	 */
	public static final int REGION = 12948;
	public static final long WEEK_SECONDS = 7 * 24 * 60 * 60;
	public static final long XP_BETWEEN_VISITS = 100_000;
	/**
	 * Each stream of tears stays in place this long.
	 */
	public static final int STREAM_TICKS = 16;

	static final Location JUNA = Location.of("Juna (Tears of Guthix)", 3252, 9517, 2, 3);
	private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);
	/**
	 * Close enough to Lumbridge that walking in beats fetching a games necklace from the bank.
	 */
	private static final int WALK_DISTANCE = 80;

	public static final int[] GAMES_NECKLACES = {
		ItemID.NECKLACE_OF_MINIGAMES_1, ItemID.NECKLACE_OF_MINIGAMES_2, ItemID.NECKLACE_OF_MINIGAMES_3,
		ItemID.NECKLACE_OF_MINIGAMES_4, ItemID.NECKLACE_OF_MINIGAMES_5, ItemID.NECKLACE_OF_MINIGAMES_6,
		ItemID.NECKLACE_OF_MINIGAMES_7, ItemID.NECKLACE_OF_MINIGAMES_8,
	};

	/**
	 * Juna has been an NPC since 2024 but was scenery before, so match both.
	 */
	public static final List<Target> TARGETS = List.of(Target.npc(null, "Juna"), Target.object(null, "Juna"));

	/**
	 * The order the game breaks ties in when several skills have the same xp.
	 */
	private static final String[] TIE_ORDER = {
		"ATTACK", "STRENGTH", "RANGED", "MAGIC", "DEFENCE", "HITPOINTS", "PRAYER", "AGILITY", "HERBLORE", "THIEVING",
		"CRAFTING", "RUNECRAFT", "MINING", "SMITHING", "FISHING", "COOKING", "FIREMAKING", "WOODCUTTING", "FLETCHING",
		"SLAYER", "FARMING", "CONSTRUCTION", "HUNTER", "SAILING",
	};

	private static final Pattern DAYS = Pattern.compile("come (?:back|again) (?:in (\\d+) days?|tomorrow)");
	private static final Pattern XP_DUE = Pattern.compile("one quest point or ([\\d,]+) total xp");

	private TearsOfGuthix()
	{
	}

	public enum State
	{
		/**
		 * The Tears of Guthix quest isn't done yet.
		 */
		LOCKED,
		/**
		 * No visit seen yet, so we can't tell.
		 */
		UNKNOWN,
		READY,
		WAIT,
	}

	/**
	 * What the plugin remembers about the last visit, per account. Times are epoch seconds, 0 when unknown.
	 */
	@Value
	@Builder(toBuilder = true)
	public static class Record
	{
		public static final Record EMPTY = Record.builder().build();

		long visitedAt;
		long xpAtVisit;
		int qpAtVisit;
		/**
		 * When Juna said the player can come back, rounded to days. Only used without a seen visit, or when it
		 * disagrees with it.
		 */
		long hintReadyAt;
		/**
		 * The total xp Juna asked for (total xp when she said it plus the xp due), or 0.
		 */
		long hintXpTarget;
		int hintQp;

		/**
		 * "visitedAt:xp:qp:hintReadyAt:hintXp:hintQp".
		 */
		public String serialize()
		{
			return visitedAt + ":" + xpAtVisit + ":" + qpAtVisit + ":" + hintReadyAt + ":" + hintXpTarget + ":" + hintQp;
		}

		public static Record parse(String s)
		{
			if (s == null)
			{
				return EMPTY;
			}
			String[] p = s.split(":");
			try
			{
				return Record.builder()
					.visitedAt(Long.parseLong(p[0]))
					.xpAtVisit(Long.parseLong(p[1]))
					.qpAtVisit(Integer.parseInt(p[2]))
					.hintReadyAt(Long.parseLong(p[3]))
					.hintXpTarget(Long.parseLong(p[4]))
					.hintQp(Integer.parseInt(p[5]))
					.build();
			}
			catch (NumberFormatException | ArrayIndexOutOfBoundsException e)
			{
				return EMPTY;
			}
		}
	}

	@Value
	public static class Status
	{
		public static final Status LOCKED_STATUS = new Status(State.LOCKED, 0, 0, false);

		State state;
		/**
		 * When the seven days are up, or 0 when time isn't what's blocking.
		 */
		long readyAt;
		/**
		 * Total xp still to gain (or one quest point instead), 0 when that part is met.
		 */
		long xpToGo;
		/**
		 * The time comes from Juna's "come back in N days", so it's only right to the day.
		 */
		boolean estimated;

		public boolean isReady()
		{
			return state == State.READY;
		}
	}

	/**
	 * Juna lets the player in once seven days have passed since they last entered and they have gained 100,000 total
	 * xp or a quest point since.
	 */
	public static Status status(boolean questDone, Record r, long now, long totalXp, int qp)
	{
		if (!questDone)
		{
			return Status.LOCKED_STATUS;
		}
		boolean seen = r.getVisitedAt() > 0;
		if (!seen && r.getHintReadyAt() == 0 && r.getHintXpTarget() == 0)
		{
			return new Status(State.UNKNOWN, 0, 0, false);
		}

		long readyAt = seen ? r.getVisitedAt() + WEEK_SECONDS : 0;
		boolean estimated = false;
		// Juna knows best: trust her when she says it's further off than the visit we saw (e.g. played on mobile)
		if (r.getHintReadyAt() > readyAt + WEEK_SECONDS / 14)
		{
			readyAt = r.getHintReadyAt();
			estimated = true;
		}

		long xpToGo = 0;
		if (r.getHintXpTarget() > 0)
		{
			xpToGo = qp > r.getHintQp() ? 0 : Math.max(0, r.getHintXpTarget() - totalXp);
		}
		else if (seen)
		{
			xpToGo = qp > r.getQpAtVisit() ? 0 : Math.max(0, r.getXpAtVisit() + XP_BETWEEN_VISITS - totalXp);
		}

		boolean timeOk = readyAt <= now;
		if (timeOk && xpToGo == 0)
		{
			return new Status(State.READY, 0, 0, false);
		}
		return new Status(State.WAIT, timeOk ? 0 : readyAt, xpToGo, estimated && !timeOk);
	}

	/**
	 * A new visit: Juna's old hints no longer apply.
	 */
	public static Record visited(long now, long totalXp, int qp)
	{
		return Record.builder().visitedAt(now).xpAtVisit(totalXp).qpAtVisit(qp).build();
	}

	/**
	 * Takes in what Juna (or the message box after her) said, if it's about when the player can come back. Returns
	 * the record unchanged when the text isn't about that.
	 */
	public static Record heard(Record r, String text, long now, long totalXp, int qp)
	{
		if (text == null)
		{
			return r;
		}
		String t = text.replaceAll("<[^>]*>", " ").replace(' ', ' ').replaceAll("\\s+", " ")
			.toLowerCase(java.util.Locale.ROOT);
		Record.RecordBuilder b = r.toBuilder();
		boolean changed = false;
		Matcher days = DAYS.matcher(t);
		if (days.find())
		{
			int n = days.group(1) == null ? 1 : Integer.parseInt(days.group(1));
			b.hintReadyAt(now + n * 24L * 60 * 60);
			changed = true;
		}
		Matcher xp = XP_DUE.matcher(t);
		if (xp.find())
		{
			try
			{
				b.hintXpTarget(totalXp + Long.parseLong(xp.group(1).replace(",", ""))).hintQp(qp);
				changed = true;
			}
			catch (NumberFormatException ignored)
			{
				// not a number after all
			}
		}
		else if (t.contains("not been long since your last visit"))
		{
			// She only says this once the xp or quest point part is met
			b.hintXpTarget(0).xpAtVisit(Math.min(r.getXpAtVisit(), totalXp - XP_BETWEEN_VISITS));
			changed = true;
		}
		return changed ? b.build() : r;
	}

	// ---- Reward ----

	/**
	 * The skill the tears' xp goes to: the one with the least xp, skipping skills the player can't train yet.
	 */
	public static Skill rewardSkill(Map<Skill, Integer> xp, Set<Quest> completed)
	{
		Skill best = null;
		int bestXp = Integer.MAX_VALUE;
		for (String name : TIE_ORDER)
		{
			Skill s;
			try
			{
				s = Skill.valueOf(name);
			}
			catch (IllegalArgumentException e)
			{
				continue;
			}
			int x = xp.getOrDefault(s, 0);
			if (!eligible(s, x, completed))
			{
				continue;
			}
			if (x < bestXp)
			{
				best = s;
				bestXp = x;
			}
		}
		return best;
	}

	private static boolean eligible(Skill s, int xp, Set<Quest> completed)
	{
		switch (s.name())
		{
			case "RUNECRAFT":
				return completed.contains(Quest.RUNE_MYSTERIES);
			case "HERBLORE":
				return completed.contains(Quest.DRUIDIC_RITUAL);
			case "SAILING":
				return completed.contains(Quest.PANDEMONIUM);
			case "CONSTRUCTION":
				// Needs a house; any Construction xp means one was bought
				return xp > 0;
			default:
				return true;
		}
	}

	/**
	 * Xp for one tear: 10 at the start of a skill, rising to 60 by level 30.
	 */
	public static double xpPerTear(int skillXp)
	{
		return Math.min(60, 10 + Math.floor(skillXp / 27.0) / 10);
	}

	/**
	 * A good run's tear count: one tick in the cave per quest point, with streams moving every 16 ticks.
	 */
	public static int goodTears(int qp)
	{
		int theoretical = Math.max(0, qp - 4);
		return Math.min(theoretical, (int) Math.floor(theoretical / 16.0 * 13) + 12);
	}

	/**
	 * "~2,580 xp in Agility (43 tears)".
	 */
	public static String rewardText(Skill skill, int skillXp, int qp, boolean hardDiary)
	{
		if (skill == null)
		{
			return null;
		}
		int tears = goodTears(qp);
		long xp = Math.round(tears * xpPerTear(skillXp) * (hardDiary ? 1.1 : 1));
		return String.format("~%,d xp in %s (%d tears%s)", xp, skill.getName(), tears,
			hardDiary ? ", +10% diary" : "");
	}

	// ---- Text ----

	/**
	 * Short status for the panel and infobox tooltip.
	 */
	public static String describe(Status s, long now)
	{
		switch (s.getState())
		{
			case LOCKED:
				return "Needs the Tears of Guthix quest";
			case UNKNOWN:
				return "Not tracked yet: talk to Juna or play once";
			case READY:
				return "Ready now";
			default:
				List<String> parts = new ArrayList<>();
				if (s.getReadyAt() > now)
				{
					parts.add("in " + (s.isEstimated() ? "about " : "") + timeLeft(s.getReadyAt() - now));
				}
				if (s.getXpToGo() > 0)
				{
					parts.add(String.format("after %,d more xp (or 1 quest point)", s.getXpToGo()));
				}
				return "Up " + String.join(", ", parts);
		}
	}

	private static final DateTimeFormatter CALENDAR_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
		.withZone(ZoneOffset.UTC);
	/**
	 * How long the calendar event lasts: about one run plus getting there.
	 */
	private static final long EVENT_SECONDS = 15 * 60;

	/**
	 * A Google Calendar "add event" link for when the seven days are up, or null when time isn't what's blocking
	 * (not tracked, ready, or only waiting on xp). Nothing is sent anywhere: the link just opens the form in the
	 * browser, already filled in, for the player to save.
	 */
	public static String calendarUrl(Status s, long now)
	{
		if (s.getState() != State.WAIT || s.getReadyAt() <= now)
		{
			return null;
		}
		StringBuilder details = new StringBuilder("Juna lets you in again: talk to her in the Lumbridge Swamp Caves "
			+ "(games necklace teleport).");
		if (s.isEstimated())
		{
			details.append(" Time is from Juna's \"come back in N days\", so it may be up to a day off.");
		}
		if (s.getXpToGo() > 0)
		{
			details.append(String.format(" You also need %,d more total xp (or 1 quest point) by then.",
				s.getXpToGo()));
		}
		long start = s.getReadyAt();
		return "https://calendar.google.com/calendar/render?action=TEMPLATE"
			+ "&text=" + encode("OSRS: " + NAME + " is up")
			+ "&dates=" + CALENDAR_TIME.format(Instant.ofEpochSecond(start)) + "/"
			+ CALENDAR_TIME.format(Instant.ofEpochSecond(start + EVENT_SECONDS))
			+ "&details=" + encode(details.toString());
	}

	private static String encode(String s)
	{
		return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
	}

	/**
	 * "3d 4h", "5h 20m" or "12m".
	 */
	public static String timeLeft(long seconds)
	{
		long minutes = Math.max(1, (seconds + 59) / 60);
		long days = minutes / (24 * 60);
		long hours = minutes / 60 % 24;
		if (days > 0)
		{
			return days + "d " + hours + "h";
		}
		return hours > 0 ? hours + "h " + minutes % 60 + "m" : minutes + "m";
	}

	// ---- Steps ----

	/**
	 * Where the player is in the minigame, as read by the plugin.
	 */
	@Value
	@Builder
	public static class Situation
	{
		Status status;
		long now;
		boolean inChasm;
		boolean playing;
		int tears;
		boolean lostTribeDone;
		String reward;
	}

	public static GuideStep step(Situation sit, PlayerState s)
	{
		Status status = sit.getStatus();
		if (status.getState() == State.LOCKED)
		{
			return GuideStep.builder().type(StepType.DONE).title("Finish the Tears of Guthix quest first")
				.line("You need 43 quest points, 49 Firemaking, 20 Crafting and 20 Mining.")
				.line("Open the wiki guide below, or follow it with the Quest Helper plugin.")
				.build();
		}
		if (sit.isPlaying())
		{
			return playing(sit);
		}
		if (status.getState() == State.WAIT)
		{
			GuideStep.GuideStepBuilder b = GuideStep.builder().type(StepType.WAIT)
				.title("Not up yet: " + describe(status, sit.getNow()).substring(3))
				.line("Juna lets you in once every 7 days, after you've gained 100,000 xp or a quest point.");
			if (status.getXpToGo() > 0)
			{
				b.line("Any skill counts towards the xp, so just keep training.");
			}
			return b.line("The Tears of Guthix box shows when it's up, and you'll get a notification.").build();
		}
		if (sit.isInChasm())
		{
			return talkToJuna(sit);
		}
		return travel(sit, s);
	}

	private static GuideStep playing(Situation sit)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder().type(StepType.TRAIN).title("Catch the blue tears");
		if (sit.getTears() > 0)
		{
			b.line("Tears so far: " + sit.getTears() + ".");
		}
		return b.line("Stand at the outlined blue stream and click the wall to collect.")
			.line("When it dries up, run to the blue stream with the brightest outline: it has the most time left.")
			.line("Avoid the green streams (outlined red): they take tears away.")
			.line("Time runs out by itself and you drink the tears" + (sit.getReward() == null ? "."
				: ": " + sit.getReward() + "."))
			.build();
	}

	private static GuideStep talkToJuna(Situation sit)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder().type(StepType.TRAIN)
			.title("Tell Juna a story")
			.highlights(TARGETS)
			.line("Right-click Juna and choose Story.")
			.line("Have both hands free (no weapon or shield): the bowl needs both.");
		if (sit.getStatus().getState() == State.UNKNOWN)
		{
			b.line("If it isn't your week yet she'll say when to come back, and the helper remembers it.");
		}
		if (sit.getReward() != null)
		{
			b.line("Reward: " + sit.getReward() + ".");
		}
		return b.build();
	}

	private static GuideStep travel(Situation sit, PlayerState s)
	{
		int carried = s.getInventory().count(GAMES_NECKLACES);
		int worn = s.getEquipment().count(GAMES_NECKLACES);
		WorldPoint me = s.getLocation();
		boolean nearLumbridge = me != null && me.getPlane() == 0 && me.distanceTo2D(LUMBRIDGE) <= WALK_DISTANCE;
		if (carried > 0 || worn > 0)
		{
			GuideStep.GuideStepBuilder b = GuideStep.builder().type(StepType.WALK)
				.title("Teleport to Tears of Guthix");
			if (carried > 0)
			{
				for (int id : GAMES_NECKLACES)
				{
					if (s.getInventory().count(id) > 0)
					{
						b.itemHighlight(id, ItemHighlight.USE);
					}
				}
				b.line("Rub your games necklace and pick Tears of Guthix.");
			}
			else
			{
				b.line("Right-click your worn games necklace in the equipment tab and pick Tears of Guthix.");
			}
			return b.line("You land next to Juna.").build();
		}
		if (s.bankCount(GAMES_NECKLACES) > 0 && !nearLumbridge)
		{
			Location bank = Banks.nearest(me, true);
			GuideStep.GuideStepBuilder b = GuideStep.builder().type(StepType.BANK)
				.title("Grab a games necklace")
				.highlightBank(true)
				.withdraw(GAMES_NECKLACES, 1)
				.line("Withdraw 1 games necklace: it teleports you straight to Juna.");
			if (bank != null)
			{
				b.travel(Route.to(s, bank));
			}
			return b.build();
		}
		GuideStep.GuideStepBuilder b = GuideStep.builder().type(StepType.WALK)
			.title("Go to Juna in the Lumbridge Swamp Caves")
			.travel(Route.to(s, JUNA))
			.line("Bring a lit light source: the swamp caves are dark.");
		if (sit.isLostTribeDone())
		{
			b.line("Quickest way in: the hole in the Lumbridge Castle cellar wall, then south over the stepping stone.");
		}
		else
		{
			b.line("Go in at the Lumbridge Swamp cave entrance (a rope the first time). Stop one tile short of each "
				+ "wall hole, then run past so the wall beasts miss you.");
		}
		return b.line("Next time: a games necklace from the GE teleports you right there.").build();
	}
}
