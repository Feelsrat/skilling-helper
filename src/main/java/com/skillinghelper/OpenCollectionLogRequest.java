package com.skillinghelper;

import lombok.Value;

/**
 * Asks the Collection Log Guide to open one collection log page (its steps and slots).
 */
@Value
public class OpenCollectionLogRequest
{
	/**
	 * The page name as in the game's log, e.g. "Giant Mole".
	 */
	String entry;
}
