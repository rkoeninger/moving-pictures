package com.robbix.mp5.sb;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;

import com.robbix.mp5.Engine;
import com.robbix.mp5.Game;
import com.robbix.mp5.Mediator;
import com.robbix.mp5.MeteorShowerTrigger;
import com.robbix.mp5.Utils;
import com.robbix.mp5.ai.task.MineRouteTask;
import com.robbix.mp5.ai.task.SteerTask;
import com.robbix.mp5.basics.Position;
import com.robbix.mp5.basics.Region;
import com.robbix.mp5.map.LayeredMap;
import com.robbix.mp5.map.ResourceDeposit;
import com.robbix.mp5.map.ResourceType;
import com.robbix.mp5.player.Player;
import com.robbix.mp5.ui.CommandButton;
import com.robbix.mp5.ui.DisplayPanel;
import com.robbix.mp5.ui.PlayerStatus;
import com.robbix.mp5.ui.SpriteLibrary;
import com.robbix.mp5.ui.TitleBar;
import com.robbix.mp5.ui.UnitStatus;
import com.robbix.mp5.ui.overlay.PlaceBulldozeOverlay;
import com.robbix.mp5.ui.overlay.PlaceGeyserOverlay;
import com.robbix.mp5.ui.overlay.PlaceResourceOverlay;
import com.robbix.mp5.ui.overlay.PlaceTubeOverlay;
import com.robbix.mp5.ui.overlay.PlaceUnitOverlay;
import com.robbix.mp5.ui.overlay.PlaceWallOverlay;
import com.robbix.mp5.ui.overlay.SelectUnitOverlay;
import com.robbix.mp5.ui.overlay.SpawnMeteorOverlay;
import com.robbix.mp5.unit.Cargo;
import com.robbix.mp5.unit.Unit;
import com.robbix.mp5.unit.UnitFactory;

/**
 * Test code for MovingPictures 5.
 * 
 * @author bort
 *
 */
public class Sandbox
{
	private static final String DEFAULT_TILE_SET = "newTerraDirt";
	private static final String DEFAULT_MAP = "plain";
	private static final File RES_DIR = new File("./res");
	
	private static Player currentPlayer;
	private static Game game;
	private static Engine engine;
	private static JFrame frame;
	private static DisplayPanel panel;
	private static JToolBar commandBar;
	private static Map<Integer, JMenuItem> playerMenuItems =
		new HashMap<Integer, JMenuItem>();
	
	private static JFrame slViewer, utViewer, sbPlayer;
	
	// References moved here so they can be called by addPlayer(int)
	private static JMenu playerMenu;
	private static ButtonGroup playerSelectButtonGroup;
	private static ActionListener playerSelectListener;
	private static boolean showFrameRate = false;
	
	private static JMenuItem pauseMenuItem;
	private static JMenuItem stepMenuItem;
	private static JMenuItem spriteLibMenuItem;
	private static JMenuItem unitLibMenuItem;
	private static JMenuItem soundPlayerMenuItem;
	private static JMenuItem throttleMenuItem;
	private static JMenuItem exitMenuItem;
	private static JMenuItem scrollBarsMenuItem;
	private static JMenuItem frameRateMenuItem;
	private static JMenuItem scrollSpeedMenuItem;
	private static JMenuItem commandButtonsMenuItem;
	private static JMenuItem spawnMeteorMenuItem;
	private static JMenuItem meteorShowerMenuItem;
	private static JMenuItem placeGeyserMenuItem;
	private static JMenuItem placeWallMenuItem;
	private static JMenuItem placeTubeMenuItem;
	private static JMenuItem placeBulldozeMenuItem;
	private static JMenuItem placeCommon1;
	private static JMenuItem placeCommon2;
	private static JMenuItem placeCommon3;
	private static JMenuItem placeRare1;
	private static JMenuItem placeRare2;
	private static JMenuItem placeRare3;
	private static JMenuItem playSoundMenuItem;
//	private static JMenuItem playMusicMenuItem;
	private static JMenuItem setAudioFormatMenuItem;
	private static JMenuItem removeAllUnitsMenuItem;
	private static JMenuItem addPlayerMenuItem;
	
