package com.skillinghelper.collection.data;

import lombok.Value;
import net.runelite.api.Skill;

/**
 * A full click-by-click guide another plugin in this jar has for a collection log page.
 */
@Value
public class GuideLink
{
	public enum Kind
	{
		/**
		 * A PvM Progression boss or raid guide, by ladder id.
		 */
		PVM("PvM Progression"),
		/**
		 * One Skilling Helper method, by method id.
		 */
		METHOD("Skilling Helper"),
		/**
		 * Skilling Helper's guide for a whole skill (skilling pets come from training it).
		 */
		SKILL("Skilling Helper");

		private final String plugin;

		Kind(String plugin)
		{
			this.plugin = plugin;
		}

		public String getPlugin()
		{
			return plugin;
		}
	}

	Kind kind;
	/**
	 * PvM ladder id or Skilling Helper method id; null for {@link Kind#SKILL}.
	 */
	String target;
	Skill skill;

	public static GuideLink pvm(String id)
	{
		return new GuideLink(Kind.PVM, id, null);
	}

	public static GuideLink method(String id)
	{
		return new GuideLink(Kind.METHOD, id, null);
	}

	public static GuideLink skill(Skill skill)
	{
		return new GuideLink(Kind.SKILL, null, skill);
	}
}
