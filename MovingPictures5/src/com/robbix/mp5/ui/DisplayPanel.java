package com.robbix.mp5.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.Timer;

import com.robbix.mp5.Mediator;
import com.robbix.mp5.Utils;
import com.robbix.mp5.basics.CostMap;
import com.robbix.mp5.basics.Direction;
import static com.robbix.mp5.basics.Direction.*;
import com.robbix.mp5.basics.BorderRegion;
import com.robbix.mp5.basics.LShapedRegion;
import com.robbix.mp5.basics.Neighbors;
import com.robbix.mp5.basics.Position;
import com.robbix.mp5.basics.Region;
import com.robbix.mp5.map.LayeredMap;
import com.robbix.mp5.map.ResourceDeposit;
import com.robbix.mp5.map.ResourceType;
import com.robbix.mp5.map.TileSet;
import com.robbix.mp5.player.Player;
import com.robbix.mp5.ui.ani.AmbientAnimation;
import com.robbix.mp5.ui.overlay.InputOverlay;
import com.robbix.mp5.unit.Unit;

@SuppressWarnings("serial")
public class DisplayPanel extends JComponent
{
	private LayeredMap map;
	private SpriteLibrary sprites;
	private TileSet tiles;
	private CursorSet cursors;
	
	private Point scroll = new Point();
	private int tileSize = 32;
	private int scale = 0;
	private final int minScale, maxScale;
	
	private BufferedImage cachedBackground = null;
	private Object cacheLock = new Object();
	
	private static Font costMapFont = Font.decode("Arial-9");
	private static Color TRANS_GRAY = new Color(0, 0, 0, 127);
	private static Color BACKGROUND_BLUE = new Color(127, 127, 255);
	
	private UnitStatus unitStatus;
	private PlayerStatus playerStatus;
	private TitleBar titleBar;
	
	private Player currentPlayer;
	
	private LinkedList<InputOverlay> overlays;
	private InputOverlay.ListenerAdapter adapter;
	
	private List<AmbientAnimation> animations =
		new LinkedList<AmbientAnimation>();
	
	private List<String> options = Arrays.asList(
		"Grid",
		"Background",
		"Unit Layer State",
		"Tube Connectivity",
		"Terrain Cost Map",
		"Terrain Cost Values",
		"Cost Values as Factors",
		"Night"
	);
	
	private boolean showGrid = false;
	private boolean showUnitLayerState = false;
	private boolean showTubeConnectivity = false;
	private boolean showTerrainCostMap = false;
	private boolean showTerrainCostValues = false;
	private boolean showCostValuesAsFactors = false;
	private boolean showBackground = true;
	
	private boolean night = false;
	
	private AnimatedCursor cursor = null;
	private Timer cursorTimer = null;
	
	public DisplayPanel(
		LayeredMap map,
		SpriteLibrary sprites,
		TileSet tiles,
		CursorSet cursors)
	{
		if (map == null || sprites == null || tiles == null || cursors == null)
			throw new IllegalArgumentException();
		
		this.map = map;
		map.setDisplayPanel(this);
		this.sprites = sprites;
		this.tiles = tiles;
		this.tileSize = tiles.getTileSize();
		this.maxScale = 0;
		this.minScale = -Utils.log2(tileSize);
		this.cursors = cursors;
		this.overlays = new LinkedList<InputOverlay>();
		setBackground(BACKGROUND_BLUE);
		setFocusable(true);
		setFocusTraversalKeysEnabled(false);
		Dimension size = new Dimension(
			map.getWidth()  * tileSize,
			map.getHeight() * tileSize
		);
		setPreferredSize(size);
		MouseEvents mouseEvents = new MouseEvents();
		addMouseWheelListener(mouseEvents);
		addMouseMotionListener(mouseEvents);
		addMouseListener(mouseEvents);
		this.adapter = new InputOverlay.ListenerAdapter();
		addKeyListener(adapter);
		addMouseListener(adapter);
		addMouseMotionListener(adapter);
		addMouseWheelListener(adapter);
	}
	
	public List<String> getOptionNames()
	{
		return options;
	}
	
	public boolean getOptionValue(String name)
	{
		if (name.equals("Grid"))
		{
			return showGrid;
		}
		else if (name.equals("Background"))
		{
			return showBackground;
		}
		else if (name.equals("Tube Connectivity"))
		{
			return showTubeConnectivity;
		}
		else if (name.equals("Unit Layer State"))
		{
			return showUnitLayerState;
		}
		else if (name.equals("Terrain Cost Map"))
		{
			return showTerrainCostMap;
		}
		else if (name.equals("Terrain Cost Values"))
		{
			return showTerrainCostValues;
		}
		else if (name.equals("Cost Values as Factors"))
		{
			return showCostValuesAsFactors;
		}
		else if (name.equals("Night"))
		{
			return night;
		}
		
		throw new IllegalArgumentException("Not an option: " + name);
	}
	