	public static void main(String[] args) throws IOException
	{
		Map<String, Method> demos = getDemos();
		
		/*-------------------------------------------------------------------*
		 * Parse command-line arguments
		 */
		boolean lazyLoadSprites = true;
		boolean lazyLoadSounds  = true;
		boolean soundOn         = false;
//		boolean musicOn         = false;
		String demoName         = null;
		String mapName          = null;
		String tileSetName      = null;
		
		for (int a = 0; a < args.length; ++a)
		{
			int colonIndex = Math.max(args[a].indexOf(':'), 0);
			String option = args[a].substring(colonIndex + 1);
			
			if (args[a].equals("-lazyLoadSprites"))
			{
				lazyLoadSprites = true;
			}
			else if (args[a].equals("-lazyLoadSounds"))
			{
				lazyLoadSounds = true;
			}
			else if (args[a].equals("-soundOn"))
			{
				soundOn = true;
			}
			else if (args[a].equals("-soundOff"))
			{
				soundOn = false;
			}
//			else if (args[a].equals("-musicOn"))
//			{
//				musicOn = true;
//			}
//			else if (args[a].equals("-musicOff"))
//			{
//				musicOn = false;
//			}
			else if (args[a].startsWith("-demo:") && demos.containsKey(option))
			{
				if (demoName != null || mapName != null)
					throw new IllegalArgumentException("Duplicate map/demos");
				
				demoName = option;
			}
			else if (args[a].startsWith("-map:"))
			{
				if (demoName != null || mapName != null)
					throw new IllegalArgumentException("Duplicate map/demos");
				
				mapName = option;
			}
			else if (args[a].startsWith("-tileSet:"))
			{
				if (tileSetName != null)
					throw new IllegalArgumentException("Duplicate tile sets");
				
				tileSetName = option;
			}
		}
		
		if (tileSetName == null)
		{
			tileSetName = DEFAULT_TILE_SET;
		}
		
		if (mapName == null)
		{
			if (demoName == null)
			{
				mapName = DEFAULT_MAP;
			}
			else
			{
				mapName = getDemoMaps().get(demoName);
				
				if (mapName == null)
					throw new Error("no map defined for " + demoName);
			}
		}
		
		Sandbox.trySystemLookAndFeel();
		
		/*-------------------------------------------------------------------*
		 * Load map, units, sprites, cursors
		 * Create players
		 * Create engine
		 * Init Mediator
		 */
		game = Game.load(
			RES_DIR,
			mapName,
			tileSetName,
			lazyLoadSprites,
			lazyLoadSounds
		);
		engine = new Engine(game);
		Mediator.initMediator(game);
		
		if (soundOn) Mediator.soundOn(true);
		
		currentPlayer = game.getDefaultPlayer();
		
		/*-------------------------------------------------------------------*
		 * Prepare live-updating status labels
		 */
		UnitStatusLabel unitStatusLabel = new UnitStatusLabel();
		PlayerStatusLabel playerStatusLabel = new PlayerStatusLabel();
		
		TitleBar titleBar = new TitleBar()
		{
			public void showFrameNumber(int frameNumber, double frameRate)
			{
				if (showFrameRate)
				{
					frame.setTitle(String.format(
						"Moving Pictures - [%1$d] %2$.2f fps",
						frameNumber,
						frameRate
					));
				}
			}
		};
		
		/*-------------------------------------------------------------------*
		 * Load command button icons
		 */
		final ActionListener commandButtonListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				game.getDisplay().fireCommandButton(e.getActionCommand());
			}
		};
		
		commandBar = new JToolBar();
		commandBar.setOrientation(SwingConstants.HORIZONTAL);
		commandBar.setFloatable(false);
		commandBar.setRollover(true);
		
		for (File iconDir : new File(RES_DIR, "art/commandButtons").listFiles())
		{
			if (!iconDir.isDirectory())
				continue;
			
			ArrayList<ImageIcon> icons = new ArrayList<ImageIcon>();
			
			for (File iconFile : iconDir.listFiles(Utils.BMP))
			{
				icons.add(new ImageIcon(Utils.shrink(ImageIO.read(iconFile))));
			}
			
			if (icons.isEmpty())
				continue;
			
			CommandButton button = new CommandButton(iconDir.getName(), icons);
			button.addActionListener(commandButtonListener);
			button.setFocusable(false);
			commandBar.add(button);
		}
		
		String[] extraCommands = {"kill", "idle", "build", "dock", "mine"};
		
		for (String commandName : extraCommands)
		{
			CommandButton button = new CommandButton(commandName);
			button.addActionListener(commandButtonListener);
			button.setFocusable(false);
			commandBar.add(button);
		}
		
		/*-------------------------------------------------------------------*
		 * Build UI
		 */
		panel = game.getDisplay();
		panel.setUnitStatus(unitStatusLabel);
		panel.setPlayerStatus(playerStatusLabel);
		panel.setTitleBar(titleBar);
		panel.setShowGrid(false);
		panel.setShowUnitLayerState(false);
		panel.setShowTerrainCostMap(false);
		panel.setShowTerrainCostValues(true);
		panel.pushOverlay(new SelectUnitOverlay());
		panel.showStatus(currentPlayer);
		
		pauseMenuItem = new JMenuItem("Pause");
		stepMenuItem = new JMenuItem("Step Once");
		spriteLibMenuItem = new JMenuItem("Sprite Library");
		unitLibMenuItem = new JMenuItem("Unit Library");
		soundPlayerMenuItem = new JMenuItem("Sound Player");
		throttleMenuItem = new JMenuItem("Unthrottle");
		exitMenuItem = new JMenuItem("Exit");
		scrollBarsMenuItem = new JMenuItem("Scroll Bars");
		frameRateMenuItem = new JMenuItem("Frame Rate");
		scrollSpeedMenuItem = new JMenuItem("Scroll Speed");
		commandButtonsMenuItem = new JMenuItem("Command Buttons");
		spawnMeteorMenuItem = new JMenuItem("Spawn Meteor");
		meteorShowerMenuItem = new JMenuItem("Meteor Shower");
		placeGeyserMenuItem = new JMenuItem("Place Geyser");
		placeWallMenuItem = new JMenuItem("Place Wall");
		placeTubeMenuItem = new JMenuItem("Place Tube");
		placeBulldozeMenuItem = new JMenuItem("Bulldoze");
		placeCommon1 = new JMenuItem("Common Low");
		placeCommon2 = new JMenuItem("Common Med");
		placeCommon3 = new JMenuItem("Common High");
		placeRare1 = new JMenuItem("Rare Low");
		placeRare2 = new JMenuItem("Rare Med");
		placeRare3 = new JMenuItem("Rare High");
		playSoundMenuItem = new JCheckBoxMenuItem("Play Sounds");
