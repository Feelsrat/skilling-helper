package com.skillinghelper.unlocks;

import com.google.inject.Provides;
import com.skillinghelper.OpenUnlockRequest;
import com.skillinghelper.OpenMethodRequest;
import com.skillinghelper.QuestHelperLink;
import com.skillinghelper.ShortestPathLink;
import com.skillinghelper.SkillingHelperPlugin;
import com.skillinghelper.StartDiaryGuideRequest;
import com.skillinghelper.TrainSkillRequest;
import com.skillinghelper.bank.BankItems;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.AchievementDiaryPlugin;
import com.skillinghelper.diary.DiaryGuide;
import com.skillinghelper.diary.DiaryItemOverlay;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiarySceneOverlay;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.diary.data.Diaries;
import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.diary.data.DiaryTier;
import com.skillinghelper.overlay.NameTargets;
import com.skillinghelper.travel.TravelAdvisor;
import com.skillinghelper.travel.TravelPick;
import com.skillinghelper.unlocks.data.Grind;
import com.skillinghelper.unlocks.data.QuestPart;
import com.skillinghelper.unlocks.data.Unlock;
import com.skillinghelper.unlocks.data.Unlocks;
import com.skillinghelper.unlocks.ui.UnlocksPanel;
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
 * A checklist of the account unlocks that make the game smoother (graceful, fairy rings, spirit trees, rune
 * pouch...), in a suggested order, with what each gives and which ones the player already has. The guide takes the
 * player through the next one: quests are handed to Quest Helper, diary rewards to Achievement Diary Guides, points
 * to earn to Skilling Helper, and the last part (buy it, claim it) is guided here step by step.
 */
