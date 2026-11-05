package com.skillinghelper.diary.data;

import com.skillinghelper.data.Location;
import java.util.List;
import net.runelite.api.gameval.ItemID;

/**
 * Task parts that come up in several diaries, written once.
 */
final class DiaryStages
{
	static final int[] ESSENCE = {ItemID.BLANKRUNE_DAEYALT, ItemID.BLANKRUNE_HIGH, ItemID.BLANKRUNE};

	private DiaryStages()
	{
	}

	/**
	 * Enter a rune altar through its ruins, then craft.
	 *
	 * @param where    how to find the ruins, e.g. "north-east of Varrock"
	 * @param ruins    the ruins outside
	 * @param altar    the altar room
	 * @param talisman the talisman to use on the ruins
	 */
	static List<DiaryStage> runeAltar(String where, Location ruins, Zone altar, String title, int talisman, String extra)
	{
		DiaryStage.DiaryStageBuilder inside = DiaryStage.builder()
			.title(title)
			.inside(altar)
			.object("Altar")
			.line("1. Click Craft-rune on the Altar (outlined) with the essence in your inventory.");
		if (extra != null)
		{
			inside.line(extra);
		}
		inside.line("The portal behind you takes you back out.");
		return List.of(
			DiaryStage.builder()
				.title("Enter the altar")
				.at(ruins)
				.object("Mysterious ruins")
				.use(talisman).use(ItemID.ELEMENTAL_TALISMAN)
				.line("1. The ruins are " + where + ".")
				.line("2. Wear the tiara and click Enter on the Mysterious ruins (outlined),")
				.line("3. or use the talisman (outlined) on the ruins.")
				.build(),
			inside.build());
	}

	/**
	 * Climb down the Edgeville trapdoor into the Edgeville (Varrock) dungeon.
	 */
	static DiaryStage edgevilleTrapdoor()
	{
		return DiaryStage.builder()
			.title("Climb down into Edgeville Dungeon")
			.at(Location.of("Trapdoor in Edgeville", 3097, 3468, 0, 2))
			.object("Trapdoor")
			.line("1. The trapdoor is in the ruined building north of Edgeville bank, west of the church.")
			.line("2. Click Open on the Trapdoor (outlined), then Climb-down.")
			.line("Warning: there are aggressive monsters down there at low levels; bring food.")
			.build();
	}

	static final Zone EDGEVILLE_DUNGEON = Zone.of(3067, 9821, 3288, 10000, 0);

	/**
	 * Stronghold of Security: the floors, top to bottom.
	 */
	static final Zone STRONGHOLD_1 = Zone.of(1854, 5183, 1918, 5248, 0);
	static final Zone STRONGHOLD_2 = Zone.of(1983, 5183, 2049, 5248, 0);
	static final Zone STRONGHOLD_3 = Zone.of(2113, 5246, 2177, 5313, 0);
	static final Zone STRONGHOLD_4 = Zone.of(2302, 5183, 2369, 5250, 0);

	static DiaryStage strongholdEntrance()
	{
		return DiaryStage.builder()
			.title("Climb into the Stronghold of Security")
			.at(Location.of("Stronghold of Security, Barbarian Village", 3081, 3420, 0, 2))
			.object("Entrance")
			.line("1. The entrance is a hole in the middle of Barbarian Village, west of Varrock.")
			.line("2. Click Climb-down on the Entrance (outlined).")
			.line("Inside, every door asks a security question. The safe answer is always the one that keeps your "
				+ "password and bank PIN secret and doesn't trust anyone asking for them.")
			.build();
	}
}
