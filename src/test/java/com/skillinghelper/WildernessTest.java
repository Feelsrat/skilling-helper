package com.skillinghelper;

import com.skillinghelper.banked.data.BankedActivities;
import com.skillinghelper.banked.data.BankedActivity;
import com.skillinghelper.data.Banks;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.MethodPlanner;
import com.skillinghelper.engine.PlanSegment;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.money.data.MoneyMethod;
import com.skillinghelper.money.data.MoneyMethods;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.PvmLadder;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Nothing in the Wilderness gets picked unless the player opted in, and it's always flagged.
 */
public class WildernessTest
{
	private final GuideRepository guides = new GuideRepository();

	@Test
	public void chaosAltarIsFlagged()
	{
		assertTrue(guides.byId("prayer_chaos_altar").isWilderness());
		assertFalse(guides.byId("prayer_gilded_altar").isWilderness());
	}

	@Test
	public void plannerLeavesOutWildernessUnlessAllowed()
	{
		List<Method> prayer = guides.methods(Skill.PRAYER);
		PlayerState s = PlannerTest.state(Skill.PRAYER, 50, true, ItemCounts.EMPTY);
		MethodPlanner planner = new MethodPlanner(id -> 100);
		for (TrainingPreference pref : TrainingPreference.values())
		{
			for (PlanSegment seg : planner.plan(prayer, s, pref, 80, null))
			{
				assertFalse(pref + " planned " + seg.getOption().getMethod().getId(),
					seg.getOption().getMethod().isWilderness());
			}
			List<MethodOption> ranked = planner.rank(prayer, s, pref, 80);
			assertFalse(ranked.get(0).getMethod().isWilderness());
			MethodOption chaos = ranked.stream().filter(o -> o.getMethod().isWilderness()).findFirst().orElse(null);
			assertNotNull(chaos);
			assertTrue(chaos.isWildernessSkipped());
			assertFalse(chaos.isPickable());
		}

		planner.setAllowWilderness(true);
		MethodOption chaos = planner.rank(prayer, s, TrainingPreference.FAST, 80).stream()
			.filter(o -> o.getMethod().isWilderness()).findFirst().orElse(null);
		assertNotNull(chaos);
		assertFalse(chaos.isWildernessSkipped());
	}

	@Test
	public void pickingAWildernessMethodByHandStillWorks()
	{
		Method chaos = guides.byId("prayer_chaos_altar");
		PlayerState s = PlannerTest.state(Skill.PRAYER, 50, true, ItemCounts.EMPTY);
		List<PlanSegment> plan = new MethodPlanner(id -> 100)
			.plan(guides.methods(Skill.PRAYER), s, TrainingPreference.RECOMMENDED, 60, chaos);
		assertEquals(chaos, plan.get(0).getOption().getMethod());
	}

	@Test
	public void bankingNeverLeadsIntoTheWilderness()
	{
		// Just south of the Wilderness ditch, closer to Ferox Enclave than to Edgeville bank as the crow flies
		assertFalse(Wilderness.contains(Banks.nearest(new WorldPoint(3135, 3515, 0), true)));
		// Already inside: Ferox Enclave is fine
		assertTrue(Wilderness.contains(Banks.nearest(new WorldPoint(3135, 3640, 0), true)));
	}

	@Test
	public void pvmWildernessContentIsFlagged()
	{
		for (PvmContent c : PvmLadder.ALL)
		{
			if (c.getLocation() != null && Wilderness.contains(c.getLocation()))
			{
				assertTrue(c.getName() + " is in the Wilderness but not flagged", c.isWilderness());
			}
		}
	}

	@Test
	public void bankedActivitiesStayOutOfTheWilderness()
	{
		for (BankedActivity a : BankedActivities.all())
		{
			assertFalse(a.getId(), a.getMethod().isWilderness());
		}
	}

	@Test
	public void moneyMethodsInTheWildernessAreFlagged()
	{
		Map<String, MoneyMethod> byPage = MoneyMethods.all().stream()
			.collect(Collectors.toMap(MoneyMethod::getPage, Function.identity(), (a, b) -> a));
		for (String page : List.of("Killing revenants (Magic shortbow)", "Killing lava dragons",
			"Wilderness Agility Course", "Crafting nature runes through the Abyss", "Crafting chaos runes through the Abyss", "Killing the King Black Dragon",
			"Charging air orbs", "Charging earth orbs", "Killing green dragons"))
		{
			MoneyMethod m = byPage.get("Money making guide/" + page);
			assertNotNull(page, m);
			assertTrue(page, m.isWilderness());
		}
		for (String page : List.of("Killing Bryophyta (free-to-play)", "Killing chaos druids",
			"Killing green dragons (Myths Guild)", "Opening eclectic impling jars", "Collecting wine of zamorak (free-to-play)"))
		{
			MoneyMethod m = byPage.get("Money making guide/" + page);
			assertNotNull(page, m);
			assertFalse(page, m.isWilderness());
		}
	}
}
