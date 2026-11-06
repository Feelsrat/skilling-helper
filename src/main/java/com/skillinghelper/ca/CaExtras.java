package com.skillinghelper.ca;

import com.skillinghelper.diary.data.DiaryItem;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/**
 * What single tasks need beyond their boss: a special weapon or a setup the plugin can check or should mention.
 */
final class CaExtras
{
	private static final DiaryItem DEMONBANE = DiaryItem.owned("Demonbane weapon (Silverlight, Darklight, Arclight or "
		+ "Emberlight)", "Silverlight is the reward of the Demon Slayer quest.", ItemID.EMBERLIGHT, ItemID.ARCLIGHT,
		ItemID.DARKLIGHT, ItemID.SILVERLIGHT);

	private static final DiaryItem RATBANE = DiaryItem.owned("Ratbane weapon (bone mace, bone shortbow or bone staff)",
		"Made from Scurrius' spine, which Scurrius drops.", ItemID.RAT_BONE_MACE, ItemID.RAT_BONE_BOW,
		ItemID.RAT_BONE_STAFF);

	private static final Map<Integer, List<DiaryItem>> ITEMS = Map.of(
		// Not So Great After All: finish off a greater demon with a demonbane weapon
		121, List.of(DEMONBANE),
		// Sit Rat: finish off Scurrius with a ratbane weapon
		523, List.of(RATBANE)
	);

	private static final Map<Integer, List<String>> NOTES = Map.of(
		// Cosy: four pieces of warm clothing
		285, List.of("Four pieces of warm clothing (e.g. Pyromancer outfit, Santa hat, any woolly or fur clothing)")
	);

	private CaExtras()
	{
	}

	static List<DiaryItem> items(int taskId)
	{
		return ITEMS.getOrDefault(taskId, List.of());
	}

	static List<String> notes(int taskId)
	{
		return NOTES.getOrDefault(taskId, List.of());
	}
}