@PluginDescriptor(
	name = "Account Unlocks",
	description = "Checklist of key account unlocks (graceful, fairy rings, spirit trees, rune pouch...) with what "
		+ "each gives and a step-by-step guide to the next one",
	tags = {"unlocks", "account", "progress", "checklist", "graceful", "fairy", "rings", "spirit", "trees", "pouch",
		"guide", "helper"}
)
public class AccountUnlocksPlugin extends Plugin implements UnlocksPanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	/**
	 * How often quest states and the other plugins' on/off states are read again.
	 */
	private static final int SLOW_REFRESH_TICKS = 50;

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
	private AccountUnlocksConfig config;
	@Inject
	private TravelAdvisor travel;
	@Inject
	private ModelOutlineRenderer outlineRenderer;

	private NameTargets targets;
	private DiarySceneOverlay sceneOverlay;
	private DiaryItemOverlay itemOverlay;
	private UnlocksPanel panel;
	private NavigationButton navButton;

	private boolean running;
	/**
	 * Unlock the player clicked in the list, guided first; null to go in the suggested order.
	 */
	private String picked;
	private final Set<String> skipped = new HashSet<>();

	private final Map<Quest, QuestState> quests = new HashMap<>();
	private int slowCountdown;
	private boolean questHelperOn;
	private boolean diaryGuidesOn;
	private boolean skillingHelperOn;
	private Map<Integer, Integer> bankCounts;
	private boolean checkShortestPath = true;

	/**
	 * What to do right now, read by the overlays. Null when no guide is running.
	 */
	private volatile DiaryStep step;
	@Getter
	private volatile UnlockStep unlockStep;
	private WorldPoint pathTarget;
	private boolean hintArrowSet;
	private UnlockView shownView;

	@Override
	protected void startUp()
	{
		targets = new NameTargets(client);
		sceneOverlay = new DiarySceneOverlay(client, () -> step, config::targetColor, targets, outlineRenderer);
		itemOverlay = new DiaryItemOverlay(() -> step, config::itemColor, itemManager, configManager);
		eventBus.register(targets);
		overlayManager.add(sceneOverlay);
		overlayManager.add(itemOverlay);
		panel = new UnlocksPanel(this);
		shownView = null;
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Account Unlocks")
			.icon(icon)
			.priority(15)
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
		unlockStep = null;
		picked = null;
		skipped.clear();
		bankCounts = null;
		quests.clear();
		panel = null;
		shownView = null;
	}

	@Provides
	AccountUnlocksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AccountUnlocksConfig.class);
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
		if (AccountUnlocksConfig.GROUP.equals(e.getGroup()) || Wilderness.isSettingChange(e))
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
		if (checkShortestPath)
		{
			checkShortestPath = false;
			if (config.shortestPath() && running)
			{
				ShortestPathLink.warnIfMissing(client, pluginManager);
			}
		}
		if (--slowCountdown <= 0)
		{
			slowCountdown = SLOW_REFRESH_TICKS;
			readQuests();
			readPlugins();
		}
		refresh();
	}

	// ---- Reading the player ----

	private void readQuests()
	{
		quests.clear();
		Set<Quest> wanted = new HashSet<>();
		for (Unlock u : Unlocks.all())
		{
			if (u.getDoneQuest() != null)
			{
				wanted.add(u.getDoneQuest());
			}
			for (QuestPart q : u.getQuests())
			{
				wanted.add(q.getQuest());
			}
			if (u.getGuide() != null)
			{
				wanted.addAll(u.getGuide().getQuests());
				wanted.addAll(u.getGuide().getQuestsStarted());
			}
		}
		for (Quest q : wanted)
		{
			quests.put(q, q.getState(client));
		}
	}

	private void readPlugins()
	{
		questHelperOn = QuestHelperLink.isActive(pluginManager);
		diaryGuidesOn = false;
		skillingHelperOn = false;
		for (Plugin p : pluginManager.getPlugins())
		{
			if (p instanceof AchievementDiaryPlugin)
			{
				diaryGuidesOn = pluginManager.isPluginActive(p);
			}
			else if (p instanceof SkillingHelperPlugin)
			{
				skillingHelperOn = pluginManager.isPluginActive(p);
			}
		}
	}

	private UnlockPlayer readPlayer()
	{
		Set<String> claimed = new HashSet<>();
		Map<String, int[]> progress = new HashMap<>();
		Map<Integer, Integer> varps = new HashMap<>();
		for (Unlock u : Unlocks.all())
		{
			if (!u.isDiary())
			{
				continue;
			}
			DiaryRegion r = u.getDiaryRegion();
			DiaryTier tier = u.getDiaryTier();
			String key = r.name() + "." + tier.name();
			if (client.getVarbitValue(r.rewardVarbit(tier)) > 0)
			{
				claimed.add(key);
			}
			List<DiaryTask> tasks = Diaries.tasks(r, tier);
			int done = 0;
			for (DiaryTask t : tasks)
			{
				done += DiaryGuide.isDone(t, id -> varps.computeIfAbsent(id, client::getVarpValue),
					client::getVarbitValue) ? 1 : 0;
			}
			progress.put(key, new int[]{done, tasks.size()});
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
		DiaryPlayer p = DiaryPlayer.builder()
			.location(me == null ? null : WorldPoint.fromLocalInstance(client, me.getLocalLocation()))
			.levels(levels)
			.boosted(boosted)
			.combatLevel(me == null ? 3 : me.getCombatLevel())
			.quests(new HashMap<>(quests))
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
		return UnlockPlayer.builder()
			.player(p)
			.diaryProgress(progress)
			.questHelper(questHelperOn)
			.diaryGuides(diaryGuidesOn)
			.skillingHelper(skillingHelperOn)
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
	 * Works out the step (if the guide runs) and the panel view, and sends them on when they changed. Client thread.
	 */
	private void refresh()
	{
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
		UnlockPlayer up = loggedIn ? readPlayer() : null;
		UnlockStep next = null;
		if (running && up != null)
		{
			next = UnlockGuide.resolve(Unlocks.all(), up, skipped, picked);
			Location dest = next.getStep().getDestination();
			if (dest != null && !client.getTopLevelWorldView().isInstance())
			{
				DiaryPlayer p = up.getPlayer();
				Map<Integer, Integer> bank = p.getBank();
				List<TravelPick> picks = travel.plan(id -> p.getCarried().getOrDefault(id, 0),
					id -> bank == null ? 0 : bank.getOrDefault(id, 0), p.getLocation(),
					List.of(dest.getPoint()), dest.getName(), Wilderness.contains(dest) && p.isAllowWilderness());
				if (!picks.isEmpty())
				{
					UnlockPlayer withTravel = up.toBuilder().player(p.toBuilder().travel(picks).build()).build();
					next = UnlockGuide.resolve(Unlocks.all(), withTravel, skipped, picked);
				}
			}
			if (picked != null)
			{
				Unlock u = Unlocks.byId(picked);
				if (u == null || UnlockGuide.done(u, up))
				{
					// The picked unlock is done: back to the suggested order
					picked = null;
				}
			}
			if (next.getAction() == UnlockStep.Action.QUEST)
			{
				QuestHelperLink.warnIfMissing(client, pluginManager);
			}
		}
		unlockStep = next;
		step = next == null ? null : next.getStep();
		if (step != null)
		{
			targets.track(step.getNpcs(), step.getObjects());
			showWay(step.getDestination());
		}
		else
		{
			targets.track(Collections.emptySet(), Collections.emptySet());
			clearWay();
		}
		UnlockView view = buildView(up, next);
		UnlocksPanel target = panel;
		if (target != null && !view.equals(shownView))
		{
			shownView = view;
			SwingUtilities.invokeLater(() -> target.update(view));
		}
	}

	private UnlockView buildView(UnlockPlayer up, UnlockStep current)
	{
		List<UnlockView.Row> rows = new ArrayList<>();
		int done = 0;
		String currentId = current == null ? null : current.getUnlockId();
		String stepWiki = null;
		if (up != null)
		{
			for (Unlock u : Unlocks.all())
			{
				UnlockGuide.State s = UnlockGuide.state(u, up);
				UnlockView.RowState state;
				if (s == UnlockGuide.State.DONE)
				{
					state = UnlockView.RowState.DONE;
					done++;
				}
				else if (u.getId().equals(currentId))
				{
					state = UnlockView.RowState.CURRENT;
				}
				else if (s == UnlockGuide.State.LOCKED)
				{
					state = UnlockView.RowState.LOCKED;
				}
				else if (skipped.contains(u.getId()))
				{
					state = UnlockView.RowState.SKIPPED;
				}
				else
				{
					state = s == UnlockGuide.State.UNKNOWN ? UnlockView.RowState.UNKNOWN : UnlockView.RowState.READY;
				}
				if (u.getId().equals(currentId))
				{
					stepWiki = u.getWikiPage();
				}
				if (state == UnlockView.RowState.DONE && config.hideDone())
				{
					continue;
				}
				rows.add(new UnlockView.Row(u.getId(), u.getName(), u.getCategory().getLabel(), state,
					UnlockGuide.gives(u), UnlockGuide.how(u, up), UnlockGuide.missing(u, up), UnlockGuide.cost(u, up)));
			}
		}
		return UnlockView.builder()
			.loggedIn(up != null)
			.done(done)
			.total(Unlocks.all().size())
			.rows(rows)
			.running(running)
			.step(current)
			.stepWiki(stepWiki)
			.hideDone(config.hideDone())
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

	// ---- Panel callbacks (Swing thread) ----

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

	/**
	 * Another plugin in this jar (Account Planner) asked to open one unlock: pick it, start the guide and open the
	 * panel.
	 */
	@Subscribe
	public void onOpenUnlockRequest(OpenUnlockRequest e)
	{
		picked = e.getUnlockId();
		skipped.remove(picked);
		running = true;
		refresh();
		SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
	}

	@Override
	public void onPick(String id)
	{
		clientThread.invokeLater(() ->
		{
			picked = id;
			skipped.remove(id);
			running = true;
			refresh();
		});
	}

	@Override
	public void onSkip(String id)
	{
		clientThread.invokeLater(() ->
		{
			skipped.add(id);
			if (id.equals(picked))
			{
				picked = null;
			}
			refresh();
		});
	}

	@Override
	public void onHideDone(boolean hide)
	{
		configManager.setConfiguration(AccountUnlocksConfig.GROUP, "hideDone", hide);
	}

	@Override
	public void onAction(String id)
	{
		Unlock u = Unlocks.byId(id);
		if (u == null)
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			if (u.isDiary())
			{
				eventBus.post(new StartDiaryGuideRequest(u.getDiaryRegion(), u.getDiaryTier()));
				return;
			}
			Grind g = u.getGrind();
			if (g == null)
			{
				return;
			}
			if (g.getMethodId() != null)
			{
				eventBus.post(new OpenMethodRequest(g.getMethodId()));
			}
			else if (g.getSkill() != null)
			{
				eventBus.post(new TrainSkillRequest(g.getSkill(), client.getRealSkillLevel(g.getSkill()) + 5));
			}
		});
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
