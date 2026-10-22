package com.skillinghelper;

import com.skillinghelper.data.Mooring;
import com.skillinghelper.data.TrainingPreference;
import com.skillinghelper.data.guides.GuideRepository;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;

/**
 * The player's guide choices, saved per RuneScape account: the open skill, each skill's goal and chosen method, the
 * training style, and where they last moored their boat.
 */
@Singleton
class GuideSettings
{
	static final String KEY_PREFERENCE = "preference";
	private static final String KEY_ACTIVE_SKILL = "activeSkill";
	private static final String KEY_TARGET = "target.";
	private static final String KEY_METHOD = "method.";
	private static final String KEY_BOAT_MOORING = "boatMooring";

	private final ConfigManager configManager;
	private final SkillingHelperConfig config;
	private final GuideRepository guides;

	private final Map<Skill, Integer> targets = new EnumMap<>(Skill.class);
	private final Map<Skill, String> forcedMethods = new EnumMap<>(Skill.class);
	@Getter
	private Skill activeSkill;
	@Getter
	private TrainingPreference preference;
	@Getter
	private Mooring boatMooring;

	@Inject
	GuideSettings(ConfigManager configManager, SkillingHelperConfig config, GuideRepository guides)
	{
		this.configManager = configManager;
		this.config = config;
		this.guides = guides;
		this.preference = config.preference();
	}

	/**
	 * Reads the logged-in account's saved choices.
	 */
	void load()
	{
		targets.clear();
		forcedMethods.clear();
		for (Skill s : Skill.values())
		{
			Integer target = parseInt(get(KEY_TARGET + s.name()));
			if (target != null)
			{
				targets.put(s, target);
			}
			String method = get(KEY_METHOD + s.name());
			if (method != null && guides.byId(method) != null)
			{
				forcedMethods.put(s, method);
			}
		}
		TrainingPreference saved = parseEnum(TrainingPreference.class, get(KEY_PREFERENCE));
		preference = saved != null ? saved : config.preference();
		boatMooring = parseEnum(Mooring.class, get(KEY_BOAT_MOORING));
		activeSkill = parseEnum(Skill.class, get(KEY_ACTIVE_SKILL));
	}

	void setActiveSkill(Skill skill)
	{
		activeSkill = skill;
		set(KEY_ACTIVE_SKILL, skill == null ? null : skill.name());
	}

	/**
	 * The saved goal for a skill, or null if there isn't one.
	 */
	Integer getTarget(Skill skill)
	{
		return targets.get(skill);
	}

	/**
	 * Saves a goal, clamped to a level the player can still reach.
	 */
	void setTarget(Skill skill, int target)
	{
		int clamped = Math.max(2, Math.min(Experience.MAX_REAL_LEVEL, target));
		targets.put(skill, clamped);
		configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, KEY_TARGET + skill.name(), clamped);
	}

	/**
	 * The method the player picked for a skill, or null to let the guide choose.
	 */
	String getForcedMethod(Skill skill)
	{
		return forcedMethods.get(skill);
	}

	void setForcedMethod(Skill skill, String methodId)
	{
		if (methodId == null)
		{
			forcedMethods.remove(skill);
		}
		else
		{
			forcedMethods.put(skill, methodId);
		}
		set(KEY_METHOD + skill.name(), methodId);
	}

	void setPreference(TrainingPreference pref)
	{
		preference = pref;
		set(KEY_PREFERENCE, pref.name());
	}

	/**
	 * Follows a change to the default training style in the plugin settings.
	 */
	void useDefaultPreference()
	{
		preference = config.preference();
	}

	void setBoatMooring(Mooring mooring)
	{
		boatMooring = mooring;
		set(KEY_BOAT_MOORING, mooring.name());
	}

	private String get(String key)
	{
		return configManager.getRSProfileConfiguration(SkillingHelperConfig.GROUP, key);
	}

	private void set(String key, String value)
	{
		if (value == null)
		{
			configManager.unsetRSProfileConfiguration(SkillingHelperConfig.GROUP, key);
		}
		else
		{
			configManager.setRSProfileConfiguration(SkillingHelperConfig.GROUP, key, value);
		}
	}

	private static Integer parseInt(String value)
	{
		if (value == null)
		{
			return null;
		}
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private static <E extends Enum<E>> E parseEnum(Class<E> type, String value)
	{
		if (value == null)
		{
			return null;
		}
		try
		{
			return Enum.valueOf(type, value);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}
}
