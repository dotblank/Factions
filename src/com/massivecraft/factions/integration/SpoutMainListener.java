package com.massivecraft.factions.integration;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;

import org.getspout.spoutapi.event.spout.SpoutCraftEnableEvent;
import org.getspout.spoutapi.event.spout.SpoutListener;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.player.SpoutPlayer;
import org.getspout.spoutapi.SpoutManager;
//import org.getspout.spoutapi.gui.WidgetAnchor;


public class SpoutMainListener extends SpoutListener
{
	@Override
	public void onSpoutCraftEnable(SpoutCraftEnableEvent event)
	{
		final FPlayer me = FPlayers.i.get(event.getPlayer());

		SpoutFeatures.updateAppearances(me.getPlayer());
		updateTerritoryDisplay(me);
	}


	//-----------------------------------------------------------------------------------------//
	// Everything below this is handled in here to prevent errors on servers not running Spout
	//-----------------------------------------------------------------------------------------//

	private transient static Map<String, GenericLabel> territoryLabels = new HashMap<String, GenericLabel>();
	private transient static Map<String, NoticeLabel> territoryChangeLabels = new HashMap<String, NoticeLabel>();
	private transient static Map<String, GenericLabel> ownerLabels = new HashMap<String, GenericLabel>();
	private final static int SCREEN_WIDTH = 427;
//	private final static int SCREEN_HEIGHT = 240;


	public boolean updateTerritoryDisplay(FPlayer player)
	{
		Player p = player.getPlayer();
		if (p == null)
			return false;

		SpoutPlayer sPlayer = SpoutManager.getPlayer(p);
		if (!sPlayer.isSpoutCraftEnabled() || (Conf.spoutTerritoryDisplaySize <= 0 && ! Conf.spoutTerritoryNoticeShow))
			return false;

		doLabels(player, sPlayer);

		return true;
	}
	public boolean updateInfluenceDisplay(FPlayer player)
	{
		Player p = player.getPlayer();
		if (p == null)
			return false;

		SpoutPlayer sPlayer = SpoutManager.getPlayer(p);
		if (!sPlayer.isSpoutCraftEnabled() || (Conf.spoutTerritoryDisplaySize <= 0 && ! Conf.spoutTerritoryNoticeShow))
			return false;

		this.doHeader(player, sPlayer);

		return true;
	}

	public void updateOwnerList(FPlayer player)
	{
		SpoutPlayer sPlayer = SpoutManager.getPlayer(player.getPlayer());
		if (!sPlayer.isSpoutCraftEnabled() || (Conf.spoutTerritoryDisplaySize <= 0 && ! Conf.spoutTerritoryNoticeShow))
			return;

		FLocation here = new FLocation(player);
		Faction factionHere = Board.getFactionAt(here);

		doOwnerList(player, sPlayer, here, factionHere);
		

		return;
	}

	public void removeTerritoryLabels(String playerName)
	{
		territoryLabels.remove(playerName);
		territoryChangeLabels.remove(playerName);
		ownerLabels.remove(playerName);
	}
	
	private void doHeader(FPlayer player, SpoutPlayer sPlayer)
	{
		P.p.mmoListener.updateDisplay(sPlayer);
		FLocation here = new FLocation(player);
		Faction factionHere = Board.getFactionAt(here);
		String tag = factionHere.getTag(player);
		// ----------------------
		// Main territory display
		// ----------------------
		if (Conf.spoutTerritoryDisplayPosition > 0 && Conf.spoutTerritoryDisplaySize > 0 && Conf.spoutTerritoryNoticeTopShow)
		{
			GenericLabel label; 
			if (territoryLabels.containsKey(player.getName()))
				label = territoryLabels.get(player.getName());
			else
			{
				label = new GenericLabel();
				label.setScale(Conf.spoutTerritoryDisplaySize);
/*				// this should work once the Spout team fix it to account for text scaling; we can then get rid of alignLabel method added below
				switch (Conf.spoutTerritoryDisplayPosition) {
					case 1: label.setAlign(WidgetAnchor.TOP_LEFT).setAnchor(WidgetAnchor.TOP_LEFT); break;
					case 2: label.setAlign(WidgetAnchor.TOP_CENTER).setAnchor(WidgetAnchor.TOP_CENTER); break;
					default: label.setAlign(WidgetAnchor.TOP_RIGHT).setAnchor(WidgetAnchor.TOP_RIGHT);
				}
 */
				sPlayer.getMainScreen().attachWidget(P.p, label);
				territoryLabels.put(player.getName(), label);
			}

			String msg = tag + " - " + Math.round(Board.getInfluence(here));

			if (Conf.spoutTerritoryDisplayShowDescription && !factionHere.getDescription().isEmpty())
				msg += " - " + factionHere.getDescription();

			label.setText(msg);
			alignLabel(label, msg);
			label.setDirty(true);
		}
	}


