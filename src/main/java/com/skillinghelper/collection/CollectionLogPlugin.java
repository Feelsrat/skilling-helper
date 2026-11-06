package com.skillinghelper.collection;

import com.google.inject.Provides;
import com.skillinghelper.OpenClueGuideRequest;
import com.skillinghelper.OpenCollectionLogRequest;
import com.skillinghelper.OpenCombatAchievementsRequest;
import com.skillinghelper.OpenMethodRequest;
import com.skillinghelper.OpenMinigameRequest;
import com.skillinghelper.ShortestPathLink;
import com.skillinghelper.StartPvmGuideRequest;
import com.skillinghelper.TrainSkillRequest;
import com.skillinghelper.bank.BankItems;
import com.skillinghelper.ca.data.CaMonster;
import com.skillinghelper.ca.data.CaMonsters;
import com.skillinghelper.ca.data.CaTask;
import com.skillinghelper.ca.data.CaTasks;
import com.skillinghelper.collection.data.ClogData;
import com.skillinghelper.collection.data.ClogEntry;
import com.skillinghelper.collection.data.ClogItem;
import com.skillinghelper.collection.data.ClogLinks;
import com.skillinghelper.collection.data.ClogTab;
import com.skillinghelper.collection.data.GuideLink;
import com.skillinghelper.collection.ui.CollectionLogPanel;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.overlay.NameTargets;
import com.skillinghelper.pvm.data.PvmLadder;
import com.skillinghelper.travel.TravelAdvisor;
import com.skillinghelper.travel.TravelPick;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
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
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;

/**
 * Collection log guide: which slots and pets come quickest, which pages are closest to done, and a step-by-step
 * guide that takes the player to one slot after another (bank trip, travel, how to get in, what to click), handing
 * pages over to the full guides elsewhere in this plugin pack where they exist.
 * <p>
 * The log is synced in one go when the player clicks the log's own Search button: the game then lists every filled
 * slot, one script call each, and the plugin reads them all (the idea is WikiSync's, BSD-2; here the player does the
 * click). Between syncs, the "New item added to your collection log" chat message and any log page opened keep it
 * up to date. What it learns is saved per RuneScape profile.
 */
