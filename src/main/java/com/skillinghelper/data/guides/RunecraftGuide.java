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
	// About 2.5 minutes per trip with 28 essence
	private static final int ESSENCE_PER_HOUR = 650;

	static List<Method> methods()
	{
		return List.of(
			altar("rc_air", "Air runes (Falador)", 1, 5, Location.of("Air altar ruins", 2985, 3293, 6),
				Location.of("Air altar", 2843, 4834, 20), Banks.FALADOR_EAST, "Air",
				ItemID.AIR_TALISMAN, ItemID.TIARA_AIR, ItemID.AIRRUNE)
				.tag(MethodTag.CHEAP)
				.notes("From level 11 every essence makes two air runes. Wear an air tiara to free the talisman slot.")
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
			Method.builder()
				.id("rc_gotr")
				.skill(Skill.RUNECRAFT)
				.type(MethodType.ACTIVITY)
				.name("Guardians of the Rift")
				.minLevel(27)
				.xpPerAction(60)
				.xpPerHour(45_000)
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