	public void setOptionValue(String name, boolean isSet)
	{
		if (name.equals("Grid"))
		{
			showGrid = isSet;
			repaint();
		}
		else if (name.equals("Background"))
		{
			showBackground = isSet;
			refresh();
		}
		else if (name.equals("Tube Connectivity"))
		{
			showTubeConnectivity = isSet;
			repaint();
		}
		else if (name.equals("Unit Layer State"))
		{
			showUnitLayerState = isSet;
			repaint();
		}
		else if (name.equals("Terrain Cost Map"))
		{
			showTerrainCostMap = isSet;
			refresh();
		}
		else if (name.equals("Terrain Cost Values"))
		{
			showTerrainCostValues = isSet;
			refresh();
		}
		else if (name.equals("Cost Values as Factors"))
		{
			showCostValuesAsFactors = isSet;
			refresh();
		}
		else if (name.equals("Night"))
		{
			night = isSet;
			repaint();
		}
		else
		{
			throw new IllegalArgumentException("Not an option: " + name);
		}
	}
	
	public void refresh()
	{
		synchronized (cacheLock)
		{
			cachedBackground = null;
		}
		
		repaint();
	}
	
	public void refresh(Position pos)
	{
		refresh();
	}
	
	public void refresh(Region region)
	{
		refresh();
	}
	
	public void fireCommandButton(String command)
	{
		if (!overlays.isEmpty()) overlays.get(0).onCommand(command);
	}
	
	public void setUnitStatus(UnitStatus status)
	{
		this.unitStatus = status;
	}
	
	public void setPlayerStatus(PlayerStatus status)
	{
		this.playerStatus = status;
	}
	
	public void setTitleBar(TitleBar titleBar)
	{
		this.titleBar = titleBar;
	}
	
	public void showStatus(Unit unit)
	{
		if (unitStatus != null)
			unitStatus.showStatus(unit);
		
		if (unit != null)
			showStatus(unit.getOwner());
	}
	
	public void showStatus(Player player)
	{
		if (playerStatus != null)
			playerStatus.showStatus(player);
		
		currentPlayer = player;
	}
	
	public void showFrameNumber(int frameNumber, double frameRate)
	{
		if (titleBar != null)
			titleBar.showFrameNumber(frameNumber, frameRate);
	}
	
	public void setAnimatedCursor(String name)
	{
		this.cursor = name == null ? null : cursors.getCursor(name);
		
		if (cursorTimer != null)
		{
			cursorTimer.stop();
			cursorTimer = null;
		}
		
		if (cursor == null)
		{
			setCursor(Cursor.getDefaultCursor());
			return;
		}
		
		cursorTimer = new Timer(0, new ActionListener()
		{
			int cursorIndex = 0;
			
			public void actionPerformed(ActionEvent e)
			{
				setCursor(cursor.getCursor(cursorIndex));
				int delayMillis = cursor.getDelay(cursorIndex);
				cursorTimer.setDelay(delayMillis);
				cursorIndex++;
				
				if (cursorIndex == cursor.getFrameCount())
					cursorIndex = 0;
			}
		});
		cursorTimer.start();
	}
	
	public SpriteLibrary getSpriteLibrary()
	{
		return sprites;
	}
	
	public TileSet getTileSet()
	{
		return tiles;
	}
	
	public LayeredMap getMap()
	{
		return map;
	}
	
	public void completeOverlays()
	{
		overlays.clear();
		adapter.setOverlay(null);
		setAnimatedCursor(null);
	}
	
	public InputOverlay getCurrentOverlay()
	{
		return overlays.isEmpty() ? null : overlays.getFirst();
	}
	
	public void completeOverlay(InputOverlay overlay)
	{
		if (overlay != overlays.getFirst())
			throw new IllegalArgumentException();
		
		overlays.removeFirst();
		overlay.dispose();
		overlay.setDisplay(null);
		
		if (!overlays.isEmpty())
		{
			InputOverlay oldOverlay = overlays.getFirst();
			oldOverlay.init();
			adapter.setOverlay(oldOverlay);
		}
		else
		{
			adapter.setOverlay(null);
		}
	}
	
	public void pushOverlay(InputOverlay overlay)
	{
		if (!overlays.isEmpty())
			overlays.getFirst().dispose();
		
		overlays.addFirst(overlay);
		overlay.setDisplay(this);
		overlay.init();
		adapter.setOverlay(overlay);
	}
	
	public void setShowBackground(boolean showBackground)
	{
		this.showBackground = showBackground;
		repaint();
	}
	
	public boolean getShowBackground()
	{
		return showBackground;
	}
	
	public void setShowUnitLayerState(boolean showUnitLayerState)
	{
		this.showUnitLayerState = showUnitLayerState;
		repaint();
	}
	
	public boolean getShowUnitLayerState()
	{
		return showUnitLayerState;
	}
	
	public void setShowGrid(boolean showGrid)
	{
		this.showGrid = showGrid;
		repaint();
	}
	
	public boolean getShowGrid()
	{
		return showGrid;
	}
	
