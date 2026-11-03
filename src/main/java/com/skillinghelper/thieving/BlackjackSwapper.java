package com.skillinghelper.thieving;

import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/**
 * While the blackjacking guide is open, puts the right option on left-click on bandits and Menaphite Thugs, the way
 * the wiki says to blackjack:
 * <ul>
 * <li>Knock-Out, and straight after clicking it, Pickpocket, so the first pickpocket goes in as fast as possible.</li>
 * <li>Pickpocket for the two guaranteed pickpockets while the target is out.</li>
 * <li>Knock-Out again right away after a failed knock-out, which resets the target's aggression.</li>
 * </ul>
 * Only reorders the NPC's own options, like Menu Entry Swapper's NPC left-click swaps (which allow Knock-Out since
 * July 2026); hold Shift for the normal menu. Also notices when a target starts attacking the player, so the guide
 * can say how to get out of combat.
 */
@Singleton
public class BlackjackSwapper
{
	public static final String METHOD_ID = "thieve_blackjacking";
	static final Set<String> TARGETS = Set.of("bandit", "menaphite thug");
	static final String KNOCK_OUT = "Knock-Out";
	static final String PICKPOCKET = "Pickpocket";
	/**
	 * Guaranteed pickpockets while the target is out.
	 */
	static final int PICKPOCKETS = 2;
	/**
	 * Ticks after clicking Knock-Out to keep Pickpocket on left-click: the knock-out plus two pickpockets, 2 ticks
	 * each, with a little slack.
	 */
	static final int OUT_TICKS = 7;
	private static final Pattern KNOCK_OUT_FAILED = Pattern.compile("glances off", Pattern.CASE_INSENSITIVE);
	private static final Pattern PICKED = Pattern.compile("pick the .+'s pocket", Pattern.CASE_INSENSITIVE);

	private final Client client;
	private boolean active;
	private boolean swap;
	private int knockOutTick = -1;
	private int pickpockets;
	/**
	 * A bandit or thug attacking the player, or null.
	 */
	@Getter
	private NPC attacker;

	@Inject
	public BlackjackSwapper(Client client)
	{
		this.client = client;
	}

	/**
	 * Tracks while the blackjacking guide is shown; swaps only when the setting is on too.
	 */
	public void setActive(boolean guideOpen, boolean swapOn)
	{
		active = guideOpen;
		swap = guideOpen && swapOn;
		if (!guideOpen)
		{
			reset();
			attacker = null;
		}
	}

	private void reset()
	{
		knockOutTick = -1;
		pickpockets = 0;
	}

	static boolean isTarget(NPC npc)
	{
		return npc != null && npc.getName() != null && TARGETS.contains(Text.removeTags(npc.getName()).toLowerCase());
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		if (active && KNOCK_OUT.equalsIgnoreCase(Text.removeTags(e.getMenuOption())) && isTarget(e.getMenuEntry().getNpc()))
		{
			knockOutTick = client.getTickCount();
			pickpockets = 0;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (!active || knockOutTick < 0
			|| (e.getType() != ChatMessageType.SPAM && e.getType() != ChatMessageType.GAMEMESSAGE))
		{
			return;
		}
		String msg = Text.removeTags(e.getMessage());
		if (KNOCK_OUT_FAILED.matcher(msg).find())
		{
			// Knocking out again straight away is the quickest way to reset its aggression
			reset();
		}
		else if (PICKED.matcher(msg).find() && ++pickpockets >= PICKPOCKETS)
		{
			reset();
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged e)
	{
		if (!active)
		{
			return;
		}
		Actor source = e.getSource();
		if (source instanceof NPC && isTarget((NPC) source))
		{
			if (e.getTarget() == client.getLocalPlayer())
			{
				attacker = (NPC) source;
				reset();
			}
			else if (source == attacker)
			{
				attacker = null;
			}
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		if (e.getNpc() == attacker)
		{
			attacker = null;
		}
	}

	/**
	 * The option to put on left-click right now.
	 */
	String wanted()
	{
		boolean out = knockOutTick >= 0 && client.getTickCount() - knockOutTick <= OUT_TICKS;
		return out ? PICKPOCKET : KNOCK_OUT;
	}

	@Subscribe(priority = -1)
	public void onPostMenuSort(PostMenuSort e)
	{
		if (!swap || client.isMenuOpen() || client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}
		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		int top = entries.length - 1;
		if (top < 0)
		{
			return;
		}
		// Only swap within the hovered NPC's own options
		NPC npc = entries[top].getNpc();
		if (!isTarget(npc))
		{
			return;
		}
		String option = wanted();
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

	/**
	 * What to do when a target is attacking the player: it can't be knocked out or pickpocketed in combat.
	 */
	public static String[] combatLines()
	{
		return new String[]{
			"It's attacking you, so it can't be knocked out or pickpocketed until combat stops.",
			"Unequip the blackjack (or wield any other weapon), then click Knock-Out on it: that makes it stop "
				+ "attacking. Wait a few seconds, wield the blackjack again and carry on.",
			"No need to run away. In Ali the Dyer's house you can also climb the ladder and come back down.",
			"Next time a knock-out fails, click Knock-Out again straight away: that resets its aggression before it "
				+ "hits you.",
		};
	}
}
