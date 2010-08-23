package com.reelfx.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.Timer;

import org.apache.log4j.Logger;

import com.reelfx.Applet;

/**
 * Singleton to manage reading/writing preferences between application runs.
 * 
 * @author Daniel Dixon (http://www.danieldixon.com)
 *
 */
public class PreferencesManager {

	public static File OUTPUT_FILE = new File(Applet.BASE_FOLDER.getAbsolutePath()+File.separator+"preferences");
	public final static String FROM_PREFERENCES = "from_prefs"; // should be lowercase
	
	private static Logger logger = Logger.getLogger(PreferencesManager.class);
	private static int x = -1, y = -1, width = -1, height = -1;
	private static boolean forceRecordingToolsToCorner = false;
	private static Timer timer = null;
	private static boolean initialized = false;

	public static boolean hasPreferences() {
		return OUTPUT_FILE.exists();
	}
	
	public static void read() {
		if(!OUTPUT_FILE.exists()) return;
		
		try {
			BufferedReader input =  new BufferedReader(new FileReader(OUTPUT_FILE));
			x = Integer.parseInt(input.readLine());
			y = Integer.parseInt(input.readLine());
			width = Integer.parseInt(input.readLine());
			height = Integer.parseInt(input.readLine());
			forceRecordingToolsToCorner = Boolean.parseBoolean(input.readLine());
			input.close();
			initialized = true;
	    }
	    catch (Exception ex){
	      logger.error("Could not read the preferences file",ex);
	    }
	}
	
	public static void write() {
		// wait to write until updates have died down (i.e. while dragging capture viewport)
		if(timer == null) {
			timer = new Timer(300, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					writeWhenReady();
				}
			});
			timer.setRepeats(false);
		}
		timer.restart();
	}
	
	private static void writeWhenReady() {
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(OUTPUT_FILE));
			try {
				// get the most up-to-date data
				x = Applet.CAPTURE_VIEWPORT.x;
				output.write(x+"\n");
				y = Applet.CAPTURE_VIEWPORT.y;
				output.write(y+"\n");
				width = Applet.CAPTURE_VIEWPORT.width;
				output.write(width+"\n");
				height = Applet.CAPTURE_VIEWPORT.height;
				output.write(height+"\n");
				output.write(forceRecordingToolsToCorner+"\n");
			}
			finally {
				System.out.println("Saved preferences: "+x+" "+y+" "+width+" "+height+" "+forceRecordingToolsToCorner);
				output.close();
			}
		}
		catch (Exception ex) {
			logger.error("Could not write the preferences file",ex);
		}
	}

	public static void deleteOutput() {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				try {
					if(OUTPUT_FILE.exists() && !OUTPUT_FILE.delete())
						throw new Exception("Can't delete the old preference file!");
				} catch (Exception e) {
					logger.error(e.getMessage(),e);
				}
				return null;
			}
		});
	}
	
	public static int getX() {
		if(!initialized) read();
		return x;
	}

	public static void setX(int value) {
		x = value;
	}

	public static int getY() {
		if(!initialized) read();
		return y;
	}

	public static void setY(int value) {
		y = value;
	}

	public static int getWidth() {
		if(!initialized) read();
		return width;
	}

	public static void setWidth(int value) {
		width = value;
	}

	public static int getHeight() {
		if(!initialized) read();
		return height;
	}

	public static void setHeight(int value) {
		height = value;
	}

	public static boolean isForceRecordingToolsToCorner() {
		if(!initialized) read();
		return forceRecordingToolsToCorner;
	}

	public static void setForceRecordingToolsToCorner(boolean value) {
		forceRecordingToolsToCorner = value;
	}
}
