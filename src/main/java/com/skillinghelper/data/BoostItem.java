package com.skillinghelper.data;

import java.util.Set;
import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Tradeable items that help a skill and can be bought on the GE. Always optional: the guide shows the current price so
 * the player can decide, and highlights the item to wear once they own it.
 */
@Getter
public enum BoostItem
{
	DODGY_NECKLACE(Skill.THIEVING, "Dodgy necklace", "25% chance to avoid being stunned when pickpocketing",
		Set.of("thieve_men", "thieve_master_farmer", "thieve_ardougne_knights"), ItemID.DODGY_NECKLACE),
	RING_OF_FORGING(Skill.SMITHING, "Ring of forging", "Every iron ore smelts into a bar instead of half",
		Set.of("smith_iron_bars"), ItemID.RING_OF_FORGING),
	AMULET_OF_CHEMISTRY(Skill.HERBLORE, "Amulet of chemistry", "5% chance of a 4-dose potion, worth more on the GE",
		Set.of(), ItemID.AMULET_OF_CHEMISTRY);

	private final Skill skill;
	private final String name;
	private final String bonus;
	/**
	 * Methods the item helps with. Empty means every method of the skill.
	 */
	private final Set<String> methodIds;
	private final int[] ids;

	BoostItem(Skill skill, String name, String bonus, Set<String> methodIds, int... ids)
	{
		this.skill = skill;
		this.name = name;
		this.bonus = bonus;
		this.methodIds = methodIds;
		this.ids = ids;
	}

	public boolean appliesTo(Method m)
	{
		return m.getSkill() == skill && (methodIds.isEmpty() || methodIds.contains(m.getId()));
	}
}
