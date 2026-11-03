package com.skillinghelper.shops;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import java.util.List;
import lombok.Value;

/**
 * An NPC shop that anyone can walk into: no quest, guild level or minigame needed.
 */
@Value
public class Shop
{
	String name;
	/**
	 * Who to click to trade, e.g. "Shop keeper" and "Shop assistant".
	 */
	List<String> npcs;
	Location location;
	boolean members;

	/**
	 * The shopkeepers to outline.
	 */
	public Target target()
	{
		return Target.npc("Trade", npcs.toArray(new String[0]));
	}

	/**
	 * "Aubury" or "the Shop keeper or Shop assistant", for step text.
	 */
	public String npcNames()
	{
		return String.join(" or ", npcs);
	}
}
