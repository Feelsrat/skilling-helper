package com.skillinghelper.diary.data;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.ToolType;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;

/**
 * Falador diary, every tier. Task wording and requirements from the OSRS Wiki, listed in walking order.
 */
final class FaladorDiary
{
	private static final Location CASTLE_LADDER = Location.of("Ladder in Falador Castle", 2994, 3341, 0, 2);
	private static final Zone CASTLE_1 = Zone.of(2954, 3328, 2997, 3353, 1);
	private static final Zone CASTLE_2 = Zone.of(2954, 3328, 2997, 3353, 2);
	private static final Zone CASTLE_3 = Zone.of(2954, 3328, 2997, 3353, 3);
	private static final Location MINE_STAIRS = Location.of("Stairs to the Dwarven Mine, east Falador", 3059, 3376, 0, 2);
	private static final Zone DWARVEN_MINE = Zone.of(2979, 9698, 3069, 9855, 0);
	private static final Location SARIM_TRAPDOOR = Location.of("Trapdoor south of Port Sarim", 3008, 3150, 0, 2);
	private static final Zone ICE_DUNGEON = Zone.of(2979, 9538, 3029, 9602, 0);
	private static final Location MIND_RUINS = Location.of("Mind altar ruins, north of Falador", 2982, 3514, 0, 3);
	private static final Zone MIND_ALTAR = Zone.of(2760, 4819, 2805, 4855, 0);
	private static final DiaryItem MIND_ACCESS = DiaryItem.anyOf("Mind tiara or talisman", 1, ItemID.MIND_TALISMAN,
		ItemID.TIARA_MIND, ItemID.MIND_TALISMAN);

	private FaladorDiary()
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

	private static DiaryStage castleUp(String why)
	{
		return DiaryStage.builder()
			.title("Climb up in Falador Castle")
			.at(CASTLE_LADDER)
			.object("Ladder")
			.line("1. Walk into the White Knights' Castle in south Falador.")
			.line("2. Climb-up the Ladder (outlined) in the east wing, " + why + ".")
			.build();
	}

	private static DiaryStage reniteeStage(String title, String... lines)
	{
		DiaryStage.DiaryStageBuilder b = DiaryStage.builder()
			.title(title)
			.inside(CASTLE_1)
			.at(Location.of("Sir Renitee, Falador Castle first floor", 2982, 3341, 1, 3))
			.npc("Sir Renitee");
		for (String line : lines)
		{
			b.line(line);
		}
		return b.build();
	}

	private static DiaryStage mineStairs()
	{
		return DiaryStage.builder()
			.title("Go down into the Dwarven Mine")
			.at(MINE_STAIRS)
			.object("Staircase")
			.line("1. The stairs are in a small building in east Falador, just north of the east bank.")
			.line("2. Click Climb-down on the Staircase (outlined).")
			.build();
	}

	private static DiaryStage iceDungeonTrapdoor()
	{
		return DiaryStage.builder()
			.title("Climb into the Asgarnian Ice Dungeon")
			.at(SARIM_TRAPDOOR)
			.object("Trapdoor")
			.line("1. The trapdoor is on the peninsula south of Port Sarim, near Mudskipper Point.")
			.line("2. Click Climb-down on the Trapdoor (outlined).")
			.line("Warning: ice warriors and ice giants down here hit hard at low levels; bring food.")
			.build();
	}

