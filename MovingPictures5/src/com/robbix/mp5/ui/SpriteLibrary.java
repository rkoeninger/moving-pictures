package com.robbix.mp5.ui;

import static com.robbix.mp5.unit.Activity.TURRET;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.robbix.mp5.AsyncModuleListener;
import com.robbix.mp5.Game;
import com.robbix.mp5.Modular;
import com.robbix.mp5.ModuleEvent;
import com.robbix.mp5.ModuleListener;
import com.robbix.mp5.map.Fixture;
import com.robbix.mp5.map.Ore;
import com.robbix.mp5.unit.Activity;
import com.robbix.mp5.unit.HealthBracket;
import com.robbix.mp5.unit.Unit;
import com.robbix.mp5.unit.UnitType;
import com.robbix.utils.AutoArrayList;
import com.robbix.utils.Direction;
import com.robbix.utils.Utils;

/**
 * Async module loading is not enabled by default.
 */
public class SpriteLibrary implements Modular
{
	public static SpriteLibrary load(File rootDir, boolean lazy) throws IOException
	{
		return lazy ? loadLazy(rootDir) : preload(rootDir);
	}
	
	public static SpriteLibrary loadLazy(File rootDir)
	{
		SpriteLibrary library = new SpriteLibrary();
		library.rootDir = rootDir;
		return library;
	}
	
	public static SpriteLibrary preload(File rootDir) throws IOException
	{
		SpriteLibrary library = new SpriteLibrary();
		library.rootDir = rootDir;
		
		File[] dirs = rootDir.listFiles();
		
		for (File dir : dirs) // For each directory under the spriteset dir
		{
			File infoFile = new File(dir, "info.xml"); // Load xml file in each
			
			if (!infoFile.exists())
				continue;
			
			library.loadModuleSync(infoFile);
		}
		
		return library;
	}
	
	private List<SpriteSet> unitSets; // indexed by UnitType.serial
	private HashMap<String, SpriteSet> ambientSets; // indexed by eventName
	
	private File rootDir;
	
	private Set<String> modulesBeingLoaded;
	private Set<String> loadedModules;
	private AsyncModuleListener.Helper listenerHelper;
	
	private boolean asyncMode = false;
	private Object asyncLock = new Object();
	private AsyncLoader loader;
	
	public SpriteLibrary()
	{
		loadedModules = new HashSet<String>(64);
		modulesBeingLoaded = new HashSet<String>(64);
		unitSets = new AutoArrayList<SpriteSet>();
		ambientSets = new HashMap<String, SpriteSet>(256);
		listenerHelper = new AsyncModuleListener.Helper();
		loader = new AsyncLoader();
	}
	
	public void setAsyncModeEnabled(boolean asyncMode)
	{
		this.asyncMode = asyncMode;
	}
	
	public boolean isAsyncModeEnabled()
	{
		return asyncMode;
	}
	
	public void addModuleListener(ModuleListener listener)
	{
		listenerHelper.add(listener);
	}
	
	public void removeModuleListener(ModuleListener listener)
	{
		listenerHelper.remove(listener);
	}
	
	public void loadModuleSync(String moduleName) throws IOException
	{
		loadModuleSync(new File(rootDir, moduleName));
	}
	
	public void loadModuleSync(File xmlFile) throws IOException
	{
		String moduleName = 
			xmlFile.isDirectory()
			? xmlFile.getName()
			: xmlFile.getParentFile().getName();
		
		if (loadedModules.contains(moduleName)
		 || modulesBeingLoaded.contains(moduleName))
			return;
		
		SpriteSet set = new SpriteSetXMLLoader(xmlFile).load();
		Class<?>[] params = set.getParameterList();
		
		if (params.length == 1 && params[0].equals(String.class))
		{
			ambientSets.put(set.getName(), set);
		}
		else
		{
			UnitType type = Game.game.getUnitFactory().getType(set.getName());
			unitSets.set(type.getSerial(), set);
		}
		
		loadedModules.add(set.getName());
		listenerHelper.fireModuleLoaded(new ModuleEvent(this, set.getName()));
	}
	
	public void loadModuleAsync(String name)
	{
		loadModuleAsync(new File(rootDir, name));
	}
	
	public void loadModuleAsync(File xmlFile)
	{
		String moduleName = 
			xmlFile.isDirectory()
			? xmlFile.getName()
			: xmlFile.getParentFile().getName();
		
		synchronized (asyncLock)
		{
			if (loadedModules.contains(moduleName) || modulesBeingLoaded.contains(moduleName))
				return;
			
			modulesBeingLoaded.add(moduleName);
			listenerHelper.fireModuleLoadStarted(new ModuleEvent(this, moduleName));
			loader.load(xmlFile, new AsyncCallback());
		}
	}
	
	private class AsyncCallback implements AsyncLoader.Callback
	{
		public void loadFailed(File file, Exception exc)
		{
		}
		
