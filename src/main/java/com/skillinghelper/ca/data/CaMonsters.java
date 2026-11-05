package com.skillinghelper.ca.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.diary.data.DiaryItem;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Every boss and monster a Combat Achievement task is about. Levels are the comfortable fighting strength (combat
 * stats with fitting gear) the wiki's guides suggest; entrances are where Shortest Path is sent. Places without an
 * entrance are reached by the steps (fairy rings, boats, quest areas). Coordinates are from the wiki's maps and
 * Slayer data, not checked in game.
 */
public final class CaMonsters
{
	private static final String WAYS = "/com/skillinghelper/ca/ca-monsters.tsv";

	static final DiaryItem ANTI_DRAGON = DiaryItem.owned("Anti-dragon shield (or a dragonfire shield)",
		"Ask Duke Horacio on the first floor of Lumbridge Castle for one (free once you've started Dragon Slayer I).",
		ItemID.DRAGONFIRE_SHIELD, ItemID.DRAGONFIRE_WARD, ItemID.DRAGONFIRE_SHIELD_UNCHARGED,
		ItemID.DRAGONFIRE_WARD_UNCHARGED, ItemID.ANTIDRAGONBREATHSHIELD);
	static final DiaryItem WYVERN_SHIELD = DiaryItem.anyOf("Mirror shield (or an elemental shield or dragonfire ward)",
		1, ItemID.SLAYER_MIRROR_SHIELD, ItemID.DRAGONFIRE_WARD, ItemID.ELEMENTAL_SHIELD, ItemID.SLAYER_MIRROR_SHIELD);
	static final DiaryItem MIRROR_SHIELD = DiaryItem.anyOf("Mirror shield", 1, ItemID.SLAYER_MIRROR_SHIELD,
		ItemID.SLAYER_MIRROR_SHIELD);
	static final DiaryItem NOSE_PEG = DiaryItem.anyOf("Nose peg (or a Slayer helmet)", 1, ItemID.SLAYER_NOSEPEG,
		ItemID.SLAYER_HELM_I, ItemID.SLAYER_HELM, ItemID.SLAYER_NOSEPEG);
	static final DiaryItem FACEMASK = DiaryItem.anyOf("Facemask (or a Slayer helmet)", 1, ItemID.SLAYER_FACEMASK,
		ItemID.SLAYER_HELM_I, ItemID.SLAYER_HELM, ItemID.SLAYER_FACEMASK);
	static final DiaryItem ROCK_HAMMER = DiaryItem.anyOf("Rock hammer", 1, ItemID.SLAYER_ROCK_HAMMER,
		ItemID.SLAYER_ROCK_HAMMER);
	static final DiaryItem LEAF_BLADED = DiaryItem.anyOf("Leaf-bladed spear, sword or battleaxe", 1,
		ItemID.SLAYER_LEAFBLADED_SPEAR, ItemID.LEAFBLADED_BATTLEAXE, ItemID.LEAFBLADED_SWORD,
		ItemID.SLAYER_LEAFBLADED_SPEAR);
	static final DiaryItem STONE_BOOTS = DiaryItem.anyOf("Granite boots or boots of stone (for the Karuulm dungeon)", 1,
		ItemID.GRANITE_BOOTS, ItemID.BOOTS_OF_STONE, ItemID.GRANITE_BOOTS);
	static final DiaryItem SPADE = DiaryItem.of("Spade", ItemID.SPADE);
	static final DiaryItem LIGHT = DiaryItem.anyOf("Light source (a candle is fine)", 1, ItemID.UNLIT_CANDLE,
		ItemID.BULLSEYE_LANTERN_LIT, ItemID.OIL_LANTERN_LIT, ItemID.OIL_LAMP_LIT, ItemID.CANDLE_LANTERN_LIT,
		ItemID.TORCH_LIT, ItemID.LIT_CANDLE, ItemID.UNLIT_CANDLE);
	static final DiaryItem TINDERBOX = DiaryItem.of("Tinderbox (to light the candle)", ItemID.TINDERBOX);
	static final DiaryItem ROPES = DiaryItem.of("Rope", 2, ItemID.ROPE);
	static final DiaryItem GIANT_KEY = DiaryItem.owned("Giant key",
		"Kill hill giants (the ones in the Edgeville Dungeon, by Obor's gate, are closest) until one drops it.",
		ItemID.HILLGIANT_BOSS_KEY);
	static final DiaryItem MOSSY_KEY = DiaryItem.owned("Mossy key",
		"Kill moss giants (the ones in the Varrock Sewers, by Bryophyta's gate, are closest) until one drops it.",
		ItemID.MOSSY_KEY);

