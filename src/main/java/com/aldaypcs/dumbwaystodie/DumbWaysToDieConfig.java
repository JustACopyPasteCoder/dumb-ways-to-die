package com.aldaypcs.dumbwaystodie;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("dumbwaystodie")
public interface DumbWaysToDieConfig extends Config
{
	@Range(max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "volume",
		name = "Volume",
		description = "How loud the death sound plays. 0 mutes it."
	)
	default int volume()
	{
		return 70;
	}
}
