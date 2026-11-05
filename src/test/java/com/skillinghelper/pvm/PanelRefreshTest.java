package com.skillinghelper.pvm;

import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.PvmLadder;
import com.skillinghelper.pvm.guide.GuideState;
import com.skillinghelper.pvm.guide.LoadoutPlan;
import com.skillinghelper.pvm.guide.PvmGuide;
import com.skillinghelper.pvm.guide.PvmGuides;
import com.skillinghelper.pvm.ui.PvmProgressionPanel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

/**
 * The plugin only redraws the panel when what it would show changed, which relies on everything it sends comparing
 * by value. A class compared by identity would make every refresh look new and redraw the panel every tick again.
 */
public class PanelRefreshTest
{
	private static List<Object> shown(int level, Map<Integer, Integer> bank)
	{
		PlayerView.PlayerViewBuilder b = PlayerView.builder();
		for (Skill s : Skill.values())
		{
			b.level(s, level);
		}
		PlayerView player = b.build();
		List<Readiness> ladder = new ArrayList<>();
		for (PvmContent c : PvmLadder.BY_LEVEL)
		{
			ladder.add(Readiness.of(c, player));
		}
		GuideState.GuideStateBuilder state = GuideState.builder().members(true).bank(bank);
		for (Skill s : Skill.values())
		{
			state.level(s, level);
		}
		Map<String, LoadoutPlan> plans = new HashMap<>();
		for (PvmGuide g : PvmGuides.ALL)
		{
			plans.put(g.getContentId(), LoadoutPlan.of(g, state.build(), id -> 1000));
		}
		return Arrays.asList(ladder, player, null, new PvmProgressionPanel.GuideView(null, null, plans, true));
	}

	@Test
	public void sameStateIsNotSentTwice()
	{
		assertEquals(shown(70, Map.of(995, 1_000_000)), shown(70, Map.of(995, 1_000_000)));
	}

	@Test
	public void changesAreStillSent()
	{
		assertNotEquals(shown(70, Map.of(995, 1_000_000)), shown(71, Map.of(995, 1_000_000)));
	}
}
