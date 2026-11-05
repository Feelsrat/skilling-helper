package com.skillinghelper.diary;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.ToolType;
import com.skillinghelper.diary.data.Diaries;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryReward;
import com.skillinghelper.diary.data.DiaryStage;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.diary.data.DiaryTier;
import com.skillinghelper.engine.Format;
import com.skillinghelper.shops.ShopPlan;
import com.skillinghelper.shops.ShopPlanner;
import com.skillinghelper.travel.TravelPick;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/**
 * Turns a diary tier into the next thing to do, Quest Helper style, for someone who has never done the tasks:
 * pick the next task the player can do (in the tier's walking order), get what it needs from the bank or a shop,
 * walk there, then click what's outlined. When every task is done, claim the reward. Recomputed every tick.
 */
public final class DiaryGuide
{
	static final Set<String> BANK_NPCS = Set.of("Banker", "Gnome banker", "Ghost banker");
	static final Set<String> BANK_OBJECTS = Set.of("Bank booth", "Bank chest", "Grand Exchange booth");
	static final Set<String> GE_CLERKS = Set.of("Grand Exchange Clerk");
	/**
	 * Bought tools when none is owned: the best of these the player can use (all on the Grand Exchange).
	 */
	private static final Set<String> BUYABLE_TOOLS = Set.of("Rune", "Adamant", "Mithril", "Steel", "Iron", "Bronze");

	private DiaryGuide()
	{
	}

	// ---- Requirements ----

	/**
	 * What the player still needs before they can do the task, e.g. "7 Slayer (you have 3)". Empty when ready.
	 * Notes the plugin can't check aren't included (see {@link DiaryTask#getNotes()}).
	 */
	public static List<String> missing(DiaryTask t, DiaryPlayer p)
	{
		List<String> out = new ArrayList<>();
		if (!p.isMembers() && !t.isFreeToPlay())
		{
			out.add("A members world");
		}
		for (Map.Entry<Skill, Integer> e : t.getSkills().entrySet())
		{
			Skill s = e.getKey();
			boolean boostable = !t.getUnboostable().contains(s);
			int have = boostable ? p.boostedLevel(s) : p.level(s);
			if (have < e.getValue())
			{
				out.add(e.getValue() + " " + s.getName() + " (you have " + p.level(s) + ")"
					+ (boostable ? "" : ", no boosts"));
			}
		}
		if (t.getCombat() > 0 && p.getCombatLevel() < t.getCombat())
		{
			out.add("Combat level " + t.getCombat() + " (you have " + p.getCombatLevel() + ")");
		}
		for (Quest q : t.getQuests())
		{
			if (p.quest(q) != QuestState.FINISHED)
			{
				out.add("Quest: " + q.getName());
			}
		}
		for (Quest q : t.getQuestsStarted())
		{
			if (p.quest(q) == QuestState.NOT_STARTED)
			{
				out.add("Start the quest " + q.getName());
			}
		}
		for (DiaryTask.VarReq r : t.getVarReqs())
		{
			int have = r.getVarp() > 0 ? p.getVarp().applyAsInt(r.getVarp()) : p.getVarbit().applyAsInt(r.getVarbit());
			if (have < r.getMin())
			{
				out.add(r.getMin() + " " + r.getLabel() + " (you have " + have + ")");
			}
		}
		if (t.isWilderness() && !p.isAllowWilderness())
		{
			out.add("Wilderness turned off in the panel");
		}
		return out;
	}

	/**
	 * Whether the game says the task is done, from its varp bit or varbit.
	 */
	public static boolean isDone(DiaryTask t, IntUnaryOperator varp,
		IntUnaryOperator varbit)
	{
		if (t.getVarp() > 0)
		{
			return (varp.applyAsInt(t.getVarp()) >>> t.getBit() & 1) != 0;
		}
		return varbit.applyAsInt(t.getVarbit()) >= t.getVarbitDone();
	}

	public static boolean done(DiaryTask t, DiaryPlayer p)
	{
		return p.getDone().contains(t.key());
	}

	public static boolean claimed(DiaryRegion r, DiaryTier tier, DiaryPlayer p)
	{
		return p.getClaimed().contains(r.name() + "." + tier.name());
	}

	/**
	 * The task to do next: the one the player picked, else the first one in walking order they can do now.
	 * Null when nothing is left that they can do.
	 */
	public static DiaryTask next(List<DiaryTask> tasks, DiaryPlayer p, Set<String> skipped, String picked)
	{
		for (DiaryTask t : tasks)
		{
			if (t.key().equals(picked) && !done(t, p))
			{
				return t;
			}
		}
		for (DiaryTask t : tasks)
		{
			if (!done(t, p) && !skipped.contains(t.key()) && missing(t, p).isEmpty() && obtainable(t, p))
			{
				return t;
			}
		}
		return null;
	}