	public void setShowTerrainCostMap(boolean showTerrainCostMap)
	{
		this.showTerrainCostMap = showTerrainCostMap;
		refresh();
	}
	
	public boolean getShowTerrainCostMap()
	{
		return showTerrainCostMap;
	}

	public void setShowTerrainCostValues(boolean showTerrainCostValues)
	{
		this.showTerrainCostValues = showTerrainCostValues;
		refresh();
	}
	
	public boolean getShowTerrainCostValues()
	{
		return showTerrainCostValues;
	}
	
	public void setShowCostValuesAsFactors(boolean showCostValuesAsFactors)
	{
		this.showCostValuesAsFactors = showCostValuesAsFactors;
		refresh();
	}
	
	public boolean getShowCostValuesAsFactors()
	{
		return showCostValuesAsFactors;
	}
	
	public void cueAnimation(AmbientAnimation animation)
	{
		synchronized (animations)
		{
			animations.add(animation);
		}
	}
	
	public List<AmbientAnimation> getAnimations()
	{
		return animations;
	}
	
	private class MouseEvents extends MouseAdapter
	{
		public void mouseWheelMoved(MouseWheelEvent e)
		{
			if (e.isControlDown())
			{
				Point zoomPoint = e.getPoint();
				zoomPoint.x -= scroll.x;
				zoomPoint.y -= scroll.y;
				
				if (e.getWheelRotation() > 0) zoomIn(zoomPoint);
				else                          zoomOut(zoomPoint);
			}
		}
		
		public void mouseClicked(MouseEvent e)
		{
			if (e.isControlDown() && e.getButton() == MouseEvent.BUTTON2)
			{
				if (tileSize == tiles.getTileSize())
				{
					zoomGlobal();
				}
				else
				{
					Point zoomPoint = e.getPoint();
					zoomPoint.x -= scroll.x;
					zoomPoint.y -= scroll.y;
					zoomNormal(zoomPoint);
				}
			}
		}
	}
	
	public int getTotalWidth()
	{
		return map.getWidth() * tileSize;
	}
	
	public int getTotalHeight()
	{
		return map.getHeight() * tileSize;
	}
	
	public int getVerticalLetterBoxSpace()
	{
		return max(0, (getHeight() - getTotalHeight()) / 2);
	}
	
	public int getHorizontalLetterBoxSpace()
	{
		return max(0, (getWidth() - getTotalWidth()) / 2);
	}
	
	public Point getViewPosition()
	{
		return new Point(scroll);
	}
	
	public Point getViewCenterPosition()
	{
		return new Point(
			 (getWidth()  / 2) - scroll.x,
			 (getHeight() / 2) - scroll.y
		);
	}
	
	public int getViewX()
	{
		return scroll.x;
	}
	
	public int getViewY()
	{
		return scroll.y;
	}
	
//	public Point addViewOffset(Point p)
//	{
//		p.translate(-scroll.x, -scroll.y);
//		return p;
//	}
	
	public Point subtractViewOffset(Point p)
	{
		p.translate(scroll.x, scroll.y);
		return p;
	}
	
	public void setViewPosition(int x, int y)
	{
		scroll.x = x;
		scroll.y = y;
		centerAsNeeded();
		refresh(getDisplayRegion());
	}
	
	public void shiftViewPosition(int dx, int dy)
	{
		setViewPosition(
			scroll.x + dx,
			scroll.y + dy
		);
	}
	
	public void setViewCenterPosition(int x, int y)
	{
		setViewPosition(
			(getWidth()  / 2) - x,
			(getHeight() / 2) - y
		);
	}
	
	public void setViewCenterPosition(Position pos)
	{
		setViewPosition(
			(getWidth()  / 2) - (pos.x * tileSize + tileSize / 2),
			(getHeight() / 2) - (pos.y * tileSize + tileSize / 2)
		);
	}
	
	private void centerAsNeeded()
	{
		int diffWidth  = getWidth()  - getTotalWidth();
		int diffHeight = getHeight() - getTotalHeight();
		
		scroll.x = (diffWidth > 0)
			? diffWidth / 2
			: max(min(scroll.x, 0), diffWidth);
		
		scroll.y = (diffHeight > 0)
			? diffHeight / 2
			: max(min(scroll.y, 0), diffHeight);
		
		refresh(getDisplayRegion());
	}
	
	public void zoomIn()
	{
		zoomIn(getViewCenterPosition());
	}
	
	public void zoomOut()
	{
		zoomOut(getViewCenterPosition());
	}
	
	public void zoomNormal()
	{
		zoomNormal(getViewCenterPosition());
	}
	
	public void zoomGlobal()
	{
		setScale(minScale);
		setViewCenterPosition(getWidth() / 2, getHeight() / 2);
		refresh();
	}
	
	public void zoomIn(Position pos)
	{
		zoomIn(getPoint(pos));
	}
	
	public void zoomOut(Position pos)
	{
		zoomOut(getPoint(pos));
	}
	
	public void zoomNormal(Position pos)
	{
		zoomNormal(getPoint(pos));
	}
	
