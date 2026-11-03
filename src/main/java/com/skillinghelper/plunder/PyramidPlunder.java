package com.skillinghelper.plunder;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemHighlight;
import com.skillinghelper.engine.StepType;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.gameval.ItemID;

/**
 * The Pyramid Plunder round plan and the one thing to click next. Kept free of client references so it can be unit
 * tested. Strategy from the OSRS wiki "Pyramid Plunder" page: in the early rooms only pass the trap (and open the
 * gold chest from room 4, or everywhere at low levels), search every urn in the last two rooms you can enter, keep 90
 * seconds for the last room, open chests just before leaving a room, use a snake charm below 51 Thieving and carry a
 * lockpick for the doors.
 */
public final class PyramidPlunder
{
	public static final String METHOD_ID = "thieve_pyramid_plunder";
	/**
	 * The eight plunder rooms.
	 */
	public static final int REGION = 7749;
	public static final int ROOMS = 8;
	/**
	 * A round lasts five minutes.
	 */
	public static final int ROUND_TICKS = 500;
	/**
	 * Time to keep for the last room, as the wiki suggests (90 seconds), plus a little for finding the door.
	 */
	public static final int LAST_ROOM_TICKS = 150 + 20;
	/**
	 * Below this the snake charm makes urns faster; above it checking for snakes only slows you down.
	 */
	public static final int SNAKE_CHARM_BELOW = 51;
	/**
	 * Below this a lockpick is clearly worth it: doors open far more often, for half the door xp.
	 */
	public static final int LOCKPICK_BELOW = 71;
	/**
	 * The correct way into the pyramid changes every 25 minutes.
	 */
	public static final long ENTRANCE_SECONDS = 25 * 60;

	static final int[] URN_XP = {60, 90, 150, 215, 300, 450, 675, 825};
	static final int[] CHEST_XP = {40, 60, 100, 140, 200, 300, 450, 550};
	static final int[] DOOR_XP = {40, 60, 100, 140, 200, 300, 450, 0};

	public static final Location ALI = Location.of("Ali the Snake Charmer (Pollnivneach)", 3355, 2953, 4);
	public static final Target MONEY_POT = Target.object(null, "Money pot");
	public static final Target MUMMY = Target.npc("Start-minigame", "Guardian mummy");

	public static final int[] SCEPTRES = {
		ItemID.NTK_JEWELLED_SCEPTRE_1, ItemID.NTK_JEWELLED_SCEPTRE_2, ItemID.NTK_JEWELLED_SCEPTRE_3,
		ItemID.NTK_JEWELLED_SCEPTRE_4, ItemID.NTK_JEWELLED_SCEPTRE_5, ItemID.NTK_JEWELLED_SCEPTRE_6,
		ItemID.NTK_JEWELLED_SCEPTRE_7, ItemID.NTK_JEWELLED_SCEPTRE_8,
	};
	public static final int UNCHARGED_SCEPTRE = ItemID.NTK_JEWELLED_SCEPTRE_0;
	public static final int[] GOLD_ARTEFACTS = {ItemID.NTK_SEAL_GOLD, ItemID.NTK_SCARAB_GOLD,
		ItemID.NTK_STATUETTE_GOLD};
	public static final int[] STONE_ARTEFACTS = {ItemID.NTK_SEAL_STONE, ItemID.NTK_SCARAB_STONE,
		ItemID.NTK_STATUETTE_STONE};
	public static final int[] POTTERY_ARTEFACTS = {ItemID.NTK_IVORY_COMB, ItemID.NTK_SCARAB_POTTERY,
		ItemID.NTK_STATUETTE_POTTERY};
	public static final int SNAKE_CHARM = ItemID.SNAKE_FLUTE;
	public static final int LOCKPICK = ItemID.LOCKPICK;

	private PyramidPlunder()
	{
	}

	public enum UrnState
	{
		CLOSED,
		SNAKE,
		CHARMED,
		OPEN,
	}

