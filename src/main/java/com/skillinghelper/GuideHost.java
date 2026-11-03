package com.skillinghelper;

/**
 * Something that runs a guide the shared overlays, bank button and bank amount swapper can show: Skilling Helper
 * itself, or another plugin in this jar that builds {@link ActiveGuide}s, such as Banked XP.
 */
public interface GuideHost
{
	/**
	 * The open guide, or {@link ActiveGuide#NONE}. Read from the client and render threads.
	 */
	ActiveGuide getGuide();
}
