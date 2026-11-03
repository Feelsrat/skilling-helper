package com.skillinghelper;

import com.skillinghelper.bank.BankCache;
import com.skillinghelper.data.Mooring;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.PlayerState;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldType;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Reads everything the guide engine needs from the client into a {@link PlayerState}. Runs on the client thread.
 */
@Singleton
class PlayerSnapshot
{
	private static final int INVENTORY_SIZE = 28;

	private static final int[] RUNE_POUCHES = {ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER,
		ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER};
	private static final int[] POUCH_TYPES = {VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2,
		VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6};
	private static final int[] POUCH_AMOUNTS = {VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2,
		VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5,
		VarbitID.RUNE_POUCH_QUANTITY_6};

	private final Client client;
	private final BankCache bankCache;
	private final BirdhouseTracker birdhouses;
	private final PatchTracker patches;
	private final SlayerTaskReader slayerTask;

	@Inject
	PlayerSnapshot(Client client, BankCache bankCache, BirdhouseTracker birdhouses, PatchTracker patches,
		 SlayerTaskReader slayerTask)
	{
		this.client = client;
		this.bankCache = bankCache;
		this.birdhouses = birdhouses;
		this.patches = patches;
		this.slayerTask = slayerTask;
	}

	PlayerState take(Mooring boatMooring, Set<Quest> completedQuests, Set<Quest> startedQuests)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		Map<Integer, Integer> slots = slotsUsed(inv);
		int used = slots.values().stream().mapToInt(Integer::intValue).sum();
		Player me = client.getLocalPlayer();
		WorldPoint location = me == null ? null : worldLocation(client, me);
		return PlayerState.builder()
			.levels(levels())
			.xp(xp())
			.inventory(withRunePouch(inv))
			.inventorySlots(new ItemCounts(slots))
			.equipment(counts(client.getItemContainer(InventoryID.WORN)))
			.bank(bankCache.get())
			.freeInventorySlots(INVENTORY_SIZE - used)
			.location(location)
			.membersWorld(client.getWorldType().contains(WorldType.MEMBERS))
			.inInstance(client.getTopLevelWorldView().isInstance())
			.onBoat(me != null && isOnBoat(me))
			.boatMooring(boatMooring)
			.stopWaits(stopWaits())
			.completedQuests(copy(completedQuests))
			.startedQuests(copy(startedQuests))
			.slayerTask(slayerTask.read(location))
			.build();
	}

	private static Set<Quest> copy(Set<Quest> quests)
	{
		return quests.isEmpty() ? EnumSet.noneOf(Quest.class) : EnumSet.copyOf(quests);
	}

	private Map<Skill, Integer> levels()
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, client.getRealSkillLevel(s));
		}
		return levels;
	}

	private Map<Skill, Integer> xp()
	{
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			xp.put(s, client.getSkillExperience(s));
		}
		return xp;
	}

	private Map<String, Integer> stopWaits()
	{
		Map<String, Integer> waits = new HashMap<>(birdhouses.waits());
		waits.putAll(patches.waits());
		return waits;
	}

	/**
	 * How many inventory slots each item takes up.
	 */
	private static Map<Integer, Integer> slotsUsed(ItemContainer inv)
	{
		Map<Integer, Integer> slots = new HashMap<>();
		if (inv != null)
		{
			for (Item item : inv.getItems())
			{
				if (item.getId() > 0)
				{
					slots.merge(item.getId(), 1, Integer::sum);
				}
			}
		}
		return slots;
	}

	private static ItemCounts counts(ItemContainer container)
	{
		return container == null ? ItemCounts.EMPTY : new ItemCounts(countMap(container));
	}

	private static Map<Integer, Integer> countMap(ItemContainer container)
	{
		Map<Integer, Integer> m = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				m.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		return m;
	}

	/**
	 * The inventory, counting the runes inside a carried rune pouch as carried too.
	 */
	private ItemCounts withRunePouch(ItemContainer inv)
	{
		if (inv == null)
		{
			return ItemCounts.EMPTY;
		}
		Map<Integer, Integer> m = countMap(inv);
		boolean hasPouch = false;
		for (int pouch : RUNE_POUCHES)
		{
			hasPouch |= m.containsKey(pouch);
		}
		if (hasPouch)
		{
			EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
			for (int i = 0; i < POUCH_TYPES.length; i++)
			{
				int type = client.getVarbitValue(POUCH_TYPES[i]);
				int amount = client.getVarbitValue(POUCH_AMOUNTS[i]);
				int rune = type > 0 ? runes.getIntValue(type) : -1;
				if (rune > 0 && amount > 0)
				{
					m.merge(rune, amount, Integer::sum);
				}
			}
		}
		return new ItemCounts(m);
	}

	/**
	 * The player's position in the main world, also while standing on a boat (which has its own world view).
	 */
	static WorldPoint worldLocation(Client client, Player me)
	{
		LocalPoint local = me.getLocalLocation();
		if (local != null && local.getWorldView() != WorldView.TOPLEVEL)
		{
			WorldEntity boat = client.getTopLevelWorldView().worldEntities().byIndex(local.getWorldView());
			if (boat != null)
			{
				return WorldPoint.fromLocalInstance(client, boat.transformToMainWorld(local));
			}
		}
		return me.getWorldLocation();
	}

	static boolean isOnBoat(Player me)
	{
		LocalPoint local = me.getLocalLocation();
		return local != null && local.getWorldView() != WorldView.TOPLEVEL;
	}
}
