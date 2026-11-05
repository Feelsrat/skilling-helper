package com.skillinghelper.minigame.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.ToolType;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.diary.data.DiaryStage;
import com.skillinghelper.diary.data.Zone;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Minigames with no guide elsewhere in this pack, so the step-by-step guide lives here. Written for someone who has
 * never been there: what to bring, how to get in, who and what to click, and what to repeat.
 *
 * Spots are from RuneLite's world map minigame list; rules, requirements and shop prices from the OSRS Wiki
 * (2026-11-05). Object and NPC names to outline are from the wiki and not yet checked in game.
 */
final class GuidedMinigames
{
	private static final Location MTA = Location.area("Mage Training Arena", 3362, 3318, 8);
	/**
	 * The four rooms are under the arena (dungeon y = surface y + 6400); bounds are a guess around that.
	 */
	private static final Zone MTA_ROOMS = Zone.anyFloor(3300, 9600, 3420, 9740);
	private static final Location CASTLE_WARS = Location.of("Castle Wars lobby", 2440, 3090, 8);
	private static final Zone CASTLE_WARS_ARENA = Zone.anyFloor(2368, 3072, 2431, 3135);
	/**
	 * The two waiting rooms, underground below the castles (approximate).
	 */
	private static final Zone CASTLE_WARS_WAITING = Zone.anyFloor(2360, 9460, 2440, 9540);
	private static final Location SOUL_WARS = Location.of("Nomad's camp (Isle of Souls)", 2209, 2855, 10);
	/**
	 * The Isle of Souls around the lobby and the battlefield (approximate, generous).
	 */
	private static final Zone SOUL_WARS_AREA = Zone.anyFloor(2040, 2780, 2330, 3060);
	private static final Location VOLCANIC_MINE = Location.of("Volcanic Mine entrance (Fossil Island)", 3812, 3810, 6);
	private static final Location MIXOLOGY = Location.of("Mastering Mixology lab (under Aldarin)", 1393, 9326, 12);
	private static final Zone MIXOLOGY_LAB = Zone.anyFloor(1365, 9295, 1425, 9355);
	private static final Location AUBURNVALE = Location.of("Auburnvale (Vale Totems)", 1366, 3368, 10);
	private static final Location JUNA = Location.of("Juna (Tears of Guthix cave)", 3252, 9516, 2, 5);

	private GuidedMinigames()
	{
	}

