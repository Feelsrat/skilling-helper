package com.skillinghelper.unlocks.data;

import com.skillinghelper.diary.data.DiaryRegion;
import com.skillinghelper.diary.data.DiaryTask;
import com.skillinghelper.diary.data.DiaryTier;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Quest;

/**
 * One key account unlock (fairy rings, graceful, rune pouch...): what it gives, how the plugin tells it's unlocked,
 * and the way to get it. The way is, in order: the quests (handed to Quest Helper), a diary tier (handed to the
 * Achievement Diary guide), points to earn (handed to Skilling Helper), then the guided last part ({@link #guide}).
 */
@Value
@Builder
public class Unlock
{
	String id;
	String name;
	UnlockCategory category;
	/**
	 * What it gives you, one perk per line.
	 */
	@Singular("gives")
	List<String> gives;
	String wikiPage;
	@Builder.Default
	boolean members = true;

	// ---- How it's detected (any one that's set and holds) ----

	/**
	 * Unlocked once this quest is finished.
	 */
	Quest doneQuest;
	/**
	 * Unlocked once this varbit is at least {@link #doneVarbitMin}.
	 */
	int doneVarbit;
	@Builder.Default
	int doneVarbitMin = 1;
	/**
	 * Unlocked once the player owns one item of every group (any colour or upgrade counts), in the bank, inventory or
	 * worn.
	 */
	@Singular("own")
	List<int[]> own;
	/**
	 * For several {@link #own} groups bought one by one (graceful pieces): what each costs in the
	 * {@link #grind} currency, so the amount still to earn leaves out pieces already owned. Empty otherwise.
	 */
	int[] pieceCosts;

	// ---- How to get it ----

	@Singular
	List<QuestPart> quests;
	/**
	 * The diary tier whose reward this is: unlocked once the reward is claimed, guided by the diary plugin.
	 */
	DiaryRegion diaryRegion;
	DiaryTier diaryTier;
	Grind grind;
	/**
	 * The last part, guided step by step: requirements, what to bring and where to go. Null when the quest or diary
	 * is the whole thing.
	 */
	DiaryTask guide;

	public boolean isDiary()
	{
		return diaryRegion != null && diaryTier != null;
	}
}
