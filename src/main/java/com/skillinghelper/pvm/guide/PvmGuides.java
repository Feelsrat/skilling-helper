package com.skillinghelper.pvm.guide;

import static com.skillinghelper.pvm.guide.Gear.buy;
import com.skillinghelper.data.CombatSpell;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import com.skillinghelper.pvm.guide.Supply.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/**
 * A guide for every entry on the PvM ladder. Coordinates, drop rates and amounts are from memory of the OSRS Wiki
 * (it can't be reached from where this was written), so the place names in the step text are the thing to trust if
 * a path looks off.
 */
public final class PvmGuides
{
	private static final Option GIANT_KEY = Option.of("Giant key", ItemID.HILLGIANT_BOSS_KEY);
	private static final Option MOSSY_KEY = Option.of("Mossy key", ItemID.MOSSY_KEY);

	private static final Location EDGEVILLE_HILL_GIANTS = Location.of("the hill giants in Edgeville Dungeon", 3117,
		9845, 14);
	private static final Location VARROCK_MOSS_GIANTS = Location.of("the moss giants in the Varrock Sewers", 3165,
		9885, 14);
	private static final Location VARROCK_SEWERS = Location.of("the Varrock Sewers manhole", 3237, 3458, 3);
	private static final Location FALADOR_PARK = Location.of("Falador Park", 2989, 3378, 10);

	private static final Location PORT_SARIM_PC_DOCK = Location.of("the Pest Control boat in Port Sarim", 3041,
		3202, 4);
	private static final Location NOVICE_GANGPLANK = Location.of("the novice lander", 2657, 2639, 2);
	private static final Location NOVICE_LANDER = Location.of("the novice lander deck", 2661, 2640, 2);
	private static final Location EDGEVILLE_LEVER = Location.of("the Edgeville lever", 3090, 3475, 3);
	private static final Location KALPHITE_LAIR_ENTRANCE = Location.of("the Kalphite Lair entrance", 3226, 3108, 4);
	private static final Location ZUL_ANDRA = Location.of("Zul-Andra", 2199, 3056, 20);
	private static final Location FIGHT_CAVE_ENTRANCE = Location.of("the Fight Cave entrance", 2438, 5168, 4);
	private static final Location RELLEKKA_PIER = Location.of("Torfinn on the Rellekka pier", 2640, 3696, 4);
	private static final Location NECROPOLIS = Location.of("the Necropolis", 3355, 2725, 25);
	private static final Location QUIDAMORTEM = Location.of("Mount Quidamortem", 1233, 3565, 15);
	private static final Location VER_SINHAZA = Location.of("Ver Sinhaza", 3650, 3219, 15);
	private static final Location GOD_WARS_HOLE = Location.of("the God Wars Dungeon entrance", 2917, 3745, 6);
	private static final Location WARRIORS_GUILD = Location.of("the Warriors' Guild", 2877, 3546, 6);
	private static final Location BARBARIAN_OUTPOST = Location.of("the Barbarian Outpost", 2531, 3577, 12);
	private static final int[] MOONLIGHT_POTIONS = {
		ItemID._4DOSEMOONLIGHTPOTION, ItemID._3DOSEMOONLIGHTPOTION, ItemID._2DOSEMOONLIGHTPOTION,
		ItemID._1DOSEMOONLIGHTPOTION,
	};

	private static final GearSlot ANTI_DRAGON_SHIELD = GearSlot.required("shield",
		buy("Dragonfire shield", Skill.DEFENCE, 75, 0, true, ItemID.DRAGONFIRE_SHIELD),
		buy("Dragonfire ward", Skill.DEFENCE, 75, 0, true, ItemID.DRAGONFIRE_WARD),
		buy("Anti-dragon shield", null, 0, 0, false, ItemID.ANTIDRAGONBREATHSHIELD));

