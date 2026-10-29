package com.skillinghelper.pvm;

import com.skillinghelper.pvm.data.PvmContent;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;

/**
 * Reads the game's "Your Zulrah kill count is: 12." style messages (and the chest/raid variants).
 */
final class KillCountParser
{
	private static final Pattern KILL_COUNT = Pattern.compile(
		"Your (?:completed )?(.+?) (?:kill |completion |success |)count is: ?([0-9,]+)\\.?");

	@Value
	static class Kill
	{
		PvmContent content;
		int count;
	}

	private KillCountParser()
	{
	}

	/**
	 * The content and new count this message is about, or null if it isn't a kill count we track.
	 */
	static Kill parse(String message, List<PvmContent> ladder)
	{
		Matcher m = KILL_COUNT.matcher(message.replaceAll("<[^>]*>", ""));
		if (!m.find())
		{
			return null;
		}
		String name = m.group(1).trim();
		for (PvmContent c : ladder)
		{
			for (String kcName : c.getKcNames())
			{
				if (kcName.equalsIgnoreCase(name))
				{
					return new Kill(c, Integer.parseInt(m.group(2).replace(",", "")));
				}
			}
		}
		return null;
	}
}
