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
			salvage("sail_salvage_small", "Small shipwrecks", 15, 18_000,
				3110, 2948, 1589, 3323)
				.build(),
			salvage("sail_salvage_fishy", "Fishy shipwrecks", 26, 28_000,
				2782, 3319, 2698, 3047, 1903, 3835)
				.build(),
			salvage("sail_salvage_barracuda", "Barracuda shipwrecks", 35, 38_000,
				2996, 2917, 3123, 2808, 2447, 3021, 2478, 2823, 2142, 3350, 2222, 3444)
				.build(),
			salvage("sail_salvage_large", "Large shipwrecks", 53, 55_000,
				1563, 2679, 1886, 2508, 2459, 2144, 2845, 4030, 3394, 2138)
				.build()
		);
	}

	/**
	 * @param spots x, y pairs of every wreck site of this type, from RuneLite's world map (SalvagingSpotLocation)
	 */
	private static Method.MethodBuilder salvage(String id, String name, int level, int xpHr, int... spots)
	{
		Method.MethodBuilder b = Method.builder();
		for (int i = 2; i + 1 < spots.length; i += 2)
		{
			b.alsoAt(Location.sea(name, spots[i], spots[i + 1], 25));
		}
		return b
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
			.location(Location.sea(name, spots[0], spots[1], 25))
			.target(Target.object(null, "Shipwreck", "Salvaging hook", "Salvaging station"))
			.instruction("Sail to the " + name.toLowerCase() + ", deploy your salvaging hook and sort the salvage "
				+ "at your salvaging station")
			.notes("Fit a salvaging hook to your boat first. The path leads to the nearest wreck site of this type.")
			.wikiPage("Salvaging");
	}

	private SailingGuide()
	{
	}
}