	public static final List<PvmGuide> ALL = List.of(
		PvmGuide.builder()
			.contentId("obor")
			.gear(GearSets.melee())
			.budget(150_000)
			.supply(Supplies.untradeable("Giant key", GIANT_KEY).note("Opens Obor's gate; used up"))
			.supply(Supplies.strengthPotions(2))
			.supply(Supplies.food(Supplies.SWORDFISH))
			.stage(Stage.builder()
				.title("Get a giant key")
				.when(s -> s.carried(ItemID.HILLGIANT_BOSS_KEY) == 0)
				.place(EDGEVILLE_HILL_GIANTS)
				.line("Kill hill giants until one drops a giant key (roughly 1 in 128 kills) and pick it up.")
				.line("Bury or bank the big bones while you wait; they're worth a bit.")
				.highlight(Target.npc("Attack", "Hill Giant"))
				.build())
			.stage(Stage.builder()
				.title("Open Obor's gate")
				.place(EDGEVILLE_HILL_GIANTS)
				.line("Obor's gate is in the hill giant area. Open it with the key in your inventory.")
				.highlight(Target.object("Open", "Gate"))
				.use(ItemID.HILLGIANT_BOSS_KEY)
				.build())
			.fight(Fight.builder()
				.boss("Obor")
				.tip("Obor hits hard with melee up close and throws boulders from range.")
				.tip("He knocks you back now and then; just walk back in and keep hitting.")
				.tip("Eat around half hitpoints. Each key is one kill, so the drop table is all you get per key.")
				.build())
			.profit(Profit.gp(150_000, "Hill giant club and rune drops, but each kill needs a new key"))
			.build(),

		PvmGuide.builder()
			.contentId("bryophyta")
			.gear(GearSets.melee())
			.budget(150_000)
			.supply(Supplies.untradeable("Mossy key", MOSSY_KEY).note("Opens Bryophyta's gate; used up"))
			.supply(Supplies.secateurs().note("Growthlings regrow unless finished off with secateurs (from memory)"))
			.supply(Supplies.strengthPotions(2))
			.supply(Supplies.food(Supplies.SWORDFISH))
			.stage(Stage.builder()
				.title("Get a mossy key")
				.when(s -> s.carried(ItemID.MOSSY_KEY) == 0)
				.place(VARROCK_MOSS_GIANTS)
				.line("Kill moss giants until one drops a mossy key (roughly 1 in 150 kills) and pick it up.")
				.highlight(Target.npcContaining("Attack", "Moss giant"))
				.build())
			.stage(Stage.builder()
				.title("Open Bryophyta's gate")
				.place(VARROCK_MOSS_GIANTS)
				.line("The gate to Bryophyta's lair is next to the moss giants. Open it with the key.")
				.highlight(Target.object("Open", "Gate"))
				.use(ItemID.MOSSY_KEY)
				.build())
			.fight(Fight.builder()
				.boss("Bryophyta")
				.add(new Fight.Add("Growthling", "Growthlings are up: kill them first, secateurs in your inventory "
					+ "finish them off."))
				.tip("Bryophyta uses melee up close and magic from range, so stay next to her.")
				.tip("She summons three growthlings now and then. Kill them before going back to her.")
				.build())
			.profit(Profit.gp(150_000, "Bryophyta's essence and rune drops, one kill per key"))
			.build(),

		PvmGuide.builder()
			.contentId("scurrius")
			.gear(GearSets.melee())
			.budget(1_000_000)
			.supply(Supplies.prayerPotions(2))
			.supply(Supplies.teleport("Varrock teleports", 1, Option.of("Varrock teleport tablets",
				ItemID.POH_TABLET_VARROCKTELEPORT)).note("To bank after"))
			.supply(Supplies.food(Supplies.SWORDFISH))
			.stage(Stage.builder()
				.title("Climb down into the Varrock Sewers")
				.when(s -> s.getLocation() != null && s.getLocation().getY() < 9000)
				.place(VARROCK_SEWERS)
				.line("Climb down the manhole east of Varrock Palace.")
				.highlight(Target.object("Climb-down", "Manhole"))
				.build())
			.stage(Stage.builder()
				.title("Find Scurrius' lair")
				.line("Follow the sewer to the broken bars into the rat boss's lair and climb through.")
				.highlight(Target.object(null, "Broken bars"))
				.build())
			.fight(Fight.builder()
				.boss("Scurrius")
				.add(new Fight.Add("Giant rat", "Giant rats are up: kill them, they add up fast."))
				.tip("Scurrius teaches boss basics: protection prayers and moving out of attacks.")
				.tip("Falling rocks show a shadow first; step off those tiles. He heals at food piles if left alone.")
				.tip("Use the private instance (one-off fee) if the public room is crowded.")
				.build())
			.profit(Profit.gp(250_000, "Rune drops, Scurrius' spine and a few bones"))
			.build(),

		PvmGuide.builder()
			.contentId("pest_control")
			.gear(GearSets.melee())
			.budget(500_000)
			.supply(Supplies.food(Supplies.LOBSTERS, 10))
			.inside(PvmGuides::inPestControlGame)
			.stage(Stage.builder()
				.title("Take the boat to the Void Knights' Outpost")
				.when(s -> !atVoidOutpost(s) && !inPestControlGame(s))
				.place(PORT_SARIM_PC_DOCK)
				.line("Talk to the squire on the Port Sarim docks and travel to the outpost.")
				.highlight(Target.npc("Travel", "Squire"))
				.build())
			.stage(Stage.builder()
				.title("Board the novice lander")
				.when(s -> atVoidOutpost(s) && !NOVICE_LANDER.contains(s.getLocation()))
				.place(NOVICE_GANGPLANK)
				.line("Cross the gangplank of the novice lander (combat 40+). Higher landers give more points but "
					+ "need combat 70 and 100.")
				.highlight(Target.object("Cross", "Gangplank"))
				.build())
			.stage(Stage.builder()
				.title("Wait for the game")
				.when(s -> NOVICE_LANDER.contains(s.getLocation()))
				.line("The game starts once enough players are on board. Stay on the boat.")
				.build())
			.stage(Stage.builder()
				.title("Defend the Void Knight")
				.committed(true)
				.line("Go to a portal with the group. Kill pests near it; once its shield drops, kill the portal.")
				.line("Deal at least 50 damage in the game or you get no points.")
				.highlight(Target.npc("Attack", "Portal"))
				.build())
			.fight(Fight.builder()
				.boss("Portal")
				.add(new Fight.Add("Spinner", "Spinners are up: they heal the portal, kill them first."))
				.tip("Points come from damage dealt, so keep attacking. 50 damage per game gets you points.")
				.tip("Spend the points on Void Knight armour: top, robe, gloves and the helm for your style.")
				.build())
			.profit(Profit.reward("Void Knight armour from the points"))
			.build(),

		PvmGuide.builder()
			.contentId("giant_mole")
			.gear(GearSets.melee())
			.budget(3_000_000)
			.supply(Supplies.spade())
			.supply(Supplies.lightSource().note("The lair is dark"))
			.supply(Supplies.prayerPotions(4))
			.supply(Supplies.meleeBoost(1))
			.supply(Supplies.teleport("Falador teleports", 2, Option.of("Falador teleport tablets",
				ItemID.POH_TABLET_FALADORTELEPORT)))
			.supply(Supplies.food(Supplies.MONKFISH))
			.inside(PvmGuides::inMoleLair)
			.stage(Stage.builder()
				.title("Dig into the mole lair")
				.when(s -> !inMoleLair(s))
				.place(FALADOR_PARK)
				.line("Dig with your spade on top of a mole hill in Falador Park.")
				.highlight(Target.object(null, "Mole hill"))
				.use(ItemID.SPADE)
				.build())
			.stage(Stage.builder()
				.title("Find the Giant Mole")
				.committed(true)
				.line("She digs away when she gets hurt and comes back up somewhere else in the lair.")
				.line("Run around the lair until she's in view again; she's highlighted as soon as you see her.")
				.build())
			.fight(Fight.builder()
				.boss("Giant Mole")
				.tip("Pray Protect from Melee and she can barely touch you.")
				.tip("Bring the mole claws and skins to Wyson in Falador Park for bird nests.")
				.build())
			.profit(Profit.gp(450_000, "Mole claws and skins, traded with Wyson for bird nests"))
			.build(),

		PvmGuide.builder()
			.contentId("warriors_guild")
			.gear(GearSets.melee())
			.budget(2_000_000)
			.supply(Supply.tool("Mithril full helm", Option.of("Mithril full helm", ItemID.MITHRIL_FULL_HELM))
				.note("To animate for tokens"))
			.supply(Supply.tool("Mithril platebody", Option.of("Mithril platebody", ItemID.MITHRIL_PLATEBODY)))
			.supply(Supply.tool("Mithril platelegs", Option.of("Mithril platelegs", ItemID.MITHRIL_PLATELEGS)))
			.supply(Supplies.food(Supplies.LOBSTERS, 15))
			.stage(Stage.builder()
				.title("Earn tokens at the animator")
				.place(WARRIORS_GUILD)
				.line("You need Attack and Strength adding up to 130 (or 99 in one) to get in. Teleport to Burthorpe "
					+ "with a games necklace and walk west to the guild.")
				.line("Go to the room with the Magical Animators. Use your mithril helm on an animator (click the "
					+ "helm, then the animator): your platebody and platelegs come alive as one enemy.")
				.line("Kill it, pick your three pieces back up and repeat. Each kill gives tokens; you need 100+.")
				.highlight(Target.object(null, "Magical Animator"))
				.build())
			.stage(Stage.builder()
				.title("Kill cyclopes for defenders")
				.place(WARRIORS_GUILD)
				.line("Climb the stairs to the top floor and go through the door by Kamfreena. Entering costs 10 "
					+ "tokens, and 10 more every minute.")
				.line("Kill cyclopes with melee until a defender drops (1 in 50). Pick it up, leave the room and "
					+ "show it to Kamfreena, or the next tier won't drop.")
				.line("Go back in and repeat up to rune. The dragon defender drops in the basement.")
				.highlight(Target.object(null, "Magical Animator"))
				.highlight(Target.npc("Attack", "Cyclops"))
				.build())
			.fight(Fight.builder()
				.boss("Cyclops")
				.tip("Keep your newest defender in your shield slot so the next tier can drop.")
				.build())
			.profit(Profit.reward("Bronze to dragon defenders, the best melee off-hand for a long time"))
			.build(),

		PvmGuide.builder()
			.contentId("barrows")
			.gear(GearSets.with(GearSets.melee(), Barrows.STAFF))
			.levelSupplies(Barrows::runes)
			.possibleSupplies(Barrows.RUNES)
			.budget(5_000_000)
			.supply(Supplies.spade())
			.supply(Supplies.prayerPotions(6))
			.supply(Supplies.teleport("Barrows teleports", 1, Option.of("Barrows teleport tablets",
				ItemID.TELETAB_BARROWS)).withRestockBelow(0).note("Getting there; using it never sends you back to the bank"))
			.supply(Supplies.food(Supplies.MONKFISH))
			.inside(PvmGuides::inBarrowsUnderground)
			.stages(Barrows.stages())
			.place(Location.of("the Barrows", 3565, 3289, 20))
			.fight(Fight.builder()
				.bosses(Barrows.NAMES)
				.notes(Barrows.notes())
				.live(Barrows::progress)
				.ignoreWhen(Barrows::looted)
				.wield(Barrows::fightWeapon)
				.tip("Five brothers are weak to Magic: bring an air staff and the runes for your best wind spell, plus "
					+ "your melee weapon for Ahrim. The guide tells you which one to hold for each brother.")
				.tip("Pray Protect from Melee for Dharok, Guthan, Torag and Verac, "
					+ "Protect from Missiles for Karil and Protect from Magic for Ahrim.")
				.tip("Prayer drains slowly in the crypts and tunnels, so drink a dose when it's low.")
				.tip("The guide marks the way through the tunnels and outlines the right answer to each door puzzle.")
				.build())
			.profit(Profit.gp(1_100_000, "Runes, bolt racks and a chance at Barrows armour from each chest"))
			.build(),

		PvmGuide.builder()
			.contentId("barbarian_assault")
			.gear(GearSets.melee())
			.budget(2_000_000)
			.stage(Stage.builder()
				.title("Go to the Barbarian Outpost")
				.when(s -> !BARBARIAN_OUTPOST.contains(s.getLocation()) && PvmGuides.surface(s))
				.place(BARBARIAN_OUTPOST)
				.line("A games necklace teleports you to the Barbarian Outpost.")
				.build())
			.stage(Stage.builder()
				.title("Go down to the lobby")
				.when(PvmGuides::surface)
				.line("Talk to Captain Cain the first time for the tutorial, then climb down the ladder.")
				.highlight(Target.npc(null, "Captain Cain"))
				.highlight(Target.object("Climb-down", "Ladder"))
				.build())
			.stage(Stage.builder()
				.title("Play the waves")
				.line("Join a team of five (the official BA clan chat or world 306) and pick a role: attacker, "
					+ "defender, collector or healer. Your team leader calls what each role does.")
				.line("Ten waves end with the Penance Queen. Points per role buy the fighter torso and other gear.")
				.build())
			.fight(Fight.builder()
				.boss("Penance Queen")
				.note("Penance Queen", "Penance Queen: the collector's eggs from the cannon are what hurts her.")
				.build())
			.profit(Profit.reward("Fighter torso and the Penance gear from honour points"))
			.build(),

		PvmGuide.builder()
			.contentId("sarachnis")
			.gear(GearSets.melee())
			.budget(10_000_000)
			.supply(Supplies.prayerPotions(4))
			.supply(Supplies.meleeBoost(1))
			.supply(Supplies.teleport("House teleports", 1, Option.of("Teleport to house tablets",
				ItemID.POH_TABLET_TELEPORTTOHOUSE)).note("To bank after"))
			.supply(Supplies.food(Supplies.MONKFISH))
			.stage(Stage.builder()
				.title("Go to Forthos Dungeon")
				.when(PvmGuides::surface)
				.line("Forthos Dungeon is under the ruins south of Hosidius in Great Kourend. Kharedst's memoirs "
					+ "or the Hosidius house portal get you close.")
				.line("Climb down into the ruins, then follow the dungeon east to the thick web.")
				.highlight(Target.object(null, "Stairs"))
				.build())
			.stage(Stage.builder()
				.title("Go through the thick web")
				.line("Slash or walk through the thick web into Sarachnis' lair.")
				.highlight(Target.object(null, "Thick Web"))
				.build())
			.fight(Fight.builder()
				.boss("Sarachnis")
				.add(new Fight.Add("Spawn of Sarachnis", "Her spawn are up: kill them so they don't add damage."))
				.note("Sarachnis", "She uses melee when you're next to her and ranged when you're not.")
				.tip("Pray Protect from Melee and stay next to her.")
				.tip("She calls her spawn at two-thirds and one-third of her health.")
				.build())
			.profit(Profit.gp(900_000, "Sarachnis cudgel, jar and steady herb and rune drops"))
			.build(),

		PvmGuide.builder()
			.contentId("king_black_dragon")
			.gear(GearSets.with(GearSets.melee(), ANTI_DRAGON_SHIELD))
			.budget(3_000_000)
			.supply(Supplies.cheapAntifire(1).note("With the shield, this blocks his dragonfire fully"))
			.supply(Supplies.prayerPotions(2))
			.supply(Supplies.food(Supplies.SWORDFISH))
			.stage(Stage.builder()
				.title("Pull the Edgeville lever")
				.when(s -> !inWilderness(s) && !inKbdLair(s))
				.place(EDGEVILLE_LEVER)
				.line("The lever in the ruined house north of Edgeville bank takes you to the Deserted Keep "
					+ "(level 54 Wilderness). Only bring what you can lose.")
				.highlight(Target.object("Pull", "Lever"))
				.build())
			.stage(Stage.builder()
				.title("Pull the lever to the lair")
				.when(s -> inWilderness(s))
				.line("Pull the lever inside the Deserted Keep to drop into the King Black Dragon's lair. Drink "
					+ "your antifire first.")
				.line("Watch for player killers: if someone attacks, teleport out.")
				.highlight(Target.object("Pull", "Lever"))
				.build())
			.fight(Fight.builder()
				.boss("King Black Dragon")
				.note("King Black Dragon", "His dragonfire can poison, freeze or shock you; the shield and "
					+ "antifire cover the fire damage.")
				.tip("Wear the anti-dragon shield and drink the antifire before going in.")
				.tip("Pray Protect from Melee.")
				.build())
			.profit(Profit.gp(500_000, "Dragon bones, black dragonhides and the KBD heads for Slayer"))
			.build(),

		PvmGuide.builder()
			.contentId("moons_of_peril")
			.gear(GearSets.melee())
			.budget(10_000_000)
			.supply(Supplies.prayerPotions(4))
			.supply(Supplies.food(Supplies.SHARKS, 8))
			.watch(VarbitID.PMOON_BOSS_BLOOD_DEAD).watch(VarbitID.PMOON_BOSS_BLUE_DEAD)
			.watch(VarbitID.PMOON_BOSS_ECLIPSE_DEAD)
			.stage(Stage.builder()
				.title("Go to Neypotzli")
				.when(PvmGuides::surface)
				.line("Take the quetzal to Cam Torum, then go down into Neypotzli, the moons' temple.")
				.build())
			.stage(Stage.builder()
				.title("Make supplies at the camp")
				.when(s -> s.carried(MOONLIGHT_POTIONS) == 0 && s.carried(ItemID.BREAM_FISH_COOKED) == 0
					&& moonsDone(s) == 0)
				.line("In the supply area: catch bream with a big fishing net and cook it on the fire.")
				.line("Grind moonlight grubs into paste with a pestle and mortar and add it to vials of water for "
					+ "moonlight potions. Aim for about 4 potions and 10 bream.")
				.live(PvmGuides::moonSupplies)
				.build())
			.stage(Stage.builder()
				.title("Kill the Blood Moon")
				.when(s -> s.varbit(VarbitID.PMOON_BOSS_BLOOD_DEAD) == 0)
				.line("Go through the Blood Moon's statue room door and fight her.")
				.highlight(Target.object(null, "Statue"))
				.live(PvmGuides::moonProgress)
				.build())
			.stage(Stage.builder()
				.title("Kill the Blue Moon")
				.when(s -> s.varbit(VarbitID.PMOON_BOSS_BLUE_DEAD) == 0)
				.line("Go to the Blue Moon's room and fight her.")
				.highlight(Target.object(null, "Statue"))
				.live(PvmGuides::moonProgress)
				.build())
			.stage(Stage.builder()
				.title("Kill the Eclipse Moon")
				.when(s -> s.varbit(VarbitID.PMOON_BOSS_ECLIPSE_DEAD) == 0)
				.line("Go to the Eclipse Moon's room and fight her.")
				.highlight(Target.object(null, "Statue"))
				.live(PvmGuides::moonProgress)
				.build())
			.stage(Stage.builder()
				.title("Open the Lunar Chest")
				.line("All three are down: loot the Lunar Chest in the middle of the temple.")
				.highlight(Target.object(null, "Lunar Chest"))
				.live(PvmGuides::moonProgress)
				.build())
			.fight(Fight.builder()
				.boss("Blood Moon").boss("Blue Moon").boss("Eclipse Moon")
				.live(PvmGuides::moonProgress)
				.note("Blood Moon", "Blood Moon: she drains you and spawns blood pools around the room.")
				.note("Blue Moon", "Blue Moon: she freezes the room and sends icicles; the braziers stop the freeze.")
				.note("Eclipse Moon", "Eclipse Moon: she spins clones and a shield around the arena.")
				.tip("Melee all three. Each moon has one special attack you learn to walk around.")
				.build())
			.profit(Profit.gp(1_600_000, "Moon armour pieces and supplies from the Lunar Chest"))
			.build(),

		PvmGuide.builder()
			.contentId("kalphite_queen")
			.gear(GearSets.melee())
			.budget(10_000_000)
			.supply(Supplies.rope().note("Only needed the first time, to tie at the entrances"))
			.supply(Supplies.prayerPotions(6))
			.supply(Supplies.meleeBoost(1))
			.supply(Supplies.antidote(1))
			.supply(Supplies.food(Supplies.SHARKS))
			.stage(Stage.builder()
				.title("Go to the Kalphite Lair")
				.when(PvmGuides::surface)
				.place(KALPHITE_LAIR_ENTRANCE)
				.line("Fairy ring B-I-Q lands right next to the entrance in the desert.")
				.line("Climb down the tunnel (the first time, use a rope on it).")
				.highlight(Target.object(null, "Tunnel"))
				.use(ItemID.ROPE)
				.build())
			.stage(Stage.builder()
				.title("Go down to the Queen's chamber")
				.line("Head through the lair to the second entrance and climb down to the Queen (rope it the first "
					+ "time). Kalphite guards on the way are aggressive.")
				.highlight(Target.object(null, "Tunnel"))
				.highlight(Target.object(null, "Crevice"))
				.build())
			.fight(Fight.builder()
				.boss("Kalphite Queen")
				.note("Kalphite Queen", "Two forms: when the first dies she transforms and heals to full.")
				.tip("A Verac's flail or a keris works well on both of her forms.")
				.tip("Protect from Magic is the usual prayer; her magic hits everyone nearby.")
				.build())
			.profit(Profit.gp(1_200_000, "Dragon chainbody, KQ head and steady rune and herb drops"))
			.build(),

		PvmGuide.builder()
			.contentId("zulrah")
			.gear(GearSets.ranged())
			.budget(15_000_000)
			.supply(Supplies.antiVenom(2))
			.supply(Supplies.prayerPotions(4))
			.supply(Supplies.rangingPotions(1))
			.supply(Supplies.teleport("Zul-andra teleports", 2, Option.of("Zul-andra teleport scrolls",
				ItemID.TELEPORTSCROLL_ZULANDRA)).note("One to get there, one for next time"))
			.supply(Supplies.food(Supplies.SHARKS))
			.stage(Stage.builder()
				.title("Go to Zul-Andra")
				.when(s -> !ZUL_ANDRA.contains(s.getLocation()))
				.place(ZUL_ANDRA)
				.line("You need Regicide done up to reaching Port Tyras. Read a Zul-andra teleport scroll, or take "
					+ "fairy ring B-J-S and walk west.")
				.line("The first time, talk to High Priestess Zul-Harcinqa and offer to be the sacrifice, or you "
					+ "can't board the boat.")
				.use(ItemID.TELEPORTSCROLL_ZULANDRA)
				.build())
			.stage(Stage.builder()
				.title("Board the sacrificial boat")
				.line("Drink an anti-venom, then take the sacrificial boat at the jetty to Zulrah's shrine.")
				.line("Zulrah rises from the water as soon as you click. Stay on the platform; the only way out is "
					+ "killing her, teleporting or dying.")
				.line("Her loot lands under you. If you die, Priestess Zul-Gwenwynig at the jetty holds your items.")
				.highlight(Target.object(null, "Sacrificial boat"))
				.build())
			.fight(Fight.builder()
				.boss("Zulrah")
				.add(new Fight.Add("Snakeling", "Snakelings are up: they're weak, kill them if they get in your way."))
				.note("Zulrah", "Zulrah changes form and spot as she dives; her clouds poison the tiles they land on.")
				.tip("Learn the rotations from the wiki before you go; this plugin won't call them out (Jagex rules).")
				.tip("Ranged works on every form; most players add a magic weapon for the green form.")
				.tip("If you die, Priestess Zul-Gwenwynig in Zul-Andra keeps your items for a fee.")
				.build())
			.profit(Profit.gp(2_500_000, "Zulrah's scales, uniques and steady drops"))
			.build(),

		PvmGuide.builder()
			.contentId("fight_caves")
			.gear(GearSets.ranged())
			.budget(15_000_000)
			.supply(Supplies.rangingPotions(1))
			.supply(Supplies.prayerPotions(10))
			.supply(Supplies.brews(8))
			.supply(Supplies.superRestores(4))
			.supply(Supplies.food(Supplies.SHARKS))
			.inside(PvmGuides::inFightCave)
			.stage(Stage.builder()
				.title("Enter the Fight Cave")
				.when(s -> !inFightCave(s))
				.place(FIGHT_CAVE_ENTRANCE)
				.line("Get to the TzHaar city (fairy ring B-L-P, or the Karamja volcano) and enter the cave in "
					+ "the north-west.")
				.line("There's no bank inside, so check the loadout above is complete before you go in. The cave is a "
					+ "safe minigame: if you die you keep your items.")
				.highlight(Target.object("Enter", "Cave entrance"))
				.build())
			.stage(Stage.builder()
				.title("Work through the waves")
				.committed(true)
				.line("The card lists monsters in the wiki's kill order. If one is stuck behind a rock, stay put and "
					+ "kill the ones you can reach first.")
				.line("Safespots: keep a rock between you and the melee monsters. Mark yours once with Ground Markers.")
				.line("Log out: click once and you leave when the wave ends, progress saved.")
				.highlight(Target.npc(null, "Ket-Zek"))
				.highlight(Target.npc(null, "Tok-Xil"))
				.highlight(Target.npc(null, "Yt-MejKot"))
				.highlight(Target.npc(null, "Tz-Kek"))
				.highlight(Target.npc(null, "Tz-Kih"))
				.wave(FightCave::card)
				.priority(FightCaveWaves::firstTarget)
				.build())
			.fight(Fight.builder()
				.boss("TzTok-Jad")
				.add(new Fight.Add("Yt-HurKot", "Jad's healers are up: hit each one once so they follow you."))
				.note("TzTok-Jad", "Jad switches between a magic and a ranged attack; you have to read his animation "
					+ "yourself.")
				.tip("Pray against what's hitting you; Jad's prayer switches are up to you (no helpers allowed).")
				.tip("Before Jad: top up your prayer and drink a ranging potion so the boost lasts the fight.")
				.tip("Learn Jad's two tells from the wiki videos before you go: he rears up and breathes fire for "
					+ "magic, and slams the ground for ranged.")
				.tip("Stay out of melee range: next to you he can also stomp.")
				.tip("At half health four Yt-HurKot heal him. Hit each one once so they follow you instead, then go "
					+ "back to Jad.")
				.tip("Mark your safespots once with Ground Markers (shift + right-click a tile); they stay for every "
					+ "trip.")
				.wave(FightCave::card)
				.build())
			.profit(Profit.reward("The fire cape, and Tokkul"))
			.build(),

		PvmGuide.builder()
			.contentId("vorkath")
			.gear(GearSets.with(GearSets.with(GearSets.ranged(), ANTI_DRAGON_SHIELD), GearSets.RANGED_WEAPON_DRAGONS))
			.budget(30_000_000)
			.supply(Supplies.antifire(2))
			.supply(Supplies.antiVenom(1))
			.supply(Supplies.rangingPotions(1))
			.supply(Supplies.prayerPotions(3))
			.supply(Supplies.teleport("House teleports", 1, Option.of("Teleport to house tablets",
				ItemID.POH_TABLET_TELEPORTTOHOUSE)))
			.supply(Supplies.food(Supplies.SHARKS))
			.stage(Stage.builder()
				.title("Sail to Ungael")
				.when(s -> !onUngael(s))
				.place(RELLEKKA_PIER)
				.line("You need Dragon Slayer II done. Teleport to Rellekka (Rellekka teleport, enchanted lyre or "
					+ "Fremennik sea boots) and walk to the third pier from the west.")
				.line("Right-click Torfinn and pick \"Ungael\". If you die at Vorkath, Torfinn keeps your items "
					+ "for a fee.")
				.highlight(Target.npc("Travel", "Torfinn"))
				.build())
			.stage(Stage.builder()
				.title("Climb over the ice")
				.line("Drink your antifire and anti-venom, then climb over the ice chunks into the crater.")
				.line("Vorkath is asleep in the middle. Click him (\"Poke\") to wake him and the fight starts.")
				.line("When he dies, pick up the loot, climb back over the ice and talk to Torfinn to go again or "
					+ "teleport to bank.")
				.highlight(Target.object(null, "Ice chunks"))
				.build())
			.fight(Fight.builder()
				.boss("Vorkath")
				.add(new Fight.Add("Zombified Spawn", "The zombified spawn is up: kill it before it reaches you "
					+ "(Crumble Undead does it in one cast)."))
				.note("Vorkath", "Between normal attacks he does an acid phase (keep walking) and a freeze with the "
					+ "zombified spawn.")
				.tip("Bring runes for Crumble Undead (or a rune pouch) for the spawn.")
				.tip("Keep moving during the acid phase; this plugin won't mark tiles for you (Jagex rules).")
				.build())
			.profit(Profit.gp(2_800_000, "Blue dragon bones, dragonhide and Vorkath's head"))
			.build(),

		PvmGuide.builder()
			.contentId("god_wars")
			.gear(GearSets.melee())
			.budget(30_000_000)
			.supply(Supplies.rope().note("Only needed the first time"))
			.supply(Supplies.meleeBoost(1))
			.supply(Supplies.prayerPotions(6))
			.supply(Supplies.brews(4))
			.supply(Supplies.superRestores(4))
			.supply(Supplies.food(Supplies.SHARKS))
			.inside(PvmGuides::inGodWars)
			.stage(Stage.builder()
				.title("Go to the God Wars Dungeon")
				.when(s -> !inGodWars(s))
				.place(GOD_WARS_HOLE)
				.line("You need to have beaten Dad in Troll Stronghold first. Fastest is the Trollheim teleport "
					+ "(after Eadgar's Ruse) or Ghommal's hilt; otherwise teleport to Burthorpe and walk up past "
					+ "Death Plateau.")
				.line("Go round to the north side of the mountain. Thrower trolls throw rocks here, so keep running.")
				.line("Squeeze past the big boulder (60 Strength) or the little crack (60 Agility), then run north to "
					+ "the large hole in the snow.")
				.line("The first time, use your rope on the hole (click the rope, then the hole). Then climb down.")
				.line("Each general's area needs 70 in one stat: Bandos 70 Strength and a hammer, Armadyl 70 Ranged "
					+ "and a crossbow with a mith grapple, Saradomin 70 Agility and two ropes the first time, Zamorak "
					+ "70 Hitpoints.")
				.highlight(Target.object(null, "Boulder"))
				.highlight(Target.object(null, "Little crack"))
				.highlight(Target.object(null, "Hole"))
				.use(ItemID.ROPE)
				.build())
			.stage(Stage.builder()
				.title("Get 40 kill count")
				.when(s -> inGodWars(s) && GodWars.best(s) < 40)
				.committed(true)
				.line("Wear one item of each god you don't want attacking you (a god cape, book, blessing or "
					+ "armour piece). Followers of the gods you show leave you alone.")
				.line("Walk to the camp of the god whose general you want (Bandos is the usual first) and kill that "
					+ "god's followers until its kill count, top-left of your screen, shows 40.")
				.line("Kill count stays while you're inside. Eat when you drop below half hitpoints.")
				.live(GodWars::progress)
				.build())
			.stage(Stage.builder()
				.title("Enter the general's room")
				.when(PvmGuides::inGodWars)
				.committed(true)
				.line("Go to the big door at the end of that god's area. For Bandos, have a hammer in your "
					+ "inventory and click the door to bang the gong; going in uses up your 40 kill count.")
				.line("Inside you're locked in with the general and three bodyguards until you leave by the door "
					+ "again (that costs kill count next time too).")
				.highlight(Target.object(null, "Big door"))
				.live(GodWars::progress)
				.build())
			.fight(Fight.builder()
				.boss("General Graardor").boss("Commander Zilyana").boss("K'ril Tsutsaroth").boss("Kree'arra")
				.tip("Bandos (General Graardor) is the usual first: melee, Protect from Melee, kill his bodyguards.")
				.build())
			.profit(Profit.gp(2_500_000, "God armour pieces, hilts and steady drops"))
			.build(),

		PvmGuide.builder()
			.contentId("gauntlet")
			.gear(List.of())
			.tracked(List.of(Gauntlet.MONSTERS))
			.inside(PvmGuides::inGauntlet)
			.stage(Stage.builder()
				.title("Enter the Gauntlet")
				.when(s -> !inGauntlet(s))
				.line("Bank everything: you can't bring items in. Use a crystal teleport seed or walk to the "
					+ "north-east of Prifddinas.")
				.line("Enter the Gauntlet and pick the normal or corrupted version.")
				.highlight(Target.object(null, "The Gauntlet"))
				.build())
			.stage(Stage.builder()
				.title("Gather resources")
				.when(s -> !Gauntlet.prepped(s))
				.committed(true)
				.line("Kill the small monsters for shards and the three demi-bosses for weapon frames. Mine, chop, "
					+ "pick and fish the resource spots. A common target (from memory):")
				.live(Gauntlet::progress)
				.highlight(Target.npc("Attack", "Crystalline Bear", "Crystalline Dark Beast", "Crystalline Dragon",
					"Corrupted Bear", "Corrupted Dark Beast", "Corrupted Dragon"))
				.build())
			.stage(Stage.builder()
				.title("Make your gear and supplies")
				.committed(true)
				.line("Back at the start room: use the singing bowl to make your armour, staff and bow. Cook the "
					+ "paddlefish on the range and make egniol potions.")
				.line("Then go through the barrier to the boss room.")
				.highlight(Target.object(null, "Singing bowl"))
				.highlight(Target.object(null, "Range"))
				.highlight(Target.object(null, "Barrier"))
				.live(Gauntlet::progress)
				.build())
			.fight(Fight.builder()
				.boss("Crystalline Hunllef").boss("Corrupted Hunllef")
				.note("Crystalline Hunllef", "The Hunllef changes its protection prayer and its attack style; "
					+ "you have to count yourself.")
				.note("Corrupted Hunllef", "The Hunllef changes its protection prayer and its attack style; "
					+ "you have to count yourself.")
				.tip("Hunllef switches its protection prayer every 6 hits you land and its attack style every 4 "
					+ "attacks. Count them yourself; plugins can't help here (Jagex rules).")
				.build())
			.profit(Profit.gp(4_000_000, "Corrupted Gauntlet: crystal armour seeds and the enhanced weapon seed"))
			.build(),

		PvmGuide.builder()
			.contentId("tombs_of_amascut")
			.gear(GearSets.melee())
			.budget(50_000_000)
			.supply(Supplies.meleeBoost(1))
			.supply(Supplies.brews(4))
			.supply(Supplies.superRestores(4))
			.supply(Supplies.food(Supplies.SHARKS, 8))
			.stage(Stage.builder()
				.title("Go to the Necropolis")
				.when(s -> !NECROPOLIS.contains(s.getLocation()) && PvmGuides.surface(s))
				.place(NECROPOLIS)
				.line("Fairy ring A-K-P or the Pharaoh's sceptre takes you to the Necropolis.")
				.build())
			.stage(Stage.builder()
				.title("Start a raid")
				.when(PvmGuides::surface)
				.line("Join or make a party at the raid entrance, set the raid level (Entry Mode to learn), "
					+ "then enter.")
				.highlight(Target.object(null, "Entry"))
				.build())
			.stage(Stage.builder()
				.title("Pick a path")
				.line("Four paths, each a puzzle room then a boss: Scabaras (Kephri), Het (Akkha), Crondis (Zebak) "
					+ "and Apmeken (Ba-Ba). Finish all four, then the Wardens.")
				.line("Grab supplies from the chests between rooms.")
				.build())
			.fight(Fight.builder()
				.boss("Akkha").boss("Ba-Ba").boss("Kephri").boss("Zebak").boss("Tumeken's Warden")
				.boss("Elidinis' Warden")
				.note("Akkha", "Akkha: changes style, and spawns shadows at each quarter of health.")
				.note("Ba-Ba", "Ba-Ba: slams, rolling boulders and baboons; stay out of the falling rocks.")
				.note("Kephri", "Kephri: her shield must break first; kill the scarab swarms that heal her.")
				.note("Zebak", "Zebak: push the blood clouds and jugs; waves come across the room.")
				.note("Tumeken's Warden", "Wardens: damage the core when it's exposed, then fight the Warden.")
				.note("Elidinis' Warden", "Wardens: damage the core when it's exposed, then fight the Warden.")
				.tip("Entry Mode has no loot but no pressure either: learn every room there first.")
				.build())
			.profit(Profit.gp(3_500_000, "Uniques like the Osmumten's fang and lightbearer"))
			.build(),

		PvmGuide.builder()
			.contentId("chambers_of_xeric")
			.gear(GearSets.ranged())
			.budget(50_000_000)
			.supply(Supplies.stamina(1))
			.supply(Supplies.food(Supplies.SHARKS, 10))
			.stage(Stage.builder()
				.title("Go to Mount Quidamortem")
				.when(s -> !QUIDAMORTEM.contains(s.getLocation()) && PvmGuides.surface(s))
				.place(QUIDAMORTEM)
				.line("Xeric's talisman (Xeric's Honour) or fairy ring C-I-R takes you there.")
				.build())
			.stage(Stage.builder()
				.title("Start a raid")
				.when(PvmGuides::surface)
				.line("Join a learner team from the raids clan chat, then go down the steps at the chambers.")
				.highlight(Target.object(null, "Chambers of Xeric"))
				.build())
			.stage(Stage.builder()
				.title("Clear the rooms")
				.line("Rooms come in a random order: combat rooms (bosses) and puzzle rooms. At the end is the "
					+ "Great Olm.")
				.line("Your team makes potions inside: someone farms and picks herbs, someone brews.")
				.build())
			.fight(Fight.builder()
				.boss("Tekton").boss("Vasa Nistirio").boss("Vanguard").boss("Vespula").boss("Muttadile")
				.boss("Great Olm")
				.note("Tekton", "Tekton: big melee hits; he walks back to his anvil when out of combat.")
				.note("Vasa Nistirio", "Vasa: kill the crystals he heals from, then hit him.")
				.note("Vanguard", "Vanguards: three of them, keep their health close so none resets.")
				.note("Vespula", "Vespula: kill the portal while she flies above.")
				.note("Muttadile", "Muttadiles: the big one eats from the meat tree to heal.")
				.note("Great Olm", "Olm: kill both hands, then the head. The hands come back if one dies alone.")
				.build())
			.profit(Profit.gp(4_000_000, "Points-based uniques, including the twisted bow"))
			.build(),

		PvmGuide.builder()
			.contentId("theatre_of_blood")
			.gear(GearSets.melee())
			.budget(80_000_000)
			.supply(Supplies.meleeBoost(1))
			.supply(Supplies.brews(6))
			.supply(Supplies.superRestores(6))
			.supply(Supplies.food(Supplies.SHARKS, 6))
			.stage(Stage.builder()
				.title("Go to Ver Sinhaza")
				.when(s -> !VER_SINHAZA.contains(s.getLocation()) && PvmGuides.surface(s))
				.place(VER_SINHAZA)
				.line("Drakan's medallion has a teleport straight there.")
				.build())
			.stage(Stage.builder()
				.title("Start a raid")
				.when(PvmGuides::surface)
				.line("Form or join a party at the noticeboard and enter the theatre. Entry Mode to learn.")
				.highlight(Target.object(null, "Theatre of Blood"))
				.build())
			.stage(Stage.builder()
				.title("Clear the rooms")
				.line("Six rooms in order: Maiden, Bloat, Nylocas, Sotetseg, Xarpus, then Verzik.")
				.build())
			.fight(Fight.builder()
				.boss("The Maiden of Sugadinti").boss("Pestilent Bloat").boss("Nylocas Vasilias").boss("Sotetseg")
				.boss("Xarpus").boss("Verzik Vitur")
				.note("The Maiden of Sugadinti", "Maiden: freeze or kill the blood spawn before they reach her.")
				.note("Pestilent Bloat", "Bloat: hit him while he walks, stay under him when he stops.")
				.note("Nylocas Vasilias", "Nylocas: kill the waves with the matching style, then the boss.")
				.note("Sotetseg", "Sotetseg: walk the hidden maze when the floor goes red.")
				.note("Xarpus", "Xarpus: avoid the poison pools; don't attack while he faces you in the last phase.")
				.note("Verzik Vitur", "Verzik: three phases, ending with her spider form.")
				.build())
			.profit(Profit.gp(4_500_000, "Scythe, rapier, sanguinesti staff and other uniques"))
			.build(),

		PvmGuide.builder()
			.contentId("fortis_colosseum")
			.track("Minimus")
			.gear(GearSets.melee())
			.budget(80_000_000)
			.supply(Supplies.meleeBoost(1))
			.supply(Supplies.brews(8))
			.supply(Supplies.superRestores(8))
			.supply(Supplies.food(Supplies.SHARKS))
			.stage(Stage.builder()
				.title("Go to the Colosseum")
				.when(s -> s.npc("Minimus") == 0)
				.line("Take a quetzal or the Civitas illa Fortis teleport, then enter the Colosseum.")
				.highlight(Target.object(null, "Colosseum entrance"))
				.build())
			.stage(Stage.builder()
				.title("Start the waves")
				.line("Talk to Minimus to start. Before each wave, pick one of the offered modifiers.")
				.line("You can leave between waves and keep the rewards so far.")
				.highlight(Target.npc(null, "Minimus"))
				.build())
			.fight(Fight.builder()
				.boss("Sol Heredit")
				.note("Sol Heredit", "Sol Heredit: spear and shield combos and growing lasers; stay calm and "
					+ "move off the marked lines.")
				.tip("Pick easy modifiers on your first runs; you can cash out between waves.")
				.build())
			.profit(Profit.gp(2_000_000, "Sunfire splinters, the Dizana's quiver and wave rewards"))
			.build(),

		PvmGuide.builder()
			.contentId("inferno")
			.gear(GearSets.ranged())
			.budget(200_000_000)
			.supply(Supply.tool("Fire cape", Option.of("Fire cape", ItemID.TZHAAR_CAPE_FIRE)).withBuyable(false)
				.note("Sacrificed to enter"))
			.supply(Supplies.rangingPotions(2))
			.supply(Supplies.brews(8))
			.supply(Supplies.superRestores(12))
			.supply(Supplies.food(Supplies.ANGLERFISH))
			.stage(Stage.builder()
				.title("Get into Mor Ul Rek")
				.when(PvmGuides::surface)
				.line("Go to the TzHaar city (fairy ring B-L-P) and into the inner city, Mor Ul Rek. You need a "
					+ "fire cape on you to be let in.")
				.build())
			.stage(Stage.builder()
				.title("Enter the Inferno")
				.line("Jump into the Inferno in Mor Ul Rek and sacrifice your fire cape the first time.")
				.highlight(Target.object(null, "The Inferno"))
				.build())
			.fight(Fight.builder()
				.boss("TzKal-Zuk")
				.note("TzKal-Zuk", "Zuk: keep the shield between you and him; healers and Jad come at set points.")
				.tip("Practise the waves with the wiki's guide; no wave or prayer helpers are allowed.")
				.build())
			.profit(Profit.reward("The infernal cape"))
			.build()
	);

