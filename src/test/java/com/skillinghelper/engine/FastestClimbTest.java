package com.skillinghelper.engine;

import com.google.common.collect.ImmutableMap;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodType;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class FastestClimbTest
{
	private final MethodPlanner planner = new MethodPlanner(id -> 0);

	private static Method method(String id, Skill skill, int minLevel, int maxLevel, int xpHr)
	{
		return Method.builder().id(id).skill(skill).name(id).type(MethodType.GATHER)
			.location(Location.of("Spot", 3000, 3000, 0, 5)).instruction("Train").minLevel(minLevel)
			.maxLevel(maxLevel).xpPerAction(10).xpPerHour(xpHr).build();
	}

	private static PlayerState at(int mining, int woodcutting)
	{
		return PlayerState.builder()
			.levels(ImmutableMap.of(Skill.MINING, mining, Skill.WOODCUTTING, woodcutting))
			.xp(ImmutableMap.of(Skill.MINING, Experience.getXpForLevel(mining),
				Skill.WOODCUTTING, Experience.getXpForLevel(woodcutting)))
			.build();
	}

	@Test
	public void switchesToAFasterMethodPartway()
	{
		Method slow = method("slow", Skill.MINING, 1, 0, 10_000);
		Method fast = method("fast", Skill.MINING, 35, 0, 40_000);
		MethodPlanner.FastestClimb climb = planner.fastestClimb(Arrays.asList(slow, fast), at(30, 1), 10);

		assertEquals(Skill.MINING, climb.getSkill());
		assertEquals(40, climb.getToLevel());
		assertEquals(2, climb.getLegs().size());
		assertEquals(slow, climb.getLegs().get(0).getOption().getMethod());
		assertEquals(35, climb.getLegs().get(0).getToLevel());
		assertEquals(fast, climb.getLegs().get(1).getOption().getMethod());
		double hours = (Experience.getXpForLevel(35) - Experience.getXpForLevel(30)) / 10_000.0
			+ (Experience.getXpForLevel(40) - Experience.getXpForLevel(35)) / 40_000.0;
		assertEquals(hours, climb.getHours(), 1e-6);
	}

	@Test
	public void leavesAMethodAtTheTopOfItsRange()
	{
		// Faster, but outleveled at 35, where the slower one has to take over
		Method capped = method("capped", Skill.MINING, 1, 35, 50_000);
		Method steady = method("steady", Skill.MINING, 1, 0, 20_000);
		MethodPlanner.FastestClimb climb = planner.fastestClimb(Arrays.asList(capped, steady), at(30, 1), 10);

		assertEquals(2, climb.getLegs().size());
		assertEquals(capped, climb.getLegs().get(0).getOption().getMethod());
		assertEquals(steady, climb.getLegs().get(1).getOption().getMethod());
	}

	@Test
	public void picksTheSkillThatLevelsQuickest()
	{
		Method mining = method("mine", Skill.MINING, 1, 0, 30_000);
		Method woodcutting = method("chop", Skill.WOODCUTTING, 1, 0, 30_000);
		// Same rate, but low woodcutting levels take far less xp
		MethodPlanner.FastestClimb climb = planner.fastestClimb(Arrays.asList(mining, woodcutting), at(60, 20), 10);
		assertEquals(Skill.WOODCUTTING, climb.getSkill());
		assertEquals(1, climb.getLegs().size());
	}

	@Test
	public void nothingUsableGivesNull()
	{
		Method locked = method("locked", Skill.MINING, 50, 0, 30_000);
		assertNull(planner.fastestClimb(Collections.singletonList(locked), at(30, 1), 10));
	}

	@Test
	public void mostAfkSkipsOutleveledAndRanksByClicks()
	{
		Method busy = method("busy", Skill.MINING, 1, 0, 30_000).toBuilder().clicksPerHour(1500).build();
		Method idle = method("idle", Skill.MINING, 1, 0, 10_000).toBuilder().clicksPerHour(100).build();
		Method outgrown = method("outgrown", Skill.MINING, 1, 20, 10_000).toBuilder().clicksPerHour(10).build();
		Method chop = method("chop", Skill.WOODCUTTING, 1, 0, 10_000).toBuilder().clicksPerHour(50).build();
		java.util.List<MethodOption> ranked = planner.rankMostAfk(Arrays.asList(busy, idle, outgrown, chop),
			at(30, 30), s -> 40);

		assertEquals(2, ranked.size());
		assertEquals(chop, ranked.get(0).getMethod());
		assertEquals(idle, ranked.get(1).getMethod());
	}

	@Test
	public void mostAfkIsEmptyWhenEverythingIsOutleveled()
	{
		Method outgrown = method("outgrown", Skill.MINING, 1, 20, 10_000).toBuilder().clicksPerHour(10).build();
		assertEquals(0, planner.rankMostAfk(Collections.singletonList(outgrown), at(30, 1), s -> 40).size());
	}

	@Test
	public void fastestRanksEverySkill()
	{
		Method mining = method("mine", Skill.MINING, 1, 0, 30_000);
		Method woodcutting = method("chop", Skill.WOODCUTTING, 1, 0, 30_000);
		java.util.List<MethodPlanner.FastestClimb> ranked = planner.rankFastest(Arrays.asList(mining, woodcutting),
			at(60, 20), 10);
		assertEquals(2, ranked.size());
		assertEquals(Skill.WOODCUTTING, ranked.get(0).getSkill());
		assertEquals(Skill.MINING, ranked.get(1).getSkill());
	}

	@Test
	public void sameActivityUnderSeveralSkillsShowsOnce()
	{
		Method crabAttack = Method.builder().id("a").skill(Skill.ATTACK).name("Crab").type(MethodType.ACTIVITY)
			.location(Location.of("Spot", 3000, 3000, 0, 5)).instruction("Fight").minLevel(1).xpPerAction(10)
			.xpPerHour(20_000).clicksPerHour(20).build();
		Method crabDefence = crabAttack.toBuilder().id("d").skill(Skill.DEFENCE).build();
		Method trees = method("chop", Skill.WOODCUTTING, 1, 0, 10_000).toBuilder().clicksPerHour(50).build();
		PlayerState state = PlayerState.builder()
			.levels(ImmutableMap.of(Skill.ATTACK, 40, Skill.DEFENCE, 30, Skill.WOODCUTTING, 30))
			.build();
		java.util.List<java.util.List<MethodOption>> groups = MethodPlanner.sameActivityOnce(
			planner.rankMostAfk(Arrays.asList(crabAttack, crabDefence, trees), state, s -> 50), state);

		assertEquals(2, groups.size());
		assertEquals(2, groups.get(0).size());
		// The lower skill is the one opened
		assertEquals(Skill.DEFENCE, groups.get(0).get(0).getMethod().getSkill());
		assertEquals(trees, groups.get(1).get(0).getMethod());
	}
}
