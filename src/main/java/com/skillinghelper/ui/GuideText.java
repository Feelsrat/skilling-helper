package com.skillinghelper.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;

/**
 * Turns one line of guide text into something quick to read: the leading marker ("•", "✔", "1.") goes in its own
 * gutter, a short label ("Then:", "Optional:") becomes a tag, every extra sentence gets its own line, and long
 * asides in brackets drop to a quieter line underneath. {@link #highlight} then colours the bits worth spotting:
 * amounts, item names, gold, levels and the menu option to pick.
 * <p>
 * The guides keep writing plain sentences; all of this happens when the panel draws them, so every plugin that
 * draws its text through {@link GuideUi} gets the same look.
 */
public final class GuideText
{
	/**
	 * Asides shorter than this stay inline, e.g. "(swap)" or "(~662 gp each)".
	 */
	private static final int LONG_ASIDE = 24;
	/**
	 * A single sentence longer than this is split before ", then".
	 */
	private static final int LONG_SENTENCE = 70;

	private static final Pattern MARKER = Pattern.compile("^(•|✔|✘|✖|⚠|▶|★|☆|▣|ℹ|➜|↑|\\?|\\+|-|\\d{1,2}\\.)\\s+");
	private static final Pattern LABEL = Pattern.compile("^([A-Z][A-Za-z']*(?: [A-Za-z']+){0,2}):\\s+");
	/**
	 * Lead-ins drawn as a tag: "Then: ", "This trip: ", "Optional: ".
	 */
	private static final Set<String> LABELS = new HashSet<>(Arrays.asList(
		"then", "this trip", "optional", "tip", "note", "gear", "quest", "where", "wiki tip", "next", "also", "why",
		"warning", "money", "bring", "unlock", "after that", "first"));
	/**
	 * Where a long sentence may break, and what the break leaves at the end of the first half.
	 */
	private static final String[][] BREAKS = {{", then ", ".", "then "}, {", and ", ".", ""}, {": ", ":", ""},
		{", so ", ".", "so "}, {" + ", "", "+ "}};
	private static final Set<String> ABBREVIATIONS = new HashSet<>(Arrays.asList(
		"e.g", "i.e", "etc", "vs", "approx", "mt", "st", "dr", "mr", "mrs", "lvl", "no", "ca"));

	/**
	 * Words that start an instruction; shown brighter so the action stands out.
	 */
	private static final Set<String> VERBS = new HashSet<>(Arrays.asList(
		"attack", "bank", "bring", "buy", "bury", "cast", "catch", "check", "chop", "clean", "click", "climb",
		"collect", "cook", "craft", "cross", "cut", "deposit", "drink", "drop", "eat", "empty", "enter", "equip",
		"exchange", "feed", "fill", "fish", "fletch", "follow", "go", "harvest", "keep", "kill", "leave", "light",
		"loot", "make", "mine", "offer", "open", "pay", "pick", "plant", "pray", "put", "repeat", "restock", "return",
		"run", "search", "sell", "set", "smith", "stand", "start", "steal", "take", "talk", "teleport", "trade",
		"travel", "turn", "use", "walk", "wear", "wield", "withdraw"));

	/**
	 * "7", "1,050", "3.5", never a trailing full stop.
	 */
	private static final String NUM = "\\d(?:[\\d,.]*\\d)?";
	/**
	 * In order of priority; the first group that matches decides the colour.
	 */
	private static final Pattern HIGHLIGHTS = Pattern.compile(
		// 1: something in quotes, usually a button or chat message
		"(\"[^\"]{1,40}\")"
			// 2: an amount of gold: "~1,050 gp", "35M gp", "~100k/hr"
			+ "|((?<![\\w.])~?" + NUM + "\\s?(?:[kKmMbB]\\b)?\\s?gp(?:/hr| each)?|(?<![\\w.])~?" + NUM + "[kKmMbB]/hr)"
			// 3, 4: "28 x Raw salmon": the amount, then the item
			+ "|\\b(\\d[\\d,]*\\s?[x×])\\s(?=[A-Za-z])([A-Za-z][A-Za-z'\\-]*(?: (?!(?:and|or|then|to|from|for|in|at|on|into"
			+ "|with|until|each|per|if|of|so|when|is|are)\\b)[A-Za-z][A-Za-z'\\-]*){0,4})"
			// 5: the option to pick: "choose Bank", "choose Withdraw-X"
			+ "|(?<=choose )([A-Z][A-Za-z\\-]*(?: [a-z]+(?=[.,;)]|$))?)"
			// 6: the item in "Check for Magic logs", "Wear your Leather cowl"
			+ "|(?<=Check for |[Ww]ear your |[Ww]ield your |Withdraw all your |[Ww]ear the |[Ww]ield the )"
			+ "([A-Za-z][A-Za-z'\\-]*(?: [a-z][A-Za-z'\\-]*){0,3})"
			// 7: levels and other plain numbers: "level 70", "14+", "~45k xp/hr", "20%"
			+ "|((?:[Ll]evel |[Ll]vl )\\d+|(?<![\\w.])~?" + NUM + "[kKmM]?(?:\\+|%| xp/hr| xp\\b)?)");

