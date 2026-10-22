package com.skillinghelper.engine;

import com.skillinghelper.data.BoostItem;
import com.skillinghelper.data.CombatGear;
import com.skillinghelper.data.ItemReq;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.SkillOutfit;
import com.skillinghelper.data.ToolType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import static com.skillinghelper.engine.Format.gp;

/**
 * Builds the "what you need" checklist for a method: tools, inputs, gear worth wearing, supplies and quests, each
 * marked as carried, in the bank, missing or optional.
 */
final class RequirementChecker
{
	private static final int[] NO_ITEMS = new int[0];

	private final PriceLookup prices;
	private final Recipes recipes;
	private final BankTrip trips;

	RequirementChecker(PriceLookup prices, Recipes recipes, BankTrip trips)
	{
		this.prices = prices;
		this.recipes = recipes;
		this.trips = trips;
	}

	/**
	 * @param actions actions still needed to reach the goal, used to size input requirements
	 */
	List<Requirement> check(Method m, PlayerState s, int actions)
	{
		List<Requirement> reqs = new ArrayList<>();
		addBestTool(m, s, reqs);
		// One line per substitute, e.g. bird houses stand in for both the logs and the clockworks
		Set<String> substitutesListed = new HashSet<>();
		addTools(m, s, reqs, substitutesListed);
		addInputs(m, s, actions, reqs, substitutesListed);
		addOutfits(m, s, actions, reqs);
		addBoosts(m, s, reqs);
		addCombatGear(m, s, reqs);
		for (ItemReq supply : m.getSupplies())
		{
			reqs.add(supplyRequirement(supply, s));
		}
		for (Quest q : m.getQuests())
		{
			boolean done = s.getCompletedQuests().contains(q);
			reqs.add(info("Quest: " + q.getName(), done ? Requirement.Status.READY : Requirement.Status.MISSING));
		}
		if (m.getRequirementText() != null)
		{
			reqs.add(info(m.getRequirementText(), Requirement.Status.INFO));
		}
		return reqs;
	}

	// ---- Tools ----

	/**
	 * The method's tool family (axe, pickaxe...): the best one owned, and the best one usable if that's better.
	 */
	private void addBestTool(Method m, PlayerState s, List<Requirement> reqs)
	{
		ToolType toolType = m.getToolType();
		if (toolType == null)
		{
			return;
		}
		ToolType.Tier usable = ToolAdvisor.bestUsable(toolType, s);
		ToolAdvisor.OwnedTool owned = ToolAdvisor.bestOwned(toolType, s);
		if (owned == null)
		{
			int price = prices.price(usable.getIds()[0]);
			reqs.add(new Requirement(usable.getName() + " (best " + toolType.getNoun() + " for your level, ~"
				+ gp(price) + ")", Requirement.Status.MISSING, usable.getIds(), 1, price));
			return;
		}
		addOwnedTool(toolType, owned, s, reqs);
		if (owned.getTier() != usable)
		{
			int price = prices.price(usable.getIds()[0]);
			reqs.add(new Requirement("Upgrade: " + usable.getName() + " is the best you can use (~" + gp(price) + ")",
				Requirement.Status.OPTIONAL, NO_ITEMS, 0, price));
		}
	}

	private static void addOwnedTool(ToolType toolType, ToolAdvisor.OwnedTool owned, PlayerState s,
		List<Requirement> reqs)
	{
		String name = owned.getTier().getName();
		if (owned.getWhere() != ToolAdvisor.Where.BANK)
		{
			reqs.add(item(name, Requirement.Status.READY, owned.getItemId()));
			return;
		}
		ToolAdvisor.OwnedTool carried = ToolAdvisor.bestCarried(toolType, s);
		if (carried != null)
		{
			// A usable tool is already carried, so the better one in the bank is a recommendation only
			reqs.add(item(carried.getTier().getName(), Requirement.Status.READY, carried.getItemId()));
			reqs.add(item("Better: " + name + " is in your bank", Requirement.Status.OPTIONAL, owned.getItemId()));
		}
		else
		{
			reqs.add(item(name + " (in bank)", Requirement.Status.IN_BANK, owned.getItemId()));
		}
	}

