package com.robbix.mp5.ui.overlay;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.robbix.mp5.Mediator;
import com.robbix.mp5.player.Player;
import com.robbix.mp5.unit.Unit;

public class SelectUnitOverlay extends InputOverlay
{
	public SelectUnitOverlay()
	{
		super.closesOnEscape = false;
	}
	
	public void dispose()
	{
		panel.showStatus((Unit)null);
	}
	
	public void paintOverUnits(Graphics g, Rectangle rect)
	{
		drawInstructions(g, rect, "Select");
		
		if (isDragging())
		{
			panel.draw(g, RED, getDragRegion());
		}
	}
	
	public void onLeftClick(int x, int y)
	{
		Unit selected = panel.getMap().getUnitAbsolute(x, y);
		
		if (selected != null)
		{
			Mediator.playSound(selected.getType().getAcknowledgement());
			push(selected.isTruck()
				? new CommandTruckOverlay(selected)
				: new CommandUnitOverlay(selected));
			panel.showStatus(selected);
			panel.showStatus(selected.getOwner());
		}
	}
	
	public void onAreaDragged(int x, int y, int w, int h)
	{
		Set<Unit> selected = panel.getMap().getAllAbsolute(x, y, w, h);
		
		if (!selected.isEmpty())
		{
			Player focusedPlayer = getFocusedPlayer(selected);
			
			if (containsNonStructure(selected))
			{
				filterNonStructure(selected);
				Unit leadUnit = getLeadUnit(selected);
				Mediator.playSound(leadUnit.getType().getAcknowledgement());
				push(areAllTrucks(selected)
					? new CommandTruckOverlay(selected)
					: new CommandGroupOverlay(selected)
				);
				panel.showStatus((Unit)null);
				panel.showStatus(focusedPlayer);
			}
			else
			{
				Unit struct = getLeadUnit(selected);
				Mediator.playSound(struct.getType().getAcknowledgement());
				push(new CommandUnitOverlay(struct));
				panel.showStatus(struct);
				panel.showStatus(struct.getOwner());
			}
		}
	}
	
	private static Unit getLeadUnit(Set<Unit> units)
	{
		return units.iterator().next();
	}
	
	private static void filterNonStructure(Set<Unit> units)
	{
		Iterator<Unit> unitIterator = units.iterator();
		
		while (unitIterator.hasNext())
		{
			Unit unit = unitIterator.next();
			
			if (unit.isStructure() || unit.getType().isGuardPostType())
			{
				unitIterator.remove();
			}
		}
	}
	
	private static boolean containsNonStructure(Set<Unit> units)
	{
		for (Unit unit : units)
		{
			if (!unit.isStructure() && !unit.getType().isGuardPostType())
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static Player getFocusedPlayer(Set<Unit> units)
	{
		Map<Player, Integer> ownerCounts = new HashMap<Player, Integer>();
		
		for (Unit unit : units)
		{
			Player owner = unit.getOwner();
			Integer ownerCount = ownerCounts.get(owner);
			ownerCounts.put(owner, ownerCount == null ? 1 : ownerCount + 1);
		}
		
		Map.Entry<Player, Integer> greatestCount = null;
		
		for (Map.Entry<Player, Integer> ownerCount : ownerCounts.entrySet())
		{
			if (greatestCount == null
			 || ownerCount.getValue() > greatestCount.getValue())
			{
				greatestCount = ownerCount;
			}
		}
		
		Player focusedPlayer = greatestCount.getKey();
		Iterator<Unit> unitIterator = units.iterator();
		
		while (unitIterator.hasNext())
		{
			Unit unit = unitIterator.next();
			
			if (!unit.getOwner().equals(focusedPlayer))
			{
				unitIterator.remove();
			}
		}
		
		return focusedPlayer;
	}
	
	private static boolean areAllTrucks(Set<Unit> units)
	{
		for (Unit unit : units)
			if (!unit.isTruck())
				return false;
		
		return true;
	}
}
