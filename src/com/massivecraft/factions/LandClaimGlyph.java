package com.massivecraft.factions;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.zcore.util.DiscUtil;
import com.precipicegames.exp.ExperienceGiver;
import com.precipicegames.glyphgate.Glyph;

public class LandClaimGlyph implements Glyph{
	private File file;
	
	
	private HashMap<FLocation,Integer> glyphmap = new HashMap<FLocation,Integer>();
	private ExperienceGiver g;


	public glyphAnnounceEvent e1;


	public glyphActivateEvent e2;


	public glyphDeactivateEvent e3;


	public boolean active;
	
	
	public Map<String,Map<String,String>> dumpAsSaveFormat()
	{
		Map<String,Map<String,String>> worldCoordIds = new HashMap<String,Map<String,String>>(); 

		String worldName, coords;
		String id;

		for (Entry<FLocation, Integer> entry : glyphmap.entrySet())
		{
			worldName = entry.getKey().getWorldName();
			coords = entry.getKey().getCoordString();
			id = Integer.toString(entry.getValue());
			if ( ! worldCoordIds.containsKey(worldName))
			{
				worldCoordIds.put(worldName, new TreeMap<String,String>());
			}

			worldCoordIds.get(worldName).put(coords, id);
		}

		return worldCoordIds;
	}
	public void loadFromSaveFormat(Map<String,Map<String,String>> worldCoordIds)
	{
		glyphmap.clear();
		
		String worldName, glyphDetail;
		String[] coords;
		int x, z;
		
		for (Entry<String,Map<String,String>> entry : worldCoordIds.entrySet())
		{
			worldName = entry.getKey();
			for (Entry<String,String> entry2 : entry.getValue().entrySet())
			{
				coords = entry2.getKey().trim().split("[,\\s]+");
				x = Integer.parseInt(coords[0]);
				z = Integer.parseInt(coords[1]);
				glyphDetail = entry2.getValue();
				int i = Integer.parseInt(glyphDetail);
				glyphmap.put(new FLocation(worldName, x, z), new Integer(i));
			}
		}
	}
	public boolean save()
	{
		//Factions.log("Saving board to disk");
		
		try
		{
			DiscUtil.write(file, P.p.gson.toJson(dumpAsSaveFormat()));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			P.p.log("Failed to save the glyphs to disk.");
			return false;
		}
		
		return true;
	}
	
	public boolean load()
	{
		P.p.log("Loading glyphs from disk");
		
		if ( ! file.exists())
		{
			P.p.log("No glyphs to load from disk. Creating new file.");
			save();
			return true;
		}
		
		try
		{
			Type type = new TypeToken<Map<String,Map<String,String>>>(){}.getType();
			Map<String,Map<String,String>> worldGlyphIds = P.p.gson.fromJson(DiscUtil.read(file), type);
			loadFromSaveFormat(worldGlyphIds);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			P.p.log("Failed to load the glyphs from disk.");
			return false;
		}
			
		return true;
	}
	LandClaimGlyph()
	{
		file = new File(P.p.getDataFolder(), "glyphs.json");
		g =  (ExperienceGiver) P.p.getServer().getPluginManager().getPlugin("ExperienceGiver");
		this.e1 = new glyphAnnounceEvent(this);
		this.e2 = new glyphActivateEvent(this);
		this.e3 = new glyphDeactivateEvent(this);
		P.p.getServer().getScheduler().scheduleSyncRepeatingTask(P.p,e1, 1*60*20, 30*60*20);
		active = false;
	}
	protected class glyphAnnounceEvent implements Runnable
	{
		LandClaimGlyph plugin;
		public glyphAnnounceEvent(LandClaimGlyph landClaimGlyph) {
			plugin = landClaimGlyph;
		}

		@Override
		public void run() {
			P.p.getServer().broadcastMessage("The Land-Claim Glyphs will activate in ~5 mins");
			P.p.getServer().getScheduler().scheduleSyncDelayedTask(P.p, e2, 5*60*20);
		}
		
	}
	protected class glyphActivateEvent implements Runnable
	{
		LandClaimGlyph plugin;
		public glyphActivateEvent(LandClaimGlyph landClaimGlyph) {
			plugin = landClaimGlyph;
		}

