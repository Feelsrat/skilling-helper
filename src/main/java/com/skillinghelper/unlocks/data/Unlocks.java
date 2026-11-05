package com.skillinghelper.unlocks.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryStage;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.diary.data.DiaryTier;
import com.skillinghelper.diary.data.Zone;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Every account unlock the plugin knows, in the order it suggests doing them: cheap, low-level ones that save the
 * most time first (run energy, travel), then the ones that need more levels and quests. Facts from the OSRS Wiki
 * (2026-11); spots for the guided parts from the wiki's NPC maps.
 */
public final class Unlocks
{
	private static final int[] GRACEFUL_HOOD = {ItemID.GRACEFUL_HOOD, ItemID.GRACEFUL_HOOD_WORN,
		ItemID.ZEAH_GRACEFUL_HOOD_ARCEUUS, ItemID.ZEAH_GRACEFUL_HOOD_ARCEUUS_WORN, ItemID.ZEAH_GRACEFUL_HOOD_PISCARILIUS,
		ItemID.ZEAH_GRACEFUL_HOOD_PISCARILIUS_WORN, ItemID.ZEAH_GRACEFUL_HOOD_LOVAKENGJ,
		ItemID.ZEAH_GRACEFUL_HOOD_LOVAKENGJ_WORN, ItemID.ZEAH_GRACEFUL_HOOD_SHAYZIEN,
		ItemID.ZEAH_GRACEFUL_HOOD_SHAYZIEN_WORN, ItemID.ZEAH_GRACEFUL_HOOD_HOSIDIUS,
		ItemID.ZEAH_GRACEFUL_HOOD_HOSIDIUS_WORN, ItemID.ZEAH_GRACEFUL_HOOD_KOUREND,
		ItemID.ZEAH_GRACEFUL_HOOD_KOUREND_WORN, ItemID.GRACEFUL_HOOD_SKILLCAPECOLOUR,
		ItemID.GRACEFUL_HOOD_SKILLCAPECOLOUR_WORN, ItemID.GRACEFUL_HOOD_HALLOWED, ItemID.GRACEFUL_HOOD_HALLOWED_WORN,
		ItemID.GRACEFUL_HOOD_TRAILBLAZER, ItemID.GRACEFUL_HOOD_TRAILBLAZER_WORN, ItemID.GRACEFUL_HOOD_ADVENTURER,
		ItemID.GRACEFUL_HOOD_ADVENTURER_WORN, ItemID.GRACEFUL_HOOD_WYRM, ItemID.GRACEFUL_HOOD_WYRM_WORN};
	private static final int[] GRACEFUL_CAPE = {ItemID.GRACEFUL_CAPE, ItemID.GRACEFUL_CAPE_WORN,
		ItemID.ZEAH_GRACEFUL_CAPE_ARCEUUS, ItemID.ZEAH_GRACEFUL_CAPE_ARCEUUS_WORN, ItemID.ZEAH_GRACEFUL_CAPE_PISCARILIUS,
		ItemID.ZEAH_GRACEFUL_CAPE_PISCARILIUS_WORN, ItemID.ZEAH_GRACEFUL_CAPE_LOVAKENGJ,
		ItemID.ZEAH_GRACEFUL_CAPE_LOVAKENGJ_WORN, ItemID.ZEAH_GRACEFUL_CAPE_SHAYZIEN,
		ItemID.ZEAH_GRACEFUL_CAPE_SHAYZIEN_WORN, ItemID.ZEAH_GRACEFUL_CAPE_HOSIDIUS,
		ItemID.ZEAH_GRACEFUL_CAPE_HOSIDIUS_WORN, ItemID.ZEAH_GRACEFUL_CAPE_KOUREND,
		ItemID.ZEAH_GRACEFUL_CAPE_KOUREND_WORN, ItemID.GRACEFUL_CAPE_SKILLCAPECOLOUR,
		ItemID.GRACEFUL_CAPE_SKILLCAPECOLOUR_WORN, ItemID.GRACEFUL_CAPE_HALLOWED, ItemID.GRACEFUL_CAPE_HALLOWED_WORN,
		ItemID.GRACEFUL_CAPE_TRAILBLAZER, ItemID.GRACEFUL_CAPE_TRAILBLAZER_WORN, ItemID.GRACEFUL_CAPE_ADVENTURER,
		ItemID.GRACEFUL_CAPE_ADVENTURER_WORN, ItemID.GRACEFUL_CAPE_WYRM, ItemID.GRACEFUL_CAPE_WYRM_WORN};
	private static final int[] GRACEFUL_TOP = {ItemID.GRACEFUL_TOP, ItemID.GRACEFUL_TOP_WORN,
		ItemID.ZEAH_GRACEFUL_TOP_ARCEUUS, ItemID.ZEAH_GRACEFUL_TOP_ARCEUUS_WORN, ItemID.ZEAH_GRACEFUL_TOP_PISCARILIUS,
		ItemID.ZEAH_GRACEFUL_TOP_PISCARILIUS_WORN, ItemID.ZEAH_GRACEFUL_TOP_LOVAKENGJ,
		ItemID.ZEAH_GRACEFUL_TOP_LOVAKENGJ_WORN, ItemID.ZEAH_GRACEFUL_TOP_SHAYZIEN,
		ItemID.ZEAH_GRACEFUL_TOP_SHAYZIEN_WORN, ItemID.ZEAH_GRACEFUL_TOP_HOSIDIUS,
		ItemID.ZEAH_GRACEFUL_TOP_HOSIDIUS_WORN, ItemID.ZEAH_GRACEFUL_TOP_KOUREND, ItemID.ZEAH_GRACEFUL_TOP_KOUREND_WORN,
		ItemID.GRACEFUL_TOP_SKILLCAPECOLOUR, ItemID.GRACEFUL_TOP_SKILLCAPECOLOUR_WORN, ItemID.GRACEFUL_TOP_HALLOWED,
		ItemID.GRACEFUL_TOP_HALLOWED_WORN, ItemID.GRACEFUL_TOP_TRAILBLAZER, ItemID.GRACEFUL_TOP_TRAILBLAZER_WORN,
		ItemID.GRACEFUL_TOP_ADVENTURER, ItemID.GRACEFUL_TOP_ADVENTURER_WORN, ItemID.GRACEFUL_TOP_WYRM,
		ItemID.GRACEFUL_TOP_WYRM_WORN};
	private static final int[] GRACEFUL_LEGS = {ItemID.GRACEFUL_LEGS, ItemID.GRACEFUL_LEGS_WORN,
		ItemID.ZEAH_GRACEFUL_LEGS_ARCEUUS, ItemID.ZEAH_GRACEFUL_LEGS_ARCEUUS_WORN, ItemID.ZEAH_GRACEFUL_LEGS_PISCARILIUS,
		ItemID.ZEAH_GRACEFUL_LEGS_PISCARILIUS_WORN, ItemID.ZEAH_GRACEFUL_LEGS_LOVAKENGJ,
		ItemID.ZEAH_GRACEFUL_LEGS_LOVAKENGJ_WORN, ItemID.ZEAH_GRACEFUL_LEGS_SHAYZIEN,
		ItemID.ZEAH_GRACEFUL_LEGS_SHAYZIEN_WORN, ItemID.ZEAH_GRACEFUL_LEGS_HOSIDIUS,
		ItemID.ZEAH_GRACEFUL_LEGS_HOSIDIUS_WORN, ItemID.ZEAH_GRACEFUL_LEGS_KOUREND,
		ItemID.ZEAH_GRACEFUL_LEGS_KOUREND_WORN, ItemID.GRACEFUL_LEGS_SKILLCAPECOLOUR,
		ItemID.GRACEFUL_LEGS_SKILLCAPECOLOUR_WORN, ItemID.GRACEFUL_LEGS_HALLOWED, ItemID.GRACEFUL_LEGS_HALLOWED_WORN,
		ItemID.GRACEFUL_LEGS_TRAILBLAZER, ItemID.GRACEFUL_LEGS_TRAILBLAZER_WORN, ItemID.GRACEFUL_LEGS_ADVENTURER,
		ItemID.GRACEFUL_LEGS_ADVENTURER_WORN, ItemID.GRACEFUL_LEGS_WYRM, ItemID.GRACEFUL_LEGS_WYRM_WORN};
	private static final int[] GRACEFUL_GLOVES = {ItemID.GRACEFUL_GLOVES, ItemID.GRACEFUL_GLOVES_WORN,
		ItemID.ZEAH_GRACEFUL_GLOVES_ARCEUUS, ItemID.ZEAH_GRACEFUL_GLOVES_ARCEUUS_WORN,
		ItemID.ZEAH_GRACEFUL_GLOVES_PISCARILIUS, ItemID.ZEAH_GRACEFUL_GLOVES_PISCARILIUS_WORN,
		ItemID.ZEAH_GRACEFUL_GLOVES_LOVAKENGJ, ItemID.ZEAH_GRACEFUL_GLOVES_LOVAKENGJ_WORN,
		ItemID.ZEAH_GRACEFUL_GLOVES_SHAYZIEN, ItemID.ZEAH_GRACEFUL_GLOVES_SHAYZIEN_WORN,
		ItemID.ZEAH_GRACEFUL_GLOVES_HOSIDIUS, ItemID.ZEAH_GRACEFUL_GLOVES_HOSIDIUS_WORN,
		ItemID.ZEAH_GRACEFUL_GLOVES_KOUREND, ItemID.ZEAH_GRACEFUL_GLOVES_KOUREND_WORN,
		ItemID.GRACEFUL_GLOVES_SKILLCAPECOLOUR, ItemID.GRACEFUL_GLOVES_SKILLCAPECOLOUR_WORN,
		ItemID.GRACEFUL_GLOVES_HALLOWED, ItemID.GRACEFUL_GLOVES_HALLOWED_WORN, ItemID.GRACEFUL_GLOVES_TRAILBLAZER,
		ItemID.GRACEFUL_GLOVES_TRAILBLAZER_WORN, ItemID.GRACEFUL_GLOVES_ADVENTURER,
		ItemID.GRACEFUL_GLOVES_ADVENTURER_WORN, ItemID.GRACEFUL_GLOVES_WYRM, ItemID.GRACEFUL_GLOVES_WYRM_WORN};
	private static final int[] GRACEFUL_BOOTS = {ItemID.GRACEFUL_BOOTS, ItemID.GRACEFUL_BOOTS_WORN,
		ItemID.ZEAH_GRACEFUL_BOOTS_ARCEUUS, ItemID.ZEAH_GRACEFUL_BOOTS_ARCEUUS_WORN,
		ItemID.ZEAH_GRACEFUL_BOOTS_PISCARILIUS, ItemID.ZEAH_GRACEFUL_BOOTS_PISCARILIUS_WORN,
		ItemID.ZEAH_GRACEFUL_BOOTS_LOVAKENGJ, ItemID.ZEAH_GRACEFUL_BOOTS_LOVAKENGJ_WORN,
		ItemID.ZEAH_GRACEFUL_BOOTS_SHAYZIEN, ItemID.ZEAH_GRACEFUL_BOOTS_SHAYZIEN_WORN,
		ItemID.ZEAH_GRACEFUL_BOOTS_HOSIDIUS, ItemID.ZEAH_GRACEFUL_BOOTS_HOSIDIUS_WORN,
		ItemID.ZEAH_GRACEFUL_BOOTS_KOUREND, ItemID.ZEAH_GRACEFUL_BOOTS_KOUREND_WORN,
		ItemID.GRACEFUL_BOOTS_SKILLCAPECOLOUR, ItemID.GRACEFUL_BOOTS_SKILLCAPECOLOUR_WORN,
		ItemID.GRACEFUL_BOOTS_HALLOWED, ItemID.GRACEFUL_BOOTS_HALLOWED_WORN, ItemID.GRACEFUL_BOOTS_TRAILBLAZER,
		ItemID.GRACEFUL_BOOTS_TRAILBLAZER_WORN, ItemID.GRACEFUL_BOOTS_ADVENTURER, ItemID.GRACEFUL_BOOTS_ADVENTURER_WORN,
		ItemID.GRACEFUL_BOOTS_WYRM, ItemID.GRACEFUL_BOOTS_WYRM_WORN};
	/**
	 * Hood, cape, top, legs, gloves, boots, in marks of grace (wiki Graceful outfit page).
	 */
	private static final int[] GRACEFUL_COSTS = {35, 40, 55, 60, 30, 40};

