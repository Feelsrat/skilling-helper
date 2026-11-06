package com.skillinghelper.collection;

import com.skillinghelper.OpenCollectionLogRequest;
import com.skillinghelper.collection.data.ClogData;
import com.skillinghelper.collection.data.ClogEntry;
import com.skillinghelper.collection.data.ClogItem;
import com.skillinghelper.collection.data.ClogLinks;
import com.skillinghelper.collection.data.GuideLink;
import com.skillinghelper.data.Method;
import com.skillinghelper.data.guides.GuideRepository;
import com.skillinghelper.planner.api.AccountState;
import com.skillinghelper.planner.api.PlanStep;
import com.skillinghelper.planner.api.PlanStepProvider;
import com.skillinghelper.planner.api.StepKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.runelite.api.Skill;

/**
 * Collection Log Guide's steps for the Account Planner: one per log page that has quick slots (ones at least
 * {@link #QUICK_PERCENT}% of players have), done when every quick slot of the page is filled. Rare slots are left
 * out on purpose: a plan can't promise a 1/5,000 drop.
 */
public class ClogStepProvider implements PlanStepProvider
{
	static final double QUICK_PERCENT = 25;
	/**
	 * Rough minutes per quick slot: most come within one sitting of the activity.
	 */
	private static final int MINUTES_PER_SLOT = 30;

	private GuideRepository guides;

	@Override
	public String name()
	{
		return "Collection Log Guide";
	}

	public static String stepId(ClogEntry e)
	{
		return "clog:" + e.getName();
	}

	@Override
	public List<PlanStep> steps(AccountState a)
	{
		ClogData data = ClogData.get();
		Set<Integer> obtained = CollectionLogPlugin.parseIds(
			a.getProfileConfig().apply(CollectionLogConfig.GROUP + ".obtained"));
		Set<Integer> missing = CollectionLogPlugin.parseIds(
			a.getProfileConfig().apply(CollectionLogConfig.GROUP + ".missing"));
		List<PlanStep> out = new ArrayList<>();
		for (ClogEntry e : data.entries())
		{
			if (e.isPets())
			{
				continue;
			}
			List<ClogItem> quick = new ArrayList<>();
			int left = 0;
			for (ClogItem i : e.getItems())
			{
				if (i.getPercent() < QUICK_PERCENT)
				{
					continue;
				}
				quick.add(i);
				boolean has = obtained.contains(i.getId()) || !missing.contains(i.getId()) && a.owned(i.getId()) > 0;
				left += has ? 0 : 1;
			}
			if (!quick.isEmpty())
			{
				out.add(step(e, quick, left));
			}
		}
		return out;
	}

	private PlanStep step(ClogEntry e, List<ClogItem> quick, int left)
	{
		GuideLink link = ClogLinks.of(e);
		List<String> names = new ArrayList<>();
		for (ClogItem i : quick.subList(0, Math.min(3, quick.size())))
		{
			names.add(i.getName());
		}
		PlanStep.PlanStepBuilder b = PlanStep.builder()
			.id(stepId(e))
			.kind(StepKind.COLLECTION)
			.title("Collection log: " + e.getName())
			.why(quick.size() + " quick collection log slot" + (quick.size() == 1 ? "" : "s") + " most players have ("
				+ String.join(", ", names) + (quick.size() > 3 ? " and more" : "") + ").")
			.done(left == 0)
			.minutes(Math.max(1, left) * MINUTES_PER_SLOT)
			.value(2)
			.wilderness(e.isWilderness())
			.open(new OpenCollectionLogRequest(e.getName()))
			.openIn(name())
			.wikiPage(e.getWikiPage())
			.howTo("Press Open guide: Collection Log Guide opens this page with its steps and slots")
			.howTo(link != null ? "Its \"Open full guide\" button starts the " + link.getKind().getPlugin()
				+ " guide for it" : "Its \"Show the way\" button draws the route there");
		if (e.getSlayerLevel() > 0)
		{
			b.skill(Skill.SLAYER, e.getSlayerLevel());
		}
		if (link != null)
		{
			switch (link.getKind())
			{
				case PVM:
					b.after("boss:" + link.getTarget());
					break;
				case MINIGAME:
					b.after("minigame:" + link.getTarget());
					// fall through: same requirements as its Skilling Helper method
				case METHOD:
					Method m = link.getRequirementsOf() == null ? null : guides().byId(link.getRequirementsOf());
					if (m != null)
					{
						b.skill(m.getSkill(), Math.max(1, m.getMinLevel()));
						m.getSkillLevels().forEach(b::skill);
						b.quests(m.getQuests());
						m.getQuestsStarted().forEach(b::questStarted);
					}
					break;
				default:
			}
		}
		return b.build();
	}

	private GuideRepository guides()
	{
		if (guides == null)
		{
			guides = new GuideRepository();
		}
		return guides;
	}
}
