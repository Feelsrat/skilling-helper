package com.skillinghelper.thieving;

import com.skillinghelper.data.Location;
import com.skillinghelper.data.Target;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Route;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Live steps for Stealing artefacts in Port Piscarilius: talk to Captain Khaled, rob the house he names, hand it in.
 * Which value of {@link VarbitID#PISCARILIUS_BURGLARY_TARGET} means which house follows the Stealing Artefacts hub
 * plugin; house spots are from the wiki's map.
 */
public final class ArtefactHeist
{
	public static final String METHOD_ID = "thieve_artefacts";
	public static final int TASK_VARBIT = VarbitID.PISCARILIUS_BURGLARY_TARGET;

	static final int NO_TASK = 0;
	static final int FAILED = 7;
	static final int DELIVER = 8;

	static final Location KHALED = Location.of("Captain Khaled (Port Piscarilius foodhall)", 1845, 3752, 4);

	static final int[] ARTEFACTS = {ItemID.PISCARILIUS_STOLEN_PENDANT, ItemID.PISCARILIUS_STOLEN_RING,
		ItemID.PISCARILIUS_STOLEN_CIRCLET, ItemID.PISCARILIUS_STOLEN_HEIRLOOM, ItemID.PISCARILIUS_STOLEN_BOX};

	private static final Target KHALED_NPC = Target.npc(null, "Captain Khaled");
	private static final Target PATROLS = Target.npc(null, "Patrolman", "Patrolwoman");
	private static final Target DRAWERS = Target.object(null, "Drawers");
	private static final Target LADDER = Target.object(null, "Ladder");

	enum House
	{
		NORTH(1, "northern", 1768, 3751, false),
		SOUTH_EAST(2, "south-eastern", 1774, 3733, true),
		SOUTH(3, "southern", 1764, 3732, true),
		SOUTH_WEST(4, "south-western", 1749, 3734, true),
		WEST(5, "western", 1750, 3750, true),
		NORTH_WEST(6, "north-western", 1749, 3758, true);

		final int task;
		final String name;
		final Location place;
		final boolean upstairs;

		House(int task, String name, int x, int y, boolean upstairs)
		{
			this.task = task;
			this.name = name;
			// Any floor, so the arrow stays put once the player climbs the ladder
			this.place = Location.area("the " + name + " house", x, y, 4);
			this.upstairs = upstairs;
		}

		static House of(int task)
		{
			for (House h : values())
			{
				if (h.task == task)
				{
					return h;
				}
			}
			return null;
		}
	}

	/**
	 * The step for where the player is in the heist. Only replaces the normal "train" step; banking and walking to
	 * Port Piscarilius stay as they are.
	 */
	public static GuideStep step(GuideStep base, PlayerState s, int task)
	{
		boolean carrying = s.carried(ARTEFACTS) > 0;
		House house = House.of(task);
		GuideStep.GuideStepBuilder b = base.toBuilder().clearDetails().clearHighlights();
		if (carrying || task == DELIVER)
		{
			b.title("Take the artefact back to Captain Khaled")
				.line("You have the artefact. Walk back to Captain Khaled in the foodhall on the east side of the port, "
					+ "following the arrow. Don't teleport: that loses the artefact.")
				.line("Keep away from the outlined patrolmen and patrolwomen. If one walks towards you, wait out of "
					+ "their sight until they turn away, then go.")
				.line("Talk to Captain Khaled to hand it in, then ask him for another house.")
				.highlight(KHALED_NPC)
				.highlight(PATROLS);
			return withRoute(b, s, KHALED).build();
		}
		if (house == null)
		{
			b.title(task == FAILED ? "You were caught: get a new house" : "Get a house from Captain Khaled")
				.line("Captain Khaled is in the foodhall on the east side of Port Piscarilius (outlined). Follow the "
					+ "arrow to him.")
				.line("Talk to him and ask for a task. He names one of six houses on the docks in the south-west of "
					+ "the port; the guide then points you to it.")
				.line("Have your lockpick in your inventory and run energy left (drink a stamina potion if it's low).")
				.highlight(KHALED_NPC);
			return withRoute(b, s, KHALED).build();
		}
		b.title("Rob the " + house.name + " house")
			.line("Follow the arrow to the " + house.name + " house. The outlined patrolmen and patrolwomen walk set "
				+ "routes: if one can see you near a house, you get caught and have to start over.")
			.line("Stop just out of sight and wait until the nearest patrol has walked past and is facing away, then "
				+ "run in.")
			.highlight(PATROLS)
			.highlight(DRAWERS);
		if (house.upstairs)
		{
			b.line("The drawers are upstairs: climb the ladder inside the house, then click the outlined drawers "
				+ "(\"Search\") to pick the lock.")
				.highlight(LADDER);
		}
		else
		{
			b.line("The drawers are on the ground floor: click the outlined drawers (\"Search\") to pick the lock.");
		}
		b.line("You get the artefact and 750 xp. The guide then leads you back to Captain Khaled.");
		return withRoute(b, s, house.place).build();
	}

	private static GuideStep.GuideStepBuilder withRoute(GuideStep.GuideStepBuilder b, PlayerState s, Location place)
	{
		WorldPoint me = s.getLocation();
		return b.route(place.contains(me) ? null : Route.to(s, place));
	}

	private ArtefactHeist()
	{
	}
}
