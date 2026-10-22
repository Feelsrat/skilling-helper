package com.skillinghelper;

import com.skillinghelper.engine.Route;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Shows where the current step goes: the game's hint arrow, a world map marker and, if installed, the Shortest Path
 * plugin's route. Runs on the client thread.
 */
@Singleton
class PathDisplay
{
	private final Client client;
	private final SkillingHelperConfig config;
	private final WorldMapPointManager worldMapPointManager;
	private final ShortestPathBridge shortestPath;

	private Route shown;
	private boolean hintArrowSet;
	private WorldMapPoint mapPoint;

	@Inject
	PathDisplay(Client client, SkillingHelperConfig config, WorldMapPointManager worldMapPointManager,
		ShortestPathBridge shortestPath)
	{
		this.client = client;
		this.config = config;
		this.worldMapPointManager = worldMapPointManager;
		this.shortestPath = shortestPath;
	}

	/**
	 * Points everything at this route, or clears it all when null. Does nothing if it's already shown.
	 */
	void show(Route route)
	{
		if (Objects.equals(route, shown))
		{
			return;
		}
		shown = route;
		WorldPoint destination = route == null ? null : route.getPoint();
		showHintArrow(destination);
		showMapPoint(destination, route == null ? null : route.getStopName());
		shortestPath.show(config.shortestPath() ? route : null);
	}

	/**
	 * Clears everything, including a hint arrow left from before the settings changed.
	 */
	void clear()
	{
		Route previous = shown;
		show(null);
		if (previous == null)
		{
			clearHintArrow();
		}
	}

	/**
	 * Whether Shortest Path said it can't complete this route with the player's unlocks.
	 */
	boolean isUnreachable(Route route)
	{
		return shortestPath.isUnreachable(route);
	}

	private void showHintArrow(WorldPoint destination)
	{
		clearHintArrow();
		if (destination != null && config.hintArrow())
		{
			client.setHintArrow(destination);
			hintArrowSet = true;
		}
	}

	private void clearHintArrow()
	{
		if (hintArrowSet)
		{
			client.clearHintArrow();
			hintArrowSet = false;
		}
	}

	private void showMapPoint(WorldPoint destination, String name)
	{
		if (mapPoint != null)
		{
			worldMapPointManager.remove(mapPoint);
			mapPoint = null;
		}
		if (destination != null && config.worldMapPoint())
		{
			mapPoint = WorldMapPoint.builder()
				.worldPoint(destination)
				.image(MapArrow.image())
				.name(name)
				.tooltip("Skilling Helper: " + name)
				.snapToEdge(true)
				.jumpOnClick(true)
				.build();
			worldMapPointManager.add(mapPoint);
		}
	}
}
