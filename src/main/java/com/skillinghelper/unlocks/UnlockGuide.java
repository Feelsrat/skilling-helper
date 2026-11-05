package com.skillinghelper.unlocks;

import com.skillinghelper.QuestHelperLink;
import com.skillinghelper.data.Banks;
import com.skillinghelper.data.Location;
import com.skillinghelper.diary.DiaryGuide;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.diary.data.Diaries;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.diary.data.DiaryReward;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.unlocks.data.Grind;
import com.skillinghelper.unlocks.data.QuestPart;
import com.skillinghelper.unlocks.data.Unlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * Works out which account unlocks the player has, what's still in the way of the others, and the next step for
 * one: the quests first (through Quest Helper), then the diary tier (through the diary guide), then the points to
 * earn (through Skilling Helper), then the guided last part (bank, walk, click). Recomputed every tick.
 */
public final class UnlockGuide
{
	public enum State
	{
		DONE,
		/**
		 * Can't tell yet: it's an item and the bank hasn't been opened this session.
		 */
		UNKNOWN,
		READY,
		LOCKED
	}

	static final String DIARY_PLUGIN_OFF = "Turn on \"Achievement Diary Guides\" in the plugin list first (it comes "
		+ "with this plugin).";
	static final String SKILLING_PLUGIN_OFF = "Turn on \"Skilling Helper\" in the plugin list first to use its "
		+ "guide.";

	private UnlockGuide()
	{
	}

	// ---- What the player has ----

	public static boolean done(Unlock u, UnlockPlayer up)
	{
		DiaryPlayer p = up.getPlayer();
		if (!u.getOwn().isEmpty() && missingPieces(u, p).isEmpty())
		{
			return true;
		}
		if (u.getDoneVarbit() > 0 && p.getVarbit().applyAsInt(u.getDoneVarbit()) >= u.getDoneVarbitMin())
		{
			return true;
		}
		if (u.isDiary() && DiaryGuide.claimed(u.getDiaryRegion(), u.getDiaryTier(), p))
		{
			return true;
		}
		return u.getDoneQuest() != null && p.quest(u.getDoneQuest()) == QuestState.FINISHED;
	}

	public static State state(Unlock u, UnlockPlayer up)
	{
		if (done(u, up))
		{
			return State.DONE;
		}
		if (!missing(u, up).isEmpty())
		{
			return State.LOCKED;
		}
		return unknown(u, up) ? State.UNKNOWN : State.READY;
	}

	/**
	 * Whether the unlock is an item and the plugin hasn't seen the bank yet, so it may well be owned already.
	 */
	static boolean unknown(Unlock u, UnlockPlayer up)
	{
		return !u.getOwn().isEmpty() && up.getPlayer().getBank() == null && !done(u, up);
	}

