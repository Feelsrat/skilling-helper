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

/**
 * Reads everything the guide engine needs from the client into a {@link PlayerState}. Runs on the client thread.
 */
@Singleton
class PlayerSnapshot
{
	private static final int INVENTORY_SIZE = 28;

	private final Client client;
	private final BankCache bankCache;
	private final RateTracker rates;
	private final BirdhouseTracker birdhouses;
	private final PatchTracker patches;
	private final SlayerTaskReader slayerTask;

	@Inject
	PlayerSnapshot(Client client, BankCache bankCache, RateTracker rates, BirdhouseTracker birdhouses,
		PatchTracker patches, SlayerTaskReader slayerTask)
	{
		this.client = client;
		this.bankCache = bankCache;
		this.rates = rates;
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
			.inventory(counts(inv))
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
			.measuredXpPerHour(rates.rates())
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
		if (container == null)
		{
			return ItemCounts.EMPTY;
		}
		Map<Integer, Integer> m = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				m.merge(item.getId(), item.getQuantity(), Integer::sum);
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
