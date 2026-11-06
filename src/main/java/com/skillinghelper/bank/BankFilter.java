package com.skillinghelper.bank;

import com.skillinghelper.SkillingHelperConfig;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.bank.BankSearch;

/**
 * The one button at the top of the bank, shared by every plugin in this jar. Like Quest Helper's, it shows only
 * the items the running guide needs: whichever plugin's guide was started last (Skilling Helper, PvM, Money
 * Making, a diary, a clue...). It answers the bank's own search filter script, so nothing is clicked or moved for
 * the player. Once switched on it stays on, across bank visits and restarts, until the player switches it off.
 *
 * <p>Only one instance exists for all plugins: it only needs client-wide services, so Guice makes it in the client's
 * injector rather than each plugin's. Each plugin {@link #attach}es on start up, tells it what its guide needs with
 * {@link #show}, and {@link #detach}es on shut down. The on/off state and the "Bank filter button" setting live in
 * Skilling Helper's config group.
 */
@Singleton
public class BankFilter
{
	public static final String KEY_ON = "bankFilterOn";
	public static final String KEY_SHOW_BUTTON = "showBankButton";
	private static final int BUTTON_SIZE = 25;

	/**
	 * What one plugin's running guide wants from the bank, and how the button looks while it's the one shown.
	 */
	@EqualsAndHashCode
	public static final class Need
	{
		@Getter
		private final String label;
		private final int spriteId;
		private final int itemId;
		/**
		 * Canonical item ids to show, or null to leave the bank alone (another plugin, like Quest Helper, filters it).
		 */
		private final Set<Integer> items;

		private Need(String label, int spriteId, int itemId, Set<Integer> items)
		{
			this.label = label;
			this.spriteId = spriteId;
			this.itemId = itemId;
			this.items = items == null ? null : Set.copyOf(items);
		}

		/**
		 * A guide shown on the button by a sprite, such as a skill icon.
		 */
		public static Need sprite(String label, int spriteId, Set<Integer> items)
		{
			return new Need(label, spriteId, -1, items);
		}

		/**
		 * A guide shown on the button by an item's picture.
		 */
		public static Need item(String label, int itemId, Set<Integer> items)
		{
			return new Need(label, -1, itemId, items);
		}

		/**
		 * A guide whose items another plugin filters (Quest Helper's own bank button): ours stays out of the way.
		 */
		public static Need leaveAlone(String label, int itemId)
		{
			return new Need(label, -1, itemId, null);
		}

		/**
		 * The button's skill icon for a skill guide, or a generic one.
		 */
		public static Need skill(String label, Skill skill, Set<Integer> items)
		{
			return sprite(label, skillSprite(skill), items);
		}
	}

	private final Client client;
	private final ClientThread clientThread;
	private final BankSearch bankSearch;
	private final ItemManager itemManager;
	private final ConfigManager configManager;
	private final EventBus eventBus;

	/**
	 * The plugins that are on and using the button.
	 */
	private final Set<Object> users = new HashSet<>();
	/**
	 * Each plugin's running guide, and when each started; guarded by this.
	 */
	private final Map<Object, Need> needs = new HashMap<>();
	private final Map<Object, Long> started = new HashMap<>();
	private long startCount;

	/**
	 * The guide the filter follows: the one started last. Read on the client thread for every bank item.
	 */
	private volatile Need current;
	@Getter
	private volatile boolean active;
	private Widget button;
	private long lastTap;

