package com.skillinghelper;

import com.google.inject.Provides;
import com.skillinghelper.bank.BankAmountSwapper;
import com.skillinghelper.bank.BankCache;
import com.skillinghelper.bank.BankFilter;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.Mooring;
import com.skillinghelper.data.PatchType;
import com.skillinghelper.data.SlayerPlaces;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.data.Outleveled;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.Format;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.MethodPlanner;
import com.skillinghelper.engine.PlanSegment;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Route;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import com.skillinghelper.overlay.ItemHighlightOverlay;
import com.skillinghelper.overlay.MinimapArrowOverlay;
import com.skillinghelper.overlay.SceneHighlightOverlay;
import com.skillinghelper.shops.Shops;
import com.skillinghelper.plunder.PlunderOverlay;
import com.skillinghelper.plunder.PlunderTracker;
import com.skillinghelper.plunder.PyramidPlunder;
import com.skillinghelper.thieving.BlackjackSwapper;
import com.skillinghelper.tog.TearsOfGuthix;
import com.skillinghelper.tog.TearsOverlay;
import com.skillinghelper.tog.TogTracker;
import com.skillinghelper.travel.TravelAdvisor;
import com.skillinghelper.ui.PickList;
import com.skillinghelper.ui.SkillingHelperPanel;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
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
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

