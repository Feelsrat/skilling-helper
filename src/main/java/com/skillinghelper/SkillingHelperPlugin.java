package com.skillinghelper;

import com.google.inject.Provides;
import com.skillinghelper.bank.BankCache;
import com.skillinghelper.bank.BankFilter;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.Mooring;
import com.skillinghelper.data.PatchType;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.MethodPlanner;
import com.skillinghelper.engine.PlanSegment;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.Route;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import com.skillinghelper.overlay.ItemHighlightOverlay;
import com.skillinghelper.overlay.MinimapArrowOverlay;
import com.skillinghelper.overlay.SceneHighlightOverlay;
import com.skillinghelper.ui.SkillingHelperPanel;
import com.skillinghelper.prices.WikiPriceClient;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldType;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

@Slf4j
@PluginDescriptor(
	name = "Skilling Helper",
	description = "Quest Helper-style step-by-step skilling guides with pathing, highlights and a bank filter",
	tags = {"skilling", "guide", "training", "helper", "woodcutting", "mining", "fishing", "cooking", "crafting",
		"smithing", "herblore", "runecraft", "agility", "thieving", "prayer", "magic", "construction", "hunter",
		"farming", "slayer", "sailing", "combat"}
)
public class SkillingHelperPlugin extends Plugin implements SkillingHelperPanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final String KEY_ACTIVE_SKILL = "activeSkill";
	private static final String KEY_TARGET = "target.";
	private static final String KEY_METHOD = "method.";
	private static final String KEY_PREFERENCE = "preference";
	private static final String KEY_BOAT_MOORING = "boatMooring";
	private static final int QUEST_REFRESH_TICKS = 50;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private SkillingHelperConfig config;
	@Inject
	private ConfigManager configManager;
	@Inject
	private EventBus eventBus;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ItemManager itemManager;
	@Inject
	private SkillIconManager skillIconManager;
	@Inject
	private WorldMapPointManager worldMapPointManager;
	@Inject
	private GuideRepository guides;
	@Inject
	private TargetTracker tracker;
	@Inject
	private BankCache bankCache;
	@Inject
	private BankFilter bankFilter;
	@Inject
	private ShortestPathBridge shortestPath;
	@Inject
	private BirdhouseTracker birdhouses;
	@Inject
	private PatchTracker patches;
	@Inject
	private RunTimers runTimers;
	@Inject
	private SceneHighlightOverlay sceneOverlay;
	@Inject
	private MinimapArrowOverlay minimapOverlay;
	@Inject
	private ItemHighlightOverlay itemOverlay;
	@Inject
	private WikiPriceClient wikiPrices;
	@Inject
	private ScheduledExecutorService executor;

	@Getter
	private volatile ActiveGuide guide = ActiveGuide.NONE;

	private SkillingHelperPanel panel;
	private NavigationButton navButton;
	private MethodPlanner planner;
	private StepResolver resolver;

	private Skill activeSkill;
	private final Map<Skill, Integer> targets = new EnumMap<>(Skill.class);
	private final Map<Skill, String> forcedMethods = new EnumMap<>(Skill.class);
	private TrainingPreference preference;
	private final Set<Quest> completedQuests = EnumSet.noneOf(Quest.class);
	private int questRefreshCountdown;
	private boolean profileLoaded;

	private Route pathRoute;
	private boolean wasOnBoat;
	private Mooring boatMooring;
	private boolean hintArrowSet;
	private WorldMapPoint mapPoint;
	private ScheduledFuture<?> priceRefresh;

	@Override
	protected void startUp()
	{
		PriceLookup prices = new PriceLookup()
		{
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
		};
		planner = new MethodPlanner(prices);
		resolver = new StepResolver(prices);
		tracker.setInterestingTargets(guides.all());
		preference = config.preference();

		updatePriceRefresh();

		eventBus.register(tracker);
		eventBus.register(bankFilter);
		eventBus.register(shortestPath);
		overlayManager.add(sceneOverlay);
		overlayManager.add(minimapOverlay);
		overlayManager.add(itemOverlay);

		panel = new SkillingHelperPanel(this, skillIconManager);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Skilling Helper")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				tracker.rescan();
				loadProfile();
				refresh();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		stopPriceRefresh();
		wikiPrices.clear();
		eventBus.unregister(tracker);
		eventBus.unregister(bankFilter);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(minimapOverlay);
		overlayManager.remove(itemOverlay);
		clientToolbar.removeNavigation(navButton);
		bankFilter.shutDown();
		clientThread.invoke(() ->
		{
			bankCache.save();
			clearPath();
			tracker.clear();
			resetRuns();
			eventBus.unregister(shortestPath);
		});
		guide = ActiveGuide.NONE;
		profileLoaded = false;
		panel = null;
	}

	@Provides
	SkillingHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SkillingHelperConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			profileLoaded = false;
			resetRuns();
			clearPath();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		profileLoaded = false;
		resetRuns();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarpId() != -1)
		{
			birdhouses.onVarpChanged(e.getVarpId(), e.getValue());
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (!profileLoaded)
		{
			loadProfile();
		}
		if (--questRefreshCountdown <= 0)
		{
			refreshQuests();
		}
		trackBoat();
		updateRuns();
		refresh();
	}

	/**
	 * Reads nearby farming patches and updates the run countdowns.
	 */
	private void updateRuns()
	{
		if (!profileLoaded)
		{
			return;
		}
		Player me = client.getLocalPlayer();
		patches.tick(me == null ? null : worldLocation(me));
		runTimers.update(this, "Bird houses", ItemID.BIRDHOUSE_OAK, birdhouses.getReadyAt(), config.birdhouseTimer(),
			config.birdhouseNotification());
		for (PatchType type : PatchType.values())
		{
			runTimers.update(this, type.getLabel(), type.getIcon(), patches.readyAt(type), config.farmingTimers(),
				config.farmingNotification());
		}
	}

	private void resetRuns()
	{
		birdhouses.reset();
		patches.reset();
		runTimers.clear();
	}

	private Map<String, Integer> stopWaits()
	{
		Map<String, Integer> waits = new HashMap<>(birdhouses.waits());
		waits.putAll(patches.waits());
		return waits;
	}

	/**
	 * Remembers which dock the player left their boat at, so sailing guides can send them back to it.
	 */
	private void trackBoat()
	{
		Player me = client.getLocalPlayer();
		if (me == null || !profileLoaded)
		{
			return;
		}
		boolean onBoat = isOnBoat(me);
		if (wasOnBoat && !onBoat)
		{
			Mooring docked = Mooring.at(me.getWorldLocation(), 20);
			if (docked != null && docked != boatMooring)
			{
				boatMooring = docked;
				configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_BOAT_MOORING, docked.name());
			}
		}
		wasOnBoat = onBoat;
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		if (activeSkill == null && config.autoSelectSkill() && profileLoaded && guides.hasGuide(e.getSkill())
			&& e.getXp() > 0)
		{
			selectSkill(e.getSkill());
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (e.getContainerId() == InventoryID.BANK)
		{
			bankCache.update(e.getItemContainer());
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed e)
	{
		if (e.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankCache.save();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		// Per-account state is stored under RuneScape profiles; only react to the user's settings here
		if (!SkillingHelperConfig.GROUP.equals(e.getGroup()) || e.getProfile() != null)
		{
			return;
		}
		if (KEY_PREFERENCE.equals(e.getKey()))
		{
			preference = config.preference();
		}
		if ("livePrices".equals(e.getKey()))
		{
			updatePriceRefresh();
		}
		clientThread.invokeLater(() ->
		{
			clearPath();
			refresh();
		});
	}

	// ---- Prices ----

	private int guidePrice(int itemId)
	{
		return (int) Math.min(Integer.MAX_VALUE, itemManager.getItemPrice(itemId));
	}

	private WikiPriceClient.Price livePrice(int itemId)
	{
		return config.livePrices() ? wikiPrices.get(itemManager.canonicalize(itemId)) : null;
	}

	private void updatePriceRefresh()
	{
		stopPriceRefresh();
		if (config.livePrices())
		{
			priceRefresh = executor.scheduleWithFixedDelay(wikiPrices::refresh, 0, 5, TimeUnit.MINUTES);
		}
		else
		{
			wikiPrices.clear();
		}
	}

	private void stopPriceRefresh()
	{
		if (priceRefresh != null)
		{
			priceRefresh.cancel(false);
			priceRefresh = null;
		}
		wikiPrices.cancel();
	}

	private String priceSource()
	{
		Instant updated = wikiPrices.getUpdated();
		if (config.livePrices() && wikiPrices.hasPrices() && updated != null)
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

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onSkillSelected(Skill skill)
	{
		clientThread.invokeLater(() -> selectSkill(skill));
	}

	@Override
	public void onTargetChanged(int target)
	{
		clientThread.invokeLater(() ->
		{
			if (activeSkill != null)
			{
				setTarget(activeSkill, target);
				refresh();
			}
		});
	}

	@Override
	public void onTargetReset()
	{
		clientThread.invokeLater(() ->
		{
			if (activeSkill != null)
			{
				setTarget(activeSkill, defaultTarget(activeSkill));
				refresh();
			}
		});
	}

	@Override
	public void onPreferenceChanged(TrainingPreference pref)
	{
		clientThread.invokeLater(() ->
		{
			preference = pref;
			configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_PREFERENCE, pref.name());
			refresh();
		});
	}

	@Override
	public void onMethodChosen(String methodId)
	{
		clientThread.invokeLater(() ->
		{
			if (activeSkill == null)
			{
				return;
			}
			if (methodId == null)
			{
				forcedMethods.remove(activeSkill);
				configManager.unsetRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_METHOD + activeSkill.name());
			}
			else
			{
				forcedMethods.put(activeSkill, methodId);
				configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_METHOD + activeSkill.name(), methodId);
			}
			refresh();
		});
	}

	@Override
	public void onToggleBankFilter()
	{
		bankFilter.toggle();
		clientThread.invokeLater(this::refresh);
	}

	@Override
	public void onOpenWiki(String page)
	{
		if (page != null)
		{
			LinkBrowser.browse(WIKI_URL + page);
		}
	}

	// ---- State ----

	private void loadProfile()
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			return;
		}
		bankCache.load();
		targets.clear();
		forcedMethods.clear();
		for (Skill s : Skill.values())
		{
			String t = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_TARGET + s.name());
			if (t != null)
			{
				try
				{
					targets.put(s, Integer.parseInt(t));
				}
				catch (NumberFormatException ignored)
				{
					// fall back to the default goal
				}
			}
			String m = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_METHOD + s.name());
			if (m != null && guides.byId(m) != null)
			{
				forcedMethods.put(s, m);
			}
		}
		String pref = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_PREFERENCE);
		preference = config.preference();
		if (pref != null)
		{
			try
			{
				preference = TrainingPreference.valueOf(pref);
			}
			catch (IllegalArgumentException ignored)
			{
				// keep the config default
			}
		}
		String mooring = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_BOAT_MOORING);
		boatMooring = null;
		if (mooring != null)
		{
			try
			{
				boatMooring = Mooring.valueOf(mooring);
			}
			catch (IllegalArgumentException ignored)
			{
				// unknown mooring name
			}
		}
		String active = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_ACTIVE_SKILL);
		activeSkill = null;
		if (active != null)
		{
			try
			{
				activeSkill = Skill.valueOf(active);
			}
			catch (IllegalArgumentException ignored)
			{
				// unknown skill name
			}
		}
		refreshQuests();
		birdhouses.load();
		patches.load();
		profileLoaded = true;
	}

	private void selectSkill(Skill skill)
	{
		activeSkill = skill;
		if (skill == null)
		{
			configManager.unsetRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_ACTIVE_SKILL);
		}
		else
		{
			configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_ACTIVE_SKILL, skill.name());
			Integer target = targets.get(skill);
			if (target == null || target <= client.getRealSkillLevel(skill))
			{
				setTarget(skill, defaultTarget(skill));
			}
		}
		refreshQuests();
		refresh();
	}

	private int defaultTarget(Skill skill)
	{
		return Math.min(Experience.MAX_REAL_LEVEL, client.getRealSkillLevel(skill) + config.defaultTargetOffset());
	}

	private void setTarget(Skill skill, int target)
	{
		int clamped = Math.max(2, Math.min(Experience.MAX_REAL_LEVEL, target));
		targets.put(skill, clamped);
		configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_TARGET + skill.name(), clamped);
	}

	private void refreshQuests()
	{
		questRefreshCountdown = QUEST_REFRESH_TICKS;
		if (client.getGameState() != GameState.LOGGED_IN || activeSkill == null)
		{
			return;
		}
		completedQuests.clear();
		for (Method m : guides.methods(activeSkill))
		{
			for (Quest q : m.getQuests())
			{
				if (q.getState(client) == QuestState.FINISHED)
				{
					completedQuests.add(q);
				}
			}
		}
	}

	private PlayerState snapshot()
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, client.getRealSkillLevel(s));
			xp.put(s, client.getSkillExperience(s));
		}
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		int used = 0;
		if (inv != null)
		{
			for (Item item : inv.getItems())
			{
				if (item.getId() > 0)
				{
					used++;
				}
			}
		}
		Player me = client.getLocalPlayer();
		return PlayerState.builder()
			.levels(levels)
			.xp(xp)
			.inventory(counts(inv))
			.equipment(counts(worn))
			.bank(bankCache.get())
			.freeInventorySlots(28 - used)
			.location(me == null ? null : worldLocation(me))
			.membersWorld(client.getWorldType().contains(WorldType.MEMBERS))
			.inInstance(client.getTopLevelWorldView().isInstance())
			.onBoat(me != null && isOnBoat(me))
			.boatMooring(boatMooring)
			.stopWaits(stopWaits())
			.completedQuests(EnumSet.copyOf(completedQuestsOrEmpty()))
			.build();
	}

	/**
	 * The player's position in the main world, also while standing on a boat (which has its own world view).
	 */
	private WorldPoint worldLocation(Player me)
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

	private static boolean isOnBoat(Player me)
	{
		LocalPoint local = me.getLocalLocation();
		return local != null && local.getWorldView() != WorldView.TOPLEVEL;
	}

	private Set<Quest> completedQuestsOrEmpty()
	{
		return completedQuests.isEmpty() ? EnumSet.noneOf(Quest.class) : completedQuests;
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
	 * Rebuilds the guide from the current game state. Runs on the client thread.
	 */
	private void refresh()
	{
		if (client.getGameState() != GameState.LOGGED_IN || !profileLoaded || panel == null)
		{
			return;
		}

		ActiveGuide next;
		if (activeSkill == null)
		{
			next = ActiveGuide.NONE;
		}
		else
		{
			next = buildGuide(activeSkill);
		}
		guide = next;

		GuideStep step = next.getStep();
		boolean showPath = step != null && step.getType() != StepType.TRAIN && step.getType() != StepType.DONE;
		setPath(showPath ? step.getRoute() : null);
		if (showPath && shortestPath.isUnreachable(step.getRoute()))
		{
			next = next.toBuilder().step(step.toBuilder()
				.line("Shortest Path can't find a full route there with your current unlocks, so it shows how close "
					+ "it can get. You may need a quest, level or item for a transport, or a teleport closer.")
				.build()).build();
			guide = next;
		}

		final ActiveGuide forPanel = next;
		final TrainingPreference pref = preference;
		final boolean filterOn = bankFilter.isActive();
		final SkillingHelperPanel p = panel;
		SwingUtilities.invokeLater(() -> p.update(forPanel, pref, filterOn));
	}

	private ActiveGuide buildGuide(Skill skill)
	{
		PlayerState state = snapshot();
		int level = state.level(skill);
		int target = targets.getOrDefault(skill, Math.min(Experience.MAX_REAL_LEVEL, level + config.defaultTargetOffset()));
		long xpRemaining = MethodPlanner.xpBetween(state.xp(skill), target);
		List<Method> methods = guides.methods(skill);

		if (methods.isEmpty())
		{
			return ActiveGuide.builder()
				.skill(skill)
				.level(level)
				.targetLevel(target)
				.xpRemaining(xpRemaining)
				.player(state)
				.hasGuide(false)
				.priceSource(priceSource())
				.wikiPage(GuideRepository.wikiTrainingPage(skill, state.isMembersWorld()))
				.build();
		}

		String forcedId = forcedMethods.get(skill);
		Method forced = forcedId == null ? null : guides.byId(forcedId);
		List<MethodOption> options = planner.rank(methods, state, preference, target);
		List<PlanSegment> plan = planner.plan(methods, state, preference, target, forced);

		MethodOption current = null;
		if (!plan.isEmpty())
		{
			current = plan.get(0).getOption();
		}
		else if (!options.isEmpty() && options.get(0).isAvailable())
		{
			current = options.get(0);
		}

		ActiveGuide.ActiveGuideBuilder b = ActiveGuide.builder()
			.skill(skill)
			.level(level)
			.targetLevel(target)
			.xpRemaining(xpRemaining)
			.options(options)
			.plan(plan)
			.player(state)
			.hasGuide(true)
			.priceSource(priceSource())
			.wikiPage(GuideRepository.wikiTrainingPage(skill, state.isMembersWorld()));

		if (current == null)
		{
			GuideStep step = level >= target
				? GuideStep.empty("Goal reached: " + skill.getName() + " " + target + "!",
				"Set a new target level to keep going.")
				: GuideStep.empty("No method available yet",
				"None of the built-in methods fit your account right now. Check the method list for what's missing.");
			return b.step(step).build();
		}

		Method method = current.getMethod();
		boolean forcedActive = forced != null && forced == method;
		List<Requirement> reqs = resolver.requirements(method, state, current.getActions());
		GuideStep step = resolver.resolve(method, state, target, dropProduce(method), reqs);
		return b.method(method)
			.forced(forcedActive)
			.option(current)
			.requirements(reqs)
			.step(step)
			.bankItems(StepResolver.bankItems(reqs, step))
			.build();
	}

	private boolean dropProduce(Method m)
	{
		switch (config.inventoryFullAction())
		{
			case ALWAYS_BANK:
				return m.getBank() == null;
			case ALWAYS_DROP:
				return true;
			case METHOD_DEFAULT:
			default:
				return m.isDropByDefault() || m.getBank() == null;
		}
	}

	// ---- Pathing ----

	private void clearPath()
	{
		Route previous = pathRoute;
		setPath(null);
		if (previous == null && hintArrowSet)
		{
			client.clearHintArrow();
			hintArrowSet = false;
		}
	}

	private void setPath(Route route)
	{
		if (Objects.equals(route, pathRoute))
		{
			return;
		}
		pathRoute = route;
		WorldPoint destination = route == null ? null : route.getPoint();
		String name = route == null ? null : route.getStopName();

		if (hintArrowSet)
		{
			client.clearHintArrow();
			hintArrowSet = false;
		}
		if (destination != null && config.hintArrow())
		{
			client.setHintArrow(destination);
			hintArrowSet = true;
		}

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

		shortestPath.show(config.shortestPath() ? route : null);
	}
}
