package com.massivecraft.factions.iface;

import org.bukkit.ChatColor;

import com.massivecraft.factions.struct.Relation;

public interface RelationParticipator
{
	public String describeTo(RelationParticipator that);
	public String describeTo(RelationParticipator that, boolean ucfirst);
	
	public Relation getRelationTo(RelationParticipator that);
	public Relation getRelationTo(RelationParticipator that, boolean ignorePeaceful);
	
	public ChatColor getRelationColor(RelationParticipator to);
}