//		playMusicMenuItem = new JCheckBoxMenuItem("Play Music");
		setAudioFormatMenuItem = new JMenuItem("Set Format...");
		removeAllUnitsMenuItem = new JMenuItem("Remove All");
		addPlayerMenuItem = new JMenuItem("Add Player...");
		
		final JMenuBar menuBar = new JMenuBar();
		final JMenu engineMenu = new JMenu("Engine");
		final JMenu displayMenu = new JMenu("Display");
		final JMenu terrainMenu = new JMenu("Terrain");
		final JMenu unitMenu = new JMenu("Units");
		playerMenu = new JMenu("Players");
		stepMenuItem.setEnabled(false);
		throttleMenuItem.setEnabled(engine.isThrottled());
		final JMenu disastersMenu = new JMenu("Disasters");
		final JMenu placeResourceMenu = new JMenu("Add Ore");
		final JMenu soundMenu = new JMenu("Sound");
		playSoundMenuItem.setSelected(game.getSoundBank().isRunning());
		final JMenu addUnitMenu = new JMenu("Add");
		playerMenu.add(addPlayerMenuItem);
		playerMenu.addSeparator();
		
		/*-------------------------------------------------------------------*
		 * Add player select menu items
		 */
		playerSelectButtonGroup = new ButtonGroup();
		
		playerSelectListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				selectPlayer(Integer.valueOf(e.getActionCommand()));
			}
		};
		
		for (Player player : game.getPlayers())
		{
			final JMenuItem playerSelectMenuItem =
				new JRadioButtonMenuItem(player.getName());
			playerSelectButtonGroup.add(playerSelectMenuItem);
			
			if (currentPlayer.equals(player))
				playerSelectMenuItem.setSelected(true);
			
			playerSelectMenuItem.setActionCommand(
				String.valueOf(player.getID())
			);
			playerMenu.add(playerSelectMenuItem);
			playerSelectMenuItem.addActionListener(playerSelectListener);
			playerMenuItems.put(player.getID(), playerSelectMenuItem);
		}
		
		/*-------------------------------------------------------------------*
		 * Add place unit menu items
		 */
		final List<String> names = game.getUnitFactory().getUnitNames();
		final List<String> types = game.getUnitFactory().getUnitTypes();
		
		// I know it's a bubble sort
		for (int a = 0; a < names.size(); ++a)
		{
			for (int b = 0; b < names.size(); ++b)
			{
				if (names.get(a).compareTo(names.get(b)) < 0)
				{
					String temp;
					temp = names.get(a);
					names.set(a, names.get(b));
					names.set(b, temp);
					temp = types.get(a);
					types.set(a, types.get(b));
					types.set(b, temp);
				}
			}
		}
		
		final ActionListener addUnitListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				game.getUnitFactory().setDefaultOwner(currentPlayer);
				panel.pushOverlay(new PlaceUnitOverlay(
					game.getUnitFactory(),
					e.getActionCommand()
				));
			}
		};
		
		for (int i = 0; i < types.size(); ++i)
		{
			final JMenuItem addUnitMenuItem = new JMenuItem(names.get(i));
			addUnitMenu.add(addUnitMenuItem);
			addUnitMenuItem.setActionCommand(types.get(i));
			addUnitMenuItem.addActionListener(addUnitListener);
		}
		
		/*-------------------------------------------------------------------*
		 * Add display option menu items
		 */
		final List<String> options = panel.getOptionNames();
		
		ActionListener displayOptionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				panel.setOptionValue(
					e.getActionCommand(),
					!panel.getOptionValue(e.getActionCommand())
				);
			}
		};
		
		for (int i = 0; i < options.size(); ++i)
		{
			final JMenuItem panelOptionMenuItem = new JMenuItem(options.get(i));
			displayMenu.add(panelOptionMenuItem);
			panelOptionMenuItem.setActionCommand(options.get(i));
			
			if (options.get(i).equals("Grid"))
			{
				panelOptionMenuItem.setAccelerator(KeyStroke.getKeyStroke(
					KeyEvent.VK_G,
					KeyEvent.ALT_DOWN_MASK
				));
			}
			
			panelOptionMenuItem.addActionListener(displayOptionListener);
		}
		
		MenuItemListener miListener = new MenuItemListener();
		spriteLibMenuItem.addActionListener(miListener);
		unitLibMenuItem.addActionListener(miListener);
		soundPlayerMenuItem.addActionListener(miListener);
		commandButtonsMenuItem.addActionListener(miListener);
		scrollSpeedMenuItem.addActionListener(miListener);
		scrollBarsMenuItem.addActionListener(miListener);
		frameRateMenuItem.addActionListener(miListener);
		meteorShowerMenuItem.addActionListener(miListener);
		spawnMeteorMenuItem.addActionListener(miListener);
		playSoundMenuItem.addActionListener(miListener);
