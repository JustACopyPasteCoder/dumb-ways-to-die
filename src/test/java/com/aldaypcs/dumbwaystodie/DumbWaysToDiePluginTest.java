package com.aldaypcs.dumbwaystodie;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DumbWaysToDiePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DumbWaysToDiePlugin.class);
		RuneLite.main(args);
	}
}