@Slf4j
@PluginDescriptor(
	name = "Collection Log Guide",
	description = "Step-by-step guide to the collection log slots and pets that come quickest, and the pages nearly done",
	tags = {"collection", "log", "clog", "pets", "pet", "slots", "uniques", "guide", "helper"}
)
public class CollectionLogPlugin extends Plugin implements CollectionLogPanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final String NEW_ITEM = "New item added to your collection log:";
	private static final String KEY_OBTAINED = "obtained";
	private static final String KEY_MISSING = "missing";
	private static final String KEY_SYNCED = "syncedAt";
	/**
	 * The log's search runs this once for each filled slot it lists, the item id second.
	 */
	private static final int SEARCH_ITEM_SCRIPT = 4100;
	/**
	 * Ticks after the last listed slot before the search counts as done.
	 */
	private static final int SYNC_SETTLE_TICKS = 2;
	/**
	 * A search listing fewer than this many slots (or under 80% of the game's count) was a typed search, not a
	 * full listing, and only ticks those slots off.
	 */
	private static final int MIN_FULL_SYNC = 20;
	private static final int QUEST_REFRESH_TICKS = 50;
	private static final int NEAR_BOSS = 15;
	private static final int LEFT_PLACE = 60;
	private static final DateTimeFormatter SYNC_TIME = DateTimeFormatter.ofPattern("d MMM HH:mm")
		.withZone(ZoneId.systemDefault());

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
	private CollectionLogConfig config;
	@Inject
	private GuideRepository skillGuides;
	@Inject
	private TravelAdvisor travel;
	@Inject
	private ClogItemOverlay itemOverlay;
	@Inject
	private ModelOutlineRenderer outlineRenderer;

	private final ClogData data = ClogData.get();
	private ClogRequirements requirements;
	private NameTargets targets;
	private ClogSceneOverlay sceneOverlay;
	private ClogSyncOverlay syncOverlay;
	private CollectionLogPanel panel;
	private NavigationButton navButton;

	private final Set<Integer> obtained = new HashSet<>();
	private final Set<Integer> missing = new HashSet<>();
	private Set<Integer> bankOwned = Collections.emptySet();
	private Map<Integer, Integer> bankCounts;
	private Map<String, Integer> idsByGameName;
	private boolean loaded;
	private long syncedAt;
	private final Set<Integer> searchBurst = new HashSet<>();
	private int searchBurstTick;
	@Getter
	private volatile boolean needsSync;

	private final Map<Quest, QuestState> quests = new HashMap<>();
	private Set<Quest> wantedQuests;
	private int questCountdown;

	private ClogView.Mode mode = ClogView.Mode.QUICKEST;
	private ClogTab tab = ClogTab.BOSSES;

	// The running guide
	private boolean running;
	/**
	 * The page the player picked to work through, or null to go for the quickest slot anywhere.
	 */
	private String page;
	/**
	 * The slot being gone for now, and the page it's gone for at.
	 */
	private Integer target;
	private String targetPage;
	private final Set<Integer> skipped = new HashSet<>();
	/**
	 * The page whose place the player reached (stays until they wander off).
	 */
	private String arrivedPage;
	/**
	 * The hand-off already opened, so the other plugin's panel is opened once and not every tick.
	 */
	private String openedHandOff;
	@Getter
	private volatile DiaryStep step;

	private WorldPoint pathTarget;
	private boolean hintArrowSet;
	private boolean checkShortestPath;
	private Set<String> trackedNpcs = Collections.emptySet();
	private Set<String> trackedObjects = Collections.emptySet();
	private ClogView shownView;
	private Set<String> caMonsters;

	@Override
	protected void startUp()
	{
		requirements = new ClogRequirements(PvmLadder.ALL, skillGuides.all());
		targets = new NameTargets(client);
		sceneOverlay = new ClogSceneOverlay(client, this, config, targets, outlineRenderer);
		syncOverlay = new ClogSyncOverlay(client, this, config);
		eventBus.register(targets);
		overlayManager.add(sceneOverlay);
		overlayManager.add(syncOverlay);
		overlayManager.add(itemOverlay);
		panel = new CollectionLogPanel(this);
		shownView = null;
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Collection Log Guide")
			.icon(icon)
			.priority(13)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		questCountdown = 0;
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				load();
			}
			refresh();
		});
	}

	@Override
	protected void shutDown()
	{
		eventBus.unregister(targets);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(syncOverlay);
		overlayManager.remove(itemOverlay);
		clientToolbar.removeNavigation(navButton);
		NameTargets t = targets;
		clientThread.invoke(() ->
		{
			clearWay();
			t.clear();
		});
		obtained.clear();
		missing.clear();
		searchBurst.clear();
		bankOwned = Collections.emptySet();
		bankCounts = null;
		idsByGameName = null;
		loaded = false;
		syncedAt = 0;
		needsSync = false;
		quests.clear();
		stopGuide();
		skipped.clear();
		trackedNpcs = Collections.emptySet();
		trackedObjects = Collections.emptySet();
		panel = null;
		shownView = null;
	}

	@Provides
	CollectionLogConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CollectionLogConfig.class);
	}

	// ---- Game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			questCountdown = 0;
			if (!loaded)
			{
				load();
			}
		}
		else if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			loaded = false;
			obtained.clear();
			missing.clear();
			searchBurst.clear();
			bankOwned = Collections.emptySet();
			bankCounts = null;
			quests.clear();
			pathTarget = null;
			hintArrowSet = false;
			skipped.clear();
			arrivedPage = null;
			refresh();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		bankOwned = Collections.emptySet();
		bankCounts = null;
		load();
		refresh();
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired e)
	{
		if (e.getScriptId() != SEARCH_ITEM_SCRIPT || e.getScriptEvent() == null)
		{
			return;
		}
		Object[] args = e.getScriptEvent().getArguments();
		if (args != null && args.length > 1 && args[1] instanceof Integer)
		{
			searchBurst.add((Integer) args[1]);
			searchBurstTick = client.getTickCount();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired e)
	{
		if (e.getScriptId() == ScriptID.COLLECTION_DRAW_LIST)
		{
			// The page's items are drawn by the end of the script; read them once it's done
			clientThread.invokeLater(this::readLogPage);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		String msg = Text.removeTags(e.getMessage());
		if (!msg.startsWith(NEW_ITEM))
		{
			return;
		}
		String name = msg.substring(NEW_ITEM.length()).trim();
		int id = idByGameName(name);
		if (id > 0 && obtained.add(id))
		{
			missing.remove(id);
			save();
			refresh();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (e.getContainerId() == InventoryID.BANK)
		{
			bankOwned = slotsIn(e.getItemContainer());
			bankCounts = BankItems.count(e.getItemContainer(), itemManager);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if ((CollectionLogConfig.GROUP.equals(e.getGroup()) && !KEY_OBTAINED.equals(e.getKey())
			&& !KEY_MISSING.equals(e.getKey()) && !KEY_SYNCED.equals(e.getKey())) || Wilderness.isSettingChange(e))
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
		if (!loaded)
		{
			load();
		}
		if (!searchBurst.isEmpty() && client.getTickCount() - searchBurstTick >= SYNC_SETTLE_TICKS)
		{
			finishSearch();
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
		}
		refresh();
	}

	/**
	 * Another plugin in this jar (Account Planner) asked for one log page: guide it.
	 */
	@Subscribe
	public void onOpenCollectionLogRequest(OpenCollectionLogRequest e)
	{
		if (data.entry(e.getEntry()) == null)
		{
			return;
		}
		guidePage(e.getEntry(), null);
		refresh();
		SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
	}

	// ---- Reading the log ----

	/**
	 * The log's search listed its slots: a full listing replaces what the plugin knew; a short one (a typed search)
	 * only ticks those slots off.
	 */
	private void finishSearch()
	{
		Set<Integer> listed = new HashSet<>();
		for (Integer id : searchBurst)
		{
			if (data.isSlot(id))
			{
				listed.add(id);
			}
			else
			{
				int canon = itemManager.canonicalize(id);
				if (data.isSlot(canon))
				{
					listed.add(canon);
				}
			}
		}
		searchBurst.clear();
		int gameCount = client.getVarpValue(VarPlayerID.COLLECTION_COUNT);
		boolean full = gameCount > 0 ? listed.size() >= Math.max(1, gameCount * 4 / 5)
			: listed.size() >= MIN_FULL_SYNC;
		if (full)
		{
			obtained.clear();
			obtained.addAll(listed);
			missing.clear();
			for (Integer id : data.items().keySet())
			{
				if (!listed.contains(id))
				{
					missing.add(id);
				}
			}
			syncedAt = System.currentTimeMillis();
			if (configManager.getRSProfileKey() != null)
			{
				configManager.setRSProfileConfiguration(CollectionLogConfig.GROUP, KEY_SYNCED, syncedAt);
			}
			chat("Collection Log Guide: synced, " + listed.size() + " slots filled.");
			log.debug("Collection log synced: {} filled slots", listed.size());
		}
		else
		{
			obtained.addAll(listed);
			missing.removeAll(listed);
		}
		save();
	}

	/**
	 * Reads the log page on screen: every slot is an item widget, drawn see-through until it's filled.
	 */
	private void readLogPage()
	{
		Widget items = client.getWidget(InterfaceID.Collection.ITEMS_CONTENTS);
		if (items == null || items.getDynamicChildren() == null)
		{
			return;
		}
		boolean changed = false;
		for (Widget w : items.getDynamicChildren())
		{
			int id = w.getItemId();
			if (!data.isSlot(id))
			{
				continue;
			}
			if (w.getOpacity() == 0)
			{
				changed |= obtained.add(id);
				changed |= missing.remove(id);
			}
			else
			{
				changed |= missing.add(id);
				changed |= obtained.remove(id);
			}
		}
		if (changed)
		{
			save();
			refresh();
		}
	}

	private int idByGameName(String name)
	{
		if (idsByGameName == null)
		{
			// Item names as the game writes them; a name shared by several slots (e.g. "Decorative helm" in three
			// colours) can't be told apart from chat, so it's left for the next sync
			Map<String, Integer> byName = new HashMap<>();
			Set<String> shared = new HashSet<>();
			for (Integer id : data.items().keySet())
			{
				String n = itemManager.getItemComposition(id).getMembersName().toLowerCase();
				if (byName.putIfAbsent(n, id) != null)
				{
					shared.add(n);
				}
			}
			shared.forEach(byName::remove);
			idsByGameName = byName;
		}
		Integer id = idsByGameName.get(name.toLowerCase());
		return id != null ? id : data.idByName(name);
	}

	private Set<Integer> slotsIn(ItemContainer c)
	{
		Set<Integer> out = new HashSet<>();
		if (c != null)
		{
			for (Item item : c.getItems())
			{
				if (item.getId() > 0)
				{
					int id = itemManager.canonicalize(item.getId());
					if (data.isSlot(id))
					{
						out.add(id);
					}
				}
			}
		}
		return out;
	}

	private Set<Integer> owned()
	{
		if (!config.countOwned())
		{
			return Collections.emptySet();
		}
		Set<Integer> out = new HashSet<>(bankOwned);
		out.addAll(slotsIn(client.getItemContainer(InventoryID.INV)));
		out.addAll(slotsIn(client.getItemContainer(InventoryID.WORN)));
		return out;
	}

	private void load()
	{
		if (configManager.getRSProfileKey() == null)
		{
			return;
		}
		obtained.clear();
		missing.clear();
		obtained.addAll(parseIds(configManager.getRSProfileConfiguration(CollectionLogConfig.GROUP, KEY_OBTAINED)));
		missing.addAll(parseIds(configManager.getRSProfileConfiguration(CollectionLogConfig.GROUP, KEY_MISSING)));
		missing.removeAll(obtained);
		Long at = configManager.getRSProfileConfiguration(CollectionLogConfig.GROUP, KEY_SYNCED, Long.class);
		syncedAt = at == null ? 0 : at;
		loaded = true;
		log.debug("Collection log: {} filled and {} empty slots remembered", obtained.size(), missing.size());
	}

	private void save()
	{
		if (configManager.getRSProfileKey() == null)
		{
			return;
		}
		configManager.setRSProfileConfiguration(CollectionLogConfig.GROUP, KEY_OBTAINED, joinIds(obtained));
		configManager.setRSProfileConfiguration(CollectionLogConfig.GROUP, KEY_MISSING, joinIds(missing));
	}

	static Set<Integer> parseIds(String s)
	{
		Set<Integer> out = new HashSet<>();
		if (s == null || s.isEmpty())
		{
			return out;
		}
		for (String part : s.split(","))
		{
			try
			{
				out.add(Integer.parseInt(part.trim()));
			}
			catch (NumberFormatException ignored)
			{
				// Skip anything that isn't an id
			}
		}
		return out;
	}

	static String joinIds(Set<Integer> ids)
	{
		return new TreeSet<>(ids).stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	/**
	 * "6 Nov 14:05", in the player's time zone.
	 */
	public static String syncLabel(long at)
	{
		return SYNC_TIME.format(Instant.ofEpochMilli(at));
	}

	private void readQuests()
	{
		if (wantedQuests == null)
		{
			Set<Quest> wanted = new HashSet<>(requirements.quests());
			for (CaMonster m : CaMonsters.all())
			{
				wanted.addAll(m.getQuests());
				wanted.addAll(m.getQuestsStarted());
			}
			wantedQuests = wanted;
		}
		quests.clear();
		for (Quest q : wantedQuests)
		{
			quests.put(q, q.getState(client));
		}
	}

	private DiaryPlayer readPlayer()
	{
		Map<Skill, Integer> boosted = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			boosted.put(s, client.getBoostedSkillLevel(s));
		}
		Player me = client.getLocalPlayer();
		Widget bankWidget = client.getWidget(InterfaceID.Bankmain.ITEMS);
		return DiaryPlayer.builder()
			.location(me == null ? null : WorldPoint.fromLocalInstance(client, me.getLocalLocation()))
			.levels(levelsNow())
			.boosted(boosted)
			.combatLevel(me == null ? 3 : me.getCombatLevel())
			.quests(new HashMap<>(quests))
			.done(Set.of())
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

	private Map<Skill, Integer> levelsNow()
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, client.getRealSkillLevel(s));
		}
		return levels;
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

	// ---- The view and the guide ----

	/**
	 * Works out the lists and the guide's step, and sends them on when they changed. Client thread.
	 */
	private void refresh()
	{
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
		if (!loggedIn)
		{
			step = null;
			needsSync = false;
			trackNames(null, Set.of());
			clearWay();
			push(ClogView.builder().loggedIn(false).mode(mode).tab(tab).slots(List.of()).entries(List.of())
				.wildernessAllowed(Wilderness.allowed(configManager)).build());
			return;
		}

		Map<Skill, Integer> levels = levelsNow();
		Map<Quest, QuestState> questStates = new HashMap<>(quests);
		Map<String, ClogRequirements.Check> checks = new HashMap<>();
		boolean allowWild = Wilderness.allowed(configManager);
		ClogProgress progress = new ClogProgress(obtained, missing, owned());
		ClogPlanner planner = new ClogPlanner(data, progress,
			(e, link) -> checks.computeIfAbsent((e == null ? "" : e.getName()) + "|" + link,
				k -> requirements.check(e, link, levels, questStates)),
			allowWild, config.showLocked(), caMonsters());

		int gameCount = client.getVarpValue(VarPlayerID.COLLECTION_COUNT);
		int known = 0;
		for (Integer id : obtained)
		{
			known += data.isSlot(id) ? 1 : 0;
		}
		int unsynced = syncedAt == 0 ? 0 : Math.max(0, gameCount - known);
		needsSync = syncedAt == 0 || unsynced > 0;

		ClogView.Guide guide = null;
		if (running)
		{
			guide = guide(planner, progress, levels, questStates);
		}
		else
		{
			step = null;
			trackNames(null, Set.of());
			clearWay();
		}

		List<ClogView.SlotRow> slots = List.of();
		List<ClogView.EntryRow> entries = List.of();
		if (guide == null)
		{
			switch (mode)
			{
				case QUICKEST:
					slots = planner.quickest();
					break;
				case CLOSEST:
					entries = planner.closest();
					break;
				case PETS:
					slots = planner.pets();
					break;
				default:
					entries = planner.browse(tab);
			}
		}
		push(ClogView.builder()
			.loggedIn(true)
			.have(planner.have())
			.total(data.items().size())
			.seen(planner.seen())
			.gameCount(gameCount)
			.gameTotal(client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX))
			.syncedAt(syncedAt)
			.unsynced(unsynced)
			.mode(mode)
			.tab(tab)
			.slots(slots)
			.entries(entries)
			.guide(guide)
			.wildernessAllowed(allowWild)
			.showLocked(config.showLocked())
			.build());
	}

	/**
	 * Picks the slot to go for (moving on when the last one was logged) and works out the step for it.
	 */
	private ClogView.Guide guide(ClogPlanner planner, ClogProgress progress, Map<Skill, Integer> levels,
		Map<Quest, QuestState> questStates)
	{
		if (target != null && progress.has(target))
		{
			ClogItem got = data.items().get(target);
			target = null;
			if (got != null)
			{
				chat("Collection Log Guide: " + got.getName() + " logged! On to the next slot.");
			}
		}
		if (target == null)
		{
			ClogView.SlotRow next = planner.next(page, skipped);
			if (next == null && page != null)
			{
				chat("Collection Log Guide: nothing left on " + page + " to go for. Going for the quickest slots "
					+ "anywhere now.");
				page = null;
				next = planner.next(null, skipped);
			}
			if (next == null)
			{
				step = DiaryStep.builder().kind(DiaryStep.Kind.DONE).title("Nothing left to go for right now")
					.line("Every slot you can do now is filled or skipped. Train or quest for the locked ones "
						+ "(tick \"Show locked\" to see them), then start again.")
					.build();
				trackNames(step, Set.of());
				clearWay();
				return new ClogView.Guide(null, null, step, null, null, List.of(), 0, 0);
			}
			target = next.getId();
			targetPage = page != null ? page : next.getEntry();
			openedHandOff = null;
		}
		ClogEntry entry = data.entry(targetPage);
		ClogItem slot = data.items().get(target);
		if (entry == null || slot == null)
		{
			target = null;
			stopGuide();
			return null;
		}
		GuideLink link = entry.isPets() ? ClogLinks.of(slot, data) : ClogLinks.of(entry);
		ClogRequirements.Check check = requirements.check(entry.isPets() ? null : entry, link, levels, questStates);
		DiaryPlayer p = readPlayer();
		CaMonster monster = ClogLinks.monster(entry);
		forgetIfLeft(entry, monster, p.getLocation());

		ClogGuide.Context.ContextBuilder context = ClogGuide.Context.builder()
			.player(p)
			.entry(entry)
			.slot(slot)
			.left(planner.left(entry))
			.link(link)
			.check(check)
			.near(isNear(entry, monster, p.getLocation()))
			.arrived(entry.getName().equals(arrivedPage));
		DiaryStep next = ClogGuide.resolve(context.build());
		Location dest = next.getDestination();
		if (link == null && dest != null && !client.getTopLevelWorldView().isInstance())
		{
			Map<Integer, Integer> bank = p.getBank();
			List<TravelPick> picks = travel.plan(id -> p.getCarried().getOrDefault(id, 0),
				id -> bank == null ? 0 : bank.getOrDefault(id, 0), p.getLocation(),
				List.of(dest.getPoint()), dest.getName(), Wilderness.contains(dest) && p.isAllowWilderness());
			if (!picks.isEmpty())
			{
				next = ClogGuide.resolve(context.player(p.toBuilder().travel(picks).build()).build());
			}
		}
		if (link == null && next.getKind() == DiaryStep.Kind.DO)
		{
			arrivedPage = entry.getName();
		}
		if (link != null && !check.isLocked())
		{
			String key = entry.getName() + "|" + link;
			if (!key.equals(openedHandOff))
			{
				openedHandOff = key;
				openGuide(link);
			}
		}
		step = next;
		trackNames(next, monster == null ? new HashSet<>(entry.npcNames()) : monster.getNpcs());
		showWay(link == null && next.getDestination() != null ? next.getDestination().getPoint() : null);
		return new ClogView.Guide(entry.getName(), slot.getName(), next,
			link == null ? null : ClogPlanner.label(link), ClogLinks.caBoss(entry, caMonsters()),
			planner.pageSlots(entry), planner.have(entry), entry.getItems().size());
	}

	/**
	 * Forgets that the player reached the page's place once they're back on the surface and well away from it.
	 */
	private void forgetIfLeft(ClogEntry e, CaMonster m, WorldPoint me)
	{
		if (arrivedPage != null && !arrivedPage.equals(e.getName()))
		{
			arrivedPage = null;
		}
		WorldPoint at = m != null && m.getAt() != null ? m.getAt().getPoint() : e.getLocation();
		if (arrivedPage == null || at == null || me == null)
		{
			return;
		}
		boolean surface = me.getY() < 4160 && at.getY() < 4160;
		if (surface && me.distanceTo2D(at) > LEFT_PLACE)
		{
			arrivedPage = null;
		}
	}

	private boolean isNear(ClogEntry e, CaMonster m, WorldPoint me)
	{
		if (me == null)
		{
			return false;
		}
		Set<String> names = m != null ? m.getNpcs() : new HashSet<>(e.npcNames());
		for (Map.Entry<NPC, String> en : targets.getNpcs())
		{
			WorldPoint wp = en.getKey().getWorldLocation();
			if (names.contains(en.getValue()) && wp != null && wp.distanceTo2D(me) <= NEAR_BOSS)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Tracks what the step outlines, plus the page's boss so the guide notices when it's close.
	 */
	private void trackNames(DiaryStep s, Set<String> boss)
	{
		Set<String> npcs = new HashSet<>(boss);
		Set<String> objects = new HashSet<>();
		if (s != null)
		{
			npcs.addAll(s.getNpcs());
			objects.addAll(s.getObjects());
		}
		if (!npcs.equals(trackedNpcs) || !objects.equals(trackedObjects))
		{
			trackedNpcs = npcs;
			trackedObjects = objects;
			targets.track(npcs, objects);
		}
	}

	private Set<String> caMonsters()
	{
		if (caMonsters == null)
		{
			Set<String> names = new HashSet<>();
			for (CaTask t : CaTasks.all())
			{
				if (t.getMonster() != null)
				{
					names.add(t.getMonster().toLowerCase());
				}
			}
			caMonsters = names;
		}
		return caMonsters;
	}

	private void push(ClogView view)
	{
		CollectionLogPanel to = panel;
		if (to != null && !view.equals(shownView))
		{
			shownView = view;
			SwingUtilities.invokeLater(() -> to.update(view));
		}
	}

	private void chat(String msg)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
	}

	/**
	 * Opens the full guide for a page in its own plugin.
	 */
	private void openGuide(GuideLink link)
	{
		switch (link.getKind())
		{
			case PVM:
				eventBus.post(new StartPvmGuideRequest(link.getTarget()));
				break;
			case METHOD:
				eventBus.post(new OpenMethodRequest(link.getTarget()));
				break;
			case CLUE:
				eventBus.post(new OpenClueGuideRequest(link.getTarget()));
				break;
			case MINIGAME:
				eventBus.post(new OpenMinigameRequest(link.getTarget()));
				break;
			default:
				int level = client.getRealSkillLevel(link.getSkill());
				eventBus.post(new TrainSkillRequest(link.getSkill(),
					Math.min(Experience.MAX_REAL_LEVEL, level + 10)));
		}
	}

	/**
	 * Starts (or switches) the guide to a page, on one slot of it or its quickest one. Client thread.
	 */
	private void guidePage(String entry, Integer slot)
	{
		running = true;
		page = entry;
		target = slot;
		targetPage = slot == null ? null : entry;
		if (slot != null)
		{
			skipped.remove(slot);
		}
		openedHandOff = null;
		checkShortestPath = true;
	}

	private void stopGuide()
	{
		running = false;
		page = null;
		target = null;
		targetPage = null;
		arrivedPage = null;
		openedHandOff = null;
		step = null;
	}

	// ---- Showing the way ----

	private void showWay(WorldPoint to)
	{
		if (Objects.equals(to, pathTarget))
		{
			return;
		}
		clearWay();
		if (to == null)
		{
			return;
		}
		pathTarget = to;
		if (config.hintArrow())
		{
			client.setHintArrow(to);
			hintArrowSet = true;
		}
		if (config.shortestPath())
		{
			Map<String, Object> path = new HashMap<>();
			path.put("target", to);
			// Only path through the Wilderness when the place itself is there, whatever Shortest Path's setting says
			Map<String, Object> overrides = new HashMap<>();
			overrides.put("avoidWilderness", !Wilderness.contains(to));
			ShortestPathLink.allowCarriedTeleports(configManager, overrides);
			path.put("config", overrides);
			ShortestPathLink.path(eventBus, this, path);
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

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onMode(ClogView.Mode m)
	{
		clientThread.invokeLater(() ->
		{
			mode = m;
			refresh();
		});
	}

	@Override
	public void onTab(ClogTab t)
	{
		clientThread.invokeLater(() ->
		{
			tab = t;
			refresh();
		});
	}

	@Override
	public void onStart()
	{
		clientThread.invokeLater(() ->
		{
			guidePage(null, null);
			refresh();
		});
	}

	@Override
	public void onStop()
	{
		clientThread.invokeLater(() ->
		{
			stopGuide();
			clearWay();
			refresh();
		});
	}

	@Override
	public void onPickEntry(String name)
	{
		clientThread.invokeLater(() ->
		{
			if (data.entry(name) != null)
			{
				guidePage(name, null);
				refresh();
			}
		});
	}

	@Override
	public void onPickSlot(String entry, int itemId)
	{
		clientThread.invokeLater(() ->
		{
			ClogEntry e = data.entry(entry);
			if (e != null && data.isSlot(itemId))
			{
				guidePage(entry, itemId);
				// A slot picked from the lists: once it's logged, go back to the quickest anywhere (pets: next pet)
				page = e.isPets() ? entry : null;
				refresh();
			}
		});
	}

	@Override
	public void onSkip()
	{
		clientThread.invokeLater(() ->
		{
			if (target != null)
			{
				skipped.add(target);
				target = null;
			}
			refresh();
		});
	}

	@Override
	public void onOpenGuide()
	{
		clientThread.invokeLater(() ->
		{
			openedHandOff = null;
			refresh();
		});
	}

	@Override
	public void onOpenCombatAchievements(String monster)
	{
		clientThread.invokeLater(() -> eventBus.post(OpenCombatAchievementsRequest.boss(monster)));
	}

	@Override
	public void onAllowWilderness(boolean allow)
	{
		Wilderness.setAllowed(configManager, allow);
	}

	@Override
	public void onShowLocked(boolean show)
	{
		configManager.setConfiguration(CollectionLogConfig.GROUP, "showLocked", show);
	}

	@Override
	public void onOpenWiki(String entryName)
	{
		ClogEntry e = entryName == null ? null : data.entry(entryName);
		LinkBrowser.browse(WIKI_URL + (e == null ? "Collection_log" : e.getWikiPage()));
	}
}
