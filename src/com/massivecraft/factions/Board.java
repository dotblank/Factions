package com.massivecraft.factions;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.bukkit.ChatColor;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.util.AsciiCompass;
import com.massivecraft.factions.zcore.util.DiscUtil;


public class Board
{

	private static transient File file = new File(P.p.getDataFolder(), "board.json");
	private static transient HashMap<FLocation, Claim> flocationIds = new HashMap<FLocation, Claim>();
	
	//----------------------------------------------//
	// Get and Set
	//----------------------------------------------//
	public static String getIdAt(FLocation flocation)
	{
		if ( ! flocationIds.containsKey(flocation))
		{
			return "0";
		}
		
		return flocationIds.get(flocation).Ids;
	}
	
	public static Faction getFactionAt(FLocation flocation)
	{
		return Factions.i.get(getIdAt(flocation));
	}
	
	public static void setIdAt(String id, FLocation flocation)
	{
		clearOwnershipAt(flocation);

		if (id == "0")
		{
			removeAt(flocation);
		}
		Claim c = flocationIds.get(flocation);
		c = (c == null) ? new Claim() : c;
		c.Ids = id;
		flocationIds.put(flocation, c);
	}
	
	public static void setFactionAt(Faction faction, FLocation flocation)
	{
		setIdAt(faction.getId(), flocation);
	}
	
	public static void removeAt(FLocation flocation)
	{
		clearOwnershipAt(flocation);
		flocationIds.remove(flocation);
	}
	
	// not to be confused with claims, ownership referring to further member-specific ownership of a claim
	public static void clearOwnershipAt(FLocation flocation)
	{
		Faction faction = getFactionAt(flocation);
		if (faction != null && faction.isNormal())
		{
			faction.clearClaimOwnership(flocation);
		}
	}
	
	public static void unclaimAll(String factionId)
	{
		Faction faction = Factions.i.get(factionId);
		if (faction != null && faction.isNormal())
		{
			faction.clearAllClaimOwnership();
		}

		Iterator<Entry<FLocation, Claim>> iter = flocationIds.entrySet().iterator();
		while (iter.hasNext())
		{
			Entry<FLocation, Claim> entry = iter.next();
			if (entry.getValue().Ids.equals(factionId))
			{
				iter.remove();
			}
		}
	}

	// Is this coord NOT completely surrounded by coords claimed by the same faction?
	// Simpler: Is there any nearby coord with a faction other than the faction here?
	public static boolean isBorderLocation(FLocation flocation)
	{
		Faction faction = getFactionAt(flocation);
		FLocation a = flocation.getRelative(1, 0);
		FLocation b = flocation.getRelative(-1, 0);
		FLocation c = flocation.getRelative(0, 1);
		FLocation d = flocation.getRelative(0, -1);
		return faction != getFactionAt(a) || faction != getFactionAt(b) || faction != getFactionAt(c) || faction != getFactionAt(d);
	}

	// Is this coord connected to any coord claimed by the specified faction?
	public static boolean isConnectedLocation(FLocation flocation, Faction faction)
	{
		FLocation a = flocation.getRelative(1, 0);
		FLocation b = flocation.getRelative(-1, 0);
		FLocation c = flocation.getRelative(0, 1);
		FLocation d = flocation.getRelative(0, -1);
		return faction == getFactionAt(a) || faction == getFactionAt(b) || faction == getFactionAt(c) || faction == getFactionAt(d);
	}
	
	
	//----------------------------------------------//
	// Cleaner. Remove orphaned foreign keys
	//----------------------------------------------//
	
