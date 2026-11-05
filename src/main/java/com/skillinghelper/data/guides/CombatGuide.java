package com.skillinghelper.data.guides;

import com.skillinghelper.data.CombatGear;
import com.skillinghelper.data.CombatStyle;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.QuestGroup;
import com.skillinghelper.data.Target;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Melee, Ranged and Hitpoints training, based on the OSRS wiki "Free-to-play melee training", "Pay-to-play melee
 * training" and "Ranged training" guides. Combat xp is 4 per damage in the trained style (1.33 for Hitpoints), so
 * xp per kill is worked out from the monster's hitpoints. Rates assume gear and stats typical for the level range.
 */
final class CombatGuide
{
	private static final String MELEE_F2P_WIKI = "Free-to-play_melee_training";
	private static final String MELEE_P2P_WIKI = "Pay-to-play_melee_training";
	private static final String RANGED_WIKI = "Ranged_training";
	private static final String GEMSTONE_CRAB = "gemstone_crab";
	private static final String NMZ = "nmz";
	/**
	 * Nightmare Zone opens once any 5 of these quests are done (wiki "Nightmare Zone", Requirements).
	 */
	static final QuestGroup NMZ_QUESTS = QuestGroup.of("Nightmare Zone quests", 5,
		Quest.THE_ASCENT_OF_ARCEUUS, Quest.CONTACT, Quest.THE_CORSAIR_CURSE, Quest.THE_DEPTHS_OF_DESPAIR,
		Quest.DESERT_TREASURE_I, Quest.DRAGON_SLAYER_I, Quest.DREAM_MENTOR, Quest.FAIRYTALE_I__GROWING_PAINS,
		Quest.FAMILY_CREST, Quest.FIGHT_ARENA, Quest.THE_FREMENNIK_ISLES, Quest.GETTING_AHEAD, Quest.THE_GRAND_TREE,
		Quest.THE_GREAT_BRAIN_ROBBERY, Quest.GRIM_TALES, Quest.HAUNTED_MINE, Quest.HOLY_GRAIL,
		Quest.HORROR_FROM_THE_DEEP, Quest.IN_SEARCH_OF_THE_MYREQUE, Quest.LEGENDS_QUEST, Quest.LOST_CITY,
		Quest.LUNAR_DIPLOMACY, Quest.MONKEY_MADNESS_I, Quest.MOUNTAIN_DAUGHTER, Quest.MY_ARMS_BIG_ADVENTURE,
		Quest.ONE_SMALL_FAVOUR, Quest.RECIPE_FOR_DISASTER, Quest.ROVING_ELVES, Quest.SHADOW_OF_THE_STORM,
		Quest.SHILO_VILLAGE, Quest.SONG_OF_THE_ELVES, Quest.TALE_OF_THE_RIGHTEOUS, Quest.TREE_GNOME_VILLAGE,
		Quest.TROLL_ROMANCE, Quest.TROLL_STRONGHOLD, Quest.VAMPYRE_SLAYER, Quest.WHAT_LIES_BELOW,
		Quest.WITCHS_HOUSE);
	/**
	 * The AFK customisable-rumble bosses from the wiki Nightmare Zone strategies (melee and ranged), best first: the
	 * usual five, then the ones to swap in. The guide asks for 5 of them.
	 */
	static final QuestGroup NMZ_AFK_BOSSES = QuestGroup.labelled("Rumble bosses", 5,
		Quest.VAMPYRE_SLAYER, "Count Draynor",
		Quest.THE_DEPTHS_OF_DESPAIR, "Sand Snake",
		Quest.WHAT_LIES_BELOW, "King Roald",
		Quest.MOUNTAIN_DAUGHTER, "The Kendal",
		Quest.LUNAR_DIPLOMACY, "Me",
		Quest.TREE_GNOME_VILLAGE, "Khazard warlord",
		Quest.LOST_CITY, "Tree spirit",
		Quest.THE_ASCENT_OF_ARCEUUS, "Trapped Soul",
		Quest.GETTING_AHEAD, "Headless Beast");
	/**
	 * The Gemstone Crab burrows every 10 minutes and pops up at the next of these three spots, from the wiki.
	 */
	static final Location[] GEMSTONE_CRAB_SPOTS = {
		Location.of("Gemstone Crab (north)", 1275, 3160, 12),
		Location.of("Gemstone Crab (east)", 1351, 3110, 12),
		Location.of("Gemstone Crab (south)", 1237, 3042, 12),
	};

