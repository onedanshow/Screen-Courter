/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.reelfx.model;


import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.reelfx.Applet;
import com.reelfx.model.util.ProcessWrapper;
import com.reelfx.model.util.StreamGobbler;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import javax.sound.sampled.Mixer;

import org.apache.commons.io.FileUtils;

/**
 *
 * @author daniel
 */
public class ScreenRecorder extends ProcessWrapper {
	
	private static String EXT = Applet.IS_MAC ? ".mov" : Applet.IS_WINDOWS ? ".avi" : ".mp4";
	
	// FILE LOCATIONS
	public static File OUTPUT_FILE = new File(Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"output-java"+EXT);
	//public static File VLC_JAR = new File(System.getProperty("java.class.path")+File.separator+"bin-mac.jar");
	//public static File VLC_JAR = new File("/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar");
	protected static File MAC_EXEC = new File(Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+"mac-screen-recorder");
	protected static File CAM_EXEC = new File(Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+"CamCommandLine.exe");
	
	// VIDEO SETTINGS
	public static double SCALE = 0.8;
	public static int FPS = 15;  // can't go above 20, else it starts dropping frames/audio
	public static int BIT_RATE = 384; // started at 5028 and went down
	
	// STATES
	public final static int RECORDING_STARTED = 100;
	public final static int RECORDING_COMPLETE = 101;
	
    private Process recordingProcess;
    private StreamGobbler errorGobbler, inputGobbler;
    private Mixer audioSource = null;
    private int audioIndex = 0;
    
    /**
     * If this guy is to handle audio as well, give it the Java Mixer object to read from.
     * 
     * @param mixer
     */
	public synchronized void start(Mixer mixer,int index) {
		audioSource = mixer;
		audioIndex = index;
		super.start();
	}

	public void run() {
    	try {
    		if(Applet.IS_MAC) {
	        	List<String> macArgs = new ArrayList<String>();
	            macArgs.add(MAC_EXEC.getAbsolutePath());
	            macArgs.add(OUTPUT_FILE.getAbsolutePath());

	            ProcessBuilder pb = new ProcessBuilder(macArgs);
	            recordingProcess = pb.start();
	            fireProcessUpdate(RECORDING_STARTED);
	            
	            errorGobbler = new StreamGobbler(recordingProcess.getErrorStream(), false, "mac E");
	            inputGobbler = new StreamGobbler(recordingProcess.getInputStream(), false, "mac O");
	            
	            System.out.println("Starting listener threads...");
	            errorGobbler.start();
	            inputGobbler.start();
	            
	            recordingProcess.waitFor();
	            fireProcessUpdate(RECORDING_COMPLETE);
    		} 
    		
    		else if(Applet.IS_LINUX) {
    			deleteOutput();
    			
    			//Toolkit tk = Toolkit.getDefaultToolkit();
    	        //Dimension dim = tk.getScreenSize();
    	        
    			// only get it for the screen we're on
    			int height = Applet.GRAPHICS_CONFIG.getDevice().getDisplayMode().getHeight();
    			int width = Applet.GRAPHICS_CONFIG.getDevice().getDisplayMode().getWidth();
    	        
    			List<String> ffmpegArgs = new ArrayList<String>();
    			//ffmpegArgs.add("/usr/bin/ffmpeg");
    	    	ffmpegArgs.add(Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+"ffmpeg");
    	    	// screen capture settings
    	    	ffmpegArgs.addAll(parseParameters("-f x11grab -s "+width+"x"+height+" -r "+FPS+" -b "+BIT_RATE+"k -i :0.0+0,0"));
    	    	// microphone settings (good resource: http://www.oreilly.de/catalog/multilinux/excerpt/ch14-05.htm)
    	    	if(audioSource != null) {
    	    		String driver = audioIndex > 0 ? "/dev/dsp"+audioIndex : "/dev/dsp";
    	    		ffmpegArgs.addAll(parseParameters("-f oss -ac 1 -ar "+AudioRecorder.FREQ+" -i "+driver));
    	    	}
    	    	// output file settings
    	    	ffmpegArgs.addAll(parseParameters("-vcodec libx264 -r "+FPS+" -s "+Math.round(width*SCALE)+"x"+Math.round(height*SCALE)));
    	    	ffmpegArgs.add(OUTPUT_FILE.getAbsolutePath());
    	    	System.out.println("Executing this command: "+prettyCommand(ffmpegArgs));
    	        ProcessBuilder pb = new ProcessBuilder(ffmpegArgs);
    	        recordingProcess = pb.start();
    	        fireProcessUpdate(RECORDING_STARTED);
    	        
    	        errorGobbler = new StreamGobbler(recordingProcess.getErrorStream(), false, "ffmpeg E");
	            inputGobbler = new StreamGobbler(recordingProcess.getInputStream(), false, "ffmpeg O");
	            
	            System.out.println("Starting listener threads...");
	            errorGobbler.start();
	            inputGobbler.start();
	            
	            recordingProcess.waitFor();
	            
	            fireProcessUpdate(RECORDING_COMPLETE);
    		}
    		
    		else if(Applet.IS_WINDOWS) {
	            List<String> camArgs = new ArrayList<String>();
	            camArgs.add(CAM_EXEC.getAbsolutePath());
	            camArgs.addAll(parseParameters("-outfile "+OUTPUT_FILE));
	            
	        	System.out.println("Executing this command: "+prettyCommand(camArgs));
	            ProcessBuilder pb = new ProcessBuilder(camArgs);
	            recordingProcess = pb.start();
	            fireProcessUpdate(RECORDING_STARTED);
	            
	            errorGobbler = new StreamGobbler(recordingProcess.getErrorStream(), false, "cam E");
	            inputGobbler = new StreamGobbler(recordingProcess.getInputStream(), false, "cam O");
	            
	            System.out.println("Starting listener threads...");
	            errorGobbler.start();
	            inputGobbler.start();
	            
	            recordingProcess.waitFor();
	            
	            fireProcessUpdate(RECORDING_COMPLETE);
    		}
            
      } catch (IOException ioe) {
    	  ioe.printStackTrace();
      } catch (Exception ie) {
    	  ie.printStackTrace();
      }
	}
	
	public void startRecording() {      
		if(Applet.IS_MAC) {
	    	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.println("start");
	    	pw.flush();
		}
		// nothing for linux or windows
	}
	
	public void stopRecording() {   
		if(Applet.IS_LINUX) {
	    	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.print("q");
	    	pw.flush();
		} else if(Applet.IS_MAC) {
	    	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.println("stop");
	    	pw.flush();
		} else if(Applet.IS_WINDOWS) {
			PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.print("\n");
	    	pw.flush();
		}
	}
	
	public void closeDown() {
		if(Applet.IS_MAC && recordingProcess != null) {
	    	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.println("quit");
	    	pw.flush();
		}
		// nothing for linux or windows
	}
	
    protected void finalize() throws Throwable {
    	super.finalize();
    	closeDown();
    	recordingProcess.destroy();
    }
    
    public static void deleteOutput() {
		try {
			if(OUTPUT_FILE.exists() && !OUTPUT_FILE.delete())
				throw new Exception("Can't delete the old video file!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
