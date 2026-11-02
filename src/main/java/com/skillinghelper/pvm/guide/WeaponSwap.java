package com.skillinghelper.pvm.guide;

import com.skillinghelper.engine.ItemHighlight;
import lombok.Value;

/**
 * Which of the carried weapons to hold right now, and why.
 */
@Value
public class WeaponSwap
{
	/**
	 * The weapons that count, best first.
	 */
	GearSlot slot;
	/**
	 * Why, e.g. "Dharok is weak to Magic". Shown when the player isn't holding one yet.
	 */
	String reason;
	/**
	 * Extra line shown once it's in hand, e.g. which spell to autocast; may be null.
	 */
	String note;

	/**
	 * Adds the line and outlines the weapon in the inventory when the player isn't holding one of {@link #slot}.
	 */
	void apply(PvmStep.PvmStepBuilder b, GuideState s)
	{
		for (Gear g : slot.getOptions())
		{
			if (s.worn(g.getIds()) > 0)
			{
				if (note != null)
				{
					b.line(note);
				}
				return;
			}
		}
		for (Gear g : slot.getOptions())
		{
			for (int id : g.getIds())
			{
				if (s.inventory(id) > 0)
				{
					b.line("➜ Wield your " + g.getName() + " (outlined): " + reason + ".");
					b.itemHighlight(id, ItemHighlight.EQUIP);
					if (note != null)
					{
						b.line(note);
					}
					return;
				}
			}
		}
	}
}
