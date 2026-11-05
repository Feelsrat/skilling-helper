package com.skillinghelper.collection;

import com.google.inject.Provides;
import com.skillinghelper.OpenClueGuideRequest;
import com.skillinghelper.OpenMethodRequest;
import com.skillinghelper.ShortestPathLink;
import com.skillinghelper.StartPvmGuideRequest;
import com.skillinghelper.TrainSkillRequest;
import com.skillinghelper.collection.data.ClogData;
import com.skillinghelper.collection.data.ClogEntry;
import com.skillinghelper.collection.data.ClogItem;
import com.skillinghelper.collection.data.ClogLinks;
import com.skillinghelper.collection.data.ClogTab;
import com.skillinghelper.collection.data.GuideLink;
import com.skillinghelper.collection.ui.CollectionLogPanel;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.overlay.NameTargets;
import com.skillinghelper.pvm.data.PvmLadder;
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
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPostFired;
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
 * Collection log helper: which slots and pets come quickest, which pages are closest to done, and how to go and get
 * them, handing off to the full click-by-click guides elsewhere in this plugin pack where they exist.
 * <p>
 * The log is read the way the game shows it: every log page the player opens is read (filled slots draw at full
 * opacity), and the "New item added to your collection log" chat message ticks new slots off. What it learns is
 * saved per RuneScape profile.
 */
