package com.skillinghelper.quests;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * What the quest order guide says to do now. Compared by value so the panel only redraws on a change.
 */
@Value
@Builder
public class QuestStep
{
	public enum Kind
	{
		/**
		 * Start a quest (or its first part): go to the start, then hand over to Quest Helper.
		 */
		START,
		/**
		 * A quest already started: keep going with Quest Helper.
		 */
		CONTINUE,
		DIARY,
		ACTIVITY,
		/**
		 * Nothing on the route can be done right now: what to train.
		 */
		TRAIN,
		/**
		 * The whole route is done.
		 */
		FINISHED
	}

	Kind kind;
	/**
	 * The route row this step is for, or null.
	 */
	String rowKey;
	String title;
	@Singular
	List<String> lines;
	/**
	 * Where to draw the route to, or null.
	 */
	WorldPoint destination;
	/**
	 * NPC to outline near the start, or null.
	 */
	String npc;
	/**
	 * The quest's line in the in-game Quest list, outlined there, or null.
	 */
	String questListName;
	/**
	 * Wiki page for the "Open wiki guide" button.
	 */
	String wikiPage;
	/**
	 * Diary "REGION.TIER" for the "Show in Achievement Diary Guides" button, or null.
	 */
	String diary;
	/**
	 * Quest Helper is on but its "Auto start helper" setting is off: offer to turn it on.
	 */
	boolean offerAutoStart;
	/**
	 * Travel items to outline in the bank.
	 */
	@Singular("travelItem")
	List<Integer> travel;
}
