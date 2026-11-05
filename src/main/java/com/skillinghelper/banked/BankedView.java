package com.skillinghelper.banked;

import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.Requirement;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

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
	/**
	 * The skill the player wants to train, or null for all of them.
	 */
	Skill focus;
	/**
	 * Skills with a use for something in the bank, for the picker.
	 */
	@Builder.Default
	List<Skill> skills = Collections.emptyList();
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
	/**
	 * Product of a profit run started from Money Making, or null for a normal banked xp run.
	 */
	String profitRun;
	/**
	 * Gold one product of the profit run sells for, after tax.
	 */
	int profitEach;
}