	/**
	 * What to click next. Each comes with the object the overlay highlights.
	 */
	public enum Action
	{
		/**
		 * Outside: search the pyramid door that leads to the Guardian mummy.
		 */
		ENTER,
		/**
		 * In one of the three empty rooms behind a wrong door: leave through its door.
		 */
		LEAVE_EMPTY_ROOM,
		/**
		 * In the lobby: start a round with the Guardian mummy.
		 */
		START,
		PASS_TRAP,
		CHECK_URN,
		CHARM_URN,
		SEARCH_URN,
		OPEN_CHEST,
		OPEN_SARCOPHAGUS,
		SEARCH_SARCOPHAGUS,
		PICK_DOOR,
		LEAVE,
	}

	/**
	 * What to loot in one room.
	 */
	@Value
	public static class RoomPlan
	{
		int room;
		boolean urns;
		boolean chest;
		boolean sarcophagus;
		/**
		 * The last room you can enter: leave once it's done.
		 */
		boolean last;
	}

	/**
	 * Everything the next action depends on during a round.
	 */
	@Value
	@Builder
	public static class Situation
	{
		int level;
		int room;
		int ticksLeft;
		boolean trapActive;
		/**
		 * Urns in this room not searched yet.
		 */
		int urnsLeft;
		/**
		 * The urn the overlay picked to do next, or null when there's none in view.
		 */
		UrnState nextUrn;
		boolean chestClosed;
		boolean sarcophagusClosed;
		/**
		 * Opened but not searched yet.
		 */
		boolean sarcophagusOpen;
		int doorsClosed;
		boolean carriesCharm;
		boolean carriesLockpick;
		boolean exitInView;
		boolean inventoryFull;
		boolean sceptreHunt;
		int prayerLevel;
	}

	/**
	 * The deepest room this Thieving level opens: room 1 at 21, one more every 10 levels, room 8 at 91.
	 */
	public static int maxRoom(int level)
	{
		if (level < 21)
		{
			return 0;
		}
		return Math.min(ROOMS, 1 + (level - 21) / 10);
	}

	public static int roomLevel(int room)
	{
		return 21 + (room - 1) * 10;
	}

	public static boolean useCharm(int level)
	{
		return level < SNAKE_CHARM_BELOW;
	}

	public static boolean useLockpick(int level)
	{
		return level < LOCKPICK_BELOW;
	}

	/**
	 * What to loot in {@code room} for a player whose deepest room is {@code maxRoom}. Urns only pay off in the last
	 * two rooms; chests are instant, so take them from room 4 on, in the urn rooms, and everywhere while the deepest
	 * room is still low. Hunting the sceptre adds every chest and the sarcophagi from room 4.
	 */
	public static RoomPlan plan(int room, int maxRoom, boolean sceptreHunt)
	{
		boolean urns = room >= maxRoom - 1;
		boolean chest = urns || room >= 4 || maxRoom <= 4 || sceptreHunt;
		boolean sarcophagus = sceptreHunt && room >= 4;
		return new RoomPlan(room, urns, chest, sarcophagus, room >= maxRoom);
	}

	/**
	 * The next thing to click inside a round.
	 */
	public static Action decide(Situation s)
	{
		int maxRoom = Math.max(1, maxRoom(s.level));
		RoomPlan plan = plan(s.room, maxRoom, s.sceptreHunt);
		if (s.trapActive)
		{
			return Action.PASS_TRAP;
		}
		// Keep 90 seconds for the last room: in the room before it, move on when time gets short
		boolean timeHere = plan.last || s.ticksLeft > LAST_ROOM_TICKS;
		if (plan.urns && timeHere && s.urnsLeft > 0 && s.nextUrn != null && s.nextUrn != UrnState.OPEN)
		{
			if (useCharm(s.level) && s.carriesCharm)
			{
				switch (s.nextUrn)
				{
					case CLOSED:
						return Action.CHECK_URN;
					case SNAKE:
						return Action.CHARM_URN;
					default:
						break;
				}
			}
			return Action.SEARCH_URN;
		}
		// Chests and sarcophagi go last: a scarab swarm or mummy is easier to leave behind on the way out
		if (plan.chest && s.chestClosed)
		{
			return Action.OPEN_CHEST;
		}
		if (plan.sarcophagus && s.sarcophagusOpen)
		{
			return Action.SEARCH_SARCOPHAGUS;
		}
		if (plan.sarcophagus && s.sarcophagusClosed && timeHere)
		{
			return Action.OPEN_SARCOPHAGUS;
		}
		return plan.last ? Action.LEAVE : Action.PICK_DOOR;
	}

