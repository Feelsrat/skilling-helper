package com.skillinghelper;

import com.skillinghelper.data.BoostItem;
import com.skillinghelper.data.CombatGear;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.SkillOutfit;
import com.skillinghelper.data.Target;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.MethodPlanner;
import com.skillinghelper.engine.PlanSegment;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.StepResolver;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * Snapshot of everything the guide engine says for every method in a range of player situations: the requirement
 * checklist, the next step (text, route, highlights, bank amounts) and the planner's ranking and plan.
 * <p>
 * It pins today's behaviour so refactoring can't change what players see by accident. Each case is stored as a
 * short hash in {@code guide-snapshots.txt}. When a change is meant to alter the guide, first run the tests to see
 * which cases change and check their text, then regenerate the file with {@code UPDATE_SNAPSHOTS=true ./gradlew test}.
 * The full text of every case that differs is written to {@code build/guide-snapshots-actual/}.
 */
public class GuideSnapshotTest
{
	private static final String RESOURCE = "/guide-snapshots.txt";
	private static final Path SOURCE = Paths.get("src/test/resources/guide-snapshots.txt");
	private static final Path ACTUAL_DIR = Paths.get("build/guide-snapshots-actual");
	private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);

	/**
	 * Varied prices, some unknown (0), so costs, affordability and make-or-buy choices all get exercised.
	 */
	private static final PriceLookup PRICES = new PriceLookup()
	{
		@Override
		public int price(int itemId)
		{
			return itemId % 7 == 0 ? 0 : itemId % 997 + 1;
		}

		@Override
		public int sellPrice(int itemId)
		{
			return itemId % 7 == 0 ? 0 : itemId % 1009 + 3;
		}
	};

	private final GuideRepository guides = new GuideRepository();
	private final StepResolver resolver = new StepResolver(PRICES);
	private final MethodPlanner planner = new MethodPlanner(PRICES);

	@Test
	public void guideOutputMatchesSnapshot() throws IOException
	{
		Map<String, String> actual = new TreeMap<>();
		for (Method m : guides.all())
		{
			for (Map.Entry<String, PlayerState> scenario : scenarios(m).entrySet())
			{
				actual.put(m.getId() + " " + scenario.getKey(), describeStep(m, scenario.getValue()));
			}
		}
		for (Skill skill : Skill.values())
		{
			if (!guides.hasGuide(skill))
			{
				continue;
			}
			for (int level : new int[]{1, 30, 60, 90})
			{
				for (boolean rich : new boolean[]{false, true})
				{
					for (TrainingPreference pref : TrainingPreference.values())
					{
						actual.put("plan " + skill + " " + level + (rich ? " rich " : " poor ") + pref,
							describePlan(skill, planState(skill, level, rich), pref, Math.min(99, level + 15)));
					}
				}
			}
		}

		if (Boolean.getBoolean("snapshots.update") || "true".equals(System.getenv("UPDATE_SNAPSHOTS")))
		{
			writeSnapshots(actual);
			return;
		}

		Map<String, String> expected = readSnapshots();
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, String> e : actual.entrySet())
		{
			String want = expected.get(e.getKey());
			if (want == null)
			{
				problems.add("new case: " + e.getKey());
			}
			else if (!want.equals(hash(e.getValue())))
			{
				problems.add("changed: " + e.getKey());
				writeActual(e.getKey(), e.getValue());
			}
		}
		for (String key : expected.keySet())
		{
			if (!actual.containsKey(key))
			{
				problems.add("missing case: " + key);
			}
		}
		if (!problems.isEmpty())
		{
			fail(problems.size() + " guide snapshot(s) differ (full text in " + ACTUAL_DIR + "):\n"
				+ String.join("\n", problems.subList(0, Math.min(40, problems.size()))));
		}
		assertTrue("snapshot cases", actual.size() > 1000);
	}

	// ---- Scenarios ----

	private static Map<String, PlayerState> scenarios(Method m)
	{
		Map<String, PlayerState> out = new TreeMap<>();
		int level = Math.max(1, m.getMinLevel());
		for (boolean strong : new boolean[]{false, true})
		{
			String p = strong ? "strong " : "weak ";
			PlayerState base = playerAt(m, level, strong);
			ItemCounts everything = everything(m);
			ItemCounts kit = kit(m);
			WorldPoint spot = m.getLocation().getPoint();
			WorldPoint bank = m.getBank() == null ? LUMBRIDGE : m.getBank().getPoint();

			out.put(p + "bank-unknown", base.toBuilder().location(LUMBRIDGE).build());
			out.put(p + "bank-empty", base.toBuilder().location(LUMBRIDGE).bank(ItemCounts.EMPTY)
				.inventory(ItemCounts.of(ItemID.COINS, 10_000)).build());
			out.put(p + "bank-empty-rich", base.toBuilder().location(LUMBRIDGE).bank(ItemCounts.of(ItemID.COINS, 50_000_000))
				.build());
			out.put(p + "owns-all-in-bank", base.toBuilder().location(LUMBRIDGE).bank(everything).build());
			out.put(p + "owns-all-at-bank", base.toBuilder().location(bank).bank(everything).build());
			out.put(p + "kit-at-spot", base.toBuilder().location(spot).bank(everything).inventory(kit)
				.inventorySlots(kit).freeInventorySlots(10).build());
			out.put(p + "kit-away", base.toBuilder().location(LUMBRIDGE).bank(everything).inventory(kit)
				.inventorySlots(kit).freeInventorySlots(10).build());
			out.put(p + "kit-worn-at-spot", base.toBuilder().location(spot).bank(everything).inventory(kit)
				.equipment(kit).inventorySlots(kit).freeInventorySlots(10).build());
			ItemCounts full = withProducts(kit, m);
			out.put(p + "full-of-products", base.toBuilder().location(spot).bank(everything).inventory(full)
				.inventorySlots(full).freeInventorySlots(0).build());
			ItemCounts parts = withParts(kit, m);
			ItemCounts partsOnly = withoutMadeInputs(everything, m);
			out.put(p + "parts-carried", base.toBuilder().location(spot).bank(partsOnly).inventory(parts)
				.inventorySlots(parts).freeInventorySlots(5).build());
			out.put(p + "parts-carried-away", base.toBuilder().location(LUMBRIDGE).bank(partsOnly).inventory(parts)
				.inventorySlots(parts).freeInventorySlots(5).build());
			out.put(p + "parts-in-bank", base.toBuilder().location(bank).bank(partsOnly).build());
			ItemCounts geared = withGear(kit, m);
			out.put(p + "gear-carried", base.toBuilder().location(spot).bank(everything).inventory(geared)
				.inventorySlots(geared).freeInventorySlots(5).build());
			out.put(p + "goal-reached", playerAt(m, 99, strong).toBuilder().location(spot).bank(everything).build());
			out.put(p + "in-instance", base.toBuilder().inInstance(true).location(spot).bank(everything).inventory(kit)
				.build());
			if (!m.getStops().isEmpty())
			{
				out.put(p + "run-all-done", base.toBuilder().location(spot).bank(everything).inventory(kit)
					.stopWaits(waits(m, m.getStops().size())).build());
				out.put(p + "run-half-done", base.toBuilder().location(m.getStops().get(m.getStops().size() - 1)
					.getLocation().getPoint()).bank(everything).inventory(kit)
					.stopWaits(waits(m, m.getStops().size() / 2)).build());
			}
		}
		return out;
	}

	private static PlayerState playerAt(Method m, int level, boolean strong)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			int l = s == m.getSkill() ? level : strong ? 70 : 1;
			levels.put(s, l);
			xp.put(s, Experience.getXpForLevel(l) + (s == m.getSkill() ? 1234 : 0));
		}
		return PlayerState.builder()
			.levels(levels)
			.xp(xp)
			.freeInventorySlots(28)
			.membersWorld(strong)
			.completedQuests(strong && !m.getQuests().isEmpty() ? EnumSet.copyOf(m.getQuests())
				: EnumSet.noneOf(Quest.class))
			.build();
	}

	private static PlayerState planState(Skill skill, int level, boolean rich)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			int l = s == skill ? level : rich ? 60 : 1;
			levels.put(s, l);
			xp.put(s, Experience.getXpForLevel(l));
		}
		return PlayerState.builder()
			.levels(levels)
			.xp(xp)
			.freeInventorySlots(28)
			.membersWorld(rich)
			.bank(rich ? ItemCounts.of(ItemID.COINS, 200_000_000) : ItemCounts.EMPTY)
			.build();
	}

	/**
	 * Lots of every item the method could use, plus what it makes, and plenty of coins.
	 */
	private static ItemCounts everything(Method m)
	{
		Map<Integer, Integer> items = new HashMap<>();
		for (int id : relatedIds(m))
		{
			items.put(id, 5_000);
		}
		items.put(ItemID.COINS, 100_000_000);
		return new ItemCounts(items);
	}

	/**
	 * What the player would carry to train: one of each tool and a trip's worth of each input.
	 */
	private static ItemCounts kit(Method m)
	{
		Map<Integer, Integer> items = new HashMap<>();
		if (m.getToolType() != null)
		{
			int[] ids = m.getToolType().allIds();
			items.put(ids[ids.length - 1], 1);
		}
		for (ItemReq tool : m.getTools())
		{
			items.put(tool.primaryId(), Math.max(1, tool.getQuantity()));
		}
		for (ItemReq input : m.getInputs())
		{
			items.put(input.primaryId(), Math.max(1, input.getQuantity()));
		}
		return new ItemCounts(items);
	}

	private static ItemCounts withProducts(ItemCounts kit, Method m)
	{
		Map<Integer, Integer> items = new HashMap<>(kit.asMap());
		for (int id : m.getProducts())
		{
			items.merge(id, 10, Integer::sum);
		}
		return new ItemCounts(items);
	}

	/**
	 * The kit with every input that has a recipe swapped for enough of its parts to make some.
	 */
	private static ItemCounts withParts(ItemCounts kit, Method m)
	{
		Map<Integer, Integer> items = new HashMap<>(kit.asMap());
		for (ItemReq input : m.getInputs())
		{
			if (!input.hasRecipe())
			{
				continue;
			}
			items.remove(input.primaryId());
			for (ItemReq part : input.getRecipe())
			{
				items.put(part.primaryId(), Math.max(1, part.getPerAction() * 5));
			}
		}
		return new ItemCounts(items);
	}

	private static ItemCounts withoutMadeInputs(ItemCounts bank, Method m)
	{
		Map<Integer, Integer> items = new HashMap<>(bank.asMap());
		for (ItemReq input : m.getInputs())
		{
			if (input.hasRecipe())
			{
				for (int id : input.getIds())
				{
					items.remove(id);
				}
			}
		}
		return new ItemCounts(items);
	}

	/**
	 * The kit plus one piece of every skilling outfit, boost item and combat gear tier the method suggests.
	 */
	private static ItemCounts withGear(ItemCounts kit, Method m)
	{
		Map<Integer, Integer> items = new HashMap<>(kit.asMap());
		for (BoostItem b : BoostItem.values())
		{
			if (b.appliesTo(m))
			{
				items.put(b.getIds()[0], 1);
			}
		}
		for (SkillOutfit o : SkillOutfit.values())
		{
			if (o.getSkill() == m.getSkill())
			{
				for (int[] slot : o.getSlots())
				{
					items.put(slot[0], 1);
				}
			}
		}
		for (CombatGear slot : CombatGear.values())
		{
			if (slot.getStyle() == m.getGearStyle())
			{
				for (CombatGear.Tier t : slot.getTiers())
				{
					items.put(t.getIds()[0], 1);
				}
			}
		}
		return new ItemCounts(items);
	}

	private static TreeSet<Integer> relatedIds(Method m)
	{
		TreeSet<Integer> ids = new TreeSet<>();
		if (m.getToolType() != null)
		{
			add(ids, m.getToolType().allIds());
		}
		List<ItemReq> reqs = new ArrayList<>(m.getTools());
		reqs.addAll(m.getInputs());
		reqs.addAll(m.getSupplies());
		for (ItemReq r : reqs)
		{
			add(ids, r.getIds());
			if (r.hasSubstitute())
			{
				add(ids, r.getSubstituteIds());
			}
			for (ItemReq part : r.getRecipe())
			{
				add(ids, part.getIds());
			}
		}
		ids.addAll(m.getProducts());
		for (BoostItem b : BoostItem.values())
		{
			if (b.appliesTo(m))
			{
				add(ids, b.getIds());
			}
		}
		for (SkillOutfit o : SkillOutfit.values())
		{
			if (o.getSkill() == m.getSkill())
			{
				for (int[] slot : o.getSlots())
				{
					add(ids, slot);
				}
			}
		}
		for (CombatGear slot : CombatGear.values())
		{
			if (slot.getStyle() == m.getGearStyle())
			{
				for (CombatGear.Tier t : slot.getTiers())
				{
					add(ids, t.getIds());
				}
			}
		}
		return ids;
	}

	private static void add(TreeSet<Integer> ids, int[] add)
	{
		for (int id : add)
		{
			ids.add(id);
		}
	}

	private static Map<String, Integer> waits(Method m, int done)
	{
		Map<String, Integer> waits = new HashMap<>();
		for (int i = 0; i < done; i++)
		{
			waits.put(m.getStops().get(i).getId(), 10 + 7 * i);
		}
		return waits;
	}

	// ---- Describing the output ----

	private String describeStep(Method m, PlayerState s)
	{
		int target = Math.min(99, s.level(m.getSkill()) + 10);
		int actions = (int) Math.min(Integer.MAX_VALUE, Math.max(1,
			MethodPlanner.xpBetween(s.xp(m.getSkill()), target) / Math.max(1.0, m.xpPerAction(s.level(m.getSkill())))));
		StringBuilder sb = new StringBuilder();
		List<Requirement> reqs = resolver.requirements(m, s, actions);
		for (Requirement r : reqs)
		{
			sb.append("req ").append(r.getStatus()).append(' ').append(r.getText()).append(' ')
				.append(Arrays.toString(r.getItemIds())).append(" q=").append(r.getQuantity())
				.append(" cost=").append(r.getBuyCost()).append('\n');
		}
		for (boolean drop : new boolean[]{false, true})
		{
			GuideStep step = resolver.resolve(m, s, target, drop, reqs);
			sb.append("-- drop=").append(drop).append('\n');
			describe(sb, step);
			sb.append("bankItems ").append(new TreeMap<>(StepResolver.bankItems(reqs, step))).append('\n');
		}
		return sb.toString();
	}

	private static void describe(StringBuilder sb, GuideStep step)
	{
		sb.append("type ").append(step.getType()).append('\n');
		sb.append("title ").append(step.getTitle()).append('\n');
		for (String line : step.getDetails())
		{
			sb.append("line ").append(line).append('\n');
		}
		if (step.getRoute() != null)
		{
			sb.append("route ").append(step.getDestination()).append(' ').append(step.getDestinationName())
				.append(" title=").append(step.getRoute().title())
				.append(" reach=").append(step.getRoute().reachDistance())
				.append(" sea=").append(step.getRoute().bySea())
				.append(" wildy=").append(step.getRoute().entersWilderness())
				.append(" targets=").append(step.getRoute().pathTargets().size()).append('\n');
		}
		for (Target t : step.getHighlights())
		{
			sb.append("highlight ").append(t.getKind()).append(' ').append(new TreeSet<>(t.getNames())).append(' ')
				.append(t.getAction()).append('\n');
		}
		sb.append("bank ").append(step.isHighlightBank()).append('\n');
		sb.append("items ").append(new TreeMap<>(step.getItemHighlights())).append('\n');
		sb.append("withdraw ").append(new TreeMap<>(step.getWithdrawAmounts())).append('\n');
		sb.append("deposit ").append(new TreeMap<>(step.getDepositAmounts())).append('\n');
	}

	private String describePlan(Skill skill, PlayerState s, TrainingPreference pref, int target)
	{
		StringBuilder sb = new StringBuilder();
		List<Method> methods = guides.methods(skill);
		for (MethodOption o : planner.rank(methods, s, pref, target))
		{
			option(sb.append("rank "), o);
		}
		for (PlanSegment seg : planner.plan(methods, s, pref, target, null))
		{
			option(sb.append("plan ").append(seg.getFromLevel()).append('-').append(seg.getToLevel()).append(' ')
				.append(seg.getXp()).append(' '), seg.getOption());
		}
		Method forced = methods.get(methods.size() - 1);
		for (PlanSegment seg : planner.plan(methods, s, pref, target, forced))
		{
			option(sb.append("forced ").append(seg.getFromLevel()).append('-').append(seg.getToLevel()).append(' '),
				seg.getOption());
		}
		return sb.toString();
	}

	private static void option(StringBuilder sb, MethodOption o)
	{
		sb.append(o.getMethod().getId()).append(" blockers=").append(o.getBlockers())
			.append(String.format(" gp=%.3f actions=%d buy=%d afford=%b owned=%.3f buyPer=%.3f xph=%.1f"
					+ " xpa=%.3f score=%.4f", o.getGpPerAction(), o.getActions(), o.getBuyCost(), o.isAffordable(),
				o.getOwnedInputFraction(), o.getBuyCostPerAction(), o.getXpPerHour(), o.getXpPerAction(),
				o.getScore()))
			.append('\n');
	}

	// ---- Snapshot file ----

	private static String hash(String text)
	{
		try
		{
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 8; i++)
			{
				sb.append(String.format("%02x", digest[i]));
			}
			return sb.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static Map<String, String> readSnapshots() throws IOException
	{
		Map<String, String> out = new HashMap<>();
		try (InputStream in = GuideSnapshotTest.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				fail("Missing " + RESOURCE + ": run with UPDATE_SNAPSHOTS=true to create it");
			}
			for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n"))
			{
				int tab = line.lastIndexOf('\t');
				if (tab > 0)
				{
					out.put(line.substring(0, tab), line.substring(tab + 1));
				}
			}
		}
		return out;
	}

	private static void writeSnapshots(Map<String, String> actual) throws IOException
	{
		Path target = Paths.get(System.getProperty("snapshots.file", SOURCE.toString()));
		StringBuilder sb = new StringBuilder();
		actual.forEach((key, text) -> sb.append(key).append('\t').append(hash(text)).append('\n'));
		Files.createDirectories(target.toAbsolutePath().getParent());
		Files.write(target, sb.toString().getBytes(StandardCharsets.UTF_8));
		String dump = System.getProperty("snapshots.dump");
		if (dump != null)
		{
			StringBuilder all = new StringBuilder();
			actual.forEach((key, text) -> all.append("=== ").append(key).append('\n').append(text));
			Files.write(Paths.get(dump), all.toString().getBytes(StandardCharsets.UTF_8));
		}
	}

	private static void writeActual(String key, String text) throws IOException
	{
		Files.createDirectories(ACTUAL_DIR);
		Files.write(ACTUAL_DIR.resolve(key.replaceAll("[^A-Za-z0-9_.-]", "_") + ".txt"),
			text.getBytes(StandardCharsets.UTF_8));
	}
}
