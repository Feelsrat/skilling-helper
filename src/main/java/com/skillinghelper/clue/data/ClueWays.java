package com.skillinghelper.clue.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.diary.data.DiaryStage;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.diary.data.Zone;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Ways to get a clue scroll of each tier that the plugin can guide, step by step, for someone who has never done it,
 * plus tips for the ones it can't. Built as diary-style tasks so the shared guide engine handles buying, banking,
 * walking and outlining. Rates are from the wiki (per kill, pickpocket or jar).
 */
public final class ClueWays
{
	/**
	 * Jar ids: baby, young, gourmet, earth, essence, eclectic, nature, magpie, ninja, dragon (II_CAPTURED_IMPLING_n).
	 */
	static final int YOUNG_JAR = ItemID.II_CAPTURED_IMPLING_2;
	static final int GOURMET_JAR = ItemID.II_CAPTURED_IMPLING_3;
	static final int ESSENCE_JAR = ItemID.II_CAPTURED_IMPLING_5;
	static final int ECLECTIC_JAR = ItemID.II_CAPTURED_IMPLING_6;
	static final int MAGPIE_JAR = ItemID.II_CAPTURED_IMPLING_8;
	static final int NINJA_JAR = ItemID.II_CAPTURED_IMPLING_9;
	static final int DRAGON_JAR = ItemID.II_CAPTURED_IMPLING_10;

	public static final Location WATSON = Location.of("Watson's house, Hosidius", 1646, 3574, 0, 4);
	private static final Location LUMBRIDGE_GOBLINS = Location.of("Goblins east of Lumbridge", 3250, 3235, 0, 10);
	private static final Location DARK_WIZARDS = Location.of("Dark wizards' stone circle, south of Varrock", 3226, 3369,
		0, 8);
	private static final Location HAM_TRAPDOOR = Location.of("H.A.M. Hideout trapdoor, west of Lumbridge", 3166, 3251, 0,
		3);
	private static final Zone HAM_HIDEOUT = Zone.of(3137, 9600, 3199, 9663, 0);
	private static final Location CIVITAS = Location.of("Wealthy citizens, Civitas illa Fortis", 1690, 3095, 0, 15);
	private static final Location FALADOR_GUARDS = Location.of("Guards by Falador's west gate", 2966, 3392, 0, 10);

	private static final Map<ClueTier, List<DiaryTask>> WAYS = new EnumMap<>(ClueTier.class);
	private static final Map<ClueTier, List<String>> TIPS = new EnumMap<>(ClueTier.class);

