package com.skillinghelper.diary.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.ToolType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;

/**
 * Morytania diary, every tier. Task wording and requirements from the OSRS Wiki, listed in walking order.
 */
final class MorytaniaDiary
{
	/**
	 * Done bit per wiki task number across both Morytania diary varps (32 on is the second).
	 */
	private static final Map<DiaryTier, int[]> BITS = Map.of(
		DiaryTier.EASY, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
		DiaryTier.MEDIUM, new int[]{12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22},
		DiaryTier.HARD, new int[]{23, 24, 25, 26, 27, 28, 29, 30, 33, 34},
		DiaryTier.ELITE, new int[]{35, 36, 37, 38, 39, 40});

	private static final Location CANIFIS = Location.of("Canifis", 3494, 3484, 0, 6);
	private static final Location SLAYER_TOWER = Location.of("Slayer Tower entrance", 3428, 3535, 0, 3);
	private static final Zone SLAYER_TOWER_GROUND = Zone.of(3404, 3530, 3453, 3580, 0);
	private static final Zone SLAYER_TOWER_1 = Zone.of(3401, 3529, 3456, 3581, 1);
	private static final Zone SLAYER_TOWER_2 = Zone.of(3404, 3530, 3453, 3580, 2);
	private static final Location PORT_PHASMATYS = Location.of("Port Phasmatys", 3687, 3479, 0, 6);
	private static final Location ECTOFUNTUS = Location.of("Ectofuntus, north of Port Phasmatys", 3660, 3520, 0, 3);
	private static final Location GROTTO = Location.of("Nature Grotto, Mort Myre Swamp", 3440, 3337, 0, 2);
	private static final Zone GROTTO_ZONE = Zone.anyFloor(3430, 9728, 3453, 9750);
	private static final DiaryItem RAKE = DiaryItem.of("Rake", ItemID.RAKE);
	private static final DiaryItem DIBBER = DiaryItem.of("Seed dibber", ItemID.DIBBER);
	private static final DiaryItem WITCHWOOD = DiaryItem.owned("Witchwood icon", "Buy it from any Slayer master's shop.", ItemID.WITCHWOOD_ICON);

	private MorytaniaDiary()
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

	private static DiaryStage grottoDown()
	{
		return simple("Go into the Nature Grotto", GROTTO, null, "Grotto",
			"1. The grotto is in the middle of Mort Myre Swamp. A fairy ring to BIP or the Salve Graveyard teleport is closest.",
			"2. Jump the bridge and click Enter on the Grotto (outlined). Ghasts in the swamp rot food, so bring a druid pouch.");
	}

	private static DiaryStage slayerTowerUp(String why)
	{
		return simple("Go up the Slayer Tower", SLAYER_TOWER, null, "Staircase",
			"1. The Slayer Tower is north of Canifis. Go in and Climb-up the Staircase (outlined) " + why + ".");
	}

