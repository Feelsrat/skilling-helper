package com.skillinghelper;

import com.skillinghelper.data.Method;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemHighlight;
import com.skillinghelper.engine.MethodOption;
import com.skillinghelper.engine.PlanSegment;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Requirement;
import com.skillinghelper.tog.TearsOfGuthix;
import com.skillinghelper.ui.PickList;
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
@Builder(toBuilder = true)
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
	/**
	 * Hide methods the player has outgrown from the list (shared "Hide outleveled guides" setting).
	 */
	boolean hideOutleveled;
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
	/**
	 * Xp per hour the player is actually getting in this skill this session: 0 before any xp, -1 while measuring.
	 */
	int sessionXpPerHour;
	/**
	 * The open activity guide that isn't about one skill (Tears of Guthix), or null.
	 */
	String activity;
	/**
	 * Extra facts about the activity for the panel, e.g. its reward.
	 */
	@Builder.Default
	List<String> activityInfo = Collections.emptyList();
	/**
	 * Whether the weekly Tears of Guthix is up, shown even with no guide open.
	 */
	TearsOfGuthix.State weeklyState;
	String weeklyStatus;
	/**
	 * A quick pick's ranked list of guides, shown in place of a guide until the player opens one. Null otherwise.
	 */
	PickList picks;

	public boolean isActive()
	{
		return skill != null || activity != null;
	}
}