	/**
	 * The panel step for a round: what to click, why, and the plan for the whole round.
	 */
	public static GuideStep step(Situation s, Action a)
	{
		int maxRoom = Math.max(1, maxRoom(s.level));
		int r = Math.max(1, Math.min(ROOMS, s.room));
		GuideStep.GuideStepBuilder b = GuideStep.builder().type(StepType.TRAIN);
		String time = "Room " + r + " of " + maxRoom + ", " + timeLeft(s.ticksLeft) + " left.";
		switch (a)
		{
			case PASS_TRAP:
				b.title("Pass the speartrap");
				b.line("Click the outlined speartrap: \"Pass\" it before walking on, or it hits you.");
				break;
			case CHECK_URN:
				b.title("Check the urn for snakes");
				b.line("Click \"Check for Snakes\" on the outlined urn.");
				b.line("Next: \"Charm Snake\", then \"Search\". If the check fails, just check again.");
				b.line("Why: below " + SNAKE_CHARM_BELOW + " Thieving the charm saves failed searches and poison, "
					+ "for the same xp.");
				break;
			case CHARM_URN:
				b.title("Charm the snake");
				b.line("Click \"Charm Snake\" on the outlined urn, then \"Search\" it.");
				break;
			case SEARCH_URN:
				b.title("Search the urn");
				b.line("Click \"Search\" on the outlined urn (" + URN_XP[r - 1] + " xp). " + s.urnsLeft
					+ (s.urnsLeft == 1 ? " urn" : " urns") + " left in this room.");
				if (s.nextUrn == UrnState.CLOSED && useCharm(s.level) && !s.carriesCharm)
				{
					b.line("Tip: a snake charm from Ali in Pollnivneach makes urns safer below "
						+ SNAKE_CHARM_BELOW + " Thieving.");
				}
				else if (s.nextUrn == UrnState.CLOSED && !useCharm(s.level))
				{
					b.line("Just search: at your level checking for snakes is slower than the odd bite.");
				}
				break;
			case OPEN_CHEST:
				b.title("Open the gold chest");
				b.line("Click \"Search\" on the outlined Grand Gold Chest (" + CHEST_XP[r - 1] + " xp).");
				b.line(s.prayerLevel >= 43
					? "Tip: turn on Protect from Melee first in case scarabs come out."
					: "If scarabs come out, move on: they only follow you in this room.");
				break;
			case OPEN_SARCOPHAGUS:
				b.title("Open the sarcophagus");
				b.line("Click \"Open\" on the outlined sarcophagus: Strength xp and a sceptre chance.");
				b.line("A mummy can rise out of it. Pick a door straight after.");
				break;
			case SEARCH_SARCOPHAGUS:
				b.title("Search the sarcophagus");
				b.line("Click \"Search\" on the outlined sarcophagus for its artefact and sceptre roll.");
				break;
			case PICK_DOOR:
				b.title("Pick the tomb door");
				b.line("Click \"Pick-lock\" on the outlined Tomb Door (" + (s.carriesLockpick
					? DOOR_XP[r - 1] / 2 + " xp with your lockpick" : DOOR_XP[r - 1] + " xp") + ").");
				b.line("One door goes on; if it's a dead end the next door gets outlined. After three wrong doors the "
					+ "fourth always works.");
				if (!s.carriesLockpick && useLockpick(s.level))
				{
					b.line("Tip: bring a lockpick next round: doors open much more often at your level.");
				}
				if (!plan(r, maxRoom, s.sceptreHunt).last && plan(r + 1, maxRoom, s.sceptreHunt).last
					&& s.ticksLeft <= LAST_ROOM_TICKS && s.urnsLeft > 0)
				{
					b.line("Time to move on: the last room is worth more than these urns.");
				}
				break;
			case LEAVE:
			default:
				b.title("Leave and start a new round");
				b.line(s.exitInView ? "Right-click the outlined exit Tomb Door and \"Quick-leave\"."
					: "Leave through any exit Tomb Door (\"Quick-leave\"), or teleport out.");
				b.line("A fresh round restarts the 5-minute timer, which beats waiting this one out.");
				break;
		}
		if (s.inventoryFull)
		{
			b.line("Warning: your inventory is full. Drop a pottery or stone artefact (or eat) to keep looting; "
				+ "keep gold ones and ivory combs, they're worth the most.");
		}
		b.line(time);
		for (String line : roundPlan(maxRoom, r, s.sceptreHunt))
		{
			b.line(line);
		}
		return b.build();
	}