	public static void clean()
	{
		Iterator<Entry<FLocation, Claim>> iter = flocationIds.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<FLocation, Claim> entry = iter.next();
			if ( ! Factions.i.exists(entry.getValue().Ids))
			{
				P.p.log("Board cleaner removed "+entry.getValue()+" from "+entry.getKey());
				iter.remove();
			}
		}
	}	
	
	//----------------------------------------------//
	// Coord count
	//----------------------------------------------//
	
	public static int getFactionCoordCount(String factionId)
	{
		int ret = 0;
		for (Claim thatFactionId : flocationIds.values())
		{
			if(thatFactionId.Ids.equals(factionId))
			{
				ret += 1;
			}
		}
		return ret;
	}
	
	public static int getFactionCoordCount(Faction faction)
	{
		return getFactionCoordCount(faction.getId());
	}
	
	public static int getFactionCoordCountInWorld(Faction faction, String worldName)
	{
		String factionId = faction.getId();
		int ret = 0;
		Iterator<Entry<FLocation, Claim>> iter = flocationIds.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<FLocation, Claim> entry = iter.next();
			if (entry.getValue().Ids.equals(factionId) && entry.getKey().getWorldName().equals(worldName))
			{
				ret += 1;
			}
		}
		return ret;
	}
	
	//----------------------------------------------//
	// Map generation
	//----------------------------------------------//
	
	/**
	 * The map is relative to a coord and a faction
	 * north is in the direction of decreasing x
	 * east is in the direction of decreasing z
	 */
	public static ArrayList<String> getMap(Faction faction, FLocation flocation, double inDegrees)
	{
		ArrayList<String> ret = new ArrayList<String>();
		Faction factionLoc = getFactionAt(flocation);
		ret.add(P.p.txt.titleize("("+flocation.getCoordString()+") "+factionLoc.getTag(faction)));
		
		int halfWidth = Conf.mapWidth / 2;
		int halfHeight = Conf.mapHeight / 2;
		FLocation topLeft = flocation.getRelative(-halfHeight, halfWidth);
		int width = halfWidth * 2 + 1;
		int height = halfHeight * 2 + 1;
		
		if (Conf.showMapFactionKey)
		{
			height--;
		}
		
		Map<String, Character> fList = new HashMap<String, Character>();
		int chrIdx = 0;
		
		// For each row
		for (int dx = 0; dx < height; dx++) {
			// Draw and add that row
			String row = "";
			for (int dz = 0; dz > -width; dz--) {
				if(dz == -(halfWidth) && dx == halfHeight) {
					row += ChatColor.AQUA+"+";
				} else {
					FLocation flocationHere = topLeft.getRelative(dx, dz);
					Faction factionHere = getFactionAt(flocationHere);
					Relation relation = faction.getRelationTo(factionHere);
					if (factionHere.isNone()) {
						row += ChatColor.GRAY+"-";
					} else if (factionHere.isSafeZone()) {
						row += ChatColor.GOLD+"+";
					} else if (factionHere.isWarZone()) {
						row += ChatColor.DARK_RED+"+";
					} else if (
						   factionHere == faction
						|| factionHere == factionLoc
						|| relation.isAtLeast(Relation.ALLY)
						|| (Conf.showNeutralFactionsOnMap && relation.equals(Relation.NEUTRAL))
						|| (Conf.showEnemyFactionsOnMap && relation.equals(Relation.ENEMY))
						) {
						if (!fList.containsKey(factionHere.getTag()))
							fList.put(factionHere.getTag(), Conf.mapKeyChrs[chrIdx++]);
						char tag = fList.get(factionHere.getTag());
						row += factionHere.getRelationTo(faction).getColor() + "" + tag;
					} else {
						row += ChatColor.GRAY+"-";
					}
				}
			}
			ret.add(row);
		}
		
		// Get the compass
		ArrayList<String> asciiCompass = AsciiCompass.getAsciiCompass(inDegrees, ChatColor.RED, P.p.txt.parse("<a>"));

		// Add the compass
		ret.set(1, asciiCompass.get(0)+ret.get(1).substring(3*3));
		ret.set(2, asciiCompass.get(1)+ret.get(2).substring(3*3));
		ret.set(3, asciiCompass.get(2)+ret.get(3).substring(3*3));
		
		// Add the faction key
		if (Conf.showMapFactionKey) {
			String fRow = "";
			for(String key : fList.keySet()) {
				fRow += String.format("%s%s: %s ", ChatColor.GRAY, fList.get(key), key);
			}
			ret.add(fRow);
		}
		
		return ret;
	}
	
	
	// -------------------------------------------- //
	// Persistance
	// -------------------------------------------- //
	
	public static Map<String,Map<String,String>> dumpAsSaveFormat() {
		Map<String,Map<String,String>> worldCoordIds = new HashMap<String,Map<String,String>>(); 
		
		String worldName, coords;
		String id;
		
		for (Entry<FLocation, Claim> entry : flocationIds.entrySet()) {
			worldName = entry.getKey().getWorldName();
			coords = entry.getKey().getCoordString();
			id = entry.getValue().getClaimString();
			if ( ! worldCoordIds.containsKey(worldName)) {
				worldCoordIds.put(worldName, new TreeMap<String,String>());
			}
			
			worldCoordIds.get(worldName).put(coords, id);
		}
		
		return worldCoordIds;
	}
	public static void setInfluence(FLocation l, double influenceConvertedStart)
	{
		Claim c = flocationIds.get(l);
		c = (c == null) ? new Claim() : c;
		c.Power = influenceConvertedStart;
		flocationIds.put(l, c);
	}
	public static double getInfluence(FLocation l)
	{
		Claim c = flocationIds.get(l);
		c = (c == null) ? new Claim() : c;
		return c.Power;
	}
	public static void loadFromSaveFormat(Map<String,Map<String,String>> worldCoordIds)
	{
		flocationIds.clear();
		
		String worldName;
		String[] coords, claimDetail;
		int x, z;
		
		for (Entry<String,Map<String,String>> entry : worldCoordIds.entrySet())
		{
			worldName = entry.getKey();
			for (Entry<String,String> entry2 : entry.getValue().entrySet())
			{
				coords = entry2.getKey().trim().split("[,\\s]+");
				x = Integer.parseInt(coords[0]);
				z = Integer.parseInt(coords[1]);
				claimDetail = entry2.getValue().trim().split("[,\\s]+");
				Claim c  = new Claim();
				c.Ids = claimDetail[0];
				c.Power = Double.parseDouble(claimDetail[1]);
				flocationIds.put(new FLocation(worldName, x, z), c);
			}
		}
	}
	
	public static boolean save()
	{
		//Factions.log("Saving board to disk");
		
		try
		{
			DiscUtil.write(file, P.p.gson.toJson(dumpAsSaveFormat()));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			P.p.log("Failed to save the board to disk.");
			return false;
		}
		
		return true;
	}
	public static boolean calculateclaim(Claim parent, Claim child, int time)
	{
		double orate = child.Power/Conf.influenceConversionRate;
		double crate = parent.Power/Conf.influenceConversionRate;
		double sign;
		boolean changed = false;
		//System.out.println("Conversion test " + child.Power);
		if(orate<crate && Math.abs(parent.Power-child.Power) > Conf.influenceDelta)
		{
			//Do not convert safe or war zones
			if(parent.Ids.startsWith("-") || child.Ids.startsWith("-"))
				return false;
			// Let decay convert instead
			if(parent.Ids.equals("0"))
				return false;
			//Contribute power as opposed to removing
			if(parent.Ids.equalsIgnoreCase(child.Ids))
				sign = 1;
			else
				sign = -1;
			
			child.Power = child.Power + sign*((crate-orate)*(double)time);
			
			//The Land Claim was overturned
			if(child.Power <= 0)
			{
				child.Ids = parent.Ids;
				child.Power = Conf.influenceConvertedStart;
			}
			changed = true;
		}
		return changed;
	}
	public static void UpdateClaims(int time)
	{
		if(!Conf.enableInfluenceSystem)
			return;
		
		double crate = 0.0;
		FLocation flc = new FLocation();
		Claim claim;
		HashMap<FLocation, Claim> changedIDs = new HashMap<FLocation, Claim>();
		changedIDs.putAll(flocationIds);
		for(Entry<FLocation, Claim> entry : flocationIds.entrySet())
		{
			crate = entry.getValue().Power/Conf.influenceConversionRate;
			if(crate <= 0)
				continue;

			flc.setWorldName(entry.getKey().getWorldName());
			
			// Go +Z
			flc.setZ((int)entry.getKey().getZ()+1);
			flc.setX((int)entry.getKey().getX());
			claim = changedIDs.get(flc);
			if(claim == null)
			{
				claim = new Claim();
			}
			if(calculateclaim(entry.getValue(),claim,time))
			{
				FLocation tf = new FLocation();
				tf.setX((int) flc.getX());
				tf.setZ((int) flc.getZ());
				tf.setWorldName(flc.getWorldName());
				changedIDs.put(tf, claim);
			}
			
			
			//go -Z
			flc.setZ((int)entry.getKey().getZ()-1);
			flc.setX((int)entry.getKey().getX());
			claim = changedIDs.get(flc);
			if(claim == null)
			{
				claim = new Claim();
			}
			if(calculateclaim(entry.getValue(),claim,time))
			{
				FLocation tf = new FLocation();
				tf.setX((int) flc.getX());
				tf.setZ((int) flc.getZ());
				tf.setWorldName(flc.getWorldName());
				changedIDs.put(tf, claim);
			}
			
			//go +X
			flc.setZ((int)entry.getKey().getZ());
			flc.setX((int)entry.getKey().getX()+1);
			claim = changedIDs.get(flc);
			if(claim == null)
			{
				claim = new Claim();
			}
			if(calculateclaim(entry.getValue(),claim,time))
			{
				FLocation tf = new FLocation();
				tf.setX((int) flc.getX());
				tf.setZ((int) flc.getZ());
				tf.setWorldName(flc.getWorldName());
				changedIDs.put(tf, claim);
			}
			
			//go -X
			flc.setZ((int)entry.getKey().getZ());
			flc.setX((int)entry.getKey().getX()-1);
			claim = changedIDs.get(flc);
			if(claim == null)
			{
				claim = new Claim();
			}
			if(calculateclaim(entry.getValue(),claim,time))
			{
				FLocation tf = new FLocation();
				tf.setX((int) flc.getX());
				tf.setZ((int) flc.getZ());
				tf.setWorldName(flc.getWorldName());
				changedIDs.put(tf, claim);
			}
			
			
			if(getFactionAt(entry.getKey()).isNormal())
			{
				entry.getValue().Power -= (Conf.influenceFactionDecay + (Conf.influenceFactionExpDecay* Math.pow(entry.getValue().Power,1.9))*(double)time);
				if(entry.getValue().Power <= 0)
				{
					Claim c = new Claim();
					changedIDs.put(entry.getKey(), c);
				}
			}
			else if(getFactionAt(entry.getKey()).isNone())
			{
				if(entry.getValue().Power < 500)
				{
					entry.getValue().Power += Conf.influenceFactionDecay * time;
					changedIDs.put(entry.getKey(), entry.getValue());
				}
				else if (entry.getValue().Power < 500)
				{
					entry.getValue().Power = 500;
					changedIDs.put(entry.getKey(), entry.getValue());
				}
			}
			  
		}
		//Snap in additional chunks
		flocationIds.putAll(changedIDs);
		
		//update players chunk data
		for(FPlayer p : FPlayers.i.get())
		{
			p.sendFactionHeaderMessage();
		}
	}
	
	public static boolean load()
	{
		P.p.log("Loading board from disk");
		
		if ( ! file.exists())
		{
			P.p.log("No board to load from disk. Creating new file.");
			save();
			return true;
		}
		
		try
		{
			Type type = new TypeToken<Map<String,Map<String,String>>>(){}.getType();
			Map<String,Map<String,String>> worldCoordIds = P.p.gson.fromJson(DiscUtil.read(file), type);
			loadFromSaveFormat(worldCoordIds);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			P.p.log("Failed to load the board from disk.");
			return false;
		}
			
		return true;
	}

	/*private static boolean loadOld() {
		File folderBoard = new File(P.p.getDataFolder(), "board");

		if ( ! folderBoard.isDirectory())
			return false;

		P.log("Board file doesn't exist, attempting to load old pre-1.1 data.");

		String ext = ".json";

		class jsonFileFilter implements FileFilter {
			@Override
			public boolean accept(File file) {
				return (file.getName().toLowerCase().endsWith(".json") && file.isFile());
			}
		}

		File[] jsonFiles = folderBoard.listFiles(new jsonFileFilter());
		for (File jsonFile : jsonFiles) {
			// Extract the name from the filename. The name is filename minus ".json"
			String name = jsonFile.getName();
			name = name.substring(0, name.length() - ext.length());
			try {
				JsonParser parser = new JsonParser();
				JsonObject json = (JsonObject) parser.parse(DiscUtil.read(jsonFile));
				JsonArray coords = json.getAsJsonArray("coordFactionIds");
				Iterator<JsonElement> coordSet = coords.iterator();
				while(coordSet.hasNext()) {
					JsonArray coordDat = (JsonArray) coordSet.next();
					JsonObject coord = coordDat.get(0).getAsJsonObject();
					int coordX = coord.get("x").getAsInt();
					int coordZ = coord.get("z").getAsInt();
					int factionId = coordDat.get(1).getAsInt();
					flocationIds.put(new FLocation(name, coordX, coordZ), factionId);
				}
				P.log("loaded pre-1.1 board "+name);
			} catch (Exception e) {
				e.printStackTrace();
				P.log(Level.WARNING, "failed to load board "+name);
			}
		}
		return true;
	}*/
}



















