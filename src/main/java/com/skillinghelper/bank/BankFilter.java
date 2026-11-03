package com.skillinghelper.bank;

import com.skillinghelper.SkillingHelperConfig;
import com.skillinghelper.GuideHost;
import com.skillinghelper.SkillingHelperPlugin;
import java.util.Map;
import java.util.function.BooleanSupplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
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
 * Adds a button to the bank that, like Quest Helper's, shows only the items the open guide needs. It hooks the
 * bank's own search filter script, so nothing is clicked or moved for the player. Once switched on it stays on,
 * across bank visits and restarts, until the player switches it off.
 */
@Singleton
public class BankFilter
{
	private static final int BUTTON_SIZE = 25;

	private final Client client;
	private final ClientThread clientThread;
	private final BankSearch bankSearch;
	private final ItemManager itemManager;
	private final GuideHost plugin;
	private final ConfigManager configManager;
	private final Options options;

	@Getter
	private boolean active;
	private Widget button;

	@Inject
	BankFilter(Client client, ClientThread clientThread, BankSearch bankSearch, ItemManager itemManager,
		SkillingHelperPlugin plugin, SkillingHelperConfig config, ConfigManager configManager)
	{
		this(client, clientThread, bankSearch, itemManager, plugin, configManager,
			new Options(SkillingHelperConfig.GROUP, 0, "Skilling Helper items", config::showBankButton,
				config::bankFilterOn));
	}

	/**
	 * For another plugin in this jar that runs its own guide, with its own button slot and saved setting.
	 */
	public BankFilter(Client client, ClientThread clientThread, BankSearch bankSearch, ItemManager itemManager,
		GuideHost plugin, ConfigManager configManager, Options options)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.bankSearch = bankSearch;
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.configManager = configManager;
		this.options = options;
		this.active = options.getSavedOn().getAsBoolean();
	}

	/**
	 * Where a plugin's button goes and where its on/off setting is saved.
	 */
	@Value
	public static class Options
	{
		/**
		 * Config group the "bankFilterOn" setting is saved in.
		 */
		String configGroup;
		/**
		 * Which button this is, counting from 0 next to the bank's tutorial button.
		 */
		int slot;
		/**
		 * The button's name in its menu option.
		 */
		String label;
		BooleanSupplier showButton;
		BooleanSupplier savedOn;
	}

	/**
	 * Picks the saved setting back up: the plugin may have been switched off and on, which keeps this instance.
	 */
	public void startUp()
	{
		active = options.getSavedOn().getAsBoolean();
	}

	public void toggle()
	{
		clientThread.invoke(() -> setActive(!active));
	}

	public void setActive(boolean active)
	{
		this.active = active;
		configManager.setConfiguration(options.getConfigGroup(), "bankFilterOn", active);
		updateButton();
		if (client.getItemContainer(InventoryID.BANK) != null)
		{
			bankSearch.layoutBank();
		}
	}

	public void shutDown()
	{
		clientThread.invoke(() ->
		{
			// Show the whole bank again, but remember the setting for next time
			active = false;
			if (client.getItemContainer(InventoryID.BANK) != null)
			{
				bankSearch.layoutBank();
			}
			removeButton();
		});
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
		// No guide open means no items to show, so show the whole bank
		if (!active || !plugin.getGuide().isActive())
		{
			return;
		}
		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		switch (e.getEventName())
		{
			case "getSearchingTagTab":
				intStack[intStackSize - 1] = 1;
				break;
			case "bankBuildTab":
				// single tab view, so tab separators don't leave gaps
				intStack[intStackSize - 1] = 1;
				break;
			case "bankSearchFilter":
			{
				int itemId = intStack[intStackSize - 1];
				Map<Integer, ?> wanted = plugin.getGuide().getBankItems();
				boolean show = itemId > -1 && wanted.containsKey(itemManager.canonicalize(itemId));
				intStack[intStackSize - 2] = show ? 1 : 0;
				break;
			}
			default:
				break;
		}
	}

	private void addButton()
	{
		if (!options.getShowButton().getAsBoolean())
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
		button.setOriginalX(tut.getOriginalX() + tut.getOriginalWidth() + 4 + options.getSlot() * (BUTTON_SIZE + 4));
		button.setOriginalY(tut.getOriginalY());
		button.setOriginalWidth(BUTTON_SIZE);
		button.setOriginalHeight(BUTTON_SIZE);
		button.setXPositionMode(tut.getXPositionMode());
		button.setYPositionMode(tut.getYPositionMode());
		button.setHasListener(true);
		button.setAction(0, "Toggle");
		button.setName("<col=ff9040>" + options.getLabel() + "</col>");
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
		Skill skill = plugin.getGuide().getSkill();
		button.setSpriteId(skillSprite(skill));
		button.setOpacity(active ? 0 : 140);
		button.setAction(0, active ? "Show all items" : "Show only items for this guide");
	}

	private void removeButton()
	{
		if (button != null)
		{
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

	private static int skillSprite(Skill skill)
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
