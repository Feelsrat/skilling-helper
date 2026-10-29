package com.skillinghelper.pvm.guide;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import com.skillinghelper.engine.ItemHighlight;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Picks what to do right now for a guide, Quest Helper style: fight when the boss is here, otherwise get the loadout
 * ready (buy, bank, wear), then work through the guide's stages and go to the fight.
 */
public final class GuideResolver
{
	/**
	 * Below this share of hitpoints with no food left, the fight step suggests leaving.
	 */
	private static final double LOW_HP = 0.4;

	public static PvmStep resolve(PvmGuide guide, String contentName, GuideState s, LoadoutPlan plan)
	{
		Fight fight = guide.getFight();
		if (fight != null && bossHere(fight, s))
		{
			return fightStep(guide, contentName, s, plan);
		}
		boolean inside = guide.getInside().test(s);
		if (s.isJustKilled() && inside)
		{
			return PvmStep.builder()
				.kind(PvmStep.Kind.LOOT)
				.title("Pick up the loot")
				.line("Nice kill! Grab the drop, then carry on with the next step.")
				.build();
		}

		Stage stage = firstStage(guide, s);
		if (inside || (stage != null && stage.isCommitted()))
		{
			return stage != null ? stageStep(stage, s) : travelStep(guide, contentName, s);
		}

		if (s.isBankOpen())
		{
			PvmStep bank = bankStep(plan, s, null);
			if (bank != null)
			{
				return bank;
			}
		}
		PvmStep equip = equipStep(plan);
		if (equip != null)
		{
			return equip;
		}
		if (!s.bankKnown())
		{
			Location bank = Banks.nearest(s.getLocation(), s.isMembers());
			return PvmStep.builder()
				.kind(PvmStep.Kind.BANK)
				.title("Open your bank")
				.line("Open your bank once so I can see which gear and supplies you already have.")
				.destination(bank)
				.highlightBank(true)
				.build();
		}
		PvmStep buy = buyStep(plan, s);
		if (buy != null)
		{
			return buy;
		}
		if (needsBank(plan, s))
		{
			Location bank = Banks.nearest(s.getLocation(), s.isMembers());
			return bankStep(plan, s, bank);
		}
		return stage != null ? stageStep(stage, s) : travelStep(guide, contentName, s);
	}

	static boolean bossHere(Fight fight, GuideState s)
	{
		for (String boss : fight.getBosses())
		{
			if (s.npc(boss) > 0)
			{
				return true;
			}
		}
		return false;
	}

	private static Stage firstStage(PvmGuide guide, GuideState s)
	{
		for (Stage stage : guide.getStages())
		{
			if (stage.getWhen().test(s))
			{
				return stage;
			}
		}
		return null;
	}

	// ---- The fight ----

	private static PvmStep fightStep(PvmGuide guide, String contentName, GuideState s, LoadoutPlan plan)
	{
		Fight fight = guide.getFight();
		PvmStep.PvmStepBuilder b = PvmStep.builder()
			.kind(PvmStep.Kind.FIGHT)
			.title("Fight " + fightName(fight, s, contentName));
		for (String boss : fight.getBosses())
		{
			b.highlight(Target.npc(null, boss));
		}
		for (Fight.Add add : fight.getAdds())
		{
			b.addHighlight(Target.npc(null, add.getName()));
			int alive = s.npc(add.getName());
			if (alive > 0)
			{
				b.line(add.getText() + (alive > 1 ? " (" + alive + " up)" : ""));
			}
		}
		for (Map.Entry<String, String> note : fight.getNotes().entrySet())
		{
			if (s.npc(note.getKey()) > 0)
			{
				b.line(note.getValue());
			}
		}
		for (String line : fight.getLive().apply(s))
		{
			b.line(line);
		}
		supplyWarnings(b, s, plan);
		return b.build();
	}

	private static String fightName(Fight fight, GuideState s, String fallback)
	{
		for (String boss : fight.getBosses())
		{
			if (s.npc(boss) > 0)
			{
				return boss;
			}
		}
		return fallback;
	}

