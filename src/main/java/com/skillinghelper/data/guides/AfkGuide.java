package com.skillinghelper.data.guides;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import com.skillinghelper.data.ToolType;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Low-effort methods from the OSRS wiki "Guide:AFK Skilling Methods" (and its combat table) that the skill guides
 * didn't have yet. They are activities with their own loop (a spot that never runs out, a minigame, an exchange)
 * rather than the plain gather-and-bank loop, so each one spells out its steps. Xp rates are the wiki's numbers,
 * which assume good gear; the "Your pace" setting scales them down.
 */
final class AfkGuide
{
	private static final String WIKI = "Guide:AFK_Skilling_Methods";
	private static final Location KRUKS_MONKEYS = Location.of("Kruk's Dungeon maniacal monkeys", 2660, 9151, 20);
	private static final Location BLAST_FURNACE = Location.of("Blast Furnace (Keldagrim)", 1948, 4963, 10);
	private static final String BLAST_FURNACE_TRIP = "The Blast Furnace is under Keldagrim. The quickest way is the "
		+ "Blast Furnace minigame teleport (grouping tab), or walk into Keldagrim and take the stairs in the "
		+ "building on the west side down. Members' world 302 (\"Blast Furnace\") always has the furnace running.";

	static List<Method> methods()
	{
		return List.of(
			// Woodcutting
			activity("wc_infected_roots", Skill.WOODCUTTING, "Infected roots (Ruins of Mokhaiotl)", 80, 25, 7_000,
				Location.of("Ruins of Mokhaiotl infected roots", 1311, 9512, 1, 25))
				.tag(MethodTag.PROFIT)
				.rateAt(99, 9_100)
				.quest(Quest.THE_FINAL_DAWN)
				.toolType(ToolType.AXE)
				.target(Target.object("Chop", "Infected root"))
				.instruction("Chop an infected root: it never runs out, so you keep chopping until your inventory is "
					+ "full of logs")
				.notes("Very little xp, but the most AFK Woodcutting there is (up to 30 minutes with a log basket) "
					+ "and the demon tears it gives sell well.")
				.getThere("The roots are at the north end of the Ruins of Mokhaiotl, under the Crypt of Tonali in "
					+ "the Tlati Rainforest (Varlamore). After The Final Dawn, a Mokhaiotl waystone teleports you "
					+ "straight inside; otherwise go through the Crypt of Tonali and down the Tonali Cavern.")
				.howTo("Bring your best axe and a log basket if you have one. Leave everything else in the bank: "
					+ "you want an empty inventory.")
				.howTo("Click a highlighted infected root (\"Chop\"). Your character keeps chopping on their own.")
				.howTo("Demon tears stack in one slot. With a log basket open, logs go into the basket first, so one "
					+ "click lasts much longer.")
				.howTo("When your inventory is full of logs, drop them (shift-click) or bank them, and click the "
					+ "root again.")
				.build(),
			activity("wc_engorged_bloodwood", Skill.WOODCUTTING, "Engorged bloodwood tree (Vampyrium)", 77, 100,
				45_000, Location.of("Engorged bloodwood tree (Vampyrium)", 2696, 7829, 8))
				.tag(MethodTag.PROFIT)
				.rateAt(90, 60_000).rateAt(99, 70_000)
				.quest(Quest.THE_BLOOD_MOON_RISES)
				.toolType(ToolType.AXE)
				.target(Target.object("Chop", "Engorged bloodwood tree"))
				.target(Target.npcContaining("Take-many", "bucket dealer"))
				.instruction("Take empty buckets from the bucket dealer and chop the engorged bloodwood tree to fill "
					+ "them with bloodwood sap")
				.notes("Gives buckets of bloodwood sap for seeking arrows. Unlike the normal bloodwood trees it "
					+ "needs no other inputs, but it gives less sap.")
				.getThere("Vampyrium needs The Blood Moon Rises. Drakan's medallion teleports you to Castle Drakan; "
					+ "the engorged bloodwood tree is south of the Ruined Village: follow the arrow.")
				.howTo("Bring your best axe and an otherwise empty inventory.")
				.howTo("Right-click the highlighted aranei bucket dealer next to the tree and pick \"Take-many\" to "
					+ "fill your inventory with empty buckets. They're free.")
				.howTo("Click the highlighted engorged bloodwood tree (\"Chop\"). Each bucket fills with sap as you "
					+ "chop, and your character keeps going on their own.")
				.howTo("Now and then a letvek crawls into a bucket: click the letvek in a bucket in your inventory "
					+ "to let it go.")
				.howTo("When every bucket is full, bank or drop the sap, take more buckets and repeat.")
				.build(),

			// Mining
			activity("mine_barronite", Skill.MINING, "Barronite rocks (Camdozaal)", 14, 16, 12_000,
				Location.of("Camdozaal mines", 2920, 5815, 15))
				.members(false).tag(MethodTag.CHEAP)
				.rateAt(50, 16_000).rateAt(99, 20_000)
				.quest(Quest.BELOW_ICE_MOUNTAIN)
				.toolType(ToolType.PICKAXE)
				.tool(ItemReq.tool("Hammer (to crush deposits)", ItemID.HAMMER, ItemID.IMCANDO_HAMMER))
				.target(Target.object("Mine", "Barronite rocks"))
				.target(Target.object(null, "Barronite Crusher"))
				.instruction("Mine barronite rocks: the shards stack, so you only stop for the odd barronite deposit")
				.notes("Free-to-play friendly and very low effort. Crush the deposits at the Barronite Crusher for "
					+ "Smithing xp and a chance at the imcando hammer and barronite mace pieces.")
				.getThere("Camdozaal is under Ice Mountain (north of Falador) and opens after Below Ice Mountain: go "
					+ "into the ruins on the mountain and down into the mines, then follow the arrow.")
				.howTo("Bring a pickaxe and a hammer, and leave the rest of your inventory empty.")
				.howTo("Click a highlighted barronite rock (\"Mine\"). Barronite shards stack in one slot, so you can "
					+ "mine for a long time without your inventory filling up.")
				.howTo("Every rock runs dry after about 20 seconds: click the next highlighted rock.")
				.howTo("Barronite deposits don't stack. When you have a handful, use them on the highlighted "
					+ "Barronite Crusher to break them open, then keep mining.")
				.build(),
			activity("mine_daeyalt", Skill.MINING, "Daeyalt essence (Darkmeyer)", 60, 5, 7_000,
				Location.of("Daeyalt essence mine (Darkmeyer)", 3680, 9757, 15))
				.tag(MethodTag.PROFIT)
				.rateAt(99, 8_850)
				.quest(Quest.SINS_OF_THE_FATHER)
				.toolType(ToolType.PICKAXE)
				.target(Target.objectContaining("Mine", "Daeyalt"))
				.target(Target.npc(null, "Noranna Tytanin"))
				.instruction("Mine the active daeyalt essence rock and trade the shards to Noranna Tytanin for daeyalt "
					+ "essence")
				.notes("Low xp, but very AFK and daeyalt essence gives 50% more Runecraft xp than normal essence.")
				.requirementText("Wear vyre noble clothing in Darkmeyer unless you've reached the end of The Blood "
					+ "Moon Rises")
				.getThere("Drakan's medallion teleports you to Darkmeyer. The daeyalt essence mine is under the city: "
					+ "follow the arrow to its entrance.")
				.howTo("Bring a pickaxe and wear your vyre noble clothes (or the guards attack you).")
				.howTo("Only one rock in the mine is active at a time. Click the highlighted daeyalt essence rock "
					+ "(\"Mine\") and your character keeps mining on their own.")
				.howTo("Every minute or so the active rock moves: when your character stops, click the new one.")
				.howTo("The shards stack. Every so often, talk to Noranna Tytanin (highlighted) near the exit to "
					+ "swap the shards for daeyalt essence, which goes straight to your bank.")
				.build(),
			activity("mine_infernal_shale", Skill.MINING, "Infernal shale (Chasm of Fire)", 78, 40, 8_000,
				Location.of("Chasm of Fire infernal shale rocks", 1457, 10090, 12))
				.tag(MethodTag.PROFIT)
				.rateAt(99, 9_860)
				.quest(Quest.A_KINGDOM_DIVIDED)
				.toolType(ToolType.PICKAXE)
				.tool(ItemReq.tool("Chisel", ItemID.CHISEL))
				.tool(ItemReq.tool("Hammer", ItemID.HAMMER, ItemID.IMCANDO_HAMMER))
				.target(Target.object("Mine", "Infernal shale rocks"))
				.instruction("Mine the infernal shale rocks and crush the shale with your hammer and chisel")
				.notes("Low xp, but the crushed shale is used by Yama's gear upgrades and sells well.")
				.requirementText("Talk to Yama in the Chasm of Fire and sign his contract to reach the rocks")
				.getThere("Xeric's talisman (\"Xeric's Inferno\") lands next to the Chasm of Fire. Climb down to the "
					+ "bottom level; the rocks are in its north-east part (sign Yama's contract first).")
				.howTo("Bring a pickaxe, a chisel and a hammer.")
				.howTo("Click a highlighted infernal shale rock (\"Mine\"). Your character keeps mining on their own.")
				.howTo("Use the chisel on the shale to crush it: crushed shale stacks, so your inventory stays "
					+ "empty and you can keep mining for a long time.")
				.build(),
			activity("mine_salax_salt", Skill.MINING, "Salt deposits (Duke Sucellus)", 72, 15, 10_000,
				Location.of("Ghorrock Prison salt deposits", 3038, 6448, 8))
				.rateAt(99, 10_600)
				.quest(Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE)
				.toolType(ToolType.PICKAXE)
				.target(Target.object("Mine", "Salt Deposit"))
				.instruction("Mine the salt deposit: it never runs out and the salt stacks")
				.notes("Very little xp, but one click keeps you mining for 5 minutes.")
				.requirementText("72 Mining (needed for Desert Treasure II)")
				.getThere("The salt deposits are in the middle of Duke Sucellus's room in the Ghorrock Prison, the "
					+ "way you went in Desert Treasure II: follow the arrow.")
				.howTo("Bring a pickaxe. You don't need to fight the Duke.")
				.howTo("Click a highlighted salt deposit (\"Mine\"). The salt stacks, so your inventory never fills.")
				.howTo("After 5 minutes the chat says your arms get tired: click the deposit again.")
				.build(),
			activity("mine_sunstone_monolith", Skill.MINING, "Sunstone monolith (Wyrmscraig)", 53, 30, 6_000,
				Location.of("Wyrmscraig mine sunstone monolith", 2600, 2248, 6))
				.rateAt(99, 8_000)
				.skillLevel(Skill.SAILING, 62)
				.toolType(ToolType.PICKAXE)
				.target(Target.object("Mine", "Sunstone monolith"))
				.instruction("Mine the sunstone monolith: it never runs out")
				.notes("Sunstone is used for golem crafting next to the mine. Starting Fallen From Grace locks the "
					+ "monolith until you get further into the quest.")
				.getThere("Wyrmscraig needs 62 Sailing. A necklace of passage teleports you to Wyrmscraig; the mine "
					+ "is on the east side of the island.")
				.howTo("Bring a pickaxe and an empty inventory.")
				.howTo("Click the highlighted sunstone monolith (the big glowing rock, \"Mine\"). It never runs "
					+ "out, so your character keeps mining until the inventory is full.")
				.howTo("Use the sunstone for golem crafting next to the mine, or drop it, and click the monolith "
					+ "again.")
				.build(),
			activity("mine_ascension_rocks", Skill.MINING, "Ascension rocks (Fractured Archive)", 80, 40, 4_500,
				Location.of("Ascension rocks (Fractured Archive lobby)", 4113, 5536, 8))
				.tag(MethodTag.PROFIT)
				.rateAt(99, 5_500)
				.quest(Quest.WHILE_GUTHIX_SLEEPS)
				.toolType(ToolType.PICKAXE)
				.target(Target.object("Mine", "Ascension rocks"))
				.instruction("Mine the three active ascension rocks; when they sink, three others come up")
				.notes("Low xp, but the ascension shards fletch into ammo (about 18k Fletching xp an hour) and "
					+ "sell well.")
				.getThere("The ascension rocks are south-west of The Fractured Archive's lobby (While Guthix Sleeps "
					+ "done). The first visit starts The Fractured Archive miniquest.")
				.howTo("Bring a pickaxe.")
				.howTo("Three rocks are active at a time. Click a highlighted ascension rock (\"Mine\").")
				.howTo("When the rock sinks into the ground, click one of the three that just came up.")
				.howTo("If a sprite appears with a high-pitched sound, click it (\"Look-at\") for up to 2 bonus "
					+ "shards.")
				.build(),

			// Fishing
			activity("fish_karambwan", Skill.FISHING, "Karambwans (fairy ring D-K-P)", 65, 50, 28_000,
				Location.of("Karambwan fishing spots", 2899, 3118, 8))
				.tag(MethodTag.PROFIT)
				.rateAt(80, 34_000).rateAt(99, 40_000)
				.quest(Quest.TAI_BWO_WANNAI_TRIO)
				.tool(ItemReq.tool("Karambwan vessel", ItemID.TBWT_KARAMBWAN_VESSEL,
					ItemID.TBWT_KARAMBWAN_VESSEL_LOADED_WITH_KARAMBWANJI))
				.supply(ItemReq.supply("Raw karambwanji (bait)", 500, ItemID.TBWT_RAW_KARAMBWANJI))
				.target(Target.npc("Fish", "Fishing spot"))
				.instruction("Fish the karambwan spot with your karambwan vessel and raw karambwanji")
				.notes("The spot never moves, so one click lasts a whole inventory. A fish barrel doubles that. "
					+ "Karambwans are great food and sell well.")
				.getThere("Fairy ring D-K-P lands right next to the spots, north of the Karamja shipyard. The nearest "
					+ "bank is Zanaris (fairy ring back), or use a crafting cape / ring of the elements.")
				.howTo("Bring your karambwan vessel and a stack of raw karambwanji (buy them, or catch them with a "
					+ "small net south of Lubufu's hut).")
				.howTo("Click the highlighted fishing spot (\"Fish\"). Your character keeps fishing until the "
					+ "inventory is full.")
				.howTo("Bank the karambwans at Zanaris (take the fairy ring back) and repeat.")
				.build(),
			activity("fish_karambwanji", Skill.FISHING, "Karambwanji (south of Lubufu)", 5, 5, 4_000,
				Location.of("Karambwanji fishing spots", 2799, 3015, 10))
				.tag(MethodTag.CHEAP)
				.rateAt(99, 5_100)
				.questStarted(Quest.TAI_BWO_WANNAI_TRIO)
				.tool(ItemReq.tool("Small fishing net", ItemID.NET))
				.target(Target.npc("Net", "Fishing spot"))
				.instruction("Net karambwanji at the spot south of Lubufu's hut")
				.notes("Very low xp, but raw karambwanji stack, so one click can last 20 minutes. They're the bait for "
					+ "karambwans.")
				.getThere("The spots are in the river south of Lubufu's hut, north-west of Tai Bwo Wannai on "
					+ "Karamja. Fairy ring C-K-R lands close by.")
				.howTo("Bring a small fishing net. You need to have started Tai Bwo Wannai Trio (talk to Lubufu).")
				.howTo("Click the highlighted fishing spot (\"Net\"). Raw karambwanji stack in one slot, so your "
					+ "character keeps going for a long time.")
				.howTo("When your character stops, click the spot again.")
				.build(),
			activity("fish_dark_crabs", Skill.FISHING, "Dark crabs (Wilderness Resource Area)", 85, 130, 30_000,
				Location.of("Wilderness Resource Area", 3186, 3926, 12))
				.tag(MethodTag.PROFIT)
				.rateAt(99, 40_000)
				.tool(ItemReq.tool("Lobster pot", ItemID.LOBSTER_POT))
				.supply(ItemReq.supply("Dark fishing bait", 500, ItemID.WILDERNESS_FISHING_BAIT))
				.supply(ItemReq.supply("Coins (7,500 entry fee)", 7_500, ItemID.COINS))
				.target(Target.npc("Cage", "Fishing spot"))
				.target(Target.npc(null, "Piles"))
				.instruction("Fish dark crabs inside the Wilderness Resource Area and note them with Piles")
				.notes("The spot never moves, so one click lasts a whole inventory. The elite Wilderness Diary "
					+ "removes the entry fee and catches dark crabs much faster. Players can attack you here: "
					+ "bring only what you can afford to lose.")
				.getThere("The Resource Area is in level 51 Wilderness, north of the Mage Arena. Teleport with a "
					+ "Wilderness sword, the Ghorrock or Annakarl teleport, or a burning amulet to Lava Maze and "
					+ "run north-east. Pay the 7,500 coins entry fee at the gate.")
				.howTo("Bring a lobster pot, dark fishing bait and 7,500 coins. Leave your valuables in the bank.")
				.howTo("Click a highlighted fishing spot (\"Cage\"). The spots never move, so your character keeps "
					+ "fishing until the inventory is full.")
				.howTo("Use the raw dark crabs on Piles (highlighted) to note them for 50 coins each, then fish "
					+ "again.")
				.howTo("If another player shows up and attacks you, teleport out straight away.")
				.build(),
			activity("fish_sacred_eels", Skill.FISHING, "Sacred eels (Zul-Andra)", 87, 105, 25_000,
				Location.of("Zul-Andra fishing spots", 2195, 3065, 15))
				.tag(MethodTag.PROFIT)
				.rateAt(99, 27_300)
				.questStarted(Quest.REGICIDE)
				.tool(ItemReq.tool("Fishing rod", ItemID.FISHING_ROD, ItemID.FISHINGROD_PEARL))
				.supply(ItemReq.supply("Fishing bait", 500, ItemID.FISHING_BAIT))
				.tool(ItemReq.tool("Knife (to dissect the eels)", ItemID.KNIFE))
				.target(Target.npc("Bait", "Fishing spot"))
				.instruction("Bait-fish sacred eels at Zul-Andra and cut them open with a knife for Zulrah's scales")
				.notes("Each eel cut with a knife gives Zulrah's scales and Cooking xp (72 Cooking). Low effort and "
					+ "profitable.")
				.getThere("Zul-Andra is south of Lletya; a Zul-Andra teleport scroll lands right there. You need "
					+ "to have reached Tyras Camp in Regicide, and speak to the High Priestess once first.")
				.howTo("Bring a fishing rod, fishing bait and a knife.")
				.howTo("Click a highlighted fishing spot (\"Bait\"). Your character keeps fishing until the spot "
					+ "moves or the inventory is full.")
				.howTo("When the inventory is full, use the knife on an eel to dissect them all into Zulrah's scales "
					+ "(they stack), then fish again.")
				.build(),
			activity("fish_infernal_eels", Skill.FISHING, "Infernal eels (Mor Ul Rek)", 80, 95, 30_000,
				Location.of("Mor Ul Rek eel spots", 2540, 5100, 30))
				.tag(MethodTag.PROFIT)
				.rateAt(99, 36_500)
				.tool(ItemReq.tool("Oily fishing rod", ItemID.OILY_FISHING_ROD, ItemID.FISHINGROD_PEARL_OILY))
				.tool(ItemReq.tool("Ice gloves (worn)", ItemID.ICE_GLOVES, ItemID.SMITHING_UNIFORM_GLOVES_ICE))
				.tool(ItemReq.tool("Hammer", ItemID.HAMMER, ItemID.IMCANDO_HAMMER))
				.supply(ItemReq.supply("Fishing bait", 500, ItemID.FISHING_BAIT))
				.target(Target.npc("Bait", "Fishing spot"))
				.instruction("Bait-fish infernal eels in Mor Ul Rek wearing ice gloves, and crack them with a hammer")
				.notes("Cracking the eels with a hammer gives Tokkul, onyx bolt tips and lava scale shards.")
				.requirementText("A fire cape to enter Mor Ul Rek")
				.getThere("Mor Ul Rek is the inner TzHaar city under Karamja. Show a fire cape to the guard to get "
					+ "in; a TzHaar fight cave teleport or fairy ring B-L-P gets you close.")
				.howTo("Wear ice gloves and bring an oily fishing rod, fishing bait and a hammer.")
				.howTo("Click a highlighted fishing spot (\"Bait\"). Your character keeps fishing on their own.")
				.howTo("When the inventory is full, use the hammer on an eel to crack them all open, then fish "
					+ "again.")
				.build(),
			activity("fish_civitas_keys", Skill.FISHING, "House keys pond (Civitas illa Fortis)", 1, 10, 2_000,
				Location.of("Civitas illa Fortis park pond", 1738, 3101, 6))
				.tag(MethodTag.CHEAP)
				.tool(ItemReq.tool("Big fishing net", ItemID.BIG_NET))
				.target(Target.npc("Net", "Fishing spot"))
				.instruction("Fish the pond in the park with a big net for house keys")
				.notes("Barely any xp, but the spot never moves and it fishes up house keys for stealing valuables "
					+ "(Thieving) without any thieving.")
				.getThere("The pond is in the park in Civitas illa Fortis (Varlamore). The quetzal or a Civitas "
					+ "teleport gets you there.")
				.howTo("Bring a big fishing net.")
				.howTo("Click the highlighted fishing spot (\"Net\"). It never moves; your character keeps "
					+ "fishing until the inventory is full of junk.")
				.howTo("Drop the junk (keep the house keys and caskets) and click the spot again.")
				.build(),

			// Agility
			activity("agility_bf_pedals", Skill.AGILITY, "Blast Furnace pedals", 30, 1, 5_000, BLAST_FURNACE)
				.rateAt(99, 6_000)
				.supply(Supplies.STAMINA)
				.target(Target.object("Pedal", "Pedals"))
				.instruction("Work the Blast Furnace pedals; each tick of pedalling is 1 Agility xp")
				.notes("Slow but nearly clickless: one click lasts until your run energy runs out. Full graceful "
					+ "and energy or stamina potions make each stretch longer.")
				.getThere(BLAST_FURNACE_TRIP)
				.howTo("Bring energy or stamina potions; wear graceful if you have it.")
				.howTo("Click the highlighted pedals (\"Pedal\"). Your character keeps pedalling and uses up run "
					+ "energy.")
				.howTo("When your run energy is gone, drink a potion (or wait for it to come back) and click the "
					+ "pedals again.")
				.build(),
			activity("agility_poh_traps", Skill.AGILITY, "POH teleport-trap course", 1, 20, 8_000,
				Location.of("Rimmington house portal", 2954, 3224, 8))
				.instanced(true)
				.rateAt(70, 10_000).rateAt(99, 12_000)
				.skillLevel(Skill.CONSTRUCTION, 88)
				.target(Target.objectContaining(null, "Teleport trap"))
				.instruction("Let the Tok-Xil at the end of your dungeon corridor pull you over the teleport traps")
				.notes("Needs a big build (throne room with oak lever, dungeon corridors with teleport traps and a "
					+ "Tok-Xil guard). Once built, 5 to 15 minutes go by between clicks.")
				.wikiPage("Guide:POH_Agility")
				.setupTitle("Build the course (once)")
				.setupStep("Follow the wiki's POH Agility guide to build it: a throne room with an oak lever on the "
					+ "ground floor, dungeon corridors lined with teleport traps, and a Tok-Xil (Construction) at "
					+ "the end of the corridor.")
				.howTo("Go into your house in building mode off, pull the oak lever to switch the dungeon monsters "
					+ "and traps on.")
				.howTo("Go down to the dungeon and turn auto-retaliate on. The Tok-Xil starts attacking you and your "
					+ "character runs at it over the teleport traps.")
				.howTo("Every trap you pass is 20 Agility xp; failing one sends you back to the start, and your "
					+ "character runs again by itself.")
				.howTo("Come back when your prayer or hitpoints get low, top them up and start again.")
				.build(),
			activity("agility_vampyrium_rockslide", Skill.AGILITY, "Vampyrium rockslide (every 7.5 min)", 78, 550,
				4_400, Location.of("Vampyrium rockslide (outside Castle Drakan)", 2555, 7820, 6))
				.quest(Quest.THE_BLOOD_MOON_RISES)
				.target(Target.objectContaining(null, "Rockslide"))
				.instruction("Go down the rockslide shortcut once every 7.5 minutes for 550 Agility xp")
				.notes("Only 4,400 xp an hour, but just one click every 7.5 minutes, so it fits next to anything "
					+ "else. Clicking too early resets the timer.")
				.getThere("Drakan's medallion teleports you to Castle Drakan; the rockslide is just outside it.")
				.howTo("Wait until 7.5 minutes have gone by since you last got any Agility xp.")
				.howTo("Click the highlighted rockslide to go down the hill (78 Agility). That's the 550 xp.")
				.howTo("Climb back up when you like (no xp) and set a 7.5 minute timer before the next go.")
				.build(),

			// Strength
			activity("strength_bf_pump", Skill.STRENGTH, "Blast Furnace pump", 30, 2, 12_000,
				Location.of("Blast Furnace pump", 1950, 4961, 6))
				.target(Target.object("Operate", "Pump"))
				.instruction("Operate the Blast Furnace pump: 2 Strength xp a tick and no Hitpoints xp")
				.notes("Slow, but it's pure Strength (no Hitpoints xp) and one click lasts up to 100 minutes. Use "
					+ "world 302, where other players run the furnace.")
				.getThere(BLAST_FURNACE_TRIP)
				.howTo("Hop to world 302 (\"Blast Furnace\"), where players keep the furnace running.")
				.howTo("Click the highlighted pump (\"Operate\"). Your character keeps pumping on their own.")
				.howTo("If the stove gets too hot or someone stops it, click the pump again.")
				.build(),

			// Thieving
			activity("thieve_summer_garden", Skill.THIEVING, "Summer garden (Sorceress's Garden)", 65, 2_760, 140_000,
				Location.of("Sorceress's Garden", 2912, 5472, 30))
				.tag(MethodTag.MINIGAME).tag(MethodTag.FAST)
				.rateAt(99, 150_000)
				.quest(Quest.PRINCE_ALI_RESCUE)
				.tool(ItemReq.tool("Pestle and mortar", ItemID.PESTLE_AND_MORTAR))
				.supply(ItemReq.supply("Beer glasses", 22, ItemID.BEER_GLASS))
				.supply(Supplies.STAMINA)
				.target(Target.object("Pick-fruit", "Sq'irk tree"))
				.target(Target.object(null, "Fountain"))
				.instruction("One-click summer garden: wait at the start tile for the elementals, click the sq'irk tree "
					+ "and your character runs the whole way in")
				.notes("Also gives Farming xp, and the sq'irkjuice boosts Thieving at Osman. The \"One Click Summer "
					+ "Garden\" RuneLite plugin plays a countdown sound for the click.")
				.wikiPage("Sorceress's_Garden/Summer")
				.getThere("Teleport to Al Kharid (amulet of glory or the Sorceress's Garden minigame teleport) and "
					+ "go to the sorceress's house north-east of the Shantay Pass. Prince Ali Rescue must be done.")
				.howTo("Bring a pestle and mortar, about 22 beer glasses and 3 stamina potions.")
				.howTo("In the garden, go through the north (summer) gate and stand on the tile just before the "
					+ "first elemental's path.")
				.howTo("Watch elementals 1 and 3: when they bounce off the inner walls at the same time, click the "
					+ "highlighted sq'irk tree in the middle as elemental 1 moves on. Your character runs all the "
					+ "way in by themselves.")
				.howTo("If you get caught, you're sent back to the start: wait for the bounce and try again.")
				.howTo("Picking the fruit teleports you out. Use the pestle and mortar on 2 sq'irks to make a "
					+ "sq'irkjuice, then go back in and repeat. Bank the juice or turn it in to Osman for Thieving "
					+ "xp.")
				.build(),

			// Hunter
			activity("hunter_maniacal_monkeys", Skill.HUNTER, "Maniacal monkeys (Kruk's Dungeon)", 60, 1_000,
				60_000, Location.of("Glough's Laboratory (Kruk's Dungeon)", 2903, 9125, 10))
				.tag(MethodTag.FAST)
				.rateAt(75, 90_000).rateAt(99, 120_000)
				.quest(Quest.MONKEY_MADNESS_II)
				.supply(ItemReq.supply("Bananas (bait)", 27, ItemID.BANANA))
				.target(Target.objectContaining("Set-trap", "boulder"))
				.target(Target.npc(null, "Maniacal monkey"))
				.instruction("Bait the boulder traps with bananas and wait for a maniacal monkey to get caught")
				.notes("The monkeys aren't killed: they drop their tail and escape. Very low effort for the xp.")
				.getThere("Fly to Ape Atoll by gnome glider (royal seed pod to the Grand Tree), investigate the "
					+ "jungle grass south of the glider to enter Kruk's Dungeon, cross the monkey bars with the "
					+ "Kruk greegree and head north to Glough's Laboratory.")
				.howTo("Bring a full inventory of bananas.")
				.howTo("Click a highlighted boulder trap (\"Set-trap\") to bait it with a banana.")
				.howTo("Wait for a maniacal monkey to walk into it. When it's caught you get 1,000 xp and a damaged "
					+ "monkey tail: drop the tail.")
				.howTo("Set the trap again straight away and repeat.")
				.build(),
			activity("hunter_stymphikes", Skill.HUNTER, "Stymphikes (Vampyrium)", 82, 1_725, 110_000,
				Location.of("Stymphike trees (Vampyrium)", 2654, 7838, 8))
				.tag(MethodTag.FAST)
				.rateAt(90, 125_000).rateAt(99, 140_000)
				.quest(Quest.THE_BLOOD_MOON_RISES)
				.tool(ItemReq.tool("Hunter's spear", ItemID.HUNTING_STYMPHIKE_SPEAR, ItemID.HG_HUNTER_SPEAR))
				.supply(ItemReq.supply("Letvek (bait)", 100, ItemID.LETVEK))
				.target(Target.object("Bait", "Stymphike tree"))
				.target(Target.objectContaining("Hide", "bush"))
				.target(Target.npc(null, "Stymphike"))
				.instruction("Bait a stymphike tree with letvek, hide in the bush and spear the stymphike when it lands")
				.notes("About 3 clicks every 45 seconds. Turn on sound effects: a scratching sound means the bird is "
					+ "down.")
				.getThere("Vampyrium needs The Blood Moon Rises. Teleport with Drakan's medallion to Castle Drakan and "
					+ "follow the arrow to the stymphike trees east of Sangvesti.")
				.howTo("Bring a hunter's spear (or a makeshift spear) and a stack of letvek. Catch letvek in box "
					+ "traps on Vampyrium (76 Hunter) or buy them.")
				.howTo("Click a highlighted stymphike tree (\"Bait\") to bait it with letvek.")
				.howTo("Straight away click the bush next to it (\"Hide\"). You must hide, or no stymphike comes.")
				.howTo("After about 40 seconds a stymphike lands: click it, or wait and your character spears it "
					+ "when they peek out.")
				.howTo("As soon as the xp drop shows, bait the tree and hide again.")
				.build(),

			// Magic
			activity("magic_splashing", Skill.MAGIC, "Splashing (rats west of the GE)", 1, 5.5, 6_600,
				Location.of("Rats west of the Grand Exchange", 3122, 3486, 6))
				.members(false).tag(MethodTag.CHEAP)
				.rateAt(13, 13_800).rateAt(35, 27_000).rateAt(59, 41_400).rateAt(75, 51_000).rateAt(95, 60_600)
				.supply(ItemReq.supply("Air runes", 1_000, ItemID.AIRRUNE))
				.supply(ItemReq.supply("Mind runes (or the runes for your best strike/bolt/blast spell)", 1_000,
					ItemID.MINDRUNE))
				.target(Target.npc("Attack", "Rat"))
				.instruction("Autocast your best combat spell on a rat while wearing armour that makes every cast "
					+ "miss")
				.notes("Your armour has to bring your Magic attack bonus to -64 or lower so every cast splashes and "
					+ "the rat never dies (full plate armour and a kiteshield do it). After 20 minutes your "
					+ "character stops: click once to keep going.")
				.wikiPage("Splashing/Guide")
				.getThere("The rats are in the house just west of the Grand Exchange.")
				.howTo("Wear full metal armour (helmet, platebody, platelegs and kiteshield, any metal) so your Magic "
					+ "attack bonus is -64 or lower. Check it in the equipment stats screen.")
				.howTo("Open the combat options tab, click \"Choose spell\" and pick your best strike, bolt or blast "
					+ "spell. Turn auto-retaliate on.")
				.howTo("Click a highlighted rat (\"Attack\"). Every cast splashes (blue smoke), so the rat never dies "
					+ "and you keep getting xp.")
				.howTo("Every 20 minutes click the game once, or your character stops casting.")
				.build(),
			activity("magic_barrage_monkeys", Skill.MAGIC, "Ice Barrage maniacal monkeys", 94, 200, 200_000,
				KRUKS_MONKEYS)
				.tag(MethodTag.FAST)
				.questStarted(Quest.MONKEY_MADNESS_II)
				.supply(ItemReq.supply("Death runes", 2_000, ItemID.DEATHRUNE))
				.supply(ItemReq.supply("Blood runes", 1_000, ItemID.BLOODRUNE))
				.supply(ItemReq.supply("Water runes (or a water staff)", 3_000, ItemID.WATERRUNE))
				.target(Target.npc("Attack", "Maniacal monkey"))
				.instruction("Bunch the maniacal monkeys up and Ice Barrage the stack every 10 minutes")
				.notes("Very fast but expensive (runes). Needs the Ancient Magicks (Desert Treasure I). Pick up the "
					+ "prayer potions they drop.")
				.getThere("Get to Ape Atoll (royal seed pod and gnome glider, or the Ape Atoll teleport) and go into "
					+ "Kruk's Dungeon in the north-west of the island. Wear a monkey greegree if you haven't "
					+ "finished Monkey Madness II.")
				.howTo("Switch to the Ancient Magicks and bring death, blood and water runes (a water staff saves "
					+ "the water runes).")
				.howTo("Go to the big room full of maniacal monkeys (through the second hole with vines, south-east). "
					+ "Stand next to a wall so they bunch up on you.")
				.howTo("Autocast Ice Barrage (combat options tab, \"Choose spell\") and attack a monkey in the "
					+ "middle of the stack: the spell hits every monkey around it.")
				.howTo("After about 10 minutes they stop attacking: run west along the south passage until monkeys "
					+ "there attack you, then come back.")
				.build()
		);
	}

	private static Method.MethodBuilder activity(String id, Skill skill, String name, int level, double xp, int xpHr,
		Location location)
	{
		return Method.builder()
			.id(id)
			.skill(skill)
			.type(MethodType.ACTIVITY)
			.name(name)
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour(xpHr)
			.members(true)
			.tag(MethodTag.AFK)
			.location(location)
			.wikiPage(WIKI);
	}

	private AfkGuide()
	{
	}
}
