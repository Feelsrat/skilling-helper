package com.skillinghelper;

import lombok.Value;
import net.runelite.api.Skill;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar, such as PvM Progression, to open
 * the Skilling Helper guide for a skill with a goal level.
 */
@Value
public class TrainSkillRequest
{
	Skill skill;
	int targetLevel;
}
