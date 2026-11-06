package com.skillinghelper.clue;

import com.google.inject.Provides;
import com.skillinghelper.OpenClueGuideRequest;
import com.skillinghelper.ShortestPathLink;
import com.skillinghelper.bank.BankFilter;
import com.skillinghelper.bank.BankItems;
import com.skillinghelper.clue.data.ClueData;
import com.skillinghelper.clue.data.ClueItem;
import com.skillinghelper.clue.data.ClueTier;
import com.skillinghelper.clue.data.ClueWays;
import com.skillinghelper.clue.ui.CluePanel;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.DiaryGuide;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.overlay.NameTargets;
import com.skillinghelper.travel.TravelAdvisor;
import com.skillinghelper.travel.TravelPick;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.ChatMessageType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;

/**
 * Clue scroll helper around RuneLite's own Clue Scroll plugin: which clues, scroll boxes and caskets you already have
 * (bank and inventory), how to get a clue of each tier (guided step by step), what to bring, STASH units, and what
 * each tier's caskets can give. Solving the steps themselves is left to the built-in Clue Scroll plugin.
 */
@Slf4j
@PluginDescriptor(
	name = "Clue Scroll Guide",
	description = "How to get clue scrolls of each tier, the clues and caskets you already have, what to bring, "
		+ "STASH units and what each tier's rewards can be",
	tags = {"clue", "clues", "scroll", "treasure", "trails", "casket", "stash", "watson", "guide", "helper"}
)
public class ClueScrollGuidePlugin extends Plugin implements CluePanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final String CLUE_PLUGIN = "ClueScrollPlugin";
	private static final int SLOW_REFRESH_TICKS = 50;
	private static final Pattern COMPLETED = Pattern.compile(
		"You have completed ([\\d,]+) (beginner|easy|medium|hard|elite|master) Treasure Trails?");

	@Inject
	private BankFilter bankFilter;
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
	private ClueScrollGuideConfig config;
	@Inject
	private TravelAdvisor travel;
	@Inject
	private ClueItemOverlay itemOverlay;
	@Inject
	private ModelOutlineRenderer outlineRenderer;

	private NameTargets targets;
	private ClueStepReader stepReader;
	private ClueSceneOverlay sceneOverlay;
	private CluePanel panel;
	private NavigationButton navButton;

	private ClueTier tier = ClueTier.BEGINNER;
	private boolean running;
	/**
	 * The way to get a clue the player clicked, per tier; none = best one they can do.
	 */
	private final Map<ClueTier, String> pickedWays = new EnumMap<>(ClueTier.class);
	/**
	 * The STASH unit the player asked to be guided to, or null.
	 */
	private String stashKey;

	/**
	 * Item id to what it is (empty when not a clue item). Names are read on the client thread.
	 */
	private final Map<Integer, Optional<ClueItem>> clueItems = new HashMap<>();
	private Map<Integer, Integer> bankCounts;
	private boolean childrenOfTheSun;
	private boolean cluePluginOn = true;
	private int slowCountdown;
	private boolean checkShortestPath = true;

	@Getter
	private volatile DiaryStep step;
	private WorldPoint pathTarget;
	private boolean hintArrowSet;
	private ClueView shownView;

	@Override
	protected void startUp()
	{
		targets = new NameTargets(client);
		stepReader = new ClueStepReader(client, pluginManager);
		sceneOverlay = new ClueSceneOverlay(client, this, config, targets, outlineRenderer);
		eventBus.register(targets);
		bankFilter.attach(this);
		overlayManager.add(sceneOverlay);
		overlayManager.add(itemOverlay);
		tier = savedTier();
		panel = new CluePanel(this);
		shownView = null;
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Clue Scroll Guide")
			.icon(icon)
			.priority(14)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		slowCountdown = 0;
		checkShortestPath = true;
		clientThread.invokeLater(this::refresh);
	}

	@Override
	protected void shutDown()
	{
		eventBus.unregister(targets);
		bankFilter.detach(this);
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
		stashKey = null;
		pickedWays.clear();
		bankCounts = null;
		clueItems.clear();
		panel = null;
		shownView = null;
	}

	@Provides
	ClueScrollGuideConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClueScrollGuideConfig.class);
	}

	// ---- Game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			slowCountdown = 0;
			checkShortestPath = true;
		}
		else if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
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
		if (ClueScrollGuideConfig.GROUP.equals(e.getGroup()) || Wilderness.isSettingChange(e)
			|| ("runelite".equals(e.getGroup()) && CLUE_PLUGIN.equalsIgnoreCase(e.getKey())))
		{
			clientThread.invokeLater(() ->
			{
				slowCountdown = 0;
				clearWay();
				refresh();
			});
		}
	}

	/**
	 * Remembers how many clues of each tier this account finished, from the message opening a casket gives.
	 */
	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		Matcher m = COMPLETED.matcher(Text.removeTags(e.getMessage()));
		if (m.find())
		{
			configManager.setRSProfileConfiguration(ClueScrollGuideConfig.GROUP, "completed." + m.group(2),
				m.group(1).replace(",", ""));
			refresh();
		}
	}

	/**
	 * Clues of the tier this account has finished, as last seen in the chat, or 0.
	 *
	 * @param profileConfig "group.key" to this account's saved value
	 */
	public static int completed(UnaryOperator<String> profileConfig, ClueTier tier)
	{
		String s = profileConfig.apply(ClueScrollGuideConfig.GROUP + ".completed." + tier.key());
		try
		{
			return s == null ? 0 : Integer.parseInt(s);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	/**
	 * Another plugin in this jar (e.g. the Collection Log Guide) asks to show a tier.
	 */
	@Subscribe
	public void onOpenClueGuideRequest(OpenClueGuideRequest e)
	{
		ClueTier t = ClueTier.fromKey(e.getTier() == null ? "" : e.getTier().toLowerCase());
		if (t == null)
		{
			return;
		}
		configManager.setConfiguration(ClueScrollGuideConfig.GROUP, "tier", t.name());
		tier = t;
		stashKey = null;
		running = true;
		checkShortestPath = true;
		refresh();
		NavigationButton button = navButton;
		if (button != null)
		{
			SwingUtilities.invokeLater(() -> clientToolbar.openPanel(button));
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
			if (config.shortestPath() && (running || stashKey != null))
			{
				ShortestPathLink.warnIfMissing(client, pluginManager);
			}
		}
		refresh();
	}

	// ---- Reading the player ----

	private ClueItem classify(int id)
	{
		return clueItems.computeIfAbsent(id, k -> Optional.ofNullable(ClueItem.of(k,
			itemManager.getItemComposition(k).getName()))).orElse(null);
	}

	private boolean cluePluginActive()
	{
		for (Plugin p : pluginManager.getPlugins())
		{
			if (CLUE_PLUGIN.equals(p.getClass().getSimpleName()))
			{
				return pluginManager.isPluginActive(p);
			}
		}
		return false;
	}

	private DiaryPlayer readPlayer()
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> boosted = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
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
			.quests(childrenOfTheSun ? Map.of(Quest.CHILDREN_OF_THE_SUN, QuestState.FINISHED)
				: Collections.emptyMap())
			.varbit(client::getVarbitValue)
			.varp(client::getVarpValue)
			.carried(carried())
			.bank(bankCounts == null ? null : new HashMap<>(bankCounts))
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
		if (loggedIn && --slowCountdown <= 0)
		{
			slowCountdown = SLOW_REFRESH_TICKS;
			childrenOfTheSun = Quest.CHILDREN_OF_THE_SUN.getState(client) == QuestState.FINISHED;
			cluePluginOn = cluePluginActive();
		}
		DiaryPlayer p = loggedIn ? readPlayer() : null;
		ClueHoldings holdings = p == null ? ClueHoldings.of(Collections.emptyMap(), null, id -> null)
			: ClueHoldings.of(p.getCarried(), p.getBank(), this::classify);
		DiaryStep next = null;
		if (p != null && (running || stashKey != null))
		{
			next = resolve(holdings, p);
			Location dest = next.getDestination();
			if (dest != null && !client.getTopLevelWorldView().isInstance())
			{
				Map<Integer, Integer> bank = p.getBank();
				List<TravelPick> picks = travel.plan(id -> p.getCarried().getOrDefault(id, 0),
					id -> bank == null ? 0 : bank.getOrDefault(id, 0), p.getLocation(),
					List.of(dest.getPoint()), dest.getName(), Wilderness.contains(dest) && p.isAllowWilderness());
				if (!picks.isEmpty())
				{
					next = resolve(holdings, p.toBuilder().travel(picks).build());
				}
			}
		}
		step = next;
		showInBank();
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
		ClueView view = buildView(p, holdings, next);
		CluePanel target = panel;
		if (target != null && !view.equals(shownView))
		{
			shownView = view;
			SwingUtilities.invokeLater(() -> target.update(view));
		}
	}

	private DiaryStep resolve(ClueHoldings holdings, DiaryPlayer p)
	{
		ClueData.Stash stash = stash(stashKey);
		if (stash != null)
		{
			return ClueGuide.reword(DiaryGuide.resolve(ClueGuide.stashTask(stash), p));
		}
		ClueStep clueStep = holdings.get(tier, ClueItem.Kind.CLUE).total() > 0
			? stepReader.read(p.getLocation(), p.getBank()) : null;
		return ClueGuide.resolve(tier, holdings, p, pickedWays.get(tier), cluePluginOn, clueStep);
	}

	private static ClueData.Stash stash(String key)
	{
		if (key == null)
		{
			return null;
		}
		for (ClueTier t : ClueTier.values())
		{
			for (ClueData.Stash s : ClueData.stashes(t))
			{
				if (s.key().equals(key))
				{
					return s;
				}
			}
		}
		return null;
	}

	private ClueView buildView(DiaryPlayer p, ClueHoldings holdings, DiaryStep current)
	{
		ClueView.ClueViewBuilder v = ClueView.builder()
			.loggedIn(p != null)
			.tier(tier)
			.completed(p == null ? 0 : completed(this::profileConfig, tier))
			.holdings(holdings)
			.running(running || stashKey != null)
			.step(current)
			.cluePluginOn(cluePluginOn)
			.wildernessAllowed(Wilderness.allowed(configManager));
		if (p == null)
		{
			return v.build();
		}
		v.members(p.isMembers());
		DiaryTask way = ClueGuide.pickWay(tier, p, pickedWays.get(tier));
		List<ClueView.WayRow> ways = new ArrayList<>();
		for (DiaryTask t : ClueWays.ways(tier))
		{
			ways.add(new ClueView.WayRow(t.key(), t.label(), DiaryGuide.missing(t, p), DiaryGuide.cost(t, p),
				way != null && way.key().equals(t.key()), t.isWilderness()));
		}
		v.ways(ways);

		List<ClueView.KitRow> kit = new ArrayList<>();
		kit.add(new ClueView.KitRow("Spade", ClueGuide.owns(p, ClueGuide.SPADES)));
		if (tier.ordinal() >= ClueTier.HARD.ordinal())
		{
			kit.add(new ClueView.KitRow("Light source (lit lantern or torch)", ClueGuide.owns(p, ClueGuide.LIGHTS)));
		}
		v.kit(kit);

		v.constructionLevel(p.level(Skill.CONSTRUCTION));
		List<ClueView.StashRow> stashes = new ArrayList<>();
		for (ClueData.Stash s : ClueData.stashes(tier))
		{
			stashes.add(new ClueView.StashRow(s.key(), s.getName(), s.getWhere(), s.getItems(), ownAll(s, p),
				Wilderness.contains(s.getPoint()), s.key().equals(stashKey)));
		}
		v.stashes(stashes);
		v.averageCasket(ClueRewards.averageValue(tier, this::price));
		v.topUniques(ClueRewards.topUniques(tier, this::price, 6));
		return v.build();
	}

	private int price(int id)
	{
		return (int) Math.min(Integer.MAX_VALUE, itemManager.getItemPrice(id));
	}

	private static Boolean ownAll(ClueData.Stash s, DiaryPlayer p)
	{
		boolean all = true;
		for (int i = 0; i < s.getItems().size(); i++)
		{
			int id = i < s.getIds().size() ? s.getIds().get(i) : 0;
			if (id <= 0)
			{
				return null;
			}
			if (p.owned(id) == 0)
			{
				if (p.getBank() == null)
				{
					return null;
				}
				all = false;
			}
		}
		return all;
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

	private String profileConfig(String groupAndKey)
	{
		int dot = groupAndKey.indexOf('.');
		return dot <= 0 ? null : configManager.getRSProfileConfiguration(groupAndKey.substring(0, dot),
			groupAndKey.substring(dot + 1));
	}

	private ClueTier savedTier()
	{
		String s = configManager.getConfiguration(ClueScrollGuideConfig.GROUP, "tier");
		try
		{
			return s == null ? ClueTier.BEGINNER : ClueTier.valueOf(s);
		}
		catch (IllegalArgumentException e)
		{
			return ClueTier.BEGINNER;
		}
	}

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onSelectTier(ClueTier t)
	{
		configManager.setConfiguration(ClueScrollGuideConfig.GROUP, "tier", t.name());
		clientThread.invokeLater(() ->
		{
			tier = t;
			stashKey = null;
			refresh();
		});
	}

	@Override
	public void onStartGuide()
	{
		clientThread.invokeLater(() ->
		{
			running = true;
			stashKey = null;
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
			stashKey = null;
			refresh();
		});
	}

	@Override
	public void onPickWay(String key)
	{
		clientThread.invokeLater(() ->
		{
			pickedWays.put(tier, key);
			stashKey = null;
			running = true;
			checkShortestPath = true;
			refresh();
		});
	}

	@Override
	public void onPickStash(String key)
	{
		clientThread.invokeLater(() ->
		{
			stashKey = key;
			checkShortestPath = true;
			refresh();
		});
	}

	@Override
	public void onAllowWilderness(boolean allow)
	{
		Wilderness.setAllowed(configManager, allow);
	}

	@Override
	public void onTurnOnCluePlugin()
	{
		for (Plugin p : pluginManager.getPlugins())
		{
			if (CLUE_PLUGIN.equals(p.getClass().getSimpleName()))
			{
				pluginManager.setPluginEnabled(p, true);
				try
				{
					pluginManager.startPlugin(p);
				}
				catch (PluginInstantiationException ex)
				{
					log.debug("Couldn't start the Clue Scroll plugin", ex);
				}
			}
		}
		clientThread.invokeLater(() ->
		{
			slowCountdown = 0;
			refresh();
		});
	}

	@Override
	public void onOpenWiki()
	{
		LinkBrowser.browse(WIKI_URL + tier.getWikiPage());
	}

	/**
	 * Tells the bank button shared by every plugin in this jar what the current step needs.
	 */
	private void showInBank()
	{
		DiaryStep step = this.step;
		bankFilter.show(this, step == null ? null : BankFilter.Need.item("Clue Scroll items", ItemID.TRAIL_CLUE_BEGINNER, step.bankItems()));
	}
}