	public void zoomIn(Point center)
	{
		if (scale < maxScale)
		{
			setScaleCentered(scale + 1, center);
			refresh(getDisplayRegion());
		}
	}
	
	public void zoomOut(Point center)
	{
		if (scale > minScale)
		{
			setScaleCentered(scale - 1, center);
			refresh(getDisplayRegion());
		}
	}
	
	public void zoomNormal(Point center)
	{
		if (scale != 0)
		{
			setScaleCentered(0, center);
			refresh(getDisplayRegion());
		}
	}
	
	/**
	 * Should be the root of methods: setSize, setBounds, resize.
	 */
	@SuppressWarnings("deprecation")
	public void reshape(int x, int y, int width, int height)
	{
		super.reshape(x, y, width, height);
		centerAsNeeded();
	}
	
	/**
	 * Gets the current size of grid tiles considering the TileSet's
	 * standard tile size and the current zoom level.
	 */
	public int getTileSize()
	{
		return tileSize;
	}
	
	/**
	 * Gets the current zoom scale.
	 * 0 is normal,
	 * negative values are zoomed out,
	 * positive values are zoomed in.
	 * 
	 * tileSize = baseTileSize * 2^scale
	 */
	public int getScale()
	{
		return scale;
	}
	
	/**
	 * Sets the zoom scale and sets tileSize accordingly.
	 * 
	 * tileSize = baseTileSize * 2^scale
	 */
	public void setScale(int scale)
	{
		int normalTileSize = tiles.getTileSize();
		int newTileSize = scale < 0
			? normalTileSize >> -scale
			: normalTileSize << scale;
		
		if (newTileSize < 1 || newTileSize > 32)
			throw new IllegalArgumentException("scale " + scale + " out of range");
		
		this.tileSize = newTileSize;
		this.scale = scale;
		setPreferredSize(new Dimension(
			map.getWidth()  * tileSize,
			map.getHeight() * tileSize
		));
	}
	
	public void setScaleCentered(int scale, Point center)
	{
		double xFraction = center.x / (double) getTotalWidth();
		double yFraction = center.y / (double) getTotalHeight();
		setScale(scale);
		setViewCenterPosition(
			(int) (xFraction * getTotalWidth()),
			(int) (yFraction * getTotalHeight())
		);
	}
	
	/**
	 * Translates a pixel Point on the panel to a grid Position on the map.
	 */
	public Position getPosition(Point point)
	{
		Position pos = Mediator.getPosition(
			(point.x + scroll.x) / tileSize,
			(point.y + scroll.y) / tileSize
		);
		return map.getBounds().contains(pos) ? pos : null;
	}
	
	/**
	 * Translates a pair of pixel co-ordinates on the panel
	 * to a grid Position on the map.
	 */
	public Position getPosition(int x, int y)
	{
		Position pos = Mediator.getPosition(
			(x - scroll.x) / tileSize,
			(y - scroll.y) / tileSize
		);
		return map.getBounds().contains(pos) ? pos : null;
	}
	
	/**
	 * Translates a grid Position on the map to a pixel Point on the panel
	 * that is the upper-left corner of that grid Position as drawn.
	 */
	public Point getPoint(Position pos)
	{
		return new Point(
			pos.x * tileSize + scroll.x,
			pos.y * tileSize + scroll.y
		);
	}
	
	/**
	 * Translates a grid Position on the map to a pixel Point on the panel
	 * that is in the center of that grid Position as drawn.
	 */
	public Point getCenteredPoint(Position pos)
	{
		int half = tileSize / 2;
		return new Point(
			pos.x * tileSize + half + scroll.x,
			pos.y * tileSize + half + scroll.y
		);
	}
	
	/**
	 * Translates a pixel Rectangle on the panel to a grid Region,
	 * including all grid spots the Rectangle intersects.
	 */
	public Region getRegion(Rectangle rect)
	{
		int minX = (int) floor(rect.x / (double) tileSize);
		int minY = (int) floor(rect.y / (double) tileSize);
		int maxX = (int) ceil((rect.x + rect.width) / (double) tileSize);
		int maxY = (int) ceil((rect.y + rect.height) / (double) tileSize);
		
		return map.getBounds().getIntersection(
			new Region(minX, minY, maxX - minX, maxY - minY)
		);
	}
	
	/**
	 * Translates a pixel Rectangle on the panel to a grid Region,
	 * including only grid spots fully enclosed by the Rectangle.
	 */
	public Region getEnclosedRegion(Rectangle rect)
	{
		int minX = (int) ceil(rect.x / (double) tileSize);
		int minY = (int) ceil(rect.y / (double) tileSize);
		int maxX = (int) floor((rect.x + rect.width) / (double) tileSize);
		int maxY = (int) floor((rect.y + rect.height) / (double) tileSize);
		
		return map.getBounds().getIntersection(
			new Region(minX, minY, maxX - minX, maxY - minY)
		);
	}
	
