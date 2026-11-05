package com.skillinghelper;

import com.skillinghelper.engine.Route;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
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
	private static final String NAMESPACE = ShortestPathLink.NAMESPACE;

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
	/**
	 * For the shared travel item setting, or null in tests.
	 */
	private final ConfigManager configManager;
	/**
	 * Starts this bridge's query ids, so two plugins in this jar don't take each other's answers.
	 */
	private final String queryPrefix;

	private Route shown;
	/**
	 * What was last sent to Shortest Path, so walking around (which only reorders the stops, nearest first) doesn't
	 * make it search again from scratch.
	 */
	private List<Object> sentKey;
	private int queryCount;
	private String pendingQuery;
	/**
	 * The {@link #sendKey} of the route Shortest Path said it can't complete, or null.
	 */
	private List<Object> unreachable;

	@Inject
	ShortestPathBridge(EventBus eventBus, SkillingHelperConfig config, ConfigManager configManager)
	{
		this(eventBus, config, configManager, "skillinghelper:");
	}

	/**
	 * For another plugin in this jar that shows its own paths.
	 */
	public ShortestPathBridge(EventBus eventBus, SkillingHelperConfig config, ConfigManager configManager,
		String queryPrefix)
	{
		this.eventBus = eventBus;
		this.config = config;
		this.configManager = configManager;
		this.queryPrefix = queryPrefix;
	}

	ShortestPathBridge(EventBus eventBus, SkillingHelperConfig config, String queryPrefix)
	{
		this(eventBus, config, null, queryPrefix);
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
		List<Object> next = sendKey(route, config.shortestPathTransports());
		shown = route;
		if (next.equals(sentKey) && ShortestPathLink.ownedBy(this))
		{
			// Same targets and settings: Shortest Path is already showing (or working out) this path
			return;
		}
		sentKey = next;
		unreachable = null;

		Map<String, Object> overrides = new HashMap<>();
		// Arriving anywhere in the spot counts, not just on its exact centre tile
		overrides.put("unreachableTargetDistanceThreshold", route.reachDistance());
		// Only path into the Wilderness when the place itself is there, even if Shortest Path's own setting allows it
		overrides.put("avoidWilderness", !route.entersWilderness());
		if (config.shortestPathTransports())
		{
			for (String key : TRANSPORTS)
			{
				overrides.put(key, true);
			}
		}

		if (configManager != null)
		{
			ShortestPathLink.allowCarriedTeleports(configManager, overrides);
		}

		Map<String, Object> path = new HashMap<>();
		path.put("target", route.pathTargets());
		path.put("config", overrides);
		ShortestPathLink.path(eventBus, this, path);

		// Queries always walk, so they can't judge a sailing route
		if (route.bySea())
		{
			pendingQuery = null;
			return;
		}
		pendingQuery = queryPrefix + (++queryCount);
		Map<String, Object> query = new HashMap<>();
		query.put("id", pendingQuery);
		query.put("target", route.pathTargets());
		eventBus.post(new PluginMessage(NAMESPACE, "query", query));
	}

	public void clear()
	{
		if (shown != null)
		{
			ShortestPathLink.clear(eventBus, this);
		}
		shown = null;
		sentKey = null;
		pendingQuery = null;
		unreachable = null;
	}

	/**
	 * Everything that decides what Shortest Path is asked: the route's kind and its stops in any order (the targets
	 * are sent as one set, and the reach and Wilderness settings follow from the stops), plus the transport setting.
	 */
	static List<Object> sendKey(Route route, boolean transports)
	{
		return List.of(route.getKind(), new HashSet<>(route.getStops()), transports);
	}

	/**
	 * Whether Shortest Path said it can't complete this route with the player's unlocks and settings.
	 */
	public boolean isUnreachable(Route route)
	{
		return route != null && unreachable != null && unreachable.equals(sendKey(route, config.shortestPathTransports()));
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
			unreachable = sentKey;
		}
	}
}
