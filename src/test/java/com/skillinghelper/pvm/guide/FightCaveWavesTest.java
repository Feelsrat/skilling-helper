package com.skillinghelper.pvm.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.skillinghelper.pvm.guide.FightCaveWaves.Monster;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class FightCaveWavesTest
{
	@Test
	public void wavesMatchTheWikiTable()
	{
		assertEquals(List.of(Monster.TZ_KIH), FightCaveWaves.wave(1));
		assertEquals(List.of(Monster.TZ_KIH, Monster.TZ_KIH), FightCaveWaves.wave(2));
		assertEquals(List.of(Monster.TZ_KEK), FightCaveWaves.wave(3));
		assertEquals(List.of(Monster.TZ_KEK, Monster.TZ_KEK), FightCaveWaves.wave(6));
		assertEquals(List.of(Monster.TOK_XIL), FightCaveWaves.wave(7));
		assertEquals(List.of(Monster.TOK_XIL, Monster.TOK_XIL), FightCaveWaves.wave(14));
		assertEquals(List.of(Monster.YT_MEJKOT), FightCaveWaves.wave(15));
		assertEquals(List.of(Monster.YT_MEJKOT, Monster.YT_MEJKOT), FightCaveWaves.wave(30));
		assertEquals(List.of(Monster.KET_ZEK), FightCaveWaves.wave(31));
		assertEquals(List.of(Monster.KET_ZEK, Monster.YT_MEJKOT, Monster.YT_MEJKOT),
			FightCaveWaves.wave(61));
		assertEquals(List.of(Monster.KET_ZEK, Monster.KET_ZEK), FightCaveWaves.wave(62));
		assertEquals(List.of(Monster.JAD), FightCaveWaves.wave(63));
		assertTrue(FightCaveWaves.wave(64).isEmpty());
	}

	@Test
	public void killOrderPutsRangedAndMagicFirst()
	{
		// Wave 12 is Tok-Xil, Tz-Kek and two Tz-Kih
		assertEquals("Tok-Xil (90), Tz-Kek (45), Tz-Kih x2 (22)", FightCaveWaves.describe(FightCaveWaves.wave(12)));
	}

	@Test
	public void readsTheWaveMessage()
	{
		assertEquals(12, FightCaveWaves.parseWave("Wave: 12"));
		assertEquals(-1, FightCaveWaves.parseWave("Wave: 70"));
		assertEquals(-1, FightCaveWaves.parseWave("Welcome to the Fight Cave."));
	}

	@Test
	public void panelShowsOnlyTheCurrentWaveAndDoses()
	{
		GuideState s = GuideState.builder()
			.location(new WorldPoint(2400, 5080, 0))
			.flag(FightCaveWaves.WAVE_FLAG + 14)
			.inv(ItemID._4DOSEPRAYERRESTORE, 2)
			.inv(ItemID._3DOSEPRAYERRESTORE, 1)
			.usedSlots(3)
			.build();
		List<String> lines = PvmGuides.FightCave.lines(s);
		assertEquals("Wave 14 of 63: Tok-Xil x2 (90)", lines.get(0));
		assertTrue(lines.get(1).startsWith("• Tok-Xil: ranged"));
		assertTrue(lines.stream().noneMatch(l -> l.contains("Yt-MejKot")));
		assertEquals("Left: 11 prayer doses", lines.get(lines.size() - 1));
	}
}