	/**
	 * Reminders about the player's own supplies. Nothing about the boss.
	 */
	private static void supplyWarnings(PvmStep.PvmStepBuilder b, GuideState s, LoadoutPlan plan)
	{
		int food = 0;
		for (LoadoutPlan.SupplyLine l : plan.getSupplies())
		{
			if ("Food".equals(l.getSupply().getName()))
			{
				food = l.getCarried();
			}
		}
		boolean lowHp = s.getMaxHitpoints() > 0 && s.getHitpoints() < s.getMaxHitpoints() * LOW_HP;
		if (lowHp && food == 0)
		{
			b.line("Low on hitpoints with no food left: teleport out and restock.");
		}
		else if (food > 0 && food <= 3)
		{
			b.line("Only " + food + " food left: finish this kill, then restock.");
		}
	}

	// ---- Loadout ----

	private static boolean needsBank(LoadoutPlan plan, GuideState s)
	{
		for (LoadoutPlan.GearLine g : plan.getGear())
		{
			if (g.getHave() == LoadoutPlan.Have.IN_BANK)
			{
				return true;
			}
		}
		for (LoadoutPlan.SupplyLine l : plan.getSupplies())
		{
			if (l.isShort(s.isTripStocked()) && !l.getWithdraw().isEmpty())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * What to buy when the player is out of something they need and has none in the bank. Other missing gear and
	 * supplies only show as buys in the panel's loadout.
	 */
	private static PvmStep buyStep(LoadoutPlan plan, GuideState s)
	{
		List<String> lines = new ArrayList<>();
		long total = 0;
		// Before setting off everything has to be there in full. Once under way, only a missing weapon, tool or food
		// is worth a trip; the rest is bought on the next one
		boolean blocking = false;
		for (LoadoutPlan.GearLine g : plan.getGear())
		{
			if (g.getHave() == LoadoutPlan.Have.BUY && required(g))
			{
				lines.add("• 1 x " + g.getGear().getName() + gp(g.getPrice()));
				total += g.getPrice();
				blocking = true;
			}
		}
		for (LoadoutPlan.SupplyLine l : plan.getSupplies())
		{
			if (l.isShort(s.isTripStocked()) && l.getToBuy() > 0)
			{
				lines.add("• " + l.getToBuy() + " x " + l.buyName() + gp((long) l.getPrice() * l.getToBuy()));
				total += (long) l.getPrice() * l.getToBuy();
				blocking |= !s.isTripStocked() || l.getSupply().isRequired() || l.getSupply().getAmount() == Supply.FILL;
			}
		}
		if (!blocking)
		{
			return null;
		}
		PvmStep.PvmStepBuilder b = PvmStep.builder()
			.kind(PvmStep.Kind.BUY)
			.title("Buy your loadout at the Grand Exchange")
			.destination(Banks.GRAND_EXCHANGE);
		for (String line : lines)
		{
			b.line(line);
		}
		if (total > 0)
		{
			b.line(String.format("About %,d gp in total.", total));
		}
		b.line("Then bank and I'll tell you exactly what to take.");
		return b.build();
	}

	/**
	 * Only the weapon is worth a trip to the GE on its own; the rest is shown as suggestions in the panel.
	 */
	private static boolean required(LoadoutPlan.GearLine g)
	{
		return "weapon".equals(g.getSlot());
	}

	/**
	 * Withdraw exactly what the loadout still needs. With {@code bank} set, also walks there first.
	 */
	private static PvmStep bankStep(LoadoutPlan plan, GuideState s, Location bank)
	{
		PvmStep.PvmStepBuilder b = PvmStep.builder()
			.kind(PvmStep.Kind.BANK)
			.highlightBank(true);
		boolean any = false;
		int slotsNeeded = 0;
		for (LoadoutPlan.GearLine g : plan.getGear())
		{
			if (g.getHave() == LoadoutPlan.Have.IN_BANK)
			{
				b.line("• 1 x " + g.getGear().getName() + " (wear it)");
				b.itemHighlight(g.getItemId(), ItemHighlight.WITHDRAW);
				slotsNeeded++;
				any = true;
			}
		}
		for (LoadoutPlan.SupplyLine l : plan.getSupplies())
		{
			for (Map.Entry<Integer, Integer> w : l.getWithdraw().entrySet())
			{
				b.line("• " + w.getValue() + " x " + optionName(l.getSupply(), w.getKey()));
				b.itemHighlight(w.getKey(), ItemHighlight.WITHDRAW);
				slotsNeeded += l.getSupply().slots(w.getValue());
				any = true;
			}
			if (l.getToBuy() > 0 && l.getWithdraw().isEmpty() && l.getCarried() == 0 && l.getSupply().isRequired())
			{
				b.line("✘ No " + l.buyName().toLowerCase() + " in your bank: get " + l.getToBuy() + " first.");
			}
		}
		if (!any)
		{
			return null;
		}
		b.title(bank == null || s.in(bank) || s.isBankOpen() ? "Take out your loadout" : "Bank at " + bank.getName());
		if (bank != null && !s.in(bank) && !s.isBankOpen())
		{
			b.destination(bank);
		}
		if (s.isBankOpen())
		{
			b.line("The attack icon at the top of the bank shows only your loadout.");
		}
		if (slotsNeeded > s.freeSlots())
		{
			b.line("Deposit your inventory first: you need " + slotsNeeded + " free slots.");
		}
		return b.build();
	}

	private static String optionName(Supply supply, int id)
	{
		for (Supply.Option o : supply.getOptions())
		{
			for (int i : o.getIds())
			{
				if (i == id)
				{
					return o.getName();
				}
			}
		}
		return supply.getName();
	}

	private static PvmStep equipStep(LoadoutPlan plan)
	{
		PvmStep.PvmStepBuilder b = PvmStep.builder()
			.kind(PvmStep.Kind.EQUIP)
			.title("Put on your gear");
		boolean any = false;
		for (LoadoutPlan.GearLine g : plan.getGear())
		{
			if (g.getHave() == LoadoutPlan.Have.CARRIED)
			{
				b.line("• Wear your " + g.getGear().getName());
				b.itemHighlight(g.getItemId(), ItemHighlight.EQUIP);
				any = true;
			}
		}
		return any ? b.build() : null;
	}

	// ---- Getting there ----

	private static PvmStep stageStep(Stage stage, GuideState s)
	{
		PvmStep.PvmStepBuilder b = PvmStep.builder()
			.kind(PvmStep.Kind.STAGE)
			.title(stage.getTitle());
		if (stage.getPlace() != null && !s.in(stage.getPlace()))
		{
			b.destination(stage.getPlace());
			b.line("Head to " + stage.getPlace().getName() + ".");
		}
		for (String line : stage.getLines())
		{
			b.line(line);
		}
		for (String line : stage.getLive().apply(s))
		{
			b.line(line);
		}
		for (Target t : stage.getHighlights())
		{
			b.highlight(t);
		}
		for (int id : stage.getUseItems())
		{
			b.itemHighlight(id, ItemHighlight.USE);
		}
		return b.build();
	}

	private static PvmStep travelStep(PvmGuide guide, String contentName, GuideState s)
	{
		Location place = guide.getPlace();
		boolean there = place == null || s.in(place);
		PvmStep.PvmStepBuilder b = PvmStep.builder()
			.kind(PvmStep.Kind.TRAVEL)
			.title(there ? "Start " + contentName : "Go to " + (place == null ? contentName : place.getName()));
		if (!there)
		{
			b.destination(place);
		}
		for (String line : guide.getArrival())
		{
			b.line(line);
		}
		for (Target t : guide.getEntrance())
		{
			b.highlight(t);
		}
		return b.build();
	}

	private static String gp(long gp)
	{
		return gp > 0 ? String.format(" (~%,d gp)", gp) : "";
	}

	/**
	 * Every NPC and object name a guide might highlight, so only those get tracked.
	 */
	public static void addNames(PvmGuide guide, Set<String> npcs, Set<String> objects)
	{
		List<Target> all = new ArrayList<>(guide.getEntrance());
		for (Stage stage : guide.getStages())
		{
			all.addAll(stage.getHighlights());
		}
		for (Target t : all)
		{
			(t.getKind() == Target.Kind.NPC ? npcs : objects).addAll(t.getNames());
		}
		for (String name : guide.getTracked())
		{
			npcs.add(name.toLowerCase(java.util.Locale.ROOT));
		}
		if (guide.getFight() != null)
		{
			for (String boss : guide.getFight().getBosses())
			{
				npcs.add(boss.toLowerCase(java.util.Locale.ROOT));
			}
			for (Fight.Add add : guide.getFight().getAdds())
			{
				npcs.add(add.getName().toLowerCase(java.util.Locale.ROOT));
			}
		}
	}

	private GuideResolver()
	{
	}
}
