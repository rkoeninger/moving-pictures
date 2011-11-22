package com.robbix.mp5.ui;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.robbix.mp5.unit.Unit;
import com.robbix.utils.Position;
import com.robbix.utils.RGraphics;
import com.robbix.utils.Utils;

public class UnitDisplayObject extends DisplayObject
{
	private Unit unit;
	
	public UnitDisplayObject(Unit unit)
	{
		this.unit = unit;
	}
	
	public boolean isAlive()
	{
		return !unit.isDead() && !unit.isFloating();
	}
	
	public Rectangle2D getBounds()
	{
		Point2D absPoint = unit.getAbsPoint();
		return new Rectangle2D.Double(
			absPoint.getX(),
			absPoint.getY(),
			unit.getWidth(),
			unit.getHeight()
		);
	}
	
	public void paint(RGraphics g)
	{
		drawUnit(g, unit);
	}
	
	private void drawUnit(RGraphics g, Unit unit)
	{
		if (!unit.isTurret() && panel.getScale() < panel.getMinimumShowUnitScale())
		{
			g.setColor(unit.getOwner().getColor());
			g.fillRegion(unit.getOccupiedBounds());
			return;
		}
		
		Point2D point = unit.getAbsPoint();
		Sprite sprite = panel.getSpriteLibrary().getSprite(unit);
		
		if (panel.isShowingShadows() && !unit.isTurret())
		{
			Point2D shadowOffset = panel.getShadowOffset();
			Point2D shadowPoint = new Point2D.Double(
				point.getX() + shadowOffset.getX(),
				point.getY() + shadowOffset.getY()
			);
			g.drawImage(sprite.getShadow(), shadowPoint);
		}
		
		if (!unit.isTurret() && sprite == SpriteSet.BLANK_SPRITE)
		{
			g.setColor(unit.getOwner().getColor());
			g.fillRegion(unit.getOccupiedBounds());
		}
		else
		{
			if (unit.getOwner() != null) sprite.paint(g, point, unit.getOwner().getColorHue());
                                    else sprite.paint(g, point);
		}
		
		if (unit.hasTurret())
		{
			drawUnit(g, unit.getTurret());
		}
		else if (unit.isGuardPost() || unit.isStructure())
		{
			drawStatusLight(g, unit);
		}
	}
	
	private void drawStatusLight(RGraphics g, Unit unit)
	{
		if (!panel.getCurrentPlayer().owns(unit))
			return;
		
		Position pos = unit.getPosition();
		
		if (unit.isIdle())
		{
			panel.getSpriteLibrary().getSprite("aStructureStatus", "idle").paint(g, pos);
		}
		else if (unit.isDisabled())
		{
			SpriteGroup seq = panel.getSpriteLibrary().getAmbientSpriteGroup("aStructureStatus", "disabled");
			seq.getSprite(Utils.getTimeBasedIndex(100, seq.getSpriteCount())).paint(g, pos);
		}
		else if (unit.isStructure())
		{
			panel.getSpriteLibrary().getSprite("aStructureStatus", "active").paint(g, pos);
		}
	}
}