		public void loadComplete(SpriteSet set)
		{
			synchronized (asyncLock)
			{
				Class<?>[] params = set.getParameterList();
				
				if (params.length == 1 && params[0].equals(String.class))
				{
					ambientSets.put(set.getName(), set);
				}
				else
				{
					UnitType type = Game.game.getUnitFactory().getType(set.getName());
					unitSets.set(type.getSerial(), set);
				}
				
				modulesBeingLoaded.remove(set.getName());
				loadedModules.add(set.getName());
				listenerHelper.fireModuleLoaded(new ModuleEvent(this, set.getName()));
			}
		}
	}
	
	public void loadModule(String name) throws IOException
	{
		loadModule(new File(rootDir, name));
	}
	
	public void loadModule(File xmlFile) throws IOException
	{
		if (asyncMode)
		{
			loadModuleAsync(xmlFile);
		}
		else
		{
			loadModuleSync(xmlFile);
		}
	}
	
	public boolean isLoaded(String module)
	{
		synchronized (asyncLock)
		{
			return loadedModules.contains(module);
		}
	}
	
	public boolean isBeingLoaded(String module)
	{
		synchronized (asyncLock)
		{
			return modulesBeingLoaded.contains(module);
		}
	}
	
	public boolean unloadModule(String name)
	{
		synchronized (asyncLock)
		{
			if (ambientSets.containsKey(name))
			{
				ambientSets.remove(name);
				loadedModules.remove(name);
				listenerHelper.fireModuleUnloaded(new ModuleEvent(this, name));
				return true;
			}
			
			UnitType type = Game.game.getUnitFactory().getType(name);
			
			if (type != null)
			{
				unitSets.set(type.getSerial(), null);
				loadedModules.remove(name);
				listenerHelper.fireModuleUnloaded(new ModuleEvent(this, name));
				return true;
			}
			
			return false;
		}
	}
	
	public Set<String> getLoadedModules()
	{
		synchronized (asyncLock)
		{
			Set<String> modules = getLoadedUnitModules();
			modules.addAll(getLoadedAmbientModules());
			return modules;
		}
	}
	
	public Set<String> getLoadedUnitModules()
	{
		synchronized (asyncLock)
		{
			Set<String> moduleNames = new HashSet<String>();
			
			for (int i = 0; i < unitSets.size(); ++i)
				if (unitSets.get(i) != null)
					moduleNames.add(unitSets.get(i).getName());
			
			return moduleNames;
		}
	}
	
	public Set<String> getLoadedAmbientModules()
	{
		synchronized (asyncLock)
		{
			return ambientSets.keySet();
		}
	}
	
	public SpriteSet getSpriteSet(String name)
	{
		synchronized (asyncLock)
		{
			if (ambientSets.containsKey(name))
				return ambientSets.get(name);
			
			UnitType type = Game.game.getUnitFactory().getType(name);
			
			if (type == null)
				return null;
			
			return unitSets.get(type.getSerial());
		}
	}
	
	public SpriteSet getUnitSpriteSet(UnitType type)
	{
		synchronized (asyncLock)
		{
			if (unitSets.get(type.getSerial()) == null)
			{
				if (isBeingLoaded(type.getName()))
				{
					return SpriteSet.BLANK;
				}
				
				try
				{
					loadModule(type.getName());
					
					return asyncMode ? SpriteSet.BLANK : unitSets.get(type.getSerial());
				}
				catch (IOException ioe)
				{
					throw new Error(ioe);
				}
			}
			
			return unitSets.get(type.getSerial());
		}
	}
	
	public SpriteSet getAmbientSpriteSet(String eventName)
	{
		synchronized (asyncLock)
		{
			if (! ambientSets.containsKey(eventName))
			{
				if (isBeingLoaded(eventName))
				{
					return SpriteSet.BLANK;
				}
				
				try
				{
					loadModule(eventName);
					return SpriteSet.BLANK;
				}
				catch (IOException ioe)
				{
					throw new Error(ioe);
				}
			}
			
			return ambientSets.get(eventName);
		}
	}
	
	public Point2D getHotspot(Unit turret)
	{
		return getHotspot(turret.getType(), turret.getDirection());
	}
	
	public Point2D getHotspot(UnitType turretType, Direction dir)
	{
		SpriteSet set = getUnitSpriteSet(turretType);
		SpriteGroup group = set.get(TURRET);
		TurretSprite sprite = (TurretSprite) group.getSprite(dir.ordinal());
		return sprite.getHotspot();
	}
	
	public Sprite getUnknownDepositSprite()
	{
		SpriteGroup seq = getAmbientSpriteGroup("aResource", "unknown");
		return seq.getSprite(Utils.getTimeBasedIndex(100, seq.getSpriteCount()));
	}
	
	public SpriteGroup getSpriteGroup(Ore res)
	{
		return getAmbientSpriteGroup("aResource", res.toString());
	}
	
	public Sprite getSprite(Ore res)
	{
		SpriteGroup seq = getSpriteGroup(res);
		return seq.getSprite(Utils.getTimeBasedIndex(100, seq.getSpriteCount()));
	}
	
