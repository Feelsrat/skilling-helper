package com.skillinghelper.banked;

import com.google.inject.Provides;
import com.skillinghelper.ActiveGuide;
import com.skillinghelper.GuideHost;
import com.skillinghelper.PathDisplay;
import com.skillinghelper.PlayerSnapshot;
import com.skillinghelper.ShortestPathBridge;
import com.skillinghelper.SkillingHelperConfig;
import com.skillinghelper.TargetTracker;
import com.skillinghelper.bank.BankAmountSwapper;
import com.skillinghelper.bank.BankCache;
import com.skillinghelper.bank.BankFilter;
import com.skillinghelper.banked.data.BankedActivities;
import com.skillinghelper.banked.data.BankedActivity;
import com.skillinghelper.banked.ui.BankedXpPanel;
import com.skillinghelper.data.Method;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import com.skillinghelper.overlay.ItemHighlightOverlay;
import com.skillinghelper.overlay.MinimapArrowOverlay;
import com.skillinghelper.overlay.SceneHighlightOverlay;
import com.skillinghelper.shops.Shops;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
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
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.bank.BankSearch;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * Looks at the bank for items that can be turned into xp (bones, herbs, logs, raw fish, gems, ores, bars, hides...),
 * shows how much xp they're worth per skill, and walks the player through using them all up, one item after the
 * other, with the same bank trips, paths and highlights as Skilling Helper's guides.
 */
