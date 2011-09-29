package com.robbix.mp5.ui;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.robbix.mp5.Utils;
import com.robbix.mp5.basics.Offset;
import com.robbix.mp5.basics.Tuple;

public class Sprite
{
	private Offset offset;
	private int baseHue;
	private Map<HSTuple, Image> hueScaleMap;
	private HSTuple baseTuple;
	
	public Sprite(Image image, int xOff, int yOff)
	{
		this(image, -1, new Offset(xOff, yOff));
	}
	
	public Sprite(Image image, int baseHue, int xOff, int yOff)
	{
		this(image, baseHue, new Offset(xOff, yOff));
	}
	
	public Sprite(Image image, Offset offset)
	{
		this(image, -1, offset);
	}
	
	public Sprite(Image image, int baseHue, Offset offset)
	{
		this.baseHue = baseHue;
		this.offset = offset;
		this.hueScaleMap = new HashMap<HSTuple, Image>();
		this.baseTuple = new HSTuple(baseHue, 0);
		hueScaleMap.put(baseTuple, image);
	}
	
	public Image getImage()
	{
		return hueScaleMap.get(new HSTuple(baseHue, 0));
	}
	
	public Image getImage(int hue)
	{
		HSTuple tuple = new HSTuple(baseHue == -1 ? -1 : hue, 0);
		BufferedImage image = (BufferedImage) hueScaleMap.get(tuple);
		
		if (image == null)
		{
			image = (BufferedImage) hueScaleMap.get(new HSTuple(baseHue, 0));
			image = Utils.recolorUnit(image, baseHue, hue);
			hueScaleMap.put(tuple, image);
		}
		
		return image;
	}
	
	public Image getImage(int hue, int scale)
	{
		if (hue == -1 || baseHue == -1)
			hue = baseHue;
		
		HSTuple tuple = new HSTuple(hue, scale);
		BufferedImage image = (BufferedImage) hueScaleMap.get(tuple);
		
		if (image == null)
		{
			image = (BufferedImage) hueScaleMap.get(baseTuple);
			image = Utils.recolorUnit(image, baseHue, hue);
			
			if (scale < 0)
			{
				for (int s = -1; s >= scale; s--)
				{
					image = Utils.shrink(image);
					hueScaleMap.put(new HSTuple(hue, s), image);
				}
			}
			else
			{
				for (int s = 1; s <= scale; s++)
				{
					image = Utils.stretch(image);
					hueScaleMap.put(new HSTuple(hue, s), image);
				}
			}
		}
		
		return image;
	}
	
	public int getXOffset()
	{
		return offset.dx;
	}
	
	public int getYOffset()
	{
		return offset.dy;
	}
	
	public int getXOffset(int scale)
	{
		return offset.getDX(scale);
	}
	
	public int getYOffset(int scale)
	{
		return offset.getDY(scale);
	}
	
	private static class HSTuple extends Tuple<Integer, Integer>
	{
		public HSTuple(Integer hue, Integer scale)
		{
			super(hue, scale);
		}
	}
}