	/**
	 * A monster worth training on.
	 */
	private static final class Monster
	{
		final String key;
		final String name;
		final String[] npcs;
		final int hitpoints;
		final int level;
		final int meleeXpHr;
		final boolean members;
		final Location location;
		final String notes;
		final String requirement;
		final boolean afk;
		/**
		 * Melee xp per hour at higher levels, as level, xp per hour pairs.
		 */
		final int[] meleeRates;
		/**
		 * Lowest level for melee and Hitpoints training here, or 0 when it's a ranged-only spot.
		 */
		int meleeLevel;
		/**
		 * Lowest level for Ranged training here, or 0 when it isn't a ranged spot.
		 */
		int rangedLevel;
		/**
		 * The Ranged method throws chinchompas into groups of monsters instead of firing a bow.
		 */
		boolean chinning;
		/**
		 * The Ranged method uses a dwarf multicannon as well.
		 */
		boolean cannon;
		String rangedName;
		int rangedXpHr;
		/**
		 * Ranged xp per hour at higher levels, when it isn't just the melee rate scaled.
		 */
		int[] rangedRates;
		Quest quest;
		/**
		 * A quest that only has to be started (or partly done) to reach the spot.
		 */
		Quest questStarted;
		ItemReq tool;
		int slayerLevel;
		/**
		 * Numbered beginner steps for melee (and Ranged, unless {@link #rangedHowTo} is set).
		 */
		String[] howTo = {};
		String[] rangedHowTo;

		Monster(String key, String name, String[] npcs, int hitpoints, int level, int meleeXpHr, boolean members,
			Location location, String notes, String requirement, boolean afk, int... meleeRates)
		{
			this.key = key;
			this.name = name;
			this.npcs = npcs;
			this.hitpoints = hitpoints;
			this.level = level;
			this.meleeXpHr = meleeXpHr;
			this.members = members;
			this.location = location;
			this.notes = notes;
			this.requirement = requirement;
			this.afk = afk;
			this.meleeRates = meleeRates;
			this.meleeLevel = level;
			this.rangedLevel = level;
		}

		Monster meleeOnly()
		{
			rangedLevel = 0;
			return this;
		}

		Monster rangedOnly()
		{
			meleeLevel = 0;
			return this;
		}

		Monster rangedFrom(int level)
		{
			rangedLevel = level;
			return this;
		}

		/**
		 * Ranged xp per hour from {@code level}, then as level, xp per hour pairs.
		 */
		Monster rangedRates(int xpHr, int... rates)
		{
			rangedXpHr = xpHr;
			rangedRates = rates;
			return this;
		}

		Monster chinning(String name)
		{
			chinning = true;
			rangedName = name;
			return this;
		}

		Monster cannon(String name)
		{
			cannon = true;
			rangedName = name;
			return this;
		}

		Monster quest(Quest quest)
		{
			this.quest = quest;
			return this;
		}

		Monster questStarted(Quest quest)
		{
			this.questStarted = quest;
			return this;
		}

		Monster slayer(int level)
		{
			slayerLevel = level;
			return this;
		}

		Monster howTo(String... steps)
		{
			howTo = steps;
			return this;
		}

		Monster rangedHowTo(String... steps)
		{
			rangedHowTo = steps;
			return this;
		}

		Monster tool(ItemReq tool)
		{
			this.tool = tool;
			return this;
		}
	}

	private static final List<ItemReq> CANNON = List.of(
		ItemReq.tool("Cannon base", ItemID.TWPART1),
		ItemReq.tool("Cannon stand", ItemID.TWPART2),
		ItemReq.tool("Cannon barrels", ItemID.TWPART3),
		ItemReq.tool("Cannon furnace", ItemID.TWPART4));
	/**
	 * About 6 cannonballs per ogre (60 hitpoints, the cannon hits up to 30).
	 */
	private static final ItemReq CANNONBALLS = ItemReq.stack("Cannonballs", 6, ItemID.MCANNONBALL,
		ItemID.GRANITE_CANNONBALL);

