package com.skillinghelper.diary.data;

import com.skillinghelper.data.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;

/**
 * Ardougne diary, every tier. Task wording and requirements from the OSRS Wiki, listed in walking order.
 */
final class ArdougneDiary
{
	/**
	 * Done bit per wiki task number, counted across both Ardougne diary varps (32 on is the second). The game skips a
	 * few bits and swaps two elite tasks, so the numbers aren't in a straight run.
	 */
	private static final Map<DiaryTier, int[]> BITS = Map.of(
		DiaryTier.EASY, new int[]{0, 1, 2, 4, 5, 6, 7, 9, 11, 12},
		DiaryTier.MEDIUM, new int[]{13, 14, 15, 16, 17, 18, 19, 20, 21, 23, 24, 25},
		DiaryTier.HARD, new int[]{26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37},
		DiaryTier.ELITE, new int[]{38, 39, 41, 40, 42, 43, 44, 45});

	private static final Location YANILLE_ANVIL = Location.of("Anvil in Yanille", 2613, 3081, 0, 2);
	private static final Location KHAZARD_TRAWLER = Location.of("Fishing Trawler, Port Khazard", 2675, 3170, 0, 3);
	private static final DiaryItem RAKE = DiaryItem.of("Rake", ItemID.RAKE);
	private static final DiaryItem DIBBER = DiaryItem.of("Seed dibber", ItemID.DIBBER);
	private static final DiaryItem SPADE = DiaryItem.of("Spade", ItemID.SPADE);

	private ArdougneDiary()
	{
	}

	static List<DiaryTask> tasks()
	{
		List<DiaryTask> out = new ArrayList<>();
		easy(out);
		medium(out);
		hard(out);
		elite(out);
		return out;
	}

	private static DiaryStage simple(String title, Location at, String npcOrNull, String objectOrNull, String... lines)
	{
		DiaryStage.DiaryStageBuilder b = DiaryStage.builder().title(title).at(at);
		if (npcOrNull != null)
		{
			b.npc(npcOrNull);
		}
		if (objectOrNull != null)
		{
			b.object(objectOrNull);
		}
		for (String line : lines)
		{
			b.line(line);
		}
		return b.build();
	}

