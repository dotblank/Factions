package com.massivecraft.factions;

public class Claim
{
	public String Ids = "0";
	public double Power = 500;
	public String getClaimString()
	{
		return Ids + "," + Double.toString(Power);
	}
}