	private static void easy(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.EASY;
		out.add(task(t, 6, "Enter Mort Myre Swamp", "Enter Mort Myre Swamp.")
			.stage(simple("Walk through the swamp gate", Location.of("Mort Myre gate, south of Canifis", 3443, 3458, 0, 2), null,
				"Gate", "1. Open the Gate (outlined) south-west of Canifis and walk into the swamp."))
			.build());
		out.add(task(t, 7, "Kill a ghoul", "Kill a Ghoul.")
			.stage(simple("Kill a ghoul", Location.of("Ghouls west of Canifis", 3434, 3461, 0, 6), "Ghoul", null,
				"1. Ghouls wander by the Mort Myre fence west of Canifis. Kill one (outlined)."))
			.build());
		out.add(task(t, 3, "Task from Mazchna", "Get a slayer task from the Slayer Master in Canifis.")
			.combat(20)
			.stage(simple("Get a task from Mazchna", Location.of("Mazchna, Canifis", 3513, 3510, 0, 3), "Mazchna", null,
				"1. Talk to Mazchna (outlined) in north Canifis and ask for an assignment."))
			.build());
		out.add(task(t, 5, "Sbott's tanning", "Have Sbott in Canifis tan something for you.")
			.item(DiaryItem.of("Cowhide", ItemID.COW_HIDE))
			.item(DiaryItem.of("Coins", 20, ItemID.COINS))
			.stage(simple("Have Sbott tan a hide", Location.of("Sbott, Canifis", 3490, 3501, 0, 3), "Sbott", null,
				"1. Right-click Sbott (outlined) and choose Trade, then tan the cowhide."))
			.build());
		out.add(task(t, 10, "Werewolf with Wolfbane", "Kill a werewolf in its human form using the Wolfbane Dagger.")
			.item(DiaryItem.owned("Wolfbane", "Reward from Priest in Peril; Drezel in Paterdomus gives a new one.",
				ItemID.DAGGER_WOLFBANE))
			.stage(simple("Kill a Canifis citizen with Wolfbane", CANIFIS, "Boris", null,
				"1. Wield Wolfbane. Canifis citizens are werewolves in human form.",
				"2. Attack any citizen (Boris, Eduard, Yuri...) and kill them with the dagger."))
			.build());
		out.add(task(t, 4, "Kill a banshee", "Kill a Banshee in the Slayer Tower.")
			.skill(Skill.SLAYER, 15)
			.note("Earmuffs (or a slayer helmet) against the screams")
			.stage(simple("Kill a banshee", SLAYER_TOWER, "Banshee", null,
				"1. Wear earmuffs and walk into the Slayer Tower.",
				"2. Kill a Banshee (outlined) on the ground floor."))
			.build());
		out.add(task(t, 1, "Snelm from scratch", "Craft any Snelm from scratch in Morytania.")
			.skill(Skill.CRAFTING, 15)
			.item(DiaryItem.of("Chisel", ItemID.CHISEL))
			.item(DiaryItem.owned("Blamish snail shell", "Kill snails in Mort Myre Swamp until one drops.",
				ItemID.SHELLROUND_SWAMP, ItemID.SHELLROUND_RED_BLACK, ItemID.SHELLROUND_YELLOW, ItemID.SHELLROUND_BLUE,
				ItemID.SHELLROUND_ORANGE, ItemID.SHELLPOINT_SWAMP, ItemID.SHELLPOINT_RED_BLACK, ItemID.SHELLPOINT_YELLOW,
				ItemID.SHELLPOINT_BLUE))
			.stage(DiaryStage.builder()
				.title("Chisel the shell into a snelm outside the swamp")
				.at(CANIFIS)
				.use(ItemID.CHISEL)
				.line("1. Stand in Morytania but outside Mort Myre Swamp (Canifis is fine).")
				.line("2. Use the Chisel on the snail shell.")
				.build())
			.build());
		out.add(task(t, 9, "Bonemeal at the Ectofuntus", "Offer some bonemeal at the Ectofuntus.")
			.item(DiaryItem.of("Pot", ItemID.POT_EMPTY))
			.item(DiaryItem.of("Bucket", ItemID.BUCKET_EMPTY))
			.item(DiaryItem.of("Bones", ItemID.BONES))
			.stage(simple("Grind the bones upstairs", ECTOFUNTUS, null, "Staircase",
				"1. Climb the Staircase (outlined) in the Ectofuntus temple to the grinder.",
				"2. Use the Bones on the Loader, Wind the grinder, then Empty the bin with the Pot."))
			.stage(DiaryStage.builder()
				.title("Fill the bucket with slime")
				.carrying(ItemID.POT_BONEMEAL)
				.started(true)
				.at(Location.of("Ectofuntus slime trapdoor", 3653, 3519, 0, 2))
				.object("Trapdoor").object("Pool of Slime")
				.line("1. Go down the Trapdoor (outlined) and use the Bucket on the Pool of Slime at the bottom.")
				.build())
			.stage(DiaryStage.builder()
				.title("Worship the Ectofuntus")
				.carrying(ItemID.BUCKET_ECTOPLASM)
				.started(true)
				.at(ECTOFUNTUS)
				.object("Ectofuntus")
				.line("1. Click Worship on the Ectofuntus (outlined) with the bonemeal and slime in your inventory.")
				.build())
			.build());
		out.add(task(t, 2, "Thin snail at Port Phasmatys", "Cook a thin Snail on the Port Phasmatys range.")
			.skill(Skill.COOKING, 12)
			.note("Ghosts Ahoy done or 2 ecto-tokens to get into Port Phasmatys")
			.item(DiaryItem.of("Thin snail", ItemID.SNAIL_CORPSE1))
			.stage(simple("Cook the snail in Port Phasmatys", Location.of("Range, Port Phasmatys", 3676, 3468, 0, 2), null, "Range",
				"1. Wear a Ghostspeak amulet if you have one. Pay the ghost at the gate, or walk in after Ghosts Ahoy.",
				"2. Use the Thin snail on the Range (outlined) in the south of the town."))
			.build());
		out.add(task(t, 8, "Morytania scarecrow", "Place a Scarecrow in the Morytania flower patch.")
			.skill(Skill.FARMING, 23)
			.items(DiaryStages.SCARECROW_ITEMS)
			.stages(DiaryStages.scarecrow(Location.of("Flower patch west of Port Phasmatys", 3602, 3526, 0, 3)))
			.build());
		out.add(task(t, 11, "Nature altar prayer", "Restore your prayer points at the nature altar.")
			.quest(Quest.NATURE_SPIRIT)
			.note("Prayer points below full")
			.stage(grottoDown())
			.stage(DiaryStage.builder()
				.title("Pray at the nature altar")
				.inside(GROTTO_ZONE)
				.at(Location.of("Altar of nature, Nature Grotto", 3442, 9741, 1, 2))
				.object("Altar of nature")
				.line("1. Use a little prayer first, then click Pray-at on the Altar of nature (outlined).")
				.build())
			.build());
	}

