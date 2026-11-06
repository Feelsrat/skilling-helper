package com.skillinghelper.clue;

import com.skillinghelper.clue.data.ClueData;
import com.skillinghelper.clue.data.ClueItem;
import com.skillinghelper.clue.data.ClueTier;
import com.skillinghelper.clue.data.ClueWays;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.DiaryGuide;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.diary.data.DiaryStage;
import com.skillinghelper.diary.data.DiaryTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/**
 * The clue guide for one tier, for someone who has never done a clue: open what you already have first (caskets,
 * scroll boxes, bottles), then do the clue you hold with the kit it needs, and only then go and get a new one.
 * Each step is read from RuneLite's own Clue Scroll plugin and turned into what to bring, where to walk and what to
 * click. Recomputed every tick.
 */
public final class ClueGuide
{
	static final int[] SPADES = {ItemID.SPADE, ItemID.LARGE_SPADE, ItemID.TRAIL_GILDED_SPADE};
	/**
	 * Lit light sources that work in the dark caves some steps send you to.
	 */
	static final int[] LIGHTS = {ItemID.BULLSEYE_LANTERN_LIT, ItemID.OIL_LANTERN_LIT, ItemID.OIL_LAMP_LIT,
		ItemID.CANDLE_LANTERN_LIT, ItemID.CAVE_GOBLIN_MINING_HELMET_LIT, ItemID.TORCH_LIT, ItemID.LIT_CANDLE,
		ItemID.SEERS_HEADBAND_EASY, ItemID.SEERS_HEADBAND_MEDIUM, ItemID.SEERS_HEADBAND_HARD,
		ItemID.SEERS_HEADBAND_ELITE};
	static final DiaryItem SPADE = DiaryItem.anyOf("Spade", 1, ItemID.SPADE, SPADES);

	private ClueGuide()
	{
	}

	/**
	 * The next step for the tier.
	 *
	 * @param way the way to get a clue the player picked in the panel, or null for the best one they can do
	 * @param cluePlugin whether RuneLite's Clue Scroll plugin is on
	 */
	public static DiaryStep resolve(ClueTier tier, ClueHoldings h, DiaryPlayer p, String way, boolean cluePlugin)
	{
		return resolve(tier, h, p, way, cluePlugin, null);
	}

