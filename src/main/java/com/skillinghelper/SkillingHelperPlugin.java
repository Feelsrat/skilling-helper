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
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.MethodPlanner;
import com.skillinghelper.engine.PlanSegment;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import com.skillinghelper.overlay.ItemHighlightOverlay;
import com.skillinghelper.overlay.MinimapArrowOverlay;
import com.skillinghelper.overlay.SceneHighlightOverlay;
import com.skillinghelper.ui.SkillingHelperPanel;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
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
public class SkillingHelperPlugin extends Plugin implements SkillingHelperPanel.Listener
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
	private RateTracker rates;
	@Inject
	private SceneHighlightOverlay sceneOverlay;
	@Inject
	private MinimapArrowOverlay minimapOverlay;
	@Inject
	private ItemHighlightOverlay itemOverlay;

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
	 * The player stopped the guide with the Stop button, so gaining xp shouldn't open one again.
	 */
	private boolean autoSelectPaused;

	@Override
	protected void startUp()
	{
		planner = new MethodPlanner(prices);
		resolver = new StepResolver(prices);
		tracker.setInterestingTargets(guides.all());
		settings.useDefaultPreference();
		prices.updateRefresh();

		eventBus.register(tracker);
		bankFilter.startUp();
		eventBus.register(bankFilter);
		eventBus.register(bankAmountSwapper);
		eventBus.register(shortestPath);
		overlayManager.add(sceneOverlay);
		overlayManager.add(minimapOverlay);
		overlayManager.add(itemOverlay);
		addPanel();

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
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(minimapOverlay);
		overlayManager.remove(itemOverlay);
		clientToolbar.removeNavigation(navButton);
		bankFilter.shutDown();
		clientThread.invoke(() ->
		{
			bankCache.save();
			path.clear();
			tracker.clear();
			rates.save();
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

	// ---- Game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			prices.onLoggedIn();
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
		if (--questRefreshCountdown <= 0)
		{
			refreshQuests();
		}
		trackBoat();
		updateRuns();
		ActiveGuide current = guide;
		if (profileLoaded)
		{
			rates.tick(current.isActive() ? current.getMethod() : null,
				current.getSkill() == null ? 0 : client.getSkillExperience(current.getSkill()));
		}
		refresh();
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		if (settings.getActiveSkill() == null && config.autoSelectSkill() && !autoSelectPaused && profileLoaded
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
		rates.reset();
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

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onSkillSelected(Skill skill)
	{
		clientThread.invokeLater(() -> selectSkill(skill));
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
		settings.load();
		refreshQuests();
		birdhouses.load();
		patches.load();
		rates.load();
		profileLoaded = true;
	}

	private void selectSkill(Skill skill)
	{
		autoSelectPaused = false;
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
			quests.addAll(m.getQuests());
		}
		if (skill == Skill.SLAYER)
		{
			// Quests that open up places to do Slayer tasks
			quests.addAll(SlayerPlaces.get().quests());
		}
		for (Quest q : quests)
		{
			QuestState state = q.getState(client);
			if (state == QuestState.FINISHED)
			{
				completedQuests.add(q);
			}
			if (state != QuestState.NOT_STARTED)
			{
				startedQuests.add(q);
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
		ActiveGuide next = skill == null ? ActiveGuide.NONE : buildGuide(skill);
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

	private ActiveGuide buildGuide(Skill skill)
	{
		PlayerState state = snapshot.take(settings.getBoatMooring(), completedQuests, startedQuests);
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
			.wikiPage(GuideRepository.wikiTrainingPage(skill, state.isMembersWorld()));

		List<Method> methods = guides.methods(skill);
		if (methods.isEmpty())
		{
			return b.hasGuide(false).build();
		}

		String forcedId = settings.getForcedMethod(skill);
		Method forced = forcedId == null ? null : guides.byId(forcedId);
		TrainingPreference pref = settings.getPreference();
		List<MethodOption> options = planner.rank(methods, state, pref, target);
		List<PlanSegment> plan = planner.plan(methods, state, pref, target, forced);
		b.options(options).plan(plan).hasGuide(true);

		MethodOption current = currentOption(plan, options);
		if (current == null)
		{
			return b.step(noMethodStep(skill, level, target)).build();
		}
		Method method = current.getMethod();
		List<Requirement> reqs = resolver.requirements(method, state, current.getActions());
		// Size trips and batches to this leg of the plan, the same span the requirement amounts cover
		int legTarget = plan.isEmpty() ? target : plan.get(0).getToLevel();
		GuideStep step = resolver.resolve(method, state, legTarget, dropProduce(method), reqs);
		return b.method(method)
			.forced(forced != null && forced == method)
			.option(current)
			.requirements(reqs)
			.step(step)
			.bankItems(StepResolver.bankItems(reqs, step))
			.build();
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
		if (!options.isEmpty() && options.get(0).isAvailable())
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