	/**
	 * Whether every item the task needs can be bought or is owned, so the guide won't get stuck on it.
	 */
	public static boolean obtainable(DiaryTask t, DiaryPlayer p)
	{
		for (DiaryItem item : t.getItems())
		{
			if (required(item, p) && item.getBuyId() <= 0 && item.getTool() == null && ownedCount(item, p) < item.getAmount())
			{
				return false;
			}
		}
		return true;
	}

	// ---- Steps ----

	/**
	 * The next step for a whole tier.
	 */
	public static DiaryStep resolve(DiaryRegion region, DiaryTier tier, DiaryPlayer p, Set<String> skipped,
		String picked)
	{
		List<DiaryTask> tasks = Diaries.tasks(region, tier);
		List<DiaryTask> left = new ArrayList<>();
		for (DiaryTask t : tasks)
		{
			if (!done(t, p))
			{
				left.add(t);
			}
		}
		if (left.isEmpty() && !tasks.isEmpty())
		{
			return claimed(region, tier, p) ? doneStep(region, tier, p) : claimStep(region, tier, p);
		}
		DiaryTask task = next(tasks, p, skipped, picked);
		if (task == null)
		{
			return lockedTier(left, p, skipped);
		}
		return resolve(task, p);
	}

	/**
	 * The next step for one task.
	 */
	public static DiaryStep resolve(DiaryTask t, DiaryPlayer p)
	{
		List<String> missing = missing(t, p);
		if (!missing.isEmpty())
		{
			DiaryStep.DiaryStepBuilder b = step(DiaryStep.Kind.LOCKED, t).title("You can't do this task yet");
			b.line("Still needed:");
			missing.forEach(m -> b.line("✘ " + m));
			b.line("Pick another task in the panel, or the guide picks the next one when you skip this.");
			return b.build();
		}

		int stageIndex = stageIndex(t, p);
		DiaryStage stage = t.getStages().get(stageIndex);
		if (!stage.isStarted())
		{
			DiaryStep supplies = supplies(t, p);
			if (supplies != null)
			{
				return supplies;
			}
		}

		WorldPoint me = p.getLocation();
		boolean inside = stage.getInside() != null && stage.getInside().contains(me);
		Location at = stage.getAt();
		if (at != null && !inside && !at.contains(me))
		{
			DiaryStep.DiaryStepBuilder b = step(DiaryStep.Kind.TRAVEL, t)
				.title("Walk to " + at.getName())
				.destination(at);
			// No "N tiles away" line: it would change every tick and redraw the panel the whole walk
			b.line("Follow the yellow arrow, or the line on the ground if you have the Shortest Path plugin.");
			for (TravelPick travel : p.getTravel())
			{
				b.line(travel.isCarried() ? travel.carriedLine() : travel.tipLine());
			}
			b.line("Then: " + stage.getTitle());
			return b.build();
		}

		DiaryStep.DiaryStepBuilder b = step(DiaryStep.Kind.DO, t)
			.title(stage.getTitle())
			.npcs(stage.getNpcs())
			.objects(stage.getObjects())
			.use(stage.getUse());
		stage.getLines().forEach(b::line);
		if (t.getStages().size() > 1)
		{
			b.line("Part " + (stageIndex + 1) + " of " + t.getStages().size() + " of: " + t.getText());
		}
		return b.build();
	}

	/**
	 * Which part of the task the player is on: the last stage whose condition holds, else the first.
	 */
	static int stageIndex(DiaryTask t, DiaryPlayer p)
	{
		List<DiaryStage> stages = t.getStages();
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
		return 0;
	}