	private static final List<Monster> MONSTERS = List.of(
		new Monster("chickens", "Chickens (Lumbridge)", new String[]{"Chicken"}, 3, 1, 2_500, false,
			Location.of("Lumbridge chicken pen", 3231, 3297, 8),
			"Chickens can't really hurt you. Pick up feathers for Fishing if you like.", null, false, 10, 5_000)
			.howTo("The chickens are in a fenced pen just north of Lumbridge: follow the arrow, then click the "
					+ "gate to go in.",
				"Open the combat options tab (the crossed swords icon) and click the attack style named above, "
					+ "so the xp goes to the right skill. Wear your best weapon and armour (the guide tells you what "
					+ "to equip).",
				"Left-click a highlighted chicken (\"Attack\"). Your character keeps fighting it until it dies.",
				"When it dies, click the next chicken. Chickens barely hurt you, so you don't need food.",
				"Optional: pick up the feathers they drop (click them on the ground) to use for fishing later."),
		new Monster("cows", "Cows (Lumbridge)", new String[]{"Cow", "Cow calf"}, 8, 10, 6_000, false,
			Location.of("Lumbridge cow field", 3257, 3270, 12),
			"Bank the cowhides for Crafting, or sell them.", null, false, 20, 10_000)
			.howTo("The cows are in the big fenced field east of Lumbridge, across the river: follow the arrow, "
					+ "then click the gate to go in.",
				"Open the combat options tab (the crossed swords icon) and click the attack style named above, "
					+ "so the xp goes to the right skill. Wear your best weapon and armour (the guide tells you what "
					+ "to equip).",
				"Left-click a highlighted cow (\"Attack\"). Your character keeps fighting it until it dies, then "
					+ "click the next one.",
				"Cows hit very weakly. If your hitpoints drop below half, eat something or wait a bit.",
				"Optional: pick up the cowhides (click them on the ground) and bank them to sell or tan for "
					+ "Crafting."),
		new Monster("al_kharid_warriors", "Al Kharid warriors", new String[]{"Al Kharid warrior"}, 19, 20, 12_000,
			false, Location.of("Al Kharid palace", 3293, 3173, 10),
			"They hit back, so bring some food.", null, false, 30, 18_000, 40, 22_000)
			.howTo("The Al Kharid warriors are inside the Al Kharid palace, in the middle of Al Kharid: follow the "
					+ "arrow. Bring some food (the guide takes it out at the bank).",
				"Open the combat options tab (the crossed swords icon) and click the attack style named above, "
					+ "so the xp goes to the right skill. Wear your best weapon and armour (the guide tells you what "
					+ "to equip).",
				"Left-click a highlighted Al Kharid warrior (\"Attack\"). Your character fights until it dies; "
					+ "then click the next one.",
				"They hit back, so eat a piece of food whenever your hitpoints drop below half.",
				"When the food runs out, walk to Al Kharid bank (just south-west of the palace) for more."),
		new Monster("hill_giants", "Hill giants (Edgeville Dungeon)", new String[]{"Hill Giant"}, 35, 30, 18_000, false,
			Location.of("Edgeville Dungeon hill giants", 3117, 9845, 12),
			"Bring food. Big bones are worth burying or selling.",
			"A brass key opens the hill giant room door", false, 50, 30_000, 70, 40_000)
			.howTo("The hill giants are in Edgeville Dungeon: climb down the trapdoor just south of Edgeville bank "
					+ "and follow the arrow through the dungeon. Next time you can use the brass key that lies just "
					+ "north of their room: it opens a shed west of the Cooks' Guild that leads straight down to them.",
				"Open the combat options tab (the crossed swords icon) and click the attack style named above, "
					+ "so the xp goes to the right skill. Wear your best weapon and armour (the guide tells you what "
					+ "to equip).",
				"Left-click a highlighted hill giant (\"Attack\"). Your character fights until it dies; then click "
					+ "the next one.",
				"They hit hard for low levels: eat whenever your hitpoints drop below half.",
				"Pick up the big bones they drop if you want to bury them for Prayer or sell them. When your "
					+ "food runs out, go back to Edgeville bank."),
		new Monster("sand_crabs", "Sand crabs (Hosidius)", new String[]{"Sand Crab"}, 60, 20, 15_000, true,
			Location.of("Hosidius sand crab beach", 1740, 3468, 30),
			"Very AFK: stand next to a few crabs and they wake up. Run away and back every 10 minutes when they "
				+ "stop attacking you.", null, true, 40, 30_000, 60, 45_000, 80, 60_000, 99, 70_000)
			.howTo("The sand crabs are on the beach north of Hosidius in Great Kourend: follow the arrow (sail with "
					+ "Veos from Port Sarim the first time, then use the Kourend Castle teleport or a skills "
					+ "necklace).",
				"Open the combat options tab (the crossed swords icon) and click the attack style named above, "
					+ "so the xp goes to the right skill. Wear your best weapon and armour (the guide tells you what "
					+ "to equip).",
				"Stand on an open tile with 2-4 crabs around you (they look like rocks until they wake up). They "
					+ "wake up and attack you on their own.",
				"Turn on auto-retaliate (in the combat tab) and leave it running. Eat when your hitpoints get "
					+ "low.",
				"After about 10 minutes they stop waking up because they got used to you. Walk away until "
					+ "they're off your screen (about 30 tiles), wait a few seconds, then walk back to the same spot.",
				"If someone is already standing where you want to be, pick another group of crabs or hop worlds."),
		// The crab can't die and takes any damage, so a "kill" here is ~100 damage at the reduced 3.5 xp per damage
		new Monster(GEMSTONE_CRAB, "Gemstone Crab (Varlamore)", new String[]{"Gemstone Crab"}, 88, 1, 5_000, true,
			GEMSTONE_CRAB_SPOTS[0],
			"Very AFK and it never hits back. Every 10 minutes it burrows and moves to one of the other two spots: "
				+ "crawl through the cave next to the old spot to come out at the new one. The top 16 damage dealers "
				+ "can mine the shell afterwards for gems (bring any pickaxe).", null, true,
			20, 18_000, 40, 35_000, 60, 52_000, 80, 70_000, 99, 85_000)
			.howTo("The Gemstone Crab is in Varlamore, outside Civitas illa Fortis: take a quetzal or follow the "
					+ "arrow. It sits at one of three spots and moves every 10 minutes; the guide points to the spots.",
				"Open the combat options tab (the crossed swords icon) and click the attack style named above, "
					+ "so the xp goes to the right skill. Wear your best weapon and armour (the guide tells you what "
					+ "to equip).",
				"Click the highlighted Gemstone Crab (\"Attack\") and leave your character fighting it. It never "
					+ "hits back, so you don't need food.",
				"When the crab burrows (after about 10 minutes), click the highlighted cave/tunnel next to where "
					+ "it was and crawl through: you come out next to its new spot. Attack it again.",
				"If you were in the top 16 for damage, mine the crab's shell with a pickaxe when it burrows for "
					+ "gems."),
		new Monster("nmz", "Nightmare Zone", new String[]{}, 400, 60, 55_000, true,
			Location.of("Nightmare Zone", 2606, 3115, 8),
			"You can't die for real in here: if you die, the dream ends and you keep all your items.",
			null, true, 80, 85_000, 99, 110_000)
			// Ranged NMZ is slower than melee there; the wiki starts it at 70 with a magic shortbow or blowpipe
			.rangedFrom(70).rangedRates(60_000, 80, 85_000, 99, 120_000),
		new Monster("rock_crabs", "Rock crabs (Rellekka)", new String[]{"Rock Crab"}, 50, 10, 12_000, true,
			Location.of("Rellekka rock crabs", 2706, 3722, 15),
			"AFK like sand crabs: stand between a few rocks and the crabs wake up. When they stop attacking you "
				+ "(about 10 minutes), walk well away and come back.", null, true, 25, 20_000, 40, 28_000)
			.howTo("Stand on an open tile with 2-4 crabs around you (they look like rocks until they wake up). They "
					+ "wake up and attack you on their own",
				"Turn on auto-retaliate (the crossed-swords option in the combat tab) and leave it running. "
					+ "Eat when your hitpoints get low",
				"After about 10 minutes they stop waking up because they got used to you. Walk away until they "
					+ "are off your screen (about 30 tiles), wait a few seconds, then walk back to the same spot",
				"Let other players have their own spot: if someone is standing where you want to be, pick "
					+ "another group of crabs or hop worlds"),
		new Monster("ammonite_crabs", "Ammonite crabs (Fossil Island)", new String[]{"Ammonite Crab"}, 100, 30,
			25_000, true, Location.of("Fossil Island north coast ammonite crabs", 3718, 3870, 30),
			"The best crabs: 100 hitpoints and low Defence. They wake up when you stand next to them; when they "
				+ "stop attacking you, walk away and come back. The Museum Camp bank chest is close by.",
			null, true, 50, 40_000, 70, 55_000, 85, 70_000, 99, 80_000)
			.quest(Quest.BONE_VOYAGE)
			.howTo("Get to Fossil Island: take the barge from the Digsite (Bone Voyage unlocks it) or use a digsite "
					+ "pendant, then walk north to the coast",
				"Stand on an open tile with 2-4 crabs around you (they look like rocks until they wake up). They "
					+ "wake up and attack you on their own",
				"Turn on auto-retaliate (the crossed-swords option in the combat tab) and leave it running. "
					+ "Eat when your hitpoints get low",
				"After about 10 minutes they stop waking up because they got used to you. Walk away until they "
					+ "are off your screen (about 30 tiles), wait a few seconds, then walk back to the same spot",
				"Let other players have their own spot: if someone is standing where you want to be, pick "
					+ "another group of crabs or hop worlds"),
		new Monster("ogres_cannon", "Ogres (Castle Wars)", new String[]{"Ogre"}, 60, 1, 40_000, true,
			Location.of("Ogres east of Castle Wars", 2505, 3100, 15),
			"Set the cannon up among the ogres and attack them too. The cannon does most of the damage, so this "
				+ "is the fastest Ranged xp until chinchompas at 45, but cannonballs cost money.",
			null, false, 30, 45_000, 45, 50_000)
			.cannon("Cannon ogres (Castle Wars)").quest(Quest.DWARF_CANNON).rangedOnly()
			.howTo("Find a spot among the ogres where no other player has a cannon. If someone is there, hop worlds",
				"Click the cannon base in your inventory and choose Set-up. You need all 4 parts (base, stand, "
					+ "barrels, furnace) in your inventory; the cannon builds itself",
				"Click the cannon and choose Fire. It loads your cannonballs and starts shooting every ogre around "
					+ "it",
				"Equip your bow and arrows, set Rapid in the combat tab, and attack the ogres too: you get Ranged "
					+ "xp from your own hits and from the cannon's",
				"When the chat says your cannon is out of ammo, click Fire on it again. Keep cannonballs in your "
					+ "inventory",
				"Before you leave, right-click the cannon and choose Pick-up. If you walk away it breaks down after "
					+ "a while and you have to get it back from Nulodion at the Dwarven Mine"),
		new Monster("maniacal_monkeys", "Maniacal monkeys (Kruk's Dungeon)", new String[]{"Maniacal monkey"}, 65,
			75, 140_000, true, Location.of("Kruk's Dungeon maniacal monkeys", 2660, 9151, 20),
			"Fastest melee and Ranged xp. They drop 1-dose prayer potions that keep your prayer topped up. "
				+ "Bring a light source.",
			"Needs Monkey Madness II started up to chapter II (to enter Kruk's Dungeon)", false,
			85, 170_000, 99, 200_000)
			.questStarted(Quest.MONKEY_MADNESS_II)
			.howTo("Get to Ape Atoll (royal seed pod, the Ape Atoll teleport, or the gnome glider), then go into Kruk's "
					+ "Dungeon in the north-west of the island. Wear a monkey greegree on the way if you haven't "
					+ "finished Monkey Madness II, or the guards attack you",
				"Inside, go through the second hole with vines, climb down and head south-east to a big room full "
					+ "of maniacal monkeys. Right-click the hole and choose Look-in to see if someone is already there",
				"Stand next to the wall and turn on auto-retaliate. The monkeys come to you",
				"Pick up the prayer potions they drop and drink them to keep your prayer up",
				"After about 10 minutes they stop attacking you. Run west along the south passage until monkeys "
					+ "there attack you, then come back to your spot")
			.rangedHowTo("Get to Ape Atoll (royal seed pod, the Ape Atoll teleport, or the gnome glider), then go into Kruk's "
					+ "Dungeon in the north-west of the island. Wear a monkey greegree on the way if you haven't "
					+ "finished Monkey Madness II, or the guards attack you",
				"Inside, go through the second hole with vines, climb down and head south-east to a big room full "
					+ "of maniacal monkeys. Right-click the hole and choose Look-in to see if someone is already there",
				"Wield your chinchompas (they go in your weapon slot) and pick Long fuse in the combat tab",
				"Walk back and forth a couple of tiles so the monkeys bunch up on top of each other, then throw a "
					+ "chinchompa at the middle of the group: it hits every monkey around the one you click",
				"Pick up the prayer potions they drop. After about 10 minutes they stop attacking you: run west "
					+ "along the south passage until monkeys there attack you, then come back")
			.chinning("Chinning maniacal monkeys (Kruk's Dungeon)").rangedFrom(45)
			.rangedRates(200_000, 55, 260_000, 65, 350_000, 75, 420_000, 85, 480_000, 99, 550_000),
		new Monster("skeletal_monkeys", "Skeletal monkeys (Ape Atoll Dungeon)", new String[]{"Skeleton"}, 77, 45,
			170_000, true, Location.of("Ape Atoll Dungeon skeletal monkeys", 2740, 9114, 15),
			"Only worth it if you can't get to the maniacal monkeys. They are undead, so a salve amulet helps. "
				+ "Bring prayer potions and a light source.",
			"Needs Monkey Madness I done far enough to enter the Ape Atoll Dungeon", false)
			.questStarted(Quest.MONKEY_MADNESS_I)
			.rangedHowTo("Get to Ape Atoll (royal seed pod, the Ape Atoll teleport, or the gnome glider). Wear a monkey "
					+ "greegree so the monkeys in town leave you alone",
				"Go down into the Ape Atoll Dungeon (the way in you used in Monkey Madness I). Watch for the spike "
					+ "traps on the floor (small dots): walk around them",
				"Wield your chinchompas (they go in your weapon slot) and pick Long fuse in the combat tab",
				"Let a few skeletal monkeys come to you so they stand together, then throw a chinchompa at the "
					+ "middle of the group: it hits every monkey around the one you click",
				"Eat or drink prayer potions as you need them, and run off and back when they stop attacking you "
					+ "(about 10 minutes)")
			.chinning("Chinning skeletal monkeys (Ape Atoll Dungeon)")
			.rangedRates(170_000, 55, 220_000, 65, 300_000, 75, 360_000, 85, 410_000, 99, 470_000)
			.rangedOnly(),
		new Monster("sulphur_naguas", "Sulphur naguas (Neypotzli)", new String[]{"Sulphur Nagua"}, 100, 55, 60_000,
			true, Location.of("Neypotzli Ancient Prison", 1360, 9563, 15),
			"Very fast melee xp thanks to their negative armour, best with a glacial temotli or sulphur blades. "
				+ "Moonlight potions made inside keep your prayer up.",
			"Needs Perilous Moons done far enough to enter Neypotzli", false, 65, 80_000, 75, 110_000)
			.questStarted(Quest.PERILOUS_MOONS).slayer(48).meleeOnly()
			.howTo("Go to Cam Torum in Varlamore (quetzal to Cam Torum), run to the north end of the city and go into "
					+ "Neypotzli",
				"From the antechamber, take the south-west entrance into the Ancient Prison",
				"Stand where you can see the sun icons on the floor: that's where the naguas respawn, so standing "
					+ "near several of them keeps them coming to you",
				"Turn on auto-retaliate. Each nagua hits twice per attack, so watch your hitpoints and eat",
				"After 5 attacks a nagua raises its weapon. Step 2 tiles away until it has swung, or it drains 5 "
					+ "prayer points",
				"Pick up the noted ores and runes they drop, and bank at the Cam Torum bank when you run out of "
					+ "food"),
		new Monster("gargoyles", "Gargoyles (Slayer Tower)", new String[]{"Gargoyle"}, 105, 80, 70_000, true,
			Location.of("Slayer Tower top floor gargoyles", 3440, 3542, 2, 12),
			"Good melee xp and alchables. Finish each gargoyle with a rock hammer when it's low.",
			null, false, 90, 80_000, 99, 90_000)
			.slayer(75).meleeOnly()
			.howTo("Go to the Slayer Tower in Morytania (Slayer ring, fairy ring CKS, or Canifis and walk north) "
					+ "and climb to the top floor",
				"Attack a gargoyle. Eat when your hitpoints get low",
				"When it's down to 8 hitpoints or less it can't die from hits: use your rock hammer on it to smash "
					+ "it (the Gargoyle smasher Slayer reward does this for you)",
				"Pick up the noted drops and the alchables, and keep going until you run out of food")
			.tool(ItemReq.tool("Rock hammer", ItemID.SLAYER_ROCK_HAMMER, ItemID.GRANITE_HAMMER,
				ItemID.SLAYER_ROCK_THROWNHAMMER))
	);

