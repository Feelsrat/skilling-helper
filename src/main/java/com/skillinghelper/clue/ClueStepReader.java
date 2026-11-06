package com.skillinghelper.clue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.cluescrolls.ClueScrollPlugin;
import net.runelite.client.plugins.cluescrolls.clues.AnagramClue;
import net.runelite.client.plugins.cluescrolls.clues.CipherClue;
import net.runelite.client.plugins.cluescrolls.clues.ClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.CoordinateClue;
import net.runelite.client.plugins.cluescrolls.clues.CrypticClue;
import net.runelite.client.plugins.cluescrolls.clues.EmoteClue;
import net.runelite.client.plugins.cluescrolls.clues.FairyRingClue;
import net.runelite.client.plugins.cluescrolls.clues.FaloTheBardClue;
import net.runelite.client.plugins.cluescrolls.clues.HotColdClue;
import net.runelite.client.plugins.cluescrolls.clues.LocationClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.LocationsClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.MapClue;
import net.runelite.client.plugins.cluescrolls.clues.MusicClue;
import net.runelite.client.plugins.cluescrolls.clues.NamedObjectClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.NpcClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.ObjectClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.SkillChallengeClue;
import net.runelite.client.plugins.cluescrolls.clues.ThreeStepCrypticClue;
import net.runelite.client.plugins.cluescrolls.clues.emote.Emote;
import net.runelite.client.plugins.cluescrolls.clues.item.ItemRequirement;

/**
 * Reads the step the player is on from RuneLite's built-in Clue Scroll plugin, which already knows every clue's
 * answer, spot, NPC, object, emote and items. That plugin has to be on (it works out the clue when the player reads
 * it); this guide then turns the step into its own beginner instructions. Client thread.
 */
@Slf4j
class ClueStepReader
{
	private final Client client;
	private final PluginManager pluginManager;
	private ClueScrollPlugin cluePlugin;
	private boolean broken;

	ClueStepReader(Client client, PluginManager pluginManager)
	{
		this.client = client;
		this.pluginManager = pluginManager;
	}

	/**
	 * The current step, or null when the Clue Scroll plugin is off or hasn't identified a clue (it needs the clue
	 * read once).
	 *
	 * @param bank last-seen bank, item id to amount, or null
	 */
	ClueStep read(WorldPoint me, Map<Integer, Integer> bank)
	{
		if (broken)
		{
			return null;
		}
		try
		{
			ClueScrollPlugin plugin = plugin();
			if (plugin == null || !pluginManager.isPluginActive(plugin))
			{
				return null;
			}
			ClueScroll clue = plugin.getClue();
			return clue == null ? null : convert(clue, plugin, me, bank);
		}
		catch (RuntimeException | LinkageError e)
		{
			// The Clue Scroll plugin changed shape in a RuneLite update: fall back to the general guide
			log.debug("Couldn't read the clue from the Clue Scroll plugin", e);
			broken = true;
			return null;
		}
	}

	private ClueScrollPlugin plugin()
	{
		if (cluePlugin == null)
		{
			for (Plugin p : pluginManager.getPlugins())
			{
				if (p instanceof ClueScrollPlugin)
				{
					cluePlugin = (ClueScrollPlugin) p;
				}
			}
		}
		return cluePlugin;
	}

