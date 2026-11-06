package com.skillinghelper.collection;

import com.skillinghelper.collection.data.ClogData;
import com.skillinghelper.collection.data.ClogEntry;
import com.skillinghelper.collection.data.ClogItem;
import com.skillinghelper.collection.data.ClogLinks;
import com.skillinghelper.collection.data.ClogTab;
import com.skillinghelper.collection.data.GuideLink;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Works out which collection log slots are worth going for. Pure: no client access.
 * <p>
 * "Quickest" uses the wiki's completion figures (the share of tracked players who have each slot). A slot most
 * players have is one that comes quickly, whatever the reason (a common drop, a cheap reward-shop buy, a fast
 * minigame), so the figure ranks drop tables, shops and activities on one scale without guessing kill times.
 */
public final class ClogPlanner
{
	static final int QUICKEST_ROWS = 25;
	static final int CLOSEST_ROWS = 15;
	private static final int NEXT_SLOTS = 3;

	private final ClogData data;
	private final ClogProgress progress;
	/**
	 * Requirement check for a page (and the guide link that applies to it).
	 */
	private final BiFunction<ClogEntry, GuideLink, ClogRequirements.Check> checks;
	private final boolean allowWilderness;
	private final boolean showLocked;
	/**
	 * Every monster name in the Combat Achievements list, lower case.
	 */
	private final Set<String> caMonsters;

	public ClogPlanner(ClogData data, ClogProgress progress,
		BiFunction<ClogEntry, GuideLink, ClogRequirements.Check> checks, boolean allowWilderness, boolean showLocked)
	{
		this(data, progress, checks, allowWilderness, showLocked, Set.of());
	}

	public ClogPlanner(ClogData data, ClogProgress progress,
		BiFunction<ClogEntry, GuideLink, ClogRequirements.Check> checks, boolean allowWilderness, boolean showLocked,
		Set<String> caMonsters)
	{
		this.caMonsters = caMonsters;
		this.data = data;
		this.progress = progress;
		this.checks = checks;
		this.allowWilderness = allowWilderness;
		this.showLocked = showLocked;
	}

	// ---- Totals ----

	public int have()
	{
		int n = 0;
		for (Integer id : data.items().keySet())
		{
			n += progress.has(id) ? 1 : 0;
		}
		return n;
	}

	public int seen()
	{
		int n = 0;
		for (Integer id : data.items().keySet())
		{
			ClogProgress.State s = progress.state(id);
			n += s == ClogProgress.State.OBTAINED || s == ClogProgress.State.MISSING ? 1 : 0;
		}
		return n;
	}

	public int have(ClogEntry e)
	{
		int n = 0;
		for (ClogItem i : e.getItems())
		{
			n += progress.has(i.getId()) ? 1 : 0;
		}
		return n;
	}

	// ---- Lists ----

	/**
	 * Slots not filled yet, the ones most players have first. Each slot shows once, at the page best to get it
	 * from (one the player can do, with a full guide if there is one).
	 */
	public List<ClogView.SlotRow> quickest()
	{
		List<ClogView.SlotRow> out = new ArrayList<>();
		for (ClogItem item : data.items().values())
		{
			if (progress.has(item.getId()))
			{
				continue;
			}
			ClogView.SlotRow row = slot(item);
			if (shown(row.getMissing(), row.isWilderness()))
			{
				out.add(row);
			}
		}
		out.sort(Comparator.comparingDouble((ClogView.SlotRow r) -> -rank(r.getPercent()))
			.thenComparing(ClogView.SlotRow::getName));
		return out.size() > QUICKEST_ROWS ? new ArrayList<>(out.subList(0, QUICKEST_ROWS)) : out;
	}

	/**
	 * Pages with at least one slot filled and some left, fewest left first.
	 */
	public List<ClogView.EntryRow> closest()
	{
		List<ClogView.EntryRow> out = new ArrayList<>();
		for (ClogEntry e : data.entries())
		{
			if (e.isPets())
			{
				continue;
			}
			int have = have(e);
			if (have == 0 || have == e.getItems().size())
			{
				continue;
			}
			ClogView.EntryRow row = entryRow(e);
			if (shown(row.getMissing(), row.isWilderness()))
			{
				out.add(row);
			}
		}
		out.sort(Comparator.comparingInt((ClogView.EntryRow r) -> r.getTotal() - r.getHave())
			.thenComparingDouble(r -> -(double) r.getHave() / r.getTotal())
			.thenComparing(ClogView.EntryRow::getName));
		return out.size() > CLOSEST_ROWS ? new ArrayList<>(out.subList(0, CLOSEST_ROWS)) : out;
	}

