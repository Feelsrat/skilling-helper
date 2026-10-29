package com.skillinghelper.pvm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.skillinghelper.pvm.data.PvmLadder;
import org.junit.Test;

public class KillCountParserTest
{
	private static void assertKill(String message, String id, int count)
	{
		KillCountParser.Kill kill = KillCountParser.parse(message, PvmLadder.ALL);
		assertEquals(id, kill.getContent().getId());
		assertEquals(count, kill.getCount());
	}

	@Test
	public void readsBossRaidAndChestCounts()
	{
		assertKill("Your Zulrah kill count is: <col=ff0000>1,204</col>.", "zulrah", 1204);
		assertKill("Your Barrows chest count is: <col=ff0000>3</col>.", "barrows", 3);
		assertKill("Your completed Chambers of Xeric count is: <col=ff0000>12</col>.", "chambers_of_xeric", 12);
		assertKill("Your completed Tombs of Amascut: Entry Mode count is: 2.", "tombs_of_amascut", 2);
		assertKill("Your TzTok-Jad kill count is: 1.", "fight_caves", 1);
		assertKill("Your Gauntlet completion count is: 4.", "gauntlet", 4);
		assertKill("Your Kree'arra kill count is: 7.", "god_wars", 7);
	}

	@Test
	public void ignoresOtherMessages()
	{
		assertNull(KillCountParser.parse("Your Goblin kill count is: 5.", PvmLadder.ALL));
		assertNull(KillCountParser.parse("Welcome to Old School RuneScape.", PvmLadder.ALL));
	}
}