	private static void easy(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.EASY;
		out.add(task(t, 10, "Probita's pets", "Check what pets you have Insured with Probita in Ardougne.")
			.stage(simple("Ask Probita about your insured pets", Location.of("Probita, East Ardougne", 2621, 3294, 0, 3),
				"Probita", null,
				"1. Probita's pet shop is in the west of East Ardougne, near the bridge.",
				"2. Right-click Probita (outlined) and choose Check."))
			.build());
		out.add(task(t, 4, "Church altar", "Use the altar in East Ardougne's church.")
			.note("Prayer points below full (turn on a prayer for a moment first)")
			.stage(simple("Pray at the East Ardougne church altar", Location.of("East Ardougne church", 2618, 3309, 0, 3),
				null, "Altar",
				"1. Use a little prayer first, so you're below full.",
				"2. Click Pray-at on the Altar (outlined) in the church north-west of the market."))
			.build());
		out.add(task(t, 3, "Sell silk", "Sell Silk to Silk trader in Ardougne for 60 coins each.")
			.item(DiaryItem.of("Silk", ItemID.SILK))
			.stage(simple("Haggle with the Silk merchant", Location.of("Silk merchant, Ardougne market", 2655, 3300, 0, 3),
				"Silk merchant", null,
				"1. Talk to the Silk merchant (outlined) in the market.",
				"2. Offer the silk for 120 coins, then when he refuses, ask for 60 coins each.",
				"He won't talk to you if you've just stolen from his stall."))
			.build());
		out.add(task(t, 2, "Steal a cake", "Steal a cake from the Ardougne market stalls.")
			.skill(Skill.THIEVING, 5)
			.stage(simple("Steal from the baker's stall", Location.of("Baker's stall, Ardougne market", 2668, 3311, 0, 2),
				null, "Baker's stall",
				"1. Right-click the Baker's stall (outlined) and choose Steal-from.",
				"2. Walk away a few tiles afterwards: the guards attack if they saw you."))
			.build());
		out.add(task(t, 1, "Cromperty's teleport", "Have Wizard Cromperty teleport you to the Rune Essence mine.")
			.quest(Quest.RUNE_MYSTERIES)
			.stage(simple("Ask Wizard Cromperty to teleport you", Location.of("Wizard Cromperty, north-east Ardougne", 2683, 3326, 0, 3),
				"Wizard Cromperty", null,
				"1. Cromperty's house is in north-east East Ardougne.",
				"2. Right-click Wizard Cromperty (outlined) and choose Teleport. Click a portal in the mine to leave."))
			.build());
		out.add(task(t, 8, "Wilderness lever", "Use the Ardougne Lever to teleport to the Wilderness.")
			.wilderness(true)
			.note("Leave valuables in the bank: you arrive in deep Wilderness")
			.stage(DiaryStage.builder()
				.title("Pull the Ardougne lever")
				.at(Location.of("Lever, north of Ardougne Castle", 2561, 3311, 0, 2))
				.object("Lever")
				.line("1. Bank anything valuable. Player killers wait by the other lever.")
				.line("2. Pull the Lever (outlined) north of the castle and say yes to the warning.")
				.line("3. Pull the lever there at once to come straight back.")
				.build())
			.build());
		out.add(task(t, 6, "Combat Training Camp", "Enter the Combat Training Camp north of W. Ardougne.")
			.quest(Quest.BIOHAZARD)
			.stage(simple("Walk into the Combat Training Camp", Location.of("Combat Training Camp gate", 2518, 3356, 0, 2),
				null, "Gate",
				"1. The camp is north of West Ardougne.",
				"2. Open the Gate (outlined) and walk in."))
			.build());
		out.add(task(t, 5, "Fishing Trawler", "Go out fishing on the Fishing Trawler.")
			.stage(simple("Board the Fishing Trawler", KHAZARD_TRAWLER, null, "Gangplank",
				"1. Cross the Gangplank (outlined) in Port Khazard.",
				"2. Once the boat sets off, the task is done. You can stay and fish or leave."))
			.build());
		out.add(task(t, 7, "Identify a rusty sword", "Have Tindel Marchant identify a Rusted Sword for you.")
			.item(DiaryItem.owned("Rusty sword", "See the OSRS Wiki's \"Rusty sword\" page for where to find one.",
				ItemID.DIGSITESWORD))
			.item(DiaryItem.of("Coins", 100, ItemID.COINS))
			.stage(simple("Ask Tindel to identify the sword", Location.of("Tindel Marchant, Port Khazard", 2676, 3152, 0, 3),
				"Tindel Marchant", null,
				"1. Talk to Tindel Marchant (outlined) in Port Khazard with the Rusty sword and 100 coins.",
				"There's a 1 in 100 chance it crumbles to nothing; then you need another."))
			.build());
		out.add(task(t, 9, "Aleck's Hunter Emporium", "View Aleck's Hunter Emporium in Yanille.")
			.stage(simple("Browse Aleck's shop", Location.of("Aleck's Hunter Emporium, Yanille", 2566, 3083, 0, 3),
				"Aleck", null,
				"1. Aleck's shop is in west Yanille.",
				"2. Right-click Aleck (outlined) and choose Trade."))
			.build());
	}

