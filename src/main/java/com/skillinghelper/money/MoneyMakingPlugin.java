package com.skillinghelper.money;

import com.google.inject.Provides;
import com.skillinghelper.OpenMethodRequest;
import com.skillinghelper.OpenMoneyMakingRequest;
import com.skillinghelper.ShortestPathLink;
import com.skillinghelper.SkillingHelperPlugin;
import com.skillinghelper.StartPvmGuideRequest;
import com.skillinghelper.bank.BankItems;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.Format;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.money.data.MoneyItem;
import com.skillinghelper.money.data.MoneyMethod;
import com.skillinghelper.money.data.MoneyMethods;
import com.skillinghelper.money.ui.MoneyMakingPanel;
import com.skillinghelper.prices.WikiPriceClient;
import com.skillinghelper.pvm.PvmProgressionPlugin;
import com.skillinghelper.travel.TravelAdvisor;
import com.skillinghelper.travel.TravelPick;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import net.runelite.api.widgets.Widget;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
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
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * Every money making method from the OSRS Wiki's money making guide, filtered to what the account can do and sorted
 * by profit at today's GE prices, with a step-by-step guide for the one picked: supplies, the way there, and the
 * wiki's instructions. Methods that Skilling Helper or PvM Progression have a click-by-click guide for open there.
 */