	private static DiaryStep claimStep(DiaryRegion region, DiaryTier tier, DiaryPlayer p)
	{
		Location spot = region.getTaskmasterSpot();
		String who = region.getTaskmasters().get(0);
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder().kind(DiaryStep.Kind.CLAIM)
			.title("Every " + tier.getLabel() + " task is done: claim your reward from " + who)
			.npcs(new HashSet<>(region.getTaskmasters()));
		if (!spot.contains(p.getLocation()))
		{
			b.destination(spot).line("Walk to " + who + " (" + spot.getName() + "), follow the arrow.");
		}
		List<DiaryTier> toClaim = new ArrayList<>();
		for (DiaryTier earlier = tier.previous(); earlier != null; earlier = earlier.previous())
		{
			if (!claimed(region, earlier, p))
			{
				toClaim.add(0, earlier);
			}
		}
		b.line("1. Click " + who + " (outlined) and choose Talk-to.");
		b.line("2. Pick \"I have a question about my Achievement Diary.\" and claim your reward.");
		if (!toClaim.isEmpty())
		{
			b.line("You have to claim the earlier tiers first, so talk to " + who + " once for each: "
				+ labels(toClaim) + ", then " + tier.getLabel() + ".");
		}
		b.line("Make sure you have a free inventory slot for the reward and the lamp.");
		DiaryReward reward = Diaries.reward(region, tier);
		if (reward != null)
		{
			b.line("You get: " + reward.getItem() + ".");
		}
		return b.build();
	}

	private static DiaryStep doneStep(DiaryRegion region, DiaryTier tier, DiaryPlayer p)
	{
		DiaryReward reward = Diaries.reward(region, tier);
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder().kind(DiaryStep.Kind.DONE)
			.title(region.getLabel() + " " + tier.getLabel() + " is complete");
		if (reward != null)
		{
			b.line("✔ You have the " + reward.getItem() + ". Keep it in your bank: its perks only work while you have it.");
		}
		DiaryTier next = tier.ordinal() + 1 < DiaryTier.values().length ? DiaryTier.values()[tier.ordinal() + 1] : null;
		if (next != null)
		{
			b.line("Next: pick " + next.getLabel() + " in the panel.");
		}
		return b.build();
	}

	private static DiaryStep lockedTier(List<DiaryTask> left, DiaryPlayer p, Set<String> skipped)
	{
		DiaryStep.DiaryStepBuilder b = DiaryStep.builder().kind(DiaryStep.Kind.LOCKED)
			.title("Nothing left you can do in this tier yet");
		for (DiaryTask t : left)
		{
			List<String> missing = missing(t, p);
			if (skipped.contains(t.key()))
			{
				b.line("• " + t.label() + ": skipped (click it in the list to do it)");
			}
			else if (!missing.isEmpty())
			{
				b.line("✘ " + t.label() + ": needs " + String.join(", ", missing));
			}
			else
			{
				b.line("✘ " + t.label() + ": needs " + unobtainable(t, p));
			}
		}
		return b.build();
	}

	// ---- Supplies ----

	/**
	 * One item still to get for the task: the id to take out or buy and how many.
	 */
	@Value
	static class Need
	{
		DiaryItem item;
		int id;
		int amount;
		int inBank;
	}