	static List<Minigame> all()
	{
		return List.of(
			Minigame.builder()
				.id("mage_training_arena")
				.name("Mage Training Arena")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.MAGIC)
				.summary("Four rooms of Magic puzzles north of Emir's Arena. Each room gives its own pizazz points, "
					+ "and you need points from all four for the rewards, most of all the Bones to Peaches spell.")
				.wikiPage("Mage_Training_Arena")
				.location(MTA)
				.getThere("Teleport to Emir's Arena with a ring of dueling, then walk north to the big domed "
					+ "building. Once you've talked to the Entrance Guardian, the Minigame Teleport goes there too.")
				.skill(Skill.MAGIC, 7)
				.note("33 Magic for all four rooms (Telekinetic Grab); 55 for High Alchemy in the Alchemists' room")
				.stage(DiaryStage.builder()
					.title("Talk to the Entrance Guardian, then pick a room")
					.at(MTA)
					.inside(MTA_ROOMS)
					.line("1. Go in through the big doors and click the Entrance Guardian (outlined), choose "
						+ "Talk-to. The first time he explains the rooms and unlocks the Minigame Teleport.")
					.line("2. The four coloured portals on the ground floor each take you to a room. Click one "
						+ "(outlined) and choose Enter.")
					.line("3. Graveyard (15 Magic, Bones to Bananas): bring earth and water runes and nature runes. "
						+ "Pick up bones from the piles, cast Bones to Bananas, put the bananas in the food chute.")
					.line("4. Telekinetic Theatre (33 Magic): bring law runes and an air staff. Look at the maze from "
						+ "the outside, cast Telekinetic Grab on the statue to slide it, and get it onto the "
						+ "middle square. Talk to the maze guardian to go to the next maze.")
					.line("5. Enchanting Chamber (7 Magic): bring cosmic runes and the staff for your enchant spell. "
						+ "Take the shape the room shows as the bonus, enchant shapes into orbs and put them in "
						+ "the hole in the middle. Dragonstones give double.")
					.line("6. Alchemists' Playground (21 Magic): bring nature runes and a fire staff, no coins "
						+ "allowed. Search cupboards for the item the board says is worth 30 coins, alch it, "
						+ "and put the coins in the coin collector before the prices change.")
					.line("7. Points for each room only show in that room. Leave any room through the exit portal.")
					.line("8. Spend points upstairs with the Rewards Guardian: get Bones to Peaches first (it "
						+ "doubles Graveyard points).")
					.npc("Entrance Guardian")
					.npc("Rewards Guardian")
					.object("Telekinetic Teleport")
					.object("Graveyard Teleport")
					.object("Enchanters Teleport")
					.object("Alchemists Teleport")
					.build())
				.reward(Reward.unlock("Bones to Peaches", "A spell that turns bones into peaches that heal; huge for "
					+ "Graveyard points and some bosses", "200 Telekinetic, 300 Alchemist, 2,000 Enchantment, 200 "
					+ "Graveyard", 0, VarbitID.MAGICTRAINING_BONESPEACHES))
				.reward(Reward.item("Master wand", "+20 Magic attack: a free, strong early magic weapon", "480 Telekinetic, "
					+ "530 Alchemist, 4,800 Enchantment, 480 Graveyard (and the wand before it)", 0,
					ItemID.MAGICTRAINING_WAND_MASTER))
				.reward(Reward.item("Mage's book", "Good offhand for Magic", "500 Telekinetic, 550 Alchemist, 6,000 "
					+ "Enchantment, 500 Graveyard", 0, ItemID.MAGICTRAINING_BOOKOFMAGIC))
				.reward(Reward.also("Infinity robes", "Magic robes (50 Magic, 25 Defence); mostly replaced by "
					+ "mystic or ahrim's", ItemID.MAGICTRAINING_INFINITYTOP))
				.build(),

			Minigame.builder()
				.id("castle_wars")
				.name("Castle Wars")
				.category(MinigameCategory.COMBAT)
				.summary("Capture the flag: two teams try to steal each other's flag. It's safe (you keep your "
					+ "items if you die) and anyone can play, even free-to-play.")
				.wikiPage("Castle_Wars")
				.location(CASTLE_WARS)
				.area(CASTLE_WARS_WAITING)
				.getThere("Teleport with a ring of dueling to Castle Wars (or the Minigame Teleport).")
				.team("Team game (two teams of players on the world)")
				.note("Only combat gear, runes and potions are allowed in; no capes")
				.points(Points.item("Castle Wars tickets", ItemID.CASTLEWARS_TICKET))
				.stage(DiaryStage.builder()
					.title("Join a team")
					.at(CASTLE_WARS)
					.line("1. Bank anything that isn't combat gear, runes or potions, and take off your cape "
						+ "(the bank chest is outlined).")
					.line("2. Click the Guthix Portal (outlined) to join whichever team needs players. You can "
						+ "pick the Saradomin or Zamorak portal instead.")
					.line("3. You wait in your team's waiting room until the game starts (every 20 minutes).")
					.object("Guthix Portal")
					.object("Saradomin Portal")
					.object("Zamorak Portal")
					.object("Bank chest")
					.build())
				.stage(DiaryStage.builder()
					.title("Play the game")
					.inside(CASTLE_WARS_ARENA)
					.started(true)
					.line("1. Your team's castle is the one your cape colour matches. Leave the spawn room and "
						+ "follow your team.")
					.line("2. To score: get into the enemy castle's top floor, take their flag and bring it back "
						+ "to your own flag stand.")
					.line("3. If you'd rather not fight, stay near your own flag and stop runners, or repair "
						+ "barricades. Everyone who stays active gets tickets.")
					.line("4. If you die you lose nothing: you go back to your spawn room.")
					.line("5. At the end you get tickets (2 to 4 a game). Trade them to Lanthus in the lobby.")
					.build())
				.reward(Reward.other("Castle Wars bracelet", "Bonus damage against the enemy team; made with "
					+ "Crafting (only useful here)"))
				.reward(Reward.item("Decorative armour (gold)", "Only cosmetic, but gold decorative armour is "
					+ "a well-known flex", "400-800 tickets a piece", 0, ItemID.CASTLEWARS_ARMOUR_BODY_3))
				.reward(Reward.also("Team hoods, cloaks and banners", "Cosmetic, 10-100 tickets"))
				.build(),

			Minigame.builder()
				.id("soul_wars")
				.name("Soul Wars")
				.category(MinigameCategory.COMBAT)
				.summary("A 15-minute team game: capture obelisks and graveyards and weaken the enemy's Avatar. "
					+ "It's safe, and zeal tokens buy combat, Prayer or Slayer xp, which is why many go.")
				.wikiPage("Soul_Wars")
				.location(SOUL_WARS)
				.area(SOUL_WARS_AREA)
				.getThere("Walk into the Soul Wars portal in Edgeville (south of the bank), or use the "
					+ "Minigame Teleport.")
				.team("Team game (needs 20 players in the lobby to start)")
				.combat(40)
				.totalLevel(500)
				.stage(DiaryStage.builder()
					.title("Do the tutorial, then join a team")
					.at(SOUL_WARS)
					.line("1. Bank food, potions, capes and non-combat items: they aren't allowed in.")
					.line("2. The first time, click Nomad (outlined), choose Talk-to and do his short tutorial.")
					.line("3. Then go through the blue or red portal (outlined) in the west lobby and wait for "
						+ "the game to start.")
					.line("4. Talk to Nomad any time to spend zeal tokens.")
					.npc("Nomad")
					.object("Blue portal")
					.object("Red portal")
					.build())
				.stage(DiaryStage.builder()
					.title("Play the game")
					.line("1. Stay active or you get kicked: fight, capture the obelisk in the middle, take "
						+ "graveyards, or carry soul fragments.")
					.line("2. Kill the enemy team's players and creatures for soul fragments, and bring them "
						+ "to the Soul Obelisk when your team owns it.")
					.line("3. When the enemy Avatar is weakened, help attack it at the far end of their base.")
					.line("4. Dying is safe: you come back at your graveyard with your items.")
					.line("5. After 15 minutes you get zeal (up to 60 for a win, 40 for a loss).")
					.build())
				.reward(Reward.other("Combat, Prayer and Slayer xp", "Zeal buys xp in any combat skill at 30+, "
					+ "the reason most people play"))
				.reward(Reward.item("Ectoplasmator", "Bonus Prayer xp from spectral creatures you kill", "250 zeal", 0,
					ItemID.SOUL_WARS_ECTOPLASMATOR))
				.reward(Reward.item("Soul cape", "A cape with +4 Prayer, upgradeable with an Ava's device", "2,500 zeal",
					0, ItemID.SOUL_CAPE_BLUE, ItemID.SOUL_CAPE_RED))
				.reward(Reward.also("Spoils of war", "Random supplies for 30 zeal", ItemID.SOUL_WARS_SPOILS))
				.reward(Reward.also("Lil' creator", "Pet from the spoils of war", ItemID.SOULWARSPET_BLUE,
					ItemID.SOULWARSPET_RED))
				.build(),

			Minigame.builder()
				.id("volcanic_mine")
				.name("Volcanic Mine")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.MINING)
				.summary("A 10-minute team mining game inside the Fossil Island volcano: mine lava chunks, keep the "
					+ "vents' pressure steady and break the boulder before the volcano erupts.")
				.wikiPage("Volcanic_Mine")
				.location(VOLCANIC_MINE)
				.getThere("Use a digsite pendant (Fossil Island) or the barge at the Digsite, then walk to the "
					+ "north-east of the island and up the volcano.")
				.team("Team game (solo works but gives fewer points)")
				.skill(Skill.MINING, 50)
				.quest(Quest.BONE_VOYAGE)
				.item(DiaryItem.tool(ToolType.PICKAXE))
				.item(DiaryItem.of("Numulite (Pete's entry fee)", 30, ItemID.FOSSIL_NUMULITE))
				.stage(DiaryStage.builder()
					.title("Pay Petrified Pete and go in")
					.at(VOLCANIC_MINE)
					.line("1. Bring only your pickaxe (worn or carried) and the 30 numulite; prayer and food "
						+ "aren't needed. Bank the rest at the bank chest next to Pete.")
					.line("2. Click Petrified Pete (outlined), pay the entry fee and go into the mine. Paying "
						+ "3,000 numulite once lets you in for free forever.")
					.npc("Petrified Pete")
					.object("Bank chest")
					.build())
				.stage(DiaryStage.builder()
					.title("Mine until the boulder")
					.started(true)
					.line("1. Mine the lava rocks (the chunks) on the platforms. Don't stand in the lava.")
					.line("2. Watch the vent pressure on the screen: if a vent is low, block it (click it); if it's "
						+ "high, unblock it. Each block or unblock gives points.")
					.line("3. When the boulder rolls along the channel, mine it with everyone. The boulder gives "
						+ "most of the points.")
					.line("4. Leave by the rope before the volcano erupts (the timer at the top). If you die or "
						+ "teleport out you get nothing.")
					.line("5. Back outside Pete gives you Mining xp and points to spend in his shop.")
					.build())
				.reward(Reward.item("Prospector helmet", "+2.5% Mining xp with the full Prospector kit (same as the "
					+ "Motherlode one)", "26,000 points", 0, ItemID.MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_HAT_GOLD))
				.reward(Reward.item("Prospector jacket", "Part of the Prospector kit", "39,000 points", 0,
					ItemID.MOTHERLODE_REWARD_TOP, ItemID.MOTHERLODE_REWARD_TOP_GOLD))
				.reward(Reward.item("Prospector legs", "Part of the Prospector kit", "34,000 points", 0,
					ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_LEGS_GOLD))
				.reward(Reward.item("Prospector boots", "Part of the Prospector kit", "21,000 points", 0,
					ItemID.MOTHERLODE_REWARD_BOOTS, ItemID.MOTHERLODE_REWARD_BOOTS_GOLD))
				.reward(Reward.other("Fast Mining xp", "From 70 Mining it's 65,000-90,000 xp an hour with a team"))
				.reward(Reward.also("Ore packs and ores", "Points buy runite and other ore"))
				.reward(Reward.also("Ash covered tome", "40,000 points; part of the volcanic ash farming upgrade",
					ItemID.FOSSIL_MINE_ULTRASOIL_BOOK))
				.build(),