	/**
	 * The next step for the tier.
	 *
	 * @param step the clue step the player is on, read from RuneLite's Clue Scroll plugin, or null when unknown
	 */
	public static DiaryStep resolve(ClueTier tier, ClueHoldings h, DiaryPlayer p, String way, boolean cluePlugin,
		ClueStep step)
	{
		if (tier.isMembers() && !p.isMembers())
		{
			return DiaryStep.builder().kind(DiaryStep.Kind.LOCKED)
				.title(tier.getLabel() + " clues are members-only")
				.line("Hop to a members world, or pick Beginner: those work on free worlds.")
				.build();
		}
		ClueHoldings.Stack mimic = h.get(tier, ClueItem.Kind.MIMIC);
		if (mimic.getCarried() > 0)
		{
			return mimicStep(p);
		}
		ClueHoldings.Stack caskets = h.get(tier, ClueItem.Kind.REWARD_CASKET);
		if (caskets.getCarried() > 0)
		{
			return DiaryStep.builder().kind(DiaryStep.Kind.DO)
				.title("Open your " + tier.getLabel().toLowerCase() + " reward casket")
				.use(ids(caskets.getCarriedIds()))
				.line("1. Click the reward casket in your inventory (outlined) and choose Open.")
				.line("2. Make room first: a casket gives up to " + ClueRewards.maxRolls(tier) + " items.")
				.line("The chat tells you what it's worth. What it can give is under Rewards in the panel.")
				.build();
		}
		ClueHoldings.Stack clue = h.get(tier, ClueItem.Kind.CLUE);
		if (clue.total() > 0)
		{
			if (step == null)
			{
				return reword(DiaryGuide.resolve(readTask(tier, clue, p, cluePlugin), p));
			}
			if (step.getLocation() != null && Wilderness.contains(step.getLocation()) && !p.isAllowWilderness())
			{
				return DiaryStep.builder().kind(DiaryStep.Kind.LOCKED)
					.title("This clue step is in the Wilderness")
					.line("Other players can attack and kill you there, so the guide doesn't go there unless you "
						+ "turn on Wilderness in the panel.")
					.line("Turn it on (and bring only what you can lose), or drop the clue and get a new one.")
					.build();
			}
			DiaryStep out = reword(DiaryGuide.resolve(solveTask(tier, clue, step, p), p));
			if (out.getKind() == DiaryStep.Kind.DO && out.getDestination() == null && step.getLocation() != null
				&& radius(step) == 0)
			{
				// Keep the arrow on the dig tile while standing on it
				WorldPoint wp = step.getLocation();
				out = out.toBuilder().destination(Location.of("the dig spot", wp.getX(), wp.getY(), wp.getPlane(), 0))
					.build();
			}
			return out;
		}
		ClueHoldings.Stack containers = h.get(tier, ClueItem.Kind.CONTAINER);
		if (containers.getCarried() > 0)
		{
			return DiaryStep.builder().kind(DiaryStep.Kind.DO)
				.title("Open your clue bottle, nest or geode")
				.use(ids(containers.getCarriedIds()))
				.line("1. Click it in your inventory (outlined) and choose Open (or Crack for a geode).")
				.line("2. You get a " + tier.getLabel().toLowerCase() + " clue scroll.")
				.build();
		}
		ClueHoldings.Stack boxes = h.get(tier, ClueItem.Kind.SCROLL_BOX);
		if (boxes.getCarried() > 0)
		{
			return DiaryStep.builder().kind(DiaryStep.Kind.DO)
				.title("Open a scroll box")
				.use(ids(boxes.getCarriedIds()))
				.line("1. Click the scroll box in your inventory (outlined) and choose Open.")
				.line("2. You get a " + tier.getLabel().toLowerCase() + " clue scroll. Keep the other boxes for later: "
					+ "you can only hold one clue of each tier at a time.")
				.build();
		}
		if (boxes.getBanked() > 0 || containers.getBanked() > 0)
		{
			ClueHoldings.Stack from = boxes.getBanked() > 0 ? boxes : containers;
			String what = boxes.getBanked() > 0 ? "Scroll box" : "Clue bottle, nest or geode";
			return reword(DiaryGuide.resolve(fetchTask(tier, "box", what, array(from.getBankIds()), 1,
				"Open it for a " + tier.getLabel().toLowerCase() + " clue scroll."), p));
		}
		if (caskets.getBanked() > 0)
		{
			return reword(DiaryGuide.resolve(fetchTask(tier, "casket", "Reward casket (" + tier.key() + ")",
				array(caskets.getBankIds()), Math.min(caskets.getBanked(), 28),
				"Then open them one at a time (click, Open)."), p));
		}
		if (tier == ClueTier.MASTER)
		{
			return watson(h, p);
		}
		DiaryTask chosen = pickWay(tier, p, way);
		if (chosen == null)
		{
			return noWay(tier, p);
		}
		return reword(DiaryGuide.resolve(chosen, p));
	}

	/**
	 * The way the guide uses to get a clue: the picked one if it's for this tier, else the first one the player can do.
	 */
	public static DiaryTask pickWay(ClueTier tier, DiaryPlayer p, String picked)
	{
		List<DiaryTask> ways = ClueWays.ways(tier);
		for (DiaryTask t : ways)
		{
			if (t.key().equals(picked))
			{
				return t;
			}
		}
		for (DiaryTask t : ways)
		{
			if (DiaryGuide.missing(t, p).isEmpty() && DiaryGuide.obtainable(t, p))
			{
				return t;
			}
		}
		return null;
	}

	private static DiaryStep noWay(ClueTier tier, DiaryPlayer p)
	{
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder().kind(DiaryStep.Kind.LOCKED)
			.title("No guided way to get a " + tier.getLabel().toLowerCase() + " clue yet");
		for (DiaryTask t : ClueWays.ways(tier))
		{
			b.line("✘ " + t.label() + ": needs " + String.join(", ", DiaryGuide.missing(t, p)));
		}
		ClueWays.tips(tier).forEach(tip -> b.line("• " + tip));
		return b.build();
	}