@Slf4j
@PluginDescriptor(
	name = "Collection Log Guide",
	description = "Which collection log slots and pets come quickest, which pages are nearly done, and how to get them",
	tags = {"collection", "log", "clog", "pets", "pet", "slots", "uniques", "guide", "helper"}
)
public class CollectionLogPlugin extends Plugin implements CollectionLogPanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final String NEW_ITEM = "New item added to your collection log:";
	private static final String KEY_OBTAINED = "obtained";
	private static final String KEY_MISSING = "missing";
	private static final int QUEST_REFRESH_TICKS = 50;
	private static final int REFRESH_TICKS = 5;

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
	private ModelOutlineRenderer outlineRenderer;

	private final ClogData data = ClogData.get();
	private ClogRequirements requirements;
	private NameTargets targets;
	private ClogSceneOverlay sceneOverlay;
	private CollectionLogPanel panel;
	private NavigationButton navButton;

	private final Set<Integer> obtained = new HashSet<>();
	private final Set<Integer> missing = new HashSet<>();
	private Set<Integer> bankOwned = Collections.emptySet();
	private Map<String, Integer> idsByGameName;
	private boolean loaded;

	private final Map<Quest, QuestState> quests = new HashMap<>();
	private int questCountdown;
	private int refreshCountdown;

	private ClogView.Mode mode = ClogView.Mode.QUICKEST;
	private ClogTab tab = ClogTab.BOSSES;
	private String selected;
	private boolean showingWay;
	private WorldPoint pathTarget;
	private boolean hintArrowSet;
	private boolean checkShortestPath;
	private volatile Set<String> outlined = Collections.emptySet();
	private ClogView shownView;

	@Override
	protected void startUp()
	{
		requirements = new ClogRequirements(PvmLadder.ALL, skillGuides.all());
		targets = new NameTargets(client);
		sceneOverlay = new ClogSceneOverlay(client, this, config, targets, outlineRenderer);
		eventBus.register(targets);
		overlayManager.add(sceneOverlay);
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
		clientToolbar.removeNavigation(navButton);
		NameTargets t = targets;
		clientThread.invoke(() ->
		{
			clearWay();
			t.clear();
		});
		obtained.clear();
		missing.clear();
		bankOwned = Collections.emptySet();
		idsByGameName = null;
		loaded = false;
		quests.clear();
		selected = null;
		showingWay = false;
		outlined = Collections.emptySet();
		panel = null;
		shownView = null;
	}

	@Provides
	CollectionLogConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CollectionLogConfig.class);
	}

	Set<String> getOutlined()
	{
		return outlined;
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
			bankOwned = Collections.emptySet();
			quests.clear();
			clearWay();
			showingWay = false;
			refresh();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		bankOwned = Collections.emptySet();
		load();
		refresh();
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
			refresh();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (CollectionLogConfig.GROUP.equals(e.getGroup()) || Wilderness.isSettingChange(e))
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
		if (checkShortestPath)
		{
			checkShortestPath = false;
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
		if (--refreshCountdown <= 0)
		{
			refreshCountdown = REFRESH_TICKS;
			refresh();
		}
	}

	// ---- Reading the log ----

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
			// colours) can't be told apart from chat, so it's left for the log page to fill in
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

	private void readQuests()
	{
		quests.clear();
		for (Quest q : requirements.quests())
		{
			quests.put(q, q.getState(client));
		}
	}

	// ---- The view ----

	/**
	 * Works out the lists and the picked page, and sends them to the panel when they changed. Client thread.
	 */
	private void refresh()
	{
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
		ClogEntry entry = selected == null ? null : data.entry(selected);
		if (!loggedIn)
		{
			outlined = Collections.emptySet();
			targets.track(Collections.emptySet(), Collections.emptySet());
			push(ClogView.builder().loggedIn(false).mode(mode).tab(tab).slots(List.of()).entries(List.of())
				.wildernessAllowed(Wilderness.allowed(configManager)).build());
			return;
		}

		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, client.getRealSkillLevel(s));
		}
		Map<Quest, QuestState> questStates = new HashMap<>(quests);
		Map<String, ClogRequirements.Check> checks = new HashMap<>();
		boolean allowWild = Wilderness.allowed(configManager);
		ClogPlanner planner = new ClogPlanner(data, new ClogProgress(obtained, missing, owned()),
			(e, link) -> checks.computeIfAbsent((e == null ? "" : e.getName()) + "|" + link,
				k -> requirements.check(e, link, levels, questStates)),
			allowWild, config.showLocked());

		ClogView.EntryDetail detail = null;
		if (entry != null)
		{
			detail = planner.detail(entry, showingWay, travelTips(entry));
			Set<String> npcs = new HashSet<>(entry.npcNames());
			outlined = npcs;
			targets.track(npcs, Collections.emptySet());
		}
		else
		{
			outlined = Collections.emptySet();
			targets.track(Collections.emptySet(), Collections.emptySet());
		}
		List<ClogView.SlotRow> slots = List.of();
		List<ClogView.EntryRow> entries = List.of();
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
		push(ClogView.builder()
			.loggedIn(true)
			.have(planner.have())
			.total(data.items().size())
			.seen(planner.seen())
			.gameCount(client.getVarpValue(VarPlayerID.COLLECTION_COUNT))
			.gameTotal(client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX))
			.mode(mode)
			.tab(tab)
			.slots(slots)
			.entries(entries)
			.detail(detail)
			.wildernessAllowed(allowWild)
			.showLocked(config.showLocked())
			.build());
	}

	private List<String> travelTips(ClogEntry e)
	{
		Player me = client.getLocalPlayer();
		if (e.getLocation() == null || me == null || client.getTopLevelWorldView().isInstance())
		{
			return List.of();
		}
		Map<Integer, Integer> carried = new HashMap<>();
		for (int containerId : new int[]{InventoryID.INV, InventoryID.WORN})
		{
			ItemContainer c = client.getItemContainer(containerId);
			if (c != null)
			{
				for (Item item : c.getItems())
				{
					if (item.getId() > 0)
					{
						carried.merge(itemManager.canonicalize(item.getId()), item.getQuantity(), Integer::sum);
					}
				}
			}
		}
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		Map<Integer, Integer> banked = new HashMap<>();
		if (bank != null)
		{
			for (Item item : bank.getItems())
			{
				if (item.getId() > 0)
				{
					banked.merge(itemManager.canonicalize(item.getId()), item.getQuantity(), Integer::sum);
				}
			}
		}
		List<TravelPick> picks = travel.plan(id -> carried.getOrDefault(id, 0), id -> banked.getOrDefault(id, 0),
			WorldPoint.fromLocalInstance(client, me.getLocalLocation()), List.of(e.getLocation()), e.getName(),
			e.isWilderness() && Wilderness.allowed(configManager));
		List<String> out = new ArrayList<>();
		for (TravelPick p : picks)
		{
			out.add(p.isCarried() ? p.carriedLine() : p.tipLine());
		}
		return out;
	}

	private void push(ClogView view)
	{
		CollectionLogPanel target = panel;
		if (target != null && !view.equals(shownView))
		{
			shownView = view;
			SwingUtilities.invokeLater(() -> target.update(view));
		}
	}

	// ---- Showing the way ----

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
			Map<String, Object> path = new HashMap<>();
			path.put("target", target);
			// Only path through the Wilderness when the place itself is there, whatever Shortest Path's setting says
			Map<String, Object> overrides = new HashMap<>();
			overrides.put("avoidWilderness", !Wilderness.contains(target));
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
	public void onPickEntry(String name)
	{
		clientThread.invokeLater(() ->
		{
			if (!Objects.equals(name, selected))
			{
				showingWay = false;
				clearWay();
			}
			selected = name;
			refresh();
		});
	}

	@Override
	public void onBack()
	{
		clientThread.invokeLater(() ->
		{
			selected = null;
			showingWay = false;
			clearWay();
			refresh();
		});
	}

	@Override
	public void onShowWay(boolean show)
	{
		clientThread.invokeLater(() ->
		{
			ClogEntry e = selected == null ? null : data.entry(selected);
			showingWay = show && e != null && e.getLocation() != null;
			if (showingWay)
			{
				checkShortestPath = true;
				showWay(e.getLocation());
			}
			else
			{
				clearWay();
			}
			refresh();
		});
	}

	@Override
	public void onOpenGuide(String entryName, int itemId)
	{
		clientThread.invokeLater(() ->
		{
			ClogEntry e = data.entry(entryName);
			if (e == null)
			{
				return;
			}
			GuideLink link = ClogLinks.of(e);
			ClogItem item = data.items().get(itemId);
			if (item != null && e.isPets())
			{
				link = ClogLinks.of(item, data);
			}
			if (link == null)
			{
				return;
			}
			showingWay = false;
			clearWay();
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
				default:
					int level = client.getRealSkillLevel(link.getSkill());
					eventBus.post(new TrainSkillRequest(link.getSkill(),
						Math.min(Experience.MAX_REAL_LEVEL, level + 10)));
			}
			refresh();
		});
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
		ClogEntry e = data.entry(entryName);
		LinkBrowser.browse(WIKI_URL + (e == null ? "Collection_log" : e.getWikiPage()));
	}
}
