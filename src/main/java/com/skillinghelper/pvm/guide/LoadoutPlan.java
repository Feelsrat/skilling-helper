package com.skillinghelper.pvm.guide;

import com.skillinghelper.engine.PriceLookup;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * The exact loadout for one guide and one player: which gear to wear from what they own (or buy), how many of each
 * supply to carry and where they come from, and what a trip costs.
 */
@Value
public class LoadoutPlan
{
	public static final int INVENTORY_SIZE = 28;

	public enum Have
	{
		WORN,
		/**
		 * In the inventory: wear it.
		 */
		CARRIED,
		IN_BANK,
		/**
		 * Not owned (or the bank hasn't been seen yet): buy it.
		 */
		BUY
	}

	@Value
	public static class GearLine
	{
		String slot;
		Gear gear;
		Have have;
		/**
		 * The id the player owns, or the one to buy.
		 */
		int itemId;
		int price;
		/**
		 * A better piece the player could buy within the guide's budget, or null.
		 */
		Gear upgrade;
		int upgradePrice;
	}

	@Value
	public static class SupplyLine
	{
		Supply supply;
		/**
		 * Exactly how many to carry.
		 */
		int amount;
		int restockBelow;
		int carried;
		/**
		 * What to take out of the bank: id to amount.
		 */
		Map<Integer, Integer> withdraw;
		/**
		 * How many to buy because the bank doesn't have enough.
		 */
		int toBuy;
		/**
		 * GE price of one of the first option.
		 */
		int price;

		public boolean isLow()
		{
			return carried < restockBelow;
		}

		public int missing()
		{
			return Math.max(0, amount - carried);
		}

		public String buyName()
		{
			return supply.getOptions().get(0).getName();
		}
	}

	List<GearLine> gear;
	List<SupplyLine> supplies;
	/**
	 * GE cost of the consumables one full trip uses.
	 */
	long tripCost;
	/**
	 * GE cost of everything still to buy.
	 */
	long buyCost;

	public static LoadoutPlan of(PvmGuide guide, GuideState s, PriceLookup prices)
	{
		List<GearLine> gear = new ArrayList<>();
		Gear weapon = null;
		long buy = 0;
		for (GearSlot slot : guide.getGear())
		{
			GearLine line = gearLine(slot, guide.getBudget(), s, prices);
			if (line == null)
			{
				continue;
			}
			gear.add(line);
			if (line.getHave() == Have.BUY)
			{
				buy += line.getPrice();
			}
			if ("weapon".equals(slot.getName()))
			{
				weapon = line.getGear();
			}
		}

		List<Supply> wanted = new ArrayList<>();
		Supply ammo = weapon == null ? null : Supplies.ammo(weapon.getAmmo());
		if (ammo != null)
		{
			wanted.add(ammo);
		}
		wanted.addAll(guide.getSupplies());

		int fixedSlots = 0;
		for (Supply sup : wanted)
		{
			// Ammo goes in the quiver slot, not the inventory
			if (sup.getAmount() != Supply.FILL && sup != ammo)
			{
				fixedSlots += sup.slots(sup.getAmount());
			}
		}
		int fill = Math.max(0, INVENTORY_SIZE - fixedSlots);

		List<SupplyLine> lines = new ArrayList<>();
		long trip = 0;
		for (Supply sup : wanted)
		{
			int amount = sup.getAmount() == Supply.FILL ? fill : sup.getAmount();
			if (amount <= 0)
			{
				continue;
			}
			int restock = sup.getAmount() == Supply.FILL ? Math.max(1, amount / 3) : sup.getRestockBelow();
			int carried = s.carried(sup.allIds());
			int need = Math.max(0, amount - carried);
			Map<Integer, Integer> withdraw = new LinkedHashMap<>();
			for (Supply.Option o : sup.getOptions())
			{
				for (int id : o.getIds())
				{
					if (need == 0)
					{
						break;
					}
					int take = Math.min(need, s.banked(id));
					if (take > 0)
					{
						withdraw.put(id, take);
						need -= take;
					}
				}
			}
			int toBuy = sup.isBuyable() ? need : 0;
			int price = prices.price(sup.getOptions().get(0).getIds()[0]);
			buy += (long) toBuy * price;
			if (sup.isConsumed())
			{
				trip += (long) amount * price;
			}
			lines.add(new SupplyLine(sup, amount, restock, carried, withdraw, toBuy, price));
		}
		return new LoadoutPlan(gear, lines, trip, buy);
	}

	/**
	 * The best piece for a slot that the player owns and can wear, else the best one to buy. Null when nothing in
	 * the slot can be worn or bought.
	 */
	private static GearLine gearLine(GearSlot slot, int budget, GuideState s, PriceLookup prices)
	{
		Gear owned = null;
		Gear bestBuy = null;
		int bestBuyPrice = 0;
		for (Gear g : slot.getOptions())
		{
			if (!canWear(g, s))
			{
				continue;
			}
			if (s.owns(g.getIds()))
			{
				owned = g;
				break;
			}
			if (bestBuy == null && g.isBuyable())
			{
				int price = prices.price(g.getBuyId());
				// An unknown price (0) is still worth suggesting
				if (budget <= 0 || price <= budget)
				{
					bestBuy = g;
					bestBuyPrice = price;
				}
			}
		}
		if (owned == null)
		{
			return bestBuy == null ? null
				: new GearLine(slot.getName(), bestBuy, Have.BUY, bestBuy.getBuyId(), bestBuyPrice, null, 0);
		}
		int id = ownedId(owned, s);
		Have have = s.worn(owned.getIds()) > 0 ? Have.WORN
			: s.inventory(owned.getIds()) > 0 ? Have.CARRIED : Have.IN_BANK;
		// bestBuy can only be set before the owned piece was reached, so it's an upgrade
		return new GearLine(slot.getName(), owned, have, id, 0, bestBuy, bestBuyPrice);
	}

	private static int ownedId(Gear g, GuideState s)
	{
		for (int id : g.getIds())
		{
			if (s.worn(id) > 0)
			{
				return id;
			}
		}
		for (int id : g.getIds())
		{
			if (s.inventory(id) > 0)
			{
				return id;
			}
		}
		for (int id : g.getIds())
		{
			if (s.banked(id) > 0)
			{
				return id;
			}
		}
		return g.getBuyId();
	}

	static boolean canWear(Gear g, GuideState s)
	{
		return (g.getSkill() == null || s.level(g.getSkill()) >= g.getLevel())
			&& s.level(Skill.DEFENCE) >= g.getDefence()
			&& (!g.isMembers() || s.isMembers());
	}

	/**
	 * Every item id in the loadout, to keep in the inventory and highlight in the bank.
	 */
	public java.util.Set<Integer> itemIds()
	{
		java.util.Set<Integer> ids = new java.util.HashSet<>();
		for (GearLine g : gear)
		{
			for (int id : g.getGear().getIds())
			{
				ids.add(id);
			}
		}
		for (SupplyLine l : supplies)
		{
			for (int id : l.getSupply().allIds())
			{
				ids.add(id);
			}
		}
		return ids;
	}
}