	/**
	 * Hand Watson one easy, medium, hard and elite clue for a master clue.
	 */
	private static DiaryStep watson(ClueHoldings h, DiaryPlayer p)
	{
		List<ClueTier> lower = List.of(ClueTier.EASY, ClueTier.MEDIUM, ClueTier.HARD, ClueTier.ELITE);
		List<String> missing = new ArrayList<>();
		DiaryTask.DiaryTaskBuilder task = DiaryTask.builder()
			.id("clue.MASTER.watson")
			.name("Hand your clues to Watson")
			.text("Give Watson one easy, medium, hard and elite clue scroll for a master clue");
		for (ClueTier t : lower)
		{
			ClueHoldings.Stack clue = h.get(t, ClueItem.Kind.CLUE);
			if (clue.total() == 0)
			{
				missing.add(t.getLabel().toLowerCase());
			}
			else
			{
				task.item(DiaryItem.anyOf(t.getLabel() + " clue scroll", 1, 0, clue.ids()));
			}
		}
		if (!missing.isEmpty())
		{
			DiaryStep.DiaryStepBuilder b = DiaryStep.builder().kind(DiaryStep.Kind.LOCKED)
				.title("Collect one easy, medium, hard and elite clue for Watson");
			for (ClueTier t : lower)
			{
				b.line((missing.contains(t.getLabel().toLowerCase()) ? "✘ " : "✔ ") + t.getLabel() + " clue scroll");
			}
			b.line("Pick a tier you're missing in the panel to be guided to one. Don't do those clues: keep them for Watson.");
			b.line("Or open caskets: 1 in 5 elite caskets (and 1/15 hard, 1/30 medium, 1/50 easy) has a master clue.");
			return b.build();
		}
		task.stage(DiaryStage.builder()
			.title("Give your four clues to Watson")
			.at(ClueWays.WATSON)
			.npc("Watson")
			.line("Watson lives in the fenced-in house west of the Forthos ruins in Hosidius.")
			.line("1. Click Watson (outlined) and choose Talk-to.")
			.line("2. He takes the easy, medium, hard and elite clue scrolls and gives you a master clue.")
			.line("You can also hand them in one at a time, whenever you have them.")
			.build());
		return reword(DiaryGuide.resolve(task.build(), p));
	}

	private static DiaryStep mimicStep(DiaryPlayer p)
	{
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder().kind(DiaryStep.Kind.DO)
			.title("Your casket is the Mimic: fight it at Watson's house")
			.objects(Set.of("Strange casket"))
			.line("1. Bring weapon, armour, food and prayer potions; the Mimic is a boss.")
			.line("2. In Watson's house, click the Strange casket (outlined) and fight the Mimic.")
			.line("3. Beating it gives the casket's loot plus the Mimic's own rewards.");
		if (!ClueWays.WATSON.contains(p.getLocation()))
		{
			b.destination(ClueWays.WATSON);
		}
		return b.build();
	}

	/**
	 * Doing the clue the player holds when the step isn't known yet: take it out of the bank if needed and read it,
	 * so RuneLite's Clue Scroll plugin can work out the step and this guide can walk the player through it.
	 */
	static DiaryTask readTask(ClueTier tier, ClueHoldings.Stack clue, DiaryPlayer p, boolean cluePlugin)
	{
		DiaryTask.DiaryTaskBuilder t = solveBase(tier, clue);
		DiaryStage.DiaryStageBuilder s = DiaryStage.builder()
			.title("Read your " + tier.getLabel().toLowerCase() + " clue")
			.use(ids(clue.getCarriedIds()));
		if (!cluePlugin)
		{
			s.line("First turn on RuneLite's Clue Scroll plugin (the button in this panel). It knows the answer to "
				+ "every clue; this guide reads it and then tells you where to go and what to click.");
		}
		s.line("1. Click the clue in your inventory (outlined) and choose Read.")
			.line("2. This panel then shows the step: where to walk, what to bring and what to click.")
			.line("Got a puzzle box or light box instead? Click it and choose Open: RuneLite's Puzzle Solver plugin "
				+ "shows which tile to click next.");
		kitLines(tier, s, p, false);
		return t.stage(s.build()).build();
	}

