package com.skillinghelper.diary.data;

import com.skillinghelper.data.Location;
import java.util.List;
import lombok.Getter;
import net.runelite.api.gameval.VarbitID;

/**
 * The twelve Achievement Diary areas, with who hands out the rewards and the varbits that say each tier's reward
 * was claimed.
 */
@Getter
public enum DiaryRegion
{
	ARDOUGNE("Ardougne", "Ardougne_Diary", List.of("Two-pints"),
		Location.of("Flying Horse Inn, East Ardougne", 2574, 3323, 4),
		VarbitID.ARDOUGNE_EASY_REWARD, VarbitID.ARDOUGNE_MEDIUM_REWARD, VarbitID.ARDOUGNE_HARD_REWARD,
		VarbitID.ARDOUGNE_ELITE_REWARD),
	DESERT("Desert", "Desert_Diary", List.of("Jarr"),
		Location.of("Shantay Pass", 3302, 3122, 4),
		VarbitID.DESERT_EASY_REWARD, VarbitID.DESERT_MEDIUM_REWARD, VarbitID.DESERT_HARD_REWARD,
		VarbitID.DESERT_ELITE_REWARD),
	FALADOR("Falador", "Falador_Diary", List.of("Sir Rebral"),
		Location.of("White Knights' Castle courtyard", 2978, 3347, 4),
		VarbitID.FALADOR_EASY_REWARD, VarbitID.FALADOR_MEDIUM_REWARD, VarbitID.FALADOR_HARD_REWARD,
		VarbitID.FALADOR_ELITE_REWARD),
	FREMENNIK("Fremennik", "Fremennik_Diary", List.of("Thorodin"),
		Location.of("South of Rellekka", 2659, 3627, 4),
		VarbitID.FREMENNIK_EASY_REWARD, VarbitID.FREMENNIK_MEDIUM_REWARD, VarbitID.FREMENNIK_HARD_REWARD,
		VarbitID.FREMENNIK_ELITE_REWARD),
	KANDARIN("Kandarin", "Kandarin_Diary", List.of("The 'Wedge'"),
		Location.of("Camelot Castle", 2759, 3476, 4),
		VarbitID.KANDARIN_EASY_REWARD, VarbitID.KANDARIN_MEDIUM_REWARD, VarbitID.KANDARIN_HARD_REWARD,
		VarbitID.KANDARIN_ELITE_REWARD),
	KARAMJA("Karamja", "Karamja_Diary", List.of("Pirate Jackie the Fruit", "Kaleb Paramaya", "Jungle forester"),
		Location.of("Brimhaven Agility Arena", 2809, 3193, 4),
		VarbitID.ATJUN_EASY_REWARD, VarbitID.ATJUN_MED_REWARD, VarbitID.ATJUN_HARD_REWARD,
		VarbitID.KARAMJA_ELITE_REWARD),
	KOUREND("Kourend & Kebos", "Kourend_%26_Kebos_Diary", List.of("Elise"),
		Location.of("Kourend Castle garden", 1648, 3665, 4),
		VarbitID.KOUREND_EASY_REWARD, VarbitID.KOUREND_MEDIUM_REWARD, VarbitID.KOUREND_HARD_REWARD,
		VarbitID.KOUREND_ELITE_REWARD),
	LUMBRIDGE("Lumbridge & Draynor", "Lumbridge_%26_Draynor_Diary", List.of("Mikasi", "Hatius Cosaintus"),
		Location.of("East of Lumbridge Castle", 3234, 3214, 3),
		VarbitID.LUMBRIDGE_EASY_REWARD, VarbitID.LUMBRIDGE_MEDIUM_REWARD, VarbitID.LUMBRIDGE_HARD_REWARD,
		VarbitID.LUMBRIDGE_ELITE_REWARD),
	MORYTANIA("Morytania", "Morytania_Diary", List.of("Le-sabrè"),
		Location.of("Canifis pub", 3465, 3478, 4),
		VarbitID.MORYTANIA_EASY_REWARD, VarbitID.MORYTANIA_MEDIUM_REWARD, VarbitID.MORYTANIA_HARD_REWARD,
		VarbitID.MORYTANIA_ELITE_REWARD),
	VARROCK("Varrock", "Varrock_Diary", List.of("Toby"),
		Location.of("Behind Varrock General Store", 3225, 3415, 4),
		VarbitID.VARROCK_EASY_REWARD, VarbitID.VARROCK_MEDIUM_REWARD, VarbitID.VARROCK_HARD_REWARD,
		VarbitID.VARROCK_ELITE_REWARD),
	WESTERN("Western Provinces", "Western_Provinces_Diary", List.of("Elder Gnome child"),
		Location.of("Tree Gnome Stronghold", 2467, 3459, 4),
		VarbitID.WESTERN_EASY_REWARD, VarbitID.WESTERN_MEDIUM_REWARD, VarbitID.WESTERN_HARD_REWARD,
		VarbitID.WESTERN_ELITE_REWARD),
	WILDERNESS("Wilderness", "Wilderness_Diary", List.of("Lesser Fanatic"),
		Location.of("Edgeville", 3121, 3516, 4),
		VarbitID.WILDERNESS_EASY_REWARD, VarbitID.WILDERNESS_MEDIUM_REWARD, VarbitID.WILDERNESS_HARD_REWARD,
		VarbitID.WILDERNESS_ELITE_REWARD);

	private final String label;
	private final String wikiPage;
	/**
	 * Who hands out the rewards (outlined on the claim step).
	 */
	private final List<String> taskmasters;
	private final Location taskmasterSpot;
	private final int[] rewardVarbits;

	DiaryRegion(String label, String wikiPage, List<String> taskmasters, Location taskmasterSpot, int... rewardVarbits)
	{
		this.label = label;
		this.wikiPage = wikiPage;
		this.taskmasters = taskmasters;
		this.taskmasterSpot = taskmasterSpot;
		this.rewardVarbits = rewardVarbits;
	}

	public int rewardVarbit(DiaryTier tier)
	{
		return rewardVarbits[tier.ordinal()];
	}

	@Override
	public String toString()
	{
		return label;
	}
}
