package com.skillinghelper.ui;

import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

/**
 * Holds back a side panel's redraws while the panel is closed, and does them when it's opened again. Rebuilding a
 * panel costs tens of milliseconds on the Swing thread, which is wasted on a panel nobody is looking at.
 * <p>
 * Only the latest call of each kind is kept, replayed in the order they were last made, so the panel ends up the
 * same as if every call had run. A panel that has never been seen on screen keeps redrawing as before: some
 * clients (RuneMobile) draw panels off-screen, where they never count as showing. Swing thread only.
 */
public final class HiddenRedraws
{
	private final JComponent panel;
	private final Map<String, Runnable> pending = new LinkedHashMap<>();
	private boolean seenShowing;

	public HiddenRedraws(JComponent panel)
	{
		this.panel = panel;
		panel.addHierarchyListener(e ->
		{
			if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing())
			{
				shown();
			}
		});
	}

	/**
	 * Runs {@code redraw} now if the panel is on screen, otherwise when it next is (replacing an earlier held
	 * redraw of the same {@code kind}).
	 */
	public void run(String kind, Runnable redraw)
	{
		if (seenShowing && !panel.isShowing())
		{
			// Re-insert so held redraws replay in the order of their latest call
			pending.remove(kind);
			pending.put(kind, redraw);
			return;
		}
		pending.remove(kind);
		redraw.run();
	}

	/**
	 * Whether any redraw is waiting for the panel to open.
	 */
	public boolean hasPending()
	{
		return !pending.isEmpty();
	}

	void shown()
	{
		seenShowing = true;
		List<Runnable> replay = new ArrayList<>(pending.values());
		pending.clear();
		replay.forEach(Runnable::run);
	}
}
