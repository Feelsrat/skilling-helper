package com.skillinghelper.data.guides;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.MethodTag;
import com.skillinghelper.data.MethodType;
import com.skillinghelper.data.Target;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Based on the OSRS wiki "Sailing training" guide. Sailing is new, so rates are rough and the guide sticks to text and
 * highlights. Shipwreck and port locations match RuneLite's world map.
 */
final class SailingGuide
{
	private static final String WIKI = "Sailing_training";

	static List<Method> methods()
	{
		return List.of(
			Method.builder()
				.id("sail_pandemonium")
				.skill(Skill.SAILING)
				.type(MethodType.ACTIVITY)
				.name("Pandemonium (quest) and port tasks")
				.minLevel(1)
				.xpPerAction(150)
				.xpPerHour(8_000)
				.members(true)
				.tag(MethodTag.CHEAP)
				.location(Location.of("Port Sarim docks", 3050, 3192, 12))
				.target(Target.object(null, "Gangplank", "Helm", "Sails", "Noticeboard"))
				.instruction("Start Pandemonium at Port Sarim to get your first boat, then take courier and bounty "
					+ "tasks from the port noticeboards")
				.notes("Port tasks give steady early xp and unlock new ports as you level.")
				.wikiPage(WIKI)
				.build(),
			salvage("sail_salvage_small", "Small shipwrecks", 15, 3110, 2948, 18_000)
				.build(),
			salvage("sail_salvage_fishy", "Fishy shipwrecks", 26, 2782, 3319, 28_000)
				.build(),
			salvage("sail_salvage_barracuda", "Barracuda shipwrecks", 35, 2996, 2917, 38_000)
				.build(),
			salvage("sail_salvage_large", "Large shipwrecks", 53, 1563, 2679, 55_000)
				.build()
		);
	}

	private static Method.MethodBuilder salvage(String id, String name, int level, int x, int y, int xpHr)
	{
		return Method.builder()
			.id(id)
			.skill(Skill.SAILING)
			.type(MethodType.ACTIVITY)
			.name("Salvage " + name.toLowerCase())
			.minLevel(level)
			.xpPerAction(xpHr / 400.0)
			.xpPerHour(xpHr)
			.members(true)
			.tag(MethodTag.AFK).tag(MethodTag.PROFIT)
			.quest(Quest.PANDEMONIUM)
			.location(Location.of(name, x, y, 25))
			.target(Target.object(null, "Shipwreck", "Salvaging hook", "Salvaging station"))
			.instruction("Sail to the " + name.toLowerCase() + ", deploy your salvaging hook and sort the salvage "
				+ "at your salvaging station")
			.notes("Fit a salvaging hook to your boat first. There are more wrecks of this type around the sea; "
				+ "open the world map with Sailing markers on to find them.")
			.wikiPage("Salvaging");
	}

	private SailingGuide()
	{
	}
}
