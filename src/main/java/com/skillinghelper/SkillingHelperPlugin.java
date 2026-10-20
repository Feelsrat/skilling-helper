package com.skillinghelper;

import com.google.inject.Provides;
import com.skillinghelper.bank.BankCache;
import com.skillinghelper.bank.BankFilter;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemCounts;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.MethodPlanner;
import com.skillinghelper.engine.PlanSegment;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.PriceLookup;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.engine.StepResolver;
import com.skillinghelper.engine.StepType;
import com.skillinghelper.overlay.ItemHighlightOverlay;
import com.skillinghelper.overlay.MinimapArrowOverlay;
import com.skillinghelper.overlay.SceneHighlightOverlay;
import com.skillinghelper.ui.SkillingHelperPanel;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
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
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

@Slf4j
@PluginDescriptor(
	name = "Skilling Helper",
	description = "Quest Helper-style step-by-step skilling guides with pathing, highlights and a bank filter",
	tags = {"skilling", "guide", "training", "helper", "woodcutting", "mining", "fishing", "cooking"}
)
public class SkillingHelperPlugin extends Plugin implements SkillingHelperPanel.Listener
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final String KEY_ACTIVE_SKILL = "activeSkill";
	private static final String KEY_TARGET = "target.";
	private static final String KEY_METHOD = "method.";
	private static final String KEY_PREFERENCE = "preference";
	private static final int QUEST_REFRESH_TICKS = 50;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private SkillingHelperConfig config;
	@Inject
	private ConfigManager configManager;
	@Inject
	private EventBus eventBus;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ItemManager itemManager;
	@Inject
	private SkillIconManager skillIconManager;
	@Inject
	private WorldMapPointManager worldMapPointManager;
	@Inject
	private GuideRepository guides;
	@Inject
	private TargetTracker tracker;
	@Inject
	private BankCache bankCache;
	@Inject
	private BankFilter bankFilter;
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

	private Skill activeSkill;
	private final Map<Skill, Integer> targets = new EnumMap<>(Skill.class);
	private final Map<Skill, String> forcedMethods = new EnumMap<>(Skill.class);
	private TrainingPreference preference;
	private final Set<Quest> completedQuests = EnumSet.noneOf(Quest.class);
	private int questRefreshCountdown;
	private boolean profileLoaded;

	private WorldPoint pathDestination;
	private boolean hintArrowSet;
	private WorldMapPoint mapPoint;

	@Override
	protected void startUp()
	{
		PriceLookup prices = id -> (int) Math.min(Integer.MAX_VALUE, itemManager.getItemPrice(id));
		planner = new MethodPlanner(prices);
		resolver = new StepResolver(prices);
		tracker.setInterestingTargets(guides.all());
		preference = config.preference();

		eventBus.register(tracker);
		eventBus.register(bankFilter);
		overlayManager.add(sceneOverlay);
		overlayManager.add(minimapOverlay);
		overlayManager.add(itemOverlay);

		panel = new SkillingHelperPanel(this, skillIconManager);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Skilling Helper")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

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

	@Override
	protected void shutDown()
	{
		eventBus.unregister(tracker);
		eventBus.unregister(bankFilter);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(minimapOverlay);
		overlayManager.remove(itemOverlay);
		clientToolbar.removeNavigation(navButton);
		bankFilter.shutDown();
		clientThread.invoke(() ->
		{
			bankCache.save();
			clearPath();
			tracker.clear();
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

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			profileLoaded = false;
			clearPath();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		profileLoaded = false;
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
		refresh();
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		if (activeSkill == null && config.autoSelectSkill() && profileLoaded && guides.hasGuide(e.getSkill())
			&& e.getXp() > 0)
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
		if (KEY_PREFERENCE.equals(e.getKey()))
		{
			preference = config.preference();
		}
		clientThread.invokeLater(() ->
		{
			clearPath();
			refresh();
		});
	}

	// ---- Panel callbacks (Swing thread) ----

	@Override
	public void onSkillSelected(Skill skill)
	{
		clientThread.invokeLater(() -> selectSkill(skill));
	}

	@Override
	public void onTargetChanged(int target)
	{
		clientThread.invokeLater(() ->
		{
			if (activeSkill != null)
			{
				setTarget(activeSkill, target);
				refresh();
			}
		});
	}

	@Override
	public void onTargetReset()
	{
		clientThread.invokeLater(() ->
		{
			if (activeSkill != null)
			{
				setTarget(activeSkill, defaultTarget(activeSkill));
				refresh();
			}
		});
	}

	@Override
	public void onPreferenceChanged(TrainingPreference pref)
	{
		clientThread.invokeLater(() ->
		{
			preference = pref;
			configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_PREFERENCE, pref.name());
			refresh();
		});
	}

	@Override
	public void onMethodChosen(String methodId)
	{
		clientThread.invokeLater(() ->
		{
			if (activeSkill == null)
			{
				return;
			}
			if (methodId == null)
			{
				forcedMethods.remove(activeSkill);
				configManager.unsetRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_METHOD + activeSkill.name());
			}
			else
			{
				forcedMethods.put(activeSkill, methodId);
				configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_METHOD + activeSkill.name(), methodId);
			}
			refresh();
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
		targets.clear();
		forcedMethods.clear();
		for (Skill s : Skill.values())
		{
			String t = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_TARGET + s.name());
			if (t != null)
			{
				try
				{
					targets.put(s, Integer.parseInt(t));
				}
				catch (NumberFormatException ignored)
				{
					// fall back to the default goal
				}
			}
			String m = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_METHOD + s.name());
			if (m != null && guides.byId(m) != null)
			{
				forcedMethods.put(s, m);
			}
		}
		String pref = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_PREFERENCE);
		preference = config.preference();
		if (pref != null)
		{
			try
			{
				preference = TrainingPreference.valueOf(pref);
			}
			catch (IllegalArgumentException ignored)
			{
				// keep the config default
			}
		}
		String active = configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_ACTIVE_SKILL);
		activeSkill = null;
		if (active != null)
		{
			try
			{
				activeSkill = Skill.valueOf(active);
			}
			catch (IllegalArgumentException ignored)
			{
				// unknown skill name
			}
		}
		refreshQuests();
		profileLoaded = true;
	}

	private void selectSkill(Skill skill)
	{
		activeSkill = skill;
		if (skill == null)
		{
			configManager.unsetRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_ACTIVE_SKILL);
		}
		else
		{
			configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_ACTIVE_SKILL, skill.name());
			Integer target = targets.get(skill);
			if (target == null || target <= client.getRealSkillLevel(skill))
			{
				setTarget(skill, defaultTarget(skill));
			}
		}
		refreshQuests();
		refresh();
	}

	private int defaultTarget(Skill skill)
	{
		return Math.min(Experience.MAX_REAL_LEVEL, client.getRealSkillLevel(skill) + config.defaultTargetOffset());
	}

	private void setTarget(Skill skill, int target)
	{
		int clamped = Math.max(2, Math.min(Experience.MAX_REAL_LEVEL, target));
		targets.put(skill, clamped);
		configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_TARGET + skill.name(), clamped);
	}

	private void refreshQuests()
	{
		questRefreshCountdown = QUEST_REFRESH_TICKS;
		if (client.getGameState() != GameState.LOGGED_IN || activeSkill == null)
		{
			return;
		}
		completedQuests.clear();
		for (Method m : guides.methods(activeSkill))
		{
			for (Quest q : m.getQuests())
			{
				if (q.getState(client) == QuestState.FINISHED)
				{
					completedQuests.add(q);
				}
			}
		}
	}

	private PlayerState snapshot()
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			levels.put(s, client.getRealSkillLevel(s));
			xp.put(s, client.getSkillExperience(s));
		}
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		int used = 0;
		if (inv != null)
		{
			for (Item item : inv.getItems())
			{
				if (item.getId() > 0)
				{
					used++;
				}
			}
		}
		Player me = client.getLocalPlayer();
		return PlayerState.builder()
			.levels(levels)
			.xp(xp)
			.inventory(counts(inv))
			.equipment(counts(worn))
			.bank(bankCache.get())
			.freeInventorySlots(28 - used)
			.location(me == null ? null : me.getWorldLocation())
			.membersWorld(client.getWorldType().contains(WorldType.MEMBERS))
			.completedQuests(EnumSet.copyOf(completedQuestsOrEmpty()))
			.build();
	}

	private Set<Quest> completedQuestsOrEmpty()
	{
		return completedQuests.isEmpty() ? EnumSet.noneOf(Quest.class) : completedQuests;
	}

	private static ItemCounts counts(ItemContainer container)
	{
		if (container == null)
		{
			return ItemCounts.EMPTY;
		}
		Map<Integer, Integer> m = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				m.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		return new ItemCounts(m);
	}

	/**
	 * Rebuilds the guide from the current game state. Runs on the client thread.
	 */
	private void refresh()
	{
		if (client.getGameState() != GameState.LOGGED_IN || !profileLoaded || panel == null)
		{
			return;
		}

		ActiveGuide next;
		if (activeSkill == null)
		{
			next = ActiveGuide.NONE;
		}
		else
		{
			next = buildGuide(activeSkill);
		}
		guide = next;

		GuideStep step = next.getStep();
		boolean showPath = step != null && step.getType() != StepType.TRAIN && step.getType() != StepType.DONE;
		setPath(showPath ? step.getDestination() : null, step == null ? null : step.getDestinationName());

		final ActiveGuide forPanel = next;
		final TrainingPreference pref = preference;
		final boolean filterOn = bankFilter.isActive();
		final SkillingHelperPanel p = panel;
		SwingUtilities.invokeLater(() -> p.update(forPanel, pref, filterOn));
	}

	private ActiveGuide buildGuide(Skill skill)
	{
		PlayerState state = snapshot();
		int level = state.level(skill);
		int target = targets.getOrDefault(skill, Math.min(Experience.MAX_REAL_LEVEL, level + config.defaultTargetOffset()));
		long xpRemaining = MethodPlanner.xpBetween(state.xp(skill), target);
		List<Method> methods = guides.methods(skill);

		if (methods.isEmpty())
		{
			return ActiveGuide.builder()
				.skill(skill)
				.level(level)
				.targetLevel(target)
				.xpRemaining(xpRemaining)
				.player(state)
				.hasGuide(false)
				.wikiPage(GuideRepository.wikiTrainingPage(skill, state.isMembersWorld()))
				.build();
		}

		String forcedId = forcedMethods.get(skill);
		Method forced = forcedId == null ? null : guides.byId(forcedId);
		List<MethodOption> options = planner.rank(methods, state, preference, target);
		List<PlanSegment> plan = planner.plan(methods, state, preference, target, forced);

		MethodOption current = null;
		if (!plan.isEmpty())
		{
			current = plan.get(0).getOption();
		}
		else if (!options.isEmpty() && options.get(0).isAvailable())
		{
			current = options.get(0);
		}

		ActiveGuide.ActiveGuideBuilder b = ActiveGuide.builder()
			.skill(skill)
			.level(level)
			.targetLevel(target)
			.xpRemaining(xpRemaining)
			.options(options)
			.plan(plan)
			.player(state)
			.hasGuide(true)
			.wikiPage(GuideRepository.wikiTrainingPage(skill, state.isMembersWorld()));

		if (current == null)
		{
			GuideStep step = level >= target
				? GuideStep.empty("Goal reached: " + skill.getName() + " " + target + "!",
				"Set a new target level to keep going.")
				: GuideStep.empty("No method available yet",
				"None of the built-in methods fit your account right now. Check the method list for what's missing.");
			return b.step(step).build();
		}

		Method method = current.getMethod();
		boolean forcedActive = forced != null && forced == method;
		List<Requirement> reqs = resolver.requirements(method, state, current.getActions());
		GuideStep step = resolver.resolve(method, state, target, dropProduce(method), reqs);
		return b.method(method)
			.forced(forcedActive)
			.option(current)
			.requirements(reqs)
			.step(step)
			.bankItems(StepResolver.bankItems(reqs, step))
			.build();
	}

	private boolean dropProduce(Method m)
	{
		switch (config.inventoryFullAction())
		{
			case ALWAYS_BANK:
				return m.getBank() == null;
			case ALWAYS_DROP:
				return true;
			case METHOD_DEFAULT:
			default:
				return m.isDropByDefault() || m.getBank() == null;
		}
	}

	// ---- Pathing ----

	private void clearPath()
	{
		WorldPoint previous = pathDestination;
		setPath(null, null);
		if (previous == null && hintArrowSet)
		{
			client.clearHintArrow();
			hintArrowSet = false;
		}
	}

	private void setPath(WorldPoint destination, String name)
	{
		if (Objects.equals(destination, pathDestination))
		{
			return;
		}
		pathDestination = destination;

		if (hintArrowSet)
		{
			client.clearHintArrow();
			hintArrowSet = false;
		}
		if (destination != null && config.hintArrow())
		{
			client.setHintArrow(destination);
			hintArrowSet = true;
		}

		if (mapPoint != null)
		{
			worldMapPointManager.remove(mapPoint);
			mapPoint = null;
		}
		if (destination != null && config.worldMapPoint())
		{
			mapPoint = WorldMapPoint.builder()
				.worldPoint(destination)
				.image(MapArrow.image())
				.name(name)
				.tooltip(name == null ? "Skilling Helper" : "Skilling Helper: " + name)
				.snapToEdge(true)
				.jumpOnClick(true)
				.build();
			worldMapPointManager.add(mapPoint);
		}

		if (config.shortestPath())
		{
			if (destination != null)
			{
				Map<String, Object> data = new HashMap<>();
				data.put("target", destination);
				eventBus.post(new PluginMessage("shortestpath", "path", data));
			}
			else
			{
				eventBus.post(new PluginMessage("shortestpath", "clear"));
			}
		}
	}
}