	@Inject
	BankFilter(Client client, ClientThread clientThread, BankSearch bankSearch, ItemManager itemManager,
		ConfigManager configManager, EventBus eventBus)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.bankSearch = bankSearch;
		this.itemManager = itemManager;
		this.configManager = configManager;
		this.eventBus = eventBus;
	}

	/**
	 * A plugin starting up. The first one adds the button to an open bank and picks the saved on/off back up.
	 */
	public void attach(Object owner)
	{
		boolean first;
		synchronized (this)
		{
			first = users.isEmpty();
			if (!users.add(owner))
			{
				return;
			}
		}
		if (first)
		{
			active = Boolean.TRUE.equals(configManager.getConfiguration(SkillingHelperConfig.GROUP, KEY_ON,
				Boolean.class));
			eventBus.register(this);
			clientThread.invokeLater(() ->
			{
				addButton();
				relayout();
			});
		}
	}

	/**
	 * A plugin shutting down: its guide stops counting, and the last one out takes the button away and shows the
	 * whole bank again (the on/off setting is kept for next time).
	 */
	public void detach(Object owner)
	{
		boolean last;
		synchronized (this)
		{
			if (!users.remove(owner))
			{
				return;
			}
			needs.remove(owner);
			started.remove(owner);
			current = pickCurrent();
			last = users.isEmpty();
		}
		if (last)
		{
			eventBus.unregister(this);
			clientThread.invoke(() ->
			{
				current = null;
				relayout();
				removeButton();
			});
		}
		else
		{
			clientThread.invoke(this::refresh);
		}
	}

	/**
	 * What the plugin's guide needs from the bank now, or null when it has no guide running. Cheap to call every
	 * tick: nothing happens unless it changed.
	 */
	public void show(Object owner, Need need)
	{
		Need before;
		Need after;
		synchronized (this)
		{
			if (!users.contains(owner) || Objects.equals(needs.get(owner), need))
			{
				return;
			}
			if (need == null)
			{
				needs.remove(owner);
				started.remove(owner);
			}
			else
			{
				if (!needs.containsKey(owner))
				{
					started.put(owner, ++startCount);
				}
				needs.put(owner, need);
			}
			before = current;
			after = pickCurrent();
			current = after;
		}
		if (!Objects.equals(before, after))
		{
			clientThread.invoke(this::refresh);
		}
	}

	private Need pickCurrent()
	{
		Object best = null;
		long bestAt = -1;
		for (Map.Entry<Object, Long> e : started.entrySet())
		{
			if (e.getValue() > bestAt)
			{
				best = e.getKey();
				bestAt = e.getValue();
			}
		}
		return best == null ? null : needs.get(best);
	}

	public void toggle()
	{
		clientThread.invoke(() -> setActive(!active));
	}

	public void setActive(boolean on)
	{
		active = on;
		configManager.setConfiguration(SkillingHelperConfig.GROUP, KEY_ON, on);
		refresh();
		// Once more after this tick's scripts, in case the first layout ran too early to show
		clientThread.invokeLater(this::relayout);
	}

	/**
	 * Re-draws the button and re-filters an open bank. Client thread.
	 */
	private void refresh()
	{
		updateButton();
		relayout();
	}

	private void relayout()
	{
		if (client.getItemContainer(InventoryID.BANK) != null && client.getWidget(InterfaceID.Bankmain.ITEMS) != null)
		{
			bankSearch.layoutBank();
		}
	}

	/**
	 * A tap on the bank button. Runs after the click is done rather than inside it, and ignores a second op for
	 * the same tap, in case a touch client delivers one tap twice (that would switch the filter on and off again).
	 */
	private void buttonTapped()
	{
		long now = System.currentTimeMillis();
		if (now - lastTap < 400)
		{
			return;
		}
		lastTap = now;
		setActive(!active);
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired e)
	{
		if (e.getScriptId() == ScriptID.BANKMAIN_INIT)
		{
			addButton();
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed e)
	{
		if (e.getGroupId() == InterfaceID.BANKMAIN)
		{
			button = null;
		}
	}

	// Runs after the bank tags plugin so our answer to the filter wins while the button is on
	@Subscribe(priority = -1)
	public void onScriptCallbackEvent(ScriptCallbackEvent e)
	{
		Need need = current;
		// No guide running means no items to show, so show the whole bank; an empty list too, since an empty bank
		// looks broken rather than "nothing needed"
		if (!active || need == null || need.items == null || need.items.isEmpty())
		{
			return;
		}
		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		switch (e.getEventName())
		{
			case "getSearchingTagTab":
			case "bankBuildTab":
				// One tab view, so tab separators don't leave gaps
				intStack[intStackSize - 1] = 1;
				break;
			case "bankSearchFilter":
			{
				int itemId = intStack[intStackSize - 1];
				boolean show = itemId > -1 && need.items.contains(itemManager.canonicalize(itemId));
				intStack[intStackSize - 2] = show ? 1 : 0;
				break;
			}
			default:
				break;
		}
	}

	private void addButton()
	{
		if (button != null || Boolean.FALSE.equals(configManager.getConfiguration(SkillingHelperConfig.GROUP,
			KEY_SHOW_BUTTON, Boolean.class)))
		{
			return;
		}
		Widget tut = client.getWidget(InterfaceID.Bankmain.BANK_TUT);
		if (tut == null || tut.getParent() == null)
		{
			return;
		}
		Widget parent = tut.getParent();
		button = parent.createChild(-1, WidgetType.GRAPHIC);
		button.setOriginalX(tut.getOriginalX() + tut.getOriginalWidth() + 4);
		button.setOriginalY(tut.getOriginalY());
		button.setOriginalWidth(BUTTON_SIZE);
		button.setOriginalHeight(BUTTON_SIZE);
		button.setXPositionMode(tut.getXPositionMode());
		button.setYPositionMode(tut.getYPositionMode());
		button.setHasListener(true);
		button.setOnOpListener((JavaScriptCallback) ev -> clientThread.invokeLater(this::buttonTapped));
		updateButton();
		button.revalidate();
	}

	private void updateButton()
	{
		if (button == null)
		{
			return;
		}
		Need need = current;
		if (need != null && need.itemId > 0)
		{
			button.setSpriteId(-1);
			button.setItemId(need.itemId);
			button.setItemQuantity(1);
			button.setItemQuantityMode(ItemQuantityMode.NEVER);
		}
		else
		{
			button.setItemId(-1);
			button.setSpriteId(need != null && need.spriteId > 0 ? need.spriteId : SpriteID.Staticons.WOODCUTTING);
		}
		String label = need == null ? "no guide running" : need.label;
		button.setName("<col=ff9040>" + label + "</col>");
		button.setOpacity(active ? 0 : 140);
		String action;
		if (active)
		{
			action = "Show all items";
		}
		else if (need != null && need.items == null)
		{
			action = "Show only items for this guide (Quest Helper's bank button filters quest items)";
		}
		else
		{
			action = "Show only items for this guide";
		}
		button.setAction(0, action);
		button.revalidate();
	}

	private void removeButton()
	{
		if (button == null)
		{
			return;
		}
		Widget parent = button.getParent();
		if (parent != null && parent.getChildren() != null)
		{
			Widget[] children = parent.getChildren();
			for (int i = 0; i < children.length; i++)
			{
				if (children[i] == button)
				{
					children[i] = null;
				}
			}
			parent.revalidate();
		}
		button = null;
	}

	/**
	 * The stat icon for a skill guide's button.
	 */
	public static int skillSprite(Skill skill)
	{
		if (skill == null)
		{
			return SpriteID.Staticons.WOODCUTTING;
		}
		switch (skill)
		{
			case ATTACK: return SpriteID.Staticons.ATTACK;
			case STRENGTH: return SpriteID.Staticons.STRENGTH;
			case DEFENCE: return SpriteID.Staticons.DEFENCE;
			case RANGED: return SpriteID.Staticons.RANGED;
			case PRAYER: return SpriteID.Staticons.PRAYER;
			case MAGIC: return SpriteID.Staticons.MAGIC;
			case HITPOINTS: return SpriteID.Staticons.HITPOINTS;
			case AGILITY: return SpriteID.Staticons.AGILITY;
			case HERBLORE: return SpriteID.Staticons.HERBLORE;
			case THIEVING: return SpriteID.Staticons.THIEVING;
			case CRAFTING: return SpriteID.Staticons.CRAFTING;
			case FLETCHING: return SpriteID.Staticons.FLETCHING;
			case MINING: return SpriteID.Staticons.MINING;
			case SMITHING: return SpriteID.Staticons.SMITHING;
			case FISHING: return SpriteID.Staticons.FISHING;
			case COOKING: return SpriteID.Staticons.COOKING;
			case FIREMAKING: return SpriteID.Staticons.FIREMAKING;
			case RUNECRAFT: return SpriteID.Staticons2.RUNECRAFT;
			case SLAYER: return SpriteID.Staticons2.SLAYER;
			case FARMING: return SpriteID.Staticons2.FARMING;
			case HUNTER: return SpriteID.Staticons2.HUNTER;
			case CONSTRUCTION: return SpriteID.Staticons2.CONSTRUCTION;
			case SAILING: return SpriteID.Staticons2.SAILING;
			default: return SpriteID.Staticons.WOODCUTTING;
		}
	}
}
