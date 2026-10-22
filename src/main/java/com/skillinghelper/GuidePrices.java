package com.skillinghelper;

import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.prices.WikiPriceClient;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.game.ItemManager;

/**
 * Grand Exchange prices for the guides: RuneLite's GE guide prices, or live OSRS Wiki prices when the player opts
 * in. Owns the wiki refresh schedule, which only polls while logged in.
 */
@Singleton
class GuidePrices implements PriceLookup
{
	private static final Duration REFRESH = Duration.ofMinutes(5);

	private final Client client;
	private final SkillingHelperConfig config;
	private final ItemManager itemManager;
	private final WikiPriceClient wiki;
	private final ScheduledExecutorService executor;

	private ScheduledFuture<?> refreshTask;

	@Inject
	GuidePrices(Client client, SkillingHelperConfig config, ItemManager itemManager, WikiPriceClient wiki,
		ScheduledExecutorService executor)
	{
		this.client = client;
		this.config = config;
		this.itemManager = itemManager;
		this.wiki = wiki;
		this.executor = executor;
	}

	@Override
	public int price(int itemId)
	{
		WikiPriceClient.Price live = livePrice(itemId);
		return live != null && live.getHigh() > 0 ? live.getHigh() : guidePrice(itemId);
	}

	@Override
	public int sellPrice(int itemId)
	{
		WikiPriceClient.Price live = livePrice(itemId);
		return live != null && live.getLow() > 0 ? live.getLow() : guidePrice(itemId);
	}

	private int guidePrice(int itemId)
	{
		return (int) Math.min(Integer.MAX_VALUE, itemManager.getItemPrice(itemId));
	}

	private WikiPriceClient.Price livePrice(int itemId)
	{
		return config.livePrices() ? wiki.get(itemManager.canonicalize(itemId)) : null;
	}

	/**
	 * Starts or stops the wiki refresh to match the "Live OSRS Wiki prices" setting.
	 */
	void updateRefresh()
	{
		stopRefresh();
		if (config.livePrices())
		{
			refreshTask = executor.scheduleWithFixedDelay(this::refreshIfLoggedIn, 0, REFRESH.toMinutes(),
				TimeUnit.MINUTES);
		}
		else
		{
			wiki.clear();
		}
	}

	void shutDown()
	{
		stopRefresh();
		wiki.clear();
	}

	/**
	 * Prices aren't fetched while logged out, so catch up straight away on login.
	 */
	void onLoggedIn()
	{
		if (config.livePrices() && stale())
		{
			wiki.refresh();
		}
	}

	/**
	 * Prices are only shown in a guide, so don't keep polling the wiki from the login screen.
	 */
	private void refreshIfLoggedIn()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			wiki.refresh();
		}
	}

	private boolean stale()
	{
		Instant updated = wiki.getUpdated();
		return updated == null || Duration.between(updated, Instant.now()).compareTo(REFRESH) >= 0;
	}

	private void stopRefresh()
	{
		if (refreshTask != null)
		{
			refreshTask.cancel(false);
			refreshTask = null;
		}
		wiki.cancel();
	}

	/**
	 * Where the prices in the panel come from, for the player.
	 */
	String sourceText()
	{
		Instant updated = wiki.getUpdated();
		if (config.livePrices() && wiki.hasPrices() && updated != null)
		{
			long minutes = Duration.between(updated, Instant.now()).toMinutes();
			return "Prices: live OSRS Wiki, instant buy/sell (" + (minutes < 1 ? "just now" : minutes + " min ago") + ")";
		}
		if (config.livePrices())
		{
			return "Prices: RuneLite GE guide (live wiki prices loading...)";
		}
		return "Prices: RuneLite GE guide. Turn on live wiki prices in the plugin settings for real-time prices.";
	}
}
