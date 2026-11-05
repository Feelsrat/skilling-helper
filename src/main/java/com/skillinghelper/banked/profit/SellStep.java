package com.skillinghelper.banked.profit;

import com.skillinghelper.data.Banks;
import com.skillinghelper.data.Target;
import com.skillinghelper.engine.Format;
import com.skillinghelper.engine.GuideStep;
import com.skillinghelper.engine.ItemHighlight;
import com.skillinghelper.engine.PlayerState;
import com.skillinghelper.engine.Route;
import com.skillinghelper.engine.StepType;

/**
 * The last part of a profit run: take what was made to the Grand Exchange and sell it, explained for someone who has
 * never used the GE.
 */
public final class SellStep
{
	public static final Target CLERK = Target.npc("Exchange", "Grand Exchange Clerk");

	private SellStep()
	{
	}

	/**
	 * The next selling step, or null when the player has none of the product left (all sold or in GE offers).
	 *
	 * @param notedId the product's noted id, or -1 when it has none
	 * @param each    gold one product sells for after tax, for the expected total
	 */
	public static GuideStep next(PlayerState s, int productId, int notedId, String productName, int each)
	{
		int carried = s.carried(productId) + (notedId > 0 ? s.carried(notedId) : 0);
		int banked = s.bankCount(productId);
		if (carried <= 0 && banked <= 0)
		{
			return null;
		}
		String name = productName.toLowerCase();
		boolean atGe = Banks.GRAND_EXCHANGE.contains(s.getLocation());
		long total = (long) (carried + banked) * each;
		if (banked > 0)
		{
			GuideStep.GuideStepBuilder b = GuideStep.builder()
				.type(StepType.BANK)
				.title("Take your " + name + " out of the bank as notes" + (atGe ? "" : " at the Grand Exchange"))
				.highlightBank(true);
			if (!atGe)
			{
				b.travel(Route.to(s, Banks.GRAND_EXCHANGE));
				b.line("Walk to the Grand Exchange in north-west Varrock (follow the arrow). It has bankers in the "
					+ "middle.");
			}
			b.line("1. Click a banker or bank booth (outlined) and choose Bank.");
			b.line("2. At the bottom of the bank, click \"Note\" so everything comes out as one stack.");
			b.line(String.format("3. Click the %s (outlined) to take out all %,d.", name, banked));
			b.line("4. Close the bank. Next you sell them to a clerk.");
			b.withdraw(new int[]{productId}, 0);
			return b.build();
		}
		GuideStep.GuideStepBuilder b = GuideStep.builder()
			.type(StepType.GET_ITEMS)
			.title("Sell your " + name + " at the Grand Exchange")
			.highlight(CLERK)
			.highlightBank(false);
		if (!atGe)
		{
			b.travel(Route.to(s, Banks.GRAND_EXCHANGE));
			b.line("Walk to the Grand Exchange in north-west Varrock (follow the arrow).");
		}
		b.line("1. Click a Grand Exchange Clerk (outlined) and choose Exchange.");
		b.line("2. Click the " + name + " in your inventory (outlined): that opens a sell offer for all of them.");
		b.line("3. Press \"-5%\" once so it sells straight away, then Confirm.");
		b.line("4. When the offer bar turns green, click it and collect your coins.");
		b.line("You should get about " + Format.gp(total) + " after the 2% GE tax.");
		b.itemHighlight(productId, ItemHighlight.USE);
		if (notedId > 0)
		{
			b.itemHighlight(notedId, ItemHighlight.USE);
		}
		return b.build();
	}
}
