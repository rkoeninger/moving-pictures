package com.robbix.mp5.ui.overlay;

import java.awt.Graphics;
import java.util.Set;

import com.robbix.mp5.Mediator;
import com.robbix.mp5.ai.task.MineRouteTask;
import com.robbix.mp5.unit.Unit;
import com.robbix.mp5.utils.Position;
import com.robbix.mp5.utils.Utils;

public class SelectMineRouteOverlay extends InputOverlay
{
	private Unit smelter, mine;
	private Set<Unit> trucks;
	
	public SelectMineRouteOverlay(Unit truck)
	{
		this(Utils.asSet(truck));
	}
	
	public SelectMineRouteOverlay(Set<Unit> trucks)
	{
		super("dock");
		
		this.trucks = trucks;
	}
	
	public void paintImpl(Graphics g)
	{
		for (Unit truck : trucks)
			drawSelectedUnitBox(g, truck);
	}
	
	public void onLeftClick()
	{
		Unit selected = getPotentialSelection();
		
		if (selected != null)
		{
			if (selected.isMine())
			{
				mine = selected;
				Mediator.playSound("beep6");
			}
			else if (selected.isSmelter())
			{
				smelter = selected;
				Mediator.playSound("beep6");
			}
		}
		
		if (mine != null && smelter != null)
		{
			for (Unit truck : trucks)
				truck.assignNow(new MineRouteTask(mine, smelter));
			
			complete();
		}
	}
	
	private Unit getPotentialSelection()
	{
		Position pos = getCursorPosition();
		Position n = pos.shift(0, -1);
		Position e = pos.shift(1, 0);
		
		Unit occupant = panel.getMap().getUnit(pos);
		
		if (occupant != null && (occupant.isMine() || occupant.isSmelter()))
			return occupant;
		
		occupant = panel.getMap().getUnit(n);
		
		if (occupant != null && occupant.isSmelter())
			return occupant;
		
		occupant = panel.getMap().getUnit(e);
		
		if (occupant != null && occupant.isMine())
			return occupant;
		
		return null;
	}
}
