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
	public void cardShowsOnlyTheCurrentWaveAndDoses()
	{
		GuideState s = GuideState.builder()
			.location(new WorldPoint(2400, 5080, 0))
			.flag(FightCaveWaves.WAVE_FLAG + 12)
			.npc("tok-xil", 1)
			.npc("tz-kek", 1)
			.inv(ItemID._4DOSEPRAYERRESTORE, 2)
			.inv(ItemID._3DOSEPRAYERRESTORE, 1)
			.prayer(70)
			.level(Skill.PRAYER, 70)
			.usedSlots(3)
			.build();
		WaveCard card = PvmGuides.FightCave.card(s);
		assertEquals(12, card.getWave());
		assertEquals(List.of(
			new WaveCard.Row(2, "Tz-Kih", 22, "Melee", Monster.TZ_KIH.getNote(), false),
			new WaveCard.Row(1, "Tok-Xil", 90, "Ranged", Monster.TOK_XIL.getNote(), true),
			new WaveCard.Row(1, "Tz-Kek", 45, "Melee", Monster.TZ_KEK.getNote(), false)), card.getRows());
		assertEquals("Prayer 11 · Restore 0 · Brew 0", card.getSupplies());
		assertTrue(card.getAlerts().isEmpty());
	}

	@Test
	public void cardBeforeTheFirstWaveMessageHasNoRows()
	{
		WaveCard card = PvmGuides.FightCave.card(GuideState.builder().prayer(70).level(Skill.PRAYER, 70).build());
		assertEquals(-1, card.getWave());
		assertTrue(card.getRows().isEmpty());
	}

	@Test
	public void jadWaveAddsHealersOnceTheyAreUp()
	{
		GuideState s = GuideState.builder().flag(FightCaveWaves.WAVE_FLAG + 63).npc("yt-hurkot", 4).build();
		List<WaveCard.Row> rows = FightCaveWaves.rows(63, s);
		assertEquals("TzTok-Jad", rows.get(0).getName());
		assertEquals(4, rows.get(1).getCount());
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