	private static void medium(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.MEDIUM;
		out.add(task(t, 4, "Ardougne Teleport", "Cast the Ardougne Teleport spell.")
			.quest(Quest.PLAGUE_CITY)
			.skill(Skill.MAGIC, 51)
			.item(DiaryItem.of("Law rune", 2, ItemID.LAWRUNE))
			.item(DiaryItem.of("Water rune", 2, ItemID.WATERRUNE))
			.stage(DiaryStage.builder()
				.title("Cast Ardougne Teleport")
				.line("1. Open the spellbook and click Ardougne Teleport.")
				.build())
			.build());
		out.add(task(t, 8, "Pickpocket a master farmer", "Pickpocket the master farmer north of Ardougne.")
			.skill(Skill.THIEVING, 38)
			.stage(simple("Pickpocket the Master Farmer", Location.of("Master Farmer, north of Ardougne", 2637, 3362, 0, 5),
				"Master Farmer", null,
				"1. The Master Farmer walks around the farming patches north of East Ardougne.",
				"2. Right-click him (outlined) and choose Pickpocket. One success is enough."))
			.build());
		out.add(task(t, 3, "Ardougne strawberries", "Harvest some strawberries from the Ardougne farming patch.")
			.skill(Skill.FARMING, 31)
			.item(DiaryItem.of("Strawberry seed", 3, ItemID.STRAWBERRY_SEED))
			.item(SPADE).item(DIBBER).item(RAKE)
			.stage(simple("Grow strawberries north of Ardougne", Location.of("Allotment, north of Ardougne", 2667, 3371, 0, 3),
				null, "Allotment",
				"1. Rake an Allotment (outlined) by the Ardougne farming patches.",
				"2. Use the 3 Strawberry seeds on it. They take about 1 hour 20 minutes.",
				"3. Pay the gardener a basket of apples to protect them if you like.",
				"4. When grown, click Harvest."))
			.build());
		out.add(task(t, 1, "Unicorn pen by fairy ring", "Enter the Unicorn pen in Ardougne zoo using Fairy rings.")
			.questStarted(Quest.FAIRYTALE_II__CURE_A_QUEEN)
			.item(DiaryStages.FAIRY_STAFF)
			.stages(DiaryStages.fairyRing("BIS", "You arrive in the Ardougne Zoo unicorn pen, which finishes it."))
			.build());
		out.add(task(t, 12, "Island east of the Necromancer Tower", "Visit the Island East of the Necromancer's Tower.")
			.questStarted(Quest.FAIRYTALE_II__CURE_A_QUEEN)
			.item(DiaryStages.FAIRY_STAFF)
			.stages(DiaryStages.fairyRing("AIR", "You land on the island, which finishes it."))
			.build());
		out.add(task(t, 10, "Kill a swordchick", "Kill a swordchick in the Tower of Life.")
			.quest(Quest.TOWER_OF_LIFE)
			.item(DiaryItem.of("Raw chicken", ItemID.RAW_CHICKEN))
			.item(DiaryItem.of("Raw swordfish", ItemID.RAW_SWORDFISH))
			.stage(simple("Go down into the Tower of Life basement", Location.of("Tower of Life", 2648, 3212, 0, 2),
				null, "Trapdoor",
				"1. The Tower of Life is south of East Ardougne. Climb-down the Trapdoor (outlined) inside."))
			.stage(DiaryStage.builder()
				.title("Make and kill a swordchick")
				.inside(Zone.of(3008, 4352, 3070, 4415, 0))
				.at(Location.of("Symbol of Life altar", 3034, 4362, 0, 3))
				.object("Symbol of life")
				.npc("Swordchick")
				.started(true)
				.line("1. Use the Raw chicken and Raw swordfish on the Symbol of Life (outlined), pressing the flat side.")
				.line("2. Kill the Swordchick (outlined) that appears.")
				.build())
			.build());
		out.add(task(t, 2, "Yanille wall grapple", "Grapple over Yanille's south wall.")
			.skill(Skill.AGILITY, 39).skill(Skill.STRENGTH, 38).skill(Skill.RANGED, 21)
			.item(DiaryStages.MITH_GRAPPLE)
			.item(DiaryStages.ANY_CROSSBOW)
			.stage(simple("Grapple Yanille's south wall", Location.of("South wall of Yanille", 2556, 3072, 0, 2),
				null, "Wall",
				"1. Wield the crossbow and Mith grapple.",
				"2. Stand outside Yanille's south wall and choose Grapple on the Wall (outlined)."))
			.stage(DiaryStage.builder()
				.title("Jump down from the wall")
				.inside(Zone.of(2539, 3070, 2581, 3077, 1))
				.started(true)
				.object("Wall").object("Battlement")
				.line("1. Click Jump on the wall's edge (outlined) to get down inside Yanille.")
				.build())
			.build());
		out.add(task(t, 6, "Bert's sand", "Claim buckets of sand from Bert in Yanille.")
			.quest(Quest.THE_HAND_IN_THE_SAND)
			.note("Free inventory space for the sand (it goes to your bank)")
			.stage(simple("Ask Bert for your sand", Location.of("Bert, Yanille", 2551, 3100, 0, 3), "Bert", null,
				"1. Talk to Bert (outlined) in north-west Yanille and ask for your free sand."))
			.build());
		out.add(task(t, 9, "Cave nightshade", "Collect some Nightshade from the Skavid Caves.")
			.questStarted(Quest.WATCHTOWER)
			.item(DiaryItem.owned("Skavid map", "Given during the Watchtower quest; ask the Watchtower Wizard for another.",
				ItemID.SKAVIDMAP))
			.item(DiaryStages.LIGHT_SOURCE)
			.stage(simple("Enter the northern skavid cave", Location.of("Skavid caves, north of Gu'Tanoth", 2524, 3069, 0, 2),
				null, "Cave entrance",
				"1. Light your light source.",
				"2. The caves are in the hills north of Gu'Tanoth, west of Yanille. Enter the northernmost one (outlined)."))
			.stage(DiaryStage.builder()
				.title("Pick up the cave nightshade")
				.inside(Zone.of(2527, 9460, 2536, 9470, 0))
				.line("1. Pick up the Cave nightshade on the cave floor. Leave and come back if it's gone.")
				.build())
			.build());
		out.add(task(t, 7, "Fishing Platform", "Catch any fish on the Fishing Platform.")
			.questStarted(Quest.SEA_SLUG)
			.item(DiaryItem.of("Small fishing net", ItemID.NET))
			.stage(simple("Sail to the Fishing Platform", Location.of("Holgart, Witchaven", 2719, 3305, 0, 3), "Holgart", null,
				"1. Talk to Holgart (outlined) on the Witchaven jetty and ask to go to the platform."))
			.stage(DiaryStage.builder()
				.title("Catch a fish on the platform")
				.inside(Zone.of(2760, 3271, 2795, 3293, 0))
				.npc("Fishing spot")
				.line("1. Click Net on a Fishing spot (outlined) and catch anything.")
				.build())
			.build());
		out.add(task(t, 11, "Iban's staff", "Equip Iban's upgraded staff or upgrade an Iban staff.")
			.quest(Quest.UNDERGROUND_PASS)
			.item(DiaryItem.owned("Iban's staff", "Get it at the end of Underground Pass, or buy a new one from "
				+ "the Dark Mage in West Ardougne.", ItemID.IBANSTAFF_UPGRADED, ItemID.IBAN_STAFF_UPGRADED_ORN,
				ItemID.IBANSTAFF, ItemID.IBAN_STAFF_ORN))
			.note("200,000 coins to upgrade it (not needed if it's already upgraded)")
			.stage(simple("Upgrade or equip Iban's staff", Location.of("Dark Mage, West Ardougne", 2455, 3312, 0, 4),
				"Dark Mage", null,
				"1. Already upgraded? Just wield it, anywhere.",
				"2. Otherwise talk to the Dark Mage (outlined) in north-west West Ardougne with the staff and 200,000 coins."))
			.build());
		out.add(task(t, 5, "Balloon to Castle Wars", "Travel to Castlewars by Hot Air Balloon.")
			.quest(Quest.ENLIGHTENED_JOURNEY)
			.skill(Skill.FIREMAKING, 50)
			.note("Castle Wars balloon route unlocked (else 10 more yew logs the first time)")
			.item(DiaryItem.of("Yew logs", ItemID.YEW_LOGS))
			.stage(DiaryStage.builder()
				.title("Fly to Castle Wars by balloon")
				.object("Basket")
				.line("1. Go to any hot air balloon (Entrana, Taverley, Varrock, Crafting Guild, Grand Tree).")
				.line("2. Click Use on the Basket (outlined), then pick Castle Wars on the map.")
				.build())
			.build());
	}

