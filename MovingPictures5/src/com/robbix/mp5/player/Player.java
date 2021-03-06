package com.robbix.mp5.player;

import java.util.EnumMap;
import java.util.Map;

import com.robbix.mp5.map.ResourceType;
import com.robbix.mp5.unit.Cost;
import com.robbix.mp5.unit.Unit;
import com.robbix.utils.RColor;

public class Player
{
	private String name;
	private int id;
	private RColor color;
	
	private Map<ResourceType, Integer> resources;
	private Population population;
	
	public Player(int id, String name, RColor color)
	{
		this.id = id;
		this.name = name;
		this.color = color;
		this.resources = new EnumMap<ResourceType, Integer>(ResourceType.class);
	}
	
	public String getName()
	{
		return name;
	}
	
	public int getID()
	{
		return id;
	}
	
	public RColor getColor()
	{
		return color;
	}
	
	public boolean owns(Unit unit)
	{
		return equals(unit.getOwner());
	}
	
	public Map<ResourceType, Integer> getResources()
	{
		return resources;
	}
	
	public boolean canAfford(Cost cost)
	{
		for (ResourceType type : ResourceType.values())
		{
			int costAmount = cost.getAmount(type);
			Integer hasAmount = resources.get(type);
			
			if (costAmount == 0)
				continue;
			
			if (hasAmount == null)
				hasAmount = 0;
			
			if (hasAmount < costAmount)
				return false;
		}
		
		return true;
	}
	
	public void spend(Cost cost)
	{
		for (ResourceType type : ResourceType.values())
		{
			int costAmount = cost.getAmount(type);
			Integer hasAmount = resources.get(type);
			
			if (costAmount == 0)
				continue;
			
			if (hasAmount == null)
				hasAmount = 0;
			
			if (hasAmount < costAmount)
				throw new RuntimeException("can't afford it");
			
			resources.put(type, hasAmount - costAmount);
		}
	}
	
	public void addResource(ResourceType type, int amount)
	{
		Integer currentAmount = resources.get(type);
		
		if (currentAmount == null)
			currentAmount = 0;
		
		currentAmount += amount;
		resources.put(type, currentAmount);
	}
	
	public Population getPopulation()
	{
		return population;
	}
	
	public String getStatusString()
	{
		Integer commonOre = resources.get(ResourceType.COMMON_ORE);
		Integer rareOre = resources.get(ResourceType.RARE_ORE);
		
		if (commonOre == null)
			commonOre = 0;
		
		if (rareOre == null)
			rareOre = 0;
		
		return "[" + id + "] " + name + " " +
		commonOre + " common, " + rareOre + " rare";
	}
	
	public String toString()
	{
		return "Player " + id + ", \"" + name + "\"";
	}
}