//		playMusicMenuItem.addActionListener(miListener);
		setAudioFormatMenuItem.addActionListener(miListener);
		placeCommon1.addActionListener(miListener);
		placeCommon2.addActionListener(miListener);
		placeCommon3.addActionListener(miListener);
		placeRare1.addActionListener(miListener);
		placeRare2.addActionListener(miListener);
		placeRare3.addActionListener(miListener);
		placeBulldozeMenuItem.addActionListener(miListener);
		placeGeyserMenuItem.addActionListener(miListener);
		placeWallMenuItem.addActionListener(miListener);
		placeTubeMenuItem.addActionListener(miListener);
		removeAllUnitsMenuItem.addActionListener(miListener);
		pauseMenuItem.addActionListener(miListener);
		throttleMenuItem.addActionListener(miListener);
		stepMenuItem.addActionListener(miListener);
		exitMenuItem.addActionListener(miListener);
		addPlayerMenuItem.addActionListener(miListener);
		
		pauseMenuItem.setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_PAUSE, 0)
		);
		stepMenuItem.setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)
		);
		
		engineMenu.add(pauseMenuItem);
		engineMenu.add(throttleMenuItem);
		engineMenu.add(stepMenuItem);
		engineMenu.addSeparator();
		engineMenu.add(spriteLibMenuItem);
		engineMenu.add(unitLibMenuItem);
		engineMenu.add(soundPlayerMenuItem);
		engineMenu.addSeparator();
		engineMenu.add(exitMenuItem);
		displayMenu.add(scrollBarsMenuItem);
		displayMenu.add(frameRateMenuItem);
		displayMenu.add(scrollSpeedMenuItem);
		displayMenu.add(commandButtonsMenuItem);
		soundMenu.add(playSoundMenuItem);
