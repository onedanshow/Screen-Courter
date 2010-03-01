package com.reelfx;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.reelfx.util.StreamGobbler;

public class PostProcessor extends Thread {
	
	public static String OUTPUT_FILE = RfxApplet.DESKTOP_FOLDER.getAbsolutePath()+File.separator+"output-final.mp4";
	
	Process postProcess;
	StreamGobbler errorGobbler, inputGobbler;
	
	public void run() {
		try {
	        //Process p = Runtime.getRuntime().exec("/Applications/VLC.app/Contents/MacOS/VLC -I telnet --telnet-host=localhost:4442 -I rc --rc-host=localhost:4444");
	        //Process p = Runtime.getRuntime().exec("/Applications/VLC.app/Contents/MacOS/VLC -I rc --rc-host=localhost:4444");
	
	    	List<String> vlcArgs = new ArrayList<String>();
	    	vlcArgs.add("/opt/local/bin/ffmpeg");
	    	vlcArgs.add("-ar");
	    	vlcArgs.add("44100");
	    	vlcArgs.add("-i");
	    	vlcArgs.add(AudioRecorder.OUTPUT_FILE);
	    	vlcArgs.add("-i"); 
	    	vlcArgs.add(ScreenRecorder.OUTPUT_FILE);
	    	vlcArgs.add("-vcodec"); 
	    	vlcArgs.add("libx264");
	    	vlcArgs.add("-vpre");
	    	vlcArgs.add("normal");
	    	vlcArgs.add(OUTPUT_FILE);
	        ProcessBuilder pb = new ProcessBuilder(vlcArgs);
	        postProcess = pb.start();
	
	        errorGobbler = new StreamGobbler(postProcess.getErrorStream(), false, "ffmpeg E");
	        inputGobbler = new StreamGobbler(postProcess.getInputStream(), false, "ffmpeg O");
	        
	        System.out.println("Starting listener threads...");
	        errorGobbler.start();
	        inputGobbler.start();   
	        
	        // TODO monitor the progress of the event
	        // TODO delete the temporary files when done
	        // TODO allow canceling of the transcoding
	        // TODO increment output file name if another already exists
	        
	  } catch (IOException ioe) {
		  ioe.printStackTrace();
	  } catch (Exception ie) {
		  ie.printStackTrace();
	  }
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		postProcess.destroy();
	}
}
