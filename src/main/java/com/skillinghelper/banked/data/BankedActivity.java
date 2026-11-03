package com.skillinghelper.banked.data;

import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.engine.PlayerState;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * One way to turn an item in the bank into xp, e.g. "Make prayer potions" for ranarr potions (unf) and clean ranarr.
 * The {@link #method} is an ordinary guide method, so the step engine handles the bank trips, walking, highlights and
 * buying any missing secondaries; this adds which items count as banked and how the place is picked.
 */
@Value
@Builder(toBuilder = true)
public class BankedActivity
{
	/**
	 * Where the activity happens.
	 */
	public enum Place
	{
		/**
		 * The method's own location and bank.
		 */
		FIXED,
		/**
		 * At whichever bank is closest, e.g. cleaning herbs or burying bones.
		 */
		ANY_BANK,
		/**
		 * A range next to a bank: Catherby on members' worlds, Al Kharid on free ones.
		 */
		RANGE,
	}

	@NonNull
	Method method;
	@NonNull
	Place place;
	/**
	 * Groups activities that use up the same bank item, e.g. burning or fletching yew logs. The player picks one
	 * activity per group.
	 */
	@NonNull
	String group;
	/**
	 * Name of the banked item, e.g. "Yew logs".
	 */
	@NonNull
	String itemName;
	/**
	 * Item shown for the group in the panel.
	 */
	int iconId;
	/**
	 * Items that count towards the banked amount, e.g. both the unfinished potions and the clean herbs that make them.
	 */
	@NonNull
	int[] countIds;
	/**
	 * How many of {@link #countIds} one action uses.
	 */
	int perAction;
	/**
	 * Order in a run: activities that make the items of a later one go first (clean herbs before potions, smelt bars
	 * before smithing, cut bows before stringing).
	 */
	int stage;

	public String getId()
	{
		return method.getId();
	}

	public Skill getSkill()
	{
		return method.getSkill();
	}

	public int getLevel()
	{
		return method.getMinLevel();
	}

	public String getName()
	{
		return method.getName();
	}

	/**
	 * The item the activity is about: the method's first input.
	 */
	public ItemReq keyInput()
	{
		return method.getInputs().get(0);
	}

	/**
	 * How many banked items (in the bank or carried) this activity can use right now.
	 */
	public int itemCount(PlayerState s)
	{
		return s.owned(countIds);
	}

	/**
	 * Actions the banked items allow, ignoring secondaries the guide can buy.
	 */
	public int actions(PlayerState s)
	{
		return itemCount(s) / Math.max(1, perAction);
	}

	/**
	 * Xp for using up every banked item at the player's current level.
	 */
	public long xp(PlayerState s)
	{
		return (long) Math.floor(actions(s) * method.xpPerAction(s.level(method.getSkill())));
	}
}