	/**
	 * Varbits with the God Wars kill count per god: Armadyl, Bandos, Saradomin, Zamorak.
	 */
	public static final int[] GOD_WARS_COUNTERS = {
		VarbitID.GODWARS_COUNTER_ARMADYL, VarbitID.GODWARS_COUNTER_BANDOS, VarbitID.GODWARS_COUNTER_SARADOMIN,
		VarbitID.GODWARS_COUNTER_ZAMORAK,
	};

	private static final Map<String, PvmGuide> BY_ID = ALL.stream()
		.collect(Collectors.toMap(PvmGuide::getContentId, g -> g));

	public static PvmGuide forContent(String contentId)
	{
		return BY_ID.get(contentId);
	}

	// ---- Places ----

	/**
	 * Above ground (not in a dungeon, cave city or instance far off the main map).
	 */
	static boolean surface(GuideState s)
	{
		return s.getLocation() != null && s.getLocation().getY() < 4500;
	}

	static boolean atVoidOutpost(GuideState s)
	{
		WorldPoint p = s.getLocation();
		return p != null && p.getX() >= 2624 && p.getX() <= 2690 && p.getY() >= 2624 && p.getY() <= 2690;
	}

	static boolean inPestControlGame(GuideState s)
	{
		return s.getLocation() != null && s.getLocation().getRegionID() == 10536;
	}

