package com.skillinghelper;

import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar, such as Collection Log Guide, to open
 * the Minigame Guides panel on one minigame and start its guide (handed to Skilling Helper or PvM Progression when the full guide is there). Ids are the {@code Minigame.id} values, e.g. "castle_wars".
 */
@Value
public class OpenMinigameRequest
{
	String minigameId;
}
