package com.skillinghelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class ShortestPathLinkTravelTest
{
	@Test
	public void liftsPermanentOnlySettings()
	{
		assertEquals("Inventory", ShortestPathLink.liftPerm(null));
		assertEquals("Inventory", ShortestPathLink.liftPerm("INVENTORY_NON_CONSUMABLE"));
		assertEquals("Inventory and Bank", ShortestPathLink.liftPerm("INVENTORY_AND_BANK_NON_CONSUMABLE"));
	}

	@Test
	public void leavesOtherSettingsAlone()
	{
		assertNull(ShortestPathLink.liftPerm("NONE"));
		assertNull(ShortestPathLink.liftPerm("INVENTORY"));
		assertNull(ShortestPathLink.liftPerm("ALL"));
	}
}
