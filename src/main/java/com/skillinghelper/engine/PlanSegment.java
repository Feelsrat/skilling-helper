package com.skillinghelper.engine;

import lombok.Value;

/**
 * One leg of a training plan: train from {@link #fromLevel} to {@link #toLevel} using {@link #option}.
 */
@Value
public class PlanSegment
{
	int fromLevel;
	int toLevel;
	long xp;
	MethodOption option;
}
