package com.skillinghelper.ca;

import com.google.inject.Provides;
import com.skillinghelper.OpenCombatAchievementsRequest;
import com.skillinghelper.ShortestPathLink;
import com.skillinghelper.StartPvmGuideRequest;
import com.skillinghelper.bank.BankItems;
import com.skillinghelper.ca.data.CaMonster;
import com.skillinghelper.ca.data.CaMonsters;
import com.skillinghelper.ca.data.CaTask;
import com.skillinghelper.ca.data.CaTasks;
import com.skillinghelper.ca.data.CaTier;
import com.skillinghelper.ca.ui.CaPanel;
import com.skillinghelper.data.Location;
import com.skillinghelper.data.Wilderness;
import com.skillinghelper.diary.DiaryPlayer;
import com.skillinghelper.diary.DiaryStep;
import com.skillinghelper.overlay.NameTargets;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
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
 * Combat Achievements, tier by tier: which tasks are done (read from the game), the points and what each tier's
 * reward gives, the tasks ranked easiest first for the player's stats and gear, and a Quest Helper style guide
 * through each one: what to bring, the way there, how to get in and what the task asks, with the boss outlined.
 */
@PluginDescriptor(
	name = "Combat Achievement Guides",
	description = "Combat Achievements tier by tier: easiest tasks for you first, step-by-step guides, and what each "
		+ "tier's reward gives",
	tags = {"combat", "achievements", "tasks", "ca", "ghommal", "hilt", "guide", "helper", "boss", "rewards"}
)
public class CombatAchievementsPlugin extends Plugin implements CaPanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final String KEY_SCOPE = "scope";
	private static final String KEY_KC = "killCounts";
	private static final String ALL = "ALL";
	private static final int QUEST_REFRESH_TICKS = 50;
	private static final int PAGE = 25;
	/**
	 * The boss counts as close when it's within this many tiles.
	 */
	private static final int NEAR_BOSS = 15;
	/**
	 * Back on the surface and this far from the entrance: the player left, so the walk starts over.
	 */
	private static final int LEFT_ENTRANCE = 60;
	private static final Pattern KILL_COUNT = Pattern.compile(
		"Your (?:completed |subdued )?(.+?) (?:kill |completion |success |chest |harvest |)count is: ?([0-9,]+)");

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
	private CombatAchievementsConfig config;
	@Inject
	private TravelAdvisor travel;
	@Inject
	private CaItemOverlay itemOverlay;
	@Inject
	private ModelOutlineRenderer outlineRenderer;

	private NameTargets targets;
	private CaSceneOverlay sceneOverlay;
	private CaPanel panel;
	private NavigationButton navButton;

	/**
	 * Tier picked in the panel, or null for every tier.
	 */
	private CaTier scope;
	/**
	 * Only this boss's tasks, when another plugin opened the panel on one boss; null for all.
	 */
	private String focus;
	private int limit = PAGE;
	private boolean running;
	private String picked;
	private final Set<String> skipped = new HashSet<>();
	private final Set<String> arrived = new HashSet<>();
	private final Map<String, Integer> killCounts = new HashMap<>();

	private final Map<Quest, QuestState> quests = new HashMap<>();
	private int questCountdown;
	private Map<Integer, Integer> bankCounts;
	private boolean checkShortestPath = true;

	@Getter
	private volatile DiaryStep step;
	private WorldPoint pathTarget;
	private boolean hintArrowSet;
	private CaView shownView;
	private Set<String> trackedNpcs = Collections.emptySet();
	private Set<String> trackedObjects = Collections.emptySet();

	@Override
	protected void startUp()
	{
		targets = new NameTargets(client);
		sceneOverlay = new CaSceneOverlay(client, this, config, targets, outlineRenderer);
		eventBus.register(targets);
		overlayManager.add(sceneOverlay);
		overlayManager.add(itemOverlay);
		scope = savedScope();
		panel = new CaPanel(this);
		shownView = null;
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Combat Achievement Guides")
			.icon(icon)
			.priority(17)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		questCountdown = 0;
		checkShortestPath = true;
		clientThread.invokeLater(() ->
		{
			loadKillCounts();
			refresh();
		});
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
		focus = null;
		skipped.clear();
		arrived.clear();
		killCounts.clear();
		bankCounts = null;
		quests.clear();
		trackedNpcs = Collections.emptySet();
		trackedObjects = Collections.emptySet();
		panel = null;
		shownView = null;
	}

	@Provides
	CombatAchievementsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CombatAchievementsConfig.class);
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
			arrived.clear();
			refresh();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		loadKillCounts();
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
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		Matcher m = KILL_COUNT.matcher(Text.removeTags(e.getMessage()));
		if (m.find())
		{
			killCounts.merge(m.group(1).trim().toLowerCase(), Integer.parseInt(m.group(2).replace(",", "")),
				Math::max);
			configManager.setRSProfileConfiguration(CombatAchievementsConfig.GROUP, KEY_KC, killCounts.entrySet()
				.stream().map(en -> en.getKey().replace(",", "") + "=" + en.getValue())
				.collect(Collectors.joining(",")));
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if ((CombatAchievementsConfig.GROUP.equals(e.getGroup()) && !KEY_KC.equals(e.getKey())
			&& !KEY_SCOPE.equals(e.getKey())) || Wilderness.isSettingChange(e))
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
		if (--questCountdown <= 0)
		{
			questCountdown = QUEST_REFRESH_TICKS;
			readQuests();
		}
		refresh();
	}

	/**
	 * Another plugin in this jar (e.g. the Collection Log Guide) asked to show one boss's tasks.
	 */
	@Subscribe
	public void onOpenCombatAchievementsRequest(OpenCombatAchievementsRequest e)
	{
		if (e.getTier() != null)
		{
			try
			{
				CaTier tier = CaTier.valueOf(e.getTier());
				scope = tier;
				focus = null;
				picked = null;
				limit = PAGE;
				running = true;
				checkShortestPath = true;
				configManager.setConfiguration(CombatAchievementsConfig.GROUP, KEY_SCOPE, tier.name());
				refresh();
				SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
			}
			catch (IllegalArgumentException ignored)
			{
				// Not a tier: nothing to open
			}
			return;
		}
		String want = e.getMonster() == null ? "" : e.getMonster().trim();
		for (CaMonster m : CaMonsters.all())
		{
			if (m.getName().equalsIgnoreCase(want))
			{
				focus = m.getName();
				scope = null;
				limit = PAGE;
				refresh();
				SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
				return;
			}
		}
	}

	// ---- Reading the player ----

	private void loadKillCounts()
	{
		killCounts.clear();
		String saved = configManager.getRSProfileConfiguration(CombatAchievementsConfig.GROUP, KEY_KC);
		if (saved == null || saved.isEmpty())
		{
			return;
		}
		for (String pair : saved.split(","))
		{
			int eq = pair.lastIndexOf('=');
			if (eq > 0)
			{
				try
				{
					killCounts.put(pair.substring(0, eq), Integer.parseInt(pair.substring(eq + 1)));
				}
				catch (NumberFormatException ignored)
				{
					// A hand-edited value: skip it
				}
			}
		}
	}

	private void readQuests()
	{
		quests.clear();
		Set<Quest> wanted = new HashSet<>();
		for (CaTask t : CaTasks.all())
		{
			wanted.addAll(CaGuide.diaryTask(t).getQuests());
			wanted.addAll(CaGuide.diaryTask(t).getQuestsStarted());
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
		for (CaTask t : CaTasks.all())
		{
			int varp = t.varp();
			if (varp > 0 && (varps.computeIfAbsent(varp, client::getVarpValue) >>> t.bit() & 1) != 0)
			{
				done.add(t.key());
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

	private int points(DiaryPlayer p)
	{
		int fromGame = client.getVarbitValue(VarbitID.CA_POINTS);
		if (fromGame > 0)
		{
			return fromGame;
		}
		int sum = 0;
		for (CaTask t : CaTasks.all())
		{
			if (p.getDone().contains(t.key()))
			{
				sum += t.getTier().getPoints();
			}
		}
		return sum;
	}

	private Map<CaTier, Integer> thresholds()
	{
		Map<CaTier, Integer> out = new EnumMap<>(CaTier.class);
		int previous = 0;
		for (CaTier t : CaTier.values())
		{
			int v = client.getVarbitValue(t.getThresholdVarbit());
			// Only trust the game's numbers when they look like thresholds (rising, above zero)
			out.put(t, v > previous ? v : t.getFallbackThreshold());
			previous = out.get(t);
		}
		return out;
	}

	/**
	 * Tiers whose reward was claimed, from the best Ghommal's hilt owned (each tier's hilt replaces the one before).
	 * Empty before the bank was seen.
	 */
	private Set<CaTier> claimed(DiaryPlayer p)
	{
		Set<CaTier> out = java.util.EnumSet.noneOf(CaTier.class);
		if (p.getBank() == null)
		{
			return out;
		}
		CaTier best = null;
		for (CaTier t : CaTier.values())
		{
			for (int id : t.getHiltIds())
			{
				if (p.owned(id) > 0)
				{
					best = t;
				}
			}
		}
		if (best != null)
		{
			for (CaTier t : CaTier.values())
			{
				if (t.ordinal() <= best.ordinal())
				{
					out.add(t);
				}
			}
		}
		return out;
	}

	// ---- The guide ----

	private List<CaTask> scopeTasks()
	{
		List<CaTask> out = new ArrayList<>();
		for (CaTask t : CaTasks.all())
		{
			if ((scope == null || t.getTier() == scope) && (focus == null || focus.equals(t.getMonster())))
			{
				out.add(t);
			}
		}
		return out;
	}

	/**
	 * Works out the ranking, the step (if a guide runs) and the panel view, and sends them on when they changed.
	 * Client thread.
	 */
	private void refresh()
	{
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
		if (!loggedIn)
		{
			step = null;
			trackNames(null, null);
			clearWay();
			pushView(CaView.builder().loggedIn(false).scope(scope).tiers(List.of()).tasks(List.of()).build());
			return;
		}
		DiaryPlayer p = readPlayer();
		CaRanker.Power power = CaRanker.power(p);
		List<CaRanker.Ranked> ranked = CaRanker.rank(scopeTasks(), p, power, killCounts, skipped);
		int points = points(p);
		CaGuide.Context.ContextBuilder context = CaGuide.Context.builder()
			.player(p)
			.power(power)
			.kc(new HashMap<>(killCounts))
			.points(points)
			.thresholds(thresholds())
			.claimed(claimed(p));

		DiaryStep next = null;
		CaRanker.Ranked current = null;
		if (running)
		{
			if (picked != null && p.getDone().contains(picked))
			{
				picked = null;
			}
			current = CaGuide.next(ranked, skipped, picked);
			CaMonster monster = current == null ? null : current.getMonster();
			String key = current == null ? null : current.getTask().key();
			forgetIfLeft(key, monster, p.getLocation());
			context.nearMonster(monster != null && near(monster, p.getLocation()))
				.arrived(key != null && arrived.contains(key));
			CaGuide.Context c = context.build();
			next = CaGuide.resolve(ranked, c, skipped, picked);
			Location dest = next.getDestination();
			if (dest != null && !client.getTopLevelWorldView().isInstance())
			{
				Map<Integer, Integer> bank = p.getBank();
				List<TravelPick> picks = travel.plan(id -> p.getCarried().getOrDefault(id, 0),
					id -> bank == null ? 0 : bank.getOrDefault(id, 0), p.getLocation(),
					List.of(dest.getPoint()), dest.getName(), Wilderness.contains(dest) && p.isAllowWilderness());
				if (!picks.isEmpty())
				{
					next = CaGuide.resolve(ranked, context.player(p.toBuilder().travel(picks).build()).build(),
						skipped, picked);
				}
			}
			if (next.getKind() == DiaryStep.Kind.DO && next.getTaskKey() != null)
			{
				arrived.add(next.getTaskKey());
			}
			trackNames(next, monster);
			showWay(next.getDestination());
		}
		else
		{
			trackNames(null, null);
			clearWay();
		}
		step = next;
		pushView(buildView(p, power, ranked, points, context.build(), next, current));
	}

	/**
	 * Forgets that the player reached a task's entrance once they're back on the surface and well away from it.
	 */
	private void forgetIfLeft(String key, CaMonster m, WorldPoint me)
	{
		arrived.removeIf(k -> !k.equals(key));
		if (key == null || m == null || m.getAt() == null || me == null)
		{
			return;
		}
		WorldPoint at = m.getAt().getPoint();
		boolean surface = me.getY() < 4160 && at.getY() < 4160;
		if (surface && me.distanceTo2D(at) > LEFT_ENTRANCE)
		{
			arrived.remove(key);
		}
	}

	private boolean near(CaMonster m, WorldPoint me)
	{
		if (me == null)
		{
			return false;
		}
		for (Map.Entry<NPC, String> e : targets.getNpcs())
		{
			WorldPoint wp = e.getKey().getWorldLocation();
			if (m.getNpcs().contains(e.getValue()) && wp != null && wp.distanceTo2D(me) <= NEAR_BOSS)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Tracks what the step outlines, plus the task's boss so the guide notices when it's close.
	 */
	private void trackNames(DiaryStep s, CaMonster monster)
	{
		Set<String> npcs = new HashSet<>();
		Set<String> objects = new HashSet<>();
		if (s != null)
		{
			npcs.addAll(s.getNpcs());
			objects.addAll(s.getObjects());
		}
		if (monster != null)
		{
			npcs.addAll(monster.getNpcs());
		}
		if (!npcs.equals(trackedNpcs) || !objects.equals(trackedObjects))
		{
			trackedNpcs = npcs;
			trackedObjects = objects;
			targets.track(npcs, objects);
		}
	}

	private CaView buildView(DiaryPlayer p, CaRanker.Power power, List<CaRanker.Ranked> ranked, int points,
		CaGuide.Context c, DiaryStep current, CaRanker.Ranked currentTask)
	{
		List<CaView.TierRow> tiers = new ArrayList<>();
		Map<CaTier, int[]> counts = new EnumMap<>(CaTier.class);
		for (CaTask t : CaTasks.all())
		{
			int[] n = counts.computeIfAbsent(t.getTier(), k -> new int[2]);
			n[1]++;
			if (p.getDone().contains(t.key()))
			{
				n[0]++;
			}
		}
		for (CaTier t : CaTier.values())
		{
			int threshold = c.getThresholds().getOrDefault(t, t.getFallbackThreshold());
			int[] n = counts.getOrDefault(t, new int[2]);
			Boolean claimed = p.getBank() == null ? null : c.getClaimed().contains(t);
			tiers.add(new CaView.TierRow(t, threshold, n[0], n[1], points >= threshold, claimed));
		}
		String currentKey = current == null ? null : current.getTaskKey();
		List<CaView.TaskRow> rows = new ArrayList<>();
		int left = 0;
		int done = 0;
		for (CaRanker.Ranked r : ranked)
		{
			if (r.getFit() == CaRanker.Fit.DONE)
			{
				done++;
				continue;
			}
			left++;
			if (rows.size() >= limit)
			{
				continue;
			}
			CaTask t = r.getTask();
			rows.add(new CaView.TaskRow(t.key(), t.getName(), t.getTier(), t.getMonster(), t.getType().getLabel(),
				t.getText(), r.getFit(), r.getReason(), r.getMissing(), t.key().equals(currentKey),
				skipped.contains(t.key()), r.getMonster() != null && r.getMonster().isWilderness()));
		}
		boolean onTask = currentTask != null && current != null && currentTask.getTask().key().equals(currentKey);
		return CaView.builder()
			.loggedIn(true)
			.scope(scope)
			.focus(focus)
			.points(points)
			.tiers(tiers)
			.power(power.getValue())
			.powerStyle(power.getStyle().name().charAt(0) + power.getStyle().name().substring(1).toLowerCase())
			.weapon(power.getWeapon())
			.tasks(rows)
			.limit(limit)
			.left(left)
			.doneInScope(done)
			.totalInScope(ranked.size())
			.running(running)
			.step(current)
			.pvmId(onTask && currentTask.getMonster() != null ? currentTask.getMonster().getPvmId() : null)
			.wikiPage(onTask ? currentTask.getTask().wikiPage() : null)
			.wildernessAllowed(p.isAllowWilderness())
			.build();
	}

	private void pushView(CaView view)
	{
		CaPanel target = panel;
		if (target != null && !view.equals(shownView))
		{
			shownView = view;
			SwingUtilities.invokeLater(() -> target.update(view));
		}
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

	private CaTier savedScope()
	{
		String s = configManager.getConfiguration(CombatAchievementsConfig.GROUP, KEY_SCOPE);
		if (s == null || ALL.equals(s))
		{
			return s == null ? CaTier.EASY : null;
		}
		try
		{
			return CaTier.valueOf(s);
		}
		catch (IllegalArgumentException e)
		{
			return CaTier.EASY;
		}
	}

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onScope(CaTier tier)
	{
		configManager.setConfiguration(CombatAchievementsConfig.GROUP, KEY_SCOPE, tier == null ? ALL : tier.name());
		clientThread.invokeLater(() ->
		{
			if (tier != scope)
			{
				picked = null;
			}
			scope = tier;
			focus = null;
			limit = PAGE;
			refresh();
		});
	}

	@Override
	public void onClearFocus()
	{
		clientThread.invokeLater(() ->
		{
			focus = null;
			refresh();
		});
	}

	@Override
	public void onShowMore()
	{
		clientThread.invokeLater(() ->
		{
			limit += PAGE;
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
	public void onOpenWiki(String page)
	{
		LinkBrowser.browse(WIKI_URL + (page == null ? "Combat_Achievements" : page));
	}

	@Override
	public void onBossGuide(String pvmId)
	{
		// PvM Progression picks this up when it's turned on
		clientThread.invokeLater(() -> eventBus.post(new StartPvmGuideRequest(pvmId)));
	}
}
