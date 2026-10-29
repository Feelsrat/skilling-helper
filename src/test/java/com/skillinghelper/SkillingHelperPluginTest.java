package com.skillinghelper;

import com.skillinghelper.pvm.PvmProgressionPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SkillingHelperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SkillingHelperPlugin.class, PvmProgressionPlugin.class);
		RuneLite.main(args);
	}
}