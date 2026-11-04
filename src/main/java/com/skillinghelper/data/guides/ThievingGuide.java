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
				.xpPerHour(15_000)
				.members(true)
				.tag(MethodTag.MINIGAME).tag(MethodTag.FAST)
				.quest(Quest.ICTHLARINS_LITTLE_HELPER)
				.location(Location.of("Pyramid Plunder (Sophanem)", 3288, 2794, 15))
				.target(Target.npc("Start-minigame", "Guardian mummy"))
				.bank(Location.of("Nardah bank", 3428, 2892, 5))
				.product(ItemID.NTK_IVORY_COMB).product(ItemID.NTK_SCARAB_POTTERY).product(ItemID.NTK_STATUETTE_POTTERY)
				.product(ItemID.NTK_SEAL_STONE).product(ItemID.NTK_SCARAB_STONE).product(ItemID.NTK_STATUETTE_STONE)
				.product(ItemID.NTK_SEAL_GOLD).product(ItemID.NTK_SCARAB_GOLD).product(ItemID.NTK_STATUETTE_GOLD)
				.supply(ItemReq.supply("Lockpick", 1, ItemID.LOCKPICK))
				.supply(Supplies.food(5))
				.supply(Supplies.STAMINA)
				.instruction("Search the pyramid door that leads to the Guardian mummy and start a round. The guide "
					+ "then outlines one thing at a time: trap, urns, chest, door")
				.notes("Per the wiki: skip the early rooms' urns, search every urn in your last two rooms, keep 90 "
					+ "seconds for the last one. A lockpick helps doors a lot at low levels. Just search urns: "
					+ "checking and charming snakes is slower.")
				.wikiPage("Pyramid_Plunder")
				// Wiki Thieving training (with a sceptre): 125k at 71-80, 190k at 81-90, 270k at 91+. Below 71 the wiki only
				// says it's much lower; halved after in-game play came in at about half the old estimate
				.rateAt(41, 30_000).rateAt(51, 45_000).rateAt(61, 70_000).rateAt(71, 110_000).rateAt(81, 170_000)
				.rateAt(91, 250_000)
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
				.build(),
			Method.builder()
				.id("thieve_fruit_stall")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Fruit stalls (Hosidius)")
				.minLevel(25)
				.xpPerAction(28.5)
				.xpPerHour(35_000)
				.members(true)
				.tag(MethodTag.POWER).tag(MethodTag.CHEAP)
				.location(Location.of("Hosidius fruit stalls", 1767, 3593, 35))
				.dropByDefault(true)
				.target(Target.object("Steal-from", "Fruit Stall"))
				.instruction("Steal from the fruit stalls and drop the fruit as you go")
				.notes("Use the easternmost house by the beach: two stalls close together and no guard dogs. Eat strange "
					+ "fruit to restore run energy.")
				.wikiPage(WIKI)
				.rateAt(40, 45_000).rateAt(50, 50_000)
				.build(),
			Method.builder()
				.id("thieve_blackjacking")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Blackjacking (Pollnivneach)")
				.minLevel(45)
				.xpPerAction(65)
				.xpPerHour(86_000)
				.members(true)
				.tag(MethodTag.FAST)
				.location(Location.of("Pollnivneach", 3350, 2960, 25))
				.xpAt(new int[]{55, 84}).xpAt(new int[]{65, 137})
				.quest(Quest.THE_FEUD)
				.requirementText("A blackjack (any kind)")
				.supply(Supplies.food(15))
				.target(Target.npc(null, "Bandit", "Menaphite Thug"))
				.instruction("Lure a bandit into an empty house and shut the curtain. Click Knock-Out, then Pickpocket "
					+ "straight away, then Pickpocket again the moment the first xp drop shows: two guaranteed "
					+ "pickpockets while it's out. If a knock-out fails, click Knock-Out again at once. With "
					+ "\"Left-click Knock-Out/Pickpocket\" on (the default), the left-click is Knock-Out while it's "
					+ "up and Pickpocket once it's down (hold Shift for the normal menu)")
				.notes("Failed knock-outs are a plain roll on your Thieving level (about 59% success at 45, 72% at 65, "
					+ "94% at 99); the blackjack and the target don't change it, so failing a lot at low levels is "
					+ "normal. What costs xp is combat: a failed knock-out makes it aggressive, and Knock-Out again "
					+ "straight away resets that before it hits. Keep your inventory full with inventory-blocking "
					+ "food (jugs of wine from Faisal at the bar, or noted food unnoted at the Banknote Exchange "
					+ "Merchant), so a mistimed Pickpocket on an awake bandit does nothing instead of stunning you. "
					+ "If it does start attacking, unequip the blackjack and click Knock-Out to stop it; no need to "
					+ "run. Bearded bandits from 45, beardless from 55 (Ali the Dyer's house in the north has a "
					+ "ladder to reset), Menaphite Thugs from 65 (the house next to the bar). Rogue equipment "
					+ "doubles the coins. Only part of The Feud is needed.")
				.wikiPage("Blackjack")
				.rateAt(50, 90_000).rateAt(55, 121_000).rateAt(60, 126_000).rateAt(65, 209_000)
				.rateAt(75, 226_000).rateAt(85, 241_000).rateAt(99, 261_000)
				.build(),
			Method.builder()
				.id("thieve_artefacts")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Stealing artefacts (Port Piscarilius)")
				.minLevel(49)
				.xpPerAction(500)
				.xpPerHour(60_000)
				.members(true)
				.tag(MethodTag.FAST).tag(MethodTag.PROFIT)
				.location(Location.of("Port Piscarilius", 1800, 3750, 40))
				.tool(ItemReq.tool("Lockpick", ItemID.LOCKPICK))
				.supply(Supplies.STAMINA)
				.target(Target.npc(null, "Captain Khaled"))
				.target(Target.object(null, "Drawers"))
				.instruction("Get an assignment from Captain Khaled, sneak into the marked house past the patrols and "
					+ "steal the artefact from the drawers, then hand it in")
				.notes("Each heist also gives 500 to 1,000 coins. Getting caught costs a lot of time, so wait for the "
					+ "patrol to pass.")
				.wikiPage("Stealing_artefacts")
				.rateAt(65, 90_000).rateAt(80, 140_000).rateAt(99, 230_000)
				.build(),
			Method.builder()
				.id("thieve_varlamore_valuables")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Stealing valuables (Civitas illa Fortis)")
				.minLevel(50)
				.xpPerAction(96)
				.xpPerHour(65_000)
				.members(true)
				.tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.location(Location.of("Civitas illa Fortis houses", 1695, 3098, 20))
				.quest(Quest.CHILDREN_OF_THE_SUN)
				.target(Target.npc("Pickpocket", "Wealthy citizen"))
				.target(Target.npc(null, "Oriana"))
				.instruction("Pickpocket wealthy citizens at the bazaar for house keys, then loot the chests, jewellery "
					+ "cases and wardrobes in the empty house")
				.notes("Low effort. Click the furniture with the flashing arrow quickly for a big bonus. Trade the "
					+ "valuables to Oriana for coins.")
				.wikiPage("Stealing_valuables")
				.rateAt(70, 80_000).rateAt(90, 95_000)
				.build(),
			Method.builder()
				.id("thieve_vyres")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Pickpocket vyres (Darkmeyer)")
				.minLevel(82)
				.xpPerAction(306.9)
				.xpPerHour(120_000)
				.members(true)
				.tag(MethodTag.PROFIT)
				.location(Location.of("Darkmeyer", 3640, 3370, 30))
				.quest(Quest.SINS_OF_THE_FATHER)
				.requirementText("Wear vyre noble clothing so the vyres don't attack you")
				.supply(Supplies.food(15))
				.target(Target.npcContaining("Pickpocket", "von"))
				.instruction("Pickpocket the vyres in Darkmeyer")
				.notes("Rogue equipment, dodgy necklaces and the hard Ardougne diary make this much better. "
					+ POUCHES)
				.wikiPage(WIKI)
				.rateAt(99, 180_000)
				.build(),
			Method.builder()
				.id("thieve_elves")
				.skill(Skill.THIEVING)
				.type(MethodType.ACTIVITY)
				.name("Pickpocket elves (Lletya)")
				.minLevel(85)
				.xpPerAction(353.3)
				.xpPerHour(120_000)
				.members(true)
				.tag(MethodTag.PROFIT)
				.location(Location.of("Lletya", 2336, 3161, 20))
				.quest(Quest.MOURNINGS_END_PART_I)
				.supply(Supplies.food(15))
				.target(Target.npc("Pickpocket", "Goreu", "Arvel", "Mawrth", "Kelyn"))
				.instruction("Pickpocket the elves in Lletya")
				.notes("Elves can drop crystal shards and enhanced crystal teleport seeds. Use dodgy necklaces and "
					+ "rogue equipment. " + POUCHES)
				.wikiPage(WIKI)
				.rateAt(99, 150_000)
				.build()
		);
	}

	private ThievingGuide()
	{
	}
}
