package com.robbix.mp5.ai.task;

import com.robbix.mp5.Mediator;
import com.robbix.mp5.basics.Filter;
import com.robbix.mp5.basics.Position;
import com.robbix.mp5.map.LayeredMap;
import com.robbix.mp5.ui.SpriteLibrary;
import com.robbix.mp5.unit.Activity;
import com.robbix.mp5.unit.Unit;

public class BuildMineTask extends Task
{
	private Unit mine;
	
	public BuildMineTask(Unit mine)
	{
		super(false, new Filter<Unit>()
		{
			public boolean accept(Unit unit)
			{
				return unit != null && unit.getType().getName().endsWith("Miner");
			}
		});
		
		this.mine = mine;
	}
	
	public void step(Unit unit)
	{
		Position pos = unit.getPosition();
		LayeredMap map = unit.getMap();
		
		SpriteLibrary lib = Mediator.game.getSpriteLibrary();
		int buildFrames = lib.getBuildGroupLength(mine.getType());
		
		map.remove(unit);
		map.putUnit(mine, pos.shift(-1, 0));
		mine.setActivity(Activity.BUILD);
		mine.assignNow(new BuildTask(buildFrames, mine.getType().getBuildTime()));
		unit.completeTask(this);
	}
}