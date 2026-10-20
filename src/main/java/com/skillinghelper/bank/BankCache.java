package com.skillinghelper.bank;

import com.skillinghelper.SkillingHelperConfig;
import com.skillinghelper.engine.ItemCounts;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

/**
 * Remembers the bank contents per account (RuneLite profile) so guides know what you own even when the bank
 * is closed or after a restart.
 */
@Singleton
public class BankCache
{
	private static final String KEY = "bank";

	private final ConfigManager configManager;
	private final ItemManager itemManager;
	private ItemCounts bank;
	private boolean dirty;

	@Inject
	BankCache(ConfigManager configManager, ItemManager itemManager)
	{
		this.configManager = configManager;
		this.itemManager = itemManager;
	}

	/**
	 * Last known bank, or null if never seen on this account.
	 */
	public ItemCounts get()
	{
		return bank;
	}

	public void load()
	{
		String raw = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY);
		bank = raw == null ? null : parse(raw);
		dirty = false;
	}

	public void update(ItemContainer container)
	{
		Map<Integer, Integer> counts = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			counts.merge(itemManager.canonicalize(item.getId()), item.getQuantity(), Integer::sum);
		}
		bank = new ItemCounts(counts);
		dirty = true;
	}

	public void save()
	{
		if (!dirty || bank == null)
		{
			return;
		}
		configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY, serialize(bank));
		dirty = false;
	}

	public void reset()
	{
		bank = null;
		dirty = false;
	}

	static String serialize(ItemCounts counts)
	{
		StringBuilder sb = new StringBuilder();
		counts.asMap().forEach((id, qty) ->
		{
			if (sb.length() > 0)
			{
				sb.append(',');
			}
			sb.append(id).append(':').append(qty);
		});
		return sb.toString();
	}

	static ItemCounts parse(String raw)
	{
		Map<Integer, Integer> counts = new HashMap<>();
		for (String pair : raw.split(","))
		{
			int colon = pair.indexOf(':');
			if (colon <= 0)
			{
				continue;
			}
			try
			{
				counts.put(Integer.parseInt(pair.substring(0, colon)), Integer.parseInt(pair.substring(colon + 1)));
			}
			catch (NumberFormatException ignored)
			{
				// skip corrupt entries
			}
		}
		return new ItemCounts(counts);
	}
}
