package com.skillinghelper.collection.data;

import com.skillinghelper.data.Wilderness;
import java.util.List;
import java.util.Set;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * One page of the collection log, e.g. "Giant Mole" or "Easy Treasure Trails", with its slots in the game's order.
 */
@Value
public class ClogEntry
{
	/**
	 * Wilderness content whose wiki page has no map spot, or whose spot is the way in rather than the place itself.
	 */
	private static final Set<String> WILDERNESS = Set.of("Callisto and Artio", "Chaos Elemental", "Chaos Fanatic",
		"Crazy archaeologist", "Scorpia", "Venenatis and Spindel", "Vet'ion and Calvar'ion", "Revenants",
		"Elder Chaos Druids");

	ClogTab tab;
	String name;
	String wikiPage;
	/**
	 * Where to go (from the wiki's map of the page), or null when the wiki has none on the real map (instances,
	 * things done anywhere such as clue scrolls).
	 */
	WorldPoint location;
	/**
	 * Slayer level the monster needs, 0 for none.
	 */
	int slayerLevel;
	/**
	 * The wiki page's first sentence: what this is, for players who have never heard of it.
	 */
	String intro;
	List<ClogItem> items;

	public boolean isWilderness()
	{
		return WILDERNESS.contains(name) || location != null && Wilderness.contains(location);
	}

	public boolean isPets()
	{
		return "All Pets".equals(name) || "Skilling Pets".equals(name);
	}

	/**
	 * The name of the monster or boss to outline, for boss pages: "Callisto and Artio" -> Callisto, Artio.
	 */
	public List<String> npcNames()
	{
		if (tab != ClogTab.BOSSES)
		{
			return List.of();
		}
		switch (name)
		{
			case "Dagannoth Kings":
				return List.of("Dagannoth Rex", "Dagannoth Prime", "Dagannoth Supreme");
			case "Barrows Chests":
			case "The Fight Caves":
			case "The Inferno":
			case "The Gauntlet":
			case "Fortis Colosseum":
			case "Moons of Peril":
			case "Royal Titans":
			case "Tempoross":
			case "Wintertodt":
			case "Zalcano":
				return List.of();
			case "Grotesque Guardians":
				return List.of("Dusk", "Dawn");
			default:
				String n = name.startsWith("The ") ? name.substring(4) : name;
				return List.of(n.split(" and "));
		}
	}
}
