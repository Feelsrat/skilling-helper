package com.skillinghelper;

import java.awt.Color;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.Notifier;
import net.runelite.client.config.Notification;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

/**
 * Countdown infoboxes for runs (bird houses, herbs, trees...) and a notification when one is ready. Runs on the
 * client thread.
 */
@Singleton
class RunTimers
{
	/**
	 * Stop showing "Ready" after this long, e.g. when the player has stopped doing a run.
	 */
	private static final long FORGET_AFTER = 12 * 60 * 60;

	@Inject
	private InfoBoxManager infoBoxManager;
	@Inject
	private ItemManager itemManager;
	@Inject
	private Notifier notifier;

	private final Map<String, Countdown> shown = new HashMap<>();
	private final Map<String, Boolean> notified = new HashMap<>();

	/**
	 * Updates one run's countdown. Call every game tick.
	 *
	 * @param readyAt when the run is ready, in epoch seconds, or 0 if there's nothing growing
	 */
	void update(Plugin plugin, String label, int icon, long readyAt, boolean show, Notification notification)
	{
		long now = Instant.now().getEpochSecond();
		Boolean done = notified.get(label);
		if (done == null)
		{
			// First look after login: don't notify for runs that were already ready
			done = readyAt <= now;
		}
		else if (readyAt > now)
		{
			done = false;
		}
		else if (!done)
		{
			done = true;
			notifier.notify(notification, "Your " + label.toLowerCase() + " are ready: time for a run!");
		}
		notified.put(label, done);

		boolean active = show && readyAt > 0 && now - readyAt < FORGET_AFTER;
		Countdown box = shown.get(label);
		if (active)
		{
			if (box == null)
			{
				box = new Countdown(plugin, label, icon);
				shown.put(label, box);
				infoBoxManager.addInfoBox(box);
			}
			box.readyAt = readyAt;
		}
		else if (box != null)
		{
			infoBoxManager.removeInfoBox(box);
			shown.remove(label);
		}
	}

	void clear()
	{
		shown.values().forEach(infoBoxManager::removeInfoBox);
		shown.clear();
		notified.clear();
	}

	private final class Countdown extends InfoBox
	{
		private final String label;
		private long readyAt;

		Countdown(Plugin plugin, String label, int icon)
		{
			super(itemManager.getImage(icon), plugin);
			this.label = label;
		}

		private long minutesLeft()
		{
			long left = readyAt - Instant.now().getEpochSecond();
			return left <= 0 ? 0 : (left + 59) / 60;
		}

		@Override
		public String getText()
		{
			long left = minutesLeft();
			return left == 0 ? "Ready" : left >= 60 ? String.format("%dh%02d", left / 60, left % 60) : left + "m";
		}

		@Override
		public Color getTextColor()
		{
			return minutesLeft() == 0 ? Color.GREEN : Color.WHITE;
		}

		@Override
		public String getTooltip()
		{
			long left = minutesLeft();
			return left == 0 ? label + " are ready: time for a run" : label + " ready in ~" + left + " min";
		}
	}
}
