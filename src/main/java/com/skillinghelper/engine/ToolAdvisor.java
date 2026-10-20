package com.skillinghelper.engine;

import com.skillinghelper.data.ToolType;
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * Works out the best tool the player can use and the best one they already own.
 */
public final class ToolAdvisor
{
	public enum Where
	{
		EQUIPPED,
		INVENTORY,
		BANK
	}

	@Value
	public static class OwnedTool
	{
		ToolType.Tier tier;
		int itemId;
		Where where;
	}

	/**
	 * Best tier the player's level allows, whether or not they own it.
	 */
	public static ToolType.Tier bestUsable(ToolType type, PlayerState state)
	{
		int level = state.level(type.getSkill());
		for (ToolType.Tier tier : type.getTiers())
		{
			if (tier.getLevel() <= level)
			{
				return tier;
			}
		}
		return type.getTiers().get(type.getTiers().size() - 1);
	}

	/**
	 * Best tier the player owns and can use, preferring the copy that is easiest to reach.
	 */
	@Nullable
	public static OwnedTool bestOwned(ToolType type, PlayerState state)
	{
		int level = state.level(type.getSkill());
		for (ToolType.Tier tier : type.getTiers())
		{
			if (tier.getLevel() > level)
			{
				continue;
			}
			for (int id : tier.getIds())
			{
				if (state.getEquipment().has(id))
				{
					return new OwnedTool(tier, id, Where.EQUIPPED);
				}
			}
			for (int id : tier.getIds())
			{
				if (state.getInventory().has(id))
				{
					return new OwnedTool(tier, id, Where.INVENTORY);
				}
			}
			for (int id : tier.getIds())
			{
				if (state.bankCount(id) > 0)
				{
					return new OwnedTool(tier, id, Where.BANK);
				}
			}
		}
		return null;
	}

	/**
	 * Best usable tier the player is carrying (inventory or worn), or null.
	 */
	@Nullable
	public static OwnedTool bestCarried(ToolType type, PlayerState state)
	{
		int level = state.level(type.getSkill());
		for (ToolType.Tier tier : type.getTiers())
		{
			if (tier.getLevel() > level)
			{
				continue;
			}
			for (int id : tier.getIds())
			{
				if (state.getEquipment().has(id))
				{
					return new OwnedTool(tier, id, Where.EQUIPPED);
				}
				if (state.getInventory().has(id))
				{
					return new OwnedTool(tier, id, Where.INVENTORY);
				}
			}
		}
		return null;
	}

	/**
	 * Whether the player can wield the tool to free up an inventory slot.
	 */
	public static boolean canWield(ToolType.Tier tier, PlayerState state)
	{
		return tier.getWieldLevel() > 0 && state.level(Skill.ATTACK) >= tier.getWieldLevel();
	}

	private ToolAdvisor()
	{
	}
}
