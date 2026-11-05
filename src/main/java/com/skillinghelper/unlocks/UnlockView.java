package com.skillinghelper.unlocks;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Everything the side panel shows, built on the client thread. Compared by value so the panel only redraws when
 * something it shows changed.
 */
@Value
@Builder
public class UnlockView
{
	public enum RowState
	{
		DONE,
		/**
		 * The guide is on this one right now.
		 */
		CURRENT,
		READY,
		/**
		 * Can't tell until the bank is opened.
		 */
		UNKNOWN,
		LOCKED,
		SKIPPED
	}

	@Value
	public static class Row
	{
		String id;
		String name;
		String category;
		RowState state;
		List<String> gives;
		/**
		 * How it's got, one line.
		 */
		String how;
		List<String> missing;
		long cost;
	}

	boolean loggedIn;
	int done;
	int total;
	List<Row> rows;
	boolean running;
	UnlockStep step;
	/**
	 * Wiki page of the unlock the guide is on, or null.
	 */
	String stepWiki;
	boolean hideDone;
}
