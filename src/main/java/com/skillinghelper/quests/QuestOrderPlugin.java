package com.skillinghelper.quests;

import com.google.inject.Provides;
import com.skillinghelper.QuestHelperLink;
import com.skillinghelper.ShortestPathLink;
import com.skillinghelper.bank.BankItems;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.AchievementDiaryConfig;
import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryTier;
import com.skillinghelper.overlay.NameTargets;
import com.skillinghelper.quests.data.OrderRow;
import com.skillinghelper.quests.data.QuestData;
import com.skillinghelper.quests.data.QuestInfo;
import com.skillinghelper.quests.data.Route;
import com.skillinghelper.quests.ui.QuestOrderPanel;
import com.skillinghelper.travel.TravelAdvisor;
import com.skillinghelper.travel.TravelPick;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * Walks the player through the OSRS Wiki's optimal quest order for their account: which quests are done, which
 * they can do now, what's missing for the rest, and what to do next. Each quest is handed over to the Quest Helper
 * plugin for the click-by-click steps; this guide gets the player to the start and outlines the quest in the Quest
 * list so it's one right-click away.
 */
@PluginDescriptor(
	name = "Quest Order Guide",
	description = "The wiki's optimal quest order for your account: what to do next, what's missing, and a hand-over "
		+ "to Quest Helper for the steps",
	tags = {"quest", "quests", "order", "optimal", "guide", "helper", "questhelper", "ironman"}
)
public class QuestOrderPlugin extends Plugin implements QuestOrderPanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	/**
	 * How often quest states are read again (they're client scripts). A finished quest also triggers a read.
	 */
	private static final int QUEST_REFRESH_TICKS = 50;
	private static final int UPCOMING_ROWS = 25;
	private static final String TICKED_KEY = "ticked";

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private EventBus eventBus;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ItemManager itemManager;
	@Inject
	private PluginManager pluginManager;
	@Inject
	private ConfigManager configManager;
	@Inject
	private QuestOrderConfig config;
	@Inject
	private TravelAdvisor travel;
	@Inject
	private QuestListOverlay listOverlay;
	@Inject
	private QuestItemOverlay itemOverlay;
	@Inject
	private ModelOutlineRenderer outlineRenderer;

	private NameTargets targets;
	private QuestSceneOverlay sceneOverlay;
	private QuestOrderPanel panel;
	private NavigationButton navButton;

	private boolean running;
	/**
	 * Row the player clicked in the list, done first; null to follow the route.
	 */
	private String picked;
	private final Set<String> skipped = new HashSet<>();
	private final Map<String, QuestState> quests = new HashMap<>();
	private int questCountdown;
	private Map<Integer, Integer> bankCounts;
	private boolean checkPlugins = true;

	/**
	 * What to do right now, read by the overlays. Null when the guide isn't running.
	 */
	@Getter
	private volatile QuestStep step;
	private WorldPoint pathTarget;
	private boolean hintArrowSet;
	private QuestView shownView;

	@Override
	protected void startUp()
	{
		targets = new NameTargets(client);
		sceneOverlay = new QuestSceneOverlay(client, this, config, targets, outlineRenderer);
		eventBus.register(targets);
		overlayManager.add(sceneOverlay);
		overlayManager.add(listOverlay);
		overlayManager.add(itemOverlay);
		panel = new QuestOrderPanel(this);
		shownView = null;
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Quest Order Guide")
			.icon(icon)
			.priority(12)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		questCountdown = 0;
		checkPlugins = true;
		clientThread.invokeLater(this::refresh);
	}

	@Override
	protected void shutDown()
	{
		eventBus.unregister(targets);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(listOverlay);
		overlayManager.remove(itemOverlay);
		clientToolbar.removeNavigation(navButton);
		NameTargets t = targets;
		clientThread.invoke(() ->
		{
			clearWay();
			t.clear();
		});
		running = false;
		step = null;
		picked = null;
		skipped.clear();
		quests.clear();
		bankCounts = null;
		panel = null;
		shownView = null;
	}

	@Provides
	QuestOrderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(QuestOrderConfig.class);
	}

	// ---- Game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			questCountdown = 0;
			checkPlugins = true;
		}
		else if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			quests.clear();
			bankCounts = null;
			pathTarget = null;
			hintArrowSet = false;
			skipped.clear();
			picked = null;
			refresh();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() == ChatMessageType.GAMEMESSAGE)
		{
			String m = e.getMessage();
			if (m.contains("Quest complete") || m.contains("completed a quest") || m.contains("started a new quest"))
			{
				// Read the quest states on the next tick
				questCountdown = 0;
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (e.getContainerId() == InventoryID.BANK)
		{
			bankCounts = BankItems.count(e.getItemContainer(), itemManager);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (QuestOrderConfig.GROUP.equals(e.getGroup()) || Wilderness.isSettingChange(e))
		{
			clientThread.invokeLater(() ->
			{
				clearWay();
				refresh();
			});
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			return;
		}
		if (checkPlugins && running)
		{
			checkPlugins = false;
			if (config.warnQuestHelper())
			{
				QuestHelperLink.warnIfMissing(client, pluginManager);
			}
			if (config.shortestPath())
			{
				ShortestPathLink.warnIfMissing(client, pluginManager);
			}
		}
		if (--questCountdown <= 0)
		{
			questCountdown = QUEST_REFRESH_TICKS;
			readQuests();
		}
		refresh();
	}

	// ---- Reading the player ----

	private void readQuests()
	{
		quests.clear();
		for (String name : QuestData.quests().keySet())
		{
			Quest q = QuestData.gameQuest(name);
			if (q != null)
			{
				quests.put(name, q.getState(client));
			}
		}
	}

	private QuestPlayer readPlayer()
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> boosted = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, client.getRealSkillLevel(s));
			boosted.put(s, client.getBoostedSkillLevel(s));
		}
		Set<String> claimed = new HashSet<>();
		for (DiaryRegion r : DiaryRegion.values())
		{
			for (DiaryTier t : DiaryTier.values())
			{
				if (client.getVarbitValue(r.rewardVarbit(t)) > 0)
				{
					claimed.add(r.name() + "." + t.name());
				}
			}
		}
		Player me = client.getLocalPlayer();
		return QuestPlayer.builder()
			.quests(new HashMap<>(quests))
			.levels(levels)
			.boosted(boosted)
			.combatLevel(me == null ? 3 : me.getCombatLevel())
			.questPoints(client.getVarpValue(VarPlayerID.QP))
			.members(client.getWorldType().contains(WorldType.MEMBERS))
			.claimedDiaries(claimed)
			.ticked(ticked())
			.allowWilderness(Wilderness.allowed(configManager))
			.location(me == null ? null : WorldPoint.fromLocalInstance(client, me.getLocalLocation()))
			.questHelper(QuestHelperLink.isActive(pluginManager))
			.build();
	}

	private Route route()
	{
		switch (config.route())
		{
			case MAIN:
				return Route.MAIN;
			case IRONMAN:
				return Route.IRONMAN;
			default:
				boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
				return loggedIn && client.getVarbitValue(VarbitID.IRONMAN) > 0 ? Route.IRONMAN : Route.MAIN;
		}
	}

	// ---- The guide ----

	/**
	 * Works out the step and the panel view, and sends them on when they changed. Client thread.
	 */
	private void refresh()
	{
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
		QuestPlayer p = loggedIn ? readPlayer() : null;
		Route route = route();
		List<QuestPlanner.RowState> states = p == null ? List.of() : QuestPlanner.evaluate(route, p, skipped);
		QuestStep next = null;
		if (p != null)
		{
			if (picked != null && states.stream().anyMatch(s -> s.getRow().key().equals(picked)
				&& s.getStatus() == QuestPlanner.Status.DONE))
			{
				// The picked row is done: back to the route
				picked = null;
			}
			next = QuestPlanner.step(states, picked, p, config.shortestPath());
			WorldPoint dest = next.getDestination();
			if (running && dest != null && !client.getTopLevelWorldView().isInstance())
			{
				Map<Integer, Integer> bank = bankCounts;
				Map<Integer, Integer> carried = carried();
				List<TravelPick> picks = travel.plan(id -> carried.getOrDefault(id, 0),
					id -> bank == null ? 0 : bank.getOrDefault(id, 0), p.getLocation(), List.of(dest),
					next.getTitle(), Wilderness.contains(dest) && p.isAllowWilderness());
				if (!picks.isEmpty())
				{
					next = QuestPlanner.step(states, picked, p.toBuilder().travel(picks).build(),
						config.shortestPath());
				}
			}
		}
		step = running ? next : null;
		if (running && next != null)
		{
			targets.track(next.getNpc() == null ? Collections.emptySet() : Set.of(next.getNpc()),
				Collections.emptySet());
			showWay(next.getDestination());
		}
		else
		{
			targets.track(Collections.emptySet(), Collections.emptySet());
			clearWay();
		}
		QuestView view = buildView(p, route, states, next);
		QuestOrderPanel target = panel;
		if (target != null && !view.equals(shownView))
		{
			shownView = view;
			SwingUtilities.invokeLater(() -> target.update(view));
		}
	}

	private QuestView buildView(QuestPlayer p, Route route, List<QuestPlanner.RowState> states, QuestStep next)
	{
		int done = 0;
		List<QuestView.Row> upcoming = new ArrayList<>();
		for (QuestPlanner.RowState s : states)
		{
			if (s.getStatus() == QuestPlanner.Status.DONE)
			{
				done++;
				continue;
			}
			if (upcoming.size() >= UPCOMING_ROWS)
			{
				continue;
			}
			OrderRow row = s.getRow();
			String detail = "";
			if (row.getKind() == OrderRow.Kind.QUEST || row.getKind() == OrderRow.Kind.PARTIAL)
			{
				QuestInfo q = QuestData.quest(row.getRef());
				detail = q.getDifficulty() + ", " + q.getLength().toLowerCase();
			}
			upcoming.add(new QuestView.Row(row.key(), row.getIndex() + 1, QuestPlanner.label(row), s.getStatus(),
				s.getMissing(), s.isWilderness(), next != null && row.key().equals(next.getRowKey()), detail));
		}
		return QuestView.builder()
			.loggedIn(p != null)
			.route(route)
			.routeAuto(config.route() == QuestOrderConfig.RouteChoice.AUTO)
			.questHelper(p != null ? p.isQuestHelper() : QuestHelperLink.isActive(pluginManager))
			.done(done)
			.total(states.size())
			.questPoints(p == null ? 0 : p.getQuestPoints())
			.running(running)
			.step(next)
			.upcoming(upcoming)
			.wildernessAllowed(Wilderness.allowed(configManager))
			.build();
	}

	private Map<Integer, Integer> carried()
	{
		Map<Integer, Integer> counts = new HashMap<>();
		for (int containerId : new int[]{InventoryID.INV, InventoryID.WORN})
		{
			ItemContainer c = client.getItemContainer(containerId);
			if (c == null)
			{
				continue;
			}
			for (Item item : c.getItems())
			{
				if (item.getId() > 0)
				{
					counts.merge(itemManager.canonicalize(item.getId()), item.getQuantity(), Integer::sum);
				}
			}
		}
		return counts;
	}

	/**
	 * Points the hint arrow and Shortest Path at the quest's start, or clears them.
	 */
	private void showWay(WorldPoint target)
	{
		if (Objects.equals(target, pathTarget))
		{
			return;
		}
		clearWay();
		if (target == null)
		{
			return;
		}
		pathTarget = target;
		if (config.hintArrow())
		{
			client.setHintArrow(target);
			hintArrowSet = true;
		}
		if (config.shortestPath())
		{
			Map<String, Object> data = new HashMap<>();
			data.put("target", target);
			// Only path through the Wilderness when the place itself is there, whatever Shortest Path's setting says
			Map<String, Object> overrides = new HashMap<>();
			overrides.put("avoidWilderness", !Wilderness.contains(target));
			ShortestPathLink.allowCarriedTeleports(configManager, overrides);
			data.put("config", overrides);
			ShortestPathLink.path(eventBus, this, data);
		}
	}

	private void clearWay()
	{
		if (pathTarget != null)
		{
			ShortestPathLink.clear(eventBus, this);
		}
		if (hintArrowSet)
		{
			client.clearHintArrow();
			hintArrowSet = false;
		}
		pathTarget = null;
	}

	// ---- Ticked activities (saved per account) ----

	private Set<String> ticked()
	{
		String s = configManager.getRSProfileConfiguration(QuestOrderConfig.GROUP, TICKED_KEY);
		Set<String> out = new LinkedHashSet<>();
		if (s != null && !s.isEmpty())
		{
			Collections.addAll(out, s.split(";"));
		}
		return out;
	}

	private void setTicked(String ref, boolean on)
	{
		Set<String> t = ticked();
		if (on ? t.add(ref) : t.remove(ref))
		{
			configManager.setRSProfileConfiguration(QuestOrderConfig.GROUP, TICKED_KEY, String.join(";", t));
		}
	}

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onStartGuide()
	{
		clientThread.invokeLater(() ->
		{
			running = true;
			checkPlugins = true;
			refresh();
		});
	}

	@Override
	public void onStopGuide()
	{
		clientThread.invokeLater(() ->
		{
			running = false;
			picked = null;
			refresh();
		});
	}

	@Override
	public void onPick(String key)
	{
		clientThread.invokeLater(() ->
		{
			picked = key;
			skipped.remove(key);
			running = true;
			checkPlugins = true;
			refresh();
		});
	}

	@Override
	public void onSkip(String key)
	{
		clientThread.invokeLater(() ->
		{
			skipped.add(key);
			if (key.equals(picked))
			{
				picked = null;
			}
			refresh();
		});
	}

	@Override
	public void onMarkDone(String key)
	{
		// Activity keys are "ACTIVITY:<ref>"
		String ref = key.substring(key.indexOf(':') + 1);
		clientThread.invokeLater(() ->
		{
			setTicked(ref, true);
			refresh();
		});
	}

	@Override
	public void onRoute(QuestOrderConfig.RouteChoice choice)
	{
		configManager.setConfiguration(QuestOrderConfig.GROUP, "route", choice);
	}

	@Override
	public void onAllowWilderness(boolean allow)
	{
		Wilderness.setAllowed(configManager, allow);
	}

	@Override
	public void onShowDiary(String diary)
	{
		String[] parts = diary.split("\\.");
		configManager.setConfiguration(AchievementDiaryConfig.GROUP, "region", parts[0]);
		configManager.setConfiguration(AchievementDiaryConfig.GROUP, "tier", parts[1]);
	}

	@Override
	public void onOpenWiki(String page)
	{
		LinkBrowser.browse(WIKI_URL + page.replace(' ', '_').replace("?", "%3F").replace("&", "%26"));
	}

	@Override
	public void onOpenRoutePage()
	{
		clientThread.invokeLater(() ->
		{
			Route r = route();
			SwingUtilities.invokeLater(() -> LinkBrowser.browse(WIKI_URL + r.getWikiPage()));
		});
	}

	@Override
	public void onGetQuestHelper()
	{
		LinkBrowser.browse("https://runelite.net/plugin-hub/show/quest-helper");
	}
}