	/**
	 * Translates a grid Region on the map to a pixel Rectangle on the panel.
	 */
	public Rectangle getRectangle(Region region)
	{
		return new Rectangle(
			region.x * tileSize + scroll.x,
			region.y * tileSize + scroll.y,
			region.w * tileSize,
			region.h * tileSize
		);
	}
	
	/**
	 * Gets the region of pixels currently visible on this display.
	 */
	public Rectangle getDisplayRect()
	{
		return new Rectangle(
			scroll.x,
			scroll.y,
			min(getTotalWidth(),  getWidth()),
			min(getTotalHeight(), getHeight())
		);
	}
	
	/**
	 * Gets the region of positions currently visible on this display.
	 */
	public Region getDisplayRegion()
	{
		return getRegion(getDisplayRect());
	}
	
	public Region getVisibleRegion()
	{
		return getRegion(new Rectangle(
			max(0, -scroll.x),
			max(0, -scroll.y),
			min(getTotalWidth(),  getWidth()),
			min(getTotalHeight(), getHeight())
		));
	}
	
	/*------------------------------------------------------------------------------------------[*]
	 * Paint method and delegates.
	 */
	
	/**
	 * Paints the visible rect of the map, including terrain, units and
	 * overlays.
	 */
	public void paintComponent(Graphics g)
	{
		drawLetterBox(g);
		Rectangle overlayRect = new Rectangle(getSize());
		Rectangle rect = getDisplayRect();
		Region region = getRegion(rect);
		
		if (showBackground)
		{
			drawTerrain(g, rect);
		}
		else
		{
			g.setColor(Color.BLACK);
			fill(g, rect);
		}
		
		if (showGrid && tileSize >= 8)
			drawGrid(g, rect, region);
		
		if (! overlays.isEmpty())
			overlays.getFirst().paintOverTerrain(g, overlayRect);
		
		if (showTubeConnectivity)
			drawTubeConnectivity(g, region);
		
		for (Unit unit : map.getUnitIterator(true))
			if (region.intersects(unit.getOccupiedBounds()))
				drawUnit(g, unit);
		
		synchronized (animations)
		{
			for (AmbientAnimation animation : animations)
				if (rect.intersects(animation.getBounds()))
					animation.paint(g);
		}
		
		for (ResourceDeposit res : map.getResourceDeposits())
			if (region.contains(res.getPosition()))
				drawResourceDeposit(g, res);
		
		if (night)
		{
			g.setColor(TRANS_GRAY);
			fill(g, rect);
		}
		
		if (! overlays.isEmpty())
			overlays.getFirst().paintOverUnits(g, overlayRect);
	}
	
	/**
	 * Draws the background-colored letterboxes around the
	 * map display area.
	 */
	private void drawLetterBox(Graphics g)
	{
		g.setColor(getBackground());
		int hSpace = getHorizontalLetterBoxSpace();
		int vSpace = getVerticalLetterBoxSpace();
		
		// Left side, including corners
		g.fillRect(0, 0, hSpace, getHeight());
		
		// Right side, including corners
		g.fillRect(getWidth() - hSpace, 0, hSpace, getHeight());
		
		// Top side
		g.fillRect(hSpace, 0, getWidth() - hSpace * 2, vSpace);
		
		// Bottom side
		g.fillRect(hSpace, getHeight() - vSpace, getWidth() - hSpace * 2, vSpace);
	}
	
	/**
	 * Draws grid of size tileSize over Region in Color.BLACK.
	 */
	private void drawGrid(Graphics g, Rectangle rect, Region region)
	{
		g.setColor(Color.BLACK);
		int x0 = rect.x;
		int y0 = rect.y;
		int w = getTotalWidth();
		int h = getTotalHeight();
		
		// Vertical lines
		for (int x = max(1, region.x); x < region.getMaxX(); ++x)
			g.drawLine(x0 + x * tileSize, y0, x0 + x * tileSize, y0 + h - 1);
		
		// Horizontal lines
		for (int y = max(1, region.y); y < region.getMaxY(); ++y)
			g.drawLine(x0, y0 + y * tileSize, x0 + w - 1, y0 + y * tileSize);
	}
	
	/**
	 * Draws terrain (surface or cost map) depending on options using
	 * Graphics g in the visible rect.
	 */
	private void drawTerrain(Graphics g, Rectangle rect)
	{
		synchronized (cacheLock)
		{
			Region region = getVisibleRegion();
			region = map.getBounds().getIntersection(region);
			
			if (cachedBackground == null)
			{
				cachedBackground = new BufferedImage(
					rect.width,
					rect.height,
					BufferedImage.TYPE_INT_RGB
				);
			}
			
			Graphics cg = cachedBackground.getGraphics();
			
			if (showTerrainCostMap) drawCostMap(cg, region);
							   else drawSurface(cg, region);
			
			cg.dispose();
			Rectangle r = new Rectangle(getHorizontalLetterBoxSpace(), getVerticalLetterBoxSpace(), rect.width, rect.height);
			draw(g, cachedBackground, r.getLocation());
			g.setColor(Color.RED);
			draw(g, new Rectangle(r.x, r.y, r.width-1, r.height-1));
		}
	}
	
