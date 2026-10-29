package com.skillinghelper.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * How quickly the player actually plays. Guide rates are wiki figures for efficient play (best tools, no breaks,
 * tick-perfect clicking), which a relaxed player gets about half of. AFK methods are capped by the game rather than
 * by clicking, so they lose less.
 */
@Getter
@RequiredArgsConstructor
public enum RatePace
{
	RELAXED("Relaxed", 0.5, 0.65),
	STEADY("Steady", 0.75, 0.85),
	EFFICIENT("Efficient (wiki rates)", 1, 1);

	private final String label;
	private final double activeFactor;
	private final double afkFactor;

	/**
	 * The share of the guide's xp per hour a player at this pace gets with this method.
	 */
	public double factor(Method m)
	{
		return m.hasTag(MethodTag.AFK) ? afkFactor : activeFactor;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