	private void addTools(Method m, PlayerState s, List<Requirement> reqs, Set<String> substitutesListed)
	{
		for (ItemReq tool : m.getTools())
		{
			if (!addSubstitute(tool, s, reqs, substitutesListed))
			{
				reqs.add(itemRequirement(tool, s, 1, 1));
			}
		}
	}

	/**
	 * Lists the substitute instead of the item when the player owns one, once per substitute.
	 *
	 * @return whether the substitute took the item's place
	 */
	private boolean addSubstitute(ItemReq req, PlayerState s, List<Requirement> reqs, Set<String> substitutesListed)
	{
		if (!s.ownsSubstitute(req))
		{
			return false;
		}
		if (substitutesListed.add(req.getSubstituteName()))
		{
			Requirement r = itemRequirement(ItemReq.tool(req.getSubstituteName(), req.getSubstituteIds()), s, 1, 1);
			reqs.add(r.withText(r.getText() + " (instead of " + req.getName().toLowerCase() + ")"));
		}
		return true;
	}

	// ---- Inputs ----

	private void addInputs(Method m, PlayerState s, int actions, List<Requirement> reqs, Set<String> substitutesListed)
	{
		Map<ItemReq, Integer> tripAmounts = trips.tripAmounts(m, s, actions);
		for (ItemReq input : m.getInputs())
		{
			if (addSubstitute(input, s, reqs, substitutesListed))
			{
				continue;
			}
			long needed = (long) Math.max(actions, 1) * input.getPerAction();
			if (recipes.making(input, s, Math.max(actions, 1)))
			{
				addRecipe(input, s, needed, Math.max(actions, 1), reqs);
				continue;
			}
			int trip = (int) Math.min(input.isWholeStack() ? Integer.MAX_VALUE
				: tripAmounts.getOrDefault(input, input.getQuantity()), Math.max(needed, 1));
			Requirement r = itemRequirement(input, s, needed, trip);
			if (input.hasSubstitute() && r.getStatus() == Requirement.Status.MISSING)
			{
				r = r.withText(r.getText() + ", or use a " + input.getSubstituteName().toLowerCase());
			}
			reqs.add(r);
		}
	}

	/**
	 * An input the player makes themselves: how to make it, then one line per part.
	 */
	private void addRecipe(ItemReq input, PlayerState s, long needed, long actions, List<Requirement> reqs)
	{
		long toMake = Math.max(1, needed - s.owned(input.getIds()));
		int perTrip = Recipes.perTrip(input, s, actions);
		reqs.add(info(input.getName() + ": make them yourself. " + input.getRecipeText() + ".",
			Requirement.Status.INFO));
		for (ItemReq part : input.getRecipe())
		{
			Requirement r = itemRequirement(part, s, toMake * part.getPerAction(),
				Math.max(1, perTrip * part.getPerAction()));
			reqs.add(r.withText(r.getText() + " (to make " + input.getName().toLowerCase() + ")"));
		}
	}

	private Requirement itemRequirement(ItemReq req, PlayerState s, long needed, int trip)
	{
		int carried = s.carried(req.getIds());
		int banked = s.bankCount(req.getIds());
		String name = req.getName();
		boolean consumed = req.getPerAction() > 0;
		if (!consumed && req.getQuantity() > 1)
		{
			return toolSet(req, carried, banked);
		}
		String counts = consumed
			? String.format(" (%,d carried, %,d in bank, %,d needed for goal)", carried, banked, needed)
			: "";
		if (carried > 0)
		{
			return new Requirement(name + counts, Requirement.Status.READY, req.getIds(), trip, 0);
		}
		if (banked > 0)
		{
			return new Requirement(name + (consumed ? counts : " (in bank)"), Requirement.Status.IN_BANK, req.getIds(),
				trip, 0);
		}
		if (req.getSource() != null)
		{
			return new Requirement(name + counts + " - " + req.getSource(), Requirement.Status.MISSING, req.getIds(),
				trip, 0);
		}
		long toBuy = consumed ? Math.max(0, needed - banked) : 1;
		int unit = prices.price(req.primaryId());
		long cost = toBuy * unit;
		String buy = toBuy > 1
			? String.format(" - buy %,d x ~%s = ~%s", toBuy, gp(unit), gp(cost))
			: " - buy for ~" + gp(cost);
		return new Requirement(name + counts + buy, Requirement.Status.MISSING, req.getIds(), trip, cost);
	}

