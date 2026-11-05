package com.skillinghelper.diary;

import com.google.inject.Provides;
import com.skillinghelper.ShortestPathLink;
import com.skillinghelper.StartDiaryGuideRequest;
import com.skillinghelper.bank.BankItems;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.data.Diaries;
import com.skillinghelper.diary.data.DiaryItem;
import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.diary.data.DiaryTier;
import com.skillinghelper.diary.ui.DiaryPanel;
import com.skillinghelper.overlay.NameTargets;
import com.skillinghelper.travel.TravelAdvisor;
import com.skillinghelper.travel.TravelPick;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
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
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
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
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * Achievement Diary guides, Quest Helper style: which tasks of a tier are done (read from the diary varps), what's
 * still missing for the rest, and a step-by-step guide through each task with the route drawn and what to click
 * outlined. Also shows what every tier's reward is and does.
 */
@PluginDescriptor(
	name = "Achievement Diary Guides",
	description = "Step-by-step guides through every Achievement Diary task, and what each tier's reward gives you",
	tags = {"achievement", "diary", "diaries", "guide", "helper", "tasks", "rewards", "lumbridge", "draynor"}
)
public class AchievementDiaryPlugin extends Plugin implements DiaryPanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	/**
	 * How often quest states are read again (they're client scripts).
	 */
	private static final int QUEST_REFRESH_TICKS = 50;

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
	private AchievementDiaryConfig config;
	@Inject
	private TravelAdvisor travel;
	@Inject
	private ModelOutlineRenderer outlineRenderer;

	private NameTargets targets;
	private DiarySceneOverlay sceneOverlay;
	private DiaryItemOverlay itemOverlay;
	private DiaryPanel panel;
	private NavigationButton navButton;

	private DiaryRegion region = DiaryRegion.LUMBRIDGE;
	private DiaryTier tier = DiaryTier.EASY;
	private boolean running;
	/**
	 * Task the player clicked in the list, done first; null to go in walking order.
	 */
	private String picked;
	private final Set<String> skipped = new HashSet<>();

	private final Map<Quest, QuestState> quests = new HashMap<>();
	private int questCountdown;
	private Map<Integer, Integer> bankCounts;
	private boolean checkShortestPath = true;

	/**
	 * What to do right now, read by the overlays. Null when no guide is running.
	 */
	@Getter
	private volatile DiaryStep step;
	private WorldPoint pathTarget;
	private boolean hintArrowSet;
	private DiaryView shownView;

	@Override
	protected void startUp()
	{
		targets = new NameTargets(client);
		sceneOverlay = new DiarySceneOverlay(client, this::getStep, config::targetColor, targets, outlineRenderer);
		itemOverlay = new DiaryItemOverlay(this::getStep, config::itemColor, itemManager, configManager);
		eventBus.register(targets);
		overlayManager.add(sceneOverlay);
		overlayManager.add(itemOverlay);
		region = savedRegion();
		tier = savedTier();
		panel = new DiaryPanel(this);
		shownView = null;
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Achievement Diary Guides")
			.icon(icon)
			.priority(11)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		questCountdown = 0;
		checkShortestPath = true;
		clientThread.invokeLater(this::refresh);
	}

	@Override
	protected void shutDown()
	{
		eventBus.unregister(targets);
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
		picked = null;
		skipped.clear();
		bankCounts = null;
		quests.clear();
		panel = null;
		shownView = null;
	}

	@Provides
	AchievementDiaryConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AchievementDiaryConfig.class);
	}

	// ---- Game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			questCountdown = 0;
			checkShortestPath = true;
		}
		else if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			quests.clear();
			bankCounts = null;
			pathTarget = null;
			hintArrowSet = false;
			skipped.clear();
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
		if (AchievementDiaryConfig.GROUP.equals(e.getGroup()) || Wilderness.isSettingChange(e))
		{
			boolean pick = AchievementDiaryConfig.GROUP.equals(e.getGroup())
				&& ("region".equals(e.getKey()) || "tier".equals(e.getKey()));
			clientThread.invokeLater(() ->
			{
				if (pick)
				{
					// Also set from the Quest Order Guide's "Show in Achievement Diary Guides"
					DiaryRegion r = savedRegion();
					DiaryTier t = savedTier();
					if (r != region || t != tier)
					{
						picked = null;
						region = r;
						tier = t;
					}
				}
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
	 * Another plugin in this jar (Account Unlocks) asked to guide one diary tier: pick it, start, open the panel.
	 */
	@Subscribe
	public void onStartDiaryGuideRequest(StartDiaryGuideRequest e)
	{
		configManager.setConfiguration(AchievementDiaryConfig.GROUP, "region", e.getRegion().name());
		configManager.setConfiguration(AchievementDiaryConfig.GROUP, "tier", e.getTier().name());
		if (e.getRegion() != region || e.getTier() != tier)
		{
			picked = null;
		}
		region = e.getRegion();
		tier = e.getTier();
		running = true;
		checkShortestPath = true;
		refresh();
		SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
	}

	// ---- Reading the player ----

	private void readQuests()
	{
		quests.clear();
		Set<Quest> wanted = new HashSet<>();
		for (DiaryTask t : Diaries.all())
		{
			wanted.addAll(t.getQuests());
			wanted.addAll(t.getQuestsStarted());
		}
		for (Quest q : wanted)
		{
			quests.put(q, q.getState(client));
		}
	}

	private DiaryPlayer readPlayer()
	{
		Set<String> done = new HashSet<>();
		Map<Integer, Integer> varps = new HashMap<>();
		for (DiaryTask t : Diaries.all())
		{
			if (DiaryGuide.isDone(t, id -> varps.computeIfAbsent(id, client::getVarpValue), client::getVarbitValue))
			{
				done.add(t.key());
			}
		}
		Set<String> claimed = new HashSet<>();
		for (DiaryRegion r : DiaryRegion.values())
		{
			for (DiaryTier tr : DiaryTier.values())
			{
				if (client.getVarbitValue(r.rewardVarbit(tr)) > 0)
				{
					claimed.add(r.name() + "." + tr.name());
				}
			}
		}
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
			.quests(new HashMap<>(quests))
			.done(done)
			.claimed(claimed)
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
		DiaryPlayer p = loggedIn ? readPlayer() : null;
		DiaryStep next = null;
		if (running && p != null)
		{
			next = DiaryGuide.resolve(region, tier, p, skipped, picked);
			Location dest = next.getDestination();
			if (dest != null && !client.getTopLevelWorldView().isInstance())
			{
				Map<Integer, Integer> bank = p.getBank();
				List<TravelPick> picks = travel.plan(id -> p.getCarried().getOrDefault(id, 0),
					id -> bank == null ? 0 : bank.getOrDefault(id, 0), p.getLocation(),
					List.of(dest.getPoint()), dest.getName(), Wilderness.contains(dest) && p.isAllowWilderness());
				if (!picks.isEmpty())
				{
					next = DiaryGuide.resolve(region, tier, p.toBuilder().travel(picks).build(), skipped, picked);
				}
			}
			if (picked != null && p.getDone().contains(picked))
			{
				// The picked task is done: back to walking order
				picked = null;
			}
		}
		step = next;
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
		DiaryView view = buildView(p, next);
		DiaryPanel target = panel;
		if (target != null && !view.equals(shownView))
		{
			shownView = view;
			SwingUtilities.invokeLater(() -> target.update(view));
		}
	}

	private DiaryView buildView(DiaryPlayer p, DiaryStep current)
	{
		Map<DiaryRegion, Integer> claimedTiers = new EnumMap<>(DiaryRegion.class);
		List<DiaryView.TierRow> tierRows = new ArrayList<>();
		List<DiaryView.TaskRow> taskRows = new ArrayList<>();
		Set<String> stillNeeded = new LinkedHashSet<>();
		long tierCost = 0;
		if (p != null)
		{
			for (DiaryRegion r : DiaryRegion.values())
			{
				int n = 0;
				for (DiaryTier tr : DiaryTier.values())
				{
					n += DiaryGuide.claimed(r, tr, p) ? 1 : 0;
				}
				claimedTiers.put(r, n);
			}
			for (DiaryTier tr : DiaryTier.values())
			{
				List<DiaryTask> tasks = Diaries.tasks(region, tr);
				int done = 0;
				for (DiaryTask t : tasks)
				{
					done += DiaryGuide.done(t, p) ? 1 : 0;
				}
				tierRows.add(new DiaryView.TierRow(tr, done, tasks.size(), DiaryGuide.claimed(region, tr, p)));
			}
			for (DiaryTask t : Diaries.tasks(region, tier))
			{
				List<String> missing = DiaryGuide.done(t, p) ? List.of() : DiaryGuide.missing(t, p);
				DiaryView.TaskState state;
				if (DiaryGuide.done(t, p))
				{
					state = DiaryView.TaskState.DONE;
				}
				else if (current != null && t.key().equals(current.getTaskKey()))
				{
					state = DiaryView.TaskState.CURRENT;
				}
				else if (!missing.isEmpty())
				{
					state = DiaryView.TaskState.LOCKED;
				}
				else if (skipped.contains(t.key()))
				{
					state = DiaryView.TaskState.SKIPPED;
				}
				else
				{
					state = DiaryView.TaskState.READY;
				}
				stillNeeded.addAll(missing);
				List<String> items = new ArrayList<>();
				for (DiaryItem item : t.getItems())
				{
					items.add((item.getAmount() > 1 ? String.format("%,d", item.getAmount()) + " x " : "")
						+ item.getName());
				}
				long cost = state == DiaryView.TaskState.DONE ? 0 : DiaryGuide.cost(t, p);
				tierCost += cost;
				taskRows.add(new DiaryView.TaskRow(t.key(), t.getNumber(), t.label(), t.getText(), state, missing,
					t.getNotes(), items, t.isWilderness(), cost));
			}
		}
		taskRows.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
		return DiaryView.builder()
			.region(region)
			.tier(tier)
			.loggedIn(p != null)
			.guided(Diaries.guided(region))
			.claimedTiers(claimedTiers)
			.tiers(tierRows)
			.tasks(taskRows)
			.stillNeeded(new ArrayList<>(stillNeeded))
			.tierCost(tierCost)
			.running(running)
			.step(current)
			.wildernessAllowed(Wilderness.allowed(configManager))
			.build();
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

	// ---- Saved picks ----

	private DiaryRegion savedRegion()
	{
		String s = configManager.getConfiguration(AchievementDiaryConfig.GROUP, "region");
		try
		{
			return s == null ? DiaryRegion.LUMBRIDGE : DiaryRegion.valueOf(s);
		}
		catch (IllegalArgumentException e)
		{
			return DiaryRegion.LUMBRIDGE;
		}
	}

	private DiaryTier savedTier()
	{
		String s = configManager.getConfiguration(AchievementDiaryConfig.GROUP, "tier");
		try
		{
			return s == null ? DiaryTier.EASY : DiaryTier.valueOf(s);
		}
		catch (IllegalArgumentException e)
		{
			return DiaryTier.EASY;
		}
	}

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onSelect(DiaryRegion r, DiaryTier t)
	{
		configManager.setConfiguration(AchievementDiaryConfig.GROUP, "region", r.name());
		configManager.setConfiguration(AchievementDiaryConfig.GROUP, "tier", t.name());
		clientThread.invokeLater(() ->
		{
			if (r != region || t != tier)
			{
				picked = null;
			}
			region = r;
			tier = t;
			refresh();
		});
	}

	@Override
	public void onStartGuide()
	{
		clientThread.invokeLater(() ->
		{
			running = true;
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
			picked = null;
			refresh();
		});
	}

	@Override
	public void onPickTask(String key)
	{
		clientThread.invokeLater(() ->
		{
			picked = key;
			skipped.remove(key);
			running = true;
			refresh();
		});
	}

	@Override
	public void onSkipTask(String key)
	{
		clientThread.invokeLater(() ->
		{
			skipped.add(key);
			if (key.equals(picked))
			{
				picked = null;
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
	public void onOpenWiki()
	{
		LinkBrowser.browse(WIKI_URL + region.getWikiPage());
	}
}