	/**
	 * Every pet: the ones still to get first, the commonest first, then the ones the player has. Locked and
	 * Wilderness pets are listed too (marked), so the list is the whole set.
	 */
	public List<ClogView.SlotRow> pets()
	{
		ClogEntry all = data.entry("All Pets");
		List<ClogView.SlotRow> out = new ArrayList<>();
		if (all == null)
		{
			return out;
		}
		for (ClogItem item : all.getItems())
		{
			out.add(slot(item));
		}
		out.sort(Comparator.comparing((ClogView.SlotRow r) -> progress.has(r.getId()))
			.thenComparing(r -> !r.getMissing().isEmpty() || r.isWilderness() && !allowWilderness)
			.thenComparingDouble(r -> -rank(r.getPercent()))
			.thenComparing(ClogView.SlotRow::getName));
		return out;
	}

	/**
	 * Every page of one tab, in the game's order.
	 */
	public List<ClogView.EntryRow> browse(ClogTab tab)
	{
		List<ClogView.EntryRow> out = new ArrayList<>();
		for (ClogEntry e : data.entries())
		{
			if (e.getTab() == tab)
			{
				out.add(entryRow(e));
			}
		}
		return out;
	}

	// ---- The guide ----

	/**
	 * Whether the plugin can take the player to this slot from this page.
	 */
	public boolean guided(ClogEntry e, ClogItem item)
	{
		return e.isPets() ? ClogLinks.of(item, data) != null : ClogGuide.guidable(e);
	}

	/**
	 * The slot to go for next: the quickest one of {@code page} if given and it has one left, else the quickest
	 * slot overall the player can do and be guided to. Locked and Wilderness ones (unless opted in) and
	 * {@code skipped} ones are passed over. Null when there's nothing left.
	 */
	public ClogView.SlotRow next(String page, Set<Integer> skipped)
	{
		if (page != null)
		{
			ClogEntry e = data.entry(page);
			if (e != null)
			{
				for (ClogItem i : left(e))
				{
					ClogView.SlotRow row = e.isPets() ? slot(i) : pageSlot(e, i);
					if (!skipped.contains(i.getId()) && row.isGuided())
					{
						return row;
					}
				}
			}
			return null;
		}
		List<ClogView.SlotRow> all = new ArrayList<>();
		for (ClogItem item : data.items().values())
		{
			if (progress.has(item.getId()) || skipped.contains(item.getId()))
			{
				continue;
			}
			ClogView.SlotRow row = slot(item);
			if (row.isGuided() && row.getMissing().isEmpty() && (allowWilderness || !row.isWilderness()))
			{
				all.add(row);
			}
		}
		return all.stream().max(Comparator.comparingDouble((ClogView.SlotRow r) -> rank(r.getPercent()))
			.thenComparing(ClogView.SlotRow::getName, Comparator.reverseOrder())).orElse(null);
	}

	/**
	 * The page's slots still to get, the quickest first.
	 */
	public List<ClogItem> left(ClogEntry e)
	{
		List<ClogItem> left = new ArrayList<>();
		for (ClogItem i : e.getItems())
		{
			if (!progress.has(i.getId()))
			{
				left.add(i);
			}
		}
		left.sort(Comparator.comparingDouble(i -> -i.rankPercent()));
		return left;
	}

	/**
	 * Every slot of the page as rows, in the game's order.
	 */
	public List<ClogView.SlotRow> pageSlots(ClogEntry e)
	{
		List<ClogView.SlotRow> out = new ArrayList<>();
		for (ClogItem i : e.getItems())
		{
			out.add(e.isPets() ? slot(i) : pageSlot(e, i));
		}
		return out;
	}