	/**
	 * Several copies of a tool, e.g. 4 box traps: all of them should be carried.
	 */
	private Requirement toolSet(ItemReq req, int carried, int banked)
	{
		int want = req.getQuantity();
		String name = String.format("%d x %s", want, req.getName());
		if (carried >= want)
		{
			return new Requirement(name, Requirement.Status.READY, req.getIds(), want, 0);
		}
		if (carried + banked >= want)
		{
			return new Requirement(name + String.format(" (%d carried, %d in bank)", carried, banked),
				Requirement.Status.IN_BANK, req.getIds(), want - carried, 0);
		}
		int toBuy = want - carried - banked;
		int unit = prices.price(req.primaryId());
		return new Requirement(name + String.format(" (you have %d) - buy %d x ~%s", carried + banked, toBuy, gp(unit)),
			Requirement.Status.MISSING, req.getIds(), want - carried, (long) toBuy * unit);
	}

	/**
	 * An optional supply such as food: fine to skip, so it's only ever a suggestion.
	 */
	private Requirement supplyRequirement(ItemReq req, PlayerState s)
	{
		int carried = s.carried(req.getIds());
		int banked = s.bankCount(req.getIds());
		String amount = req.isWholeStack() ? "" : req.getQuantity() + " x ";
		int quantity = Math.max(1, req.getQuantity());
		if (carried > 0)
		{
			return new Requirement("Optional: " + req.getName() + " (carried)", Requirement.Status.READY, req.getIds(),
				quantity, 0);
		}
		if (banked > 0)
		{
			return new Requirement("Optional: " + amount + req.getName() + " (" + String.format("%,d", banked)
				+ " in bank)", Requirement.Status.SUGGESTED, req.getIds(), quantity, 0);
		}
		int unit = prices.price(req.primaryId());
		String price = unit > 0 ? " (~" + gp(unit) + " each on the GE)" : "";
		return info("Optional: bring " + amount + req.getName().toLowerCase() + price, Requirement.Status.SUGGESTED);
	}

	// ---- Gear worth wearing ----

	/**
	 * Skilling outfit pieces to wear, and what the missing pieces would save.
	 */
	private static void addOutfits(Method m, PlayerState s, int actions, List<Requirement> reqs)
	{
		double goalHours = m.getXpPerHour() <= 0 ? 0 : (double) actions * m.getXpPerAction() / m.getXpPerHour();
		for (SkillOutfit outfit : SkillOutfit.values())
		{
			if (outfit.getSkill() != m.getSkill())
			{
				continue;
			}
			int missingPieces = 0;
			for (int[] slot : outfit.getSlots())
			{
				if (s.getEquipment().has(slot))
				{
					continue;
				}
				int id = ItemIds.firstOwned(slot, s);
				if (id == -1)
				{
					missingPieces++;
					continue;
				}
				reqs.add(item(outfit.getName() + " piece: wear it (" + outfit.getBonus() + ")" + inBankNote(id, s),
					Requirement.Status.OPTIONAL, id));
			}
			if (missingPieces > 0 && s.bankKnown())
			{
				reqs.add(info(unlockText(outfit, missingPieces, goalHours, m), Requirement.Status.UNLOCK));
			}
		}
	}

