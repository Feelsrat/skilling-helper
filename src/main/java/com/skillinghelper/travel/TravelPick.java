package com.skillinghelper.travel;

import lombok.Value;

/**
 * The travel item a guide suggests bringing to get to where it's going.
 */
@Value
public class TravelPick
{
	TravelOption option;
	/**
	 * The one item to bring: the one already carried, or else the one in the bank.
	 */
	int itemId;
	/**
	 * Already in the inventory or worn.
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

	public String itemName()
	{
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
	 * The guide line while the item is still in the bank.
	 */
	public String bankLine()
	{
		return "Travel: take your " + label() + ", outlined in its own colour. It gets you " + near()
			+ ", and Shortest Path uses it once it's in your inventory.";
	}

	/**
	 * The guide line while the item is carried.
	 */
	public String carriedLine()
	{
		return "Travel: use your " + label() + " to get " + near() + ". Shortest Path's route uses it.";
	}

	/**
	 * A reminder on a walking step while the item is still in the bank.
	 */
	public String tipLine()
	{
		return "Tip: your " + label() + " in the bank gets you " + near() + ". Take it next time you bank.";
	}

	private String near()
	{
		String to = destination == null ? "there" : destination;
		return landDistance <= 5 ? "right to " + to : "within " + landDistance + " tiles of " + to;
	}
}
