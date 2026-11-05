package com.skillinghelper.ui;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * A ranked list of guides for one of the dropdown's quick picks (Best gold/hr, Most AFK...), so the player chooses
 * which one to open.
 */
@Value
@Builder
public class PickList
{
	SkillingHelperPanel.QuickPick pick;
	/**
	 * What the list is ranked by, e.g. "Most gold per hour first".
	 */
	String note;
	/**
	 * Shown when nothing qualifies.
	 */
	String emptyText;
	@Singular
	List<Row> rows;

	/**
	 * One guide in the list.
	 */
	@Value
	@Builder
	public static class Row
	{
		Skill skill;
		/**
		 * The headline, usually the method's name.
		 */
		String title;
		/**
		 * The number it's ranked by, e.g. "~1.2M gp/hr".
		 */
		String value;
		/**
		 * Smaller line under it, e.g. the route or the level range.
		 */
		String detail;
		/**
		 * Method to choose for the skill when opened, or null to leave the choice as it is.
		 */
		String methodId;
		/**
		 * Let the guide choose the method again, for routes that switch method on the way.
		 */
		boolean clearForced;
		/**
		 * Goal to set when opened, or 0 to keep the saved one.
		 */
		int target;
	}
}