	/**
	 * One piece of a line.
	 */
	@Value
	public static class Part
	{
		String text;
		/**
		 * An aside: drawn quieter than the main text.
		 */
		boolean aside;
	}

	/**
	 * A line split up for display.
	 */
	@Value
	public static class Line
	{
		/**
		 * The leading marker without its space ("•", "✔", "3."), or null.
		 */
		String marker;
		/**
		 * A short lead-in label without its colon ("Then", "Optional"), or null.
		 */
		String label;
		/**
		 * The first part is the line itself, the rest go underneath it.
		 */
		List<Part> parts;

		public String main()
		{
			return parts.isEmpty() ? "" : parts.get(0).getText();
		}
	}

	private GuideText()
	{
	}

	public static Line parse(String raw)
	{
		String s = raw == null ? "" : raw.trim().replaceAll("\\.{2,}$", ".").replaceAll("\\s+", " ");
		String marker = null;
		Matcher m = MARKER.matcher(s);
		if (m.find() && m.end() < s.length())
		{
			marker = m.group(1);
			s = s.substring(m.end());
		}
		String label = null;
		Matcher l = LABEL.matcher(s);
		if (l.find() && l.end() < s.length() && LABELS.contains(l.group(1).toLowerCase(Locale.ROOT)))
		{
			label = l.group(1);
			s = capitalize(s.substring(l.end()));
		}

		List<Part> parts = new ArrayList<>();
		for (String sentence : sentences(s))
		{
			List<Part> pieces = new ArrayList<>();
			addWithAsides(pieces, sentence);
			for (Part piece : pieces)
			{
				if (piece.isAside())
				{
					parts.add(piece);
					continue;
				}
				for (String shortPiece : splitLong(piece.getText()))
				{
					parts.add(new Part(shortPiece, false));
				}
			}
		}
		if (parts.isEmpty())
		{
			parts.add(new Part(s, false));
		}
		// Never start with an aside: there has to be something to hang it off
		if (parts.get(0).isAside())
		{
			Part first = parts.remove(0);
			parts.add(0, new Part("(" + first.getText() + ")", false));
		}
		return new Line(marker, label, Collections.unmodifiableList(parts));
	}

