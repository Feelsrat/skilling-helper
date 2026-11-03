package com.skillinghelper.tog;

import com.skillinghelper.SkillingHelperConfig;
import java.awt.Color;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

/**
 * Follows the weekly Tears of Guthix minigame for the logged-in account: remembers when the player last went in (and
 * what Juna said about coming back), shows a ready / not-ready infobox, and tracks the tear streams in the cave.
 * Everything runs on the client thread.
 */
@Singleton
public class TogTracker
{
	private static final String KEY = "tog.record";
	private static final int QUEST_REFRESH_TICKS = 50;
	/**
	 * A second sighting of the minigame this soon after the first is the same visit.
	 */
	private static final long SAME_VISIT_SECONDS = 60 * 60;

	@Inject
	private Client client;
	@Inject
	private ConfigManager configManager;
	@Inject
	private SkillingHelperConfig config;
	@Inject
	private InfoBoxManager infoBoxManager;
	@Inject
	private ItemManager itemManager;
	@Inject
	private Notifier notifier;

	private TearsOfGuthix.Record record = TearsOfGuthix.Record.EMPTY;
	private boolean loaded;
	private int questCountdown;
	private boolean questDone;
	@Getter
	private boolean lostTribeDone;
	private final Set<Quest> rewardQuests = EnumSet.noneOf(Quest.class);
	@Getter
	private TearsOfGuthix.Status status = TearsOfGuthix.Status.LOCKED_STATUS;
	@Getter
	private boolean inChasm;
	@Getter
	private boolean playing;
	@Getter
	private int tears;
	@Getter
	private String reward;
	private String lastDialogue;
	private Boolean wasReady;
	private StatusBox box;

	/**
	 * Tear streams in view and the tick each appeared on.
	 */
	private final Map<DecorativeObject, Integer> streams = new HashMap<>();

