package com.skillinghelper;

import com.skillinghelper.data.Location;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Route;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class ShortestPathBridgeTest
{
	private static final Location WEST = Location.of("Spot", 3000, 3000, 6);
	private static final Location EAST = Location.of("Spot", 3100, 3000, 6);

	private final EventBus eventBus = new EventBus();
	private final List<String> sent = new ArrayList<>();
	private final SkillingHelperConfig config = new SkillingHelperConfig()
	{
	};

	@Subscribe
	public void onPluginMessage(PluginMessage e)
	{
		if (ShortestPathLink.NAMESPACE.equals(e.getNamespace()) && !"result".equals(e.getName()))
		{
			sent.add(e.getName());
		}
	}

	@Before
	public void setUp()
	{
		ShortestPathLink.reset();
		eventBus.register(this);
	}

	private static Route routeFrom(int x)
	{
		PlayerState s = PlayerState.builder().location(new WorldPoint(x, 3000, 0)).build();
		return Route.to(s, List.of(WEST, EAST));
	}

	private void answer(String id, boolean reached)
	{
		Map<String, Object> data = new HashMap<>();
		data.put("id", id);
		data.put("reached", reached);
		eventBus.post(new PluginMessage(ShortestPathLink.NAMESPACE, "result", data));
	}

	@Test
	public void walkingAroundDoesNotMakeShortestPathSearchAgain()
	{
		ShortestPathBridge bridge = new ShortestPathBridge(eventBus, config, "test:");
		eventBus.register(bridge);
		Route nearWest = routeFrom(2990);
		Route nearEast = routeFrom(3110);
		// Same stops, nearest first: only the order differs
		assertNotEquals(nearWest, nearEast);

		bridge.show(nearWest);
		assertEquals(List.of("path", "query"), sent);
		answer("test:1", false);
		assertTrue(bridge.isUnreachable(nearWest));

		bridge.show(nearEast);
		assertEquals("nothing new is sent", List.of("path", "query"), sent);
		assertTrue("still known to be unreachable", bridge.isUnreachable(nearEast));

		bridge.show(Route.to(PlayerState.builder().build(), WEST));
		assertEquals(List.of("path", "query", "path", "query"), sent);
		assertFalse(bridge.isUnreachable(nearEast));
	}

	@Test
	public void clearingOnlyRemovesYourOwnPath()
	{
		ShortestPathBridge skilling = new ShortestPathBridge(eventBus, config, "a:");
		ShortestPathBridge other = new ShortestPathBridge(eventBus, config, "b:");
		skilling.show(routeFrom(2990));
		other.show(routeFrom(2990));
		sent.clear();

		// The other guide's path is showing now, so stopping this one leaves it alone
		skilling.clear();
		assertEquals(List.of(), sent);
		other.clear();
		assertEquals(List.of("clear"), sent);
		other.clear();
		assertEquals(List.of("clear"), sent);
	}

	@Test
	public void showsAgainAfterAnotherGuideTookOver()
	{
		ShortestPathBridge skilling = new ShortestPathBridge(eventBus, config, "a:");
		Route route = routeFrom(2990);
		skilling.show(route);
		Object money = new Object();
		ShortestPathLink.path(eventBus, money, new HashMap<>());
		sent.clear();

		skilling.show(route);
		assertEquals(List.of("path", "query"), sent);
	}

	@Test
	public void recognisesShortestPath()
	{
		assertTrue(ShortestPathLink.isShortestPath("shortestpath.ShortestPathPlugin", "Shortest Path"));
		assertTrue(ShortestPathLink.isShortestPath("other.Name", "Shortest Path"));
		assertFalse(ShortestPathLink.isShortestPath("com.example.QuestHelperPlugin", "Quest Helper"));
	}
}
