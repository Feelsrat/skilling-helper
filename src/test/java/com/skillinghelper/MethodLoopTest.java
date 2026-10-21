package com.skillinghelper;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import com.skillinghelper.engine.ToolAdvisor;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Walks every method through the basic loop: with everything in the bank it banks, and carrying it all at the spot
 * it trains.
 */
public class MethodLoopTest
{
	@Test
	public void everyMethodBanksThenTrains()
	{
		GuideRepository guides = new GuideRepository();
		StepResolver resolver = new StepResolver(id -> 50);
		for (Method m : guides.all())
		{
			int level = Math.max(1, m.getMinLevel());
			Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
			Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
			for (Skill s : Skill.values())
			{
				int l = s == m.getSkill() ? level : 99;
				levels.put(s, l);
				xp.put(s, Experience.getXpForLevel(l));
			}

			Map<Integer, Integer> carried = new HashMap<>();
			int slots = 0;
			if (m.getToolType() != null)
			{
				PlayerState lv = PlayerState.builder().levels(levels).build();
				carried.put(ToolAdvisor.bestUsable(m.getToolType(), lv).getIds()[0], 1);
				slots++;
			}
			for (ItemReq tool : m.getTools())
			{
				carried.merge(tool.getIds()[0], Math.max(1, tool.getQuantity()), Integer::sum);
				slots++;
			}
			for (ItemReq input : m.getInputs())
			{
				carried.merge(input.getIds()[0], input.isWholeStack() ? 1_000 : Math.max(1, input.getQuantity()),
					Integer::sum);
				slots++;
			}
			Map<Integer, Integer> bank = new HashMap<>(carried);
			bank.put(ItemID.COINS, 10_000_000);

			PlayerState ready = PlayerState.builder()
				.levels(levels)
				.xp(xp)
				.inventory(new ItemCounts(carried))
				.bank(new ItemCounts(bank))
				.freeInventorySlots(28 - slots)
				.membersWorld(true)
				.completedQuests(EnumSet.allOf(Quest.class))
				.location(m.getStops().isEmpty() ? m.getLocation().getPoint()
					: m.getStops().get(0).getLocation().getPoint())
				.build();
			int target = Math.min(99, level + 5);
			GuideStep step = resolver.resolve(m, ready, target, false, resolver.requirements(m, ready, 100));
			assertEquals(m.getId() + ": " + step.getTitle(), StepType.TRAIN, step.getType());

			if (!m.getInputs().isEmpty() || !m.getTools().isEmpty() || m.getToolType() != null)
			{
				PlayerState empty = ready.toBuilder().inventory(ItemCounts.EMPTY).freeInventorySlots(28).build();
				step = resolver.resolve(m, empty, target, false, resolver.requirements(m, empty, 100));
				assertEquals(m.getId() + ": " + step.getTitle(), StepType.BANK, step.getType());
			}
		}
	}
}