	static List<Method> methods()
	{
		List<Method> m = new ArrayList<>();
		for (Monster mon : MONSTERS)
		{
			if (mon.meleeLevel > 0)
			{
				m.add(melee(mon, Skill.ATTACK, "Accurate").build());
				m.add(melee(mon, Skill.STRENGTH, "Aggressive").build());
				m.add(melee(mon, Skill.DEFENCE, "Defensive").build());
				m.add(hitpoints(mon).build());
			}
			if (mon.rangedLevel > 0)
			{
				m.add(ranged(mon).build());
			}
		}
		return m;
	}

	private static Method.MethodBuilder base(Monster mon, Skill skill, String prefix)
	{
		Method.MethodBuilder b = Method.builder()
			.id(prefix + "_" + mon.key)
			.skill(skill)
			.type(MethodType.ACTIVITY)
			.name(mon.name)
			.members(mon.members)
			.location(mon.location)
			.notes(mon.notes)
			.requirementText(mon.requirement)
			.wikiPage(mon.members ? MELEE_P2P_WIKI : MELEE_F2P_WIKI);
		if (mon.npcs.length > 0)
		{
			b.target(Target.npc("Attack", mon.npcs));
		}
		if (mon.afk)
		{
			b.tag(MethodTag.AFK);
		}
		if (mon.hitpoints > 3 && !GEMSTONE_CRAB.equals(mon.key))
		{
			b.supply(Supplies.food(mon.hitpoints >= 60 ? 20 : 10));
		}
		if (NMZ.equals(mon.key))
		{
			nightmareZone(b, skill);
		}
		for (String step : mon.howTo)
		{
			b.howTo(step);
		}
		if (mon.quest != null)
		{
			b.quest(mon.quest);
		}
		if (mon.questStarted != null)
		{
			b.questStarted(mon.questStarted);
		}
		if (NMZ.equals(mon.key))
		{
			b.questGroup(NMZ_QUESTS).questGroup(NMZ_AFK_BOSSES);
		}
		if (mon.slayerLevel > 0)
		{
			b.skillLevel(Skill.SLAYER, mon.slayerLevel);
		}
		if (mon.tool != null)
		{
			b.tool(mon.tool);
		}
		if (GEMSTONE_CRAB.equals(mon.key))
		{
			for (int i = 1; i < GEMSTONE_CRAB_SPOTS.length; i++)
			{
				b.alsoAt(GEMSTONE_CRAB_SPOTS[i]);
			}
			b.quest(Quest.CHILDREN_OF_THE_SUN).target(Target.npc("Mine", "Gemstone Crab Shell")).supply(Supplies.PICKAXE);
		}
		return b;
	}