@PluginDescriptor(
	name = "Money Making",
	description = "The OSRS Wiki money making guides you can do right now, sorted by gp/hr, with step-by-step guides",
	tags = {"money", "gold", "gp", "profit", "money making", "guide", "wiki", "moneymaking"}
)
public class MoneyMakingPlugin extends Plugin implements MoneyMakingPanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	/**
	 * How often quests are read again (they're client scripts) and the list re-sorted for price changes.
	 */
	private static final int REFRESH_TICKS = 100;

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
	private GuideRepository skillGuides;
	@Inject
	private MoneyMakingConfig config;
	@Inject
	private ConfigManager configManager;
	@Inject
	private TravelAdvisor travel;
	@Inject
	private MoneyTargets targets;
	@Inject
	private MoneySceneOverlay sceneOverlay;
	@Inject
	private MoneyItemOverlay itemOverlay;
	@Inject
	private MoneyBankFilter bankFilter;
	@Inject
	private WikiPriceClient wikiPrices;
	@Inject
	private WikiVolumeClient wikiVolumes;

	private MoneyMakingPanel panel;
	private NavigationButton navButton;
	private List<MoneyMethod> methods = Collections.emptyList();
	/**
	 * Every quest the methods name that RuneLite knows, by lower-case name.
	 */
	private final Map<String, Quest> quests = new HashMap<>();
	private final Set<String> questsDone = new HashSet<>();
	/**
	 * Skilling Helper methods that are wiki money making guides, by wiki page name.
	 */
	private final Map<String, Method> skillMethods = new HashMap<>();
	private int refreshCountdown;
	private boolean rankDirty = true;
	/**
	 * The ranking the panel was last sent, to skip sending the same again.
	 */
	private List<MoneyOption> shownRanking;
	private Map<String, String> shownFullGuides;
	/**
	 * Logged in: check on the next tick that Shortest Path is there.
	 */
	private boolean checkShortestPath = true;
	private List<MoneyOption> ranked = Collections.emptyList();
	private MoneyPlayer player = MoneyPlayer.NONE;

	// ---- The running guide ----

	private MoneyMethod guide;
	/**
	 * The player reached the spot once, or said they have everything: stop asking for supplies.
	 */
	private boolean suppliesDone;
	/**
	 * What to do right now, read by the overlays. Null when no guide is running.
	 */
	@Getter
	private volatile MoneyStep step;
	private WorldPoint pathTarget;
	private boolean hintArrowSet;
	/**
	 * Supplies of the running guide, for the bank button. Empty when no guide is running.
	 */
	@Getter
	private volatile Set<Integer> bankItems = Set.of();
	/**
	 * Bank contents from the last time it was open this session, or null if it hasn't been.
	 */
	private Map<Integer, Integer> bankCounts;
	private final Map<Integer, Boolean> stackable = new HashMap<>();

	/**
	 * Ticks until live wiki prices are fetched again, when they're turned on.
	 */
	private int priceCountdown;
	private int volumeCountdown;

	/**
	 * Supplies at the instant-buy price and products at the instant-sell price when live wiki prices are on, else
	 * RuneLite's GE guide price for both.
	 */
	private final PriceLookup prices = new PriceLookup()
	{
		@Override
		public int price(int itemId)
		{
			WikiPriceClient.Price live = live(itemId);
			return live != null && live.getHigh() > 0 ? live.getHigh() : guide(itemId);
		}

		@Override
		public int sellPrice(int itemId)
		{
			WikiPriceClient.Price live = live(itemId);
			return live != null && live.getLow() > 0 ? live.getLow() : guide(itemId);
		}

		private WikiPriceClient.Price live(int itemId)
		{
			return config.livePrices() ? wikiPrices.get(itemManager.canonicalize(itemId)) : null;
		}

		private int guide(int itemId)
		{
			return (int) Math.min(Integer.MAX_VALUE, itemManager.getItemPrice(itemId));
		}
	};

	@Override
	protected void startUp()
	{
		methods = MoneyMethods.all();
		quests.clear();
		Map<String, Quest> byName = new HashMap<>();
		for (Quest q : Quest.values())
		{
			byName.put(q.getName().toLowerCase(Locale.ROOT), q);
		}
		for (MoneyMethod m : methods)
		{
			for (String name : m.getQuests())
			{
				Quest q = byName.get(name.toLowerCase(Locale.ROOT));
				if (q != null)
				{
					quests.put(name.toLowerCase(Locale.ROOT), q);
				}
			}
		}
		skillMethods.clear();
		for (Method m : skillGuides.all())
		{
			if (m.getWikiPage() != null && m.getWikiPage().startsWith("Money_making_guide/"))
			{
				skillMethods.putIfAbsent(m.getWikiPage().replace('_', ' '), m);
			}
		}

		eventBus.register(targets);
		bankFilter.startUp();
		eventBus.register(bankFilter);
		overlayManager.add(sceneOverlay);
		overlayManager.add(itemOverlay);
		panel = new MoneyMakingPanel(this);
		shownRanking = null;
		shownFullGuides = null;
		panel.setWildernessAllowed(Wilderness.allowed(configManager));
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Money Making")
			.icon(icon)
			.priority(9)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		rankDirty = true;
		refreshCountdown = 0;
		checkShortestPath = true;
	}

	@Override
	protected void shutDown()
	{
		eventBus.unregister(targets);
		eventBus.unregister(bankFilter);
		bankFilter.shutDown();
		wikiVolumes.cancel();
		wikiVolumes.clear();
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(itemOverlay);
		clientToolbar.removeNavigation(navButton);
		clientThread.invoke(() ->
		{
			clearWay();
			targets.clear();
		});
		guide = null;
		step = null;
		bankItems = Set.of();
		bankCounts = null;
		panel = null;
		ranked = Collections.emptyList();
	}

	@Provides
	MoneyMakingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MoneyMakingConfig.class);
	}

	// ---- Game events ----

	/**
	 * Another plugin in this jar (Account Planner) needs the player to make some gold: open the list.
	 */
	@Subscribe
	public void onOpenMoneyMakingRequest(OpenMoneyMakingRequest e)
	{
		if (e.getGoal() > 0)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Money Making: pick a money maker to make about "
				+ Format.gp(e.getGoal()) + " for your next planned step.", null);
		}
		SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			rankDirty = true;
			refreshCountdown = 0;
			checkShortestPath = true;
		}
		else if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			questsDone.clear();
			bankCounts = null;
			pathTarget = null;
			hintArrowSet = false;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		if (player.level(e.getSkill()) != e.getLevel())
		{
			rankDirty = true;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (e.getContainerId() != InventoryID.BANK)
		{
			return;
		}
		Map<Integer, Integer> counts = BankItems.count(e.getItemContainer(), itemManager);
		boolean first = bankCounts == null;
		bankCounts = counts;
		if (first)
		{
			// Now gold and owned supplies are known, so affordability can be checked
			rankDirty = true;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (Wilderness.isSettingChange(e))
		{
			boolean allowed = Wilderness.allowed(configManager);
			MoneyMakingPanel p = panel;
			if (p != null)
			{
				SwingUtilities.invokeLater(() -> p.setWildernessAllowed(allowed));
			}
			return;
		}
		if (MoneyMakingConfig.GROUP.equals(e.getGroup()))
		{
			if ("livePrices".equals(e.getKey()))
			{
				priceCountdown = 0;
				volumeCountdown = 0;
				refreshCountdown = 3;
				if (!config.livePrices())
				{
					wikiVolumes.cancel();
					wikiVolumes.clear();
				}
			}
			clientThread.invokeLater(() ->
			{
				clearWay();
				rankDirty = true;
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
			if (config.shortestPath())
			{
				ShortestPathLink.warnIfMissing(client, pluginManager);
			}
		}
		if (config.livePrices() && --priceCountdown <= 0)
		{
			// About every 5 minutes; the next ranking picks the new prices up
			priceCountdown = REFRESH_TICKS * 5;
			wikiPrices.refresh();
		}
		if (config.livePrices() && --volumeCountdown <= 0)
		{
			// Daily volumes barely move, so every half hour is plenty
			volumeCountdown = REFRESH_TICKS * 30;
			wikiVolumes.refresh();
		}
		if (--refreshCountdown <= 0)
		{
			refreshCountdown = REFRESH_TICKS;
			readQuests();
			rankDirty = true;
		}
		if (rankDirty)
		{
			rankDirty = false;
			rank();
		}
		updateGuide();
	}

	// ---- Ranking ----

	private void readQuests()
	{
		questsDone.clear();
		for (Map.Entry<String, Quest> e : quests.entrySet())
		{
			if (e.getValue().getState(client) == QuestState.FINISHED)
			{
				questsDone.add(e.getKey());
			}
		}
	}

	private void rank()
	{
		MoneyPlayer.MoneyPlayerBuilder b = MoneyPlayer.builder()
			.questsDone(new HashSet<>(questsDone))
			.knownQuests(new HashSet<>(quests.keySet()))
			.members(client.getWorldType().contains(WorldType.MEMBERS))
			.stackable(stackableInputs());
		if (bankCounts != null)
		{
			Map<Integer, Integer> owned = new HashMap<>(bankCounts);
			carried().forEach((id, n) -> owned.merge(id, n, Integer::sum));
			b.owned(owned).coins(owned.getOrDefault(ItemID.COINS, 0));
		}
		for (Skill s : Skill.values())
		{
			b.level(s, client.getRealSkillLevel(s));
		}
		player = b.build();
		ranked = MoneyRanker.rank(methods, player, prices,
			config.livePrices() ? wikiVolumes.get() : Collections.emptyMap());
		boolean skillOn = isActive(SkillingHelperPlugin.class);
		boolean pvmOn = isActive(PvmProgressionPlugin.class);
		Map<String, String> fullGuides = new HashMap<>();
		for (MoneyMethod m : methods)
		{
			String label = fullGuideLabel(m, skillOn, pvmOn);
			if (label != null)
			{
				fullGuides.put(m.getPage(), label);
			}
		}
		final List<MoneyOption> list = ranked;
		final MoneyMakingPanel p = panel;
		// Re-ranked every minute, but the list only needs rebuilding (on the Swing thread) when it changed
		if (p != null && !(list.equals(shownRanking) && fullGuides.equals(shownFullGuides)))
		{
			shownRanking = list;
			shownFullGuides = fullGuides;
			SwingUtilities.invokeLater(() -> p.update(list, fullGuides));
		}
	}

	/**
	 * Which plugin has a click-by-click guide for this method, or null.
	 */
	private String fullGuideLabel(MoneyMethod m, boolean skillOn, boolean pvmOn)
	{
		Method skill = skillMethods.get(m.getPage());
		// Skilling Helper guides stop at the goal level, so at 99 the simple guide here is the one that keeps going
		if (skillOn && skill != null && client.getRealSkillLevel(skill.getSkill()) < 99)
		{
			return "Skilling Helper";
		}
		if (pvmOn && pvmContent(m) != null)
		{
			return "PvM Progression";
		}
		return null;
	}

	private static String pvmContent(MoneyMethod m)
	{
		return FullGuides.pvmContent(m.getPage());
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

	// ---- The running guide ----

	private void updateGuide()
	{
		MoneyMethod running = guide;
		if (running == null)
		{
			return;
		}
		MoneyOption option = null;
		for (MoneyOption o : ranked)
		{
			if (o.getMethod() == running)
			{
				option = o;
			}
		}
		if (option == null)
		{
			option = MoneyRanker.evaluate(running, player, prices,
				config.livePrices() ? wikiVolumes.get() : Collections.emptyMap());
		}
		Player me = client.getLocalPlayer();
		WorldPoint here = me == null ? null : WorldPoint.fromLocalInstance(client, me.getLocalLocation());
		if (running.getSpot() != null && running.getSpot().contains(here))
		{
			suppliesDone = true;
		}
		Widget bankWidget = client.getWidget(InterfaceID.Bankmain.ITEMS);
		boolean bankOpen = bankWidget != null && !bankWidget.isHidden();
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		Map<Integer, Integer> carried = carried();
		Location spot = running.getSpot();
		Map<Integer, Integer> bank = bankCounts;
		List<TravelPick> picks = spot == null || client.getTopLevelWorldView().isInstance() ? List.of()
			: travel.plan(id -> carried.getOrDefault(id, 0), id -> bank == null ? 0 : bank.getOrDefault(id, 0),
				here, List.of(spot.getPoint()), spot.getName(), Wilderness.contains(spot));
		MoneyStep next = MoneyGuide.resolve(option, MoneyGuide.Player.builder()
			.location(here)
			.carried(carried)
			.travel(picks)
			.bank(bankCounts == null ? null : new HashMap<>(bankCounts))
			.bankOpen(bankOpen)
			.inventoryFull(inv != null && inv.count() >= MoneyGuide.INVENTORY_SLOTS)
			.members(player.isMembers())
			.stackable(stackableSupplies(running))
			.suppliesDone(suppliesDone)
			.preferShops(config.preferShops())
			.buyPrice(prices::price)
			.build());
		MoneyStep previous = step;
		step = next;
		targets.track(next.getNpcs(), next.getObjects());
		showWay(next.getDestination(), running);
		Set<Integer> wanted = new HashSet<>(inputIds(running));
		for (TravelPick pick : picks)
		{
			if (!pick.isCarried())
			{
				wanted.add(pick.getItemId());
			}
		}
		setBankItems(wanted);
		if (!Objects.equals(previous, next))
		{
			final MoneyMakingPanel p = panel;
			if (p != null)
			{
				SwingUtilities.invokeLater(() -> p.updateStep(running.getPage(), next));
			}
		}
	}

	/**
	 * Which of the method's supplies stack in one slot, read from the item definitions once each. Client thread.
	 */
	private Set<Integer> stackableSupplies(MoneyMethod m)
	{
		Set<Integer> out = new HashSet<>();
		for (MoneyItem item : m.getInputs())
		{
			int id = MoneyGuide.itemId(item);
			if (id > 0 && stackable.computeIfAbsent(id, i -> itemManager.getItemComposition(i).isStackable()))
			{
				out.add(id);
			}
		}
		return out;
	}

	private Set<Integer> stackableInputs()
	{
		Set<Integer> out = new HashSet<>();
		for (MoneyMethod m : methods)
		{
			out.addAll(stackableSupplies(m));
		}
		return out;
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
	 * Points the hint arrow and Shortest Path at the step's destination, or clears them.
	 */
	private void showWay(Location destination, MoneyMethod m)
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

	private void stopGuide()
	{
		guide = null;
		step = null;
		suppliesDone = false;
		setBankItems(Set.of());
		clearWay();
		targets.track(Collections.emptySet(), Collections.emptySet());
	}

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onStartGuide(String page)
	{
		clientThread.invokeLater(() ->
		{
			MoneyMethod m = find(page);
			if (m == null)
			{
				return;
			}
			if (config.handOff() && handOff(m))
			{
				stopGuide();
				pushStep(null);
				return;
			}
			stopGuide();
			guide = m;
			setBankItems(inputIds(m));
			updateGuide();
		});
	}

	/**
	 * Starts the click-by-click guide another plugin in this jar has for the method. False when there's none.
	 */
	private boolean handOff(MoneyMethod m)
	{
		String label = fullGuideLabel(m, isActive(SkillingHelperPlugin.class), isActive(PvmProgressionPlugin.class));
		if ("Skilling Helper".equals(label))
		{
			eventBus.post(new OpenMethodRequest(skillMethods.get(m.getPage()).getId()));
			return true;
		}
		if ("PvM Progression".equals(label))
		{
			eventBus.post(new StartPvmGuideRequest(pvmContent(m)));
			return true;
		}
		return false;
	}

	@Override
	public void onStopGuide()
	{
		clientThread.invokeLater(() ->
		{
			stopGuide();
			pushStep(null);
		});
	}

	@Override
	public void onHaveSupplies()
	{
		clientThread.invokeLater(() ->
		{
			suppliesDone = true;
			updateGuide();
		});
	}

	@Override
	public void onAllowWilderness(boolean allow)
	{
		Wilderness.setAllowed(configManager, allow);
	}

	@Override
	public void onOpenWiki(String path)
	{
		LinkBrowser.browse(WIKI_URL + path);
	}

	private void pushStep(MoneyStep s)
	{
		final MoneyMakingPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(() -> p.updateStep(null, s));
		}
	}

	private static Set<Integer> inputIds(MoneyMethod m)
	{
		Set<Integer> ids = new HashSet<>();
		for (MoneyItem item : m.getInputs())
		{
			if (MoneyGuide.itemId(item) > 0)
			{
				ids.add(MoneyGuide.itemId(item));
			}
		}
		return ids;
	}

	private void setBankItems(Set<Integer> ids)
	{
		if (!ids.equals(bankItems))
		{
			bankItems = Set.copyOf(ids);
			bankFilter.relayout();
		}
	}

	private MoneyMethod find(String page)
	{
		for (MoneyMethod m : methods)
		{
			if (m.getPage().equals(page))
			{
				return m;
			}
		}
		return null;
	}
}