	private static void easy(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.EASY;
		out.add(task(t, 1, "Family crest", "Find out what your family crest is from Sir Renitee.")
			.skill(Skill.CONSTRUCTION, 16)
			.stage(castleUp("then walk west to Sir Renitee"))
			.stage(reniteeStage("Ask Sir Renitee about your family crest",
				"1. Talk to Sir Renitee (outlined) and ask \"What can you tell me about my family crest?\".",
				"2. Keep clicking through until the whole conversation ends, or it won't count."))
			.build());
		out.add(task(t, 4, "Haircut", "Get a Haircut or a Shave from the Falador Hairdresser.")
			.stage(DiaryStage.builder()
				.title("Get a haircut or shave")
				.at(Location.of("Hairdresser, west Falador", 2945, 3380, 0, 3))
				.npc("Hairdresser")
				.line("1. The hairdresser is next to Falador west bank.")
				.line("2. Talk to the Hairdresser (outlined), ask for a haircut or shave and pick one. It's free.")
				.build())
			.build());
		out.add(task(t, 5, "Fill a bucket", "Fill a bucket from the pump north of Falador west bank.")
			.item(DiaryItem.of("Bucket", ItemID.BUCKET_EMPTY))
			.stage(DiaryStage.builder()
				.title("Fill a bucket at the waterpump")
				.at(Location.of("Waterpump north of Falador west bank", 2947, 3382, 0, 2))
				.object("Waterpump")
				.use(ItemID.BUCKET_EMPTY)
				.line("1. Use the Bucket (outlined) on the Waterpump (outlined) just north of the west bank.")
				.build())
			.build());
		out.add(task(t, 2, "Climb the west wall", "Climb over the western Falador wall.")
			.skill(Skill.AGILITY, 5)
			.stage(DiaryStage.builder()
				.title("Climb over the western wall")
				.at(Location.of("Crumbling wall, west Falador", 2935, 3355, 0, 2))
				.object("Crumbling wall")
				.line("1. The crumbling wall is south-west of Falador west bank.")
				.line("2. Click Climb-over on the Crumbling wall (outlined).")
				.build())
			.build());
		out.add(task(t, 6, "Kill a duck", "Kill a duck in Falador Park.")
			.stage(DiaryStage.builder()
				.title("Kill a duck in Falador Park")
				.at(Location.of("Falador Park pond", 2989, 3379, 0, 6))
				.npc("Duck")
				.line("1. Ducks swim on the pond in Falador Park.")
				.line("2. Attack a Duck (outlined). Magic or a ranged weapon is easiest since they stay on the water.")
				.build())
			.build());
		out.add(task(t, 9, "Motherlode strut", "Repair a broken strut in the Motherlode Mine.")
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(mineStairs())
			.stage(DiaryStage.builder()
				.title("Walk into the Motherlode Mine")
				.inside(DWARVEN_MINE)
				.at(Location.of("Motherlode Mine entrance", 3059, 9764, 0, 2))
				.object("Cave")
				.line("1. Walk south-west through the Dwarven Mine to the cave entrance (outlined).")
				.line("2. Click Enter on the Cave.")
				.build())
			.stage(DiaryStage.builder()
				.title("Fix a broken strut")
				.inside(Zone.of(3712, 5636, 3780, 5695, 0))
				.at(Location.of("Water wheels, Motherlode Mine", 3742, 5669, 0, 4))
				.object("Broken strut")
				.line("1. Walk to the water wheels in the middle of the mine.")
				.line("2. Click Hammer on a Broken strut (outlined). If none are broken, mine pay-dirt and wait.")
				.build())
			.build());
		out.add(task(t, 11, "Blurite limbs", "Smith some Blurite Limbs on Doric's anvil.")
			.quest(Quest.THE_KNIGHTS_SWORD).quest(Quest.DORICS_QUEST)
			.skill(Skill.MINING, 10).skill(Skill.SMITHING, 13)
			.item(DiaryItem.of("Hammer", ItemID.HAMMER))
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.stage(iceDungeonTrapdoor())
			.stage(DiaryStage.builder()
				.title("Mine a blurite ore")
				.inside(ICE_DUNGEON)
				.at(Location.of("Blurite rocks, Asgarnian Ice Dungeon", 3049, 9566, 0, 3))
				.object("Blurite rocks")
				.line("1. Walk east to the blue Blurite rocks (outlined) and Mine one ore.")
				.build())
			.stage(DiaryStage.builder()
				.title("Smelt the ore into a blurite bar")
				.carrying(ItemID.BLURITE_ORE)
				.at(Location.of("Furnace, Falador", 2976, 3369, 0, 2))
				.object("Furnace")
				.use(ItemID.BLURITE_ORE)
				.line("1. Go to the furnace in north Falador, by the park.")
				.line("2. Use the Blurite ore on the Furnace (outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Smith blurite limbs on Doric's anvil")
				.carrying(ItemID.BLURITE_BAR)
				.at(Location.of("Doric's anvil, north of Falador", 2950, 3451, 0, 3))
				.object("Anvil")
				.use(ItemID.BLURITE_BAR)
				.line("1. Doric's house is north of Falador, by the road to Taverley.")
				.line("2. Use the Blurite bar on the Anvil (outlined) and choose Blurite limbs.")
				.build())
			.build());
		out.add(task(t, 7, "Mind tiara", "Make a mind tiara.")
			.item(DiaryItem.of("Tiara", ItemID.TIARA))
			.item(DiaryItem.of("Mind talisman", ItemID.MIND_TALISMAN))
			.stages(DiaryStages.runeAltar("north of Falador, west of the Goblin Village road", MIND_RUINS, MIND_ALTAR,
				"Make a mind tiara", ItemID.MIND_TALISMAN, null))
			.stage(DiaryStage.builder()
				.title("Use the tiara on the altar")
				.inside(MIND_ALTAR)
				.object("Altar")
				.use(ItemID.TIARA)
				.line("1. Use the Tiara (outlined) on the Altar (outlined) with the talisman in your inventory.")
				.build())
			.build());
		out.add(task(t, 3, "Sarah's farm shop", "Browse Sarah's farm shop.")
			.stage(DiaryStage.builder()
				.title("Browse Sarah's farming shop")
				.at(Location.of("Sarah's farm shop, south of Falador", 3039, 3292, 0, 3))
				.npc("Sarah")
				.line("1. Sarah's shop is in the farm south of Falador.")
				.line("2. Right-click Sarah (outlined) and choose Trade.")
				.build())
			.build());
		out.add(task(t, 8, "Boat to Entrana", "Take the boat to Entrana.")
			.note("No weapons or armour worn or in your inventory")
			.stage(DiaryStage.builder()
				.title("Sail to Entrana")
				.at(Location.of("Port Sarim docks", 3046, 3235, 0, 4))
				.npc("Monk of Entrana")
				.line("1. Bank every weapon and piece of armour first; the monks search you.")
				.line("2. Talk to a Monk of Entrana (outlined) on the Port Sarim docks and say yes to the trip.")
				.build())
			.build());
		out.add(task(t, 10, "Security book", "Claim a security book from the Security guard at Port Sarim jail.")
			.stage(DiaryStage.builder()
				.title("Climb up in Port Sarim jail")
				.at(Location.of("Port Sarim jail ladder", 3010, 3184, 0, 2))
				.object("Ladder")
				.line("1. The jail is in south Port Sarim. Climb-up the Ladder (outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Ask the Security guard about security")
				.inside(Zone.of(3009, 3178, 3021, 3197, 1))
				.npc("Security Guard").npc("Security guard")
				.line("1. Talk to the Security Guard (outlined), ask about security and finish the conversation.")
				.build())
			.build());
	}

