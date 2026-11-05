package com.skillinghelper.engine;

import com.skillinghelper.data.Method;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.data.guides.GuideRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Changing the training style must re-pick the method: the guide follows the plan's first leg, so the top of the
 * method list has to be that same method, for every skill, level and style.
 */
public class StyleSwitchTest
{
	private final GuideRepository guides = new GuideRepository();

	private static PlayerState state(Skill skill, int level, long gp)
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, s == skill ? level : 70);
			xp.put(s, Experience.getXpForLevel(s == skill ? level : 70));
		}
		ItemCounts bank = gp <= 0 ? ItemCounts.EMPTY : ItemCounts.of(ItemID.COINS, (int) gp);
		return PlayerState.builder().levels(levels).xp(xp).bank(bank).freeInventorySlots(28).membersWorld(true).build();
	}

	@Test
	public void topOfListIsTheMethodTheGuidePicks()
	{
		MethodPlanner planner = new MethodPlanner(id -> 100);
		List<String> mismatches = new ArrayList<>();
		for (Skill skill : Skill.values())
		{
			List<Method> methods = guides.methods(skill);
			if (methods.isEmpty())
			{
				continue;
			}
			for (int level = 1; level < 99; level += 3)
			{
				for (long gp : new long[]{0, 10_000_000})
				{
					PlayerState s = state(skill, level, gp);
					int target = Math.min(99, level + 10);
					for (TrainingPreference pref : TrainingPreference.values())
					{
						List<MethodOption> ranked = planner.rank(methods, s, pref, target);
						List<PlanSegment> plan = planner.plan(methods, s, pref, target, null);
						if (plan.isEmpty() || ranked.isEmpty())
						{
							continue;
						}
						Method picked = plan.get(0).getOption().getMethod();
						Method top = ranked.get(0).getMethod();
						if (picked != top)
						{
							mismatches.add(skill + " " + level + " " + pref + " gp=" + gp + ": list " + top.getId()
								+ " / guide " + picked.getId());
						}
					}
				}
			}
		}
		assertTrue(mismatches.size() + " mismatches, e.g. " + mismatches.subList(0, Math.min(15, mismatches.size())),
			mismatches.isEmpty());
	}
}