	/**
	 * One line per group of rooms with the same plan, marking the rooms already done.
	 */
	public static List<String> roundPlan(int maxRoom, int currentRoom, boolean sceptreHunt)
	{
		List<String> out = new ArrayList<>();
		int start = 1;
		while (start <= maxRoom)
		{
			RoomPlan first = plan(start, maxRoom, sceptreHunt);
			int end = start;
			while (end < maxRoom && !first.urns && !first.last && same(first, plan(end + 1, maxRoom, sceptreHunt)))
			{
				end++;
			}
			String marker = currentRoom > end ? "✔ " : currentRoom >= start ? "➜ " : "• ";
			String rooms = start == end ? "Room " + start : "Rooms " + start + "-" + end;
			out.add(marker + rooms + ": " + describe(first, maxRoom));
			start = end + 1;
		}
		return out;
	}

	private static boolean same(RoomPlan a, RoomPlan b)
	{
		return a.urns == b.urns && a.chest == b.chest && a.sarcophagus == b.sarcophagus && a.last == b.last;
	}

	static String describe(RoomPlan p, int maxRoom)
	{
		List<String> parts = new ArrayList<>();
		parts.add("trap");
		if (p.urns)
		{
			parts.add("every urn");
		}
		if (p.chest)
		{
			parts.add("chest");
		}
		if (p.sarcophagus)
		{
			parts.add("sarcophagus");
		}
		parts.add(p.last ? "leave" : "door");
		String text = String.join(", then ", parts) + ".";
		if (p.urns && !p.last)
		{
			text += " Keep 90 seconds for room " + maxRoom + ".";
		}
		return text;
	}

	/**
	 * "4:05" style time left.
	 */
	public static String timeLeft(int ticks)
	{
		int seconds = Math.max(0, ticks) * 6 / 10;
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}

	// ---- Before a round ----

