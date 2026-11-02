package com.skillinghelper.engine;

import com.skillinghelper.data.CombatGear;
import com.skillinghelper.data.CombatSpell;
import com.skillinghelper.data.ItemReq;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Skill;

/**
 * Picks combat gear for the player's levels: which ranged weapon they'll use and the ammo it fires, and the runes
 * of the fire spell they'll autocast.
 */
final class CombatGearAdvisor
{
	/**
	 * The ranged weapon the player will train with: the best one they own and can wield, else the best one they can
	 * buy. Null if they can't wield any.
	 */
	static CombatGear.Tier rangedWeapon(PlayerState s)
	{
		CombatGear.Tier best = null;
		for (CombatGear.Tier t : CombatGear.RANGED_WEAPON.getTiers())
		{
			if (!canWear(t, s))
			{
				continue;
			}
			if (s.owned(t.getIds()) > 0)
			{
				return t;
			}
			if (best == null)
			{
				best = t;
			}
		}
		return best;
	}

	/**
	 * The best ammo the player can fire from the weapon they'll use, or null.
	 */
	static CombatGear.Tier bestAmmo(PlayerState s)
	{
		CombatGear.Tier weapon = rangedWeapon(s);
		if (weapon == null)
		{
			return null;
		}
		for (CombatGear.Tier t : CombatGear.RANGED_AMMO.getTiers())
		{
			if (t.firedFrom(weapon) && canWear(t, s))
			{
				return t;
			}
		}
		return null;
	}

	/**
	 * Any input but the generic ammo one is returned as it is. The ammo input is named after what the player should use: the kind they fire if they already have some,
	 * otherwise the exact ammo to buy, which is also what its price comes from.
	 */
	static ItemReq resolve(ItemReq input, PlayerState s)
	{
		if (input == CombatSpell.CATALYST_INPUT || input == CombatSpell.AIR_INPUT || input == CombatSpell.FIRE_INPUT)
		{
			return spellRunes(input, s);
		}
		if (input != CombatGear.AMMO_INPUT)
		{
			return input;
		}
		CombatGear.Tier best = bestAmmo(s);
		if (best == null)
		{
			return input;
		}
		List<Integer> ids = new ArrayList<>();
		for (int id : best.getIds())
		{
			ids.add(id);
		}
		for (CombatGear.Tier t : CombatGear.RANGED_AMMO.getTiers())
		{
			if (t.getAmmo() == best.getAmmo() && t != best)
			{
				for (int id : t.getIds())
				{
					ids.add(id);
				}
			}
		}
		int[] kindIds = ids.stream().mapToInt(Integer::intValue).toArray();
		if (best.getAmmo() != CombatGear.Ammo.DARTS)
		{
			input = input.withSubstituteName(null).withSubstituteIds(null);
		}
		boolean hasSome = s.owned(kindIds) > 0;
		String name = hasSome ? best.getAmmo().name().charAt(0) + best.getAmmo().name().substring(1).toLowerCase()
			: best.getName();
		return input.withName(name).withIds(kindIds);
	}

	/**
	 * The runes of the best fire spell the player can cast. The catalyst is named after the spell, so the player
	 * knows which one to autocast.
	 */
	private static ItemReq spellRunes(ItemReq input, PlayerState s)
	{
		CombatSpell spell = CombatSpell.best(s.level(Skill.MAGIC), s.isMembersWorld());
		if (spell == null)
		{
			return input;
		}
		if (input == CombatSpell.CATALYST_INPUT)
		{
			return input.withName(spell.getCatalystName() + " (" + spell.getSpellName() + ")")
				.withIds(new int[]{spell.getCatalyst()});
		}
		return input.withPerAction(input == CombatSpell.AIR_INPUT ? spell.getAir() : spell.getFire());
	}

	static boolean canWear(CombatGear.Tier t, PlayerState s)
	{
		return (t.getSkill() == null || s.level(t.getSkill()) >= t.getLevel())
			&& s.level(Skill.DEFENCE) >= t.getDefenceLevel()
			&& s.level(Skill.ATTACK) >= t.getAttackLevel()
			&& (!t.isMembers() || s.isMembersWorld());
	}

	private CombatGearAdvisor()
	{
	}
}