	private static final Location SLAYER_TOWER = Location.of("the Slayer Tower entrance", 3428, 3537, 4);
	private static final Location VARROCK_SEWERS = Location.of("the manhole east of Varrock Palace", 3237, 3458, 3);
	private static final Location TAVERLEY_DUNGEON = Location.of("the Taverley Dungeon ladder", 2884, 3397, 3);
	private static final Location GOD_WARS = Location.of("the God Wars Dungeon entrance", 2917, 3747, 5);
	private static final Location CATACOMBS = Location.of("the Catacombs of Kourend entrance", 1636, 3673, 4);
	private static final Location KARUULM = Location.of("the Mount Karuulm dungeon entrance", 1311, 3807, 4);
	private static final Location ICE_DUNGEON = Location.of("the Asgarnian Ice Dungeon trapdoor", 3008, 3150, 3);
	private static final Location CHASM_OF_FIRE = Location.of("the Chasm of Fire entrance", 1433, 3671, 4);
	private static final Location GIANTS_DEN = Location.of("the Giants' Den entrance", 1419, 3588, 4);
	private static final Location TZHAAR = Location.of("the TzHaar city", 2438, 5168, 8);
	private static final Location RELLEKKA_TORFINN = Location.of("Torfinn's dock in Rellekka", 2640, 3697, 5);

	private static final String SLAYER_TASK = "Only while you have a Slayer task for it";
	private static final String GWD_IN = "Climb down the hole into the God Wars Dungeon (the first time you tie a rope "
		+ "to it). Bring 40 kills worth of time: each boss room needs 40 kills of that god's followers first "
		+ "(fewer with Combat Achievement rewards), shown top left.";