	/**
	 * Splits after ".", "!", "?" and ";" (not inside brackets or quotes, and not after "e.g.").
	 */
	static List<String> sentences(String s)
	{
		List<String> out = new ArrayList<>();
		int depth = 0;
		boolean quoted = false;
		int start = 0;
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '(')
			{
				depth++;
			}
			else if (c == ')')
			{
				depth = Math.max(0, depth - 1);
			}
			else if (c == '"')
			{
				quoted = !quoted;
			}
			else if (depth == 0 && !quoted && (c == '.' || c == '!' || c == '?' || c == ';')
				&& i + 1 < s.length() && s.charAt(i + 1) == ' ' && i + 2 < s.length())
			{
				char next = s.charAt(i + 2);
				boolean breakHere = c == ';' || Character.isUpperCase(next) || Character.isDigit(next) || next == '"'
					|| next == '(';
				if (c == '.' && isAbbreviation(s, start, i))
				{
					breakHere = false;
				}
				if (breakHere)
				{
					String sentence = c == ';' ? s.substring(start, i).trim() + "." : s.substring(start, i + 1).trim();
					if (!sentence.isEmpty())
					{
						out.add(sentence);
					}
					start = i + 2;
				}
			}
		}
		String rest = s.substring(Math.min(start, s.length())).trim();
		if (!rest.isEmpty())
		{
			out.add(rest);
		}
		for (int i = 1; i < out.size(); i++)
		{
			out.set(i, capitalize(out.get(i)));
		}
		return out;
	}

	private static boolean isAbbreviation(String s, int from, int dot)
	{
		int w = dot;
		while (w > from && s.charAt(w - 1) != ' ' && s.charAt(w - 1) != '(')
		{
			w--;
		}
		return ABBREVIATIONS.contains(s.substring(w, dot).toLowerCase(Locale.ROOT));
	}

	/**
	 * Breaks a long sentence at its most central ", then", ", and", ":", ", so" or " + ", again and again until every
	 * piece is short or can't break any more.
	 */
	static List<String> splitLong(String sentence)
	{
		if (sentence.length() <= LONG_SENTENCE)
		{
			return Collections.singletonList(sentence);
		}
		int best = -1;
		String[] how = null;
		for (String[] b : BREAKS)
		{
			for (int at = indexOutsideBrackets(sentence, b[0], 0); at >= 0;
				at = indexOutsideBrackets(sentence, b[0], at + 1))
			{
				int rest = sentence.length() - at - b[0].length();
				if (b[0].equals(", and ") && !startsWithVerb(sentence.substring(at + b[0].length())))
				{
					// "..., and is always in demand" would leave a half sentence
					continue;
				}
				if (at >= 20 && rest >= 20 && (best < 0
					|| Math.abs(at - sentence.length() / 2) < Math.abs(best - sentence.length() / 2)))
				{
					best = at;
					how = b;
				}
			}
		}
		if (how == null)
		{
			return Collections.singletonList(sentence);
		}
		List<String> out = new ArrayList<>(splitLong(sentence.substring(0, best) + how[1]));
		out.addAll(splitLong(capitalize(how[2] + sentence.substring(best + how[0].length()))));
		return out;
	}

	/**
	 * Moves long bracketed asides, and anything after " - ", onto their own lines.
	 */
	private static void addWithAsides(List<Part> parts, String sentence)
	{
		StringBuilder main = new StringBuilder();
		List<Part> after = new ArrayList<>();
		int depth = 0;
		int open = -1;
		for (int i = 0; i < sentence.length(); i++)
		{
			char c = sentence.charAt(i);
			if (c == '(')
			{
				if (depth++ == 0)
				{
					open = i;
				}
			}
			else if (c == ')' && depth > 0)
			{
				if (--depth == 0)
				{
					String inner = sentence.substring(open + 1, i).trim();
					if (inner.length() >= LONG_ASIDE)
					{
						after.add(new Part(capitalize(inner), true));
					}
					else
					{
						main.append(sentence, open, i + 1);
					}
				}
				continue;
			}
			else if (depth == 0 && c == ' ' && sentence.startsWith(" - ", i) && i + 3 < sentence.length())
			{
				after.add(new Part(capitalize(sentence.substring(i + 3).trim()), false));
				break;
			}
			if (depth == 0)
			{
				main.append(c);
			}
		}
		if (depth > 0)
		{
			// Unbalanced bracket: leave the rest as written
			main.append(sentence.substring(open));
		}
		String text = main.toString().replaceAll("\\s+([,.;:!?])", "$1").replaceAll("\\s{2,}", " ").trim();
		if (!text.isEmpty() && !text.equals("."))
		{
			parts.add(new Part(text, false));
		}
		parts.addAll(after);
	}

	private static boolean startsWithVerb(String s)
	{
		int end = s.indexOf(' ');
		return VERBS.contains((end < 0 ? s : s.substring(0, end)).toLowerCase(Locale.ROOT));
	}

	private static int indexOutsideBrackets(String s, String needle, int from)
	{
		int depth = 0;
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '(')
			{
				depth++;
			}
			else if (c == ')')
			{
				depth = Math.max(0, depth - 1);
			}
			else if (depth == 0 && i >= from && s.startsWith(needle, i))
			{
				return i;
			}
		}
		return -1;
	}

	private static String capitalize(String s)
	{
		if (s.isEmpty() || !Character.isLowerCase(s.charAt(0)))
		{
			return s;
		}
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/**
	 * Colours for {@link #highlight}, as "#rrggbb".
	 */
	@Value
	public static class Palette
	{
		String bright;
		String gold;
		String option;
	}

	/**
	 * Escapes {@code text} for a Swing HTML label and colours its key bits: amounts and gold in gold, item names,
	 * numbers and the leading action in bright white, buttons and menu options in orange.
	 */
	public static String highlight(String text, Palette p)
	{
		StringBuilder out = new StringBuilder();
		Matcher first = Pattern.compile("^([A-Z][a-z]+)\\b").matcher(text);
		int from = 0;
		if (first.find() && VERBS.contains(first.group(1).toLowerCase(Locale.ROOT)))
		{
			out.append(color(p.getBright(), first.group(1)));
			from = first.end();
		}
		Matcher m = HIGHLIGHTS.matcher(text);
		int last = from;
		while (from < text.length() && m.find(from))
		{
			if (m.end() == m.start())
			{
				from = m.end() + 1;
				continue;
			}
			out.append(escape(text.substring(last, m.start())));
			if (m.group(1) != null)
			{
				out.append(color(p.getOption(), m.group(1)));
			}
			else if (m.group(2) != null)
			{
				out.append(color(p.getGold(), m.group(2)));
			}
			else if (m.group(3) != null)
			{
				out.append(color(p.getGold(), m.group(3))).append(' ').append(color(p.getBright(), m.group(4)));
			}
			else if (m.group(5) != null)
			{
				out.append(color(p.getOption(), m.group(5)));
			}
			else if (m.group(6) != null)
			{
				out.append(color(p.getBright(), m.group(6)));
			}
			else
			{
				out.append(color(p.getBright(), m.group(7)));
			}
			last = m.end();
			from = m.end();
		}
		out.append(escape(text.substring(Math.min(last, text.length()))));
		return out.toString();
	}

	private static String color(String hex, String s)
	{
		return "<font color='" + hex + "'>" + escape(s) + "</font>";
	}

	public static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
