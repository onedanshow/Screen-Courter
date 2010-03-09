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

import org.apache.commons.io.FileUtils;

/**
 *
 * @author daniel
 */
public class ScreenRecorder extends ProcessWrapper {
	
	// FILE LOCATIONS
	public static String OUTPUT_FILE = Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"output-java"+(Applet.IS_MAC ? ".mov" : ".mp4");
	//public static File VLC_JAR = new File(System.getProperty("java.class.path")+File.separator+"bin-mac.jar");
	//public static File VLC_JAR = new File("/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar");
	protected static File VLC_EXEC = new File(Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+"VLC");
	
	// VIDEO SETTINGS
	public static double SCALE = 0.8;
	public static int FPS = 10;
	public static int BIT_RATE = 2048;
	
	// STATES
	public final static int RECORDING_STARTED = 100;
	public final static int RECORDING_COMPLETE = 101;
	
    Process recordingProcess;
    StreamGobbler errorGobbler, inputGobbler;

	public void run() {
    	try {
    		if(Applet.IS_MAC) {
	        	List<String> vlcArgs = new ArrayList<String>();
	            vlcArgs.add(VLC_EXEC.getAbsolutePath());
	        	//vlcArgs.add("vlc-bin"+File.separator+"VLC");
	        	vlcArgs.add("-IRC");
	        	//vlcArgs.add("--rc-host=localhost:4444"); // formatting is mac specific
	        	vlcArgs.add("--rc-fake-tty");
	        	vlcArgs.add("--sout=#transcode{vcodec=mp4v,vb="+BIT_RATE+",fps="+FPS+",scale="+SCALE+"}:standard{access=file,mux=mov,dst="+OUTPUT_FILE+"}");
	        	
	            ProcessBuilder pb = new ProcessBuilder(vlcArgs);
	            recordingProcess = pb.start();
	            fireProcessUpdate(RECORDING_STARTED);
	            
	            errorGobbler = new StreamGobbler(recordingProcess.getErrorStream(), false, "vlc E");
	            inputGobbler = new StreamGobbler(recordingProcess.getInputStream(), false, "vlc O");
	            
	            System.out.println("Starting listener threads...");
	            errorGobbler.start();
	            inputGobbler.start();
	            
	            recordingProcess.waitFor();
	            fireProcessUpdate(RECORDING_COMPLETE);
    		} 
    		
    		else if(Applet.IS_LINUX) {
    			File old = new File(OUTPUT_FILE);
    			if( old.exists() && !old.delete() )
    				throw new IOException("Could not delete the old video file: " + old.getAbsolutePath());
    			
    			Toolkit tk = Toolkit.getDefaultToolkit();
    	        Dimension dim = tk.getScreenSize();
    	        
    			List<String> ffmpegArgs = new ArrayList<String>();
    			//ffmpegArgs.add("/usr/bin/ffmpeg");
    	    	ffmpegArgs.add(Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+"ffmpeg");
    	    	// screen capture settings
    	    	ffmpegArgs.addAll(parseParameters("-f x11grab -s "+dim.width+"x"+dim.height+" -r "+FPS+" -b "+BIT_RATE+"k -i :0.0"));
    	    	// microphone settings
    	    	ffmpegArgs.addAll(parseParameters("-f oss -ac 1 -ar "+AudioRecorder.FREQ+" -i /dev/audio"));
    	    	// output file settings
    	    	ffmpegArgs.addAll(parseParameters("-vcodec libx264 -r 10 -s "+Math.round(dim.width*SCALE)+"x"+Math.round(dim.height*SCALE)));
    	    	ffmpegArgs.add(OUTPUT_FILE);
    	    	System.out.println("Executing this command: "+ffmpegArgs.toString());
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
            
      } catch (IOException ioe) {
    	  ioe.printStackTrace();
      } catch (Exception ie) {
    	  ie.printStackTrace();
      }
	}
	
	public void startRecording() {      
		if(Applet.IS_MAC) {
	    	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.println("add screen://");
	    	pw.flush();
		}
		// nothing for linux
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
		}
	}
	
	public void closeDown() {
		if(Applet.IS_MAC) {
	    	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.println("quit");
	    	pw.flush();
		}
		// nothing for linux
	}
	
    protected void finalize() throws Throwable {
    	super.finalize();
    	recordingProcess.destroy();
    }
    
    public static void deleteOutput() {
		File oldOutput = new File(OUTPUT_FILE);
		try {
			if(oldOutput.exists() && !oldOutput.delete())
				throw new Exception("Can't delete the old audio file!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
