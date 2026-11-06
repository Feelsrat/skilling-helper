package com.skillinghelper;

import com.skillinghelper.banked.BankedXpPlugin;
import com.skillinghelper.clue.ClueScrollGuidePlugin;
import com.skillinghelper.collection.CollectionLogPlugin;
import com.skillinghelper.diary.AchievementDiaryPlugin;
import com.skillinghelper.money.MoneyMakingPlugin;
import com.skillinghelper.planner.AccountPlannerPlugin;
import com.skillinghelper.pvm.PvmProgressionPlugin;
import com.skillinghelper.quests.QuestOrderPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SkillingHelperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SkillingHelperPlugin.class, PvmProgressionPlugin.class,
			MoneyMakingPlugin.class, BankedXpPlugin.class, AchievementDiaryPlugin.class,
			QuestOrderPlugin.class, CollectionLogPlugin.class, ClueScrollGuidePlugin.class,
			AccountPlannerPlugin.class);
		RuneLite.main(args);
	}
}