	private static void hard(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.HARD;
		out.add(task(t, 5, "Watchtower Teleport", "Teleport to the Watchtower.")
			.quest(Quest.WATCHTOWER)
			.skill(Skill.MAGIC, 58)
			.item(DiaryItem.of("Law rune", 2, ItemID.LAWRUNE))
			.item(DiaryItem.of("Earth rune", 2, ItemID.EARTHRUNE))
			.stage(DiaryStage.builder()
				.title("Cast Watchtower Teleport")
				.line("1. Open the spellbook and click Watchtower Teleport.")
				.build())
			.build());
		out.add(task(t, 3, "Ardougne Castle chest", "Steal from a chest in Ardougne Castle.")
			.skill(Skill.THIEVING, 72)
			.note("A lockpick helps")
			.stage(simple("Go upstairs in Ardougne Castle", Location.of("Ardougne Castle stairs", 2572, 3296, 0, 2), null,
				"Staircase", "1. Walk into Ardougne Castle and Climb-up the Staircase (outlined)."))
			.stage(DiaryStage.builder()
				.title("Steal from the chest")
				.inside(Zone.of(2568, 3282, 2591, 3310, 1))
				.at(Location.of("Chest, Ardougne Castle first floor", 2588, 3291, 1, 2))
				.object("Chest")
				.line("1. Right-click the Chest (outlined) and choose Search for traps.")
				.build())
			.build());
		out.add(task(t, 4, "Monkey cage", "Have a zookeeper put you in Ardougne Zoo's monkey cage.")
			.questStarted(Quest.MONKEY_MADNESS_I)
			.item(DiaryItem.owned("Karamjan monkey greegree", "Made during Monkey Madness I; Zooknock on Ape Atoll makes "
				+ "another if you bring a monkey talisman and a karamjan monkey bone.", ItemID.MM_MONKEY_GREEGREE_FOR_NORMAL_MONKEY))
			.stage(simple("Get put in the monkey cage", Location.of("Monkey cage, Ardougne Zoo", 2607, 3277, 0, 3),
				"Monkey minder", null,
				"1. Wield the Karamjan monkey greegree to turn into a monkey.",
				"2. Talk to the Monkey minder (outlined) by the zoo's monkey cage."))
			.build());
		out.add(task(t, 10, "House in Yanille", "Enter your POH from Yanille.")
			.skill(Skill.CONSTRUCTION, 50).unboostable(Skill.CONSTRUCTION)
			.item(DiaryItem.of("Coins", 25_000, ItemID.COINS))
			.stage(simple("Move your house to Yanille", Location.of("Estate agent, East Ardougne", 2638, 3293, 0, 3),
				"Estate agent", null,
				"1. Talk to the Estate agent (outlined) in East Ardougne and move your house to Yanille (25,000 coins).",
				"Already in Yanille? Skip this and go to the portal."))
			.stage(DiaryStage.builder()
				.title("Enter your house from Yanille")
				.at(Location.of("House portal, Yanille", 2544, 3098, 0, 2))
				.object("Portal")
				.line("1. Click Home on the Portal (outlined) in west Yanille.")
				.build())
			.build());
		out.add(task(t, 9, "Mithril platebody", "Smith a Mithril platebody near Ardougne.")
			.skill(Skill.SMITHING, 68)
			.item(DiaryItem.of("Mithril bar", 5, ItemID.MITHRIL_BAR))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.stage(simple("Smith a mithril platebody in Yanille", YANILLE_ANVIL, null, "Anvil",
				"1. Use the Mithril bars on the Anvil (outlined) in Yanille and pick Platebody."))
			.build());
		out.add(task(t, 2, "Magic Guild", "Enter the Magic Guild.")
			.skill(Skill.MAGIC, 66)
			.stage(simple("Walk into the Magic Guild", Location.of("Magic Guild, Yanille", 2584, 3088, 0, 2), null, "Door",
				"1. Open the guild Door (outlined) in Yanille and walk in."))
			.build());
		out.add(task(t, 6, "Red salamander", "Catch a Red Salamander.")
			.skill(Skill.HUNTER, 59)
			.item(DiaryItem.of("Rope", ItemID.ROPE))
			.item(DiaryItem.of("Small fishing net", ItemID.NET))
			.stage(simple("Catch a red salamander", Location.of("Red salamanders, south of Ourania Altar", 2474, 3239, 0, 4),
				"Red salamander", "Young tree",
				"1. Set a net trap on a Young tree (outlined) south of the Ourania altar, by the river.",
				"2. Wait for a Red salamander to get caught, then check the trap."))
			.build());
		out.add(task(t, 7, "Gnome village palm", "Check the health of a Palm tree near tree gnome village.")
			.skill(Skill.FARMING, 68)
			.item(DiaryItem.of("Palm sapling", ItemID.PLANTPOT_PALM_SAPLING))
			.item(SPADE).item(RAKE)
			.item(DiaryItem.of("Papaya fruit (to pay the gardener)", 15, ItemID.PAPAYA))
			.stage(simple("Grow a palm tree by Tree Gnome Village", Location.of("Fruit tree patch, Tree Gnome Village", 2490, 3180, 0, 3),
				"Gileth", "Fruit Tree Patch",
				"1. Rake the patch next to Gileth (outlined) outside Tree Gnome Village, then plant the Palm sapling.",
				"2. Pay Gileth the papayas to protect it.",
				"3. Come back when it's grown (about 16 hours) and click Check-health."))
			.build());
		out.add(task(t, 8, "Poison ivy berries", "Pick some Poison Ivy berries from the patch south of Ardougne.")
			.skill(Skill.FARMING, 70)
			.item(DiaryItem.of("Poison ivy seed", ItemID.POISONIVY_BUSH_SEED))
			.item(DIBBER).item(RAKE)
			.stage(simple("Grow poison ivy by the Ardougne Monastery", Location.of("Bush patch, Ardougne Monastery", 2618, 3226, 0, 3),
				null, "Bush Patch",
				"1. Rake the Bush patch (outlined) next to the monastery south of Ardougne.",
				"2. Plant the Poison ivy seed. It can't get diseased.",
				"3. Come back in about 5 hours and click Pick-from on the bush."))
			.build());
		out.add(task(t, 11, "Dragon sq shield", "Smith a Dragon sq shield in West Ardougne.")
			.questStarted(Quest.PLAGUE_CITY)
			.skill(Skill.SMITHING, 60)
			.item(DiaryItem.of("Shield left half", ItemID.DRAGONSHIELD_A))
			.item(DiaryItem.of("Shield right half", ItemID.DRAGONSHIELD_B))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.stage(simple("Smith the shield halves in West Ardougne", Location.of("Anvil, West Ardougne", 2501, 3330, 0, 2),
				null, "Anvil",
				"1. The anvil is in north-west West Ardougne.",
				"2. Use a shield half on the Anvil (outlined)."))
			.build());
		out.add(task(t, 1, "Legends' totem", "Recharge some Jewellery at the Totem in the Legends Guild.")
			.quest(Quest.LEGENDS_QUEST)
			.item(DiaryItem.owned("Skills necklace or combat bracelet (not full)", "Buy one on the Grand Exchange "
					+ "and use a charge, or use one you've emptied.", ItemID.JEWL_NECKLACE_OF_SKILLS_5,
				ItemID.JEWL_NECKLACE_OF_SKILLS_4, ItemID.JEWL_NECKLACE_OF_SKILLS_3, ItemID.JEWL_NECKLACE_OF_SKILLS_2,
				ItemID.JEWL_NECKLACE_OF_SKILLS_1, ItemID.JEWL_NECKLACE_OF_SKILLS, ItemID.JEWL_BRACELET_OF_COMBAT_5,
				ItemID.JEWL_BRACELET_OF_COMBAT_4, ItemID.JEWL_BRACELET_OF_COMBAT_3, ItemID.JEWL_BRACELET_OF_COMBAT_2,
				ItemID.JEWL_BRACELET_OF_COMBAT_1, ItemID.JEWL_BRACELET_OF_COMBAT))
			.stage(simple("Recharge it at the totem pole", Location.of("Totem pole, Legends' Guild", 2729, 3378, 0, 3),
				null, "Totem pole",
				"1. Use the necklace or bracelet on the Totem pole (outlined) outside the Legends' Guild."))
			.build());
		out.add(task(t, 12, "Craft death runes", "Craft some Death runes from Essence.")
			.quest(Quest.MOURNINGS_END_PART_I)
			.skill(Skill.RUNECRAFT, 65)
			.item(DiaryItem.anyOf("Pure essence", 1, ItemID.BLANKRUNE_HIGH, DiaryStages.PURE_ESSENCE))
			.stage(DiaryStage.builder()
				.title("Get to the death altar")
				.line("1. The easiest way is the Abyss: enter it from the Wilderness ditch north of Edgeville via the "
					+ "Mage of Zamorak, then take the Death rift.")
				.line("2. Or go through the Temple of Light under the Mourner headquarters with a death talisman.")
				.build())
			.stage(DiaryStage.builder()
				.title("Craft death runes")
				.inside(Zone.of(2183, 4815, 2232, 4860, 0))
				.object("Altar")
				.line("1. Click Craft-rune on the Altar (outlined).")
				.build())
			.build());
	}