	private static void medium(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.MEDIUM;
		out.add(task(t, 14, "Falador Teleport", "Teleport to Falador.")
			.skill(Skill.MAGIC, 37)
			.item(DiaryItem.of("Law rune", 1, ItemID.LAWRUNE))
			.item(DiaryItem.of("Air rune", 3, ItemID.AIRRUNE))
			.item(DiaryItem.of("Water rune", 1, ItemID.WATERRUNE))
			.stage(DiaryStage.builder()
				.title("Cast Falador Teleport")
				.line("1. Open the spellbook and click Falador Teleport.")
				.build())
			.build());
		out.add(task(t, 8, "Pickpocket a guard", "Pickpocket a Falador guard.")
			.skill(Skill.THIEVING, 40)
			.stage(DiaryStage.builder()
				.title("Pickpocket a Falador guard")
				.at(Location.of("Guards by Falador west bank", 2961, 3381, 0, 6))
				.npc("Guard")
				.line("1. Right-click a Guard (outlined) and choose Pickpocket. One success is enough.")
				.build())
			.build());
		out.add(task(t, 11, "Dwarven Mine crevice", "Squeeze through the crevice in the Dwarven mines.")
			.skill(Skill.AGILITY, 42)
			.stage(mineStairs())
			.stage(DiaryStage.builder()
				.title("Squeeze through the crevice")
				.inside(DWARVEN_MINE)
				.at(Location.of("Crevice, Dwarven Mine", 3034, 9806, 0, 2))
				.object("Crevice")
				.line("1. Walk west to the Crevice (outlined) and click Squeeze-through.")
				.build())
			.build());
		out.add(task(t, 7, "North wall grapple", "Grapple up and then jump off the north Falador wall.")
			.skill(Skill.AGILITY, 11).skill(Skill.STRENGTH, 37).skill(Skill.RANGED, 19)
			.item(DiaryStages.MITH_GRAPPLE)
			.item(DiaryStages.ANY_CROSSBOW)
			.stage(DiaryStage.builder()
				.title("Grapple up the north wall")
				.at(Location.of("North Falador wall", 3032, 3389, 0, 2))
				.object("Wall")
				.line("1. Wield the crossbow and the Mith grapple (in the ammo slot).")
				.line("2. Right-click the Wall (outlined) on the north-east side of Falador and choose Grapple.")
				.build())
			.stage(DiaryStage.builder()
				.title("Jump off the wall")
				.inside(Zone.of(3022, 3389, 3039, 3390, 1))
				.started(true)
				.object("Wall").object("Battlement")
				.line("1. Click Jump on the wall's edge (outlined) to get down.")
				.build())
			.build());
		out.add(task(t, 12, "Willow logs in Taverley", "Chop and burn some Willow logs in Taverley.")
			.skill(Skill.WOODCUTTING, 30).skill(Skill.FIREMAKING, 30)
			.item(DiaryItem.tool(ToolType.AXE))
			.item(DiaryItem.of("Tinderbox", ItemID.TINDERBOX))
			.stage(DiaryStage.builder()
				.title("Chop a willow in Taverley")
				.at(Location.of("Willows by the Taverley lake", 2925, 3412, 0, 4))
				.object("Willow tree").object("Willow")
				.line("1. The willows are by the lake in south-east Taverley.")
				.line("2. Chop a Willow (outlined) until you get Willow logs.")
				.build())
			.stage(DiaryStage.builder()
				.title("Burn the willow logs in Taverley")
				.carrying(ItemID.WILLOW_LOGS)
				.use(ItemID.TINDERBOX).use(ItemID.WILLOW_LOGS)
				.line("1. Stay in Taverley and use the Tinderbox on the Willow logs (both outlined).")
				.build())
			.build());
		out.add(task(t, 3, "Crystal chest", "Unlock the Crystal chest in Taverley.")
			.item(DiaryItem.of("Crystal key", ItemID.CRYSTAL_KEY))
			.stage(DiaryStage.builder()
				.title("Open the crystal chest")
				.at(Location.of("Crystal chest, Taverley", 2914, 3452, 0, 3))
				.object("Closed chest").object("Crystal chest")
				.use(ItemID.CRYSTAL_KEY)
				.line("1. The chest is in a house in north Taverley, east of the herblore shop.")
				.line("2. Use the Crystal key on the chest (outlined).")
				.build())
			.build());
		out.add(task(t, 9, "Pray in Initiate armour", "Pray at the Altar of Guthix in Taverley whilst wearing full Initiate.")
			.quest(Quest.RECRUITMENT_DRIVE)
			.skill(Skill.PRAYER, 10).skill(Skill.DEFENCE, 20)
			.item(DiaryItem.of("Initiate sallet", ItemID.BASIC_TK_HELM))
			.item(DiaryItem.of("Initiate hauberk", ItemID.BASIC_TK_BODY))
			.item(DiaryItem.of("Initiate cuisse", ItemID.BASIC_TK_LEGS))
			.stage(DiaryStage.builder()
				.title("Pray at the Altar of Guthix in full Initiate")
				.at(Location.of("Altar of Guthix, Taverley", 2925, 3483, 0, 3))
				.object("Altar of Guthix")
				.line("1. Wear the Initiate sallet, hauberk and cuisse.")
				.line("2. Click Pray-at on the Altar of Guthix (outlined) in the stone circle north of Taverley.")
				.build())
			.build());
		out.add(task(t, 2, "Telegrab Wine of Zamorak", "Telegrab some Wine of Zamorak at the Chaos Temple by the Wilderness.")
			.skill(Skill.MAGIC, 33)
			.item(DiaryItem.of("Law rune", 1, ItemID.LAWRUNE))
			.item(DiaryItem.of("Air rune", 1, ItemID.AIRRUNE))
			.stage(DiaryStage.builder()
				.title("Telegrab the Wine of Zamorak")
				.at(Location.of("Chaos Temple north-west of Goblin Village", 2934, 3516, 0, 2))
				.inside(Zone.of(2930, 3513, 2940, 3518, 0))
				.npc("Monk of Zamorak")
				.line("1. The temple is north-west of the Goblin Village (south of the Wilderness, it's safe).")
				.line("2. Stand inside and cast Telekinetic Grab on the Wine of Zamorak on the table.")
				.line("3. Stay inside until the jug lands in your inventory. The monks attack if you pick it up by hand.")
				.build())
			.build());
		out.add(task(t, 10, "Gold in the Crafting Guild", "Mine some Gold ore at the Crafting Guild.")
			.skill(Skill.CRAFTING, 40).skill(Skill.MINING, 40)
			.item(DiaryItem.tool(ToolType.PICKAXE))
			.item(DiaryItem.anyOf("Brown apron", 1, ItemID.BROWN_APRON, ItemID.GOLDEN_APRON, ItemID.BROWN_APRON,
				ItemID.SKILLCAPE_CRAFTING_TRIMMED, ItemID.SKILLCAPE_CRAFTING))
			.stage(DiaryStage.builder()
				.title("Go into the Crafting Guild")
				.at(Location.of("Crafting Guild door, Rimmington", 2933, 3289, 0, 2))
				.object("Guild Door")
				.line("1. Wear the Brown apron (or golden apron / Crafting cape).")
				.line("2. Open the Guild Door (outlined), north-west of Rimmington.")
				.build())
			.stage(DiaryStage.builder()
				.title("Mine gold ore in the guild")
				.inside(Zone.of(2929, 3276, 2943, 3288, 0))
				.at(Location.of("Gold rocks, Crafting Guild", 2938, 3280, 0, 3))
				.object("Gold rocks")
				.line("1. Mine the yellow Gold rocks (outlined) in the south of the guild until you get one ore.")
				.build())
			.build());
		out.add(task(t, 1, "Bullseye lantern", "Light a Bullseye lantern at the Chemist's in Rimmington.")
			.skill(Skill.FIREMAKING, 49)
			.item(DiaryItem.of("Bullseye lantern", ItemID.BULLSEYE_LANTERN_UNLIT))
			.item(DiaryItem.of("Tinderbox", ItemID.TINDERBOX))
			.stage(DiaryStage.builder()
				.title("Light the lantern in the Chemist's")
				.at(Location.of("Chemist's house, Rimmington", 2932, 3210, 0, 2))
				.inside(Zone.of(2929, 3207, 2936, 3213, 0))
				.use(ItemID.TINDERBOX).use(ItemID.BULLSEYE_LANTERN_UNLIT)
				.line("1. Walk into the Chemist's house in west Rimmington.")
				.line("2. Use the Tinderbox on the Bullseye lantern (both outlined).")
				.build())
			.build());
		out.add(task(t, 13, "Fruit basket", "Craft a fruit basket on the Falador Farm loom.")
			.skill(Skill.CRAFTING, 36)
			.item(DiaryItem.of("Willow branch", 6, ItemID.WILLOW_BRANCH))
			.stage(DiaryStage.builder()
				.title("Weave a basket on the farm loom")
				.at(Location.of("Loom in the farm south of Falador", 3039, 3287, 0, 2))
				.object("Loom")
				.use(ItemID.WILLOW_BRANCH)
				.line("1. The loom is in the farmhouse next to Sarah's shop.")
				.line("2. Use the Willow branches on the Loom (outlined) to make a basket.")
				.build())
			.build());
		out.add(task(t, 4, "Scarecrow", "Place a Scarecrow in the Falador farm flower patch.")
			.skill(Skill.FARMING, 23)
			.item(DiaryItem.of("Empty sack", ItemID.SACK_EMPTY))
			.item(DiaryItem.of("Bronze spear", ItemID.BRONZE_SPEAR))
			.item(DiaryItem.of("Watermelon", ItemID.WATERMELON))
			.stage(DiaryStage.builder()
				.title("Fill the sack with hay")
				.at(Location.of("Hay bales in the chicken coop south of Falador", 3019, 3297, 0, 3))
				.object("Hay bales").object("Haystack")
				.use(ItemID.SACK_EMPTY)
				.line("1. Use the Empty sack (outlined) on the Hay bales (outlined) in the chicken coop.")
				.build())
			.stage(DiaryStage.builder()
				.title("Put the hay sack on the spear")
				.carrying(ItemID.SCARECROW_TORSO)
				.started(true)
				.use(ItemID.SCARECROW_TORSO).use(ItemID.BRONZE_SPEAR)
				.line("1. Use the Hay sack on the Bronze spear (both outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Add the watermelon head")
				.carrying(ItemID.SCARECROW_TORSO_SPEAR)
				.started(true)
				.use(ItemID.WATERMELON).use(ItemID.SCARECROW_TORSO_SPEAR)
				.line("1. Use the Watermelon on the hay sack and spear (both outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Plant the scarecrow in the flower patch")
				.carrying(ItemID.SCARECROW_COMPLETE)
				.started(true)
				.at(Location.of("Flower patch, farm south of Falador", 3054, 3307, 0, 3))
				.object("Flower Patch").object("Flower patch")
				.use(ItemID.SCARECROW_COMPLETE)
				.line("1. Rake the Flower patch (outlined) if it has weeds.")
				.line("2. Use the Scarecrow on the Flower patch.")
				.build())
			.build());
		out.add(task(t, 6, "Rat Pits", "Visit the Port Sarim Rat Pits.")
			.questStarted(Quest.RATCATCHERS)
			.stage(DiaryStage.builder()
				.title("Climb down to the Rat Pits")
				.at(Location.of("Manhole in Port Sarim", 3018, 3232, 0, 2))
				.object("Manhole")
				.line("1. The manhole is in north Port Sarim, by the food shop.")
				.line("2. Open the Manhole (outlined) and Climb-down.")
				.build())
			.build());
		out.add(task(t, 5, "Kill a Mogre", "Kill a Mogre at Mudskipper Point.")
			.quest(Quest.SKIPPY_AND_THE_MOGRES)
			.skill(Skill.SLAYER, 32)
			.item(DiaryItem.owned("Fishing explosive", "Buy it from a Slayer master's shop (Turael in Burthorpe, "
				+ "Vannaka in Edgeville Dungeon) for about 60 coins.", ItemID.FISHING_EXPLOSIVE))
			.stage(DiaryStage.builder()
				.title("Blast a Mogre out of the water and kill it")
				.at(Location.of("Ominous fishing spot, Mudskipper Point", 3005, 3117, 0, 3))
				.object("Ominous Fishing Spot").object("Ominous fishing spot")
				.npc("Mogre")
				.use(ItemID.FISHING_EXPLOSIVE)
				.line("1. Mudskipper Point is south of Port Sarim. Bring food: Mogres hit up to 6.")
				.line("2. Use the Fishing explosive on the Ominous Fishing Spot (outlined).")
				.line("3. Kill the Mogre (outlined) that jumps out.")
				.build())
			.build());
	}

