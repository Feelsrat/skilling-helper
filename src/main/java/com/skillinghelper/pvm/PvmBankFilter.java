package com.skillinghelper.pvm;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.bank.BankSearch;

/**
 * A bank button that shows only the running PvM guide's loadout, like Skilling Helper's. It answers the bank's own
 * search filter script, so nothing is clicked or moved for the player. It stays on until switched off.
 */
@Singleton
class PvmBankFilter
{
	static final String KEY_ON = "bankFilterOn";
	private static final int BUTTON_SIZE = 25;
	/**
	 * Skilling Helper's button sits right of the bank tutorial button; this one goes one further along.
	 */
	private static final int OFFSET = 4 + BUTTON_SIZE + 4;

	private final Client client;
	private final ClientThread clientThread;
	private final BankSearch bankSearch;
	private final ItemManager itemManager;
	private final PvmProgressionPlugin plugin;
	private final ConfigManager configManager;

	@Getter
	private boolean active;
	private Widget button;

	@Inject
	PvmBankFilter(Client client, ClientThread clientThread, BankSearch bankSearch, ItemManager itemManager,
		PvmProgressionPlugin plugin, ConfigManager configManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.bankSearch = bankSearch;
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.configManager = configManager;
	}

	void startUp()
	{
		active = Boolean.TRUE.equals(configManager.getConfiguration(PvmProgressionPlugin.GROUP, KEY_ON,
			Boolean.class));
	}

	void shutDown()
	{
		clientThread.invoke(() ->
		{
			// Show the whole bank again, but remember the setting for next time
			active = false;
			relayout();
			removeButton();
		});
	}

	/**
	 * Re-filters an open bank, e.g. when the guide starts, stops or its loadout changes.
	 */
	void relayout()
	{
		if (client.getItemContainer(InventoryID.BANK) != null && client.getWidget(InterfaceID.Bankmain.ITEMS) != null)
		{
			bankSearch.layoutBank();
		}
	}

	private void setActive(boolean on)
	{
		active = on;
		configManager.setConfiguration(PvmProgressionPlugin.GROUP, KEY_ON, on);
		updateButton();
		relayout();
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
		Set<Integer> wanted = plugin.getBankItems();
		// No guide running means nothing to filter by, so show the whole bank
		if (!active || wanted.isEmpty())
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
				boolean show = itemId > -1 && wanted.contains(itemManager.canonicalize(itemId));
				intStack[intStackSize - 2] = show ? 1 : 0;
				break;
			}
			default:
				break;
		}
	}

	private void addButton()
	{
		if (plugin.getBankItems().isEmpty())
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
		button.setOriginalX(tut.getOriginalX() + tut.getOriginalWidth() + OFFSET);
		button.setOriginalY(tut.getOriginalY());
		button.setOriginalWidth(BUTTON_SIZE);
		button.setOriginalHeight(BUTTON_SIZE);
		button.setXPositionMode(tut.getXPositionMode());
		button.setYPositionMode(tut.getYPositionMode());
		button.setSpriteId(SpriteID.Staticons.ATTACK);
		button.setHasListener(true);
		button.setName("<col=ff9040>PvM loadout</col>");
		button.setOnOpListener((JavaScriptCallback) ev -> setActive(!active));
		updateButton();
		button.revalidate();
	}

	private void updateButton()
	{
		if (button == null)
		{
			return;
		}
		button.setOpacity(active ? 0 : 140);
		button.setAction(0, active ? "Show all items" : "Show only my PvM loadout");
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
}
