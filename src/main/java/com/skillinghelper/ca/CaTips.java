package com.skillinghelper.ca;

import com.skillinghelper.ca.data.CaTask;
import com.skillinghelper.ca.data.CaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Plain-words advice for a task: what its kind of task asks of you, plus the OSRS Wiki's strategy notes. Notes that
 * would break the Plugin Hub's boss rules are left out: prayer advice (unless the task itself is about a prayer) and,
 * for wave minigames, anything about what spawns next.
 */
final class CaTips
{
	static final int MAX_WIKI_TIPS = 6;

	private static final Pattern PRAYER = Pattern.compile(
		"\\b(pray|prays|praying|prayed|prayers?|protected|protect from|protection prayers?|overheads?|piety|rigour|augury|"
			+ "eagle eye|mystic might|deflect|flick|flicking|soul split)\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern NEXT_WAVE = Pattern.compile("\\b(next wave|spawns? (next|in)|wave \\d+|spawn(ing)? "
		+ "(location|point|order))\\b", Pattern.CASE_INSENSITIVE);
	private static final Set<String> WAVE_CONTENT = Set.of("TzTok-Jad", "TzKal-Zuk", "Fortis Colosseum",
		"TzHaar-Ket-Rak's Challenges");

	private CaTips()
	{
	}

	/**
	 * What this kind of task means in practice, for someone who's never done one.
	 */
	static List<String> forType(CaTask t)
	{
		List<String> out = new ArrayList<>();
		CaType type = t.getType();
		switch (type)
		{
			case KILL_COUNT:
				if (t.count() > 1)
				{
					out.add("Kills from before count too: the game keeps your kill count.");
				}
				break;
			case RESTRICTION:
				out.add("Stick to the rule in the task for the whole kill. If you break it, finish the kill anyway and "
					+ "try again on the next one.");
				break;
			case MECHANICAL:
				out.add("This is about one of its attacks or mechanics. Read the tips below, then try it; a failed "
					+ "attempt costs nothing but the kill.");
				break;
			case PERFECTION:
				out.add("Don't get hit by the attacks the task lists. If you are, finish the kill and try again.");
				out.add("Bring extra food: safe kills matter more than fast ones here.");
				break;
			case SPEED:
				out.add("The timer runs from the start of the fight. Bring your best gear and boosting potions, and "
					+ "practise a few normal kills first.");
				break;
			case STAMINA:
				out.add("Kill it " + t.count() + " times without leaving. Bring a full inventory of food and "
					+ "potions, and use the drops to keep going.");
				break;
			default:
				break;
		}
		if (t.getText().toLowerCase(Locale.ROOT).contains("without leaving") && type != CaType.STAMINA)
		{
			out.add("Don't leave the area (or teleport out) until it's done.");
		}
		return out;
	}

	/**
	 * The wiki's strategy sentences that are allowed in a hub plugin, at most {@link #MAX_WIKI_TIPS}.
	 */
	static List<String> wiki(CaTask t)
	{
		// Only when the task itself asks for a prayer (e.g. "with Protect from Magic active") is prayer advice the task
		boolean aboutPrayer = t.getText().toLowerCase().matches(".*(protect from|piety|rigour|augury|prayer active).*");
		boolean waves = WAVE_CONTENT.contains(t.getMonster());
		List<String> out = new ArrayList<>();
		for (String s : t.getStrategy())
		{
			if (out.size() >= MAX_WIKI_TIPS)
			{
				break;
			}
			if (!aboutPrayer && PRAYER.matcher(s).find())
			{
				continue;
			}
			if (waves && NEXT_WAVE.matcher(s).find())
			{
				continue;
			}
			if (s.endsWith(":") || s.length() < 12)
			{
				continue;
			}
			out.add(s);
		}
		return out;
	}
}
