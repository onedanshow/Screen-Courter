package com.reelfx.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.reelfx.Applet;
import com.reelfx.model.util.ProcessWrapper;
import com.reelfx.model.util.StreamGobbler;

public class PostProcessor extends ProcessWrapper implements ActionListener {
	
	// FILE LOCATIONS
	public static String OUTPUT_FILE = Applet.DESKTOP_FOLDER.getAbsolutePath()+File.separator+"output-final.mp4";
	
	// STATES
	public final static int POST_PROCESS_COMPLETE = 0;
	
	protected Process postProcess;
	protected StreamGobbler errorGobbler, inputGobbler;
	
	public void run() {
		try {
	        //Process p = Runtime.getRuntime().exec("/Applications/VLC.app/Contents/MacOS/VLC -I telnet --telnet-host=localhost:4442 -I rc --rc-host=localhost:4444");
	        //Process p = Runtime.getRuntime().exec("/Applications/VLC.app/Contents/MacOS/VLC -I rc --rc-host=localhost:4444");
			if(Applet.IS_MAC) {
				List<String> ffmpegArgs = new ArrayList<String>();
		    	ffmpegArgs.add(Applet.RFX_FOLDER.getAbsoluteFile()+File.separator+"bin-mac"+File.separator+"ffmpeg");
		    	ffmpegArgs.add("-ar");
		    	ffmpegArgs.add("44100");
		    	ffmpegArgs.add("-i");
		    	ffmpegArgs.add(AudioRecorder.OUTPUT_FILE);
		    	ffmpegArgs.add("-i"); 
		    	ffmpegArgs.add(ScreenRecorder.OUTPUT_FILE);
		    	ffmpegArgs.addAll(getFfmpegX264Params());
		    	ffmpegArgs.add(OUTPUT_FILE);
		        ProcessBuilder pb = new ProcessBuilder(ffmpegArgs);
		        postProcess = pb.start();
		
		        errorGobbler = new StreamGobbler(postProcess.getErrorStream(), false, "ffmpeg E");
		        inputGobbler = new StreamGobbler(postProcess.getInputStream(), false, "ffmpeg O");
		        
		        System.out.println("Starting listener threads...");
		        errorGobbler.addActionListener("frame", this);
		        //inputGobbler.addActionListener("ffmpeg", this);
		        errorGobbler.start();
		        inputGobbler.start();  
		        
		        postProcess.waitFor();
			}
			else if(Applet.IS_LINUX) {
				FileUtils.moveFile(new File(ScreenRecorder.OUTPUT_FILE), new File(OUTPUT_FILE));
			}
	        
	        fireProcessUpdate(POST_PROCESS_COMPLETE);
	        
	        // TODO monitor the progress of the event
	        // TODO delete the temporary files when done
	        // TODO allow canceling of the transcoding
	        // TODO increment output file name if another already exists
	        // TODO allow people to save to desktop if they wish
	        
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
	
	/**
	 * Called when a stream gobbler finds a line that relevant to this wrapper.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().contains("frame")) {
			System.out.println("Found a frame!");
		}
	}
}
