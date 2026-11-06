package com.skillinghelper.planner;

import com.google.inject.Provides;
import com.skillinghelper.SkillingHelperConfig;
import com.skillinghelper.bank.BankCache;
import com.skillinghelper.bank.BankItems;
import com.skillinghelper.data.Outleveled;
import com.skillinghelper.data.RatePace;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.planner.api.StepKind;
import com.skillinghelper.planner.ui.PlannerPanel;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
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
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
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
 * Plans the whole account: which quest, level, diary, boss (and, as more plugins plug in, minigame, unlock, combat
 * task...) to do next on the way to max, each step opening its own guide in the plugin that has it. Steps come
 * from {@link StepProviders}; {@link PathPlanner} puts them in order.
 */
@Slf4j
@PluginDescriptor(
	name = "Account Planner",
	description = "Your path to max: quests, levels, diaries and bosses in a sensible order, each opening its guide",
	tags = {"planner", "plan", "path", "max", "progression", "account", "goals", "efficient", "guide", "helper"}
)
public class AccountPlannerPlugin extends Plugin implements PlannerPanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	/**
	 * How often quest states and diary/boss progress are read again (quests are client scripts).
	 */
	private static final int REFRESH_TICKS = 50;
	/**
	 * How often the route checks whether its current step is done.
	 */
	private static final int ROUTE_REFRESH_TICKS = 5;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private EventBus eventBus;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ConfigManager configManager;
	@Inject
	private AccountPlannerConfig config;
	@Inject
	private ItemManager itemManager;
	@Inject
	private PluginManager pluginManager;
	@Inject
	private GuideRepository guides;
	@Inject
	private BankCache bankCache;
	@Inject
	private ScheduledExecutorService executor;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private RouteOverlay routeOverlay;

	private PlannerPanel panel;
	private NavigationButton navButton;

	private final Map<Quest, QuestState> quests = new HashMap<>();
	private final Set<Quest> wantedQuests = new HashSet<>();
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private final Set<String> skipped = new LinkedHashSet<>();
	private Map<Integer, Integer> bankCounts;
	private String profileLoaded;
	private int countdown;
	private boolean dirty;

	private SkillHours skillHours;
	private RatePace pace;
	private Object lastInputs;
	/**
	 * Bumped for every plan started, so a slow plan never overwrites a newer one.
	 */
	private int generation;
	/**
	 * Every step of the last plan worked out, by id, for the panel's buttons.
	 */
	private Map<String, PlanStep> planned = Collections.emptyMap();
	private PlannerView shownView;
	private PathPlanner.Plan lastPlan;
	private List<String> lastProviders = Collections.emptyList();
	private Set<StepKind> lastHidden = Collections.emptySet();
	private boolean lastWilderness;

	/**
	 * The guided route: one step at a time, each started in its own plugin, moving on by itself.
	 */
	private boolean routeOn;
	private String routeStepId;
	private String routeStepTitle;
	/**
	 * Steps finished since the route was started this session.
	 */
	private int routeDone;
	/**
	 * Open the current step's guide again once the first plan after a login is in.
	 */
	private boolean resumePending;
	private int routeCountdown;

	@Override
	protected void startUp()
	{
		panel = new PlannerPanel(this);
		shownView = null;
		lastInputs = null;
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Account Planner")
			.icon(icon)
			.priority(12)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(routeOverlay);
		countdown = 0;
		dirty = true;
		clientThread.invokeLater(this::replan);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(routeOverlay);
		routeOverlay.setLines(null, null, null, 0);
		routeOn = false;
		routeStepId = null;
		routeStepTitle = null;
		routeDone = 0;
		resumePending = false;
		lastPlan = null;
		panel = null;
		shownView = null;
		lastInputs = null;
		planned = Collections.emptyMap();
		quests.clear();
		wantedQuests.clear();
		levels.clear();
		skipped.clear();
		bankCounts = null;
		profileLoaded = null;
	}

	@Provides
	AccountPlannerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AccountPlannerConfig.class);
	}

	// ---- Game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			countdown = 0;
			dirty = true;
		}
		else if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			quests.clear();
			levels.clear();
			bankCounts = null;
			profileLoaded = null;
			routeOn = false;
			routeStepId = null;
			lastPlan = null;
			routeOverlay.setLines(null, null, null, 0);
			replan();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		profileLoaded = null;
		bankCounts = null;
		dirty = true;
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		Integer before = levels.put(e.getSkill(), e.getLevel());
		if (before != null && before != e.getLevel())
		{
			dirty = true;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (e.getContainerId() == InventoryID.BANK)
		{
			bankCounts = BankItems.count(e.getItemContainer(), itemManager);
			dirty = true;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (AccountPlannerConfig.GROUP.equals(e.getGroup()) || Wilderness.isSettingChange(e)
			|| Outleveled.isSettingChange(e) || SkillingHelperConfig.GROUP.equals(e.getGroup()))
		{
			clientThread.invokeLater(() ->
			{
				dirty = true;
				replan();
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
		if (--countdown <= 0)
		{
			countdown = REFRESH_TICKS;
			readQuests();
			dirty = true;
		}
		else if (routeOn && --routeCountdown <= 0)
		{
			// Notice the current step is done within a few seconds: read its quest and ask the providers again
			routeCountdown = ROUTE_REFRESH_TICKS;
			PlanStep current = routeStepId == null ? null : planned.get(routeStepId);
			if (current != null && current.getFinishes() != null)
			{
				quests.put(current.getFinishes(), current.getFinishes().getState(client));
			}
			dirty = true;
		}
		if (dirty)
		{
			replan();
		}
	}

	// ---- Reading the account ----

	private void readQuests()
	{
		for (Quest q : wantedQuests)
		{
			quests.put(q, q.getState(client));
		}
	}

	private void loadProfile()
	{
		String key = configManager.getRSProfileKey();
		if (Objects.equals(key, profileLoaded))
		{
			return;
		}
		profileLoaded = key;
		skipped.clear();
		skipped.addAll(split(configManager.getRSProfileConfiguration(AccountPlannerConfig.GROUP,
			AccountPlannerConfig.KEY_SKIPPED)));
		String route = configManager.getRSProfileConfiguration(AccountPlannerConfig.GROUP,
			AccountPlannerConfig.KEY_ROUTE);
		routeOn = route != null;
		routeStepId = route == null || route.isEmpty() ? null : route;
		routeStepTitle = null;
		resumePending = routeOn;
		routeDone = 0;
	}

	private AccountState readAccount()
	{
		Map<Skill, Integer> real = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			real.put(s, client.getRealSkillLevel(s));
		}
		levels.putAll(real);
		Map<Integer, Integer> owned = carried();
		Map<Integer, Integer> bank = bankCounts;
		if (bank == null)
		{
			ItemCounts cached = bankCache.get();
			bank = cached == null ? null : cached.asMap();
		}
		if (bank != null)
		{
			bank.forEach((id, n) -> owned.merge(id, n, Integer::sum));
		}
		Player me = client.getLocalPlayer();
		return AccountState.builder()
			.levels(real)
			.combatLevel(me == null ? 3 : me.getCombatLevel())
			.quests(new HashMap<>(quests))
			.varbit(client::getVarbitValue)
			.varp(client::getVarpValue)
			.profileConfig(this::profileConfig)
			.owned(owned)
			.bankKnown(bank != null)
			.price(id -> (int) Math.min(Integer.MAX_VALUE, itemManager.getItemPrice(id)))
			.members(client.getWorldType().contains(WorldType.MEMBERS))
			.allowWilderness(Wilderness.allowed(configManager))
			.hideOutleveled(Outleveled.hidden(configManager))
			.build();
	}

	/**
	 * "group.key" from this account's profile config.
	 */
	private String profileConfig(String groupAndKey)
	{
		int dot = groupAndKey.indexOf('.');
		if (dot <= 0)
		{
			return null;
		}
		return configManager.getRSProfileConfiguration(groupAndKey.substring(0, dot), groupAndKey.substring(dot + 1));
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

	// ---- The plan ----

	/**
	 * Asks every provider for its steps, plans them when anything changed, and sends the panel a new view when
	 * that changed. Client thread.
	 */
	private void replan()
	{
		dirty = false;
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
		if (!loggedIn)
		{
			lastInputs = null;
			generation++;
			planned = Collections.emptyMap();
			show(PlannerView.builder().loggedIn(false).build());
			return;
		}
		loadProfile();
		AccountState account = readAccount();
		List<PlanStep> steps = collect(account);
		if (askForQuests(steps))
		{
			// New quests to read: read them now and ask again, so the first plan doesn't treat them as not started
			readQuests();
			account = account.toBuilder().quests(new HashMap<>(quests)).build();
			steps = collect(account);
		}
		List<String> names = new ArrayList<>();
		StepProviders.ALL.forEach(p -> names.add(p.name()));

		Set<StepKind> hidden = hiddenKinds();
		RatePace currentPace = configManager.getConfiguration(SkillingHelperConfig.GROUP, "ratePace", RatePace.class);
		if (currentPace == null)
		{
			currentPace = RatePace.RELAXED;
		}
		if (skillHours == null || currentPace != pace)
		{
			pace = currentPace;
			skillHours = new SkillHours(guides::methods, pace);
		}
		List<Object> inputs = Arrays.asList(steps, account.getLevels(), account.getQuests(),
			account.isBankKnown() ? account.coins() : -1L, new HashSet<>(skipped), hidden,
			account.isAllowWilderness(), pace, config.planLength());
		if (inputs.equals(lastInputs))
		{
			return;
		}
		lastInputs = inputs;

		// Planning takes tens of milliseconds on slow machines, so it runs off the client thread. It only reads
		// levels, quests, coins and settings, so the client-bound readers are left out of the copy it gets.
		AccountState snapshot = account.toBuilder().varbit(id -> 0).varp(id -> 0).profileConfig(k -> null)
			.price(id -> 0).build();
		PathPlanner.Settings settings = new PathPlanner.Settings(new HashSet<>(skipped), hidden);
		SkillHours hours = skillHours;
		int gen = ++generation;
		List<PlanStep> planSteps = steps;
		executor.execute(() ->
		{
			try
			{
				PathPlanner.Plan plan = PathPlanner.plan(planSteps, snapshot, hours, settings);
				clientThread.invokeLater(() ->
				{
					if (gen == generation && client.getGameState() == GameState.LOGGED_IN)
					{
						lastPlan = plan;
						lastProviders = names;
						lastHidden = hidden;
						lastWilderness = snapshot.isAllowWilderness();
						Map<String, PlanStep> byId = new HashMap<>();
						plan.getSteps().forEach(p -> byId.put(p.getStep().getId(), p.getStep()));
						planned = byId;
						followRoute();
						publish();
					}
				});
			}
			catch (RuntimeException ex)
			{
				log.debug("Account plan failed", ex);
			}
		});
	}

	/**
	 * Sends the panel the last plan, with the route's current step first while the route runs. Client thread.
	 */
	private void publish()
	{
		PathPlanner.Plan plan = lastPlan;
		if (plan == null)
		{
			return;
		}
		List<PathPlanner.Planned> order = new ArrayList<>(plan.getSteps());
		if (routeOn && routeStepId != null)
		{
			for (int i = 0; i < order.size(); i++)
			{
				if (order.get(i).getStep().getId().equals(routeStepId))
				{
					order.add(0, order.remove(i));
					break;
				}
			}
		}
		List<PlannerView.Row> rows = new ArrayList<>();
		for (PathPlanner.Planned p : order)
		{
			if (rows.size() >= config.planLength())
			{
				break;
			}
			PlanStep s = p.getStep();
			rows.add(new PlannerView.Row(s.getId(), s.getKind(), s.getTitle(), s.getWhy(), p.getReason(),
				p.getHours(), s.getHowTo(), s.getNotes(), s.getOpenIn(), s.getOpen() != null, s.getWikiPage(),
				s.isWilderness()));
		}
		show(PlannerView.builder()
			.loggedIn(true)
			.rows(rows)
			.planSize(plan.getSteps().size())
			.totalHours(plan.getTotalHours())
			.progress(new EnumMap<>(plan.getProgress()))
			.hiddenKinds(lastHidden)
			.skipped(skipped.size())
			.unreachable(plan.getUnreachable())
			.wildernessAllowed(lastWilderness)
			.providers(lastProviders)
			.routeOn(routeOn)
			.routeDone(routeDone)
			.build());
		updateOverlay(order);
	}

	// ---- The route ----

	/**
	 * While the route runs: when its current step is done (gone from the plan), start the next one in its own
	 * plugin's guide. After a login it opens the current step's guide again. Client thread.
	 */
	private void followRoute()
	{
		if (!routeOn || lastPlan == null)
		{
			return;
		}
		List<PathPlanner.Planned> order = lastPlan.getSteps();
		if (order.isEmpty())
		{
			chat("the route is finished: every planned step is done. Well played!");
			setRoute(false, null);
			return;
		}
		PlanStep current = routeStepId == null ? null : planned.get(routeStepId);
		if (current == null)
		{
			String finished = routeStepTitle;
			if (routeStepId != null)
			{
				routeDone++;
			}
			begin(order.get(0).getStep(), finished);
		}
		else if (resumePending)
		{
			resumePending = false;
			openGuide(current, true);
		}
	}

	private void begin(PlanStep step, String finished)
	{
		resumePending = false;
		routeStepTitle = step.getTitle();
		setRoute(true, step.getId());
		chat((finished != null ? "done: " + finished + ". " : "") + "Next: " + step.getTitle()
			+ (step.getOpenIn() != null ? " (guided in " + step.getOpenIn() + ")" : "") + ".");
		openGuide(step, true);
	}

	/**
	 * Posts the step's open request so its plugin starts guiding it.
	 */
	private void openGuide(PlanStep s, boolean fromRoute)
	{
		if (s.getOpen() == null)
		{
			if (fromRoute)
			{
				chat("no guide for this one yet: the planner panel says how to do it. Press Skip there when "
					+ "you're done with it.");
			}
			return;
		}
		if (!isActive(s.getOpenIn()))
		{
			chat("turn on the " + s.getOpenIn() + " plugin (in the plugin list) so it can guide this step.");
			return;
		}
		eventBus.post(s.getOpen());
	}

	private void setRoute(boolean on, String stepId)
	{
		routeOn = on;
		routeStepId = on ? stepId : null;
		if (!on)
		{
			routeStepTitle = null;
		}
		if (profileLoaded == null)
		{
			return;
		}
		if (on)
		{
			configManager.setRSProfileConfiguration(AccountPlannerConfig.GROUP, AccountPlannerConfig.KEY_ROUTE,
				stepId == null ? "" : stepId);
		}
		else
		{
			configManager.unsetRSProfileConfiguration(AccountPlannerConfig.GROUP, AccountPlannerConfig.KEY_ROUTE);
		}
	}

	private void updateOverlay(List<PathPlanner.Planned> order)
	{
		if (!routeOn || order.isEmpty() || !config.routeBox())
		{
			routeOverlay.setLines(null, null, null, 0);
			return;
		}
		PlanStep now = order.get(0).getStep();
		PlanStep next = order.size() > 1 ? order.get(1).getStep() : null;
		routeOverlay.setLines(now.getKind().getLabel() + ": " + now.getTitle(), now.getOpenIn(),
			next == null ? null : next.getTitle(), routeDone);
	}

	private void chat(String message)
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Account route: " + message, null);
		}
	}

	private static List<PlanStep> collect(AccountState account)
	{
		List<PlanStep> steps = new ArrayList<>();
		for (PlanStepProvider p : StepProviders.ALL)
		{
			try
			{
				steps.addAll(p.steps(account));
			}
			catch (RuntimeException ex)
			{
				log.debug("Plan step provider {} failed", p.name(), ex);
			}
		}
		return steps;
	}

	/**
	 * Remembers which quests the steps mention, so only those are read. True when there are new ones.
	 */
	private boolean askForQuests(List<PlanStep> steps)
	{
		boolean added = false;
		for (PlanStep s : steps)
		{
			for (Quest q : s.getQuests())
			{
				added |= wantedQuests.add(q);
			}
			for (Quest q : s.getQuestsStarted())
			{
				added |= wantedQuests.add(q);
			}
			if (s.getFinishes() != null)
			{
				added |= wantedQuests.add(s.getFinishes());
			}
		}
		return added;
	}

	private void show(PlannerView view)
	{
		PlannerPanel target = panel;
		if (target != null && !view.equals(shownView))
		{
			shownView = view;
			SwingUtilities.invokeLater(() -> target.update(view));
		}
	}

	private Set<StepKind> hiddenKinds()
	{
		Set<StepKind> out = EnumSet.noneOf(StepKind.class);
		for (String s : split(configManager.getConfiguration(AccountPlannerConfig.GROUP,
			AccountPlannerConfig.KEY_HIDDEN_KINDS)))
		{
			try
			{
				out.add(StepKind.valueOf(s));
			}
			catch (IllegalArgumentException ex)
			{
				log.debug("Unknown step kind {}", s);
			}
		}
		return out;
	}

	private static List<String> split(String raw)
	{
		List<String> out = new ArrayList<>();
		if (raw == null)
		{
			return out;
		}
		for (String s : raw.split(","))
		{
			if (!s.trim().isEmpty())
			{
				out.add(s.trim());
			}
		}
		return out;
	}

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onOpen(String stepId)
	{
		clientThread.invokeLater(() ->
		{
			PlanStep s = planned.get(stepId);
			if (s == null || s.getOpen() == null)
			{
				return;
			}
			if (!isActive(s.getOpenIn()))
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Account Planner: turn on the "
					+ s.getOpenIn() + " plugin (in the plugin list) to open this guide.", null);
				return;
			}
			eventBus.post(s.getOpen());
		});
	}

	private boolean isActive(String pluginName)
	{
		if (pluginName == null)
		{
			return true;
		}
		for (Plugin p : pluginManager.getPlugins())
		{
			if (pluginName.equals(p.getName()))
			{
				return pluginManager.isPluginActive(p);
			}
		}
		return false;
	}

	@Override
	public void onStartRoute()
	{
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN || lastPlan == null || lastPlan.getSteps().isEmpty())
			{
				return;
			}
			routeDone = 0;
			routeOn = true;
			begin(lastPlan.getSteps().get(0).getStep(), null);
			publish();
		});
	}

	@Override
	public void onStopRoute()
	{
		clientThread.invokeLater(() ->
		{
			setRoute(false, null);
			publish();
		});
	}

	@Override
	public void onSkip(String stepId)
	{
		clientThread.invokeLater(() ->
		{
			if (stepId.equals(routeStepId))
			{
				// Skipped, not done: the route just moves on
				routeStepTitle = null;
				routeDone--;
			}
			skipped.add(stepId);
			saveSkipped();
			replan();
		});
	}

	@Override
	public void onUnskipAll()
	{
		clientThread.invokeLater(() ->
		{
			skipped.clear();
			saveSkipped();
			replan();
		});
	}

	private void saveSkipped()
	{
		if (skipped.isEmpty())
		{
			configManager.unsetRSProfileConfiguration(AccountPlannerConfig.GROUP, AccountPlannerConfig.KEY_SKIPPED);
		}
		else
		{
			configManager.setRSProfileConfiguration(AccountPlannerConfig.GROUP, AccountPlannerConfig.KEY_SKIPPED,
				String.join(",", skipped));
		}
	}

	@Override
	public void onShowKind(StepKind kind, boolean show)
	{
		Set<StepKind> hidden = hiddenKinds();
		if (show)
		{
			hidden.remove(kind);
		}
		else
		{
			hidden.add(kind);
		}
		// Saving fires ConfigChanged, which plans again
		configManager.setConfiguration(AccountPlannerConfig.GROUP, AccountPlannerConfig.KEY_HIDDEN_KINDS,
			hidden.stream().map(Enum::name).collect(Collectors.joining(",")));
	}

	@Override
	public void onAllowWilderness(boolean allow)
	{
		Wilderness.setAllowed(configManager, allow);
	}

	@Override
	public void onWiki(String page)
	{
		LinkBrowser.browse(WIKI_URL + page);
	}
}