	static boolean inWilderness(GuideState s)
	{
		WorldPoint p = s.getLocation();
		return p != null && p.getX() >= 2944 && p.getX() <= 3391 && p.getY() >= 3520 && p.getY() <= 3967;
	}

	static boolean inKbdLair(GuideState s)
	{
		WorldPoint p = s.getLocation();
		return p != null && p.getX() >= 2240 && p.getX() <= 2303 && p.getY() >= 4672 && p.getY() <= 4735;
	}

	public static boolean inFightCave(GuideState s)
	{
		return s.getLocation() != null && s.getLocation().getRegionID() == 9551;
	}

	static boolean onUngael(GuideState s)
	{
		WorldPoint p = s.getLocation();
		return p != null && p.getX() >= 2240 && p.getX() <= 2320 && p.getY() >= 3990 && p.getY() <= 4100;
	}

	static boolean inGauntlet(GuideState s)
	{
		return Gauntlet.inside(s);
	}

	static int moonsDone(GuideState s)
	{
		return s.varbit(VarbitID.PMOON_BOSS_BLOOD_DEAD) + s.varbit(VarbitID.PMOON_BOSS_BLUE_DEAD)
			+ s.varbit(VarbitID.PMOON_BOSS_ECLIPSE_DEAD);
	}

	static List<String> moonProgress(GuideState s)
	{
		return List.of(
			(s.varbit(VarbitID.PMOON_BOSS_BLOOD_DEAD) > 0 ? "✔ " : "✘ ") + "Blood Moon",
			(s.varbit(VarbitID.PMOON_BOSS_BLUE_DEAD) > 0 ? "✔ " : "✘ ") + "Blue Moon",
			(s.varbit(VarbitID.PMOON_BOSS_ECLIPSE_DEAD) > 0 ? "✔ " : "✘ ") + "Eclipse Moon");
	}

