package com.skillinghelper.money;

import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MoneyNpcNameTest
{
	@Test
	public void killingMethodsOutlineTheMonster()
	{
		assertTrue(MoneyGuide.npcsFromName("Killing blue dragons").contains("Blue dragon"));
		assertTrue(MoneyGuide.npcsFromName("Killing the Giant Mole (Twisted Bow)").contains("Giant Mole"));
		assertTrue(MoneyGuide.npcsFromName("Killing Lizardman Shaman in the Lizardman Canyon")
			.contains("Lizardman shaman"));
		assertTrue(MoneyGuide.npcsFromName("Killing Monks of Zamorak").contains("Monk of Zamorak"));
		assertTrue(MoneyGuide.npcsFromName("Killing the Kraken boss.").contains("Kraken"));
		assertTrue(MoneyGuide.npcsFromName("Killing Cerberus").contains("Cerberus"));
		assertTrue(MoneyGuide.npcsFromName("Pickpocketing master farmers").contains("Master farmer"));
		assertTrue(MoneyGuide.npcsFromName("Killing araxytes off-task.").contains("Araxyte"));
	}

	@Test
	public void otherMethodsOutlineNothing()
	{
		assertEquals(Set.of(), MoneyGuide.npcsFromName("Making plain pizzas"));
	}
}