	private static void medium(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.MEDIUM;
		out.add(task(t, 2, "Canifis rooftops", "Complete a lap of the Canifis agility course.")
			.skill(Skill.AGILITY, 40)
			.stage(simple("Run a lap of the Canifis rooftops", Location.of("Canifis rooftop course start", 3507, 3489, 0, 2), null,
				"Tall tree", "1. Climb the Tall tree (outlined) next to Canifis bank, then cross each obstacle to the end."))
			.build());
		out.add(task(t, 1, "Swamp lizard", "Catch a swamp lizard.")
			.skill(Skill.HUNTER, 29)
			.item(DiaryItem.of("Rope", ItemID.ROPE))
			.item(DiaryItem.of("Small fishing net", ItemID.NET))
			.stage(simple("Catch a swamp lizard", Location.of("Swamp lizards, east of the Slepe road", 3532, 3447, 0, 5),
				"Swamp lizard", "Young tree",
				"1. Set a net trap on a Young tree (outlined) in the swamp south-east of Canifis.",
				"2. Wait for a Swamp lizard to get caught, then check the trap."))
			.build());
		out.add(task(t, 3, "Hollow tree bark", "Obtain some Bark from a Hollow tree.")
			.skill(Skill.WOODCUTTING, 45)
			.item(DiaryItem.tool(ToolType.AXE))
			.stage(simple("Chop a hollow tree", Location.of("Hollow trees north of Port Phasmatys road", 3663, 3451, 0, 3), null,
				"Hollow tree", "1. Chop the Hollow tree (outlined) until you get Bark."))
			.build());
		out.add(task(t, 10, "Ectophial teleport", "Use an ectophial to return to Port Phasmatys.")
			.quest(Quest.GHOSTS_AHOY)
			.item(DiaryItem.owned("Ectophial", "Reward from Ghosts Ahoy; ask the Ghost disciple at the Ectofuntus for another.",
				ItemID.ECTOPHIAL))
			.stage(DiaryStage.builder()
				.title("Empty the ectophial")
				.use(ItemID.ECTOPHIAL)
				.line("1. Click Empty on the Ectophial (outlined). Refill it at the Ectofuntus afterwards.")
				.build())
			.build());
		out.add(task(t, 8, "Cannonballs at Port Phasmatys", "Make a batch of cannonballs at the Port Phasmatys furnace.")
			.quest(Quest.DWARF_CANNON)
			.skill(Skill.SMITHING, 35)
			.item(DiaryItem.of("Steel bar", ItemID.STEEL_BAR))
			.item(DiaryItem.owned("Ammo mould", "From Dwarf Cannon; Nulodion by the Dwarven Mine gives another.",
				ItemID.DOUBLE_AMMO_MOULD, ItemID.AMMO_MOULD))
			.stage(simple("Make cannonballs in Port Phasmatys", Location.of("Furnace, Port Phasmatys", 3689, 3479, 0, 2), null,
				"Furnace", "1. Use the Steel bar on the Furnace (outlined) in Port Phasmatys."))
			.build());
		out.add(task(t, 4, "Dragontooth Isle", "Travel to Dragontooth Isle.")
			.item(DiaryItem.owned("Ghostspeak amulet", "From the Restless Ghost; Father Urhney in Lumbridge Swamp gives another.",
				ItemID.AMULET_OF_GHOSTSPEAK_ENCHANTED, ItemID.AMULET_OF_GHOSTSPEAK))
			.item(DiaryItem.owned("Ecto-tokens", "Worship at the Ectofuntus and collect them from the Ghost disciple.",
				ItemID.ECTOTOKEN))
			.stage(simple("Sail to Dragontooth Isle", Location.of("Ghost captain, Port Phasmatys", 3703, 3487, 0, 3),
				"Ghost captain", null,
				"1. Wear the Ghostspeak amulet.",
				"2. Talk to the Ghost captain (outlined) on the east dock and pay 25 ecto-tokens."))
			.build());
		out.add(task(t, 7, "Swamp boat at the Hollows", "Board the Swampy boat at the Hollows.")
			.questStarted(Quest.NATURE_SPIRIT)
			.stage(simple("Board the swamp boat", Location.of("Swamp boat, the Hollows", 3499, 3378, 0, 2), null, "Swamp boaty",
				"1. The Hollows are south of the Canifis-Mort'ton road (fairy ring BKR is close).",
				"2. Click Board on the Swamp boaty (outlined)."))
			.build());
		out.add(task(t, 11, "Guthix balance in Morytania", "Mix a Guthix Balance potion while in Morytania.")
			.questStarted(Quest.IN_AID_OF_THE_MYREQUE)
			.skill(Skill.HERBLORE, 22)
			.item(DiaryItem.of("Restore potion(3)", ItemID._3DOSESTATRESTORE))
			.item(DiaryItem.of("Garlic", ItemID.GARLIC))
			.item(DiaryItem.of("Silver dust", ItemID.SILVER_DUST))
			.stage(DiaryStage.builder()
				.title("Mix a Guthix balance in Morytania")
				.at(CANIFIS)
				.use(ItemID.GARLIC).use(ItemID._3DOSESTATRESTORE)
				.line("1. Anywhere in Morytania, use the Garlic on the Restore potion, then add the Silver dust.")
				.build())
			.build());
		out.add(task(t, 5, "Terror dog", "Kill a Terror Dog.")
			.quest(Quest.LAIR_OF_TARN_RAZORLOR)
			.skill(Skill.SLAYER, 40)
			.note("A slayer ring (Tarn's Lair teleport) saves the trap rooms")
			.stage(DiaryStage.builder()
				.title("Kill a terror dog in Tarn's Lair")
				.npc("Terror dog")
				.line("1. Teleport to Tarn's Lair with a slayer ring, or walk through the lair from the Haunted Woods.")
				.line("2. Kill a Terror dog (outlined) in Tarn's old room.")
				.build())
			.build());
		out.add(task(t, 9, "Fever spider", "Kill a Fever Spider on Braindeath Island.")
			.questStarted(Quest.RUM_DEAL)
			.skill(Skill.SLAYER, 42)
			.note("Slayer gloves stop the fever disease")
			.stage(simple("Sail to Braindeath Island", Location.of("Pirate Pete, north of Port Phasmatys", 3681, 3536, 0, 3),
				"Pirate Pete", null, "1. Talk to Pirate Pete (outlined) and sail to Braindeath Island."))
			.stage(DiaryStage.builder()
				.title("Kill a fever spider")
				.inside(Zone.anyFloor(2111, 5040, 2177, 5184))
				.npc("Fever spider")
				.line("1. Climb down to the brewery basement and kill a Fever spider (outlined).")
				.build())
			.build());
		out.add(task(t, 6, "Trouble Brewing", "Complete a game of trouble brewing.")
			.quest(Quest.CABIN_FEVER)
			.skill(Skill.COOKING, 40)
			.note("Empty inventory and head slot, and another player to play against")
			.stage(simple("Play a game of Trouble Brewing", Location.of("Honest Jimmy, Mos Le'Harmless", 3811, 3021, 0, 3),
				"Honest Jimmy", null,
				"1. Talk to Honest Jimmy (outlined) on Mos Le'Harmless and join a team.",
				"2. Stay until the game ends. You don't have to win."))
			.build());
	}