	/**
	 * Nightmare Zone for someone who has never been: what to bring, how to pay and start a customisable rumble
	 * with the AFK bosses they have unlocked, and what to do in the dream. Absorptions and overloads cost points,
	 * so the first dreams use the player's own potions and food.
	 */
	private static void nightmareZone(Method.MethodBuilder b, Skill skill)
	{
		boolean ranged = skill == Skill.RANGED;
		String potion = ranged ? "ranging potion" : "super combat potion";
		b.tag(MethodTag.MINIGAME)
			.instanced(true)
			.supply(ItemReq.supply("Coins (the dream fee: 22,000 per customisable rumble)", 22_000, ItemID.COINS))
			.supply(ranged
				? ItemReq.supply("Ranging potions", 4, ItemID._4DOSERANGERSPOTION, ItemID._3DOSERANGERSPOTION,
					ItemID._2DOSERANGERSPOTION, ItemID._1DOSERANGERSPOTION)
				.bestFirst("Ranging potion (4)", "Ranging potion (3)", "Ranging potion (2)", "Ranging potion (1)")
				: ItemReq.supply("Super combat potions", 4, ItemID._4DOSE2COMBAT, ItemID._3DOSE2COMBAT,
					ItemID._2DOSE2COMBAT, ItemID._1DOSE2COMBAT)
				.bestFirst("Super combat potion (4)", "Super combat potion (3)", "Super combat potion (2)",
					"Super combat potion (1)"))
			.supply(ItemReq.supply("Dwarven rock cake (100 gp from Rohak under White Wolf Mountain, after Recipe for "
				+ "Disaster's Mountain Dwarf part) or a locator orb", 1, ItemID.HUNDRED_DWARF_COOL_ROCKCAKE,
				ItemID.HUNDRED_DWARF_HOT_ROCKCAKE, ItemID.DS2_ORB))
			.target(Target.npc(null, "Dominic Onion"))
			.target(Target.objectContaining(null, "coffer"))
			.target(Target.object("Drink", "Potion"))
			.target(Target.objectContaining(null, "rewards chest"))
			.target(Target.objectContaining("Take", "absorption", "overload"))
			.setupTitle("Set up your Nightmare Zone dream")
			.setupStep("Put coins in Dominic's coffer (the chest next to Dominic Onion): click it, choose Deposit and "
				+ "put in at least 22,000. A customisable normal rumble costs 22,000 each time")
			.setupStep("Have points from earlier dreams? Open the Rewards chest, go to the Benefits tab and buy "
				+ "Absorption (1,000 points a dose) and Overload (1,500 points a dose). First time here you have 0 "
				+ "points, so skip this and the next step")
			.setupStep("Take potions out of the barrels next to the chest: 4 overloads (from the overload barrel) and "
				+ "fill the rest of your inventory with absorptions, keeping 1 slot for your rock cake")
			.setupStep("Talk to Dominic Onion and choose Dream, then Rumble, then Customisable, then Normal. Say yes to "
				+ "the fee")
			.setupStep("Drink the potion on the stand in the middle of the room. A boss list opens: turn off every "
				+ "boss except {Rumble bosses}, then Accept. These bosses are easy to fight and don't have tricky "
				+ "attacks")
			.howTo("Run to the middle of the arena. Do NOT drink the potions on the north and south walls: they "
				+ "wake you up and end the dream")
			.howTo("Drink an overload first (you need 51+ hitpoints for it) and wait until it has finished hurting "
				+ "you. No overloads yet? Drink a " + potion + " instead")
			.howTo("Drink absorptions until the number on the left of your screen is 950 or more. Absorption soaks "
				+ "up the damage you'd take")
			.howTo("Eat (Guzzle) your rock cake until you have 1 hitpoint. Now every hit on you does only 1 "
				+ "damage, taken from the absorption number")
			.howTo(ranged
				? "Equip your bow and arrows, set Rapid in the combat tab and turn on auto-retaliate. The bosses come "
					+ "to you"
				: "Turn on auto-retaliate in the combat tab. The bosses come to you")
			.howTo("Every 5 minutes the overload wears off and heals you 50: drink another one, then rock cake back "
				+ "down to 1 hitpoint")
			.howTo("Your hitpoints slowly come back. Turning the Rapid Heal prayer on and off once a minute stops "
				+ "that, so you stay at 1")
			.howTo("Drink more absorptions when the number gets below 200. After 20 minutes your character stops "
				+ "fighting back on its own: click a boss to start again")
			.howTo("First time, with no absorptions: keep your hitpoints up with food and potions instead, and drink "
				+ "a wake potion on the north or south wall to leave before you run out. Your points show in the "
				+ "top left; spend them at the Rewards chest next time")
			.insideItem(ItemID.NZONE4DOSEOVERLOADPOTION).insideItem(ItemID.NZONE3DOSEOVERLOADPOTION)
			.insideItem(ItemID.NZONE2DOSEOVERLOADPOTION).insideItem(ItemID.NZONE1DOSEOVERLOADPOTION)
			.insideItem(ItemID.NZONE4DOSEABSORPTIONPOTION).insideItem(ItemID.NZONE3DOSEABSORPTIONPOTION)
			.insideItem(ItemID.NZONE2DOSEABSORPTIONPOTION).insideItem(ItemID.NZONE1DOSEABSORPTIONPOTION)
			.insideItem(ItemID.HUNDRED_DWARF_COOL_ROCKCAKE).insideItem(ItemID.HUNDRED_DWARF_HOT_ROCKCAKE)
			.insideItem(ItemID.DS2_ORB);
	}

