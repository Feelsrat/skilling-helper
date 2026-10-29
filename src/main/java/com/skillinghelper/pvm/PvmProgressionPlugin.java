package com.skillinghelper.pvm;

import com.skillinghelper.TrainSkillRequest;
import com.skillinghelper.pvm.data.ItemNeed;
import com.skillinghelper.pvm.data.PvmContent;
import com.skillinghelper.pvm.data.PvmLadder;
import com.skillinghelper.pvm.data.QuestReq;
import com.skillinghelper.pvm.ui.PvmProgressionPanel;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * Recommends the next boss or raid to work towards from your stats, quests and kill counts, with a step-by-step
 * checklist of what it needs. It only shows information outside of fights: no in-fight helpers of any kind.
 */
@Slf4j
@PluginDescriptor(
	name = "PvM Progression",
	description = "Recommends which boss or raid to work towards next from your stats, with a checklist of what it needs",
	tags = {"pvm", "boss", "bossing", "raids", "progression", "guide", "recommendation"}
)
public class PvmProgressionPlugin extends Plugin implements PvmProgressionPanel.Listener
{
	static final String GROUP = "pvm-progression";
	private static final String KEY_KILL_COUNTS = "killCounts";
	private static final String KEY_MARKED_DONE = "markedDone";
	private static final String KEY_BANK = "bankOwned";
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";
	private static final int QUEST_REFRESH_TICKS = 50;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ConfigManager configManager;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private EventBus eventBus;
	@Inject
	private ItemManager itemManager;

	private PvmProgressionPanel panel;
	private NavigationButton navButton;

	/**
	 * Every item id the ladder checks for, so only those are remembered from the bank.
	 */
	private final Set<Integer> trackedItems = new HashSet<>();
	private final Map<String, Integer> killCounts = new HashMap<>();
	private final Set<String> markedDone = new HashSet<>();
	private final Set<Integer> bankOwned = new HashSet<>();
	private final Set<Integer> carriedOwned = new HashSet<>();
	private final Map<Quest, QuestState> questStates = new HashMap<>();
	private boolean profileLoaded;
	private boolean dirty;
	private int questRefreshCountdown;
	private PvmContent pathShown;

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

		panel = new PvmProgressionPanel(this);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("PvM Progression")
			.icon(icon)
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
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
		clientThread.invoke(this::clearPath);
		panel = null;
		profileLoaded = false;
		killCounts.clear();
		markedDone.clear();
		bankOwned.clear();
		carriedOwned.clear();
		questStates.clear();
		trackedItems.clear();
	}

	// ---- Game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN && !profileLoaded)
		{
			loadProfile();
			// Quest states aren't ready on the login tick, so read them on the next game tick
			questRefreshCountdown = 0;
			dirty = true;
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
		if (--questRefreshCountdown <= 0)
		{
			refreshQuests();
			dirty = true;
		}
		if (dirty)
		{
			refresh();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		dirty = true;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		int id = e.getContainerId();
		if (id == InventoryID.BANK)
		{
			bankOwned.clear();
			bankOwned.addAll(trackedIn(e.getItemContainer()));
			saveSet(KEY_BANK, bankOwned);
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
		KillCountParser.Kill kill = KillCountParser.parse(e.getMessage(), PvmLadder.ALL);
		if (kill == null)
		{
			return;
		}
		String id = kill.getContent().getId();
		// God Wars has a count per general: any of them counts
		killCounts.merge(id, kill.getCount(), Math::max);
		configManager.setRSProfileConfiguration(GROUP, KEY_KILL_COUNTS, killCounts.entrySet().stream()
			.map(en -> en.getKey() + ":" + en.getValue())
			.collect(Collectors.joining(",")));
		dirty = true;
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
			Map<String, Object> data = new HashMap<>();
			data.put("target", content.getLocation());
			if (content.isWilderness())
			{
				Map<String, Object> overrides = new HashMap<>();
				overrides.put("avoidWilderness", false);
				data.put("config", overrides);
			}
			eventBus.post(new PluginMessage("shortestpath", "path", data));
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

	// ---- State ----

	private void clearPath()
	{
		if (pathShown != null)
		{
			eventBus.post(new PluginMessage("shortestpath", "clear", new HashMap<>()));
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
		String pathId = pathShown == null ? null : pathShown.getId();
		PvmProgressionPanel p = panel;
		SwingUtilities.invokeLater(() -> p.update(ladder, player, pathId));
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
		profileLoaded = true;
	}

	private void saveSet(String key, Set<Integer> ids)
	{
		configManager.setRSProfileConfiguration(GROUP, key, ids.stream().map(String::valueOf)
			.collect(Collectors.joining(",")));
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
}