	private static void hard(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.HARD;
		out.add(task(t, 2, "Saradomin crest", "Change your family crest to the Saradomin symbol.")
			.skill(Skill.PRAYER, 70).unboostable(Skill.PRAYER)
			.skill(Skill.CONSTRUCTION, 16)
			.item(DiaryItem.of("Coins", 5_000, ItemID.COINS))
			.stage(castleUp("then walk west to Sir Renitee"))
			.stage(reniteeStage("Change your crest to Saradomin",
				"1. Talk to Sir Renitee (outlined) and ask to change your family crest.",
				"2. Pick the Saradomin symbol and pay 5,000 coins.",
				"If it's already Saradomin, change it to another and back (5,000 more)."))
			.build());
		out.add(task(t, 5, "Falador rooftops", "Complete a lap of the Falador rooftop agility course.")
			.skill(Skill.AGILITY, 50)
			.stage(DiaryStage.builder()
				.title("Run a lap of the Falador rooftops")
				.at(Location.of("Falador rooftop course start", 3036, 3341, 0, 2))
				.object("Rough wall").object("Tightrope").object("Hand holds").object("Gap").object("Ledge")
				.object("Edge")
				.line("1. Climb the Rough wall (outlined) east of Falador's east bank.")
				.line("2. Cross each outlined obstacle until you jump off the last Edge. Falling means starting again.")
				.build())
			.build());
		out.add(task(t, 6, "Prospector in the Mining Guild", "Enter the mining guild while wearing a Prospector helmet.")
			.skill(Skill.MINING, 60)
			.item(DiaryItem.owned("Prospector helmet", "Buy it from Prospector Percy in the Motherlode Mine for "
				+ "40 golden nuggets (found while cleaning pay-dirt).", ItemID.MOTHERLODE_REWARD_HAT))
			.stage(mineStairs())
			.stage(DiaryStage.builder()
				.title("Walk into the Mining Guild in the prospector helmet")
				.inside(DWARVEN_MINE)
				.at(Location.of("Mining Guild door, Dwarven Mine", 3046, 9756, 0, 2))
				.object("Door")
				.line("1. Wear the Prospector helmet.")
				.line("2. Open the guild Door (outlined) in the south of the mine and walk in.")
				.build())
			.build());
		out.add(task(t, 11, "Dwarven helmet in the mines", "Equip a dwarven helmet within the dwarven mines.")
			.quest(Quest.GRIM_TALES)
			.skill(Skill.DEFENCE, 50)
			.item(DiaryItem.of("Dwarven helmet", ItemID.GRIM_WEAR_HELMET))
			.stage(mineStairs())
			.stage(DiaryStage.builder()
				.title("Put on the dwarven helmet")
				.inside(DWARVEN_MINE)
				.use(ItemID.GRIM_WEAR_HELMET)
				.line("1. Click the Dwarven helmet (outlined) to wear it while you're in the mine.")
				.build())
			.build());
		out.add(task(t, 3, "Giant Mole", "Kill the Giant Mole beneath Falador Park.")
			.item(DiaryItem.of("Spade", ItemID.SPADE))
			.item(DiaryStages.LIGHT_SOURCE)
			.note("Gear and food to kill a level 230 boss (it digs away at low health)")
			.stage(DiaryStage.builder()
				.title("Dig into the mole's lair")
				.at(Location.of("Mole hills, Falador Park", 2989, 3378, 0, 4))
				.object("Mole hill")
				.line("1. Light your light source first; the lair is pitch dark.")
				.line("2. Stand on a Mole hill (outlined) in Falador Park and click Dig on your spade.")
				.build())
			.stage(DiaryStage.builder()
				.title("Kill the Giant Mole")
				.inside(Zone.of(1731, 5132, 1789, 5248, 0))
				.npc("Giant Mole")
				.line("1. Find the Giant Mole (outlined) and attack it.")
				.line("2. When it digs away, look for the dirt on the ground and follow it.")
				.build())
			.build());
		out.add(task(t, 9, "Pray in Proselyte armour", "Recharge your prayer in the Port Sarim church while wearing full Proselyte.")
			.quest(Quest.THE_SLUG_MENACE)
			.skill(Skill.DEFENCE, 30).skill(Skill.PRAYER, 20)
			.item(DiaryItem.of("Proselyte sallet", ItemID.BASIC_TK_RANK2_HELM))
			.item(DiaryItem.of("Proselyte hauberk", ItemID.BASIC_TK_RANK2_BODY))
			.item(DiaryItem.anyOf("Proselyte cuisse or tasset", 1, ItemID.BASIC_TK_RANK2_LEGS,
				ItemID.BASIC_TK_RANK2_LEGS, ItemID.BASIC_TK_RANK2_PLATESKIRT))
			.stage(DiaryStage.builder()
				.title("Pray at the Port Sarim church altar in full Proselyte")
				.at(Location.of("Port Sarim church", 2995, 3177, 0, 3))
				.object("Altar")
				.line("1. Wear the Proselyte sallet, hauberk and cuisse (or tasset).")
				.line("2. Use a bit of prayer first (any prayer for a moment), then click Pray-at on the Altar (outlined).")
				.build())
			.build());
		out.add(task(t, 4, "Skeletal Wyvern", "Kill a Skeletal Wyvern in the Asgarnia Ice Dungeon.")
			.skill(Skill.SLAYER, 72)
			.note("An elemental, mind or dragonfire shield / ancient wyvern shield against the icy breath")
			.stage(iceDungeonTrapdoor())
			.stage(DiaryStage.builder()
				.title("Go into the wyvern cave")
				.inside(ICE_DUNGEON)
				.at(Location.of("Wyvern cave entrance, Ice Dungeon", 3055, 9560, 0, 2))
				.object("Icy cavern")
				.line("1. Walk east past the blurite rocks to the cave entrance (outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Kill a Skeletal Wyvern")
				.inside(Zone.of(3030, 9556, 3083, 9602, 0))
				.npc("Skeletal Wyvern")
				.line("1. Wear your shield against icy breath and Protect from Missiles (they hit hard in melee).")
				.line("2. Kill one Skeletal Wyvern (outlined).")
				.build())
			.build());
		out.add(task(t, 1, "140 mind runes", "Craft 140 Mind runes simultaneously from Essence without the use of Extracts.")
			.skill(Skill.RUNECRAFT, 56)
			.note("Raiments of the Eye pieces lower the level needed")
			.item(DiaryItem.anyOf("Essence", 27, ItemID.BLANKRUNE_HIGH, DiaryStages.ESSENCE))
			.item(MIND_ACCESS)
			.stages(DiaryStages.runeAltar("north of Falador, west of the Goblin Village road", MIND_RUINS, MIND_ALTAR,
				"Craft 140 mind runes at once", ItemID.MIND_TALISMAN,
				"At 56 Runecraft each essence makes 5 or more, so a full inventory makes over 140."))
			.build());
		out.add(task(t, 10, "Warriors' Guild", "Enter the Warriors' Guild.")
			.note("130 Attack + Strength combined, or 99 in one of them")
			.stage(DiaryStage.builder()
				.title("Walk into the Warriors' Guild")
				.at(Location.of("Warriors' Guild door, Burthorpe", 2877, 3546, 0, 2))
				.object("Door")
				.line("1. The guild is in Burthorpe, north-west of Taverley.")
				.line("2. Open the Door (outlined) and walk in.")
				.build())
			.build());
		out.add(task(t, 8, "Rogues' Den safe", "Crack a wall safe within Rogues' Den.")
			.skill(Skill.THIEVING, 50)
			.note("Bring some food: a failed crack hurts. A stethoscope helps")
			.stage(DiaryStage.builder()
				.title("Climb into the Rogues' Den")
				.at(Location.of("Rogues' Den trapdoor, Burthorpe", 2905, 3537, 0, 2))
				.object("Trapdoor")
				.line("1. The trapdoor is in the Toad and Chicken pub in Burthorpe. Climb-down it (outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Crack a wall safe")
				.inside(Zone.of(3036, 4956, 3067, 4991, 1))
				.object("Wall safe")
				.line("1. Click Crack on a Wall safe (outlined) along the walls of the main room, not in the maze.")
				.line("2. Keep trying until one opens.")
				.build())
			.build());
		out.add(task(t, 7, "Blue dragon under the Heroes' Guild", "Kill the Blue Dragon under the Heroes' Guild.")
			.quest(Quest.HEROES_QUEST)
			.item(DiaryStages.DRAGONFIRE_SHIELD)
			.stage(DiaryStage.builder()
				.title("Go into the Heroes' Guild")
				.at(Location.of("Heroes' Guild, Burthorpe", 2902, 3510, 0, 3))
				.object("Door").object("Ladder")
				.line("1. Wear your anti-dragon shield and bring food.")
				.line("2. Go into the Heroes' Guild and Climb-down the Ladder (outlined) in the north-west room.")
				.build())
			.stage(DiaryStage.builder()
				.title("Kill the blue dragon")
				.inside(Zone.of(2883, 9881, 2943, 9919, 0))
				.npc("Blue dragon")
				.line("1. Walk to the Blue dragon (outlined) in the basement and kill it.")
				.build())
			.build());
	}