	/**
	 * Draws the terrain costmap using Graphics g with in given visible Region.
	 */
	private void drawCostMap(Graphics g, Region region)
	{
		CostMap terrainCost = map.getTerrainCostMap();
		g.setFont(costMapFont);
		
		for (Position pos : region)
		{
			g.setColor(Utils.getGrayscale(terrainCost.getScaleFactor(pos)));
			fill(g, pos);
			
			if (showTerrainCostValues)
			{
				g.setColor(Utils.getBlackWhiteComplement(g.getColor()));
				
				double cost = showCostValuesAsFactors
					? terrainCost.getScaleFactor(pos)
					: terrainCost.get(pos);
				
				g.drawString(
					Double.isInfinite(cost)
						? "Inf"
						: String.format("%.2f", cost),
					pos.x * tileSize + 3,
					pos.y * tileSize + 11
				);
			}
		}
	}
	
	/**
	 * Draws the terrain surface image using Graphics g in the visible Region.
	 */
	private void drawSurface(Graphics g, Region region)
	{
		for (Position pos : region)
		{
			draw(g, tiles.getTile(map.getTileCode(pos)).getImage(scale), pos);
			
			if (map.hasMinePlatform(pos))
			{
				draw(g, sprites.getSprite("aCommonMine", "platform"), pos);
			}
			else if (map.hasGeyser(pos))
			{
				draw(g, sprites.getSprite("aGeyser", "geyser"), pos);
			}
		}
	}
	
	/**
	 * Highlights tubes and buildings as active/potentially active or disabled.
	 */
	private void drawTubeConnectivity(Graphics g, Region region)
	{
		for (int x = region.x; x < region.getMaxX(); ++x)
		for (int y = region.y; y < region.getMaxY(); ++y)
		{
			Position pos = Mediator.getPosition(x, y);
			
			Unit occupant = map.getUnit(pos);
			boolean structNeedsConnection = occupant != null && occupant.needsConnection();
			boolean structHasConnection = occupant != null && occupant.isConnected();
			
			if (map.hasTube(pos) || structNeedsConnection)
			{
				g.setColor(
					map.isAlive(pos) || structHasConnection
					? InputOverlay.TRANS_GREEN
					: InputOverlay.TRANS_RED
				);
				
				fill(g, pos);
			}
		}
	}
	
	/**
	 * Gets the sprite for the given Unit in its current state and renders it
	 * along with any overlays - i.e. the UnitLayer state.
	 */
	private void drawUnit(Graphics g, Unit unit)
	{
		if (!unit.isTurret() && scale < -2)
		{
			g.setColor(unit.getOwner().getColor());
			fill(g, unit.getOccupiedBounds());
			return;
		}
		
		Sprite sprite = sprites.getSprite(unit);
		Point unitPoint = new Point(unit.getAbsX(), unit.getAbsY());
		
		if (showUnitLayerState && !unit.isTurret())
		{
			g.setColor(Utils.getTranslucency(Color.RED, 127));
			
			for (Position pos : unit.getFootprint().iterator(unit.getPosition()))
				fill(g, pos);
			
			g.setColor(Utils.getTranslucency(Color.BLUE, 127));
			
			for (Position pos : unit.getMap().getReservations(unit))
				fill(g, pos);
		}
		
		draw(g, sprite, unitPoint, unit.getOwner());
		
		if (unit.hasTurret())
		{
			drawUnit(g, unit.getTurret());
		}
		else
		{
			drawStatusLight(g, unit);
		}
	}
	
	/**
	 * Draws upper-left corner status light for structures and guard posts.
	 * Green "active" lights are not draw for guard posts.
	 * Unit must be owned by current viewing player to have status light drawn.
	 */
	private void drawStatusLight(Graphics g, Unit unit)
	{
		if (!currentPlayer.owns(unit))
			return;
		
		if (unit.isGuardPost() || unit.isStructure())
		{
			Position pos = unit.getPosition();
			
			if (unit.isIdle())
			{
				draw(g, sprites.getSprite("aStructureStatus", "idle"), pos);
			}
			else if (unit.isDisabled())
			{
				SpriteGroup seq = sprites.getAmbientSpriteGroup("aStructureStatus", "disabled");
				draw(g, seq.getSprite(Utils.getTimeBasedIndex(100, seq.getSpriteCount())), pos);
			}
			else if (unit.isStructure())
			{
				draw(g, sprites.getSprite("aStructureStatus", "active"), pos);
			}
		}
	}
	
	/**
	 * Draws a ResourceDeposit at that deposit's position.
	 */
	private void drawResourceDeposit(Graphics g, ResourceDeposit res)
	{
		if (scale < -1)
		{
			g.setColor(res.getType() == ResourceType.COMMON_ORE
				? new Color(255, 92, 0)
				: new Color(255, 255, 106));
			fill(g, res.getPosition());
			return;
		}
		
		Sprite sprite = res.isSurveyedBy(currentPlayer)
			? sprites.getSprite(res)
			: sprites.getUnknownDepositSprite();
		
		draw(g, sprite, res.getPosition());
	}
	