	private ClueStep convert(ClueScroll clue, ClueScrollPlugin plugin, WorldPoint me, Map<Integer, Integer> bank)
	{
		ClueStep.ClueStepBuilder b = ClueStep.builder()
			.spade(clue.isRequiresSpade())
			.light(clue.isRequiresLight())
			.enemy(clue.getEnemy() == null ? null : clue.getEnemy().getText());
		if (clue instanceof EmoteClue)
		{
			EmoteClue c = (EmoteClue) clue;
			b.type(ClueStep.Type.EMOTE).text(c.getText()).solution(c.getLocationName());
			for (Emote e : new Emote[]{c.getFirstEmote(), c.getSecondEmote()})
			{
				if (e != null)
				{
					b.emote(e.getName());
				}
			}
			if (c.getStashUnit() != null)
			{
				String stash = objectName(c.getStashUnit().getObjectId());
				b.stash(stash == null ? "STASH unit" : stash);
			}
			needs(b, c.getItemRequirements(), bank);
		}
		else if (clue instanceof CoordinateClue)
		{
			b.type(ClueStep.Type.COORDINATE).spade(true);
		}
		else if (clue instanceof MapClue)
		{
			MapClue c = (MapClue) clue;
			b.type(ClueStep.Type.MAP).solution(c.getDescription());
			if (c.getObjectId() <= 0)
			{
				b.spade(true);
			}
		}
		else if (clue instanceof CrypticClue)
		{
			CrypticClue c = (CrypticClue) clue;
			b.type(ClueStep.Type.CRYPTIC).text(c.getText()).solution(c.getSolution(plugin)).answer(c.getAnswer());
		}
		else if (clue instanceof AnagramClue)
		{
			AnagramClue c = (AnagramClue) clue;
			b.type(ClueStep.Type.ANAGRAM).text(c.getTextProvider().apply(plugin)).solution(c.getArea())
				.answer(c.getAnswerProvider() == null ? null : c.getAnswerProvider().apply(plugin));
		}
		else if (clue instanceof CipherClue)
		{
			CipherClue c = (CipherClue) clue;
			b.type(ClueStep.Type.CIPHER).text(c.getText()).answer(c.getAnswer())
				.solution(c.getAreaProvider() == null ? null : c.getAreaProvider().apply(plugin));
		}
		else if (clue instanceof MusicClue)
		{
			b.type(ClueStep.Type.MUSIC).song(((MusicClue) clue).getSong());
		}
		else if (clue instanceof FairyRingClue)
		{
			b.type(ClueStep.Type.FAIRY_RING).text(((FairyRingClue) clue).getText()).spade(true);
		}
		else if (clue instanceof HotColdClue)
		{
			HotColdClue c = (HotColdClue) clue;
			b.type(ClueStep.Type.HOT_COLD).text(c.getText()).solution(c.getSolution()).spade(true);
		}
		else if (clue instanceof SkillChallengeClue)
		{
			SkillChallengeClue c = (SkillChallengeClue) clue;
			b.type(ClueStep.Type.CHALLENGE).text(c.getChallenge()).solution(c.getReturnText());
			needs(b, c.getItemRequirements(), bank);
		}
		else if (clue instanceof FaloTheBardClue)
		{
			FaloTheBardClue c = (FaloTheBardClue) clue;
			b.type(ClueStep.Type.FALO).text(c.getText());
			needs(b, c.getItemRequirements(), bank);
		}
		else if (clue instanceof ThreeStepCrypticClue)
		{
			b.type(ClueStep.Type.THREE_STEP).text(((ThreeStepCrypticClue) clue).getText());
		}
		else
		{
			b.type(ClueStep.Type.OTHER);
		}

		List<WorldPoint> spots = new ArrayList<>();
		if (clue instanceof LocationsClueScroll)
		{
			WorldPoint[] all = ((LocationsClueScroll) clue).getLocations(plugin);
			if (all != null)
			{
				for (WorldPoint wp : all)
				{
					if (wp != null)
					{
						spots.add(wp);
					}
				}
			}
		}
		if (me != null)
		{
			spots.sort(Comparator.comparingInt(wp -> wp.distanceTo2D(me)));
		}
		spots.forEach(b::spot);
		WorldPoint location = null;
		if (clue instanceof LocationClueScroll)
		{
			location = ((LocationClueScroll) clue).getLocation(plugin);
		}
		else if (clue instanceof CrypticClue)
		{
			location = ((CrypticClue) clue).getLocation(plugin);
		}
		else if (clue instanceof AnagramClue)
		{
			location = ((AnagramClue) clue).getLocation(plugin);
		}
		else if (clue instanceof MapClue)
		{
			location = ((MapClue) clue).getLocation(plugin);
		}
		if (location == null && !spots.isEmpty())
		{
			location = spots.get(0);
		}
		b.location(location);
		if (clue instanceof NpcClueScroll)
		{
			String[] npcs = ((NpcClueScroll) clue).getNpcs(plugin);
			if (npcs != null)
			{
				for (String n : npcs)
				{
					if (n != null && !n.isEmpty())
					{
						b.npc(n);
					}
				}
			}
		}
		if (clue instanceof ObjectClueScroll)
		{
			int[] ids = ((ObjectClueScroll) clue).getObjectIds();
			if (ids != null)
			{
				for (int id : ids)
				{
					String name = objectName(id);
					if (name != null)
					{
						b.object(name);
					}
				}
			}
		}
		if (clue instanceof NamedObjectClueScroll)
		{
			String[] names = ((NamedObjectClueScroll) clue).getObjectNames();
			if (names != null)
			{
				for (String n : names)
				{
					b.object(n);
				}
			}
		}
		return b.build();
	}

	private String objectName(int id)
	{
		if (id <= 0)
		{
			return null;
		}
		ObjectComposition def = client.getObjectDefinition(id);
		if (def == null)
		{
			return null;
		}
		if (def.getImpostorIds() != null)
		{
			ObjectComposition impostor = def.getImpostor();
			if (impostor != null)
			{
				def = impostor;
			}
		}
		String name = def.getName();
		return name == null || name.isEmpty() || "null".equals(name) ? null : name;
	}

	private void needs(ClueStep.ClueStepBuilder b, ItemRequirement[] reqs, Map<Integer, Integer> bank)
	{
		if (reqs == null)
		{
			return;
		}
		Item[] inv = items(InventoryID.INV);
		Item[] worn = items(InventoryID.WORN);
		for (ItemRequirement r : reqs)
		{
			boolean have = r.fulfilledBy(inv) || r.fulfilledBy(worn);
			int bankId = 0;
			if (!have && bank != null)
			{
				for (int id : bank.keySet())
				{
					if (r.fulfilledBy(id))
					{
						bankId = id;
						break;
					}
				}
			}
			b.item(new ClueStep.Need(r.getCollectiveName(client), have, bankId));
		}
	}

	private Item[] items(int container)
	{
		ItemContainer c = client.getItemContainer(container);
		return c == null ? new Item[0] : c.getItems();
	}
}
