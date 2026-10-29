package com.skillinghelper.pvm.guide;

import static com.skillinghelper.pvm.guide.Gear.buy;
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

	private static final GearSlot ANTI_DRAGON_SHIELD = GearSlot.of("shield",
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
			.arrival("Cross the gangplank onto the novice boat (combat 40+) and wait for a game.")
			.arrival("In the game: kill the pests near the portals and the portals once their shields drop.")
			.enter(Target.object("Cross", "Gangplank"))
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
				.place(Location.of("the Warriors' Guild animator", 2851, 3536, 6))
				.line("Use your mithril armour on the Magical Animator and kill the armour it makes. Pick the "
					+ "pieces back up and repeat until you have 100+ tokens (10 a minute upstairs).")
				.line("Then climb to the top floor and kill cyclopes until a defender drops; show it to Lorelai for "
					+ "the next tier.")
				.highlight(Target.object(null, "Magical Animator"))
				.highlight(Target.npc("Attack", "Cyclops"))
				.build())
			.fight(Fight.builder()
				.boss("Cyclops")
				.tip("Keep a defender in your shield slot so the next tier can drop.")
				.build())
			.profit(Profit.reward("Bronze to dragon defenders, the best melee off-hand for a long time"))
			.build(),

		PvmGuide.builder()
			.contentId("barrows")
			.gear(GearSets.melee())
			.budget(5_000_000)
			.supply(Supplies.spade())
			.supply(Supplies.prayerPotions(6))
			.supply(Supplies.teleport("Barrows teleports", 1, Option.of("Barrows teleport tablets",
				ItemID.TELETAB_BARROWS)).note("To get back next run"))
			.supply(Supplies.food(Supplies.MONKFISH))
			.inside(PvmGuides::inBarrowsUnderground)
			.stages(Barrows.stages())
			.place(Location.of("the Barrows", 3565, 3289, 20))
			.fight(Fight.builder()
				.bosses(Barrows.NAMES)
				.live(Barrows::progress)
				.tip("Melee works on all six. Pray Protect from Melee for Dharok, Guthan, Torag and Verac, "
					+ "Protect from Missiles for Karil and Protect from Magic for Ahrim.")
				.tip("Prayer drains slowly in the crypts and tunnels, so drink a dose when it's low.")
				.tip("Turn on RuneLite's Barrows plugin: it shows the puzzle door answers in the tunnels.")
				.build())
			.profit(Profit.gp(1_100_000, "Runes, bolt racks and a chance at Barrows armour from each chest"))
			.build(),

		PvmGuide.builder()
			.contentId("barbarian_assault")
			.gear(GearSets.melee())
			.budget(2_000_000)
			.arrival("Head down the ladder in the Barbarian Outpost and join a team of five in the lobby.")
			.arrival("Most players find teams in the official BA clan chat or on world 306.")
			.enter(Target.object(null, "Ladder"))
			.profit(Profit.reward("Fighter torso and the Penance gear from honour points"))
			.build(),

		PvmGuide.builder()
			.contentId("sarachnis")
			.gear(GearSets.melee())
			.budget(10_000_000)
			.supply(Supplies.prayerPotions(4))
			.supply(Supplies.meleeBoost(1))
			.supply(Supplies.food(Supplies.MONKFISH))
			.arrival("Go through the thick web in Forthos Dungeon to reach Sarachnis' lair.")
			.enter(Target.object(null, "Thick Web"))
			.fight(Fight.builder()
				.boss("Sarachnis")
				.add(new Fight.Add("Spawn of Sarachnis", "Her spawn are up: kill them so they don't add damage."))
				.tip("Pray Protect from Melee; she switches to ranged when you're not next to her.")
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
			.arrival("Pull the lever in Edgeville to reach the Deserted Keep, then pull the lever there to enter "
				+ "the lair. It's deep Wilderness: only bring what you can lose.")
			.enter(Target.object("Pull", "Lever"))
			.fight(Fight.builder()
				.boss("King Black Dragon")
				.tip("Wear the anti-dragon shield and drink the antifire before going in.")
				.tip("Pray Protect from Melee; his breath attacks are covered by the shield and potion.")
				.build())
			.profit(Profit.gp(500_000, "Dragon bones, black dragonhides and the KBD heads for Slayer"))
			.build(),

		PvmGuide.builder()
			.contentId("moons_of_peril")
			.gear(GearSets.melee())
			.budget(10_000_000)
			.supply(Supplies.prayerPotions(4))
			.supply(Supplies.food(Supplies.SHARKS, 10))
			.arrival("In Neypotzli, use the supply area to catch and cook bream and make moonlight potions.")
			.arrival("Each moon has its own room; kill all three, then loot the Lunar Chest.")
			.fight(Fight.builder()
				.boss("Blood Moon").boss("Blue Moon").boss("Eclipse Moon")
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
			.arrival("Climb down the tunnel to the Kalphite Lair and head for the Queen's room.")
			.enter(Target.object(null, "Tunnel"))
			.fight(Fight.builder()
				.boss("Kalphite Queen")
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
			.supply(Supplies.teleport("Zul-andra teleports", 1, Option.of("Zul-andra teleport scrolls",
				ItemID.TELEPORTSCROLL_ZULANDRA)))
			.supply(Supplies.food(Supplies.SHARKS))
			.arrival("Take the boat from the jetty in Zul-Andra to Zulrah's shrine.")
			.enter(Target.object(null, "Sacrificial boat"))
			.fight(Fight.builder()
				.boss("Zulrah")
				.add(new Fight.Add("Snakeling", "Snakelings are up: they're weak, kill them if they get in your way."))
				.tip("Learn the rotations from the wiki before you go; this plugin won't call them out (Jagex rules).")
				.tip("Ranged works on every form; most players add a magic weapon for the green form.")
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
			.arrival("Enter the cave in the north-west of Mor Ul Rek's outer city.")
			.enter(Target.object("Enter", "Cave entrance"))
			.fight(Fight.builder()
				.boss("TzTok-Jad")
				.add(new Fight.Add("Yt-HurKot", "Jad's healers are up: hit each one once so they follow you."))
				.tip("63 waves. Pray against what's hitting you; Jad's prayer switches are up to you (no helpers "
					+ "allowed).")
				.tip("Logging out after a wave saves your progress at the next wave.")
				.build())
			.profit(Profit.reward("The fire cape, and Tokkul"))
			.build(),

		PvmGuide.builder()
			.contentId("vorkath")
			.gear(GearSets.with(GearSets.ranged(), ANTI_DRAGON_SHIELD))
			.budget(30_000_000)
			.supply(Supplies.antifire(2))
			.supply(Supplies.antiVenom(1))
			.supply(Supplies.rangingPotions(1))
			.supply(Supplies.prayerPotions(3))
			.supply(Supplies.teleport("House teleports", 1, Option.of("Teleport to house tablets",
				ItemID.POH_TABLET_TELEPORTTOHOUSE)))
			.supply(Supplies.food(Supplies.SHARKS))
			.arrival("Talk to Torfinn on the Rellekka pier to sail to Ungael, then climb over the ice chunks.")
			.enter(Target.npc("Travel", "Torfinn"))
			.fight(Fight.builder()
				.boss("Vorkath")
				.add(new Fight.Add("Zombified Spawn", "The zombified spawn is up: kill it before it reaches you "
					+ "(Crumble Undead does it in one cast)."))
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
				.title("Get 40 kill count")
				.when(s -> inGodWars(s) && GodWars.best(s) < 40)
				.committed(true)
				.line("Kill the followers of the god whose general you want until that god shows 40.")
				.live(GodWars::progress)
				.build())
			.stage(Stage.builder()
				.title("Enter the general's room")
				.when(PvmGuides::inGodWars)
				.committed(true)
				.line("Go through the big door at the end of that god's area.")
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
			.arrival("Bring nothing: you gather and craft all your gear and food inside.")
			.arrival("Enter the Gauntlet in north-east Prifddinas and pick the normal or corrupted version.")
			.fight(Fight.builder()
				.boss("Crystalline Hunllef").boss("Corrupted Hunllef")
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
			.arrival("Start at the raid entrance in the Necropolis. Pick Entry Mode for your first runs.")
			.profit(Profit.gp(3_500_000, "Uniques like the Osmumten's fang and lightbearer"))
			.build(),

		PvmGuide.builder()
			.contentId("chambers_of_xeric")
			.gear(GearSets.ranged())
			.budget(50_000_000)
			.supply(Supplies.stamina(1))
			.supply(Supplies.food(Supplies.SHARKS, 10))
			.arrival("Go up Mount Quidamortem and join a learner team at the entrance; potions are made inside.")
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
			.arrival("Start at Ver Sinhaza. Do Entry Mode with a team first.")
			.profit(Profit.gp(4_500_000, "Scythe, rapier, sanguinesti staff and other uniques"))
			.build(),

		PvmGuide.builder()
			.contentId("fortis_colosseum")
			.gear(GearSets.melee())
			.budget(80_000_000)
			.supply(Supplies.meleeBoost(1))
			.supply(Supplies.brews(8))
			.supply(Supplies.superRestores(8))
			.supply(Supplies.food(Supplies.SHARKS))
			.arrival("Enter the Fortis Colosseum in Civitas illa Fortis and pick a modifier before each wave.")
			.fight(Fight.builder()
				.boss("Sol Heredit")
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
			.arrival("Enter the Inferno in Mor Ul Rek, the inner TzHaar city.")
			.fight(Fight.builder()
				.boss("TzKal-Zuk")
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
	 * The six brothers: which have been killed this run, and which one has the tunnel.
	 */
	public static final class Barrows
	{
		public static final int CRYPT_REGION = 14231;
		public static final String TUNNEL_FLAG = "barrows-tunnel:";

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

		private static final Location CHEST_ROOM = Location.of("the chest room", 3551, 9695, 0, 4);

		static boolean killed(GuideState s, int i)
		{
			return s.varbit(KILLED[i]) > 0;
		}

		static boolean tunnel(GuideState s, int i)
		{
			return s.flag(TUNNEL_FLAG + i);
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

		static List<String> progress(GuideState s)
		{
			List<String> out = new ArrayList<>();
			out.add("Brothers down: " + killedCount(s) + "/6");
			for (int i = 0; i < NAMES.size(); i++)
			{
				String name = NAMES.get(i).split(" ")[0];
				out.add((killed(s, i) ? "✔ " : tunnel(s, i) ? "➜ " : "✘ ") + name
					+ (tunnel(s, i) ? " (tunnel)" : ""));
			}
			return out;
		}

		static List<Stage> stages()
		{
			List<Stage> stages = new ArrayList<>();
			Function<GuideState, List<String>> live = Barrows::progress;
			stages.add(Stage.builder()
				.title("Search the sarcophagus")
				.when(s -> inBarrowsUnderground(s) && s.getLocation().getPlane() == 3)
				.committed(true)
				.line("Search the sarcophagus to wake the brother and kill him, then climb the staircase back up.")
				.line("If it says there's a hidden tunnel, that's the tunnel crypt: say No and come back last.")
				.highlight(Target.object("Search", "Sarcophagus"))
				.highlight(Target.object("Climb-up", "Staircase"))
				.live(live)
				.build());
			stages.add(Stage.builder()
				.title("Get to the chest")
				.when(s -> inBarrowsUnderground(s) && !CHEST_ROOM.contains(s.getLocation()))
				.committed(true)
				.place(CHEST_ROOM)
				.line("Head for the middle room. Each door asks a puzzle (RuneLite's Barrows plugin can show the "
					+ "answer).")
				.live(live)
				.build());
			stages.add(Stage.builder()
				.title("Open the chest")
				.when(PvmGuides::inBarrowsUnderground)
				.committed(true)
				.line("Open the chest; the last brother comes out. Kill him, search the chest, then teleport out.")
				.highlight(Target.object(null, "Chest"))
				.live(live)
				.build());
			for (int i = 0; i < MOUNDS.size(); i++)
			{
				int b = i;
				stages.add(Stage.builder()
					.title("Dig into " + MOUNDS.get(i).getName())
					.when(s -> !killed(s, b) && !tunnel(s, b) && tunnelBrother(s) != b)
					.place(MOUNDS.get(i))
					.line("Stand on top of the mound and dig with your spade.")
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
					.line("Dig into this mound, search the sarcophagus and say Yes to enter the tunnel.")
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
