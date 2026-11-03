package com.skillinghelper.ui;

import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GuideTextTest
{
	private static final GuideText.Palette P = new GuideText.Palette("W", "G", "O");

	private static List<String> parts(GuideText.Line l)
	{
		return l.getParts().stream().map(p -> (p.isAside() ? "~" : "") + p.getText()).collect(Collectors.toList());
	}

	@Test
	public void markerLabelAndSentencesAreSplitOut()
	{
		GuideText.Line l = GuideText.parse("• Then: Mine the rocks. Fix struts for bonus ore..");
		assertEquals("•", l.getMarker());
		assertEquals("Then", l.getLabel());
		assertEquals(List.of("Mine the rocks.", "Fix struts for bonus ore."), parts(l));
	}

	@Test
	public void longAsidesAndBuyNotesGoOnTheirOwnLines()
	{
		GuideText.Line l = GuideText.parse(
			"• Check for Oak logs (0 carried, 0 in bank, 69 needed for goal) - buy 69 x ~525 gp = ~36,225 gp");
		assertEquals(List.of("Check for Oak logs", "~0 carried, 0 in bank, 69 needed for goal",
			"Buy 69 x ~525 gp = ~36,225 gp"), parts(l));
		// Short asides stay put
		assertEquals(List.of("2 x stamina potions (~662 gp each)"),
			parts(GuideText.parse("2 x stamina potions (~662 gp each)")));
	}

	@Test
	public void onlyKnownLeadInsBecomeLabels()
	{
		assertNull(GuideText.parse("Lumberjack outfit piece: wear it").getLabel());
		assertNull(GuideText.parse("Only 3 food left: finish this kill").getLabel());
		assertEquals("This trip", GuideText.parse("This trip: 27 x maple logs.").getLabel());
	}

	@Test
	public void abbreviationsAndBracketsDoNotSplit()
	{
		assertEquals(1, GuideText.parse("Bring food, e.g. Lobsters (one. Two) please").getParts().size());
	}

	@Test
	public void longSentencesBreakAtThenAndImperativeAnd()
	{
		assertEquals(List.of("Selling the products gets ~54 gp of the ~515 gp per action back.",
			"So sell them as you go to stretch your gold."),
			parts(GuideText.parse("Selling the products gets ~54 gp of the ~515 gp per action back, so sell them as "
				+ "you go to stretch your gold.")));
		// ", and is ..." has no verb to start a new line with
		assertEquals(1, GuideText.parse("Snape grass is used as a Herblore secondary ingredient for Prayer potions, "
			+ "and is always in high demand.").getParts().size());
	}

	@Test
	public void highlightsAmountsItemsGoldAndOptions()
	{
		String h = GuideText.highlight("Withdraw 28 x Raw salmon and choose Bank for ~1,050 gp at level 70.", P);
		assertTrue(h, h.startsWith("<font color='W'>Withdraw</font> "));
		assertTrue(h, h.contains("<font color='G'>28 x</font> <font color='W'>Raw salmon</font> and"));
		assertTrue(h, h.contains("choose <font color='O'>Bank</font>"));
		assertTrue(h, h.contains("<font color='G'>~1,050 gp</font>"));
		assertTrue(h, h.contains("<font color='W'>level 70</font>."));
		assertEquals("a &lt;b&gt; &amp;", GuideText.highlight("a <b> &", P));
	}
}
