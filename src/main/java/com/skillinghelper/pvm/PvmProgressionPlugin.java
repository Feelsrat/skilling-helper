package com.skillinghelper.pvm;

import com.google.inject.Provides;
import com.skillinghelper.ShortestPathLink;
import com.skillinghelper.StartPvmGuideRequest;
import com.skillinghelper.TrainSkillRequest;
import com.skillinghelper.bank.BankItems;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import com.skillinghelper.data.Outleveled;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.pvm.data.ItemNeed;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.PvmLadder;
import com.skillinghelper.pvm.data.QuestReq;
import com.skillinghelper.pvm.guide.FightCaveWaves;
import com.skillinghelper.pvm.guide.Gear;
import com.skillinghelper.pvm.guide.GuideResolver;
import com.skillinghelper.pvm.guide.GuideState;
import com.skillinghelper.pvm.guide.LoadoutPlan;
import com.skillinghelper.pvm.guide.PvmGuide;
import com.skillinghelper.pvm.guide.PvmGuides;
import com.skillinghelper.pvm.guide.PvmStep;
import com.skillinghelper.pvm.guide.Supplies;
import com.skillinghelper.pvm.guide.Supply;
import com.skillinghelper.pvm.ui.PvmProgressionPanel;
import com.skillinghelper.travel.TravelAdvisor;
import com.skillinghelper.travel.TravelPick;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.WorldType;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.widgets.Widget;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;

/**
 * Recommends the next boss or raid to work towards from your stats, quests and kill counts, and guides you through
 * it step by step: the exact loadout to bring, the way there, and what to click. During fights it only highlights
 * what's on screen (the boss and its adds) and tracks progress; it never predicts attacks, prayers or safe tiles.
 */
@Slf4j
@PluginDescriptor(
	name = "PvM Progression",
	description = "Recommends which boss or raid to do next, with step-by-step guides and exact loadouts",
	tags = {"pvm", "boss", "bossing", "raids", "progression", "guide", "recommendation", "loadout"}
)
public class PvmProgressionPlugin extends Plugin implements PvmProgressionPanel.Listener
{
	static final String GROUP = "pvm-progression";
	private static final String KEY_KILL_COUNTS = "killCounts";
	private static final String KEY_MARKED_DONE = "markedDone";
	private static final String KEY_BANK = "bankOwned";
	private static final String KEY_BANK_COUNTS = "bankCounts";
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final int QUEST_REFRESH_TICKS = 50;
	/**
	 * How many ticks after a kill message the guide says to pick up the loot.
	 */
	private static final int LOOT_TICKS = 8;
	/**
	 * NPCs this close count as "here" for the guide (the boss in view, adds alive).
	 */
	private static final int NPC_RANGE = 20;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ConfigManager configManager;
	@Inject
	private TravelAdvisor travel;

	@Inject
	private PluginManager pluginManager;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private EventBus eventBus;
	@Inject
	private ItemManager itemManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private PvmProgressionConfig config;
	@Inject
	private GuideTargets targets;
	@Inject
	private PvmSceneOverlay sceneOverlay;
	@Inject
	private PvmItemOverlay itemOverlay;
	@Inject
	private HiscoreClient hiscoreClient;
	@Inject
	private PvmBankFilter bankFilter;
	@Inject
	private PvmWidgetOverlay widgetOverlay;
	@Inject
	private BarrowsTunnel barrowsTunnel;

