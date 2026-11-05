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
 * Desert diary, every tier. Task wording and requirements from the OSRS Wiki, listed in walking order.
 */
final class DesertDiary
{
	/**
	 * Done bit per wiki task number across both Desert diary varps (32 on is the second).
	 */
	private static final Map<DiaryTier, int[]> BITS = Map.of(
		DiaryTier.EASY, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
		DiaryTier.MEDIUM, new int[]{12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23},
		DiaryTier.HARD, new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33},
		DiaryTier.ELITE, new int[]{34, 36, 37, 38, 39, 40});

	private static final String HEAT = "Bring a few full waterskins: the desert heat hurts without them.";
	private static final Location SHANTAY = Location.of("Shantay Pass", 3304, 3116, 0, 3);
	private static final Location KALPHITE = Location.of("Kalphite Lair entrance, west of Shantay Pass", 3228, 3109, 0, 2);
	private static final Zone KALPHITE_LAIR = Zone.anyFloor(3454, 9473, 3520, 9531);
	private static final Location NARDAH = Location.of("Nardah", 3425, 2906, 0, 6);
	private static final Location PLUNDER = Location.of("Jalsavrah (Pyramid Plunder), Sophanem", 3289, 2800, 0, 2);
	private static final Zone PLUNDER_LOBBY = Zone.anyFloor(1926, 4419, 1976, 4465);
	private static final Zone PLUNDER_ROOMS = Zone.of(1922, 4417, 1984, 4480, 0);
	private static final DiaryItem ROPE = DiaryItem.of("Rope", ItemID.ROPE);

	private DesertDiary()
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

	private static DiaryStage kalphiteEntrance()
	{
		return simple("Climb into the Kalphite Lair", KALPHITE, null, "Tunnel entrance",
			"1. The entrance is west of the Shantay Pass, through the gate. " + HEAT,
			"2. Use the Rope on the Tunnel entrance (outlined) the first time, then Climb-down.");
	}

	private static DiaryStage plunderEntrance()
	{
		return simple("Go into the Pyramid Plunder pyramid", PLUNDER, null, "Pyramid Entrance",
			"1. The pyramid is in Sophanem, south of the desert. Search any of its doors (outlined).",
			"2. The door with the Guardian mummy inside is the right one; the others are empty, so try the next.");
	}

	private static DiaryStage plunderStart()
	{
		return DiaryStage.builder()
			.title("Ask the Guardian mummy to start")
			.inside(PLUNDER_LOBBY)
			.npc("Guardian mummy")
			.line("1. Talk to the Guardian mummy (outlined) and say you want to start the game.")
			.line("The Skilling Helper's Pyramid Plunder guide shows every click inside if you have it on.")
			.build();
	}

	private static void easy(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.EASY;
		out.add(task(t, 11, "Carpet to Pollnivneach", "Travel from the Shantay Pass to Pollnivneach by Magic Carpet.")
			.item(DiaryItem.of("Coins", 205, ItemID.COINS))
			.stage(simple("Ride the magic carpet to Pollnivneach", Location.of("Rug merchant, Shantay Pass", 3310, 3108, 0, 3),
				"Rug Merchant", null,
				"1. Buy a Shantay pass from Shantay (5 coins) and go through the pass.",
				"2. Talk to the Rug Merchant (outlined) just south and fly to Pollnivneach (200 coins)."))
			.build());
		out.add(task(t, 10, "Cactus water", "Cut a desert cactus open to fill a waterskin.")
			.item(DiaryItem.anyOf("Empty waterskin", 1, ItemID.WATER_SKIN0, ItemID.WATER_SKIN0))
			.item(DiaryItem.of("Knife", ItemID.KNIFE))
			.stage(simple("Cut a cactus with an empty waterskin", Location.of("Cacti south of the Shantay Pass", 3290, 3103, 0, 4),
				null, "Cactus",
				"1. Drink or empty one waterskin first so it isn't full.",
				"2. Click Cut on a Cactus (outlined) south of the pass. Try again if it fails."))
			.build());
		out.add(task(t, 4, "Desert robes", "Enter the Desert with a set of Desert robes equipped.")
			.item(DiaryItem.of("Desert shirt", ItemID.DESERT_SHIRT))
			.item(DiaryItem.of("Desert robe", ItemID.DESERT_ROBE))
			.item(DiaryItem.of("Desert boots", ItemID.DESERT_BOOTS))
			.stage(simple("Walk into the desert in desert robes", SHANTAY, null, "Shantay pass",
				"1. Wear the Desert shirt, robe and boots.",
				"2. Go through the Shantay pass (a pass costs 5 coins from Shantay)."))
			.build());
		out.add(task(t, 3, "Enter the Kalphite Hive", "Enter the Kalphite Hive.")
			.item(ROPE)
			.stage(kalphiteEntrance())
			.build());
		out.add(task(t, 7, "5 potato cacti", "Collect 5 Potato Cactus from the Kalphite Hive.")
			.item(ROPE)
			.item(DiaryItem.of("Potato cactus", ItemID.CACTUS_POTATO))
			.note("Antipoison and food (kalphites hit hard)")
			.stage(kalphiteEntrance())
			.stage(DiaryStage.builder()
				.title("Pick up a potato cactus 5 times")
				.inside(KALPHITE_LAIR)
				.line("1. Drop the Potato cactus you brought and pick it up again, 5 times.")
				.build())
			.build());
		out.add(task(t, 1, "Golden warbler", "Catch a Golden Warbler.")
			.skill(Skill.HUNTER, 5)
			.item(DiaryItem.of("Bird snare", ItemID.HUNTING_OJIBWAY_BIRD_SNARE))
			.stage(simple("Catch a golden warbler", Location.of("Uzer hunter area", 3404, 3148, 0, 6), "Golden warbler", null,
				"1. The warblers live in the desert east of the Shantay Pass, north-west of Uzer. " + HEAT,
				"2. Lay the Bird snare on the ground and wait for a Golden warbler to fly in, then check it."))
			.build());
		out.add(task(t, 2, "5 clay in the north-east desert", "Mine 5 clay in the north-eastern desert.")
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Mine 5 clay near Uzer", Location.of("Clay rocks north of Uzer", 3420, 3163, 0, 4), null, "Clay rocks",
				"1. The clay rocks are north of Uzer. " + HEAT,
				"2. Mine the Clay rocks (outlined) until you have 5 clay."))
			.build());
		out.add(task(t, 6, "Nardah herbalist", "Have the Nardah Herbalist clean a Herb for you.")
			.item(DiaryItem.anyOf("Any grimy herb", 1, ItemID.UNIDENTIFIED_GUAM, ItemID.UNIDENTIFIED_GUAM))
			.item(DiaryItem.of("Coins", 200, ItemID.COINS))
			.stage(simple("Ask Zahur to clean the herb", NARDAH, "Zahur", null,
				"1. Zahur stands by the fountain in Nardah.",
				"2. Talk to Zahur (outlined) and ask him to clean the herb (200 coins). Don't use the herb on him."))
			.build());
		out.add(task(t, 5, "Kill a vulture", "Kill a Vulture.")
			.note("A ranged or magic attack helps")
			.stage(simple("Kill a vulture", Location.of("Vultures north of Sophanem", 3334, 2865, 0, 6), "Vulture", null,
				"1. Vultures circle north of Sophanem. " + HEAT,
				"2. Kill one (outlined)."))
			.build());
		out.add(task(t, 8, "Sell artefacts to Simon", "Sell some artefacts to Simon Templeton.")
			.questStarted(Quest.ICTHLARINS_LITTLE_HELPER)
			.item(DiaryItem.owned("Pyramid Plunder artefact", "Loot one from an urn or chest in Pyramid Plunder.",
				ItemID.NTK_IVORY_COMB, ItemID.NTK_SCARAB_POTTERY, ItemID.NTK_STATUETTE_POTTERY, ItemID.NTK_SCARAB_STONE,
				ItemID.NTK_STATUETTE_STONE, ItemID.NTK_SEAL_STONE, ItemID.NTK_SCARAB_GOLD, ItemID.NTK_STATUETTE_GOLD,
				ItemID.NTK_SEAL_GOLD))
			.stage(simple("Sell an artefact to Simon Templeton", Location.of("Simon Templeton, Agility Pyramid", 3346, 2827, 0, 3),
				"Simon Templeton", null,
				"1. Simon is at the Agility Pyramid camp south of Pollnivneach.",
				"2. Use an artefact on Simon (outlined) and sell it. Pyramid tops don't count."))
			.build());
		out.add(task(t, 9, "First sarcophagus", "Open the Sarcophagus in the first room of Pyramid Plunder.")
			.questStarted(Quest.ICTHLARINS_LITTLE_HELPER)
			.skill(Skill.THIEVING, 21)
			.stage(plunderEntrance())
			.stage(plunderStart())
			.stage(DiaryStage.builder()
				.title("Open the sarcophagus")
				.inside(PLUNDER_ROOMS)
				.at(Location.of("Sarcophagus, first room", 1928, 4465, 0, 3))
				.object("Sarcophagus")
				.line("1. In the first room, click Open on the Sarcophagus (outlined). A mummy may come out.")
				.build())
			.build());
	}

	private static void medium(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.MEDIUM;
		out.add(task(t, 10, "Visit the Genie", "Visit the Genie.")
			.questStarted(Quest.SPIRITS_OF_THE_ELID)
			.item(ROPE)
			.item(DiaryStages.LIGHT_SOURCE)
			.stage(simple("Climb into the Genie's cave", Location.of("Cave west of Nardah", 3374, 2904, 0, 2), null, "Crevice",
				"1. Light your light source. Tie the rope to the crevice (outlined) west of Nardah and climb down."))
			.stage(DiaryStage.builder()
				.title("Talk to the Genie")
				.inside(Zone.of(3367, 9298, 3378, 9324, 0))
				.npc("Genie")
				.line("1. Walk to the Genie (outlined) and talk to it.")
				.build())
			.build());
		out.add(task(t, 7, "Elidinis statuette", "Pray at the Elidinis statuette in Nardah.")
			.quest(Quest.SPIRITS_OF_THE_ELID)
			.note("Prayer points below full")
			.stage(simple("Pray at the Elidinis statuette", Location.of("Elidinis shrine, Nardah", 3427, 2930, 0, 3), null,
				"Elidinis Statuette",
				"1. Use a little prayer first.",
				"2. Click Pray-at on the statuette (outlined) in the shrine in north Nardah."))
			.build());
		out.add(task(t, 11, "Redirected house tablet", "Teleport to Pollnivneach with a redirected teleport to house tablet.")
			.skill(Skill.CONSTRUCTION, 20)
			.item(DiaryItem.owned("Pollnivneach house teleport", "Use a Scroll of redirection (Nightmare Zone, or the "
				+ "Construction shop in some houses) on a Teleport to house tablet.", ItemID.POH_TABLET_TELEPORTTOHOUSE))
			.stage(DiaryStage.builder()
				.title("Break the redirected tablet")
				.line("1. Use the Scroll of redirection on a Teleport to house tablet to make a Pollnivneach teleport.")
				.line("2. Click Break on it.")
				.build())
			.build());
		out.add(task(t, 1, "Agility Pyramid summit", "Climb to the summit of the Agility Pyramid.")
			.skill(Skill.AGILITY, 30)
			.stage(simple("Climb the Agility Pyramid", Location.of("Agility Pyramid", 3355, 2832, 0, 3), null, "Stairs",
				"1. The pyramid is south of Pollnivneach. " + HEAT,
				"2. Climb the Stairs (outlined) and cross the obstacles up every level to the top.",
				"3. Reaching the summit (grab the pyramid top) finishes it."))
			.build());
		out.add(task(t, 2, "Desert lizard", "Slay a desert lizard.")
			.skill(Skill.SLAYER, 22)
			.item(DiaryItem.owned("Ice cooler", "Buy them from any Slayer master's shop.", ItemID.SLAYER_ICY_WATER))
			.stage(simple("Kill a desert lizard", Location.of("Desert lizards, south of Uzer", 3437, 3067, 0, 6), "Lizard",
				null,
				"1. Lizards live in the desert south-east of the Shantay Pass, near Uzer. " + HEAT,
				"2. Fight one (outlined); when it's at low health, use an Ice cooler on it to finish it."))
			.build());
		out.add(task(t, 4, "Phoenix feather", "Steal a feather from the Desert Phoenix.")
			.skill(Skill.THIEVING, 25)
			.stage(simple("Steal a phoenix feather", Location.of("Desert Phoenix, north of Uzer", 3417, 3154, 0, 5),
				"Desert Phoenix", null,
				"1. The Desert Phoenix flies north of Uzer.",
				"2. Right-click it (outlined) and choose Steal-from."))
			.build());
		out.add(task(t, 3, "Orange salamander", "Catch an Orange Salamander.")
			.skill(Skill.HUNTER, 47)
			.item(ROPE)
			.item(DiaryItem.of("Small fishing net", ItemID.NET))
			.stage(simple("Catch an orange salamander", Location.of("Orange salamanders, Uzer", 3404, 3134, 0, 5),
				"Orange salamander", "Young tree",
				"1. Set a net trap on a Young tree (outlined) north-west of Uzer.",
				"2. Wait for an Orange salamander to get caught, then check it."))
			.build());
		out.add(task(t, 5, "Carpet to Uzer", "Travel to Uzer via Magic Carpet.")
			.quest(Quest.THE_GOLEM)
			.item(DiaryItem.of("Coins", 205, ItemID.COINS))
			.stage(simple("Ride the magic carpet to Uzer", Location.of("Rug merchant, Shantay Pass", 3310, 3108, 0, 3),
				"Rug Merchant", null,
				"1. Talk to the Rug Merchant (outlined) south of the Shantay Pass and fly to Uzer."))
			.build());
		out.add(task(t, 12, "Teak near Uzer", "Chop some Teak Logs near Uzer.")
			.skill(Skill.WOODCUTTING, 35)
			.item(DiaryItem.tool(ToolType.AXE))
			.stage(simple("Chop a teak near Uzer", Location.of("Teak trees north-east of the Uzer oasis", 3510, 3073, 0, 4),
				null, "Teak tree",
				"1. Chop a Teak tree (outlined) north-east of the Uzer oasis."))
			.build());
		out.add(task(t, 8, "Combat potion in the desert", "Create a combat potion in the desert.")
			.skill(Skill.HERBLORE, 36)
			.item(DiaryItem.of("Harralander potion (unf)", ItemID.HARRALANDERVIAL))
			.item(DiaryItem.of("Goat horn dust", ItemID.GROUND_DESERT_GOAT_HORN))
			.stage(DiaryStage.builder()
				.title("Mix a combat potion in the open desert")
				.at(Location.of("Desert south of the Shantay Pass", 3300, 3090, 0, 8))
				.use(ItemID.GROUND_DESERT_GOAT_HORN).use(ItemID.HARRALANDERVIAL)
				.line("1. Stand in the open desert, not in a town.")
				.line("2. Use the Goat horn dust on the Harralander potion (unf) (both outlined).")
				.build())
			.build());
		out.add(task(t, 9, "Camulet teleport", "Teleport to Enakhra's Temple with the Camulet.")
			.quest(Quest.ENAKHRAS_LAMENT)
			.item(DiaryItem.owned("Camulet", "Reward from Enakhra's Lament; Lazim at the temple makes another.",
				ItemID.CAMULET))
			.stage(DiaryStage.builder()
				.title("Rub the Camulet")
				.use(ItemID.CAMULET)
				.line("1. Right-click the Camulet (outlined) and choose Rub.")
				.build())
			.build());
		out.add(task(t, 6, "Eagle to the desert", "Travel to the Desert via Eagle.")
			.quest(Quest.EAGLES_PEAK)
			.item(ROPE)
			.stage(simple("Go into the Eagles' Peak cave", Location.of("Eagles' Peak cave entrance", 2329, 3495, 0, 2), null,
				"Cave entrance", "1. Enter the cave (outlined) at the top of Eagles' Peak, north-west of Tree Gnome Stronghold."))
			.stage(DiaryStage.builder()
				.title("Ride the desert eagle")
				.inside(Zone.anyFloor(1986, 4944, 2030, 4985))
				.at(Location.of("Desert eagle, Eagles' Peak", 2027, 4964, 3, 3))
				.npc("Desert eagle")
				.line("1. Use the Rope on the Desert eagle (outlined) to fly to the desert.")
				.build())
			.build());
	}

	private static void hard(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.HARD;
		out.add(task(t, 1, "Menaphite thug", "Knock out and pickpocket a Menaphite Thug.")
			.questStarted(Quest.THE_FEUD)
			.skill(Skill.THIEVING, 65)
			.item(DiaryItem.anyOf("Any blackjack", 1, ItemID.BLACKJACK_OAK, ItemID.ROGUETRADER_BJ_MAPLE,
				ItemID.ROGUETRADER_BJ_ASSAULT_MAPLE, ItemID.ROGUETRADER_BJ_DEFEND_MAPLE, ItemID.BLACKJACK_WILLOW,
				ItemID.ROGUETRADER_BJ_ASSAULT_WILLOW, ItemID.ROGUETRADER_BJ_DEFEND_WILLOW, ItemID.BLACKJACK_OAK,
				ItemID.ROGUETRADER_BJ_ASSAULT_OAK, ItemID.ROGUETRADER_BJ_DEFEND_OAK))
			.stage(simple("Knock out and pickpocket a Menaphite Thug", Location.of("Menaphite thugs, Pollnivneach", 3347, 2959, 0, 5),
				"Menaphite Thug", null,
				"1. Wield the blackjack. Menaphite Thugs stand in south-east Pollnivneach.",
				"2. Right-click a Thug (outlined), choose Knock-Out, then Pickpocket it straight away."))
			.build());
		out.add(task(t, 5, "Pollnivneach rooftops", "Complete a lap of the Pollnivneach agility course.")
			.skill(Skill.AGILITY, 70)
			.stage(simple("Run a lap of the Pollnivneach rooftops", Location.of("Pollnivneach rooftop course start", 3351, 2962, 0, 2),
				null, "Basket",
				"1. Climb onto the Basket (outlined) in north Pollnivneach.",
				"2. Cross each obstacle until you drop off the last one."))
			.build());
		out.add(task(t, 2, "Mine granite", "Mine some Granite.")
			.skill(Skill.MINING, 45)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(simple("Mine granite in the quarry", Location.of("Desert quarry", 3167, 2911, 0, 5), null, "Rocks",
				"1. The quarry is south-west of the Bedabin camp. " + HEAT,
				"2. Mine the granite Rocks (outlined) until you get Granite."))
			.build());
		out.add(task(t, 9, "Yew logs on the mayor's balcony", "Burn some yew logs on the Nardah Mayor's balcony.")
			.skill(Skill.FIREMAKING, 60)
			.item(DiaryItem.of("Yew logs", ItemID.YEW_LOGS))
			.item(DiaryItem.of("Tinderbox", ItemID.TINDERBOX))
			.stage(simple("Go up to the mayor's balcony", Location.of("Mayor's house stairs, Nardah", 3447, 2912, 0, 2), null,
				"Staircase", "1. The mayor lives just east of the fountain. Climb-up the Staircase (outlined)."))
			.stage(DiaryStage.builder()
				.title("Burn the yew logs on the balcony")
				.inside(Zone.of(3436, 2907, 3452, 2924, 1))
				.use(ItemID.TINDERBOX).use(ItemID.YEW_LOGS)
				.line("1. Step out onto the balcony and use the Tinderbox on the Yew logs (both outlined).")
				.build())
			.build());
		out.add(task(t, 10, "Mithril platebody in Nardah", "Create a Mithril Platebody in Nardah.")
			.skill(Skill.SMITHING, 68)
			.item(DiaryItem.of("Mithril bar", 5, ItemID.MITHRIL_BAR))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.stage(simple("Smith a mithril platebody in Nardah", Location.of("Anvil, Nardah", 3409, 2921, 0, 2), null, "Anvil",
				"1. The anvil is in the westernmost building of Nardah.",
				"2. Use the Mithril bars on the Anvil (outlined) and pick Platebody."))
			.build());
		out.add(task(t, 3, "Humidify in the desert", "Refill your waterskins in the Desert using Lunar magic.")
			.quest(Quest.DREAM_MENTOR)
			.skill(Skill.MAGIC, 68)
			.note("Lunar spellbook selected")
			.item(DiaryItem.anyOf("Waterskin (not full)", 1, ItemID.WATER_SKIN0, ItemID.WATER_SKIN0))
			.item(DiaryItem.of("Astral rune", 1, ItemID.ASTRALRUNE))
			.item(DiaryItem.of("Fire rune", 1, ItemID.FIRERUNE))
			.item(DiaryItem.of("Water rune", 3, ItemID.WATERRUNE))
			.stage(DiaryStage.builder()
				.title("Cast Humidify in the open desert")
				.at(Location.of("Desert south of the Shantay Pass", 3300, 3090, 0, 8))
				.line("1. Stand in the open desert, away from towns.")
				.line("2. Cast Humidify from the Lunar spellbook.")
				.build())
			.build());
		out.add(task(t, 7, "Ancient Magicks", "Activate Ancient Magicks at the altar in the Jaldraocht Pyramid.")
			.quest(Quest.DESERT_TREASURE_I)
			.stage(simple("Go into the Jaldraocht Pyramid", Location.of("Jaldraocht Pyramid, west desert", 3233, 2889, 0, 2),
				null, "Doorway", "1. Enter the pyramid (outlined back door) west of the desert. " + HEAT))
			.stage(DiaryStage.builder()
				.title("Pray at the altar")
				.inside(Zone.of(3199, 9282, 3265, 9340, 0))
				.at(Location.of("Altar, Jaldraocht Pyramid", 3233, 9311, 0, 3))
				.object("Altar")
				.line("1. Click Pray-at on the Altar (outlined) to switch to Ancient Magicks.")
				.build())
			.build());
		out.add(task(t, 6, "Dust devil in a slayer helmet", "Slay a Dust Devil in the desert cave with a Slayer helmet equipped.")
			.questStarted(Quest.DESERT_TREASURE_I)
			.skill(Skill.SLAYER, 65).skill(Skill.DEFENCE, 10)
			.item(DiaryItem.owned("Slayer helmet", "Unlock it with 400 slayer points, then make it at 55 Crafting.",
				ItemID.SLAYER_HELM_I, ItemID.SLAYER_HELM, ItemID.SLAYER_HELM_I_BLACK, ItemID.SLAYER_HELM_BLACK,
				ItemID.SLAYER_HELM_I_GREEN, ItemID.SLAYER_HELM_GREEN, ItemID.SLAYER_HELM_I_RED, ItemID.SLAYER_HELM_RED,
				ItemID.SLAYER_HELM_I_PURPLE, ItemID.SLAYER_HELM_PURPLE, ItemID.SLAYER_HELM_I_TURQUOISE,
				ItemID.SLAYER_HELM_TURQUOISE, ItemID.SLAYER_HELM_I_HYDRA, ItemID.SLAYER_HELM_HYDRA,
				ItemID.SLAYER_HELM_I_TWISTED, ItemID.SLAYER_HELM_TWISTED, ItemID.SLAYER_HELM_I_JAD, ItemID.SLAYER_HELM_JAD,
				ItemID.SLAYER_HELM_I_VERZIK, ItemID.SLAYER_HELM_VERZIK, ItemID.SLAYER_HELM_I_ZUK, ItemID.SLAYER_HELM_ZUK,
				ItemID.SLAYER_HELM_I_ARAXYTE, ItemID.SLAYER_HELM_ARAXYTE, ItemID.SLAYER_HELM_I_HOODED,
				ItemID.SLAYER_HELM_HOODED))
			.stage(simple("Climb down the Smoke Well", Location.of("Smoke Well, Pollnivneach", 3310, 2962, 0, 2), null, "Well",
				"1. The Smoke Well is in west Pollnivneach. Wear the Slayer helmet and climb down."))
			.stage(DiaryStage.builder()
				.title("Kill a dust devil")
				.inside(Zone.of(3166, 9344, 3332, 9408, 0))
				.npc("Dust devil")
				.line("1. With the Slayer helmet on, kill a Dust devil (outlined).")
				.build())
			.build());
		out.add(task(t, 8, "Locust rider with Keris", "Defeat a Locust Rider with Keris.")
			.questStarted(Quest.CONTACT)
			.skill(Skill.ATTACK, 50)
			.item(DiaryItem.owned("Keris", "Reward from Contact!; Osman or a weapon store in Sophanem sells a new one.",
				ItemID.KERIS_PARTISAN_AMASCUT, ItemID.KERIS_PARTISAN_SUN, ItemID.KERIS_PARTISAN_CORRUPTION,
				ItemID.KERIS_PARTISAN_BREACH, ItemID.KERIS_PARTISAN, ItemID.CONTACT_KERIS_P__, ItemID.CONTACT_KERIS_P_,
				ItemID.CONTACT_KERIS_P, ItemID.CONTACT_KERIS))
			.item(DiaryStages.LIGHT_SOURCE)
			.stage(simple("Climb into the Sophanem dungeon", Location.of("Temple trapdoor, Sophanem", 3315, 2797, 0, 2), null,
				"Trapdoor", "1. Light your light source. Go down the Trapdoor (outlined) in the Sophanem temple."))
			.stage(DiaryStage.builder()
				.title("Kill a locust rider")
				.inside(Zone.anyFloor(2110, 4340, 2180, 4415))
				.npc("Locust rider")
				.line("1. Find a Locust rider (outlined). Damage it with anything.")
				.line("2. Wield the Keris for the final hit so it dies to the Keris.")
				.build())
			.build());
		out.add(task(t, 4, "Kalphite Queen", "Kill the Kalphite Queen.")
			.item(ROPE)
			.note("Gear and food for a boss fight (see the wiki strategy guide)")
			.stage(kalphiteEntrance())
			.stage(DiaryStage.builder()
				.title("Go into the Queen's chamber and kill her")
				.inside(KALPHITE_LAIR)
				.at(Location.of("Kalphite Queen's tunnel", 3510, 9498, 2, 3))
				.object("Tunnel entrance")
				.npc("Kalphite Queen")
				.line("1. Use a rope on the inner Tunnel entrance (outlined) and climb down to the Queen.")
				.line("2. Kill the Kalphite Queen (both forms).")
				.build())
			.build());
	}

	private static void elite(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.ELITE;
		out.add(task(t, 1, "Wild pie in Nardah", "Bake a wild pie at the Nardah Clay Oven.")
			.skill(Skill.COOKING, 85)
			.item(DiaryItem.of("Raw wild pie", ItemID.UNCOOKED_WILD_PIE))
			.stage(simple("Bake the wild pie in Nardah", Location.of("Clay oven, Nardah", 3434, 2886, 0, 2), null, "Clay oven",
				"1. Use the Raw wild pie on the Clay oven (outlined) in south Nardah."))
			.build());
		out.add(task(t, 3, "Dragon darts at the Bedabin camp", "Fletch some Dragon darts at the Bedabin Camp.")
			.skill(Skill.FLETCHING, 95)
			.item(DiaryItem.of("Dragon dart tip", ItemID.DRAGON_DART_TIP))
			.item(DiaryItem.of("Feather", ItemID.FEATHER))
			.stage(DiaryStage.builder()
				.title("Fletch dragon darts in the Bedabin camp")
				.at(Location.of("Bedabin Camp", 3172, 3036, 0, 4))
				.inside(Zone.of(3163, 3024, 3181, 3049, 0))
				.use(ItemID.FEATHER).use(ItemID.DRAGON_DART_TIP)
				.line("1. The Bedabin camp is in the west desert. Stand inside it.")
				.line("2. Use the Feather on the Dragon dart tip (both outlined).")
				.build())
			.build());
		out.add(task(t, 6, "Sophanem altar prayer", "Restore at least 85 Prayer points when praying at the Altar in Sophanem.")
			.questStarted(Quest.ICTHLARINS_LITTLE_HELPER)
			.skill(Skill.PRAYER, 85).unboostable(Skill.PRAYER)
			.stage(simple("Pray at the Sophanem altar with 85 points missing", Location.of("Altar, Sophanem temple", 3281, 2774, 0, 3),
				null, "Altar",
				"1. Use up 85 or more Prayer points first (leave prayers on for a while).",
				"2. Click Pray-at on the Altar (outlined) in the Sophanem temple."))
			.build());
		out.add(task(t, 5, "Grand gold chest", "Steal from the Grand Gold Chest in the final room of Pyramid Plunder.")
			.questStarted(Quest.ICTHLARINS_LITTLE_HELPER)
			.skill(Skill.THIEVING, 91).unboostable(Skill.THIEVING)
			.stage(plunderEntrance())
			.stage(plunderStart())
			.stage(DiaryStage.builder()
				.title("Reach the last room and open its gold chest")
				.inside(PLUNDER_ROOMS)
				.at(Location.of("Grand gold chest, last room", 1973, 4431, 0, 3))
				.object("Grand Gold Chest").object("Tomb Door")
				.line("1. Go through each room's tomb door to the eighth room.")
				.line("2. Click Search on the Grand Gold Chest (outlined).")
				.build())
			.build());
		out.add(task(t, 2, "Ice Barrage in the desert", "Cast Ice Barrage against a foe in the Desert.")
			.quest(Quest.DESERT_TREASURE_I)
			.skill(Skill.MAGIC, 94)
			.note("Ancient Magicks spellbook selected")
			.item(DiaryItem.of("Water rune", 6, ItemID.WATERRUNE))
			.item(DiaryItem.of("Blood rune", 2, ItemID.BLOODRUNE))
			.item(DiaryItem.of("Death rune", 4, ItemID.DEATHRUNE))
			.stage(simple("Ice Barrage a vulture", Location.of("Vultures north of Sophanem", 3334, 2865, 0, 6), "Vulture", null,
				"1. Away from any town, cast Ice Barrage on a Vulture (outlined) until one hits."))
			.build());
		out.add(task(t, 4, "Talk to the KQ head", "Speak to the KQ head in your POH.")
			.quest(Quest.PRIEST_IN_PERIL)
			.skill(Skill.CONSTRUCTION, 78)
			.item(DiaryItem.owned("Stuffed KQ head", "Get a KQ head from the Kalphite Queen and pay the Varrock "
				+ "taxidermist 50,000 coins to stuff it.", ItemID.POH_TROPHYDROP_KALPHITEQUEEN))
			.item(DiaryItem.of("Mahogany plank", 2, ItemID.PLANK_MAHOGANY))
			.item(DiaryItem.of("Gold leaf", 2, ItemID.GOLD_LEAF))
			.item(DiaryItem.of("Saw", ItemID.POH_SAW))
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.stage(DiaryStage.builder()
				.title("Mount the KQ head and talk to it")
				.line("1. In your house (build mode on), build a head trophy space in your Skill Hall and mount the KQ head.")
				.line("2. Leave build mode and click Talk-to on the head.")
				.build())
			.build());
	}

	private static DiaryTask.DiaryTaskBuilder task(DiaryTier tier, int number, String name, String text)
	{
		int index = BITS.get(tier)[number - 1];
		return DiaryTask.builder()
			.region(DiaryRegion.DESERT)
			.tier(tier)
			.number(number)
			.name(name)
			.text(text)
			.varp(index < 32 ? VarPlayerID.DESERT_ACHIEVEMENT_DIARY : VarPlayerID.DESERT_ACHIEVEMENT_DIARY2)
			.bit(index % 32);
	}
}