	/**
	 * The player is in the lobby: start a round.
	 */
	public static GuideStep startStep(int level, boolean carriesCharm, boolean carriesLockpick, boolean sceptreHunt,
		boolean canCharge)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.type(StepType.TRAIN)
			.title("Start a round")
			.highlight(MUMMY)
			.line("Right-click the Guardian mummy and \"Start-minigame\".");
		if (canCharge)
		{
			b.line("First: \"Talk-to\" the mummy to charge your Pharaoh's sceptre with artefacts. Then it teleports "
				+ "you straight back here each round.");
			b.itemHighlight(UNCHARGED_SCEPTRE, ItemHighlight.USE);
		}
		prepLines(b, level, carriesCharm, carriesLockpick);
		for (String line : roundPlan(Math.max(1, maxRoom(level)), 0, sceptreHunt))
		{
			b.line(line);
		}
		return b.build();
	}

	/**
	 * Outside the pyramid in Sophanem: teleport in with a sceptre, or search the door that leads to the mummy.
	 *
	 * @param knownDoor whether the right door is known (seen within the last 25 minutes)
	 */
	public static GuideStep enterStep(int level, boolean carriesCharm, boolean carriesLockpick, int sceptreId,
		boolean knownDoor)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder().type(StepType.TRAIN);
		if (sceptreId > 0)
		{
			b.title("Teleport in with the sceptre")
				.line("Rub your Pharaoh's sceptre and pick Jalsavrah: it puts you next to the Guardian mummy.")
				.itemHighlight(sceptreId, ItemHighlight.USE);
		}
		else if (knownDoor)
		{
			b.title("Enter the pyramid")
				.line("\"Search\" the outlined door: it led to the Guardian mummy last time.");
		}
		else
		{
			b.title("Find the way in")
				.line("\"Search\" the outlined pyramid door. Only one of the four leads to the Guardian mummy.")
				.line("If you land in an empty room, leave and the next door gets outlined. The right door stays the "
					+ "same for about 25 minutes.");
		}
		prepLines(b, level, carriesCharm, carriesLockpick);
		return b.build();
	}

	/**
	 * Behind a wrong door: nothing here, so leave and try the next door.
	 */
	public static GuideStep emptyRoomStep()
	{
		return GuideStep.builder()
			.type(StepType.TRAIN)
			.title("Wrong door: leave this room")
			.line("This room is empty. Click the outlined door to leave, then search the next outlined pyramid door.")
			.line("Leaving always puts you outside the north door.")
			.build();
	}

	private static void prepLines(GuideStep.GuideStepBuilder b, int level, boolean carriesCharm,
		boolean carriesLockpick)
	{
		if (useCharm(level) && !carriesCharm)
		{
			b.line("Bring: a snake charm. Below " + SNAKE_CHARM_BELOW + " Thieving it makes urns faster and safer.");
		}
		if (useLockpick(level) && !carriesLockpick)
		{
			b.line("Bring: a lockpick. Doors open far more often at your level, for half the door xp.");
		}
	}

	/**
	 * An uncharged sceptre can be charged by the Guardian mummy for 6 gold, 12 stone or 24 pottery/ivory artefacts.
	 */
	public static boolean canCharge(boolean hasUnchargedSceptre, int gold, int stone, int pottery)
	{
		return hasUnchargedSceptre && (gold >= 6 || stone >= 12 || pottery >= 24);
	}

	/**
	 * What to do with the artefacts, added when banking them.
	 */
	public static List<String> artefactLines()
	{
		return List.of(
			"Money: sell the artefacts on the Grand Exchange. It pays about the same as Simon Templeton and is "
				+ "quicker; ivory combs are worth much more there.",
			"Tip: with a Pharaoh's sceptre, keep some to charge it: 6 gold, 12 stone or 24 pottery/ivory "
				+ "artefacts per charge, from the Guardian mummy.");
	}

	/**
	 * Below 51 Thieving without a snake charm anywhere: Ali in Pollnivneach gives one for a coin, on the way.
	 */
	public static boolean needsCharm(int level, boolean bankKnown, int owned)
	{
		return useCharm(level) && bankKnown && owned == 0;
	}

	public static GuideStep charmSourceStep(boolean atAli)
	{
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.type(atAli ? StepType.GET_ITEMS : StepType.WALK)
			.title(atAli ? "Get a snake charm" : "Get a snake charm from Ali")
			.highlight(MONEY_POT)
			.line("Use 1 coin on the Money pot next to Ali the Snake Charmer, south of the bar in Pollnivneach. He "
				+ "gives you a snake charm and a snake basket.")
			.line("Below " + SNAKE_CHARM_BELOW + " Thieving the charm stops urn snakes biting, which saves time and "
				+ "food. Pollnivneach is on the carpet route to Sophanem.");
		return b.build();
	}
}