	static List<String> moonSupplies(GuideState s)
	{
		return List.of(
			"Moonlight potions: " + s.carried(MOONLIGHT_POTIONS) + "/4",
			"Cooked bream: " + s.carried(ItemID.BREAM_FISH_COOKED) + "/10");
	}

	static boolean inMoleLair(GuideState s)
	{
		WorldPoint p = s.getLocation();
		return p != null && p.getX() >= 1728 && p.getX() <= 1791 && p.getY() >= 5120 && p.getY() <= 5247;
	}

	/**
	 * In a Barrows crypt (top floor) or the tunnels (ground floor) under the mounds.
	 */
	static boolean inBarrowsUnderground(GuideState s)
	{
		return s.getLocation() != null && s.getLocation().getRegionID() == Barrows.CRYPT_REGION;
	}

	static boolean inGodWars(GuideState s)
	{
		WorldPoint p = s.getLocation();
		return p != null && p.getX() >= 2816 && p.getX() <= 2943 && p.getY() >= 5248 && p.getY() <= 5375;
	}

	/**
	 * The Fight Cave wave list plus how many doses of each potion the player has left.
	 */
	static final class FightCave
	{
		private static final Option[] POTIONS = {
			Option.of("Prayer", ItemID._4DOSEPRAYERRESTORE, ItemID._3DOSEPRAYERRESTORE, ItemID._2DOSEPRAYERRESTORE,
				ItemID._1DOSEPRAYERRESTORE),
			Option.of("Restore", ItemID._4DOSE2RESTORE, ItemID._3DOSE2RESTORE, ItemID._2DOSE2RESTORE,
				ItemID._1DOSE2RESTORE),
			Option.of("Brew", ItemID._4DOSEPOTIONOFSARADOMIN, ItemID._3DOSEPOTIONOFSARADOMIN,
				ItemID._2DOSEPOTIONOFSARADOMIN, ItemID._1DOSEPOTIONOFSARADOMIN),
		};
		private static final Option RANGING = Option.of("ranging", ItemID._4DOSERANGERSPOTION,
			ItemID._3DOSERANGERSPOTION, ItemID._2DOSERANGERSPOTION, ItemID._1DOSERANGERSPOTION);