	private static void elite(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.ELITE;
		out.add(task(t, 3, "Pickpocket a Hero", "Pickpocket a Hero.")
			.skill(Skill.THIEVING, 80)
			.stage(simple("Pickpocket a Hero", Location.of("Heroes, Ardougne market", 2630, 3292, 0, 5), "Hero", null,
				"1. Heroes walk around the south-west of the Ardougne market.",
				"2. Right-click one (outlined) and choose Pickpocket. Bring food: failures stun and hit."))
			.build());
		out.add(task(t, 7, "Ardougne rooftops", "Complete a lap of Ardougne's rooftop agility course.")
			.skill(Skill.AGILITY, 90)
			.stage(DiaryStage.builder()
				.title("Run a lap of the Ardougne rooftops")
				.at(Location.of("Ardougne rooftop course start", 2673, 3298, 0, 2))
				.object("Wooden Beams").object("Gap").object("Plank").object("Steep roof")
				.line("1. Climb the Wooden Beams (outlined) south-east of the market.")
				.line("2. Cross each outlined obstacle until you jump off the last gap.")
				.build())
			.build());
		out.add(task(t, 6, "Ardougne torstol", "Pick some Torstol from the patch north of Ardougne.")
			.skill(Skill.FARMING, 85)
			.item(DiaryItem.of("Torstol seed", ItemID.TORSTOL_SEED))
			.item(DIBBER).item(RAKE)
			.stage(simple("Grow torstol north of Ardougne", Location.of("Herb patch, north of Ardougne", 2670, 3374, 0, 3),
				null, "Herb patch",
				"1. Rake the Herb patch (outlined) and plant the Torstol seed. Ultracompost helps it survive.",
				"2. Come back in about 80 minutes and click Pick."))
			.build());
		out.add(task(t, 2, "Yanille dungeon basement", "Picklock the door to the basement of Yanille Agility Dungeon.")
			.skill(Skill.THIEVING, 82)
			.item(DiaryItem.of("Lockpick", ItemID.LOCKPICK))
			.stage(simple("Go down into the Yanille agility dungeon", Location.of("Yanille dungeon stairs", 2604, 3079, 0, 2),
				null, "Staircase",
				"1. Climb-down the Staircase (outlined) in east Yanille."))
			.stage(DiaryStage.builder()
				.title("Picklock the basement door")
				.inside(Zone.of(2560, 9472, 2621, 9535, 0))
				.at(Location.of("Locked door, Yanille dungeon", 2601, 9482, 0, 2))
				.object("Door")
				.line("1. Walk south-east to the locked Door (outlined) and click Pick-lock.")
				.build())
			.build());
		out.add(task(t, 4, "Rune crossbow from scratch", "Make a rune crossbow yourself from scratch within Witchaven or Yanille.")
			.skill(Skill.CRAFTING, 10).skill(Skill.SMITHING, 91).skill(Skill.FLETCHING, 69)
			.item(DiaryItem.of("Sinew", ItemID.XBOWS_SINEW))
			.item(DiaryItem.of("Runite bar", ItemID.RUNITE_BAR))
			.item(DiaryItem.of("Yew logs", ItemID.YEW_LOGS))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.item(DiaryItem.of("Knife", ItemID.KNIFE))
			.stage(simple("Spin the crossbow string in Witchaven", Location.of("Spinning wheel, Witchaven", 2731, 3278, 0, 2),
				null, "Spinning wheel",
				"1. Bank first and bring only these items: doing another diary task midway resets this one.",
				"2. Use the Sinew on the Spinning wheel (outlined) in Witchaven to make a crossbow string."))
			.stage(DiaryStage.builder()
				.title("Smith runite limbs in Yanille")
				.carrying(ItemID.XBOWS_CROSSBOW_STRING)
				.started(true)
				.at(YANILLE_ANVIL)
				.object("Anvil")
				.use(ItemID.RUNITE_BAR)
				.line("1. Use the Runite bar on the Anvil (outlined) in Yanille and pick Limbs.")
				.build())
			.stage(DiaryStage.builder()
				.title("Fletch and put the crossbow together")
				.carrying(ItemID.XBOWS_CROSSBOW_LIMBS_RUNITE)
				.started(true)
				.use(ItemID.KNIFE).use(ItemID.YEW_LOGS).use(ItemID.XBOWS_CROSSBOW_LIMBS_RUNITE)
				.line("1. Still in Yanille: use the Knife on the Yew logs and make a Yew stock.")
				.line("2. Use the Runite limbs on the stock.")
				.line("3. Use the Crossbow string on the unstrung crossbow.")
				.build())
			.build());
		out.add(task(t, 5, "Imbue a salve amulet", "Imbue a Salve amulet at Nightmare Zone, or equip a Salve amulet that was imbued there.")
			.note("800,000 Nightmare Zone points, unless it's already imbued there")
			.item(DiaryItem.owned("Salve amulet", "Obtained through the Haunted Mine quest; the wiki's \"Salve amulet\" page shows how.",
				ItemID.NZONE_SALVE_AMULET_E, ItemID.NZONE_SALVE_AMULET, ItemID.LOTR_CRYSTALSHARD_NECKLACE_UPGRADE,
				ItemID.CRYSTALSHARD_NECKLACE))
			.stage(simple("Imbue the salve amulet at Nightmare Zone", Location.of("Nightmare Zone, Yanille", 2608, 3116, 0, 3),
				"Dominic Onion", "Rewards Chest",
				"1. Already imbued at Nightmare Zone? Just wear it.",
				"2. Otherwise open the Rewards chest (outlined), go to Benefits and imbue the Salve amulet."))
			.build());
		out.add(task(t, 1, "Trawler manta ray", "Catch a Manta ray in the Fishing Trawler and cook it in Port Khazard.")
			.skill(Skill.FISHING, 81).unboostable(Skill.FISHING)
			.skill(Skill.COOKING, 91)
			.stage(simple("Catch a manta ray on the Fishing Trawler", KHAZARD_TRAWLER, null, "Gangplank",
				"1. Board the Fishing Trawler (outlined gangplank) and play the minigame.",
				"2. Collect the reward net at the end until you get a Raw manta ray.",
				"Doing any other diary task before you cook it resets this one."))
			.stage(DiaryStage.builder()
				.title("Cook the manta ray in Port Khazard")
				.carrying(ItemID.RAW_MANTARAY)
				.at(Location.of("Range, Port Khazard", 2659, 3160, 0, 3))
				.object("Range")
				.use(ItemID.RAW_MANTARAY)
				.line("1. Use the Raw manta ray on the Range (outlined) in Port Khazard.")
				.build())
			.build());
		out.add(task(t, 8, "Ice Barrage in Castle Wars", "Cast Ice Barrage on another player within Castle Wars.")
			.quest(Quest.DESERT_TREASURE_I)
			.skill(Skill.MAGIC, 94)
			.note("Ancient Magicks spellbook selected")
			.item(DiaryItem.of("Water rune", 6, ItemID.WATERRUNE))
			.item(DiaryItem.of("Blood rune", 2, ItemID.BLOODRUNE))
			.item(DiaryItem.of("Death rune", 4, ItemID.DEATHRUNE))
			.stage(simple("Ice Barrage a player in a Castle Wars game", Location.of("Castle Wars", 2440, 3089, 0, 5),
				null, "Guthix Portal",
				"1. Join a Castle Wars game (the Guthix portal, outlined, picks a team for you).",
				"2. Cast Ice Barrage on a player from the other team until one hits."))
			.build());
	}

	private static DiaryTask.DiaryTaskBuilder task(DiaryTier tier, int number, String name, String text)
	{
		int index = BITS.get(tier)[number - 1];
		return DiaryTask.builder()
			.region(DiaryRegion.ARDOUGNE)
			.tier(tier)
			.number(number)
			.name(name)
			.text(text)
			.varp(index < 32 ? VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY : VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2)
			.bit(index % 32);
	}
}