	static
	{
		WAYS.put(ClueTier.BEGINNER, List.of(
			kill(ClueTier.BEGINNER, "goblins", "Kill goblins east of Lumbridge (1/64)", 0, LUMBRIDGE_GOBLINS, "Goblin",
				"About 1 in 64 goblins drops one. They're weak and fight back for 1 damage at most."),
			kill(ClueTier.BEGINNER, "darkwizards", "Kill dark wizards south of Varrock (1/35 to 1/50)", 20, DARK_WIZARDS,
				"Dark wizard", "They hit with magic, so bring some food (shrimps, trout) and eat when you drop below half."),
			jars(ClueTier.BEGINNER, "young", "Loot young impling jars (1/25, plus 1/50 easy)", YOUNG_JAR, "Young impling jar",
				25)));
		TIPS.put(ClueTier.BEGINNER, List.of(
			"The quest X Marks the Spot gives you a beginner clue at the end (if you hold none).",
			"While skilling, fishing, mining and woodcutting can give a clue bottle, geode or nest: open it for a clue.",
			"Almost every low-level monster can drop one; the full list is below."));

		WAYS.put(ClueTier.EASY, List.of(
			jars(ClueTier.EASY, "gourmet", "Loot gourmet impling jars (1/25)", GOURMET_JAR, "Gourmet impling jar", 25),
			ham(),
			citizens(),
			jars(ClueTier.EASY, "young", "Loot young impling jars (1/50)", YOUNG_JAR, "Young impling jar", 50)));
		TIPS.put(ClueTier.EASY, List.of(
			"Wear H.A.M. robes in the hideout: each piece makes it less likely you're thrown out when caught.",
			"Finishing the Easy Combat Achievements gives 5% more easy clues (not from implings).",
			"Killing thugs in the Edgeville Wilderness dungeon with a ring of wealth (i) is also popular (Wilderness)."));

		WAYS.put(ClueTier.MEDIUM, List.of(
			jars(ClueTier.MEDIUM, "eclectic", "Loot eclectic impling jars (1/25)", ECLECTIC_JAR, "Eclectic impling jar", 25),
			jars(ClueTier.MEDIUM, "essence", "Loot essence impling jars (1/50)", ESSENCE_JAR, "Essence impling jar", 50),
			kill(ClueTier.MEDIUM, "faladorguards", "Kill guards in Falador (1/128)", 40, FALADOR_GUARDS, "Guard",
				"They hit back a little: bring food and eat when you drop below half. A dwarf multicannon speeds it up a lot.")));
		TIPS.put(ClueTier.MEDIUM, List.of(
			"With 75 Thieving, pickpocketing gnomes in the Tree Gnome Stronghold gives one in about 150.",
			"Catching eclectic implings yourself in Puro-Puro (Hunter 50) is cheaper than buying jars."));

		WAYS.put(ClueTier.HARD, List.of(
			jars(ClueTier.HARD, "ninja", "Loot ninja impling jars (1/25)", NINJA_JAR, "Ninja impling jar", 25),
			jars(ClueTier.HARD, "magpie", "Loot magpie impling jars (1/50)", MAGPIE_JAR, "Magpie impling jar", 50)));
		TIPS.put(ClueTier.HARD, List.of(
			"Many Slayer monsters drop them: hellhounds 1/64, and superior Slayer monsters about 1 in 13.",
			"Sarachnis (1/40) and the Dagannoth Kings (1/42) are good mid-level bosses for hard clues.",
			"Finishing the Hard Combat Achievements gives 5% more hard clues (not from implings)."));

		WAYS.put(ClueTier.ELITE, List.of(
			jars(ClueTier.ELITE, "dragon", "Loot dragon impling jars (1/50)", DRAGON_JAR, "Dragon impling jar", 50)));
		TIPS.put(ClueTier.ELITE, List.of(
			"Barbarian Assault: a high-level gamble (needs a Queen kill) gives one 1 in 15.",
			"Skotizo gives one 1 in 5: each kill needs a dark totem from the Catacombs of Kourend.",
			"Hallowed Sepulchre: the Grand Hallowed Coffin at the end of floor 5 gives one 1 in 30 (Agility 92).",
			"Bosses: The Gauntlet 1/25, Vorkath 1/65, Zulrah 1/75. The full list is below."));

		WAYS.put(ClueTier.MASTER, Collections.emptyList());
		TIPS.put(ClueTier.MASTER, List.of(
			"Hand Watson (Hosidius) one easy, medium, hard and elite clue scroll and he gives you a master clue.",
			"Or open caskets: easy 1/50, medium 1/30, hard 1/15 and elite 1/5 contain a master clue."));
	}

	private ClueWays()
	{
	}

	/**
	 * The ways the plugin guides, best first.
	 */
	public static List<DiaryTask> ways(ClueTier tier)
	{
		return WAYS.getOrDefault(tier, Collections.emptyList());
	}

	public static List<String> tips(ClueTier tier)
	{
		return TIPS.getOrDefault(tier, Collections.emptyList());
	}

	public static DiaryTask way(String key)
	{
		for (List<DiaryTask> list : WAYS.values())
		{
			for (DiaryTask t : list)
			{
				if (t.key().equals(key))
				{
					return t;
				}
			}
		}
		return null;
	}

