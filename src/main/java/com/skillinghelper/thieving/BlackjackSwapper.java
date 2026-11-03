package com.skillinghelper.thieving;

import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/**
 * While the blackjacking guide is open, makes the bandit's or thug's left-click "Knock-Out", and after a knock-out
 * lands, "Pickpocket" for the two pickpockets that fit while it's out. Only reorders the NPC's own options, like
 * Menu Entry Swapper's NPC left-click swaps (which allow Knock-Out since July 2026); hold Shift for the normal menu.
 */
@Singleton
public class BlackjackSwapper
{
	public static final String METHOD_ID = "thieve_blackjacking";
	static final Set<String> TARGETS = Set.of("bandit", "menaphite thug");
	static final String KNOCK_OUT = "Knock-Out";
	static final String PICKPOCKET = "Pickpocket";
	/**
	 * Pickpockets that fit while the target is out.
	 */
	static final int PICKPOCKETS = 2;
	/**
	 * Ticks after a knock-out lands to keep Pickpocket on left-click, in case a pickpocket message is missed.
	 */
	static final int OUT_TICKS = 5;
	private static final Pattern KNOCKED_OUT = Pattern.compile("render (?:them|him|her|it) unconscious",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern PICKED = Pattern.compile("pick the .+'s pocket", Pattern.CASE_INSENSITIVE);

	private final Client client;
	private boolean active;
	private int knockedOutTick = -1;
	private int pickpockets;

	@Inject
	public BlackjackSwapper(Client client)
	{
		this.client = client;
	}

	/**
	 * Turns the swap on while the blackjacking guide is shown and the setting is on.
	 */
	public void setActive(boolean on)
	{
		active = on;
		if (!on)
		{
			reset();
		}
	}

	public void reset()
	{
		knockedOutTick = -1;
		pickpockets = 0;
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (!active || (e.getType() != ChatMessageType.SPAM && e.getType() != ChatMessageType.GAMEMESSAGE))
		{
			return;
		}
		String msg = Text.removeTags(e.getMessage());
		if (KNOCKED_OUT.matcher(msg).find())
		{
			knockedOutTick = client.getTickCount();
			pickpockets = 0;
		}
		else if (knockedOutTick >= 0 && PICKED.matcher(msg).find() && ++pickpockets >= PICKPOCKETS)
		{
			reset();
		}
	}

	/**
	 * The option to put on left-click right now.
	 */
	String wanted()
	{
		boolean out = knockedOutTick >= 0 && client.getTickCount() - knockedOutTick <= OUT_TICKS;
		return out ? PICKPOCKET : KNOCK_OUT;
	}

	@Subscribe(priority = -1)
	public void onPostMenuSort(PostMenuSort e)
	{
		if (!active || client.isMenuOpen() || client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}
		String option = wanted();
		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		int top = entries.length - 1;
		if (top < 0)
		{
			return;
		}
		// Only swap within the hovered NPC's own options
		NPC npc = entries[top].getNpc();
		if (npc == null || npc.getName() == null || !TARGETS.contains(Text.removeTags(npc.getName()).toLowerCase()))
		{
			return;
		}
		for (int i = top - 1; i >= 0; i--)
		{
			MenuEntry entry = entries[i];
			if (entry.getNpc() == npc && option.equalsIgnoreCase(Text.removeTags(entry.getOption())))
			{
				entries[i] = entries[top];
				entries[top] = entry;
				menu.setMenuEntries(entries);
				return;
			}
		}
	}
}