	private static void elite(List<DiaryTask> out)
	{
		DiaryTier t = DiaryTier.ELITE;
		out.add(task(t, 6, "Saradomin brew in the east bank", "Mix a Saradomin brew in Falador east bank.")
			.skill(Skill.HERBLORE, 81)
			.item(DiaryItem.of("Toadflax potion (unf)", ItemID.TOADFLAXVIAL))
			.item(DiaryItem.of("Crushed nest", ItemID.CRUSHED_BIRD_NEST))
			.stage(DiaryStage.builder()
				.title("Mix the brew inside Falador east bank")
				.at(Location.of("Falador east bank", 3013, 3356, 0, 2))
				.inside(Zone.of(3009, 3355, 3018, 3358, 0))
				.use(ItemID.CRUSHED_BIRD_NEST).use(ItemID.TOADFLAXVIAL)
				.line("1. Stand inside Falador east bank.")
				.line("2. Use the Crushed nest on the Toadflax potion (unf) (both outlined).")
				.build())
			.build());
		out.add(task(t, 4, "Skillcape emote on the castle", "Perform a skillcape or quest cape emote at the top of Falador Castle.")
			.item(DiaryStages.SKILLCAPE)
			.stage(DiaryStage.builder()
				.title("Climb the castle's west tower")
				.at(Location.of("Spiral staircase, Falador Castle west wing", 2954, 3338, 0, 2))
				.object("Staircase")
				.line("1. In the castle's west wing, Climb-up the spiral Staircase (outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Keep climbing")
				.inside(CASTLE_1)
				.at(Location.of("Spiral staircase, first floor", 2960, 3338, 1, 2))
				.object("Staircase")
				.line("1. Climb-up the Staircase (outlined) again.")
				.build())
			.stage(DiaryStage.builder()
				.title("Keep climbing")
				.inside(CASTLE_2)
				.at(Location.of("Spiral staircase, second floor", 2957, 3338, 2, 2))
				.object("Staircase")
				.line("1. Climb-up the Staircase (outlined) to the roof.")
				.build())
			.stage(DiaryStage.builder()
				.title("Do the Skillcape emote on the roof")
				.inside(CASTLE_3)
				.line("1. Wear the cape.")
				.line("2. Open the Emotes tab (the smiley face) and click Skillcape.")
				.build())
			.build());
		out.add(task(t, 2, "White 2h sword", "Purchase a White 2h Sword from Sir Vyvin.")
			.quest(Quest.WANTED)
			.note("White Knight Master rank (1,300 Black Knight kill score)")
			.item(DiaryItem.of("Coins", 1_920, ItemID.COINS))
			.stage(castleUp("then the stairs next to it"))
			.stage(DiaryStage.builder()
				.title("Go up one more floor")
				.inside(CASTLE_1)
				.at(Location.of("Castle staircase, first floor", 2985, 3338, 1, 2))
				.object("Staircase")
				.line("1. Climb-up the Staircase (outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Buy the White 2h sword from Sir Vyvin")
				.inside(CASTLE_2)
				.at(Location.of("Sir Vyvin, Falador Castle", 2981, 3338, 2, 3))
				.npc("Sir Vyvin")
				.line("1. Right-click Sir Vyvin (outlined), choose Trade and buy the White 2h sword.")
				.build())
			.build());
		out.add(task(t, 3, "Magic roots", "Find at least 3 magic roots at once when digging up your magic tree in Falador.")
			.skill(Skill.FARMING, 91).skill(Skill.WOODCUTTING, 75)
			.item(DiaryItem.of("Magic sapling", ItemID.PLANTPOT_MAGIC_TREE_SAPLING))
			.item(DiaryItem.of("Spade", ItemID.SPADE))
			.item(DiaryItem.tool(ToolType.AXE))
			.stage(DiaryStage.builder()
				.title("Grow a magic tree in Falador Park, then dig up its roots")
				.at(Location.of("Tree patch, Falador Park", 3004, 3373, 0, 3))
				.object("Tree patch").object("Magic tree").object("Tree stump")
				.line("1. Rake the tree patch if needed and plant the Magic sapling.")
				.line("2. It takes about 8 hours to grow; come back later.")
				.line("3. Chop the grown tree down yourself (the gardener won't do for this task).")
				.line("4. Dig on the stump with your spade; at 91 Farming you get 3 or more roots.")
				.build())
			.build());
		out.add(task(t, 5, "Taverley strange floor", "Jump over the strange floor in Taverley dungeon.")
			.skill(Skill.AGILITY, 80)
			.stage(DiaryStage.builder()
				.title("Climb down into Taverley Dungeon")
				.at(Location.of("Taverley Dungeon ladder", 2884, 3397, 0, 2))
				.object("Ladder")
				.line("1. The ladder is in a small building just south of Taverley.")
				.line("2. Click Climb-down on the Ladder (outlined).")
				.build())
			.stage(DiaryStage.builder()
				.title("Jump the strange floor")
				.inside(Zone.of(2809, 9786, 2949, 9846, 0))
				.at(Location.of("Strange floor, Taverley Dungeon", 2879, 9813, 0, 2))
				.object("Strange floor")
				.line("1. Click Jump-over on the Strange floor (outlined).")
				.line("2. You must take no damage. If you get hit, try again.")
				.build())
			.build());
		out.add(task(t, 1, "252 air runes", "Craft 252 Air Runes simultaneously from Essence without the use of Extracts.")
			.skill(Skill.RUNECRAFT, 88)
			.note("Raiments of the Eye pieces lower the level needed")
			.item(DiaryItem.anyOf("Essence", 28, ItemID.BLANKRUNE_HIGH, DiaryStages.ESSENCE))
			.item(DiaryItem.anyOf("Air tiara or talisman", 1, ItemID.AIR_TALISMAN, ItemID.TIARA_ELEMENTAL,
				ItemID.TIARA_AIR, ItemID.ELEMENTAL_TALISMAN, ItemID.AIR_TALISMAN))
			.stages(DiaryStages.runeAltar("south of Falador, by the road to Rimmington",
				Location.of("Air altar ruins, south of Falador", 2985, 3292, 0, 3), Zone.of(2859, 4819, 2895, 4851, 0),
				"Craft 252 air runes at once", ItemID.AIR_TALISMAN,
				"Wear the tiara so the essence fills all 28 slots; at 88 each essence makes 9."))
			.build());
	}

	/**
	 * Starts a task. The done bits are in wiki order across both Falador diary varps, except bit 19, which is unused.
	 */
	private static DiaryTask.DiaryTaskBuilder task(DiaryTier tier, int number, String name, String text)
	{
		int index;
		switch (tier)
		{
			case EASY:
				index = number - 1;
				break;
			case MEDIUM:
				index = number <= 8 ? 10 + number : 11 + number;
				break;
			case HARD:
				index = 25 + number;
				break;
			default:
				index = 36 + number;
		}
		return DiaryTask.builder()
			.region(DiaryRegion.FALADOR)
			.tier(tier)
			.number(number)
			.name(name)
			.text(text)
			.varp(index < 32 ? VarPlayerID.FALADOR_ACHIEVEMENT_DIARY : VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2)
			.bit(index % 32);
	}
}