	/*------------------------------------------------------------------------------------------[*]
	 * Draw helpers. Helpers account for tileSize and scrollPoint.
	 */
	
	public void drawEdge(Graphics g, Position pos, Direction dir)
	{
		int x0 = pos.x * tileSize + scroll.x;
		int y0 = pos.y * tileSize + scroll.y;
		int x1 = (pos.x + 1) * tileSize + scroll.x;
		int y1 = (pos.y + 1) * tileSize + scroll.y;
		
		switch (dir)
		{
		case N: g.drawLine(x0, y0, x1, y0); break;
		case S: g.drawLine(x0, y1, x1, y1); break;
		case E: g.drawLine(x1, y0, x1, y1); break;
		case W: g.drawLine(x0, y0, x0, y1); break;
		}
	}
	
	public void drawEdges(Graphics g, Position pos, Neighbors neighbors)
	{
		if (neighbors.has(N)) drawEdge(g, pos, N);
		if (neighbors.has(S)) drawEdge(g, pos, S);
		if (neighbors.has(E)) drawEdge(g, pos, E);
		if (neighbors.has(W)) drawEdge(g, pos, W);
	}
	
	public void draw(Graphics g, Position pos)
	{
		g.drawRect(
			pos.x * tileSize + scroll.x,
			pos.y * tileSize + scroll.y,
			tileSize,
			tileSize
		);
	}
	
	public void fill(Graphics g, Position pos)
	{
		g.fillRect(
			pos.x * tileSize + scroll.x,
			pos.y * tileSize + scroll.y,
			tileSize,
			tileSize
		);
	}
	
	public void drawPosition(Graphics g, int x, int y)
	{
		g.drawRect(
			x * tileSize + scroll.x,
			y * tileSize + scroll.y,
			tileSize,
			tileSize
		);
	}
	
	public void fillPosition(Graphics g, int x, int y)
	{
		g.fillRect(
			x * tileSize + scroll.x,
			y * tileSize + scroll.y,
			tileSize,
			tileSize
		);
	}
	
	public void draw(Graphics g, Region region)
	{
		g.drawRect(
			region.x * tileSize + scroll.x,
			region.y * tileSize + scroll.y,
			region.w * tileSize,
			region.h * tileSize
		);
	}
	
	public void fill(Graphics g, Region region)
	{
		g.fillRect(
			region.x * tileSize + scroll.x,
			region.y * tileSize + scroll.y,
			region.w * tileSize,
			region.h * tileSize
		);
	}
	
	public void draw(Graphics g, LShapedRegion region)
	{
		Direction leg1Dir = region.getFirstLegDirection();
		Direction leg2Dir = region.getSecondLegDirection();
		Position end1 = region.getFirstEnd();
		Position end2 = region.getSecondEnd();
		Position elbow = region.getElbow();
		
		drawEdges(g, end1, Neighbors.allBut(leg1Dir));
		drawEdges(g, end2, Neighbors.allBut(leg2Dir));
		drawEdge(g, elbow, leg1Dir);
		drawEdge(g, elbow, leg2Dir);
		
		connectFacingCorners(g, end1, elbow);
		connectFacingCorners(g, end2, elbow);
	}
	
	private void connectFacingCorners(Graphics g, Position from, Position to)
	{
		if (! to.isColinear(from))
			throw new IllegalArgumentException("not colinear");
		
		int tx0 = to.x * tileSize + scroll.x;
		int ty0 = to.y * tileSize + scroll.y;
		int tx1 = tx0 + tileSize;
		int ty1 = ty0 + tileSize;
		
		int fx0 = from.x * tileSize + scroll.x;
		int fy0 = from.y * tileSize + scroll.y;
		int fx1 = fx0 + tileSize;
		int fy1 = fy0 + tileSize;
		
		switch (Direction.getDirection(from, to))
		{
		case N:
			g.drawLine(fx0, fy0, tx0, ty1);
			g.drawLine(fx1, fy0, tx1, ty1);
			break;
		case S:
			g.drawLine(fx0, fy1, tx0, ty0);
			g.drawLine(fx1, fy1, tx1, ty0);
			break;
		case E:
			g.drawLine(fx1, fy0, tx0, ty0);
			g.drawLine(fx1, fy1, tx0, ty1);
			break;
		case W:
			g.drawLine(fx0, fy0, tx1, ty0);
			g.drawLine(fx0, fy1, tx1, ty1);
			break;
		}
	}
	
	public void fill(Graphics g, LShapedRegion region)
	{
		Position end1 = region.getFirstEnd();
		Position end2 = region.getSecondEnd();
		Position elbow = region.getElbow();
		
		fill(g, end1);
		fill(g, end2);
		fill(g, elbow);
		
		fillSpaceBetween(g, end1, elbow);
		fillSpaceBetween(g, end2, elbow);
	}
	
