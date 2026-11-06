package com.skillinghelper.minigame;

import com.google.inject.Provides;
import com.skillinghelper.OpenMethodRequest;
import com.skillinghelper.OpenMinigameRequest;
import com.skillinghelper.ShortestPathLink;
import com.skillinghelper.SkillingHelperPlugin;
import com.skillinghelper.StartPvmGuideRequest;
import com.skillinghelper.bank.BankCache;
import com.skillinghelper.bank.BankItems;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.minigame.data.Minigame;
import com.skillinghelper.minigame.data.Minigames;
import com.skillinghelper.minigame.data.Reward;
import com.skillinghelper.minigame.ui.MinigamePanel;
import com.skillinghelper.overlay.NameTargets;
import com.skillinghelper.pvm.PvmProgressionPlugin;
import com.skillinghelper.travel.TravelAdvisor;
import com.skillinghelper.travel.TravelPick;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
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
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
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
 * Minigame guides: what every minigame needs, which of its rewards are worth chasing (and which you already own),
 * and a step-by-step guide for each one, Quest Helper style. Minigames that already have a full guide in Skilling
 * Helper or PvM Progression hand over to it.
 */
@PluginDescriptor(
	name = "Minigame Guides",
	description = "Step-by-step minigame guides: requirements, which rewards are worth chasing, and which you own",
	tags = {"minigame", "minigames", "guide", "helper", "rewards", "tempoross", "wintertodt", "pest", "castle", "soul"}
)
public class MinigameGuidesPlugin extends Plugin implements MinigamePanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final String SELECTED_KEY = "minigame";
	/**
	 * How often quest states are read again (they're client scripts).
	 */
	private static final int QUEST_REFRESH_TICKS = 50;

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
	private MinigameGuidesConfig config;
	@Inject
	private TravelAdvisor travel;
	@Inject
	private BankCache bankCache;
	@Inject
	private MinigameItemOverlay itemOverlay;
	@Inject
	private ModelOutlineRenderer outlineRenderer;

	private NameTargets targets;
	private MinigameSceneOverlay sceneOverlay;
	private MinigamePanel panel;
	private NavigationButton navButton;
	/**
	 * Skilling Helper's methods, for the level range of minigames it guides.
	 */
	private final Map<String, Method> skillMethods = new HashMap<>();

	private Minigame selected;
	private boolean running;
	private final Map<Quest, QuestState> quests = new HashMap<>();
	private int questCountdown;
	private Map<Integer, Integer> bankCounts;
	private boolean checkShortestPath = true;

	/**
	 * What to do right now, read by the overlays. Null when no guide is running.
	 */
	@Getter
	private volatile DiaryStep step;
	private WorldPoint pathTarget;
	private boolean hintArrowSet;
	private MinigameView shownView;

	@Override
	protected void startUp()
	{
		targets = new NameTargets(client);
		sceneOverlay = new MinigameSceneOverlay(client, this, config, targets, outlineRenderer);
		eventBus.register(targets);
		overlayManager.add(sceneOverlay);
		overlayManager.add(itemOverlay);
		skillMethods.clear();
		for (Method m : new GuideRepository().all())
		{
			skillMethods.put(m.getId(), m);
		}
		selected = savedPick();
		panel = new MinigamePanel(this);
		shownView = null;
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Minigame Guides")
			.icon(icon)
			.priority(16)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		questCountdown = 0;
		checkShortestPath = true;
		clientThread.invokeLater(this::refresh);
	}

	@Override
	protected void shutDown()
	{
		eventBus.unregister(targets);
		overlayManager.remove(sceneOverlay);
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
		bankCounts = null;
		quests.clear();
		skillMethods.clear();
		panel = null;
		shownView = null;
	}

	@Provides
	MinigameGuidesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MinigameGuidesConfig.class);
	}

	// ---- Game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			questCountdown = 0;
			checkShortestPath = true;
		}
		else if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			quests.clear();
			bankCounts = null;
			pathTarget = null;
			hintArrowSet = false;
			refresh();
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
		if (MinigameGuidesConfig.GROUP.equals(e.getGroup()) || Wilderness.isSettingChange(e))
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
		if (checkShortestPath)
		{
			checkShortestPath = false;
			if (config.shortestPath() && running)
			{
				ShortestPathLink.warnIfMissing(client, pluginManager);
			}
		}
		if (--questCountdown <= 0)
		{
			questCountdown = QUEST_REFRESH_TICKS;
			readQuests();
			if (bankCounts == null && bankCache.get() == null)
			{
				// The bank Skilling Helper saved for this account, so owned rewards show before the bank is opened
				bankCache.load();
			}
		}
		refresh();
	}

	/**
	 * Another plugin in this jar asked to show one minigame: select it, start its guide and open the panel.
	 */
	@Subscribe
	public void onOpenMinigameRequest(OpenMinigameRequest e)
	{
		Minigame m = Minigames.byId(e.getMinigameId());
		if (m == null)
		{
			return;
		}
		configManager.setConfiguration(MinigameGuidesConfig.GROUP, SELECTED_KEY, m.getId());
		selected = m;
		// Start it straight away (the Account Planner's guided route relies on that); minigames whose full guide is
		// in another plugin hand over to it, the rest run here
		if (m.getSkillingMethod() != null)
		{
			running = false;
			eventBus.post(new OpenMethodRequest(m.getSkillingMethod()));
		}
		else if (m.getPvmContent() != null)
		{
			running = false;
			eventBus.post(new StartPvmGuideRequest(m.getPvmContent()));
		}
		else
		{
			running = true;
			checkShortestPath = true;
		}
		refresh();
		SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
	}

	// ---- Reading the player ----

	private void readQuests()
	{
		quests.clear();
		Set<Quest> wanted = new HashSet<>();
		for (Minigame m : Minigames.all())
		{
			wanted.addAll(m.getQuests());
			wanted.addAll(m.getQuestsStarted());
		}
		for (Quest q : wanted)
		{
			quests.put(q, q.getState(client));
		}
	}

	private Map<Integer, Integer> bank()
	{
		if (bankCounts != null)
		{
			return new HashMap<>(bankCounts);
		}
		ItemCounts saved = bankCache.get();
		return saved == null ? null : new HashMap<>(saved.asMap());
	}

	private DiaryPlayer readPlayer()
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> boosted = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			if ("Overall".equals(s.getName()))
			{
				continue;
			}
			levels.put(s, client.getRealSkillLevel(s));
			boosted.put(s, client.getBoostedSkillLevel(s));
		}
		Player me = client.getLocalPlayer();
		Widget bankWidget = client.getWidget(InterfaceID.Bankmain.ITEMS);
		return DiaryPlayer.builder()
			.location(me == null ? null : WorldPoint.fromLocalInstance(client, me.getLocalLocation()))
			.levels(levels)
			.boosted(boosted)
			.combatLevel(me == null ? 3 : me.getCombatLevel())
			.quests(new HashMap<>(quests))
			.varbit(client::getVarbitValue)
			.varp(client::getVarpValue)
			.carried(carried())
			.bank(bank())
			.bankOpen(bankWidget != null && !bankWidget.isHidden())
			.members(client.getWorldType().contains(WorldType.MEMBERS))
			.allowWilderness(Wilderness.allowed(configManager))
			.preferShops(config.preferShops())
			.buyPrice(id -> (int) Math.min(Integer.MAX_VALUE, itemManager.getItemPrice(id)))
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

	// ---- The guide ----

	/**
	 * Works out the step (if a guide runs) and the panel view, and sends them on when they changed. Client thread.
	 */
	private void refresh()
	{
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
		DiaryPlayer p = loggedIn ? readPlayer() : null;
		DiaryStep next = null;
		if (running && p != null && selected != null && selected.guidedHere())
		{
			boolean instance = client.getTopLevelWorldView().isInstance();
			next = MinigameGuide.resolve(selected, p, instance);
			Location dest = next.getDestination();
			if (dest != null && !instance)
			{
				Map<Integer, Integer> bank = p.getBank();
				List<TravelPick> picks = travel.plan(id -> p.getCarried().getOrDefault(id, 0),
					id -> bank == null ? 0 : bank.getOrDefault(id, 0), p.getLocation(),
					List.of(dest.getPoint()), dest.getName(), Wilderness.contains(dest) && p.isAllowWilderness());
				if (!picks.isEmpty())
				{
					next = MinigameGuide.resolve(selected, p.toBuilder().travel(picks).build(), instance);
				}
			}
		}
		step = next;
		if (next != null)
		{
			targets.track(next.getNpcs(), next.getObjects());
			showWay(next.getDestination());
		}
		else
		{
			targets.track(Collections.emptySet(), Collections.emptySet());
			clearWay();
		}
		MinigameView view = buildView(p, next);
		MinigamePanel target = panel;
		if (target != null && !view.equals(shownView))
		{
			shownView = view;
			SwingUtilities.invokeLater(() -> target.update(view));
		}
	}

	private MinigameView buildView(DiaryPlayer p, DiaryStep current)
	{
		MinigameView.MinigameViewBuilder b = MinigameView.builder()
			.loggedIn(p != null)
			.bankKnown(p != null && p.getBank() != null)
			.running(running)
			.step(current)
			.wildernessAllowed(Wilderness.allowed(configManager));
		List<MinigameView.Row> rows = new ArrayList<>();
		if (p != null)
		{
			for (Minigame m : Minigames.all())
			{
				int[] owned = MinigameGuide.ownedCount(m, p);
				rows.add(new MinigameView.Row(m.getId(), m.getName(), m.getCategory().getLabel(),
					MinigameGuide.missing(m, p).isEmpty(), owned[0], owned[1]));
			}
		}
		b.rows(rows);

		Minigame m = selected;
		if (m == null)
		{
			return b.missing(List.of()).met(List.of()).notes(List.of()).bring(List.of()).balances(List.of())
				.rewards(List.of()).handoff(MinigameView.Handoff.NONE).build();
		}
		MinigameView.Handoff handoff = m.getSkillingMethod() != null ? MinigameView.Handoff.SKILLING_HELPER
			: m.getPvmContent() != null ? MinigameView.Handoff.PVM_PROGRESSION : MinigameView.Handoff.NONE;
		boolean handoffActive = handoff == MinigameView.Handoff.SKILLING_HELPER ? isActive(SkillingHelperPlugin.class)
			: handoff == MinigameView.Handoff.PVM_PROGRESSION && isActive(PvmProgressionPlugin.class);

		List<String> bring = new ArrayList<>();
		for (DiaryItem item : m.getItems())
		{
			bring.add((item.getAmount() > 1 ? String.format("%,d", item.getAmount()) + " x " : "") + item.getName());
		}
		List<MinigameView.RewardRow> rewards = new ArrayList<>();
		List<Reward> affordable = p == null ? List.of() : MinigameGuide.affordable(m, p);
		for (Reward r : m.getRewards())
		{
			Boolean owned = p == null || !r.checkable() ? null : MinigameGuide.owned(r, p);
			rewards.add(new MinigameView.RewardRow(r.getName(), r.getWhy(), r.getCost(), r.isWorth(), owned,
				affordable.contains(r)));
		}
		return b.id(m.getId())
			.name(m.getName())
			.summary(m.getSummary())
			.levelRange(levelRange(m))
			.team(m.getTeam())
			.dangerous(m.isDangerous())
			.wilderness(m.isWilderness())
			.getThere(m.getGetThere())
			.missing(p == null ? List.of() : MinigameGuide.missing(m, p))
			.met(p == null ? List.of() : met(m, p))
			.notes(m.getNotes())
			.bring(bring)
			.balances(p == null ? List.of() : MinigameGuide.balances(m, p))
			.rewards(rewards)
			.handoff(handoff)
			.handoffActive(handoffActive)
			.guidedHere(m.guidedHere())
			.build();
	}

	/**
	 * The requirements the player meets, for the ticked list.
	 */
	private static List<String> met(Minigame m, DiaryPlayer p)
	{
		List<String> missing = MinigameGuide.missing(m, p);
		List<String> out = new ArrayList<>();
		m.getSkills().forEach((s, lvl) ->
		{
			String label = lvl + " " + s.getName();
			if (missing.stream().noneMatch(x -> x.startsWith(label + " ")))
			{
				out.add(label);
			}
		});
		if (m.getCombat() > 0 && missing.stream().noneMatch(x -> x.startsWith("Combat level")))
		{
			out.add("Combat level " + m.getCombat());
		}
		if (m.getTotalLevel() > 0 && missing.stream().noneMatch(x -> x.startsWith("Total level")))
		{
			out.add("Total level " + m.getTotalLevel());
		}
		for (Quest q : m.getQuests())
		{
			if (!missing.contains("Quest: " + q.getName()))
			{
				out.add("Quest: " + q.getName());
			}
		}
		for (Quest q : m.getQuestsStarted())
		{
			if (!missing.contains("Start the quest " + q.getName()))
			{
				out.add("Started: " + q.getName());
			}
		}
		return out;
	}

	/**
	 * "Lvl 50 → 99 Firemaking": Skilling Helper's range for minigames it guides, else the minimum to 99.
	 */
	private String levelRange(Minigame m)
	{
		if (m.getTrains() == null)
		{
			return null;
		}
		Method method = m.getSkillingMethod() == null ? null : skillMethods.get(m.getSkillingMethod());
		String range = method != null ? method.levelRange() : "Lvl " + m.minLevel() + " → 99";
		return range + " " + m.getTrains().getName();
	}

	private boolean isActive(Class<? extends Plugin> type)
	{
		for (Plugin p : pluginManager.getPlugins())
		{
			if (type.isInstance(p))
			{
				return pluginManager.isPluginActive(p);
			}
		}
		return false;
	}

	/**
	 * Points the hint arrow and Shortest Path at the step's destination, or clears them.
	 */
	private void showWay(Location destination)
	{
		WorldPoint target = destination == null ? null : destination.getPoint();
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

	private Minigame savedPick()
	{
		String id = configManager.getConfiguration(MinigameGuidesConfig.GROUP, SELECTED_KEY);
		Minigame m = id == null ? null : Minigames.byId(id);
		return m != null ? m : Minigames.all().get(0);
	}

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onSelect(String id)
	{
		Minigame m = Minigames.byId(id);
		if (m == null)
		{
			return;
		}
		configManager.setConfiguration(MinigameGuidesConfig.GROUP, SELECTED_KEY, id);
		clientThread.invokeLater(() ->
		{
			if (m != selected)
			{
				running = false;
			}
			selected = m;
			refresh();
		});
	}

	@Override
	public void onStartGuide()
	{
		clientThread.invokeLater(() ->
		{
			Minigame m = selected;
			if (m == null)
			{
				return;
			}
			if (m.getSkillingMethod() != null)
			{
				eventBus.post(new OpenMethodRequest(m.getSkillingMethod()));
				return;
			}
			if (m.getPvmContent() != null)
			{
				eventBus.post(new StartPvmGuideRequest(m.getPvmContent()));
				return;
			}
			running = true;
			checkShortestPath = true;
			refresh();
		});
	}

	@Override
	public void onStopGuide()
	{
		clientThread.invokeLater(() ->
		{
			running = false;
			refresh();
		});
	}

	@Override
	public void onAllowWilderness(boolean allow)
	{
		Wilderness.setAllowed(configManager, allow);
	}

	@Override
	public void onOpenWiki()
	{
		Minigame m = selected;
		if (m != null)
		{
			LinkBrowser.browse(WIKI_URL + m.getWikiPage());
		}
	}
}
