package com.skillinghelper;

import com.skillinghelper.banked.BankedXpPlugin;
import com.skillinghelper.money.MoneyMakingPlugin;
import com.skillinghelper.pvm.PvmProgressionPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SkillingHelperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SkillingHelperPlugin.class, PvmProgressionPlugin.class,
			MoneyMakingPlugin.class, BankedXpPlugin.class);
		RuneLite.main(args);
	}
}