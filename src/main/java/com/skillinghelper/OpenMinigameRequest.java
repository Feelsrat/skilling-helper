package com.skillinghelper;

import lombok.Value;

/**
 * Posted on the event bus (on the client thread) by other plugins in this jar, such as Collection Log Guide, to open
 * the Minigame Guides panel on one minigame. Ids are the {@code Minigame.id} values, e.g. "castle_wars".
 */
@Value
public class OpenMinigameRequest
{
	String minigameId;
}