@PluginDescriptor(
	name = "Skilling Helper",
	description = "Quest Helper-style step-by-step skilling guides with pathing, highlights and a bank filter",
	tags = {"skilling", "guide", "training", "helper", "woodcutting", "mining", "fishing", "cooking", "crafting",
		"smithing", "herblore", "runecraft", "agility", "thieving", "prayer", "magic", "construction", "hunter",
		"farming", "slayer", "sailing", "combat"}
)
public class SkillingHelperPlugin extends Plugin implements SkillingHelperPanel.Listener, GuideHost
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final int QUEST_REFRESH_TICKS = 50;
	private static final int MOORING_RADIUS = 20;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private SkillingHelperConfig config;
	@Inject
	private ConfigManager configManager;

	@Inject
	private PluginManager pluginManager;
	@Inject
	private EventBus eventBus;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private SkillIconManager skillIconManager;
	@Inject
	private GuideRepository guides;
	@Inject
	private GuideSettings settings;
	@Inject
	private GuidePrices prices;
	@Inject
	private PlayerSnapshot snapshot;
	@Inject
	private PathDisplay path;
	@Inject
	private TargetTracker tracker;
	@Inject
	private BankCache bankCache;
	@Inject
	private BankFilter bankFilter;
	@Inject
	private BankAmountSwapper bankAmountSwapper;
	@Inject
	private ShortestPathBridge shortestPath;
	@Inject
	private BirdhouseTracker birdhouses;
	@Inject
	private PatchTracker patches;
	@Inject
	private RunTimers runTimers;
	@Inject
	private SessionXpTracker sessionXp;
	@Inject
	private TogTracker tog;
	@Inject
	private TearsOverlay tearsOverlay;
	@Inject
	private PlunderTracker plunder;
	@Inject
	private PlunderOverlay plunderOverlay;
	@Inject
	private BlackjackSwapper blackjack;
	@Inject
	private SceneHighlightOverlay sceneOverlay;
	@Inject
	private MinimapArrowOverlay minimapOverlay;
	@Inject
	private ItemHighlightOverlay itemOverlay;
	@Inject
	private TravelAdvisor travel;

	@Getter
	private volatile ActiveGuide guide = ActiveGuide.NONE;

	private SkillingHelperPanel panel;
	private NavigationButton navButton;
	private MethodPlanner planner;
	private StepResolver resolver;

	private final Set<Quest> completedQuests = EnumSet.noneOf(Quest.class);
	private final Set<Quest> startedQuests = EnumSet.noneOf(Quest.class);
	private int questRefreshCountdown;
	private boolean profileLoaded;
	private boolean wasOnBoat;
	/**
	 * Logged in: check on the next tick that Shortest Path is there.
	 */
	private boolean checkShortestPath;
	/**
	 * The player stopped the guide with the Stop button, so gaining xp shouldn't open one again.
	 */
	private boolean autoSelectPaused;
	/**
	 * The Tears of Guthix guide is open instead of a skill.
	 */
	private boolean tearsOpen;
	/**
	 * The quick pick list on show in place of a guide, or null.
	 */
	private PickList picks;

	@Override
	protected void startUp()
	{
		planner = new MethodPlanner(prices);
		resolver = new StepResolver(prices);
		tracker.setInterestingTargets(guides.all());
		tracker.addInterestingTargets(TearsOfGuthix.TARGETS);
		tracker.addInterestingTargets(Shops.targets());
		tracker.addInterestingTargets(List.of(PyramidPlunder.MUMMY, PyramidPlunder.MONEY_POT));
		settings.useDefaultPreference();
		prices.updateRefresh();

		eventBus.register(tracker);
		bankFilter.startUp();
		eventBus.register(bankFilter);
		eventBus.register(bankAmountSwapper);
		eventBus.register(shortestPath);
		eventBus.register(tog);
		eventBus.register(plunder);
		eventBus.register(blackjack);
		overlayManager.add(sceneOverlay);
		overlayManager.add(tearsOverlay);
		overlayManager.add(plunderOverlay);
		overlayManager.add(minimapOverlay);
		overlayManager.add(itemOverlay);
		addPanel();

		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				checkShortestPath = true;
				tracker.rescan();
				plunder.rescan();
				loadProfile();
				refresh();
			}
		});
	}

	private void addPanel()
	{
		panel = new SkillingHelperPanel(this, skillIconManager);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Skilling Helper")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		prices.shutDown();
		eventBus.unregister(tracker);
		eventBus.unregister(bankFilter);
		eventBus.unregister(bankAmountSwapper);
		eventBus.unregister(tog);
		eventBus.unregister(plunder);
		eventBus.unregister(blackjack);
		blackjack.setActive(false, false);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(tearsOverlay);
		overlayManager.remove(plunderOverlay);
		overlayManager.remove(minimapOverlay);
		overlayManager.remove(itemOverlay);
		clientToolbar.removeNavigation(navButton);
		bankFilter.shutDown();
		clientThread.invoke(() ->
		{
			bankCache.save();
			path.clear();
			tracker.clear();
			plunder.clear();
			resetRuns();
			eventBus.unregister(shortestPath);
		});
		guide = ActiveGuide.NONE;
		profileLoaded = false;
		tearsOpen = false;
		picks = null;
		panel = null;
	}

	@Provides
	SkillingHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SkillingHelperConfig.class);
	}

	// ---- Game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			prices.onLoggedIn();
			checkShortestPath = true;
			travel.invalidate();
		}
		if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			profileLoaded = false;
			resetRuns();
			path.clear();
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
		if (checkShortestPath)
		{
			checkShortestPath = false;
			if (config.shortestPath())
			{
				ShortestPathLink.warnIfMissing(client, pluginManager);
			}
		}
		if (--questRefreshCountdown <= 0)
		{
			refreshQuests();
		}
		trackBoat();
		updateRuns();
		if (profileLoaded)
		{
			tog.tick(this);
		}
		Skill activeSkill = settings.getActiveSkill();
		plunder.setSwapEnabled(config.plunderSwapUrns());
		plunder.setCharmOn(config.plunderSnakeCharm());
		plunder.tick(plunderGuideOpen(activeSkill), config.plunderSceptreHunt());
		blackjack.setActive(blackjackGuideOpen(activeSkill), config.blackjackSwap());
		sessionXp.tick(activeSkill, activeSkill == null ? 0 : client.getSkillExperience(activeSkill));
		refresh();
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		if (settings.getActiveSkill() == null && !tearsOpen && picks == null && config.autoSelectSkill() && !autoSelectPaused && profileLoaded
			&& guides.hasGuide(e.getSkill()) && e.getXp() > 0)
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
		if (GuideSettings.KEY_PREFERENCE.equals(e.getKey()))
		{
			settings.useDefaultPreference();
		}
		if ("livePrices".equals(e.getKey()))
		{
			prices.updateRefresh();
		}
		if ("bankFilterOn".equals(e.getKey()))
		{
			// Saved by the bank button itself, nothing in the guide changes
			return;
		}
		clientThread.invokeLater(() ->
		{
			path.clear();
			refresh();
		});
	}

	// ---- Runs and boats ----

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
		patches.tick(me == null ? null : PlayerSnapshot.worldLocation(client, me));
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
		sessionXp.reset();
		tog.reset();
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
		boolean onBoat = PlayerSnapshot.isOnBoat(me);
		if (wasOnBoat && !onBoat)
		{
			Mooring docked = Mooring.at(me.getWorldLocation(), MOORING_RADIUS);
			if (docked != null && docked != settings.getBoatMooring())
			{
				settings.setBoatMooring(docked);
			}
		}
		wasOnBoat = onBoat;
	}

	/**
	 * Another plugin in this jar (PvM Progression) asked to train a skill to a level: open that guide.
	 */
	@Subscribe
	public void onTrainSkillRequest(TrainSkillRequest e)
	{
		Skill skill = e.getSkill();
		selectSkill(skill);
		int target = Math.max(e.getTargetLevel(), client.getRealSkillLevel(skill) + 1);
		setTargetAndRefresh(Math.min(Experience.MAX_REAL_LEVEL, target));
		SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
	}

	/**
	 * Another plugin in this jar (Money Making) asked to run one method: open its skill with that method chosen.
	 */
	@Subscribe
	public void onOpenMethodRequest(OpenMethodRequest e)
	{
		Method method = guides.byId(e.getMethodId());
		if (method == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		settings.setForcedMethod(method.getSkill(), method.getId());
		selectSkill(method.getSkill());
		SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
	}

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onSkillSelected(Skill skill)
	{
		clientThread.invokeLater(() -> selectSkill(skill));
	}

	@Override
	public void onQuickPick(SkillingHelperPanel.QuickPick pick)
	{
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN || !profileLoaded)
			{
				return;
			}
			if (pick == SkillingHelperPanel.QuickPick.TEARS_OF_GUTHIX)
			{
				openTears();
				return;
			}
			PickList list = pick == SkillingHelperPanel.QuickPick.LOWEST_SKILL ? lowestSkills()
				: pick == SkillingHelperPanel.QuickPick.BEST_GOLD ? bestGold()
				: pick == SkillingHelperPanel.QuickPick.FASTEST_LEVELS ? fastestLevels()
				: mostAfk();
			selectSkill(null);
			picks = list;
			refresh();
		});
	}

	@Override
	public void onPickChosen(PickList.Row row)
	{
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN || !profileLoaded)
			{
				return;
			}
			Skill skill = row.getSkill();
			if (row.getMethodId() != null)
			{
				settings.setForcedMethod(skill, row.getMethodId());
			}
			else if (row.isClearForced())
			{
				settings.setForcedMethod(skill, null);
			}
			selectSkill(skill);
			if (row.getTarget() > 0)
			{
				setTargetAndRefresh(row.getTarget());
			}
		});
	}

	/**
	 * Opens the weekly Tears of Guthix guide in place of a skill.
	 */
	private void openTears()
	{
		selectSkill(null);
		tearsOpen = true;
		refresh();
	}

	/**
	 * How many guides a quick pick lists.
	 */
	private static final int PICK_ROWS = 10;

	/**
	 * The player's skills that have a guide, lowest level first, each with the method its guide would start with.
	 */
	private PickList lowestSkills()
	{
		PlayerState state = stateForAllGuides();
		List<Skill> guided = new ArrayList<>();
		for (Skill s : Skill.values())
		{
			if (guides.hasGuide(s))
			{
				guided.add(s);
			}
		}
		PickList.PickListBuilder list = PickList.builder()
			.pick(SkillingHelperPanel.QuickPick.LOWEST_SKILL)
			.note("Your skills from the lowest level up, with the method each guide would start with.")
			.emptyText("Every skill with a guide is already 99.");
		for (Skill skill : limit(MethodPlanner.rankLowest(guided, state)))
		{
			int target = goal(skill, state);
			MethodOption first = planner.rank(guides.methods(skill), state, settings.getPreference(), target).stream()
				.filter(o -> o.isPickable() && !o.getMethod().isOutleveled(state.level(skill), state.combatLevel()))
				.findFirst().orElse(null);
			list.row(PickList.Row.builder()
				.skill(skill)
				.title(skill.getName())
				.value("Level " + state.level(skill))
				.detail(first == null ? "No method you can use yet" : first.getMethod().getName())
				.build());
		}
		return list.build();
	}

	/**
	 * Each skill's best money maker the player can do now, most gold per hour first.
	 */
	private PickList bestGold()
	{
		PlayerState state = stateForAllGuides();
		PickList.PickListBuilder list = PickList.builder()
			.pick(SkillingHelperPanel.QuickPick.BEST_GOLD)
			.note("The best money maker in each skill you can do now, most gold per hour first, at your pace.")
			.emptyText("None of the guides make money with your current levels, quests and gold.");
		for (List<MethodOption> group : limit(MethodPlanner.sameActivityOnce(
			planner.rankProfitable(guides.all(), state, s -> goal(s, state)), state)))
		{
			MethodOption o = group.get(0);
			Method m = o.getMethod();
			list.row(PickList.Row.builder()
				.skill(m.getSkill())
				.title(m.getName())
				.value(Format.gp(o.gpPerHour()) + String.format("/hr · ~%,d xp/hr", Math.round(o.getXpPerHour())))
				.detail(alsoTrains(group, m.levelRange()))
				.methodId(m.getId())
				.build());
		}
		return list.build();
	}

	/**
	 * Every skill's quickest climb of {@value MethodPlanner#FASTEST_LEVELS} levels, switching method on the way where
	 * a faster one unlocks, quickest first.
	 */
	private PickList fastestLevels()
	{
		PlayerState state = stateForAllGuides();
		PickList.PickListBuilder list = PickList.builder()
			.pick(SkillingHelperPanel.QuickPick.FASTEST_LEVELS)
			.note("Where you'd gain " + MethodPlanner.FASTEST_LEVELS + " levels the quickest at your pace, "
				+ "switching to a faster method on the way when one unlocks.")
			.emptyText("None of the guides can be used with your current levels, quests and gold.");
		for (MethodPlanner.FastestClimb climb : limit(planner.rankFastest(guides.all(), state,
			MethodPlanner.FASTEST_LEVELS)))
		{
			List<PlanSegment> legs = climb.getLegs();
			StringBuilder route = new StringBuilder();
			for (PlanSegment leg : legs)
			{
				route.append(route.length() == 0 ? "" : ", then ").append(leg.getOption().getMethod().getName());
			}
			boolean oneMethod = legs.size() == 1;
			list.row(PickList.Row.builder()
				.skill(climb.getSkill())
				.title(climb.getSkill().getName() + " " + climb.getFromLevel() + " → " + climb.getToLevel())
				.value("~" + Format.hours(climb.getHours()))
				.detail(route.toString())
				// One method all the way: choose it. Otherwise let the plan switch on the way.
				.methodId(oneMethod ? legs.get(0).getOption().getMethod().getId() : null)
				.clearForced(!oneMethod)
				.target(climb.getToLevel())
				.build());
		}
		return list.build();
	}

	/**
	 * Each skill's most AFK method the player can do now and hasn't outleveled, fewest clicks first.
	 */
	private PickList mostAfk()
	{
		PlayerState state = stateForAllGuides();
		PickList.PickListBuilder list = PickList.builder()
			.pick(SkillingHelperPanel.QuickPick.MOST_AFK)
			.note("The most AFK method in each skill you can do now, fewest clicks per hour first. Methods you've "
				+ "outleveled are left out.")
			.emptyText("No guide you haven't outleveled can be used with your current levels, quests and gold.");
		for (List<MethodOption> group : limit(MethodPlanner.sameActivityOnce(
			planner.rankMostAfk(guides.all(), state, s -> goal(s, state)), state)))
		{
			MethodOption o = group.get(0);
			Method m = o.getMethod();
			list.row(PickList.Row.builder()
				.skill(m.getSkill())
				.title(m.getName())
				.value(String.format("~%,d clicks/hr · ~%,d xp/hr", m.getClicksPerHour(), Math.round(o.getXpPerHour())))
				.detail(alsoTrains(group, m.levelRange()))
				.methodId(m.getId())
				.build());
		}
		return list.build();
	}

	/**
	 * "Lvl 1 → 99 · Also trains Strength, Defence" when the same activity was listed under other skills; opening
	 * the row trains the lowest one, the skill shown.
	 */
	private static String alsoTrains(List<MethodOption> group, String detail)
	{
		if (group.size() < 2)
		{
			return detail;
		}
		StringBuilder others = new StringBuilder();
		for (MethodOption o : group.subList(1, group.size()))
		{
			others.append(others.length() == 0 ? "" : ", ").append(o.getMethod().getSkill().getName());
		}
		return detail + " · Also trains " + others;
	}

	private static <T> List<T> limit(List<T> ranked)
	{
		return ranked.size() <= PICK_ROWS ? ranked : ranked.subList(0, PICK_ROWS);
	}

	/**
	 * The saved goal for a skill, or the default one above the player's level.
	 */
	private int goal(Skill skill, PlayerState state)
	{
		Integer saved = settings.getTarget(skill);
		return saved != null && saved > state.level(skill) ? saved
			: Math.min(Experience.MAX_REAL_LEVEL, state.level(skill) + config.defaultTargetOffset());
	}

	/**
	 * The player's state with every guide's quests read, for picks that look across all skills.
	 */
	private PlayerState stateForAllGuides()
	{
		Set<Quest> allQuests = EnumSet.noneOf(Quest.class);
		for (Method m : guides.all())
		{
			allQuests.addAll(m.questsToCheck());
		}
		Set<Quest> done = EnumSet.noneOf(Quest.class);
		Set<Quest> started = EnumSet.noneOf(Quest.class);
		readQuests(allQuests, done, started);
		planner.setPace(config.ratePace());
		planner.setAllowWilderness(config.allowWilderness());
		return snapshot.take(settings.getBoatMooring(), done, started, config.preferShops());
	}

	@Override
	public void onStopGuide()
	{
		clientThread.invokeLater(() ->
		{
			selectSkill(null);
			// Stay off until a skill is picked again, even with auto-select on
			autoSelectPaused = true;
		});
	}

	@Override
	public void onTargetChanged(int target)
	{
		clientThread.invokeLater(() -> setTargetAndRefresh(target));
	}

	@Override
	public void onTargetReset()
	{
		clientThread.invokeLater(() ->
		{
			Skill skill = settings.getActiveSkill();
			if (skill != null)
			{
				setTargetAndRefresh(defaultTarget(skill));
			}
		});
	}

	private void setTargetAndRefresh(int target)
	{
		Skill skill = settings.getActiveSkill();
		if (skill != null)
		{
			settings.setTarget(skill, target);
			refresh();
		}
	}

	@Override
	public void onPreferenceChanged(TrainingPreference pref)
	{
		clientThread.invokeLater(() ->
		{
			settings.setPreference(pref);
			refresh();
		});
	}

	@Override
	public void onMethodChosen(String methodId)
	{
		clientThread.invokeLater(() ->
		{
			Skill skill = settings.getActiveSkill();
			if (skill != null)
			{
				settings.setForcedMethod(skill, methodId);
				refresh();
			}
		});
	}

	@Override
	public void onAllowWilderness(boolean allow)
	{
		// Saved in the shared setting; the config change refreshes this guide and the other plugins
		Wilderness.setAllowed(configManager, allow);
	}

	@Override
	public void onHideOutleveled(boolean hide)
	{
		Outleveled.setHidden(configManager, hide);
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

	@Override
	public void onOpenLink(String url)
	{
		if (url != null)
		{
			LinkBrowser.browse(url);
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
		settings.load();
		refreshQuests();
		birdhouses.load();
		patches.load();
		tog.load();
		profileLoaded = true;
	}

	private void selectSkill(Skill skill)
	{
		autoSelectPaused = false;
		tearsOpen = false;
		picks = null;
		settings.setActiveSkill(skill);
		if (skill != null)
		{
			Integer target = settings.getTarget(skill);
			if (target == null || target <= client.getRealSkillLevel(skill))
			{
				settings.setTarget(skill, defaultTarget(skill));
			}
		}
		refreshQuests();
		refresh();
	}

	private int defaultTarget(Skill skill)
	{
		return Math.min(Experience.MAX_REAL_LEVEL, client.getRealSkillLevel(skill) + config.defaultTargetOffset());
	}

	/**
	 * Re-reads which of the open skill's method quests are done. Quest states are scripts, so this runs every
	 * {@value #QUEST_REFRESH_TICKS} ticks rather than every tick.
	 */
	private void refreshQuests()
	{
		questRefreshCountdown = QUEST_REFRESH_TICKS;
		Skill skill = settings.getActiveSkill();
		if (client.getGameState() != GameState.LOGGED_IN || skill == null)
		{
			return;
		}
		completedQuests.clear();
		startedQuests.clear();
		Set<Quest> quests = EnumSet.noneOf(Quest.class);
		for (Method m : guides.methods(skill))
		{
			quests.addAll(m.questsToCheck());
		}
		if (skill == Skill.SLAYER)
		{
			// Quests that open up places to do Slayer tasks
			quests.addAll(SlayerPlaces.get().quests());
		}
		readQuests(quests, completedQuests, startedQuests);
	}

	private void readQuests(Set<Quest> quests, Set<Quest> completed, Set<Quest> started)
	{
		for (Quest q : quests)
		{
			QuestState state = q.getState(client);
			if (state == QuestState.FINISHED)
			{
				completed.add(q);
			}
			if (state != QuestState.NOT_STARTED)
			{
				started.add(q);
			}
		}
	}

	// ---- Building the guide ----

	/**
	 * Rebuilds the guide from the current game state. Runs on the client thread.
	 */
	private void refresh()
	{
		// Read once: shutDown clears it from another thread
		final SkillingHelperPanel p = panel;
		if (client.getGameState() != GameState.LOGGED_IN || !profileLoaded || p == null)
		{
			return;
		}

		Skill skill = settings.getActiveSkill();
		ActiveGuide next = tearsOpen ? buildTearsGuide()
			: skill != null ? buildGuide(skill)
			: picks != null ? ActiveGuide.builder().picks(picks).build()
			: ActiveGuide.NONE;
		long now = Instant.now().getEpochSecond();
		next = next.toBuilder()
			.weeklyState(tog.getStatus().getState())
			.weeklyStatus(TearsOfGuthix.describe(tog.getStatus(), now))
			.weeklyCalendarUrl(TearsOfGuthix.calendarUrl(tog.getStatus(), now))
			.build();
		guide = next;
		next = showPath(next);
		guide = next;

		final ActiveGuide forPanel = next;
		final TrainingPreference pref = settings.getPreference();
		final boolean filterOn = bankFilter.isActive();
		SwingUtilities.invokeLater(() -> p.update(forPanel, pref, filterOn));
	}

	/**
	 * Points the arrows and path at the step's destination, and explains when Shortest Path can't get there.
	 */
	private ActiveGuide showPath(ActiveGuide current)
	{
		GuideStep step = current.getStep();
		boolean showPath = step != null && step.getType() != StepType.TRAIN && step.getType() != StepType.DONE;
		path.show(showPath ? step.getRoute() : null);
		if (!showPath || !path.isUnreachable(step.getRoute()))
		{
			return current;
		}
		return current.toBuilder().step(step.toBuilder()
			.line("Shortest Path can't find a full route there with your current unlocks, so it shows how close "
				+ "it can get. You may need a quest, level or item for a transport, or a teleport closer.")
			.build()).build();
	}

	private ActiveGuide buildTearsGuide()
	{
		PlayerState state = snapshot.take(settings.getBoatMooring(), completedQuests, startedQuests,
			config.preferShops());
		TearsOfGuthix.Situation sit = TearsOfGuthix.Situation.builder()
			.status(tog.getStatus())
			.now(Instant.now().getEpochSecond())
			.inChasm(tog.isInChasm())
			.playing(tog.isPlaying())
			.tears(tog.getTears())
			.lostTribeDone(tog.isLostTribeDone())
			.reward(tog.getReward())
			.build();
		GuideStep step = TearsOfGuthix.step(sit, state);
		List<String> info = new ArrayList<>();
		if (tog.getReward() != null)
		{
			info.add("Reward: " + tog.getReward() + ", going to your lowest skill.");
		}
		info.add("Once every 7 days, after gaining 100,000 xp or a quest point since your last visit.");
		info.add("More quest points mean more time in the cave and more tears.");
		return ActiveGuide.builder()
			.activity(TearsOfGuthix.NAME)
			.hasGuide(true)
			.player(state)
			.step(step)
			.activityInfo(info)
			.bankItems(StepResolver.bankItems(List.of(), step))
			.wikiPage(TearsOfGuthix.WIKI_PAGE)
			.build();
	}

	private ActiveGuide buildGuide(Skill skill)
	{
		PlayerState state = snapshot.take(settings.getBoatMooring(), completedQuests, startedQuests,
			config.preferShops());
		int level = state.level(skill);
		Integer saved = settings.getTarget(skill);
		int target = saved != null ? saved : Math.min(Experience.MAX_REAL_LEVEL, level + config.defaultTargetOffset());
		ActiveGuide.ActiveGuideBuilder b = ActiveGuide.builder()
			.skill(skill)
			.level(level)
			.targetLevel(target)
			.xpRemaining(MethodPlanner.xpBetween(state.xp(skill), target))
			.player(state)
			.priceSource(prices.sourceText())
			.sessionXpPerHour(sessionXp.xpPerHour(skill))
			.hideOutleveled(config.hideOutleveled())
			.wikiPage(GuideRepository.wikiTrainingPage(skill, state.isMembersWorld()));

		List<Method> methods = guides.methods(skill);
		if (methods.isEmpty())
		{
			return b.hasGuide(false).build();
		}

		String forcedId = settings.getForcedMethod(skill);
		Method forced = forcedId == null ? null : guides.byId(forcedId);
		TrainingPreference pref = settings.getPreference();
		planner.setPace(config.ratePace());
		planner.setAllowWilderness(config.allowWilderness());
		List<MethodOption> options = planner.rank(methods, state, pref, target);
		List<PlanSegment> plan = planner.plan(methods, state, pref, target, forced);
		b.options(options).plan(plan).hasGuide(true);

		MethodOption current = currentOption(plan, options);
		if (current == null)
		{
			return b.step(noMethodStep(skill, level, target)).build();
		}
		Method method = current.getMethod();
		state = state.toBuilder().travel(travel.pick(state, StepResolver.destinations(method, state))).build();
		b.player(state);
		List<Requirement> reqs = resolver.requirements(method, state, current.getActions());
		// Size trips and batches to this leg of the plan, the same span the requirement amounts cover
		int legTarget = plan.isEmpty() ? target : plan.get(0).getToLevel();
		GuideStep step = resolver.resolve(method, state, legTarget, dropProduce(method), reqs);
		if (PyramidPlunder.METHOD_ID.equals(method.getId()) || (skill == Skill.THIEVING && plunder.isInRound()))
		{
			step = plunderStep(step, state);
		}
		if (BlackjackSwapper.METHOD_ID.equals(method.getId()) && blackjack.getAttacker() != null
			&& step.getType() != StepType.DONE)
		{
			GuideStep.GuideStepBuilder combat = step.toBuilder().title("Get out of combat").clearDetails();
			for (String line : BlackjackSwapper.combatLines())
			{
				combat.line(line);
			}
			step = combat.build();
		}
		return b.method(method)
			.forced(forced != null && forced == method)
			.option(current)
			.requirements(reqs)
			.step(step)
			.bankItems(StepResolver.bankItems(reqs, step, state.getTravel()))
			.build();
	}

	/**
	 * Whether blackjacking is the method the Thieving guide is showing.
	 */
	private boolean blackjackGuideOpen(Skill activeSkill)
	{
		Method m = guide.getMethod();
		return !tearsOpen && activeSkill == Skill.THIEVING && m != null && BlackjackSwapper.METHOD_ID.equals(m.getId());
	}

	/**
	 * Whether Pyramid Plunder is the method being shown, or the player is mid-round with the Thieving guide open.
	 */
	private boolean plunderGuideOpen(Skill activeSkill)
	{
		if (tearsOpen || activeSkill != Skill.THIEVING)
		{
			return false;
		}
		Method m = guide.getMethod();
		return plunder.isInRound() || (m != null && PyramidPlunder.METHOD_ID.equals(m.getId()));
	}

	/**
	 * At and in the pyramid the round plan takes over; on the way, get a snake charm first while it still helps.
	 */
	private GuideStep plunderStep(GuideStep base, PlayerState state)
	{
		// Full of artefacts after a round: bank them first, and say what they're for
		if (!plunder.isInRound() && base.getType() == StepType.BANK)
		{
			GuideStep.GuideStepBuilder banked = base.toBuilder();
			PyramidPlunder.artefactLines().forEach(banked::line);
			return banked.build();
		}
		// Finish a round that's under way even once the goal is reached
		GuideStep own = plunder.getStep();
		if (own != null && (plunder.isInRound() || base.getType() != StepType.DONE))
		{
			return own;
		}
		if (base.getType() == StepType.DONE)
		{
			return base;
		}
		if (!PyramidPlunder.needsCharm(state.level(Skill.THIEVING), config.plunderSnakeCharm(),
			state.bankKnown(), state.owned(PyramidPlunder.SNAKE_CHARM)))
		{
			return base;
		}
		boolean atAli = PyramidPlunder.ALI.contains(state.getLocation());
		GuideStep charm = PyramidPlunder.charmSourceStep(atAli);
		return atAli ? charm : charm.toBuilder().travel(Route.to(state, PyramidPlunder.ALI)).build();
	}

	/**
	 * The method to train with now: the plan's first leg, or the best available method if there's no plan.
	 */
	private static MethodOption currentOption(List<PlanSegment> plan, List<MethodOption> options)
	{
		if (!plan.isEmpty())
		{
			return plan.get(0).getOption();
		}
		if (!options.isEmpty() && options.get(0).isPickable())
		{
			return options.get(0);
		}
		return null;
	}

	private static GuideStep noMethodStep(Skill skill, int level, int target)
	{
		if (level >= target)
		{
			return GuideStep.empty("Goal reached: " + skill.getName() + " " + target + "!",
				"Set a new target level to keep going.");
		}
		return GuideStep.empty("No method available yet",
			"None of the built-in methods fit your account right now. Check the method list for what's missing.");
	}

	private boolean dropProduce(Method m)
	{
		switch (config.inventoryFullAction())
		{
			case ALWAYS_BANK:
				// Methods without a bank of their own use the closest one
				return false;
			case ALWAYS_DROP:
				return true;
			case METHOD_DEFAULT:
			default:
				return m.isDropByDefault() || m.getBank() == null;
		}
	}
}