	private static String unlockText(SkillOutfit outfit, int missingPieces, double goalHours, Method m)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(missingPieces == outfit.getSlots().length ? "Worth chasing: " : "Missing " + missingPieces
			+ " piece" + (missingPieces == 1 ? "" : "s") + " of the ").append(outfit.getName())
			.append(" (").append(outfit.getBonus()).append("). Earn it from ").append(outfit.getSource());
		if (m.getId().equals(outfit.getSourceMethodId()))
		{
			sb.append(", which this method does");
		}
		sb.append('.');
		if (outfit.getXpBonus() > 0 && goalHours > 0)
		{
			double saved = goalHours * outfit.getXpBonus() / (1 + outfit.getXpBonus());
			sb.append(" The full set would save ~").append(Format.hours(saved)).append(" on this goal.");
		}
		return sb.toString();
	}

	/**
	 * Tradeable helpers such as the dodgy necklace: worn, owned (wear it) or worth buying.
	 */
	private void addBoosts(Method m, PlayerState s, List<Requirement> reqs)
	{
		for (BoostItem boost : BoostItem.values())
		{
			if (!boost.appliesTo(m))
			{
				continue;
			}
			if (s.getEquipment().has(boost.getIds()))
			{
				reqs.add(new Requirement(boost.getName() + " (" + boost.getBonus() + ")", Requirement.Status.READY,
					boost.getIds(), 1, 0));
			}
			else if (s.owned(boost.getIds()) > 0)
			{
				boolean carried = s.getInventory().has(boost.getIds());
				reqs.add(new Requirement(boost.getName() + ": wear it (" + boost.getBonus() + ")"
					+ (carried ? "" : " (in bank)"), Requirement.Status.OPTIONAL, boost.getIds(), 1, 0));
			}
			else
			{
				int price = prices.price(boost.getIds()[0]);
				reqs.add(info("Optional buy: " + boost.getName() + " (~" + gp(price) + " on the GE): "
					+ boost.getBonus(), Requirement.Status.SUGGESTED));
			}
		}
	}

	/**
	 * Suggests the best weapon and armour per slot: wear what you own, or buy an upgrade from the GE.
	 */
	private void addCombatGear(Method m, PlayerState s, List<Requirement> reqs)
	{
		if (m.getGearStyle() == null)
		{
			return;
		}
		for (CombatGear slot : CombatGear.values())
		{
			if (slot.getStyle() == m.getGearStyle())
			{
				addGearSlot(slot, s, reqs);
			}
		}
	}

	/**
	 * One gear slot. Tiers are listed best first, so a lower index is better gear.
	 */
	private void addGearSlot(CombatGear slot, PlayerState s, List<Requirement> reqs)
	{
		List<CombatGear.Tier> tiers = slot.getTiers();
		int bestUsable = -1;
		int worn = -1;
		int owned = -1;
		int ownedId = -1;
		for (int i = 0; i < tiers.size(); i++)
		{
			CombatGear.Tier t = tiers.get(i);
			if (!canWear(t, s))
			{
				continue;
			}
			if (bestUsable == -1)
			{
				bestUsable = i;
			}
			if (worn == -1 && s.getEquipment().has(t.getIds()))
			{
				worn = i;
			}
			if (owned == -1 && s.owned(t.getIds()) > 0)
			{
				owned = i;
				ownedId = ItemIds.firstOwned(t.getIds(), s);
			}
		}
		if (bestUsable == -1)
		{
			return;
		}
		if (owned != -1 && ownedId != -1 && (worn == -1 || owned < worn))
		{
			reqs.add(item("Gear: wear your " + tiers.get(owned).getName() + inBankNote(ownedId, s),
				Requirement.Status.OPTIONAL, ownedId));
		}
		int have = worn != -1 && (owned == -1 || worn < owned) ? worn : owned;
		if (s.bankKnown() && (have == -1 || bestUsable < have))
		{
			CombatGear.Tier best = tiers.get(bestUsable);
			reqs.add(info("Gear upgrade (" + slot.getSlotName() + "): " + best.getName() + " (~"
				+ gp(prices.price(best.getIds()[0])) + " on the GE)", Requirement.Status.SUGGESTED));
		}
	}

	private static boolean canWear(CombatGear.Tier t, PlayerState s)
	{
		return s.level(t.getSkill()) >= t.getLevel()
			&& s.level(Skill.DEFENCE) >= t.getDefenceLevel()
			&& (!t.isMembers() || s.isMembersWorld());
	}

	// ---- Building lines ----

	/**
	 * " (in bank)" unless the item is carried.
	 */
	private static String inBankNote(int itemId, PlayerState s)
	{
		return s.getInventory().has(itemId) ? "" : " (in bank)";
	}

	/**
	 * A line about one specific item, one of which is needed.
	 */
	private static Requirement item(String text, Requirement.Status status, int itemId)
	{
		return new Requirement(text, status, new int[]{itemId}, 1, 0);
	}

	/**
	 * A line that isn't about an item in the bank.
	 */
	private static Requirement info(String text, Requirement.Status status)
	{
		return new Requirement(text, status, NO_ITEMS, 0, 0);
	}
}
