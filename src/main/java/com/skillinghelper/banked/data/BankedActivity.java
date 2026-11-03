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
	 * Actions possible with what the player owns.
	 *
	 * @param buyMissing true to count only the banked item and buy whatever else runs short (runes, secondaries);
	 *                   false to stop when anything the activity uses runs out
	 */
	public int actions(PlayerState s, boolean buyMissing)
	{
		int actions = actions(s);
		if (buyMissing)
		{
			return actions;
		}
		for (ItemReq input : method.getInputs())
		{
			actions = Math.min(actions, actionsFrom(input, s));
		}
		return actions;
	}

	/**
	 * The input that runs out before the banked item does, or null when nothing else limits it.
	 */
	public ItemReq limitedBy(PlayerState s)
	{
		int banked = actions(s);
		ItemReq limit = null;
		int fewest = banked;
		for (ItemReq input : method.getInputs())
		{
			int n = actionsFrom(input, s);
			if (n < fewest)
			{
				fewest = n;
				limit = input;
			}
		}
		return limit;
	}

	/**
	 * How many actions an input's owned amount covers, counting what its parts can make (herbs and vials into
	 * unfinished potions) and treating a substitute (a fire staff for fire runes) as unlimited.
	 */
	private static int actionsFrom(ItemReq input, PlayerState s)
	{
		if (input.getPerAction() <= 0 || s.ownsSubstitute(input))
		{
			return Integer.MAX_VALUE;
		}
		long owned = s.owned(input.getIds());
		if (input.hasRecipe())
		{
			long makeable = Long.MAX_VALUE;
			for (ItemReq part : input.getRecipe())
			{
				if (part.getPerAction() > 0)
				{
					makeable = Math.min(makeable, s.owned(part.getIds()) / part.getPerAction());
				}
			}
			if (makeable != Long.MAX_VALUE)
			{
				owned += makeable;
			}
		}
		return (int) Math.min(Integer.MAX_VALUE, owned / input.getPerAction());
	}

	/**
	 * Xp for using up every banked item at the player's current level.
	 */
	public long xp(PlayerState s)
	{
		return xp(s, true);
	}

	public long xp(PlayerState s, boolean buyMissing)
	{
		return (long) Math.floor(actions(s, buyMissing) * method.xpPerAction(s.level(method.getSkill())));
	}
}