	/**
	 * Hiscore boss entries per ladder entry, to find kills from before the plugin was installed.
	 */
	private static final Map<String, List<HiscoreSkill>> HISCORES = Map.ofEntries(
		Map.entry("obor", List.of(HiscoreSkill.OBOR)),
		Map.entry("bryophyta", List.of(HiscoreSkill.BRYOPHYTA)),
		Map.entry("scurrius", List.of(HiscoreSkill.SCURRIUS)),
		Map.entry("giant_mole", List.of(HiscoreSkill.GIANT_MOLE)),
		Map.entry("barrows", List.of(HiscoreSkill.BARROWS_CHESTS)),
		Map.entry("sarachnis", List.of(HiscoreSkill.SARACHNIS)),
		Map.entry("king_black_dragon", List.of(HiscoreSkill.KING_BLACK_DRAGON)),
		Map.entry("moons_of_peril", List.of(HiscoreSkill.LUNAR_CHESTS)),
		Map.entry("kalphite_queen", List.of(HiscoreSkill.KALPHITE_QUEEN)),
		Map.entry("zulrah", List.of(HiscoreSkill.ZULRAH)),
		Map.entry("fight_caves", List.of(HiscoreSkill.TZTOK_JAD)),
		Map.entry("vorkath", List.of(HiscoreSkill.VORKATH)),
		Map.entry("god_wars", List.of(HiscoreSkill.GENERAL_GRAARDOR, HiscoreSkill.COMMANDER_ZILYANA,
			HiscoreSkill.KRIL_TSUTSAROTH, HiscoreSkill.KREEARRA)),
		Map.entry("gauntlet", List.of(HiscoreSkill.THE_GAUNTLET, HiscoreSkill.THE_CORRUPTED_GAUNTLET)),
		Map.entry("tombs_of_amascut", List.of(HiscoreSkill.TOMBS_OF_AMASCUT, HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT)),
		Map.entry("chambers_of_xeric", List.of(HiscoreSkill.CHAMBERS_OF_XERIC,
			HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE)),
		Map.entry("theatre_of_blood", List.of(HiscoreSkill.THEATRE_OF_BLOOD, HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE)),
		Map.entry("fortis_colosseum", List.of(HiscoreSkill.SOL_HEREDIT)),
		Map.entry("inferno", List.of(HiscoreSkill.TZKAL_ZUK)));

	/**
	 * Whether this session already asked the hiscores for past kills.
	 */
	private boolean hiscoresChecked;

	private PvmProgressionPanel panel;
	private NavigationButton navButton;

	/**
	 * Every item id the ladder and the guides check for, so only those are remembered from the bank.
	 */
	private final Set<Integer> trackedItems = new HashSet<>();
	private final Map<String, Integer> killCounts = new HashMap<>();
	private final Set<String> markedDone = new HashSet<>();
	private final Set<Integer> bankOwned = new HashSet<>();
	private final Map<Integer, Integer> bankCounts = new HashMap<>();
	private boolean bankSeen;
	private boolean bankOpen;
	private final Set<Integer> carriedOwned = new HashSet<>();
	private final Map<Quest, QuestState> questStates = new HashMap<>();
	private boolean profileLoaded;
	private boolean dirty;
	/**
	 * What the panel was last sent by {@link #refresh}, to skip sending the same again.
	 */
	private List<Object> lastShown;
	/**
	 * Logged in: check on the next tick that Shortest Path is there.
	 */
	private boolean checkShortestPath;
	private int questRefreshCountdown;
	private PvmContent pathShown;

	// ---- The running guide ----

	private PvmContent guideContent;
	/**
	 * NPC and object names the scene overlay is tracking for the running guide.
	 */
	private Set<String> trackedNpcs = Set.of();
	private Set<String> trackedObjects = Set.of();
	private PvmGuide guide;
	/**
	 * What to do right now, read by the overlays. Null when no guide is running.
	 */
	@Getter
	private volatile PvmStep step;
	/**
	 * Canonical ids of the running guide's loadout, for the bank filter. Empty when no guide is running.
	 */
	@Getter
	private volatile Set<Integer> bankItems = Set.of();
	/**
	 * Left the bank with the full loadout this trip; cleared once a supply runs low.
	 */
	private boolean tripStocked;
	private Location routed;
	private boolean routedLocal;
	private boolean hintArrowSet;
	private int lastKillTick = -100;
	private final Set<String> guideFlags = new HashSet<>();
	/**
	 * The Barrows brother whose crypt the player is in, or -1.
	 */
	private int barrowsCrypt = -1;
	/**
	 * The picture to click in the Barrows door puzzle, read by the widget overlay.
	 */
	@Getter
	private volatile Widget puzzleAnswer;

	private final PriceLookup prices = id -> (int) Math.min(Integer.MAX_VALUE, itemManager.getItemPrice(id));