//		soundMenu.add(playMusicMenuItem);
		soundMenu.addSeparator();
		soundMenu.add(setAudioFormatMenuItem);
		terrainMenu.add(placeWallMenuItem);
		terrainMenu.add(placeTubeMenuItem);
		terrainMenu.add(placeGeyserMenuItem);
		terrainMenu.add(placeBulldozeMenuItem);
		placeResourceMenu.add(placeCommon1);
		placeResourceMenu.add(placeCommon2);
		placeResourceMenu.add(placeCommon3);
		placeResourceMenu.add(placeRare1);
		placeResourceMenu.add(placeRare2);
		placeResourceMenu.add(placeRare3);
		terrainMenu.add(placeResourceMenu);
		disastersMenu.add(spawnMeteorMenuItem);
		disastersMenu.add(meteorShowerMenuItem);
		unitMenu.add(addUnitMenu);
		unitMenu.addSeparator();
		unitMenu.add(removeAllUnitsMenuItem);
		
		menuBar.add(engineMenu);
		menuBar.add(displayMenu);
		menuBar.add(soundMenu);
		menuBar.add(terrainMenu);
		menuBar.add(disastersMenu);
		menuBar.add(playerMenu);
		menuBar.add(unitMenu);
		
		JPanel statusesPanel = new JPanel();
		statusesPanel.setLayout(new GridLayout(2, 1));
		statusesPanel.add(unitStatusLabel);
		statusesPanel.add(playerStatusLabel);
		
		Rectangle windowBounds = Utils.getWindowBounds();
		
		frame = new JFrame("Moving Pictures");
		frame.setJMenuBar(menuBar);
		frame.setLayout(new BorderLayout());
		frame.add(commandBar, BorderLayout.NORTH);
		frame.add(game.getView(), BorderLayout.CENTER);
		frame.add(statusesPanel, BorderLayout.SOUTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setIconImages(getWindowIcons());
		frame.pack();
		
		boolean maximize = false;
		
		if (frame.getWidth()  > windowBounds.width
		 && frame.getHeight() > windowBounds.height)
		{
			maximize = true;
		}
		
		frame.setSize(
			Math.min(frame.getWidth(),  windowBounds.width),
			Math.min(frame.getHeight(), windowBounds.height)
		);
		
		if (maximize)
		{
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		}
		else
		{
			frame.setLocation(
				windowBounds.x + (windowBounds.width  - frame.getWidth())  / 2,
				windowBounds.y + (windowBounds.height - frame.getHeight()) / 2
			);
		}
		
		/*--------------------------------------------------------------------*
		 * Setup demo
		 * Start mechanics
		 */
		if (demoName != null)
		{
			Method setupDemo = demos.get(demoName);
			
			if (setupDemo == null)
				throw new IllegalArgumentException(demoName + " not found");
			
			try
			{
				setupDemo.invoke(null, game);
			}
			catch (Exception exc)
			{
				throw new Error("Failed to setup demo", exc);
			}
		}
		
		engine.play();
		frame.setVisible(true);
	}
	
	private static class MenuItemListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == spriteLibMenuItem)
			{
				if (slViewer == null)
				{
					slViewer = new SpriteViewer(game);
					slViewer.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
					
					try
					{
						slViewer.setIconImages(getWindowIcons());
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				}
				
				slViewer.setVisible(true);
			}
			else if (e.getSource() == unitLibMenuItem)
			{
				if (utViewer == null)
				{
					utViewer = new UnitTypeViewer(game);
					utViewer.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
				
					try
					{
						utViewer.setIconImages(getWindowIcons());
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				}
				
				utViewer.setVisible(true);
			}
			else if (e.getSource() == soundPlayerMenuItem)
			{
				if (sbPlayer == null)
				{
					sbPlayer = new SoundPlayer(game);
					sbPlayer.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
					
					try
					{
						sbPlayer.setIconImages(getWindowIcons());
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				}
				
				sbPlayer.setVisible(true);
			}
			else if (e.getSource() == commandButtonsMenuItem)
			{
				if (commandBar.getParent() == frame.getRootPane())
				{
					frame.remove(commandBar);
					frame.validate();
				}
				else
				{
					commandBar.setOrientation(SwingConstants.HORIZONTAL);
					frame.add(commandBar, BorderLayout.NORTH);
					frame.validate();
				}
			}
			else if (e.getSource() == scrollSpeedMenuItem)
			{
				for (;;)
				{
					int scrollSpeed = game.getView().getScrollSpeed();
					
					String result = JOptionPane.showInputDialog(
						frame,
						"Scroll Speed (1+)",
						String.valueOf(scrollSpeed)
					);
					
					if (result == null)
						return;
					
					try
					{
						scrollSpeed = Integer.parseInt(result);
						game.getView().setScrollSpeed(scrollSpeed);
						break;
					}
					catch (NumberFormatException nfe)
					{
						continue;
					}
				}
			}
			else if (e.getSource() == scrollBarsMenuItem)
			{
				game.getView().showScrollBars(! game.getView().areScrollBarsVisible());
				frame.validate();
			}
			else if (e.getSource() == frameRateMenuItem)
			{
				showFrameRate = !showFrameRate;
				
				if (!showFrameRate)
				{
					frame.setTitle("Moving Pictures");
				}
			}
			else if (e.getSource() == meteorShowerMenuItem)
			{
				game.getTriggers().add(new MeteorShowerTrigger(1, 300));
				Mediator.playSound("savant_meteorApproaching");
			}
			else if (e.getSource() == spawnMeteorMenuItem)
			{
				panel.pushOverlay(new SpawnMeteorOverlay());
			}
			else if (e.getSource() == playSoundMenuItem)
			{
				Mediator.soundOn(playSoundMenuItem.isSelected());
			}
//			else if (e.getSource() == playMusicMenuItem)
//			{
//				if (playMusicMenuItem.isSelected())
//				{
//					
//				}
//				else
//				{
//					
//				}
//			}
			else if (e.getSource() == setAudioFormatMenuItem)
			{
				AudioFormat format = game.getSoundBank().getFormat();
				
				for (;;)
				{
					format = AudioFormatDialog.showDialog(frame, format);
					
					if (format == null)
						return;
					
					Line.Info info = new DataLine.Info(SourceDataLine.class, format);
					
					if (! AudioSystem.isLineSupported(info))
						continue;
					
					game.getSoundBank().setFormat(format);
					return;
				}
			}
			else if (e.getSource() == placeCommon1)
			{
				panel.pushOverlay(new PlaceResourceOverlay(ResourceDeposit.get1BarCommon()));
			}
			else if (e.getSource() == placeCommon2)
			{
				panel.pushOverlay(new PlaceResourceOverlay(ResourceDeposit.get2BarCommon()));
			}
			else if (e.getSource() == placeCommon3)
			{
				panel.pushOverlay(new PlaceResourceOverlay(ResourceDeposit.get3BarCommon()));
			}
			else if (e.getSource() == placeRare1)
			{
				panel.pushOverlay(new PlaceResourceOverlay(ResourceDeposit.get1BarRare()));
			}
			else if (e.getSource() == placeRare2)
			{
				panel.pushOverlay(new PlaceResourceOverlay(ResourceDeposit.get2BarRare()));
			}
			else if (e.getSource() == placeRare3)
			{
				panel.pushOverlay(new PlaceResourceOverlay(ResourceDeposit.get3BarRare()));
			}
			else if (e.getSource() == placeBulldozeMenuItem)
			{
				panel.pushOverlay(new PlaceBulldozeOverlay());
			}
			else if (e.getSource() == placeGeyserMenuItem)
			{
				panel.pushOverlay(new PlaceGeyserOverlay());
			}
			else if (e.getSource() == placeWallMenuItem)
			{
				panel.pushOverlay(new PlaceWallOverlay());
			}
			else if (e.getSource() == placeTubeMenuItem)
			{
				panel.pushOverlay(new PlaceTubeOverlay());
			}
			else if (e.getSource() == pauseMenuItem)
			{
				engine.pause();
				stepMenuItem.setEnabled(!engine.isRunning());
				pauseMenuItem.setText(
					engine.isRunning()
					? "Pause"
					: "Resume"
				);
			}
			else if (e.getSource() == throttleMenuItem)
			{
				engine.toggleThrottle();
				throttleMenuItem.setText(
					engine.isThrottled()
					? "Unthrottle"
					: "Throttle"
				);
			}
			else if (e.getSource() == stepMenuItem)
			{
				engine.step();
			}
			else if (e.getSource() == exitMenuItem)
			{
				System.exit(0);
			}
			else if (e.getSource() == removeAllUnitsMenuItem)
			{
				int result = JOptionPane.showConfirmDialog(
					frame,
					"Are you sure?",
					"Remove All Units",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE
				);
				
				if (result != JOptionPane.YES_OPTION)
					return;
				
				game.getMap().clearAllUnits();
			}
			else if (e.getSource() == addPlayerMenuItem)
			{
				String name = JOptionPane.showInputDialog(frame, "Player Name");
				
				if (name == null)
					return;
				
				int colorHue;
				
				for (;;)
				{
					String colorHueString = JOptionPane.showInputDialog(
						frame,
						"Player Color\nMust be integer 0 to 359"
					);
					
					if (colorHueString == null)
						return;
					
					try
					{
						colorHue = Integer.valueOf(colorHueString);
					}
					catch (NumberFormatException nfe)
					{
						continue;
					}
					
					if (colorHue >= 0 && colorHue <= 359)
						break;
				}
				
				Player newPlayer = new Player(game.getPlayers().size(), name, colorHue);
				game.addPlayer(newPlayer);
				final JMenuItem playerSelectMenuItem =
					new JRadioButtonMenuItem(name);
				playerSelectButtonGroup.add(playerSelectMenuItem);
				playerSelectMenuItem.setActionCommand(
					String.valueOf(newPlayer.getID()));
				playerMenu.add(playerSelectMenuItem);
				playerSelectMenuItem.addActionListener(playerSelectListener);
				playerSelectMenuItem.setSelected(true);
				currentPlayer = newPlayer;
				game.getUnitFactory().setDefaultOwner(currentPlayer);
			}
		}
	}
	
	private static class UnitStatusLabel extends JLabel
	implements UnitStatus, ActionListener
	{
		private static final long serialVersionUID = 1L;
		Unit myUnit = null;
		Timer timer;
		
		public UnitStatusLabel()
		{
			setPreferredSize(new Dimension(100, 20));
			timer = new Timer(100, this);
			timer.start();
		}
		
		public synchronized void actionPerformed(ActionEvent e)
		{
			setText(myUnit != null ? myUnit.getStatusString() : "");
		}
		
		public synchronized void showStatus(Unit unit)
		{
			myUnit = unit;
		}
	}
	
	private static class PlayerStatusLabel extends JLabel
	implements PlayerStatus, ActionListener
	{
		private static final long serialVersionUID = 1L;
		Player myPlayer = null;
		Timer timer;
		
		public PlayerStatusLabel()
		{
			setPreferredSize(new Dimension(100, 20));
			timer = new Timer(100, this);
			timer.start();
		}
		
		public synchronized void actionPerformed(ActionEvent e)
		{
			setText(myPlayer != null ? myPlayer.getStatusString() : "");
		}
		
		public synchronized void showStatus(Player player)
		{
			Player oldMyPlayer = myPlayer;
			myPlayer = player;
			
			if (player != null && !player.equals(oldMyPlayer))
				selectPlayer(player.getID());
		}
	}
	
	// Call after Game.addPlayer() to sync with sandbox UI
	private static void selectPlayer(int playerID)
	{
		Player player = game.getPlayer(playerID);
		currentPlayer = player;
		Mediator.factory.setDefaultOwner(player);
		game.getDisplay().showStatus(currentPlayer);
		JMenuItem menuItem = playerMenuItems.get(playerID);
		
		if (menuItem != null)
			menuItem.setSelected(true);
	}
	
	// Call after Game.addPlayer() to sync with sandbox UI
	private static void addPlayer(int playerID)
	{
		Player newPlayer = game.getPlayer(playerID);
		String name = newPlayer.getName();
		JMenuItem playerSelectMenuItem = new JRadioButtonMenuItem(name);
		playerSelectButtonGroup.add(playerSelectMenuItem);
		playerSelectMenuItem.setActionCommand(String.valueOf(playerID));
		playerMenu.add(playerSelectMenuItem);
		playerSelectMenuItem.addActionListener(playerSelectListener);
		playerSelectMenuItem.setSelected(true);
		playerMenuItems.put(playerID, playerSelectMenuItem);
	}
	
	public static List<String> getAvailableTileSets()
	{
		List<String> tileSets = new ArrayList<String>();
		
		for (File file : new File("./res/tileset").listFiles())
		{
			if (file.isDirectory() && new File(file, "plain").exists())
			{
				tileSets.add(file.getName());
			}
		}
		
		return tileSets;
	}
	
	public static List<String> getAvailableMaps()
	{
		List<String> maps = new ArrayList<String>();
		
		for (File file : new File("./res/terrain").listFiles())
		{
			String fileName = file.getName();
			
			if (file.isFile() && fileName.endsWith(".txt"))
			{
				maps.add(fileName.substring(0, fileName.length() - 4));
			}
		}
		
		return maps;
	}
	
	public static Map<String, Method> getDemos()
	{
		Map<String, Method> demos = new HashMap<String, Method>();
		
		for (Method method : Sandbox.class.getDeclaredMethods())
		{
			int paramCount = method.getParameterTypes().length;
			String methodName = method.getName();
			
			if (methodName.startsWith("setup")
			 && methodName.endsWith("Demo")
			 && paramCount == 1
			 && method.getParameterTypes()[0].equals(Game.class))
			{
				char firstChar = Character.toLowerCase(methodName.charAt(5));
				methodName = methodName.substring(6, methodName.length());
				methodName = firstChar + methodName;
				
				demos.put(methodName, method);
			}
		}
		
		return demos;
	}
	
	public static Map<String, String> getDemoMaps()
	{
		Map<String, String> demoMaps = new HashMap<String, String>();
		
		for (Method method : Sandbox.class.getDeclaredMethods())
		{
			int paramCount = method.getParameterTypes().length;
			String methodName = method.getName();
			
			if (methodName.startsWith("map")
			 && methodName.endsWith("Demo")
			 && paramCount == 0)
			{
				char firstChar = Character.toLowerCase(methodName.charAt(3));
				String demoName = methodName.substring(4, methodName.length());
				demoName = firstChar + demoName;
				
				String mapName = null;
				
				try
				{
					mapName = (String) method.invoke(null, (Object[])null);
				}
				catch (Exception exc)
				{
					throw new Error("could not get map name for demo");
				}
				
				demoMaps.put(demoName, mapName);
			}
		}
		
		return demoMaps;
	}
	
	public static String mapMeteorDemo()
	{
		return "widePlain";
	}
	
	public static void setupMeteorDemo(Game game)
	{
		LayeredMap map = game.getMap();
		UnitFactory factory = game.getUnitFactory();
		
		Player player1 = new Player(1, "Targets", 45);
		game.addPlayer(player1);
		addPlayer(1);
		selectPlayer(1);
		factory.setDefaultOwner(player1);
		
		for (int x = 0; x < 12; ++x)
		for (int y = 0; y < 20; ++y)
		{
			map.putUnit(factory.newUnit("pScout"), Mediator.getPosition(x, y));
		}
		
		for (int x = 12; x < 30; x += 2)
		for (int y = 13; y < 19; y += 2)
		{
			map.putUnit(factory.newUnit("pResidence"), Mediator.getPosition(x, y));
		}
		
		map.putUnit(factory.newUnit("eVehicleFactory"), Mediator.getPosition(12, 1));
		map.putUnit(factory.newUnit("eVehicleFactory"), Mediator.getPosition(16, 1));
		map.putUnit(factory.newUnit("eVehicleFactory"), Mediator.getPosition(20, 1));
		map.putUnit(factory.newUnit("eVehicleFactory"), Mediator.getPosition(24, 1));
		map.putUnit(factory.newUnit("eStructureFactory"), Mediator.getPosition(12, 5));
		map.putUnit(factory.newUnit("eStructureFactory"), Mediator.getPosition(16, 5));
		map.putUnit(factory.newUnit("eStructureFactory"), Mediator.getPosition(20, 5));
		map.putUnit(factory.newUnit("eStructureFactory"), Mediator.getPosition(24, 5));
		map.putUnit(factory.newUnit("eCommonSmelter"), Mediator.getPosition(12, 9));
		map.putUnit(factory.newUnit("eCommonSmelter"), Mediator.getPosition(16, 9));
		map.putUnit(factory.newUnit("eCommonSmelter"), Mediator.getPosition(20, 9));
		map.putUnit(factory.newUnit("eCommonSmelter"), Mediator.getPosition(24, 9));
		
		SpriteLibrary lib = game.getSpriteLibrary();
		
		try
		{
			lib.loadModule("pScout");
			lib.loadModule("pResidence");
			lib.loadModule("eVehicleFactory");
			lib.loadModule("eStructureFactory");
			lib.loadModule("eCommonSmelter");
			lib.loadModule("aDeath");
			lib.loadModule("aMeteor");
			lib.loadModule("aStructureStatus");
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	public static String mapFactoryDemo()
	{
		return "widePlain";
	}
	
	public static void setupFactoryDemo(Game game)
	{
		LayeredMap map = game.getMap();
		UnitFactory factory = game.getUnitFactory();
		
		Player player1 = new Player(1, "Factories", 275);
		game.addPlayer(player1);
		addPlayer(1);
		selectPlayer(1);
		factory.setDefaultOwner(player1);
		
		player1.addResource(ResourceType.COMMON_ORE, 50000);
		player1.addResource(ResourceType.RARE_ORE,   50000);
		player1.addResource(ResourceType.FOOD,       50000);
		
		Unit convec1     = factory.newUnit("eConVec");
		Unit convec2     = factory.newUnit("eConVec");
		Unit convec3     = factory.newUnit("eConVec");
		Unit convec4     = factory.newUnit("eConVec");
		Unit earthworker = factory.newUnit("eEarthworker");
		Unit dozer       = factory.newUnit("pRoboDozer");
		
		convec1.setCargo(Cargo.newConVecCargo("eVehicleFactory"));
		convec2.setCargo(Cargo.newConVecCargo("eStructureFactory"));
		convec3.setCargo(Cargo.newConVecCargo("eCommonSmelter"));
		convec4.setCargo(Cargo.newConVecCargo("eCommandCenter"));
		
		map.putUnit(convec1,     Mediator.getPosition(9,  7));
		map.putUnit(convec2,     Mediator.getPosition(10, 7));
		map.putUnit(convec3,     Mediator.getPosition(11, 7));
		map.putUnit(convec4,     Mediator.getPosition(12, 7));
		map.putUnit(earthworker, Mediator.getPosition(10, 9));
		map.putUnit(dozer,       Mediator.getPosition(11, 9));
		
		SpriteLibrary lib = game.getSpriteLibrary();
		
		try
		{
			lib.loadModule("eConVec");
			lib.loadModule("eEarthworker");
			lib.loadModule("pRoboDozer");
			lib.loadModule("eVehicleFactory");
			lib.loadModule("eStructureFactory");
			lib.loadModule("eCommonSmelter");
			lib.loadModule("eCommandCenter");
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	public static String mapMineRouteDemo()
	{
		return "bigPlain";
	}
	
	public static void setupMineRouteDemo(Game game)
	{
		LayeredMap map = game.getMap();
		UnitFactory factory = game.getUnitFactory();
		
		Player player1 = new Player(1, "Mining Operation", 200);
		game.addPlayer(player1);
		addPlayer(1);
		selectPlayer(1);
		
		List<Unit> mines    = new ArrayList<Unit>();
		List<Unit> smelters = new ArrayList<Unit>();
		
		for (int x = 2; x < 44; x += 6)
		for (int y = 2; y < 20; y += 6)
		{
			Unit smelter = factory.newUnit("eCommonSmelter", player1);
			map.putUnit(smelter, Mediator.getPosition(x, y));
			map.putTube(Mediator.getPosition(x + 5, y + 1));
			map.putTube(Mediator.getPosition(x + 2, y + 4));
			map.putTube(Mediator.getPosition(x + 2, y + 5));
			smelters.add(smelter);
		}
		
		map.putUnit(factory.newUnit("eCommandCenter", player1), Mediator.getPosition(44, 2));
		
		for (int x = 2;  x < 46; x += 4)
		for (int y = 38; y < 45; y += 4)
		{
			ResourceDeposit res = ResourceDeposit.get2BarCommon();
			map.putResourceDeposit(res, Mediator.getPosition(x + 1, y));
			Unit mine = factory.newUnit("eCommonMine", player1);
			map.putUnit(mine, Mediator.getPosition(x, y));
			mines.add(mine);
		}
		
		Unit truck;
		int truckIndex = 0;
		
		Collections.shuffle(mines);
		Collections.shuffle(smelters);
		
		for (Unit mine : mines)
		for (Unit smelter : smelters)
		{
			if (Utils.randInt(0, 5) % 6 == 0)
			{
				Position pos = Mediator.getPosition(
					truckIndex % 42 + 2,
					truckIndex / 42 + 24
				);
				
				truck = factory.newUnit("eCargoTruck", player1);
				truck.assignNow(new MineRouteTask(mine, smelter));
				map.putUnit(truck, pos);
			}
			
			truckIndex++;
		}
		
		SpriteLibrary lib = game.getSpriteLibrary();
		
		try
		{
			lib.loadModule("eCargoTruck");
			lib.loadModule("eCommandCenter");
			lib.loadModule("eCommonSmelter");
			lib.loadModule("eCommonMine");
			lib.loadModule("aResource");
			lib.loadModule("aStructureStatus");
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		game.getDisplay().setViewCenterPosition(Mediator.getPosition(24, 26));
	}
	
	public static String mapCombatDemo()
	{
		return "bigPlain";
	}
	
	public static void setupCombatDemo(Game game)
	{
		LayeredMap map = game.getMap();
		UnitFactory factory = game.getUnitFactory();
		Region bounds = map.getBounds();
		Position center = Mediator.getPosition(bounds.w / 2, bounds.h / 2);
		Player player1 = new Player(1, "Axen", 320);
		Player player2 = new Player(2, "Emma", 200);
		Player player3 = new Player(3, "Nguyen", 40);
		Player player4 = new Player(4, "Frost", 160);
		Player player5 = new Player(5, "Brook", 95);
		game.addPlayer(player1);
		game.addPlayer(player2);
		game.addPlayer(player3);
		game.addPlayer(player4);
		game.addPlayer(player5);
		addPlayer(1);
		addPlayer(2);
		addPlayer(3);
		addPlayer(4);
		addPlayer(5);
		selectPlayer(5);
		
		for (int x = 1; x <= 15; ++x)
		for (int y = 1; y <= 11; ++y)
		{
			Unit tank = factory.newUnit("pMicrowaveLynx", player1);
			map.putUnit(tank, Mediator.getPosition(x, y));
			tank.assignNow(new SteerTask(center));
		}
		
		for (int x = 36; x <= 46; ++x)
		for (int y = 1;  y <= 15;  ++y)
		{
			Unit tank = factory.newUnit("eLaserLynx", player2);
			map.putUnit(tank, Mediator.getPosition(x, y));
			tank.assignNow(new SteerTask(center));
		}

		for (int x = 1;  x <= 11;  ++x)
		for (int y = 31; y <= 46; ++y)
		{
			Unit tank = factory.newUnit("pRPGLynx", player3);
			map.putUnit(tank, Mediator.getPosition(x, y));
			tank.assignNow(new SteerTask(center));
		}

		for (int x = 31; x <= 46; ++x)
		for (int y = 36; y <= 46; ++y)
		{
			Unit tank = factory.newUnit("eRailGunLynx", player4);
			map.putUnit(tank, Mediator.getPosition(x, y));
			tank.assignNow(new SteerTask(center));
		}
		
		for (int x = 20; x <= 29; ++x)
		for (int y = 20; y <= 29; ++y)
		{
			Unit tank = factory.newUnit("eAcidCloudLynx", player5);
			map.putUnit(tank, Mediator.getPosition(x, y));
		}
		
		SpriteLibrary lib = game.getSpriteLibrary();
		
		try
		{
			lib.loadModule("eLynxChassis");
			lib.loadModule("pLynxChassis");
			lib.loadModule("eLaserSingleTurret");
			lib.loadModule("eRailGunSingleTurret");
			lib.loadModule("eAcidCloudSingleTurret");
			lib.loadModule("pMicrowaveSingleTurret");
			lib.loadModule("pRPGSingleTurret");
			lib.loadModule("aDeath");
			lib.loadModule("aRocket");
			lib.loadModule("aAcidCloud");
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		game.getDisplay().setViewCenterPosition(center);
	}
	
	/**
	 * Attempts to set swing look and feel to the local system's
	 * native theme. Returns true if successful.
	 */
	public static boolean trySystemLookAndFeel()
	{
		try
		{
			String className = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(className);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	public static List<Image> getWindowIcons() throws IOException
	{
		return Arrays.asList(
			(Image) ImageIO.read(new File(RES_DIR, "art/smallIcon.png")),
			(Image) ImageIO.read(new File(RES_DIR, "art/mediumIcon.png"))
		);
	}
}
