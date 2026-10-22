package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
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
 * Based on the OSRS wiki "Thieving training" guide.
 */
final class ThievingGuide
{
	private static final String WIKI = "Thieving_training";
	private static final String POUCHES = "Open your coin pouches before you have 28 or you'll stop pickpocketing.";

	static List<Method> methods()
	{
		return List.of(
			Method.builder()
				.id("thieve_men")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Pickpocket men (Lumbridge)")
				.minLevel(1)
				.xpPerAction(8)
				.xpPerHour(3_000)
				.coinsPerAction(3)
				.members(true)
				.tag(MethodTag.CHEAP)
				.location(Location.of("Lumbridge Castle courtyard", 3222, 3218, 12))
				.target(Target.npc("Pickpocket", "Man", "Woman"))
				.supply(Supplies.food(5))
				.instruction("Pickpocket the men and women")
				.notes("You get stunned now and then; just wait it out. " + POUCHES)
				.wikiPage(WIKI)
				.build(),
			Method.builder()
				.id("thieve_tea_stall")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Tea stall (Varrock)")
				.minLevel(5)
				.xpPerAction(16)
				.xpPerHour(13_000)
				.members(true)
				.tag(MethodTag.AFK).tag(MethodTag.POWER)
				.location(Location.of("Varrock east tea stall", 3270, 3411, 6))
				.dropByDefault(true)
				.product(ItemID.CUP_OF_TEA)
				.target(Target.object("Steal-from", "Tea stall"))
				.instruction("Steal from the tea stall and drop the tea")
				.wikiPage(WIKI)
				.build(),
			Method.builder()
				.id("thieve_cake_stall")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Baker's stall (Ardougne)")
				.minLevel(5)
				.xpPerAction(16)
				.xpPerHour(16_000)
				.members(true)
				.tag(MethodTag.AFK).tag(MethodTag.POWER)
				.location(Location.of("Ardougne market baker's stall", 2667, 3310, 6))
				.bank(Banks.ARDOUGNE_SOUTH)
				.dropByDefault(true)
				.product(ItemID.CAKE).product(ItemID.BREAD).product(ItemID.CHOCOLATE_SLICE)
				.target(Target.object("Steal-from", "Baker's stall"))
				.instruction("Steal from the baker's stall from the north-east corner so the guard can't see you")
				.notes("Keep the cakes if you want free food for later.")
				.wikiPage(WIKI)
				.build(),
			Method.builder()
				.id("thieve_pyramid_plunder")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Pyramid Plunder")
				.minLevel(21)
				.xpPerAction(200)
				.xpPerHour(30_000)
				.members(true)
				.tag(MethodTag.MINIGAME).tag(MethodTag.FAST)
				.quest(Quest.ICTHLARINS_LITTLE_HELPER)
				.location(Location.of("Pyramid Plunder (Sophanem)", 3288, 2787, 15))
				.target(Target.object(null, "Urn", "Grand Gold Chest", "Speartrap", "Tomb Door", "Sarcophagus"))
				.target(Target.npc(null, "Guardian mummy"))
				.supply(Supplies.food(5))
				.supply(Supplies.STAMINA)
				.instruction("Talk to the Guardian mummy to start, then loot urns and the gold chest in each room and "
					+ "find the right tomb door")
				.notes("Xp per hour rises a lot as you unlock deeper rooms every 10 levels.")
				.wikiPage("Pyramid_Plunder")
				.rateAt(51, 80_000).rateAt(71, 150_000).rateAt(91, 220_000)
				.build(),
			Method.builder()
				.id("thieve_rogues_den")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Rogues' Den (Rogue outfit)")
				.minLevel(50)
				.xpPerAction(1_100)
				.xpPerHour(40_000)
				.members(true)
				.tag(MethodTag.MINIGAME)
				.location(Location.of("Rogues' Den (Burthorpe)", 2905, 3537, 10))
				.target(Target.npc(null, "Brian O'Richard"))
				.supply(Supplies.STAMINA)
				.instruction("Talk to Brian O'Richard to enter the maze, bring nothing but stamina potions, and work "
					+ "through the traps to crack the safe")
				.notes("Each run gives a Rogue outfit piece; the full set doubles pickpocket loot for good. "
					+ "Needs 50 Agility too.")
				.wikiPage("Rogues%27_Den")
				.build(),
			Method.builder()
				.id("thieve_master_farmer")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Pickpocket master farmers (Draynor)")
				.minLevel(38)
				.xpPerAction(43)
				.xpPerHour(25_000)
				.members(true)
				.tag(MethodTag.PROFIT)
				.location(Location.of("Draynor market", 3080, 3250, 8))
				.bank(Banks.DRAYNOR)
				.target(Target.npc("Pickpocket", "Master Farmer"))
				.supply(Supplies.food(10))
				.instruction("Pickpocket the master farmer and bank the seeds")
				.notes("Ranarr and snapdragon seeds make this decent money for Farming later.")
				.wikiPage(WIKI)
				.build(),
			Method.builder()
				.id("thieve_ardougne_knights")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Pickpocket knights of Ardougne")
				.minLevel(55)
				.xpPerAction(84.3)
				.xpPerHour(60_000)
				.coinsPerAction(50)
				.members(true)
				.tag(MethodTag.FAST).tag(MethodTag.PROFIT)
				.location(Location.of("Ardougne market", 2655, 3302, 12))
				.bank(Banks.ARDOUGNE_SOUTH)
				.target(Target.npc("Pickpocket", "Knight of Ardougne"))
				.supply(Supplies.food(20))
				.instruction("Pickpocket a knight of Ardougne over and over")
				.notes("Bring food. The Ardougne diary and a dodgy necklace raise success a lot. " + POUCHES)
				.wikiPage(WIKI)
				.rateAt(70, 90_000).rateAt(85, 130_000).rateAt(99, 150_000)
				.build()
		);
	}

	private ThievingGuide()
	{
	}
}
