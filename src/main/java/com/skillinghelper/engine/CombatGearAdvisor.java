package com.skillinghelper.engine;

import com.skillinghelper.data.CombatGear;
import com.skillinghelper.data.CombatSpell;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import net.runelite.api.Skill;

/**
 * Picks combat gear for the player's levels: which ranged weapon they'll use and the ammo it fires, and the runes
 * of the fire spell they'll autocast.
 */
final class CombatGearAdvisor
{
	/**
	 * The ranged weapon the player will train with: the best one they own and can wield, else the best one they can
	 * buy that isn't {@link CombatGear.Tier#isPricey() pricey}. Null if they can't wield any.
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
			if (best == null && t.isBuyable() && !t.isPricey())
			{
				best = t;
			}
		}
		return best;
	}

	/**
	 * Whether the ranged weapon the player will use leaves a hand free for an offhand (crossbows do, bows and the
	 * blowpipe don't).
	 */
	static boolean rangedOffhand(PlayerState s)
	{
		CombatGear.Tier weapon = rangedWeapon(s);
		return weapon != null && !weapon.isTwoHanded();
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
	 * Spell rune inputs become the runes of the player's best spell, or null when that spell doesn't use one.
	 * Any other input but the generic ammo one is returned as it is. The ammo input is named after what the player
	 * should use: the best ammo they own and can fire, otherwise the best they can buy. Lower tiers they also own are
	 * left out. It is null when the weapon needs no ammo.
	 */
	static ItemReq resolve(ItemReq input, PlayerState s)
	{
		if (input == CombatSpell.CATALYST_INPUT || input == CombatSpell.AIR_INPUT || input == CombatSpell.ELEMENT_INPUT)
		{
			return spellRunes(input, s);
		}
		if (input == CombatGear.CHINCHOMPA_INPUT)
		{
			return chinchompas(input, s);
		}
		if (input != CombatGear.AMMO_INPUT)
		{
			return input;
		}
		CombatGear.Tier weapon = rangedWeapon(s);
		if (weapon != null && weapon.getAmmo() == CombatGear.Ammo.NONE)
		{
			// The bow of faerdhinen makes its own arrows
			return null;
		}
		CombatGear.Tier best = bestOwnedAmmo(s);
		if (best == null)
		{
			best = bestAmmo(s);
		}
		if (best == null)
		{
			return input;
		}
		// Only the one best tier: lower ammo the player also owns isn't suggested
		if (best.getAmmo() != CombatGear.Ammo.DARTS)
		{
			input = input.withSubstituteName(null).withSubstituteIds(null);
		}
		return input.withName(best.getName()).withIds(best.getIds().clone());
	}

	/**
	 * The best chinchompas the player owns and can throw, else the best they can buy for their Ranged level.
	 */
	private static ItemReq chinchompas(ItemReq input, PlayerState s)
	{
		int ranged = s.level(Skill.RANGED);
		int pick = -1;
		for (int i = 0; i < CombatGear.CHINCHOMPA_IDS.length; i++)
		{
			if (ranged < CombatGear.CHINCHOMPA_LEVELS[i])
			{
				continue;
			}
			if (s.owned(CombatGear.CHINCHOMPA_IDS[i]) > 0)
			{
				pick = i;
				break;
			}
			if (pick < 0)
			{
				pick = i;
			}
		}
		if (pick < 0)
		{
			return input;
		}
		return input.withName(CombatGear.CHINCHOMPA_NAMES[pick]).withIds(new int[]{CombatGear.CHINCHOMPA_IDS[pick]});
	}

	/**
	 * Whether the method throws chinchompas, which take the weapon slot, so no bow, ammo or offhand is suggested.
	 */
	static boolean chinning(Method m)
	{
		return m.getInputs().contains(CombatGear.CHINCHOMPA_INPUT);
	}

	/**
	 * The best ammo the player owns and can fire from the weapon they'll use, or null.
	 */
	static CombatGear.Tier bestOwnedAmmo(PlayerState s)
	{
		CombatGear.Tier weapon = rangedWeapon(s);
		if (weapon == null)
		{
			return null;
		}
		for (CombatGear.Tier t : CombatGear.RANGED_AMMO.getTiers())
		{
			if (t.firedFrom(weapon) && canWear(t, s) && s.owned(t.getIds()) > 0)
			{
				return t;
			}
		}
		return null;
	}

	/**
	 * The runes of the best combat spell the player can cast, or null when this spell doesn't use the input (wind
	 * spells have no second element). The catalyst is named after the spell, so the player knows which one to
	 * autocast.
	 */
	private static ItemReq spellRunes(ItemReq input, PlayerState s)
	{
		CombatSpell spell = CombatSpell.best(s.level(Skill.MAGIC), s.isMembersWorld());
		if (input == CombatSpell.CATALYST_INPUT)
		{
			return input.withName(spell.getCatalyst().getRuneName() + " (" + spell.getSpellName() + ")")
				.withIds(new int[]{spell.getCatalyst().getRune()});
		}
		if (input == CombatSpell.AIR_INPUT)
		{
			return input.withPerAction(spell.getAir());
		}
		CombatSpell.Element element = spell.getElement();
		if (element == null)
		{
			return null;
		}
		return input.withName(element.getRuneName())
			.withIds(new int[]{element.getRune()})
			.withPerAction(spell.getElementRunes())
			.withSubstituteName(element.getStaffName())
			.withSubstituteIds(element.getStaves());
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