		/**
		 * The wave card: this wave's monsters, the wiki's tip, potion reminders and doses left.
		 */
		static WaveCard card(GuideState s)
		{
			int n = FightCaveWaves.currentWave(s);
			WaveCard.WaveCardBuilder b = WaveCard.builder()
				.wave(n)
				.total(FightCaveWaves.JAD_WAVE)
				.supplies(supplies(s));
			if (n > 0)
			{
				b.rows(FightCaveWaves.rows(n, s)).tip(FightCaveWaves.note(n));
			}
			return b.alerts(potionLines(s)).build();
		}

		/**
		 * When to drink, from the player's own prayer, hitpoints and Ranged level. Like RuneLite's own low prayer
		 * and boost reminders: nothing here looks at the monsters.
		 */
		static List<String> potionLines(GuideState s)
		{
			List<String> lines = new ArrayList<>();
			int prayer = s.getPrayer();
			int prayerDoses = doses(s, POTIONS[0]) + doses(s, POTIONS[1]);
			if (prayer <= Math.max(10, s.level(Skill.PRAYER) / 4))
			{
				lines.add(prayerDoses > 0 ? "Prayer is at " + prayer + ": sip a prayer potion or super restore."
					: "Prayer is at " + prayer + " and you're out of prayer potions.");
			}
			boolean lowHp = s.getMaxHitpoints() > 0 && s.getHitpoints() < s.getMaxHitpoints() / 2;
			if (lowHp && doses(s, POTIONS[2]) > 0)
			{
				lines.add("Hitpoints are at " + s.getHitpoints() + ": sip a Saradomin brew (or eat).");
			}
			int ranged = s.level(Skill.RANGED);
			int boost = s.boosted(Skill.RANGED) - ranged;
			if (boost < 0 && doses(s, POTIONS[1]) > 0)
			{
				lines.add("Brews have drained your Ranged (" + s.boosted(Skill.RANGED) + "/" + ranged
					+ "): sip a super restore.");
			}
			else if (boost < 4 && doses(s, RANGING) > 0)
			{
				lines.add(boost <= 0 ? "Your Ranged boost has worn off: sip a ranging potion."
					: "Ranged boost is down to +" + boost + ": sip a ranging potion.");
			}
			return lines;
		}

		private static int doses(GuideState s, Option potion)
		{
			int doses = 0;
			int[] ids = potion.getIds();
			for (int i = 0; i < ids.length; i++)
			{
				doses += s.inventory(ids[i]) * (4 - i);
			}
			return doses;
		}

		/**
		 * e.g. "Prayer 31 · Restore 8 · Brew 20" (doses). Ids are listed 4-dose first.
		 */
		static String supplies(GuideState s)
		{
			List<String> parts = new ArrayList<>();
			for (Option potion : POTIONS)
			{
				parts.add(potion.getName() + " " + doses(s, potion));
			}
			return String.join(" · ", parts);
		}
	}

	/**
	 * The six brothers: which have been killed this run, which one has the tunnel, and every step of a run from
	 * digging into the first mound to leaving with the loot.
	 */
	public static final class Barrows
	{
		public static final int CRYPT_REGION = 14231;
		public static final String TUNNEL_FLAG = "barrows-tunnel:";
		/**
		 * Set once the chest has been looted, until the player is back above ground.
		 */
		public static final String LOOTED_FLAG = "barrows-looted";

		static final List<String> NAMES = List.of("Ahrim the Blighted", "Dharok the Wretched", "Guthan the Infested",
			"Karil the Tainted", "Torag the Corrupted", "Verac the Defiled");

		/**
		 * Mound dig spots, the same ones RuneLite's Barrows plugin uses.
		 */
		public static final List<Location> MOUNDS = List.of(
			Location.of("Ahrim's mound", 3566, 3289, 2),
			Location.of("Dharok's mound", 3575, 3298, 2),
			Location.of("Guthan's mound", 3577, 3283, 2),
			Location.of("Karil's mound", 3566, 3275, 2),
			Location.of("Torag's mound", 3553, 3283, 2),
			Location.of("Verac's mound", 3557, 3298, 2));

		public static final int[] KILLED = {
			VarbitID.BARROWS_KILLED_AHRIM, VarbitID.BARROWS_KILLED_DHAROK, VarbitID.BARROWS_KILLED_GUTHAN,
			VarbitID.BARROWS_KILLED_KARIL, VarbitID.BARROWS_KILLED_TORAG, VarbitID.BARROWS_KILLED_VERAC,
		};