	private static void hard(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.HARD;
		out.add(task(t, 7, "Bittercap mushrooms", "Harvest some Bittercap Mushrooms from the patch in Canifis.")
			.skill(Skill.FARMING, 53)
			.item(DiaryItem.of("Mushroom spore", ItemID.MUSHROOM_SEED))
			.item(RAKE).item(DIBBER)
			.stage(simple("Grow bittercaps in Canifis", Location.of("Mushroom patch, Canifis", 3452, 3473, 0, 3), null,
				"Mushroom patch",
				"1. Rake the Mushroom patch (outlined) west of Canifis and plant the spore.",
				"2. Come back in about 4 hours and pick the mushrooms."))
			.build());
		out.add(task(t, 2, "Advanced spike chain", "Climb up the advanced spike chain within Slayer Tower.")
			.skill(Skill.AGILITY, 71)
			.note("Nose peg or slayer helmet if you pass the aberrant spectres")
			.stage(slayerTowerUp("to the first floor"))
			.stage(DiaryStage.builder()
				.title("Climb the advanced spiky chain")
				.inside(SLAYER_TOWER_1)
				.at(Location.of("Spiky chain, Slayer Tower first floor", 3447, 3576, 1, 2))
				.object("Spikey chain")
				.line("1. Walk to the north-east, near the infernal mages, and Climb-up the Spikey chain (outlined).")
				.line("Only a climb without damage counts; try again if you get hurt.")
				.build())
			.build());
		out.add(task(t, 9, "Salve bridge shortcut", "Use the shortcut to get to the bridge over the Salve.")
			.skill(Skill.AGILITY, 65)
			.stage(simple("Jump down the shortcut to the Salve bridge", Location.of("Railing by the Salve bridge", 3424, 3476, 0, 2),
				null, "Railing", "1. Click Jump-over on the Railing (outlined) west of Canifis, going down towards the bridge."))
			.build());
		out.add(task(t, 8, "Piety at the nature altar", "Pray at the Altar of Nature with Piety activated.")
			.quest(Quest.NATURE_SPIRIT).quest(Quest.KINGS_RANSOM)
			.skill(Skill.PRAYER, 70).unboostable(Skill.PRAYER)
			.skill(Skill.DEFENCE, 70).unboostable(Skill.DEFENCE)
			.note("Piety unlocked (Knight Waves Training Grounds)")
			.stage(grottoDown())
			.stage(DiaryStage.builder()
				.title("Pray at the nature altar with Piety on")
				.inside(GROTTO_ZONE)
				.at(Location.of("Altar of nature, Nature Grotto", 3442, 9741, 1, 2))
				.object("Altar of nature")
				.line("1. Turn on Piety, then click Pray-at on the Altar of nature (outlined).")
				.build())
			.build());
		out.add(task(t, 5, "Hard temple trek", "Complete a temple trek with a hard companion.")
			.quest(Quest.IN_AID_OF_THE_MYREQUE)
			.note("Food and a weapon for the trek's fights")
			.stage(simple("Escort a hard companion to Burgh de Rott", Location.of("Paterdomus, temple trek start", 3477, 3236, 0, 4),
				null, null,
				"1. At Paterdomus, talk to a hard companion (the wiki's Temple Trekking page lists them) and start the trek.",
				"2. Walk the route to Burgh de Rott; the events don't have to be done."))
			.build());
		out.add(task(t, 10, "Abandoned Mine mithril", "Mine some Mithril ore in the Abandoned Mine.")
			.quest(Quest.HAUNTED_MINE)
			.skill(Skill.MINING, 55)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Go into the Abandoned Mine", Location.of("Abandoned Mine, south of Mort'ton", 3440, 3232, 0, 2), null,
				"Cart tunnel", "1. Enter the mine (outlined) south-east of Mort'ton; the crystal-mine key shortcut is quickest."))
			.stage(DiaryStage.builder()
				.title("Mine mithril")
				.inside(Zone.of(2767, 4405, 2817, 4556, 0))
				.object("Mithril rocks").object("Rocks")
				.line("1. Mine the Mithril rocks (outlined) by the crystal shortcut until you get one ore.")
				.build())
			.build());
		out.add(task(t, 1, "Kharyrll portal", "Enter the Kharyrll Portal in your POH.")
			.quest(Quest.DESERT_TREASURE_I)
			.skill(Skill.CONSTRUCTION, 50).skill(Skill.MAGIC, 66)
			.note("A Kharyrll portal built in your house portal chamber")
			.stage(DiaryStage.builder()
				.title("Use the Kharyrll portal in your house")
				.line("1. In your house, walk through the Kharyrll portal (or teleport there from a portal nexus).")
				.build())
			.build());
		out.add(task(t, 3, "Harmony Island watermelon", "Harvest some Watermelon from the Allotment patch on Harmony Island.")
			.questStarted(Quest.THE_GREAT_BRAIN_ROBBERY)
			.skill(Skill.FARMING, 47)
			.item(DiaryItem.of("Watermelon seed", 3, ItemID.WATERMELON_SEED))
			.item(DiaryItem.of("Spade", ItemID.SPADE)).item(RAKE).item(DIBBER)
			.stage(simple("Grow watermelons on Harmony Island", Location.of("Allotment, Harmony Island", 3794, 2836, 0, 3),
				null, "Allotment",
				"1. Get to Harmony Island via Brother Tranquility on Mos Le'Harmless.",
				"2. Rake the Allotment (outlined), plant the 3 seeds, come back in about 80 minutes and harvest."))
			.build());
		out.add(task(t, 4, "Mahogany on Mos Le'Harmless", "Chop and burn some mahogany logs on Mos Le'Harmless.")
			.quest(Quest.CABIN_FEVER)
			.skill(Skill.WOODCUTTING, 50).skill(Skill.FIREMAKING, 50)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DiaryItem.of("Tinderbox", ItemID.TINDERBOX))
			.stage(simple("Chop a mahogany on Mos Le'Harmless", Location.of("Mahogany trees, east Mos Le'Harmless", 3826, 3056, 0, 3),
				null, "Mahogany", "1. Chop a Mahogany (outlined) until you get logs."))
			.stage(DiaryStage.builder()
				.title("Burn the logs on the island")
				.carrying(ItemID.MAHOGANY_LOGS)
				.use(ItemID.TINDERBOX).use(ItemID.MAHOGANY_LOGS)
				.line("1. Still on Mos Le'Harmless, use the Tinderbox on the Mahogany logs (both outlined).")
				.build())
			.build());
		out.add(task(t, 6, "Cave horror", "Kill a Cave Horror.")
			.quest(Quest.CABIN_FEVER)
			.skill(Skill.SLAYER, 58)
			.item(WITCHWOOD)
			.item(DiaryStages.LIGHT_SOURCE)
			.stage(simple("Go into the Mos Le'Harmless caves", Location.of("Cave entrance, Mos Le'Harmless", 3748, 2973, 0, 2),
				null, "Cave entrance", "1. Wear the Witchwood icon and light your light source, then enter the cave (outlined)."))
			.stage(DiaryStage.builder()
				.title("Kill a cave horror")
				.inside(Zone.of(3711, 9340, 3840, 9474, 0))
				.npc("Cave horror")
				.line("1. Kill a Cave horror (outlined).")
				.build())
			.build());
	}

	private static void elite(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.ELITE;
		out.add(task(t, 4, "Black d'hide body in Canifis bank", "Craft a Black dragonhide body in Canifis bank.")
			.skill(Skill.CRAFTING, 84)
			.item(DiaryItem.of("Black dragon leather", 3, ItemID.DRAGON_LEATHER_BLACK))
			.item(DiaryItem.of("Needle", ItemID.NEEDLE))
			.item(DiaryItem.of("Thread", ItemID.THREAD))
			.stage(DiaryStage.builder()
				.title("Craft the body inside Canifis bank")
				.at(Location.of("Canifis bank", 3512, 3480, 0, 2))
				.inside(Zone.of(3509, 3476, 3514, 3483, 0))
				.use(ItemID.NEEDLE).use(ItemID.DRAGON_LEATHER_BLACK)
				.line("1. Stand in Canifis bank and use the Needle on the Black dragon leather (both outlined).")
				.build())
			.build());
		out.add(task(t, 5, "Abyssal demon in the tower", "Kill an Abyssal demon in the Slayer Tower.")
			.skill(Skill.SLAYER, 85)
			.stage(slayerTowerUp("twice, to the top floor"))
			.stage(DiaryStage.builder()
				.title("Keep climbing")
				.inside(SLAYER_TOWER_1)
				.at(Location.of("Slayer Tower stairs, first floor", 3415, 3541, 1, 2))
				.object("Staircase")
				.line("1. Climb-up the Staircase (outlined) again.")
				.build())
			.stage(DiaryStage.builder()
				.title("Kill an abyssal demon")
				.inside(SLAYER_TOWER_2)
				.npc("Abyssal demon")
				.line("1. Kill an Abyssal demon (outlined) on the top floor. These don't need a task.")
				.build())
			.build());
		out.add(task(t, 3, "Fertile Soil on the herb patch", "Fertilize the Morytania herb patch using Lunar Magic.")
			.quest(Quest.LUNAR_DIPLOMACY)
			.skill(Skill.MAGIC, 83)
			.note("Lunar spellbook selected")
			.item(DiaryItem.of("Nature rune", 2, ItemID.NATURERUNE))
			.item(DiaryItem.of("Astral rune", 3, ItemID.ASTRALRUNE))
			.item(DiaryItem.of("Earth rune", 15, ItemID.EARTHRUNE))
			.stage(simple("Cast Fertile Soil on the herb patch", Location.of("Herb patch west of Port Phasmatys", 3606, 3530, 0, 3),
				null, "Herb patch", "1. Cast Fertile Soil on the Herb patch (outlined). The patch must have something planted."))
			.build());
		out.add(task(t, 2, "Shade on a magic pyre", "Cremate any Shade remains on a Magic or Redwood pyre.")
			.quest(Quest.SHADES_OF_MORTTON)
			.skill(Skill.FIREMAKING, 80)
			.item(DiaryItem.of("Magic pyre logs", ItemID.MAGIC_LOGS_PYRE))
			.item(DiaryItem.of("Loar remains", ItemID.SHADE_BONES1))
			.item(DiaryItem.of("Tinderbox", ItemID.TINDERBOX))
			.stage(simple("Burn the remains on a pyre in Mort'ton", Location.of("Funeral pyre, Mort'ton temple", 3500, 3266, 0, 3),
				null, "Funeral pyre",
				"1. Use the Magic pyre logs on a Funeral pyre (outlined) in Mort'ton, then the Loar remains.",
				"2. Light it with the tinderbox."))
			.build());
		out.add(task(t, 1, "Bare-handed shark", "Catch a shark in Burgh de Rott with your bare hands.")
			.questStarted(Quest.IN_AID_OF_THE_MYREQUE)
			.skill(Skill.FISHING, 96).skill(Skill.STRENGTH, 76)
			.note("Barbarian Fishing learned from Otto; no harpoon in your inventory")
			.stage(simple("Catch a shark bare-handed", Location.of("Fishing spots, Burgh de Rott", 3479, 3189, 0, 4),
				"Fishing spot", null,
				"1. Bank any harpoon. At the Burgh de Rott fishing spots (outlined), click Harpoon with no harpoon.",
				"2. Keep going until you catch a shark with your hands."))
			.build());
		out.add(task(t, 6, "Barrows in a full set", "Loot the Barrows chest while wearing any complete barrows set.")
			.skill(Skill.DEFENCE, 70).unboostable(Skill.DEFENCE)
			.note("A full Barrows set with its weapon (70 Attack/Strength/Magic/Ranged as the set needs)")
			.item(DiaryItem.of("Spade", ItemID.SPADE))
			.stage(simple("Loot the Barrows chest in a full set", Location.of("Barrows", 3565, 3289, 0, 8), null, "Chest",
				"1. Wear the whole set and its weapon.",
				"2. Kill the brothers (dig on each mound with the spade), then open the Chest (outlined) in the tunnels."))
			.build());
	}

	private static DiaryTask.DiaryTaskBuilder task(DiaryTier tier, int number, String name, String text)
	{
		int index = BITS.get(tier)[number - 1];
		return DiaryTask.builder()
			.region(DiaryRegion.MORYTANIA)
			.tier(tier)
			.number(number)
			.name(name)
			.text(text)
			.varp(index < 32 ? VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY : VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2)
			.bit(index % 32);
	}
}