	private static DiaryTask.DiaryTaskBuilder base(ClueTier tier, String slug, String name)
	{
		return DiaryTask.builder()
			.id("clue." + tier.name() + "." + slug)
			.name(name)
			.text(name)
			.freeToPlay(!tier.isMembers());
	}

	private static DiaryTask kill(ClueTier tier, String slug, String name, int combat, Location at, String npc,
		String note)
	{
		return base(tier, slug, name)
			.combat(combat)
			.stage(DiaryStage.builder()
				.title("Kill " + npc.toLowerCase() + "s until a clue scroll drops")
				.at(at)
				.npc(npc)
				.line("1. Wear your best weapon and armour (click them in your inventory and choose Wield or Wear).")
				.line("2. Click a " + npc + " (outlined) and choose Attack.")
				.line("3. When it dies, look at the loot on the ground. If a clue scroll is there, click it and choose Take.")
				.line("4. Keep going until you have one. " + note)
				.build())
			.build();
	}

	private static DiaryTask jars(ClueTier tier, String slug, String name, int jarId, String jarName, int amount)
	{
		return base(tier, slug, name)
			.item(DiaryItem.of(jarName, amount, jarId))
			.stage(DiaryStage.builder()
				.title("Get " + amount + " " + jarName.toLowerCase() + "s")
				.line("Buy them on the Grand Exchange. " + amount + " is about how many it takes, on average, for one clue.")
				.build())
			.stage(DiaryStage.builder()
				.title("Loot the " + jarName.toLowerCase() + "s")
				.carrying(jarId)
				.started(true)
				.use(jarId)
				.line("1. Click a " + jarName + " in your inventory (outlined) and choose Loot.")
				.line("2. Keep looting jars until a clue scroll shows up in your inventory.")
				.line("3. Each jar gives other loot too. When your inventory is full, bank it or drop what you don't want.")
				.line("You can do this anywhere, even standing in the bank.")
				.build())
			.build();
	}

	private static DiaryTask ham()
	{
		return base(ClueTier.EASY, "ham", "Pickpocket H.A.M. Members (1/50)")
			.skill(Skill.THIEVING, 15)
			.stage(DiaryStage.builder()
				.title("Climb down into the H.A.M. Hideout")
				.at(HAM_TRAPDOOR)
				.object("Trapdoor")
				.line("The hideout is under a trapdoor in the field west of Lumbridge, near the general store.")
				.line("1. Click the trapdoor (outlined) and choose Pick-Lock. It can take a few tries.")
				.line("2. Once it's open, click it and choose Climb-down.")
				.build())
			.stage(DiaryStage.builder()
				.title("Pickpocket H.A.M. Members")
				.inside(HAM_HIDEOUT)
				.npc("H.A.M. Member")
				.line("1. Click a H.A.M. Member (outlined) and choose Pickpocket.")
				.line("2. Keep pickpocketing. About 1 in 50 gives an easy clue scroll.")
				.line("3. If you get caught you're stunned for a moment and may be thrown out: just climb back down.")
				.line("4. Put on any H.A.M. robes you get: they make being thrown out less likely.")
				.line("5. Drop junk loot (buttons, thread) when your inventory fills up.")
				.build())
			.build();
	}

	private static DiaryTask citizens()
	{
		return base(ClueTier.EASY, "citizens", "Pickpocket wealthy citizens in Civitas illa Fortis (1/85)")
			.skill(Skill.THIEVING, 50)
			.quest(Quest.CHILDREN_OF_THE_SUN)
			.stage(DiaryStage.builder()
				.title("Pickpocket wealthy citizens")
				.at(CIVITAS)
				.npc("Wealthy citizen")
				.line("1. Find a Wealthy citizen (outlined) in the streets of Civitas illa Fortis.")
				.line("2. Wait until a street urchin distracts them (they say so above their head).")
				.line("3. Click the citizen and choose Pickpocket: while they're distracted it never fails and keeps going by itself.")
				.line("4. About 1 in 85 gives an easy clue scroll. Very few clicks, so it's relaxed.")
				.build())
			.build();
	}
}