	private static final Location ROGUES_DEN = Location.of("Rogues' Den trapdoor, Burthorpe", 2906, 3537, 0, 3);
	private static final Zone ROGUES_DEN_INSIDE = Zone.anyFloor(3030, 4940, 3070, 4995);
	private static final Location MOTHERLODE = Location.of("Motherlode Mine", 3757, 5666, 0, 12);
	private static final Zone MOTHERLODE_INSIDE = Zone.of(3712, 5632, 3775, 5695, 0);
	private static final Location TURAEL = Location.of("Turael, Burthorpe", 2931, 3536, 0, 4);

	private static final String SLAYER_POINTS_HOW = "Turael (Burthorpe) gives no points. Get your tasks from Mazchna "
		+ "in Canifis (combat level 20) or Vannaka in Edgeville Dungeon (combat level 40) instead.";
	private static final String SLAYER_POINTS_HOW2 = "Your first 5 tasks give no points; after that every task does, "
		+ "and every 10th task gives a lot more.";
	private static final String SLAYER_POINTS_HOW3 = "Skilling Helper's Slayer guide takes you to each task's "
		+ "monsters (click the button below).";

	private static final List<Unlock> ALL = List.of(
		Unlock.builder()
			.id("graceful")
			.name("Graceful outfit")
			.category(UnlockCategory.GEAR)
			.gives("Your run energy comes back 30% faster with the full set")
			.gives("Weighs almost nothing (-25 kg), so you run longer")
			.gives("The best everyday outfit for walking, skilling and clue scrolls")
			.wikiPage("Graceful_outfit")
			.own(GRACEFUL_HOOD).own(GRACEFUL_CAPE).own(GRACEFUL_TOP).own(GRACEFUL_LEGS).own(GRACEFUL_GLOVES)
			.own(GRACEFUL_BOOTS)
			.pieceCosts(GRACEFUL_COSTS)
			.grind(Grind.builder()
				.label("marks of grace")
				.itemId(ItemID.GRACE)
				.amount(260)
				.skill(Skill.AGILITY)
				.minLevel(10)
				.line("Marks of grace appear on the rooftops while you run rooftop Agility courses. Click one "
					+ "(Take) when you see it.")
				.line("Run the course that fits your level: Draynor from 10 Agility, Al Kharid 20, Varrock 30, "
					+ "Canifis 40 (the best for marks), Falador 50, Seers' 60.")
				.line("Skilling Helper's Agility guide picks the course and shows every obstacle (click the button "
					+ "below).")
				.build())
			.guide(DiaryTask.builder()
				.id("unlock.graceful")
				.text("Buy the graceful outfit from Grace in the Rogues' Den")
				.name("Graceful outfit")
				.stage(DiaryStage.builder()
					.title("Climb down into the Rogues' Den")
					.at(ROGUES_DEN)
					.line("1. Go into The Toad and Chicken pub in Burthorpe.")
					.line("2. Click the trapdoor (outlined) and choose Climb-down.")
					.object("Trapdoor")
					.build())
				.stage(DiaryStage.builder()
					.title("Buy the outfit from Grace")
					.inside(ROGUES_DEN_INSIDE)
					.line("1. Click Grace (outlined) and choose Trade.")
					.line("2. Buy the pieces you don't have yet: hood 35, cape 40, top 55, legs 60, gloves 30 and "
						+ "boots 40 marks of grace.")
					.line("3. Wear every piece: the full set gives the extra run energy bonus.")
					.npc("Grace")
					.build())
				.build())
			.build(),
		Unlock.builder()
			.id("house")
			.name("Player-owned house")
			.category(UnlockCategory.SKILLING)
			.gives("Lets you train Construction")
			.gives("Later: a portal room, jewellery box and pool that teleport and restore you in one place")
			.gives("Needed for Construction xp from lamps and minigames (Wintertodt, Tempoross)")
			.wikiPage("Player-owned_house")
			.doneVarbit(VarbitID.POH_HOUSE_LOCATION)
			.guide(DiaryTask.builder()
				.id("unlock.house")
				.text("Buy a house from an estate agent")
				.name("Player-owned house")
				.item(DiaryItem.of("Coins", 1000, ItemID.COINS))
				.stage(DiaryStage.builder()
					.title("Buy a house from the estate agent")
					.at(Location.of("Varrock estate agent", 3240, 3476, 0, 4))
					.line("1. Click the Estate agent (outlined) and choose Talk-to.")
					.line("2. Ask how to get a house and say yes: it costs 1,000 coins.")
					.line("3. Your house is in Rimmington. Go in through the house portal there (choose Build mode "
						+ "to build rooms).")
					.line("The estate agent can move it closer to a bank later, once your Construction level is "
						+ "high enough.")
					.npc("Estate agent")
					.build())
				.build())
			.build(),
		Unlock.builder()
			.id("dramen_staff")
			.name("Dramen staff and Zanaris")
			.category(UnlockCategory.TRAVEL)
			.gives("The Dramen staff: you need it wielded to use fairy rings")
			.gives("Entry to Zanaris, the fairy city, through the shed in Lumbridge Swamp")
			.gives("Zanaris sells dragon longswords and dragon daggers")
			.wikiPage("Lost_City")
			.doneQuest(Quest.LOST_CITY)
			.quest(QuestPart.builder()
				.quest(Quest.LOST_CITY)
				.skill(Skill.CRAFTING, 31)
				.skill(Skill.WOODCUTTING, 36)
				.note("You fight a tree spirit near the end: bring food. Quest Helper lists what to bring.")
				.build())
			.build(),
		Unlock.builder()
			.id("fairy_rings")
			.name("Fairy rings")
			.category(UnlockCategory.TRAVEL)
			.gives("Teleport between fairy rings all over the world with a three-letter code")
			.gives("Fast trips to farming patches, slayer spots, Zanaris and many more")
			.gives("Skilling Helper and the other guides route you through them once they're open")
			.wikiPage("Fairy_ring")
			.doneVarbit(VarbitID.FAIRY2_QUEENCURE_QUEST)
			.doneVarbitMin(40)
			.quest(QuestPart.builder()
				.quest(Quest.LOST_CITY)
				.skill(Skill.CRAFTING, 31)
				.skill(Skill.WOODCUTTING, 36)
				.build())
			.quest(QuestPart.builder()
				.quest(Quest.FAIRYTALE_I__GROWING_PAINS)
				.note("It needs Nature Spirit done first; Quest Helper shows the quests it needs.")
				.build())
			.quest(QuestPart.builder()
				.quest(Quest.FAIRYTALE_II__CURE_A_QUEEN)
				.varbit(VarbitID.FAIRY2_QUEENCURE_QUEST)
				.varbitMin(40)
				.note("You only need to start it: the fairy rings open after the first few steps, and the guide "
					+ "moves on by itself when they do. Finishing the quest can wait.")
				.build())
			.build(),
		Unlock.builder()
			.id("spirit_trees")
			.name("Spirit trees")
			.category(UnlockCategory.TRAVEL)
			.gives("Teleport between spirit trees, one of them right next to the Grand Exchange")
			.gives("Quick trips to the Tree Gnome Stronghold and Tree Gnome Village")
			.gives("Later you can plant your own spirit trees (83 Farming)")
			.wikiPage("Spirit_tree")
			.doneQuest(Quest.TREE_GNOME_VILLAGE)
			.quest(QuestPart.builder()
				.quest(Quest.TREE_GNOME_VILLAGE)
				.note("You fight a level 112 warlord at the end: bring food. Ranged or magic over the fence works.")
				.build())
			.build(),
		Unlock.builder()
			.id("gnome_gliders")
			.name("Gnome gliders")
			.category(UnlockCategory.TRAVEL)
			.gives("Fly between glider spots: Gnome Stronghold, White Wolf Mountain, Digsite, Al Kharid, Karamja and more")
			.gives("A fast way to the Kharidian Desert and Karamja")
			.wikiPage("Gnome_glider")
			.doneQuest(Quest.THE_GRAND_TREE)
			.quest(QuestPart.builder()
				.quest(Quest.THE_GRAND_TREE)
				.skill(Skill.AGILITY, 25)
				.note("You fight a level 172 demon at the end; safe-spotting it with ranged or magic is easiest.")
				.build())
			.build(),
		diaryReward("ardougne_cloak", "Ardougne cloak 1", UnlockCategory.TRAVEL, DiaryRegion.ARDOUGNE),
		diaryReward("explorers_ring", "Explorer's ring 1", UnlockCategory.MAGIC, DiaryRegion.LUMBRIDGE),
		diaryReward("karamja_gloves", "Karamja gloves 1", UnlockCategory.SKILLING, DiaryRegion.KARAMJA),
		diaryReward("varrock_armour", "Varrock armour 1", UnlockCategory.SKILLING, DiaryRegion.VARROCK),
		nuggetUnlock("coal_bag", "Coal bag", "Holds 27 coal, so you carry twice the coal for smithing and mining",
			"Coal_bag", ItemID.COAL_BAG, ItemID.COAL_BAG_OPEN),
		nuggetUnlock("gem_bag", "Gem bag", "Holds 60 of each uncut gem, so gems from mining don't fill your inventory",
			"Gem_bag", ItemID.GEM_BAG, ItemID.GEM_BAG_OPEN),
		Unlock.builder()
			.id("ectophial")
			.name("Ectophial")
			.category(UnlockCategory.TRAVEL)
			.gives("Unlimited teleports to the Ectofuntus near Port Phasmatys, Morytania")
			.gives("Refills itself after every teleport")
			.gives("The quickest way to Morytania (Canifis, Barrows and slayer spots)")
			.wikiPage("Ectophial")
			.doneQuest(Quest.GHOSTS_AHOY)
			.own(new int[]{ItemID.ECTOPHIAL, ItemID.ECTOPHIAL_EMPTY})
			.quest(QuestPart.builder()
				.quest(Quest.GHOSTS_AHOY)
				.skill(Skill.AGILITY, 25)
				.skill(Skill.COOKING, 20)
				.note("It needs Priest in Peril and The Restless Ghost done first; Quest Helper shows them.")
				.build())
			.build(),
		Unlock.builder()
			.id("avas")
			.name("Ava's accumulator")
			.category(UnlockCategory.GEAR)
			.gives("Picks up most of your arrows and bolts after you fire them")
			.gives("The best Ranged cape until much later")
			.wikiPage("Ava%27s_accumulator")
			.own(new int[]{ItemID.ANMA_50_REWARD, ItemID.AVAS_ASSEMBLER, ItemID.AVAS_ASSEMBLER_TROUVER,
				ItemID.AVAS_ASSEMBLER_MASORI, ItemID.AVAS_ASSEMBLER_MASORI_TROUVER, ItemID.AVAS_ASSEMBLER_BROKEN,
				ItemID.AVAS_ASSEMBLER_MASORI_BROKEN})
			.quest(QuestPart.builder()
				.quest(Quest.ANIMAL_MAGNETISM)
				.skill(Skill.SLAYER, 18)
				.skill(Skill.CRAFTING, 19)
				.skill(Skill.RANGED, 30)
				.skill(Skill.WOODCUTTING, 35)
				.note("Finish it with 50 Ranged and Ava hands you the accumulator straight away (under 50 you get "
					+ "the weaker attractor).")
				.build())
			.guide(DiaryTask.builder()
				.id("unlock.avas")
				.text("Get Ava's accumulator from Ava in Draynor Manor")
				.name("Ava's accumulator")
				.skill(Skill.RANGED, 50)
				.item(DiaryItem.of("Steel arrows", 75, ItemID.STEEL_ARROW))
				.item(DiaryItem.of("Coins", 999, ItemID.COINS))
				.stage(DiaryStage.builder()
					.title("Ask Ava for an accumulator")
					.at(Location.of("Ava, Draynor Manor", 3094, 3357, 0, 4))
					.line("1. Go into Draynor Manor, north of Draynor Village.")
					.line("2. Click Ava (outlined) and choose Talk-to.")
					.line("3. Ask for a new device and pick the accumulator: she takes the 75 steel arrows and 999 "
						+ "coins.")
					.line("4. Wear it in your cape slot. Don't wear a metal body with it: it stops working.")
					.npc("Ava")
					.build())
				.build())
			.build(),
		Unlock.builder()
			.id("slayer_helm")
			.name("Slayer helmet (unlock)")
			.category(UnlockCategory.GEAR)
			.gives("Lets you make a Slayer helmet (55 Crafting)")
			.gives("+16.67% accuracy and damage against your Slayer task, like a black mask")
			.gives("Also protects you like a nose peg, earmuffs, facemask and spiny helmet in one")
			.wikiPage("Slayer_helmet")
			.doneVarbit(VarbitID.SLAYER_HELM_UNLOCKED)
			.grind(Grind.builder()
				.label("Slayer points")
				.varbit(VarbitID.SLAYER_POINTS)
				.amount(400)
				.skill(Skill.SLAYER)
				.line(SLAYER_POINTS_HOW)
				.line(SLAYER_POINTS_HOW2)
				.line(SLAYER_POINTS_HOW3)
				.build())
			.guide(DiaryTask.builder()
				.id("unlock.slayer_helm")
				.text("Unlock Malevolent masquerade from a Slayer master")
				.name("Slayer helmet (unlock)")
				.stage(DiaryStage.builder()
					.title("Buy the Slayer helmet unlock")
					.at(TURAEL)
					.line("1. Click Turael (outlined) and choose Rewards. Any Slayer master works.")
					.line("2. Open the Unlock tab and buy \"Malevolent masquerade\" for 400 points.")
					.line("3. With 55 Crafting, put the parts together (the wiki page lists them).")
					.npc("Turael")
					.build())
				.build())
			.build(),
		Unlock.builder()
			.id("rune_pouch")
			.name("Rune pouch")
			.category(UnlockCategory.MAGIC)
			.gives("Holds 16,000 each of three kinds of rune in one inventory slot")
			.gives("Cast spells straight from it: frees space for alching, teleports and combat spells")
			.wikiPage("Rune_pouch")
			.own(new int[]{ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER, ItemID.DIVINE_RUNE_POUCH,
				ItemID.DIVINE_RUNE_POUCH_TROUVER})
			.grind(Grind.builder()
				.label("Slayer points")
				.varbit(VarbitID.SLAYER_POINTS)
				.amount(750)
				.skill(Skill.SLAYER)
				.line(SLAYER_POINTS_HOW)
				.line(SLAYER_POINTS_HOW2)
				.line(SLAYER_POINTS_HOW3)
				.line("Or buy it at the Mage Training Arena with pizazz points instead.")
				.build())
			.guide(DiaryTask.builder()
				.id("unlock.rune_pouch")
				.text("Buy a rune pouch from a Slayer master")
				.name("Rune pouch")
				.stage(DiaryStage.builder()
					.title("Buy the rune pouch")
					.at(TURAEL)
					.line("1. Click Turael (outlined) and choose Rewards. Any Slayer master works.")
					.line("2. Open the Buy tab and buy the rune pouch for 750 points.")
					.line("3. Use runes on it to put them in (three kinds).")
					.npc("Turael")
					.build())
				.build())
			.build(),
		Unlock.builder()
			.id("barrows_gloves")
			.name("Barrows gloves")
			.category(UnlockCategory.GEAR)
			.gives("The best all-round gloves for melee, ranged and magic until late game")
			.gives("The Culinaromancer's chest also sells food and the other Recipe for Disaster gloves")
			.wikiPage("Barrows_gloves")
			.own(new int[]{ItemID.HUNDRED_GAUNTLETS_LEVEL_10})
			.quest(QuestPart.builder()
				.quest(Quest.RECIPE_FOR_DISASTER)
				.note("A series of quests (each part unlocks better gloves). The last part needs 175 quest points "
					+ "and a lot of quests; Quest Helper guides every part.")
				.build())
			.guide(DiaryTask.builder()
				.id("unlock.barrows_gloves")
				.text("Buy Barrows gloves from the Culinaromancer's chest")
				.name("Barrows gloves")
				.item(DiaryItem.of("Coins", 130_000, ItemID.COINS))
				.stage(DiaryStage.builder()
					.title("Climb down to Lumbridge Castle cellar")
					.at(Location.of("Lumbridge Castle kitchen", 3209, 3216, 0, 4))
					.line("1. Go into the kitchen on the ground floor of Lumbridge Castle.")
					.line("2. Click the trapdoor (outlined) and choose Climb-down.")
					.object("Trapdoor")
					.build())
				.stage(DiaryStage.builder()
					.title("Buy the gloves from the Culinaromancer's chest")
					.inside(Zone.of(3200, 9605, 3230, 9640, 0))
					.line("1. Click the chest (outlined) and choose Buy-items.")
					.line("2. Buy the Barrows gloves (130,000 coins; 104,000 after the Lumbridge elite diary).")
					.object("Chest")
					.build())
				.build())
			.build(),
		Unlock.builder()
			.id("ancients")
			.name("Ancient Magicks")
			.category(UnlockCategory.MAGIC)
			.gives("The Ancient spellbook: Ice Barrage and the other multi-target combat spells")
			.gives("Teleports into the Wilderness and Kharidian Desert")
			.gives("Switch spellbooks at the altar in the Jaldraocht Pyramid")
			.wikiPage("Ancient_Magicks")
			.doneQuest(Quest.DESERT_TREASURE_I)
			.quest(QuestPart.builder()
				.quest(Quest.DESERT_TREASURE_I)
				.skill(Skill.MAGIC, 50)
				.skill(Skill.FIREMAKING, 50)
				.skill(Skill.THIEVING, 53)
				.skill(Skill.SLAYER, 10)
				.note("It needs several quests first and has four boss fights; Quest Helper shows them all.")
				.build())
			.build(),
		Unlock.builder()
			.id("lunars")
			.name("Lunar spellbook")
			.category(UnlockCategory.MAGIC)
			.gives("Utility spells: Humidify, Plank Make, String Jewellery, Spellbook Swap and many more")
			.gives("Teleports to Moonclan, Waterbirth, Barbarian Outpost, Khazard, Fishing Guild, Catherby and Ice Plateau")
			.gives("NPC Contact and Vengeance")
			.wikiPage("Lunar_spellbook")
			.doneQuest(Quest.LUNAR_DIPLOMACY)
			.quest(QuestPart.builder()
				.quest(Quest.LUNAR_DIPLOMACY)
				.skill(Skill.MAGIC, 65)
				.skill(Skill.CRAFTING, 61)
				.skill(Skill.MINING, 60)
				.skill(Skill.WOODCUTTING, 55)
				.skill(Skill.FIREMAKING, 49)
				.skill(Skill.DEFENCE, 40)
				.skill(Skill.HERBLORE, 5)
				.note("It needs The Fremennik Trials and Lost City first; Quest Helper shows them.")
				.build())
			.build(),
		diaryReward("fairy_rings_no_staff", "Fairy rings without a staff", UnlockCategory.TRAVEL,
			DiaryRegion.LUMBRIDGE, DiaryTier.ELITE)
	);

