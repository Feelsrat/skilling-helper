package com.skillinghelper.data.guides;

import com.skillinghelper.data.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * All training methods, grouped by skill.
 */
@Singleton
public class GuideRepository
{
	private final Map<Skill, List<Method>> bySkill = new EnumMap<>(Skill.class);

	public GuideRepository()
	{
		List<Method> all = new ArrayList<>();
		all.addAll(WoodcuttingGuide.methods());
		all.addAll(MiningGuide.methods());
		all.addAll(FishingGuide.methods());
		all.addAll(CookingGuide.methods());
		all.addAll(FiremakingGuide.methods());
		all.addAll(FletchingGuide.methods());
		bySkill.putAll(all.stream().collect(Collectors.groupingBy(Method::getSkill, () -> new EnumMap<>(Skill.class),
			Collectors.toUnmodifiableList())));
	}

	public List<Method> methods(Skill skill)
	{
		return bySkill.getOrDefault(skill, Collections.emptyList());
	}

	public boolean hasGuide(Skill skill)
	{
		return bySkill.containsKey(skill);
	}

	public List<Method> all()
	{
		return bySkill.values().stream().flatMap(List::stream).collect(Collectors.toList());
	}

	public Method byId(String id)
	{
		return all().stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
	}

	/**
	 * Wiki training guide for a skill, used as a fallback for skills without built-in guides.
	 */
	public static String wikiTrainingPage(Skill skill, boolean members)
	{
		String name = skill.getName();
		return (members ? "Pay-to-play_" : "Free-to-play_") + name + "_training";
	}
}
