package com.skillinghelper.thieving;

import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.StepType;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ArtefactHeistTest
{
	private static final GuideStep BASE = GuideStep.builder().type(StepType.TRAIN).title("Train").line("x").build();

	private static PlayerState at(int x, int y, Map<Integer, Integer> inv)
	{
		return PlayerState.builder().location(new WorldPoint(x, y, 0)).inventory(new ItemCounts(inv)).bank(ItemCounts.EMPTY).build();
	}

	@Test
	public void noTaskSendsYouToKhaled()
	{
		GuideStep step = ArtefactHeist.step(BASE, at(1790, 3740, Map.of()), 0);
		assertEquals("Get a house from Captain Khaled", step.getTitle());
		assertNotNull(step.getDestination());
		assertTrue(step.getHighlights().get(0).matches("Captain Khaled", null));
	}

	@Test
	public void taskPointsAtTheHouseAndItsDrawers()
	{
		GuideStep step = ArtefactHeist.step(BASE, at(1845, 3752, Map.of()), 4);
		assertEquals("Rob the south-western house", step.getTitle());
		assertEquals(1749, step.getDestination().getX());
		assertTrue(step.getHighlights().stream().anyMatch(t -> t.matches("Drawers", null)));
		assertTrue(step.getHighlights().stream().anyMatch(t -> t.matches("Patrolwoman", null)));
	}

	@Test
	public void insideTheHouseThereIsNoArrow()
	{
		GuideStep step = ArtefactHeist.step(BASE, PlayerState.builder().location(new WorldPoint(1749, 3734, 1))
			.inventory(ItemCounts.EMPTY).bank(ItemCounts.EMPTY).build(), 4);
		assertNull(step.getRoute());
	}

	@Test
	public void carryingTheArtefactGoesBack()
	{
		GuideStep step = ArtefactHeist.step(BASE, at(1749, 3734, Map.of(ItemID.PISCARILIUS_STOLEN_RING, 1)), 4);
		assertEquals("Take the artefact back to Captain Khaled", step.getTitle());
		assertEquals(1845, step.getDestination().getX());
	}
}
