package com.skillinghelper.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Tool families with tiers ordered best first. Used to recommend the best tool the player can use and to
 * highlight the best one they own.
 */
@Getter
public enum ToolType
{
	AXE("axe", Skill.WOODCUTTING,
		tier("Crystal axe", 71, 70, ItemID.CRYSTAL_AXE),
		tier("Infernal axe", 61, 60, ItemID.INFERNAL_AXE),
		tier("Dragon axe", 61, 60, ItemID.DRAGON_AXE, ItemID.TRAILBLAZER_AXE_NO_INFERNAL),
		tier("Rune axe", 41, 40, ItemID.RUNE_AXE, ItemID.TRAIL_GILDED_AXE),
		tier("Adamant axe", 31, 30, ItemID.ADAMANT_AXE),
		tier("Mithril axe", 21, 20, ItemID.MITHRIL_AXE),
		tier("Black axe", 11, 10, ItemID.BLACK_AXE),
		tier("Steel axe", 6, 5, ItemID.STEEL_AXE),
		tier("Iron axe", 1, 1, ItemID.IRON_AXE),
		tier("Bronze axe", 1, 1, ItemID.BRONZE_AXE)),
	PICKAXE("pickaxe", Skill.MINING,
		tier("Crystal pickaxe", 71, 70, ItemID.CRYSTAL_PICKAXE),
		tier("Infernal pickaxe", 61, 60, ItemID.INFERNAL_PICKAXE),
		tier("Dragon pickaxe", 61, 60, ItemID.DRAGON_PICKAXE),
		tier("Rune pickaxe", 41, 40, ItemID.RUNE_PICKAXE, ItemID.TRAIL_GILDED_PICKAXE),
		tier("Adamant pickaxe", 31, 30, ItemID.ADAMANT_PICKAXE),
		tier("Mithril pickaxe", 21, 20, ItemID.MITHRIL_PICKAXE),
		tier("Black pickaxe", 11, 10, ItemID.BLACK_PICKAXE),
		tier("Steel pickaxe", 6, 5, ItemID.STEEL_PICKAXE),
		tier("Iron pickaxe", 1, 1, ItemID.IRON_PICKAXE),
		tier("Bronze pickaxe", 1, 1, ItemID.BRONZE_PICKAXE)),
	HARPOON("harpoon", Skill.FISHING,
		tier("Crystal harpoon", 71, 0, ItemID.CRYSTAL_HARPOON),
		tier("Infernal harpoon", 75, 0, ItemID.INFERNAL_HARPOON),
		tier("Dragon harpoon", 61, 0, ItemID.DRAGON_HARPOON),
		tier("Harpoon", 1, 0, ItemID.HARPOON));

	/**
	 * One tier of a tool. {@code wieldLevel} is the Attack level needed to wield it (0 if it can't be wielded).
	 */
	@Value
	public static class Tier
	{
		String name;
		int level;
		int wieldLevel;
		int[] ids;

		public boolean matches(int itemId)
		{
			for (int id : ids)
			{
				if (id == itemId)
				{
					return true;
				}
			}
			return false;
		}
	}

	private final String noun;
	private final Skill skill;
	private final List<Tier> tiers;

	ToolType(String noun, Skill skill, Tier... tiers)
	{
		this.noun = noun;
		this.skill = skill;
		this.tiers = Collections.unmodifiableList(Arrays.asList(tiers));
	}

	private static Tier tier(String name, int level, int wieldLevel, int... ids)
	{
		return new Tier(name, level, wieldLevel, ids);
	}

	/**
	 * All item ids of every tier, used to recognise any tool of this type.
	 */
	public int[] allIds()
	{
		return tiers.stream().flatMapToInt(t -> Arrays.stream(t.getIds())).toArray();
	}
}