	public Sprite getDefaultSprite(Ore res)
	{
		return getSpriteGroup(res).getFirst();
	}
	
	public Sprite getTranslucentDefault(Ore res, double aFactor)
	{
		Sprite sprite = getDefaultSprite(res);
		
		if (sprite == null)
			return null;
		
		if (sprite == SpriteSet.BLANK_SPRITE)
			return sprite;
		
		return sprite.getFadedCopy(aFactor);
	}
	
	public Sprite getSprite(Fixture fixture)
	{
		return getSpriteGroup(fixture).getFirst();
	}
	
	public Sprite getTranslucentSprite(Fixture fixture, double aFactor)
	{
		Sprite sprite = getSprite(fixture);
		
		if (sprite == SpriteSet.BLANK_SPRITE)
			return sprite;
		
		return sprite.getFadedCopy(aFactor);
	}
	
	public SpriteGroup getSpriteGroup(Fixture fixture)
	{
		switch (fixture)
		{
		case TUBE:   return getSpriteGroup("oTerrainFixture", "tube");
		case WALL:   return getSpriteGroup("oTerrainFixture", "wall");
		case GEYSER: return getSpriteGroup("aGeyser",         "geyser");
		case MAGMA:  return getSpriteGroup("aMagmaVent",      "magmaVent");
		}
		
		throw new IllegalArgumentException("invalid fixture " + fixture);
	}
	
	public Sprite getSprite(String setName, String eventName)
	{
		SpriteGroup group = getAmbientSpriteGroup(setName, eventName);
		
		if (group == null)
			return SpriteSet.BLANK_SPRITE;
		
		return group.getFirst();
	}
	
	public SpriteGroup getSpriteGroup(String setName, String eventName)
	{
		SpriteGroup group = getAmbientSpriteGroup(setName, eventName);
		
		if (group == null)
			return SpriteSet.BLANK_GROUP;
		
		return group;
	}
	
	public SpriteGroup getAmbientSpriteGroup(String setName, String eventName)
	{
		return getAmbientSpriteSet(setName).get(eventName);
	}
	
	public int getBuildGroupLength(UnitType unitType)
	{
		if (!unitType.isStructureType() && !unitType.isGuardPostType())
			throw new IllegalArgumentException(unitType + " has no build sequence");
		
		SpriteSet set = getUnitSpriteSet(unitType);
		SpriteGroup buildGroup = set.get(Activity.BUILD);
		return buildGroup.getFrameCount();
	}
	
	public int getDumpGroupLength(Unit unit)
	{
		if (!unit.isTruck())
			throw new IllegalArgumentException("Must be truck");
		
		SpriteSet set = getUnitSpriteSet(unit.getType());
		SpriteGroup dumpGroup = set.get(unit.getCargo().getType(), Activity.DUMP, unit.getDirection());
		return dumpGroup.getFrameCount();
	}
	
	public Sprite getDefaultSprite(UnitType unitType)
	{
		SpriteSet set = getUnitSpriteSet(unitType);
		Object[] spriteArgs = unitType.getDefaultSpriteArgs();
		return spriteArgs == null ? null : set.get(spriteArgs).getFirst();
	}
	
	public Sprite getDefaultAmbientSprite(String setName)
	{
		SpriteSet set = getAmbientSpriteSet(setName);
		SpriteGroup group = set.get(set.getArgumentList()[0]);
		return group.getFirst();
	}
	
	public Sprite getDefaultSprite(Unit unit)
	{
		return getDefaultSprite(unit.getType());
	}
	
	public Sprite getTranslucentDefault(UnitType unitType, double aFactor)
	{
		Sprite sprite = getDefaultSprite(unitType);
		
		if (sprite == null)
			return null;
		
		if (sprite == SpriteSet.BLANK_SPRITE)
			return sprite;
		
		return sprite.getFadedCopy(aFactor);
	}
	
	public Sprite getTranslucentDefault(Unit unit, double aFactor)
	{
		return getTranslucentDefault(unit.getType(), aFactor);
	}
	
	/**
	 * Does not recolor for owner.
	 */
	public Sprite getSprite(Unit unit)
	{
		SpriteSet set = getUnitSpriteSet(unit.getType());
		SpriteGroup group = set.get(unit.getSpriteArgs());
		
		if (group.isEnumGroup())
		{
			Class<? extends Enum<?>> enumType = group.getEnumType();
			
			if (enumType.equals(Direction.class))
			{
				return group.getFrame(unit.getDirection());
			}
			else if (enumType.equals(HealthBracket.class))
			{
				return group.getFrame(unit.getHealthBracket());
			}
			else
			{
				throw new Error("Unsupported enum type " + enumType);
			}
		}
		else
		{
			return group.getFrame(unit.getAnimationFrame()
				% group.getFrameCount()
			);
			//FIXME: temporary, group looping should take care of looping
			//
			//         - linear sequences are running one,two frame over.
		}
	}
}