	private ClogView.SlotRow pageSlot(ClogEntry e, ClogItem i)
	{
		GuideLink link = ClogLinks.of(e);
		return new ClogView.SlotRow(i.getId(), i.getName(), e.getName(), i.getPercent(), progress.state(i.getId()),
			checks.apply(e, link).getMissing(), e.isWilderness(), link == null ? null : label(link),
			ClogGuide.guidable(e));
	}

	// ---- Rows ----

	ClogView.SlotRow slot(ClogItem item)
	{
		GuideLink petLink = ClogLinks.petLink(item.getName());
		if (petLink != null)
		{
			// Skilling pets and Herbi: their guide is the skill's (or method's), opened from the pet page itself
			String page = data.entriesOf(item.getId()).stream().anyMatch(e -> "Skilling Pets".equals(e.getName()))
				? "Skilling Pets" : "All Pets";
			return new ClogView.SlotRow(item.getId(), item.getName(), page, item.getPercent(),
				progress.state(item.getId()), checks.apply(null, petLink).getMissing(), false, label(petLink), true);
		}
		ClogEntry best = null;
		ClogRequirements.Check bestCheck = null;
		GuideLink bestLink = null;
		int bestScore = Integer.MIN_VALUE;
		for (ClogEntry e : data.entriesOf(item.getId()))
		{
			if (e.isPets() && data.entriesOf(item.getId()).size() > 1 && !"Skilling Pets".equals(e.getName()))
			{
				continue;
			}
			GuideLink link = e.isPets() ? ClogLinks.of(item, data) : ClogLinks.of(e);
			ClogRequirements.Check c = checks.apply(e.isPets() ? null : e, link);
			int score = (c.isLocked() ? 0 : 4) + (e.isWilderness() && !allowWilderness ? 0 : 2) + (link != null || ClogGuide.guidable(e) ? 1 : 0);
			if (score > bestScore)
			{
				best = e;
				bestCheck = c;
				bestLink = link;
				bestScore = score;
			}
		}
		if (best == null)
		{
			return new ClogView.SlotRow(item.getId(), item.getName(), "", item.getPercent(),
				progress.state(item.getId()), List.of(), false, null, false);
		}
		return new ClogView.SlotRow(item.getId(), item.getName(), best.getName(), item.getPercent(),
			progress.state(item.getId()), bestCheck.getMissing(), best.isWilderness(),
			bestLink == null ? null : label(bestLink), bestLink != null || ClogGuide.guidable(best));
	}

	private ClogView.EntryRow entryRow(ClogEntry e)
	{
		List<ClogItem> left = new ArrayList<>();
		for (ClogItem i : e.getItems())
		{
			if (!progress.has(i.getId()))
			{
				left.add(i);
			}
		}
		left.sort(Comparator.comparingDouble(i -> -i.rankPercent()));
		List<String> next = new ArrayList<>();
		for (ClogItem i : left.subList(0, Math.min(NEXT_SLOTS, left.size())))
		{
			next.add(i.getName() + (i.getPercent() >= 0 ? " (" + percent(i.getPercent()) + ")" : ""));
		}
		GuideLink link = ClogLinks.of(e);
		ClogRequirements.Check c = e.isPets() ? ClogRequirements.Check.NONE : checks.apply(e, link);
		return new ClogView.EntryRow(e.getName(), e.getTab(), e.getItems().size() - left.size(), e.getItems().size(),
			next, c.getMissing(), e.isWilderness(), link == null ? null : label(link), ClogGuide.guidable(e));
	}

	private boolean shown(List<String> missing, boolean wilderness)
	{
		return (showLocked || missing.isEmpty()) && (allowWilderness || !wilderness);
	}

	private static double rank(double percent)
	{
		return percent < 0 ? 0.5 : percent;
	}

	static String label(GuideLink link)
	{
		return link.getKind() == GuideLink.Kind.SKILL ? "Skilling Helper (" + link.getSkill().getName() + ")"
			: link.getKind().getPlugin();
	}

	/**
	 * "61%", or "0.4%" for rare slots.
	 */
	public static String percent(double p)
	{
		if (p < 0)
		{
			return "?";
		}
		return p >= 10 ? Math.round(p) + "%" : String.format(Locale.ROOT, "%.1f%%", p);
	}

}