	private void doLabels(FPlayer player, SpoutPlayer sPlayer)
	{
		doHeader(player,sPlayer);
		FLocation here = new FLocation(player);
		Faction factionHere = Board.getFactionAt(here);
		String tag = factionHere.getTag(player);

		// -----------------------
		// Fading territory notice
		// -----------------------
		if (Conf.spoutTerritoryNoticeShow && Conf.spoutTerritoryNoticeSize > 0)
		{
			NoticeLabel label; 
			if (territoryChangeLabels.containsKey(player.getName()))
				label = territoryChangeLabels.get(player.getName());
			else
			{
				label = new NoticeLabel(Conf.spoutTerritoryNoticeLeaveAfterSeconds);
				label.setScale(Conf.spoutTerritoryNoticeSize);
				label.setY(Conf.spoutTerritoryNoticeTop);
				sPlayer.getMainScreen().attachWidget(P.p, label);
				territoryChangeLabels.put(player.getName(), label);
			}

			String msg = tag;

			if (Conf.spoutTerritoryNoticeShowDescription && !factionHere.getDescription().isEmpty())
				msg += " - " + factionHere.getDescription();

			label.setText(msg);
			alignLabel(label, msg, 2);
			label.resetNotice();
			label.setDirty(true);
		}

		// and owner list, of course
		doOwnerList(player, sPlayer, here, factionHere);
	}
	
	private void doOwnerList(FPlayer player, SpoutPlayer sPlayer, FLocation here, Faction factionHere)
	{
		// ----------
		// Owner list
		// ----------
		if (Conf.spoutTerritoryDisplayPosition > 0 && Conf.spoutTerritoryDisplaySize > 0 && Conf.spoutTerritoryOwnersShow && Conf.ownedAreasEnabled)
		{
			GenericLabel label; 
			if (ownerLabels.containsKey(player.getName()))
				label = ownerLabels.get(player.getName());
			else
			{
				label = new GenericLabel();
				label.setScale(Conf.spoutTerritoryDisplaySize);
				label.setY((int)(10 * Conf.spoutTerritoryDisplaySize));
				sPlayer.getMainScreen().attachWidget(P.p, label);
				ownerLabels.put(player.getName(), label);
			}

			String msg = "";

			if (player.getFaction() == factionHere)
			{
				msg = factionHere.getOwnerListString(here);

				if (!msg.isEmpty())
					msg = Conf.ownedLandMessage + msg;
			}

			label.setText(msg);
			alignLabel(label, msg);
			label.setDirty(true);
		}
	}


	// this is only necessary because Spout text size scaling is currently bugged and breaks their built-in alignment methods
	public void alignLabel(GenericLabel label, String text)
	{
		alignLabel(label, text, Conf.spoutTerritoryDisplayPosition);
	}
	public void alignLabel(GenericLabel label, String text, int alignment)
	{
		int labelWidth = (int)((float)GenericLabel.getStringWidth(text) * Conf.spoutTerritoryDisplaySize);
		if (labelWidth > SCREEN_WIDTH)
		{
				label.setX(0);
				return;
		}

		switch (alignment)
		{
			case 1:		// left aligned
				label.setX(0);
				break;
			case 2:		// center aligned
				label.setX((SCREEN_WIDTH - labelWidth) / 2);
				break;
			default:	// right aligned
				label.setX(SCREEN_WIDTH - labelWidth);
		}
	}


	private static class NoticeLabel extends GenericLabel
	{
		private int initial;
		private int countdown;  // current delay countdown

		public NoticeLabel(float secondsOfLife)
		{
			initial = (int)(secondsOfLife * 20);
			resetNotice();
		}

		public final void resetNotice()
		{
			countdown = initial;
		}

		@Override
		public void onTick()
		{
			if (countdown <= 0)
				return;

			this.countdown -= 1;

			if (this.countdown <= 0)
			{
				this.setText("");
				this.setDirty(true);
			}
		}
	}
}