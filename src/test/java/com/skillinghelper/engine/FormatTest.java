package com.skillinghelper.engine;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FormatTest
{
	@Test
	public void smallAmountsShowEveryCoin()
	{
		assertEquals("0 gp", Format.gp(0));
		assertEquals("99,999 gp", Format.gp(99_999));
	}

	@Test
	public void largeAmountsAreShortened()
	{
		assertEquals("100K gp", Format.gp(100_000));
		assertEquals("9999K gp", Format.gp(9_999_999));
		assertEquals("10M gp", Format.gp(10_000_000));
		assertEquals("2147M gp", Format.gp(Integer.MAX_VALUE));
	}

	@Test
	public void hoursUnderOneShowMinutesAtLeastOne()
	{
		assertEquals("1m", Format.hours(0));
		assertEquals("30m", Format.hours(0.5));
		assertEquals("59m", Format.hours(59 / 60.0));
	}

	@Test
	public void hoursShowPaddedMinutes()
	{
		assertEquals("1h 00m", Format.hours(1));
		assertEquals("3h 05m", Format.hours(3 + 5 / 60.0));
	}

	@Test
	public void geTaxIsTwoPercentWithFloorAndCap()
	{
		assertEquals(0, PriceLookup.geTax(49));
		assertEquals(1, PriceLookup.geTax(50));
		assertEquals(20, PriceLookup.geTax(1_000));
		assertEquals(5_000_000, PriceLookup.geTax(Integer.MAX_VALUE));
	}
}
