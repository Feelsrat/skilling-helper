package com.skillinghelper.bank;

import com.skillinghelper.SkillingHelperConfig;
import com.skillinghelper.SkillingHelperPlugin;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.StepType;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

/**
 * While the guide says what to take out of the bank, moves the game's own Withdraw/Deposit option for the right
 * amount to left-click, the way Menu Entry Swapper does. It only reorders the existing options, so every click is
 * still one action the player chooses. Amounts the game has no option for fall back to Withdraw-X: the player types
 * the amount once and the game then offers it as its own option on later trips.
 */
@Singleton
public class BankAmountSwapper
{
	private final Client client;
	private final SkillingHelperPlugin plugin;
	private final SkillingHelperConfig config;
	private final ItemManager itemManager;

	@Inject
	BankAmountSwapper(Client client, SkillingHelperPlugin plugin, SkillingHelperConfig config, ItemManager itemManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
	}

	// Runs after Menu Entry Swapper so the amount for this trip wins; holding shift keeps its shift-click swaps
	@Subscribe(priority = -1)
	public void onPostMenuSort(PostMenuSort e)
	{
		if (!config.swapBankAmounts() || client.isMenuOpen() || client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}
		GuideStep step = plugin.getGuide().getStep();
		if (step == null || step.getType() != StepType.BANK || client.getItemContainer(InventoryID.BANK) == null)
		{
			return;
		}
		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		if (entries.length == 0)
		{
			return;
		}
		MenuEntry top = entries[entries.length - 1];
		Widget w = top.getWidget();
		if (w == null || w.getItemId() <= 0)
		{
			return;
		}
		if (w.getId() == InterfaceID.Bankmain.ITEMS)
		{
			swapWithdraw(menu, entries, step, w);
		}
		else if (w.getId() == InterfaceID.Bankside.ITEMS)
		{
			swapDeposit(menu, entries, step, w.getItemId());
		}
	}

	private void swapWithdraw(Menu menu, MenuEntry[] entries, GuideStep step, Widget w)
	{
		// Withdrawing as notes would make "all" and the counts below mean something else
		if (client.getVarbitValue(VarbitID.BANK_WITHDRAWNOTES) == 1 || w.getItemQuantity() <= 0)
		{
			return;
		}
		Integer amount = amountFor(step.getWithdrawAmounts(), w.getItemId());
		if (amount != null)
		{
			swapTo(menu, entries, withdrawOption(w.getItemId(), amount, w.getItemQuantity()));
		}
	}

	private void swapDeposit(Menu menu, MenuEntry[] entries, GuideStep step, int itemId)
	{
		Integer amount = amountFor(step.getDepositAmounts(), itemId);
		if (amount != null)
		{
			int carried = carried(itemId);
			swapTo(menu, entries, amount == 0 || amount >= carried ? "Deposit-All" : "Deposit-" + amount, "Deposit-X");
		}
	}

	private Integer amountFor(Map<Integer, Integer> amounts, int itemId)
	{
		Integer amount = amounts.get(itemId);
		return amount != null ? amount : amounts.get(itemManager.canonicalize(itemId));
	}

	/**
	 * The option that takes out exactly what's needed, best first: Withdraw-All when that's the same thing (the bank
	 * has no more than needed, or it fills the inventory's last free slots), then the game's own amount options
	 * (1, 5, 10 and the last amount typed), then Withdraw-X.
	 */
	private String[] withdrawOption(int itemId, int amount, int inBank)
	{
		if (amount == 0 || inBank <= amount)
		{
			return new String[]{"Withdraw-All"};
		}
		boolean stackable = itemManager.getItemComposition(itemId).isStackable();
		if (!stackable && amount >= freeSlots())
		{
			return new String[]{"Withdraw-All"};
		}
		return new String[]{"Withdraw-" + amount, "Withdraw-X"};
	}

	/**
	 * Puts the first of these options that the menu has on left-click.
	 */
	private void swapTo(Menu menu, MenuEntry[] entries, String... options)
	{
		for (String option : options)
		{
			for (int i = entries.length - 1; i >= 0; i--)
			{
				MenuEntry entry = entries[i];
				if (!option.equalsIgnoreCase(Text.removeTags(entry.getOption())))
				{
					continue;
				}
				if (i == entries.length - 1)
				{
					return;
				}
				// Low priority options can't be left-click
				if (entry.getType() == MenuAction.CC_OP_LOW_PRIORITY)
				{
					entry.setType(MenuAction.CC_OP);
				}
				entries[i] = entries[entries.length - 1];
				entries[entries.length - 1] = entry;
				menu.setMenuEntries(entries);
				return;
			}
		}
	}

	private int freeSlots()
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv == null)
		{
			return 28;
		}
		int used = 0;
		for (Item item : inv.getItems())
		{
			if (item.getId() > 0)
			{
				used++;
			}
		}
		return 28 - used;
	}

	private int carried(int itemId)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		return inv == null ? 0 : inv.count(itemId);
	}
}
