package com.skillinghelper.data.guides;

import com.skillinghelper.data.Banks;
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
 * Based on the OSRS wiki "Free-to-play Runecraft training" and "Pay-to-play Runecraft training" guides. Altar ruins
 * coordinates match RuneLite's world map; the altar rooms inside count as being at the training spot too.
 */
final class RunecraftGuide
{
	private static final String F2P_WIKI = "Free-to-play_Runecraft_training";
	private static final String P2P_WIKI = "Pay-to-play_Runecraft_training";
	private static final ItemReq ESSENCE = ItemReq.consumed("Rune or pure essence", 28, 1, ItemID.BLANKRUNE,
		ItemID.BLANKRUNE_HIGH);
	// About 1.7 minutes per trip with 28 essence, running between the bank and the altar
	private static final int ESSENCE_PER_HOUR = 1_000;

	static List<Method> methods()
	{
		return List.of(
			altar("rc_air", "Air runes (Falador)", 1, 5, Location.of("Air altar ruins", 2985, 3293, 6),
				Location.of("Air altar", 2843, 4834, 20), Banks.FALADOR_EAST, "Air",
				ItemID.AIR_TALISMAN, ItemID.TIARA_AIR, ItemID.AIRRUNE)
				.tag(MethodTag.CHEAP)
				.notes("From level 11 every essence makes two air runes. Wear an air tiara to free the talisman slot.")
				.build(),
			altar("rc_mind", "Mind runes (Falador)", 2, 5.5, Location.of("Mind altar ruins", 2982, 3514, 6),
				Location.of("Mind altar", 2786, 4841, 20), Banks.FALADOR_WEST, "Mind",
				ItemID.MIND_TALISMAN, ItemID.TIARA_MIND, ItemID.MINDRUNE)
				.notes("North of Falador, just outside the Wilderness. Mind runes sell steadily for low-level magic.")
				.build(),
			altar("rc_water", "Water runes (Lumbridge Swamp)", 5, 6, Location.of("Water altar ruins", 3185, 3165, 6),
				Location.of("Water altar", 2716, 4836, 20), Banks.DRAYNOR, "Water",
				ItemID.WATER_TALISMAN, ItemID.TIARA_WATER, ItemID.WATERRUNE)
				.build(),
			altar("rc_earth", "Earth runes (Varrock)", 9, 6.5, Location.of("Earth altar ruins", 3306, 3474, 6),
				Location.of("Earth altar", 2657, 4841, 20), Banks.VARROCK_EAST, "Earth",
				ItemID.EARTH_TALISMAN, ItemID.TIARA_EARTH, ItemID.EARTHRUNE)
				.build(),
			altar("rc_fire", "Fire runes (Al Kharid)", 14, 7, Location.of("Fire altar ruins", 3313, 3255, 6),
				Location.of("Fire altar", 2585, 4838, 20), Banks.AL_KHARID, "Fire",
				ItemID.FIRE_TALISMAN, ItemID.TIARA_FIRE, ItemID.FIRERUNE)
				.tag(MethodTag.FAST)
				.notes("Members can teleport to the Emir's Arena with a ring of dueling to save running.")
				.build(),
			altar("rc_body", "Body runes (Edgeville)", 20, 7.5, Location.of("Body altar ruins", 3053, 3445, 6),
				Location.of("Body altar", 2522, 4840, 20), Banks.EDGEVILLE, "Body",
				ItemID.BODY_TALISMAN, ItemID.TIARA_BODY, ItemID.BODYRUNE)
				.build(),
			Method.builder()
				.id("rc_lava")
				.skill(Skill.RUNECRAFT)
				.type(MethodType.PROCESS)
				.name("Lava runes (Fire altar)")
				.minLevel(23)
				.xpPerAction(10.5)
				.xpPerHour(50_000)
				.members(true)
				.tag(MethodTag.FAST)
				.location(Location.of("Fire altar ruins", 3313, 3255, 6))
				.alsoAt(Location.of("Fire altar", 2585, 4838, 20))
				.bank(Banks.AL_KHARID)
				.tool(ItemReq.tool("Fire tiara or talisman", ItemID.TIARA_FIRE, ItemID.FIRE_TALISMAN))
				.tool(ItemReq.tool("Earth talisman", ItemID.EARTH_TALISMAN))
				.input(ESSENCE)
				.input(ItemReq.consumed("Earth runes", 28, 1, ItemID.EARTHRUNE))
				.supply(ItemReq.supply("Binding necklace", 1, ItemID.MAGIC_EMERALD_NECKLACE))
				.supply(Supplies.STAMINA)
				.product(ItemID.LAVARUNE)
				.target(Target.object("Enter", "Mysterious ruins"))
				.target(Target.object(null, "Altar"))
				.instruction("Enter the fire altar with essence and earth runes, then use the earth talisman on the altar "
					+ "to make lava runes")
				.notes("The fastest Runecraft xp. Wear a binding necklace or half the runes fail. Essence pouches and a "
					+ "ring of dueling to the Emir's Arena make it much faster.")
				.wikiPage(P2P_WIKI)
				.rateAt(50, 60_000).rateAt(70, 70_000).rateAt(90, 80_000)
				.build(),
			Method.builder()
				.id("rc_ourania")
				.skill(Skill.RUNECRAFT)
				.type(MethodType.ACTIVITY)
				.name("Ourania Altar (ZMI)")
				.minLevel(1)
				.xpPerAction(15)
				.xpPerHour(25_000)
				.members(true)
				.tag(MethodTag.PROFIT)
				.location(Location.of("Ourania Cave entrance", 2453, 3231, 8))
				.alsoAt(Location.of("Ourania Altar", 3058, 5580, 25))
				.input(ItemReq.consumed("Pure essence", 28, 1, ItemID.BLANKRUNE_HIGH))
				.supply(Supplies.STAMINA)
				.target(Target.object(null, "Altar"))
				.target(Target.npc(null, "Eniola"))
				.instruction("Bank with Eniola (pay one rune per visit), run to the Ourania Altar and craft random runes")
				.notes("Gives random runes from every altar, more high-level ones as you level. Lunar Diplomacy's "
					+ "Ourania Teleport lands at the cave. Gives double xp compared to normal altars.")
				.wikiPage("Ourania_Altar")
				.rateAt(50, 45_000).rateAt(70, 55_000).rateAt(90, 65_000)
				.build(),
			altar("rc_cosmic", "Cosmic runes (Zanaris)", 27, 8, Location.of("Cosmic altar ruins", 2408, 4377, 6),
				Location.of("Cosmic altar", 2142, 4813, 20), Location.of("Zanaris bank", 2383, 4458, 5), "Cosmic",
				ItemID.COSMIC_TALISMAN, ItemID.TIARA_COSMIC, ItemID.COSMICRUNE)
				.members(true).quest(Quest.LOST_CITY)
				.notes("In Zanaris (Lost City). Pure essence only. Double cosmics from 59.")
				.wikiPage(P2P_WIKI)
				.build(),
			Method.builder()
				.id("rc_astral")
				.skill(Skill.RUNECRAFT)
				.type(MethodType.PROCESS)
				.name("Astral runes (Lunar Isle)")
				.minLevel(40)
				.xpPerAction(8.7)
				.xpPerHour(25_000)
				.members(true)
				.tag(MethodTag.PROFIT)
				.quest(Quest.LUNAR_DIPLOMACY)
				.location(Location.of("Astral altar", 2158, 3864, 6))
				.bank(Location.of("Lunar Isle bank", 2099, 3919, 5))
				.input(ItemReq.consumed("Pure essence", 28, 1, ItemID.BLANKRUNE_HIGH))
				.supply(Supplies.STAMINA)
				.product(ItemID.ASTRALRUNE)
				.target(Target.object("Craft-rune", "Altar"))
				.instruction("Run from the Lunar Isle bank to the astral altar and craft astral runes")
				.notes("No talisman needed. Wear a seal of passage unless you've done Dream Mentor. Double astrals "
					+ "from 82.")
				.wikiPage(P2P_WIKI)
				.rateAt(82, 40_000)
				.build(),
			altar("rc_nature", "Nature runes (Karamja)", 44, 9, Location.of("Nature altar ruins", 2869, 3019, 6),
				Location.of("Nature altar", 2400, 4841, 20), Banks.SHILO, "Nature",
				ItemID.NATURE_TALISMAN, ItemID.TIARA_NATURE, ItemID.NATURERUNE)
				.members(true).tag(MethodTag.PROFIT)
				.notes("Pure essence only. Bank at Shilo Village; nature runes always sell for alching.")
				.wikiPage(P2P_WIKI)
				.build(),
			Method.builder()
				.id("rc_gotr")
				.skill(Skill.RUNECRAFT)
				.type(MethodType.ACTIVITY)
				.name("Guardians of the Rift")
				.minLevel(27)
				.xpPerAction(60)
				.xpPerHour(30_000)
				.members(true)
				.tag(MethodTag.MINIGAME).tag(MethodTag.FAST)
				.quest(Quest.TEMPLE_OF_THE_EYE)
				.location(Location.of("Guardians of the Rift", 3615, 9487, 30))
				.toolType(ToolType.PICKAXE)
				.target(Target.object(null, "Guardian parts", "Large guardian remains", "Huge guardian remains"))
				.target(Target.object(null, "Workbench", "Deposit Pool", "Portal"))
				.target(Target.npc(null, "The Great Guardian"))
				.instruction("Mine guardian fragments, craft essence at the workbench, enter the active altar portals, "
					+ "craft runes and power up the Great Guardian")
				.notes("Earns the Raiments of the Eye outfit (up to +60% runes) and abyssal pouches. "
					+ "The best way to start members Runecraft.")
				.wikiPage("Guardians_of_the_Rift")
				.rateAt(50, 40_000).rateAt(77, 55_000).rateAt(90, 60_000)
				.build(),
			Method.builder()
				.id("rc_blood_arceuus")
				.skill(Skill.RUNECRAFT)
				.type(MethodType.ACTIVITY)
				.name("Blood runes (Arceuus, AFK)")
				.minLevel(77)
				.xpPerAction(24)
				.xpPerHour(38_000)
				.members(true)
				.tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.location(Location.of("Dense runestones", 1764, 3855, 12))
				.alsoAt(Location.of("Blood altar", 1716, 3827, 12))
				.toolType(ToolType.PICKAXE)
				.tool(ItemReq.tool("Chisel", ItemID.CHISEL))
				.target(Target.object("Chip", "Dense runestone"))
				.target(Target.object("Venerate", "Dark Altar"))
				.target(Target.object("Bind", "Blood Altar"))
				.instruction("Mine dense essence, venerate it at the Dark Altar, chisel it into fragments and bind "
					+ "them at the Blood Altar")
				.requirementText("38 Mining and 38 Crafting")
				.wikiPage(P2P_WIKI)
				.build(),
			Method.builder()
				.id("rc_soul_arceuus")
				.skill(Skill.RUNECRAFT)
				.type(MethodType.ACTIVITY)
				.name("Soul runes (Arceuus, AFK)")
				.minLevel(90)
				.xpPerAction(30)
				.xpPerHour(45_000)
				.members(true)
				.tag(MethodTag.AFK).tag(MethodTag.PROFIT)
				.location(Location.of("Dense runestones", 1764, 3855, 12))
				.alsoAt(Location.of("Soul altar", 1814, 3856, 12))
				.toolType(ToolType.PICKAXE)
				.tool(ItemReq.tool("Chisel", ItemID.CHISEL))
				.target(Target.object("Chip", "Dense runestone"))
				.target(Target.object("Venerate", "Dark Altar"))
				.target(Target.object("Bind", "Soul Altar"))
				.instruction("Mine dense essence, venerate it at the Dark Altar, chisel it into fragments and bind "
					+ "them at the Soul Altar")
				.requirementText("38 Mining and 38 Crafting")
				.notes("Like blood runes but a little more xp and profit.")
				.wikiPage(P2P_WIKI)
				.build()
		);
	}

	private static Method.MethodBuilder altar(String id, String name, int level, double xp, Location ruins,
		Location altarRoom, Location bank, String element, int talisman, int tiara, int rune)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.RUNECRAFT)
			.type(MethodType.PROCESS)
			.name(name)
			.minLevel(level)
			.xpPerAction(xp)
			.xpPerHour((int) (xp * ESSENCE_PER_HOUR))
			.location(ruins)
			.alsoAt(altarRoom)
			.bank(bank)
			.tool(ItemReq.tool(element + " talisman or tiara", tiara, talisman))
			.input(ESSENCE)
			.supply(Supplies.STAMINA)
			.product(rune)
			.target(Target.object("Enter", "Mysterious ruins"))
			.target(Target.object("Craft-rune", "Altar"))
			.instruction("Enter the ruins and click the altar to craft " + element.toLowerCase() + " runes")
			.wikiPage(F2P_WIKI);
	}

	private RunecraftGuide()
	{
	}
}