			Minigame.builder()
				.id("mastering_mixology")
				.name("Mastering Mixology")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.HERBLORE)
				.summary("Turn herbs into pastes and mix them into potion orders in a lab under Aldarin. Great "
					+ "Herblore xp for cheap herbs, and resin buys the Alchemist's outfit and the reagent pouch.")
				.wikiPage("Mastering_Mixology")
				.location(MIXOLOGY)
				.getThere("Teleport to Aldarin (quetzal, or the Minigame Teleport) and take the stairs down in "
					+ "the lab building.")
				.skill(Skill.HERBLORE, 60)
				.unboostable(Skill.HERBLORE)
				.quest(Quest.CHILDREN_OF_THE_SUN)
				.item(DiaryItem.anyOf("Cheap clean herbs (guam or tarromin)", 27, ItemID.GUAM_LEAF, ItemID.GUAM_LEAF,
					ItemID.MARENTILL, ItemID.TARROMIN, ItemID.HARRALANDER))
				.points(Points.varp("mox resin", VarPlayerID.MIXOLOGY_MOX_POINTS))
				.points(Points.varp("aga resin", VarPlayerID.MIXOLOGY_AGA_POINTS))
				.points(Points.varp("lye resin", VarPlayerID.MIXOLOGY_LYE_POINTS))
				.stage(DiaryStage.builder()
					.title("Make pastes, then fill the orders")
					.at(MIXOLOGY)
					.inside(MIXOLOGY_LAB)
					.line("1. Put your clean herbs in the refiner next to the bank chest (outlined). Guam, "
						+ "marrentill, tarromin and harralander make mox paste; ranarr, toadflax and others make "
						+ "lye; irit and the rest make aga.")
					.line("2. Click the hopper (outlined) to add the pastes.")
					.line("3. The board top left shows three potion orders. Pull the mox, aga or lye levers "
						+ "in the order the potion needs, then take the potion from the mixing vessel.")
					.line("4. Finish it at the station the order says: retort (concentrate), agitator "
						+ "(homogenise) or alembic (crystalise). Clicking the station again speeds it up.")
					.line("5. Put the finished potion on the conveyor belt. Do all three orders, then repeat.")
					.line("6. Spend the resin with the shop NPC by the entrance; get the reagent pouch and "
						+ "the outfit first.")
					.object("Refiner")
					.object("Hopper")
					.object("Mox lever")
					.object("Aga lever")
					.object("Lye lever")
					.object("Mixing vessel")
					.object("Retort")
					.object("Agitator")
					.object("Alembic")
					.object("Conveyor belt")
					.object("Bank chest")
					.build())
				.reward(Reward.item("Prescription goggles", "Alchemist's outfit: chance to save secondary "
					+ "ingredients when making potions", null, 0, ItemID.MM_ALCHEMIST_HAT, ItemID.MM_ALCHEMIST_HAT_ALT))
				.reward(Reward.item("Alchemist labcoat", "Part of the Alchemist's outfit", null, 0,
					ItemID.MM_ALCHEMIST_BODY, ItemID.MM_ALCHEMIST_BODY_ALT))
				.reward(Reward.item("Alchemist pants", "Part of the Alchemist's outfit", null, 0,
					ItemID.MM_ALCHEMIST_LEGS, ItemID.MM_ALCHEMIST_LEGS_ALT))
				.reward(Reward.item("Alchemist gloves", "Part of the Alchemist's outfit", null, 0,
					ItemID.MM_ALCHEMIST_GLOVES))
				.reward(Reward.item("Reagent pouch", "Holds herblore secondaries", null, 0, ItemID.MM_SECONDARY_POUCH))
				.reward(Reward.item("Alchemist's amulet", "Amulet of chemistry upgrade that never breaks", null, 0,
					ItemID.AMULET_OF_CHEMISTRY_IMBUED_CHARGED))
				.build(),

			Minigame.builder()
				.id("vale_totems")
				.name("Vale Totems")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.FLETCHING)
				.summary("Build and carve totems around the Auburn Valley in Varlamore and decorate them with "
					+ "fletched bows; ents leave offerings at finished totems. AFK-friendly Fletching xp.")
				.wikiPage("Vale_Totems")
				.location(AUBURNVALE)
				.getThere("Use a quetzal to Auburnvale (Varlamore), or walk there from Civitas illa Fortis.")
				.skill(Skill.FLETCHING, 20)
				.quest(Quest.VALE_TOTEMS)
				.item(DiaryItem.anyOf("Knife (or fletching knife)", 1, ItemID.KNIFE, ItemID.FLETCHING_KNIFE, ItemID.KNIFE))
				.item(DiaryItem.tool(ToolType.AXE))
				.stage(DiaryStage.builder()
					.title("Build, carve and decorate totems")
					.at(AUBURNVALE)
					.line("1. Chop logs nearby (oak at 20 Fletching, willow 35, maple 50, yew 65, magic 80, "
						+ "redwood 90). Each totem takes 5 logs.")
					.line("2. Click a totem site (outlined) and build the totem with one log.")
					.line("3. Look at the 3 spirit animals around the site and carve those animals on the totem: "
						+ "wrong animals give less xp and fewer offerings.")
					.line("4. Fletch the other 4 logs with your knife and use them on the totem to decorate it.")
					.line("5. Go round all 8 sites (the totem icons on the minimap). Ents visit finished totems "
						+ "and leave offerings: pick them up when you pass.")
					.line("6. Every 100 offerings, rummage them for a reward and a research point to spend with "
						+ "Ranulph in western Auburnvale.")
					.npc("Ranulph")
					.object("Totem site")
					.object("Offerings")
					.build())
				.reward(Reward.item("Fletching knife", "Fletch faster than with a normal knife", "350 points", 0,
					ItemID.FLETCHING_KNIFE))
				.reward(Reward.item("Bow string spool", "Holds your bow strings in one slot", "250 points", 0,
					ItemID.BOWSTRING_SPOOL))
				.reward(Reward.also("Greenman mask", "Cosmetic", ItemID.GREENMAN_MASK))
				.build(),

			Minigame.builder()
				.id("tears_of_guthix")
				.name("Tears of Guthix")
				.category(MinigameCategory.SKILLING)
				.summary("Once a week, collect blue tears in Juna's cave for free xp in your lowest skill. Takes "
					+ "a minute or two.")
				.wikiPage("Tears_of_Guthix_(minigame)")
				.location(JUNA)
				.getThere("A games necklace has a Tears of Guthix teleport right next to Juna. Otherwise go "
					+ "through the Lumbridge Swamp caves (bring a light source).")
				.quest(Quest.TEARS_OF_GUTHIX)
				.note("Once a week, and you need 1 more quest point than last time or 100,000 xp gained since")
				.stage(DiaryStage.builder()
					.title("Talk to Juna and collect the blue tears")
					.at(JUNA)
					.line("1. Click Juna (outlined) and choose Tell-story; she gives you a stone bowl.")
					.line("2. Walk along the wall and click the streams of blue tears. Avoid the green ones: they "
						+ "take tears away. Skilling Helper's Tears of Guthix overlay marks them if it's on.")
					.line("3. When the time runs out you leave the cave and get xp in your lowest skill for the "
						+ "tears you collected.")
					.npc("Juna")
					.build())
				.reward(Reward.other("Free xp in your lowest skill", "Every week; best for slow skills like Runecraft "
					+ "or Agility"))
				.build()
		);
	}
}