	/**
	 * The clue step the player is on, as a guided task: bring the clue, a spade and the emote items (from the bank
	 * if needed), walk to the spot, then click exactly what the step needs.
	 */
	static DiaryTask solveTask(ClueTier tier, ClueHoldings.Stack clue, ClueStep step, DiaryPlayer p)
	{
		DiaryTask.DiaryTaskBuilder t = solveBase(tier, clue);
		WorldPoint where = step.getLocation();
		t.wilderness(where != null && Wilderness.contains(where));
		if (step.isSpade())
		{
			t.item(SPADE);
		}
		boolean haveLight = owns(p, LIGHTS);
		if (step.isLight() && haveLight)
		{
			t.item(DiaryItem.owned("Lit light source", "Light a lantern, candle or torch with a tinderbox.", LIGHTS));
		}
		List<String> missing = new ArrayList<>();
		List<String> wear = new ArrayList<>();
		for (ClueStep.Need n : step.getItems())
		{
			wear.add(n.getName());
			if (!n.isHave())
			{
				if (n.getBankId() > 0)
				{
					t.item(DiaryItem.of(n.getName(), n.getBankId()));
				}
				else
				{
					missing.add(n.getName());
				}
			}
		}

		DiaryStage.DiaryStageBuilder s = DiaryStage.builder().title(title(step));
		if (where != null)
		{
			s.at(Location.of(placeName(step), where.getX(), where.getY(), where.getPlane(), radius(step)));
		}
		s.npcs(step.getNpcs()).objects(step.getObjects());
		if (step.getSolution() != null && !step.getSolution().isEmpty())
		{
			s.line("The clue means: " + step.getSolution());
		}
		int n = 1;
		switch (step.getType())
		{
			case EMOTE:
				if (!missing.isEmpty())
				{
					s.line(step.getStash() != null
						? "Not carried or in your bank: " + String.join(", ", missing) + ". If you filled the "
						+ "STASH unit here, click it (outlined) and choose Search to take them out."
						: "You still need: " + String.join(", ", missing) + ". Get them first (Grand Exchange).");
				}
				if (step.getStash() != null)
				{
					s.object(step.getStash());
				}
				if (!wear.isEmpty())
				{
					s.line(n++ + ". Wear " + String.join(", ", wear) + ": click each in your inventory and choose "
						+ "Wear or Wield.");
				}
				if (!step.getEmotes().isEmpty())
				{
					s.line(n++ + ". Open the Emotes tab (the smiley face icon) and click " + String.join(", then ",
						step.getEmotes()) + ".");
				}
				s.line(n++ + ". Uri (a ghostly man) appears next to you: click him and choose Talk-to.");
				if (step.getEnemy() != null)
				{
					s.line("A " + step.getEnemy() + " may appear first and attack: kill it, then do the emotes again.");
				}
				break;
			case MUSIC:
				s.line(n++ + ". Open the Music tab (the music note icon) and click " + quote(step.getSong())
					+ " so it plays. You need to have unlocked it.");
				s.line(n++ + ". Click Cecilia (outlined) and choose Talk-to.");
				break;
			case HOT_COLD:
				s.line(n++ + ". Click the strange device in your inventory and choose Feel: the chat says how warm "
					+ "you are.");
				s.line(n++ + ". Walk where the arrow points and Feel again. RuneLite narrows the spot down each time.");
				s.line(n++ + ". When the device says you're right on it, click your spade (outlined) and choose Dig.");
				break;
			case FAIRY_RING:
				s.line("This clue is a fairy ring code: " + step.getText() + ". The ring nearest the spot is the "
					+ "quickest way there (needs Fairy Tale II started and a dramen or lunar staff).");
				s.line(n++ + ". Stand on the tile the arrow points at.");
				s.line(n++ + ". Click your spade in your inventory (outlined) and choose Dig.");
				break;
			default:
				if (!step.getNpcs().isEmpty())
				{
					s.line(n++ + ". Click " + String.join(" or ", step.getNpcs()) + " (outlined) and choose Talk-to.");
					if (step.getAnswer() != null && !step.getAnswer().isEmpty())
					{
						s.line(n++ + ". They ask you a question: type " + step.getAnswer() + " and press Enter.");
					}
				}
				else if (!step.getObjects().isEmpty())
				{
					s.line(n++ + ". Click the " + String.join(" or ", step.getObjects())
						+ " (outlined) and choose Search (or Open).");
				}
				else if (step.isSpade())
				{
					s.line(n++ + ". Stand on the tile the arrow points at.");
					s.line(n++ + ". Click your spade in your inventory (outlined) and choose Dig.");
				}
				else if (step.getText() != null)
				{
					s.line("The clue says: " + step.getText());
				}
				if (step.getType() == ClueStep.Type.CHALLENGE || step.getType() == ClueStep.Type.FALO)
				{
					if (!step.getItems().isEmpty())
					{
						s.line("Bring: " + String.join(", ", wear) + (missing.isEmpty() ? "."
							: " (still missing: " + String.join(", ", missing) + ")."));
					}
					if (step.getText() != null && step.getType() == ClueStep.Type.CHALLENGE)
					{
						s.line("Do this, then talk to them again: " + step.getText());
					}
				}
				if (step.getEnemy() != null)
				{
					s.line("A " + step.getEnemy() + " appears and attacks: kill it, then do it again.");
				}
				break;
		}
		if (step.isSpade())
		{
			s.use(SPADES[0]).use(SPADES[1]).use(SPADES[2]);
		}
		if (step.isLight() && !haveLight)
		{
			s.line("This spot is dark: bring a lit lantern, candle or torch (light it with a tinderbox).");
		}
		if (where == null && !step.getSpots().isEmpty())
		{
			s.line("It can be at " + step.getSpots().size() + " spots: the arrow points at the nearest.");
		}
		s.line("Done? The clue turns into the next step (or a reward casket) and this panel moves on by itself.");
		kitLines(tier, s, p, true);
		return t.stage(s.build()).build();
	}