	private static final List<CaMonster> ALL = List.of(
		m("Aberrant Spectre", 50).at(SLAYER_TOWER).skill(Skill.SLAYER, 60).quest(Quest.PRIEST_IN_PERIL)
			.item(NOSE_PEG)
			.enter("Walk in through the Slayer Tower's front door and climb the stairs to the first floor.")
			.enter("Wear your nose peg (or Slayer helmet) before you go near them.")
			.npc("Aberrant spectre").build(),
		m("Abyssal Sire", 85).skill(Skill.SLAYER, 85).note(SLAYER_TASK + " (abyssal demons)")
			.enter("Use the fairy ring code DIP: it lands you in the Abyssal Nexus next to the Sire's chambers.")
			.enter("Walk into one of the four chambers (peek at the eyes by the entrance to see if one is free).")
			.npc("Abyssal Sire").build(),
		m("Alchemical Hydra", 90).at(KARUULM).skill(Skill.SLAYER, 95).note(SLAYER_TASK + " (hydras)")
			.item(STONE_BOOTS)
			.enter("Wear your boots, then climb into the Karuulm Slayer Dungeon at the foot of Mount Karuulm.")
			.enter("Go down to the lower level and head north past the hydras to the lair guarded by Orrvor quo Maten.")
			.npc("Alchemical Hydra").build(),
		m("Amoxliatl", 75).quest(Quest.THE_HEART_OF_DARKNESS)
			.enter("Go into the Ruins of Tapoyauik in Varlamore and find the large door to the west.")
			.enter("After The Heart of Darkness, the lift in the Frost Nagua chamber is a shortcut there.")
			.build(),
		m("Araxxor", 88).skill(Skill.SLAYER, 92).quest(Quest.PRIEST_IN_PERIL)
			.enter("Enter the Morytania Spider Cave north-east of Darkmeyer.")
			.enter("Crawl through the web tunnel just north-west of the cave entrance into Araxxor's lair.")
			.build(),
		m("Barrows", 60).noAttack(true).pvmId("barrows").quest(Quest.PRIEST_IN_PERIL)
			.at(Location.of("the Barrows", 3565, 3289, 10)).item(SPADE)
			.npc("Ahrim the Blightbringer").npc("Dharok the Wretched").npc("Guthan the Infested")
			.npc("Karil the Tainted").npc("Torag the Corrupted").npc("Verac the Defiled").object("Chest")
			.object("Sarcophagus").object("Staircase")
			.enter("Stand on top of a brother's mound and dig with your spade to drop into his crypt.")
			.enter("Search the sarcophagus: the brother appears. Kill him, climb the stairs, repeat on every mound.")
			.enter("One crypt has a tunnel instead: go through it to the chest in the middle and open it.")
			.kcName("barrows chest").build(),
		m("Basilisk Knight", 75).skill(Skill.SLAYER, 60).quest(Quest.THE_FREMENNIK_EXILES).item(MIRROR_SHIELD)
			.note(SLAYER_TASK + " (basilisks)")
			.enter("Go to Jormungand's Prison under the Island of Stone (reached from Rellekka after The Fremennik "
				+ "Exiles).")
			.enter("Wear your mirror shield before you go near them.")
			.build(),
		m("Black Dragon", 60).at(TAVERLEY_DUNGEON).item(ANTI_DRAGON)
			.note("A dusty key (from the Jailer in Taverley Dungeon) or 70 Agility for the pipe shortcut")
			.enter("Climb down the ladder into Taverley Dungeon.")
			.enter("Go through the gate with your dusty key (or squeeze through the pipe with 70 Agility) to the "
				+ "black dragons in the north-west.")
			.enter("Wear your anti-dragon shield the whole time and drink an antifire potion if you have one.")
			.build(),
		m("Bloodveld", 45).skill(Skill.SLAYER, 50).at(Location.of("the Stronghold Slayer Cave entrance", 2427, 3424, 4))
			.enter("Go into the Stronghold Slayer Cave in the Tree Gnome Stronghold (by Nieve).")
			.enter("The bloodvelds are in the north part of the cave.")
			.build(),
		m("Brutal Black Dragon", 80).skill(Skill.SLAYER, 77).at(CATACOMBS).item(ANTI_DRAGON)
			.enter("Go into the Catacombs of Kourend through the statue in the middle of Kourend Castle's courtyard.")
			.enter("The brutal black dragons are in the north-west of the catacombs. Wear your anti-dragon shield.")
			.build(),
		m("Brutus", 30).members(false).quest(Quest.THE_IDES_OF_MILK).at(Location.of("the Lumbridge cow field", 3263, 3297, 6))
			.enter("Go to the north-east corner of the cow field east of Lumbridge.")
			.enter("Try to release Brutus: you're moved into your own copy of the field where he spawns.")
			.build(),
		m("Bryophyta", 40).members(false).pvmId("bryophyta").at(VARROCK_SEWERS).item(MOSSY_KEY).object("Manhole")
			.object("Gate")
			.enter("Open the manhole east of Varrock Palace and climb down.")
			.enter("Walk west through the sewers past the moss giants to the big gate.")
			.enter("Click the gate (Open) with the mossy key in your inventory to go in.")
			.build(),
		m("Callisto", 85).wilderness(true).at(Location.of("Callisto's Den", 3291, 3849, 10))
			.enter("Callisto's Den is in the north-east Wilderness. Bring only what you're willing to lose.")
			.build(),
		m("Cerberus", 88).at(TAVERLEY_DUNGEON).skill(Skill.SLAYER, 91).note(SLAYER_TASK + " (hellhounds)")
			.enter("Climb down into Taverley Dungeon and head to the cave entrance past the hellhounds in the north-east.")
			.enter("Climb down the winch into one of the three rooms.")
			.npc("Cerberus").build(),
		m("Chambers of Xeric", 90).noAttack(true).pvmId("chambers_of_xeric")
			.at(Location.of("Mount Quidamortem", 1233, 3567, 6)).object("Chambers of Xeric")
			.enter("Go to Mount Quidamortem (Lovakengj minecart to Mount Quidamortem is the quickest).")
			.enter("Join a party or make your own at the noticeboard, then enter the Chambers.")
			.kcName("chambers of xeric").build(),
		m("Chambers of Xeric: Challenge Mode", 95).noAttack(true).wikiPage("Chambers of Xeric/Challenge Mode").pvmId("chambers_of_xeric")
			.at(Location.of("Mount Quidamortem", 1233, 3567, 6))
			.enter("Go to Mount Quidamortem and start a raid. Challenge Mode needs a raid party set to Challenge Mode "
				+ "(you need a raid kill count first).")
			.kcName("chambers of xeric challenge mode").build(),
		m("Chaos Elemental", 75).wilderness(true).at(Location.of("the Chaos Elemental, west of Rogues' Castle", 3261, 3927, 10))
			.enter("It roams west of the Rogues' Castle in deep Wilderness. Bring only what you're willing to lose.")
			.build(),
		m("Chaos Fanatic", 60).wilderness(true).at(Location.of("the Chaos Fanatic, west of the Lava Maze", 2979, 3846, 8))
			.enter("He stands west of the Lava Maze in the Wilderness. Bring only what you're willing to lose.")
			.build(),
		m("Commander Zilyana", 87).pvmId("god_wars").at(GOD_WARS).skill(Skill.AGILITY, 70)
			.questStarted(Quest.TROLL_STRONGHOLD)
			.enter(GWD_IN).enter("Saradomin's room is in the south-west: climb down the rock with your rope (70 Agility).")
			.build(),
		m("Corporeal Beast", 90).team(2).wilderness(true)
			.enter("Rub a games necklace and choose Corporeal Beast: it puts you in the safe room by its door.")
			.enter("Go through the large doorway to fight it.")
			.build(),
		m("Corrupted Hunllef", 85).noAttack(true).pvmId("gauntlet").quest(Quest.SONG_OF_THE_ELVES).wikiPage("The Corrupted Gauntlet")
			.enter("Go to the Gauntlet in north Prifddinas and enter the Corrupted Gauntlet.")
			.enter("You go in with nothing: gather and make your gear inside, then fight the Hunllef.")
			.kcName("corrupted gauntlet").build(),
		m("Crazy Archaeologist", 60).wilderness(true).at(Location.of("the Ruins (west) in the Wilderness", 2977, 3702, 8))
			.enter("He's at the ruins in the west Wilderness. Bring only what you're willing to lose.")
			.build(),
		m("Crystalline Hunllef", 75).noAttack(true).pvmId("gauntlet").quest(Quest.SONG_OF_THE_ELVES).wikiPage("The Gauntlet")
			.enter("Go to the Gauntlet in north Prifddinas and enter the normal Gauntlet.")
			.enter("You go in with nothing: gather and make your gear inside, then fight the Hunllef.")
			.kcName("gauntlet").build(),
		dks("Dagannoth Prime"),
		dks("Dagannoth Rex"),
		dks("Dagannoth Supreme"),
		m("Demonic Gorilla", 85).quest(Quest.MONKEY_MADNESS_II)
			.enter("Go to the Crash Site Cavern on Ape Atoll (after Monkey Madness II).").build(),
		m("Deranged Archaeologist", 60).quest(Quest.BONE_VOYAGE)
			.at(Location.of("the Deranged Archaeologist, Fossil Island swamp", 3683, 3706, 8))
			.enter("Take a digsite pendant (or the boat from the Digsite) to Fossil Island, then the mushroom "
				+ "(Mycelium) to the Sticky Swamp.")
			.enter("Chop the swamp vine with an axe to reach him.")
			.build(),
		m("Doom of Mokhaiotl", 92).note("Varlamore boss: check the wiki for what unlocks it")
			.enter("Go to the Ruins of Mokhaiotl (Mokhaiotl waystone) and into the Tonali Cavern.").build(),
		m("Duke Sucellus", 85).quest(Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE)
			.enter("Go into Weiss's Salt Mine, then Ghorrock Dungeon, then the Ghorrock Prison asylum.").build(),
		m("Fire Giant", 30).at(GIANTS_DEN)
			.enter("Go into the Giants' Den (west of Shayzien, by the coast) and find the fire giants.").build(),
		m("Fortis Colosseum", 92).noAttack(true).pvmId("fortis_colosseum").quest(Quest.CHILDREN_OF_THE_SUN)
			.enter("Go to the Fortis Colosseum in Civitas illa Fortis (Varlamore) and talk to Minimus to start.")
			.kcName("colosseum glory").build(),
		m("Fragment of Seren", 85).quest(Quest.SONG_OF_THE_ELVES)
			.note("Fought during Song of the Elves; check the wiki for the rematch")
			.build(),
		m("Galvek", 85).quest(Quest.DRAGON_SLAYER_II).item(ANTI_DRAGON)
			.note("Fought during Dragon Slayer II; check the wiki for the rematch").build(),
		m("Gargoyle", 60).at(SLAYER_TOWER).skill(Skill.SLAYER, 75).quest(Quest.PRIEST_IN_PERIL).item(ROCK_HAMMER)
			.enter("Go into the Slayer Tower and climb to the top floor.")
			.enter("When a gargoyle is low on health, use the rock hammer on it to finish it.")
			.build(),
		m("General Graardor", 85).pvmId("god_wars").at(GOD_WARS).skill(Skill.STRENGTH, 70)
			.questStarted(Quest.TROLL_STRONGHOLD)
			.enter(GWD_IN).enter("Bandos' room is in the north-east: bang the big door with a hammer (70 Strength).")
			.build(),
		m("Giant Mole", 50).pvmId("giant_mole").at(Location.of("Falador Park", 2989, 3378, 6)).item(SPADE).item(LIGHT)
			.item(TINDERBOX)
			.enter("Stand on one of the mole hills in Falador Park and dig with your spade.")
			.enter("Light your candle (use the tinderbox on it) so the tunnels don't hurt you.")
			.build(),
		m("Giants", 25).noAttack(true).wikiPage("Giants' Den").at(GIANTS_DEN).npc("Hill Giant").npc("Moss Giant").npc("Fire Giant")
			.enter("Go into the Giants' Den (west of Shayzien, by the coast).")
			.enter("Kill one hill giant, one moss giant and one fire giant in there.")
			.build(),
		m("Glough", 75).quest(Quest.MONKEY_MADNESS_II).note("Fought during Monkey Madness II; check the wiki for the rematch")
			.build(),
		m("Greater Demon", 45).at(CHASM_OF_FIRE)
			.enter("Climb down into the Chasm of Fire (Kourend, east of the Hosidius/Shayzien border).")
			.enter("The greater demons are on the lower levels.")
			.build(),
		m("Grotesque Guardians", 80).at(SLAYER_TOWER).skill(Skill.SLAYER, 75).quest(Quest.PRIEST_IN_PERIL)
			.note(SLAYER_TASK + " (gargoyles); the first time you need a brittle key from a gargoyle")
			.npc("Dusk").npc("Dawn")
			.enter("Go to the top floor of the Slayer Tower and through the roof gate.").build(),
		m("Hellhound", 50).at(TAVERLEY_DUNGEON)
			.enter("Climb down into Taverley Dungeon; the hellhounds are past the blue dragons in the north.").build(),
		m("Hespori", 60).skill(Skill.FARMING, 65).at(Location.of("the Farming Guild", 1249, 3718, 6))
			.note("A hespori seed planted in the patch inside the Farming Guild cave, fully grown")
			.enter("Go into the cave in the west wing of the Farming Guild.")
			.enter("When the hespori patch is fully grown, click it to start the fight.")
			.build(),
		m("K'ril Tsutsaroth", 85).pvmId("god_wars").at(GOD_WARS).skill(Skill.HITPOINTS, 70)
			.questStarted(Quest.TROLL_STRONGHOLD)
			.enter(GWD_IN).enter("Zamorak's room is in the south-east: cross the river (70 Hitpoints).").build(),
		m("Kalphite Queen", 75).pvmId("kalphite_queen").at(Location.of("the Kalphite Lair entrance", 3226, 3108, 4))
			.item(ROPES)
			.enter("Use a rope on the tunnel entrance south-west of the Shantay Pass and climb down.")
			.enter("Use the second rope on the crevice to the queen's level and climb down.")
			.build(),
		m("King Black Dragon", 70).pvmId("king_black_dragon").wilderness(true)
			.at(Location.of("the King Black Dragon lever room", 3016, 3849, 6)).item(ANTI_DRAGON)
			.enter("Pull the lever in the Wilderness lever room to drop into the lair (or use the lever in "
				+ "Edgeville's lever room to get there faster).")
			.enter("Wear your anti-dragon shield and drink an antifire potion before going in.")
			.build(),
		m("Kraken", 70).skill(Skill.SLAYER, 87).note(SLAYER_TASK + " (cave krakens)")
			.at(Location.of("the Kraken Cove entrance", 2276, 3611, 4))
			.enter("Go into Kraken Cove south-west of the Piscatoris Fishing Colony and to the boss room.")
			.enter("Disturb the four whirlpools, then the big one to wake the Kraken.")
			.build(),
		m("Kree'arra", 85).pvmId("god_wars").at(GOD_WARS).skill(Skill.RANGED, 70).questStarted(Quest.TROLL_STRONGHOLD)
			.enter(GWD_IN).enter("Armadyl's room is in the north-west: grapple across with a crossbow and mith "
				+ "grapple (70 Ranged).").build(),
		m("Kurask", 60).skill(Skill.SLAYER, 70).at(Location.of("the Fremennik Slayer Dungeon entrance", 2796, 3615, 4))
			.item(LEAF_BLADED)
			.enter("Go into the Fremennik Slayer Dungeon south-east of Rellekka and find the kurasks at the end.")
			.enter("Only leaf-bladed weapons, broad ammo or Magic Dart hurt them.")
			.build(),
		m("Leviathan", 85).quest(Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE)
			.enter("Talk to the Catalytic Guardian in the Temple of the Eye to go to the Scar.")
			.enter("Go east past the Abyssal Rift to Wizard Persten's camp and take the rowboat.").build(),
		m("Lizardman Shaman", 60).at(Location.of("Lizardman Canyon", 1424, 3718, 10))
			.enter("Go to the west side of Lizardman Canyon (south-east Kourend) and climb the handholds.")
			.enter("Shamans jump on you and spawn exploding lizards: keep moving away from them.")
			.build(),
		m("Mad Angel", 75).note("Sailing-era content: check the wiki for what unlocks it")
			.enter("Go to Ardeaglais, the cathedral on the south side of Wyrmscraig.").build(),
		m("Maggot King", 85).note("Check the wiki for what unlocks the Vampyrium")
			.enter("Teleport to Castle Drakan with Drakan's medallion, go into the Vampyrium and run east.").build(),
		m("Moons of Peril", 75).noAttack(true).pvmId("moons_of_peril").quest(Quest.PERILOUS_MOONS)
			.enter("Go to Neypotzli under Cam Torum (Varlamore). Fight the moons one by one, restocking between.")
			.kcName("lunar chest").build(),
		m("Nex", 95).team(4).at(GOD_WARS).skill(Skill.AGILITY, 70).skill(Skill.STRENGTH, 70)
			.skill(Skill.HITPOINTS, 70).skill(Skill.RANGED, 70).questStarted(Quest.TROLL_STRONGHOLD)
			.note("A frozen key (pieces from the four God Wars bosses)")
			.enter(GWD_IN).enter("Go through the frozen door in the south of the dungeon with your frozen key.").build(),
		m("None", 50).noAttack(true).bring("Bring the runes for your thrall spell").wikiPage("Thralls").npc("Chicken")
			.note("Needs the Arceuus spellbook and Resurrect thrall spells (A Kingdom Divided)")
			.enter("Switch to the Arceuus spellbook, cast a Resurrect Lesser/Superior/Greater Ghost or Skeleton or "
				+ "Zombie, and let it hit anything until it has dealt 100 damage.").build(),
		m("Obor", 40).members(false).pvmId("obor").at(Location.of("the Edgeville trapdoor", 3097, 3468, 3)).item(GIANT_KEY)
			.object("Trapdoor").object("Gate")
			.enter("Open the trapdoor in Edgeville and climb down.")
			.enter("Walk west past the hill giants to the big gate and open it with the giant key in your inventory.")
			.build(),
		m("Phantom Muspah", 85).quest(Quest.SECRETS_OF_THE_NORTH)
			.enter("Go into Weiss's Salt Mine and on into Ghorrock Dungeon.")
			.enter("Climb into the crevice in the south-west of the dungeon.").build(),
		m("Phosani's Nightmare", 95).quest(Quest.PRIEST_IN_PERIL).npc("Phosani's Nightmare")
			.enter("Go to Slepe (Drakan's medallion to Ver Sinhaza, then north and east).")
			.enter("Go down the stairs south of the graveyard and use the Sisterhood Sanctuary's bed for Phosani's.")
			.build(),
		m("Royal Titans", 65).noAttack(true).at(ICE_DUNGEON).npc("Branda the Fire Queen").npc("Eldric the Ice King")
			.enter("Climb down the trapdoor south of Port Sarim into the Asgarnian Ice Dungeon.")
			.enter("The tunnel to the Royal Titans is in the west of the dungeon.").build(),
		m("Sarachnis", 65).pvmId("sarachnis")
			.enter("Go into Forthos Dungeon (east of the Forthos ruin, south of Hosidius) and down to the burial tomb.")
			.enter("Cut the webs with a slash weapon or knife to get in.").build(),
		m("Scorpia", 75).wilderness(true).at(Location.of("the Scorpion Pit", 3233, 3945, 6))
			.enter("Climb into the cave in the Scorpion Pit in the north-east Wilderness.").build(),
		m("Scurrius", 50).pvmId("scurrius").at(VARROCK_SEWERS)
			.enter("Open the manhole east of Varrock Palace and climb down.")
			.enter("Walk east to Scurrius' lair and climb through the broken bars.").build(),
		m("Shellbane gryphon", 65).npc("Shellbane gryphon")
			.enter("Use fairy ring CJQ and go north to the cave in the centre of the Great Conch.").build(),
		m("Skeletal Wyvern", 70).at(ICE_DUNGEON).skill(Skill.SLAYER, 72).item(WYVERN_SHIELD)
			.enter("Climb down into the Asgarnian Ice Dungeon and find the wyvern area.")
			.enter("Wear your shield the whole time: their icy breath hurts badly without it.").build(),
		m("Skotizo", 70).at(CATACOMBS).note("A dark totem (made from three totem pieces dropped in the catacombs)")
			.enter("Go into the Catacombs of Kourend and use the dark totem on the altar in the middle.").build(),
		m("Tempoross", 20).noAttack(true).bring("Bring a little food; everything else is in the boxes by the boat").skill(Skill.FISHING, 35)
			.enter("Take Ferryman Sathwood's ferry south of the Al Kharid bank to the sea spirit dock.")
			.enter("Board the boat. Buckets, rope, hammer and harpoon can all be taken from the boxes there.")
			.kcName("tempoross").build(),
		m("The Hueycoatl", 70).team(2).quest(Quest.CHILDREN_OF_THE_SUN)
			.enter("Go to the Darkfrost in Varlamore. It's a group boss: join others or rent a private fight.")
			.kcName("hueycoatl").build(),
		m("The Mimic", 75).note("A mimic casket from an elite or master clue (turn mimics on at the strange casket)")
			.enter("Take the mimic casket upstairs in Watson's house in Hosidius and use it on the strange casket.")
			.build(),
		m("The Nightmare", 90).team(4).quest(Quest.PRIEST_IN_PERIL)
			.enter("Go to Slepe (Drakan's medallion to Ver Sinhaza, then north and east).")
			.enter("Go down the stairs south of the graveyard to the Sisterhood Sanctuary and join a group fight.")
			.kcName("nightmare").build(),
		m("Theatre of Blood", 92).noAttack(true).team(3).pvmId("theatre_of_blood").quest(Quest.PRIEST_IN_PERIL)
			.enter("Teleport to Ver Sinhaza (Drakan's medallion) and walk north to the Theatre.")
			.enter("Make or join a party at the notice board and go in.").kcName("theatre of blood").build(),
		m("Theatre of Blood: Entry Mode", 80).noAttack(true).wikiPage("Theatre of Blood/Entry Mode").pvmId("theatre_of_blood")
			.quest(Quest.PRIEST_IN_PERIL)
			.enter("Teleport to Ver Sinhaza (Drakan's medallion) and walk north to the Theatre.")
			.enter("Pick Entry Mode on the notice board; you can do it alone.").kcName("theatre of blood entry mode")
			.build(),
		m("Theatre of Blood: Hard Mode", 97).noAttack(true).team(4).wikiPage("Theatre of Blood/Hard Mode").pvmId("theatre_of_blood")
			.quest(Quest.PRIEST_IN_PERIL)
			.enter("Teleport to Ver Sinhaza (Drakan's medallion) and walk north to the Theatre.")
			.enter("Set the party to Hard Mode at the notice board.").kcName("theatre of blood hard mode").build(),
		m("Thermonuclear Smoke Devil", 80).skill(Skill.SLAYER, 93).note(SLAYER_TASK + " (smoke devils)")
			.at(Location.of("the Smoke Devil Dungeon entrance", 2411, 3061, 4)).item(FACEMASK)
			.enter("Wear your facemask, go into the Smoke Devil Dungeon south of Castle Wars and through the crevice "
				+ "in the north-west.").build(),
		m("Tombs of Amascut", 85).noAttack(true).pvmId("tombs_of_amascut").quest(Quest.BENEATH_CURSED_SANDS)
			.enter("Go to the Necropolis in the south of the Kharidian Desert (fairy ring AKP).")
			.enter("Make a party at the entrance, set the raid level and go in.").kcName("tombs of amascut").build(),
		m("Tombs of Amascut: Entry Mode", 70).noAttack(true).wikiPage("Tombs of Amascut/Entry Mode").pvmId("tombs_of_amascut")
			.quest(Quest.BENEATH_CURSED_SANDS)
			.enter("Go to the Necropolis (fairy ring AKP), make a party and pick Entry Mode (raid level 0).")
			.kcName("tombs of amascut entry mode").build(),
		m("Tombs of Amascut: Expert Mode", 95).noAttack(true).wikiPage("Tombs of Amascut/Expert Mode").pvmId("tombs_of_amascut")
			.quest(Quest.BENEATH_CURSED_SANDS)
			.enter("Go to the Necropolis (fairy ring AKP), make a party and set the raid level to 300 or more.")
			.kcName("tombs of amascut expert mode").build(),
		m("Tormented Demon", 85).quest(Quest.WHILE_GUTHIX_SLEEPS)
			.enter("Go to the Ancient Guthixian Temple (reached through the Tears of Guthix cave area).").build(),
		m("TzHaar-Ket-Rak's Challenges", 90).noAttack(true).at(TZHAAR).note("Inside Mor Ul Rek: you need a fire cape to get in")
			.npc("TzHaar-Ket-Rak")
			.enter("Go into Mor Ul Rek (show your fire cape at the entrance) and talk to TzHaar-Ket-Rak.").build(),
		m("TzKal-Zuk", 97).noAttack(true).pvmId("inferno").at(TZHAAR).note("A fire cape to sacrifice")
			.enter("Go into Mor Ul Rek and jump into the Inferno.").kcName("tzkal-zuk").build(),
		m("TzTok-Jad", 75).noAttack(true).pvmId("fight_caves").at(TZHAAR).wikiPage("TzHaar Fight Cave")
			.enter("Go into the TzHaar city under the Karamja volcano and enter the Fight Cave.")
			.kcName("tztok-jad").build(),
		m("Vardorvis", 88).quest(Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE)
			.enter("Go to the Stranglewood (Ring of shadows or fairy ring) and climb the rocks to the Ritual Site in "
				+ "the north-west.").build(),
		m("Venenatis", 85).wilderness(true).at(Location.of("the Silk Chasm", 3319, 3798, 8))
			.enter("Climb into the Silk Chasm in the Wilderness. Bring only what you're willing to lose.").build(),
		m("Vet'ion", 85).wilderness(true).at(Location.of("Vet'ion's Rest", 3219, 3788, 8))
			.enter("Go into Vet'ion's Rest in the Wilderness. Bring only what you're willing to lose.").build(),
		m("Vorkath", 85).pvmId("vorkath").quest(Quest.DRAGON_SLAYER_II).at(RELLEKKA_TORFINN).item(ANTI_DRAGON)
			.npc("Torfinn")
			.enter("Talk to Torfinn on the third dock from the west in Rellekka and sail to Ungael.")
			.enter("Climb over the ice chunks into the crater and poke Vorkath to wake him.")
			.build(),
		m("Whisperer", 90).quest(Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE).wikiPage("The Whisperer")
			.enter("Go to the sinkhole north-west of the Ruins of Camdozaal (under Ice Mountain) into the Lassar "
				+ "Undercity, then down to the sunken cathedral.").build(),
		m("Wintertodt", 20).noAttack(true).bring("Bring food (cakes or wines are cheap), and warm clothing so the cold hurts less").skill(Skill.FIREMAKING, 50).at(Location.of("the Doors of Dinh", 1630, 3962, 6))
			.object("Doors of Dinh")
			.enter("Go through the Doors of Dinh. The crates inside have a free axe, knife, hammer and tinderbox.")
			.enter("Chop bruma roots, fletch them into kindling with the knife and feed the burning braziers.")
			.kcName("wintertodt").build(),
		m("Wyrm", 65).skill(Skill.SLAYER, 62).at(KARUULM).item(STONE_BOOTS)
			.enter("Wear your boots and go into the Karuulm Slayer Dungeon; the wyrms are on the lower level.").build(),
		m("Yama", 95).at(CHASM_OF_FIRE).note("Check the wiki for what unlocks Yama")
			.enter("Climb down into the Chasm of Fire and talk to the Voice of Yama at the bottom.").build(),
		m("Zalcano", 70).noAttack(true).bring("Bring your best pickaxe and a little food").team(2).quest(Quest.SONG_OF_THE_ELVES)
			.enter("Go to Prifddinas and down into the volcanic caverns where Zalcano is fought.").build(),
		m("Zulrah", 85).pvmId("zulrah").quest(Quest.REGICIDE)
			.enter("Use a Zul-Andra teleport (or fairy ring BJS) to reach Zul-Andra.")
			.enter("Board the sacrificial boat east of the village to go to the shrine.").build()
	);