		/**
		 * Each brother's sarcophagus, to tell which crypt the player is in.
		 */
		public static final int[] SARCOPHAGI = {
			ObjectID.BARROW_AHRIM_SARCOPHAGUS, ObjectID.BARROW_DHAROK_SARCOPHAGUS, ObjectID.BARROW_GUTHAN_SARCOPHAGUS,
			ObjectID.BARROW_KARIL_SARCOPHAGUS, ObjectID.BARROW_TORAG_SARCOPHAGUS, ObjectID.BARROW_VERAC_SARCOPHAGUS,
		};

		/**
		 * The wiki's simple clockwise rotation (Dharok, Guthan, Karil, Torag, Verac, Ahrim): tied for the least
		 * running between mounds.
		 */
		private static final int[] ROTATION = {1, 2, 3, 4, 5, 0};

		/**
		 * What each brother does, from the wiki. Descriptions only.
		 */
		private static final String[] STYLES = {
			"Ahrim attacks with Magic and can lower your stats. Weak to Ranged and Melee.",
			"Dharok attacks with Melee and hits harder the lower his hitpoints get. Weak to Magic.",
			"Guthan attacks with Melee and heals himself with some of his hits. Weak to Magic.",
			"Karil attacks with Ranged and can lower your Agility. Weak to Magic and Melee.",
			"Torag attacks with Melee and can drain your run energy. Weak to Magic.",
			"Verac attacks with Melee and some of his hits go through armour and prayer. Weak to Magic.",
		};

		/**
		 * An air staff to swap to for the brothers weak to Magic (all but Ahrim). Wind spells hit their elemental
		 * weakness and the staff pays for the air runes. The staff of air's level is the first wind spell worth
		 * bringing: Wind Bolt at 17.
		 */
		static final GearSlot STAFF = GearSlot.swap("magic weapon",
			buy("Smoke battlestaff", Skill.MAGIC, 40, 0, true, ItemID.SMOKE_BATTLESTAFF),
			buy("Mystic air staff", Skill.MAGIC, 40, 0, true, ItemID.MYSTIC_AIR_STAFF),
			buy("Air battlestaff", Skill.MAGIC, 30, 0, true, ItemID.AIR_BATTLESTAFF),
			buy("Staff of air", Skill.MAGIC, 17, 0, false, ItemID.STAFF_OF_AIR));

		/**
		 * The wind spells worth casting (Wind Bolt and up), best first, from the shared spell table. Barrows sticks
		 * to wind even when a fire spell of the same level hits harder, because the brothers are weak to air.
		 */
		private static final List<CombatSpell> SPELLS = java.util.Arrays.stream(CombatSpell.values())
			.filter(spell -> spell.getElement() == null && spell.getLevel() >= CombatSpell.WIND_BOLT.getLevel())
			.collect(Collectors.toList());

		static final List<Supply> RUNES = SPELLS.stream().map(Barrows::catalyst).collect(Collectors.toList());

		/**
		 * The rune a wind spell uses besides air, and how many to bring for five brothers plus a few tunnel
		 * monsters: fewer casts are needed as the spells hit harder.
		 */
		private static Supply catalyst(CombatSpell spell)
		{
			CombatSpell.Catalyst rune = spell.getCatalyst();
			int amount = rune == CombatSpell.Catalyst.CHAOS ? 200 : rune == CombatSpell.Catalyst.DEATH ? 150 : 100;
			return Supply.stack(rune.getRuneName(), amount, Option.of(rune.getRuneName(), rune.getRune()));
		}

		private static CombatSpell spell(GuideState s)
		{
			for (CombatSpell spell : SPELLS)
			{
				if (s.level(Skill.MAGIC) >= spell.getLevel() && (s.isMembers() || !spell.isMembers()))
				{
					return spell;
				}
			}
			return null;
		}

		static List<Supply> runes(GuideState s)
		{
			CombatSpell spell = spell(s);
			if (spell == null)
			{
				return List.of();
			}
			Supply runes = RUNES.get(SPELLS.indexOf(spell));
			return List.of(runes.withRestockBelow(runes.getAmount() / 4).note("For " + spell.getSpellName()));
		}

		/**
		 * The weapon to hold for this brother: melee for Ahrim, the air staff for the rest. Null (any weapon) when
		 * the player can't cast a wind spell yet.
		 */
		static WeaponSwap weaponFor(GuideState s, int brother)
		{
			CombatSpell spell = spell(s);
			if (brother < 0 || spell == null)
			{
				return null;
			}
			if (brother == 0)
			{
				return new WeaponSwap(GearSets.MELEE_WEAPON, "Ahrim is weak to Melee", null);
			}
			return new WeaponSwap(STAFF, shortName(brother) + " is weak to Magic",
				"Autocast " + spell.getSpellName() + " on your staff (combat tab).");
		}

		static WeaponSwap fightWeapon(GuideState s)
		{
			for (int i = 0; i < NAMES.size(); i++)
			{
				if (s.npc(NAMES.get(i)) > 0)
				{
					return weaponFor(s, i);
				}
			}
			return null;
		}

		private static final Location CHEST_ROOM = Location.of("the chest room", 3551, 9695, 0, 4);
		private static final WorldPoint BARROWS = new WorldPoint(3565, 3289, 0);
		private static final String FEARLESS = "Yeah I'm fearless!";
		private static final String SCARED = "No way";

		static Map<String, String> notes()
		{
			Map<String, String> notes = new java.util.LinkedHashMap<>();
			for (int i = 0; i < NAMES.size(); i++)
			{
				notes.put(NAMES.get(i), STYLES[i]);
			}
			return notes;
		}

		static boolean killed(GuideState s, int i)
		{
			return s.varbit(KILLED[i]) > 0;
		}

		static boolean tunnel(GuideState s, int i)
		{
			return s.flag(TUNNEL_FLAG + i);
		}

		static boolean looted(GuideState s)
		{
			return s.flag(LOOTED_FLAG);
		}

		static int killedCount(GuideState s)
		{
			int n = 0;
			for (int i = 0; i < KILLED.length; i++)
			{
				n += killed(s, i) ? 1 : 0;
			}
			return n;
		}

		/**
		 * Every brother but this one is dead.
		 */
		static boolean othersKilled(GuideState s, int brother)
		{
			for (int i = 0; i < KILLED.length; i++)
			{
				if (i != brother && !killed(s, i))
				{
					return false;
				}
			}
			return true;
		}

		/**
		 * The brother whose crypt has the tunnel: the one remembered from the sarcophagus, or the last one left.
		 */
		static int tunnelBrother(GuideState s)
		{
			int left = -1;
			int count = 0;
			for (int i = 0; i < KILLED.length; i++)
			{
				if (tunnel(s, i))
				{
					return i;
				}
				if (!killed(s, i))
				{
					left = i;
					count++;
				}
			}
			return count == 1 ? left : -1;
		}

		/**
		 * The next mound to dig into, going round in the rotation and leaving the tunnel crypt for last.
		 */
		static int nextMound(GuideState s)
		{
			int tunnel = tunnelBrother(s);
			for (int i : ROTATION)
			{
				if (!killed(s, i) && !tunnel(s, i) && tunnel != i)
				{
					return i;
				}
			}
			return -1;
		}

		static String shortName(int i)
		{
			return NAMES.get(i).split(" ")[0];
		}

		static List<String> progress(GuideState s)
		{
			List<String> out = new ArrayList<>();
			out.add("Brothers down: " + killedCount(s) + "/6");
			for (int i = 0; i < NAMES.size(); i++)
			{
				out.add((killed(s, i) ? "✔ " : tunnel(s, i) ? "➜ " : "✘ ") + shortName(i)
					+ (tunnel(s, i) ? " (tunnel)" : ""));
			}
			return out;
		}

		private static boolean inCrypt(GuideState s)
		{
			return inBarrowsUnderground(s) && s.getLocation().getPlane() == 3;
		}

		private static boolean inTunnels(GuideState s)
		{
			return inBarrowsUnderground(s) && s.getLocation().getPlane() == 0;
		}

		private static boolean knownCrypt(GuideState s)
		{
			return inCrypt(s) && s.getCrypt() >= 0;
		}

		private static List<String> withProgress(GuideState s, List<String> first)
		{
			List<String> out = new ArrayList<>(first);
			out.addAll(progress(s));
			return out;
		}