	@Override
	protected void startUp()
	{
		for (PvmContent c : PvmLadder.ALL)
		{
			for (ItemNeed i : c.getItems())
			{
				trackedItems.addAll(i.getIds());
			}
		}
		for (PvmGuide g : PvmGuides.ALL)
		{
			for (Gear gear : g.allGear())
			{
				for (int id : gear.getIds())
				{
					trackedItems.add(id);
				}
			}
			for (Supply s : g.allSupplies())
			{
				for (int id : s.allIds())
				{
					trackedItems.add(id);
				}
			}
		}
		// Ammo is picked from the weapon, so it isn't in any guide's supplies
		trackedItems.addAll(AMMO_IDS);

		panel = new PvmProgressionPanel(this);
		lastShown = null;
		panel.setWildernessAllowed(Wilderness.allowed(configManager));
		panel.setHideOutleveled(Outleveled.hidden(configManager));
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("PvM Progression")
			.icon(icon)
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		eventBus.register(targets);
		bankFilter.startUp();
		eventBus.register(bankFilter);
		overlayManager.add(sceneOverlay);
		overlayManager.add(itemOverlay);
		overlayManager.add(widgetOverlay);

		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				checkShortestPath = true;
				loadProfile();
				refreshQuests();
				refresh();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(itemOverlay);
		overlayManager.remove(widgetOverlay);
		puzzleAnswer = null;
		eventBus.unregister(targets);
		eventBus.unregister(bankFilter);
		bankFilter.shutDown();
		clientThread.invoke(() ->
		{
			stopGuide();
			clearPath();
		});
		panel = null;
		profileLoaded = false;
		killCounts.clear();
		markedDone.clear();
		bankOwned.clear();
		bankCounts.clear();
		bankSeen = false;
		carriedOwned.clear();
		questStates.clear();
		trackedItems.clear();
	}