	/**
	 * The {@link Unlock#getOwn()} groups the player has nothing of yet.
	 */
	static List<Integer> missingPieces(Unlock u, DiaryPlayer p)
	{
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < u.getOwn().size(); i++)
		{
			boolean has = false;
			for (int id : u.getOwn().get(i))
			{
				if (p.owned(id) > 0)
				{
					has = true;
					break;
				}
			}
			if (!has)
			{
				out.add(i);
			}
		}
		return out;
	}

	/**
	 * What's still in the way of the part the player is on now (not of later parts: a quest's levels matter while
	 * it's the quest's turn). Empty when they can get on with it.
	 */
	public static List<String> missing(Unlock u, UnlockPlayer up)
	{
		DiaryPlayer p = up.getPlayer();
		List<String> out = new ArrayList<>();
		if (done(u, up))
		{
			return out;
		}
		if (u.isMembers() && !p.isMembers())
		{
			out.add("A members world");
			return out;
		}
		QuestPart quest = nextQuest(u, p);
		if (quest != null)
		{
			for (Map.Entry<Skill, Integer> e : quest.getSkills().entrySet())
			{
				levelCheck(out, p, e.getKey(), e.getValue());
			}
			return out;
		}
		if (u.isDiary())
		{
			return out;
		}
		Grind grind = u.getGrind();
		if (grind != null && stillToEarn(u, p) > 0)
		{
			if (grind.getMinLevel() > 0)
			{
				levelCheck(out, p, grind.getSkill(), grind.getMinLevel());
			}
			return out;
		}
		if (u.getGuide() != null)
		{
			out.addAll(DiaryGuide.missing(u.getGuide(), p));
		}
		return out;
	}

	private static void levelCheck(List<String> out, DiaryPlayer p, Skill s, int level)
	{
		if (p.boostedLevel(s) < level)
		{
			out.add(level + " " + s.getName() + " (you have " + p.level(s) + ")");
		}
	}

	/**
	 * The first quest on the way that isn't done (or far enough) yet, or null.
	 */
	static QuestPart nextQuest(Unlock u, DiaryPlayer p)
	{
		for (QuestPart q : u.getQuests())
		{
			if (!questDone(q, p))
			{
				return q;
			}
		}
		return null;
	}

	static boolean questDone(QuestPart q, DiaryPlayer p)
	{
		if (p.quest(q.getQuest()) == QuestState.FINISHED)
		{
			return true;
		}
		return q.getVarbit() > 0 && p.getVarbit().applyAsInt(q.getVarbit()) >= q.getVarbitMin();
	}

	/**
	 * Points or tokens the player already has towards the grind.
	 */
	static int earned(Grind g, DiaryPlayer p)
	{
		return g.getItemId() > 0 ? p.owned(g.getItemId()) : p.getVarbit().applyAsInt(g.getVarbit());
	}

	/**
	 * How many points or tokens are needed in all for what's still missing (pieces already owned left out).
	 */
	static int needed(Unlock u, DiaryPlayer p)
	{
		Grind g = u.getGrind();
		if (g == null)
		{
			return 0;
		}
		int[] costs = u.getPieceCosts();
		if (costs == null || costs.length == 0)
		{
			return g.getAmount();
		}
		int total = 0;
		for (int i : missingPieces(u, p))
		{
			total += i < costs.length ? costs[i] : 0;
		}
		return total;
	}

	public static int stillToEarn(Unlock u, DiaryPlayer p)
	{
		Grind g = u.getGrind();
		return g == null ? 0 : Math.max(0, needed(u, p) - earned(g, p));
	}

	// ---- Descriptions ----

	/**
	 * What the unlock gives: its own list, or for a diary reward the reward's perks.
	 */
	public static List<String> gives(Unlock u)
	{
		if (!u.getGives().isEmpty() || !u.isDiary())
		{
			return u.getGives();
		}
		DiaryReward reward = Diaries.reward(u.getDiaryRegion(), u.getDiaryTier());
		return reward == null ? List.of() : reward.getPerks();
	}

	/**
	 * One line on how it's got, e.g. "Quests: Lost City, Fairytale I (start Fairytale II)".
	 */
	public static String how(Unlock u, UnlockPlayer up)
	{
		DiaryPlayer p = up.getPlayer();
		List<String> parts = new ArrayList<>();
		if (!u.getQuests().isEmpty())
		{
			List<String> names = new ArrayList<>();
			for (QuestPart q : u.getQuests())
			{
				String name = (q.getVarbit() > 0 ? "start " : "") + q.getQuest().getName();
				names.add(questDone(q, p) ? "✔ " + name : name);
			}
			parts.add((u.getQuests().size() > 1 ? "Quests: " : "Quest: ") + String.join(", ", names));
		}
		if (u.isDiary())
		{
			int[] prog = up.getDiaryProgress().get(diaryKey(u));
			parts.add(diaryName(u) + " diary" + (prog == null ? "" : " (" + prog[0] + " of " + prog[1] + " tasks done)"));
		}
		Grind g = u.getGrind();
		if (g != null)
		{
			int need = needed(u, p);
			parts.add(String.format("%,d", need) + " " + g.getLabel()
				+ (p.getBank() == null && g.getItemId() > 0 ? "" : " (you have " + String.format("%,d", earned(g, p)) + ")"));
		}
		if (u.getGuide() != null && nextQuest(u, p) == null)
		{
			for (DiaryItem item : u.getGuide().getItems())
			{
				parts.add((item.getAmount() > 1 ? String.format("%,d", item.getAmount()) + " " : "")
					+ item.getName().toLowerCase());
			}
		}
		return String.join(", then ", parts);
	}

	static String diaryKey(Unlock u)
	{
		return u.getDiaryRegion().name() + "." + u.getDiaryTier().name();
	}

	static String diaryName(Unlock u)
	{
		return u.getDiaryRegion().getLabel() + " " + u.getDiaryTier().getLabel();
	}

	/**
	 * Gold the guided last part still costs (coins and items to buy), 0 for the other parts.
	 */
	public static long cost(Unlock u, UnlockPlayer up)
	{
		if (u.getGuide() == null || done(u, up))
		{
			return 0;
		}
		return DiaryGuide.cost(u.getGuide(), up.getPlayer());
	}

	// ---- Picking and steps ----

	/**
	 * The unlock to guide: the one picked in the panel, else the first in the suggested order the player can work on.
	 */
	public static Unlock next(List<Unlock> unlocks, UnlockPlayer up, Set<String> skipped, String picked)
	{
		for (Unlock u : unlocks)
		{
			if (u.getId().equals(picked) && !done(u, up))
			{
				return u;
			}
		}
		for (Unlock u : unlocks)
		{
			State s = state(u, up);
			if ((s == State.READY || s == State.UNKNOWN) && !skipped.contains(u.getId()))
			{
				return u;
			}
		}
		return null;
	}

	public static UnlockStep resolve(List<Unlock> unlocks, UnlockPlayer up, Set<String> skipped, String picked)
	{
		Unlock u = next(unlocks, up, skipped, picked);
		if (u == null)
		{
			DiaryStep.DiaryStepBuilder b = DiaryStep.builder().kind(DiaryStep.Kind.LOCKED);
			boolean all = true;
			for (Unlock x : unlocks)
			{
				all &= done(x, up);
			}
			if (all)
			{
				b.title("Every unlock on the list is done").line("✔ Nice: nothing left here.");
			}
			else
			{
				b.title("Nothing left you can work on yet");
				for (Unlock x : unlocks)
				{
					if (done(x, up))
					{
						continue;
					}
					List<String> missing = missing(x, up);
					b.line(skipped.contains(x.getId()) ? "• " + x.getName() + ": skipped (click it in the list to do it)"
						: "✘ " + x.getName() + ": needs " + String.join(", ", missing));
				}
			}
			return new UnlockStep(null, b.build(), UnlockStep.Action.NONE);
		}
		return resolve(u, up);
	}

	/**
	 * The next step for one unlock.
	 */
	public static UnlockStep resolve(Unlock u, UnlockPlayer up)
	{
		DiaryPlayer p = up.getPlayer();
		if (done(u, up))
		{
			DiaryStep.DiaryStepBuilder b = step(u, DiaryStep.Kind.DONE).title("✔ " + u.getName() + " is unlocked");
			gives(u).forEach(g -> b.line("• " + g));
			return new UnlockStep(u.getId(), b.build(), UnlockStep.Action.NONE);
		}
		List<String> missing = missing(u, up);
		if (!missing.isEmpty())
		{
			DiaryStep.DiaryStepBuilder b = step(u, DiaryStep.Kind.LOCKED).title("You can't work on this yet");
			b.line("Still needed:");
			missing.forEach(m -> b.line("✘ " + m));
			b.line("Pick another unlock in the panel, or skip this one and the guide picks the next.");
			return new UnlockStep(u.getId(), b.build(), UnlockStep.Action.NONE);
		}
		if (unknown(u, up))
		{
			return new UnlockStep(u.getId(), openBankFirst(u, p), UnlockStep.Action.NONE);
		}
		QuestPart quest = nextQuest(u, p);
		if (quest != null)
		{
			return new UnlockStep(u.getId(), questStep(u, quest, up), UnlockStep.Action.QUEST);
		}
		if (u.isDiary())
		{
			return new UnlockStep(u.getId(), diaryStep(u, up), UnlockStep.Action.DIARY);
		}
		Grind grind = u.getGrind();
		int toEarn = stillToEarn(u, p);
		if (grind != null && toEarn > 0)
		{
			return new UnlockStep(u.getId(), grindStep(u, grind, toEarn, up), UnlockStep.Action.TRAIN);
		}
		DiaryTask task = guideTask(u, p);
		if (task == null)
		{
			// Every part is done but the game doesn't say it's unlocked yet (e.g. it updates on the next tick)
			return new UnlockStep(u.getId(), step(u, DiaryStep.Kind.DONE).title(u.getName())
				.line("Every part is done: it should show as unlocked in a moment.").build(), UnlockStep.Action.NONE);
		}
		DiaryStep s = DiaryGuide.resolve(task, p);
		return new UnlockStep(u.getId(), retag(s, u), UnlockStep.Action.NONE);
	}

	/**
	 * The guided last part, with the tokens to carry (e.g. the marks of grace for the pieces still missing).
	 */
	static DiaryTask guideTask(Unlock u, DiaryPlayer p)
	{
		DiaryTask task = u.getGuide();
		Grind g = u.getGrind();
		if (task == null || g == null || g.getItemId() <= 0)
		{
			return task;
		}
		int need = needed(u, p);
		if (need <= 0)
		{
			return task;
		}
		String label = Character.toUpperCase(g.getLabel().charAt(0)) + g.getLabel().substring(1);
		return task.toBuilder()
			.item(new DiaryItem(label, need, new int[]{g.getItemId()}, 0, "Earn them first: " + g.getLabel()
				+ " can't be bought.", null, 0))
			.build();
	}

	private static DiaryStep questStep(Unlock u, QuestPart q, UnlockPlayer up)
	{
		String name = q.getQuest().getName();
		boolean started = up.getPlayer().quest(q.getQuest()) == QuestState.IN_PROGRESS;
		DiaryStep.DiaryStepBuilder b = step(u, DiaryStep.Kind.DO)
			.title((q.getVarbit() > 0 ? "Start the quest: " : started ? "Finish the quest: " : "Do the quest: ") + name);
		b.line(u.getName() + " comes from this quest. Quest Helper shows every step:");
		b.line("1. Open the Quest list tab (the blue quest icon in the side tabs).");
		b.line("2. Right-click \"" + name + "\" and choose \"Start Quest Helper\", then follow its steps.");
		if (q.getNote() != null)
		{
			b.line(q.getNote());
		}
		if (!up.isQuestHelper())
		{
			b.line("⚠ " + QuestHelperLink.MISSING);
		}
		b.line("This guide moves on by itself when the quest is done.");
		return b.build();
	}

	private static DiaryStep diaryStep(Unlock u, UnlockPlayer up)
	{
		String diary = diaryName(u);
		int[] prog = up.getDiaryProgress().get(diaryKey(u));
		boolean allDone = prog != null && prog[0] >= prog[1] && prog[1] > 0;
		DiaryStep.DiaryStepBuilder b = step(u, DiaryStep.Kind.DO)
			.title(allDone ? "Claim the " + diary + " diary reward" : "Do the " + diary + " diary");
		b.line(u.getName() + " is the reward for the " + diary + " Achievement Diary"
			+ (prog == null ? "." : ": " + prog[0] + " of " + prog[1] + " tasks done."));
		b.line("Click \"Open diary guide\" below: Achievement Diary Guides walks you through every task"
			+ (allDone ? " and the claim." : ", then the claim."));
		if (!up.isDiaryGuides())
		{
			b.line("⚠ " + DIARY_PLUGIN_OFF);
		}
		b.line("This guide moves on by itself when the reward is claimed.");
		return b.build();
	}

	private static DiaryStep grindStep(Unlock u, Grind g, int toEarn, UnlockPlayer up)
	{
		DiaryPlayer p = up.getPlayer();
		DiaryStep.DiaryStepBuilder b = step(u, DiaryStep.Kind.DO)
			.title("Earn " + String.format("%,d", toEarn) + " more " + g.getLabel());
		b.line("You need " + String.format("%,d", needed(u, p)) + " " + g.getLabel() + " for " + u.getName()
			+ " and have " + String.format("%,d", earned(g, p)) + ".");
		g.getLines().forEach(b::line);
		if (!up.isSkillingHelper())
		{
			b.line("⚠ " + SKILLING_PLUGIN_OFF);
		}
		b.line("This guide moves on by itself once you have enough.");
		return b.build();
	}

	private static DiaryStep openBankFirst(Unlock u, DiaryPlayer p)
	{
		Location bank = Banks.nearest(p.getLocation(), p.isMembers());
		DiaryStep.DiaryStepBuilder b = step(u, DiaryStep.Kind.OPEN_BANK)
			.title("Open your bank once")
			.npcs(Set.of("Banker", "Gnome banker", "Ghost banker"))
			.objects(Set.of("Bank booth", "Bank chest", "Grand Exchange booth"));
		if (bank != null && !bank.contains(p.getLocation()))
		{
			b.destination(bank).line("Walk to " + bank.getName() + " (follow the arrow).");
		}
		b.line("Click a bank booth or banker (outlined) and choose Bank.");
		b.line("The guide can then see whether you already have " + u.getName() + " (and anything else on the list).");
		return b.build();
	}

	/**
	 * The diary guide's step for the guided part, keyed to the unlock so the panel knows which row it is.
	 */
	private static DiaryStep retag(DiaryStep s, Unlock u)
	{
		return DiaryStep.builder()
			.kind(s.getKind())
			.taskKey(u.getId())
			.title(s.getTitle())
			.lines(s.getLines())
			.destination(s.getDestination())
			.npcs(s.getNpcs())
			.objects(s.getObjects())
			.withdraw(s.getWithdraw())
			.use(s.getUse())
			.travel(s.getTravel())
			.build();
	}

	private static DiaryStep.DiaryStepBuilder step(Unlock u, DiaryStep.Kind kind)
	{
		return DiaryStep.builder().kind(kind).taskKey(u.getId());
	}
}