	private static DiaryTask.DiaryTaskBuilder solveBase(ClueTier tier, ClueHoldings.Stack clue)
	{
		DiaryTask.DiaryTaskBuilder t = DiaryTask.builder()
			.id("clue." + tier.name() + ".solve")
			.name("Do your " + tier.getLabel().toLowerCase() + " clue")
			.text("Do your " + tier.getLabel().toLowerCase() + " clue")
			.freeToPlay(!tier.isMembers());
		if (clue.getCarried() == 0)
		{
			t.item(DiaryItem.owned(tier.getLabel() + " clue scroll", "It's in your bank.", clue.ids()));
		}
		return t;
	}

	private static void kitLines(ClueTier tier, DiaryStage.DiaryStageBuilder s, DiaryPlayer p, boolean known)
	{
		if (!known)
		{
			s.line("Keep a spade with you: many steps are digs.");
		}
		if (tier.ordinal() >= ClueTier.HARD.ordinal())
		{
			s.line("Some " + tier.getLabel().toLowerCase() + " steps make you kill something first: carry a weapon, "
				+ "armour and food.");
		}
	}

	private static String title(ClueStep step)
	{
		switch (step.getType())
		{
			case EMOTE:
				return "Do the emote" + (step.getEmotes().isEmpty() ? "" : ": " + String.join(", then ", step.getEmotes()));
			case MUSIC:
				return "Play " + quote(step.getSong()) + " to Cecilia";
			case HOT_COLD:
				return "Find the spot with the strange device";
			default:
				if (!step.getNpcs().isEmpty())
				{
					return "Talk to " + String.join(" or ", step.getNpcs());
				}
				if (!step.getObjects().isEmpty())
				{
					return "Search the " + String.join(" or ", step.getObjects());
				}
				return step.isSpade() ? "Dig here with your spade" : "Do the clue step";
		}
	}

	private static String placeName(ClueStep step)
	{
		if (step.getType() == ClueStep.Type.EMOTE && step.getSolution() != null)
		{
			return step.getSolution();
		}
		if (!step.getNpcs().isEmpty())
		{
			return String.join(" or ", step.getNpcs());
		}
		if (!step.getObjects().isEmpty())
		{
			return "the " + String.join(" or ", step.getObjects());
		}
		return step.isSpade() ? "the dig spot" : "the clue spot";
	}