	@Provides
	PvmProgressionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvmProgressionConfig.class);
	}

	// ---- Game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			checkShortestPath = true;
		}
		if (e.getGameState() == GameState.LOGGED_IN && !profileLoaded)
		{
			loadProfile();
			// Quest states aren't ready on the login tick, so read them on the next game tick
			questRefreshCountdown = 0;
			dirty = true;
		}
		if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			hiscoresChecked = false;
			// Logging out between Fight Cave waves keeps the player's place, so keep the wave too
			guideFlags.removeIf(f -> !f.startsWith(FightCaveWaves.WAVE_FLAG));
			bankOpen = false;
			showRoute(null);
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		loadProfile();
		questRefreshCountdown = 0;
		dirty = true;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
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
			dirty = true;
		}
		if (!hiscoresChecked)
		{
			checkHiscores();
		}
		if (dirty)
		{
			refresh();
		}
		else if (guide != null)
		{
			updateStep();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		dirty = true;
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded e)
	{
		if (e.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankOpen = true;
			dirty = true;
		}
		else if (e.getGroupId() == InterfaceID.BARROWS_PUZZLE)
		{
			puzzleAnswer = puzzleAnswer();
		}
	}

	/**
	 * The Barrows door puzzle's answer, worked out the same way as RuneLite's Barrows plugin: the right picture's
	 * model is the last one in the sequence minus 3.
	 */
	private Widget puzzleAnswer()
	{
		Widget sequence = client.getWidget(InterfaceID.BarrowsPuzzle._1);
		if (sequence == null)
		{
			return null;
		}
		int answer = sequence.getModelId() - 3;
		for (int id : new int[]{InterfaceID.BarrowsPuzzle.PIC_A, InterfaceID.BarrowsPuzzle.PIC_B,
			InterfaceID.BarrowsPuzzle.PIC_C})
		{
			Widget w = client.getWidget(id);
			if (w != null && w.getModelId() == answer)
			{
				return w;
			}
		}
		return null;
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed e)
	{
		if (e.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankOpen = false;
			dirty = true;
		}
		else if (e.getGroupId() == InterfaceID.BARROWS_PUZZLE)
		{
			puzzleAnswer = null;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		int id = e.getContainerId();
		if (id == InventoryID.BANK)
		{
			bankOwned.clear();
			bankCounts.clear();
			BankItems.count(e.getItemContainer(), itemManager).forEach((canon, qty) ->
			{
				if (trackedItems.contains(canon))
				{
					bankOwned.add(canon);
					bankCounts.put(canon, qty);
				}
			});
			bankSeen = true;
			configManager.setRSProfileConfiguration(GROUP, KEY_BANK, bankOwned.stream().map(String::valueOf)
				.collect(Collectors.joining(",")));
			configManager.setRSProfileConfiguration(GROUP, KEY_BANK_COUNTS, bankCounts.entrySet().stream()
				.map(en -> en.getKey() + ":" + en.getValue())
				.collect(Collectors.joining(",")));
			dirty = true;
		}
		else if (id == InventoryID.INV || id == InventoryID.WORN)
		{
			carriedOwned.clear();
			carriedOwned.addAll(trackedIn(client.getItemContainer(InventoryID.INV)));
			carriedOwned.addAll(trackedIn(client.getItemContainer(InventoryID.WORN)));
			dirty = true;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		int wave = FightCaveWaves.parseWave(Text.removeTags(e.getMessage()));
		if (wave > 0)
		{
			guideFlags.removeIf(f -> f.startsWith(FightCaveWaves.WAVE_FLAG));
			guideFlags.add(FightCaveWaves.WAVE_FLAG + wave);
			dirty = true;
			return;
		}
		KillCountParser.Kill kill = KillCountParser.parse(e.getMessage(), PvmLadder.ALL);
		if (kill == null)
		{
			return;
		}
		String id = kill.getContent().getId();
		// God Wars has a count per general: any of them counts
		killCounts.merge(id, kill.getCount(), Math::max);
		saveKillCounts();
		if (guideContent != null && guideContent.getId().equals(id))
		{
			if ("barrows".equals(id))
			{
				// A looted chest ends the run: the guide says to leave rather than pick up loot
				guideFlags.clear();
				guideFlags.add(PvmGuides.Barrows.LOOTED_FLAG);
			}
			else
			{
				lastKillTick = client.getTickCount();
			}
		}
		dirty = true;
	}

	/**
	 * Looks up the player's boss kill counts on the official hiscores once per login, so bosses killed before the
	 * plugin was installed count too. The hiscores only list a boss from about 5 kills.
	 */
	private void checkHiscores()
	{
		Player me = client.getLocalPlayer();
		if (me == null || me.getName() == null)
		{
			return;
		}
		hiscoresChecked = true;
		hiscoreClient.lookupAsync(me.getName(), HiscoreEndpoint.NORMAL).whenComplete((result, ex) ->
		{
			if (ex != null || result == null)
			{
				log.debug("Hiscore lookup failed", ex);
				return;
			}
			clientThread.invokeLater(() -> mergeHiscores(result));
		});
	}

	private void mergeHiscores(HiscoreResult result)
	{
		boolean changed = false;
		for (Map.Entry<String, List<HiscoreSkill>> e : HISCORES.entrySet())
		{
			int total = 0;
			for (HiscoreSkill skill : e.getValue())
			{
				net.runelite.client.hiscore.Skill entry = result.getSkill(skill);
				if (entry != null && entry.getLevel() > 0)
				{
					total += entry.getLevel();
				}
			}
			if (total > killCounts.getOrDefault(e.getKey(), 0))
			{
				killCounts.put(e.getKey(), total);
				changed = true;
			}
		}
		if (changed)
		{
			saveKillCounts();
			dirty = true;
		}
	}

	private void saveKillCounts()
	{
		configManager.setRSProfileConfiguration(GROUP, KEY_KILL_COUNTS, killCounts.entrySet().stream()
			.map(en -> en.getKey() + ":" + en.getValue())
			.collect(Collectors.joining(",")));
	}

	/**
	 * Another plugin in this jar (Money Making) asked to run the guide for one boss or raid.
	 */
	@Subscribe
	public void onStartPvmGuideRequest(StartPvmGuideRequest e)
	{
		for (PvmContent c : PvmLadder.ALL)
		{
			if (c.getId().equals(e.getContentId()))
			{
				onStartGuide(c);
				SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
				return;
			}
		}
	}

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onTrain(Skill skill, int level)
	{
		// Skilling Helper picks this up when it's turned on
		clientThread.invokeLater(() -> eventBus.post(new TrainSkillRequest(skill, level)));
	}

	@Override
	public void onShowPath(PvmContent content)
	{
		clientThread.invokeLater(() ->
		{
			if (content.getLocation() == null)
			{
				return;
			}
			stopGuide();
			postPath(content.getLocation(), content.isWilderness());
			pathShown = content;
			refresh();
		});
	}

	@Override
	public void onClearPath()
	{
		clientThread.invokeLater(() ->
		{
			clearPath();
			refresh();
		});
	}

	@Override
	public void onOpenWiki(String page)
	{
		LinkBrowser.browse(WIKI_URL + page);
	}

	@Override
	public void onToggleDone(PvmContent content)
	{
		clientThread.invokeLater(() ->
		{
			if (!markedDone.remove(content.getId()))
			{
				markedDone.add(content.getId());
			}
			configManager.setRSProfileConfiguration(GROUP, KEY_MARKED_DONE, String.join(",", markedDone));
			refresh();
		});
	}

	@Override
	public void onStartGuide(PvmContent content)
	{
		clientThread.invokeLater(() ->
		{
			PvmGuide g = PvmGuides.forContent(content.getId());
			if (g == null)
			{
				return;
			}
			stopGuide();
			clearPath();
			guide = g;
			guideContent = content;
			trackTargets(null);
			refresh();
		});
	}

	/**
	 * Outlines what the guide clicks, plus anyone the step itself names, such as a shopkeeper to buy from.
	 */
	private void trackTargets(PvmStep current)
	{
		Set<String> npcs = new HashSet<>();
		Set<String> objects = new HashSet<>();
		GuideResolver.addNames(guide, npcs, objects);
		if (current != null)
		{
			for (Target t : current.getHighlights())
			{
				(t.getKind() == Target.Kind.NPC ? npcs : objects).addAll(t.getNames());
			}
		}
		if (!npcs.equals(trackedNpcs) || !objects.equals(trackedObjects))
		{
			trackedNpcs = npcs;
			trackedObjects = objects;
			targets.track(npcs, objects);
		}
	}

	@Override
	public void onAllowWilderness(boolean allow)
	{
		Wilderness.setAllowed(configManager, allow);
	}

	@Override
	public void onHideOutleveled(boolean hide)
	{
		Outleveled.setHidden(configManager, hide);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (Wilderness.isSettingChange(e))
		{
			boolean allowed = Wilderness.allowed(configManager);
			PvmProgressionPanel p = panel;
			if (p != null)
			{
				SwingUtilities.invokeLater(() -> p.setWildernessAllowed(allowed));
			}
		}
		if (Outleveled.isSettingChange(e))
		{
			boolean hide = Outleveled.hidden(configManager);
			PvmProgressionPanel p = panel;
			if (p != null)
			{
				SwingUtilities.invokeLater(() -> p.setHideOutleveled(hide));
			}
		}
	}

	@Override
	public void onStopGuide()
	{
		clientThread.invokeLater(() ->
		{
			stopGuide();
			refresh();
		});
	}

	// ---- The running guide ----

	private void stopGuide()
	{
		boolean filtered = !bankItems.isEmpty();
		bankItems = Set.of();
		tripStocked = false;
		if (filtered)
		{
			bankFilter.relayout();
		}
		guide = null;
		guideContent = null;
		step = null;
		guideFlags.clear();
		barrowsCrypt = -1;
		trackedNpcs = Set.of();
		trackedObjects = Set.of();
		targets.track(Set.of(), Set.of());
		showRoute(null);
	}

	/**
	 * Works out the current step and points the path and hint arrow at it. Returns true if the step changed.
	 */
	private boolean updateStep()
	{
		if (guide == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}
		trackBarrows();
		GuideState state = guideState();
		if (!PvmGuides.inFightCave(state))
		{
			guideFlags.removeIf(f -> f.startsWith(FightCaveWaves.WAVE_FLAG));
		}
		LoadoutPlan plan = LoadoutPlan.of(guide, state, prices);
		if (plan.isStocked())
		{
			tripStocked = true;
		}
		else if (plan.isAnyLow())
		{
			tripStocked = false;
		}
		state = state.toBuilder().tripStocked(tripStocked).travel(travelPick(state)).build();
		PvmStep next = GuideResolver.resolve(guide, guideContent.getName(), state, plan);
		showRoute(next.getDestination(), next.isLocalRoute());
		Set<Integer> items = plan.bankItems();
		TravelPick pick = state.getTravel();
		if (pick != null && !pick.isCarried())
		{
			items = new HashSet<>(items);
			items.add(pick.getItemId());
		}
		if (!items.equals(bankItems))
		{
			bankItems = items;
			bankFilter.relayout();
		}
		if (next.equals(step))
		{
			return false;
		}
		PvmStep previous = step;
		step = next;
		trackTargets(next);
		// The panel only shows the text: don't redraw it every tick just because the marked path moved
		if (previous != null && Objects.equals(previous.getTitle(), next.getTitle()) && previous.getLines().equals(next.getLines())
			&& Objects.equals(previous.getWave(), next.getWave()))
		{
			return false;
		}
		PvmProgressionPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(() -> p.updateStep(next));
		}
		return true;
	}

	/**
	 * Which crypt the player is in (from the sarcophagus next to them), and which one has the tunnel: the game only
	 * says so in the dialogue after searching its sarcophagus, so that's remembered for the rest of the run.
	 */
	private void trackBarrows()
	{
		barrowsCrypt = -1;
		Player me = client.getLocalPlayer();
		if (!"barrows".equals(guideContent.getId()) || me == null)
		{
			return;
		}
		WorldPoint p = me.getWorldLocation();
		if (p.getRegionID() != PvmGuides.Barrows.CRYPT_REGION)
		{
			// Back above ground: the next run starts fresh once the chest was looted
			guideFlags.remove(PvmGuides.Barrows.LOOTED_FLAG);
			return;
		}
		if (p.getPlane() != 3)
		{
			return;
		}
		int best = Integer.MAX_VALUE;
		for (GuideTargets.Tracked<TileObject> t : targets.getObjects())
		{
			WorldPoint wp = t.getEntity().getWorldLocation();
			int i = indexOf(PvmGuides.Barrows.SARCOPHAGI, t.getEntity().getId());
			if (i >= 0 && wp.getPlane() == p.getPlane() && wp.distanceTo2D(p) < best && wp.distanceTo2D(p) <= 20)
			{
				best = wp.distanceTo2D(p);
				barrowsCrypt = i;
			}
		}
		if (barrowsCrypt >= 0 && tunnelDialogOpen())
		{
			guideFlags.removeIf(f -> f.startsWith(PvmGuides.Barrows.TUNNEL_FLAG));
			guideFlags.add(PvmGuides.Barrows.TUNNEL_FLAG + barrowsCrypt);
		}
	}

	/**
	 * "You've found a hidden tunnel, do you want to enter?" or its options are on screen.
	 */
	private boolean tunnelDialogOpen()
	{
		Widget box = client.getWidget(InterfaceID.Messagebox.TEXT);
		if (box != null && !box.isHidden() && box.getText() != null
			&& box.getText().toLowerCase(Locale.ROOT).contains("hidden tunnel"))
		{
			return true;
		}
		Widget options = client.getWidget(InterfaceID.Chatmenu.OPTIONS);
		if (options != null && !options.isHidden() && options.getDynamicChildren() != null)
		{
			for (Widget o : options.getDynamicChildren())
			{
				if (o.getText() != null && o.getText().toLowerCase(Locale.ROOT).contains("fearless"))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static int indexOf(int[] ids, int id)
	{
		for (int i = 0; i < ids.length; i++)
		{
			if (ids[i] == id)
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * The travel item worth bringing to the guide's place, or null.
	 */
	private TravelPick travelPick(GuideState s)
	{
		Location place = guide.getPlace();
		WorldPoint point = place != null ? place.getPoint() : guideContent.getLocation();
		if (point == null || s.isInInstance())
		{
			return null;
		}
		Map<Integer, Integer> bank = s.getBank();
		return travel.pick(id -> s.getInventory().containsKey(id) || s.getWorn().containsKey(id),
			id -> bank != null && bank.getOrDefault(id, 0) > 0, s.getLocation(), List.of(point),
			place != null ? place.getName() : guideContent.getName(),
			guideContent.isWilderness() && Wilderness.contains(point));
	}

	private GuideState guideState()
	{
		GuideState.GuideStateBuilder b = GuideState.builder()
			.members(client.getWorldType().contains(WorldType.MEMBERS))
			.preferShops(config.preferShops())
			.bank(bankSeen ? new HashMap<>(bankCounts) : null)
			.bankOpen(bankOpen)
			.hitpoints(client.getBoostedSkillLevel(Skill.HITPOINTS))
			.maxHitpoints(client.getRealSkillLevel(Skill.HITPOINTS))
			.prayer(client.getBoostedSkillLevel(Skill.PRAYER))
			.inInstance(client.getTopLevelWorldView().isInstance())
			.justKilled(client.getTickCount() - lastKillTick <= LOOT_TICKS)
			.flags(guideFlags)
			.crypt(barrowsCrypt);
		for (Skill skill : Skill.values())
		{
			b.level(skill, client.getRealSkillLevel(skill));
			b.boosted(skill, client.getBoostedSkillLevel(skill));
		}
		int used = 0;
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv != null)
		{
			Map<Integer, Integer> counts = new HashMap<>();
			for (Item item : inv.getItems())
			{
				if (item.getId() > 0)
				{
					counts.merge(item.getId(), item.getQuantity(), Integer::sum);
					used++;
				}
			}
			b.inventory(counts);
		}
		b.usedSlots(used);
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn != null)
		{
			Map<Integer, Integer> counts = new HashMap<>();
			for (Item item : worn.getItems())
			{
				if (item.getId() > 0)
				{
					counts.merge(item.getId(), item.getQuantity(), Integer::sum);
				}
			}
			b.worn(counts);
		}
		Player me = client.getLocalPlayer();
		WorldPoint here = null;
		if (me != null)
		{
			LocalPoint local = me.getLocalLocation();
			here = local == null ? me.getWorldLocation() : WorldPoint.fromLocalInstance(client, local);
			b.location(here);
		}
		if (guide != null)
		{
			Map<Integer, Integer> varbits = new HashMap<>();
			for (int v : PvmGuides.Barrows.KILLED)
			{
				varbits.put(v, client.getVarbitValue(v));
			}
			for (int v : PvmGuides.GOD_WARS_COUNTERS)
			{
				varbits.put(v, client.getVarbitValue(v));
			}
			for (int v : guide.getWatched())
			{
				varbits.put(v, client.getVarbitValue(v));
			}
			b.varbits(varbits);
			Map<String, Integer> npcs = new HashMap<>();
			WorldPoint mine = me == null ? null : me.getWorldLocation();
			for (GuideTargets.Tracked<NPC> t : targets.getNpcs())
			{
				WorldPoint wp = t.getEntity().getWorldLocation();
				if (mine != null && wp != null && wp.getPlane() == mine.getPlane() && wp.distanceTo2D(mine) <= NPC_RANGE
					&& !t.getEntity().isDead())
				{
					npcs.merge(t.getName().toLowerCase(Locale.ROOT), 1, Integer::sum);
				}
			}
			b.npcs(npcs);
			if ("barrows".equals(guideContent.getId()) && here != null
				&& here.getRegionID() == PvmGuides.Barrows.CRYPT_REGION && here.getPlane() == 0)
			{
				b.tunnel(barrowsTunnel.route());
			}
		}
		return b.build();
	}

	/**
	 * Hint arrow and Shortest Path route to where the step goes, or clears them when null.
	 */
	private void showRoute(Location place)
	{
		showRoute(place, false);
	}

	/**
	 * With {@code local}, only the hint arrow points there: Shortest Path can't route through e.g. the Barrows
	 * tunnels' doors.
	 */
	private void showRoute(Location place, boolean local)
	{
		if (Objects.equals(place, routed) && local == routedLocal)
		{
			return;
		}
		routed = place;
		routedLocal = local;
		if (hintArrowSet)
		{
			client.clearHintArrow();
			hintArrowSet = false;
		}
		if (place == null)
		{
			if (pathShown == null)
			{
				ShortestPathLink.clear(eventBus, this);
			}
			return;
		}
		if (config.hintArrow())
		{
			client.setHintArrow(place.getPoint());
			hintArrowSet = true;
		}
		if (local)
		{
			if (pathShown == null)
			{
				ShortestPathLink.clear(eventBus, this);
			}
		}
		else if (config.shortestPath())
		{
			postPath(place.getPoint(), guideContent != null && guideContent.isWilderness());
		}
	}

	private void postPath(WorldPoint target, boolean wilderness)
	{
		Map<String, Object> data = new HashMap<>();
		data.put("target", target);
		// Only path through the Wilderness when the place itself is there, whatever Shortest Path's own setting says
		Map<String, Object> overrides = new HashMap<>();
		overrides.put("avoidWilderness", !(wilderness && Wilderness.contains(target)));
		ShortestPathLink.allowCarriedTeleports(configManager, overrides);
		data.put("config", overrides);
		ShortestPathLink.path(eventBus, this, data);
	}

	// ---- State ----

	private void clearPath()
	{
		if (pathShown != null)
		{
			ShortestPathLink.clear(eventBus, this);
			pathShown = null;
		}
	}

	private void refreshQuests()
	{
		questRefreshCountdown = QUEST_REFRESH_TICKS;
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		for (PvmContent c : PvmLadder.ALL)
		{
			for (QuestReq q : c.getQuests())
			{
				questStates.put(q.getQuest(), q.getQuest().getState(client));
			}
		}
	}

	private void refresh()
	{
		dirty = false;
		if (panel == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		PlayerView.PlayerViewBuilder view = PlayerView.builder()
			.quests(questStates)
			.owned(bankOwned)
			.owned(carriedOwned)
			.killCounts(killCounts)
			.markedDone(markedDone);
		for (Skill skill : Skill.values())
		{
			view.level(skill, client.getRealSkillLevel(skill));
		}
		PlayerView player = view.build();

		List<Readiness> ladder = new ArrayList<>();
		for (PvmContent c : PvmLadder.BY_LEVEL)
		{
			ladder.add(Readiness.of(c, player));
		}

		// Every guide's loadout, so the panel can show the one picked even when it isn't running
		GuideState state = guideState();
		Map<String, LoadoutPlan> plans = new HashMap<>();
		for (PvmGuide g : PvmGuides.ALL)
		{
			plans.put(g.getContentId(), LoadoutPlan.of(g, state, prices));
		}
		updateStep();

		String pathId = pathShown == null ? null : pathShown.getId();
		String guideId = guideContent == null ? null : guideContent.getId();
		PvmStep current = step;
		boolean bankKnown = bankSeen;
		PvmProgressionPanel.GuideView guides = new PvmProgressionPanel.GuideView(guideId, current, plans, bankKnown);
		// Every xp drop asks for a refresh, but the panel only needs redrawing when what it shows changed:
		// rebuilding it takes tens of milliseconds on the Swing thread
		List<Object> shown = Arrays.asList(ladder, player, pathId, guides);
		if (shown.equals(lastShown))
		{
			return;
		}
		lastShown = shown;
		PvmProgressionPanel p = panel;
		SwingUtilities.invokeLater(() -> p.update(ladder, player, pathId, guides));
	}

	private Set<Integer> trackedIn(ItemContainer container)
	{
		Set<Integer> found = new HashSet<>();
		if (container == null)
		{
			return found;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				int id = itemManager.canonicalize(item.getId());
				if (trackedItems.contains(id))
				{
					found.add(id);
				}
			}
		}
		return found;
	}

	private void loadProfile()
	{
		killCounts.clear();
		markedDone.clear();
		bankOwned.clear();
		bankCounts.clear();
		String kcs = configManager.getRSProfileConfiguration(GROUP, KEY_KILL_COUNTS);
		for (String entry : split(kcs))
		{
			String[] parts = entry.split(":");
			if (parts.length == 2)
			{
				try
				{
					killCounts.put(parts[0], Integer.parseInt(parts[1]));
				}
				catch (NumberFormatException ex)
				{
					log.debug("Bad kill count entry {}", entry);
				}
			}
		}
		markedDone.addAll(split(configManager.getRSProfileConfiguration(GROUP, KEY_MARKED_DONE)));
		for (String id : split(configManager.getRSProfileConfiguration(GROUP, KEY_BANK)))
		{
			try
			{
				bankOwned.add(Integer.parseInt(id));
			}
			catch (NumberFormatException ex)
			{
				log.debug("Bad bank item id {}", id);
			}
		}
		String counts = configManager.getRSProfileConfiguration(GROUP, KEY_BANK_COUNTS);
		for (String entry : split(counts))
		{
			String[] parts = entry.split(":");
			try
			{
				bankCounts.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
			}
			catch (NumberFormatException | ArrayIndexOutOfBoundsException ex)
			{
				log.debug("Bad bank count entry {}", entry);
			}
		}
		if (counts != null)
		{
			// Saves from before placeholders were skipped can list an item with 0 left
			bankOwned.removeIf(id -> bankCounts.getOrDefault(id, 0) <= 0);
			bankCounts.values().removeIf(q -> q <= 0);
		}
		// Older saves only have what's owned, not how many: the bank needs opening once for exact amounts
		bankSeen = counts != null;
		profileLoaded = true;
	}

	private static List<String> split(String raw)
	{
		List<String> out = new ArrayList<>();
		if (raw != null)
		{
			for (String s : raw.split(","))
			{
				if (!s.isEmpty())
				{
					out.add(s);
				}
			}
		}
		return out;
	}

	private static final Set<Integer> AMMO_IDS = ammoIds();

	private static Set<Integer> ammoIds()
	{
		Set<Integer> ids = new HashSet<>();
		for (Gear.Ammo ammo : Gear.Ammo.values())
		{
			Supply s = Supplies.ammo(ammo);
			if (s != null)
			{
				for (int id : s.allIds())
				{
					ids.add(id);
				}
			}
		}
		return ids;
	}
}