	private Unlocks()
	{
	}

	public static List<Unlock> all()
	{
		return ALL;
	}

	public static Unlock byId(String id)
	{
		for (Unlock u : ALL)
		{
			if (u.getId().equals(id))
			{
				return u;
			}
		}
		return null;
	}

	private static Unlock diaryReward(String id, String name, UnlockCategory category, DiaryRegion region)
	{
		return diaryReward(id, name, category, region, DiaryTier.EASY);
	}

	/**
	 * A diary tier's reward. What it gives comes from the diary rewards table; the Achievement Diary guide does it.
	 */
	private static Unlock diaryReward(String id, String name, UnlockCategory category, DiaryRegion region,
		DiaryTier tier)
	{
		return Unlock.builder()
			.id(id)
			.name(name)
			.category(category)
			.wikiPage(region.getWikiPage())
			.diaryRegion(region)
			.diaryTier(tier)
			.build();
	}

	/**
	 * Bought from Prospector Percy in the Motherlode Mine for 100 golden nuggets.
	 */
	private static Unlock nuggetUnlock(String id, String name, String gives, String wikiPage, int... ids)
	{
		return Unlock.builder()
			.id(id)
			.name(name)
			.category(UnlockCategory.SKILLING)
			.gives(gives)
			.gives("Can't be lost, and only costs golden nuggets from the Motherlode Mine")
			.wikiPage(wikiPage)
			.own(ids)
			.grind(Grind.builder()
				.label("golden nuggets")
				.itemId(ItemID.MOTHERLODE_NUGGET)
				.amount(100)
				.skill(Skill.MINING)
				.minLevel(30)
				.methodId("mine_motherlode")
				.line("Golden nuggets come out of the sack in the Motherlode Mine, with the ore from your pay-dirt.")
				.line("Skilling Helper's Motherlode Mine guide shows every step (click the button below). Keep the "
					+ "nuggets: they stack in one slot.")
				.build())
			.guide(DiaryTask.builder()
				.id("unlock." + id)
				.text("Buy the " + name.toLowerCase() + " from Prospector Percy")
				.name(name)
				.stage(DiaryStage.builder()
					.title("Buy the " + name.toLowerCase() + " from Prospector Percy")
					.at(MOTHERLODE)
					.inside(MOTHERLODE_INSIDE)
					.line("1. Click Prospector Percy (outlined) in the Motherlode Mine and choose Trade.")
					.line("2. Buy the " + name.toLowerCase() + " for 100 golden nuggets.")
					.line("3. Right-click it and choose Open: what you mine then goes straight into it.")
					.npc("Prospector Percy")
					.build())
				.build())
			.build();
	}
}