		static List<Stage> stages()
		{
			List<Stage> stages = new ArrayList<>();
			Function<GuideState, List<String>> live = Barrows::progress;

			// ---- Done: get out ----
			stages.add(Stage.builder()
				.title("Leave the tunnels")
				.when(s -> inBarrowsUnderground(s) && looted(s))
				.committed(true)
				.line("Chest looted! Break your Barrows teleport tablet (outlined in your inventory) to leave.")
				.line("No tablet? Climb the outlined ladder. Brothers that jump out now don't count, so don't fight "
					+ "them.")
				.use(ItemID.TELETAB_BARROWS)
				.highlight(Target.object("Climb-up", "Ladder"))
				.build());

			// ---- In a crypt ----
			stages.add(Stage.builder()
				.title("Go down into the tunnels")
				.when(s -> knownCrypt(s) && tunnelBrother(s) == s.getCrypt() && othersKilled(s, s.getCrypt()))
				.committed(true)
				.line("Search the outlined sarcophagus, then click \"" + FEARLESS + "\" to drop into the tunnels.")
				.highlight(Target.object("Search", "Sarcophagus"))
				.dialogOption(FEARLESS)
				.live(live)
				.build());
			stages.add(Stage.builder()
				.title("Tunnel crypt: come back last")
				.when(s -> knownCrypt(s) && tunnelBrother(s) == s.getCrypt() && !killed(s, s.getCrypt()))
				.committed(true)
				.line("This crypt has the tunnel to the chest. If you're asked, click \"No way\", then climb the "
					+ "outlined staircase.")
				.line("Come back here once the other brothers are dead.")
				.highlight(Target.object("Climb-up", "Staircase"))
				.dialogOption(SCARED)
				.live(live)
				.build());
			stages.add(Stage.builder()
				.title("Climb out of the crypt")
				.when(s -> knownCrypt(s) && killed(s, s.getCrypt()))
				.committed(true)
				.live(s -> withProgress(s, List.of(shortName(s.getCrypt()) + " is down. Climb the outlined staircase "
					+ "back up.")))
				.highlight(Target.object("Climb-up", "Staircase"))
				.build());
			stages.add(Stage.builder()
				.title("Search the sarcophagus")
				.when(Barrows::knownCrypt)
				.committed(true)
				.wield(s -> weaponFor(s, s.getCrypt()))
				.live(s -> withProgress(s, List.of(
					"Search the outlined sarcophagus: " + shortName(s.getCrypt()) + " climbs out. Kill him, then "
						+ "climb the stairs.",
					STYLES[s.getCrypt()],
					"If it asks about a hidden tunnel instead, this is the tunnel crypt: I'll remember it.")))
				.highlight(Target.object("Search", "Sarcophagus"))
				.dialogOption(SCARED)
				.build());
			stages.add(Stage.builder()
				.title("Search the sarcophagus")
				.when(Barrows::inCrypt)
				.committed(true)
				.line("Search the sarcophagus to wake the brother and kill him, then climb the staircase back up.")
				.line("If it says there's a hidden tunnel, that's the tunnel crypt: say No and come back last.")
				.highlight(Target.object("Search", "Sarcophagus"))
				.highlight(Target.object("Climb-up", "Staircase"))
				.live(live)
				.build());

			// ---- In the tunnels ----
			stages.add(Stage.builder()
				.title("Open the chest")
				.when(s -> inTunnels(s) && s.getTunnel() != null && s.getTunnel().atChest())
				.committed(true)
				.wield(s -> tunnelBrother(s) < 0 || killed(s, tunnelBrother(s)) ? null : weaponFor(s, tunnelBrother(s)))
				.live(s ->
				{
					int last = tunnelBrother(s);
					String who = last >= 0 && !killed(s, last) ? shortName(last) : "the last brother";
					return withProgress(s, List.of(
						"Click the outlined chest to open it. " + who + " climbs out if he hasn't shown up yet: kill "
							+ "him.",
						"Then click the chest again to search it and take your loot."));
				})
				.tiles(s -> s.getTunnel().getPath())
				.highlight(Target.object("Open", "Chest"))
				.highlight(Target.object("Search", "Chest"))
				.build());
			stages.add(Stage.builder()
				.title("Follow the path to the chest")
				.when(s -> inTunnels(s) && s.getTunnel() != null)
				.committed(true)
				.goTo(s -> new Location("the next door", s.getTunnel().getDoor(), 0, false, false))
				.local(true)
				.tiles(s -> s.getTunnel().getPath())
				.objectAt(s -> s.getTunnel().getDoor())
				.line("Follow the marked tiles and click Open on the outlined door.")
				.line("If a picture puzzle pops up, click the answer outlined in green.")
				.line("A monster may jump out of a door: kill it for a better chest, or just keep going.")
				.live(live)
				.build());
			stages.add(Stage.builder()
				.title("Get to the chest")
				.when(Barrows::inTunnels)
				.committed(true)
				.place(CHEST_ROOM)
				.local(true)
				.line("Head for the middle room. Only some doors open (they're outlined): try the ones towards the "
					+ "middle.")
				.line("If a picture puzzle pops up, click the answer outlined in green.")
				.highlight(Target.object("Open", "Door"))
				.highlight(Target.object("Open", "Chest"))
				.highlight(Target.object("Search", "Chest"))
				.live(live)
				.build());

			// ---- Above ground ----
			stages.add(Stage.builder()
				.title("Teleport to the Barrows")
				.when(s -> !inBarrowsUnderground(s) && s.getLocation() != null
					&& s.getLocation().distanceTo2D(BARROWS) > 60 && s.inventory(ItemID.TELETAB_BARROWS) > 0)
				.line("Break a Barrows teleport tablet (outlined in your inventory).")
				.use(ItemID.TELETAB_BARROWS)
				.build());
			for (int i = 0; i < MOUNDS.size(); i++)
			{
				int b = i;
				stages.add(Stage.builder()
					.title("Dig into " + MOUNDS.get(i).getName())
					.when(s -> nextMound(s) == b)
					.place(MOUNDS.get(i))
					.wield(s -> weaponFor(s, b))
					.tiles(s -> List.of(MOUNDS.get(b).getPoint()))
					.line("Stand on the marked tile and click your spade (outlined) to dig.")
					.line(STYLES[i])
					.line("Top up prayer and hitpoints before you dig: the crypt drains prayer.")
					.use(ItemID.SPADE)
					.live(live)
					.build());
			}
			for (int i = 0; i < MOUNDS.size(); i++)
			{
				int b = i;
				stages.add(Stage.builder()
					.title("Enter the tunnel from " + MOUNDS.get(i).getName())
					.when(s -> tunnelBrother(s) == b)
					.place(MOUNDS.get(i))
					.wield(s -> weaponFor(s, b))
					.tiles(s -> List.of(MOUNDS.get(b).getPoint()))
					.line("Last one: this mound has the tunnel. Stand on the marked tile and dig with your spade.")
					.line("Then search the sarcophagus and click \"" + FEARLESS + "\".")
					.use(ItemID.SPADE)
					.live(live)
					.build());
			}
			return stages;
		}

		private Barrows()
		{
		}
	}

	/**
	 * Preparation inside the Gauntlet, counted from the inventory. Normal and corrupted items share one target.
	 */
	static final class Gauntlet
	{
		private static final String[] MONSTERS = {
			"Crystalline Bat", "Crystalline Bear", "Crystalline Dark Beast", "Crystalline Dragon", "Corrupted Bat",
			"Corrupted Bear", "Corrupted Dark Beast", "Corrupted Dragon", "Crystalline Hunllef", "Corrupted Hunllef",
		};

		private static final Object[][] TARGETS = {
			{"Shards", 400, new int[]{ItemID.GAUNTLET_CRYSTAL_SHARD, ItemID.GAUNTLET_CRYSTAL_SHARD_HM}},
			{"Ore", 3, new int[]{ItemID.GAUNTLET_ORE, ItemID.GAUNTLET_ORE_HM}},
			{"Phren bark", 3, new int[]{ItemID.GAUNTLET_BARK, ItemID.GAUNTLET_BARK_HM}},
			{"Linum tirinum", 3, new int[]{ItemID.GAUNTLET_FIBRE, ItemID.GAUNTLET_FIBRE_HM}},
			{"Weapon frames", 2, new int[]{ItemID.GAUNTLET_GENERIC_COMPONENT, ItemID.GAUNTLET_GENERIC_COMPONENT_HM}},
			{"Grym leaves", 2, new int[]{ItemID.GAUNTLET_HERB, ItemID.GAUNTLET_HERB_HM}},
			{"Raw paddlefish", 20, new int[]{ItemID.GAUNTLET_RAW_FOOD, ItemID.GAUNTLET_FOOD}},
		};

		static boolean inside(GuideState s)
		{
			for (String m : MONSTERS)
			{
				if (s.npc(m) > 0)
				{
					return true;
				}
			}
			for (Object[] t : TARGETS)
			{
				if (s.carried((int[]) t[2]) > 0)
				{
					return true;
				}
			}
			return s.carried(ItemID.GAUNTLET_TELEPORT_CRYSTAL, ItemID.GAUNTLET_TELEPORT_CRYSTAL_HM) > 0;
		}

		static boolean prepped(GuideState s)
		{
			for (Object[] t : TARGETS)
			{
				if (s.carried((int[]) t[2]) < (int) t[1])
				{
					return false;
				}
			}
			return true;
		}

		static List<String> progress(GuideState s)
		{
			List<String> out = new ArrayList<>();
			for (Object[] t : TARGETS)
			{
				int have = s.carried((int[]) t[2]);
				out.add((have >= (int) t[1] ? "✔ " : "• ") + t[0] + ": " + have + "/" + t[1]);
			}
			return out;
		}

		private Gauntlet()
		{
		}
	}

	/**
	 * Kill count towards each general's door.
	 */
	static final class GodWars
	{
		private static final String[] GODS = {"Armadyl", "Bandos", "Saradomin", "Zamorak"};
		private static final int[] COUNTERS = GOD_WARS_COUNTERS;

		static int best(GuideState s)
		{
			int best = 0;
			for (int c : COUNTERS)
			{
				best = Math.max(best, s.varbit(c));
			}
			return best;
		}

		static List<String> progress(GuideState s)
		{
			List<String> out = new ArrayList<>();
			for (int i = 0; i < GODS.length; i++)
			{
				int kc = s.varbit(COUNTERS[i]);
				out.add((kc >= 40 ? "✔ " : "• ") + GODS[i] + ": " + kc + "/40");
			}
			return out;
		}

		private GodWars()
		{
		}
	}

	private PvmGuides()
	{
	}
}
