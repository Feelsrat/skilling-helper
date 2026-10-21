package com.skillinghelper;

import com.skillinghelper.engine.Route;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;

/**
 * Drives the Shortest Path plugin through its plugin messages: shows the path for a {@link Route}, and asks it
 * separately whether the route can actually be completed so the guide can explain when it can't.
 */
@Slf4j
@Singleton
public class ShortestPathBridge
{
	private static final String NAMESPACE = "shortestpath";
	private static final String QUERY_PREFIX = "skillinghelper:";

	/**
	 * Transports that only get used when the player meets their requirements, which Shortest Path checks itself.
	 * Several are off by default there, which leaves islands and guilds "unreachable".
	 */
	private static final String[] TRANSPORTS = {
		"useAgilityShortcuts", "useBoats", "useCanoes", "useCharterShips", "useShips", "useFairyRings",
		"useGnomeGliders", "useHotAirBalloons", "useMagicCarpets", "useMagicMushtrees", "useMinecarts",
		"useQuetzals", "useSpiritTrees", "useTeleportationLevers", "useTeleportationPortals",
		"useTeleportationMinigames", "useSailingMoves",
	};

	private final EventBus eventBus;
	private final SkillingHelperConfig config;

	private Route shown;
	private int queryCount;
	private String pendingQuery;
	private Route unreachable;

	@Inject
	ShortestPathBridge(EventBus eventBus, SkillingHelperConfig config)
	{
		this.eventBus = eventBus;
		this.config = config;
	}

	/**
	 * Shows the path to {@code route}, or clears it when null. Runs on the client thread.
	 */
	public void show(Route route)
	{
		if (route == null)
		{
			clear();
			return;
		}
		shown = route;
		unreachable = null;

		Map<String, Object> overrides = new HashMap<>();
		// Arriving anywhere in the spot counts, not just on its exact centre tile
		overrides.put("unreachableTargetDistanceThreshold", route.reachDistance());
		if (route.entersWilderness())
		{
			overrides.put("avoidWilderness", false);
		}
		if (config.shortestPathTransports())
		{
			for (String key : TRANSPORTS)
			{
				overrides.put(key, true);
			}
		}

		Map<String, Object> path = new HashMap<>();
		path.put("target", route.pathTargets());
		path.put("config", overrides);
		eventBus.post(new PluginMessage(NAMESPACE, "path", path));

		// Queries always walk, so they can't judge a sailing route
		if (route.bySea())
		{
			pendingQuery = null;
			return;
		}
		pendingQuery = QUERY_PREFIX + (++queryCount);
		Map<String, Object> query = new HashMap<>();
		query.put("id", pendingQuery);
		query.put("target", route.pathTargets());
		eventBus.post(new PluginMessage(NAMESPACE, "query", query));
	}

	public void clear()
	{
		if (shown != null)
		{
			eventBus.post(new PluginMessage(NAMESPACE, "clear", new HashMap<>()));
		}
		shown = null;
		pendingQuery = null;
		unreachable = null;
	}

	/**
	 * Whether Shortest Path said it can't complete this route with the player's unlocks and settings.
	 */
	public boolean isUnreachable(Route route)
	{
		return route != null && route.equals(unreachable);
	}

	@Subscribe
	public void onPluginMessage(PluginMessage e)
	{
		if (!NAMESPACE.equals(e.getNamespace()) || !"result".equals(e.getName()) || e.getData() == null)
		{
			return;
		}
		Object id = e.getData().get("id");
		if (pendingQuery == null || !pendingQuery.equals(id))
		{
			return;
		}
		pendingQuery = null;
		Object reached = e.getData().get("reached");
		log.debug("Shortest Path route to {}: reached={} reason={}", shown == null ? null : shown.getPlaceName(),
			reached, e.getData().get("reason"));
		if (Boolean.FALSE.equals(reached) && !"CANCELLED".equals(e.getData().get("reason")))
		{
			unreachable = shown;
		}
	}
}