	/**
	 * The bank or shop step for whatever the task still needs, or null when the player carries it all.
	 */
	static DiaryStep supplies(DiaryTask t, DiaryPlayer p)
	{
		List<Need> needs = needs(t, p);
		if (needs.isEmpty())
		{
			return null;
		}
		for (Need n : needs)
		{
			if (n.getId() <= 0)
			{
				return step(DiaryStep.Kind.GET_ITEM, t)
					.title("Get a " + n.getItem().getName() + " first")
					.line(n.getItem().getHowToGet() != null ? n.getItem().getHowToGet()
						: "It can't be bought on the Grand Exchange.")
					.line("Or pick another task in the panel.")
					.build();
			}
		}

		Map<Integer, Integer> withdraw = new LinkedHashMap<>();
		Map<Need, Integer> toBuy = new LinkedHashMap<>();
		boolean bankKnown = p.getBank() != null;
		for (Need n : needs)
		{
			withdraw.merge(n.getId(), n.getAmount(), Integer::sum);
			if (bankKnown && n.getInBank() < n.getAmount())
			{
				toBuy.put(n, n.getAmount() - n.getInBank());
			}
		}

		ShopPlan shop = toBuy.isEmpty() || !p.isPreferShops() ? null : shopPlan(toBuy, p);
		if (shop != null && !p.isBankOpen())
		{
			DiaryStep.DiaryStepBuilder b = step(DiaryStep.Kind.BUY, t)
				.title(shop.title())
				.withdraw(withdraw)
				.npcs(new HashSet<>(shop.getShop().getNpcs()));
			Location where = shop.getShop().getLocation();
			if (!where.contains(p.getLocation()))
			{
				b.destination(where).line("Walk to " + where.getName() + " (follow the arrow).");
			}
			shop.describe().forEach(b::line);
			b.line("Have the coins in your inventory (take them out of a bank first).");
			b.line("Then open a bank and the guide moves on.");
			return b.build();
		}
		if (!toBuy.isEmpty() && !p.isBankOpen())
		{
			Location ge = Banks.GRAND_EXCHANGE;
			Set<String> npcs = new HashSet<>(GE_CLERKS);
			npcs.addAll(BANK_NPCS);
			DiaryStep.DiaryStepBuilder b = step(DiaryStep.Kind.BUY, t)
				.title("Buy what this task needs at the Grand Exchange")
				.withdraw(withdraw)
				.npcs(npcs)
				.objects(BANK_OBJECTS);
			if (!ge.contains(p.getLocation()))
			{
				b.destination(ge).line("Walk to the Grand Exchange in north-west Varrock (follow the arrow).");
			}
			b.line("Buy:");
			long total = 0;
			for (Map.Entry<Need, Integer> e : toBuy.entrySet())
			{
				Need n = e.getKey();
				b.line("- " + amount(n.getItem(), n.getId(), e.getValue()) + price(n, e.getValue(), p));
				total += (long) e.getValue() * Math.max(0, p.getBuyPrice().applyAsInt(n.getId()));
			}
			if (total > 0)
			{
				b.line("That's about " + Format.gp(total) + " at Grand Exchange prices.");
			}
			b.line("1. Have coins in your inventory (take them out of a bank first).");
			b.line("2. Click a Grand Exchange Clerk (outlined) and choose Exchange.");
			b.line("3. Click the Buy arrow on an empty slot and type the item's name.");
			b.line("4. Set the quantity, press +5% once or twice so it buys fast, then Confirm.");
			b.line("5. When it's done, click the slot and Collect to bank.");
			b.line("6. Then open the bank (booths outlined) and the guide moves on.");
			return b.build();
		}
		if (!p.isBankOpen())
		{
			Location bank = Banks.nearest(p.getLocation(), p.isMembers());
			DiaryStep.DiaryStepBuilder b = step(DiaryStep.Kind.OPEN_BANK, t)
				.title("Open your bank")
				.withdraw(withdraw)
				.npcs(BANK_NPCS)
				.objects(BANK_OBJECTS);
			if (bank != null && !bank.contains(p.getLocation()))
			{
				b.destination(bank).line("Walk to " + bank.getName() + " (follow the arrow).");
			}
			b.line("Click a bank booth or banker (outlined) and choose Bank.");
			b.line("For \"" + t.label() + "\" you need:");
			needs.forEach(n -> b.line("- " + amount(n.getItem(), n.getId(), n.getAmount())));
			travelLines(b, p);
			return b.build();
		}
		DiaryStep.DiaryStepBuilder b = step(DiaryStep.Kind.WITHDRAW, t)
			.title("Take out what this task needs")
			.withdraw(withdraw);
		b.line("Take these out (outlined in the bank):");
		needs.forEach(n -> b.line("- " + amount(n.getItem(), n.getId(), n.getAmount())));
		travelLines(b, p);
		b.line("To take an exact number, right-click the item and choose Withdraw-X, then type it.");
		if (!toBuy.isEmpty())
		{
			b.line("You don't have enough of some of these: take out coins and buy them at the Grand Exchange"
				+ (shop != null ? " or " + shop.getShop().getName() : "") + ".");
		}
		return b.build();
	}

	/**
	 * What the task still needs, picking the best owned version of each item: what's carried first, then the best
	 * one in the bank, else the one to buy (id 0 when it can't be bought).
	 */
	static List<Need> needs(DiaryTask t, DiaryPlayer p)
	{
		List<Need> out = new ArrayList<>();
		for (DiaryItem item : t.getItems())
		{
			if (!required(item, p))
			{
				continue;
			}
			int[] ids = usableIds(item, p);
			int carried = 0;
			for (int id : ids)
			{
				carried += p.getCarried().getOrDefault(id, 0);
			}
			if (carried >= item.getAmount())
			{
				continue;
			}
			int want = item.getAmount() - carried;
			int bestBanked = 0;
			for (int id : ids)
			{
				int inBank = p.getBank() == null ? 0 : p.getBank().getOrDefault(id, 0);
				if (inBank > 0)
				{
					bestBanked = id;
					break;
				}
			}
			if (bestBanked > 0)
			{
				out.add(new Need(item, bestBanked, want, p.getBank().get(bestBanked)));
			}
			else
			{
				out.add(new Need(item, buyId(item, p), want, 0));
			}
		}
		return out;
	}