		public void run()
		{
			plugin.active = true;
			P.p.getServer().broadcastMessage("The Land-Claim Glyphs are now active for 40s");
			//P.p.getServer().broadcastMessage("They will deactivate in ~40 seconds");
			P.p.getServer().getScheduler().scheduleSyncDelayedTask(P.p, plugin.e3, 40*20);
		}
	}
	protected class glyphDeactivateEvent implements Runnable
	{
		LandClaimGlyph plugin;
		public glyphDeactivateEvent(LandClaimGlyph landClaimGlyph) {
			plugin = landClaimGlyph;
		}


		@Override
		public void run() {
			plugin.active = false;
			P.p.getServer().broadcastMessage("The Land-Claim Glyphs are no longer active!");
		}
		
	}
	private static int[][] matrix = {
			{35, 0, 76, 0,35},
			{0,-1,-1,-1,0},
			{76,-1,-1,-1,76},
			{0,-1,-1,-1,0},
			{35, 0, 76, 0,35}
	};

	public void activate(Player caster, Location loc) {
		//System.out.println("Attempting to activate rune");
		if(!this.active)
		{
			caster.sendMessage("This glyph cannot be used at this time");
			caster.sendMessage("This glyph can be used every 35 minutes");
			return;
		}
		
		if(Conf.worldsNoClaiming.contains(loc.getWorld().getName()))
			return;
		
		FLocation l = new FLocation(loc);
		Integer id = this.glyphmap.get(l);
		if(id != null)
		{
			switch(id.intValue())
			{
			case -1:
				addInfluence(caster, l);
				break;
			default:
				initialize(caster, l);
			}
		}
		else
		{
			initialize(caster,l);
		}
	}

	private void initialize(Player caster, FLocation l) {
		if(g==null)
		{
			System.out.println("Experience System not found)");
			return;
		}
		Faction f = Board.getFactionAt(l);
		if(f.isNone())
		{
			if(g.check(caster) >= Conf.influenceGlyphWildernessMininmum/Conf.influencePerXP)
			{
				FPlayer fp = FPlayers.i.get(caster);
				Faction factionPlayer = fp.getFaction();
				if(factionPlayer == null)
				{
					caster.sendMessage("You must be a member of a faction to activate this glyph");
					return;
				}
				Board.setFactionAt(factionPlayer, l);
				Board.setInfluence(l, Conf.influenceConvertedStart);
				g.remove(caster, (int) (Conf.influenceGlyphWildernessMininmum/Conf.influencePerXP));
				glyphmap.put(l, new Integer(-1));
				caster.sendMessage("Land claim glyph activated successfully");
			}
			else
			{
				caster.sendMessage("You need at least " + Conf.influenceGlyphWildernessMininmum/Conf.influencePerXP + "xp to activate the glyph here");
			}
		}
		if(f.getId().startsWith("-"))
		{
			caster.sendMessage("You cannot place an infuence glyph here!");
		}
		if(f.isNormal())
		{
			if(f.getOnlinePlayers().contains(caster))
			{
				if(g.check(caster) >= Conf.influenceGlyphMininmum/Conf.influencePerXP)
				{
					FPlayer fp = FPlayers.i.get(caster);
					if(!fp.isInOwnTerritory())
					{
						caster.sendMessage("You must be a member of " + f.getTag() + " to activate this glyph");
						return;
					}
					g.remove(caster, (int) (Conf.influenceGlyphMininmum/Conf.influencePerXP));
					glyphmap.put(l, new Integer(-1));
				}
				else
				{
					caster.sendMessage("You need at least " + Conf.influenceGlyphMininmum/Conf.influencePerXP + "xp to activate the glyph here");
				}
			}
			else
			{
				caster.sendMessage("You cannot place an infuence glyph here!");
			}
		}
	}
	private void addInfluence(Player caster, FLocation l) {
		Faction f = Board.getFactionAt(l);
		if(f.isNormal())
		{
			int xp = g.check(caster);
			if(xp >= 1)
			{
				Board.setInfluence(l, Board.getInfluence(l) + Conf.influencePerXP);
				g.remove(caster, 1);
				FPlayer fp = FPlayers.i.get(caster);
				if(fp != null)
					fp.sendFactionHeaderMessage();
			}
			else
			{
				caster.sendMessage("You do not have xp to place in this glyph");
			}
		}
		else
		{
			glyphmap.remove(l);
		}
		// TODO Auto-generated method stub
		
	}
	public int[][] getRuneMatrix() {
		return matrix;
	}

}

