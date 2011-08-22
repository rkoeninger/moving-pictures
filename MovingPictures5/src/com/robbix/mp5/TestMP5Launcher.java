package com.robbix.mp5;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;

public class TestMP5Launcher
{
	public static void main(String[] args)
	{
		Utils.trySystemLookAndFeel();
		
		final JDialog dialog = new JDialog();
		dialog.setTitle("TestMP5 Launcher");
		final JComboBox mapComboBox = new JComboBox(getMapAndDemoOptions());
		mapComboBox.setSelectedItem("map:plain");
		final JComboBox tileSetBox = new JComboBox(getTileSetOptions());
		tileSetBox.setSelectedItem("tileSet:newTerraDirt");
		final JCheckBox lazySpritesCheckBox = new JCheckBox("Load Sprites on Demand");
		lazySpritesCheckBox.setSelected(true);
		final JCheckBox lazySoundsCheckBox = new JCheckBox("Load Sounds on Demand");
		lazySoundsCheckBox.setSelected(true);
		final JCheckBox soundOnCheckBox = new JCheckBox("Sound On");
		soundOnCheckBox.setSelected(false);
		final JCheckBox musicOnCheckBox = new JCheckBox("Music On");
		musicOnCheckBox.setSelected(false);
		final JButton launchButton = new JButton("Launch");
		
		dialog.setLayout(new GridLayout(7, 1));
		dialog.add(mapComboBox);
		dialog.add(tileSetBox);
		dialog.add(lazySpritesCheckBox);
		dialog.add(lazySoundsCheckBox);
		dialog.add(soundOnCheckBox);
		dialog.add(musicOnCheckBox);
		dialog.add(launchButton);
		dialog.setResizable(false);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		
		launchButton.requestFocusInWindow();
		
		dialog.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
		
		launchButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				List<String> argsList = new ArrayList<String>();
				
				if (lazySpritesCheckBox.isSelected())
					argsList.add("-lazyLoadSprites");
				
				if (lazySoundsCheckBox.isSelected())
					argsList.add("-lazyLoadSounds");
				
				argsList.add(soundOnCheckBox.isSelected()
					? "-soundOn"
					: "-soundOff"
				);
				
				argsList.add(musicOnCheckBox.isSelected()
					? "-musicOn"
					: "-musicOff"
				);
				
				argsList.add("-" + mapComboBox.getSelectedItem());
				argsList.add("-" + tileSetBox.getSelectedItem());
				
				try
				{
					dialog.setVisible(false);
					TestMP5.main(argsList.toArray(new String[0]));
				}
				catch (IOException e1)
				{
					e1.printStackTrace();
					System.exit(0);
				}
			}
		});
	}
	
	private static String[] getMapAndDemoOptions()
	{
		List<String> mapList = TestMP5.getAvailableMaps();
		Set<String> demoList = TestMP5.getDemos().keySet();
		String[] mapsAndDemos = new String[mapList.size() + demoList.size()];
		
		int i = 0;
		
		for (String mapName : mapList)
		{
			mapsAndDemos[i++] = "map:" + mapName;
		}
		
		for (String demoName : demoList)
		{
			mapsAndDemos[i++] = "demo:" + demoName;
		}
		
		return mapsAndDemos;
	}
	
	private static String[] getTileSetOptions()
	{
		List<String> tileSetList = TestMP5.getAvailableTileSets();
		String[] tileSets = new String[tileSetList.size()];
		
		int i = 0;
		
		for (String tileSetName : tileSetList)
		{
			tileSets[i++] = "tileSet:" + tileSetName;
		}
		
		return tileSets;
	}
}
