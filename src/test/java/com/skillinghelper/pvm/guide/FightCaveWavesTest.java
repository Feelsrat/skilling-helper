package com.skillinghelper.pvm.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.skillinghelper.data.Target;
import com.skillinghelper.pvm.guide.FightCaveWaves.Monster;
import java.util.List;
import net.runelite.api.Skill;
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
		assertEquals("Tz-Kih x2 (22), Tok-Xil (90), Tz-Kek (45)", FightCaveWaves.describe(FightCaveWaves.wave(12)));
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
		assertTrue(lines.get(1).startsWith("• Tok-Xil: accurate ranged"));
		assertTrue(lines.stream().noneMatch(l -> l.contains("Yt-MejKot")));
		assertEquals("Left: 11 prayer doses", lines.get(lines.size() - 1));
	}

	@Test
	public void firstTargetFollowsTheKillOrder()
	{
		GuideState s = GuideState.builder().npc("tz-kih", 2).npc("tok-xil", 1).build();
		assertEquals(List.of(Target.npc(null, "Tz-Kih")), FightCaveWaves.firstTarget(s));
		GuideState noBats = GuideState.builder().npc("yt-mejkot", 1).npc("ket-zek", 1).build();
		assertEquals(List.of(Target.npc(null, "Ket-Zek")), FightCaveWaves.firstTarget(noBats));
		assertTrue(FightCaveWaves.firstTarget(GuideState.builder().build()).isEmpty());
	}

	@Test
	public void potionRemindersComeFromYourOwnStats()
	{
		GuideState s = GuideState.builder()
			.level(Skill.PRAYER, 70)
			.level(Skill.RANGED, 80)
			.boosted(Skill.RANGED, 80)
			.prayer(12)
			.hitpoints(30)
			.maxHitpoints(80)
			.inv(ItemID._4DOSEPRAYERRESTORE, 1)
			.inv(ItemID._4DOSEPOTIONOFSARADOMIN, 1)
			.inv(ItemID._4DOSERANGERSPOTION, 1)
			.build();
		List<String> lines = PvmGuides.FightCave.potionLines(s);
		assertEquals(List.of(
			"Prayer is at 12: sip a prayer potion or super restore.",
			"Hitpoints are at 30: sip a Saradomin brew (or eat).",
			"Your Ranged boost has worn off: sip a ranging potion."), lines);

		GuideState drained = GuideState.builder()
			.level(Skill.PRAYER, 70)
			.level(Skill.RANGED, 80)
			.boosted(Skill.RANGED, 72)
			.prayer(60)
			.inv(ItemID._4DOSE2RESTORE, 1)
			.build();
		assertEquals(List.of("Brews have drained your Ranged (72/80): sip a super restore."),
			PvmGuides.FightCave.potionLines(drained));
	}

	@Test
	public void wikiNotesLeaveOutPrayerAndSpawnAdvice()
	{
		for (int n = 1; n <= FightCaveWaves.JAD_WAVE; n++)
		{
			String note = FightCaveWaves.note(n);
			if (note != null)
			{
				String lower = note.toLowerCase();
				assertTrue(n + ": " + note, !lower.contains("protect from") && !lower.contains("spawn"));
			}
		}
		assertEquals(null, FightCaveWaves.note(3));
		assertTrue(FightCaveWaves.note(1).startsWith("Hit-and-run"));
	}
}
