package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import java.util.List;
import net.runelite.api.Quest;
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
				.supply(Supplies.COINS)
				.useItem(ItemID.DRAGON_BONES)
				.instruction("Use the bones on a gilded altar with both incense burners lit, then unnote more at Phials "
					+ "in Rimmington")
				.notes("350% xp per bone. If you don't own a gilded altar, join a house party (world 330 is the usual "
					+ "one) through the Rimmington portal.")
				.requirementText("A gilded altar with lit burners: your own house (75 Construction) or a friend's")
				.wikiPage(WIKI)
				.getThere("Buy dragon bones noted (one slot) and bring coins: Phials in Rimmington swaps noted "
					+ "bones for real ones for 5 gp each.")
				.howTo("Go to the house portal in Rimmington. If you own a house with a gilded altar, enter it. "
					+ "Otherwise, hop to world 330 and look in the chat for someone advertising a house party "
					+ "with a gilded altar, then click the portal, pick \"Friend's house\" and type their name.")
				.howTo("Find the chapel and check both incense burners next to the altar are lit (the host "
					+ "usually lights them).")
				.howTo("Click a bone, then click the altar (\"Use\" the bones on it). Your character offers them "
					+ "one by one; press Space to offer them all if a menu pops up.")
				.howTo("When you run out, leave through the portal, use the noted bones on Phials (just outside "
					+ "the portal) and pick \"Exchange All\". Then go back in and repeat.")
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
				.supply(Supplies.COINS)
				.useItem(ItemID.DRAGON_BONES)
				.instruction("Use the bones on the Chaos altar; the Elder Chaos druid unnotes more for 50 gp each")
				.notes("Same 350% xp as a gilded altar and half the bones are saved, so it's the cheapest gp/xp. "
					+ "It's in the Wilderness: player killers visit often, so only bring what you can lose.")
				.wikiPage("Chaos_Temple_(Wilderness)")
				.getThere("The Chaos Temple is in level 38 Wilderness. Other players can attack you there, so "
					+ "only bring the bones you'll use and some coins. A burning amulet (Chaos Temple) teleports "
					+ "you right next to it.")
				.howTo("Inside, use your noted bones on the highlighted Elder Chaos druid and pick \"Exchange "
					+ "All\" (50 gp each).")
				.howTo("Click a bone, then click the Chaos altar. Your character offers them one by one; about "
					+ "half the time the bone is saved.")
				.howTo("When you run out, unnote more with the druid. If another player shows up and attacks, "
					+ "teleport out straight away.")
				.build(),
			Method.builder()
				.id("prayer_ectofuntus")
				.skill(Skill.PRAYER)
				.type(MethodType.ACTIVITY)
				.name("Ectofuntus with dragon bones")
				.minLevel(1)
				.xpPerAction(288)
				.xpPerHour(100_000)
				.members(true)
				.tag(MethodTag.CHEAP)
				.quest(Quest.GHOSTS_AHOY)
				.location(Location.area("Ectofuntus (Port Phasmatys)", 3659, 3519, 12))
				.bank(Location.of("Port Phasmatys bank", 3688, 3467, 5))
				.input(ItemReq.consumed("Dragon bones", 9, 1, ItemID.DRAGON_BONES))
				.input(ItemReq.consumed("Empty pots", 9, 1, ItemID.POT_EMPTY))
				.input(ItemReq.consumed("Empty buckets", 9, 1, ItemID.BUCKET_EMPTY))
				.target(Target.object(null, "Ectofuntus", "Loader", "Bin", "Pool of Slime"))
				.instruction("Fill the buckets at the slime pool in the basement, grind the bones in the loader "
					+ "upstairs, then worship at the Ectofuntus")
				.notes("400% of burying xp per bone, so a little cheaper per xp than a gilded altar but slower. "
					+ "The Morytania diary gives free bonemeal and slime. Only part of Ghosts Ahoy is needed for the "
					+ "Ectophial teleport.")
				.wikiPage("Ectofuntus")
				.getThere("The Ectofuntus is the big green temple just west of Port Phasmatys in Morytania. The "
					+ "Ectophial (from Ghosts Ahoy) teleports you right next to it. Take 9 dragon bones, 9 empty "
					+ "pots and 9 empty buckets each trip; you need ecto-tokens to get through the town's energy "
					+ "barrier.")
				.howTo("Go down the trapdoor next to the Ectofuntus to the basement, walk down to the bottom and "
					+ "use the buckets on the Pool of Slime to fill them.")
				.howTo("Go back up to the top floor of the temple. Use a bone on the Loader, then wind the "
					+ "grinder, then click the Bin to collect bonemeal into an empty pot. Do this for every bone.")
				.howTo("Go down to the ground floor and click the Ectofuntus (\"Worship\") once for each bonemeal "
					+ "and bucket of slime pair. That's when the xp comes.")
				.howTo("Teleport or walk to Port Phasmatys bank and repeat. Talk to the ghost disciple next to "
					+ "the Ectofuntus to collect your ecto-tokens.")
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
			.howTo("Stand next to any banker or bank booth (the Grand Exchange has lots in the middle).")
			.howTo("Open the bank and take out 28 " + bones.toLowerCase() + " (the guide highlights them).")
			.howTo("Close the bank and click each bone in your inventory once (\"Bury\"). Click the next one as soon "
				+ "as the last one is buried.")
			.howTo("When they're all buried, take out 28 more and repeat.")
			.wikiPage(WIKI);
	}

	private PrayerGuide()
	{
	}
}
