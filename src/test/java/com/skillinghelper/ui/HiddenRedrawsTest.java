package com.skillinghelper.ui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class HiddenRedrawsTest
{
	private final List<String> drawn = new ArrayList<>();

	@Test
	public void panelNeverSeenKeepsRedrawing()
	{
		// e.g. RuneMobile draws panels off-screen, so they never count as showing
		HiddenRedraws redraws = new HiddenRedraws(new JPanel());
		redraws.run("update", () -> drawn.add("a"));
		redraws.run("update", () -> drawn.add("b"));
		assertEquals(List.of("a", "b"), drawn);
		assertFalse(redraws.hasPending());
	}

	@Test
	public void closedPanelCatchesUpWithTheLatestOfEachWhenOpened()
	{
		HiddenRedraws redraws = new HiddenRedraws(new JPanel());
		redraws.shown();
		// Now closed (not on screen)
		redraws.run("update", () -> drawn.add("update 1"));
		redraws.run("step", () -> drawn.add("step 1"));
		redraws.run("update", () -> drawn.add("update 2"));
		assertEquals(List.of(), drawn);
		assertTrue(redraws.hasPending());

		redraws.shown();
		// Latest of each kind, in the order they were last asked for
		assertEquals(List.of("step 1", "update 2"), drawn);
		assertFalse(redraws.hasPending());
	}
}
