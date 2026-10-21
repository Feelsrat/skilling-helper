package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import java.util.List;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Based on the OSRS wiki "Prayer training" guide. Prayer is almost always bought with gold, so these methods lean
 * heavily on GE prices for bones.
 */
final class PrayerGuide
{
	private static final String WIKI = "Prayer_training";
	private static final Location RIMMINGTON_PORTAL = Location.of("Rimmington house portal", 2954, 3224, 12);
	private static final Location CHAOS_TEMPLE = Location.of("Chaos Temple (level 38 Wilderness)", 2948, 3820, 8);

	static List<Method> methods()
	{
		return List.of(
			bury("prayer_bury_bones", "Bury bones", 1, 4.5, "Bones", ItemID.BONES).tag(MethodTag.CHEAP).build(),
			bury("prayer_bury_big_bones", "Bury big bones", 1, 15, "Big bones", ItemID.BIG_BONES).build(),
			bury("prayer_bury_dragon_bones", "Bury dragon bones", 1, 72, "Dragon bones", ItemID.DRAGON_BONES).build(),
			Method.builder()
				.id("prayer_gilded_altar")
				.skill(Skill.PRAYER)
				.type(MethodType.PROCESS)
				.name("Gilded altar with dragon bones")
				.minLevel(1)
				.xpPerAction(252)
				.xpPerHour(450_000)
				.members(true)
				.tag(MethodTag.FAST)
				.location(RIMMINGTON_PORTAL)
				.instanced(true)
				.bank(Banks.GRAND_EXCHANGE)
				.input(ItemReq.stack("Dragon bones (noted)", 1, ItemID.DRAGON_BONES, ItemID.Cert.DRAGON_BONES))
				.target(Target.object(null, "Altar"))
				.target(Target.object(null, "Portal"))
				.target(Target.npc("Exchange", "Phials"))
				.useItem(ItemID.DRAGON_BONES)
				.instruction("Use the bones on a gilded altar with both incense burners lit, then unnote more at Phials "
					+ "in Rimmington")
				.notes("350% xp per bone. If you don't own a gilded altar, join a house party (world 330 is the usual "
					+ "one) through the Rimmington portal.")
				.requirementText("A gilded altar with lit burners: your own house (75 Construction) or a friend's")
				.wikiPage(WIKI)
				.build(),
			Method.builder()
				.id("prayer_chaos_altar")
				.skill(Skill.PRAYER)
				.type(MethodType.PROCESS)
				.name("Chaos Temple (Wilderness) with dragon bones")
				.minLevel(1)
				.xpPerAction(252)
				.xpPerHour(500_000)
				.members(true)
				.tag(MethodTag.FAST).tag(MethodTag.CHEAP)
				.location(CHAOS_TEMPLE)
				.bank(Banks.GRAND_EXCHANGE)
				.input(ItemReq.stack("Dragon bones (noted)", 1, ItemID.DRAGON_BONES, ItemID.Cert.DRAGON_BONES))
				.target(Target.object(null, "Chaos altar"))
				.target(Target.npc("Exchange", "Elder Chaos druid"))
				.useItem(ItemID.DRAGON_BONES)
				.instruction("Use the bones on the Chaos altar; the Elder Chaos druid unnotes more for 50 gp each")
				.notes("Same 350% xp as a gilded altar and half the bones are saved, so it's the cheapest gp/xp. "
					+ "It's in the Wilderness: player killers visit often, so only bring what you can lose.")
				.wikiPage("Chaos_Temple_(Wilderness)")
				.build()
		);
	}

	private static Method.MethodBuilder bury(String id, String name, int level, double xp, String bones, int bone)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.PRAYER)
			.type(MethodType.PROCESS)
			.name(name)
			.minLevel(level)
			.xpPerAction(xp)
			// About 1,500 bones an hour while bank-standing
			.xpPerHour((int) (xp * 1_500))
			.tag(MethodTag.BANKSTANDING)
			.location(Banks.GRAND_EXCHANGE)
			.bank(Banks.GRAND_EXCHANGE)
			.input(ItemReq.consumed(bones, 28, 1, bone))
			.useItem(bone)
			.instruction("Bury the " + bones.toLowerCase() + " (click each one), then bank and repeat")
			.wikiPage(WIKI);
	}

	private PrayerGuide()
	{
	}
}
