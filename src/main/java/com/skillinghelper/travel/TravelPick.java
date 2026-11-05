package com.skillinghelper.travel;

import lombok.Value;
import net.runelite.api.gameval.ItemID;

/**
 * Something a guide suggests bringing to get to where it's going: a travel item, or coins for a transport that costs
 * them.
 */
@Value
public class TravelPick
{
	TravelOption option;
	/**
	 * The one item to bring: the one already carried, or else the one in the bank. Coins for a paid transport.
	 */
	int itemId;
	/**
	 * Already in the inventory or worn (for coins: enough of them in the inventory).
	 */
	boolean carried;
	/**
	 * Tiles from where it lands to the destination, as the crow flies.
	 */
	int landDistance;
	/**
	 * Where the guide is heading, for the text.
	 */
	String destination;
	/**
	 * How many to take out of the bank: 1 for an item, the coins still missing for a paid transport.
	 */
	int amount;

	public TravelPick(TravelOption option, int itemId, boolean carried, int landDistance, String destination)
	{
		this(option, itemId, carried, landDistance, destination, 1);
	}

	public TravelPick(TravelOption option, int itemId, boolean carried, int landDistance, String destination,
		int amount)
	{
		this.option = option;
		this.itemId = itemId;
		this.carried = carried;
		this.landDistance = landDistance;
		this.destination = destination;
		this.amount = amount;
	}

	public boolean isCoins()
	{
		return itemId == ItemID.COINS;
	}

	public String itemName()
	{
		if (isCoins())
		{
			return "Coins";
		}
		return option.getKind() == TravelOption.Kind.FAIRY_RING ? TravelData.staffName(itemId) : option.getGroup();
	}

	/**
	 * "Skills necklace (Fishing Guild teleport)".
	 */
	public String label()
	{
		return itemName() + " (" + option.how() + ")";
	}

	/**
	 * The guide line while it's still in the bank.
	 */
	public String bankLine()
	{
		if (isCoins())
		{
			return "Travel: take " + String.format("%,d", amount) + " coins for the " + option.how()
				+ ", outlined in the travel colour. Shortest Path only uses it with the coins in your inventory.";
		}
		return "Travel: take your " + label() + ", outlined in its own colour. It gets you " + near()
			+ ", and Shortest Path uses it once it's in your inventory.";
	}

	/**
	 * The guide line while it's carried.
	 */
	public String carriedLine()
	{
		if (isCoins())
		{
			return "Travel: keep your coins for the " + option.how() + " (" + String.format("%,d", option.getCoins())
				+ " gp). Shortest Path's route uses it.";
		}
		return "Travel: use your " + label() + " to get " + near() + ". Shortest Path's route uses it.";
	}

	/**
	 * A reminder on a walking step while it's still in the bank.
	 */
	public String tipLine()
	{
		if (isCoins())
		{
			return "Tip: the " + option.how() + " costs " + String.format("%,d", option.getCoins())
				+ " gp. Take coins next time you bank and Shortest Path can use it.";
		}
		return "Tip: your " + label() + " in the bank gets you " + near() + ". Take it next time you bank.";
	}

	private String near()
	{
		String to = destination == null ? "there" : destination;
		return landDistance <= 5 ? "right to " + to : "within " + landDistance + " tiles of " + to;
	}
}