	private static final Map<String, CaMonster> BY_NAME = new HashMap<>();
	private static Map<String, List<String>> ways;

	static
	{
		for (CaMonster m : ALL)
		{
			BY_NAME.put(m.getName(), m);
		}
	}

	private CaMonsters()
	{
	}

	public static List<CaMonster> all()
	{
		return ALL;
	}

	/**
	 * The monster a task is about, or null if it isn't listed (a newer task than the plugin knows).
	 */
	public static CaMonster get(String name)
	{
		return BY_NAME.get(name);
	}

	/**
	 * The wiki's own lines on how to get to this monster, possibly empty.
	 */
	public static synchronized List<String> waysThere(String monster)
	{
		if (ways == null)
		{
			Map<String, List<String>> out = new LinkedHashMap<>();
			InputStream in = CaMonsters.class.getResourceAsStream(WAYS);
			if (in != null)
			{
				try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
				{
					String line;
					while ((line = r.readLine()) != null)
					{
						if (line.isEmpty() || line.startsWith("#"))
						{
							continue;
						}
						String[] f = line.split("\t");
						out.put(f[0], Collections.unmodifiableList(new ArrayList<>(Arrays.asList(f).subList(1, f.length))));
					}
				}
				catch (IOException e)
				{
					throw new UncheckedIOException(e);
				}
			}
			ways = out;
		}
		return ways.getOrDefault(monster, Collections.emptyList());
	}

	private static CaMonster.CaMonsterBuilder m(String name, int level)
	{
		return CaMonster.builder().name(name).level(level).npc(name).kcName(name.toLowerCase());
	}

	private static CaMonster dks(String name)
	{
		return m(name, 80).wikiPage("Dagannoth Kings").at(Location.of("Jarvald's boat in Rellekka", 2620, 3685, 5))
			.npc("Jarvald").npc("Dagannoth Prime").npc("Dagannoth Rex").npc("Dagannoth Supreme")
			.note("Opening the doors needs a second player or a pet rock (from Askeladden in Rellekka) and a rune "
				+ "thrownaxe")
			.enter("Talk to Jarvald on the Rellekka dock and travel to Waterbirth Island.")
			.enter("Go into the cave, open the doors (pet rock on the pressure pad, thrownaxe special on the door) and "
				+ "take the ladders down to the kings' level.")
			.build();
	}
}