	public void load()
	{
		record = TearsOfGuthix.Record.parse(configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY));
		loaded = true;
		questCountdown = 0;
		wasReady = null;
		lastDialogue = null;
	}

	public void reset()
	{
		loaded = false;
		record = TearsOfGuthix.Record.EMPTY;
		status = TearsOfGuthix.Status.LOCKED_STATUS;
		playing = false;
		inChasm = false;
		streams.clear();
		removeBox();
	}

	public void shutDown()
	{
		reset();
	}

	/**
	 * Call every game tick while logged in.
	 */
	public void tick(Plugin plugin)
	{
		if (!loaded)
		{
			return;
		}
		if (--questCountdown <= 0)
		{
			refreshQuests();
		}
		long now = Instant.now().getEpochSecond();
		long totalXp = client.getOverallExperience();
		int qp = client.getVarpValue(VarPlayerID.QP);

		Player me = client.getLocalPlayer();
		inChasm = me != null && me.getWorldLocation() != null
			&& me.getWorldLocation().getRegionID() == TearsOfGuthix.REGION;
		boolean nowPlaying = inChasm && (client.getVarbitValue(VarbitID.TOG_MINIGAME_COLLECTING) != 0
			|| holds(InventoryID.WORN) || holds(InventoryID.INV));
		if (nowPlaying && !playing && now - record.getVisitedAt() > SAME_VISIT_SECONDS)
		{
			save(TearsOfGuthix.visited(now, totalXp, qp));
		}
		playing = nowPlaying;
		tears = playing ? client.getVarbitValue(VarbitID.TOG_TEARS_COLLECTED) : 0;
		if (inChasm && !playing)
		{
			readJuna(now, totalXp, qp);
		}

		status = TearsOfGuthix.status(questDone, record, now, totalXp, qp);
		notifyWhenReady();
		updateBox(plugin, now);
	}

	private boolean holds(int container)
	{
		ItemContainer c = client.getItemContainer(container);
		return c != null && c.contains(ItemID.TOG_BOWL);
	}

	/**
	 * Quest states are scripts, so read them now and then rather than every tick.
	 */
	private void refreshQuests()
	{
		questCountdown = QUEST_REFRESH_TICKS;
		questDone = Quest.TEARS_OF_GUTHIX.getState(client) == QuestState.FINISHED;
		lostTribeDone = Quest.THE_LOST_TRIBE.getState(client) == QuestState.FINISHED;
		rewardQuests.clear();
		for (Quest q : new Quest[]{Quest.RUNE_MYSTERIES, Quest.DRUIDIC_RITUAL, Quest.PANDEMONIUM})
		{
			if (q.getState(client) == QuestState.FINISHED)
			{
				rewardQuests.add(q);
			}
		}
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		for (Skill s : Skill.values())
		{
			xp.put(s, client.getSkillExperience(s));
		}
		Skill skill = TearsOfGuthix.rewardSkill(xp, rewardQuests);
		reward = skill == null ? null : TearsOfGuthix.rewardText(skill, xp.get(skill),
			client.getVarpValue(VarPlayerID.QP), client.getVarbitValue(VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE) == 1);
	}

	/**
	 * Juna says when the player can come back; remember it so the infobox is right even for visits we didn't see.
	 */
	private void readJuna(long now, long totalXp, int qp)
	{
		String text = null;
		Widget name = client.getWidget(InterfaceID.ChatLeft.NAME);
		if (name != null && !name.isHidden() && "Juna".equals(name.getText()))
		{
			Widget w = client.getWidget(InterfaceID.ChatLeft.TEXT);
			text = w == null ? null : w.getText();
		}
		if (text == null)
		{
			Widget w = client.getWidget(InterfaceID.Messagebox.TEXT);
			text = w == null || w.isHidden() ? null : w.getText();
		}
		if (text == null || text.equals(lastDialogue))
		{
			return;
		}
		lastDialogue = text;
		TearsOfGuthix.Record heard = TearsOfGuthix.heard(record, text, now, totalXp, qp);
		if (heard != record)
		{
			save(heard);
		}
	}

	private void save(TearsOfGuthix.Record r)
	{
		record = r;
		configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY, r.serialize());
	}

	private void notifyWhenReady()
	{
		boolean ready = status.isReady();
		// Only on the change while logged in, not for a week that was already up at login
		if (Boolean.FALSE.equals(wasReady) && ready)
		{
			notifier.notify(config.togNotification(), "Tears of Guthix is up: Juna will let you in again.");
		}
		if (status.getState() != TearsOfGuthix.State.UNKNOWN)
		{
			wasReady = ready;
		}
	}

	// ---- Infobox ----

	private void updateBox(Plugin plugin, long now)
	{
		boolean show = config.togInfobox() && status.getState() != TearsOfGuthix.State.LOCKED;
		if (!show)
		{
			removeBox();
			return;
		}
		if (box == null)
		{
			box = new StatusBox(plugin);
			infoBoxManager.addInfoBox(box);
		}
		box.status = status;
		box.now = now;
	}

	private void removeBox()
	{
		if (box != null)
		{
			infoBoxManager.removeInfoBox(box);
			box = null;
		}
	}

	private final class StatusBox extends InfoBox
	{
		private TearsOfGuthix.Status status = TearsOfGuthix.Status.LOCKED_STATUS;
		private long now;

		StatusBox(Plugin plugin)
		{
			super(itemManager.getImage(ItemID.TOG_BOWL), plugin);
		}

		@Override
		public String getText()
		{
			switch (status.getState())
			{
				case READY:
					return "Ready";
				case UNKNOWN:
					return "?";
				default:
					if (status.getReadyAt() > now)
					{
						long left = status.getReadyAt() - now;
						return left >= 24 * 60 * 60 ? left / (24 * 60 * 60) + "d"
							: TearsOfGuthix.timeLeft(left).replaceFirst(" \\d+m$", "");
					}
					return status.getXpToGo() >= 1000 ? status.getXpToGo() / 1000 + "k xp" : "xp";
			}
		}

		@Override
		public Color getTextColor()
		{
			switch (status.getState())
			{
				case READY:
					return Color.GREEN;
				case UNKNOWN:
					return Color.YELLOW;
				default:
					return status.getReadyAt() > now ? Color.WHITE : Color.ORANGE;
			}
		}

		@Override
		public String getTooltip()
		{
			return "Tears of Guthix: " + TearsOfGuthix.describe(status, now);
		}
	}

	// ---- Tear streams ----

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned e)
	{
		int id = e.getDecorativeObject().getId();
		if (isBlue(id) || isGreen(id))
		{
			streams.put(e.getDecorativeObject(), client.getTickCount());
		}
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned e)
	{
		streams.remove(e.getDecorativeObject());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOADING || e.getGameState() == GameState.LOGIN_SCREEN
			|| e.getGameState() == GameState.HOPPING)
		{
			streams.clear();
		}
	}

	public Map<DecorativeObject, Integer> getStreams()
	{
		return Collections.unmodifiableMap(streams);
	}

	public static boolean isBlue(int id)
	{
		return id == ObjectID.TOG_WEEPING_WALL_GOOD_R || id == ObjectID.TOG_WEEPING_WALL_GOOD_L;
	}

	public static boolean isGreen(int id)
	{
		return id == ObjectID.TOG_WEEPING_WALL_BAD_R || id == ObjectID.TOG_WEEPING_WALL_BAD_L;
	}
}
