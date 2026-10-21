package com.skillinghelper;

import com.skillinghelper.data.Method;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemHighlight;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.PlanSegment;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Requirement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * Immutable snapshot of the open guide. Built on the client thread each tick and read by overlays and the panel.
 */
@Value
@Builder
public class ActiveGuide
{
	public static final ActiveGuide NONE = ActiveGuide.builder().build();

	Skill skill;
	int level;
	int targetLevel;
	long xpRemaining;
	Method method;
	/**
	 * True when the player picked this method instead of letting the planner choose.
	 */
	boolean forced;
	MethodOption option;
	GuideStep step;
	@Builder.Default
	List<Requirement> requirements = Collections.emptyList();
	@Builder.Default
	List<PlanSegment> plan = Collections.emptyList();
	@Builder.Default
	List<MethodOption> options = Collections.emptyList();
	@Builder.Default
	Map<Integer, ItemHighlight> bankItems = Collections.emptyMap();
	PlayerState player;
	boolean hasGuide;
	String wikiPage;
	/**
	 * Where the GE prices shown come from, for the panel.
	 */
	String priceSource;

	public boolean isActive()
	{
		return skill != null;
	}
}
