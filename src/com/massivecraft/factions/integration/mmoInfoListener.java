package com.massivecraft.factions.integration;

import java.util.HashMap;

import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.Widget;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;

import mmo.Core.MMOListener;
import mmo.Core.InfoAPI.MMOInfoEvent;

public class mmoInfoListener extends MMOListener {
	private P plugin;
	private HashMap<SpoutPlayer,GenericLabel> labels;
	public mmoInfoListener(P plug_in)
	{
		plugin = plug_in;
		labels = new HashMap<SpoutPlayer,GenericLabel>();
	}
	public GenericLabel updateDisplay(SpoutPlayer splayer)
	{
		FPlayer player = FPlayers.i.get(splayer);
		FLocation here = new FLocation(player);
		Faction factionHere = Board.getFactionAt(here);
		String tag = factionHere.getTag(player);
		GenericLabel Text;
		if(!labels.containsKey(splayer) && labels.get(splayer) == null)
		{
			Text = new GenericLabel();
			Text.setResize(true);
			Text.setFixed(true);
			labels.put(splayer, Text);
		}
		else
		{
			Text = labels.get(splayer);
		}
		String msg = tag + " - " + Math.round(Board.getInfluence(here));
		Text.setText(msg);
		Text.setDirty(true);
		return Text;
	}
	public void onMMOInfo(MMOInfoEvent event){
		if(event.isToken("factions"))
		{
			event.setWidget(plugin, this.updateDisplay(event.getPlayer()));
		}
	}
}