	/**
	 * How close counts as there: dig spots need the exact tile, NPCs wander.
	 */
	static int radius(ClueStep step)
	{
		if (step.getType() == ClueStep.Type.EMOTE)
		{
			return 3;
		}
		if (!step.getNpcs().isEmpty())
		{
			return 6;
		}
		if (!step.getObjects().isEmpty())
		{
			return 3;
		}
		return step.isSpade() ? 0 : 3;
	}

	private static String quote(String s)
	{
		return "\"" + (s == null ? "the song" : s) + "\"";
	}

	/**
	 * Walk to a STASH unit, build it if needed, and put its emote items in. The plugin can't see whether a unit is
	 * built, so the steps cover both.
	 */
	public static DiaryTask stashTask(ClueData.Stash s)
	{
		ClueTier tier = s.getTier();
		DiaryTask.DiaryTaskBuilder t = DiaryTask.builder()
			.id("clue.stash." + s.key())
			.name("STASH: " + s.getName())
			.text("Build and fill the " + tier.getLabel().toLowerCase() + " STASH unit: " + s.getName())
			.wilderness(Wilderness.contains(s.getPoint()));
		for (int i = 0; i < s.getItems().size(); i++)
		{
			int id = i < s.getIds().size() ? s.getIds().get(i) : 0;
			if (id > 0)
			{
				t.item(DiaryItem.of(s.getItems().get(i), id));
			}
		}
		String lower = tier.getLabel().toLowerCase();
		WorldPoint wp = s.getPoint();
		return t.stage(DiaryStage.builder()
			.title("Build and fill the STASH unit")
			.at(Location.of(s.getName(), wp.getX(), wp.getY(), wp.getPlane(), 3))
			.objects(Set.of("Inconspicuous bush", "Inconspicuous crate", "Inconspicuous hole", "Inconspicuous rocks",
				"STASH (" + lower + ")", "STASH unit (" + lower + ")"))
			.line("It's " + s.getWhere() + ". Before it's built it looks like an ordinary bush, crate, hole or rocks.")
			.line("1. Not built yet? Bring a hammer, a saw, 2 " + tier.getPlankName().toLowerCase() + "s and 10 nails"
				+ (tier.getGoldLeaf() > 0 ? " and a gold leaf" : "") + ", and have " + tier.getStashLevel()
				+ " Construction (boosts work). Click it and choose Build.")
			.line("2. Once it's built, click it and choose Search, then put in: " + String.join(", ", s.getItems()) + ".")
			.line("Next time an emote clue sends you here, search it, wear the items, do the emote, and put them back.")
			.build())
			.build();
	}

	/**
	 * Take something out of the bank and use it.
	 */
	private static DiaryTask fetchTask(ClueTier tier, String slug, String name, int[] ids, int amount, String then)
	{
		return DiaryTask.builder()
			.id("clue." + tier.name() + "." + slug)
			.name("Take out your " + name.toLowerCase())
			.text("Take out your " + name.toLowerCase())
			.freeToPlay(!tier.isMembers())
			.item(DiaryItem.owned(name, "It's in your bank.", ids).withAmount(amount))
			.stage(DiaryStage.builder().title(name).line(then).build())
			.build();
	}

	/**
	 * The shared guide engine talks about diary "tasks"; here the player is getting or doing a clue.
	 */
	public static DiaryStep reword(DiaryStep step)
	{
		List<String> lines = new ArrayList<>();
		step.getLines().forEach(l -> lines.add(words(l)));
		return step.toBuilder().title(words(step.getTitle())).clearLines().lines(lines).build();
	}

	private static String words(String s)
	{
		return s == null ? null : s.replace("what this task needs", "what you need")
			.replace("this task", "this")
			.replace("another task", "another way")
			.replace("the next one when you skip this", "another one");
	}

	static boolean owns(DiaryPlayer p, int[] ids)
	{
		return Arrays.stream(ids).anyMatch(id -> p.owned(id) > 0);
	}

	private static int[] array(List<Integer> ids)
	{
		return ids.stream().mapToInt(Integer::intValue).toArray();
	}

	private static Set<Integer> ids(List<Integer> ids)
	{
		return new HashSet<>(ids);
	}
}