@PluginDescriptor(
	name = "Banked XP Guide",
	description = "Turns the xp sitting in your bank into a step-by-step guide: what to withdraw, where to go and what "
		+ "to click",
	tags = {"banked", "xp", "bank", "guide", "skilling", "herblore", "prayer", "cooking", "fletching", "crafting",
		"smithing", "firemaking", "construction"}
)
public class BankedXpPlugin extends Plugin implements GuideHost, BankedXpPanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final String CHOICE_PREFIX = "choice.";
	private static final String SKILLING_HELPER_BANK_TIP = "Tip: press the Skilling Helper button";
	private static final String BANK_TIP = "Tip: press the Banked XP button in the bank to show only these items.";
	private static final int QUEST_REFRESH_TICKS = 100;
	/**
	 * Quests the activities need, read every {@value #QUEST_REFRESH_TICKS} ticks.
	 */
	private static final Set<Quest> QUESTS = questsUsed();

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private EventBus eventBus;
	@Inject
	private ConfigManager configManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ItemManager itemManager;
	@Inject
	private BankSearch bankSearch;
	@Inject
	private ModelOutlineRenderer outlines;
	@Inject
	private BankCache bankCache;
	@Inject
	private PlayerSnapshot snapshot;
	@Inject
	private WorldMapPointManager worldMapPointManager;
	@Inject
	private BankedXpConfig config;

	@Getter
	private volatile ActiveGuide guide = ActiveGuide.NONE;

	private SkillingHelperConfig displayConfig;
	private TargetTracker tracker;
	private ShortestPathBridge shortestPath;
	private PathDisplay path;
	private BankFilter bankFilter;
	private BankAmountSwapper amountSwapper;
	private SceneHighlightOverlay sceneOverlay;
	private MinimapArrowOverlay minimapOverlay;
	private ItemHighlightOverlay itemOverlay;
	private BankedXpPanel panel;
	private NavigationButton navButton;
	private BankedPlanner planner;

	private final Set<Quest> completedQuests = EnumSet.noneOf(Quest.class);
	private final Map<String, String> choices = new HashMap<>();
	private int questCountdown;
	private boolean profileLoaded;
	/**
	 * The account changed, so the shared bank cache may hold another account's bank.
	 */
	private boolean reloadBank;
	private boolean running;
	/**
	 * The activity the guide was on last tick, to notice when it's used up.
	 */
	private String lastActivity;
	private BankedView lastView;

	private final PriceLookup prices = id -> (int) Math.min(Integer.MAX_VALUE, itemManager.getItemPrice(id));

	@Override
	protected void startUp()
	{
		planner = new BankedPlanner(prices);
		// Display settings are shared with Skilling Helper so all the guides look alike
		displayConfig = configManager.getConfig(SkillingHelperConfig.class);
		tracker = new TargetTracker(client);
		List<Method> methods = new ArrayList<>();
		for (BankedActivity a : BankedActivities.all())
		{
			methods.add(a.getMethod());
		}
		tracker.setInterestingTargets(methods);
		tracker.addInterestingTargets(Shops.targets());
		shortestPath = new ShortestPathBridge(eventBus, displayConfig, "bankedxp:");
		path = new PathDisplay(client, displayConfig, worldMapPointManager, shortestPath);
		bankFilter = new BankFilter(client, clientThread, bankSearch, itemManager, this, configManager,
			new BankFilter.Options(BankedXpConfig.GROUP, 3, "Banked XP items", config::showBankButton,
				config::bankFilterOn));
		amountSwapper = new BankAmountSwapper(client, this, displayConfig, itemManager);
		sceneOverlay = new SceneHighlightOverlay(client, this, displayConfig, tracker, outlines);
		minimapOverlay = new MinimapArrowOverlay(client, this, displayConfig);
		itemOverlay = new ItemHighlightOverlay(this, displayConfig, itemManager);

		eventBus.register(tracker);
		eventBus.register(shortestPath);
		eventBus.register(bankFilter);
		eventBus.register(amountSwapper);
		bankFilter.startUp();
		overlayManager.add(sceneOverlay);
		overlayManager.add(minimapOverlay);
		overlayManager.add(itemOverlay);

		panel = new BankedXpPanel(this, itemManager);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Banked XP Guide")
			.icon(icon)
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				tracker.rescan();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		eventBus.unregister(tracker);
		eventBus.unregister(bankFilter);
		eventBus.unregister(amountSwapper);
		bankFilter.shutDown();
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(minimapOverlay);
		overlayManager.remove(itemOverlay);
		clientToolbar.removeNavigation(navButton);
		final PathDisplay p = path;
		final TargetTracker t = tracker;
		final ShortestPathBridge sp = shortestPath;
		clientThread.invoke(() ->
		{
			p.clear();
			t.clear();
			eventBus.unregister(sp);
		});
		guide = ActiveGuide.NONE;
		running = false;
		profileLoaded = false;
		lastActivity = null;
		lastView = null;
		panel = null;
	}

	@Provides
	BankedXpConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankedXpConfig.class);
	}

	// ---- Game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGIN_SCREEN || e.getGameState() == GameState.HOPPING)
		{
			if (e.getGameState() == GameState.LOGIN_SCREEN)
			{
				profileLoaded = false;
				reloadBank = true;
				running = false;
			}
			guide = ActiveGuide.NONE;
			path.clear();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		profileLoaded = false;
		reloadBank = true;
		running = false;
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
	public void onGameTick(GameTick e)
	{
		if (!profileLoaded)
		{
			loadProfile();
		}
		if (--questCountdown <= 0)
		{
			refreshQuests();
		}
		refresh();
	}

	private void loadProfile()
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			return;
		}
		// Skilling Helper shares this cache and may have a newer copy than the saved one, unless the account changed
		if (bankCache.get() == null || reloadBank)
		{
			bankCache.load();
			reloadBank = false;
		}
		choices.clear();
		for (BankedActivity a : BankedActivities.all())
		{
			String saved = configManager.getRSProfileConfiguration(BankedXpConfig.GROUP, CHOICE_PREFIX + a.getGroup());
			if (saved != null)
			{
				choices.put(a.getGroup(), saved);
			}
		}
		refreshQuests();
		profileLoaded = true;
	}

	private void refreshQuests()
	{
		questCountdown = QUEST_REFRESH_TICKS;
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		completedQuests.clear();
		for (Quest q : QUESTS)
		{
			if (q.getState(client) == QuestState.FINISHED)
			{
				completedQuests.add(q);
			}
		}
	}

	private static Set<Quest> questsUsed()
	{
		Set<Quest> quests = EnumSet.noneOf(Quest.class);
		for (BankedActivity a : BankedActivities.all())
		{
			quests.addAll(a.getMethod().getQuests());
		}
		return quests;
	}

	// ---- Building the guide ----

	/**
	 * Re-reads the bank and position, picks the activity and step, and updates the overlays and panel. Client thread.
	 */
	private void refresh()
	{
		final BankedXpPanel p = panel;
		if (p == null)
		{
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN || !profileLoaded)
		{
			show(p, BankedView.LOGGED_OUT);
			return;
		}

		PlayerState state = snapshot.take(null, completedQuests, completedQuests, displayConfig.preferShops());
		BankedView.BankedViewBuilder view = BankedView.builder()
			.loggedIn(true)
			.bankKnown(state.bankKnown());
		if (!state.bankKnown())
		{
			guide = ActiveGuide.NONE;
			path.show(null);
			show(p, view.running(running).build());
			return;
		}

		List<BankedPlanner.Group> groups = planner.groups(state, choices);
		List<BankedPlanner.Option> queue = BankedPlanner.queue(groups);
		view.groups(groups).queue(queue).totals(BankedPlanner.totals(queue, state));

		if (!running)
		{
			guide = ActiveGuide.NONE;
			path.show(null);
			lastActivity = null;
			show(p, view.running(false).build());
			return;
		}
		if (queue.isEmpty())
		{
			finish();
			show(p, view.running(false).build());
			return;
		}

		BankedPlanner.Option current = queue.get(0);
		announce(current, queue);
		BankedPlanner.Plan plan = planner.plan(current.getActivity(), state);
		GuideStep step = withBankTip(plan.getStep());
		ActiveGuide next = ActiveGuide.builder()
			.skill(current.getActivity().getSkill())
			.level(state.level(current.getActivity().getSkill()))
			.method(plan.getMethod())
			.requirements(plan.getRequirements())
			.step(step)
			.bankItems(StepResolver.bankItems(plan.getRequirements(), step))
			.player(state)
			.hasGuide(true)
			.wikiPage(plan.getMethod().getWikiPage())
			.build();
		step = showPath(step);
		guide = next.toBuilder().step(step).build();
		show(p, view.running(true).current(current).step(step).requirements(plan.getRequirements()).build());
	}

	private void show(BankedXpPanel p, BankedView view)
	{
		if (view.equals(lastView))
		{
			return;
		}
		lastView = view;
		SwingUtilities.invokeLater(() -> p.update(view));
	}

	/**
	 * Says in the chat when the guide moves on from a used-up item.
	 */
	private void announce(BankedPlanner.Option current, List<BankedPlanner.Option> queue)
	{
		String id = current.getActivity().getId();
		if (lastActivity != null && !lastActivity.equals(id) && config.chatMessages())
		{
			BankedActivity previous = BankedActivities.byId(lastActivity);
			boolean usedUp = previous != null && !BankedPlanner.SKIP.equals(choices.get(previous.getGroup()))
				&& queue.stream().noneMatch(o -> o.getActivity().getGroup().equals(previous.getGroup()));
			if (usedUp)
			{
				chat("Done with your " + previous.getItemName().toLowerCase() + ". Next: "
					+ current.getActivity().getName().toLowerCase() + " (" + queue.size() + " left).");
			}
		}
		lastActivity = id;
	}

	private void finish()
	{
		if (running && config.chatMessages())
		{
			chat("That's all the banked xp you've chosen to use. Nice!");
		}
		running = false;
		lastActivity = null;
		guide = ActiveGuide.NONE;
		path.show(null);
	}

	private void chat(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Banked XP: " + message, null);
	}

	/**
	 * The shared step engine mentions Skilling Helper's bank button; point at this plugin's instead.
	 */
	private static GuideStep withBankTip(GuideStep step)
	{
		boolean mentions = step.getDetails().stream().anyMatch(l -> l.startsWith(SKILLING_HELPER_BANK_TIP));
		if (!mentions)
		{
			return step;
		}
		GuideStep.GuideStepBuilder b = step.toBuilder().clearDetails();
		for (String line : step.getDetails())
		{
			b.line(line.startsWith(SKILLING_HELPER_BANK_TIP) ? BANK_TIP : line);
		}
		return b.build();
	}

	/**
	 * Points the arrows and path at the step's destination, and explains when Shortest Path can't get there.
	 */
	private GuideStep showPath(GuideStep step)
	{
		boolean showPath = step.getType() != StepType.TRAIN && step.getType() != StepType.DONE;
		path.show(showPath ? step.getRoute() : null);
		if (!showPath || !path.isUnreachable(step.getRoute()))
		{
			return step;
		}
		return step.toBuilder()
			.line("Shortest Path can't find a full route there with your current unlocks, so it shows how close "
				+ "it can get. You may need a quest, level or item for a transport, or a teleport closer.")
			.build();
	}

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onStart()
	{
		clientThread.invokeLater(() ->
		{
			running = true;
			lastActivity = null;
			refresh();
		});
	}

	@Override
	public void onStop()
	{
		clientThread.invokeLater(() ->
		{
			running = false;
			refresh();
		});
	}

	@Override
	public void onChoice(String group, String choice)
	{
		clientThread.invokeLater(() ->
		{
			if (Objects.equals(choices.get(group), choice))
			{
				return;
			}
			choices.put(group, choice);
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				configManager.setRSProfileConfiguration(BankedXpConfig.GROUP, CHOICE_PREFIX + group, choice);
			}
			refresh();
		});
	}

	@Override
	public void onToggleBankFilter()
	{
		bankFilter.toggle();
	}

	@Override
	public void onOpenWiki(String page)
	{
		if (page != null)
		{
			LinkBrowser.browse(WIKI_URL + page);
		}
	}
}
