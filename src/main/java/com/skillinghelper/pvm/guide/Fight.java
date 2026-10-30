package com.skillinghelper.pvm.guide;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * What the guide shows once the boss is in view. Only things that are on screen right now are highlighted (the
 * boss and the adds it has spawned); nothing predicts attacks, prayers or where to stand, which Jagex's
 * third-party client rules forbid.
 */
@Value
@Builder
public class Fight
{
	/**
	 * NPC names of the boss (or bosses).
	 */
	@Singular
	List<String> bosses;
	/**
	 * Adds the boss spawns, highlighted in their own colour while they're alive.
	 */
	@Singular
	List<Add> adds;
	/**
	 * What to know before going in. Shown in the panel, not during the fight.
	 */
	@Singular
	List<String> tips;
	/**
	 * What a boss does, by NPC name, shown while it's in view (raids have one per room). Descriptions only: nothing
	 * about when it attacks next or what to pray.
	 */
	@Singular
	Map<String, String> notes;
	/**
	 * Progress lines during the fight, e.g. how many brothers are down. Never about what the boss does next.
	 */
	@Builder.Default
	Function<GuideState, List<String>> live = s -> List.of();
	/**
	 * The wave that's up, for a boss at the end of a wave minigame; null otherwise.
	 */
	@Builder.Default
	Function<GuideState, WaveCard> wave = s -> null;

	@Value
	public static class Add
	{
		String name;
		/**
		 * Shown while at least one is alive, e.g. "Growthlings are up: kill them".
		 */
		String text;
	}
}