	/**
	 * What the task costs in gold: coin fees, plus the Grand Exchange price of each item the player doesn't own
	 * enough of yet. Things that can't be bought aren't counted.
	 */
	public static long cost(DiaryTask t, DiaryPlayer p)
	{
		long gp = 0;
		for (DiaryItem item : t.getItems())
		{
			if (!required(item, p))
			{
				continue;
			}
			if (isCoins(item))
			{
				gp += item.getAmount();
				continue;
			}
			int buy = buyId(item, p);
			int missing = item.getAmount() - ownedCount(item, p);
			if (buy > 0 && missing > 0)
			{
				gp += (long) missing * Math.max(0, p.getBuyPrice().applyAsInt(buy));
			}
		}
		return gp;
	}

	private static boolean isCoins(DiaryItem item)
	{
		return item.getIds().length > 0 && item.getIds()[0] == ItemID.COINS;
	}

	private static String price(Need n, int amount, DiaryPlayer p)
	{
		long each = Math.max(0, p.getBuyPrice().applyAsInt(n.getId()));
		return each > 0 ? " (~" + Format.gp(each * amount) + ")" : "";
	}

	private static boolean required(DiaryItem item, DiaryPlayer p)
	{
		return item.getAmount() > 0 && (item.getSkipVarbit() <= 0 || p.getVarbit().applyAsInt(item.getSkipVarbit()) <= 0);
	}

	/**
	 * The ids that count for the item, best first. For tools only the tiers the player has the level for.
	 */
	static int[] usableIds(DiaryItem item, DiaryPlayer p)
	{
		if (item.getTool() == null)
		{
			return item.getIds();
		}
		List<Integer> ids = new ArrayList<>();
		for (ToolType.Tier tier : item.getTool().getTiers())
		{
			if (p.level(item.getTool().getSkill()) >= tier.getLevel())
			{
				for (int id : tier.getIds())
				{
					ids.add(id);
				}
			}
		}
		return ids.stream().mapToInt(Integer::intValue).toArray();
	}

	private static int buyId(DiaryItem item, DiaryPlayer p)
	{
		if (item.getTool() == null)
		{
			return item.getBuyId();
		}
		for (ToolType.Tier tier : item.getTool().getTiers())
		{
			String metal = tier.getName().split(" ")[0];
			if (BUYABLE_TOOLS.contains(metal) && p.level(item.getTool().getSkill()) >= tier.getLevel())
			{
				return tier.getIds()[0];
			}
		}
		return 0;
	}

	private static int ownedCount(DiaryItem item, DiaryPlayer p)
	{
		int n = 0;
		for (int id : usableIds(item, p))
		{
			n += p.owned(id);
		}
		return n;
	}

	private static String unobtainable(DiaryTask t, DiaryPlayer p)
	{
		for (DiaryItem item : t.getItems())
		{
			if (required(item, p) && item.getBuyId() <= 0 && item.getTool() == null && ownedCount(item, p) < item.getAmount())
			{
				return item.getName() + (item.getHowToGet() != null ? " (" + item.getHowToGet() + ")" : "");
			}
		}
		return "nothing";
	}

	private static ShopPlan shopPlan(Map<Need, Integer> toBuy, DiaryPlayer p)
	{
		List<ShopPlanner.Want> wants = new ArrayList<>();
		toBuy.forEach((n, amount) -> wants.add(new ShopPlanner.Want(n.getId(), n.getItem().getName(), amount,
			p.getBuyPrice().applyAsInt(n.getId()))));
		return ShopPlanner.plan(wants, p.getLocation(), p.isMembers());
	}

	private static void travelLines(DiaryStep.DiaryStepBuilder b, DiaryPlayer p)
	{
		Set<Integer> ids = new HashSet<>();
		for (TravelPick travel : p.getTravel())
		{
			if (!travel.isCarried())
			{
				b.line("- " + travel.bankLine());
				ids.add(travel.getItemId());
			}
		}
		if (!ids.isEmpty())
		{
			b.travel(ids);
		}
	}

	private static String amount(DiaryItem item, int id, int n)
	{
		String name = item.getName();
		if (item.getTool() != null)
		{
			for (ToolType.Tier tier : item.getTool().getTiers())
			{
				if (tier.matches(id))
				{
					name = tier.getName();
				}
			}
		}
		return (n > 1 ? String.format("%,d", n) + " x " : "") + name;
	}

	private static String labels(List<DiaryTier> tiers)
	{
		List<String> out = new ArrayList<>();
		tiers.forEach(t -> out.add(t.getLabel()));
		return String.join(", ", out);
	}

	private static DiaryStep.DiaryStepBuilder step(DiaryStep.Kind kind, DiaryTask t)
	{
		return DiaryStep.builder().kind(kind).taskKey(t.key());
	}
}
