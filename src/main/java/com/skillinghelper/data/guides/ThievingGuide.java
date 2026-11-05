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
				.howTo("The men and women walk around Lumbridge Castle's courtyard and the town around it: follow "
					+ "the arrow.")
				.howTo(pickpocket("man or woman"))
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
				.howTo("The tea stall is in east Varrock, a short walk east of Varrock east bank: follow the arrow.")
				.howTo(stall("tea stall"))
				.howTo("When your inventory fills with cups of tea, drop them all (hold Shift and click) and "
					+ "carry on.")
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
				.howTo("The baker's stall is in the middle of East Ardougne market: follow the arrow.")
				.howTo("Stand on the tile at the north-east corner of the stall: the guard can't see you there.")
				.howTo(stall("baker's stall"))
				.howTo("When your inventory is full, drop the bread and chocolate slices and keep the cakes if "
					+ "you want free food.")
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
				.getThere("Pyramid Plunder is in Sophanem, in the south of the desert (you need Icthlarin's "
					+ "Little Helper). The pharaoh's sceptre or the magic carpet to Sophanem gets you there. "
					+ "Bring a lockpick, some food and stamina potions.")
				.howTo("Walk into the pyramid in the middle of Sophanem and click the highlighted Guardian mummy "
					+ "(\"Start-minigame\").")
				.howTo("From then on the guide outlines one thing at a time for you: the speartrap, the urns, the "
					+ "golden chest and the next door. Click what's outlined.")
				.howTo("Each round lasts 5 minutes. When time runs out you're put outside: start a new round with "
					+ "the mummy.")
				.howTo("Bank the artefacts at Nardah or sell them to Simon Templeton at the Agility Pyramid.")
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
				.getThere("The Rogues' Den is under the Toad and Chicken inn in Burthorpe: go into the inn and "
					+ "climb down the trapdoor. You need 50 Agility and 50 Thieving.")
				.getThere("You can't take any items into the maze, so bank everything except stamina potions "
					+ "first (there's a banker in the den).")
				.howTo("The first time, talk to Brian O'Richard to start. After that, just go through the "
					+ "doorway.")
				.howTo("The maze is a path full of traps: spinning blades, tiles that look different, hidden "
					+ "traps to search for, doors to pick, and flash powder to throw at the guard. Every mistake "
					+ "drains a little Agility or Thieving; if either reaches zero you get thrown out (it's safe, "
					+ "you don't die).")
				.howTo("Follow a route guide for the maze (the wiki's Rogues' Den page has the full "
					+ "click-by-click path and a tile marker file for RuneLite's Ground Markers).")
				.howTo("At the end, click the wall safe to crack it: you get a Rogue outfit piece or crate and go "
					+ "back to the lobby. Repeat until you have all five pieces.")
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
				.howTo("The master farmers walk around the Draynor Village market, next to the bank: follow the "
					+ "arrow.")
				.howTo(pickpocket("master farmer"))
				.howTo("The seeds stack. When your inventory is full of other stuff, bank the seeds at Draynor "
					+ "bank right next door.")
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
				.howTo("The knights of Ardougne walk around the East Ardougne market square, next to the south "
					+ "bank: follow the arrow. Pick one that's away from the others.")
				.howTo(pickpocket("knight of Ardougne"))
				.howTo("Bank for more food at the south bank when you run out.")
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
				.howTo("The fruit stalls are in Hosidius (Great Kourend). Use the easternmost house by the beach: "
					+ "it has two stalls close together and no guard dogs.")
				.howTo(stall("fruit stall"))
				.howTo("Drop the fruit as you go (hold Shift and click). Eat strange fruit if you find any to "
					+ "refill your run energy.")
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
				.getThere("You need part of The Feud done and a blackjack (from Ali the Discount Seller in "
					+ "Pollnivneach). Get to Pollnivneach by the magic carpet from the Shantay Pass. Fill your "
					+ "inventory with food or jugs of wine.")
				.howTo("Find a bandit (the highlighted ones) and lure it into an empty house: right-click it, "
					+ "pick \"Lure\", walk into the house and close the curtain behind you.")
				.howTo("Click \"Knock-Out\". If it works the bandit sits down dazed.")
				.howTo("Straight away click \"Pickpocket\", then click \"Pickpocket\" again the moment the first "
					+ "xp shows up. That's two pickpockets per knock-out.")
				.howTo("Then knock it out again and repeat. If a knock-out fails, click Knock-Out again at once "
					+ "before it fights back.")
				.howTo("If it does start attacking you, take off the blackjack and click Knock-Out to stop it. "
					+ "Eat when you're low.")
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
				.location(Location.of("Port Piscarilius", 1800, 3745, 60))
				.tool(ItemReq.tool("Lockpick", ItemID.LOCKPICK))
				.supply(Supplies.STAMINA)
				.target(Target.npc(null, "Captain Khaled"))
				.target(Target.npc(null, "Patrolman", "Patrolwoman"))
				.target(Target.object(null, "Drawers"))
				.instruction("Get an assignment from Captain Khaled, sneak into the marked house past the patrols and "
					+ "steal the artefact from the drawers, then hand it in")
				.notes("Each heist also gives 500 to 1,000 coins. Getting caught costs a lot of time, so wait for the "
					+ "patrol to pass.")
				.wikiPage("Stealing_artefacts")
				.rateAt(65, 90_000).rateAt(80, 140_000).rateAt(99, 230_000)
				.getThere("Stealing artefacts is in Port Piscarilius (Great Kourend). Bring a lockpick and "
					+ "stamina potions.")
				.howTo("Talk to the highlighted Captain Khaled in the foodhall on the east side of Port "
					+ "Piscarilius. He tells you which house to rob (they're in the south-west of the port).")
				.howTo("Walk to that house, watching the patrolmen and patrolwomen walking the streets. Wait "
					+ "until they've walked past before you go in.")
				.howTo("Click the highlighted drawers inside to pick the lock and take the artefact.")
				.howTo("Sneak back out the same way, avoiding the patrols, and hand the artefact to Captain "
					+ "Khaled. Ask him for a new house and repeat.")
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
				.getThere("This is in Civitas illa Fortis in Varlamore (you need Children of the Sun). Bring some "
					+ "food.")
				.howTo("Pickpocket the highlighted wealthy citizens around the city until you get a house key.")
				.howTo("Go to the house the key belongs to, go in, and search the chests, jewellery cases and "
					+ "wardrobes. When an item flashes with an arrow over it, click it quickly for a bigger "
					+ "bonus.")
				.howTo("When the owner comes home, leave. Then sell the valuables to the highlighted Oriana for "
					+ "coins and repeat.")
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
				.getThere("You need Sins of the Father done and must wear vyre noble clothing (top, legs, shoes), "
					+ "or the vyres attack you. Drakan's medallion teleports you to Darkmeyer.")
				.howTo(pickpocket("vyre"))
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
				.getThere("You need Mourning's End Part I done to enter Lletya, in the forest of Tirannwn. "
					+ "Teleport crystals or the Lletya teleport are the quickest ways there.")
				.howTo(pickpocket("elf"))
				.build()
		);
	}

	/**
	 * The beginner steps every pickpocketing method shares.
	 */
	private static List<String> pickpocket(String who)
	{
		return List.of(
			"Left-click a highlighted " + who + " with \"Pickpocket\". If the left-click says something else, "
				+ "right-click them and pick \"Pickpocket\".",
			"If it works you get loot (coins arrive as coin pouches). If it fails, you get stunned for a few seconds "
				+ "(stars over your head) and take a small hit: just wait and click again.",
			"Eat food when your hitpoints drop below about half.",
			"Click a coin pouch in your inventory to open them all before you reach 28, or you can't pickpocket any "
				+ "more until you do.");
	}

	/**
	 * The beginner steps every stall shares.
	 */
	private static List<String> stall(String which)
	{
		return List.of(
			"Stand right next to the highlighted " + which + " and left-click it with \"Steal-from\".",
			"After each steal the stall is empty for a few seconds. Stay where you are and click it again as soon as "
				+ "the goods are back.",
			"If the stall owner or a guard sees you stealing, they shout and attack. Step away for a moment, then "
				+ "come back.");
	}

	private ThievingGuide()
	{
	}
}
