package com.skillinghelper;

import com.skillinghelper.data.Target;
import com.skillinghelper.engine.SlayerTask;
import com.skillinghelper.engine.SlayerTaskStep;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Reads the current Slayer task from the game, the same way RuneLite's Slayer plugin does, and keeps the task's
 * monsters tracked for highlighting. Runs on the client thread.
 */
@Singleton
class SlayerTaskReader
{
	/**
	 * The SLAYER_TARGET value for boss tasks, whose boss is in SLAYER_TARGET_BOSSID.
	 */
	private static final int BOSS_TASK = 98;
	/**
	 * How close the task's monsters need to be to count as found.
	 */
	private static final int NEARBY_TILES = 15;

	private final Client client;
	private final TargetTracker tracker;

	private int nameKey = -1;
	private String name;
	private int areaKey = -1;
	private String areaName;

	@Inject
	SlayerTaskReader(Client client, TargetTracker tracker)
	{
		this.client = client;
		this.tracker = tracker;
	}

	/**
	 * The current task, or null when there's none.
	 */
	SlayerTask read(WorldPoint me)
	{
		int remaining = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
		if (remaining <= 0)
		{
			tracker.setTaskTarget(null);
			return null;
		}
		int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		int areaId = client.getVarpValue(VarPlayerID.SLAYER_AREA);
		String task = taskName(taskId);
		Target target = task == null ? null : SlayerTaskStep.target(task);
		tracker.setTaskTarget(target);
		return SlayerTask.builder()
			.taskId(taskId)
			.name(task)
			.remaining(remaining)
			.areaId(areaId)
			.areaName(areaName(areaId))
			.masterId(client.getVarbitValue(VarbitID.SLAYER_MASTER))
			.points(client.getVarbitValue(VarbitID.SLAYER_POINTS))
			.monstersNearby(target != null && tracker.hasNpcNear(target, me, NEARBY_TILES))
			.build();
	}

	private String taskName(int taskId)
	{
		int key = taskId == BOSS_TASK ? -2 - client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID) : taskId;
		if (key != nameKey)
		{
			nameKey = key;
			name = lookUpTaskName(taskId);
		}
		return name;
	}

	private String lookUpTaskName(int taskId)
	{
		int row;
		if (taskId == BOSS_TASK)
		{
			List<Integer> bossRows = client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID,
				DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0, client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
			if (bossRows.isEmpty())
			{
				return null;
			}
			row = (Integer) client.getDBTableField(bossRows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
		}
		else
		{
			List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, taskId);
			if (rows.isEmpty())
			{
				return null;
			}
			row = rows.get(0);
		}
		return (String) client.getDBTableField(row, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0)[0];
	}

	private String areaName(int areaId)
	{
		if (areaId != areaKey)
		{
			areaKey = areaId;
			areaName = null;
			if (areaId > 0)
			{
				List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerArea.ID, DBTableID.SlayerArea.COL_AREA_ID, 0,
					areaId);
				if (!rows.isEmpty())
				{
					areaName = (String) client.getDBTableField(rows.get(0), DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER,
						0)[0];
				}
			}
		}
		return areaName;
	}
}