	private static Method.MethodBuilder melee(Monster mon, Skill skill, String style)
	{
		return withRates(base(mon, skill, skill.getName().toLowerCase()), mon, 1.0)
			.minLevel(mon.meleeLevel)
			.xpPerAction(4.0 * mon.hitpoints)
			.xpPerHour(mon.meleeXpHr)
			.tag(MethodTag.CHEAP)
			.gearStyle(CombatStyle.MELEE)
			.instruction("Set your combat style to " + style + " (" + skill.getName() + " xp) and attack the "
				+ targetNoun(mon));
	}

	private static Method.MethodBuilder hitpoints(Monster mon)
	{
		return withRates(base(mon, Skill.HITPOINTS, "hitpoints"), mon, 1 / 3.0)
			// Hitpoints starts at 10, so the level-1 monster stays level 1 to keep the plan simple
			.minLevel(mon.meleeLevel)
			.xpPerAction(1.33 * mon.hitpoints)
			.xpPerHour(mon.meleeXpHr / 3)
			.tag(MethodTag.CHEAP)
			.gearStyle(CombatStyle.MELEE)
			.instruction("Attack the " + targetNoun(mon) + " with any combat style; Hitpoints xp comes with every hit");
	}

	private static Method.MethodBuilder ranged(Monster mon)
	{
		Method.MethodBuilder b = base(mon, Skill.RANGED, "ranged")
			.minLevel(mon.rangedLevel)
			.xpPerAction(4.0 * mon.hitpoints)
			.gearStyle(CombatStyle.RANGED)
			.wikiPage(RANGED_WIKI);
		if (mon.rangedName != null)
		{
			b.name(mon.rangedName);
		}
		if (mon.rangedHowTo != null)
		{
			b.clearHowTo();
			for (String step : mon.rangedHowTo)
			{
				b.howTo(step);
			}
		}
		if (mon.rangedRates != null)
		{
			b.xpPerHour(mon.rangedXpHr);
			for (int i = 0; i + 1 < mon.rangedRates.length; i += 2)
			{
				b.rateAt(mon.rangedRates[i], mon.rangedRates[i + 1]);
			}
		}
		else
		{
			withRates(b, mon, 1.1).xpPerHour((int) (mon.meleeXpHr * 1.1));
		}
		if (mon.chinning)
		{
			// The chinchompa is the weapon, so no bow or arrows; which kind depends on Ranged level and the bank
			return b.input(CombatGear.CHINCHOMPA_INPUT)
				.instruction("Wield your chinchompas, set the Long fuse style (Ranged and Defence xp) or Medium fuse "
					+ "(Ranged only), and throw them at the biggest group of " + targetNoun(mon));
		}
		// Roughly one arrow lost per kill once you pick arrows back up (or wear Ava's device). Which ammo depends
		// on the player's weapon and Ranged level, not the monster, so the guide picks it at runtime
		b.input(CombatGear.AMMO_INPUT);
		if (mon.cannon)
		{
			return b.tools(CANNON)
				.input(CANNONBALLS)
				.target(Target.object("Fire", "Dwarf multicannon"))
				.instruction("Set up your cannon among the " + targetNoun(mon) + ", load it with cannonballs, then "
					+ "equip your best ranged weapon and ammo, set Rapid and attack them too");
		}
		return b.instruction("Equip your best ranged weapon and ammo, set Rapid and attack the " + targetNoun(mon));
	}

	/**
	 * The monster's melee rates scaled for another skill: Hitpoints gets a third of the xp, Ranged a bit more.
	 */
	private static Method.MethodBuilder withRates(Method.MethodBuilder b, Monster mon, double scale)
	{
		for (int i = 0; i + 1 < mon.meleeRates.length; i += 2)
		{
			b.rateAt(mon.meleeRates[i], (int) (mon.meleeRates[i + 1] * scale));
		}
		return b;
	}

	private static String targetNoun(Monster mon)
	{
		return mon.npcs.length == 0 ? "monsters" : mon.npcs[0].toLowerCase() + "s";
	}

	private CombatGuide()
	{
	}
}