	private void fillSpaceBetween(Graphics g, Position from, Position to)
	{
		if (! to.isColinear(from))
			throw new IllegalArgumentException("not colinear");
		
		int tx0 = to.x * tileSize + scroll.x;
		int ty0 = to.y * tileSize + scroll.y;
		int tx1 = tx0 + tileSize;
		int ty1 = ty0 + tileSize;
		
		int fx0 = from.x * tileSize + scroll.x;
		int fy0 = from.y * tileSize + scroll.y;
		int fx1 = fx0 + tileSize;
		int fy1 = fy0 + tileSize;
		
		switch (Direction.getDirection(from, to))
		{
		case N:
			g.fillRect(tx0, ty1, fx1 - fx0, fy0 - ty1);
			break;
		case S:
			g.fillRect(fx0, fy1, fx1 - fx0, ty0 - fy1);
			break;
		case E:
			g.fillRect(fx1, fy0, tx0 - fx1, ty1 - ty0);
			break;
		case W:
			g.fillRect(tx1, ty0, fx0 - tx1, ty1 - ty0);
			break;
		}
	}
	
	public void draw(Graphics g, BorderRegion region)
	{
		g.drawRect(
			region.x * tileSize + scroll.x,
			region.y * tileSize + scroll.y,
			region.w * tileSize,
			region.h * tileSize
		);
		
		if (region.w > 2 && region.h > 2)
		{
			g.drawRect(
				(region.x + 1) * tileSize + scroll.x,
				(region.y + 1) * tileSize + scroll.y,
				(region.w - 2) * tileSize,
				(region.h - 2) * tileSize
			);
		}
	}
	
	public void fill(Graphics g, BorderRegion region)
	{
		g.fillRect(
			region.x * tileSize + scroll.x,
			region.y * tileSize + scroll.y,
			region.w * tileSize,
			tileSize
		);
		
		if (region.h > 1)
		{
			g.fillRect(
				region.x * tileSize + scroll.x,
				(region.y + region.h - 1) * tileSize + scroll.y,
				region.w * tileSize,
				tileSize
			);
		}
		
		if (region.h > 2)
		{
			g.fillRect(
				region.x * tileSize + scroll.x,
				(region.y + 1) * tileSize + scroll.y,
				tileSize,
				(region.h - 2) * tileSize
			);
			
			if (region.w > 1)
			{
				g.fillRect(
					(region.x + region.w - 1) * tileSize + scroll.x,
					(region.y + 1) * tileSize + scroll.y,
					tileSize,
					(region.h - 2) * tileSize
				);
			}
		}
	}
	
	public void draw(Graphics g, Rectangle rect)
	{
		g.drawRect(rect.x, rect.y, rect.width, rect.height);
	}
	
	/**
	 * Does not account for scale/scroll,
	 * draws literally to Graphics.
	 */
	public void fill(Graphics g, Rectangle rect)
	{
		g.fillRect(rect.x, rect.y, rect.width, rect.height);
	}
	
	/**
	 * Does not account for scale/scroll,
	 * draws literally to Graphics.
	 */
	public void draw(Graphics g, Image img, Rectangle rect)
	{
		g.drawImage(
			img,
			rect.x, rect.y, rect.x + rect.width, rect.y + rect.height,
			rect.x, rect.y, rect.x + rect.width, rect.y + rect.height,
			null
		);
	}
	
	public void draw(Graphics g, Image img, Point point)
	{
		g.drawImage(
			img,
			point.x, point.y,
			null
		);
	}
	
	// TODO: scrolling/scaling?
	public void draw(Graphics g, Image img, Position pos)
	{
		g.drawImage(img, pos.x * tileSize, pos.y * tileSize, null);
	}
	
	public void draw(Graphics g, Sprite sprite, Position pos)
	{
		Image img = sprite.getImage();
		int x = pos.x * tileSize + scroll.x + sprite.getXOffset(scale);
		int y = pos.y * tileSize + scroll.y + sprite.getYOffset(scale);
		int w = img.getWidth(null);
		int h = img.getHeight(null);
		int w2 = scale < 0 ? w >> -scale : w << scale;
		int h2 = scale < 0 ? h >> -scale : h << scale;
		g.drawImage(
			img,
			x, y, x + w2, y + h2,
			0, 0, w, h,
			null
		);
	}
	
	// TODO: scrolling/scaling?
	public void draw(Graphics g, Sprite sprite, Point point, Player owner)
	{
		Image img = sprite.getImage(owner == null ? -1 : owner.getColorHue());
		int x = point.x + scroll.x + sprite.getXOffset(scale);
		int y = point.y + scroll.y + sprite.getYOffset(scale);
		int w = img.getWidth(null);
		int h = img.getHeight(null);
		int w2 = scale < 0 ? w >> -scale : w << scale;
		int h2 = scale < 0 ? h >> -scale : h << scale;
		g.drawImage(
			img,
			x, y, x + w2, y + h2,
			0, 0, w, h,
			null
		);
	}
}
