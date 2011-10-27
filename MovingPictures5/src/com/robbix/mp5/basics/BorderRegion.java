package com.robbix.mp5.basics;

import static java.lang.Math.*;

import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class BorderRegion implements RIterable<Position>
{
	public final int x;
	public final int y;
	public final int w;
	public final int h;
	public final int a;
	
	private List<Position> elements;
	
	public BorderRegion(int x, int y, int w, int h)
	{
		if (w < 0)
		{
			x += w;
			w = -w;
		}
		
		if (h < 0)
		{
			y += h;
			h = -h;
		}
		
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.a = (w * 2) + (max(0, h - 2) * 2);
		
		elements = new ArrayList<Position>();
		
		if (a == 0)
			return;
		
		// top, left->right, including corners
		for (int i = x; i <= x + w - 1; ++i)
			elements.add(new Position(i, y));
		
		if (h > 2)
		{
			// right, top->bottom, excluding corners
			for (int i = y + 1; i <= y + h - 2; ++i)
				elements.add(new Position(x + w - 1, i));
		}
		
		if (h > 1)
		{
			// bottom, right->left, including corners
			for (int i = x + w - 1; i >= x; --i)
				elements.add(new Position(i, y + h - 1));
		}
		
		if (h > 2 && w > 1)
		{
			// left, bottom->top, excluding corners
			for (int i = y + h - 2; i >= y + 1; --i)
				elements.add(new Position(x, i));
		}
	}
	
	public BorderRegion(Position a, Position b)
	{
		// + 1 needed so b is included
		this(min(a.x, b.x), min(a.y, b.y), abs(a.x - b.x) + 1, abs(a.y - b.y) + 1);
	}
	
	public BorderRegion(Region region)
	{
		this(region.x, region.y, region.w, region.h);
	}
	
	public boolean contains(Position pos)
	{
		return elements.contains(pos);
	}
	
	public RIterator<Position> iterator()
	{
		return RIterator.iterate(elements);
	}
	
	public void draw(Graphics g, ColorScheme colors, int edgeSize)
	{
		draw(g, colors, new Point(), edgeSize);
	}
	
	public void draw(Graphics g, ColorScheme colors, Point offset, int edgeSize)
	{
		g.setColor(colors.fill);
		
		g.fillRect(
			x * edgeSize + offset.x,
			y * edgeSize + offset.y,
			w * edgeSize,
			edgeSize
		);
		
		if (h > 1)
		{
			g.fillRect(
				x * edgeSize + offset.x,
				(y + h - 1) * edgeSize + offset.y,
				w * edgeSize,
				edgeSize
			);
		}
		
		if (h > 2)
		{
			g.fillRect(
				x * edgeSize + offset.x,
				(y + 1) * edgeSize + offset.y,
				edgeSize,
				(h - 2) * edgeSize
			);
			
			if (w > 1)
			{
				g.fillRect(
					(x + w - 1) * edgeSize + offset.x,
					(y + 1) * edgeSize + offset.y,
					edgeSize,
					(h - 2) * edgeSize
				);
			}
		}
		
		g.setColor(colors.edge);
		
		g.drawRect(
			x * edgeSize + offset.x,
			y * edgeSize + offset.y,
			w * edgeSize,
			h * edgeSize
		);
		
		if (w > 2 && h > 2)
		{
			g.drawRect(
				(x + 1) * edgeSize + offset.x,
				(y + 1) * edgeSize + offset.y,
				(w - 2) * edgeSize,
				(h - 2) * edgeSize
			);
		}
	}
}
