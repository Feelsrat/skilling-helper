package com.skillinghelper.banked;

import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.Requirement;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Everything the panel shows, built on the client thread each tick. The panel only redraws when it changes.
 */
@Value
@Builder
public class BankedView
{
	public static final BankedView LOGGED_OUT = BankedView.builder().build();

	boolean loggedIn;
	boolean bankKnown;
	boolean running;
	@Builder.Default
	List<BankedPlanner.Group> groups = Collections.emptyList();
	@Builder.Default
	List<BankedPlanner.SkillTotal> totals = Collections.emptyList();
	@Builder.Default
	List<BankedPlanner.Option> queue = Collections.emptyList();
	/**
	 * The activity the running guide is on, or null.
	 */
	BankedPlanner.Option current;
	GuideStep step;
	@Builder.Default
	List<Requirement> requirements = Collections.emptyList();
}
