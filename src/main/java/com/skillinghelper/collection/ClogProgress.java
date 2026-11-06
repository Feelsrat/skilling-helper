package com.skillinghelper.collection;

import java.util.Set;
import lombok.Value;

/**
 * What this plugin knows about the player's collection log.
 */
@Value
public class ClogProgress
{
	public enum State
	{
		/**
		 * Shown as obtained in the log, or announced by the game's "New item added" message.
		 */
		OBTAINED,
		/**
		 * Not seen in the log yet, but the player has one in the bank, inventory or worn. Almost always means the
		 * slot is filled (bought items are the exception), so it counts as obtained until the log page says no.
		 */
		OWNED,
		/**
		 * The log page showed this slot empty.
		 */
		MISSING,
		/**
		 * The player hasn't opened a log page with this slot yet.
		 */
		UNKNOWN
	}

	Set<Integer> obtained;
	Set<Integer> missing;
	Set<Integer> owned;

	public State state(int itemId)
	{
		if (obtained.contains(itemId))
		{
			return State.OBTAINED;
		}
		if (missing.contains(itemId))
		{
			return State.MISSING;
		}
		if (owned.contains(itemId))
		{
			return State.OWNED;
		}
		return State.UNKNOWN;
	}

	public boolean has(int itemId)
	{
		State s = state(itemId);
		return s == State.OBTAINED || s == State.OWNED;
	}
}
